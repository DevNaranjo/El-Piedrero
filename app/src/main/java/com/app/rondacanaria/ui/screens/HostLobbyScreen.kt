package com.app.rondacanaria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.model.Team
import com.app.rondacanaria.domain.usecase.SessionStatus
import com.app.rondacanaria.ui.ScoreUiState
import com.app.rondacanaria.ui.ScoreViewModel
import com.app.rondacanaria.ui.components.TvCastDialog
import com.app.rondacanaria.ui.qr.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreen(
    uiState: ScoreUiState,
    viewModel: ScoreViewModel
) {
    val connectionInfo = uiState.hostConnectionInfo
    val qrBitmap = remember(connectionInfo) {
        connectionInfo?.toJson()?.let { QrCodeGenerator.generateQrBitmap(it, 512) }
    }
    var showIncompletePlayersDialog by remember { mutableStateOf(false) }
    var showExitHostRoomConfirmation by remember { mutableStateOf(false) }
    var showTvCastDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (showTvCastDialog) {
            showTvCastDialog = false
        } else if (showIncompletePlayersDialog) {
            showIncompletePlayersDialog = false
        } else if (showExitHostRoomConfirmation) {
            showExitHostRoomConfirmation = false
        } else {
            showExitHostRoomConfirmation = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.isHost) "Mesa Anfitrión" else "Sala de Espera",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        val hostName = uiState.gameState.connectedPlayers.find { it.isHost }?.name
                            ?: uiState.connectingHostName
                            ?: uiState.hostConnectionInfo?.hostName
                        Text(
                            text = if (uiState.isHost) "El Piedrero 🃏" else "Mesa de $hostName 🃏",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitHostRoomConfirmation = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showTvCastDialog = true }) {
                        Icon(Icons.Default.Tv, contentDescription = "Transmitir a Smart TV")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Escanea para unirte a la partida:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Código QR de conexión",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Código QR listo para escanear",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxCapacity = if (uiState.gameState.maxPlayers in listOf(2, 3, 4, 6, 8)) {
                uiState.gameState.maxPlayers
            } else if (uiState.maxPlayers in listOf(2, 3, 4, 6, 8)) {
                uiState.maxPlayers
            } else {
                4
            }

            if (!uiState.isHost && uiState.sessionStatus != SessionStatus.CONNECTED) {
                val countdownText = if (uiState.reconnectCountdown != null && uiState.reconnectCountdown > 0) {
                    val mm = uiState.reconnectCountdown / 60
                    val ss = uiState.reconnectCountdown % 60
                    " (${mm}:${if (ss < 10) "0$ss" else "$ss"} restantes)"
                } else ""
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reconectando a la sala...$countdownText",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Jugadores conectados (${uiState.gameState.connectedPlayers.size}/$maxCapacity):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            val isThreePlayers = uiState.gameState.maxPlayers == 3 || uiState.maxPlayers == 3
            if (isThreePlayers) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 En partidas de 3 cada jugador es su propio equipo. Si alguien va al baño o no juega una mano, pulsa su botón 'Reserva' para seguir jugando 1 vs 1 sin cambiar de sala.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.gameState.connectedPlayers) { player ->
                    val isDealer = (uiState.gameState.dealerPlayerId ?: uiState.gameState.connectedPlayers.firstOrNull()?.id) == player.id
                    Card(
                        modifier = if (uiState.isHost) {
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDealer(player.id) }
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val teamColor = when (player.team) {
                                    Team.TEAM_A -> MaterialTheme.colorScheme.primary
                                    Team.TEAM_B -> MaterialTheme.colorScheme.secondary
                                    Team.TEAM_C -> MaterialTheme.colorScheme.tertiary
                                    Team.TEAM_D -> MaterialTheme.colorScheme.outline
                                    Team.RESERVE -> Color(0xFFFFB300)
                                    Team.SPECTATOR -> Color.Gray
                                }

                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(teamColor)
                                    )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isDealer) "${player.name} 🃏" else player.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isDealer) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isDealer) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Reparte 1º",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (player.isHost) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Host)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (player.isLeader) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFFFD54F).copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFA000))
                                    ) {
                                        Text(
                                            text = "👑 Líder",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            val isTwoPlayers = (uiState.gameState.maxPlayers == 2 || uiState.maxPlayers == 2) && !isThreePlayers

                            if (isTwoPlayers) {
                                // En salas de 2 jugadores a la derecha no sale nada
                            } else if (isThreePlayers) {
                                val isPlayerReserve = player.team == Team.RESERVE
                                val playerIndex = uiState.gameState.connectedPlayers.indexOfFirst { it.id == player.id }
                                val myOriginalTeam = when (playerIndex) {
                                    0 -> Team.TEAM_A
                                    1 -> Team.TEAM_B
                                    2 -> Team.TEAM_C
                                    else -> Team.TEAM_A
                                }

                                if (uiState.isHost) {
                                    if (isPlayerReserve) {
                                        FilledTonalButton(
                                            onClick = { viewModel.switchPlayerTeam(player.id, myOriginalTeam) },
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Activar")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.switchPlayerTeam(player.id, Team.RESERVE) },
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("A la reserva")
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPlayerReserve) Color(0xFFFFE082) else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Text(
                                                text = if (isPlayerReserve) "💤 Reserva" else "En Juego",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPlayerReserve) Color(0xFF5D4037) else MaterialTheme.colorScheme.onPrimaryContainer,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                val hostTeam = uiState.gameState.connectedPlayers.find { it.isHost }?.team ?: Team.TEAM_A
                                val canBeLeader = uiState.gameState.maxPlayers in listOf(4, 6, 8) && player.team != hostTeam && player.team != Team.SPECTATOR && !player.isHost
                                var showTeamMenu by remember { mutableStateOf(false) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.isHost && canBeLeader) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (player.isLeader) Color(0xFFFFD54F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (player.isLeader) Color(0xFFFFA000) else Color.LightGray),
                                            modifier = Modifier
                                                .clickable { viewModel.togglePlayerLeader(player.id) }
                                                .padding(end = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Text(text = "👑", fontSize = 13.sp)
                                                if (player.isLeader) {
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "Líder",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFE65100)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Box {
                                        Surface(
                                            color = when (player.team) {
                                                Team.TEAM_A -> MaterialTheme.colorScheme.primaryContainer
                                                Team.TEAM_B -> MaterialTheme.colorScheme.secondaryContainer
                                                Team.TEAM_C -> MaterialTheme.colorScheme.tertiaryContainer
                                                Team.TEAM_D -> MaterialTheme.colorScheme.surfaceVariant
                                                Team.RESERVE -> Color(0xFFFFE082)
                                                Team.SPECTATOR -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                .then(
                                                    if (uiState.isHost) Modifier.clickable { showTeamMenu = true } else Modifier
                                                )
                                        ) {
                                        val (teamColor, teamLabel) = when (player.team) {
                                            Team.TEAM_A -> MaterialTheme.colorScheme.onPrimaryContainer to uiState.gameState.nameTeamA
                                            Team.TEAM_B -> MaterialTheme.colorScheme.onSecondaryContainer to uiState.gameState.nameTeamB
                                            Team.TEAM_C -> MaterialTheme.colorScheme.onTertiaryContainer to uiState.gameState.nameTeamC
                                            Team.TEAM_D -> MaterialTheme.colorScheme.onSurfaceVariant to uiState.gameState.nameTeamD
                                            Team.RESERVE -> Color.Black to "💤 Reserva"
                                            Team.SPECTATOR -> Color.Gray to "Espectador"
                                        }
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = teamLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = teamColor
                                            )
                                            if (uiState.isHost) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Cambiar equipo",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = teamColor
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showTeamMenu,
                                        onDismissRequest = { showTeamMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(uiState.gameState.nameTeamA) },
                                            onClick = {
                                                viewModel.switchPlayerTeam(player.id, Team.TEAM_A)
                                                showTeamMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(uiState.gameState.nameTeamB) },
                                            onClick = {
                                                viewModel.switchPlayerTeam(player.id, Team.TEAM_B)
                                                showTeamMenu = false
                                            }
                                        )
                                        if (uiState.gameState.maxPlayers in listOf(6, 8)) {
                                            DropdownMenuItem(
                                                text = { Text(uiState.gameState.nameTeamC) },
                                                onClick = {
                                                    viewModel.switchPlayerTeam(player.id, Team.TEAM_C)
                                                    showTeamMenu = false
                                                }
                                            )
                                        }
                                        if (uiState.gameState.maxPlayers == 8) {
                                            DropdownMenuItem(
                                                text = { Text(uiState.gameState.nameTeamD) },
                                                onClick = {
                                                    viewModel.switchPlayerTeam(player.id, Team.TEAM_D)
                                                    showTeamMenu = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("💤 Reserva (Poner en reserva)", textAlign = TextAlign.Center) },
                                            onClick = {
                                                viewModel.switchPlayerTeam(player.id, Team.RESERVE)
                                                showTeamMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Selección de equipos en reserva (solo anfitrión) para 6 y 8 jugadores
            if ((uiState.gameState.maxPlayers == 6 || uiState.gameState.maxPlayers == 8) && uiState.isHost) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "👑 Equipos que van a jugar (Solo Anfitrión)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (uiState.gameState.maxPlayers == 6) {
                                "Selecciona qué 2 equipos jugarán en mesa (el restante esperará en reserva):"
                            } else {
                                "Selecciona qué 2 equipos jugarán en mesa (los otros 2 quedarán en reserva):"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val effectiveReserves = when {
                            uiState.gameState.reserveTeams.isNotEmpty() -> uiState.gameState.reserveTeams
                            uiState.gameState.maxPlayers == 6 -> listOf(Team.TEAM_C)
                            uiState.gameState.maxPlayers == 8 -> listOf(Team.TEAM_C, Team.TEAM_D)
                            else -> emptyList()
                        }

                        if (uiState.gameState.maxPlayers == 6) {
                            val allTeams6 = listOf(
                                Team.TEAM_A to uiState.gameState.nameTeamA,
                                Team.TEAM_B to uiState.gameState.nameTeamB,
                                Team.TEAM_C to uiState.gameState.nameTeamC
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allTeams6.forEach { (team, name) ->
                                    val isPlaying = !effectiveReserves.contains(team)
                                    FilterChip(
                                        selected = isPlaying,
                                        onClick = {
                                            if (!isPlaying) {
                                                val currentPlaying = allTeams6.map { it.first }.filter { !effectiveReserves.contains(it) }
                                                val newPlaying = (currentPlaying.takeLast(1) + team).toSet()
                                                val newReserves = allTeams6.map { it.first }.filter { !newPlaying.contains(it) }
                                                viewModel.updateReserveTeams(newReserves)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = if (isPlaying) "⚔️ Juega ($name)" else "💤 Reserva ($name)",
                                                fontSize = 11.5.sp,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else if (uiState.gameState.maxPlayers == 8) {
                            val allTeams8 = listOf(
                                Team.TEAM_A to uiState.gameState.nameTeamA,
                                Team.TEAM_B to uiState.gameState.nameTeamB,
                                Team.TEAM_C to uiState.gameState.nameTeamC,
                                Team.TEAM_D to uiState.gameState.nameTeamD
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allTeams8.chunked(2).forEach { rowTeams ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowTeams.forEach { (team, name) ->
                                            val isPlaying = !effectiveReserves.contains(team)
                                            FilterChip(
                                                selected = isPlaying,
                                                onClick = {
                                                    if (!isPlaying) {
                                                        val currentPlaying = allTeams8.map { it.first }.filter { !effectiveReserves.contains(it) }
                                                        val newPlaying = (currentPlaying.takeLast(1) + team).toSet()
                                                        val newReserves = allTeams8.map { it.first }.filter { !newPlaying.contains(it) }
                                                        viewModel.updateReserveTeams(newReserves)
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = if (isPlaying) "⚔️ Juega ($name)" else "💤 Reserva ($name)",
                                                        fontSize = 11.5.sp,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (uiState.isHost) {
                Button(
                    onClick = {
                        val currentCount = uiState.gameState.connectedPlayers.size
                        if (currentCount < maxCapacity) {
                            showIncompletePlayersDialog = true
                        } else {
                            viewModel.navigateToScoreboard()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir al Marcador de Piedras", fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Esperando a que el anfitrión inicie la partida...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Advertencia si se inicia la partida multijugador sin todos los jugadores
    if (showIncompletePlayersDialog) {
        val currentCount = uiState.gameState.connectedPlayers.size
        val effectiveCapacity = if (uiState.gameState.maxPlayers in listOf(2, 3, 4, 6, 8)) {
            uiState.gameState.maxPlayers
        } else if (uiState.maxPlayers in listOf(2, 3, 4, 6, 8)) {
            uiState.maxPlayers
        } else {
            4
        }

        AlertDialog(
            onDismissRequest = { showIncompletePlayersDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Seguro que quieres iniciar la partida",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Aún no están todos los jugadores conectados ($currentCount de $effectiveCapacity).",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIncompletePlayersDialog = false
                        viewModel.navigateToScoreboard()
                    }
                ) {
                    Text("Sí, empezar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showIncompletePlayersDialog = false }
                ) {
                    Text("No, esperar", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo de confirmación para salir de la sala
    if (showExitHostRoomConfirmation) {
        val hostPlayerName = uiState.gameState.connectedPlayers.find { it.isHost }?.name
            ?: uiState.connectingHostName
            ?: uiState.hostConnectionInfo?.hostName
            ?: "el anfitrión"

        AlertDialog(
            onDismissRequest = { showExitHostRoomConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (uiState.isHost) "Seguro que quieres salir, se cerrará la sala" else "estás seguro que quieres salir de la sala de $hostPlayerName",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (uiState.isHost) {
                        "Si sales al menú principal, la sala se cerrará y se desconectará a los jugadores unidos."
                    } else {
                        "Saldrás de la mesa de juego y volverás al menú de selección de modo."
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitHostRoomConfirmation = false
                        viewModel.exitGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitHostRoomConfirmation = false }
                ) {
                    Text("No", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo de autorización de cambio de equipo en el lobby para el Anfitrión
    val pendingChangeRequest by viewModel.pendingTeamChangeRequest.collectAsState()
    if (pendingChangeRequest != null) {
        val req = pendingChangeRequest!!
        val targetTeamName = when (req.targetTeam) {
            Team.TEAM_A -> uiState.gameState.nameTeamA
            Team.TEAM_B -> uiState.gameState.nameTeamB
            Team.TEAM_C -> uiState.gameState.nameTeamC
            Team.TEAM_D -> uiState.gameState.nameTeamD
            Team.RESERVE -> "💤 Reserva"
            Team.SPECTATOR -> "Espectador"
        }

        AlertDialog(
            onDismissRequest = { viewModel.rejectTeamChange() },
            icon = {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Solicitud de Cambio de Equipo",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "El jugador '${req.playerName}' solicita cambiarse a $targetTeamName.\n\n¿Deseas autorizar el cambio?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.approveTeamChange(req) }
                ) {
                    Text("Autorizar", textAlign = TextAlign.Center)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.rejectTeamChange() }
                ) {
                    Text("Rechazar", textAlign = TextAlign.Center)
                }
            }
        )
    }

    if (showTvCastDialog) {
        TvCastDialog(
            onCastStarted = { viewModel.setTvCastingActive(true) },
            onDismiss = { showTvCastDialog = false }
        )
    }
}
