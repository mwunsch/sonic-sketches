(ns sonic-sketches.forecast
  "Utilities for connecting to the Forecast API (https://developer.forecast.io/)"
  (:use [clojure.string :only [join]])
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]))

(def api-key (System/getenv "FORECAST_API_KEY"))

(def nyc-geo
  [40.7903 73.9597])

(defn call
  [lat lon]
  (let [base-url "https://api.forecast.io/forecast"
        coord (join "," (map str [lat lon]))
        url (join "/" [base-url api-key coord])]
    (http/get url)))
