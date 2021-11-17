(ns kodekamp.server
  (:require [cheshire.generate :refer [add-encoder]]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [datomic.api :as d]
            [integrant.core :as ig]
            [kodekamp.app :as app]
            [kodekamp.fs :as fs]
            [realize.core :as realize]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.timbre :as log])
  (:import (java.time Instant LocalDateTime)
           (org.eclipse.jetty.server Server)))

(defmethod ig/init-key :app/context [_ context]
  context)

(defn wrap-context [handler context]
  (fn [req]
    (handler (assoc req
                    :app/context context
                    :db (d/db (:conn context))))))

(defn content-type-header [res]
  (->> (keys (:headers res))
       (filter (comp #{"content-type"} str/lower-case name))
       first))

(defn get-content-type [res]
  (when-let [header (content-type-header res)]
    (get-in res [:headers header])))

(def edn-re #"application/(vnd.+)?edn")

(defn wants-edn? [req res]
  (or (some->> res get-content-type (re-find edn-re))
      (re-find edn-re (or (get-in req [:headers "accept"]) ""))))

(defn edn-response [req res]
  (if (and (coll? (:body res))
           (wants-edn? req res))
    (with-redefs [*print-namespace-maps* false]
      (-> res
          (update :body (comp pr-str realize/realize))
          (dissoc (content-type-header res))
          (assoc-in [:headers "content-type"] "application/edn; charset=utf-8")))
    res))

(defn wrap-edn-response [handler]
  (fn [req]
    (edn-response req (handler req))))

(defn wrap-expose-edn-body
  "The edn params middleware puts the decoded EDN body in :edn-params, while the
  corresponding JSON middleware overwrites :body. Individual handlers should not
  need to check both, so this will place :edn-params in :body if available."
  [handler]
  (fn [req]
    (handler (if (coll? (:edn-params req))
               (assoc req :body (:edn-params req))
               req))))

(defn wrap-default-content-type [handler]
  (fn [req]
    (handler
     (if-not (get-in req [:headers "content-type"])
       (assoc-in req [:headers "content-type"] "application/json")
       req))))

(defn write-req-res [{:keys [req exception res] :as exchange}]
  (prn (keys req))
  (fs/write-file
   (format "%s/%04d-%02d-%02dT%02d/%02d-%02d-%s-%s.edn"
           (-> req :app/context :config :request-log-dir)
           (.getYear (:now req))
           (.getMonthValue (:now req))
           (.getDayOfMonth (:now req))
           (.getHour (:now req))
           (.getMinute (:now req))
           (.getSecond (:now req))
           (name (:request-method req))
           (-> (:uri req)
               (str/replace #"^/|/$" "")
               (str/replace #"/" "-")))
   (let [content (realize/realize
                  (cond-> exchange
                    (:req exchange)
                    (update :req select-keys [:method :uri :params :form-params :body :headers])

                    (-> exchange :req :body)
                    (update-in [:req :body] #(try
                                               (if (or (string? %)
                                                       (map? %)
                                                       (coll? %))
                                                 %
                                                 (slurp %))
                                               (catch Exception e
                                                 (log/warn e "Can't slurp body" {:body %})
                                                 %)))))]
     (with-out-str (pprint/pprint content)))))

(defn log-req-res [handler req]
  (let [exchange (transient {:req req})
          res (try
                (handler req)
                (catch Exception e
                  (log/error e "Boom boom!")
                  (assoc! exchange :exception {:message (.getMessage e)
                                               :ex-data (ex-data e)})
                  {:status 500}))]
      (write-req-res (persistent! (assoc! exchange :res res)))
      res))

(defn wrap-log-req-res [handler]
  (fn [req]
    (log-req-res handler req)))

(defn wrap-now [handler]
  (fn [req]
    (handler (assoc req :now (LocalDateTime/now)))))

(defmethod ig/init-key :app/handler [_ {:keys [context]}]
  (-> app/app
      wrap-log-req-res
      (wrap-context context)
      wrap-now
      wrap-params
      wrap-edn-params
      wrap-edn-response
      wrap-expose-edn-body
      wrap-edn-response
      (wrap-json-body {:keywords? true})
      wrap-json-response
      wrap-default-content-type))

(defmethod ig/init-key :adapter/jetty [_ {:keys [config handler]}]
  (jetty/run-jetty handler {:port (:jetty/port config)
                            :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ ^Server server]
  (when server (.stop server)))

(add-encoder Instant
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (str c))))
