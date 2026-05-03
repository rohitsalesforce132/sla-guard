# SLA-Guard: CAMARA QoD Client Constitution

## Project Principles

1. **Spec-Driven:** All CAMARA API clients are generated from official OpenAPI specs, never hand-written
2. **Type-Safe:** Use Java records/Pydantic models that match the CAMARA spec exactly
3. **Resilient:** Every API call has retry logic with exponential backoff
4. **Observable:** Every CAMARA API interaction is logged with correlation IDs
5. **No Vendor Lock-In:** CAMARA client works with any operator's QoD endpoint
6. **Test First:** Generate conformance tests from the OpenAPI spec

## Coding Standards

- Java 17+ with Quarkus 3.x
- Use `@RegisterRestClient` for CAMARA API clients
- All models must match CAMARA OpenAPI spec field names (camelCase)
- Use `Optional<>` for nullable CAMARA fields
- API errors wrapped in domain-specific exceptions (CAMARA QoD Session Exception)
- Health checks for CAMARA API connectivity

## Testing Standards

- Unit tests with mocked CAMARA responses (from spec examples)
- Conformance tests that validate request/response against OpenAPI schema
- Integration tests with WireMock for CAMARA API simulation

## Architecture Constraints

- CAMARA client is a separate CDI bean, injectable everywhere
- Configuration via Quarkus config (not hardcoded)
- QoS profile labels must match CAMARA spec exactly (QOS_E, QOS_S, QOS_M, QOS_L, QOS_B)
- Session lifecycle managed by QoDSessionManager (create → monitor → delete)
