# Backend PRD — Project Beacon (Cloud Command Node)
### Ingest · dedup · enrich · escalate · notify · dashboard

**Owner:** Backend Lead (teammate)
**Status:** v2.0 · Hackathon build
**Companion docs:** `Beacon_Complete_PRD.md`, `Beacon_01_Frontend_PRD.md`, `Beacon_03_API_Contract.md`

> Tags: **P0** MVP/demo-critical · **P1** strong add · **P2** roadmap.

---

## 1. Purpose & Scope

The backend is the **Command Node** — the cloud egress and intelligence layer that exists only where connectivity is present. It does everything the offline phone deliberately cannot: receive forwarded packets, deduplicate across gateways, enrich with live context, decide escalation, notify responders, and power the live disaster dashboard.

**Architectural spine (state it to judges):** all live-data, weather, and AI logic lives here — never on the victim's offline phone. The phone stays dumb and robust; the cloud is smart.

---

## 2. Responsibilities

| ID | Requirement | Priority |
|---|---|---|
| BKD-1 | `POST /sos/ingest`; **dedup on `msg_id`** across multiple gateways | P0 |
| BKD-2 | Notify contacts/responders with location link via SMS | P0 |
| BKD-3 | Return delivery status / ACK for propagation back through the mesh | P1 |
| BKD-4 | Live-context enrichment (weather / disaster / area risk) to refine severity | P1 |
| BKD-5 | AI interpretation of partial text; priority prediction | P1 |
| BKD-6 | **Tiered escalation** — low → contacts, high → responders. Logic may only raise urgency, never cancel | P1 |
| BKD-7 | Signature verification + replay protection + rate limiting | P1 |
| BKD-8 | **Live disaster dashboard** (WebSocket) fusing anonymous observations into a map | P2 |
| BKD-9 | Incident clustering / disaster-severity prediction (AI) | P2 |
| BKD-10 | Responder dispatch integration | P2 |

---

## 3. Fail-Safe Principle (non-negotiable)

Ambiguity resolves **toward sending/escalating**. A false positive is a wasted "check on them" message; a false negative is a death — asymmetric cost. Enrichment and AI only ever *raise* priority or add context; **no code path suppresses an alert.**

---

## 4. Processing Pipeline

```
POST /sos/ingest (from a gateway phone)
  → validate + verify signature
  → dedup on msg_id  ──(seen)──► return status: duplicate
  → enrich: weather / risk / time            (P1)
  → interpret partial text (AI)              (P1)
  → compute priority = f(level, confidence, context)
  → tier:  low → notify contacts   |   high → escalate to responders
  → send SMS + location link
  → record status for ACK-back               (P1)
  → publish to disaster dashboard            (P2)
```

---

## 5. Data & Storage

- **Dedup store:** `msg_id` → first-seen record (Redis for speed at scale; in-memory acceptable for MVP).
- **SOS records:** packet + gateway metadata + computed priority + notification status (PostgreSQL; SQLite acceptable for MVP).
- **Observations (P2):** anonymous signals feeding the disaster map.
- Payload may arrive **encrypted**; backend handles routing metadata + decrypts only where authorized.

---

## 6. Technology Stack

| Concern | Choice | Notes |
|---|---|---|
| API | **FastAPI** | Async, fast to build a webhook-style ingest |
| DB | **PostgreSQL** | SOS records + analytics *(SQLite ok for MVP)* |
| Cache/dedup | **Redis** | Fast cross-gateway dedup *(in-memory ok for MVP)* |
| Realtime | **WebSockets** | Live dashboard *(P2)* |
| Push | **Firebase Cloud Messaging** | Responder/app notifications |
| SMS | Cloud SMS API (MSG91 / Fast2SMS for India; Twilio fastest for demo) | Reaches non-smartphone responders |
| AI | **Gemini API** | Text interpretation, prioritization, clustering *(P1–P2)* |
| Deploy | **Docker** | Reproducible; **pre-warm before demo** to avoid cold-start |

---

## 7. Non-Functional

- **No cold-start mid-demo** — free tiers sleep; expose a keepalive/health ping and warm it before presenting.
- **Idempotency** — duplicate `msg_id` returns `duplicate`, never a second alert.
- **Security** — verify `sig`; per-origin rate-limit; reject malformed/expired packets with the documented error envelope.
- **The API contract is language-agnostic** — building on FastAPI vs. anything else does not change field names or endpoints; the Android forwarder is unaffected.

---

## 8. Acceptance Criteria (MVP)

- A single valid `POST /sos/ingest` fires exactly one real SMS with a working location link.
- The same `msg_id` posted twice → one alert, second returns `duplicate`.
- `GET /health` reports notifier connectivity and serves as keepalive.
- Malformed / bad-signature / oversized packets return the documented codes.

---

## 16. Roadmap

Live disaster dashboard (WebSocket) · AI clustering + severity prediction · responder dispatch integration · ACK-back · payload decryption pipeline · analytics.

*IIC 3.0 · Project Beacon · v2.0*
