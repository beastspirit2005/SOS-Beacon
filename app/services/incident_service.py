import time
import uuid
import logging
from sqlalchemy.orm import Session
from ..database.models import SosIncident, IncidentEvent, IncidentCluster, GatewayLog
from ..schemas.packet import SosPacket
from .groq_ai import enrich_incident_with_groq
from .redis_bus import publish_event

logger = logging.getLogger(__name__)

def process_incoming_packet(packet: SosPacket, db: Session) -> dict:
    """
    Core Incident Engine:
    1. Validates & deduplicates packet by msg_id and gateway logging.
    2. Performs Groq AI Triage & enrichment.
    3. Normalizes and persists to PostgreSQL.
    4. Evaluates proximity to detect Mass Casualty Incident Clusters.
    5. Publishes event to Redis Streams for downstream websocket & notification dispatchers.
    """
    current_time = int(time.time() * 1000)

    # Gateway log deduplication
    if packet.gateway_id:
        existing_gw = db.query(GatewayLog).filter(
            GatewayLog.msg_id == packet.msg_id,
            GatewayLog.gateway_id == packet.gateway_id
        ).first()
        if not existing_gw:
            gw_log = GatewayLog(
                msg_id=packet.msg_id,
                gateway_id=packet.gateway_id,
                received_at=current_time
            )
            db.add(gw_log)

    # Check if incident already ingested globally
    existing_incident = db.query(SosIncident).filter(SosIncident.msg_id == packet.msg_id).first()
    if existing_incident:
        logger.info(f"Packet {packet.msg_id} already ingested globally. Returning existing SOS ID.")
        return {
            "sos_id": existing_incident.sos_id,
            "status": existing_incident.status,
            "duplicate": True
        }

    # Perform Groq AI Triage
    triage_result = enrich_incident_with_groq(
        payload=packet.payload,
        trigger_type=packet.trigger_type,
        severity=packet.severity
    )

    sos_id = f"bcn-sos-{uuid.uuid4().hex[:8]}"
    
    incident = SosIncident(
        sos_id=sos_id,
        msg_id=packet.msg_id,
        origin_id=packet.origin_id,
        created_at=packet.created_at,
        received_at=current_time,
        lat=packet.lat,
        lon=packet.lon,
        acc=packet.acc,
        severity=triage_result.get("recommended_severity", packet.severity),
        priority=triage_result.get("priority", packet.priority),
        confidence=packet.confidence,
        trigger_type=packet.trigger_type,
        payload=packet.payload,
        ai_summary=triage_result.get("summary"),
        escalation_tier=triage_result.get("action_items"),
        status="CREATED",
        delivery_status="delivered"
    )
    
    db.add(incident)

    # Record lifecycle creation event
    event = IncidentEvent(
        id=str(uuid.uuid4()),
        incident_id=sos_id,
        event_type="sos.created",
        actor_id=packet.origin_id,
        metadata_json=f'{{"hops": {packet.hops}, "gateway": "{packet.gateway_id}"}}',
        timestamp=current_time
    )
    db.add(event)
    db.commit()
    db.refresh(incident)

    # Evaluate Cluster Detection (Blueprint Section 28)
    check_and_update_cluster(incident, db)

    # Publish to Event Bus
    publish_event("sos.created", {
        "sos_id": incident.sos_id,
        "msg_id": incident.msg_id,
        "lat": incident.lat,
        "lon": incident.lon,
        "severity": incident.severity,
        "priority": incident.priority,
        "ai_summary": incident.ai_summary,
        "timestamp": current_time
    })

    return {
        "sos_id": incident.sos_id,
        "status": incident.status,
        "duplicate": False
    }

def check_and_update_cluster(new_incident: SosIncident, db: Session, radius_km: float = 0.5):
    """
    Collective Incident Intelligence: Detects if multiple incidents occur within 500m radius to form a Mass Casualty Cluster.
    """
    recent_time = new_incident.received_at - (15 * 60 * 1000) # 15 min window
    
    nearby_incidents = db.query(SosIncident).filter(
        SosIncident.received_at >= recent_time,
        SosIncident.sos_id != new_incident.sos_id
    ).all()

    cluster_members = [new_incident]
    for inc in nearby_incidents:
        # Simple Euclidean approximation for short distances (~500m ~0.005 degrees)
        lat_diff = abs(inc.lat - new_incident.lat)
        lon_diff = abs(inc.lon - new_incident.lon)
        if lat_diff < 0.005 and lon_diff < 0.005:
            cluster_members.append(inc)

    if len(cluster_members) >= 3:
        cluster_id = f"cluster-{uuid.uuid4().hex[:6]}"
        avg_lat = sum(i.lat for i in cluster_members) / len(cluster_members)
        avg_lon = sum(i.lon for i in cluster_members) / len(cluster_members)

        new_cluster = IncidentCluster(
            id=cluster_id,
            name=f"Mass Casualty Cluster ({len(cluster_members)} Victims)",
            center_lat=avg_lat,
            center_lon=avg_lon,
            radius_meters=300.0,
            victim_count=len(cluster_members),
            confidence=0.92,
            created_at=int(time.time() * 1000)
        )
        db.add(new_cluster)

        for member in cluster_members:
            member.cluster_id = cluster_id
        
        db.commit()
        logger.info(f"Detected Mass Casualty Cluster {cluster_id} with {len(cluster_members)} victims!")
