package com.app.rondacanaria.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val gameState = uiState.gameState
    val isMultiplayerClient = !uiState.isLocalGame && !uiState.isHost

    var selectedTeamForCanto by remember(uiState.myTeam, isMultiplayerClient) {
        mutableStateOf(if (isMultiplayerClient && uiState.myTeam != Team.SPECTATOR && uiState.myTeam != Team.RESERVE) uiState.myTeam else Team.TEAM_A)
    }
    var showEndGameConfirmation by remember { mutableStateOf(false) }
    var customAdjustTeam by remember { mutableStateOf<Team?>(null) }
    var showSwitchTeamDialog by remember { mutableStateOf(false) }
    var showMoveHistoryDialog by remember { mutableStateOf(false) }

    val hasTeamC = gameState.maxPlayers in listOf(3, 6, 8) || uiState.maxPlayers in listOf(3, 6, 8)
    val hasTeamD = gameState.maxPlayers == 8 || uiState.maxPlayers == 8

    val effectiveReserveTeams = when {
        gameState.reserveTeams.isNotEmpty() -> gameState.reserveTeams
        (gameState.maxPlayers == 6 || uiState.maxPlayers == 6) -> listOf(Team.TEAM_C)
        (gameState.maxPlayers == 8 || uiState.maxPlayers == 8) -> listOf(Team.TEAM_C, Team.TEAM_D)
        else -> emptyList()
    }

    val isThreePlayers = gameState.maxPlayers == 3 || uiState.maxPlayers == 3
    val isTwoPlayers = (gameState.maxPlayers == 2 || uiState.maxPlayers == 2) && !isThreePlayers

    val maxDeals = when {
        isTwoPlayers -> 6
        isThreePlayers -> 4
        else -> 3
    }

    // En tríos solo se puede usar la opción de suplente ANTES de contar cualquier piedra.
    val noPiedrasAun = gameState.moveHistory.isEmpty()

    // En 2 jugadores NO hay reservas bajo ningún concepto.
    // En 3 jugadores se permite reserva solo si no se ha contado ninguna piedra todavía.
    // En 6 y 8 jugadores (con 3 o 4 equipos) se permite rotar qué 2 equipos juegan en mesa tanto en local como en multijugador.
    val canOpenReserveOrTeamDialog = when {
        isTwoPlayers -> false
        isThreePlayers -> noPiedrasAun
        hasTeamC || hasTeamD -> true
        else -> !uiState.isLocalGame
    }

    val canSwitchTeam = !uiState.isLocalGame && !isTwoPlayers && !isThreePlayers

    val isTeamAReserve = effectiveReserveTeams.contains(Team.TEAM_A)
    val isTeamBReserve = effectiveReserveTeams.contains(Team.TEAM_B)
    val isTeamCReserve = effectiveReserveTeams.contains(Team.TEAM_C)
    val isTeamDReserve = effectiveReserveTeams.contains(Team.TEAM_D)

    val showTeamA = !isTeamAReserve
    val showTeamB = !isTeamBReserve
    val showTeamC = hasTeamC && !isTeamCReserve
    val showTeamD = hasTeamD && !isTeamDReserve

    val isMultiplayer = !uiState.isLocalGame
    val isReserve = isMultiplayer && (uiState.myTeam == Team.RESERVE || effectiveReserveTeams.contains(uiState.myTeam) || uiState.myTeam == Team.SPECTATOR)

    // En local (dispositivo compartido en mesa): SIEMPRE se pueden modificar ambos equipos activos en mesa.
    // En multijugador: cada jugador SOLO puede modificar su respectivo equipo, de los rivales NO.
    val canModifyA = showTeamA && (uiState.isLocalGame || (uiState.myTeam == Team.TEAM_A && !isReserve))
    val canModifyB = showTeamB && (uiState.isLocalGame || (uiState.myTeam == Team.TEAM_B && !isReserve))
    val canModifyC = showTeamC && (uiState.isLocalGame || (uiState.myTeam == Team.TEAM_C && !isReserve))
    val canModifyD = showTeamD && (uiState.isLocalGame || (uiState.myTeam == Team.TEAM_D && !isReserve))

    val activeTeamsCount = listOf(showTeamA, showTeamB, showTeamC, showTeamD).count { it }
    val isCompactCards = activeTeamsCount > 2

    LaunchedEffect(effectiveReserveTeams) {
        val activeTeamsList = listOfNotNull(
            if (showTeamA) Team.TEAM_A else null,
            if (showTeamB) Team.TEAM_B else null,
            if (showTeamC) Team.TEAM_C else null,
            if (showTeamD) Team.TEAM_D else null
        )
        if (effectiveReserveTeams.contains(selectedTeamForCanto) || !activeTeamsList.contains(selectedTeamForCanto)) {
            selectedTeamForCanto = activeTeamsList.firstOrNull() ?: Team.TEAM_A
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("El Piedrero 🃏", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (uiState.isLocalGame) {
                                "Partida Local"
                            } else if (uiState.isHost) {
                                "Mesa Anfitrión"
                            } else {
                                val teamName = when (uiState.myTeam) {
                                    Team.TEAM_A -> gameState.nameTeamA
                                    Team.TEAM_B -> gameState.nameTeamB
                                    Team.TEAM_C -> gameState.nameTeamC
                                    Team.TEAM_D -> gameState.nameTeamD
                                    Team.RESERVE -> "💤 Reserva"
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
                    IconButton(
                        onClick = { viewModel.undoLastMove() },
                        enabled = gameState.moveHistory.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer última jugada",
                            tint = if (gameState.moveHistory.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = { showMoveHistoryDialog = true }) {
                        BadgedBox(
                            badge = {
                                if (gameState.moveHistory.isNotEmpty()) {
                                    Badge {
                                        Text("${gameState.moveHistory.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Registro de movimientos")
                        }
                    }
                    if (canOpenReserveOrTeamDialog) {
                        IconButton(onClick = { showSwitchTeamDialog = true }) {
                            Icon(
                                imageVector = if (isThreePlayers) Icons.Default.PauseCircle else Icons.Default.SwapHoriz,
                                contentDescription = if (isThreePlayers) "Poner en Reserva / Descanso" else "Cambiar Equipo"
                            )
                        }
                    }
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
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isLocalGame && uiState.sessionStatus != SessionStatus.CONNECTED) {
                ConnectionStatusBanner(status = uiState.sessionStatus, isHost = uiState.isHost)
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Aviso si el jugador está en la Reserva
            if (isReserve) {
                Surface(
                    color = Color(0xFFFFE082),
                    shape = RoundedCornerShape(10.dp),
                    modifier = if (canOpenReserveOrTeamDialog) {
                        Modifier
                            .fillMaxWidth()
                            .clickable { showSwitchTeamDialog = true }
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💤 Reserva", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isThreePlayers) {
                                "En descanso / baño. Pulsa aquí para volver a jugar."
                            } else {
                                "Estás en Reserva (Solo lectura). Pulsa aquí para entrar a jugar."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Sumador de Reparto de Cartas
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Reparto de cartas:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Mano / Reparto ${gameState.currentDeal} de $maxDeals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.changeDeal(gameState.currentDeal - 1) },
                            enabled = !isReserve && gameState.currentDeal > 1,
                            modifier = Modifier
                                .size(44.dp)
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Reparto anterior",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "${gameState.currentDeal}º / $maxDeals",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        val isAtMaxDeals = gameState.currentDeal >= maxDeals
                        FilledIconButton(
                            onClick = {
                                val nextDeal = if (isAtMaxDeals) 1 else gameState.currentDeal + 1
                                viewModel.changeDeal(nextDeal)
                            },
                            enabled = !isReserve,
                            modifier = Modifier
                                .size(44.dp)
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isAtMaxDeals) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isAtMaxDeals) Icons.Default.Refresh else Icons.Default.Add,
                                contentDescription = if (isAtMaxDeals) "Reiniciar a reparto 1" else "Siguiente reparto",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tarjeta Equipo A (si no está en reserva)
                if (showTeamA) {
                    RondaScoreCard(
                        modifier = Modifier.weight(1f),
                        teamName = gameState.nameTeamA,
                        score = gameState.scoreTeamA,
                        wins = gameState.winsTeamA,
                        isSelected = selectedTeamForCanto == Team.TEAM_A,
                        teamColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        isCompact = isCompactCards,
                        canModify = canModifyA,
                        onSelect = { if (canModifyA) selectedTeamForCanto = Team.TEAM_A },
                        onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_A, delta, if (delta > 0) "+1 piedra manual" else "-1 piedra manual") },
                        onCustomAdjustClick = { if (canModifyA) customAdjustTeam = Team.TEAM_A }
                    )
                }

                // Tarjeta Equipo B (si no está en reserva)
                if (showTeamB) {
                    RondaScoreCard(
                        modifier = Modifier.weight(1f),
                        teamName = gameState.nameTeamB,
                        score = gameState.scoreTeamB,
                        wins = gameState.winsTeamB,
                        isSelected = selectedTeamForCanto == Team.TEAM_B,
                        teamColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        isCompact = isCompactCards,
                        canModify = canModifyB,
                        onSelect = { if (canModifyB) selectedTeamForCanto = Team.TEAM_B },
                        onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_B, delta, if (delta > 0) "+1 piedra manual" else "-1 piedra manual") },
                        onCustomAdjustClick = { if (canModifyB) customAdjustTeam = Team.TEAM_B }
                    )
                }

                // Tarjeta Equipo C (si 3, 6 u 8 jugadores y no está en reserva)
                if (showTeamC) {
                    RondaScoreCard(
                        modifier = Modifier.weight(1f),
                        teamName = gameState.nameTeamC,
                        score = gameState.scoreTeamC,
                        wins = gameState.winsTeamC,
                        isSelected = selectedTeamForCanto == Team.TEAM_C,
                        teamColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        isCompact = isCompactCards,
                        canModify = canModifyC,
                        onSelect = { if (canModifyC) selectedTeamForCanto = Team.TEAM_C },
                        onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_C, delta, if (delta > 0) "+1 piedra manual" else "-1 piedra manual") },
                        onCustomAdjustClick = { if (canModifyC) customAdjustTeam = Team.TEAM_C }
                    )
                }

                // Tarjeta Equipo D (si 8 jugadores y no está en reserva)
                if (showTeamD) {
                    RondaScoreCard(
                        modifier = Modifier.weight(1f),
                        teamName = gameState.nameTeamD,
                        score = gameState.scoreTeamD,
                        wins = gameState.winsTeamD,
                        isSelected = selectedTeamForCanto == Team.TEAM_D,
                        teamColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        isCompact = isCompactCards,
                        canModify = canModifyD,
                        onSelect = { if (canModifyD) selectedTeamForCanto = Team.TEAM_D },
                        onManualAdjust = { delta -> viewModel.manualScoreChange(Team.TEAM_D, delta, if (delta > 0) "+1 piedra manual" else "-1 piedra manual") },
                        onCustomAdjustClick = { if (canModifyD) customAdjustTeam = Team.TEAM_D }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selector interactivo o indicador de equipo propio con botón para cambiar
            if (isMultiplayer) {
                val myTeamName = when (uiState.myTeam) {
                    Team.TEAM_A -> gameState.nameTeamA
                    Team.TEAM_B -> gameState.nameTeamB
                    Team.TEAM_C -> gameState.nameTeamC
                    Team.TEAM_D -> gameState.nameTeamD
                    Team.RESERVE -> "💤 Reserva"
                    else -> "Espectador"
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isThreePlayers || isTwoPlayers) "Jugador: $myTeamName" else "Equipo: $myTeamName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isThreePlayers && !noPiedrasAun) {
                                    Text(
                                        text = "Suplente bloqueado (ya hay piedras)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        if (isThreePlayers && canOpenReserveOrTeamDialog) {
                            FilledTonalButton(
                                onClick = { showSwitchTeamDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val isMyTeamReserve = uiState.myTeam == Team.RESERVE
                                Icon(
                                    imageVector = if (isMyTeamReserve) Icons.Default.PlayArrow else Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMyTeamReserve) "Activarme" else "Suplente",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (canSwitchTeam) {
                            FilledTonalButton(
                                onClick = { showSwitchTeamDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cantar a:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val teams = buildList {
                            if (showTeamA) add(Team.TEAM_A to gameState.nameTeamA)
                            if (showTeamB) add(Team.TEAM_B to gameState.nameTeamB)
                            if (showTeamC) add(Team.TEAM_C to gameState.nameTeamC)
                            if (showTeamD) add(Team.TEAM_D to gameState.nameTeamD)
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            teams.forEach { (team, name) ->
                                FilterChip(
                                    selected = selectedTeamForCanto == team,
                                    onClick = { selectedTeamForCanto = team },
                                    label = {
                                        Text(
                                            text = name,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        if (hasTeamC || hasTeamD) {
                            FilledTonalButton(
                                onClick = { showSwitchTeamDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Equipos", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botonera de Jugadas y Cantos en Cuadrícula Simétrica de 2 Columnas (Material 3)
            val cantoTargetTeam = if (isMultiplayer) uiState.myTeam else selectedTeamForCanto

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fila 1: Cantos básicos de mano
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.RONDA) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = CantoType.RONDA.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.PARRANDA) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = CantoType.PARRANDA.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Fila 2: Cantos mayores de mano
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.CARACOL) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = CantoType.CARACOL.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.CARACOLILLO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(
                            text = CantoType.CARACOLILLO.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Fila 3: Jugadas de mesa (Majo y Limpiar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.MAJO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = CantoType.MAJO.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.LIMPIAR) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = CantoType.LIMPIAR.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Fila 4: Jugadas de mesa dobles (Majo y Limpio, Contramajo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.MAJO_Y_LIMPIO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            text = CantoType.MAJO_Y_LIMPIO.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.CONTRAMAJO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            text = CantoType.CONTRAMAJO.displayName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Fila 5: Cadena de majos de alto valor (Requetemajo, Requetecontramajo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.REQUETEMAJO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = CantoType.REQUETEMAJO.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.callCanto(cantoTargetTeam, CantoType.REQUETECONTRAMAJO) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(
                            text = CantoType.REQUETECONTRAMAJO.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila fija de acciones principales: Deshacer y Terminar Partida (50% de ancho c/u)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.undoLastMove() },
                    enabled = gameState.moveHistory.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deshacer",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = { showEndGameConfirmation = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Terminar Partida",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo con el registro de movimientos de la partida actual
    if (showMoveHistoryDialog) {
        MoveHistoryDialog(
            history = gameState.moveHistory,
            nameTeamA = gameState.nameTeamA,
            nameTeamB = gameState.nameTeamB,
            nameTeamC = gameState.nameTeamC,
            nameTeamD = gameState.nameTeamD,
            currentDeal = gameState.currentDeal,
            maxDeals = maxDeals,
            onUndo = { viewModel.undoLastMove() },
            onDismiss = { showMoveHistoryDialog = false }
        )
    }

    // Diálogo para Sumar Piedras de Golpe (+N)
    if (customAdjustTeam != null) {
        val targetTeam = customAdjustTeam!!
        val targetName = when (targetTeam) {
            Team.TEAM_A -> gameState.nameTeamA
            Team.TEAM_B -> gameState.nameTeamB
            Team.TEAM_C -> gameState.nameTeamC
            Team.TEAM_D -> gameState.nameTeamD
            else -> "Equipo"
        }
        val currentScore = when (targetTeam) {
            Team.TEAM_A -> gameState.scoreTeamA.totalPiedras
            Team.TEAM_B -> gameState.scoreTeamB.totalPiedras
            Team.TEAM_C -> gameState.scoreTeamC.totalPiedras
            Team.TEAM_D -> gameState.scoreTeamD.totalPiedras
            else -> 0
        }

        AddCustomPiedrasDialog(
            teamName = targetName,
            currentScore = currentScore,
            onDismiss = { customAdjustTeam = null },
            onConfirm = { amount ->
                viewModel.manualScoreChange(targetTeam, amount, "Suma rápida (+$amount)")
            }
        )
    }

    // Diálogo interactivo para Cambiar de Equipo o pasar a Reserva
    if (canOpenReserveOrTeamDialog && showSwitchTeamDialog) {
        SwitchTeamDialog(
            currentTeam = uiState.myTeam,
            teamAName = gameState.nameTeamA,
            teamBName = gameState.nameTeamB,
            teamCName = gameState.nameTeamC,
            teamDName = gameState.nameTeamD,
            hasTeamC = hasTeamC,
            hasTeamD = hasTeamD,
            maxPlayers = if (isThreePlayers) 3 else (if (isTwoPlayers) 2 else gameState.maxPlayers),
            isHost = uiState.isHost || uiState.isLocalGame,
            isLocal = uiState.isLocalGame,
            reserveTeams = effectiveReserveTeams,
            onUpdateReserveTeams = { viewModel.updateReserveTeams(it) },
            onDismiss = { showSwitchTeamDialog = false },
            onSelectTeam = { chosenTeam ->
                viewModel.switchMyTeam(chosenTeam)
                if (chosenTeam != Team.RESERVE && chosenTeam != Team.SPECTATOR) {
                    selectedTeamForCanto = chosenTeam
                }
                if (!uiState.isHost && !uiState.isLocalGame) {
                    Toast.makeText(context, "Solicitud enviada al Anfitrión. Esperando autorización...", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Diálogo de autorización de cambio de equipo para el Anfitrión (solo en multijugador)
    val pendingChangeRequest by viewModel.pendingTeamChangeRequest.collectAsState()
    if (uiState.isHost && !uiState.isLocalGame && pendingChangeRequest != null) {
        val req = pendingChangeRequest!!
        val targetTeamName = when (req.targetTeam) {
            Team.TEAM_A -> gameState.nameTeamA
            Team.TEAM_B -> gameState.nameTeamB
            Team.TEAM_C -> gameState.nameTeamC
            Team.TEAM_D -> gameState.nameTeamD
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
                    Text("Terminar", textAlign = TextAlign.Center)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndGameConfirmation = false }) {
                    Text("Cancelar", textAlign = TextAlign.Center)
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
            Team.TEAM_D -> gameState.nameTeamD
            else -> "Ganador"
        }

        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(48.dp)) },
            title = { Text("¡Partida Ganada! 🏆", textAlign = TextAlign.Center) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🏆 $winnerName ha ganado la partida completando las 10 Buenas (21 piedras totales).",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏆 Marcador de Victorias",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val victoriesSummary = buildString {
                                append("${gameState.nameTeamA}: ${gameState.winsTeamA} vict.")
                                append("  |  ${gameState.nameTeamB}: ${gameState.winsTeamB} vict.")
                                if (hasTeamC) append("  |  ${gameState.nameTeamC}: ${gameState.winsTeamC} vict.")
                                if (hasTeamD) append("  |  ${gameState.nameTeamD}: ${gameState.winsTeamD} vict.")
                            }
                            Text(
                                text = victoriesSummary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if ((gameState.maxPlayers in listOf(6, 8) || uiState.maxPlayers in listOf(6, 8)) && gameState.winnerTeam != null) {
                        val allTeams = when {
                            (gameState.maxPlayers == 6 || uiState.maxPlayers == 6) -> listOf(Team.TEAM_A, Team.TEAM_B, Team.TEAM_C)
                            (gameState.maxPlayers == 8 || uiState.maxPlayers == 8) -> listOf(Team.TEAM_A, Team.TEAM_B, Team.TEAM_C, Team.TEAM_D)
                            else -> emptyList()
                        }
                        val activeTeams = allTeams.filter { !effectiveReserveTeams.contains(it) }
                        val loserTeam = activeTeams.firstOrNull { it != gameState.winnerTeam }
                        val loserName = when (loserTeam) {
                            Team.TEAM_A -> gameState.nameTeamA
                            Team.TEAM_B -> gameState.nameTeamB
                            Team.TEAM_C -> gameState.nameTeamC
                            Team.TEAM_D -> gameState.nameTeamD
                            else -> null
                        }
                        if (loserName != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💤 El $loserName pasa automáticamente a la reserva para la siguiente partida.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.isHost) {
                    Button(onClick = { viewModel.resetGame(resetWins = false) }) {
                        Text("Siguiente Partida", textAlign = TextAlign.Center)
                    }
                } else {
                    Button(onClick = { viewModel.exitGame() }) {
                        Text("Salir", textAlign = TextAlign.Center)
                    }
                }
            },
            dismissButton = {
                if (uiState.isHost) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.exitGame() }) {
                            Text("Salir al Menú", textAlign = TextAlign.Center)
                        }
                        TextButton(onClick = { viewModel.resetGame(resetWins = true) }) {
                            Text("Reiniciar a 0", textAlign = TextAlign.Center)
                        }
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
    wins: Int = 0,
    isSelected: Boolean,
    teamColor: Color,
    contentColor: Color,
    isCompact: Boolean = false,
    canModify: Boolean = true,
    onSelect: () -> Unit = {},
    onManualAdjust: (Int) -> Unit,
    onCustomAdjustClick: (() -> Unit)? = null
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
                .padding(if (isCompact) 8.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = teamName,
                    fontSize = if (isCompact) 14.5.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = if (wins > 0) Color(0xFFFFB300) else contentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "🏆 $wins ${if (wins == 1) "victoria" else "victorias"}",
                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (wins > 0) Color.Black else contentColor.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
                }
            }

            // Badge de Estado: Malas vs Buenas
            if (score.isInBuenas) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))))
                        .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isCompact) "🌟 BUENAS (${score.buenas}/10)" else "🌟 ¡EN BUENAS! (${score.buenas}/10)",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isCompact) 10.5.sp else 12.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.12f))
                        .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Malas (${score.malas}/11)",
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 10.5.sp else 12.sp
                    )
                }
            }

            // Contador Principal de Piedras
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${score.totalPiedras}",
                    fontSize = if (isCompact) 36.sp else 46.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
                Text(
                    text = "Piedras / 21",
                    fontSize = if (isCompact) 9.5.sp else 11.sp,
                    color = contentColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }

            // Botones de ajuste manual (+1 / -1 / +N) o indicador de Equipo Rival bloqueado
            if (!canModify) {
                Surface(
                    color = contentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Equipo rival bloqueado",
                            tint = contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Equipo Rival",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Restar (-)
                    FilledTonalButton(
                        onClick = { onManualAdjust(-1) },
                        enabled = score.totalPiedras > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = contentColor.copy(alpha = 0.14f),
                            contentColor = contentColor
                        )
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Botón Sumar (+)
                    FilledTonalButton(
                        onClick = { onManualAdjust(1) },
                        enabled = score.totalPiedras < TeamScore.TOTAL_PIEDRAS_VICTORY,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = contentColor.copy(alpha = 0.22f),
                            contentColor = contentColor
                        )
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Botón Sumar Golpe (+N)
                    if (onCustomAdjustClick != null) {
                        FilledTonalButton(
                            onClick = onCustomAdjustClick,
                            enabled = score.totalPiedras < TeamScore.TOTAL_PIEDRAS_VICTORY,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = contentColor.copy(alpha = 0.32f),
                                contentColor = contentColor
                            )
                        ) {
                            Text(
                                text = "+N",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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

@Composable
fun AddCustomPiedrasDialog(
    teamName: String,
    currentScore: Int,
    maxPiedras: Int = TeamScore.TOTAL_PIEDRAS_VICTORY,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    val remaining = (maxPiedras - currentScore).coerceAtLeast(0)
    val parsedInt = textValue.toIntOrNull() ?: 0
    val isValid = parsedInt in 1..remaining

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AddCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Sumar Piedras de Golpe",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Equipo: $teamName (Tiene $currentScore/21 piedras)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 2) {
                            textValue = input
                        }
                    },
                    label = { Text("Cantidad a sumar") },
                    placeholder = { Text("Ej: 7") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Suma rápida habitual:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(2, 3, 4, 5).forEach { amount ->
                        FilterChip(
                            selected = parsedInt == amount,
                            onClick = { textValue = amount.toString() },
                            label = { Text("+$amount", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(6, 7, 8, 10).forEach { amount ->
                        FilterChip(
                            selected = parsedInt == amount,
                            onClick = { textValue = amount.toString() },
                            label = { Text("+$amount", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        )
                    }
                }

                if (textValue.isNotBlank() && !isValid) {
                    Text(
                        text = if (parsedInt <= 0) "Indica un número mayor a 0" else "Máximo posible: +$remaining piedras",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(parsedInt)
                        onDismiss()
                    }
                },
                enabled = isValid
            ) {
                Text("Sumar +$parsedInt", textAlign = TextAlign.Center)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", textAlign = TextAlign.Center)
            }
        }
    )
}

@Composable
fun SwitchTeamDialog(
    currentTeam: Team,
    teamAName: String,
    teamBName: String,
    teamCName: String,
    teamDName: String,
    hasTeamC: Boolean,
    hasTeamD: Boolean,
    maxPlayers: Int = 4,
    isHost: Boolean = false,
    isLocal: Boolean = false,
    reserveTeams: List<Team> = emptyList(),
    onUpdateReserveTeams: (List<Team>) -> Unit = {},
    onDismiss: () -> Unit,
    onSelectTeam: (Team) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (maxPlayers == 3) Icons.Default.PauseCircle else Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            val dialogTitle = when {
                maxPlayers == 3 -> "Suplente / Descanso (Trío)"
                isLocal -> "Equipos en Reserva"
                else -> "Cambiar de Equipo / Reserva"
            }
            Text(
                text = dialogTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (maxPlayers == 3) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLocal) {
                                "💡 En partidas de 3 cada jugador compite individualmente. Si alguien va al baño o descansa una mano, puedes ponerlo de suplente para seguir jugando 1 vs 1."
                            } else {
                                "💡 En partidas de 3 cada jugador compite individualmente con su propio nombre. No se puede cambiar de equipo, solo elegir si estás activo o de suplente (descanso / baño)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (!isLocal) {
                    if (maxPlayers != 3) {
                        Text(
                            text = "Elige tu equipo o colócate en reserva:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                onSelectTeam(Team.TEAM_A)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (currentTeam == Team.TEAM_A) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = teamAName + if (currentTeam == Team.TEAM_A) " (Actual)" else "",
                                fontWeight = if (currentTeam == Team.TEAM_A) FontWeight.Black else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onSelectTeam(Team.TEAM_B)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (currentTeam == Team.TEAM_B) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = teamBName + if (currentTeam == Team.TEAM_B) " (Actual)" else "",
                                fontWeight = if (currentTeam == Team.TEAM_B) FontWeight.Black else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (hasTeamC) {
                            OutlinedButton(
                                onClick = {
                                    onSelectTeam(Team.TEAM_C)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (currentTeam == Team.TEAM_C) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = teamCName + if (currentTeam == Team.TEAM_C) " (Actual)" else "",
                                    fontWeight = if (currentTeam == Team.TEAM_C) FontWeight.Black else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (hasTeamD) {
                            OutlinedButton(
                                onClick = {
                                    onSelectTeam(Team.TEAM_D)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (currentTeam == Team.TEAM_D) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = teamDName + if (currentTeam == Team.TEAM_D) " (Actual)" else "",
                                    fontWeight = if (currentTeam == Team.TEAM_D) FontWeight.Black else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onSelectTeam(Team.RESERVE)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (currentTeam == Team.RESERVE) {
                                ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE082))
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = "💤 Reserva (Pasar a suplente)" + if (currentTeam == Team.RESERVE) " (Actual)" else "",
                                fontWeight = if (currentTeam == Team.RESERVE) FontWeight.Black else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        if (!isHost && currentTeam != Team.SPECTATOR) {
                            if (currentTeam == Team.RESERVE) {
                                Button(
                                    onClick = {
                                        onSelectTeam(Team.TEAM_A)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🟢 Volver a Jugar (Activarme)", textAlign = TextAlign.Center)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onSelectTeam(Team.RESERVE)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE082).copy(alpha = 0.6f))
                                ) {
                                    Text("💤 Ponerme de Suplente (Baño / Descanso)", textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // Sección para el Anfitrión / Modo Local: rotar qué equipos van a reserva y cuáles juegan
                if (isHost && (hasTeamC || hasTeamD)) {
                    if (!isLocal) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    val headerText = when {
                        isLocal && maxPlayers == 3 -> "👑 Gestión de Suplente:"
                        isLocal -> "⚔️ Equipos que van a jugar (2 activos):"
                        maxPlayers == 3 -> "👑 Gestión de Suplente (Anfitrión):"
                        else -> "👑 Equipos que van a jugar (Solo Anfitrión):"
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (maxPlayers == 3) "Toca un jugador para ponerlo de suplente (ej. baño), o desmárcalo para que jueguen los 3." else "Selecciona qué 2 equipos juegan en mesa (los otros 2 quedan en reserva).",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val availableTeams = buildList {
                        add(Team.TEAM_A to teamAName)
                        add(Team.TEAM_B to teamBName)
                        if (hasTeamC) add(Team.TEAM_C to teamCName)
                        if (hasTeamD) add(Team.TEAM_D to teamDName)
                    }

                    if (availableTeams.size == 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableTeams.forEach { (team, name) ->
                                val isRes = reserveTeams.contains(team)
                                FilterChip(
                                    selected = isRes,
                                    onClick = {
                                        if (maxPlayers == 3 && isRes) {
                                            onUpdateReserveTeams(emptyList())
                                        } else {
                                            onUpdateReserveTeams(listOf(team))
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isRes) "💤 Suplente ($name)" else name,
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
                    } else if (availableTeams.size == 4) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTeams.chunked(2).forEach { rowTeams ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowTeams.forEach { (team, name) ->
                                        val isPlaying = !reserveTeams.contains(team)
                                        FilterChip(
                                            selected = isPlaying,
                                            onClick = {
                                                if (isHost && !isPlaying) {
                                                    val currentPlaying = availableTeams.map { it.first }.filter { !reserveTeams.contains(it) }
                                                    val newPlaying = (currentPlaying.takeLast(1) + team).toSet()
                                                    val newRes = availableTeams.map { it.first }.filter { !newPlaying.contains(it) }
                                                    onUpdateReserveTeams(newRes)
                                                }
                                            },
                                            enabled = isHost,
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", textAlign = TextAlign.Center)
            }
        }
    )
}

@Composable
fun MoveHistoryDialog(
    history: List<GameMove>,
    nameTeamA: String,
    nameTeamB: String,
    nameTeamC: String,
    nameTeamD: String,
    currentDeal: Int = 1,
    maxDeals: Int = 3,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Registro de Partida 📜",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no se han producido jugadas o movimientos en esta partida.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movimientos (${history.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "En curso: Reparto $currentDeal de $maxDeals",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val groupedDeals = remember(history) {
                        history.groupBy { it.dealNumber.coerceAtLeast(1) }
                            .toList()
                            .sortedByDescending { it.first }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        groupedDeals.forEach { (dealNum, movesInDeal) ->
                            item(key = "deal_header_$dealNum") {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Style,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Reparto $dealNum de $maxDeals",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Text(
                                            text = "${movesInDeal.size} ${if (movesInDeal.size == 1) "jugada" else "jugadas"}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            items(movesInDeal.asReversed(), key = { it.id }) { move ->
                                val teamName = when (move.teamId) {
                                    Team.TEAM_A -> nameTeamA
                                    Team.TEAM_B -> nameTeamB
                                    Team.TEAM_C -> nameTeamC
                                    Team.TEAM_D -> nameTeamD
                                    else -> "Equipo"
                                }
                                val isPositive = move.deltaPiedras > 0
                                val deltaColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                val deltaBadgeBg = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = teamName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "R${move.dealNumber.coerceAtLeast(1)}",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = move.reason + if (move.authorName != null) " (por ${move.authorName})" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                text = "${move.previousTotalPiedras} ➔ ${move.newTotalPiedras} piedras",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Surface(
                                            color = deltaBadgeBg,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isPositive) "+${move.deltaPiedras}" else "${move.deltaPiedras}",
                                                color = deltaColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (history.isNotEmpty()) {
                Button(
                    onClick = {
                        onUndo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deshacer Último", textAlign = TextAlign.Center)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", textAlign = TextAlign.Center)
            }
        }
    )
}
