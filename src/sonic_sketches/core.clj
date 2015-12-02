(ns sonic-sketches.core
  (:use [overtone.live])
  (:gen-class))

(definst mousedrums []
  (example membrane-circle :mouse))

(defn play
  "Demo synth for n millis, then exit"
  [ugen-fn callback]
  (recording-start "./sounds/test.wav")
  (after-delay 1500
               #(let [synth (ugen-fn)
                     node-id (:id synth)]
                 (on-event [:overtone :node-destroyed node-id] (fn [ev] (do
                                                                         (recording-stop)
                                                                         (callback))) ::synth-destroyed-handler))))

(defn -main
  "I like to play the drums by clicking my mouse on the screen."
  [& args]
  (play #(demo (example dbrown :rand-walk))
        #(println "Stopped!")))
