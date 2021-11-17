(ns kodekamp.config-admin
  (:require [confair.config :as config]
            [confair.config-admin :as ca]))

(def secret-keys [])

(comment
  (set! *print-namespace-maps* false)

  ;; dev config

  (meta (config/from-file "./config/dev/local-config.edn"))

  (ca/conceal-value (config/from-file "./config/dev/local-config.edn")
                    :secret/dev
                    :some-secret)

  (ca/reveal-value (config/from-file "./config/dev/local-config.edn")
                   :some-secret)

  (def config-file "./config/dev/local-config.edn")

  (def overrides {:secrets {:secret/dev [:config/file "./secrets/dev.txt"]}})

  (for [k secret-keys]
    (ca/conceal-value (config/from-file config-file overrides) :secret/dev k))
)
