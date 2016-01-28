(defproject sonic-sketches "0.1.0-SNAPSHOT"
  :description "Studies in Clojure and Overtone"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [overtone "0.9.1"]
                 [amazonica "0.3.47"]
                 [forecast-clojure "1.0.3"]]
  :main ^:skip-aot sonic-sketches.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
