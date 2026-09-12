package com.app.rondacanaria.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    maxLines: Int = 2,
    minFontSize: TextUnit = 9.sp,
    targetFontSize: TextUnit = 13.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = FontWeight.Bold,
    textAlign: TextAlign? = TextAlign.Center
) {
    var resizedFontSize by remember(text, targetFontSize) { mutableStateOf(targetFontSize) }
    var shouldDraw by remember(text, targetFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        color = color,
        style = style.copy(
            fontSize = resizedFontSize,
            lineHeight = (resizedFontSize.value * 1.15f).sp
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        fontWeight = fontWeight,
        textAlign = textAlign,
        onTextLayout = { result ->
            if (result.didOverflowHeight || result.didOverflowWidth) {
                if (resizedFontSize > minFontSize) {
                    resizedFontSize = (resizedFontSize.value - 0.5f).sp
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        }
    )
}
