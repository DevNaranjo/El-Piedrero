package com.app.rondacanaria.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.model.Team
import com.app.rondacanaria.ui.ScoreUiState
import com.app.rondacanaria.ui.ScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    uiState: ScoreUiState,
    viewModel: ScoreViewModel
) {
    var playerName by remember { mutableStateOf(uiState.playerName) }
    var teamAName by remember { mutableStateOf(uiState.teamAName) }
    var teamBName by remember { mutableStateOf(uiState.teamBName) }
    var teamCName by remember { mutableStateOf(uiState.teamCName) }
    var teamDName by remember { mutableStateOf(uiState.teamDName) }
    var maxPlayers by remember { mutableStateOf(uiState.maxPlayers) }
    var hostReserve6 by remember { mutableStateOf(Team.TEAM_C) }
    var hostReserves8 by remember { mutableStateOf(setOf(Team.TEAM_C, Team.TEAM_D)) }
    var showHostDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partida en Red (Wi-Fi) 🃏", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goToModeSelection() }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al inicio")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Multijugador en Red",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Crea una sala o únete escaneando el código QR",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = playerName,
                onValueChange = {
                    playerName = it
                    viewModel.setPlayerName(it)
                },
                label = { Text("Tu Nombre / Alias") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showHostDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Mesa (Host con QR)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.openScanner() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unirse a Mesa (Escanear QR)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }

    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            title = { Text("Configurar Mesa de Ronda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Capacidad de la mesa:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            2 to "1v1",
                            3 to "Trío",
                            4 to "2x2",
                            6 to "3x2",
                            8 to "4x2"
                        ).forEach { (count, subtext) ->
                            val isSelected = maxPlayers == count
                            OutlinedButton(
                                onClick = { maxPlayers = count },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = if (isSelected) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$count",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = subtext,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    val modeDescription = when (maxPlayers) {
                        2 -> "👤 Mano a Mano: 1 contra 1 (Sin reservas)"
                        3 -> "👥 En Trío (1 vs 1 vs 1): Si alguien va al baño o no juega una mano, puede ponerse en reserva para seguir jugando 1 vs 1 sin cambiar de sala."
                        4 -> "👥 Por Parejas: 2 contra 2 (4 jugadores)"
                        6 -> "👥 6 Jugadores: 3 equipos de 2 (A, B y C con reservas)"
                        else -> "👥 8 Jugadores: 4 equipos de 2 (A, B, C y D con reservas)"
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = modeDescription,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    if (maxPlayers in listOf(2, 3)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "👤 En partidas de $maxPlayers jugadores, el nombre del equipo es automáticamente el nombre de cada jugador al conectarse (sin elección de equipo).",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = teamAName,
                            onValueChange = { teamAName = it },
                            label = { Text("Nombre Equipo A") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = teamBName,
                            onValueChange = { teamBName = it },
                            label = { Text("Nombre Equipo B") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (maxPlayers in listOf(6, 8)) {
                            OutlinedTextField(
                                value = teamCName,
                                onValueChange = { teamCName = it },
                                label = { Text("Nombre Equipo C") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (maxPlayers == 8) {
                            OutlinedTextField(
                                value = teamDName,
                                onValueChange = { teamDName = it },
                                label = { Text("Nombre Equipo D") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Selector de equipo en reserva para 6 jugadores (3 equipos de 2)
                    if (maxPlayers == 6) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Equipo que estará en reserva:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Solo jugarán 2 equipos a la vez. El equipo en reserva esperará su turno y no saldrá en el marcador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Team.TEAM_A to teamAName,
                                Team.TEAM_B to teamBName,
                                Team.TEAM_C to teamCName
                            ).forEach { (team, label) ->
                                val isReserve = hostReserve6 == team
                                FilterChip(
                                    selected = isReserve,
                                    onClick = { hostReserve6 = team },
                                    label = {
                                        Text(
                                            text = if (isReserve) "💤 Reserva ($label)" else label,
                                            fontSize = 11.sp,
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

                    // Selector de equipos en reserva para 8 jugadores (4 equipos de 2)
                    if (maxPlayers == 8) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Equipos que estarán en reserva (2 equipos):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Solo los 2 equipos activos saldrán en el marcador para no saturar la pantalla.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Team.TEAM_A to teamAName,
                                Team.TEAM_B to teamBName,
                                Team.TEAM_C to teamCName,
                                Team.TEAM_D to teamDName
                            ).chunked(2).forEach { rowTeams ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowTeams.forEach { (team, label) ->
                                        val isReserve = hostReserves8.contains(team)
                                        FilterChip(
                                            selected = isReserve,
                                            onClick = {
                                                if (isReserve) {
                                                    if (hostReserves8.size > 1) {
                                                        hostReserves8 = hostReserves8 - team
                                                    }
                                                } else {
                                                    if (hostReserves8.size < 2) {
                                                        hostReserves8 = hostReserves8 + team
                                                    } else {
                                                        hostReserves8 = setOf(hostReserves8.last(), team)
                                                    }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = if (isReserve) "💤 Reserva ($label)" else label,
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
            },
            confirmButton = {
                Button(onClick = {
                    val selectedReserves = when (maxPlayers) {
                        6 -> listOf(hostReserve6)
                        8 -> hostReserves8.toList()
                        else -> emptyList()
                    }
                    val finalA = if (maxPlayers in listOf(2, 3)) uiState.playerName.ifBlank { "Jugador 1" } else teamAName
                    val finalB = if (maxPlayers in listOf(2, 3)) "" else teamBName
                    val finalC = if (maxPlayers in listOf(2, 3)) "" else teamCName
                    viewModel.setRoomConfig(finalA, finalB, finalC, teamDName, maxPlayers, selectedReserves)
                    viewModel.startHosting(selectedReserves)
                    showHostDialog = false
                }) {
                    Text("Abrir Mesa", textAlign = TextAlign.Center)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHostDialog = false }) {
                    Text("Cancelar", textAlign = TextAlign.Center)
                }
            }
        )
    }
}
