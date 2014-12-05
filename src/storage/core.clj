(ns storage.core
  (:refer-clojure :exclude [key])
  (:require [environ.core :as environ]
            [ring.util.response :refer [response not-found content-type]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [clojure.tools.reader.edn :as edn]
            [compojure.core :refer [routes GET PUT DELETE HEAD]]
            [compojure.route :as cmpr]
            [compojure.response :refer [render]]
            [org.httpkit.server :as s]
            [clojurewerkz.spyglass.client :as mc]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [refresh]]
            [storage.store :refer [gen-storage-proxy s-get s-put! s-delete! s-exists?]]
            [storage.nrepl :refer [start-nrepl!]])
  (:import [org.apache.commons.io IOUtils]
           [storage.store FileStorage S3Storage])
  (:gen-class))

(defn get-adapter-options [env]
  (select-keys env [:aws-access-key :aws-secret-key :file-storage-path]))

(defn get-bucket-storage-map [env]
  (or (edn/read-string (env :bucket-storage-map))
      {}))

(defn gen-mc-conn [env]
  (when-let [memcached-servers (:memcached-servers env)]
    (let [memcached-user (:memcached-user env)
          memcached-pass (:memcached-pass env)]
      (if (and memcached-user memcached-pass)
        (do (prn (str memcached-user ":" memcached-pass))
            (mc/bin-connection memcached-servers memcached-user memcached-pass))
        (mc/bin-connection memcached-servers)))))

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

(defn gen-app-routes [context]
  (routes
    (GET "/" request (str "<b>Echoes:</b><p>" request "</p>"))
    (GET "/:bucket/:key" [bucket key token]
      (srv-get context token bucket key))
    (PUT "/:bucket/:key" [bucket key token data]
      (srv-put! context token bucket key (:tempfile data)))
    (DELETE "/:bucket/:key" [bucket key token]
      (srv-delete! context token bucket key))
    (cmpr/not-found "404 - Not Found")))

(defprotocol App
  (start! [_ env])
  (stop! [_ system]))

(def app
  (reify App
    (start! [_ env]
      (let [storage (gen-storage-proxy (get-adapter-options env) (get-bucket-storage-map env))
            memcached nil
            context {:storage storage
                     :memcached memcached}
            handler (gen-app-routes context)
            port (edn/read-string (or (env :server-port) "3000"))]
        (println (str "Starting http storage server at " (java.util.Date.) " on port " port "!"))
        {:context context
         :handler (-> handler
                      (wrap-multipart-params)
                      (wrap-defaults api-defaults))
         :server (s/run-server handler {:port port})
         :stop! (fn [system] (when-let [server (system :server)] (server)))
         :app _}))
    (stop! [_ system]
      (println (str "Shutting down http storage server at " (java.util.Date.) "!"))
      (when-let [stop-fn (system :server)]
        (stop-fn)
        system))))

(defn resolve-sym [sym]
  (when-let [sym-ns (namespace sym)]
    (require (symbol sym-ns)))
  (ns-resolve *ns* sym))

(defn get-app []
  (deref (resolve-sym 'storage.core/app)))

(defn start-app! [!instance env & app]
  (let [instance (start! (or app (get-app)) env)]
    (dosync
      (ref-set !instance instance))
    instance))

(defn stop-app! [!instance]
  (let [instance @!instance
        app (instance :app)]
    (stop! app instance)
    (dosync
      (ref-set !instance nil))
    nil))

(defn- get-env []
  (deref (resolve-sym 'environ.core/env)))

(defn- bind-user-tools! [!instance]
  (intern 'user '!app !instance)
  (intern 'user 'stop-app! #((deref (resolve-sym 'storage.core/stop-app!)) !instance))
  (intern 'user 'reload-app! #((deref (resolve-sym 'storage.core/reload-dev-app!)) !instance)))

(defn- reload-dev-app! [!instance & env]
  (when @!instance
    (stop-app! !instance))
  (refresh)
  ((deref (resolve-sym 'storage.core/bind-user-tools!)) !instance)
  ((deref (resolve-sym 'storage.core/start-app!)) !instance (or env
                                                                ((deref (resolve-sym 'storage.core/get-env))))))

(defn start-dev! [!instance env]
  (start-nrepl!)
  (reload-dev-app! !instance))

(defn -main [& [command & args]]
  (let [!app (ref nil)
        env environ/env]
    (case (or command "main")
      "dev" (start-dev! !app env)
      "main" (start-app! !app env app))))
