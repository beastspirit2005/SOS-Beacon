// Auth removed for demo mode
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    fetchStats();
    fetchUsers();
    fetchIncidents();
    fetchClusters();
    setInterval(() => {
        fetchStats();
        fetchIncidents();
        fetchClusters();
    }, 5000);
});

async function fetchStats() {
    try {
        const headers = { 'Content-Type': 'application/json' };
        const res = await fetch('/api/v1/admin/stats', { headers });
        if (!res.ok) return;

        const data = await res.json();
        document.getElementById('valTotal').innerText = data.total_incidents;
        document.getElementById('valActive').innerText = data.active_emergencies;
        document.getElementById('valGateways').innerText = data.active_gateways;
        document.getElementById('valClusters').innerText = data.mass_casualty_clusters;
    } catch (e) {}
}

let allUsers = [];

async function fetchUsers() {
    try {
        const res = await fetch('/api/v1/users');
        if (!res.ok) return;
        allUsers = await res.json();
        renderUserTable();
    } catch (e) {
        console.error('Failed to fetch users:', e);
    }
}

function renderUserTable() {
    const tbody = document.getElementById('userTableBody');
    tbody.innerHTML = '';
    
    allUsers.forEach(u => {
        const tr = document.createElement('tr');
        tr.style.borderBottom = '1px solid rgba(255,255,255,0.05)';
        
        let roleColor = '#38bdf8';
        if (u.role === 'ADMIN') roleColor = '#ef4444';
        if (u.role === 'OFFICER') roleColor = '#f59e0b';
        
        tr.innerHTML = `
            <td style="padding: 10px;">${u.email}</td>
            <td style="padding: 10px;">${u.name || '-'}</td>
            <td style="padding: 10px; font-weight: bold; color: ${roleColor};">${u.role}</td>
            <td style="padding: 10px; text-align: right;">
                <button onclick="editUser('${u.id}')" style="background:transparent; border:none; color:var(--color-cyan); cursor:pointer; margin-right:8px;">EDIT</button>
                <button onclick="deleteUser('${u.id}')" style="background:transparent; border:none; color:var(--color-red); cursor:pointer;">DELETE</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function editUser(id) {
    const u = allUsers.find(x => x.id === id);
    if (!u) return;
    
    document.getElementById('provUserId').value = u.id;
    document.getElementById('provEmail').value = u.email;
    document.getElementById('provName').value = u.name || '';
    document.getElementById('provRole').value = u.role;
    document.getElementById('provPassword').placeholder = '(Leave blank to keep current)';
    
    document.getElementById('btnSaveUser').innerText = 'UPDATE IDENTITY';
    document.getElementById('btnClearForm').style.display = 'inline-block';
}

function clearUserForm() {
    document.getElementById('provUserId').value = '';
    document.getElementById('provEmail').value = '';
    document.getElementById('provName').value = '';
    document.getElementById('provRole').value = 'OFFICER';
    document.getElementById('provPassword').value = '';
    document.getElementById('provPassword').placeholder = 'Set Password (New Only)';
    
    document.getElementById('btnSaveUser').innerText = 'PROVISION IDENTITY';
    document.getElementById('btnClearForm').style.display = 'none';
}

async function saveUser() {
    const id = document.getElementById('provUserId').value;
    const email = document.getElementById('provEmail').value;
    const name = document.getElementById('provName').value;
    const role = document.getElementById('provRole').value;
    const password = document.getElementById('provPassword').value;

    if (!email || !name) return alert("Email and Name required.");
    if (!id && !password) return alert("Password is required for new users.");

    try {
        const headers = { 
            'Content-Type': 'application/json'
        };

        let url = '/api/v1/users';
        let method = 'POST';
        let body = { email, name, role, password };
        
        if (id) {
            url = `/api/v1/users/${id}`;
            method = 'PUT';
            body = { email, name, role };
            // Note: Update password API not fully implemented in backend for PUT, this only updates info
        }

        const res = await fetch(url, {
            method,
            headers,
            body: JSON.stringify(body)
        });
        
        if (!res.ok) throw new Error(await res.text());
        
        clearUserForm();
        fetchUsers();
    } catch (e) {
        alert("Operation failed: " + e.message);
    }
}

async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    
    try {
        const res = await fetch(`/api/v1/users/${id}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error(await res.text());
        fetchUsers();
    } catch (e) {
        alert("Failed to delete user: " + e.message);
    }
}



// --- Map and Queue Logic ---
let map;
let markers = {};

function initMap() {
    map = L.map('map').setView([28.6139, 77.2090], 5); // zoomed out for national view
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '© OpenStreetMap contributors © CARTO'
    }).addTo(map);
}

async function fetchIncidents() {
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
    if(!list) return;
    document.getElementById('queueCount').innerText = `${incidents.length} Active`;
    list.innerHTML = '';
    
    incidents.forEach(inc => {
        const card = document.createElement('div');
        card.className = `incident-card priority-${inc.priority}`;
        card.style.cursor = 'pointer';
        card.onclick = () => focusMap(inc.lat, inc.lon, inc.sos_id);
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <span class="severity-badge severity-${inc.severity}">${inc.severity} (P${inc.priority})</span>
                <span style="font-size: 0.75rem; color: #64748b; font-family: monospace;">${new Date(inc.received_at).toLocaleTimeString()}</span>
            </div>
            <details style="margin-bottom: 0.75rem; color: #cbd5e1; font-size: 0.85rem;" onclick="event.stopPropagation()">
                <summary style="cursor: pointer; font-weight: 600; color: #38bdf8; outline: none; margin-bottom: 0.25rem;">View Summary Details</summary>
                <div style="margin-top: 0.25rem; padding: 0.5rem; background: rgba(255,255,255,0.05); border-radius: 4px; line-height: 1.4;">
                    ${inc.ai_summary || inc.payload}
                </div>
            </details>
            <div style="font-size: 0.8rem; color: #94a3b8;">Status: <strong style="color: #38bdf8;">${inc.status}</strong></div>
        `;
        list.appendChild(card);

        if (!markers[inc.sos_id]) {
            const marker = L.circleMarker([inc.lat, inc.lon], {
                color: inc.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b',
                radius: 8,
                fillOpacity: 0.8
            }).addTo(map);
            marker.bindPopup(`<b>${inc.severity}</b><br>${inc.ai_summary || inc.payload}`);
            markers[inc.sos_id] = marker;
        }
    });
}

function focusMap(lat, lon, sos_id) {
    if (map) {
        map.setView([lat, lon], 16, { animate: true, duration: 1.5 });
        if (markers[sos_id]) {
            markers[sos_id].openPopup();
        }
    }
}

// --- Clusters Logic ---
async function fetchClusters() {
    try {
        const headers = { 'Content-Type': 'application/json' };
        const res = await fetch('/api/v1/admin/clusters', { headers });
        if (!res.ok) return;
        const data = await res.json();
        renderClusters(data.clusters || []);
    } catch (e) {}
}

function renderClusters(clusters) {
    const container = document.getElementById('clustersContainer');
    if(!container) return;
    if (clusters.length === 0) {
        container.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--text-muted); grid-column: 1 / -1;"><p style="font-size: 0.9rem;">No active mass casualty clusters detected.</p></div>';
        return;
    }
    container.innerHTML = '';
    clusters.forEach(c => {
        const card = document.createElement('div');
        card.style.background = 'rgba(255,255,255,0.02)';
        card.style.border = '1px solid rgba(255,59,48,0.3)';
        card.style.padding = '1rem';
        card.style.borderRadius = '8px';
        card.style.boxShadow = '0 0 15px rgba(255,59,48,0.1)';
        
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span style="font-weight: 700; color: #ef4444;">Cluster Radius: ${c.radius_m}m</span>
                <span style="background: #ef4444; color: white; padding: 2px 6px; border-radius: 4px; font-size: 0.75rem; font-weight: bold;">${c.victim_count} VICTIMS</span>
            </div>
            <div style="font-size: 0.85rem; color: #cbd5e1; margin-bottom: 0.5rem;">Center: [${c.center_lat.toFixed(4)}, ${c.center_lon.toFixed(4)}]</div>
            <div style="font-size: 0.75rem; color: #94a3b8;">Active since: ${new Date(c.created_at).toLocaleString()}</div>
        `;
        container.appendChild(card);
    });
}
