(ns clog.models.logfile.parser
  (:require named-re.core
            [clj-time.coerce :as coerce]
            [clj-time.format :as timeformat]
            ))

(def demoline "::1 - - [08/Nov/2013:15:21:40 +0100] \"GET /phppgadmin/tables.php?subject=schema&server=%3A5432%3Aallow&database=clog&schema=public HTTP/1.1\" 200 10197")
(def demoparasoupline "88.217.33.69 - - [21/Nov/2013:22:55:25 +0100] \"GET /asset/6111/8757_a0ca.gif HTTP/1.1\" 200 281398 asset-a.parasoup.de [Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/30.0.1599.114 Chrome/30.0.1599.114 Safari/537.36] http://parasoup.de/")
(def demonintendoline "172.30.1.55 - - [31/Jul/2013:01:44:50 +0200]  \"GET /content/channel-entry?channel_id=2&published=1&date=2013-07-31+01%3A44%3A52&aclCurrentUserId=guid-80000285 HTTP/1.1\" 200 1123 \"-\" \"Zend_Http_Client\" 0.085")


(def host-prefix "(?<host>[\\w\\-\\.]*))(?::\\d+)?")
(def common-log-format (str "(?<ip>\\S+)\\s+\\S+\\s+\\S+\\s+\\[(?<datetime>.*?)\\]\\s+"
                            "\"\\S+\\s+(?<path>.*?)\\s+\\S+\"\\s+(?<status>\\S+)\\s+(?<length>\\S+)"))
(def ncsa-extended-log-format (str common-log-format
                                   "\\s+\"(?<referrer>.*?)\"\\s+\"(?<useragent>.*?)\""))
(def s3-log-format (str "\\S+\\s+(?<host>\\S+)\\s+\\[(?<datetime>.*?)\\]\\s+(?<ip>\\S+)\\s+"
                        "\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\"\\S+\\s+(?<path>.*?)\\s+\\S+\"\\s+(?<status>\\S+)\\s+\\S+\\s+(?<length>\\S+)\\s+"
                        "\\S+\\s+\\S+\\s+\\S+\\s+\"(?<referrer>.*?)\"\\s+\"(?<useragent>.*?)\""))
(def parasoup-log-format (str common-log-format
                              "\\s+(?<host>\\S+)\\s+\\[(?<useragent>.*?)\\]\\s+(?<referrer>\\S+)"))
(def nintendo-timed-log-format (str common-log-format
                              "\\s+\"(?<referrer>.*?)\"\\s+\"(?<useragent>.*?)\"\\s+\"(?<forwardedfor>.*?)\"\\s+(?<processtime>\\d+\\.\\d+)"))
(def nintendo-timed_comb-log-format (str common-log-format
                              "\\s+\"(?<referrer>.*?)\"\\s+\"(?<useragent>.*?)\"\\s+(?<processtime>\\d+\\.\\d+)"))

(def format-map {"common" common-log-format
                 "ncsa" ncsa-extended-log-format
                 "s3" s3-log-format
                 "parasoup" parasoup-log-format
                 "nintendo-timed" nintendo-timed-log-format
                 "nintendo-timed_comb" nintendo-timed_comb-log-format})

(def pattern-map
  (reduce
    (fn [acc [k v]] (assoc acc k (re-pattern v)))
    {}
    format-map))

(def datetime-formatter (timeformat/formatter "dd/MMM/yyyy:HH:mm:ss Z"))

(defn parse-datetime
  [t]
  (coerce/to-long (timeformat/parse datetime-formatter t)))

(defn parse-int
  [s]
  (when (re-find #"^\d+$" s)
    (read-string s)))

(defn parse-float
  [s]
  (when (re-find #"^\d+(|\.\d+)$" s)
    (read-string s)))

(def processor-map
  {:datetime parse-datetime
   :status parse-int
   :processtime parse-float
   :length parse-int})

(defn process-no-match
  [data]
  (reduce (fn [acc [k v]]
            (if (or (= v "-") (not v))
              (dissoc acc k)
              acc))
          data
          data))

(defn apply-processors
  [data]
  (reduce
    (fn [acc [k v]]
      (if (contains? processor-map k)
        (assoc acc k ((get processor-map k) v))
        acc))
    data
    data))

(defn process
  [data]
  (when (and data (map? data))
    (-> data
        (dissoc :0)
        (process-no-match)
        (apply-processors)
        (process-no-match))))

(defn parse-with-pattern
  [line pattern]
  (try
    (process (re-find pattern line))
    (catch Exception e nil)))

(defn detect-format
  [line]
  (second
    (let [best (first
                 (sort
                   (fn [a b]
                     (> (first a) (first b)))
                   (map
                     (fn [f] [(count (parse-with-pattern line (get pattern-map f))) f])
                     (keys pattern-map))))]
      (if (= 0 (first best))
        [nil nil]
        best))))

(defn parse-line
  ([line line-format]
   (when-let [pattern (get pattern-map line-format)]
     (parse-with-pattern line pattern))))
