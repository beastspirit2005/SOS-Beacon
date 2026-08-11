# API Contract — Project Beacon
### Single source of truth · match field names and types EXACTLY

**Status:** v2.0 · Hackathon build
**Companion docs:** `Beacon_Complete_PRD.md`, `Beacon_01_Frontend_PRD.md`, `Beacon_02_Backend_PRD.md`

Two interfaces. Build both sides in parallel against this. **One renamed field silently breaks delivery.**
1. **Mesh Packet Contract** — the on-wire packet every phone agrees on (client ↔ client).
2. **Cloud REST Contract** — the HTTPS interface a gateway phone uses to reach the backend (client ↔ cloud).

> **v2.0 change:** the packet gains a `priority` field (int 1–5, the Packet Priority Engine). It is **additive** — existing forwarding code keeps working; default `3` (manual SOS) if the priority engine (P1) isn't built yet. `severity` is retained. The backend language (FastAPI or other) does **not** affect this contract.

---

## 1. Conventions

- Base URL: `https://<backend-host>/api/v1` (never hardcoded; read from config).
- JSON bodies; timestamps are epoch **milliseconds** (int), UTC.
- Every REST request carries `X-Gateway-Id` (the forwarding phone's anonymous id) and `X-App-Version`.
- Every response carries `request_id` (uuid).

---

## 2. Mesh Packet Contract (client ↔ client)

Field names below **are the contract.** Serialize as compact JSON or Protobuf.

```
SosPacket:
  msg_id:        string   # uuid v4 — unique per SOS; DEDUP KEY
  origin_id:     string   # anonymised victim device id
  created_at:    int      # epoch ms
  lat:           float
  lon:           float
  acc:           float    # gps accuracy (m)
  severity:      string   # "info" | "warn" | "critical"
  priority:      int       # 1..5 (Packet Priority Engine; default 3)  [v2.0, additive]
  confidence:    float    # 0.0..1.0 (trigger confidence)
  trigger_type:  string   # "manual"|"partial"|"fall"|"scream"|"no_motion"|"missed_checkin"|"crash"
  ttl:           int      # remaining hops; decremented each relay; drop at 0
  hops:          int      # hops so far; incremented each relay
  payload:       string   # short message / partial text (<=240 chars; may be ENCRYPTED)
  sig:           string   # signature over the packet for authenticity
```

**Relay rules (identical on every node):** on receive, if `msg_id` in seen-cache → drop; else store, `ttl-=1`, `hops+=1`, rebroadcast if `ttl>0`. **Never mutate any field except `ttl` and `hops`.** Expire packets older than `MAX_AGE_MS`. Bandwidth favors higher `priority` when contended *(P1)*. Relays MUST forward encrypted `payload` without reading it.

---

## 3. Cloud REST Contract (gateway ↔ cloud)

### 3.1 `POST /sos/ingest`
**Headers:** `X-Gateway-Id` (required), `X-App-Version`
```
IngestRequest:  { packet: SosPacket, received_at: int }
```
**200 →**
```
IngestResult:
  sos_id:      string
  msg_id:      string       # echoed
  status:      string       # "accepted" | "duplicate"
  priority:    string       # "low" | "medium" | "high" (backend-computed dispatch tier)
  escalation:  string       # "contacts" | "responders"
  request_id:  string
```
Missing `X-Gateway-Id` → 422 · bad/absent `sig` → 401 `BAD_SIGNATURE` · malformed → 422 `INVALID_PACKET` · duplicate `msg_id` → 200 `status:"duplicate"` (idempotent, no second alert).

### 3.2 `GET /sos/{sos_id}/status`  *(P1 — ACK-back)*
`{ sos_id, delivery: "pending"|"notified"|"acknowledged", notified_at:int|null, request_id }`

### 3.3 `POST /sos/{sos_id}/ack`  *(P1)*
Body `{ "responder_id": string }` → `DeliveryStatus` with `delivery:"acknowledged"`.

### 3.4 `GET /health`
`{ "status":"ok", "notifier":"connected"|"down", "version":string }` — also the **keepalive** to prevent cold-start.

### 3.5 `WS /dashboard`  *(P2)*
WebSocket stream of anonymised incident/observation events for the live disaster map. Read-only; no PII.

---

## 4. Error Envelope (all REST errors)

`{ "error": { "code": string, "message": string, "request_id": string } }`

| Code | HTTP | Meaning |
|---|---|---|
| `INVALID_PACKET` | 422 | Schema/field validation failed |
| `BAD_SIGNATURE` | 401 | `sig` verification failed |
| `MISSING_GATEWAY_ID` | 422 | `X-Gateway-Id` absent |
| `RATE_LIMITED` | 429 | Too many packets from gateway/origin |
| `PACKET_EXPIRED` | 410 | Older than `MAX_AGE_MS` |
| `INTERNAL` | 500 | Unhandled server error |

Client mapping: 429 → back off; 5xx / `notifier:"down"` → retry once after ~4s; 401/422 → log, don't crash the relay path.

---

## 5. Identity

- **`origin_id`** (packet): the victim's anonymous device id — generated once, persisted, never regenerated.
- **`X-Gateway-Id`** (header): the *forwarding* phone's anonymous id — may differ from `origin_id`; the same SOS may be forwarded by several gateways (hence dedup on `msg_id`, not gateway).

---

## 6. Mocking Strategy (parallel build)

- **Backend ships first:** a mock `POST /sos/ingest` returning a canned `IngestResult` so the client builds forwarding + ACK before enrichment/notification is real.
- **Client ships:** a mock `SosPacket` generator so the backend tests ingest/dedup/notify without the physical mesh.
- Swap mocks for real implementations **without changing this contract.**

---

## 7. The Things That Silently Break Delivery

1. A renamed packet field (`created_at` vs `timestamp`) → dedup/enrichment misfires.
2. Dedup keyed on gateway instead of `msg_id` → duplicate or dropped alerts.
3. Backend cold-start mid-demo → first real SOS times out. **Warm it.**
4. Signature canonicalization mismatch between client signer and backend verifier → every packet 401s. Agree the exact HMAC byte-input (or stub identically) on day 1.
5. Forgetting `priority` is additive → don't reorder/rename existing fields when adding it.

*IIC 3.0 · Project Beacon · v2.0 · frozen interface*
