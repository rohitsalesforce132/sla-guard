# Context Development Lifecycle

## Overview

Context is a first-class artifact, just like code. It has a lifecycle: **Generate → Evaluate → Distribute → Observe**, supported by team practices that make context a shared, repeatable, and improvable part of software delivery.

## The Four Phases

```
┌───────────┐     ┌───────────┐     ┌─────────────┐     ┌───────────┐
│ GENERATE  │────►│ EVALUATE  │────►│ DISTRIBUTE   │────►│ OBSERVE   │
│           │     │           │     │              │     │           │
│ Create    │     │ Quality   │     │ Deliver to   │     │ Monitor   │
│ context   │     │ check     │     │ right place  │     │ & improve │
└───────────┘     └───────────┘     └─────────────┘     └───────────┘
       ▲                                                        │
       └────────────── Feedback loop ──────────────────────────┘
```

### 1. GENERATE — Create Context

Produce the context that feeds AI systems, teams, and decisions.

| Source | What Gets Generated | Example |
|--------|-------------------|---------|
| Network Slicing SLAs | Thresholds, baselines, escalation policies | SLA definitions per slice type |
| KPI Stream | Baselines, anomaly signatures, trends | Rolling 30-day baselines per cell |
| Incident History | Root cause patterns, remediation recipes | Case store with learned outcomes |
| CAMARA API Specs | QoS profiles, session templates, API schemas | QoS profile catalog |
| Team Decisions | ADRs, runbooks, escalation playbooks | `CLAUDE.md`, `copilot-instructions.md` |

**Key practice:** Every engineer who debugs an SLA breach, tunes a threshold, or writes a runbook is generating context. Capture it at the source.

### 2. EVALUATE — Quality Check Context

Measure whether context is accurate, current, and useful.

| Check | Question | Failure Mode |
|-------|----------|--------------|
| **Freshness** | When was this last updated? | Stale baseline → false breach alerts |
| **Accuracy** | Does this match production? | Drifted thresholds → missed breaches |
| **Coverage** | Are there gaps? | Missing slice type → no monitoring |
| **Relevance** | Is this the right context for this breach? | Wrong QoS profile applied |
| **Redundancy** | Are there duplicates/conflicts? | Conflicting SLA definitions per slice |

**In this project:**
- EMA prediction accuracy — is the breach predictor still calibrated?
- Baseline drift — have KPI baselines shifted since last calculation?
- CAMARA QoD session success rate — are sessions actually boosting QoS?
- False positive rate — are we triggering remediation too aggressively?

### 3. DISTRIBUTE — Get Context Where It's Needed

Deliver the right context, to the right agent/person, at the right time.

```
┌─────────────────────────────────────────────────────┐
│                  Context Registry                     │
│  (single source of truth — versioned, searchable)    │
└──────────┬──────────┬──────────┬──────────┬──────────┘
           │          │          │          │
      ┌────▼───┐ ┌───▼────┐ ┌──▼───┐ ┌───▼────┐
      │ Edge   │ │ Core   │ │Cloud │ │Human   │
      │ Monitor│ │ TMF GW │ │ LLM  │ │ NOC    │
      └────────┘ └────────┘ └──────┘ └────────┘
```

**Distribution patterns:**
- **Push:** Updated baselines and thresholds pushed to edge monitors
- **Pull:** CAMARA QoD profiles retrieved on-demand for session creation
- **Event-driven:** Breach predicted → context (baseline, history, similar cases) injected into remediation pipeline
- **Human-in-the-loop:** NOC dashboard shows breach context before approving remediation

### 4. OBSERVE — Monitor Context in Production

Track how context performs and feed back improvements.

| Metric | What It Measures | Target |
|--------|-----------------|--------|
| Prediction accuracy | Breach predictor hit rate | > 85% |
| False positive rate | Unnecessary remediation triggers | < 10% |
| Mean time to detect | Seconds from KPI arrival to breach alert | < 30s |
| Remediation success | % of cases where SLA restored after action | > 90% |
| Context staleness | Days since baseline/threshold update | < 7 days |

---

## Team Practices

### Context as Code
Store context in version-controlled files, not in people's heads.
- `CLAUDE.md` — coding context, versioned
- `copilot-instructions.md` — project context, versioned
- `DEPLOYMENT.md` — infrastructure context, versioned

### Context Reviews
Review context changes like code changes.
- Updated an SLA threshold? Review why with NOC team.
- Added a new remediation action? Validate safety guardrails.
- Changed CAMARA QoS profile? Test in staging first.

### Context Ownership
Every piece of context has an owner who keeps it fresh.
- SLA definitions → Product team
- KPI baselines → Network operations team
- CAMARA profiles → Telecom API team
- Case store → SRE team

### Context Debt
Track stale/missing context like technical debt.
- "IoT slice SLA not calibrated for peak hours" = context debt
- "CAMARA QoS profiles not tested with new vendor equipment" = context debt
- "Intent parser not updated for new slice types" = context debt

### Context Rituals
Build context maintenance into team ceremonies.
- **Sprint planning:** Review context debt, prioritize baseline recalibrations
- **Post-incident:** Extract new breach patterns into knowledge base
- **Weekly:** Check false positive rates and prediction drift
- **Monthly:** Full baseline recalculation and threshold audit

---

## Interview Answer

> "We treat context as a first-class artifact with its own lifecycle. In SLA-Guard, the context is the SLA definitions, KPI baselines, and breach history. We generate baselines from 30-day rolling windows, evaluate them weekly for drift, distribute updated thresholds to edge monitors via push, and observe prediction accuracy in production. When false positives increase, that's a signal that our context is stale — the baseline needs recalculation. The team practices that make this work: context is version-controlled, reviewed, owned by specific teams, and tracked like technical debt. In telecom, stale context means missed SLA breaches or unnecessary remediation — neither is acceptable."
