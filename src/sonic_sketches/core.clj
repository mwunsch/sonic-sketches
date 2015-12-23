(ns sonic-sketches.core
  (:use [overtone.live])
  (:require [overtone.inst.synth])
  (:gen-class))

;; ======================================================================
;; Monotron Clone by Roger Allen.
;;   via some code in https://github.com/rogerallen/explore_overtone
;;
;; Source
;; http://korg.com/monotrons
;; http://korg.com/services/products/monotron/monotron_Block_diagram.jpg
;;
;; Following patterns from
;; https://github.com/overtone/overtone/blob/master/src/overtone/inst/synth.clj
;;
;; Inspiration
;; http://www.soundonsound.com/sos/aug10/articles/korg-monotron.htm
;; http://www.timstinchcombe.co.uk/index.php?pge=mono
;;
;; This was pulled from https://github.com/overtone/overtone/blob/master/src/overtone/examples/instruments/monotron.clj
(defsynth monotron
  "Korg Monotron from website diagram:
   http://korg.com/services/products/monotron/monotron_Block_diagram.jpg."
  [note     60            ; midi note value
   volume   0.7           ; gain of the output
   mod_pitch_not_cutoff 1 ; use 0 or 1 only to select LFO pitch or cutoff modification
   pitch    0.0           ; frequency of the VCO
   rate     4.0           ; frequency of the LFO
   int      1.0           ; intensity of the LFO
   cutoff   1000.0        ; cutoff frequency of the VCF
   peak     0.5           ; VCF peak control (resonance)
   pan      0             ; stereo panning
   ]
  (let [note_freq       (midicps note)
        pitch_mod_coef  mod_pitch_not_cutoff
        cutoff_mod_coef (- 1 mod_pitch_not_cutoff)
        LFO             (* int (saw rate))
        VCO             (saw (+ note_freq pitch (* pitch_mod_coef LFO)))
        vcf_freq        (+ cutoff (* cutoff_mod_coef LFO) note_freq)
        VCF             (moog-ff VCO vcf_freq peak)
        ]
    (out 0 (pan2 (* volume VCF) pan))))

(def jingle-bells
  [{:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/2}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :E4) :duration 1/2}
   {:pitch (note :E4) :duration 1/4}
   {:pitch (note :G4) :duration 1/4}
   {:pitch (note :C4) :duration 1/4}
   {:pitch (note :D4) :duration 1/4}
   {:pitch (note :E4) :duration 1}])

(defn play-monotron
  "Accepts a monotron synth, a metronome, and a sequence of maps with :pitch and :duration"
  [tron nome notes]
  (let [beat (nome)
        {pitch :pitch duration :duration :as note} (first notes)]
    (if (some? note)
      (at (nome beat) (ctl tron :note pitch))
      (apply-by (nome (inc beat)) #(kill tron)))
    (apply-by (+ (nome (inc beat)) (* (metro-tick nome) duration))
              #'play-monotron [tron nome (rest notes)])))

(defn play
  "Record a fn that starts a synth-node and call a callback when that node is destroyed."
  [ugen-fn callback]
  (recording-start "./sounds/test.wav")
  (after-delay 1500                     ; We delay b/c recording-start has a 1.5s pause
               #(let [synth (ugen-fn)
                     node-id (:id synth)]
                 (on-event [:overtone :node-destroyed node-id] (fn [ev] (do
                                                                         (recording-stop)
                                                                         (callback))) ::synth-destroyed-handler))))

(defn -main
  [& args]
  (play #(demo (example dbrown :rand-walk))
        #(println "Stopped!")))
