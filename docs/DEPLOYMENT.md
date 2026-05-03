# Deployment Strategy — Cloud, Edge, or On-Prem?

## TL;DR: Three-Tier Split

**Train in the cloud. Infer at the edge. Control plane in the core data center.**

5G network slicing demands different deployment tiers for different components — real-time monitoring at the edge, policy decisions in the core, and analytics/training in the cloud.

## Architecture

```
Edge (Cell Site / gNodeB)          Core DC (On-Prem)           Cloud (AWS/Azure/GCP)
┌──────────────────────────┐   ┌────────────────────┐   ┌──────────────────────────┐
│ KPI ingestion (real-time) │   │ SLA monitoring hub  │   │ Time-series model training│
│ Breach prediction (local) │   │ TMF API gateway     │   │ LLM inference (intents)   │
│ CAMARA QoD session mgmt   │   │ Remediation engine  │   │ Historical analytics      │
│ Health score calculator   │   │ Dashboard / Alerts  │   │ Pattern mining (batch)    │
│ < 10ms KPI evaluation     │   │ Case store / Audit  │   │ CI/CD + Model registry    │
└──────────────────────────┘   └────────────────────┘   └──────────────────────────┘
        │ < 10ms                       │ < 100ms                      │ batch/async
        └──────────────────────────────┴──────────────────────────────┘
                    CAMARA QoD sessions: Edge ↔ CAMARA Gateway
```

## Why This Architecture?

### 1. Data Sovereignty & Privacy
Network slice KPIs contain cell-level performance data, subscriber density, and traffic patterns. Telecom regulators (DoT in India, FCC in the US) restrict how this data moves. Edge processing ensures raw KPIs never leave the operator's network — only aggregated, anonymized metrics go to the cloud for analytics.

### 2. Latency Requirements
- **Edge:** SLA evaluation must happen within the monitoring cycle (30 seconds). Breach prediction needs to run locally — you can't wait for a cloud round-trip when an SLA breach is 5 minutes away.
- **Core DC:** CAMARA QoD session creation, TMF API calls, remediation orchestration — sub-second is fine.
- **Cloud:** Intent parsing (LangChain4j), model retraining, historical analytics — seconds to hours.

### 3. Cost Economics
- The EMA-based breach predictor is lightweight (pure math, no GPU). Runs on every cell site 24/7 — local inference is essentially free.
- CAMARA QoD API calls are per-session (when breach predicted), not per-KPI — manageable cloud or on-prem cost.
- LLM inference for intent parsing is bursty (SLAs defined infrequently). Cloud-hosted, auto-scaled.

### 4. Model Lifecycle
**Train in cloud → Deploy to edge → Monitor centrally.**

1. Cloud: Train time-series prediction models on historical KPI data, backtest EMA parameters
2. Model Registry (Core DC): Versioned prediction configs (EMA alpha, thresholds, rolling windows)
3. Edge: Pull latest prediction model, evaluate SLAs locally, push breach events to core DC
4. If false positive rate increases → automated retraining → updated model pushed to edge

## SLA-Guard Specific Deployment

| Component | Where | Why |
|-----------|-------|-----|
| KPI Ingestion | Edge (gNodeB) | Real-time metrics, can't lose data |
| SLA Evaluator | Edge | Sub-second evaluation, local decision |
| Breach Predictor (EMA) | Edge | Lightweight, runs every 30s per slice |
| CAMARA QoD Integration | Core DC / On-Prem | API gateway to CAMARA platform |
| TMF640/641/622/688 | Core DC | Standard BSS/OSS integration |
| Intent Parser (LangChain4j) | Cloud | LLM inference, not latency-sensitive |
| Dashboard / Alerts | Core DC | NOC/SOC team access |
| Historical Analytics | Cloud | Data lake, batch processing |

## CAMARA QoD: Special Consideration

The CAMARA Quality on Demand API sits between the operator and the device/application. When SLA-Guard predicts a breach:
1. Edge sends breach event to Core DC
2. Core DC creates CAMARA QoD session (API call to CAMARA gateway)
3. QoS boost applied to the affected slice
4. Edge monitors improvement, sends verification back
5. Session expires or is extended based on SLA recovery

This keeps the CAMARA API call in the operator's trusted network while leveraging the telco API ecosystem.

## Scaling Strategy

- **Horizontal at edge:** One SLA monitor per gNodeB or cell cluster
- **Vertical at core:** TMF API gateway scales with slice count
- **Elastic at cloud:** Serverless LLM endpoints for intent parsing
- **CAMARA sessions:** Pool and reuse, rate-limited per operator agreement

## Interview Answer

> "For SLA-Guard, I designed a three-tier architecture: real-time KPI ingestion and SLA evaluation at the edge for sub-10ms decisions, the CAMARA QoD integration and TMF API layer in the operator's core data center for BSS/OSS compliance, and the LLM-powered intent parsing in the cloud for cost efficiency. The key insight is that the breach predictor — an EMA-based time-series model — is lightweight enough to run on every cell site, which means zero cloud dependency for the critical path. Only when a breach is predicted does the system trigger a CAMARA QoD session through the core DC."
