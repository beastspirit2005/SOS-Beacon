# Frontend PRD — Project Beacon (Android Client)
### The on-device app: detection, UI/UX, and mesh participation

**Owner:** Client team (UI/UX — Rishabh · Mesh core — teammate)
**Status:** v2.0 · Hackathon build
**Companion docs:** `Beacon_Complete_PRD.md`, `Beacon_02_Backend_PRD.md`, `Beacon_03_API_Contract.md`

> Tags: **P0** MVP/demo-critical · **P1** strong add · **P2** roadmap. Build P0 first.

---

## 1. Purpose & Scope

The "frontend" is the **Android app running on every phone** — victim, relay, and gateway are the same app in different runtime roles (§4 of the complete PRD). It owns everything that happens on-device with no connectivity: detecting an emergency, deciding whether to send, presenting the experience, packaging + signing the packet, relaying phone-to-phone, and forwarding once a gateway regains signal.

The client is split across two owners who meet only at the **`SosController` seam**:
- **UI/UX (Rishabh)** — the "Beacon" design system and all screens/states; consumes `meshState`/`deliveryState`, renders the live mesh view.
- **Mesh & systems (teammate)** — transport, routing, packet, store-carry-forward, gateway forwarding; the real `SosController`.

---

## 2. Design System — "Beacon" (UI/UX, P0)

Aesthetic: *"a signal in the dark."* Dark-first, near-black canvas (#0A0B0D), surface (#131519), off-white text; two signal accents — **safe/connected teal (#2DE1C2)** and **SOS/ember (#FF5A3C)**, used sparingly (one dominant accent per screen state). Monospace numerals for peer/hop/coordinate readouts; humanist sans for body. Spring-physics motion and a reusable **sonar-pulse** ring motif. Everything subtly alive, never static.

---

## 3. Screens & States (UI/UX, P0 unless noted)

| Screen | Purpose |
|---|---|
| **Home / SOS hero** | Breathing ember SOS button (sonar pulse), calm teal mesh-status line with animated peer count |
| **Sending / cancel window** | Draining countdown ring; **send-unless-cancelled**; accelerating pulse + per-second haptic |
| **Status / in-flight** | Renders `MeshState` — Searching → InFlight (hops rolling); resolves on delivery |
| **Delivered** | Tension→release payoff; hops/peers summary; success haptic |
| **Received Alerts** | Bystander view — SOSs this phone is relaying (metadata only, never payload) |
| **Mesh View** | **The demo money-shot** — live topology; packet pulse hopping origin→gateway |
| **Settings / permissions** | Calm permission-request flow; optional sonar sounds (muted by default) |
| **Volunteer / relay-only** | Relay-only mode, no account *(P2)* |

---

## 4. On-Device Subsystems (mesh & systems, P0 unless noted)

| ID | Requirement | Priority |
|---|---|---|
| DET-1 | Manual SOS trigger + cancel window (send-unless-cancelled) | P0 |
| DET-2 | Confidence engine (sensor + input scoring) | P1 |
| DET-3 | Automatic triggers — fall, scream, dead-man timer, missed check-in | P1 |
| DET-4 | Wearable / vehicle-crash triggers | P2 |
| MSH-1 | Nearby Connections transport (`P2P_CLUSTER`, BLE + Wi-Fi Direct) | P0 |
| MSH-2 | Build + HMAC-sign packet (schema per API contract §2) | P0 |
| MSH-3 | Dedup (seen-cache on `msg_id`) | P0 |
| MSH-4 | Epidemic flooding + TTL hop-limit | P0 |
| MSH-5 | Store-carry-forward (Room outbox; rebroadcast on new peer) | P0 |
| MSH-6 | Gateway detection → forward to backend (API contract §3) | P0 |
| MSH-7 | Smart-relay score (battery, mobility, connectivity, reliability, signal) | P1 |
| MSH-8 | Spray-and-wait routing | P2 |
| MSH-9 | ACK propagation back to origin | P2 |
| SND-1 | Attach last-known GPS to every packet | P0 |
| SND-2 | Direct-SMS fallback (`SmsManager`), fired parallel to mesh | P1 |
| SEC-1 | Payload encryption (relays forward blind) | P1/P2 |
| SVC-1 | Foreground service so relaying survives backgrounding | P1 |
| DEMO-1 | Expose `Flow<MeshTopology>` for the Mesh View | P1 |

---

## 5. The Seam (frozen — both owners build against it)

```kotlin
interface SosController {
    fun trigger(draft: SosDraft)
    val meshState: StateFlow<MeshState>
    val deliveryState: StateFlow<DeliveryState>
}
data class SosDraft(val payload: String, val triggerType: String, val severityHint: String)
```

UI calls `trigger(draft)` and renders the two flows. Mesh side owns everything downstream (`msg_id`, GPS, `sig`, `ttl`, `hops`, priority). UI builds against a `FakeSosController`; mesh builds the real one; they integrate at the seam. **Agree the `MeshTopology` field names before the Mesh View is wired.**

---

## 6. Branch & Repo Rules

- `contract/` (seam + `SosPacket`) lives on **main**, created jointly day 1 — imported, never redefined.
- UI/UX work → branch **`frontend-dev`**. Mesh work → branch **`mesh-core`**. Merge via PRs into main.

---

## 7. Acceptance Criteria (MVP)

- Full UI flow runs on a real device against `FakeSosController` with continuous motion + haptics; every empty/permission state renders; reduced-motion falls back gracefully.
- 3 phones in airplane mode: A→B→C multi-hop, packet arrives once; gateway forwards; delivery reflected in `deliveryState`.
- Mesh View shows a packet pulse traversing the full hop path.

---

## 16. Roadmap

Confidence-driven auto-triggers · smart-relay scoring · spray-and-wait · ACK-back UI · payload encryption · volunteer relay mode · wearable/vehicle triggers · offline maps.

*IIC 3.0 · Project Beacon · v2.0*
