# El Piedrero 📱🃏🪨
### Marcador Open Source de Piedras y Cantos para la Ronda Canaria

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24%2B-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android CI](https://github.com/DevNaranjo/ElPiedrero/actions/workflows/android.yml/badge.svg)](https://github.com/DevNaranjo/ElPiedrero/actions)

**El Piedrero** es una aplicación móvil nativa de código abierto para Android diseñada para llevar el tanteo de la tradicional **Ronda Canaria** de forma cómoda, automática y 100% offline (sin conexión a Internet).

Permite jugar con un solo teléfono en el centro de la mesa o sincronizar las piedras entre varios dispositivos mediante **Wi-Fi Local y código QR**, reproduciendo los audios auténticos de cada canto (*Ronda, Parranda, Caracol, Caracolillo, Majo, Limpiar, Majo y Limpio y ¡Buenas!*), con historial persistente de las **últimas 30 partidas**.

---

## 📥 Descarga Directa

Si solo quieres instalar y jugar a la Ronda Canaria con tu familia y amigos:
* Descarga el instalador listo para usar: **[`ElPiedrero.apk`](ElPiedrero.apk)**.
* Compatible con cualquier teléfono o tablet con **Android 7.0 (Nougat)** o superior.

---

## ✨ Características Principales

* 📱 **Modalidad Local (Mesa Central):** Un único móvil en el centro de la mesa registra las piedras de todos los equipos. Sin configuración ni Wi-Fi.
* 🌐 **Modalidad Multijugador en Red Local (Wi-Fi + QR):**
  * El anfitrión abre la mesa y genera un código QR.
  * Los jugadores se unen al instante apuntando la cámara, sin teclear direcciones IP.
  * **Seguridad y permisos de equipo:** Cada jugador solo puede sumar piedras a su propio equipo; las tarjetas rivales permanecen bloqueadas para evitar errores o trampas.
  * **Avisos Sonoros Sincronizados:** Al cantar una jugada, el audio suena simultáneamente en todos los móviles de la mesa.
* 📜 **Historial de Partidas (Últimas 30):** Registro automático de las últimas 30 partidas finalizadas con fecha, hora, ganador con 21 piedras y desglose de puntos.
* 👥 **Soporte de Mesas Dinámico:**
  * **2 Jugadores** (1 vs 1).
  * **3 Jugadores** (Trío con 3 equipos independientes: A, B y C).
  * **4 Jugadores** (2 vs 2 por parejas).
* 🛡️ **Seguridad y Rendimiento de Red:**
  * Protocolo seguro NDJSON con protección DoS ante desbordamiento de tramas (>32 KB).
  * Validación estricta de piedras nominales en el servidor Host.
  * Búfer reutilizable en CameraX para evitar recolecciones de basura del Garbage Collector a 60 FPS.

---

## 🃏 Sistema de Puntuación de la Ronda Canaria

La partida se disputa a un total de **21 Piedras**:
* **11 Malas (0 a 11):** Primera fase de la partida.
* **10 Buenas (12 a 21):** Al alcanzar la piedra 11, el equipo pasa automáticamente a **"Buenas"**, disparando el audio de **"¡Buenas!"** y vibración háptica en la mesa.
* **Victoria:** El primer equipo en completar las 10 Buenas (21 piedras totales) gana la partida.

### 🎵 Tabla Oficial de Cantos y Jugadas

| Jugada / Canto | Piedras Sumadas | Descripción | Audio Nativo (`assets/`) |
| :--- | :---: | :--- | :--- |
| **Ronda** | **+1** | Tres cartas del mismo índice / figura. | 🔊 `Ronda.mp3` |
| **Parranda** | **+3** | Tres cartas correlativas de la misma mano. | 🔊 `Parranda.mp3` |
| **Caracol** | **+4** | Cuatro cartas iguales o jugada mayor. | 🔊 `Caracol.mp3` |
| **Caracolillo** | **+5** | Variante tradicional de 5 piedras. | 🔊 `Caracolillo.mp3` |
| **Majo** | **+1** | Casar o majar la carta jugada por el rival. | 🔊 `Majo.mp3` |
| **Limpiar** | **+1** | Dejar la mesa completamente limpia de cartas. | 🔊 `Limpio.mp3` |
| **Majo y Limpio** | **+2** | Casar carta del contrario y limpiar la mesa a la vez. | 🔊 `Majo-y-limpio.mp3` |
| **Ajuste (+ / -)** | **+1 / -1** | Modificación manual de piedras en cualquier momento. | 📳 Tock / Háptico |

---

## 🚀 Registro de Cambios Recientes (v1.0.0)

* 🏷️ **Nueva Identidad "El Piedrero":** Actualización completa de la marca, títulos y manifiesto del sistema.
* 📜 **Historial Persistente de Partidas:** Registro rotativo de las últimas 30 partidas con tarjetas de resultados detalladas (`HistoryScreen.kt` y `GameHistoryRepository.kt`).
* 🃏 **Nuevas Jugadas Tradicionales:** Incorporación de *Limpiar (+1)*, *Majo (+1)*, *Majo y Limpio (+2)* y variante *Caracolillo (+5)* con sus audios y hápticos asociados.
* 🛡️ **Blindaje de Red y Anti-Trampas:** Límite estricto de 32 KB por trama en sockets TCP contra ataques DoS y validación de puntuaciones nominales en el servidor Host.
* ⚡ **Optimización de Rendimiento:** Búfer reutilizable en CameraX para eliminar pausas del Garbage Collector a 60 FPS y renderizado JNI en bloque para la generación del QR.
* 🧪 **Pruebas Automatizadas y CI/CD:** Suite de tests unitarios de reglas (`TeamScoreTest.kt`) y pipeline de GitHub Actions para integración continua.
* 🌍 **Lanzamiento Open Source:** Repositorio estructurado bajo Licencia MIT con `CONTRIBUTING.md` y compilación portable con Gradle Toolchains.

---

## 🏗️ Arquitectura y Tecnologías

El proyecto sigue los principios de **Clean Architecture** y **MVVM/MVI reactivo**:

```
app/src/main/java/com/app/rondacanaria/
├── data/
│   ├── audio/        # RondaAudioPlayer (reproducción con fallback sintético y hápticos)
│   ├── history/      # GameHistoryRepository (persistencia JSON offline de 30 partidas)
│   ├── model/        # Modelos de red, paquetes NDJSON, CantoType, TeamScore
│   └── network/      # SocketServer, SocketClient y utilidades de red local
├── domain/
│   ├── model/        # ConnectionInfo (datos de sala en código QR)
│   └── usecase/      # HostGameUseCase y ClientGameUseCase (lógica de red y partida)
└── ui/
    ├── qr/           # QrCodeGenerator (optimizado con JNI) y QrCameraScanner (CameraX)
    ├── screens/      # ModeSelection, Lobby, HostLobby, Scanner, Scoreboard, History
    ├── MainActivity.kt
    └── ScoreViewModel.kt
```

---

## 🛠️ Compilación y Desarrollo

### Requisitos
* Android Studio Iguana (2023.2.1) o superior.
* JDK 17 (resuelto automáticamente mediante Gradle Toolchains).

### Comandos de Terminal
```bash
# Clonar el repositorio
git clone https://github.com/DevNaranjo/ElPiedrero.git
cd ElPiedrero

# Ejecutar la suite de pruebas unitarias
./gradlew test

# Compilar el APK en modo Debug
./gradlew assembleDebug
```

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Consulta la [Guía de Contribución](CONTRIBUTING.md) para conocer las pautas de estilo y el flujo de Pull Requests.

---

## 📄 Licencia

Este proyecto está bajo la Licencia **MIT** a nombre de **DevNaranjo (2026)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
