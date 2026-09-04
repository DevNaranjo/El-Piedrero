# Política de Privacidad y Uso de Datos 📜

**Aplicación:** El Piedrero  
**Desarrollador / Mantenedor:** DevNaranjo  
**Versión del Software:** MVP v1.0.6  
**Licencia:** Licencia MIT (Open Source)  
**Fecha de última actualización:** 4 de septiembre de 2026  
**Repositorio Oficial:** [https://github.com/DevNaranjo/El-Piedrero](https://github.com/DevNaranjo/El-Piedrero)

---

## 1. Declaración de Principios e Introducción

La presente Política de Privacidad y Uso de Datos describe de manera transparente, exhaustiva y rigurosa cómo la aplicación móvil **El Piedrero** (en adelante, *"la Aplicación"*) interactúa con tu dispositivo, los permisos requeridos para su correcto funcionamiento y el tratamiento que se da a la información generada.

**El Piedrero** ha sido diseñada bajo el principio de **Privacidad desde el Diseño y por Defecto (*Privacy by Design and by Default*)**, conforme a lo estipulado en el Reglamento General de Protección de Datos de la Unión Europea (**RGPD / Reglamento UE 2016/679**), la Ley Orgánica de Protección de Datos Personales y garantía de los derechos digitales (**LOPDGDD 3/2018**) y la Ley de Protección de la Privacidad Infantil en Línea de los Estados Unidos (**COPPA**).

> [!IMPORTANT]
> **Principio Fundamental de "Cero Recopilación de Datos":**  
> La Aplicación **NO recopila, NO almacena en servidores externos, NO transmite, NO rastrea ni comercializa ningún tipo de dato personal, identificador publicitario, telemetría o metadato de uso**. El Piedrero opera bajo un modelo **100% offline y de comunicación local Peer-to-Peer (P2P)**.

---

## 2. Tratamiento y Recopilación de Datos Personales

Con el objetivo de garantizar la absoluta soberanía y privacidad de sus usuarios:

* **Sin Cuentas ni Registro:** La Aplicación no solicita, no requiere ni ofrece sistemas de inicio de sesión, creación de perfiles, registro mediante correo electrónico, autenticación biométrica ni vinculación con redes sociales o servicios de terceros.
* **Sin Servidores Centrales:** El Piedrero no se conecta a ningún servidor o infraestructura en la nube operada por el desarrollador o terceros para almacenar estados de juego, puntuaciones o registros de jugadores.
* **Sin Analíticas ni Rastreo:** La Aplicación carece de SDKs de telemetría, bibliotecas de análisis de comportamiento (como Google Analytics, Firebase Analytics, Mixpanel, etc.) o rastreadores publicitarios (*AdMob, Unity Ads, etc.*).
* **Nombres y Alias Volátiles:** El nombre o alias que el usuario introduce en la pantalla de sala multijugador se utiliza únicamente dentro de la sesión local para identificar visualmente a los miembros de la mesa y se almacena exclusivamente en la memoria interna de su terminal.

---

## 3. Permisos del Dispositivo y Justificación Técnica

Para ejecutar sus funciones nativas de marcador y conectividad entre dispositivos cercanos, la Aplicación solicita un conjunto estrictamente necesario de permisos del sistema operativo Android. A continuación se detalla la justificación técnica y el alcance de cada uno:

### A. Acceso a la Cámara (`android.permission.CAMERA`)
* **Finalidad:** Utilizado única y exclusivamente por el módulo de escaneo de códigos QR cuando el usuario decide unirse a una mesa multijugador alojada por un anfitrión (*Host*).
* **Tratamiento Técnico:** La captura se realiza a través de la API oficial `CameraX` y la biblioteca de visión por computadora `ZXing`. Los fotogramas de la cámara se analizan **en tiempo real en la memoria volátil (RAM)** del dispositivo para decodificar la cadena JSON que contiene la información de conexión local (IP local, puerto y token efímero de sesión).
* **Garantía de Privacidad:** **En ningún momento se capturan fotografías estáticas, ni se graban secuencias de vídeo, ni se almacena imagen alguna en el almacenamiento del dispositivo ni se transmiten contenidos visuales a través de la red**.
* **Cámara Trasera Forzada:** Por motivos de seguridad y ergonomía, el selector de cámara está forzado a nivel de código exclusivamente a la lente trasera (`LENS_FACING_BACK`), impidiendo cualquier activación de la cámara frontal.

### B. Comunicación en Red Local (`android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`)
* **Finalidad:** Facilitar la sincronización en tiempo real de las puntuaciones, cantos y piedras de la mesa de Ronda Canaria entre los dispositivos presentes en el mismo espacio físico.
* **Alcance:** A pesar de que Android clasifica el permiso `INTERNET` de forma genérica, **la Aplicación no se conecta a Internet ni a servicios remotos externos**. Estos permisos se emplean únicamente para:
  1. Abrir un servidor local de Sockets TCP (`ServerSocket`) en el dispositivo del Anfitrión.
  2. Conectar clientes en la misma subred Wi-Fi o Punto de Acceso portátil (*Hotspot* / Zona Wi-Fi compartida).
  3. Intercambiar tramas ligeras estructuradas en NDJSON (*Newline Delimited JSON*) protegidas con tokens y claves secretas generadas en cada partida.
* **Seguridad de Red:** La Aplicación tiene deshabilitado el tráfico HTTP en texto claro hacia el exterior mediante directivas estrictas de seguridad de red (`usesCleartextTraffic="false"`).

### C. Vibración y Feedback Háptico (`android.permission.VIBRATE`)
* **Finalidad:** Proporcionar respuesta táctil agradable al pulsar los botones de puntuación, cantar jugadas tradicionales (*Ronda, Parranda, Sobremajo, etc.*) o alcanzar la fase de *«¡Buenas!»*. La vibración se ejecuta íntegramente mediante los servicios de hardware locales de Android (`Vibrator` y `VibratorManager`).

### D. Ajustes de Audio (`android.permission.MODIFY_AUDIO_SETTINGS`)
* **Finalidad:** Permitir al motor de audio interno (`RondaAudioPlayer`) gestionar los canales sonoros de la app, facilitando el control independiente del volumen maestro, el volumen de la música ambiental y los efectos de voz, así como la atenuación inteligente (*audio ducking*) durante las partidas.

---

## 4. Almacenamiento Local del Dispositivo (*Device Storage*)

Toda la persistencia de datos requerida para recordar la configuración del usuario se gestiona localmente mediante el sistema estándar de Android `SharedPreferences`, ubicado en el directorio privado protegido de la aplicación (`/data/data/com.app.rondacanaria/shared_prefs/`):

1. **Ajustes de Sonido y Reproducción:**
   * Nivel del volumen general (`audio_master_volume`).
   * Nivel del volumen de la música de fondo (`audio_music_volume`).
   * Nivel del volumen de los efectos de sonido y cantos (`audio_sfx_volume`).
   * Estados de activación o silencio de música y efectos (`audio_music_enabled`, `audio_sfx_enabled`).
2. **Historial de Partidas:**
   * Registro numérico y cronológico de las partidas completadas en el dispositivo (nombres de los equipos, conteo final de piedras malas/buenas, jugadas registradas y fecha/hora local).
3. **Nombre / Alias de Jugador:**
   * Último nombre ingresado por el usuario en la sala para evitar tener que reescribirlo en partidas consecutivas.

> [!NOTE]
> **Aislamiento y Eliminación de Datos:**  
> Ninguno de estos archivos es accesible por otras aplicaciones instaladas en el dispositivo. Si el usuario decide desinstalar la Aplicación o pulsar en *«Borrar datos»* desde los ajustes del sistema Android, **todos los registros, historiales y preferencias se eliminan de forma inmediata, definitiva e irrecuperable**.

---

## 5. Modelo de Código Abierto (*Open Source*) y Auditoría

La Aplicación se distribuye públicamente bajo la **Licencia MIT**, una de las licencias de software libre más permisivas y transparentes reconocidas por la *Open Source Initiative (OSI)*.

Cualquier persona, desarrollador o auditor de seguridad informática tiene la libertad de inspeccionar, verificar, compilar y auditar el código fuente completo en el repositorio oficial de GitHub para corroborar que la aplicación cumple estrictamente con cada uno de los compromisos de privacidad expuestos en este documento.

---

## 6. Descargo de Responsabilidad y Limitación de Garantías (*Disclaimer*)

1. **Provisión "Tal Cual" (*AS IS*):**  
   Conforme a los términos establecidos en la Licencia MIT, la Aplicación se suministra *"TAL CUAL"*, sin garantías de ninguna clase, ya sean expresas o implícitas, incluyendo, pero no limitándose a, garantías de comerciabilidad, idoneidad para un propósito particular o ausencia de errores.

2. **Rol de Asistente y Árbitro Digital:**  
   **El Piedrero** es una herramienta de software destinada exclusivamente a asistir a los jugadores como marcador digital para facilitar el cálculo de piedras en el juego tradicional de la Ronda Canaria. El desarrollador **no se responsabiliza** por:
   * Posibles desincronizaciones en la red Wi-Fi local causadas por interferencias de señal, latencia de routers o desconexiones de terminales.
   * Pérdida o desconfiguración de puntuaciones debido a cierres forzados del sistema operativo, agotamiento de batería o fallos del dispositivo.
   * Malentendidos, disputas o controversias que puedan suscitarse entre los participantes de una partida respecto a la validez de los cantos o el conteo de piedras en la mesa de juego.

---

## 7. Propiedad Intelectual, Recursos Culturales y Protección de Menores

* **Difusión Cultural Sin Ánimo de Lucro:** Las locuciones de cantos canarios (*Ronda, Parranda, Caracol, Majo, Sobremajo, ¡Buenas!*), ilustraciones y piezas de música ambiental tradicional canaria incluidas en los recursos de la aplicación tienen como único fin la preservación, divulgación y homenaje a la cultura y folklore popular de las Islas Canarias en el ámbito lúdico.
* **Cumplimiento COPPA y Protección de Menores:** La Aplicación es apta para todos los públicos (*Family Friendly* / PEGI 3). Al no recopilar información de identificación personal, no rastrear la ubicación geográfica ni requerir interacción con extraños fuera del rango de la red local física, la Aplicación cumple plenamente con las normativas internacionales de protección de menores en entornos digitales.

---

## 8. Modificaciones a la Presente Política

Dado que la Aplicación se encuentra en constante evolución comunitaria y funcional (actualmente en fase de MVP), el desarrollador se reserva el derecho de actualizar la presente política para reflejar nuevas características de juego o eventuales modificaciones en las normativas aplicables. Cualquier cambio sustancial se reflejará documentalmente en el repositorio de GitHub con su correspondiente registro de versión.

---

## 9. Contacto y Canal de Soporte

Si tienes preguntas, sugerencias o inquietudes relativas a la presente política de privacidad, el código fuente o el funcionamiento técnico de la aplicación, puedes ponerte en contacto a través de los canales comunitarios del repositorio oficial:

* **Repositorio GitHub:** [https://github.com/DevNaranjo/El-Piedrero](https://github.com/DevNaranjo/El-Piedrero)
* **Gestión de Incidencias (*Issues*):** [https://github.com/DevNaranjo/El-Piedrero/issues](https://github.com/DevNaranjo/El-Piedrero/issues)
* **Desarrollador / Mantenedor Principal:** DevNaranjo
