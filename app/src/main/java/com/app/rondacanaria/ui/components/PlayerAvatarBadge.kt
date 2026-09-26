package com.app.rondacanaria.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.model.AvatarCatalog
import com.app.rondacanaria.data.model.Team

@Composable
fun PlayerAvatarBadge(
    avatarId: String?,
    modifier: Modifier = Modifier,
    team: Team? = null,
    isLeader: Boolean = false,
    isDealer: Boolean = false,
    size: Dp = 40.dp,
    borderWidth: Dp = 2.dp,
    showBadges: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val avatar = AvatarCatalog.getAvatarById(avatarId)

    val teamBorderColor = when (team) {
        Team.TEAM_A -> MaterialTheme.colorScheme.primary
        Team.TEAM_B -> MaterialTheme.colorScheme.secondary
        Team.TEAM_C -> MaterialTheme.colorScheme.tertiary
        Team.TEAM_D -> MaterialTheme.colorScheme.outline
        Team.RESERVE -> Color(0xFFFFB300)
        Team.SPECTATOR -> Color.Gray
        null -> avatar.primaryColor
    }

    val backgroundBrush = Brush.linearGradient(
        listOf(
            avatar.primaryColor.copy(alpha = 0.85f),
            avatar.secondaryColor.copy(alpha = 0.95f)
        )
    )

    val emojiFontSize = (size.value * 0.52f).sp

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Círculo principal del avatar con gradiente y borde
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(backgroundBrush)
                .border(BorderStroke(borderWidth, teamBorderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatar.emoji,
                fontSize = emojiFontSize,
                textAlign = TextAlign.Center
            )
        }

        if (showBadges) {
            // Insignia de Líder 👑 (arriba a la derecha)
            if (isLeader) {
                Surface(
                    modifier = Modifier
                        .size(size * 0.42f)
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp),
                    shape = CircleShape,
                    color = Color(0xFFFFD54F),
                    border = BorderStroke(1.dp, Color(0xFFFFA000)),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "👑",
                            fontSize = (size.value * 0.24f).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Insignia de Repartidor 🃏 (abajo a la derecha)
            if (isDealer) {
                Surface(
                    modifier = Modifier
                        .size(size * 0.42f)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🃏",
                            fontSize = (size.value * 0.22f).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
