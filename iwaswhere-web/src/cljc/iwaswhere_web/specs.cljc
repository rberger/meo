(ns iwaswhere-web.specs
  (:require  [iwaswhere-web.utils.parse :as p]
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

#?(:cljs (defn boolean? [x] (= (type x) js/Boolean)))

(defn number-in-range?
  "Return function that returns true if start <= val and val < end"
  [start end]
  (fn [val]
    (and (number? val) (<= start val) (< val end))))

(defn pos-int? [n] (and (integer? n) (pos? n)))

(defn is-tag?
  "Check if string is a tag, such as a hashtag with the '#' prefix or  a mention with the '@' prefix."
  [prefix]
  (fn [s]
    (re-find (re-pattern (str "^" prefix p/tag-char-class "+$")) s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Journal Entry Specs
(s/def :iww.entry/timestamp (number-in-range? 0 5000000000000))
(s/def :iww.entry/md string?)
(s/def :iww.entry/tags (s/coll-of (is-tag? "#") #{}))
(s/def :iww.entry/mentions (s/coll-of (is-tag? "@") #{}))
(s/def :iww.entry/timezone (s/nilable string?))
(s/def :iww.entry/utc-offset (number-in-range? -720 720))
(s/def :iww.entry/entry-type #{:pomodoro})
(s/def :iww.entry/comment-for (number-in-range? 0 5000000000000))
(s/def :iww.entry/latitude (s/nilable (number-in-range? -180.0 180.0)))
(s/def :iww.entry/longitude (s/nilable (number-in-range? -180.0 180.0)))
(s/def :iww.entry/planned-dur (s/and integer? pos?))
(s/def :iww.entry/planned-dur (s/and integer? pos?))
(s/def :iww.entry/interruptions (s/and integer? #(<= 0 %)))

(def entry-spec
  "basic entry, with only timestamp and markdown text mandatory"
  (s/keys :req-un [:iww.entry/timestamp]
          :opt-un [:iww.entry/entry-type
                   :iww.entry/tags
                   :iww.entry/mentions
                   :iww.entry/planned-dur
                   :iww.entry/completed-time
                   :iww.entry/interruptions
                   :iww.entry/timezone
                   :iww.entry/utc-offset
                   :iww.entry/latitude
                   :iww.entry/longitude
                   :iww.entry/md]))

(def entry-w-geo-spec
  "geodata-enriched entry, with timestamp, markdown text, latitude, longitude mandatory"
  (s/keys :req-un [:iww.entry/timestamp
                   :iww.entry/md
                   :iww.entry/latitude
                   :iww.entry/longitude]
          :opt-un [:iww.entry/entry-type
                   :iww.entry/tags
                   :iww.entry/mentions
                   :iww.entry/planned-dur
                   :iww.entry/completed-time
                   :iww.entry/interruptions
                   :iww.entry/timezone
                   :iww.entry/utc-offset]))

(def timestamp-required-spec (s/keys :req-un [:iww.entry/timestamp]))

;; the following message types expect a properly formed entry
(s/def :entry/new entry-spec)
(s/def :entry/update-local entry-spec)
(s/def :text-entry/update entry-spec)
(s/def :entry/saved entry-spec)

;; geo-enriched entries require a properly formed entry with latitude and longitude
(s/def :entry/geo-enrich entry-w-geo-spec)

;; the following message types only require timestamp to be present
(s/def :entry/remove-local timestamp-required-spec)
(s/def :cmd/pomodoro-inc timestamp-required-spec)
(s/def :cmd/pomodoro-start timestamp-required-spec)
(s/def :cmd/toggle timestamp-required-spec)


;; toggle-key requires a map with the :path key, holding a vector with at least one of keyword, number, string
(s/def :iww.cfg/path (s/cat :first-key keyword?
                            :subsequent (s/+ (s/or :string string?
                                                   :keyword keyword?
                                                   :number number?))))
(def path-map (s/keys :req-un [:iww.cfg/path]))
(s/def :cmd/toggle-key path-map)

;; message expected to not have a payload
(s/def :show/more nil?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Search Spec
(s/def :iww.search/search-text string?)
(s/def :iww.search/tags (s/coll-of (is-tag? "#") #{}))
(s/def :iww.search/not-tags (s/coll-of (is-tag? "~#") #{}))
(s/def :iww.search/mentions (s/coll-of (is-tag? "@") #{}))
(s/def :iww.search/date-string (s/nilable #(re-find #"[0-9]{4}-[0-9]{2}-[0-9]{2}" %)))
(s/def :iww.search/timestamp (s/nilable #(re-find #"[0-9]{13}" %)))
(s/def :iww.search/n pos-int?)

(def search-spec
  "spec for search, all fields mandatory"
  (s/keys :req-un [:iww.search/search-text
                   :iww.search/date-string
                   :iww.search/n
                   :iww.search/tags
                   :iww.search/not-tags
                   :iww.search/mentions
                   :iww.search/timestamp]))

(s/def :search/update search-spec)
(s/def :state/get search-spec)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Search Result Spec
(s/def :iww.search-stats/entry-count pos-int?)
(s/def :iww.search-stats/node-count pos-int?)
(s/def :iww.search-stats/edge-count pos-int?)

(def search-stats-spec
  (s/keys :req-un [:iww.search-stats/entry-count
                   :iww.search-stats/node-count
                   :iww.search-stats/edge-count]))

(s/def :iww.search-result/entries (s/* entry-spec))
(s/def :iww.search-result/hashtags (s/coll-of string? #{}))
(s/def :iww.search-result/mentions (s/coll-of string? #{}))
(s/def :iww.search-result/stats search-stats-spec)
(s/def :iww.search-result/duration-ms number?)

(s/def :state/new
  (s/keys :req-un [:iww.search-result/entries
                   :iww.search-result/hashtags
                   :iww.search-result/mentions
                   :iww.search-result/stats
                   :iww.search-result/duration-ms]))
