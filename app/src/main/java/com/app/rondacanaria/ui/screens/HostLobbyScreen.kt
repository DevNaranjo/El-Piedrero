package com.app.rondacanaria.ui.screens

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesa Anfitrión - El Piedrero", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitGame() }) {
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
                        text = "💡 En partidas de 3 cada jugador es su propio equipo. Si alguien va al baño o no juega una mano, toca su estado y ponlo como 'Suplente' para seguir jugando 1 vs 1 sin cambiar de sala.",
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

                            var showTeamMenu by remember { mutableStateOf(false) }
                            Box {
                                val isTwoPlayers = (uiState.gameState.maxPlayers == 2 || uiState.maxPlayers == 2) && !isThreePlayers
                                val canInteractWithTeam = !isTwoPlayers

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
                                    modifier = if (canInteractWithTeam) Modifier.clickable { showTeamMenu = true } else Modifier
                                ) {
                                    val (teamColor, teamLabel) = when (player.team) {
                                        Team.TEAM_A -> MaterialTheme.colorScheme.onPrimaryContainer to (if (isThreePlayers || isTwoPlayers) player.name else uiState.gameState.nameTeamA)
                                        Team.TEAM_B -> MaterialTheme.colorScheme.onSecondaryContainer to (if (isThreePlayers || isTwoPlayers) player.name else uiState.gameState.nameTeamB)
                                        Team.TEAM_C -> MaterialTheme.colorScheme.onTertiaryContainer to (if (isThreePlayers || isTwoPlayers) player.name else uiState.gameState.nameTeamC)
                                        Team.TEAM_D -> MaterialTheme.colorScheme.onSurfaceVariant to uiState.gameState.nameTeamD
                                        Team.RESERVE -> Color.Black to "💤 Suplente"
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
                                        if (canInteractWithTeam) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Cambiar estado",
                                                modifier = Modifier.size(16.dp),
                                                tint = teamColor
                                            )
                                        }
                                    }
                                }

                                if (canInteractWithTeam) {
                                    DropdownMenu(
                                        expanded = showTeamMenu,
                                        onDismissRequest = { showTeamMenu = false }
                                    ) {
                                        if (isThreePlayers) {
                                            val playerIndex = uiState.gameState.connectedPlayers.indexOfFirst { it.id == player.id }
                                            val myOriginalTeam = when (playerIndex) {
                                                0 -> Team.TEAM_A
                                                1 -> Team.TEAM_B
                                                2 -> Team.TEAM_C
                                                else -> Team.TEAM_A
                                            }

                                            if (player.team == Team.RESERVE) {
                                                DropdownMenuItem(
                                                    text = { Text("🟢 Activar Jugador", textAlign = TextAlign.Center) },
                                                    onClick = {
                                                        viewModel.switchPlayerTeam(player.id, myOriginalTeam)
                                                        showTeamMenu = false
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("💤 Poner como Suplente (Baño / Descanso)", textAlign = TextAlign.Center) },
                                                    onClick = {
                                                        viewModel.switchPlayerTeam(player.id, Team.RESERVE)
                                                        showTeamMenu = false
                                                    }
                                                )
                                            }
                                        } else {
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
                            text = "👑 Equipos en Reserva (Solo Anfitrión)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (uiState.gameState.maxPlayers == 6) {
                                "Selecciona qué equipo esperará en reserva (los 2 equipos activos saldrán en el marcador):"
                            } else {
                                "Selecciona qué 2 equipos estarán en reserva (los 2 equipos activos saldrán en el marcador):"
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Team.TEAM_A to uiState.gameState.nameTeamA,
                                    Team.TEAM_B to uiState.gameState.nameTeamB,
                                    Team.TEAM_C to uiState.gameState.nameTeamC
                                ).forEach { (team, name) ->
                                    val isRes = effectiveReserves.contains(team)
                                    FilterChip(
                                        selected = isRes,
                                        onClick = {
                                            viewModel.updateReserveTeams(listOf(team))
                                        },
                                        label = {
                                            Text(
                                                text = if (isRes) "💤 Reserva ($name)" else name,
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
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Team.TEAM_A to uiState.gameState.nameTeamA,
                                    Team.TEAM_B to uiState.gameState.nameTeamB,
                                    Team.TEAM_C to uiState.gameState.nameTeamC,
                                    Team.TEAM_D to uiState.gameState.nameTeamD
                                ).chunked(2).forEach { rowTeams ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowTeams.forEach { (team, name) ->
                                            val currentRes = effectiveReserves
                                            val isRes = currentRes.contains(team)
                                            FilterChip(
                                                selected = isRes,
                                                onClick = {
                                                    val newRes = if (isRes) {
                                                        if (currentRes.size > 1) currentRes - team else currentRes
                                                    } else {
                                                        if (currentRes.size < 2) currentRes + team else listOf(currentRes.last(), team)
                                                    }
                                                    viewModel.updateReserveTeams(newRes)
                                                },
                                                label = {
                                                    Text(
                                                        text = if (isRes) "💤 Reserva ($name)" else name,
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
                onClick = { viewModel.navigateToScoreboard() },
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
