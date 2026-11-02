package com.galaxy.m21.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val TitaniumDark = Color(0xFF07090E)
val AlabasterLight = Color(0xFFF2F5FA)
val GlassDarkSurface = Color(0xFF11151E).copy(alpha = 0.56f)
val GlassLightSurface = Color(0xFFFFFFFF).copy(alpha = 0.72f)
val GlassBorderSubtle = Color.White.copy(alpha = 0.20f)

/**
 * Global Light Mode / Dark Mode Appearance.
 */
enum class AppAppearanceMode(val title: String) {
    DARK("Dark"),
    LIGHT("Light")
}

/**
 * 3-Way Orientation Mode Selector.
 */
enum class OrientationMode(val title: String) {
    DESK_ONLY("Horizontal Only (Desk Mode)"),
    MOBILE_ONLY("Vertical Only (Mobile Mode)"),
    MIX_AUTO("Mix Mode (Auto-Switch on Rotate)")
}

/**
 * Welcome Gate Visual Styles.
 */
enum class WelcomeStyle(val title: String) {
    IOS_17_CURSIVE("iOS 17/18 Cursive Vector Draw"),
    IOS_26_LIQUID_GLASS("iOS 26/27 Luminous Liquid Glass")
}

/**
 * App Opening Physical Launch Animation Styles.
 */
enum class AppOpeningAnimStyle(val title: String) {
    IOS_PERSPECTIVE_BALLOON("iOS Perspective Balloon"),
    HYPEROS_BALLOON("HyperOS Clean Balloon"),
    ONEUI_PORTAL("OneUI Circular Portal")
}

/**
 * Multi-OS Volume HUD Styles.
 */
enum class VolumeHudStyle(val title: String) {
    IOS_SIDE_PILL("iOS Side Pill"),
    COLOROS_ARC("ColorOS Arc"),
    HYPEROS_CAPSULE("HyperOS Capsule")
}

/**
 * Swipe-Right Gemini Edge Reveal Choreography Styles.
 */
enum class GeminiEdgeStyle(val title: String) {
    BEZEL_PEEL("Bezel Peel"),
    LIQUID_BUBBLE("Liquid Bubble"),
    PORTAL_EXPAND("Iris Portal"),
    CROSSFADE("Spatial Fade")
}

/**
 * Home Screen Keyboard Presentation Mode.
 */
enum class HomeKeyboardMode(val title: String) {
    FLOATING("Floating Deck"),
    FULL_POPUP("Full Screen Keyboard")
}

/**
 * App Drawer App Grid Horizontal Placement.
 */
enum class DrawerAppAlignment(val title: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

/**
 * App Drawer Keyboard Mode.
 */
enum class DrawerKeyboardMode(val title: String) {
    FLOATING_FREEFORM("Freeform"),
    FULL_LANDSCAPE_GBOARD("Full Screen Keyboard")
}

/**
 * Minimalist MultiOS Accent Palettes.
 */
enum class ColorPalettePreset(
    val title: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surfaceGlow: Color
) {
    APPLE_LIQUID_GLASS("Pure Frost", Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF334155)),
    SPATIAL_ICE("Glacier Ice", Color(0xFF7DD3FC), Color(0xFFBAE6FD), Color(0xFF94A3B8), Color(0xFF1E3A5F)),
    TITANIUM_GRAPHITE("Titanium", Color(0xFFE5E5EA), Color(0xFFAEAEB2), Color(0xFF8E8E93), Color(0xFF2C2C2E)),
    WARM_STARLIGHT("Starlight", Color(0xFFFDE68A), Color(0xFFE6DDD0), Color(0xFFA8A29E), Color(0xFF44403C)),
    ROSE_QUARTZ("Rose Quartz", Color(0xFFFDA4AF), Color(0xFFFECDD3), Color(0xFFFDA4AF), Color(0xFF4C1D24)),
    SAGE_MIST("Sage Mist", Color(0xFF86EFAC), Color(0xFFBBF7D0), Color(0xFF86EFAC), Color(0xFF143824))
}

/**
 * Liquid Glass Tint Presets (Plus Custom HSV Color Selector support).
 */
enum class GlassTintPreset(val title: String, val color: Color, val strength: Float) {
    PURE_CLEAR("Clear", Color(0xFFFFFFFF), 0.08f),
    ICE_BLUE("Ice", Color(0xFF93C5FD), 0.22f),
    ROSE_BLUSH("Rose", Color(0xFFFDA4AF), 0.22f),
    SAGE_MINT("Sage", Color(0xFF86EFAC), 0.20f),
    WARM_AMBER("Amber", Color(0xFFFCD34D), 0.20f),
    VIOLET_HAZE("Violet", Color(0xFFC4B5FD), 0.22f),
    OBSIDIAN_SMOKE("Obsidian", Color(0xFF1E293B), 0.36f),
    CUSTOM_HUE("Custom HSV", Color(0xFFFFFFFF), 0.26f)
}

/**
 * Clock Face Styles: Analog (No Numbers), Apple-Styled, Xiaomi-Styled, and Abstract Orbit.
 */
enum class ClockStylePreset(val title: String) {
    SWISS_ANALOG("Swiss Analog"),
    APPLE_ANALOG("Apple Analog"),
    APPLE_IOS_BOLD("iOS StandBy"),
    XIAOMI_MAGAZINE("Xiaomi Magazine"),
    XIAOMI_MINIMAL("Xiaomi Thin"),
    ABSTRACT_ORBIT("Orbit Ring")
}

/**
 * Minimalist Spatial Wallpaper Backdrops (Overridden seamlessly when Gallery image is applied).
 */
enum class WallpaperStyle(val title: String) {
    LIQUID_CHROME("Liquid Chrome 60fps"),
    DEEP_CAUSTICS("Deep Caustics 60fps"),
    COSMIC_NEBULA("Cosmic Nebula 60fps"),
    LIQUID_AURORA("Spatial Silk"),
    AMOLED_VOID("Pure Minimal"),
    SOFT_HALO("Ambient Halo"),
    MONOCHROME_MIST("Studio Mist")
}

/**
 * Customizable Swipe Target Actions.
 */
enum class SwipeActionTarget(val title: String) {
    GEMINI_WEB("Gemini Web"),
    APP_DRAWER("App Drawer"),
    VINYL_STUDIO("Vinyl Studio"),
    CONTROL_CENTER("Control Center"),
    RECENTS_DECK("Recents Deck"),
    NONE("Disabled")
}

/**
 * Rich MagSafe / Cyber Charging Animation Presets.
 */
enum class ChargingAnimStyle(val title: String) {
    MAGSAFE_LIQUID_RING("MagSafe Liquid Ring"),
    PLASMA_SURGE_WAVE("Plasma Wave Surge"),
    CYBER_MINIMAL("Cyber Gauge"),
    OFF("Disabled")
}

/**
 * 8 Distinct Minimalist Rotary Knob Designs.
 */
enum class KnobStylePreset(val title: String) {
    LIQUID_GLASS("iOS Liquid Glass"),
    FROSTED_MATTE("Frosted Matte"),
    MINIMAL_HALO("Minimal Thin Ring"),
    DIETER_RAMS("Braun Pointer"),
    KNURLED_TITANIUM("Knurled Metal"),
    RECESSED_WELL("Recessed Well"),
    CONCENTRIC_ORBIT("Dual Orbit"),
    MONOLITH_DOT("Monolith Dot")
}

/**
 * Custom Slider Visual Styles.
 */
enum class SliderStylePreset(val title: String) {
    LIQUID_CAPSULE("iOS Capsule"),
    MINIMAL_HAIRLINE("Minimal Line"),
    STUDIO_TICKS("Studio Ticks"),
    TACTILE_FADER("Tactile Fader")
}

/**
 * Minimalist Monochrome Vinyl / Optical Disc Finish.
 */
enum class DiscFinishStyle(val title: String) {
    DARK_VINYL("12\" Obsidian Vinyl"),
    SILVER_OPTICAL("Brushed Optical CD")
}

@Immutable
data class MultiOsThemeColors(
    val isLight: Boolean = false,
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0x99FFFFFF),
    val iconTint: Color = Color(0xEBFFFFFF),
    val accent: Color = Color(0xFFE2E8F0),
    val onAccent: Color = Color(0xFF0A0D14),
    val backgroundBase: Color = TitaniumDark,
    val overlayScrimGlass: Color = Color(0x3D05080F),
    val overlayScrimMatte: Color = Color(0xF50A0D14),
    val subtleFill: Color = Color(0x14FFFFFF),
    val subtleBorder: Color = Color(0x33FFFFFF),
    val recessedScreenBg: Color = Color(0xD906090F)
)

val LocalMultiOsColors = compositionLocalOf { MultiOsThemeColors() }

@Composable
fun MultiOsTheme(
    appearanceMode: AppAppearanceMode = AppAppearanceMode.DARK,
    accentColor: Color? = null,
    palette: ColorPalettePreset = ColorPalettePreset.APPLE_LIQUID_GLASS,
    content: @Composable () -> Unit
) {
    val isLight = appearanceMode == AppAppearanceMode.LIGHT
    val resolvedAccent = accentColor ?: palette.primary
    val onAccent = if (resolvedAccent.luminance() > 0.50f) Color(0xFF0A0D14) else Color.White

    val multiOsColors = if (isLight) {
        MultiOsThemeColors(
            isLight = true,
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            iconTint = Color(0xFF1E293B),
            accent = resolvedAccent,
            onAccent = onAccent,
            backgroundBase = AlabasterLight,
            overlayScrimGlass = Color(0xFFE8EEF6).copy(alpha = 0.42f),
            overlayScrimMatte = Color(0xFFF1F5F9).copy(alpha = 0.96f),
            subtleFill = Color(0xFF0F172A).copy(alpha = 0.06f),
            subtleBorder = Color(0xFF0F172A).copy(alpha = 0.15f),
            recessedScreenBg = Color(0xFFE2E8F0).copy(alpha = 0.88f)
        )
    } else {
        MultiOsThemeColors(
            isLight = false,
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color.White.copy(alpha = 0.58f),
            iconTint = Color.White.copy(alpha = 0.90f),
            accent = resolvedAccent,
            onAccent = onAccent,
            backgroundBase = TitaniumDark,
            overlayScrimGlass = Color(0xFF05080F).copy(alpha = 0.26f),
            overlayScrimMatte = Color(0xFF0A0D14).copy(alpha = 0.96f),
            subtleFill = Color.White.copy(alpha = 0.08f),
            subtleBorder = Color.White.copy(alpha = 0.20f),
            recessedScreenBg = Color(0xFF06090F).copy(alpha = 0.78f)
        )
    }

    val scheme = if (isLight) {
        lightColorScheme(
            primary = resolvedAccent,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = AlabasterLight,
            surface = GlassLightSurface,
            onPrimary = onAccent,
            onSecondary = Color(0xFF0F172A),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A)
        )
    } else {
        darkColorScheme(
            primary = resolvedAccent,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = TitaniumDark,
            surface = GlassDarkSurface,
            onPrimary = onAccent,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    }

    CompositionLocalProvider(LocalMultiOsColors provides multiOsColors) {
        MaterialTheme(
            colorScheme = scheme,
            content = content
        )
    }
}
