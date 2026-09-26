package com.app.rondacanaria.ui.screens

import com.app.rondacanaria.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import com.app.rondacanaria.BuildConfig
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.app.rondacanaria.ui.components.AppTutorialDialog
import com.app.rondacanaria.ui.components.AudioSettingsDialog
import com.app.rondacanaria.ui.components.PrivacyPolicyDialog
import com.app.rondacanaria.ui.components.TvCastDialog
import com.app.rondacanaria.ui.theme.canarianBadgeContainerColor
import com.app.rondacanaria.ui.theme.elPiedreroTopAppBarColors
import com.app.rondacanaria.ui.theme.goldActionButtonColors
import com.app.rondacanaria.ui.theme.headlineContentColor
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
import com.app.rondacanaria.data.model.AvatarCatalog
import com.app.rondacanaria.data.model.Team
import com.app.rondacanaria.ui.ScoreUiState
import com.app.rondacanaria.ui.ScoreViewModel
import com.app.rondacanaria.ui.components.AvatarSelectionDialog
import com.app.rondacanaria.ui.components.LicensesDialog
import com.app.rondacanaria.ui.components.PlayerAvatarBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    uiState: ScoreUiState,
    viewModel: ScoreViewModel
) {
    var showLocalSetupDialog by remember { mutableStateOf(false) }
    var showAudioSettingsDialog by remember { mutableStateOf(false) }
    var showTvCastDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showGlobalAvatarDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var localMaxPlayers by remember { mutableStateOf(4) }
    var localTeamA by remember { mutableStateOf("Equipo A") }
    var localTeamB by remember { mutableStateOf("Equipo B") }
    var localTeamC by remember { mutableStateOf("Equipo C") }
    var localTeamD by remember { mutableStateOf("Equipo D") }
    var localReserve6 by remember { mutableStateOf(Team.TEAM_C) }
    var localReserves8 by remember { mutableStateOf(setOf(Team.TEAM_C, Team.TEAM_D)) }
    var localPlayerNames by remember {
        mutableStateOf(
            listOf("Jugador 1", "Jugador 2", "Jugador 3", "Jugador 4", "Jugador 5", "Jugador 6", "Jugador 7", "Jugador 8")
        )
    }
    var localPlayerAvatars by remember {
        mutableStateOf(
            listOf(
                AvatarCatalog.AVATARS[0].id,
                AvatarCatalog.AVATARS[1].id,
                AvatarCatalog.AVATARS[2].id,
                AvatarCatalog.AVATARS[3].id,
                AvatarCatalog.AVATARS[4].id,
                AvatarCatalog.AVATARS[5].id,
                AvatarCatalog.AVATARS[6].id,
                AvatarCatalog.AVATARS[7].id
            )
        )
    }
    var editingAvatarIndex by remember { mutableStateOf<Int?>(null) }

    // Al pulsar atrás en el menú principal: cerrar diálogos abiertos en vez de salir de la app
    BackHandler(enabled = showTvCastDialog || showAudioSettingsDialog || showLocalSetupDialog || showPrivacyDialog || showLicensesDialog || showGlobalAvatarDialog) {
        if (showGlobalAvatarDialog) {
            showGlobalAvatarDialog = false
        } else if (showTvCastDialog) {
            showTvCastDialog = false
        } else if (showLicensesDialog) {
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
                    // Avatar del Jugador (acceso directo táctil)
                    IconButton(
                        onClick = { showGlobalAvatarDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        PlayerAvatarBadge(
                            avatarId = uiState.selectedAvatarId,
                            size = 32.dp,
                            showBadges = false
                        )
                    }

                    // Historial de Partidas
                    IconButton(
                        onClick = { viewModel.goToHistory() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Ver Historial")
                    }

                    // Ajustes y Accesibilidad
                    IconButton(
                        onClick = { showAudioSettingsDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes de Sonido y Accesibilidad")
                    }

                    // Menú de 3 Puntos (Opciones avanzadas y Recorrido)
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Guía de Uso / Recorrido 📖", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.openTutorial()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cambiar mi Avatar 🎴", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    PlayerAvatarBadge(
                                        avatarId = uiState.selectedAvatarId,
                                        size = 24.dp,
                                        showBadges = false
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showGlobalAvatarDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Transmitir a Smart TV 📺", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showTvCastDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Privacidad y Datos 🛡️", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showPrivacyDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Licencias de Código Abierto ⚖️", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showLicensesDialog = true
                                }
                            )
                        }
                    }
                },
                colors = elPiedreroTopAppBarColors()
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
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                color = androidx.compose.ui.graphics.Color.Transparent,
                modifier = Modifier.size(116.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo El Piedrero",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿Cómo vas a jugar hoy?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = headlineContentColor(),
                textAlign = TextAlign.Center
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    modifier = Modifier.width(32.dp).height(2.dp),
                    shape = CircleShape
                ) {}
                Text("🪨", fontSize = 11.sp)
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    modifier = Modifier.width(32.dp).height(2.dp),
                    shape = CircleShape
                ) {}
            }

            Text(
                text = "Selecciona la modalidad para iniciar la mesa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                textAlign = TextAlign.Center
            )

            // Chip interactivo de Perfil y Avatar del Jugador
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                onClick = { showGlobalAvatarDialog = true },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    PlayerAvatarBadge(
                        avatarId = uiState.selectedAvatarId,
                        size = 36.dp,
                        showBadges = false
                    )
                    Column {
                        Text(
                            text = if (uiState.playerName.isNotBlank()) uiState.playerName else "Mi Avatar Canario",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toca para cambiar avatar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.5.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Opción 1: Marcador Local (1 Dispositivo)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLocalSetupDialog = true },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = canarianBadgeContainerColor(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Style,
                                contentDescription = "Partida Local",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partida Local",
                            style = MaterialTheme.typography.titleLarge,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Opción 2: Partida en Red (Varios Dispositivos Wi-Fi)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.goToNetworkLobby() },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = canarianBadgeContainerColor(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = "Partida Multijugador",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partida Multijugador",
                            style = MaterialTheme.typography.titleLarge,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Historial de Partidas (Últimas 30)",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subapartado: Privacidad y Protección de Datos
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Privacidad y Uso de Datos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacidad y Uso de Datos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% Offline & P2P · Cero recopilación · Licencia MIT",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subapartado: Licencias de Código Abierto (Atribución Apache 2.0 / MIT)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLicensesDialog = true },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Licencias de Software Libre",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Licencias de Código Abierto",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ZXing, AndroidX, Jetpack Compose · Apache 2.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Versión de la Aplicación
            Text(
                text = "El Piedrero v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Diálogo de Configuración para Partida Local
    if (showLocalSetupDialog) {
        AlertDialog(
            onDismissRequest = { showLocalSetupDialog = false },
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = "Configurar Partida Local",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
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
                                onClick = {
                                    localMaxPlayers = count
                                },
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

                    if (localMaxPlayers in listOf(2, 3)) {
                        Text(
                            text = "👤 Nombres de los Jugadores:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Jugador 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                onClick = { editingAvatarIndex = 0 },
                                modifier = Modifier.size(46.dp),
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                PlayerAvatarBadge(
                                    avatarId = localPlayerAvatars[0],
                                    team = Team.TEAM_A,
                                    size = 46.dp,
                                    showBadges = false
                                )
                            }

                            OutlinedTextField(
                                value = localPlayerNames[0],
                                onValueChange = { newN ->
                                    localPlayerNames = localPlayerNames.toMutableList().also { it[0] = newN }
                                    localTeamA = newN
                                },
                                label = { Text("Nombre Jugador 1") },
                                placeholder = { Text("Jugador 1") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Jugador 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                onClick = { editingAvatarIndex = 1 },
                                modifier = Modifier.size(46.dp),
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                PlayerAvatarBadge(
                                    avatarId = localPlayerAvatars[1],
                                    team = Team.TEAM_B,
                                    size = 46.dp,
                                    showBadges = false
                                )
                            }

                            OutlinedTextField(
                                value = localPlayerNames[1],
                                onValueChange = { newN ->
                                    localPlayerNames = localPlayerNames.toMutableList().also { it[1] = newN }
                                    localTeamB = newN
                                },
                                label = { Text("Nombre Jugador 2") },
                                placeholder = { Text("Jugador 2") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Jugador 3 (si Trío)
                        if (localMaxPlayers == 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    onClick = { editingAvatarIndex = 2 },
                                    modifier = Modifier.size(46.dp),
                                    shadowElevation = 2.dp,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
                                ) {
                                    PlayerAvatarBadge(
                                        avatarId = localPlayerAvatars[2],
                                        team = Team.TEAM_C,
                                        size = 46.dp,
                                        showBadges = false
                                    )
                                }

                                OutlinedTextField(
                                    value = localPlayerNames[2],
                                    onValueChange = { newN ->
                                        localPlayerNames = localPlayerNames.toMutableList().also { it[2] = newN }
                                        localTeamC = newN
                                    },
                                    label = { Text("Nombre Jugador 3") },
                                    placeholder = { Text("Jugador 3") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // 4, 6 u 8 Jugadores
                        Text(
                            text = "👥 Equipos y Jugadores (2 por equipo):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personaliza el nombre de cada equipo y de sus 2 integrantes:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val teamsConfig = when (localMaxPlayers) {
                            4 -> listOf(
                                Triple("Equipo A", localTeamA, { v: String -> localTeamA = v }) to (0 to 1),
                                Triple("Equipo B", localTeamB, { v: String -> localTeamB = v }) to (2 to 3)
                            )
                            6 -> listOf(
                                Triple("Equipo A", localTeamA, { v: String -> localTeamA = v }) to (0 to 1),
                                Triple("Equipo B", localTeamB, { v: String -> localTeamB = v }) to (2 to 3),
                                Triple("Equipo C", localTeamC, { v: String -> localTeamC = v }) to (4 to 5)
                            )
                            else -> listOf(
                                Triple("Equipo A", localTeamA, { v: String -> localTeamA = v }) to (0 to 1),
                                Triple("Equipo B", localTeamB, { v: String -> localTeamB = v }) to (2 to 3),
                                Triple("Equipo C", localTeamC, { v: String -> localTeamC = v }) to (4 to 5),
                                Triple("Equipo D", localTeamD, { v: String -> localTeamD = v }) to (6 to 7)
                            )
                        }

                        teamsConfig.forEach { (teamInfo, playerIndices) ->
                            val (defaultName, teamNameVal, onTeamNameChange) = teamInfo
                            val (idx1, idx2) = playerIndices

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = teamNameVal,
                                        onValueChange = onTeamNameChange,
                                        label = { Text("Nombre $defaultName") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Jugador 1 del equipo
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                onClick = { editingAvatarIndex = idx1 },
                                                modifier = Modifier.size(40.dp),
                                                shadowElevation = 1.dp,
                                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                            ) {
                                                PlayerAvatarBadge(
                                                    avatarId = localPlayerAvatars[idx1],
                                                    size = 40.dp,
                                                    showBadges = false
                                                )
                                            }

                                            OutlinedTextField(
                                                value = localPlayerNames[idx1],
                                                onValueChange = { newN ->
                                                    localPlayerNames = localPlayerNames.toMutableList().also { it[idx1] = newN }
                                                },
                                                label = { Text("J1") },
                                                placeholder = { Text("Jugador ${idx1 + 1}") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // Jugador 2 del equipo
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                onClick = { editingAvatarIndex = idx2 },
                                                modifier = Modifier.size(40.dp),
                                                shadowElevation = 1.dp,
                                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                            ) {
                                                PlayerAvatarBadge(
                                                    avatarId = localPlayerAvatars[idx2],
                                                    size = 40.dp,
                                                    showBadges = false
                                                )
                                            }

                                            OutlinedTextField(
                                                value = localPlayerNames[idx2],
                                                onValueChange = { newN ->
                                                    localPlayerNames = localPlayerNames.toMutableList().also { it[idx2] = newN }
                                                },
                                                label = { Text("J2") },
                                                placeholder = { Text("Jugador ${idx2 + 1}") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                    val finalTeamA = if (localMaxPlayers in listOf(2, 3)) localPlayerNames[0].ifBlank { "Jugador 1" } else localTeamA.ifBlank { "Equipo A" }
                    val finalTeamB = if (localMaxPlayers in listOf(2, 3)) localPlayerNames[1].ifBlank { "Jugador 2" } else localTeamB.ifBlank { "Equipo B" }
                    val finalTeamC = if (localMaxPlayers == 3) localPlayerNames[2].ifBlank { "Jugador 3" } else localTeamC.ifBlank { "Equipo C" }
                    val finalTeamD = localTeamD.ifBlank { "Equipo D" }

                    val finalPlayers = if (localMaxPlayers in listOf(2, 3)) {
                        listOf(
                            (localPlayerNames[0].ifBlank { "Jugador 1" }) to Team.TEAM_A,
                            (localPlayerNames[1].ifBlank { "Jugador 2" }) to Team.TEAM_B
                        ) + if (localMaxPlayers == 3) {
                            listOf((localPlayerNames[2].ifBlank { "Jugador 3" }) to Team.TEAM_C)
                        } else emptyList()
                    } else {
                        when (localMaxPlayers) {
                            4 -> listOf(
                                (localPlayerNames[0].ifBlank { "Jugador 1" }) to Team.TEAM_A,
                                (localPlayerNames[2].ifBlank { "Jugador 3" }) to Team.TEAM_B,
                                (localPlayerNames[1].ifBlank { "Jugador 2" }) to Team.TEAM_A,
                                (localPlayerNames[3].ifBlank { "Jugador 4" }) to Team.TEAM_B
                            )
                            6 -> listOf(
                                (localPlayerNames[0].ifBlank { "Jugador 1" }) to Team.TEAM_A,
                                (localPlayerNames[2].ifBlank { "Jugador 3" }) to Team.TEAM_B,
                                (localPlayerNames[1].ifBlank { "Jugador 2" }) to Team.TEAM_A,
                                (localPlayerNames[3].ifBlank { "Jugador 4" }) to Team.TEAM_B,
                                (localPlayerNames[4].ifBlank { "Jugador 5" }) to Team.TEAM_C,
                                (localPlayerNames[5].ifBlank { "Jugador 6" }) to Team.TEAM_C
                            )
                            else -> listOf(
                                (localPlayerNames[0].ifBlank { "Jugador 1" }) to Team.TEAM_A,
                                (localPlayerNames[2].ifBlank { "Jugador 3" }) to Team.TEAM_B,
                                (localPlayerNames[1].ifBlank { "Jugador 2" }) to Team.TEAM_A,
                                (localPlayerNames[3].ifBlank { "Jugador 4" }) to Team.TEAM_B,
                                (localPlayerNames[4].ifBlank { "Jugador 5" }) to Team.TEAM_C,
                                (localPlayerNames[5].ifBlank { "Jugador 6" }) to Team.TEAM_C,
                                (localPlayerNames[6].ifBlank { "Jugador 7" }) to Team.TEAM_D,
                                (localPlayerNames[7].ifBlank { "Jugador 8" }) to Team.TEAM_D
                            )
                        }
                    }

                    val finalAvatars = if (localMaxPlayers in listOf(2, 3)) {
                        listOf(localPlayerAvatars[0], localPlayerAvatars[1]) +
                            if (localMaxPlayers == 3) listOf(localPlayerAvatars[2]) else emptyList()
                    } else {
                        when (localMaxPlayers) {
                            4 -> listOf(localPlayerAvatars[0], localPlayerAvatars[2], localPlayerAvatars[1], localPlayerAvatars[3])
                            6 -> listOf(localPlayerAvatars[0], localPlayerAvatars[2], localPlayerAvatars[1], localPlayerAvatars[3], localPlayerAvatars[4], localPlayerAvatars[5])
                            else -> listOf(localPlayerAvatars[0], localPlayerAvatars[2], localPlayerAvatars[1], localPlayerAvatars[3], localPlayerAvatars[4], localPlayerAvatars[5], localPlayerAvatars[6], localPlayerAvatars[7])
                        }
                    }

                    viewModel.startLocalGame(
                        teamA = finalTeamA,
                        teamB = finalTeamB,
                        teamC = finalTeamC,
                        teamD = finalTeamD,
                        maxPlayers = localMaxPlayers,
                        reserveTeams = reserves,
                        customPlayers = finalPlayers,
                        playerAvatars = finalAvatars
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = goldActionButtonColors()
                ) {
                    Text("Empezar Partida", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocalSetupDialog = false },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cancelar", textAlign = TextAlign.Center)
                }
            }
        )
    }

    editingAvatarIndex?.let { index ->
        AvatarSelectionDialog(
            currentAvatarId = localPlayerAvatars.getOrNull(index),
            title = "Avatar para ${localPlayerNames.getOrNull(index) ?: "Jugador ${index + 1}"}",
            onAvatarSelected = { newAvatar ->
                localPlayerAvatars = localPlayerAvatars.toMutableList().also { it[index] = newAvatar }
            },
            onDismissRequest = { editingAvatarIndex = null }
        )
    }

    if (showAudioSettingsDialog) {
        AudioSettingsDialog(
            masterVolume = uiState.masterVolume,
            musicVolume = uiState.musicVolume,
            sfxVolume = uiState.sfxVolume,
            isMusicEnabled = uiState.isMusicEnabled,
            isSfxEnabled = uiState.isSfxEnabled,
            isVibrationEnabled = uiState.isVibrationEnabled,
            fontScale = uiState.fontScale,
            isDealReminderEnabled = uiState.isDealReminderEnabled,
            dealReminderSeconds = uiState.dealReminderSeconds,
            themeMode = uiState.themeMode,
            onMasterVolumeChange = { viewModel.setMasterVolume(it) },
            onMusicVolumeChange = { viewModel.setMusicVolume(it) },
            onSfxVolumeChange = { viewModel.setSfxVolume(it) },
            onToggleMusic = { viewModel.toggleMusic(it) },
            onToggleSfx = { viewModel.toggleSfx(it) },
            onToggleVibration = { viewModel.toggleVibration(it) },
            onFontScaleChange = { viewModel.setFontScale(it) },
            onToggleDealReminder = { viewModel.setDealReminderEnabled(it) },
            onDealReminderSecondsChange = { viewModel.setDealReminderSeconds(it) },
            onThemeModeChange = { viewModel.setThemeMode(it) },
            onSkipSong = { viewModel.skipSong() },
            onOpenTutorial = {
                showAudioSettingsDialog = false
                viewModel.openTutorial()
            },
            onDismiss = { showAudioSettingsDialog = false }
        )
    }

    if (showGlobalAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatarId = uiState.selectedAvatarId,
            title = "Elige tu Avatar Canario 🎴",
            onAvatarSelected = { newAvatarId ->
                viewModel.selectAvatar(newAvatarId)
            },
            onDismissRequest = { showGlobalAvatarDialog = false }
        )
    }

    if (uiState.showTutorialDialog) {
        AppTutorialDialog(
            onDismissRequest = { viewModel.dismissTutorial() },
            onComplete = { viewModel.completeTutorial() }
        )
    }

    if (showTvCastDialog) {
        TvCastDialog(
            onCastStarted = { viewModel.setTvCastingActive(true) },
            onDismiss = { showTvCastDialog = false }
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
