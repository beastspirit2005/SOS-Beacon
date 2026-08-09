import os
import requests
import logging

logger = logging.getLogger(__name__)

BREVO_API_KEY = os.getenv("BREVO_API_KEY")

def send_otp_email(to_email: str, otp_code: str):
    """
    Sends a 6-digit OTP code to the requested email via Brevo API.
    """
    if not BREVO_API_KEY:
        logger.warning(f"BREVO_API_KEY missing. Mocking OTP send: Code {otp_code} for {to_email}")
        return True

    url = "https://api.brevo.com/v3/smtp/email"
    headers = {
        "accept": "application/json",
        "api-key": BREVO_API_KEY,
        "content-type": "application/json"
    }
    payload = {
        "sender": {"name": "Project Beacon", "email": "no-reply@projectbeacon.app"},
        "to": [{"email": to_email}],
        "subject": "Your Project Beacon Login OTP",
        "htmlContent": f"""
        <div style="font-family: Arial, sans-serif; padding: 20px; background-color: #0f172a; color: #f8fafc; border-radius: 8px;">
            <h2 style="color: #2de1c4;">PROJECT BEACON</h2>
            <p>Your authentication OTP code is:</p>
            <h1 style="font-size: 36px; letter-spacing: 4px; color: #38bdf8;">{otp_code}</h1>
            <p style="font-size: 12px; color: #94a3b8;">This OTP will expire in 5 minutes. Do not share this code with anyone.</p>
        </div>
        """
    }

    try:
        res = requests.post(url, json=payload, headers=headers, timeout=5)
        res.raise_for_status()
        logger.info(f"OTP Email dispatched to {to_email}")
        return True
    except Exception as e:
        logger.error(f"Failed to dispatch Brevo OTP email: {e}")
        return False

def send_emergency_alert_email(to_email: str, incident_data: dict):
    """
    Dispatches a high-priority emergency alert notification to an assigned officer or administrator.
    """
    if not BREVO_API_KEY:
        logger.warning(f"BREVO_API_KEY missing. Mocking emergency alert to {to_email}")
        return True

    url = "https://api.brevo.com/v3/smtp/email"
    headers = {
        "accept": "application/json",
        "api-key": BREVO_API_KEY,
        "content-type": "application/json"
    }
    payload = {
        "sender": {"name": "Beacon Emergency Dispatch", "email": "alerts@projectbeacon.app"},
        "to": [{"email": to_email}],
        "subject": f"🚨 EMERGENCY DISPATCH: {incident_data.get('severity', 'CRITICAL')} - {incident_data.get('sos_id')}",
        "htmlContent": f"""
        <div style="font-family: Arial, sans-serif; padding: 20px; background-color: #18181b; color: #f4f4f5; border-radius: 8px; border: 2px solid #ef4444;">
            <h2 style="color: #ef4444;">🚨 CRITICAL EMERGENCY INCIDENT DISPATCHED</h2>
            <p><strong>Incident ID:</strong> {incident_data.get('sos_id')}</p>
            <p><strong>Severity:</strong> {incident_data.get('severity')}</p>
            <p><strong>Summary:</strong> {incident_data.get('ai_summary', incident_data.get('payload'))}</p>
            <p><strong>Coordinates:</strong> {incident_data.get('lat')}, {incident_data.get('lon')}</p>
            <p><a href="https://sos-beacon-pi.vercel.app/officer" style="background: #ef4444; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;">Open Officer Command Portal</a></p>
        </div>
        """
    }

    try:
        res = requests.post(url, json=payload, headers=headers, timeout=5)
        res.raise_for_status()
        return True
    except Exception as e:
        logger.error(f"Failed sending emergency alert email: {e}")
        return False
