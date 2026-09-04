package com.app.rondacanaria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.rondacanaria.data.history.DEFAULT_CANTO_BUTTON_ORDER
import com.app.rondacanaria.data.model.CantoType

@Composable
fun CustomizeButtonsDialog(
    currentOrder: List<CantoType>,
    onSaveOrder: (List<CantoType>) -> Unit,
    onDismiss: () -> Unit
) {
    var workingList by remember(currentOrder) { mutableStateOf(currentOrder) }

    fun moveUp(index: Int) {
        if (index > 0) {
            val newList = workingList.toMutableList()
            val temp = newList[index]
            newList[index] = newList[index - 1]
            newList[index - 1] = temp
            workingList = newList
        }
    }

    fun moveDown(index: Int) {
        if (index < workingList.lastIndex) {
            val newList = workingList.toMutableList()
            val temp = newList[index]
            newList[index] = newList[index + 1]
            newList[index + 1] = temp
            workingList = newList
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DashboardCustomize,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Distribución de Botones",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️ Los botones se colocan en parejas (de 2 en 2). Usa las flechas ▲ y ▼ para mover los cantos y jugadas a tu posición deseada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(workingList, key = { _, item -> item.name }) { index, cantoType ->
                        val rowNum = (index / 2) + 1
                        val side = if (index % 2 == 0) "Izquierda" else "Derecha"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = cantoType.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Fila $rowNum · $side",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { moveUp(index) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Mover hacia arriba",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = { moveDown(index) },
                                        enabled = index < workingList.lastIndex,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Mover hacia abajo",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveOrder(workingList)
                    onDismiss()
                }
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        workingList = DEFAULT_CANTO_BUTTON_ORDER
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restablecer")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
