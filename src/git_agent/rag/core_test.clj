(ns git-agent.rag.core-test
  (:require [clojure.string :as cs]
            [clojure.test :refer [is deftest testing use-fixtures]]
            [git-agent.indexing.core :as indexing]
            [git-agent.rag.core :as rag]))

(def gitlog-test-data-path "./resources/test-data/gitlog-test-data.txt")

(defn create-gitlog-table
  []
  (try
    (indexing/create-gitlog-table!)
    (catch Exception _e
      (println "Table already exists, skipping"))))

(defn insert-git-commits
  []
  (let [gitlog-test-data (slurp gitlog-test-data-path)
        commits (-> gitlog-test-data
                    (cs/split #"commit ")
                    rest)]
    (doseq [commit commits]
      (indexing/insert-commit-log! commit))))

(defn create-db
  []
  (create-gitlog-table)
  (insert-git-commits))

(defn destroy-db
  []
  (indexing/drop-gitlog-table))

(defn test-fixture
  [f]
  (create-db)
  (f)
  (destroy-db))

(use-fixtures :once test-fixture)


(deftest rag-query-test
  (testing "Questions about codebase"
    (let [reply (rag/query "What is name of the project?")]
      (is (re-matches #"(?i).*git[-_]?agent.*" reply)
          "Name of the project should be returned as git-agent"))
    (let [reply (rag/query "Which programming languages are used in the codebase?")]
      (is (re-matches #"(?i).*clojure.*" reply)
          "LLM should mention clojure programming language")))

  (testing "Ops related questions"
    (let [reply (rag/query "What is command for connecting using pgcli?")]
      (is (re-matches #"(?i).*pgcli -h localhost -p 5432 -U git_agent -d gitagent.*" reply)
          "LLM should mention the pgcli command from readme file"))
    (let [reply (rag/query "Which build tool does this project use?")]
      (is (re-matches #"(?i).*leiningen.*" reply)
          "LLM should mention leiningen")))

  (testing "Questions about code contribution analysis"
    (let [reply (rag/query "Who has highest number of commits?")]
      (is (re-matches #"(?i).*aniket hendre.*" reply)
          "LLM should mention Aniket Hendre"))
    (let [reply (rag/query "When was the first commit added to repository?")]
      (is (re-matches #"(?i).*may 10.*" reply)
          "LLM should mention the date of the first commit"))))