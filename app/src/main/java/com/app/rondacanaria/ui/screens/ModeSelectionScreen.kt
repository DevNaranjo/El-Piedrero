package com.app.rondacanaria.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.app.rondacanaria.R
import com.app.rondacanaria.data.model.Team
import com.app.rondacanaria.ui.ScoreUiState
import com.app.rondacanaria.ui.ScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    uiState: ScoreUiState,
    viewModel: ScoreViewModel
) {
    var showLocalSetupDialog by remember { mutableStateOf(false) }
    var localMaxPlayers by remember { mutableStateOf(4) }
    var localTeamA by remember { mutableStateOf("Equipo A") }
    var localTeamB by remember { mutableStateOf("Equipo B") }
    var localTeamC by remember { mutableStateOf("Equipo C") }
    var localTeamD by remember { mutableStateOf("Equipo D") }
    var localReserve6 by remember { mutableStateOf(Team.TEAM_C) }
    var localReserves8 by remember { mutableStateOf(setOf(Team.TEAM_C, Team.TEAM_D)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("El Piedrero 🃏", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.goToHistory() }) {
                        Icon(Icons.Default.History, contentDescription = "Ver Historial")
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo El Piedrero",
                modifier = Modifier
                    .size(115.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿Cómo vas a jugar hoy?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Selecciona la modalidad para iniciar la mesa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Opción 1: Marcador Local (1 Dispositivo)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLocalSetupDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partida Local",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Un solo móvil en el centro de la mesa para contar las piedras. Sin Wi-Fi ni configuración.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Opción 2: Partida en Red (Varios Dispositivos Wi-Fi)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.goToNetworkLobby() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partida Multijugador",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sincroniza el marcador entre varios móviles por Wi-Fi o Zona Wi-Fi mediante código QR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Opción 3: Ver Historial de Partidas
            OutlinedButton(
                onClick = { viewModel.goToHistory() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Historial de Partidas (Últimas 30)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Diálogo de Configuración para Partida Local
    if (showLocalSetupDialog) {
        AlertDialog(
            onDismissRequest = { showLocalSetupDialog = false },
            title = { Text("Configurar Partida Local", textAlign = TextAlign.Center) },
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
                            val isSelected = localMaxPlayers == count
                            OutlinedButton(
                                onClick = { localMaxPlayers = count },
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
                                border = BorderStroke(
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

                    val modeDescription = when (localMaxPlayers) {
                        2 -> "👤 Mano a Mano: 1 contra 1 (Sin reservas)"
                        3 -> "👥 En Trío (1 vs 1 vs 1): Si alguien va al baño o no juega una mano, se puede poner en reserva para seguir jugando 1 vs 1 sin tener que reiniciar."
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

                    OutlinedTextField(
                        value = localTeamA,
                        onValueChange = { localTeamA = it },
                        label = { Text(if (localMaxPlayers in listOf(2, 3)) "Nombre Jugador 1" else "Nombre Equipo A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = localTeamB,
                        onValueChange = { localTeamB = it },
                        label = { Text(if (localMaxPlayers in listOf(2, 3)) "Nombre Jugador 2" else "Nombre Equipo B") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (localMaxPlayers in listOf(3, 6, 8)) {
                        OutlinedTextField(
                            value = localTeamC,
                            onValueChange = { localTeamC = it },
                            label = { Text(if (localMaxPlayers == 3) "Nombre Jugador 3" else "Nombre Equipo C") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (localMaxPlayers == 8) {
                        OutlinedTextField(
                            value = localTeamD,
                            onValueChange = { localTeamD = it },
                            label = { Text("Nombre Equipo D") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Selector de equipo en reserva para 6 jugadores (3 equipos de 2)
                    if (localMaxPlayers == 6) {
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Team.TEAM_A to localTeamA,
                                Team.TEAM_B to localTeamB,
                                Team.TEAM_C to localTeamC
                            ).forEach { (team, label) ->
                                val isReserve = localReserve6 == team
                                FilterChip(
                                    selected = isReserve,
                                    onClick = { localReserve6 = team },
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
                    if (localMaxPlayers == 8) {
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
                                Team.TEAM_A to localTeamA,
                                Team.TEAM_B to localTeamB,
                                Team.TEAM_C to localTeamC,
                                Team.TEAM_D to localTeamD
                            ).chunked(2).forEach { rowTeams ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowTeams.forEach { (team, label) ->
                                        val isReserve = localReserves8.contains(team)
                                        FilterChip(
                                            selected = isReserve,
                                            onClick = {
                                                if (isReserve) {
                                                    if (localReserves8.size > 1) {
                                                        localReserves8 = localReserves8 - team
                                                    }
                                                } else {
                                                    if (localReserves8.size < 2) {
                                                        localReserves8 = localReserves8 + team
                                                    } else {
                                                        localReserves8 = setOf(localReserves8.last(), team)
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
                    showLocalSetupDialog = false
                    val reserves = when (localMaxPlayers) {
                        6 -> listOf(localReserve6)
                        8 -> localReserves8.toList()
                        else -> emptyList()
                    }
                    val finalTeamA = if (localMaxPlayers in listOf(2, 3)) localTeamA.ifBlank { "Jugador 1" } else localTeamA.ifBlank { "Equipo A" }
                    val finalTeamB = if (localMaxPlayers in listOf(2, 3)) localTeamB.ifBlank { "Jugador 2" } else localTeamB.ifBlank { "Equipo B" }
                    val finalTeamC = if (localMaxPlayers == 3) localTeamC.ifBlank { "Jugador 3" } else localTeamC.ifBlank { "Equipo C" }
                    val finalTeamD = localTeamD.ifBlank { "Equipo D" }

                    viewModel.startLocalGame(
                        teamA = finalTeamA,
                        teamB = finalTeamB,
                        teamC = finalTeamC,
                        teamD = finalTeamD,
                        maxPlayers = localMaxPlayers,
                        reserveTeams = reserves
                    )
                }) {
                    Text("Empezar Partida", textAlign = TextAlign.Center)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalSetupDialog = false }) {
                    Text("Cancelar", textAlign = TextAlign.Center)
                }
            }
        )
    }
}
