# STAR Method - SLA-Guard

## Opening
Built an autonomous SLA enforcement platform for 5G network slicing that predicts breaches 5-15 minutes ahead and remediates using CAMARA Quality-on-Demand API, enabling telecom operators to maintain service quality with 99.99%+ availability.

## Situation
5G network operators create virtual network slices on shared infrastructure, each with specific SLAs for latency, throughput, jitter, and availability. When SLAs are violated, operators face revenue loss from penalties and customer churn. Manual monitoring and remediation are too slow—SLA breaches are detected after they occur, leading to minutes of downtime that cost hundreds of thousands of dollars in penalties and lost revenue.

## Task
Build a real-time SLA monitoring and autonomous remediation platform using:
- CAMARA Quality on Demand API for session-based QoS boosting
- TMF Open APIs (TMF640, TMF641, TMF622, TMF688) for BSS/OSS integration
- Predictive analytics to forecast breaches 5-15 minutes before they occur
- Intent-based networking to define SLAs in natural language using LangChain4j
- Sub-second SLA evaluation with automated remediation triggers

## Action

### Architecture & Core Components
- **Quarkus 3.15.1** microservice platform with reactive programming
- **Domain Model**: 10 entity classes (NetworkSlice, SLA, SLAMetric, SLABreach, RemediationAction, QoDSession, Intent, etc.)
- **Monitoring Engine**: Scheduled pipeline that evaluates metrics every 30 seconds against SLA thresholds
- **SLA Evaluator**: Classifies metrics as COMPLIANT → WARNING (80%) → CRITICAL (95%) → BREACHED
- **Breach Predictor**: Statistical time-series analysis using Exponential Moving Average (EMA), standard deviation, and linear regression to predict breaches
- **Remediation Engine**: Autonomous decision engine that triggers QoS boosts, slice resizing, traffic reroute, or escalation
- **Health Calculator**: Computes weighted health scores (0-100) per slice

### Predictive Analytics Implementation
- Maintains rolling window of last 100 metrics per slice
- EMA with configurable alpha (default 0.3) for trend detection
- Volatility measurement using standard deviation
- Linear regression for trend projection
- Outputs: breach probability (0-1), estimated time-to-breach (minutes), confidence score, critical metric identification

### CAMARA Integration
- **QoDService**: REST client for CAMARA Quality on Demand API
- **QoS Profiles**: QOS_E (emergency), QOS_S (signaling), QOS_M (medium), QOS_L (low), QOS_B (background)
- **Session Lifecycle**: Create → Monitor → Expire/Revoke with automatic cleanup
- **Profile Selection**: Automatic profile selection based on slice type (EMERGENCY → QOS_E, URLLC → QOS_S, etc.)

### TMF Open API Integration
- **TMF640** (Service Activation): Activate/deactivate/remediate slice services
- **TMF641** (Service Inventory): CRUD for network slices with SLA status
- **TMF622** (Product Ordering): Order slice provisioning
- **TMF688** (Event Management): Publish SLA breach and remediation events

### Intent-Based Networking
- **IntentParser**: LangChain4j with OpenAI GPT-4 for natural language → SLA translation
- **Rule-based Fallback**: Regex-based extraction when AI unavailable
- **IntentEnforcer**: Monitors and enforces intent-derived SLAs with time window and trigger condition support
- **IntentExplainer**: Generates plain-text explanations of enforcement status

### REST API Layer
- 9 REST resources with 40+ endpoints
- Real-time metric ingestion with batch support
- Intent parsing and enforcement endpoints
- Dashboard with aggregate health statistics
- Full CRUD for slices, SLAs, breaches, remediations, QoD sessions

### Testing
- 5 comprehensive test suites: SLAMonitoringEngineTest, BreachPredictorTest, RemediationEngineTest, QoDServiceTest, IntentParserTest, SliceHealthCalculatorTest
- Tests cover compliant/warning/critical/breach scenarios, prediction accuracy, remediation decisions, and intent parsing

## Result
- **Sub-second SLA evaluation** with real-time monitoring every 30 seconds
- **5-15 minute breach prediction window** using statistical time-series analysis
- **Autonomous remediation** via CAMARA QoD API with automatic session management
- **Full TMF Open API compliance** (TMF640, TMF641, TMF622, TMF688)
- **Intent-based SLA definition** using natural language processing
- **Comprehensive REST API** with 40+ endpoints for slice/SLA/metric/breach/remediation management
- **Health scoring** (0-100) with 5 categories: Excellent, Good, Fair, Poor, Critical
- **Production-ready** with Quarkus best practices, CDI, scheduling, and WebSockets support

### Key Skills Demonstrated
- **Quarkus**: Microservices, CDI, scheduler, reactive programming, persistence
- **CAMARA APIs**: Quality on Demand, QoS profiles, session management
- **5G Network Slicing**: Slice types, SLA parameters, isolation levels
- **TMF Open APIs**: Service activation, inventory, ordering, event management
- **Time-Series Analysis**: EMA, trend detection, volatility measurement, breach probability
- **LangChain4j**: Natural language processing, intent parsing, AI integration
- **Intent-Based Networking**: Natural language → SLA translation, enforcement
- **Predictive Analytics**: Statistical analysis, confidence scoring, time-to-breach estimation
- **REST API Design**: Resource-oriented, proper HTTP semantics, pagination, filtering
- **Testing**: Unit tests, integration tests, mock data, edge cases

### Follow-up Questions & Answers

**Q: How does the breach predictor handle different metric types (e.g., latency vs. throughput)?**
A: The predictor uses metric-specific thresholds and directionality. For metrics where higher values are worse (latency, jitter, packet loss), breach occurs when values exceed thresholds. For metrics where lower values are worse (throughput, availability), breach occurs when values drop below thresholds. The EMA trend is compared against thresholds, with distance and volatility factored into probability calculations.

**Q: What happens if CAMARA API is unavailable during a breach?**
A: The system has multiple fallback strategies. If CAMARA API calls fail, it logs the error and attempts alternative remediation: 1) TMF640 service remediation, 2) Escalation to human operators. Mock sessions are created in dev/test mode. The system continues monitoring and will retry CAMARA API for subsequent breaches.

**Q: How does intent parsing handle ambiguous natural language?**
A: IntentParser uses a two-tier approach. First, LangChain4j with GPT-4 provides intelligent parsing. If AI fails or is unavailable, rule-based regex extraction falls back to pattern matching for numbers, units, and keywords. Confidence scores indicate parsing quality, and operators can manually adjust extracted parameters.

**Q: Can the system handle multiple concurrent SLA breaches on the same slice?**
A: Yes. Each metric type (latency, throughput, jitter, packet loss, availability) is evaluated independently. Multiple breaches are recorded separately with their own remediation actions. The overall status is determined by the worst metric (BREACHED > CRITICAL > WARNING > COMPLIANT). Remediation Engine prioritizes QoS boost for the most critical breach.

**Q: How does the system scale for thousands of network slices?**
A: The monitoring engine uses Quarkus reactive programming with non-blocking I/O. Scheduled evaluation runs efficiently by batching database queries. Metrics are cached in memory with 100-point rolling windows. Quarkus's built-in connection pooling and Hibernate optimize database access. Horizontal scaling is supported via container orchestration.

**Q: What data is retained for audit and compliance?**
A: All SLA breaches are permanently recorded with: timestamp, metric type, actual/threshold values, severity, root cause analysis, prediction confidence, and linked remediation action. Remediation actions track initiation/completion times, success/failure, external references (CAMARA session IDs), and rollback instructions. TMF688 events provide audit trail for BSS/OSS systems.

**Q: How does the health scoring algorithm work?**
A: Each metric type gets a 0-100 score: COMPLIANT = 100, WARNING = 75, CRITICAL = 50, BREACHED = 0. The overall health score is the average across all configured SLA metrics. Categories: Excellent (90-100), Good (75-89), Fair (60-74), Poor (40-59), Critical (0-39). Slices without SLAs default to 100 (full health).

**Q: Can operators customize prediction parameters?**
A: Yes. Configuration properties allow tuning: EMA alpha (default 0.3, higher = more responsive), warning threshold (80% of SLA target), critical threshold (95% of SLA target), prediction window (15 minutes), and history size (100 metrics). These can be adjusted per deployment without code changes.

**Q: How does the system integrate with existing OSS/BSS platforms?**
A: TMF Open APIs provide standard integration points. TMF641 (Service Inventory) exposes slice and SLA data. TMF640 (Service Activation) triggers remediation. TMF688 (Event Management) publishes real-time breach/remediation events. REST APIs support webhook callbacks and polling. Event-driven architecture enables integration with monitoring systems, ticketing systems, and analytics platforms.

**Q: What's the deployment architecture?**
A: Built as a Quarkus JAR with embedded Hibernate. Supports PostgreSQL for persistence (configurable). Can run as standalone JAR, Docker container, or Kubernetes deployment. CAMARA and TMF APIs are external dependencies. Horizontal scaling via Kubernetes HPA. Configuration via environment variables or ConfigMap. Supports health checks, metrics (Micrometer), and distributed tracing (OpenTelemetry).
