(defproject git-agent "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.postgresql/postgresql "42.6.0"]
                 [com.github.seancorfield/next.jdbc "1.3.939"]
                 [net.clojars.wkok/openai-clojure "0.16.0"]]
  :main ^:skip-aot git-agent.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
