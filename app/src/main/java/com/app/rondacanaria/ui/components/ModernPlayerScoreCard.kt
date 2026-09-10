package com.app.rondacanaria.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.model.TeamScore

/**
 * Tarjeta de marcador estilo "Player Card" para El Piedrero v1.1.
 * Implementa profundidad moderna, tipografía Display gigante, animaciones
 * fluidas de conteo con AnimatedContent y resplandor orgánico para el turno activo.
 */
@Composable
fun ModernPlayerScoreCard(
    modifier: Modifier = Modifier,
    teamName: String,
    score: TeamScore,
    wins: Int = 0,
    isSelected: Boolean,
    canModify: Boolean = true,
    isCompact: Boolean = false,
    onSelect: () -> Unit = {},
    onManualAdjust: (Int) -> Unit,
    onCustomAdjustClick: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(26.dp)

    // Pulso animado sutil de resplandor para el equipo en turno activo
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulseTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val activeBorder = if (isSelected) {
        BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }

    val shadowElevation = if (isSelected) 10.dp else 3.dp

    ElevatedCard(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = shadowElevation,
                shape = cardShape,
                ambientColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                spotColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            .border(activeBorder, cardShape)
            .clickable(enabled = canModify) { onSelect() },
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera: Nombre de Equipo + Candado si es rival
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                AutoResizedText(
                    text = teamName,
                    targetFontSize = if (isCompact) 14.sp else 16.sp,
                    minFontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (!canModify) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Equipo rival bloqueado",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Fila de Badges / Cápsulas (Victorias y Malas/Buenas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Victorias (🏆)
                Surface(
                    color = if (wins > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "🏆 $wins",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (wins > 0) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                // Badge Malas / Buenas
                if (score.isInBuenas) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (score.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY) "¡Ganador! 👑" else "Buenas (${score.buenas}/10)",
                            fontSize = if (isCompact) 10.sp else 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Malas (${score.malas}/11)",
                            fontSize = if (isCompact) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Aviso táctico si faltan pocas piedras en Buenas
            val remainingStones = (TeamScore.TOTAL_PIEDRAS_VICTORY - score.totalPiedras).coerceAtLeast(0)
            if (score.isInBuenas && remainingStones in 1..10) {
                val cantosPhrase = when (remainingStones) {
                    1 -> "A falta de una ronda"
                    2 -> "A falta de ronda de bufos"
                    3 -> "A falta de una parranda"
                    4 -> "A falta de un caracol"
                    5 -> "A falta de un caracolillo"
                    6 -> "A falta de caracol de bufos"
                    7 -> "A falta de caracolillo de bufos"
                    else -> "A falta de $remainingStones piedras"
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎯 $cantosPhrase",
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Número HÉROE de Piedras con AnimatedContent ultra-rápido (120ms)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                AnimatedContent<Int>(
                    targetState = score.totalPiedras,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(animationSpec = tween(120)) { height -> height } + fadeIn(animationSpec = tween(120)))
                                .togetherWith(slideOutVertically(animationSpec = tween(120)) { height -> -height } + fadeOut(animationSpec = tween(120)))
                        } else {
                            (slideInVertically(animationSpec = tween(120)) { height -> -height } + fadeIn(animationSpec = tween(120)))
                                .togetherWith(slideOutVertically(animationSpec = tween(120)) { height -> height } + fadeOut(animationSpec = tween(120)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "heroScoreNumberAnimation"
                ) { piedras ->
                    AutoResizedText(
                        text = "$piedras",
                        targetFontSize = if (isCompact) 48.sp else 58.sp,
                        minFontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isCompact) 50.sp else 60.sp
                    )
                }

                Text(
                    text = "Piedras / 21",
                    fontSize = if (isCompact) 9.5.sp else 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Botonera de Ajuste Manual Integrada (Segmented Control Fijo 46.dp)
            if (!canModify) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Equipo Rival",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Restar (-)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(enabled = score.totalPiedras > 0) { onManualAdjust(-1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Restar piedra",
                                tint = if (score.totalPiedras > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight(0.6f)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        // Botón Sumar (+)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onManualAdjust(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Sumar piedra",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onCustomAdjustClick != null) {
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight(0.6f)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )

                            // Botón Ajuste Manual (+N)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onCustomAdjustClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+N",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
