(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.piano]
            [clojure.core.async :as async])
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

(defn -main
  [& args]
  (let [tempfile (java.io.File/createTempFile "test" ".wav")
        path (.getPath tempfile)]
    (println "🎼 Recording to" path "now.")
    (recording-start path)
    (async/<!! (play (metronome 120) auld-lang-syne))
    (let [recorded-to (recording-stop)]
      (println "Finished recording to" recorded-to "🎶"))))
