(ns deckcrew.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [deckcrew.actor :as actor]
            [deckcrew.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-vessel! st {:vessel-id "V-1" :name "MV Kotoba"})
    (store/register-crew-member! st {:crew-id "crew-1" :name "A. Seaman"
                                     :vessel-id "V-1"})
    st))

(deftest commits-a-routine-service-record-log
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:crew-id "crew-1" :op :log-service-record :stake :low
                 :vessel-id "V-1" :entry-text "morning watch handover, no defects"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "crew-1"))))))

(deftest commits-a-within-threshold-supply-order
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:crew-id "crew-1" :op :coordinate-supply-order :stake :low
                 :vessel-id "V-1" :items ["mooring lines"] :cost 500}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "crew-1"))))))

(deftest holds-a-proposal-with-unregistered-vessel
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:crew-id "crew-1" :op :schedule-crew-operation :stake :low
                 :vessel-id "V-ghost"}
        result (actor/run-request! graph request {} "thread-3")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "crew-1")))))

(deftest interrupts-then-approves-a-safety-concern-flag-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:crew-id "crew-1" :op :flag-safety-concern :stake :low
                 :vessel-id "V-1" :concern-type :weather-hazard
                 :detail "deteriorating weather forecast for the evening watch"}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "crew-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "crew-1")))))))

(deftest interrupts-then-approves-an-over-threshold-supply-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:crew-id "crew-1" :op :coordinate-supply-order :stake :low
                 :vessel-id "V-1" :items ["replacement fenders"] :cost 9000}
        interrupted (actor/run-request! graph request {} "thread-5")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "crew-1")))
    (let [resumed (actor/approve! graph "thread-5")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "crew-1")))))))
