(ns storage.serve
  (:refer-clojure :exclude [key])
  (:require [environ.core :as environ]
            [ring.util.response :refer [response not-found content-type]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [compojure.core :refer [routes GET PUT DELETE HEAD]]
            [compojure.route :as cmpr]
            [compojure.response :refer [render]]
            [clojurewerkz.spyglass.client :as mc]
            [clojure.java.io :as io]
            [storage.store :refer [s-get s-put! s-delete! s-exists?]])
  (:import [org.apache.commons.io IOUtils]))

(defn make-mc-key [bucket key]
  (str bucket "/" key))

(defn srv-get [context token bucket key]
  (let [{:keys [storage memcached]} context
        mc-key (make-mc-key bucket key)]
    (let [mc-byte-array (if memcached (mc/get memcached key) nil)]
      (if-not mc-byte-array
        (if-let [stream (s-get storage bucket key)]
          (let [byte-array (IOUtils/toByteArray stream)]
            (when memcached
              (mc/set memcached mc-key 300 byte-array))
            (response (io/input-stream byte-array)))
          (not-found (str "Data not found for " bucket "/" key)))
        (response (io/input-stream mc-byte-array))))))

(defn srv-put! [context token bucket key data]
  (let [{:keys [storage memcached]} context]
    (s-put! storage bucket key data)
    (when memcached
      (mc/set memcached (make-mc-key bucket key) 300 (IOUtils/toByteArray (io/input-stream data))))
    (response (str "Successfully put " bucket "/" key))))

(defn srv-delete! [context token bucket key]
  (when-let [{:keys [storage memcached]} context]
    (s-delete! storage bucket key)
    (when memcached
      (mc/delete memcached (make-mc-key bucket key)))
    (response (str "Successfully deleted " bucket "/" key))))

(defn gen-routes [context]
  (routes
    (GET "/" request (str "<b>Echoes:</b><p>" request "</p>"))
    (GET "/:bucket/:key" [bucket key token]
      (srv-get context token bucket key))
    (PUT "/:bucket/:key" [bucket key token data]
      (srv-put! context token bucket key (:tempfile data)))
    (DELETE "/:bucket/:key" [bucket key token]
      (srv-delete! context token bucket key))
    (cmpr/not-found "404 - Not Found")))

(defn gen-handler [context]
  (-> (gen-routes context)
      (wrap-multipart-params)
      (wrap-defaults api-defaults)))
