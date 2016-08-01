(ns sonic-sketches.twitter
  "Utilities for uploading a song to Twitter"
  (:use [clojure.java.shell :only [sh]]
        [sonic-sketches.core :only [lunar-str]]
        [clojure.string :only [capitalize]])
  (:import [twitter4j TwitterFactory StatusUpdate GeoLocation]))

(defonce client (TwitterFactory/getSingleton))

(defn wav->mp4
  "Convert a path to a wav to an mp4. Returns a file object to the mp4."
  [path]
  (let [wav (java.io.File. path)
        dir (.getParent wav)
        filename (clojure.string/replace (.getName wav) #"\.wav" ".mp4")
        mp4 (java.io.File. dir filename)
        ffmpeg (sh "ffmpeg" "-y" "-i" path
                   "-filter_complex"
                   "[0:a]showwaves=s=320x180:mode=line,format=yuv420p[v]"
                   "-map" "[v]" "-map" "0:a" "-b:a" "64k" "-b:v" "256k"
                   (.getPath mp4))]
    (if (zero? (:exit ffmpeg))
      mp4
      (throw (Exception. (str "Error converting wav" (:err ffmpeg)))))))

(defn upload-media
  [file]
  (comment "TK. (https://dev.twitter.com/rest/media/uploading-media#chunkedupload)"))

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
        mp4 (wav->mp4 path)
        status (doto (StatusUpdate. statusmsg)
                 (.setLocation geo)
                 (.setMedia mp4))]
    (.updateStatus client status))) ;; This doesn't work. Twitter4j doesn't support uploading video
