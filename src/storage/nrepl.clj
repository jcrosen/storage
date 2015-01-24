(ns ^{:clojure.tools.namespace.repl/load false}
    storage.nrepl
    (:require [clojure.tools.nrepl.server :as nrepl]
              [clojure.java.io :as io]))

(defn start-nrepl! [& port]
  (let [nrepl-port (or (first port) 7888)]
    (nrepl/start-server :port nrepl-port)
    (println (str "Started nREPL server, port " nrepl-port))))
