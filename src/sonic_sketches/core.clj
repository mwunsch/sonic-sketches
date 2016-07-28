(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.drum :as drums]
            [overtone.inst.synth :refer [tb303]]
            [clojure.core.async :as async]
            [clojure.data.generators :as datagen]
            [amazonica.aws.s3 :as s3]
            [sonic-sketches.forecast :as forecast]
            [taoensso.timbre :as logger])
  (:gen-class))

(def signals
  (atom '()))

(defn clock-signal
  "Given a metronome, returns a channel that is fed on every beat."
  [nome]
  (let [out (async/chan)]
    (async/go-loop [tick (metro-tick nome)]
      (when-let [pulse (async/>! out nome)]
        (do
          (async/<! (async/timeout tick))
          (recur (metro-tick nome)))))
    (swap! signals conj out)
    out))

(defn kill-signals
  "Close all clock channels and remove them from the list of active
  signals."
  []
  (swap! signals #(->> %
                      (map async/close!)
                      (remove nil?))))

(defn sequencer
  "Abstracting the notion of playing through a sequence, this fn takes
  a clock, an input channel, and a lambda. It takes input off the
  channel at every clock tick and calls the lambda with the
  input. Returns an async channel that will block until the input is
  closed and returns the metronome powering the clock. e.g.

    (async/<!! (sequencer (clock-pulse (metronome 96))
                              (async/to-chan (range 16))
                              (fn [x] (drums/kick))))

  performs a blocking take until 16 beats have elapsed."
  [clock in f]
  (let [out (async/chan)]
    (async/go-loop []
      (if-let [pulse (async/<! clock)]
        (if-let [step (async/<! in)]
          (do
            (f step)
            (recur))
          (do
            (async/<! (async/timeout 500)) ; wait half a sec before setting val of chan
            (async/>! out pulse)
            (async/close! out)))
        (async/close! out)))
    out))

(defn drummachine
  "Accepts a clock signal and a vector of instructions. Each
  instruction is a pair of instruments and a sequence of 0's or
  1's. Returns a single async channel that blocks until all sequences
  have been completed."
  [clock instructions]
  (let [multiclock (async/mult clock)]
    (->> (for [[instrument pulses] instructions
               :let [in (async/to-chan pulses)
                     tap (async/tap multiclock (async/chan))
                     instfn #(when (pos? %) (instrument))]]
           (sequencer tap in instfn))
         async/merge
         (async/into []))))

(defn rand-notesequence
  "Produce a n notes from given scale randomly. Returns a vector
  suitable for application to a tb303."
  [n scale]
  (mapv (partial vector :note) (repeatedly n #(datagen/rand-nth scale))))

(defn rand-drumsequence
  "Randomly generates a drum sequence for each percussion
  instrument. Accepts a vector of instruments and the number of steps
  to generate. Defaults to 16 steps."
  ([percussion] (rand-drumsequence percussion 16))
  ([percussion nsteps]
   (->> #(datagen/rand-nth [0 1])
        repeatedly
        (partition nsteps)
        (mapv vector percussion))))

(defn loop-sequence
  "Loop each drum in a drumsequence n times."
  [drumsequences n]
  (for [drum drumsequences]
    (update-in drum [1] #(flatten (repeat n %)))))

(defn cycle-sequence
  "Loop the drum pattern sequence infinitely."
  [drumsequences]
  (for [drum drumsequences]
    (update-in drum [1] cycle)))

(def four-on-the-floor
  [[drums/kick       [1 0 0 0 1 0 0 0 1 0 0 0 1 0 0 0]]
   [drums/clap       [0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0]]
   [drums/closed-hat [0 0 1 0 0 0 1 0 0 0 1 0 0 0 1 0]]])

(def tempo-map
  "The values represent ranges of BPM. See:
  https://en.wikipedia.org/wiki/Tempo#Italian_tempo_markings"
  {:adagio   (range 66 76)
   :andante  (range 76 108)
   :moderato (range 108 120)
   :allegro  (range 120 168)
   :vivace   (range 168 176)
   :presto   (range 168 200)})

(def percussion
  "A list of all possible drums"
  (->> 'drums
       (get (ns-aliases *ns*))
       ns-publics
       vals
       (map var-get)))

(defn lunar-illumination
  "The lunar phase is a number 0 - 100 which indicates 'completeness'
  of a moon's lunar cycle. 50 represents a full moon. This fn
  determines how 'full' the moon is for a given phase on a scale of 0
  thru 5 where 5 is a full moon."
  [phase]
  (-> phase
      (- 50)
      Math/abs
      (- 50)
      Math/abs
      (/ 10)
      float
      Math/round))

(defn lunar-str
  "Given a lunar phase number, returns an Emoji string displaying a
  visual representation of the moon for that phase."
  [phase]
  (let [moons (->> (range 0x1f311 0x1f319)
                   (map int)
                   (mapv (partial format "%c")))
        indices (- (count moons) 1)]
    (nth moons (-> phase
                   (/ (quot 100 indices))
                   float
                   Math/round))))

(defn daily-data->map
  "Returns a  map of valuable  data points computed from  Forecast API
  data. Map values will be nil if API data is missing."
  [data]
  (let [{moon-phase :moonPhase
         hi-temp :apparentTemperatureMax
         lo-temp :apparentTemperatureMin
         sunrise :sunriseTime
         sunset :sunsetTime
         precip-probability :precipProbability
         precip-intensity :precipIntensity
         cloudy :cloudCover} (apply merge data)]
    {:lunar-phase (some->> moon-phase
                           (* 100)
                           Math/round)
     :avg-temp (when (and (some? hi-temp) (some? lo-temp))
                 (/ (+ hi-temp lo-temp) 2))
     :length-of-day (when (and (some? sunrise) (some? sunset))
                      (float (/ (- sunset sunrise) 3600)))
     :precip (when (and (some? precip-probability) (some? precip-intensity))
               precip-intensity)
     :precip-prob precip-probability
     :cloudy (when (some? cloudy) (> cloudy 0.75))}))

(def key-range
  (let [notes (vals (into (sorted-map) REVERSE-NOTES))
        octaves (zipmap (range 2 5) (repeat notes))]
    (->> (for [[octave key] octaves]
           (mapv #(mk-midi-string % octave) key))
         (apply concat)
         (into []))))

(defn temperature->key
  [temp]
  (let [index-max (- (count key-range) 1)
        scaled (-> (scale-range temp 0 100 0 index-max)
                   float
                   Math/round)
        index (cond
                (< scaled 0) 0
                (< 100 scaled) index-max
                :else scaled)]
    (nth key-range index)))

(defn gen-song
  "With a seed for a RNG, compose a song. Returns a seq of async
  channels."
  [seed & weather]
  (binding [datagen/*rnd* (java.util.Random. seed)]
    (let [today (apply merge weather)
          {:keys [lunar-phase avg-temp length-of-day precip precip-prob cloudy]
           :or {lunar-phase (datagen/uniform 0 100)
                avg-temp (datagen/uniform 0 100)
                length-of-day (datagen/uniform 6 16)
                precip (datagen/rand-nth [0 0.002 0.017 0.1 0.4])
                precip-prob (datagen/float)
                cloudy (datagen/boolean)}
           :as daily-data} (into {} (filter (comp some? val) (daily-data->map today)))
          pitch-key (temperature->key avg-temp)
          interval (if (> precip-prob 0.5)
                     (condp <= precip
                       0.1 :minor
                       0.017 :minor-pentatonic
                       0.002 :harmonic-minor
                       :major-pentatonic)
                     (if cloudy :lydian :major))
          scale (scale pitch-key interval)
          tempo (->> (lunar-illumination lunar-phase)
                     (nth (keys tempo-map)))
          bpm (->> (tempo tempo-map)
                   datagen/rand-nth)
          metro (metronome (* 2 bpm))
          drum-clock (clock-signal (metronome (* 4 bpm)))
          lead-clock (clock-signal metro)
          drums (datagen/reservoir-sample 5 percussion)
          drumsequence (-> (rand-drumsequence drums)
                           (loop-sequence 8))
          lead (partial tb303
                        :amp 0.9
                        :wave (datagen/rand-nth (range 3))
                        :r (datagen/rand-nth (range 0.01 0.80 0.01))
                        :decay (/ (metro-tick metro) 1000))
          notes (->> scale
                     (rand-notesequence 8)
                     (repeat 8)
                     (apply concat)
                     (map #(conj %
                                 :cutoff (datagen/rand-nth (range 1000 20000))))
                     (async/to-chan))]
      (logger/info (str "🎲 RNG Seed: "
                        seed " "
                        (lunar-str lunar-phase)
                        " BPM: " bpm
                        " 🎵 " pitch-key " " (name interval)
                        " ☁ " precip " in/hr"
                        " 🌡 " avg-temp " ℉"))
      (async/go
        (async/<! (->> [(drummachine drum-clock drumsequence)
                        (sequencer lead-clock notes #(apply lead %))]
                       async/merge
                       (async/into [])))
        {:rng-seed seed
         :lunar-phase lunar-phase
         :bpm bpm
         :pitch pitch-key
         :interval (name interval)
         :precipitation precip
         :avg-temp avg-temp}))))

(defmacro make-recording
  [path out]
  `(do
     (recording-start ~path)
     (let [songmap# (async/<!! ~out)
           recorded# (recording-stop)]
       [recorded# songmap#])))

(defn upload-to-s3
  "Upload a file at path to s3"
  [bucket path metadata]
  (let [recording (java.io.File. path)
        key-name (.getName recording)]
    (logger/info "Uploading" key-name "to S3 bucket" bucket)
    (s3/put-object :bucket-name bucket
                   :key key-name
                   :file recording
                   :metadata {:user-metadata metadata})))

(defn play-generated-song
  "A handy convenience function to play a song from a given time, or
  to generate one for the current time."
  ([] (play-generated-song (now)))
  ([seed]
   (let [weather (some-> (forecast/nyc-at seed)
                         :body
                         :daily
                         :data)]
     (gen-song seed weather))))

(defn local-datetime
  "For a given seed (milliseconds since unix epoch) return a java LocalDateTime"
  [seed]
  (let [tz (java.time.ZoneId/of "America/New_York")
        instant (java.time.Instant/ofEpochMilli seed)]
    (java.time.LocalDateTime/ofInstant instant tz)))

(defn generate->record->upload
  [& args]
  (let [seed (now)
        datetime (local-datetime seed)
        day-of-week (-> datetime
                        (.format (java.time.format.DateTimeFormatter/ofPattern "EEEE"))
                        .toLowerCase)
        iso8601 (.format datetime java.time.format.DateTimeFormatter/ISO_LOCAL_DATE)
        tempfile (java.io.File/createTempFile (str day-of-week "-" iso8601 "_") ".wav")
        path (.getPath tempfile)
        current-version (System/getProperty "sonic-sketches.version")
        bucket (or (first args) "sonic-sketches")
        {:keys [latitude longitude daily] :as weather} (try (:body (forecast/nyc-at seed))
                                                            (catch Exception e (logger/error "Error fetching Forecast API"
                                                                                             (.getMessage e))))]
    (logger/info "🎼 Recording to" path "now.")
    (let [[recorded-to song-meta] (make-recording path (gen-song seed (:data daily)))]
      (logger/info "Finished recording to" recorded-to "🎶")
      (upload-to-s3 bucket
                    recorded-to
                    (merge song-meta {:version current-version
                                      :latitude latitude
                                      :longitude longitude
                                      :day-of-week day-of-week
                                      :iso8601 iso8601})))))

(defn -main
  [& args]
  (volume 0.6)
  (apply generate->record->upload args)
  (kill-server)
  (shutdown-agents)
  (System/exit 0))
