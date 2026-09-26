package com.app.rondacanaria.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TutorialStep(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val body: String,
    val highlights: List<Pair<String, String>> = emptyList(),
    val tip: String? = null
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        emoji = "🃏",
        title = "¡Bienvenido a El Piedrero!",
        subtitle = "Tu marcador interactivo de Ronda Canaria",
        body = "El Piedrero está diseñado para sustituir las piedras y fichas físicas sobre la mesa por un marcador digital moderno, claro y sin discusiones. 100% offline, pensado para disfrutar en familia y con amigos de todas las edades.",
        highlights = listOf(
            "🪨 Sin líos de cuentas" to "Anota piedras buenas y malas de forma automática.",
            "👨‍🦳 100% Accesible" to "Botones grandes, alto contraste y respuesta por vibración.",
            "📶 Sin Internet" to "Funciona en cualquier lugar sin gastar datos."
        ),
        tip = "Puedes usar un solo móvil en el centro de la mesa o varios conectados por Wi-Fi."
    ),
    TutorialStep(
        emoji = "👥",
        title = "Modalidades de Juego",
        subtitle = "¿Cómo vas a jugar hoy?",
        body = "Selecciona la forma más cómoda para tu partida según los dispositivos disponibles:",
        highlights = listOf(
            "📱 Partida Local (1 Móvil)" to "Coloca el móvil en el centro de la mesa. Tú o cualquier jugador anota las piedras de 2, 3, 4, 6 u 8 participantes sin configurar nada.",
            "📶 Partida en Red (Varios Móviles)" to "El anfitrión crea la sala y los demás se unen al instante escaneando el código QR. ¡Cada jugador ve el marcador sincronizado en vivo!"
        ),
        tip = "En partidas en trío o de 6 a 8 jugadores, puedes rotar reservas sin pausar el juego."
    ),
    TutorialStep(
        emoji = "🪨",
        title = "Piedras Malas, Buenas y Chicos",
        subtitle = "La esencia del tanteo tradicional",
        body = "La app lleva la cuenta exacta de cada fase de la mano según el reglamento tradicional:",
        highlights = listOf(
            "⚪ Piedras Malas (1 a 10 ó 12)" to "Es la primera tanda de piedras a acumular.",
            "✨ Piedras Buenas" to "Al superar las malas, las piedras pasan automáticamente a ser buenas con una animación especial.",
            "🏆 Ganar el Chico" to "Quien complete primero las piedras buenas gana el Chico. La partida se juega al mejor de 2 Chicos (o Chica de desempate)."
        ),
        tip = "En parejas se juega a 10 piedras y en mano a mano a 12 piedras."
    ),
    TutorialStep(
        emoji = "📢",
        title = "Cantos y Recuento de Cartas",
        subtitle = "Anotación rápida con un solo toque",
        body = "El dock inferior te permite registrar jugadas al instante durante el transcurso de la mano:",
        highlights = listOf(
            "⚡ Ronda y Rondín" to "Anota 1 piedra por Ronda o 2 piedras por Rondín si tienes cartas repetidas.",
            "🖐️ Caída, Limpia y Enganche" to "Suma los puntos directos conseguidos al recoger cartas de la mesa.",
            "🃏 Recuento de Cartas" to "Al terminar cada mano, el asistente calcula automáticamente las piedras según las cartas sobrantes de cada equipo."
        ),
        tip = "Si te equivocas, usa el botón Deshacer (↶) de la barra superior para volver atrás."
    ),
    TutorialStep(
        emoji = "👑",
        title = "Turno de Reparto y Mano",
        subtitle = "Nunca más dudarás de a quién le toca dar",
        body = "Olvídate de discutir quién reparte las cartas de la baraja en la siguiente mano:",
        highlights = listOf(
            "👑 Indicador de Reparto" to "Una corona y un distintivo señalan con total claridad qué jugador debe repartir.",
            "⏰ Recordatorio Háptico" to "La aplicación emite una sutil vibración para recordar que es momento de barajar y repartir.",
            "🔄 Rotación Automática" to "El turno de reparto avanza en el sentido de las agujas del reloj tras cada mano jugada."
        ),
        tip = "Puedes activar o ajustar los segundos del recordatorio de reparto en Ajustes."
    ),
    TutorialStep(
        emoji = "⚙️",
        title = "Personalización y Accesibilidad",
        subtitle = "Adaptado totalmente a tu estilo",
        body = "Configura la aplicación para que se ajuste a tus preferencias y necesidades visuales:",
        highlights = listOf(
            "🎴 Avatares Canarios" to "Elige tu personaje típico canario tocando el avatar en el menú de 3 puntos (⋮) o en la sala.",
            "👓 Modo Senior" to "Aumenta el tamaño de letra hasta un 130% y activa el contraste reforzado para máxima legibilidad.",
            "📺 Transmisión a Smart TV" to "Envía el marcador en tiempo real a la televisión del salón o bar para que todos lo sigan en grande."
        ),
        tip = "¡Puedes volver a ver este recorrido en cualquier momento desde los 3 puntos (⋮) o en Ajustes!"
    )
)

@Composable
fun AppTutorialDialog(
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[currentStepIndex]
    val isLastStep = currentStepIndex == TUTORIAL_STEPS.lastIndex

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Barra superior del diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Paso ${currentStepIndex + 1} de ${TUTORIAL_STEPS.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                onComplete()
                                onDismissRequest()
                            }
                        ) {
                            Text(
                                text = "Saltar",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                onComplete()
                                onDismissRequest()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar guía",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido dinámico del paso
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "tutorialStepTransition"
                ) { currentStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Avatar / Emoji de Cabecera
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentStep.emoji,
                                    fontSize = 34.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentStep.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = currentStep.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        Text(
                            text = currentStep.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (currentStep.highlights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentStep.highlights.forEach { (titleH, descH) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = titleH,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = descH,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        currentStep.tip?.let { tipText ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tipText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Paginador de puntos (Dots Indicator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TUTORIAL_STEPS.indices.forEach { index ->
                        val isSelected = index == currentStepIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botones de navegación inferior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Anterior", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                onComplete()
                                onDismissRequest()
                            } else {
                                currentStepIndex++
                            }
                        },
                        modifier = Modifier.weight(if (currentStepIndex > 0) 1.3f else 1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLastStep) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("¡Entendido, a jugar!", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Siguiente", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
