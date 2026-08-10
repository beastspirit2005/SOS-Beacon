let map;
let markers = {};
let lastPollTime = 0;
// Auth removed for demo mode

document.addEventListener('DOMContentLoaded', () => {
    initMap();
    startPolling();
});

function initMap() {
    map = L.map('map').setView([28.6139, 77.2090], 12);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '© OpenStreetMap contributors © CARTO'
    }).addTo(map);
}

async function requestLogin() {
    const email = prompt("Enter Officer Email for OTP Authentication:");
    if (!email) return;

    try {
        await fetch('/api/v1/auth/request-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });
        const otp = prompt("Enter 6-digit OTP sent to your email:");
        if (!otp) return;

        const res = await fetch('/api/v1/auth/verify-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otp_code: otp })
        });
        const data = await res.json();
        if (data.access_token) {
            authToken = data.access_token;
            localStorage.setItem('beacon_token', authToken);
            document.getElementById('officerIdentity').innerText = `Logged in: ${data.user.name} (${data.user.role})`;
            alert("Authenticated successfully!");
            pollIncidents();
        }
    } catch (e) {
        alert("Auth failed: " + e.message);
    }
}

function startPolling() {
    pollIncidents();
    setInterval(pollIncidents, 3000);
}

async function pollIncidents() {
    try {
        const headers = { 'Content-Type': 'application/json' };
        const res = await fetch(`/api/v1/officer/incidents?since_ms=0`, { headers });
        if (!res.ok) return;

        const data = await res.json();
        renderQueue(data.incidents || []);
    } catch (e) {}
}

function renderQueue(incidents) {
    const list = document.getElementById('incidentList');
    document.getElementById('queueCount').innerText = `${incidents.length} Active`;
    list.innerHTML = '';

    incidents.forEach(inc => {
        const card = document.createElement('div');
        card.className = `incident-card priority-${inc.priority}`;
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <span class="severity-badge severity-${inc.severity}">${inc.severity} (P${inc.priority})</span>
                <span style="font-size: 0.75rem; color: #64748b; font-family: monospace;">${new Date(inc.received_at).toLocaleTimeString()}</span>
            </div>
            <div style="font-weight: 600; font-size: 0.9rem; color: #f8fafc; margin-bottom: 0.25rem;">${inc.ai_summary || inc.payload}</div>
            <div style="font-size: 0.8rem; color: #94a3b8; margin-bottom: 0.75rem;">Status: <strong style="color: #38bdf8;">${inc.status}</strong></div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn-dispatch" onclick="updateStatus('${inc.sos_id}', 'RESPONDING')">Respond</button>
                <button class="btn-dispatch" style="background: #22c55e; color: white;" onclick="updateStatus('${inc.sos_id}', 'RESOLVED')">Resolve</button>
            </div>
        `;
        list.appendChild(card);

        // Update Map Marker
        if (!markers[inc.sos_id]) {
            const marker = L.circleMarker([inc.lat, inc.lon], {
                color: inc.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b',
                radius: 10,
                fillOpacity: 0.8
            }).addTo(map);
            marker.bindPopup(`<b>${inc.severity}</b><br>${inc.ai_summary || inc.payload}`);
            markers[inc.sos_id] = marker;

            document.getElementById('alertSound').play().catch(e => {});
        }
    });
}

async function updateStatus(sos_id, new_status) {
    try {
        const headers = { 'Content-Type': 'application/json' };
        await fetch(`/api/v1/officer/incidents/${sos_id}/status?new_status=${new_status}`, {
            method: 'POST',
            headers
        });
        pollIncidents();
    } catch (e) {
        alert("Action failed: " + e.message);
    }
}
