# SLA-Guard: Autonomous SLA Enforcement Platform for 5G Network Slicing

## Overview

SLA-Guard is a production-ready Quarkus microservice platform that monitors network slice SLAs in real-time, predicts breaches 5-15 minutes ahead using statistical time-series analysis, and autonomously remediates using CAMARA Quality-on-Demand API and TMF Open APIs.

### Key Features

- ✅ **Real-time SLA Monitoring**: Sub-second evaluation every 30 seconds
- 🔮 **Predictive Breach Detection**: 5-15 minute warning window using EMA trend analysis
- 🤖 **Autonomous Remediation**: CAMARA QoD API integration for automatic QoS boosting
- 🌐 **TMF Open API Compliance**: Full support for TMF640, TMF641, TMF622, TMF688
- 💬 **Intent-Based Networking**: Define SLAs in natural language using LangChain4j
- 📊 **Health Scoring**: 0-100 health scores with 5-tier classification
- 🚀 **Production-Ready**: Quarkus 3.x, reactive programming, comprehensive testing

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         SLA-Guard                               │
├─────────────────────────────────────────────────────────────────┤
│  REST API Layer (9 Resources, 40+ Endpoints)                   │
├─────────────────────────────────────────────────────────────────┤
│  Intent-Based Networking (LangChain4j + Rule-Based)            │
├─────────────────────────────────────────────────────────────────┤
│  Monitoring Engine (Scheduled, 30s intervals)                  │
│  ├── SLA Evaluator (COMPLIANT → WARNING → CRITICAL → BREACHED) │
│  ├── Breach Predictor (EMA, Trend Analysis, Probability)       │
│  ├── Remediation Engine (QoS Boost, Escalation)                │
│  └── Health Calculator (0-100 Score)                           │
├─────────────────────────────────────────────────────────────────┤
│  External Integrations                                          │
│  ├── CAMARA QoD API (Session-Based QoS Boosting)               │
│  └── TMF Open APIs (640, 641, 622, 688)                        │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 14+ (or use H2 for development)

### Installation

```bash
# Clone the repository
git clone https://github.com/your-org/sla-guard.git
cd sla-guard

# Configure database
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit database connection settings

# Build the project
mvn clean package

# Run the application
java -jar target/sla-guard-1.0.0-SNAPSHOT-runner.jar
```

### Development Mode

```bash
mvn quarkus:dev
```

Access OpenAPI UI at: `http://localhost:8080/q/swagger-ui`

## Configuration

### Application Properties

```properties
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/slaguard

# Monitoring
sla-guard.monitoring.interval-seconds=30
sla-guard.monitoring.history-size=100

# Prediction
sla-guard.prediction.ema-alpha=0.3
sla-guard.prediction.warning-threshold=0.8
sla-guard.prediction.critical-threshold=0.95
sla-guard.prediction.prediction-window-minutes=15

# CAMARA API
sla-guard.camara.base-url=http://localhost:9090/camara
sla-guard.camara.api-key=your-camara-api-key

# Intent AI (optional)
sla-guard.intent.enabled=true
sla-guard.intent.model=gpt-4
sla-guard.intent.api-key=your-openai-api-key
```

## API Usage

### 1. Create a Network Slice

```bash
curl -X POST http://localhost:8080/api/v1/slices \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise Slice",
    "description": "Corporate network slice",
    "sliceType": "ENTERPRISE",
    "status": "ACTIVE",
    "deviceId": "device-001",
    "deviceIpAddress": "10.0.1.100",
    "applicationServerIp": "10.0.2.50"
  }'
```

### 2. Define an SLA

```bash
curl -X POST http://localhost:8080/api/v1/slices/SLICE-001/sla \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise SLA",
    "latencyTargetMs": 10.0,
    "throughputTargetMbps": 100.0,
    "availabilityTargetPercent": 99.99
  }'
```

### 3. Ingest Metrics

```bash
curl -X POST http://localhost:8080/api/v1/slices/SLICE-001/metrics \
  -H "Content-Type: application/json" \
  -d '{
    "metricType": "LATENCY",
    "value": 8.5,
    "unit": "ms"
  }'
```

### 4. Parse Intent (Natural Language)

```bash
curl -X POST http://localhost:8080/api/v1/intents/parse \
  -H "Content-Type: application/json" \
  -d '{
    "intent": "Ensure my enterprise slice maintains latency below 5ms with 99.99% availability"
  }'
```

### 5. Get Dashboard Summary

```bash
curl http://localhost:8080/api/v1/dashboard
```

### 6. Create QoD Session

```bash
curl -X POST http://localhost:8080/api/v1/qod/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sliceId": "SLICE-001",
    "qosProfile": "QOS_S",
    "durationMinutes": 15
  }'
```

## Monitoring and Prediction

### SLA Status Classification

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

### Health Scoring

- **Excellent** (90-100): All metrics compliant
- **Good** (75-89): Minor warnings
- **Fair** (60-74): Some critical metrics
- **Poor** (40-59): Breaches detected
- **Critical** (0-39): Multiple severe breaches

## CAMARA QoS Profiles

| Profile | Priority | Use Case |
|---------|----------|----------|
| QOS_E | 5 (Highest) | Emergency services |
| QOS_S | 4 | Signaling / Control plane |
| QOS_M | 3 | Premium services |
| QOS_L | 2 | Standard services |
| QOS_B | 1 (Lowest) | Background traffic |

## Slice Types

- **EMBB**: Enhanced Mobile Broadband (video, streaming)
- **URLLC**: Ultra-Reliable Low Latency Communications (control, critical apps)
- **MMTC**: Massive Machine Type Communications (IoT, sensors)
- **ENTERPRISE**: Enterprise Private Network
- **IOT**: Internet of Things
- **EMERGENCY**: Emergency Services

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SLAMonitoringEngineTest

# Run with coverage
mvn test jacoco:report
```

## Deployment

### Docker

```bash
# Build Docker image
docker build -t sla-guard:1.0.0 .

# Run container
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://db:5432/slaguard \
  -e QUARKUS_DATASOURCE_USERNAME=postgres \
  -e QUARKUS_DATASOURCE_PASSWORD=postgres \
  sla-guard:1.0.0
```

### Kubernetes

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/

# Check deployment
kubectl get pods -l app=sla-guard
```

## Documentation

- [STAR Method Summary](STAR.md) - Project achievements and interview preparation
- [GitHub Copilot Instructions](.github/copilot-instructions.md) - Development guidelines
- [QA Bank](docs/QA-BANK.md) - 50+ interview questions and answers
- [OpenAPI Documentation](http://localhost:8080/q/swagger-ui) - Interactive API docs

## Technology Stack

- **Framework**: Quarkus 3.15.1
- **Language**: Java 21
- **Database**: PostgreSQL with Hibernate ORM
- **REST**: JAX-RS with RESTEasy
- **AI/ML**: LangChain4j 0.36.2, Statistical time-series analysis
- **Standards**: CAMARA QoD API, TMF Open APIs (640, 641, 622, 688)
- **Testing**: JUnit 5, Quarkus Test
- **Build**: Maven

## License

MIT License - See LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues and questions, please open an issue on GitHub or contact the maintainers.

---

**Built with ❤️ for 5G Network Operators**
