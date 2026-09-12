package com.app.rondacanaria.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.model.CantoType

/**
 * Cuadrícula reactiva de cantos de 2 columnas con física de resortes (Spring Animations),
 * paleta semántica unificada M3 y tipografía autoajustable sin recortes de texto.
 */
@Composable
fun ReactiveCantosGrid(
    cantoButtonOrder: List<CantoType>,
    onCantoClick: (CantoType) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonPairs = remember(cantoButtonOrder) { cantoButtonOrder.chunked(2) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        buttonPairs.forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                pair.forEach { cantoType ->
                    SpringCantoButton(
                        cantoType = cantoType,
                        onClick = { onCantoClick(cantoType) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Botón individual de canto con respuesta táctil elástica (Spring Physics).
 */
@Composable
fun SpringCantoButton(
    cantoType: CantoType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "springCantoButtonScale"
    )

    val isQueenPlay = cantoType == CantoType.CARACOLILLO
    val isHeavySpecialPlay = cantoType == CantoType.SOBREMAJO || cantoType == CantoType.REQUETECONTRAMAJO
    val isSpecialPlay = isQueenPlay || isHeavySpecialPlay

    val colors = when {
        isQueenPlay -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    val border = when {
        isQueenPlay -> BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
        isHeavySpecialPlay -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    FilledTonalButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.medium,
        colors = colors,
        border = border,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isQueenPlay) {
                Text("👑 ", fontSize = 12.sp)
            } else if (isHeavySpecialPlay) {
                Text("⚡ ", fontSize = 12.sp)
            }
            AutoResizedText(
                text = cantoType.displayName,
                targetFontSize = 14.5.sp,
                minFontSize = 10.5.sp,
                fontWeight = if (isSpecialPlay) FontWeight.Black else FontWeight.Bold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}
