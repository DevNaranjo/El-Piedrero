package com.app.rondacanaria.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Componente de texto responsivo que reduce su tamaño de fuente de forma continua
 * hasta ajustarse al contenedor disponible sin generar desbordamientos ni puntos suspensivos.
 */
@Composable
fun AutoResizedText(
    text: String,
    modifier: Modifier = Modifier,
    targetFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 10.sp,
    step: TextUnit = 1.sp,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 2
) {
    var resizedFontSize by remember(text, targetFontSize) { mutableStateOf(targetFontSize) }
    var shouldDraw by remember(text, targetFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        maxLines = maxLines,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else (resizedFontSize.value * 1.15f).sp,
        overflow = TextOverflow.Clip,
        style = style.copy(fontSize = resizedFontSize),
        onTextLayout = { result ->
            if (result.didOverflowHeight || result.didOverflowWidth) {
                if (resizedFontSize > minFontSize) {
                    resizedFontSize = (resizedFontSize.value - step.value).coerceAtLeast(minFontSize.value).sp
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        },
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        }
    )
}
