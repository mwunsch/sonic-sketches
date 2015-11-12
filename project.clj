(defproject sonic-sketches "0.1.0-SNAPSHOT"
  :description "Studies in Clojure and Overtone"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [overtone "0.9.1"]
                 [beckon "0.1.1"]]
  :main ^:skip-aot sonic-sketches.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
