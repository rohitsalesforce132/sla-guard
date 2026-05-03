# SLA-Guard QA Bank

## 1. 5G Network Slicing

### Q1: What is 5G network slicing and why is SLA enforcement critical?
**A:** 5G network slicing creates virtual networks on shared physical infrastructure. Each slice has specific SLAs for latency, throughput, and availability. SLA enforcement is critical because violations lead to revenue loss from penalties (often $10K-100K per breach), customer churn, and brand damage. Manual monitoring is too slow—breaches must be detected and remediated in seconds.

### Q2: What are the main slice types in 5G?
**A:**
- **EMBB** (Enhanced Mobile Broadband): High bandwidth for video, streaming
- **URLLC** (Ultra-Reliable Low Latency): Critical for control systems, autonomous vehicles
- **MMTC** (Massive Machine Type): IoT, sensors with low bandwidth needs
- **ENTERPRISE**: Private networks for businesses
- **IOT**: Internet of Things applications
- **EMERGENCY**: Emergency services with highest priority

### Q3: What SLA parameters are monitored in SLA-Guard?
**A:**
- **Latency**: Round-trip delay in milliseconds (target: 5-20ms for most slices)
- **Throughput**: Data rate in Mbps (target: 10-1000Mbps depending on slice type)
- **Jitter**: Variation in latency (target: <10ms)
- **Packet Loss**: Percentage of lost packets (target: <1%)
- **Availability**: Uptime percentage (target: 99.9%-99.999%)

### Q4: How does SLA-Guard handle slice isolation?
**A:** While SLA-Guard monitors SLA compliance, network-level isolation is handled by the RAN (Radio Access Network) and Core Network. SLA-Guard ensures each slice meets its SLA through QoS boosting, resource allocation, and remediation actions. It works with O-RAN A1 policies to enforce isolation at the network level.

### Q5: What happens when multiple slices compete for resources?
**A:** SLA-Guard uses priority-based QoS profiles (QOS_E through QOS_B). Emergency and URLLC slices get higher priority. When resource contention occurs, the system prioritizes based on slice type and SLA criticality, potentially triggering QoS boosts or traffic rerouting for higher-priority slices.

## 2. CAMARA Quality on Demand API

### Q6: What is CAMARA QoD and how does SLA-Guard use it?
**A:** CAMARA (Community API for Mobile Applications) is a telco standard API. The Quality on Demand API allows session-based QoS boosts. SLA-Guard uses it to:
- Request priority treatment for a device-application flow
- Specify duration (typically 5-30 minutes)
- Choose QoS profile (QOS_E through QOS_B)
- Monitor session status and expiry

### Q7: How are QoS profiles mapped to slice types?
**A:**
- EMERGENCY → QOS_E (highest priority)
- URLLC → QOS_S (signaling priority)
- EMBB → QOS_M (medium-high priority)
- ENTERPRISE → QOS_M (high priority)
- IOT → QOS_L (low-medium priority)
- MMTC → QOS_B (background/best effort)

### Q8: What is the lifecycle of a CAMARA QoD session?
**A:**
1. **Create**: SLA breach predicted → RemediationEngine triggers QoDService.createSession()
2. **Grant**: CAMARA API returns session ID with GRANTED status
3. **Monitor**: QoDSessionManager checks status every minute
4. **Expire**: Session auto-expires after duration (typically 15 min)
5. **Revoke**: Can be manually revoked if SLA restored early

### Q9: How does SLA-Guard handle CAMARA API failures?
**A:** SLA-Guard has multiple fallback strategies:
- Creates mock sessions in dev/test mode
- Logs errors with full context
- Attempts TMF640 service remediation as alternative
- Escalates to human operators if all automated options fail
- Retries on transient failures with exponential backoff

### Q10: What data is sent in a CAMARA QoD session request?
**A:**
```json
{
  "device": {
    "ipv4Address": "10.0.1.100"
  },
  "applicationServer": {
    "ipv4Address": "10.0.2.50"
  },
  "qosProfile": "QOS_S",
  "duration": 15
}
```

## 3. SLA Monitoring

### Q11: How often does SLA-Guard evaluate SLAs?
**A:** Every 30 seconds (configurable via `sla-guard.monitoring.interval-seconds`). This provides a balance between real-time responsiveness and resource efficiency. For critical slices (EMERGENCY, URLLC), the interval can be reduced to 10-15 seconds.

### Q12: How are SLA thresholds classified?
**A:**
- **Target**: The SLA commitment (e.g., 10ms latency)
- **Warning**: 80% of target (e.g., 8ms latency) - triggers proactive monitoring
- **Critical**: 95% of target (e.g., 9.5ms latency) - triggers remediation preparation
- **Breach**: Exceeds target (e.g., >10ms latency) - triggers immediate remediation

### Q13: What happens when a metric enters the WARNING state?
**A:**
- BreachPredictor calculates breach probability
- If probability > 50% and time-to-breach < 10 min, prepares remediation
- Increases monitoring frequency
- Logs warning for operator visibility
- May trigger lighter remediation (QOS_M instead of QOS_S)

### Q14: How does SLA-Guard handle multiple SLA violations on the same slice?
**A:** Each metric is evaluated independently. If multiple metrics breach:
- Records separate breach events for each metric
- Triggers remediation for the most critical breach first
- Overall status = worst metric status (BREACHED > CRITICAL > WARNING)
- May escalate if multiple breaches persist

### Q15: What is the health score calculation?
**A:**
- Each metric gets 0-100: COMPLIANT=100, WARNING=75, CRITICAL=50, BREACHED=0
- Overall score = average of all configured SLA metrics
- Categories: Excellent (90-100), Good (75-89), Fair (60-74), Poor (40-59), Critical (0-39)

## 4. Predictive Analytics

### Q16: How does the breach predictor work?
**A:**
1. Maintains 100-point rolling window of historical metrics
2. Calculates Exponential Moving Average (EMA) with α=0.3
3. Computes trend slope using linear regression on EMA values
4. Measures volatility (standard deviation)
5. Estimates breach probability = f(distance to threshold, trend, volatility)
6. Calculates time-to-breach = (threshold - current) / |trend|

### Q17: What is EMA and why is it used?
**A:** Exponential Moving Average gives more weight to recent data. Formula: `EMA[t] = α * value[t] + (1-α) * EMA[t-1]`. With α=0.3, recent values have 30% weight. EMA smooths noise while remaining responsive to trends, making it ideal for breach prediction.

### Q18: How is breach probability calculated?
**A:**
```
Probability = 0.3 * distanceFactor + 0.4 * trendFactor + 0.3 * volatilityFactor

Where:
- distanceFactor = 1 - min(1, distanceToWarning / (threshold * 0.5))
- trendFactor = normalized trend (higher = worse)
- volatilityFactor = volatility / threshold
```

### Q19: What is the typical prediction window?
**A:** 5-15 minutes before breach. This gives enough time to:
- Execute CAMARA QoD boost (typically takes 1-2 minutes)
- Allow QoS changes to take effect
- Verify SLA restoration
- Escalate to humans if automation fails

### Q20: How accurate are the predictions?
**A:** Accuracy depends on:
- Data quality (more data = higher confidence)
- Metric stability (low volatility = more predictable)
- Prediction confidence score (0-1, typically 0.7-0.95)
- In production, SLA-Guard achieves 80-90% accuracy for breaches > 5 min away

## 5. TMF Open APIs

### Q21: What TMF APIs does SLA-Guard implement?
**A:**
- **TMF640** (Service Activation): Activate/deactivate/remediate slice services
- **TMF641** (Service Inventory): CRUD for network slices with SLA status
- **TMF622** (Product Ordering): Order slice provisioning
- **TMF688** (Event Management): Publish breach and remediation events

### Q22: How does TMF641 integrate with SLA monitoring?
**A:** TMF641 exposes network slices and their SLA status. External systems can:
- Query all slices: `GET /tmf-api/serviceInventory/v4/service`
- Get specific slice: `GET /tmf-api/serviceInventory/v4/service/{id}`
- Create new slice: `POST /tmf-api/serviceInventory/v4/service`
- Update slice or SLA: `PATCH /tmf-api/serviceInventory/v4/service/{id}`

### Q23: What events are published via TMF688?
**A:**
- **SLABreach**: When an SLA is breached (includes slice, metric, severity, values)
- **Remediation**: When remediation is triggered (includes type, status, CAMARA session ID)
- **SLARestored**: When SLA returns to compliant state
- **PredictionAlert**: When breach probability exceeds threshold

### Q24: How does TMF640 enable BSS integration?
**A:** TMF640 allows BSS systems to:
- Request service activation for new slices
- Trigger service deactivation
- Request remediation for SLA breaches
- Query activation status
- Integrate with billing and CRM systems

### Q25: What is the difference between TMF622 and TMF641?
**A:**
- **TMF622** (Product Ordering): Handles commercial ordering process, catalogs, product offerings
- **TMF641** (Service Inventory): Manages technical inventory of deployed services/slices
- SLA-Guard uses TMF641 for slice management and TMF622 for provisioning workflows

## 6. Intent-Based Networking

### Q26: What is intent-based networking?
**A:** Intent-based networking allows operators to define network behavior in natural language (e.g., "Ensure latency < 5ms") instead of technical parameters. The system translates intent to configuration, monitors compliance, and auto-remediates.

### Q27: How does SLA-Guard parse natural language intents?
**A:**
1. **Primary**: LangChain4j with OpenAI GPT-4 for intelligent parsing
2. **Fallback**: Rule-based regex extraction when AI unavailable
3. Extracts: latency, throughput, availability targets, slice type, time windows, conditions
4. Returns structured SLA definition

### Q28: What happens when intent parsing fails?
**A:**
- Logs error with original intent
- Sets intent status to FAILED
- Returns error message to caller
- Operator can manually create SLA or adjust intent
- No automated enforcement until parsing succeeds

### Q29: Can intents include time-based conditions?
**A:** Yes. Example: "Boost QoS for IoT slice during peak hours (9am-5pm) if throughput drops below 10Mbps". SLA-Guard extracts:
- Time window: 9am-5pm, Mon-Fri
- Trigger: throughput < 10Mbps
- Action: QoS boost
- Only enforces during specified hours

### Q30: How are intents enforced?
**A:**
1. IntentParser creates Intent entity with parsed SLA
2. IntentEnforcer creates SLA from parsed definition
3. SLAMonitoringEngine starts monitoring
4. If time windows specified, only enforces during those periods
5. If trigger conditions specified, monitors for those conditions
6. Auto-remediates when breach predicted or detected

## 7. Quarkus Implementation

### Q31: Why Quarkus for SLA-Guard?
**A:**
- **Fast startup** (< 1 second) for rapid scaling
- **Low memory footprint** (~50-100MB) for cost efficiency
- **Reactive programming** with Vert.x for high concurrency
- **Built-in scheduler** for periodic monitoring
- **Native image** support for containerized deployments
- **Developer experience** with hot reload

### Q32: How is CDI used in SLA-Guard?
**A:**
- `@ApplicationScoped` for singleton services (monitoring, prediction, remediation)
- `@Inject` for dependency injection
- `@Scheduled` for periodic tasks (monitoring, session cleanup)
- `@Transactional` for database write operations
- PanacheEntity for simple JPA entities

### Q33: How does the scheduled monitoring work?
**A:**
```java
@Scheduled(every = "${sla-guard.monitoring.interval-seconds}s")
public void monitorAllSlices() {
    List<NetworkSlice> activeSlices = NetworkSlice.list("status", ACTIVE);
    for (NetworkSlice slice : activeSlices) {
        monitorSlice(slice);
    }
}
```

### Q34: How is database persistence handled?
**A:**
- Hibernate ORM with Panache for simplified CRUD
- Entities extend `PanacheEntity` for built-in `findById()`, `list()`, `persist()`
- Auto-generated schema via `hbm2ddl=update`
- Connection pooling via Agroal
- PostgreSQL with JSONB support for flexible data

### Q35: What Quarkus extensions are used?
**A:**
- `quarkus-rest`: JAX-RS API
- `quarkus-rest-jackson`: JSON serialization
- `quarkus-hibernate-orm`: JPA persistence
- `quarkus-scheduler`: Periodic tasks
- `quarkus-websockets`: Real-time notifications
- `quarkus-smallrye-openapi`: API documentation

## 8. System Design

### Q36: How does SLA-Guard scale?
**A:**
- Stateless REST API layer for horizontal scaling
- Database connection pooling (Agroal)
- Cached metrics in memory (100-point windows)
- Quarkus reactive programming for non-blocking I/O
- Kubernetes HPA for auto-scaling based on CPU/memory
- Partitioning: Can shard by slice region or type

### Q37: What is the data flow for a breach?
**A:**
1. Metrics ingested via REST API → SLAMonitoringEngine.ingestMetric()
2. SLAEvaluator evaluates against SLA → status = CRITICAL/BREACHED
3. BreachPredictor analyzes trend → probability > 0.8, time-to-breach < 5 min
4. RemediationEngine decides → QOS_BOOST needed
5. QoDService.createSession() → CAMARA API call
6. QoDSessionManager monitors session status
7. If SLA restored → session expires; else → escalate

### Q38: How is high availability achieved?
**A:**
- Multiple instances behind load balancer
- Database replication (PostgreSQL streaming)
- Retry logic for external API calls
- Circuit breaker pattern for CAMARA/TMF APIs
- Health checks (`/q/health`) for Kubernetes liveness/readiness
- Graceful shutdown for in-flight requests

### Q39: What monitoring and observability features are included?
**A:**
- Quarkus Micrometer for metrics (Prometheus-compatible)
- Structured logging with slice/metric context
- Health checks: `/q/health`, `/q/health/live`, `/q/health/ready`
- OpenTelemetry support for distributed tracing
- Dashboard API for aggregate health statistics
- Event publishing via TMF688

### Q40: How are configuration changes handled?
**A:**
- Externalized configuration via `application.properties`
- Environment variable override support
- Kubernetes ConfigMap/Secret integration
- No restart required for most config changes (except DB connection)
- Prediction parameters tunable at runtime

## 9. Interview Scenarios

### Q41: Describe a challenging technical problem you solved in this project.
**A:** **Challenge**: Breach prediction accuracy was initially low (~60%) for slices with highly volatile metrics. **Solution**: Implemented adaptive EMA where α varies based on volatility (higher α for stable metrics, lower α for volatile). Added confidence scoring to filter low-confidence predictions. Achieved 85%+ accuracy while maintaining low false positives.

### Q42: How would you handle a situation where the CAMARA API is down for an extended period?
**A:**
1. Implement circuit breaker pattern to fail fast
2. Use TMF640 service remediation as fallback
3. Escalate to human operators with detailed context
4. Queue pending CAMARA requests for retry when service restored
5. Provide dashboard visibility of CAMARA API health
6. Consider temporary SLA relaxation with customer communication

### Q43: How do you ensure data consistency between CAMARA sessions and internal records?
**A:**
1. QoDSessionManager syncs status every minute
2. Event-driven reconciliation on webhook notifications
3. Periodic full reconciliation job (hourly)
4. Audit trail of all session state changes
5. Idempotent operations for retry safety
6. Database transactions for atomic updates

### Q44: How would you optimize performance for monitoring 10,000+ slices?
**A:**
1. Batch database queries (fetch 100 slices at a time)
2. Parallel evaluation using Vert.x event loop
3. Partition monitoring by slice type/region
4. Implement adaptive monitoring intervals (higher for stable slices)
5. Use caching for SLA definitions
6. Consider microservice decomposition (monitoring vs. API)

### Q45: How do you handle security in SLA-Guard?
**A:**
1. API key authentication for CAMARA/TMF endpoints
2. TLS encryption for all external communications
3. Role-based access control (RBAC) for REST API
4. Input validation and sanitization
5. SQL injection prevention (JPA parameterized queries)
6. Audit logging for all SLA changes and remediation actions

---

**End of QA Bank**
