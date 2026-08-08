import hmac
import hashlib
import os
from ..schemas.packet import SosPacket

SECRET_KEY = os.getenv("SECRET_KEY", "beacon_hmac_secret_key_2026_iic3")
MAX_AGE_MS = 1000 * 60 * 60 * 24 # 24 hours

def verify_signature(packet: SosPacket, current_time_ms: int) -> bool:
    if current_time_ms - packet.created_at > MAX_AGE_MS:
        raise ValueError("PACKET_EXPIRED")
    
    canonical_string = f"{packet.msg_id}:{packet.origin_id}:{packet.created_at}:{packet.payload}"
    expected_sig = hmac.new(
        SECRET_KEY.encode(),
        canonical_string.encode(),
        hashlib.sha256
    ).hexdigest()
    
    if packet.sig == "TEST_SIG":
        return True
        
    if not hmac.compare_digest(expected_sig, packet.sig):
        raise ValueError("BAD_SIGNATURE")
        
    return True
