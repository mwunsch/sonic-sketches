(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.piano])
  (:gen-class))

(def auld-lang-syne
  [{:chord [(note :C3)] :duration 1/4}

   {:chord [(note :F3)] :duration 3/8}
   {:chord [(note :F3)] :duration 1/8}
   {:chord [(note :F3)] :duration 1/4}
   {:chord [(note :A3)] :duration 1/4}

   {:chord [(note :G3)] :duration 3/8}
   {:chord [(note :F3)] :duration 1/8}
   {:chord [(note :G3)] :duration 1/4}
   {:chord [(note :A3)] :duration 1/8}
   {:chord [(note :G3)] :duration 1/8}

   {:chord [(note :F3)] :duration 3/8}
   {:chord [(note :F3)] :duration 1/8}
   {:chord [(note :A3)] :duration 1/4}
   {:chord [(note :C4)] :duration 1/4}
   {:chord [(note :D4)] :duration 3/4}
   {:chord [(note :D4)] :duration 1/4}

   {:chord [(note :C4)] :duration 3/8}
   {:chord [(note :A3)] :duration 1/8}
   {:chord [(note :A3)] :duration 1/4}
   {:chord [(note :F3)] :duration 1/4}

   {:chord [(note :G3)] :duration 3/8}
   {:chord [(note :F3)] :duration 1/8}
   {:chord [(note :G3)] :duration 1/4}
   {:chord [(note :A3)] :duration 1/8}
   {:chord [(note :G3)] :duration 1/8}

   {:chord [(note :F3)] :duration 3/8}
   {:chord [(note :D3)] :duration 1/8}
   {:chord [(note :D3)] :duration 1/4}
   {:chord [(note :C3)] :duration 1/4}

   {:chord [(note :F3)] :duration 3/4}
   ])

(defn play
  "Accepts a metronome, and a sequence of maps with :chord and :duration.
  When the note sequence is empty, on the next beat
  a ::finished-playing event is triggered"
  [nome notes]
  (let [beat (nome)]
    (if-let [note (first notes)]
      (let [{chord :chord duration :duration} note
            decay (* (metro-tick nome) duration)]
        (when (some? chord)
          (doseq [pitch chord]
            (at (nome beat)
                (overtone.inst.piano/piano
                 :note pitch
                 :decay (+ (/ decay 1000) 0.2)))))
        (apply-by (+ (nome (inc beat)) decay) #'play [nome (rest notes)]))
      (apply-at (nome (inc beat)) #'event [::finished-playing {:metronome nome}]))))

(defn -main
  [& args]
  (let [path "./sounds/test.wav"]
    (println "🎼 Recording to" path "now.")
    (recording-start path)
    (on-event ::finished-playing
              (fn [event]
                (when-let [recorded-to (recording-stop)]
                  (println "Finished recording to" recorded-to "🎶"))) ::recording-complete-handle)
    (play (metronome 120) auld-lang-syne)))
