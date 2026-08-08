from typing import List
from fastapi import WebSocket
import json

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast_incident(self, incident_data: dict):
        if not self.active_connections:
            return
        message = json.dumps(incident_data)
        dead: List[WebSocket] = []
        for connection in list(self.active_connections):  # iterate a copy to allow mutation
            try:
                await connection.send_text(message)
            except Exception:
                dead.append(connection)  # mark stale, don't remove mid-loop
        for conn in dead:
            self.disconnect(conn)  # prune all dead connections after broadcast

manager = ConnectionManager()
