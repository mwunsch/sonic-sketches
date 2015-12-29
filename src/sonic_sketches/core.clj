(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.synth])
  (:gen-class))

(def jingle-bells
  [{:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/2}

   {:pitch nil :duration 1/8}

   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/2}

   {:pitch nil :duration 1/8}

   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :G4) :duration 1/4}
   {:pitch (note :C4) :duration 1/4}
   {:pitch (note :D4) :duration 1/4}

   {:pitch (note :E4) :duration 1}

   {:pitch nil :duration 1/4}

   {:pitch (note :F4) :duration 1/4}
   {:pitch (note :F4) :duration 1/4}
   {:pitch (note :F4) :duration 1/4}
   {:pitch (note :F4) :duration 1/4}

   {:pitch (note :F4) :duration 1/4}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/2}

   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :D4) :duration 1/4}
   {:pitch (note :D4) :duration 1/4}
   {:pitch (note :E4) :duration 1/4}

   {:pitch (note :D4) :duration 1/2}
   {:pitch (note :G4) :duration 1/2}
   ])

(def auld-lang-syne
  [{:pitch (note :C3) :duration 1/4}

   {:pitch (note :F3) :duration 3/8}
   {:pitch (note :F3) :duration 1/8}
   {:pitch (note :F3) :duration 1/4}
   {:pitch (note :A3) :duration 1/4}

   {:pitch (note :G3) :duration 3/8}
   {:pitch (note :F3) :duration 1/8}
   {:pitch (note :G3) :duration 1/4}
   {:pitch (note :A3) :duration 1/8}
   {:pitch (note :G3) :duration 1/8}

   {:pitch (note :F3) :duration 3/8}
   {:pitch (note :F3) :duration 1/8}
   {:pitch (note :A3) :duration 1/4}
   {:pitch (note :C4) :duration 1/4}
   {:pitch (note :D4) :duration 3/4}
   {:pitch (note :D4) :duration 1/4}

   {:pitch (note :C4) :duration 3/8}
   {:pitch (note :A3) :duration 1/8}
   {:pitch (note :A3) :duration 1/4}
   {:pitch (note :F3) :duration 1/4}

   {:pitch (note :G3) :duration 3/8}
   {:pitch (note :F3) :duration 1/8}
   {:pitch (note :G3) :duration 1/4}
   {:pitch (note :A3) :duration 1/8}
   {:pitch (note :G3) :duration 1/8}

   {:pitch (note :F3) :duration 3/8}
   {:pitch (note :D3) :duration 1/8}
   {:pitch (note :D3) :duration 1/4}
   {:pitch (note :C3) :duration 1/4}

   {:pitch (note :F3) :duration 3/4}
   ])

(defn play
  "Accepts a metronome, and a sequence of maps with :pitch and :duration.
  When the note sequence is empty, on the next beat
  a ::finished-playing event is triggered"
  [nome notes]
  (let [beat (nome)]
    (if-let [note (first notes)]
      (let [{pitch :pitch duration :duration} note
            decay (* (metro-tick nome) duration)]
        (when (some? pitch)
          (at (nome beat)
              (overtone.inst.synth/tb303
               :note pitch
               :cutoff 2000
               :decay (/ decay 1000)
               :wave 1
               :sustain 0.8
               :release 0.25
               :attack 0.1)))
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
