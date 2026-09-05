package com.app.rondacanaria.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.app.rondacanaria.R
import com.app.rondacanaria.data.model.Team

@Composable
fun MesaCardsDealDialog(
    dealerName: String,
    dealerTeam: Team,
    dealerTeamName: String,
    currentDeal: Int,
    currentHand: Int,
    maxDeals: Int,
    canApply: Boolean = true,
    onApplyStones: (team: Team, stones: Int, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Selección múltiple de cartas 1, 2, 3 y 4
    val selectedCards = remember { mutableStateListOf<Int>() }
    // Selector de ¿Bien dada?
    var isBienDada by remember { mutableStateOf(false) }

    val cardsStones = selectedCards.sum()
    val totalStones = cardsStones + (if (isBienDada) 1 else 0)

    val explanationText = remember(selectedCards.toList(), isBienDada) {
        if (selectedCards.isNotEmpty()) {
            val sortedCards = selectedCards.sorted()
            val cardsListStr = sortedCards.joinToString(", ")
            val cardLabel = if (sortedCards.size == 1) "la carta $cardsListStr" else "las cartas $cardsListStr"
            val coincideVerb = if (sortedCards.size == 1) "ha" else "han"
            if (isBienDada) {
                "Se sumarán $totalStones piedras porque $coincideVerb coincidido $cardLabel (más 1 piedra por bien dada)"
            } else {
                val verb = if (totalStones == 1) "Se sumará" else "Se sumarán"
                val noun = if (totalStones == 1) "1 piedra" else "$totalStones piedras"
                "$verb $noun porque $coincideVerb coincidido $cardLabel"
            }
        } else {
            if (isBienDada) {
                "Se sumará 1 piedra porque no se ha repetido ninguna carta"
            } else {
                "No se sumarán piedras porque no se ha repetido ninguna carta y no ha coincidido ninguna"
            }
        }
    }

    val cardsData = listOf(
        Triple(1, R.drawable.card_mesa_1, 1),
        Triple(2, R.drawable.card_mesa_2, 2),
        Triple(3, R.drawable.card_mesa_3, 3),
        Triple(4, R.drawable.card_mesa_4, 4)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🃏", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Cartas a la Mesa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reparto $currentDeal de $maxDeals · Reparte: $dealerName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Selecciona las cartas que hayan coincidido con el orden de reparto:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Los 4 botones de cartas con sus imágenes oficiales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cardsData.forEach { (number, drawableRes, stones) ->
                        val isSelected = selectedCards.contains(number)
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 1.0f,
                            label = "scaleCard_$number"
                        )
                        val borderColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        }
                        val borderWidth = if (isSelected) 3.dp else 1.dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .scale(scale)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(borderWidth, borderColor),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shadowElevation = if (isSelected) 6.dp else 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.68f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (isSelected) {
                                            selectedCards.remove(number)
                                        } else {
                                            selectedCards.add(number)
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = drawableRes),
                                        contentDescription = "Carta $number",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp)
                                    )
                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionada",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (isSelected) {
                                            selectedCards.remove(number)
                                        } else {
                                            selectedCards.add(number)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "+$stones ${if (stones == 1) "piedra" else "piedras"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Opción selector "¿Bien dada?"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBienDada) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isBienDada) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isBienDada = !isBienDada }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¿Bien dada?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Suma +1 piedra al repartidor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBienDada,
                            onCheckedChange = { isBienDada = it }
                        )
                    }
                }

                // Texto explicativo dinámico según las condiciones
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (totalStones > 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (totalStones > 0) Icons.Default.Info else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (totalStones > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = explanationText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalStones > 0) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (totalStones > 0 && canApply) {
                        val reason = if (selectedCards.isNotEmpty()) {
                            val cardsStr = selectedCards.sorted().joinToString(", ")
                            val matchText = if (selectedCards.size == 1) "coincidió la carta $cardsStr" else "coincidieron las cartas $cardsStr"
                            "Cartas a la mesa: $matchText${if (isBienDada) " (+1 bien dada)" else ""}"
                        } else {
                            "Cartas a la mesa: bien dada (no se repitió carta)"
                        }
                        onApplyStones(dealerTeam, totalStones, reason)
                    }
                    onDismiss()
                },
                enabled = canApply || totalStones == 0
            ) {
                Text(
                    text = if (totalStones > 0) {
                        "Sumar $totalStones ${if (totalStones == 1) "piedra" else "piedras"}"
                    } else {
                        "Continuar (0 piedras)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cerrar")
            }
        }
    )
}
