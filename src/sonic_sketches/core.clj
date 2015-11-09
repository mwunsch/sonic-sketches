(ns sonic-sketches.core
  (:use [overtone.live])
  (:gen-class))

(.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (println "Bye!"))))

(definst mousedrums []
  (example membrane-circle :mouse))

(defn -main
  "I like to play the drums by clicking my mouse on the screen."
  [& args]
  (mousedrums))
