(ns sonic-sketches.core
  (:use [overtone.live])
  (:gen-class))



(definst mousedrums []
  (example membrane-circle :mouse))

(defn -main
  "I like to play the drums by clicking my mouse on the screen."
  [& args]
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(stop)))
  (mousedrums))
