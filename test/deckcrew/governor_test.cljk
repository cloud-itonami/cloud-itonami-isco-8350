(ns deckcrew.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [deckcrew.store :as store]
            [deckcrew.advisor :as advisor]
            [deckcrew.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-vessel! st {:vessel-id "V-1" :name "MV Kotoba"})
    (store/register-crew-member! st {:crew-id "crew-1" :name "A. Seaman"
                                     :vessel-id "V-1"})
    st))

(def ^:private req {:crew-id "crew-1"})

(defn- proposal [overrides]
  (merge {:op :log-service-record :effect :propose :vessel-id "V-1"
         :confidence 0.9 :stake :low}
        overrides))

(deftest ok-log-service-record
  (let [st (fresh-store)
        v (governor/check req {} (proposal {}) st)]
    (is (:ok? v))))

(deftest ok-within-threshold-supply-order
  (let [st (fresh-store)
        v (governor/check req {}
                          (proposal {:op :coordinate-supply-order :cost 500}) st)]
    (is (:ok? v))))

(deftest ok-at-exact-cost-threshold-boundary
  (testing "the supply-order cost threshold is inclusive"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal {:op :coordinate-supply-order
                                       :cost governor/supply-order-cost-threshold}) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-crew-member
  (let [st (fresh-store)
        v (governor/check {:crew-id "nobody"} {} (proposal {}) st)]
    (is (:hard? v))
    (is (some #(= :no-crew-member (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-vessel
  (let [st (fresh-store)
        v (governor/check req {} (proposal {:vessel-id "V-ghost"}) st)]
    (is (:hard? v))
    (is (some #(= :no-vessel (:rule %)) (:violations v)))))

(deftest hard-on-crew-wrong-vessel
  (let [st (fresh-store)]
    (store/register-vessel! st {:vessel-id "V-2" :name "MV Other"})
    (let [v (governor/check req {} (proposal {:vessel-id "V-2"}) st)]
      (is (:hard? v))
      (is (some #(= :crew-wrong-vessel (:rule %)) (:violations v))))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (proposal {:effect :direct-write}) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-not-in-allowlist
  (testing "closed op-allowlist — no op can directly finalize a mooring/cargo-handling decision or a heavy-weather go/no-go"
    (let [st (fresh-store)
          v (governor/check req {} (proposal {:op :finalize-mooring-operation}) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowed (:rule %)) (:violations v))))))

(deftest hard-on-scope-exclusion-mooring-finalization-phrase
  (testing "even within an allowed op, free text finalizing a mooring operation is a permanent hard block"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal {:rationale "recommend we initiate the mooring operation now"}) st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest hard-on-scope-exclusion-heavy-weather-go-no-go-phrase
  (testing "even within an allowed op, free text deciding to commence deck work in heavy weather is a permanent hard block"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal {:op :schedule-crew-operation
                                       :rationale "advisor will commence deck work in heavy weather regardless"}) st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest hard-on-scope-exclusion-officer-override-phrase
  (testing "overriding a deck officer's safety judgment is a permanent hard block"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal {:detail "override the deck officer's safety judgment on this call"}) st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest always-escalates-safety-concern-even-at-high-confidence
  (testing "equipment defect, weather hazard and crew fatigue concerns always reach a human"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal {:op :flag-safety-concern :concern-type :weather-hazard
                                       :confidence 0.99}) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-over-threshold-supply-order-even-at-high-confidence
  (let [st (fresh-store)
        v (governor/check req {}
                          (proposal {:op :coordinate-supply-order :cost 9000
                                     :confidence 0.99}) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (proposal {:confidence 0.3}) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

;; ---------------------------------------------------------------
;; Known self-tripping bug pattern (see deckcrew.advisor docstring
;; and deckcrew.governor docstring, invariant 6): the domain
;; legitimately mentions bare nouns "mooring" (ordering mooring
;; lines) and "weather" (flagging a weather hazard) in ordinary,
;; in-scope proposals. Assert the mock advisor's DEFAULT proposals —
;; across every allowed op, including ones whose default rationale
;; text mentions "mooring" or "weather" — never trip the
;; :scope-exclusion rule.
;; ---------------------------------------------------------------

(def ^:private default-requests
  [{:crew-id "crew-1" :vessel-id "V-1" :op :log-service-record
    :entry-text "no defects noted" :stake :low}
   {:crew-id "crew-1" :vessel-id "V-1" :op :schedule-crew-operation
    :stake :low}
   {:crew-id "crew-1" :vessel-id "V-1" :op :flag-safety-concern
    :concern-type :weather-hazard
    :detail "heavy weather forecast expected during the next watch"
    :stake :low}
   {:crew-id "crew-1" :vessel-id "V-1" :op :flag-safety-concern
    :concern-type :equipment-defect
    :detail "mooring winch showing signs of wear" :stake :low}
   {:crew-id "crew-1" :vessel-id "V-1" :op :coordinate-supply-order
    :items ["mooring lines" "fenders"] :cost 500 :stake :low}])

(deftest never-self-trips-on-default-mock-advisor-proposals
  (testing "bare nouns like 'mooring'/'weather' in ordinary in-scope proposals never trip scope-exclusion"
    (let [st (fresh-store)
          mock (advisor/mock-advisor)]
      (doseq [request default-requests]
        (let [p (advisor/-advise mock st request)
              v (governor/check request {} p st)]
          (is (not (some #(= :scope-exclusion (:rule %)) (:violations v)))
              (str "self-tripped on default proposal for " (:op request)
                   ": " (pr-str p))))))))
