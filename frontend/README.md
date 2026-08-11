# Project Beacon — Frontend Web Client 📱

The frontend of Project Beacon is a premium, client-side web application designed to simulate and manage an **Offline Mesh P2P SOS Relay network**. It consists of glassmorphic, micro-animated dashboards tailored for distinct user personas (Citizens, Field Responders, and National Admins).

---

## 🏛️ Project Pages

The application is split into four primary interfaces, served dynamically by the FastAPI backend:

### 1. System Overview (`/landing.html`)
*   **Purpose**: The central starting portal.
*   **Features**:
    *   Interactive system-wide flow charts showing the lifecycle of a packet (Citizen $\to$ Mesh Node $\to$ Edge Gateway $\to$ Cloud Node $\to$ Responder Dispatch).
    *   Personas directory redirecting users to specific portals.
    *   Interactive mesh connection status monitors.

### 2. Citizen SOS (`/victim.html`)
*   **Purpose**: Simulates an offline citizen broadcast station.
*   **Features**:
    *   One-tap emergency broadcast engine.
    *   Mock GPS coordinate generators with accuracy telemetry.
    *   Interactive logging console showing local caching, peer discovery, and hop counts.
    *   Real-time packet transmission lifecycle state indicators (Offline, Relayed, Ingested).

### 3. Officer Command (`/officer.html`)
*   **Purpose**: A tactical operation dashboard for emergency field responders.
*   **Features**:
    *   Priority-sorted incoming incident queues.
    *   Real-time Leaflet map displaying active GPS location pins of distress packets.
    *   Groq AI triage summaries, victim estimation, and categories extraction.
    *   One-tap response assignment controls (Dispatched, En Route, Resolved).
    *   Responsive, dark-mode design layout optimized for field tablets.

### 4. Administrative Dashboard (`/admin.html`)
*   **Purpose**: National telemetry overview.
*   **Features**:
    *   High-level telemetry stats (active edge gateways, total packets, response rates).
    *   Mass Casualty Incident (MCI) cluster detection alerts.
    *   Node logs stream displaying full JSON ingestion packets.
    *   Security access control and user authorization manager.

---

## 🎨 Design & Assets

The UI is built with a custom CSS design system optimized for readability under high stress:

*   **Colors**: Sleek, high-contrast dark theme (#0b0f19) paired with vibrant feedback gradients (emerald green, neon red, tech cyan).
*   **Typography**: Clean sans-serif sans font family (Inter) for labels, and JetBrains Mono for system log files.
*   **Visual Enhancements (`js/mesh-bg.js`)**: A custom canvas-based particle web generator that visually illustrates peer-to-peer mesh packet hops and networking connectivity.
*   **Glassmorphism**: Backdrop blur components for floating cards and popup modals.
