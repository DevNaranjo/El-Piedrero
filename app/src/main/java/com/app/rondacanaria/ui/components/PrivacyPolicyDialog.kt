package com.app.rondacanaria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Privacidad y Uso de Datos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "El Piedrero · DevNaranjo (MIT)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🛡️ Cero Recopilación de Datos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "La aplicación funciona 100% offline y en red local P2P. No se recopilan, almacenan, rastrean ni envían datos personales, métricas ni telemetría a servidores externos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PrivacySectionItem(
                    title = "👤 Sin Cuentas ni Registros",
                    description = "No se requiere correo, perfiles ni inicio de sesión. El nombre o alias de juego solo se usa en la sesión local y permanece en tu dispositivo."
                )

                PrivacySectionItem(
                    title = "📷 Permiso de Cámara",
                    description = "Utilizado exclusivamente a través de CameraX y ZXing para escanear el código QR al unirse a una mesa en red. La imagen se procesa en tiempo real en la memoria RAM y NUNCA se guarda, graba ni transmite. Restringido exclusivamente a la cámara trasera."
                )

                PrivacySectionItem(
                    title = "📶 Red Local (Wi-Fi P2P)",
                    description = "La sincronización de la mesa ocurre exclusivamente entre los móviles conectados a la misma red Wi-Fi o Punto de Acceso mediante Sockets TCP locales cifrados/ofuscados. Sin tráfico en texto claro ni conexión a servidores de Internet."
                )

                PrivacySectionItem(
                    title = "💾 Almacenamiento Local del Dispositivo",
                    description = "Los ajustes de sonido y el historial de partidas se guardan únicamente en la memoria interna protegida del teléfono (SharedPreferences). Se eliminan permanentemente al desinstalar la app."
                )

                PrivacySectionItem(
                    title = "⚖️ Descargo de Responsabilidad (Disclaimer)",
                    description = "Software proporcionado TAL CUAL (AS IS) bajo Licencia MIT como asistente y árbitro digital de apoyo para la Ronda Canaria. El desarrollador no se responsabiliza de incidencias de red o disputas sobre las piedras en la mesa de juego."
                )

                PrivacySectionItem(
                    title = "🎵 Música Ambiental (Generada por IA)",
                    description = "Las pistas de música instrumental que acompañan el juego (bgm_01 a bgm_06) han sido generadas mediante Inteligencia Artificial (IA) inspiradas en melodías y ritmos tradicionales del folclore canario, totalmente libres de derechos de autor y entidades de gestión colectiva (SGAE)."
                )

                PrivacySectionItem(
                    title = "🌱 Tradición Cultural y Menores",
                    description = "Las locuciones tradicionales de Ronda Canaria se incluyen con fines culturales y folclóricos sin ánimo de lucro. Aplicación apta para todos los públicos (PEGI 3 / COPPA)."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PrivacySectionItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
