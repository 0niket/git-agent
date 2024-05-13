(ns git-agent.indexing.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as cs]))

(defn git-log
  "Retrieve git log for a given repository path.
  
  Arguments:
    - path: A string representing the path to the git repository.
    - :max-count (optional, default 30): Maximum number of commits to return.
    - :skip (optional, default 0): Number of commits to skip.
  
  Returns a map containing:
    - :commits: A sequence of maps, each representing a commit.
    - :cursor: The number of commits skipped plus the count of commits returned.
    - :next?: A boolean indicating if there are more commits to fetch.
  
  If an exception occurs during the execution of git log command,
  a custom exception is thrown with the following structure:
    {:type :git-log-exception, :err <error message>}
  
  Example:
    (git-log \"/path/to/repository\" :max-count 10 :skip 5)
  "
  [path & {:keys [max-count skip] :or {max-count 30 skip 0}}]
  (let [log (sh "git"
                "-C"
                path
                "log"
                "-p"
                (format "--skip=%s" skip)
                (format "--max-count=%s" max-count))
        commits (if (= 0 (:exit log))
                  (-> log
                      :out
                      (cs/split #"commit ")
                      rest)
                  (throw (ex-info "git log caused an exception"
                                  {:type :git-log-exception
                                   :err (:err log)})))
        rs-count (count commits)]
    {:commits commits
     :cursor (+ skip rs-count)
     :next? (= rs-count max-count)}))