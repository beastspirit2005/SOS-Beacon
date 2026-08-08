import os
import httpx
import json
import logging
from typing import Tuple
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
MODEL_NAME   = "llama-3.1-8b-instant"

# Severity → minimum dispatch tier (fail-safe floor, M-1 fix)
_SEVERITY_FLOOR: dict = {
    "critical": ("high", "responders"),
    "warn":     ("medium", "responders"),
    "info":     ("low",    "contacts"),
}


async def enrich_incident(payload: str, severity: str, priority: int) -> Tuple[str, str, str]:
    """
    Return (summary, priority_tier, escalation).

    Tries Groq AI first (3-second timeout). Falls back to rule-based enrichment.
    Fail-safe: AI can only *raise* urgency — severity and raw priority set a hard floor.
    """
    if not GROQ_API_KEY:
        logger.warning("GROQ_API_KEY not set — using rule-based fallback enrichment.")
        return _fallback_enrichment(payload, severity, priority)

    system_prompt = (
        "You are an AI emergency dispatch assistant. Analyze the incoming SOS message. "
        "Output ONLY a JSON object with exactly these keys: "
        "'summary' (string: a crisp 1-sentence briefing of the emergency), "
        "'priority' (string: 'low', 'medium', or 'high'), "
        "'escalation' (string: 'contacts' or 'responders')."
    )
    user_prompt = f"Severity: {severity}, Priority level (1-5): {priority}, Payload: '{payload}'"

    headers = {
        "Authorization": f"Bearer {GROQ_API_KEY}",
        "Content-Type": "application/json",
    }
    data = {
        "model": MODEL_NAME,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user",   "content": user_prompt},
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.1,
    }

    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                GROQ_API_URL, headers=headers, json=data,
                timeout=3.0  # M-3 fix: was 8s — llama-3.1-8b-instant responds in <1s
            )
            response.raise_for_status()
            content = response.json()["choices"][0]["message"]["content"]
            parsed  = json.loads(content)

            summary           = parsed.get("summary",   "Emergency incident reported.").strip()
            computed_priority = parsed.get("priority",  "medium").lower().strip()
            escalation        = parsed.get("escalation","responders").lower().strip()

            # Sanitise AI output
            if computed_priority not in ("low", "medium", "high"):
                computed_priority = "high"
            if escalation not in ("contacts", "responders"):
                escalation = "responders"

            # --- Fail-safe floor: AI can escalate, never suppress (M-1 fix) ---
            floor_priority, floor_escalation = _severity_floor(severity, priority)
            computed_priority = _max_priority(computed_priority, floor_priority)
            if floor_escalation == "responders":
                escalation = "responders"

            return (summary, computed_priority, escalation)

    except Exception as e:
        logger.error(f"Groq API error (falling back to rules): {e}")
        return _fallback_enrichment(payload, severity, priority)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _severity_floor(severity: str, priority: int) -> Tuple[str, str]:
    """Return the minimum (priority_tier, escalation) based on severity + raw priority."""
    # Raw priority integer floor
    if priority >= 5:
        return ("high", "responders")
    if priority >= 4:
        return ("high", "responders")

    # Severity string floor
    tier, esc = _SEVERITY_FLOOR.get(severity, ("low", "contacts"))
    return (tier, esc)


_PRIORITY_RANK = {"low": 0, "medium": 1, "high": 2}

def _max_priority(a: str, b: str) -> str:
    """Return the higher of two priority tier strings."""
    return a if _PRIORITY_RANK.get(a, 0) >= _PRIORITY_RANK.get(b, 0) else b


def _fallback_enrichment(payload: str, severity: str, priority: int) -> Tuple[str, str, str]:
    """
    Deterministic rule-based fallback when Groq is unavailable.
    Respects both severity and priority — always fails safe toward escalation.
    """
    floor_priority, floor_escalation = _severity_floor(severity, priority)

    if floor_priority == "high":
        return (f"Critical emergency: {payload[:120]}", "high", "responders")
    elif floor_priority == "medium":
        return (f"Emergency SOS: {payload[:120]}", "medium", "responders")
    else:
        return (f"Emergency update: {payload[:120]}", "low", "contacts")
