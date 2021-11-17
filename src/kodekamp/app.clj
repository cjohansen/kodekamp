(ns kodekamp.app
  (:require [compojure.core :as compojure :refer [GET]]))

(defn index-handler [req]
  {:status 200})

(defn handler [req]
  {:status 200})

(def app
  (compojure/routes
   (GET "/" [] #'index-handler)
   handler))
