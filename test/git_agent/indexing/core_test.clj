(ns git-agent.indexing.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [git-agent.indexing.core :as indexing]))

(def repo "/Users/anikethendre/projects/instructor-clj")

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

(deftest vector-embedding-test)

(deftest db-indexing-test)

(deftest similarity-search-test)