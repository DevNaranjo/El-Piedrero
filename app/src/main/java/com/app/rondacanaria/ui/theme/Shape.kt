package com.app.rondacanaria.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// Formas y Radios de Esquina Material 3 - El Piedrero
// Estandarización de curvaturas orgánicas y suaves
// =============================================================================

val ElPiedreroShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // Micro-badges, avisos compactos
    small = RoundedCornerShape(10.dp),       // Chips, botones pequeños, controles
    medium = RoundedCornerShape(14.dp),      // Botones de acción, inputs OutlinedTextField
    large = RoundedCornerShape(20.dp),       // Tarjetas de menú, diálogos modales
    extraLarge = RoundedCornerShape(26.dp)   // Tarjetas principales de marcador (Player Card)
)
