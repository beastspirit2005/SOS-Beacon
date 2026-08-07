# Frontend PRD (My Scope) — Mesh & Systems Core
### Offline Mesh SOS Relay · Android client · Rishabh's half

**Owner:** Rishabh (Client — Mesh & Systems Core)
**Status:** Draft v1.0 · Hackathon build
**Attach to Antigravity with:** `03_API_Contract.md` (field names are binding)
**Companion docs:** `01_Frontend_PRD.md` (full client), `02_Backend_PRD.md` (teammate)

---

## 0. Read this first (branch & context rules for Antigravity)

- The `contract/` package (`SosController`, `SosDraft`, `MeshState`, `DeliveryState`, `SosPacket`) is the **frozen seam**. It is created as a **joint day-1 commit to `main`** — do NOT redefine it on my branch; import it.
- All my implementation work happens on branch **`feat/mesh-core`**. Every task commits and pushes there. Never commit to `main` after the day-1 seam.
- Field names in `SosPacket` and the REST body must match `03_API_Contract.md` **exactly**. One renamed field silently breaks delivery at the backend.
- I own the mesh; my teammate owns the UI. We only meet at the `SosController` seam. I never touch `ui/`, `triggers/`, or `sensors/`.

---

## 1. Scope

I build everything the SOS does **after** a trigger fires, up to and including handing it to the cloud: packaging, signing, relaying phone-to-phone offline, holding it when no peer is near, and forwarding it once a phone regains connectivity. My teammate calls `trigger(draft)`; I make the message travel.

**In scope (my half):** packet build + HMAC signing · Nearby Connections transport · epidemic routing (dedup + TTL) · store-carry-forward (Room) · gateway detection + REST forward · the real `MeshSosController` · direct-SMS fallback · hardening + a topology stream for the demo view.

**Out of scope (teammate):** all screens, the SOS button, cancel-window UI, sensor/auto triggers, permissions UI polish, the demo topology *rendering* (I expose the data; they draw it).

---

## 2. My packages

| Package | Owns |
|---|---|
| `contract/` | *(shared, on main)* the seam types — I import, don't edit |
| `mesh/` | transport, packet factory, signer, router, seen-cache |
| `net/` | gateway forwarder (REST), direct-SMS fallback |
| `data/` | Room outbox + persistent seen-cache |

---

## 3. The seam I must implement

My deliverable is a working `MeshSosController : SosController`:

```
interface SosController {
    fun trigger(draft: SosDraft)                 // teammate calls this
    val meshState: StateFlow<MeshState>          // I emit; they render
    val deliveryState: StateFlow<DeliveryState>  // I emit; they render
}
```

`trigger(draft)` → fetch location → build packet → sign → originate into mesh + write to outbox + fire SMS fallback in parallel. I own every field downstream (`msg_id`, GPS, `sig`, `ttl`, `hops`). The teammate never sees a packet.

---

## 4. Functional requirements (my half)

| ID | Requirement | Priority |
|---|---|---|
| MSH-1 | Nearby Connections transport (`P2P_CLUSTER`, BLE + Wi-Fi Direct), raw bytes only | P0 |
| MSH-2 | Build packet (stable `origin_id`, `ttl=6`, fields per contract) + HMAC-SHA256 sign/verify | P0 |
| MSH-3 | Seen-cache dedup on `msg_id` (in-memory + persisted) | P0 |
| MSH-4 | Epidemic flooding: relay unseen packets, `ttl--`, `hops++`, drop at `ttl=0`; mutate only `ttl`/`hops` | P0 |
| MSH-5 | Store-carry-forward: persist to Room outbox; rebroadcast to newly-connected peers | P0 |
| MSH-6 | Gateway detection → `POST /sos/ingest` per contract; handle `accepted`/`duplicate`; map error envelope | P0 |
| CTRL-1 | `MeshSosController` wiring meshState/deliveryState end-to-end | P0 |
| SND-2 | Direct-SMS fallback via `SmsManager`, fired in parallel with mesh | P1 |
| HARD-1 | Rate-limit rebroadcasts, expire old packets, structured `MESH` logging | P1 |
| DEMO-1 | Expose read-only `Flow<MeshTopology>` (endpoints + hop path) for teammate's demo view | P1 |
| SVC-1 | Foreground-service host so relaying survives backgrounding (teammate's triggers attach later) | P1 |

---

## 5. Interfaces I depend on (must agree with teammate)

- **HMAC secret + canonicalization** — the exact byte input to the signature. My signer and the backend verifier must be byte-identical or every packet returns `401 BAD_SIGNATURE`. Document the canonical form in a comment.
- **Mock backend URL** — the teammate's mock `/sos/ingest` returning a canned `IngestResult`, so I build forwarding before the real backend exists.

---

## 6. Acceptance criteria (my half is "done" when)

- 3 phones in airplane mode (BT + Wi-Fi on, cellular off): A—B—C, packet from A reaches C **exactly once**; seen-cache prevents loops; `ttl=0` not rebroadcast.
- A packet originated with no peers **persists across app restart**, then auto-rebroadcasts when a peer appears.
- A gateway phone forwards the packet to the backend; a duplicate `msg_id` returns `duplicate` and does **not** double-forward.
- Tapping my debug button drives `deliveryState` Pending → Notified and `meshState` through peer/hop changes.

---

## 7. Build order (my Antigravity prompt sequence)

1. Branch + (join main for) contract scaffold  2. Nearby Connections transport  3. Packet build + sign  4. Routing (dedup/TTL/flooding)  5. Store-carry-forward (Room)  6. Gateway forward (REST)  7. Real `MeshSosController`  8. Direct-SMS fallback  9. Hardening + topology stream + PR.

Each prompt ends with a verify step and a commit + push to `feat/mesh-core`.

---

*IIC 3.0 · Open Innovation · pairs with `03_API_Contract.md`*
