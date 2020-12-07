(ns chronicle.workload.crash
  (:require [chronicle
             [nemesis :as nemesis]
             [workload-util :as workload-util]]
            [jepsen
             [generator :as gen]]))

(defn nemesis-gen
  []
  [(gen/sleep 5)
   {:type :info :f :start}
   (gen/sleep 5)
   {:type :info :f :stop}
   (gen/sleep 10)])

(defn crash-workload
  [opts]
  {:nemesis (nemesis/node-crash)
   :generator (gen/nemesis
               (nemesis-gen)
               (gen/time-limit 45 (workload-util/client-gen opts)))})
