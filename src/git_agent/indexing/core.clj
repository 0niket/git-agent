(ns git-agent.indexing.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as cs]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [wkok.openai-clojure.api :as api]))

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

(defn create-embedding
  [text]
  (-> (api/create-embedding {:model "text-embedding-ada-002" :input text}
                            {:api-key ""})
      (get-in [:data 0 :embedding] [])))

(def db {:dbtype "postgres"
         :dbname "gitagent"
         :user "git_agent"
         :password "wildmushroom"
         :host "localhost"
         :port "5432"})

(def ds (jdbc/get-datasource db))

(def db-opts (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps}))

(defn create-gitlog-table!
  []
  (jdbc/execute!
   db-opts
   ["CREATE TABLE gitlog (
      id bigserial PRIMARY KEY,
      embedding vector(1536),
      commit text
     )"]))

(defn drop-gitlog-table
  []
  (jdbc/execute!
   db-opts
   ["DROP TABLE gitlog"]))

(defn exec-insert-commit-log!
  [commit embedding-str]
  (let [sql-stmt (format "INSERT INTO gitlog (embedding, commit)
                          VALUES ('[%s]', ?);"
                         embedding-str)]
    (jdbc/execute!
     db-opts
     [sql-stmt commit])))

(defn insert-commit-log!
  [commit]
  (->> commit
       create-embedding
       (cs/join ", ")
       (exec-insert-commit-log! commit)))

(defn exec-nearest-neighbors-by-l2-distance
  [k query]
  (let [sql-stmt (format "SELECT embedding, commit FROM gitlog
                          ORDER BY embedding <-> '[%s]'
                          limit %s;"
                         query
                         k)]
    (jdbc/execute!
     db-opts
     [sql-stmt])))

(defn query-gitlog
  [query k]
  (->> query
       create-embedding
       (cs/join ", ")
       (exec-nearest-neighbors-by-l2-distance k)))