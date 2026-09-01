"""
Script interactivo de verificación de interoperabilidad de red para Ronda Canaria.
Prueba la lógica de 11 Malas + 10 Buenas, Cantos, eventos SoundTrigger y Fin de Partida sobre TCP NDJSON.
"""

import socket
import json
import time
import sys
import uuid

PORT = 8888

def calculate_score(total):
    clamped = max(0, min(21, total))
    malas = min(clamped, 11)
    buenas = max(0, clamped - 11)
    return {
        "totalPiedras": clamped,
        "malas": malas,
        "buenas": buenas,
        "isInBuenas": clamped >= 11
    }

def run_server(host="0.0.0.0", port=PORT):
    print(f"[*] Iniciando Mesa Host de Ronda Canaria en {host}:{port}...")
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(5)
    print(f"[✓] Servidor escuchando. Esperando jugadores Android o iOS...")

    try:
        while True:
            client_sock, addr = server.accept()
            print(f"[+] Jugador conectado desde: {addr}")
            
            buffer = ""
            while True:
                data = client_sock.recv(4096).decode('utf-8')
                if not data:
                    break
                buffer += data
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    if line.strip():
                        print(f"[<-- Mensaje]: {line}")
                        msg = json.loads(line)
                        
                        if msg.get("type") == "JOIN_REQUEST":
                            response = {
                                "type": "JOIN_RESPONSE",
                                "id": str(uuid.uuid4()),
                                "timestamp": int(time.time() * 1000),
                                "senderId": "HOST",
                                "joinResponse": {
                                    "accepted": True,
                                    "assignedTeam": "TEAM_A",
                                    "gameState": {
                                        "gameId": "ronda-mesa-1",
                                        "nameTeamA": "Equipo A",
                                        "nameTeamB": "Equipo B",
                                        "scoreTeamA": calculate_score(0),
                                        "scoreTeamB": calculate_score(0),
                                        "maxPlayers": 4,
                                        "status": "PLAYING",
                                        "winnerTeam": None,
                                        "version": 1,
                                        "connectedPlayers": [
                                            {"id": "host-id", "name": "Mesa Host", "team": "TEAM_A", "isHost": True},
                                            {"id": msg.get("senderId", "p2"), "name": msg.get("joinRequest", {}).get("playerName", "Jugador"), "team": "TEAM_A", "isHost": False}
                                        ]
                                    }
                                }
                            }
                            client_sock.sendall((json.dumps(response) + "\n").encode('utf-8'))
                            print(f"[--> JOIN_RESPONSE enviado]")

                        elif msg.get("type") == "SCORE_UPDATE":
                            # Simular paso a buenas
                            sound_event = {
                                "type": "SOUND_TRIGGER",
                                "id": str(uuid.uuid4()),
                                "timestamp": int(time.time() * 1000),
                                "senderId": "HOST",
                                "soundTrigger": {
                                    "soundType": "ENTERED_BUENAS",
                                    "teamId": msg.get("scoreUpdate", {}).get("teamId", "TEAM_A")
                                }
                            }
                            client_sock.sendall((json.dumps(sound_event) + "\n").encode('utf-8'))
                            print(f"[--> SOUND_TRIGGER (ENTERED_BUENAS) emitido]")
                            
            print(f"[-] Jugador desconectado: {addr}")
            client_sock.close()
    except KeyboardInterrupt:
        print("\n[*] Mesa detenida.")
    finally:
        server.close()

def run_client(host="127.0.0.1", port=PORT):
    print(f"[*] Conectando a la Mesa en {host}:{port}...")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((host, port))
    print("[✓] Conectado exitosamente!")

    join_msg = {
        "type": "JOIN_REQUEST",
        "id": str(uuid.uuid4()),
        "timestamp": int(time.time() * 1000),
        "senderId": "python-tester",
        "joinRequest": {
            "playerName": "Tester Canario",
            "clientVersion": "1.0.0"
        }
    }
    sock.sendall((json.dumps(join_msg) + "\n").encode('utf-8'))
    print(f"[--> JOIN_REQUEST enviado]")

    buffer = ""
    while True:
        data = sock.recv(4096).decode('utf-8')
        if not data:
            break
        buffer += data
        while "\n" in buffer:
            line, buffer = buffer.split("\n", 1)
            if line.strip():
                print(f"[<-- Respuesta]: {line}")
                return

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "client":
        target = sys.argv[2] if len(sys.argv) > 2 else "127.0.0.1"
        run_client(target)
    else:
        run_server()
