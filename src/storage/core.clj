(ns storage.core
  (:refer-clojure :exclude [key])
  (:require [storage.serve :refer [gen-handler]]
            [environ.core :as environ]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.namespace.repl :refer [refresh]]
            [storage.store :refer [gen-storage]]
            [storage.util :refer [get-sym]]
            [storage.nrepl :refer [start-nrepl!]]
            [org.httpkit.server :as s]
            [clojurewerkz.spyglass.client :as mc])
  (:gen-class))

(defn get-storage-aliases [env]
  (or (edn/read-string (env :storage-aliases)) {}))

(defn get-bucket-alias-map [env]
  (or (edn/read-string (env :bucket-alias-map)) {}))

(defn gen-mc-conn [env]
  (when-let [memcached-servers (:storage-memcached-servers env)]
    (let [memcached-user (:memcached-user env)
          memcached-pass (:memcached-pass env)]
      (if (and memcached-user memcached-pass)
        (do (prn (str memcached-user ":" memcached-pass))
            (mc/bin-connection memcached-servers memcached-user memcached-pass))
        (mc/bin-connection memcached-servers)))))

(defprotocol App
  (start! [_ env])
  (stop! [_ system]))

(def app
  (reify App
    (start! [_ env]
      (let [storage (gen-storage (get-storage-aliases env)
                                 (get-bucket-alias-map env))
            memcached nil
            context {:storage storage
                     :memcached memcached}
            handler (gen-handler context)
            port (edn/read-string (or (env :serve-port) "3000"))]
        (println (str "Starting http storage app at " (java.util.Date.) " on port " port "!"))
        {:context context
         :handler handler
         :server (s/run-server handler {:port port})
         :serve-port port
         :stop! #(when-let [server (% :server)] (server))
         :app _}))
    (stop! [_ system]
      (let [port (system :port)
            on-port (if port (str " on port " port) "")]
        (println (str "Shutting down http storage app at " (java.util.Date.) on-port "!"))
        (when-let [stop-fn (system :stop!)]
          (stop-fn system)
          system)))))

(defn start-app! [!instance env & app-sym]
  (let [app (get-sym (or app-sym 'storage.core/app))
        instance (start! app env)]
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

(defn- bind-user-tools! [!instance]
  (intern 'user '!app !instance)
  (intern 'user 'stop-app! #((get-sym 'storage.core/stop-app!) !instance))
  (intern 'user 'reload-app! #((get-sym 'storage.core/reload-dev-app!) !instance)))

(defn- reload-dev-app! [!instance & env]
  (when @!instance
    (stop-app! !instance))
  (refresh)
  ((get-sym 'storage.core/bind-user-tools!) !instance)
  ((get-sym 'storage.core/start-app!) !instance (get-sym 'environ.core/env)))

(defn start-dev! [!instance env]
  (start-nrepl! (edn/read-string (or (env :nrepl-port) "7888")))
  (reload-dev-app! !instance))

(defn -main [& [command & args]]
  (let [!app (ref nil)
        env environ/env]
    (case (or command "main")
      "dev" (start-dev! !app env)
      "main" (start-app! !app env))))
