(ns chronicle.core
  (:require [chronicle.cli :as chronicle-cli]
            [chronicle.client :as client]
            [chronicle.util :as util]
            [clojure.tools.logging :refer [info warn error fatal]]
            [jepsen
             [checker :as checker]
             [cli :as cli]
             [db :as db]
             [independent :as indep]
             [nemesis :as nemesis]
             [tests :as tests]]
            [jepsen.checker.timeline :as timeline]
            [jepsen.generator :as gen]
            [jepsen.nemesis.time]
            [knossos.model :as model]))

(defn chronicle-db
  []
  (reify
    db/DB
    (setup!    [_ test node] (util/setup-node test))
    (teardown! [_ test node] (util/teardown-node))

    db/Primary
    (setup-primary! [_ test node] (util/setup-cluster test node))))

;; Simple test generator until we write real tests
(defn simple-gen
  []
  (indep/concurrent-generator
   5 ; Threads per key
   (range)
   (fn [_]
     (gen/phases
      (gen/once {:f :write :value 0 :f-type :put})
      (gen/mix [(gen/repeat {:f :read})
                (map (fn [x] {:f :write :value x :f-type :post}) (drop 1 (range)))])))))

;; Testcase setup
(defn chronicle-test
  "Run the test"
  [opts]
  (merge tests/noop-test
         opts
         {:name "Chronicle"
          :pure-generators true
          :db (chronicle-db)
          :nemesis nemesis/noop
          :client (client/base-client)
          :generator (gen/clients (gen/time-limit 30 (simple-gen)))
          :checker (checker/compose
                    {:indep (indep/checker
                             (checker/compose
                              {:timeline (timeline/html)
                               :linear (checker/linearizable
                                        {:model (model/cas-register -1)})}))
                     :perf (checker/perf)})}))

(defn -main
  "Run the test specified by the cli arguments"
  [& args]
  ;; Parse args and run the test
  (let [testData (cli/single-test-cmd {:test-fn chronicle-test
                                       :opt-spec chronicle-cli/extra-cli-opts})
        serve (cli/serve-cmd)]
    (cli/run! (merge testData serve) args)))
