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
MODEL_NAME = "llama-3.1-8b-instant"

async def enrich_incident(payload: str, severity: str, priority: int) -> Tuple[str, str, str]:
    if not GROQ_API_KEY:
        logger.warning("GROQ_API_KEY not set. Using rule-based fallback enrichment.")
        return _fallback_enrichment(payload, priority)

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
        "Content-Type": "application/json"
    }

    data = {
        "model": MODEL_NAME,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.1
    }

    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(GROQ_API_URL, headers=headers, json=data, timeout=8.0)
            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"]

            parsed = json.loads(content)
            summary = parsed.get("summary", "Emergency incident reported.").strip()
            computed_priority = parsed.get("priority", "medium").lower().strip()
            escalation = parsed.get("escalation", "responders").lower().strip()

            if computed_priority not in ["low", "medium", "high"]:
                computed_priority = "high"
            if escalation not in ["contacts", "responders"]:
                escalation = "responders"

            # Fail-safe: AI can escalate, never suppress — enforce minimum based on raw priority field
            if priority >= 5 and computed_priority != "high":
                computed_priority = "high"
                escalation = "responders"

            return (summary, computed_priority, escalation)

    except Exception as e:
        logger.error(f"Groq API Error (falling back to rules): {e}")
        return _fallback_enrichment(payload, priority)


def _fallback_enrichment(payload: str, priority: int) -> Tuple[str, str, str]:
    """Deterministic rule-based fallback when Groq is unavailable."""
    if priority >= 4:
        return (f"Critical emergency: {payload[:120]}", "high", "responders")
    elif priority == 3:
        return (f"Manual SOS: {payload[:120]}", "medium", "responders")
    else:
        return (f"Emergency update: {payload[:120]}", "low", "contacts")
