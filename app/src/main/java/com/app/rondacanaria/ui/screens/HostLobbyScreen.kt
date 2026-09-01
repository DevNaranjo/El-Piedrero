package com.app.rondacanaria.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

            Text(
                text = "Jugadores conectados (${uiState.gameState.connectedPlayers.size}/${uiState.gameState.maxPlayers}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

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
                                val (teamColor, teamLabel) = when (player.team) {
                                    Team.TEAM_A -> MaterialTheme.colorScheme.primary to uiState.gameState.nameTeamA
                                    Team.TEAM_B -> MaterialTheme.colorScheme.secondary to uiState.gameState.nameTeamB
                                    Team.TEAM_C -> MaterialTheme.colorScheme.tertiary to uiState.gameState.nameTeamC
                                    Team.SPECTATOR -> Color.Gray to "Espectador"
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

                            Surface(
                                color = when (player.team) {
                                    Team.TEAM_A -> MaterialTheme.colorScheme.primaryContainer
                                    Team.TEAM_B -> MaterialTheme.colorScheme.secondaryContainer
                                    Team.TEAM_C -> MaterialTheme.colorScheme.tertiaryContainer
                                    Team.SPECTATOR -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val (teamColor, teamLabel) = when (player.team) {
                                    Team.TEAM_A -> MaterialTheme.colorScheme.onPrimaryContainer to uiState.gameState.nameTeamA
                                    Team.TEAM_B -> MaterialTheme.colorScheme.onSecondaryContainer to uiState.gameState.nameTeamB
                                    Team.TEAM_C -> MaterialTheme.colorScheme.onTertiaryContainer to uiState.gameState.nameTeamC
                                    Team.SPECTATOR -> Color.Gray to "Espectador"
                                }
                                Text(
                                    text = teamLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = teamColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
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
                Text("Ir al Marcador de Piedras", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
