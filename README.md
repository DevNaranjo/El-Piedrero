# El Piedrero 📱🃏🪨
### Marcador Open Source de Piedras y Cantos para la Ronda Canaria

[🇪🇸 Español](README.md) • [🇬🇧 English](README_EN.md)

[![Versión](https://img.shields.io/badge/Versión-1.0.5-orange.svg)](https://github.com/DevNaranjo/El-Piedrero/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24%2B-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android CI](https://github.com/DevNaranjo/El-Piedrero/actions/workflows/android.yml/badge.svg)](https://github.com/DevNaranjo/El-Piedrero/actions)

**El Piedrero** es una aplicación móvil nativa de código abierto para Android diseñada para llevar el tanteo de la tradicional **Ronda Canaria** de forma cómoda, automática y 100% offline (sin conexión a Internet).

Permite jugar con un solo teléfono en el centro de la mesa o sincronizar las piedras entre varios dispositivos mediante **Wi-Fi Local y código QR**, reproduciendo los audios auténticos de cada canto (*Ronda, Parranda, Caracol, Caracolillo, Majo, Limpiar, Majo y Limpio y ¡Buenas!*), con historial persistente de las **últimas 30 partidas**.

---

## 📥 Descarga Directa

Si deseas instalar y jugar a la Ronda Canaria con tu familia y amigos:
* Descarga el instalador oficial listo para usar desde la sección de **[Releases de GitHub](https://github.com/DevNaranjo/El-Piedrero/releases/tag/v1.0.5)**.
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

---

## 🚀 Registro de Cambios
 
### 🃏 El Piedrero — v1.0.5 (04/09/2026 - Patch 1) — 🪨

* 📺 **Transmisión en Directo a Pantallas (TV Cast):** Nuevo diálogo interactivo (`TvCastDialog.kt`) para proyectar el tanteador en Smart TVs o monitores externos mediante Google Cast / Pantalla Inalámbrica de Android, con optimización automática permanente de audio en TV.
* 💾 **Gestión Inteligente del Ciclo de Vida y Persistencia:**
  * **Al minimizar la app:** La partida en curso se mantiene al 100% en memoria (`launchMode="singleTask"`).
  * **Al cerrar la app (deslizar en recientes):** Detección nativa con `AppCleanupService.kt` y `onTaskRemoved()` que limpia la partida y retorna limpiamente al menú de inicio.
  * **Silenciado inmediato:** Corte instantáneo de música al abrir la vista de aplicaciones recientes o cambiar de ventana (`onPause` y `onStop`).
* 🎼 **Música Ambiental Folclórica Generada por IA (Aleatoria):**
  * Incorporación de 6 piezas instrumentales originales inspiradas en el folclore canario (`bgm_01.mp3` a `bgm_06.mp3`), 100% libres de derechos fonográficos y de gestión colectiva (SGAE).
  * Reproducción aleatoria (`pickNextRandomIndex()`) sin repeticiones consecutivas y documentación legal bilingüe en `MUSIC_LICENSES.md` y en el diálogo de privacidad de la app.
* 🌐 **Multijugador Sincronizado y Reconexión de 5 Minutos:**
  * Sincronización instantánea de piedras en tiempo real entre anfitrión y clientes.
  * Margen de gracia de 5 minutos con cuenta atrás regresiva visible antes de abandonar la sala si se minimiza la app o hay microcortes Wi-Fi.
* 🃏 **Recuento de Cartas Automatizado y Seguro:**
  * Cálculo de diferencia automático en partidas locales para completar la baraja de 40 cartas.
  * Bloqueo estricto del botón de confirmación si la suma de cartas es insuficiente, informando exactamente cuántas cartas faltan.
* 🏆 **Suma Rápida Superior a 21 Piedras:** Posibilidad de sumar tanteos libres tras el recuento con aviso dinámico de victoria (*"¡Ganarás por X piedras de más!"*).
* 👁️ **Accesibilidad y Encabezado Compacto:**
  * Bloqueo de `fontScale = 1.0f` para evitar desbordamientos de interfaz ante configuraciones de fuentes gigantes de Android, manteniendo un formato responsive.
  * Ajuste de la barra superior `TopAppBar` en el marcador con botones de acción compactos (38 dp) y protección de texto en una sola línea para que "El Piedrero 🃏" luzca perfecto y centrado.
* 🛡️ **DevSecOps y Seguridad de Publicación:** Eliminación de contraseñas y keystores del árbol del proyecto, ofuscación R8 activada en release y protección reforzada en `.gitignore`.

### 🏷️ Persistencia de Registro por Manos, Interfaz Compacta y Frases Tradicionales (v1.0.03.092026.4)

* 📜 **Historial Permanente de la Partida ("Registro X"):** Las jugadas y puntos ya no se borran al finalizar las manos o realizar el recuento. Cada mano concluida se preserva de forma permanente catalogada como `Registro X (Guardado)`, mientras que la mano en juego se muestra como `Registro X (En curso)`. Se incluye selector de filtros rápidos (`Todos`, `Registro 1`, `Registro 2`...) y etiquetas por jugada (`Reg. X · R Y`).
* 👥 **Personalización Compacta de Equipos y Jugadores:** Rediseño completo del diálogo de partida local para 4, 6 y 8 jugadores. Se agrupan los participantes directamente en tarjetas compactas por equipo con los dos jugadores en dos columnas paralelas, eliminando espacios muertos, scrolls excesivos y recortes de botones.
* 🎯 **Refinamiento de Frases Tradicionales en Buenas:** Eliminación de la coletilla redundante *"para ganar"* en todas las frases de cuenta atrás. Incorporadas las frases tradicionales para 2 piedras (*"A falta de ronda de bufos"*), 6 piedras (*"A falta de un caracol de bufos"*) y 7 piedras (*"A falta de un caracolillo de bufos"*).
* 🎴 **Estandarización de Simbología:** Sustitución universal del icono por el emoji tradicional de baraja `🃏` en avisos, repartidor y pantallas de recuento.
* 📊 **Control y Aviso de Recuento Manual de Cartas:** Diálogo con verificación estricta del mazo de 40 cartas; si la suma acumulada supera dicho total, se despliega una pantalla de aviso que solicita y facilita el recuento manual de cartas.
* 🧹 **Limpieza Visual de Interfaz:** Eliminación de textos amarillos redundantes en el tanteador de todos los modos de juego (tanto local como multijugador).

### 🆕 Experiencia de Conexión, Audio Dinámico y Ajustes Visuales (v1.0.03.092026.2)

* 🔄 **Flujo de Escaneo QR y Desvinculación Inmediata de Cámara:** Cierre y liberación instantánea del hardware de la cámara (`unbindAll`) en el momento exacto en que se reconoce el código QR, previniendo el sobreuso de recursos y batería. La aplicación pasa automáticamente a una pantalla de carga dedicada (`ConnectionLoadingView`) que informa al usuario que se está sincronizando con la mesa.
* 🛡️ **Privacidad de Red y Diagnóstico Amigable sin IP:** Eliminación absoluta de direcciones IP y puertos en mensajes de error visibles. En caso de fallo de conexión, se presenta la pantalla `ConnectionErrorView` indicando el nombre del anfitrión (*"No se pudo conectar a la mesa de [Anfitrión]"*) con sugerencias claras de conectividad (Wi-Fi o Zona Wi-Fi) y opciones para reintentar el escaneo o volver al menú.
* 🔘 **Visualización Estable de Botones en Buenas:** Optimización del diseño en `RondaScoreCard`: el indicador de "En Buenas" se compacta en una sola línea protegida (`maxLines = 1`), se ajusta la escala del contador de piedras y se fija una altura mínima garantizada de 44 dp en los botones de ajuste (`+`, `−`, `+N`), evitando que se encojan o queden fuera de pantalla en teléfonos físicos con fuentes grandes o paneles estrechos.
* 🔊 **Volumen de Piedras Proporcional y Sincronizado:** El sonido acústico de sumar y restar piedras (`ToneGenerator`) ahora responde en tiempo real a los deslizadores de volumen general y de efectos.
* 🔉 **Volúmenes Iniciales Moderados:** Reducción de los valores de volumen por defecto de la aplicación (General al 80%, Efectos al 70%, Música al 35%) y atenuación de la ganancia máxima de piedras para evitar sonidos estridentes.
* 📳 **Control de Respuesta Háptica:** Nuevo interruptor de **Vibración** en el diálogo de ajustes de audio, permitiendo silenciar o activar la respuesta háptica de toques, cantos y victorias según la preferencia del jugador.
* ⚠️ **Avisos de Confirmación en Multijugador:**
  * Alerta de confirmación al anfitrión al iniciar una partida cuando aún faltan jugadores por unirse a la sala (*"No, esperar"* / *"Sí, empezar"*).
  * Diálogo de confirmación al salir de la sala (avisando del cierre de sala para el anfitrión o del abandono de la mesa para los clientes unidos).

### 🏷️ Auditoría de Calidad y Preparación para Producción (v1.0.03092026)

* 🛡️ **DevSecOps y Gestión Segura de Credenciales:** Eliminación total de contraseñas y almacenes de claves en texto plano del código fuente (`app/build.gradle.kts`). Las credenciales de firma se consumen dinámicamente mediante variables de entorno en el pipeline de CI/CD.
* 📦 **Ofuscación R8 y Reducción de Tamaño:** Habilitación de `isMinifyEnabled = true` y `isShrinkResources = true` con reglas ProGuard adaptadas para Compose, ZXing y serialización Kotlinx. El tamaño del APK en release se reduce a **8.2 MB** (reducción >40%).
* 🔒 **Seguridad de Red Local y Protección Anti-Replay:** Incorporación de un contador monotónico de secuencia (`sequenceNumber`) y validación de caducidad temporal de tramas (<60s) en `NetworkEnvelope`, neutralizando ataques de reinyección maliciosa en redes locales.
* 🔄 **Resiliencia P2P y Recuperación de Asientos:** Implementada ventana de gracia durante la desconexión física de clientes en partidas activas (`assignedPlayerSeats`). Los jugadores reconectados con el mismo identificador recuperan su asiento y tanteo sin reinicialización ni rebalanceo involuntario.
* 🎧 **Audio Focus Nativo y Convivencia con el Sistema:** Integración de `AudioFocusRequest` con `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` en `RondaAudioPlayer`, garantizando atenuación suave de la música ambiental y purga automática de ficheros de audio en la caché temporal.
* 🎼 **Legalidad y Música Tradicional Libre de Derechos:** Sustitución de piezas comerciales por composiciones instrumentales tradicionales canarias de dominio público / CC0 (`bgm_FolkCanario_Isas.wav` y `bgm_FolkCanario_Folias.wav`) debidamente documentadas en `MUSIC_LICENSES.md`.
* 📜 **Atribución de Licencias Open Source:** Diálogo interactivo accesible desde la pantalla principal (`LicensesDialog.kt`) que acredita el cumplimiento de la cláusula 4 de la Licencia Apache 2.0 (ZXing, AndroidX, Jetpack Compose).
* ♿ **Accesibilidad Integral (WCAG 2.1 AA / AAA):**
  * **Dimensiones táctiles universales:** Todos los elementos interactivos (ajuste de piedras, cantos, stepper de reparto a 48 dp, botones de sala y modales) cumplen con el estándar mínimo de **48×48 dp** (`defaultMinSize(48.dp, 48.dp)`).
  * **Alto contraste visual:** Insignias de victorias y estado "En Buenas" optimizadas a ratios de contraste superiores a **10:1** (superando WCAG AAA) con tipografías legibles (≥ 12–14 sp) para visión bajo luz solar exterior.
  * **Compatibilidad TalkBack:** Descripciones semánticas completas y contextualizadas en español en todos los botones e iconos de control.
* 📱 **Fijación de Orientación Vertical:** Bloqueo explícito en `portrait` en `AndroidManifest.xml` para garantizar estabilidad de interfaz y cámara durante partidas físicas en mesa.
* 🧪 **Suite de Pruebas Unitarias Ampliada:** Nueva suite `HostGameUseCaseTest.kt` validando límites de 21 piedras, transición de malas a buenas, reversión de victoria y reparto de cartas.
* ⚙️ **Pipeline CI/CD Robusto:** Flujo en GitHub Actions actualizado con análisis `lintVitalRelease`, compilación de APK y Android App Bundle (`bundleRelease`) y generación automatizada de resúmenes criptográficos SHA-256 (`checksums.txt`).

### 🏷️ Mejoras Anteriores

* 🏷️ **Identidad Oficial «El Piedrero»:** Sistema de diseño, nombres canarios aleatorios y soporte multi-equipo (A, B, C y D).
* 📜 **Historial Persistente de Partidas:** Registro rotativo de las últimas 30 partidas con tarjetas de resultados detalladas (`HistoryScreen.kt` y `GameHistoryRepository.kt`).
* 🃏 **Cantos Autóctonos Integrados:** Cobertura de la totalidad de cánticos y jugadas de mesa canarias con locuciones nativas sincronizadas.
* 🛡️ **Blindaje de Sockets TCP:** Cifrado simétrico AES-256-GCM con claves efímeras por QR y delimitación de tramas seguras a 32 KB.

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
git clone https://github.com/DevNaranjo/El-Piedrero.git
cd El-Piedrero

# Ejecutar la suite de pruebas unitarias
# (En Linux, macOS o PowerShell):
./gradlew test
# (En Windows CMD / Símbolo del sistema):
gradlew test

# Compilar el APK en modo Debug
# (En Linux, macOS o PowerShell):
./gradlew assembleDebug
# (En Windows CMD / Símbolo del sistema):
gradlew assembleDebug
```

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Consulta la [Guía de Contribución](CONTRIBUTING.md) para conocer las pautas de estilo y el flujo de Pull Requests.

---

## 📄 Licencia

Este proyecto está bajo la Licencia **MIT** a nombre de **DevNaranjo (2026)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

## 🔒 Privacidad y Protección de Datos

Este proyecto opera bajo un modelo 100% offline y P2P con **cero recopilación de datos**. Para consultar el tratamiento de permisos, almacenamiento y política legal completa, revisa [PRIVACY_AND_DATA_USAGE.md](PRIVACY_AND_DATA_USAGE.md).

