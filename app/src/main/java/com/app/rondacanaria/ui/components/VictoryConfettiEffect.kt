package com.app.rondacanaria.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.app.rondacanaria.ui.theme.AtlanticBluePrimaryLight
import com.app.rondacanaria.ui.theme.CanaryGoldLogoHex
import com.app.rondacanaria.ui.theme.CanaryOchreSecondaryLight
import com.app.rondacanaria.ui.theme.ErrorLight
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleShape {
    RECTANGLE,
    CIRCLE,
    STAR,
    RIBBON
}

private class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    val vRotation: Float,
    var flipRotation: Float,
    val vFlipRotation: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    val shape: ParticleShape = ParticleShape.RECTANGLE,
    val swayOffset: Float = Random.nextFloat() * 6.28f,
    val swaySpeed: Float = 2f + Random.nextFloat() * 4f
)

/**
 * Efecto de Celebración de Victoria con Confetti implementado en Compose Canvas puro.
 * Utiliza la paleta cultural "Atlántico & Oro Canario"
 * con física de gravedad, balanceo oscilante 3D y renderizado a 60/120 FPS sin dependencias externas.
 */
@Composable
fun VictoryConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 140
) {
    val colors = remember {
        listOf(
            CanaryGoldLogoHex,
            AtlanticBluePrimaryLight,
            CanaryOchreSecondaryLight,
            Color.White,
            ErrorLight,
            Color(0xFFFFD54F) // Oro brillante
        )
    }

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    var hasSpawned by remember { mutableStateOf(false) }

    fun spawnParticles(w: Float, h: Float) {
        particles.clear()
        for (i in 0 until particleCount) {
            // Emisión tipo cañón festivo desde la parte media-inferior y bordes superiores
            val startX = if (Random.nextBoolean()) {
                w * (0.15f + Random.nextFloat() * 0.70f)
            } else {
                if (Random.nextBoolean()) w * 0.05f else w * 0.95f
            }
            val startY = -20f - Random.nextFloat() * (h * 0.25f).coerceAtLeast(150f)

            val vx = (Random.nextFloat() - 0.5f) * 450f
            val vy = 150f + Random.nextFloat() * 500f
            val sizeW = 14f + Random.nextFloat() * 18f
            val sizeH = 10f + Random.nextFloat() * 16f
            val shape = when (Random.nextInt(4)) {
                0 -> ParticleShape.RECTANGLE
                1 -> ParticleShape.CIRCLE
                2 -> ParticleShape.STAR
                else -> ParticleShape.RIBBON
            }

            particles.add(
                ConfettiParticle(
                    x = startX,
                    y = startY,
                    vx = vx,
                    vy = vy,
                    rotation = Random.nextFloat() * 360f,
                    vRotation = (Random.nextFloat() - 0.5f) * 360f,
                    flipRotation = Random.nextFloat() * 360f,
                    vFlipRotation = (Random.nextFloat() - 0.5f) * 540f,
                    width = sizeW,
                    height = sizeH,
                    color = colors[Random.nextInt(colors.size)],
                    alpha = 1.0f,
                    shape = shape
                )
            )
        }
    }

    // Bucle de animación física a través de Frame Clock de Compose
    LaunchedEffect(canvasWidth, canvasHeight) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@LaunchedEffect
        if (!hasSpawned) {
            spawnParticles(canvasWidth, canvasHeight)
            hasSpawned = true
        }

        var lastFrameTimeNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos == 0L) {
                    lastFrameTimeNanos = frameTimeNanos
                    return@withFrameNanos
                }

                val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                lastFrameTimeNanos = frameTimeNanos
                val gravity = 280f

                val iterator = particles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.vy += gravity * dt
                    p.x += (p.vx + sin(p.y * 0.015f + p.swayOffset) * 60f) * dt
                    p.y += p.vy * dt
                    p.rotation += p.vRotation * dt
                    p.flipRotation += p.vFlipRotation * dt

                    // Desvanecer progresivamente cuando se acerque al fondo
                    if (p.y > canvasHeight * 0.75f) {
                        val progress = (p.y - canvasHeight * 0.75f) / (canvasHeight * 0.25f)
                        p.alpha = (1.0f - progress).coerceIn(0f, 1f)
                    }

                    // Reciclar en la parte superior si sigue viva la celebración
                    if (p.y > canvasHeight + 50f) {
                        p.y = -20f - Random.nextFloat() * 100f
                        p.x = canvasWidth * (0.05f + Random.nextFloat() * 0.90f)
                        p.vy = 180f + Random.nextFloat() * 400f
                        p.vx = (Random.nextFloat() - 0.5f) * 350f
                        p.alpha = 1.0f
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (canvasWidth != size.width || canvasHeight != size.height) {
            canvasWidth = size.width
            canvasHeight = size.height
        }

        for (p in particles) {
            if (p.alpha <= 0.01f) continue

            val scaleX = cos(Math.toRadians(p.flipRotation.toDouble())).toFloat()
            val drawColor = p.color.copy(alpha = p.alpha)

            rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(p.x, p.y)) {
                    when (p.shape) {
                        ParticleShape.RECTANGLE -> {
                            drawRect(
                                color = drawColor,
                                topLeft = Offset(p.x - p.width / 2, p.y - p.height / 2),
                                size = Size(p.width, p.height)
                            )
                        }
                        ParticleShape.CIRCLE -> {
                            drawCircle(
                                color = drawColor,
                                radius = p.width / 2.5f,
                                center = Offset(p.x, p.y)
                            )
                        }
                        ParticleShape.STAR -> {
                            drawStar(
                                center = Offset(p.x, p.y),
                                radius = p.width / 2,
                                color = drawColor
                            )
                        }
                        ParticleShape.RIBBON -> {
                            drawRect(
                                color = drawColor,
                                topLeft = Offset(p.x - p.width / 4, p.y - p.height),
                                size = Size(p.width / 2, p.height * 1.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    val spikes = 5
    val innerRadius = radius * 0.45f
    var rot = Math.PI / 2 * 3
    val step = Math.PI / spikes

    path.moveTo(center.x, center.y - radius)
    for (i in 0 until spikes) {
        val x = center.x + cos(rot).toFloat() * radius
        val y = center.y + sin(rot).toFloat() * radius
        path.lineTo(x, y)
        rot += step

        val ix = center.x + cos(rot).toFloat() * innerRadius
        val iy = center.y + sin(rot).toFloat() * innerRadius
        path.lineTo(ix, iy)
        rot += step
    }
    path.close()
    drawPath(path, color)
}
