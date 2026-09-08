(ns deckcrew.governor
  "DeckCrewGovernor — the independent safety/traceability layer named
  in this repository's README/business-model.md, gating every
  crew-scheduling/logistics operation an advisor may propose. The
  governor never dispatches hardware itself, never performs a deck
  operation, and never finalizes a mooring/cargo-handling operational
  decision or a heavy-weather deck-work go/no-go decision — this
  actor coordinates crew scheduling and logistics ONLY. Modeled on
  cloud-itonami-isco-3313's accountingsupport.governor.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. crew-member provenance     — the crew member must be
                                    independently verified/registered.
    2. no-actuation                — proposal :effect must be
                                    :propose (the governor never
                                    dispatches hardware and never
                                    performs a deck operation itself;
                                    it only gates what the advisor may
                                    propose).
    3. vessel provenance           — the vessel must be independently
                                    verified/registered before any
                                    action.
    4. crew/vessel basis           — the crew member's registered
                                    vessel assignment must match the
                                    vessel named in the proposal.
    5. closed op-allowlist         — :op must be one of
                                    #{:log-service-record
                                      :schedule-crew-operation
                                      :flag-safety-concern
                                      :coordinate-supply-order}. No
                                    other op is ever accepted, so a
                                    proposal can never finalize a
                                    mooring/cargo-handling operational
                                    decision or a heavy-weather
                                    deck-work go/no-go decision through
                                    the op itself.
    6. scope exclusion             — defense in depth against a
                                    compromised/malicious advisor:
                                    even within an allowed op, the
                                    proposal's free text must not
                                    describe directly finalizing a
                                    mooring/cargo-handling operational
                                    decision, commencing/authorizing
                                    deck work in heavy weather, or
                                    overriding a deck officer's safety
                                    judgment. This is phrased as
                                    finalization/execution ACTION
                                    phrases ('initiate the mooring
                                    operation', 'commence deck work in
                                    heavy weather'), never bare nouns
                                    ('mooring', 'weather') — bare nouns
                                    appear in ordinary in-scope
                                    proposals (ordering mooring lines,
                                    flagging a weather hazard) and a
                                    bare-noun check would self-trip on
                                    the mock advisor's own default
                                    rationale text. See
                                    `deckcrew.advisor`'s docstring and
                                    `never-self-trips-on-default-mock-advisor-proposals`
                                    below.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    7. :op :flag-safety-concern (equipment defect, weather hazard or
                                    crew fatigue concerns always reach
                                    a human).
    8. :coordinate-supply-order cost above `supply-order-cost-threshold`.
    9. low confidence (< `confidence-floor`)."
  (:require [deckcrew.store :as store]
            [kotoba.lang.text :as str]))

(def confidence-floor 0.6)
(def supply-order-cost-threshold 2000)

(def ^:private allowed-ops
  #{:log-service-record :schedule-crew-operation :flag-safety-concern
    :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; like "mooring" or "weather" (see docstring above and
;; deckcrew.advisor's self-tripping-bug note).
(def ^:private scope-exclusion-phrases
  ["initiate the mooring operation"
   "commence the mooring operation"
   "finalize the mooring operation"
   "finalize the mooring decision"
   "initiate the cargo-handling operation"
   "commence the cargo-handling operation"
   "finalize the cargo-handling operation"
   "finalize the cargo-handling decision"
   "commence deck work in heavy weather"
   "authorize deck work in heavy weather"
   "proceed with deck work in heavy weather"
   "override the deck officer's safety judgment"
   "override the deck officer"])

(defn- scope-text [proposal]
  (str/lower (str/join " " (keep proposal [:rationale :detail :action]))))

(defn- scope-violation? [proposal]
  (let [text (scope-text proposal)]
    (boolean (some #(str/includes? text %) scope-exclusion-phrases))))

(defn- hard-violations [{:keys [proposal]} crew-record vessel-record]
  (let [{:keys [op effect vessel-id]} proposal]
    (cond-> []
      (nil? crew-record)
      (conj {:rule :no-crew-member :detail "未登録 crew-member"})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はデッキ作業を直接実行しない）"})

      (nil? vessel-record)
      (conj {:rule :no-vessel :detail "未登録 vessel"})

      (and crew-record vessel-record (not= (:vessel-id crew-record) vessel-id))
      (conj {:rule :crew-wrong-vessel :detail "crew-member が別 vessel に登録されている"})

      (not (contains? allowed-ops op))
      (conj {:rule :op-not-allowed :detail "closed op-allowlist 外の op（mooring/cargo-handling の直接決定や heavy-weather go/no-go は決して許可しない）"})

      (scope-violation? proposal)
      (conj {:rule :scope-exclusion
             :detail "mooring/cargo-handling operational decision の直接確定、heavy-weather deck-work go/no-go の直接決定、deck officer の安全判断の override は permanent hard block"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `deckcrew.store/Store`. Pure — never mutates
  the store, never performs a deck operation."
  [request context proposal store]
  (let [crew-record (store/crew-member store (:crew-id request))
        vessel-record (some->> (:vessel-id proposal) (store/vessel store))
        hard (hard-violations {:request request :proposal proposal}
                              crew-record vessel-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        over-cost? (and (= :coordinate-supply-order (:op proposal))
                        (number? (:cost proposal))
                        (> (:cost proposal) supply-order-cost-threshold))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-cost?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-cost?))}))
