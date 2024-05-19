(ns git-agent.indexing.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [git-agent.indexing.core :as indexing]
            [clojure.string :as cs]
            [git-agent.utils :as utils]))

(def repo "/Users/anikethendre/projects/instructor-clj")
(def gitlog-test-data-path "./resources/test-data/gitlog-test-data.txt")

(defn create-db
  []
  (try
    (indexing/create-gitlog-table!)
    (catch Exception _e
      (println "Table already exists, skipping"))))

(defn destroy-db
  []
  (indexing/drop-gitlog-table))

(defn test-fixture
  [f]
  (create-db)
  (f)
  (destroy-db))

(use-fixtures :once test-fixture)

(deftest read-git-log-test
  (testing "Read git log"
    (is (= [:commits :cursor :next?] (keys (indexing/git-log repo)))
        "git-log should return a map with list of commits & cursor")
    (is (= 5 (-> (indexing/git-log repo :max-count 5)
                 :commits
                 count))
        "Number of commits returned should respect max-count param")
    (is (= 10 (:cursor (indexing/git-log repo :skip 5 :max-count 5)))
        "cursor should be 10 i.e. skip 5 commits and take next 5 commits")))

(deftest vector-embedding-test
  (testing "Text to vector using embedding model"
    (is (= (-> "resources/test-data/openai-embedding-test-data.edn"
               utils/load-edn
               :embedding)
           (indexing/create-embedding "foo bar")))))

(deftest similarity-search-test
  (testing "Insert test commit and retrieve results by similarity search"
    (let [gitlog-test-data (slurp gitlog-test-data-path)
          commits (-> gitlog-test-data
                      (cs/split #"commit ")
                      rest)
          test-commit (first commits)
          query "What is use of pgvector?"
          _ (indexing/insert-commit-log! test-commit)
          rs (indexing/query-gitlog query 5)]
      (is (= [:embedding :commit] (-> rs first keys)))
      (is (vector? (-> rs
                       (get-in [0 :embedding])
                       (.getValue)
                       read-string))
          "Embedding should be a vector")
      (is (> (-> rs (get-in [0 :commit]) (cs/index-of "pgvector")) -1)
          "Fetched document should be related to pgvector")
      (is (<= (count rs) 5)
          "Count of documents should be less than or equal to 5")
      (is (> (count rs) 0)
          "Similarity search should return atleast 1 document"))))