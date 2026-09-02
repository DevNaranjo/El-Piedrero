# El Piedrero 📱🃏🪨
### Marcador Open Source de Piedras y Cantos para la Ronda Canaria

[🇪🇸 Español](README.md) • [🇬🇧 English](README_EN.md)

[![Versión](https://img.shields.io/badge/Versión-1.0.02092026-orange.svg)](https://github.com/DevNaranjo/ElPiedrero/releases)
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
* **Verificación de Integridad (SHA-256):**
  ```text
  0484B51C4453376F233AFADA531DEC4F12B68BFFF6A7014351EC774D0546863A
  ```
  *(En PowerShell: `Get-FileHash ElPiedrero.apk -Algorithm SHA256` | En Linux/macOS: `sha256sum ElPiedrero.apk`)*
* **Firma Oficial:** Certificado digital RSA 2048 bits a nombre de `DevNaranjo`.

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
* 🛡️ **Seguridad Criptográfica y Red Local:**
  * **Cifrado AES-256-GCM Extremo a Extremo:** Toda la comunicación por Sockets TCP en la Wi-Fi local viaja cifrada con clave simétrica aleatoria de 256 bits generada por el Host y compartida únicamente a través del código QR físico.
  * **Autenticación con Token de Sala:** Solo los jugadores que hayan escaneado presencialmente el código QR del anfitrión poseen el token de autorización para unirse.
  * **Firma de Producción Oficial:** Compilado en modo Release y firmado con certificado propio de DevNaranjo (sin certificados genéricos de depuración ni flag `testOnly`).
  * **Protocolo con Protección DoS:** Límite estricto de 32 KB por trama contra desbordamientos de memoria.
  * **Búfer Reutilizable en CameraX:** Elimina pausas del Garbage Collector a 60 FPS al escanear el QR.

---

## 🃏 Sistema de Puntuación de la Ronda Canaria

La partida se disputa a un total de **21 Piedras**:
* **11 Malas (0 a 11):** Primera fase de la partida.
* **10 Buenas (12 a 21):** Al alcanzar la piedra 11, el equipo pasa automáticamente a **"Buenas"**, disparando el audio de **"¡Buenas!"** y vibración háptica en la mesa.
* **Victoria:** El primer equipo en completar las 10 Buenas (21 piedras totales) gana la partida.

### 🎵 Tabla Oficial de Cantos y Jugadas

| Jugada / Canto | Piedras Sumadas | Descripción | Audio Nativo (`assets/`) |
| :--- | :---: | :--- | :--- |
| **Ronda** | **+1** | 2 cartas iguales en la mano. | 🔊 `Ronda.mp3` |
| **Parranda** | **+3** | 3 cartas iguales en la mano. | 🔊 `Parranda.mp3` |
| **Caracol** | **+4** | 3 cartas correlativas de la mano. | 🔊 `Caracol.mp3` |
| **Caracolillo** | **+5** | 3 cartas correlativas del mismo palo. | 🔊 `Caracolillo.mp3` |
| **Majo** | **+1** | Tirar una carta igual que la que acaba de tirar la persona anterior. | 🔊 `Majo.mp3` |
| **Limpiar** | **+1** | Recoger y dejar la mesa completamente limpia de cartas. | 🔊 `Limpio.mp3` |
| **Majo y Limpio** | **+2** | Tirar carta igual que el contrario y limpiar la mesa a la vez. | 🔊 `Majo-y-limpio.mp3` |
| **Ajuste (+ / -)** | **+1 / -1** | Modificación manual de piedras en cualquier momento. | 📳 Tock / Háptico |

---

## 🚀 Registro de Cambios — v1.0.02092026.2

### 🆕 Últimas Mejoras (v1.0.02092026.2)

* 🛡️ **Blindaje de Red y Seguridad en Sockets:** Descarte automático en el Host de comandos remotos no autorizados (`ROOM_CONFIG_UPDATE` y `END_GAME`) emitidos por clientes. La configuración de sala y cierre de mesa queda restringida exclusivamente a la consola local del Anfitrión, mientras que la salida de un cliente se gestiona como desconexión limpia individual sin interrumpir la partida al resto de participantes.
* 🔒 **Política de Tráfico de Red Estricto:** Eliminado `android:usesCleartextTraffic` e incorporado `network_security_config.xml` con `cleartextTrafficPermitted="false"`, bloqueando tráfico HTTP no cifrado residual mientras la comunicación local P2P se mantiene protegida con AES-256-GCM.
* ⚡ **Optimización de Audio Directo (Zero-Disk-IO):** `RondaAudioPlayer` reproduce los efectos y cánticos directamente desde el descriptor nativo del APK (`AssetFileDescriptor`) mediante streaming en memoria, eliminando escrituras y copias temporales en el almacenamiento flash del dispositivo (`cacheDir`).
* 🚀 **Memoización de Historial en Compose:** Implementado `remember(history)` en la agrupación y ordenación de jugadas por reparto en `ScoreBoardScreen.kt`, previniendo recálculos innecesarios de mapas y ordenaciones durante recomposiciones y animaciones.
* 🧹 **Limpieza del Ciclo de Vida en ViewModel:** Parada explícita y liberación de escuchadores de sockets (`ServerSocket.close()`) y corrutinas de red en `ScoreViewModel.onCleared()` para prevenir fugas de recursos al salir de la app.
* 📐 **Botonera de Jugadas Refactorizada (2 Columnas Simétricas):** Distribución uniforme de las 10 jugadas tradicionales en 5 filas estrictas de 2 columnas (`Modifier.weight(1f)`), con altura normalizada de `48.dp`, esquinas uniformes `RoundedCornerShape(12.dp)` y prevención de deformaciones (`maxLines = 1` y `TextOverflow.Ellipsis`).
* ♿ **Accesibilidad y Touch Target en Sumar y Restar (48 dp):** Los controles de ajuste manual (`-`, `+`, `+N`) dentro de las tarjetas de puntuación se integraron en una barra horizontal continua al 100% de ancho con `Modifier.weight(1f)`, altura mínima usable de `48.dp` (`defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)`) y tipografía `titleMedium` (`20.sp` / `16.sp`) en negrita centrada, garantizando un área de toque cómoda y accesible especialmente para personas mayores.
* 🃏 **Normalización del Stepper de Reparto de Cartas:** Los controles del contador de manos (`-` y `+`) se elevaron a `44.dp × 44.dp` con `FilledTonalIconButton` y `FilledIconButton`, y el badge central ordinal (`"Xº / Y"`) cuenta con `padding(horizontal = 14.dp)` para una lectura holgada.
* 🔊 **Canal de Audio Independiente para Piedras y Buenas:** Se desacopló la reproducción de efectos de sonido de piedras (`PIEDRA_ADD`, `PIEDRA_SUBTRACT`) del reproductor de voz principal. Al sumar o restar piedras mientras se reproduce el audio de «¡Buenas!», el sonido de la piedra se emite de manera concurrente en un canal paralelo sin cortar ni interrumpir el audio de Buenas.
* ⚙️ **Firma y CI/CD en GitHub Actions:** Configuración automática en `app/build.gradle.kts` y el workflow de GitHub Actions para autogenerar el keystore de debug cuando no exista un certificado release privado en el runner, garantizando builds exitosos en cualquier entorno CI.
* 📱 **Acciones Inferiores Limpias (50% / 50%):** Fila inferior fija con «Deshacer» y «Terminar Partida» dividida al 50% cada una con `Modifier.weight(1f)` y altura de `50.dp`. Retirados los botones inferiores redundantes de registro y suma rápida.

### 🏷️ Mejoras Anteriores (v1.0.02092026)

* 🏷️ **Nueva Identidad «El Piedrero»:** Actualización completa de la marca, títulos y manifiesto del sistema.
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
