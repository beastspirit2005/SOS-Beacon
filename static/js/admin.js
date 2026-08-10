let authToken = localStorage.getItem('beacon_token');

document.addEventListener('DOMContentLoaded', () => {
    fetchStats();
    fetchUsers();
    setInterval(fetchStats, 5000);
});

async function fetchStats() {
    try {
        const headers = authToken ? { 'Authorization': `Bearer ${authToken}` } : {};
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
        const res = await fetch('/api/v1/users', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
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
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
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
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        if (!res.ok) throw new Error(await res.text());
        fetchUsers();
    } catch (e) {
        alert("Failed to delete user: " + e.message);
    }
}
