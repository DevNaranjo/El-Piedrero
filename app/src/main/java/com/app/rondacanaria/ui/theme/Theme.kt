package com.app.rondacanaria.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro")
}

private const val THEME_TRANSITION_DURATION_MS = 380
val ThemeTransitionSpec = tween<Color>(
    durationMillis = THEME_TRANSITION_DURATION_MS,
    easing = FastOutSlowInEasing
)

val LocalIsDarkTheme = compositionLocalOf { false }

// =============================================================================
// Esquemas de Color Material 3 - El Piedrero (Light & Dark)
// =============================================================================

private val LightColorScheme = lightColorScheme(
    primary = AtlanticBluePrimaryLight,
    onPrimary = AtlanticBlueOnPrimaryLight,
    primaryContainer = AtlanticBluePrimaryContainerLight,
    onPrimaryContainer = AtlanticBlueOnPrimaryContainerLight,

    secondary = CanaryOchreSecondaryLight,
    onSecondary = CanaryOchreOnSecondaryLight,
    secondaryContainer = CanaryOchreSecondaryContainerLight,
    onSecondaryContainer = CanaryOchreOnSecondaryContainerLight,

    tertiary = CanaryGoldTertiaryLight,
    onTertiary = CanaryGoldOnTertiaryLight,
    tertiaryContainer = CanaryGoldTertiaryContainerLight,
    onTertiaryContainer = CanaryGoldOnTertiaryContainerLight,

    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    background = NeutralBackgroundLight,
    onBackground = NeutralOnBackgroundLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralOnSurfaceLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralOnSurfaceVariantLight,

    outline = NeutralOutlineLight,
    outlineVariant = NeutralOutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = AtlanticBluePrimaryDark,
    onPrimary = AtlanticBlueOnPrimaryDark,
    primaryContainer = AtlanticBluePrimaryContainerDark,
    onPrimaryContainer = AtlanticBlueOnPrimaryContainerDark,

    secondary = CanaryOchreSecondaryDark,
    onSecondary = CanaryOchreOnSecondaryDark,
    secondaryContainer = CanaryOchreSecondaryContainerDark,
    onSecondaryContainer = CanaryOchreOnSecondaryContainerDark,

    tertiary = CanaryGoldTertiaryDark,
    onTertiary = CanaryGoldOnTertiaryDark,
    tertiaryContainer = CanaryGoldTertiaryContainerDark,
    onTertiaryContainer = CanaryGoldOnTertiaryContainerDark,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,

    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark
)

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val primary by animateColorAsState(target.primary, ThemeTransitionSpec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, ThemeTransitionSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, ThemeTransitionSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, ThemeTransitionSpec, label = "onPrimaryContainer")
    val inversePrimary by animateColorAsState(target.inversePrimary, ThemeTransitionSpec, label = "inversePrimary")
    val secondary by animateColorAsState(target.secondary, ThemeTransitionSpec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, ThemeTransitionSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, ThemeTransitionSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, ThemeTransitionSpec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, ThemeTransitionSpec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, ThemeTransitionSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, ThemeTransitionSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, ThemeTransitionSpec, label = "onTertiaryContainer")
    val background by animateColorAsState(target.background, ThemeTransitionSpec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, ThemeTransitionSpec, label = "onBackground")
    val surface by animateColorAsState(target.surface, ThemeTransitionSpec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, ThemeTransitionSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, ThemeTransitionSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, ThemeTransitionSpec, label = "onSurfaceVariant")
    val surfaceTint by animateColorAsState(target.surfaceTint, ThemeTransitionSpec, label = "surfaceTint")
    val inverseSurface by animateColorAsState(target.inverseSurface, ThemeTransitionSpec, label = "inverseSurface")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, ThemeTransitionSpec, label = "inverseOnSurface")
    val error by animateColorAsState(target.error, ThemeTransitionSpec, label = "error")
    val onError by animateColorAsState(target.onError, ThemeTransitionSpec, label = "onError")
    val errorContainer by animateColorAsState(target.errorContainer, ThemeTransitionSpec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, ThemeTransitionSpec, label = "onErrorContainer")
    val outline by animateColorAsState(target.outline, ThemeTransitionSpec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, ThemeTransitionSpec, label = "outlineVariant")
    val scrim by animateColorAsState(target.scrim, ThemeTransitionSpec, label = "scrim")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim
    )
}

@Composable
fun ElPiedreroTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val animatedColorScheme = animateColorScheme(targetColorScheme)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = ElPiedreroTypography,
            shapes = ElPiedreroShapes,
            content = content
        )
    }
}

/**
 * Colores estándar para botones de acción primaria dorados de alto contraste (Empezar, Guardar, Continuar).
 * Extraído del logo oficial (#F9C801) con texto en Azul Atlántico Profundo (contraste > 10:1 AAA).
 */
@Composable
fun goldActionButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary
)

/**
 * Determina de forma precisa y reactiva si la app se está renderizando bajo tema oscuro,
 * respetando la selección manual de ThemeMode en ajustes o el tema del sistema.
 */
@Composable
fun isAppInDarkTheme(): Boolean = LocalIsDarkTheme.current

/**
 * Colores estándar de TopAppBar con la identidad Atlántica Canaria:
 * - Modo Claro: Azul Atlántico profundo (#1B3B6F) con iconos y texto en blanco (#FFFFFF).
 * - Modo Oscuro: Superficie azul noche profundo (#0F1B2E) con iconos y texto en onSurface (#E2E2E9),
 *   animados de forma sincronizada y fluida durante el cambio de tema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun elPiedreroTopAppBarColors(): TopAppBarColors {
    val isDark = isAppInDarkTheme()
    val targetContainer = if (isDark) AtlanticMidnightDark else AtlanticBluePrimaryLight
    val targetContent = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White

    val containerColor by animateColorAsState(targetContainer, ThemeTransitionSpec, label = "topBarContainer")
    val contentColor by animateColorAsState(targetContent, ThemeTransitionSpec, label = "topBarContent")

    return TopAppBarDefaults.topAppBarColors(
        containerColor = containerColor,
        titleContentColor = contentColor,
        navigationIconContentColor = contentColor,
        actionIconContentColor = contentColor
    )
}

/**
 * Color de contenedor para insignias y medallas de selección de modo:
 * Siempre mantiene un fondo Azul Atlántico profundo para garantizar contraste > 10:1 (AAA)
 * con los iconos y bordes de Oro Canario (#F9C801), animado de forma sincronizada.
 */
@Composable
fun canarianBadgeContainerColor(): Color {
    val target = if (isAppInDarkTheme()) CanarianBadgeNavyDark else CanarianBadgeNavyLight
    val animatedColor by animateColorAsState(target, ThemeTransitionSpec, label = "badgeContainer")
    return animatedColor
}

/**
 * Color para títulos y cabeceras de pantalla destacados:
 * Azul Atlántico en modo claro, y blanco nítido (onBackground) en modo oscuro,
 * animado suavemente al alternar el modo de tema.
 */
@Composable
fun headlineContentColor(): Color {
    val isDark = isAppInDarkTheme()
    val target = if (isDark) MaterialTheme.colorScheme.onBackground else AtlanticBluePrimaryLight
    val animatedColor by animateColorAsState(target, ThemeTransitionSpec, label = "headlineContent")
    return animatedColor
}
