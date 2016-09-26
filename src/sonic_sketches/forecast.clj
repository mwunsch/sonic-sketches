(ns sonic-sketches.forecast
  "Utilities for connecting to the Forecast API (https://developer.forecast.io/)"
  (:use [clojure.string :only [join]])
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [environ.core :refer [env]]))

(def api-key
  (env :forecast-api-key))

(defn call
  "Performs a call to the Forecast API for a daily summary. Accepts a
  pair of lat/long coords and a Long time in millis (like that found
  from `System/currentTimeMillis`). Will throw an Exception if
  unsuccessful."
  [lat lon time]
  (let [base-url "https://api.darksky.net/forecast"
        unix-time (quot time 1000)
        path (join "," (map str [lat lon unix-time]))
        url (join "/" [base-url api-key path])]
    (-> (http/get url {:query-params {:exclude "minutely,hourly,currently"}})
        (update :body json/read-str :key-fn keyword))))

(def nyc-geo
  "The latitude and longitude of Grand Central Terminal"
  [40.7528 -73.9765])

(def nyc-at
  "A partial function that when given a time in millis will call the
  Forecast API and get a daily weather summary for New York City.

    e.g. (forecast/nyc-at (now))"
  (apply partial call nyc-geo))
