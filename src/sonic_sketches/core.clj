(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.drum :as drums]
            [overtone.inst.synth :refer [tb303 overpad]]
            [clojure.core.async :as async]
            [clojure.data.generators :as datagen]
            [amazonica.aws.s3 :as s3])
  (:gen-class))

(defn clock-signal
  "Given a metronome, returns a channel that is fed on every beat."
  [nome]
  (let [out (async/chan)]
    (async/go-loop [tick (metro-tick nome)]
      (when-let [pulse (async/>! out nome)]
        (do
          (async/<! (async/timeout tick))
          (recur (metro-tick nome)))))
    out))

(defn sequencer
  "Abstracting the notion of playing through a sequence, this fn takes
  a clock, an input channel, and a lambda. It takes
  input off the channel at every clock tick and calls the lambda with
  the input if pred returns true for that step. Returns an async
  channel that will block until the input is closed and returns the
  metronome powering the clock. e.g.

    (async/<!! (sequencer (clock-pulse (metronome 96))
                              (async/to-chan (range 16))
                              (fn [x] (drums/kick))
                              (constantly true)))

  performs a blocking take until 16 beats have elapsed."
  ([clock in f]
   (sequencer clock in f (constantly true)))

  ([clock in f pred]
   (let [out (async/chan)]
     (async/go-loop []
       (if-let [pulse (async/<! clock)]
         (if-let [step (async/<! in)]
           (do
             (when (pred step) (f step))
             (recur))
           (let [tick (metro-tick pulse)]
             (async/<! (async/timeout tick)) ; wait one additional tick before setting val of chan
             (async/>! out pulse)
             (async/close! out)))
         (async/close! out)))
     out)))

(defn drummachine
  "Accepts a metronome and a vector of instructions. Each instruction
  is a pair of instruments and a sequence of 0's or 1's. Returns a
  single async channel that blocks until all sequences have been
  completed."
  [nome instructions]
  (let [bpb (metro-bpb nome)
        bpm (metro-bpm nome)
        metro (metronome (* bpm bpb))
        clock (clock-signal metro)
        multiclock (async/mult clock)]
    (->> (for [[instrument pulses] instructions
               :let [in (async/to-chan pulses)
                     tap (async/tap multiclock (async/chan))]]
           (sequencer tap
                      in
                      (fn [x] (instrument))
                      pos?))
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

(defn rand-metronome
  "Given a tempo, choose a random BPM."
  [tempo]
  (->> (tempo tempo-map)
       datagen/rand-nth
       metronome))

(defn gen-song
  "With a seed for a RNG, compose a song. Returns a seq of async
  channels."
  [seed]
  (binding [datagen/*rnd* (java.util.Random. seed)]
    (let [tempo :allegro
          scale (scale :D3 :minor)
          metro (rand-metronome tempo)
          clock (clock-signal metro)
          drums (datagen/reservoir-sample 5 percussion)
          drumsequence (-> (rand-drumsequence drums)
                           (loop-sequence 8))
          lead (partial tb303
                        :amp 0.9
                        :cutoff (datagen/rand-nth (range 500 20000))
                        :wave (datagen/rand-nth (range 3))
                        :decay (/ (metro-tick metro) 1000))
          notes (->> scale
                     (rand-notesequence 8)
                     (repeat 4)
                     (apply concat)
                     (async/to-chan))]
      (->> [(drummachine metro drumsequence)
            (sequencer clock notes #(apply lead %))]
           async/merge
           (async/into [])))))

(defmacro make-recording
  [path out]
  `(do
     (println "🎼 Recording to" ~path "now.")
     (recording-start ~path)
     (async/<!! ~out)
     (let [recorded# (recording-stop)]
       (println "Finished recording to" recorded# "🎶")
       recorded#)))

(defn upload-to-s3
  "Upload a file at path to s3"
  [path & metadata]
  (let [credentials {:profile "sonic-sketch"}
        recording (java.io.File. path)
        key-name (.getName recording)]
    (println "Uploading" key-name "to S3")
    (s3/put-object credentials
                   :bucket-name "sonic-sketches"
                   :key key-name
                   :file recording
                   :metadata {:user-metadata (apply hash-map metadata)})))

(defn -main
  [& args]
  (let [tempfile (java.io.File/createTempFile "test" ".wav")
        path (.getPath tempfile)
        seed (now)
        current-version (System/getProperty "sonic-sketches.version")]
    (println "🎲 RNG Seed:" seed)
    (-> (make-recording path (gen-song seed))
        (upload-to-s3 :rng-seed seed
                      :version current-version))))
