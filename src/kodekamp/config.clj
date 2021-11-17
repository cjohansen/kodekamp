(ns kodekamp.config
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.spec.alpha :as s]
            [confair.config :as config]
            [integrant.core :as ig]))

(s/def ::cache (s/tuple #{:cache/redis :cache/directory} string?))
(s/def :datomic/uri string?)
(s/def :jetty/port number?)
(s/def ::request-log-dir string?)

(s/def ::config (s/keys :req [:datomic/uri
                              :jetty/port]
                        :req-un [::request-log-dir]
                        :opt-un [::cache]))

(defn init-config [{:keys [path]}]
  (if (and path (.exists (io/file path)))
    (config/from-file path)
    (throw (ex-info "No Kodekamp config found" {:path path}))))

(defn validate
  "Validate configuration against a spec"
  [config spec]
  (if-let [data (s/explain-data spec config)]
    (throw (ex-info
            (str "Configuration is invalid, please inspect\n"
                 (with-out-str
                   (pprint/pprint config)
                   (s/explain-out data)))
            data))
    (s/conform spec config)))

(defmethod ig/init-key :system/config [_ opts]
  (-> (init-config opts)
      (validate ::config)
      (config/mask-config)))

(comment
  (ig/init-key :system/config {:path "./config/dev/local-config.edn"})

  )
