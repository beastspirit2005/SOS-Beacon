import uuid
import time
import hmac
import hashlib
import json
import httpx
import asyncio

API_URL = "http://127.0.0.1:8000/api/v1/sos/ingest"
GATEWAY_ID = f"gateway-{uuid.uuid4().hex[:6]}"
SECRET_KEY = "beacon_hmac_secret_key_2026_iic3"

def generate_signature(msg_id, origin_id, created_at, payload):
    canonical_string = f"{msg_id}:{origin_id}:{created_at}:{payload}"
    return hmac.new(SECRET_KEY.encode(), canonical_string.encode(), hashlib.sha256).hexdigest()

async def send_sos():
    msg_id = str(uuid.uuid4())
    origin_id = f"victim-{uuid.uuid4().hex[:4]}"
    created_at = int(time.time() * 1000)
    payload = "SOS! Heavy earthquake damage, multiple trapped in Sector 4."
    
    packet = {
        "msg_id": msg_id,
        "origin_id": origin_id,
        "created_at": created_at,
        "lat": 28.6139,
        "lon": 77.2090,
        "acc": 5.0,
        "severity": "critical",
        "priority": 5,
        "confidence": 0.95,
        "trigger_type": "manual",
        "ttl": 3,
        "hops": 2,
        "payload": payload,
        "sig": generate_signature(msg_id, origin_id, created_at, payload)
    }

    req_body = {
        "packet": packet,
        "received_at": int(time.time() * 1000)
    }

    headers = {
        "X-Gateway-Id": GATEWAY_ID,
        "X-App-Version": "2.0.0"
    }

    print(f"\n[mock_gateway_client] Sending SOS (msg_id: {msg_id}) to {API_URL}...")
    
    async with httpx.AsyncClient() as client:
        # First send (should be accepted)
        res1 = await client.post(API_URL, json=req_body, headers=headers, timeout=10.0)
        print(f"Response 1 Status: {res1.status_code}")
        print(f"Response 1 Body: {json.dumps(res1.json(), indent=2)}")

        print("\n[mock_gateway_client] Sending DUPLICATE SOS (same msg_id)...")
        # Second send (should be duplicate)
        res2 = await client.post(API_URL, json=req_body, headers=headers, timeout=10.0)
        print(f"Response 2 Status: {res2.status_code}")
        print(f"Response 2 Body: {json.dumps(res2.json(), indent=2)}")

if __name__ == "__main__":
    asyncio.run(send_sos())
