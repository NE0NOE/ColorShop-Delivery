package com.rapiflete.delivery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta oficial de ColorShop Delivery diseñada en Stitch
val ColorShopPrimary = Color(0xFFA41C89)
val ColorShopMagenta = Color(0xFFC73FA7)
val ColorShopRose = Color(0xFFD92C85)
val ColorShopDeepPlum = Color(0xFF422344)
val ColorShopSlate = Color(0xFF1E293B)
val ColorShopMutedPlum = Color(0xFF725073)

val ColorShopSurface = Color(0xFFFFF3FA)
val ColorShopBackground = Color(0xFFFFF8FB)
val ColorShopSurfaceLow = Color(0xFFFFEBFB)
val ColorShopSurfaceHigh = Color(0xFFFFD6FC)
val ColorShopPinkPill = Color(0xFFFFC1D5)
val ColorShopSoftButtonBg = Color(0xFFFFEBF3)
val ColorShopSoftButtonText = Color(0xFF8C1D74)
val ColorShopOutlineVariant = Color(0xFFC9A0C8)
val ColorShopTertiaryBlue = Color(0xFF2956B7)
val ColorShopTertiaryFixed = Color(0xFFD8E3FB)

// Alias para compatibilidad con componentes previos
val TealPrimary = ColorShopPrimary
val IceBlueFreeze = ColorShopMagenta
val IceBlueContainer = ColorShopSurfaceLow
val OnIceBlueContainer = ColorShopDeepPlum

private val ColorShopLightScheme = lightColorScheme(
    primary = ColorShopPrimary,
    onPrimary = Color.White,
    primaryContainer = ColorShopSurfaceHigh,
    onPrimaryContainer = ColorShopDeepPlum,
    secondary = ColorShopMagenta,
    onSecondary = Color.White,
    secondaryContainer = ColorShopSurfaceLow,
    onSecondaryContainer = ColorShopDeepPlum,
    tertiary = ColorShopTertiaryBlue,
    tertiaryContainer = ColorShopTertiaryFixed,
    background = ColorShopBackground,
    surface = Color.White,
    surfaceVariant = ColorShopSurfaceLow,
    onSurface = ColorShopDeepPlum,
    onSurfaceVariant = ColorShopMutedPlum,
    outlineVariant = ColorShopOutlineVariant
)

@Composable
fun RapiFleteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorShopLightScheme,
        content = content
    )
}
