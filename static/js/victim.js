let activeSosId = null;
let cancelTimer = null;
let countdownVal = 5;
let pollInterval = null;

async function triggerSos() {
    const payloadText = document.getElementById('payloadText').value || "Emergency SOS Triggered from Citizen Device";
    
    // Get GPS coords or default to New Delhi mock
    navigator.geolocation.getCurrentPosition(
        (pos) => sendPacket(pos.coords.latitude, pos.coords.longitude, pos.coords.accuracy, payloadText),
        () => sendPacket(28.6139, 77.2090, 15.0, payloadText),
        { timeout: 5000 }
    );
}

async function sendPacket(lat, lon, acc, payloadText) {
    const msgId = 'bcn-msg-' + Math.random().toString(36).substring(2, 10);
    const originId = 'usr-node-' + Math.random().toString(36).substring(2, 8);
    
    const packet = {
        msg_id: msgId,
        origin_id: originId,
        created_at: Date.now(),
        lat: lat,
        lon: lon,
        acc: acc,
        severity: 'CRITICAL',
        priority: 4,
        confidence: 0.95,
        trigger_type: 'MANUAL',
        ttl: 7,
        hops: 1,
        payload: payloadText,
        signature: '3045022100' + Math.random().toString(36).substring(2, 12),
        gateway_id: 'web-gateway-edge-1'
    };

    updateStep(1, true);
    setTimeout(() => updateStep(2, true), 400);

    try {
        const res = await fetch('/api/v1/sos/ingest', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Gateway-Id': 'web-gateway-edge-1' },
            body: JSON.stringify(packet)
        });
        
        const data = await res.json();
        activeSosId = data.sos_id;

        updateStep(3, true);
        updateStep(4, true);

        startCancelWindow();
        startStatusPolling();
    } catch (e) {
        alert('Network Transmission Error: ' + e.message);
    }
}

function startCancelWindow() {
    document.getElementById('cancelBox').style.display = 'block';
    countdownVal = 5;
    document.getElementById('countdown').innerText = countdownVal;
    
    cancelTimer = setInterval(() => {
        countdownVal--;
        document.getElementById('countdown').innerText = countdownVal;
        if (countdownVal <= 0) {
            clearInterval(cancelTimer);
            document.getElementById('cancelBox').style.display = 'none';
        }
    }, 1000);
}

async function cancelSos() {
    if (cancelTimer) clearInterval(cancelTimer);
    if (!activeSosId) return;

    try {
        await fetch(`/api/v1/victim/sos/${activeSosId}/cancel`, { method: 'POST' });
        alert('Emergency transmission cancelled.');
        window.location.reload();
    } catch (e) {
        alert('Failed cancelling SOS: ' + e.message);
    }
}

function startStatusPolling() {
    if (pollInterval) clearInterval(pollInterval);
    pollInterval = setInterval(async () => {
        if (!activeSosId) return;
        try {
            const res = await fetch(`/api/v1/victim/sos/${activeSosId}/status`);
            const data = await res.json();
            if (data.status === 'RESPONDING' || data.status === 'ON_SCENE') {
                updateStep(5, true);
            }
        } catch (e) {}
    }, 3000);
}

function updateStep(stepNum, isCompleted) {
    const el = document.getElementById(`step${stepNum}`);
    if (el) {
        if (isCompleted) {
            el.className = 'timeline-step completed';
        }
    }
}
