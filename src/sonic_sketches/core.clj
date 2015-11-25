(ns sonic-sketches.core
  (:use [overtone.live])
  (:gen-class))

(definst mousedrums []
  (example membrane-circle :mouse))

(defn play-then-quit
  "Demo synth for n millis, then exit"
  [t ugen-fn]
  (recording-start "./sounds/test.wav")
  (ugen-fn)
  (after-delay t (fn [] ((do (recording-stop)
                            (System/exit 0))))))

(defn -main
  "I like to play the drums by clicking my mouse on the screen."
  [& args]
  (play-then-quit *demo-time* #(demo (example dbrown :rand-walk))))
