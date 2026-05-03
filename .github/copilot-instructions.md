# SLA-Guard - GitHub Copilot Instructions

## Project Overview
SLA-Guard is an autonomous SLA enforcement platform for 5G network slicing. It monitors network slice SLAs in real-time, predicts breaches 5-15 minutes ahead using statistical time-series analysis, and autonomously remediates using CAMARA Quality-on-Demand API and TMF Open APIs.

## Architecture

### Core Components
1. **Domain Model** (`src/main/java/com/slaguard/model/`):
   - `NetworkSlice`: 5G network slice with SLA parameters
   - `SLA`: SLA definition with targets, warning, and critical thresholds
   - `SLAMetric`: Real-time metric measurements with evaluation status
   - `SLABreach`: Recorded breach events with root cause analysis
   - `RemediationAction`: Actions taken to fix breaches (QOS_BOOST, SLICE_RESIZE, TRAFFIC_REROUTE, ESCALATION)
   - `QoDSession`: CAMARA Quality on Demand sessions
   - `Intent`: Natural language intent definitions

2. **Monitoring Engine** (`src/main/java/com/slaguard/engine/`):
   - `SLAMonitoringEngine`: Scheduled pipeline evaluating all active slices every 30 seconds
   - `SLAEvaluator`: Evaluates metrics against SLA thresholds (COMPLIANT → WARNING → CRITICAL → BREACHED)
   - `BreachPredictor`: Predicts breaches using EMA trend analysis
   - `RemediationEngine`: Decides and executes remediation actions
   - `SliceHealthCalculator`: Computes health scores (0-100) per slice

3. **Prediction Service** (`src/main/java/com/slaguard/prediction/`):
   - `TimeSeriesAnalyzer`: Statistical analysis (EMA, standard deviation, linear regression)
   - `BreachPredictionService`: Orchestrates breach prediction
   - `PredictionModel`: Configurable prediction parameters

4. **CAMARA Integration** (`src/main/java/com/slaguard/camara/`):
   - `QoDService`: REST client for CAMARA QoD API
   - `QoDSessionManager`: Session lifecycle management
   - `QoSProfileManager`: QoS profile selection (QOS_E, QOS_S, QOS_M, QOS_L, QOS_B)
   - `CamaraModels`: CAMARA API data models

5. **TMF Integration** (`src/main/java/com/slaguard/tmf/`):
   - `TMF640Resource`: Service Activation API
   - `TMF641Resource`: Service Inventory API
   - `TMF622Resource`: Product Ordering API
   - `TMF688Resource`: Event Management API

6. **Intent-Based Networking** (`src/main/java/com/slaguard/intent/`):
   - `IntentParser`: Natural language → SLA translation (LangChain4j + rule-based fallback)
   - `IntentEnforcer`: Monitors and enforces intent-derived SLAs
   - `IntentExplainer`: Plain-text explanations of enforcement status

7. **REST API** (`src/main/java/com/slaguard/resource/`):
   - `SliceResource`: CRUD for network slices
   - `SLAResource`: SLA management
   - `MetricsResource`: Metric ingestion and querying
   - `BreachResource`: Breach history
   - `RemediationResource`: Remediation actions
   - `IntentResource`: Intent parsing and enforcement
   - `DashboardResource`: Aggregate dashboard data
   - `QoDResource`: CAMARA QoD session management

## Key Concepts

### SLA Evaluation
- **COMPLIANT**: Metric within target
- **WARNING**: Metric at 80% of threshold
- **CRITICAL**: Metric at 95% of threshold
- **BREACHED**: Metric exceeds threshold

### Prediction Algorithm
1. Maintain 100-point rolling window of metrics
2. Calculate Exponential Moving Average (EMA) with α=0.3
3. Compute trend using linear regression on EMA values
4. Measure volatility via standard deviation
5. Estimate breach probability based on: distance to threshold, trend direction, volatility
6. Calculate time-to-breach: (threshold - current) / |trend|

### QoS Profiles
- **QOS_E** (Emergency): Highest priority for emergency services
- **QOS_S** (Signaling): Very high priority for control plane traffic
- **QOS_M** (Medium): High priority for premium services
- **QOS_L** (Low): Medium priority for standard services
- **QOS_B** (Background): Best effort for non-critical traffic

### Slice Types
- **EMBB**: Enhanced Mobile Broadband (video, streaming)
- **URLLC**: Ultra-Reliable Low Latency Communications (control, critical apps)
- **MMTC**: Massive Machine Type Communications (IoT, sensors)
- **ENTERPRISE**: Enterprise Private Network
- **IOT**: Internet of Things
- **EMERGENCY**: Emergency Services

## Code Conventions

### Package Structure
- `com.slaguard.model`: Domain entities (JPA @Entity)
- `com.slaguard.engine`: Core monitoring and evaluation logic
- `com.slaguard.prediction`: Predictive analytics
- `com.slaguard.camara`: CAMARA API integration
- `com.slaguard.tmf`: TMF Open API resources
- `com.slaguard.intent`: Intent-based networking
- `com.slaguard.resource`: REST API endpoints
- `com.slaguard.test`: Unit and integration tests

### Naming Conventions
- Entities: PascalCase (e.g., `NetworkSlice`, `SLAMetric`)
- Services: PascalCase ending with `Service` or `Engine` (e.g., `SLAMonitoringEngine`, `QoDService`)
- Resources: PascalCase ending with `Resource` (e.g., `SliceResource`, `MetricsResource`)
- REST endpoints: kebab-case in paths (e.g., `/api/v1/slices/{id}/metrics`)
- Database columns: camelCase (JPA @Column)
- Constants: UPPER_SNAKE_CASE

### Quarkus Patterns
- Use `@ApplicationScoped` for singleton services
- Use `@Inject` for CDI dependency injection
- Use `@Scheduled` for periodic tasks
- Use `@Transactional` for database write operations
- Use PanacheEntity for JPA entities with built-in CRUD
- Return Response objects for REST APIs with proper status codes

### Logging
- Use `io.quarkus.logging.Log` (not SLF4J)
- Log levels: DEBUG for detailed flow, INFO for significant events, WARN for recoverable issues, ERROR for failures
- Include context in log messages (e.g., sliceId, metricType, breachId)

### Error Handling
- Return 404 for missing resources
- Return 400 for validation errors
- Return 500 for unexpected errors
- Include descriptive error messages in JSON responses
- Log full stack traces for errors

## Configuration

### Application Properties
```properties
# Monitoring
sla-guard.monitoring.interval-seconds=30
sla-guard.monitoring.history-size=100

# Prediction
sla-guard.prediction.ema-alpha=0.3
sla-guard.prediction.warning-threshold=0.8
sla-guard.prediction.critical-threshold=0.95
sla-guard.prediction.prediction-window-minutes=15

# CAMARA
sla-guard.camara.base-url=http://localhost:9090/camara
sla-guard.camara.api-key=test-key

# TMF
sla-guard.tmf.base-url=http://localhost:8080/tmf-api

# Intent
sla-guard.intent.enabled=true
sla-guard.intent.model=gpt-4
sla-guard.intent.api-key=your-openai-api-key
```

## Testing Guidelines

### Test Structure
- Use `@QuarkusTest` for integration tests
- Test both happy paths and edge cases
- Test with realistic data (latency in ms, throughput in Mbps)
- Verify status classifications (COMPLIANT, WARNING, CRITICAL, BREACHED)
- Test prediction accuracy with trend data

### Test Scenarios
1. **SLA Evaluation**: Compliant, warning, critical, breach scenarios
2. **Prediction**: Stable trends, increasing trends, imminent breaches
3. **Remediation**: QoS boost, escalation, fallback strategies
4. **CAMARA Integration**: Session creation, status checks, revocation
5. **Intent Parsing**: Simple intents, complex intents, rule-based fallback
6. **Health Scoring**: Perfect health, mixed metrics, no metrics

## API Examples

### Create a Network Slice
```bash
POST /api/v1/slices
{
  "name": "Enterprise Slice",
  "description": "Corporate network slice",
  "sliceType": "ENTERPRISE",
  "status": "ACTIVE",
  "deviceId": "device-001",
  "deviceIpAddress": "10.0.1.100",
  "applicationServerIp": "10.0.2.50"
}
```

### Create an SLA
```bash
POST /api/v1/slices/{sliceId}/sla
{
  "name": "Enterprise SLA",
  "latencyTargetMs": 10.0,
  "throughputTargetMbps": 100.0,
  "availabilityTargetPercent": 99.99
}
```

### Ingest Metrics
```bash
POST /api/v1/slices/{sliceId}/metrics
{
  "metricType": "LATENCY",
  "value": 8.5,
  "unit": "ms"
}
```

### Parse Intent
```bash
POST /api/v1/intents/parse
{
  "intent": "Ensure my enterprise slice maintains latency below 5ms with 99.99% availability"
}
```

### Get Dashboard
```bash
GET /api/v1/dashboard
```

### Create QoD Session
```bash
POST /api/v1/qod/sessions
{
  "sliceId": "SLICE-001",
  "qosProfile": "QOS_S",
  "durationMinutes": 15
}
```

## Dependencies
- Quarkus 3.15.1
- Hibernate ORM with Panache
- PostgreSQL driver
- LangChain4j 0.36.2 (for AI-powered intent parsing)
- RESTEasy JAX-RS
- Jackson JSON
- SmallRye OpenAPI
- Vert.x
- Quarkus Scheduler
- Quarkus WebSockets

## Development Workflow
1. Make changes to code
2. Run `mvn quarkus:dev` for hot reload
3. Access OpenAPI UI at `http://localhost:8080/q/swagger-ui`
4. Run tests: `mvn test`
5. Build: `mvn package`
6. Run JAR: `java -jar target/sla-guard-1.0.0-SNAPSHOT-runner.jar`

## Important Notes
- No company names in code or docs (use "telecom operator", "enterprise", "carrier")
- All code must compile and pass tests
- Use Quarkus best practices (CDI, config, health checks)
- CAMARA API models must follow official CAMARA QoD spec
- TMF APIs must follow standard TMF Open API patterns
- Intent parsing has rule-based fallback if AI is unavailable
- Mock sessions created if CAMARA API is not reachable
- Database schema auto-generated via Hibernate (hbm2ddl=update)
