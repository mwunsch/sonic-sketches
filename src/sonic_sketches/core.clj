(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.drum :as drums]
            [overtone.inst.synth :refer [tb303]]
            [clojure.core.async :as async]
            [amazonica.aws.s3 :as s3])
  (:gen-class))

(defn sequencer
  "Accepts a metronome, an instrument, and a vector of 0's or 1's. If
  the pulse is 1, the instrument will play. To be used something like:

  (let [nome (metronome 120)
        drumkit [drums/kick drums/snare drums/hat3]]
    (doseq [drum drumkit] (sequencer nome drum (repeatedly 8 #(choose [0 1])))))

  Or something of that nature."
  ([nome instrument pulses]
   (let [channel (async/chan)]
     (sequencer nome instrument pulses channel)
     channel))

  ([nome instrument pulses channel]
   (let [beat (nome)]
     (if-let [pulse (first pulses)]
       (do
         (when (pos? pulse)
           (at (nome beat) (instrument)))
         (apply-by (nome (inc beat)) #'sequencer [nome instrument (rest pulses) channel]))
       (apply-at (nome (inc beat)) async/>!! [channel [nome (:name instrument)]])))))

(defn multisequence
  "Accepts a metronome and a vector of instructions. Each instruction
  is a pair of instruments and a sequence of 0's or 1's. Returns an
  async channel that will be blocked until the first sequence
  completes. eg.

  (let [nome (metronome 120)
        instruments [drums/kick drums/snare drums/tom]]
    (multisequence nome (map vector instruments (partition 8 (repeatedly #(choose [0 1]))))))"
  [nome instructions]
  (async/go (async/alts! (vec (for [[instrument pulses] instructions]
                                (sequencer nome instrument pulses))))))

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
  (let [aws-credentials {:profile "sonic-sketch"}
        tempfile (java.io.File/createTempFile "test" ".wav")
        path (.getPath tempfile)
        percussion [drums/kick drums/snare drums/tom drums/hat3]
        drumsequence (map vector percussion (partition 8 (repeatedly #(choose [0 1]))))
        nome (metronome 120)]
    (-> (make-recording path (multisequence nome drumsequence))
        upload-to-s3)))
