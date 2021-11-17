(ns kodekamp.dev
  (:require [clojure.tools.namespace.repl :as repl]
            [integrant.repl]
            [kodekamp.system]
            [taoensso.timbre :as log])
  (:import (java.lang ProcessHandle)))

(integrant.repl/set-prep! #(kodekamp.system/system-spec :dev))

(defn start []
  (integrant.repl/go)
  (log/info "Runtime configuration logged to /tmp/config.edn. My pid is" (.pid (ProcessHandle/current)))
  (log/info "Config" (str (:system/config integrant.repl.state/system))))

(defn stop []
  (integrant.repl/halt))

(defn reset []
  (integrant.repl/reset))

(defn restart []
  (stop)
  (repl/refresh :after 'kodekamp.dev/start))

(defn -main [& args]
  (start))

(comment
  (set! *print-namespace-maps* false)

  (start)
  (stop)
  (reset)
  (restart)
  )
