(ns kodekamp.system
  (:require [courier.cache :as cc]
            [integrant.core :as ig]
            [kodekamp.config]
            [kodekamp.db]))

:kodekamp.config/keep
:kodekamp.db/keep

(defmethod ig/init-key :courier/cache [_ {:keys [config]}]
  (let [[kind place] (:cache config)]
    (case kind
      :cache/directory
      (cc/create-file-cache {:dir place}))))

(defn system-spec [env]
  {:system/config (case env
                    :dev {:path "./config/dev/local-config.edn"})

   :datomic/conn {:config (ig/ref :system/config)}

   :courier/cache {:config (ig/ref :system/config)}

   :app/context {:config (ig/ref :system/config)
                 :conn (ig/ref :datomic/conn)
                 :cache (ig/ref :courier/cache)}

   :app/handler {:context (ig/ref :app/context)}

   :adapter/jetty {:config (ig/ref :system/config)
                   :handler (ig/ref :app/handler)}})

(comment

  (ig/init (system-spec :dev))

  )
