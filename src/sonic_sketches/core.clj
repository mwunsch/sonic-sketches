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
  and :duration. Optionally, accepts a third parameter: a promise that
  will be delivered once the sequence of notes has been played. This
  option is used internally for temporal recursion."
  ([nome notes]
   (let [will-complete (promise)]
     (play nome notes will-complete)
     will-complete))
  ([nome notes completion-promise]
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
                   [nome (rest notes) completion-promise]))
       (apply-at (+ t (metro-tick nome)) #'deliver [completion-promise nome])))))

(defn -main
  [& args]
  (let [path "./sounds/test.wav"]
    (println "🎼 Recording to" path "now.")
    (recording-start path)
    (let [will-complete (play (metronome 120) auld-lang-syne)]
      (when-let [nome @will-complete]
        (let [recorded-to (recording-stop)]
          (println "Finished recording to" recorded-to "🎶"))))))
