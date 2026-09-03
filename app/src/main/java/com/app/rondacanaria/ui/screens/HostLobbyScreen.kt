package com.app.rondacanaria.ui.screens

import androidx.activity.compose.BackHandler
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
import com.app.rondacanaria.ui.ScoreUiState
import com.app.rondacanaria.ui.ScoreViewModel
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

    BackHandler {
        if (showIncompletePlayersDialog) {
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
                title = { Text("Mesa Anfitrión - El Piedrero", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showExitHostRoomConfirmation = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (player.isHost) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Host)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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

                                if (isPlayerReserve) {
                                    FilledTonalButton(
                                        onClick = { viewModel.switchPlayerTeam(player.id, myOriginalTeam) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFFFE082),
                                            contentColor = Color(0xFF5D4037)
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Activar jugador", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Activar", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.switchPlayerTeam(player.id, Team.RESERVE) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PauseCircle, contentDescription = "Poner jugador en reserva", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reserva", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                var showTeamMenu by remember { mutableStateOf(false) }
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
                                            .clickable { showTeamMenu = true }
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
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Cambiar equipo",
                                                modifier = Modifier.size(16.dp),
                                                tint = teamColor
                                            )
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

            // Selección de equipos en reserva (solo anfitrión) para 6 y 8 jugadores
            if (uiState.gameState.maxPlayers == 6 || uiState.gameState.maxPlayers == 8) {
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

    // Diálogo de confirmación para salir de la sala del Anfitrión
    if (showExitHostRoomConfirmation) {
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
                    text = "Seguro que quieres salir, se cerrará la sala",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Si sales al menú principal, la sala se cerrará y se desconectará a los jugadores unidos.",
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
}
