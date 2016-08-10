(ns sonic-sketches.emoji
  "Choose emoji for data points.")

(defn lunar-str
  "Given a lunar phase number, returns an Emoji string displaying a
  visual representation of the moon for that phase."
  [phase]
  (let [moons (->> (range 0x1f311 0x1f319)
                   (map int)
                   (mapv (partial format "%c")))
        indices (- (count moons) 1)]
    (nth moons (-> phase
                   (/ (quot 100 indices))
                   float
                   Math/round))))

(defn precip-str-from-interval
  [interval]
  (case interval
    :minor "⛈"
    :minor-pentatonic "🌧"
    :harmonic-minor "☔"
    :major-pentatonic "🌥"
    :lydian "⛅"
    "☀"))

(defn temp-emoji
  [temp]
  (condp <= temp
    85 "🔥"
    80 "💦"
    75 "⛱"
    65 "🌻"
    50 "🍃"
    40 "🍂"
    20 "❄"
    "☃"))
