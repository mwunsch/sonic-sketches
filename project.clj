(defproject sonic-sketches "0.6.0-SNAPSHOT"
  :description "Studies in Clojure and Overtone"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.generators "0.1.2"]
                 [overtone "0.9.1"]
                 [amazonica "0.3.55"]
                 [clj-http "2.1.0"]
                 [clj-oauth "1.5.5"]
                 [com.taoensso/timbre "4.3.1"]
                 [environ "1.0.3"]
                 [org.twitter4j/twitter4j-core "4.0.4"]]
  :main ^:skip-aot sonic-sketches.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
