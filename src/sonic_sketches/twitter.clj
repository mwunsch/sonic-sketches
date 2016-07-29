(ns sonic-sketches.twitter
  "Utilities for uploading a song to Twitter"
  (:use [clojure.java.shell :only [sh]])
  (:import [twitter4j TwitterFactory StatusUpdate GeoLocation]))

(defonce client (TwitterFactory/getSingleton))

(defn wav->mp4
  [path]
  (let [out "test.mp4"]
    (sh "ffmpeg" "-i" path "-strict" "experimental" out)))

(defn tweet
  "Tweet the song from a path"
  [path metadata]
  (.updateStatus client "Hello, world."))
