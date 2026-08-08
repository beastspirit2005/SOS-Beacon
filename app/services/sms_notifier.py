async def send_sms_notification(origin_id: str, summary: str, lat: float, lon: float, escalation: str):
    maps_url = f"https://www.google.com/maps?q={lat},{lon}"
    message = f"BEACON ALERT [{escalation.upper()}]: {summary}. Loc: {maps_url}"
    print(f"\n{'='*50}\n[SMS DISPATCH] To: {escalation}\n[MSG] {message}\n{'='*50}\n")
    # For MVP, logging replaces physical SMS dispatch to avoid costs.
