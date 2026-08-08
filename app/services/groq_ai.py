import os
import httpx
import json
from typing import Tuple

GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
MODEL_NAME = "llama-3.1-8b-instant"

async def enrich_incident(payload: str, severity: str, priority: int) -> Tuple[str, str, str]:
    if not GROQ_API_KEY:
        return (f"Raw SOS: {payload}", "high" if priority >= 4 else "medium", "responders" if priority >= 4 else "contacts")

    system_prompt = (
        "You are an AI emergency dispatch assistant. Analyze the incoming SOS message. "
        "Output ONLY a JSON object with exactly these keys: "
        "'summary' (string: a crisp 1-sentence briefing of the emergency), "
        "'priority' (string: 'low', 'medium', or 'high'), "
        "'escalation' (string: 'contacts' or 'responders')."
    )
    
    user_prompt = f"Severity: {severity}, Priority: {priority}, Payload: '{payload}'"
    
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
            response = await client.post(GROQ_API_URL, headers=headers, json=data, timeout=5.0)
            response.raise_for_status()
            result = response.json()
            content = result["choices"][0]["message"]["content"]
            
            parsed = json.loads(content)
            summary = parsed.get("summary", "Emergency incident reported.")
            computed_priority = parsed.get("priority", "medium").lower()
            escalation = parsed.get("escalation", "responders").lower()
            
            if computed_priority not in ["low", "medium", "high"]: computed_priority = "high"
            if escalation not in ["contacts", "responders"]: escalation = "responders"
                
            return (summary, computed_priority, escalation)
            
    except Exception as e:
        print(f"Groq API Error: {e}")
        return (f"Raw SOS: {payload}", "high" if priority >= 4 else "medium", "responders" if priority >= 4 else "contacts")
