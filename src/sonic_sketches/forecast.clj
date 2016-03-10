(ns sonic-sketches.forecast
  "Utilities for connecting to the Forecast API (https://developer.forecast.io/)"
  (:use [clojure.string :only [join]])
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]))

(def api-key
  (System/getenv "FORECAST_API_KEY"))

(def nyc-geo
  "The latitude and longitude of Grand Central Terminal"
  [40.7528 -73.9765])

(defn call
  "Performs a call to the Forecast API for a daily summary. Accepts a
  pair of lat/long coords and a Long time in millis (like that found
  from System/currentTimeMillis). Will throw an Exception if
  unsuccessful."
  [coords time]
  (let [base-url "https://api.forecast.io/forecast"
        [lat lon] coords
        unix-time (quot time 1000)
        path (join "," (map str [lat lon unix-time]))
        url (join "/" [base-url api-key path])]
    (-> (http/get url {:query-params {:exclude "minutely,hourly,currently"}})
        (update :body json/read-str :key-fn keyword))))
