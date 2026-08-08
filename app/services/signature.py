import hmac
import hashlib
import os
import logging
from dotenv import load_dotenv
from ..schemas.packet import SosPacket

load_dotenv()

logger = logging.getLogger(__name__)

SECRET_KEY = os.getenv("SECRET_KEY", "beacon_hmac_secret_key_2026_iic3")
MAX_AGE_MS = 1000 * 60 * 60 * 24  # 24 hours
CLOCK_DRIFT_TOLERANCE_MS = 1000 * 60 * 5  # 5 minutes future drift allowed

def compute_signature(msg_id: str, origin_id: str, created_at: int, payload: str) -> str:
    """Generate canonical HMAC-SHA256 signature. Must match client signing exactly."""
    canonical_string = f"{msg_id}:{origin_id}:{created_at}:{payload}"
    return hmac.new(
        SECRET_KEY.encode(),
        canonical_string.encode(),
        hashlib.sha256
    ).hexdigest()

def verify_signature(packet: SosPacket, current_time_ms: int) -> bool:
    # 1. Check packet is not from the future (clock-skew / replay attack)
    if packet.created_at > current_time_ms + CLOCK_DRIFT_TOLERANCE_MS:
        logger.warning(f"Future-dated packet rejected: msg_id={packet.msg_id}")
        raise ValueError("PACKET_EXPIRED")

    # 2. Check packet has not expired
    if current_time_ms - packet.created_at > MAX_AGE_MS:
        logger.warning(f"Expired packet rejected: msg_id={packet.msg_id}")
        raise ValueError("PACKET_EXPIRED")

    # 3. Verify HMAC signature (timing-safe comparison)
    expected_sig = compute_signature(
        packet.msg_id, packet.origin_id, packet.created_at, packet.payload
    )
    if not hmac.compare_digest(expected_sig, packet.sig):
        logger.warning(f"Bad signature rejected: msg_id={packet.msg_id}")
        raise ValueError("BAD_SIGNATURE")

    return True
