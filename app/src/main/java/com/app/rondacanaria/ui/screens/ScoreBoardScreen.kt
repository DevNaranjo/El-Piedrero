package com.app.rondacanaria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.model.*
import com.app.rondacanaria.domain.usecase.SessionStatus
import com.app.rondacanaria.ui.ScoreUiState
import com.app.rondacanaria.ui.ScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreBoardScreen(
    uiState: ScoreUiState,
    viewModel: ScoreViewModel
) {
    val gameState = uiState.gameState
    val isMultiplayerClient = !uiState.isLocalGame && !uiState.isHost

    var selectedTeamForCanto by remember(uiState.myTeam, isMultiplayerClient) {
        mutableStateOf(if (isMultiplayerClient && uiState.myTeam != Team.SPECTATOR) uiState.myTeam else Team.TEAM_A)
    }
    var showEndGameConfirmation by remember { mutableStateOf(false) }

    val canModifyA = !isMultiplayerClient || uiState.myTeam == Team.TEAM_A
    val canModifyB = !isMultiplayerClient || uiState.myTeam == Team.TEAM_B
    val canModifyC = !isMultiplayerClient || uiState.myTeam == Team.TEAM_C

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("El Piedrero 🃏", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (uiState.isLocalGame) {
                                "Partida Local (Mesa Central)"
                            } else if (uiState.isHost) {
                                "Mesa Anfitrión"
                            } else {
                                val teamName = when (uiState.myTeam) {
                                    Team.TEAM_A -> gameState.nameTeamA
                                    Team.TEAM_B -> gameState.nameTeamB
                                    Team.TEAM_C -> gameState.nameTeamC
                                    else -> "Espectador"
                                }
                                "Jugador en $teamName"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitGame() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir")
                    }
                },
                actions = {
                    if (uiState.isHost) {
                        IconButton(onClick = { viewModel.resetGame() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar Puntos")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isLocalGame) {
                ConnectionStatusBanner(status = uiState.sessionStatus, isHost = uiState.isHost)
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Tarjetas de Equipos con desglose de Malas y Buenas (Soporte 2, 3 o 4 jugadores)
            val isThreePlayers = gameState.maxPlayers == 3 || uiState.maxPlayers == 3

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tarjeta Equipo A
                RondaScoreCard(
                    modifier = Modifier.weight(1f),
                    teamName = gameState.nameTeamA,
                    score = gameState.scoreTeamA,
                    isSelected = selectedTeamForCanto == Team.TEAM_A,
                    teamColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isCompact = isThreePlayers,
                    canModify = canModifyA,
                    onSelect = { if (canModifyA) selectedTeamForCanto = Team.TEAM_A },
                    onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_A, delta) }
                )

                // Tarjeta Equipo B
                RondaScoreCard(
                    modifier = Modifier.weight(1f),
                    teamName = gameState.nameTeamB,
                    score = gameState.scoreTeamB,
                    isSelected = selectedTeamForCanto == Team.TEAM_B,
                    teamColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    isCompact = isThreePlayers,
                    canModify = canModifyB,
                    onSelect = { if (canModifyB) selectedTeamForCanto = Team.TEAM_B },
                    onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_B, delta) }
                )

                // Tarjeta Equipo C (solo si son 3 jugadores)
                if (isThreePlayers) {
                    RondaScoreCard(
                        modifier = Modifier.weight(1f),
                        teamName = gameState.nameTeamC,
                        score = gameState.scoreTeamC,
                        isSelected = selectedTeamForCanto == Team.TEAM_C,
                        teamColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        isCompact = true,
                        canModify = canModifyC,
                        onSelect = { if (canModifyC) selectedTeamForCanto = Team.TEAM_C },
                        onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_C, delta) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selector interactivo o indicador de equipo propio
            if (isMultiplayerClient) {
                val myTeamName = when (uiState.myTeam) {
                    Team.TEAM_A -> gameState.nameTeamA
                    Team.TEAM_B -> gameState.nameTeamB
                    Team.TEAM_C -> gameState.nameTeamC
                    else -> "Tu Equipo"
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tu equipo: $myTeamName (Solo puedes cambiar tu tanteo)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cantar para:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val teams = if (isThreePlayers) {
                        listOf(
                            Team.TEAM_A to gameState.nameTeamA,
                            Team.TEAM_B to gameState.nameTeamB,
                            Team.TEAM_C to gameState.nameTeamC
                        )
                    } else {
                        listOf(
                            Team.TEAM_A to gameState.nameTeamA,
                            Team.TEAM_B to gameState.nameTeamB
                        )
                    }

                    teams.forEach { (team, name) ->
                        FilterChip(
                            selected = selectedTeamForCanto == team,
                            onClick = { selectedTeamForCanto = team },
                            label = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botonera de Cantos (Ronda +1, Parranda +3, Caracol +4, Caracolillo +5)
            val cantoTargetTeam = if (isMultiplayerClient) uiState.myTeam else selectedTeamForCanto

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.RONDA) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(CantoType.RONDA.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.PARRANDA) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(CantoType.PARRANDA.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.CARACOL) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(CantoType.CARACOL.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.CARACOLILLO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(CantoType.CARACOLILLO.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Fila 3: Jugadas de Mesa (Majo, Limpiar, Majo y Limpio)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.MAJO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(CantoType.MAJO.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.LIMPIAR) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(CantoType.LIMPIAR.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.MAJO_Y_LIMPIO) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(CantoType.MAJO_Y_LIMPIO.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón para Finalizar Partida
            OutlinedButton(
                onClick = { showEndGameConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Terminar Partida", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Diálogo de confirmación para Terminar Partida
    if (showEndGameConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndGameConfirmation = false },
            title = { Text("¿Terminar Partida?") },
            text = { Text("Esta acción finalizará la partida actual y notificará el fin a todos los jugadores.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.terminateGame()
                        showEndGameConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Terminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndGameConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Victoria al finalizar
    if (gameState.status == GameStatus.FINISHED && gameState.winnerTeam != null) {
        val winnerName = when (gameState.winnerTeam) {
            Team.TEAM_A -> gameState.nameTeamA
            Team.TEAM_B -> gameState.nameTeamB
            Team.TEAM_C -> gameState.nameTeamC
            else -> "Ganador"
        }

        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(48.dp)) },
            title = { Text("¡Partida Ganada! 🏆", textAlign = TextAlign.Center) },
            text = {
                Text(
                    text = "🏆 $winnerName ha ganado la partida completando las 10 Buenas (21 piedras totales).",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.exitGame() }) {
                    Text("Volver al Menú Principal")
                }
            },
            dismissButton = {
                if (uiState.isHost) {
                    TextButton(onClick = { viewModel.resetGame() }) {
                        Text("Revancha (Reiniciar)")
                    }
                }
            }
        )
    }
}

@Composable
fun RondaScoreCard(
    modifier: Modifier = Modifier,
    teamName: String,
    score: TeamScore,
    isSelected: Boolean,
    teamColor: Color,
    contentColor: Color,
    isCompact: Boolean = false,
    canModify: Boolean = true,
    onSelect: () -> Unit = {},
    onManualAdjust: (Int) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = canModify) { onSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = teamColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = teamName,
                fontSize = if (isCompact) 14.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )

            // Badge de Estado: Malas vs Buenas
            if (score.isInBuenas) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))))
                        .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isCompact) "🌟 BUENAS (${score.buenas}/10)" else "🌟 ¡EN BUENAS! (${score.buenas}/10)",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isCompact) 10.sp else 12.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(contentColor.copy(alpha = 0.12f))
                        .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Malas (${score.malas}/11)",
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 10.sp else 12.sp
                    )
                }
            }

            // Contador Principal de Piedras
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${score.totalPiedras}",
                    fontSize = if (isCompact) 42.sp else 58.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
                Text(
                    text = "Piedras / 21",
                    fontSize = if (isCompact) 9.sp else 11.sp,
                    color = contentColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }

            // Botones de ajuste manual (+1 / -1) o indicador de Equipo Rival bloqueado
            if (!canModify) {
                Surface(
                    color = contentColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Solo editable por el equipo rival",
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Equipo Rival",
                            fontSize = if (isCompact) 10.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { onManualAdjust(-1) },
                        enabled = score.totalPiedras > 0,
                        modifier = Modifier.size(if (isCompact) 36.dp else 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = contentColor.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Restar piedra", tint = contentColor)
                    }

                    FilledIconButton(
                        onClick = { onManualAdjust(1) },
                        enabled = score.totalPiedras < TeamScore.TOTAL_PIEDRAS_VICTORY,
                        modifier = Modifier.size(if (isCompact) 36.dp else 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = contentColor.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Sumar piedra", tint = contentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBanner(
    status: SessionStatus,
    isHost: Boolean
) {
    val (bgColor, text, icon) = when (status) {
        SessionStatus.CONNECTED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            if (isHost) "🟢 Mesa en línea (Wi-Fi)" else "🟢 Conectado a la Mesa",
            Icons.Default.Wifi
        )
        SessionStatus.CONNECTING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "🟡 Conectando a la mesa...",
            Icons.Default.Sync
        )
        SessionStatus.RECONNECTING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "🟠 Reconectando a la mesa...",
            Icons.Default.Sync
        )
        SessionStatus.DISCONNECTED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "🔴 Desconectado de la mesa",
            Icons.Default.WifiOff
        )
        SessionStatus.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "⚠️ Error de red",
            Icons.Default.ErrorOutline
        )
        SessionStatus.IDLE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "⚪ Sin conexión activa",
            Icons.Default.Wifi
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
