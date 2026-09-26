# El Piedrero 📱🃏🪨
### Marcador Open Source de Piedras y Cantos para la Ronda Canaria

[🇪🇸 Español](README.md) • [🇬🇧 English](README_EN.md)

[![Versión](https://img.shields.io/badge/Versión-v1.1--Beta.2%20(Code%207)-brightgreen.svg)](https://github.com/DevNaranjo/El-Piedrero/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24%2B-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android CI](https://github.com/DevNaranjo/El-Piedrero/actions/workflows/android.yml/badge.svg)](https://github.com/DevNaranjo/El-Piedrero/actions)

**El Piedrero** es una aplicación móvil nativa de código abierto para Android diseñada para llevar el tanteo de la tradicional **Ronda** de forma cómoda, automática y 100% offline (sin conexión a Internet).

Permite jugar con un solo teléfono en el centro de la mesa o sincronizar las piedras entre varios dispositivos mediante **Wi-Fi Local y código QR**, reproduciendo los audios auténticos de cada canto (*Ronda, Parranda, Caracol, Caracolillo, Majo, Limpiar, Majo y Limpio, ¡Buenas! y De bufos*), con historial persistente de las **últimas 30 partidas**.

---

## 🚀 Novedades de la Versión 1.1-Beta.2

> **Actualización enfocada en optimizar y pulir la interfaz gráfica del marcador:** animaciones fluidas en la transición de piedras malas a buenas, mejoras de accesibilidad táctil para personas mayores, microinteracciones hápticas avanzadas y personalización de temas y avatares de equipo.

* 📖 **Recorrido Guiado Interactivo (Onboarding) y Menú de 3 Puntos (⋮):**
  * **Tutorial paso a paso:** Recorrido visual y accesible para aprender a usar la app, las reglas del tanteo (malas, buenas, chicos), cantos y turnos de reparto. Se ejecuta automáticamente en el primer inicio.
  * **Acceso permanente:** Disponible en cualquier momento desde el menú de 3 puntos (⋮), en la barra superior y desde el diálogo de Ajustes y Accesibilidad.
  * **Indicador de Versión:** Visualización clara de la versión y compilación de la app al pie del menú de inicio.
* 🎭 **Sistema de Avatares Temáticos Canarios (13 Avatares Autóctonos) y Accesibilidad Táctil:**
  * **Visualización en el Marcador:** Cada tarjeta de equipo (`ModernPlayerScoreCard`) muestra los avatares superpuestos de sus integrantes durante la partida.
  * **Chip de Perfil en Pantalla Principal:** Acceso directo con un solo toque desde la pantalla de inicio para cambiar de avatar sin necesidad de entrar a una sala multijugador.
  * **Ergonomía Táctil en Partida Local:** Botones circulares táctiles grandes (46 dp) independientes del campo de texto, eliminando cualquier conflicto con el teclado virtual de Android.
  * **Catálogo identitario de 13 figuras:** *El Piedrero 🪨, El Tahúr 🃏, El Mago 👒, El Lagarto 🦎, El Bardino 🐕, El Mencey 👑, El Palmero 🌴, El Costero 🎣, El Majo 🥣, El Guayota 🌋, El Cernícalo 🦅, La Romera 💃 y El Sabio 🧔*.
  * **Insignias reactivas (`PlayerAvatarBadge`):** Borde coloreado según el equipo (Equipo A, B, C, D, Reserva o Espectador) con distintivos dinámicos de Líder 👑 y Repartidor 🃏.
  * **Selector interactivo (`AvatarSelectionDialog`):** Selección ágil tanto en multijugador como en local para cada jugador (mesas de 2, 3, 4, 6 y 8).
  * **Persistencia y sincronización:** Guardado local de perfil (`UserProfilePersistence`) y difusión en tiempo real vía WebSocket (`UPDATE_PLAYER_PROFILE`).
* ⚡ **Panel "Modo Dios" para el Anfitrión Multijugador (`GodModeHostDialog`):**
  * Consola de arbitraje en vivo para el anfitrión: ajuste de piedras y chicos de cada equipo, cambio de repartidor o mano, forzado de estados de partida, reasignación de equipos y botón de desatasque para recuentos interrumpidos.
* ⏩ **Recuento Libre y Botón "Pasar sin Contar" en Última Mano:**
  * El anfitrión y los líderes de equipo pueden gestionar el recuento independientemente de quién reparta la baraja (en local y multijugador).
  * Nuevo botón seguro *"Pasar sin contar"* en el último reparto para avanzar de mano rápidamente sin forzar el conteo de cartas.
* 👥 **Sala de Espera Reactiva en Tiempo Real:**
  * Notificaciones visuales de estado: banners animados con *"Esperando jugadores"*, alertas en vivo de conexión (*"[Nombre] uniéndose..."*) y confirmación de *"Mesa completa"*.
  * Huecos visuales (slots) con bordes punteados que representan las plazas libres de la mesa.
* 🎵 **Pista BGM 07, Mezcla Aleatoria Inteligente y Estabilidad de Muestreo:**
  * Nueva pista tradicional canaria `bgm_07.mp3` (*Círculo de Infantes*).
  * Ciclo de reproducción aleatoria inteligente con memoria (`playedIndices`) para reproducir todo el repertorio sin repeticiones consecutivas.
  * Estandarización estricta de frecuencia de muestreo a 44.100 Hz y 128 kbps CBR en el 100% de los audios, erradicando desfases de reloj en el hardware ("efecto ardilla").
  * Corrección del orden de inicialización en el ciclo de vida del reproductor de audio en Kotlin.
* 🎵 **Consolidación Integral y Estabilidad de Audio (Arquitectura MVP en RAM):**
  * **Reproducción 100% en Memoria RAM (`MediaDataSource`):** Se restaura la arquitectura original del MVP que alimenta al `MediaPlayer` desde un búfer en RAM con tamaño exacto de fin de fichero (`getSize()`), eliminando escrituras y lecturas de disco concurrentes en caché.
  * **Erradicación de aceleraciones y distorsión:** Se elimina el desbordamiento de límites de tramas MP3 en el contenedor APK (`openFd`) y la manipulación de `playbackParams` (evitando el filtro DSP *Sonic* de time-stretching), garantizando velocidad 1.0x nativa sin pulsos ni chasquidos.
  * **Transición atómica y debouncing:** Control estricto secuencial en hilo de audio dedicado que cancela y anula los listeners del reproductor saliente antes de iniciar el siguiente, eliminando solapamientos al pulsar repetidamente *"Saltar canción"*.
  * **Ciclo de vida seguro:** Métodos seguros de liberación (`safeRelease()`, `safeIsPlaying()`, `safeSetVolume()`) y anulación de callbacks que previenen estados ilegales (`IllegalStateException`).
* 👑 **Audio Contextual "De bufos" (`De-bufos.mp3`):**
  * Integración del audio oficial `De-bufos.mp3` en memoria RAM al pulsar el botón contextual *"De bufos"*, sincronizado en red local mediante `SoundType.CANTO_BUFOS` para anfitrión y clientes.
* 🌓 **Transición Suave y Sincronizada entre Modo Claro y Oscuro:**
  * Animación fluida de todos los atributos de `ColorScheme` Material 3 y componentes personalizados con curva `FastOutSlowInEasing` sobre 380 ms.
* 👥 **Flexibilidad en Multijugador (Cambio de Equipo para Anfitrión y Líder):**
  * El anfitrión de la sala y el líder de la partida ahora pueden alternar y cambiar de equipo directamente desde la sala de espera multijugador.
* 🎴 **Interfaz Moderna y Persistencia Reforzada:**
  * Implementación de `ModernPlayerScoreCard`, `FloatingControlDock`, `ReactiveCantosGrid` y `ActiveGameDataStore` para una experiencia de tanteo más visual y persistencia robusta ante cierres inesperados.

---

## 📥 Descarga Directa

Si deseas instalar y jugar a la Ronda Canaria con tu familia y amigos:
* Descarga el instalador oficial listo para usar desde la sección de **[Releases de GitHub](https://github.com/DevNaranjo/El-Piedrero/releases)** (**v1.1-Beta.2 / versionCode 7**).
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

