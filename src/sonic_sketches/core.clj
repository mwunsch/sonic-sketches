(ns sonic-sketches.core
  (:use [overtone.live])
  (:gen-class))

(definst mousedrums []
  (example membrane-circle :mouse))

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
