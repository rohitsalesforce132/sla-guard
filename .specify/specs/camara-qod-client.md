# SLA-Guard: CAMARA QoD Client Specification

## Overview

Generate a production-ready Java/Quarkus client for the CAMARA Quality on Demand (QoD) API v0.9.0 from the official OpenAPI specification at `specs/camara-qod-api.yaml`.

This client integrates with SLA-Guard's existing remediation engine to autonomously create QoD sessions when SLA breaches are predicted.

## Source Specification

- **File:** `specs/camara-qod-api.yaml`
- **Version:** CAMARA QoD API v0.9.0
- **Format:** OpenAPI 3.0.3
- **Base URL:** `{apiRoot}/qod/v0`
- **Auth:** OAuth 2.0 (client credentials)

## What to Generate

### 1. Java Data Models (from OpenAPI schemas)

Generate Java records/classes for ALL schemas in the spec:

- **CreateSession** — Request to create a QoD session
  - device: { phoneNumber, ipv4Address, ipv6Address, networkAccessIdentifier } (at least one required)
  - applicationServer: { ipv4Address, ipv6Address }
  - applicationServerPorts: { ranges: [{ from, to }], ports: [int] }
  - devicePorts: { ranges: [{ from, to }], ports: [int] }
  - qosProfile: string (QOS_E, QOS_S, QOS_M, QOS_L, QOS_B)
  - duration: int (seconds, optional)
  - notificationUrl: string (optional)
  - notificationAuthToken: string (optional)

- **SessionInfo** — Response with session details
  - sessionId: string (UUID)
  - device, applicationServer, applicationServerPorts, devicePorts (echoed from request)
  - qosProfile: string
  - deviceIpv4Addr, deviceIpv6Addr (resolved addresses)
  - startedAt: integer (epoch)
  - expiresAt: integer (epoch)
  - qosStatus: string (REQUESTED, AVAILABLE, UNAVAILABLE, AVAILABLE_STOPPED)
  - notificationUrl, notificationAuthToken (echoed)

- **QosStatusInfo** — Status change notification
  - sessionId: string
  - qosStatus: string
  - statusInfo: string (additional context)

- **ErrorInfo** — Error response (400, 401, 403, 404, 409, 422, 429, 500)
  - status: int
  - code: string
  - message: string

- **Device** — Device identifier (polymorphic: phone, IPv4, IPv6, NAI)

- **Port** — Single port number

- **PortRange** — Port range (from-to)

- **PortsSpec** — Combination of ports and ranges

### 2. REST Client Interface

```java
@RegisterRestClient(configKey = "camara-qod")
@Path("/qod/v0")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CamaraQoDClient {

    @POST
    @Path("/sessions")
    SessionInfo createSession(CreateSession request);

    @GET
    @Path("/sessions/{sessionId}")
    SessionInfo getSession(@PathParam("sessionId") String sessionId);

    @DELETE
    @Path("/sessions/{sessionId}")
    void deleteSession(@PathParam("sessionId") String sessionId);

    @GET
    @Path("/sessions")
    List<SessionInfo> listSessions(
        @QueryParam("device.phoneNumber") String phoneNumber,
        @QueryParam("device.ipv4Address.publicAddress") String ipv4,
        @QueryParam("device.ipv6Address.publicAddress") String ipv6
    );

    @GET
    @Path("/qos-profiles")
    List<QoSProfile> listQoSProfiles(@QueryParam("name") String name);

    @GET
    @Path("/qos-profiles/{name}")
    QoSProfile getQoSProfile(@PathParam("name") String name);
}
```

### 3. Session Manager Service

```java
@ApplicationScoped
public class CamaraQoDSessionManager {
    // Business logic wrapping the REST client
    SessionInfo createQoDSession(NetworkSlice slice, SLA sla, RemediationAction action);
    void monitorAndExtend(String sessionId, int additionalSeconds);
    void terminateSession(String sessionId);
    QosStatusInfo handleStatusNotification(QosStatusInfo notification);
}
```

### 4. Conformance Tests

Generate tests that validate:
- CreateSession with all device identifier types
- Session lifecycle (create → get → delete)
- QoS profile listing
- Error handling for each HTTP status code
- Notification callback handling
- Duration validation (max 86400 seconds)
- Port range validation

### 5. WireMock Stubs

Generate WireMock stub mappings for all endpoints so tests run without a live CAMARA API:
- `__files/camara/create-session-response.json`
- `__files/camara/get-session-response.json`
- `__files/camara/qos-profiles-response.json`
- `mappings/camara-create-session.json`
- `mappings/camara-get-session.json`
- `mappings/camara-delete-session.json`
- `mappings/camara-list-sessions.json`
- `mappings/camara-qos-profiles.json`
- `mappings/camara-errors.json`

## Configuration

```properties
# CAMARA QoD API Configuration
camara-qod.mp-rest/url=https://api.example.com/qod/v0
camara-qod.mp-rest/auth/oauth/client-id=${CAMARA_CLIENT_ID}
camara-qod.mp-rest/auth/oauth/client-secret=${CAMARA_CLIENT_SECRET}
camara-qod.mp-rest/auth/oauth/token-uri=https://auth.example.com/oauth2/token

# Session defaults
camara.qod.default-duration=3600
camara.qod.max-retries=3
camara.qod.retry-delay-ms=1000
camara.qod.notification-url=https://sla-guard.example.com/api/v1/qod/callback
```

## Integration with SLA-Guard

The generated client integrates with the existing `RemediationEngine`:
1. SLA breach predicted → `RemediationEngine` determines QoS boost needed
2. Maps SLA breach type to CAMARA QoS profile:
   - Latency breach → QOS_E (emergency, lowest latency)
   - Throughput breach → QOS_M or QOS_L (medium/low latency)
   - Availability breach → QOS_S (signaling priority)
3. `CamaraQoDSessionManager.createQoDSession()` creates the session
4. Session monitored until SLA restored → auto-terminate

## Success Criteria

- [ ] All OpenAPI schemas mapped to Java classes with correct types
- [ ] REST client interface matches all CAMARA QoD endpoints
- [ ] Conformance tests pass with WireMock stubs
- [ ] Session manager integrates with existing RemediationEngine
- [ ] Configuration externalized (no hardcoded URLs)
- [ ] Zero employer or company references
