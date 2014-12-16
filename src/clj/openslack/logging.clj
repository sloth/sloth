(ns openslack.logging
  "Logging implementation."
  (:require [clj-time.format :as tfmt]
            [clj-time.core :as time])
  (:import java.util.logging.Level
           java.util.logging.Logger
           java.util.logging.ConsoleHandler
           java.util.logging.LogRecord
           java.util.logging.Formatter
           java.util.logging.FileHandler))

(def ^{:dynamic true
       :doc "Logging write queue"}
  *logger-writer* (agent 0N :error-mode :continue))

(defn- make-logger-formatter
  []
  (let [tz (time/default-time-zone)
        tf (tfmt/with-zone (tfmt/formatters :date-time) tz)]
    (proxy [Formatter] []
      (format [^LogRecord rec]
        (let [now  (time/now)
              tstr (tfmt/unparse tf now)]
          (if-let [thr (.getThrown rec)]
            (let [sw (java.io.StringWriter.)
                  pw (java.io.PrintWriter. sw)]
              (.printStackTrace thr pw)
              (.close pw)
              (format "[%s] %s: %s\n%s\n" tstr
                      (.getLevel rec)
                      (.getMessage rec)
                      (.toString sw)))
            (format "[%s] %s: %s\n" tstr
                    (.getLevel rec)
                    (.getMessage rec))))))))

(defn make-logger-handler
  [formatter]
  (doto (ConsoleHandler.)
    (.setLevel Level/INFO)
    (.setFormatter formatter)))

(defn make-logger
  [handler]
  (doto (Logger/getAnonymousLogger)
    (.setUseParentHandlers false)
    (.setLevel Level/INFO)
    (.addHandler handler)))

(def ^{:dynamic :true
       :doc "Lazzy logger constructor"}
  *logger*
  (delay (-> (make-logger-formatter)
             (make-logger-handler)
             (make-logger))))

(defn info
  [& messages]
  (let [messages (map (fn [item]
                        (if (string? item)
                          item
                          (pr-str item)))
                      messages)]
    (send-off *logger-writer*
              (fn [v & args]
                (let [message (apply str messages)]
                  (.log @*logger* Level/INFO message))
                (inc v)))))

(defn log
  [level message & [exception]]
  (send-off *logger-writer*
            (fn [v & args]
              (case level
                :info (.log @*logger* Level/INFO message exception)
                :error (.log @*logger* Level/SEVERE message exception)
                :debug (.log @*logger* Level/FINE message exception)
                :warn (.log @*logger* Level/WARNING message exception))
              (inc v))))

