(ns storage.util)

(defn resolve-sym [sym]
  (when-let [sym-ns (namespace sym)]
    (require (symbol sym-ns)))
  (ns-resolve *ns* sym))

(defn get-sym [sym]
  (deref (resolve-sym sym)))
