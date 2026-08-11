# API Contract — Project Beacon 🚨
### Single source of truth · match field names and types EXACTLY

**Status:** v2.0 · Hackathon Build  
**Companion Docs:** [COMPLETE_PRD.md](file:///Users/rana/IIC/docs/COMPLETE_PRD.md), [FRONTEND_PRD.md](file:///Users/rana/IIC/docs/FRONTEND_PRD.md), [BACKEND_PRD.md](file:///Users/rana/IIC/docs/BACKEND_PRD.md)

Two interfaces define the entire communication fabric. Build both sides in parallel against this contract. **One renamed field silently breaks delivery.**

1. **Mesh Packet Contract** — the on-wire packet structure every phone agrees on (client ↔ client).
2. **Cloud REST Contract** — the HTTPS interface a gateway phone uses to reach the backend (client ↔ cloud).

> [!NOTE]
> **v2.0 change:** The packet gains a `priority` field (integer 1–5, evaluated by the Packet Priority Engine). It is **additive** — existing forwarding code keeps working, defaulting to `3` (manual SOS) if the priority engine isn't active. `severity` is retained. The backend language (FastAPI) does not affect this contract.

---

## 1. Conventions

- **Base URL**: `https://<backend-host>/api/v1` (never hardcoded; read from client config).
- **Format**: All REST bodies are JSON; timestamps are epoch **milliseconds** (integer), UTC.
- **Headers**: Every REST request carries `X-Gateway-Id` (the forwarding phone's anonymous ID) and `X-App-Version`.
- **Responses**: Every response carries a `request_id` (UUID) for tracing.

---

## 2. Mesh Packet Contract (client ↔ client)

Field names below **are the contract.** Serialize as compact JSON or Protobuf.

```yaml
SosPacket:
  msg_id:        string   # UUID v4 — unique per SOS; DEDUP KEY
  origin_id:     string   # Anonymised victim device ID
  created_at:    int      # Epoch milliseconds
  lat:           float    # Latitude coordinate
  lon:           float    # Longitude coordinate
  acc:           float    # GPS accuracy radius (meters)
  severity:      string   # "info" | "warn" | "critical"
  priority:      int      # 1..5 (Packet Priority Engine; default 3)
  confidence:    float    # 0.0..1.0 (trigger confidence score)
  trigger_type:  string   # "manual" | "partial" | "fall" | "scream" | "no_motion" | "crash"
  ttl:           int      # Time To Live (remaining hops); decremented each relay; drop at 0
  hops:          int      # Hops traversed so far; incremented each relay
  payload:       string   # Short message / partial text (<=240 chars; may be encrypted)
  sig:           string   # Signature over the packet for authenticity verification
```

**Relay Rules (identical on every node):**
On receive, if `msg_id` is in the local seen-cache $\to$ **drop**; else store, decrement `ttl` (`ttl -= 1`), increment `hops` (`hops += 1`), and rebroadcast if `ttl > 0`. **Never mutate any field except `ttl` and `hops`.** Expire packets older than `MAX_AGE_MS`. Relays MUST forward encrypted payloads without trying to decrypt or read them.

---

## 3. Cloud REST Contract (gateway ↔ cloud)

### 3.1 Ingest distress packet
*   **Path**: `POST /sos/ingest`
*   **Headers**: `X-Gateway-Id` (required), `X-App-Version`
*   **Request Body**:
    ```json
    {
      "packet": {
        "msg_id": "8482bf4f-2dfb-47e2-8822-e421cd7be060",
        "origin_id": "victim-8a9d",
        "created_at": 1786278715269,
        "lat": 28.6139,
        "lon": 77.2090,
        "acc": 5.0,
        "severity": "critical",
        "priority": 5,
        "confidence": 0.95,
        "trigger_type": "manual",
        "ttl": 3,
        "hops": 2,
        "payload": "SOS! Heavy earthquake damage, multiple trapped in Sector 4.",
        "sig": "35a402cb0dde61ad12cd5c20519c0848b661e646b3aa12b6723e07d5609e125c"
      },
      "received_at": 1786278716700
    }
    ```
*   **Response (200 OK)**:
    ```json
    {
      "sos_id": "sos-2cfb8a",
      "msg_id": "8482bf4f-2dfb-47e2-8822-e421cd7be060",
      "status": "accepted",
      "priority": "high",
      "escalation": "responders",
      "request_id": "f83a6b5c-d0b2-4d2v-b780-ff74a23a45b7"
    }
    ```
> Missing `X-Gateway-Id` $\to$ `422 Unprocessable Entity` · bad/absent `sig` $\to$ `401 BAD_SIGNATURE` · malformed packet $\to$ `422 INVALID_PACKET` · duplicate `msg_id` $\to$ `200 status: "duplicate"` (idempotent, does not trigger duplicate alerts).

### 3.2 Check Ingestion Status
*   **Path**: `GET /sos/{sos_id}/status`
*   **Response (200 OK)**:
    ```json
    {
      "sos_id": "sos-2cfb8a",
      "delivery": "acknowledged",
      "notified_at": 1786278728464,
      "request_id": "a4268f7f-a426-8f7f-a426-8f7fa4268f7f"
    }
    ```

### 3.3 Acknowledge Distress Packet (Responders Command)
*   **Path**: `POST /sos/{sos_id}/ack`
*   **Request Body**:
    ```json
    {
      "responder_id": "officer-3cfb"
    }
    ```
*   **Response (200 OK)**:
    ```json
    {
      "sos_id": "sos-2cfb8a",
      "delivery": "acknowledged",
      "request_id": "f74ce15f-f74c-e15f-f74c-e15fe4370f96"
    }
    ```

### 3.4 Liveness / Keepalive
*   **Path**: `GET /health`
*   **Response (200 OK)**:
    ```json
    {
      "status": "ok",
      "notifier": "connected",
      "version": "2.0.0"
    }
    ```

### 3.5 Telemetry Stream
*   **Path**: `WS /dashboard`
*   **Description**: Read-only WebSocket stream emitting anonymized incident events for dispatcher maps.

---

## 4. Error Envelope (All REST Errors)

All REST error responses conform to the following JSON structure:

```json
{
  "error": {
    "code": "INVALID_PACKET",
    "message": "Schema/field validation failed",
    "request_id": "b68e4f9e-837a-e280-7920-7ee9d0bca1c4"
  }
}
```

| Code | HTTP | Meaning |
| :--- | :--- | :--- |
| `INVALID_PACKET` | 422 | Schema/field validation failed |
| `BAD_SIGNATURE` | 401 | Cryptographic signature verification failed |
| `MISSING_GATEWAY_ID` | 422 | `X-Gateway-Id` header absent |
| `RATE_LIMITED` | 429 | Too many packets from a specific gateway/origin |
| `PACKET_EXPIRED` | 410 | Packet creation timestamp is older than `MAX_AGE_MS` |
| `INTERNAL` | 500 | Unhandled server-side exception |

---

## 5. Identity Context

- **`origin_id`** (Packet): The victim's anonymous device ID — generated once on setup, persisted, and never regenerated.
- **`X-Gateway-Id`** (Header): The forwarding phone's anonymous ID — can differ from `origin_id`. The same SOS packet can be forwarded by multiple gateways (hence the node deduplicates on `msg_id` instead of gateway).

---

## 6. Failure Modes (Things That Break Delivery)

1. **Renamed Packet Field** (`created_at` vs `timestamp`): Ingest fails or Pydantic validation rejects the packet.
2. **Gateway-Based Dedup**: Deduplicating based on the gateway ID instead of `msg_id` will drop relays forwarded by other paths.
3. **Signature Mismatch**: Ensure signature canonicalization logic is identical on both the client (signer) and backend (verifier).
4. **Mutating Fields in Relay**: Mutating anything other than `ttl` and `hops` in-transit invalidates the packet signature.
