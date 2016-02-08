(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.drum :as drums]
            [overtone.inst.synth :refer [tb303]]
            [clojure.core.async :as async]
            [clojure.data.generators :as datagen]
            [amazonica.aws.s3 :as s3])
  (:gen-class))

(defn step-sequencer
  "Accepts a metronome, an instrument, and a seq of 0's or 1's. If
  the pulse is 1, the instrument will play. To be used something like:

  (let [nome (metronome 120)
        drumkit [drums/kick drums/snare drums/hat3]]
    (doseq [drum drumkit] (sequencer nome drum (repeatedly 8 #(choose [0 1])))))

  Or something of that nature."
  ([nome instrument pulses]
   (let [channel (async/chan)]
     (step-sequencer nome instrument pulses channel)
     channel))

  ([nome instrument pulses channel]
   (let [t (now)
         tick (metro-tick nome)
         bars (metro-bpb nome)
         drum (:name instrument)]
     (if-let [pulse (first pulses)]
       (do
         (when (pos? pulse)
           (at t (instrument)))
         (apply-at (+ t (/ tick bars)) #'step-sequencer [nome instrument (rest pulses) channel]))
       (apply-at (+ t tick) async/>!! [channel [nome drum]])))))

(defn drummachine
  "Accepts a metronome and a vector of instructions. Each instruction
  is a pair of instruments and a sequence of 0's or 1's. Returns a seq
  of async channels suitable for use with `alts!!`"
  [nome instructions]
  (for [[instrument pulses] instructions]
    (async/go (async/<! (step-sequencer nome instrument pulses)))))

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
  {:adagio (range 66 76)
   :andante (range 76 108)
   :moderato (range 108 120)
   :allegro (range 120 168)
   :vivace (range 168 176)
   :presto (range 168 200)})

(defn rand-metronome
  "Given a tempo, choose a random BPM."
  [tempo]
  (->> (tempo tempo-map)
       datagen/rand-nth
       metronome))

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
  [path]
  (let [credentials {:profile "sonic-sketch"}
        recording (java.io.File. path)
        key-name (.getName recording)]
    (println "Uploading" key-name "to S3")
    (s3/put-object credentials
                   :bucket-name "sonic-sketches"
                   :key key-name
                   :file recording)))

(defn -main
  [& args]
  (let [tempfile (java.io.File/createTempFile "test" ".wav")
        path (.getPath tempfile)
        seed (now)]
    (binding [datagen/*rnd* (java.util.Random. seed)]
      (let [percussion [drums/kick drums/snare drums/tom drums/closed-hat drums/open-hat]
            nome (metronome 86)
            drumsequence (rand-drumsequence percussion)]
        (println "RNG Seed:" seed)
        (-> (make-recording path (async/go (async/alts! (drummachine nome drumsequence))))
            upload-to-s3)))))
