(ns kodekamp.fs
  (:import (java.io File)
           (java.nio.file Files StandardCopyOption)))

(defn ensure-dir [dir]
  (.mkdirs (File. dir)))

(defn write-file
  "Writes the file to a temporary file, then performs an atomic move, ensuring
  that cache files are never partial files."
  [file content]
  (let [target-file (if (isa? File file) file (File. ^String file))]
    (ensure-dir (.getParent target-file))
    (let [tmp-f (File/createTempFile (.getName target-file) ".tmp" (.getParentFile target-file))]
      (spit tmp-f content)
      (Files/move (.toPath tmp-f) (.toPath target-file) (into-array [StandardCopyOption/ATOMIC_MOVE])))))

(comment

  (write-file "/tmp/2021/01/12/lol.edn" "{}")

)
