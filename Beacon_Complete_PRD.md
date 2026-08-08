# Product Requirements Document — Project Beacon

**Project:** Project Beacon — a self-organizing emergency communication infrastructure
**Supersedes:** `mesh-sos-relay-PRD.md` (v1.0). This is **v2.0**, expanded from a single SOS app into a disaster-communication *platform*.
**Event:** IIC 3.0 — International Innovation Challenge · Open Innovation track
**Round 1 deadline:** 11 August 2026 · **Status:** v2.0 (build spec)

> **Scope discipline (read first).** The *vision* below is a platform. The *build* for Round 1 is the MVP core (§13) — the same reliable offline relay you're already building. Every requirement is tagged **P0** (MVP / demo-critical) · **P1** (strong add if time allows) · **P2** (roadmap / Phase 2–3). Do **not** attempt P2 before 11 Aug. Documenting the full platform while shipping a tight core is itself a maturity signal judges reward.

---

## 1. Vision

Project Beacon turns ordinary smartphones into a **decentralized emergency communication network** that keeps working after traditional networks fail. Instead of depending on telecom towers, every phone becomes part of a temporary emergency mesh that relays distress messages, coordinates responders, and builds a live situational picture.

Designed to function during: earthquakes, floods, landslides, forest fires, stampedes, terror attacks, network shutdowns, remote expeditions, and rural emergencies.

**Core contrast:**
- *Traditional:* Phone → Tower → Internet → Server — a single point of failure.
- *Beacon:* Phone ↔ Phone ↔ Phone → any connected phone → Cloud → Emergency Services — **infrastructure becomes optional.**

**One-line pitch:** *Project Beacon is a decentralized emergency communication infrastructure that turns everyday smartphones into a self-organizing disaster-response network, enabling life-saving communication even when traditional infrastructure has completely failed.*

---

## 2. Design Principles

1. **Offline-first** — everything critical works with no internet, cellular data, Wi-Fi router, or towers.
2. **Self-organizing** — no central controller; each phone independently decides whom to relay, when, whether to become a gateway, and which packet deserves priority.
3. **Fail-safe** — the system never waits for perfect certainty. When unsure → **send**. (AI and logic may *escalate*, never *suppress*.)
4. **Human-centric** — battery, bandwidth, storage, and privacy are all respected.

---

## 3. The Four Pillars

**Pillar 1 — Emergency Detection.** Triggers: manual SOS *(P0)*; fall, impact, scream detection, dead-man timer, missed check-in *(P1)*; wearable and vehicle-crash integration *(P2)*. Output: a signed **Emergency Packet**.

**Pillar 2 — Intelligent Mesh Network.** Phones automatically become relay / store-and-forward / gateway / observer nodes over BLE, Wi-Fi Direct, and Nearby Connections, using delay-tolerant multi-hop routing with automatic gateway election *(core P0; smart election P1)*.

**Pillar 3 — Emergency Intelligence.** The network understands more than single packets — multiple victims, congestion, density, gateway health, packet priority, disaster spread — and adapts *(P2, backend-side)*.

**Pillar 4 — Response Layer.** Any node that gains connectivity automatically uploads the SOS, sends SMS, calls the backend, and notifies contacts, responders, and the disaster dashboard *(upload + SMS P0; dashboard P1/P2)*.

---

## 4. Network Roles

| Role | Responsibility | Priority |
|---|---|---|
| **Victim node** | Create SOS, store evidence, broadcast packet, await ACK | P0 |
| **Relay node** | Receive, deduplicate, update TTL, rebroadcast | P0 |
| **Smart relay** | Additionally compute a **relay score** (battery, mobility, connectivity likelihood, reliability, signal) to make better forwarding decisions | P1 |
| **Gateway node** | First phone reaching internet — upload pending packets, receive ACK, return delivery status | P0 |
| **Command node (backend)** | Deduplication, AI analysis, weather enrichment, risk estimation, responder dispatch, analytics | P0 core / P1–P2 intelligence |

---

## 5. Routing Engine

Rather than blind flooding, routing combines: **epidemic routing** (fast spread, P0) · **store-carry-forward** (disconnected regions, P0) · **spray-and-wait** (battery efficiency, P1) · **smart relay selection** (better forwarding, P1). Result: fast, reliable, scalable propagation with bounded overhead.

---

## 6. Packet Priority Engine *(P1)*

Emergency packets are not equal; bandwidth automatically favors critical packets.

| Level | Meaning |
|---|---|
| **5** | Mass casualty · multiple victims · children · critical medical |
| **4** | Unconscious · crash · heavy fall |
| **3** | Manual SOS |
| **2** | Location update |
| **1** | Network maintenance |

*MVP note:* the packet carries a `priority` field; for P0 it may default to 3 (manual SOS). Level assignment logic is P1.

---

## 7. Collective Intelligence *(P2)*

Every phone contributes anonymous observations — repeated screams, multiple falls, many SOSs, rapid crowd movement, high relay density, gateway disappearance. The backend fuses these into a **live disaster map**.

---

## 8. Volunteer Mode *(P2)*

Anyone can install Beacon and, **without creating an account**, let their phone act as a relay-only node — dramatically expanding mesh coverage during a disaster.

---

## 9. Security

Every packet carries: message ID, digital signature, timestamp, TTL, hop count, encrypted payload, replay protection, authentication, rate limiting. **Relay phones cannot read the encrypted emergency content** — they forward blind. *(Signature + replay + rate-limit: P0/P1. Payload encryption: P1/P2.)*

---

## 10. AI Responsibilities

**AI never decides whether an SOS is sent.** AI *interprets* partial text, *prioritizes* emergencies, *summarizes* situations, *clusters* incidents, *predicts* disaster severity, and *recommends* responders. **AI can escalate; AI cannot suppress.** All AI runs at the backend, after gateway upload — never on the offline victim phone. *(P1–P2.)*

---

## 11. System Architecture

```
Emergency Detection
   → Confidence Engine
   → Packet Generator
   → Mesh Routing Engine
   → Relay Intelligence
   → Gateway Election
   → Cloud Backend
   → Emergency Dashboard
        ├── SMS
        ├── Contacts
        ├── Responders
        └── Disaster Analytics
```

**The load-bearing boundary:** everything from Detection through Gateway Election runs **offline on the phones**; the Cloud Backend and everything after it run **only where connectivity exists**. Live data, AI, and enrichment live exclusively on the online side — never on the victim's offline phone. *(This is also the answer to the judge question "if your phone can hit an API, why do you need a mesh?")*

---

## 12. Technology Stack

| Layer | Stack |
|---|---|
| **Mobile** | Android (Kotlin) · Nearby Connections · BLE · Wi-Fi Direct · Room DB · Foreground Services · Google Location Services |
| **Backend** | FastAPI · PostgreSQL · Redis · Docker · WebSockets · Firebase Cloud Messaging *(MVP may run FastAPI + lightweight storage; Postgres/Redis/WebSocket dashboard are P1/P2)* |
| **AI** | Gemini API · offline lightweight models (future) · disaster classification · emergency text analysis · priority prediction |
| **Mapping** | OpenStreetMap · offline cached maps · risk-layer cache *(P2)* |

*The backend language does not change the API contract — see `Beacon_03_API_Contract.md`. Frontend forwarding code is unaffected.*

---

## 13. MVP Scope (Hackathon — build this, only this)

Demonstrate, end to end: **manual SOS · three-device mesh · multi-hop relay · store-and-forward · gateway upload · SMS notification · live dashboard.** Keep AI limited to *enrichment after gateway upload* so the offline core stays rock-solid.

**The demo money shot:** three phones in airplane mode (BT + Wi-Fi on, cellular off), A→B→C multi-hop, C regains connectivity → forwards → a real SMS reaches a responder with location, and the dashboard updates live.

---

## 14. Future Roadmap

**Phase 2:** adaptive routing · offline maps · wearables · ACK propagation · volunteer relay mode · packet priority engine · payload encryption.
**Phase 3:** government emergency integration · drone relay nodes · vehicle-to-vehicle communication · satellite gateway support · cross-platform (Android + iOS).

---

## 15. Risks & Judge-Defense (carried from v1.0)

| Risk / likely question | Answer |
|---|---|
| "Isn't this just Bluetooth chat?" | Multi-hop + store-carry-forward + gateway egress is the novel, hard part — plain BLE chat has none of it |
| "If a phone can hit an API, why a mesh?" | Live data lives only at the backend; the offline phone never calls an API |
| "What if the AI is wrong?" | Fail-safe: AI only escalates, never suppresses; ambiguity resolves toward sending |
| Multi-hop reliability is fiddly | MVP scoped to 3 devices + a rehearsed, scripted demo |
| Backend cold-start mid-demo | Pre-warm the backend before presenting |
| Scope overreach before deadline | Everything beyond §13 is explicitly P1/P2 roadmap |

---

## 16. Companion Docs

`Beacon_01_Frontend_PRD.md` · `Beacon_02_Backend_PRD.md` · `Beacon_03_API_Contract.md`

*IIC 3.0 · Open Innovation · Project Beacon · v2.0*
