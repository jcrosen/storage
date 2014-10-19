(ns storage.store
  (:refer-clojure :exclude [key])
  (:require [aws.sdk.s3 :as s3]
            [clojure.java.io :as io]))

(defprotocol StorageAdapter
  "A storage adapter abstraction"
  (s-delete! [this bucket key] "delete data referenced by key from bucket")
  (s-get [this bucket key] "return data referenced by key from bucket")
  (s-put! [this bucket key data] "put data referenced by key into bucket")
  (s-exists? [this bucket key] "return true if key exists in bucket"))

(deftype S3Storage [cred]
  StorageAdapter
  (s-delete! [this bucket key]
    (s3/delete-object cred bucket key))
  (s-get [this bucket key]
    (:content (s3/get-object cred bucket key)))
  (s-put! [this bucket key data]
    (s3/put-object cred bucket key data))
  (s-exists? [this bucket key]
    (s3/object-exists? cred bucket key)))

(deftype FileStorage [base-path]
  StorageAdapter
  (s-delete! [this bucket key]
    (io/delete-file (io/file base-path bucket key) true))
  (s-get [this bucket key]
    (io/input-stream (io/file base-path bucket key)))
  (s-put! [this bucket key data]
    (io/make-parents base-path bucket key)
    (io/copy data (io/file base-path bucket key)))
  (s-exists? [this bucket key]
    (.exists (io/file base-path bucket key))))

(defn gen-storage-proxy [adapter-options bucket-storage-map]
  "Given an adapter-map like {:file FileStorage} and bucket-storage-map like {\"bucket\" :file} this function returns a StorageAdapter proxy"
  (let [adapter-map (merge {}
                      (when-let [{:keys [aws-access-key aws-secret-key]} adapter-options]
                        {:s3 (S3Storage. {:access-key aws-access-key
                        :secret-key aws-secret-key})})
                      (when-let [{:keys [file-storage-path]} adapter-options]
                        {:file (FileStorage. file-storage-path)}))
        get-adapter (fn [bucket] (adapter-map (bucket-storage-map (keyword bucket))))]
    (reify StorageAdapter
      (s-delete! [this bucket key] (s-delete! (get-adapter bucket) bucket key))
      (s-get [this bucket key] (s-get (get-adapter bucket) bucket key))
      (s-put! [this bucket key data] (s-put! (get-adapter bucket) bucket key data))
      (s-exists? [this bucket key] (s-exists? (get-adapter bucket) bucket key)))))
