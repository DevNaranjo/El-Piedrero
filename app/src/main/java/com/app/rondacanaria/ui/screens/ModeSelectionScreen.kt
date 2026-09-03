package com.app.rondacanaria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.app.rondacanaria.ui.components.AudioSettingsDialog
import com.app.rondacanaria.ui.components.PrivacyPolicyDialog
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
import com.app.rondacanaria.ui.components.LicensesDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    uiState: ScoreUiState,
    viewModel: ScoreViewModel
) {
    var showLocalSetupDialog by remember { mutableStateOf(false) }
    var showAudioSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var localMaxPlayers by remember { mutableStateOf(4) }
    var localTeamA by remember { mutableStateOf("Equipo A") }
    var localTeamB by remember { mutableStateOf("Equipo B") }
    var localTeamC by remember { mutableStateOf("Equipo C") }
    var localTeamD by remember { mutableStateOf("Equipo D") }
    var localReserve6 by remember { mutableStateOf(Team.TEAM_C) }
    var localReserves8 by remember { mutableStateOf(setOf(Team.TEAM_C, Team.TEAM_D)) }

    // Al pulsar atrás en el menú principal: cerrar diálogos abiertos en vez de salir de la app
    BackHandler(enabled = showAudioSettingsDialog || showLocalSetupDialog || showPrivacyDialog || showLicensesDialog) {
        if (showLicensesDialog) {
            showLicensesDialog = false
        } else if (showPrivacyDialog) {
            showPrivacyDialog = false
        } else if (showAudioSettingsDialog) {
            showAudioSettingsDialog = false
        } else if (showLocalSetupDialog) {
            showLocalSetupDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("El Piedrero 🃏", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showPrivacyDialog = true }) {
                        Icon(Icons.Default.Shield, contentDescription = "Privacidad y Uso de Datos")
                    }
                    IconButton(onClick = { showAudioSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes de Sonido")
                    }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Subapartado: Privacidad y Protección de Datos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Privacidad y Uso de Datos",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacidad y Uso de Datos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% Offline & P2P · Cero recopilación · Licencia MIT",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subapartado: Licencias de Código Abierto (Atribución Apache 2.0 / MIT)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLicensesDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Licencias de Software Libre",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Licencias de Código Abierto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ZXing, AndroidX, Jetpack Compose · Apache 2.0",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Configuración para Partida Local
    if (showLocalSetupDialog) {
        AlertDialog(
            onDismissRequest = { showLocalSetupDialog = false },
            title = { Text("Configurar Partida Local", textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                    // Selector de equipos que van a jugar para 6 jugadores (3 equipos de 2)
                    if (localMaxPlayers == 6) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚔️ Equipos que van a jugar (Selecciona 2):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Los 2 equipos seleccionados jugarán en mesa. El restante esperará en reserva.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val allTeams6 = listOf(
                            Team.TEAM_A to localTeamA,
                            Team.TEAM_B to localTeamB,
                            Team.TEAM_C to localTeamC
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allTeams6.forEach { (team, label) ->
                                val isPlaying = localReserve6 != team
                                FilterChip(
                                    selected = isPlaying,
                                    onClick = {
                                        if (!isPlaying) {
                                            val currentPlaying = allTeams6.map { it.first }.filter { it != localReserve6 }
                                            val newPlaying = (currentPlaying.takeLast(1) + team).toSet()
                                            localReserve6 = allTeams6.map { it.first }.first { !newPlaying.contains(it) }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isPlaying) "⚔️ Juega ($label)" else "💤 Reserva ($label)",
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

                    // Selector de equipos que van a jugar para 8 jugadores (4 equipos de 2)
                    if (localMaxPlayers == 8) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚔️ Equipos que van a jugar (Selecciona 2):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Los 2 equipos seleccionados jugarán en mesa y saldrán en el marcador. Los otros 2 esperarán en reserva.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val allTeams8 = listOf(
                            Team.TEAM_A to localTeamA,
                            Team.TEAM_B to localTeamB,
                            Team.TEAM_C to localTeamC,
                            Team.TEAM_D to localTeamD
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
                                    rowTeams.forEach { (team, label) ->
                                        val isPlaying = !localReserves8.contains(team)
                                        FilterChip(
                                            selected = isPlaying,
                                            onClick = {
                                                if (!isPlaying) {
                                                    val currentPlaying = allTeams8.map { it.first }.filter { !localReserves8.contains(it) }
                                                    val newPlaying = (currentPlaying.takeLast(1) + team).toSet()
                                                    localReserves8 = allTeams8.map { it.first }.filter { !newPlaying.contains(it) }.toSet()
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = if (isPlaying) "⚔️ Juega ($label)" else "💤 Reserva ($label)",
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

    if (showAudioSettingsDialog) {
        AudioSettingsDialog(
            masterVolume = uiState.masterVolume,
            musicVolume = uiState.musicVolume,
            sfxVolume = uiState.sfxVolume,
            isMusicEnabled = uiState.isMusicEnabled,
            isSfxEnabled = uiState.isSfxEnabled,
            onMasterVolumeChange = { viewModel.setMasterVolume(it) },
            onMusicVolumeChange = { viewModel.setMusicVolume(it) },
            onSfxVolumeChange = { viewModel.setSfxVolume(it) },
            onToggleMusic = { viewModel.toggleMusic(it) },
            onToggleSfx = { viewModel.toggleSfx(it) },
            onDismiss = { showAudioSettingsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showLicensesDialog) {
        LicensesDialog(
            onDismiss = { showLicensesDialog = false }
        )
    }
}
