(ns sonic-sketches.forecast
  "Utilities for connecting to the Forecast API (https://developer.forecast.io/)"
  (:use [clojure.string :only [join]])
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]))

(def api-key
  (System/getenv "FORECAST_API_KEY"))

(def nyc-geo
  [40.7903 73.9597])

(defn call
  [coords & params]
  (let [base-url "https://api.forecast.io/forecast"
        url (join "/" [base-url api-key (join "," (map str coords))])]
    (http/get url)))
