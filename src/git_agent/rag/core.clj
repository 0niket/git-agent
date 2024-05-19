(ns git-agent.rag.core
  (:require [wkok.openai-clojure.api :as api]
            [git-agent.indexing.core :as indexing]
            [clojure.string :as cs]))

(def prompt-path "./resources/prompts/rag.txt")

(defn query
  [question]
  (let [documents (indexing/query-gitlog question 3)
        context (->> documents
                     (map :commit)
                     (cs/join "\n\n"))
        prompt (format (slurp prompt-path) question context)
        rs (api/create-chat-completion {:model "gpt-3.5-turbo"
                                        :messages [{:role "user"
                                                    :content prompt}]}
                                       {:api-key ""})]
    (get-in rs [:choices 0 :message :content])))