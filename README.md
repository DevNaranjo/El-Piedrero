# El Piedrero 📱🃏🪨
### Marcador Open Source de Piedras y Cantos para la Ronda Canaria

[🇪🇸 Español](README.md) • [🇬🇧 English](README_EN.md)

[![Versión](https://img.shields.io/badge/Versión-v1.0--stable%20(Code%201)-brightgreen.svg)](https://github.com/DevNaranjo/El-Piedrero/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24%2B-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android CI](https://github.com/DevNaranjo/El-Piedrero/actions/workflows/android.yml/badge.svg)](https://github.com/DevNaranjo/El-Piedrero/actions)

**El Piedrero** es una aplicación móvil nativa de código abierto para Android diseñada para llevar el tanteo de la tradicional **Ronda** de forma cómoda, automática y 100% offline (sin conexión a Internet).

Permite jugar con un solo teléfono en el centro de la mesa o sincronizar las piedras entre varios dispositivos mediante **Wi-Fi Local y código QR**, reproduciendo los audios auténticos de cada canto (*Ronda, Parranda, Caracol, Caracolillo, Majo, Limpiar, Majo y Limpio y ¡Buenas!*), con historial persistente de las **últimas 30 partidas**.

---

## 📥 Descarga Directa

Si deseas instalar y jugar a la Ronda Canaria con tu familia y amigos:
* Descarga el instalador oficial listo para usar desde la sección de **[Releases de GitHub](https://github.com/DevNaranjo/El-Piedrero/releases)** (**v1.0-stable / versionCode 1**).
* Compatible con cualquier teléfono o tablet con **Android 7.0 (Nougat)** o superior (API 24+).
* **Seguridad y Verificación:** Cada release incluye los archivos `.apk` y Android App Bundle (`.aab`) optimizados mediante ofuscación R8, acompañados de su correspondiente firma digital y archivo `checksums.txt` con los resúmenes criptográficos SHA-256 oficiales.
* **Firma Oficial:** Certificado digital emitido a nombre de `DevNaranjo`.

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
  * **6 Jugadores** (3 equipos de 2 con rotación de reservas).
  * **8 Jugadores** (4 equipos de 2 con rotación de reservas).
* 🎛️ **Distribución de Botones Personalizable:** Permite reorganizar el orden de la botonera de cantos a gusto del usuario y guardarla de forma persistente.
* 🃏 **Gestión Inteligente de Reparto y Cartas a la Mesa:** Indicador visible del jugador que reparte, sugerencia del siguiente repartidor al completar cada ciclo y diálogo interactivo oficial para el recuento de las 4 cartas a la mesa en el primer reparto.
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
| **Contramajo** | **+2** | Majo en respuesta al majo del oponente. | 🔊 `Contra-majo.mp3` |
| **Requetemajo** | **+3** | Majo en respuesta al contramajo. | 🔊 `Requetemajo.mp3` |
| **Sobremajo** | **+4** | Cuarto majo consecutivo en mesa. | 🔊 `Sobremajo.mp3` |
| **Ajuste (+ / -)** | **+1 / -1** | Modificación manual de piedras en cualquier momento. | 📳 Tock / Háptico |


## 🏗️ Arquitectura y Tecnologías

El proyecto sigue los principios de **Clean Architecture** y **MVVM/MVI reactivo**:

```
app/src/main/java/com/app/rondacanaria/
├── data/
│   ├── audio/        # RondaAudioPlayer (búfer de audio prioritario, MediaPlayer y SoundPool)
│   ├── history/      # GameHistoryRepository y ButtonLayoutPersistence (persistencia JSON offline)
│   ├── model/        # Modelos de red, paquetes NDJSON, CantoType, TeamScore, GameState
│   └── network/      # SocketServer, SocketClient, cifrado AES-256-GCM y utilidades TCP
├── domain/
│   ├── model/        # ConnectionInfo (datos de sala para código QR)
│   └── usecase/      # HostGameUseCase y ClientGameUseCase (gestión de sala, roles, turnos y reglas)
└── ui/
    ├── components/   # Diálogos: MesaCardsDealDialog, AudioSettingsDialog, CustomizeButtonsDialog, etc.
    ├── qr/           # QrCodeGenerator (optimizado con JNI) y QrCameraScanner (CameraX)
    ├── screens/      # ModeSelection, Lobby, HostLobby, Scanner, Scoreboard, History
    ├── MainActivity.kt
    └── ScoreViewModel.kt
```

---

## 🛠️ Instrucciones de Ejecución y Mantenimiento

### Requisitos del Entorno
* **Android Studio:** Iguana (2023.2.1) o superior (Ladybug / Koala recomendados).
* **JDK:** Versión 17 (gestionada automáticamente mediante Gradle JVM Toolchains).
* **Android SDK:** Compile SDK 34, Min SDK 24 (Android 7.0+).

### Comandos de Ejecución y Compilación
```bash
# 1. Clonar el repositorio
git clone https://github.com/DevNaranjo/El-Piedrero.git
cd El-Piedrero

# 2. Ejecutar la suite completa de pruebas unitarias
# (Linux / macOS / PowerShell):
./gradlew testDebugUnitTest
# (Windows CMD):
gradlew testDebugUnitTest

# 3. Compilar el APK en modo Debug (instalación y pruebas directas)
./gradlew assembleDebug

# Ubicación del APK generado:
# app/build/outputs/apk/debug/app-debug.apk

# 4. Compilar el APK y Bundle de Producción (Release)
./gradlew assembleRelease bundleRelease

# Ubicación del APK Release firmado:
# app/build/outputs/apk/release/app-release.apk
```

### Directrices de Mantenimiento
1. **Reglas de Juego Inmutables:** Los cantos tradicionales y las 21 piedras (11 malas + 10 buenas) son el núcleo de la Ronda Canaria y deben permanecer matemáticamente consistentes en `TeamScore` y `HostGameUseCase`.
2. **Ciclo de Repartos:** El diálogo `MesaCardsDealDialog` y su botón asociado deben mostrarse únicamente cuando `currentDeal == 1`. En los repartos subsiguientes no deben presentarse opciones de mesa.
3. **Persistencia Local:** Los estados locales guardados en SharedPreferences mediante `LocalSavedGame` deben migrarse o inicializarse siempre con `status = GameStatus.PLAYING` para evitar bloqueos en la interfaz del marcador.
4. **Seguridad en Red P2P:** Todo mensaje nuevo en el protocolo NDJSON debe registrarse en `NetworkMessage.kt` y transmitirse con el sobre `NetworkEnvelope` bajo el cifrado AES-256-GCM.

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Consulta la [Guía de Contribución](CONTRIBUTING.md) para conocer las pautas de estilo y el flujo de Pull Requests.

---

## 📄 Licencia

Este proyecto está bajo la Licencia **MIT** a nombre de **DevNaranjo (2026)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

## 🔒 Privacidad y Protección de Datos

Este proyecto opera bajo un modelo 100% offline y P2P con **cero recopilación de datos**. Para consultar el tratamiento de permisos, almacenamiento y política legal completa, revisa [PRIVACY_AND_DATA_USAGE.md](PRIVACY_AND_DATA_USAGE.md).

