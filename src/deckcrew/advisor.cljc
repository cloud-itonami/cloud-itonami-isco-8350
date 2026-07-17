(ns deckcrew.advisor
  "Deck Crew Advisor — the advisor named in this repository's README,
  proposing a crew-scheduling/logistics operation (log a service
  record, propose a watch/duty schedule, flag a safety concern,
  coordinate a supply order) from a crew-member/vessel context.
  Swappable mock/llm; the advisor ONLY proposes —
  `deckcrew.governor` independently checks crew-member/vessel
  provenance, the closed op-allowlist and a mooring/cargo-handling/
  heavy-weather scope exclusion, and always escalates safety concerns
  and over-threshold supply orders. Modeled on
  cloud-itonami-isco-3313's advisor.

  The advisor coordinates scheduling and logistics ONLY — it never
  proposes to directly finalize a mooring/cargo-handling operational
  decision, a heavy-weather deck-work go/no-go decision, or to
  override a deck officer's safety judgment; those stay with the
  human deck crew and officer.

  A proposal: {:op :log-service-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-supply-order
               :effect :propose :crew-id str :vessel-id str
               :entry-text str :concern-type kw :items [str] :cost
               number :stake kw :confidence n :rationale str}

  Known self-tripping bug pattern (documented here so it is not
  reintroduced): the domain legitimately mentions bare nouns like
  'mooring' (e.g. ordering mooring lines) and 'weather' (e.g. a
  weather-hazard safety concern) in ordinary, in-scope proposals. The
  governor's scope-exclusion check must never match on those bare
  nouns — only on full finalization/execution action phrases such as
  'initiate the mooring operation' or 'commence deck work in heavy
  weather'. See `deckcrew.governor` and
  `deckcrew.advisor-test`/`deckcrew.governor-test`'s
  never-self-trips assertions."
  (:require [clojure.string :as str]))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- default-rationale
  [{:keys [op crew-id vessel-id entry-text concern-type detail items cost]}]
  (case op
    :log-service-record
    (str "logging service record for crew " crew-id " aboard vessel " vessel-id
         (when entry-text (str ": " entry-text)))
    :schedule-crew-operation
    (str "proposed watch/duty schedule for crew " crew-id " aboard vessel " vessel-id)
    :flag-safety-concern
    (str "flagging " (name (or concern-type :general)) " concern for crew " crew-id
         " aboard vessel " vessel-id (when detail (str ": " detail)))
    :coordinate-supply-order
    (str "proposed supply order ("
         (str/join ", " (or items ["mooring lines" "safety gear"]))
         ") for vessel " vessel-id ", cost " cost)
    (str "proposed " (name op) " for crew " crew-id " aboard vessel " vessel-id)))

(defn- infer
  [_store {:keys [op stake crew-id vessel-id entry-text concern-type detail items cost]
           :as request}]
  {:op op
   :effect :propose
   :crew-id crew-id
   :vessel-id vessel-id
   :entry-text entry-text
   :concern-type concern-type
   :detail detail
   :items items
   :cost cost
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (default-rationale request)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a deck-crew scheduling/logistics advisor. Given a request,
   propose an :op (only :log-service-record, :schedule-crew-operation,
   :flag-safety-concern or :coordinate-supply-order), the :crew-id,
   :vessel-id, an honest :confidence and a :stake. You coordinate crew
   scheduling and logistics ONLY — never propose to directly finalize
   a mooring/cargo-handling operational decision, a heavy-weather
   deck-work go/no-go decision, or to override a deck officer's
   safety judgment; those always stay with the human deck crew and
   officer. Always flag safety concerns (equipment defects, weather
   hazards, crew fatigue) rather than deciding them yourself — the
   governor always escalates them to human sign-off regardless of
   confidence, as does a supply order above the cost threshold.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
