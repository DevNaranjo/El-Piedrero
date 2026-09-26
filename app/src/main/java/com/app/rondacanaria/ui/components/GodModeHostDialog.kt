package com.app.rondacanaria.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.rondacanaria.data.model.GameState
import com.app.rondacanaria.data.model.GameStatus
import com.app.rondacanaria.data.model.Player
import com.app.rondacanaria.data.model.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodModeHostDialog(
    gameState: GameState,
    onApplyUpdate: (
        scoreA: Int,
        scoreB: Int,
        scoreC: Int,
        scoreD: Int,
        winsA: Int,
        winsB: Int,
        winsC: Int,
        winsD: Int,
        currentHand: Int,
        currentDeal: Int,
        dealerPlayerId: String?,
        status: GameStatus,
        winnerTeam: Team?,
        isCountingCards: Boolean
    ) -> Unit,
    onSwitchPlayerTeam: (playerId: String, newTeam: Team) -> Unit,
    onTogglePlayerLeader: (playerId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Estados editables en local
    var scoreA by remember { mutableIntStateOf(gameState.scoreTeamA.totalPiedras) }
    var scoreB by remember { mutableIntStateOf(gameState.scoreTeamB.totalPiedras) }
    var scoreC by remember { mutableIntStateOf(gameState.scoreTeamC.totalPiedras) }
    var scoreD by remember { mutableIntStateOf(gameState.scoreTeamD.totalPiedras) }

    var winsA by remember { mutableIntStateOf(gameState.winsTeamA) }
    var winsB by remember { mutableIntStateOf(gameState.winsTeamB) }
    var winsC by remember { mutableIntStateOf(gameState.winsTeamC) }
    var winsD by remember { mutableIntStateOf(gameState.winsTeamD) }

    var currentHand by remember { mutableIntStateOf(gameState.currentHand) }
    val maxDeals = gameState.maxDeals()
    var currentDeal by remember { mutableIntStateOf(gameState.currentDeal.coerceIn(1, maxDeals)) }
    var dealerPlayerId by remember { mutableStateOf(gameState.dealerPlayerId) }

    var gameStatus by remember { mutableStateOf(gameState.status) }
    var winnerTeam by remember { mutableStateOf(gameState.winnerTeam) }
    var isCountingCards by remember { mutableStateOf(gameState.isCountingCards) }

    val hasThreeTeams = gameState.maxPlayers == 3 || gameState.maxPlayers == 6
    val hasFourTeams = gameState.maxPlayers == 8

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cabecera épica de Modo Dios
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("⚡", fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Modo Dios",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFFFD54F),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "👑 HOST",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = Color(0xFFB71C1C),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Control total y corrección de la partida",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Barra de Pestañas
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("🪨 Puntos", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🃏 Mano", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("⚙️ Estado", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("👥 Jugadores", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                // Contenido Scrolleable según la pestaña activa
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        0 -> PointsGodTab(
                            nameA = gameState.nameTeamA,
                            scoreA = scoreA,
                            winsA = winsA,
                            onScoreAChange = { scoreA = it },
                            onWinsAChange = { winsA = it },
                            nameB = gameState.nameTeamB,
                            scoreB = scoreB,
                            winsB = winsB,
                            onScoreBChange = { scoreB = it },
                            onWinsBChange = { winsB = it },
                            hasThreeTeams = hasThreeTeams,
                            nameC = gameState.nameTeamC,
                            scoreC = scoreC,
                            winsC = winsC,
                            onScoreCChange = { scoreC = it },
                            onWinsCChange = { winsC = it },
                            hasFourTeams = hasFourTeams,
                            nameD = gameState.nameTeamD,
                            scoreD = scoreD,
                            winsD = winsD,
                            onScoreDChange = { scoreD = it },
                            onWinsDChange = { winsD = it }
                        )
                        1 -> HandGodTab(
                            currentHand = currentHand,
                            onHandChange = { currentHand = it },
                            currentDeal = currentDeal,
                            maxDeals = maxDeals,
                            onDealChange = { currentDeal = it },
                            connectedPlayers = gameState.connectedPlayers,
                            dealerPlayerId = dealerPlayerId,
                            onDealerChange = { dealerPlayerId = it }
                        )
                        2 -> StatusGodTab(
                            gameStatus = gameStatus,
                            onStatusChange = { gameStatus = it },
                            winnerTeam = winnerTeam,
                            onWinnerChange = { winnerTeam = it },
                            nameA = gameState.nameTeamA,
                            nameB = gameState.nameTeamB,
                            isCountingCards = isCountingCards,
                            onCountingCardsToggle = { isCountingCards = it }
                        )
                        3 -> PlayersGodTab(
                            players = gameState.connectedPlayers,
                            onSwitchPlayerTeam = onSwitchPlayerTeam,
                            onToggleLeader = onTogglePlayerLeader
                        )
                    }
                }

                // Barra inferior de confirmación
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onApplyUpdate(
                                    scoreA,
                                    scoreB,
                                    scoreC,
                                    scoreD,
                                    winsA,
                                    winsB,
                                    winsC,
                                    winsD,
                                    currentHand,
                                    currentDeal,
                                    dealerPlayerId,
                                    gameStatus,
                                    winnerTeam,
                                    isCountingCards
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ Aplicar a la Partida", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsGodTab(
    nameA: String,
    scoreA: Int,
    winsA: Int,
    onScoreAChange: (Int) -> Unit,
    onWinsAChange: (Int) -> Unit,
    nameB: String,
    scoreB: Int,
    winsB: Int,
    onScoreBChange: (Int) -> Unit,
    onWinsBChange: (Int) -> Unit,
    hasThreeTeams: Boolean,
    nameC: String,
    scoreC: Int,
    winsC: Int,
    onScoreCChange: (Int) -> Unit,
    onWinsCChange: (Int) -> Unit,
    hasFourTeams: Boolean,
    nameD: String,
    scoreD: Int,
    winsD: Int,
    onScoreDChange: (Int) -> Unit,
    onWinsDChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TeamScoreEditorCard(
            teamName = nameA,
            teamBadge = "Equipo A",
            accentColor = Color(0xFF1E88E5),
            score = scoreA,
            wins = winsA,
            onScoreChange = onScoreAChange,
            onWinsChange = onWinsAChange
        )

        TeamScoreEditorCard(
            teamName = nameB,
            teamBadge = "Equipo B",
            accentColor = Color(0xFFE53935),
            score = scoreB,
            wins = winsB,
            onScoreChange = onScoreBChange,
            onWinsChange = onWinsBChange
        )

        if (hasThreeTeams) {
            TeamScoreEditorCard(
                teamName = nameC,
                teamBadge = "Equipo C",
                accentColor = Color(0xFF43A047),
                score = scoreC,
                wins = winsC,
                onScoreChange = onScoreCChange,
                onWinsChange = onWinsCChange
            )
        }

        if (hasFourTeams) {
            TeamScoreEditorCard(
                teamName = nameD,
                teamBadge = "Equipo D",
                accentColor = Color(0xFFFB8C00),
                score = scoreD,
                wins = winsD,
                onScoreChange = onScoreDChange,
                onWinsChange = onWinsDChange
            )
        }
    }
}

@Composable
private fun TeamScoreEditorCard(
    teamName: String,
    teamBadge: String,
    accentColor: Color,
    score: Int,
    wins: Int,
    onScoreChange: (Int) -> Unit,
    onWinsChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = teamName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = teamBadge,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control de Piedras
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "🪨 Piedras:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onScoreChange((score - 5).coerceAtLeast(0)) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { onScoreChange((score - 1).coerceAtLeast(0)) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 38.dp, minHeight = 32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = "$score",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = { onScoreChange((score + 1).coerceAtMost(21)) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onScoreChange((score + 5).coerceAtMost(21)) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Control de Chicos (Victorias)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "🏆 Chicos ganados:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onWinsChange((wins - 1).coerceAtLeast(0)) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        color = Color(0xFFFFD54F).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 34.dp, minHeight = 30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = "$wins",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                    Button(
                        onClick = { onWinsChange(wins + 1) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandGodTab(
    currentHand: Int,
    onHandChange: (Int) -> Unit,
    currentDeal: Int,
    maxDeals: Int,
    onDealChange: (Int) -> Unit,
    connectedPlayers: List<Player>,
    dealerPlayerId: String?,
    onDealerChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mano y Reparto
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "🔢 Mano y Reparto de Cartas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Mano actual:", fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onHandChange((currentHand - 1).coerceAtLeast(1)) },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Mano $currentHand", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Button(
                            onClick = { onHandChange(currentHand + 1) },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Reparto actual:", fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (deal in 1..maxDeals) {
                            val isSelected = deal == currentDeal
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { onDealChange(deal) }
                                    .defaultMinSize(minWidth = 36.dp, minHeight = 32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text(
                                        text = "${deal}º",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Repartidor Asignado
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🃏", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Asignar Repartidor de Cartas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selecciona quién tiene la mano y reparte en la mesa:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    connectedPlayers.forEach { player ->
                        val isSelected = player.id == dealerPlayerId
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDealerChange(player.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onDealerChange(player.id) },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PlayerAvatarBadge(
                                        avatarId = player.avatarId,
                                        team = player.team,
                                        size = 26.dp,
                                        showBadges = false
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = player.name,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    if (player.isHost) {
                                        Text(" 👑", fontSize = 12.sp)
                                    } else if (player.isLeader) {
                                        Text(" 👑 (Líder)", fontSize = 11.sp, color = Color(0xFFE65100))
                                    }
                                }
                                Surface(
                                    color = when (player.team) {
                                        Team.TEAM_A -> Color(0xFF1E88E5).copy(alpha = 0.15f)
                                        Team.TEAM_B -> Color(0xFFE53935).copy(alpha = 0.15f)
                                        Team.TEAM_C -> Color(0xFF43A047).copy(alpha = 0.15f)
                                        Team.TEAM_D -> Color(0xFFFB8C00).copy(alpha = 0.15f)
                                        else -> Color.Gray.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = player.team.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusGodTab(
    gameStatus: GameStatus,
    onStatusChange: (GameStatus) -> Unit,
    winnerTeam: Team?,
    onWinnerChange: (Team?) -> Unit,
    nameA: String,
    nameB: String,
    isCountingCards: Boolean,
    onCountingCardsToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Estado del Juego
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "🕹️ Estado de la Partida", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        GameStatus.PLAYING to "▶️ En Juego",
                        GameStatus.PAUSED to "⏸️ Pausada",
                        GameStatus.FINISHED to "🏁 Finalizada"
                    ).forEach { (status, label) ->
                        val isSelected = gameStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStatusChange(status) },
                            label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(visible = gameStatus == GameStatus.FINISHED) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(text = "Equipo Ganador:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = winnerTeam == Team.TEAM_A,
                                onClick = { onWinnerChange(Team.TEAM_A) },
                                label = { Text("🏆 $nameA", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = winnerTeam == Team.TEAM_B,
                                onClick = { onWinnerChange(Team.TEAM_B) },
                                label = { Text("🏆 $nameB", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = winnerTeam == null,
                                onClick = { onWinnerChange(null) },
                                label = { Text("Ninguno", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Acciones de Desbloqueo y Emergencia
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "🚨 Desatasques y Emergencias", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Úsalas si la partida se bloquea por desconexión o fallo en los móviles de los jugadores:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Desatascar Recuento de Cartas
                OutlinedButton(
                    onClick = { onCountingCardsToggle(!isCountingCards) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCountingCards) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (isCountingCards) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCountingCards) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCountingCards) "🔓 Forzar Cancelación de Recuento Bloqueado" else "Recuento de cartas libre (Normal)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayersGodTab(
    players: List<Player>,
    onSwitchPlayerTeam: (playerId: String, newTeam: Team) -> Unit,
    onToggleLeader: (playerId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Gestiona los equipos y permisos de los jugadores en la mesa:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        players.forEach { player ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatarBadge(
                                avatarId = player.avatarId,
                                team = player.team,
                                isLeader = player.isLeader,
                                size = 32.dp,
                                showBadges = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            if (player.isHost) {
                                Text(" 👑 Anfitrión", fontSize = 11.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold)
                            } else if (player.isLeader) {
                                Text(" 👑 Líder", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!player.isHost) {
                            FilledTonalButton(
                                onClick = { onToggleLeader(player.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (player.isLeader) "Quitar Líder" else "Hacer Líder 👑",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Selector de Equipo para este jugador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Team.TEAM_A to "Eq. A",
                            Team.TEAM_B to "Eq. B",
                            Team.RESERVE to "Reserva",
                            Team.SPECTATOR to "Espect."
                        ).forEach { (team, label) ->
                            val isCurrentTeam = player.team == team
                            Surface(
                                color = if (isCurrentTeam) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isCurrentTeam) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSwitchPlayerTeam(player.id, team) }
                                    .height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isCurrentTeam) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrentTeam) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
