(ns storage.store
  (:refer-clojure :exclude [key])
  (:require [storage.util :refer [get-sym]]
            [aws.sdk.s3 :as s3]
            [clojure.java.io :as io]
            [clj-http.client :as http]))

(defprotocol StorageAdapter
  "A storage adapter abstraction"
  (s-delete! [this bucket key] "delete data referenced by key from bucket")
  (s-get [this bucket key] "return data referenced by key from bucket")
  (s-put! [this bucket key data] "put data referenced by key into bucket")
  (s-exists? [this bucket key] "return true if key exists in bucket"))

(deftype S3Storage [config]
  StorageAdapter
  (s-delete! [this bucket key]
    (s3/delete-object config bucket key))
  (s-get [this bucket key]
    (:content (s3/get-object config bucket key)))
  (s-put! [this bucket key data]
    (s3/put-object config bucket key data))
  (s-exists? [this bucket key]
    (s3/object-exists? config bucket key)))

(defn urljoin [base suffix & more]
  (let [joiner "/"
        find-joiner #((= % joiner))
        sbase (if (find-joiner (last base)) (butlast base) base)
        ssuffix (if (find-joiner (first suffix)) (rest suffix) suffix)
        newbase (str sbase "/" ssuffix)]
    (if (empty? more)
      newbase
      (recur newbase (first more) (rest more)))))

(deftype HTTPStorage [config]
  StorageAdapter
  (s-delete! [this bucket key]
    (when-let [url (urljoin (config :base-url) bucket key)]
      (http/delete url)))
  (s-get [this bucket key]
    (when-let [url (urljoin (config :base-url) bucket key)]
      (when-let [response (http/get url)]
        (io/input-stream (response :body)))))
  (s-put! [this bucket key data]
    (when-let [url (urljoin (config :base-url) bucket key)]
      (println url)))
  (s-exists? [this bucket key]
    (when-let [url (urljoin (config :base-url) bucket key)]
      (http/head url))))

(deftype FileStorage [config]
  StorageAdapter
  (s-delete! [this bucket key]
    (io/delete-file (io/file (config :base-path) bucket key) true))
  (s-get [this bucket key]
    (io/input-stream (io/file (config :base-path) bucket key)))
  (s-put! [this bucket key data]
    (io/make-parents (config :base-path) bucket key)
    (io/copy data (io/file (config :base-path) bucket key)))
  (s-exists? [this bucket key]
    (.exists (io/file (config :base-path) bucket key))))

(defn- gen-adapter [profile]
  (let [adapter-sym (first profile)
        sym (if (symbol? adapter-sym) adapter-sym (symbol adapter-sym))
        args (second profile)]
    ; Using eval here is a bit dangerous and slow but required given the
    ; constraints of clojure's interop with Java classes
    ; consider revising...
    (eval `(new ~sym ~args))))

(defn- gen-alias-storage-map [storage-aliases]
  (zipmap (keys storage-aliases) (map gen-adapter (vals storage-aliases))))

(defn gen-storage [storage-aliases bucket-alias-map]
  (let [alias-storage-map (gen-alias-storage-map storage-aliases)
        get-adapter #(alias-storage-map (bucket-alias-map (keyword %)))]
    (reify StorageAdapter
      (s-delete! [this bucket key] (s-delete! (get-adapter bucket) bucket key))
      (s-get [this bucket key] (s-get (get-adapter bucket) bucket key))
      (s-put! [this bucket key data] (s-put! (get-adapter bucket) bucket key data))
      (s-exists? [this bucket key] (s-exists? (get-adapter bucket) bucket key)))))
