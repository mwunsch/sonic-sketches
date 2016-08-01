(ns sonic-sketches.twitter
  "Utilities for uploading a song to Twitter"
  (:use [clojure.java.shell :only [sh]]
        [sonic-sketches.core :only [lunar-str]]
        [clojure.string :only [capitalize]])
  (:import [twitter4j TwitterFactory StatusUpdate GeoLocation]))

(defonce client (TwitterFactory/getSingleton))

(defn wav->mp4
  [path]
  (let [out "test.mp4"]
    (sh "ffmpeg" "-i" path "-strict" "experimental" out)))

(defn mkstatus
  [songdata]
  (let [{:keys [day-of-week iso8601 lunar-phase avg-temp precipitation]} songdata
        moonphase (lunar-str lunar-phase)
        day (capitalize day-of-week)
        message (format "It is %s." day)]
    (str message "\n\n" moonphase)))

(defn tweet
  "Tweet the song from a path"
  [path metadata]
  (let [{:keys [latitude longitude]} metadata
        statusmsg (mkstatus metadata)
        geo (GeoLocation. latitude longitude)
        status (doto (StatusUpdate. statusmsg)
                 (.setLocation geo))]
    (.updateStatus client status)))
