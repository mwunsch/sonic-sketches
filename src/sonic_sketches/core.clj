(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.piano]
            [clojure.core.async :as async]
            [amazonica.aws.s3 :as s3])
  (:gen-class))

(def auld-lang-syne
  [{:chord (chord :F3 :major) :duration 1}

   {:chord (concat (chord :F3 :major) [(note :F4)]) :duration 1.5}
   {:chord (concat (chord :F3 :major) [(note :F4)]) :duration 0.5}
   {:chord (concat [(note :F3) (note :C4)] [(note :F4)]) :duration 1}
   {:chord (concat [(note :F3) (note :A3)] [(note :F4) (note :A4)]) :duration 1}

   {:chord (concat [(note :C3)] (chord :C4 :major)) :duration 1.5}
   {:chord (concat [(note :C3)] [(note :C4) (note :D4) (note :F4)]) :duration 0.5}
   {:chord (concat [(note :C3)] (chord :C4 :major)) :duration 1}
   {:chord (concat [(note :C3) (note :Bb3)] [(note :E4) (note :A4)]) :duration 1}

   {:chord (concat (chord :F3 :major) [(note :F4)]) :duration 1.5}
   {:chord (concat (chord :F3 :major) [(note :F4)]) :duration 0.5}
   {:chord (concat [(note :F3) (note :C4)] [(note :F4) (note :A4)]) :duration 1}
   {:chord (concat [(note :F3) (note :A3)] [(note :F4) (note :C5)]) :duration 1}

   {:chord (concat [(note :Bb2)] [(note :Bb3) (note :F4) (note :D4)]) :duration 3}
   {:chord (concat [(note :Bb2)] [(note :Bb3) (note :F4) (note :D4)]) :duration 1}

   {:chord (concat [(note :F3) (note :A3)] [(note :F4) (note :C5)]) :duration 1.5}
   {:chord (concat [(note :F3) (note :C4)] [(note :F4) (note :A4)]) :duration 0.5}
   {:chord (concat [(note :F3) (note :C4)] [(note :F4) (note :A4)]) :duration 1}
   {:chord (concat (chord :F3 :major) [(note :F4)]) :duration 1}

   {:chord (concat [(note :C3)] (chord :C4 :major)) :duration 1.5}
   {:chord (concat [(note :C3)] [(note :C4) (note :D4) (note :F4)]) :duration 0.5}
   {:chord (concat [(note :C3)] (chord :C4 :major)) :duration 1}
   {:chord (concat [(note :C3) (note :Bb3)] [(note :E4) (note :A4)]) :duration 1}

   {:chord (concat [(note :F3) (note :C4)] [(note :Bb3) (note :F4)]) :duration 1.5}
   {:chord (concat [(note :F3) (note :C4)] [(note :D4)]) :duration 0.5}
   {:chord (concat [(note :F3) (note :A3)] [(note :D4)]) :duration 1}
   {:chord (chord :F3 :major) :duration 1}

   {:chord (concat (chord :F3 :major) [(note :F4)]) :duration 3}
   ])

(defn play
  "Accepts a metronome and a sequence of notes with a :chord
  and :duration. Returns a core.async channel that, when read, will
  block until the song has completed and returns the metronome.

  In its 3-arity form accepts a channel as a third parameter and will
  issue a write when the song is completed. Used internally."
  ([nome notes]
   (async/go (async/<! (let [channel (async/chan)]
                         (play nome notes channel)
                         channel))))

  ([nome notes channel]
   (let [t (now)
         beat (nome)]
     (if-let [note (first notes)]
       (let [{chord :chord duration :duration} note
             decay (* (metro-tick nome) duration)]
         (doseq [pitch chord]
           (at t (overtone.inst.piano/piano
                  :note pitch
                  :decay 0.25)))
         (apply-at (+ t decay) #'play
                   [nome (rest notes) channel]))
       (apply-at (+ t (metro-tick nome)) async/>!! [channel nome])))))

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
        path (.getPath tempfile)]
    (-> (make-recording path (play (metronome 120) auld-lang-syne))
        upload-to-s3)))
