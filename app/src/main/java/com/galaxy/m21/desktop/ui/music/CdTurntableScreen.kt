package com.galaxy.m21.desktop.ui.music

import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.m21.desktop.hardware.CdAlbum
import com.galaxy.m21.desktop.hardware.HardwareViewModel
import com.galaxy.m21.desktop.ui.components.CustomLiquidSlider
import com.galaxy.m21.desktop.ui.components.MultiOsIcons
import com.galaxy.m21.desktop.ui.components.MultiOsPullTabCapsule
import com.galaxy.m21.desktop.ui.components.SliderGlyphType
import com.galaxy.m21.desktop.ui.shaders.LiquidGlassBox
import com.galaxy.m21.desktop.ui.shaders.LocalLiquidGlassConfig
import com.galaxy.m21.desktop.ui.theme.DiscFinishStyle
import com.galaxy.m21.desktop.ui.theme.LocalMultiOsColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * MultiOS Landscape Vinyl & Optical CD Studio:
 *
 * - **ZERO prebuilt music**: Starts completely clean until the user adds audio files via the **`+` Button**
 *   (`OpenMultipleDocuments` audio picker), automatically extracting embedded circular album covers!
 * - **Accurate Interactive Mechanical Tonearm**:
 *   - Rests parked off-record (`0°`) when paused or empty.
 *   - Swings onto the vinyl grooves (`19.5°` outer groove -> `36.5°` inner groove) tracking live `cdTrackProgress`!
 *   - Drag the tonearm onto any groove to seek & start playback, or drag it back off the record to pause!
 * - **Authentic Studio Disc Lighting (NOT slapped-on rotating triangles)**:
 *   - Circular album cover and micro-groove variations spin physically underneath a stationary overhead
 *     anisotropic studio reflection field + dynamic groove micro-shimmer!
 * - **Independent Vinyl Liquid Glass Toggle**: Choose Liquid Glass Vinyl OR Matte Solid Vinyl!
 * - **Identical Symmetrical Pull Tab Capsule** (`MultiOsPullTabCapsule`) matching the Status Bar!
 */
@Composable
fun CdTurntableScreen(
    hardwareViewModel: HardwareViewModel,
    onBackToCockpit: () -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    val telemetry by hardwareViewModel.telemetryState.collectAsState()
    val cdCatalog by hardwareViewModel.cdCatalogState.collectAsState()
    val selectedCd = cdCatalog.getOrNull(telemetry.selectedCdIndex) ?: cdCatalog.firstOrNull()
    val accent = telemetry.resolveAccentColor()
    val vinylGlassOn = telemetry.vinylLiquidGlassEnabled

    // `+` Button Launcher: Pick one or multiple audio files from Android Files!
    val addAudioFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            hardwareViewModel.addSongsFromFiles(uris)
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            hardwareViewModel.applyCustomAlbumCoverForSelectedDisc(uri)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "VinylMotorSpin")
    val motorAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MotorAngle"
    )

    var manualScratchAngle by remember { mutableFloatStateOf(0f) }
    var lastTouchAngle by remember { mutableFloatStateOf(0f) }

    val effectiveDiscRotation = if (telemetry.isPlayingAudio && !telemetry.isScratching && selectedCd != null) {
        (motorAngle + manualScratchAngle) % 360f
    } else {
        manualScratchAngle % 360f
    }

    val scrim = if (telemetry.liquidGlassEnabled && vinylGlassOn) {
        themeColors.overlayScrimGlass
    } else {
        themeColors.overlayScrimMatte
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .padding(horizontal = 22.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // =========================================================================
        // IDENTICAL SYMMETRICAL TOP PULL TAB CAPSULE (Swipe Down or Tap to Close!)
        // =========================================================================
        MultiOsPullTabCapsule(
            pullUpward = false,
            onTrigger = onBackToCockpit
        )

        Spacer(modifier = Modifier.height(6.dp))

        // =========================================================================
        // STRICT LANDSCAPE 3-COLUMN VINYL STUDIO
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PANEL: User Record Crate + `+` Add Songs from Files Button
            LiquidGlassBox(
                modifier = Modifier
                    .width(268.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                borderColor = themeColors.subtleBorder,
                overrideGlassEnabled = vinylGlassOn
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MultiOsIcons.VinylDisc(Modifier.size(18.dp), themeColors.iconTint)
                            Text(
                                text = "Library",
                                color = themeColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (selectedCd != null) {
                                GlassCircleIconButton(
                                    onClick = { coverPickerLauncher.launch("image/*") }
                                ) {
                                    MultiOsIcons.GalleryImage(Modifier.size(14.dp), themeColors.iconTint)
                                }
                            }

                            // Prominent `+` Button from Files!
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        addAudioFilesLauncher.launch(arrayOf("audio/*"))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MultiOsIcons.Plus(Modifier.size(15.dp), themeColors.onAccent)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cdCatalog.isEmpty()) {
                        // Clean Empty Crate Prompt — Zero Prebuilt Music!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(themeColors.subtleFill)
                                .border(0.75.dp, themeColors.subtleBorder, RoundedCornerShape(18.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    addAudioFilesLauncher.launch(arrayOf("audio/*"))
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MultiOsIcons.Plus(Modifier.size(20.dp), themeColors.onAccent)
                                }
                                Text(
                                    text = "Add Songs from Files",
                                    color = themeColors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Album covers load automatically",
                                    color = themeColors.textSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(
                                items = cdCatalog,
                                key = { _, cd -> cd.id }
                            ) { index, cd ->
                                MinimalRecordRowItem(
                                    cd = cd,
                                    isSelected = index == telemetry.selectedCdIndex,
                                    onSelect = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        hardwareViewModel.selectCdFromRack(index)
                                    },
                                    onDelete = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        hardwareViewModel.removeSongAt(index)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // CENTER STAGE: Turntable Platter + Interactive Precision Tonearm + Transport
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TurntableDeckWithInteractiveTonearm(
                    album = selectedCd,
                    finishStyle = telemetry.discFinishStyle,
                    vinylLiquidGlass = vinylGlassOn,
                    isPlaying = telemetry.isPlayingAudio && selectedCd != null,
                    trackProgress = telemetry.cdTrackProgress,
                    rotationDegrees = effectiveDiscRotation,
                    accentColor = accent,
                    deckWidth = 340.dp,
                    platterSize = 248.dp,
                    onScratchStart = { offset, center ->
                        lastTouchAngle = computeDiscAngle(offset, center)
                        hardwareViewModel.setScratchState(true)
                    },
                    onScratchEnd = { hardwareViewModel.setScratchState(false) },
                    onScratchDrag = { pos, center ->
                        val curAngle = computeDiscAngle(pos, center)
                        var deltaDeg = curAngle - lastTouchAngle
                        if (deltaDeg > 180f) deltaDeg -= 360f
                        if (deltaDeg < -180f) deltaDeg += 360f
                        manualScratchAngle += deltaDeg
                        lastTouchAngle = curAngle
                        hardwareViewModel.scratchCdByAngle(deltaDeg)
                        if (abs(deltaDeg) > 4f) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onTonearmSeekAndPlay = { grooveProgress ->
                        hardwareViewModel.seekCdToProgress(grooveProgress, autoPlay = true)
                    },
                    onTonearmParkOffRecord = {
                        hardwareViewModel.setCdPlayingDirect(false)
                    },
                    onTonearmTapToggle = {
                        if (selectedCd == null) {
                            addAudioFilesLauncher.launch(arrayOf("audio/*"))
                        } else {
                            hardwareViewModel.togglePlayPauseCd()
                        }
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedCd?.title ?: "No Disc Loaded",
                        color = themeColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedCd?.artist ?: "Tap + to import audio files",
                        color = themeColors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1
                    )
                }

                VectorTransportRow(
                    isPlaying = telemetry.isPlayingAudio && selectedCd != null,
                    accentColor = accent,
                    onAccentColor = themeColors.onAccent,
                    onPrev = { hardwareViewModel.skipTrack(-1) },
                    onPlayPause = {
                        if (selectedCd == null) {
                            addAudioFilesLauncher.launch(arrayOf("audio/*"))
                        } else {
                            hardwareViewModel.togglePlayPauseCd()
                        }
                    },
                    onNext = { hardwareViewModel.skipTrack(1) }
                )
            }

            // RIGHT PANEL: Vinyl Liquid Glass Toggle, Disc Finish, FX Pads & Music Volume / Bass Sliders
            LiquidGlassBox(
                modifier = Modifier
                    .width(278.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                borderColor = themeColors.subtleBorder,
                overrideGlassEnabled = vinylGlassOn
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Vinyl Liquid Glass ON / OFF Pill!
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (vinylGlassOn) accent else themeColors.subtleFill)
                                    .border(0.75.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        hardwareViewModel.toggleVinylLiquidGlass()
                                    }
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (vinylGlassOn) "Liquid Glass" else "Matte Vinyl",
                                    color = if (vinylGlassOn) themeColors.onAccent else themeColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            // Obsidian Vinyl vs Silver Optical Finish
                            GlassCircleIconButton(
                                onClick = { hardwareViewModel.toggleDiscFinishStyle() }
                            ) {
                                MultiOsIcons.VinylDisc(
                                    Modifier.size(15.dp),
                                    if (telemetry.discFinishStyle == DiscFinishStyle.SILVER_OPTICAL) accent else themeColors.iconTint
                                )
                            }
                        }

                        GlassCircleIconButton(onClick = onBackToCockpit) {
                            MultiOsIcons.Close(Modifier.size(13.dp), themeColors.iconTint)
                        }
                    }

                    // Minimalist Studio FX Pads
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinimalStudioPad(
                                label = "Sub",
                                isActive = telemetry.activeRemixFx == 1,
                                accentColor = accent,
                                onAccentColor = themeColors.onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { hardwareViewModel.triggerDjRemixFx(1) }
                            )
                            MinimalStudioPad(
                                label = "Gate",
                                isActive = telemetry.activeRemixFx == 2,
                                accentColor = accent,
                                onAccentColor = themeColors.onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { hardwareViewModel.triggerDjRemixFx(2) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinimalStudioPad(
                                label = "Brake",
                                isActive = telemetry.activeRemixFx == 3,
                                accentColor = accent,
                                onAccentColor = themeColors.onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { hardwareViewModel.triggerDjRemixFx(3) }
                            )
                            MinimalStudioPad(
                                label = "Sweep",
                                isActive = telemetry.activeRemixFx == 4,
                                accentColor = accent,
                                onAccentColor = themeColors.onAccent,
                                modifier = Modifier.weight(1f),
                                onClick = { hardwareViewModel.triggerDjRemixFx(4) }
                            )
                        }
                    }

                    // Music Volume (`MusicNote` + `Speaker` Icon) & Bass Filter Sliders
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomLiquidSlider(
                            progress = telemetry.musicVolumeProgress,
                            accentColor = accent,
                            style = telemetry.sliderStyle,
                            showLabels = false,
                            glyphType = SliderGlyphType.MUSIC_SPEAKER,
                            barHeight = 36.dp,
                            onProgressChange = { hardwareViewModel.setMusicVolumeDirect(it) }
                        )

                        CustomLiquidSlider(
                            progress = telemetry.djFilterCutoff,
                            accentColor = accent,
                            style = telemetry.sliderStyle,
                            showLabels = false,
                            glyphType = SliderGlyphType.BASS_WAVE,
                            barHeight = 36.dp,
                            onProgressChange = { hardwareViewModel.setDjFilterCutoff(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Turntable Platter + Interactive Accurate Mechanical Tonearm + Realistic Non-Slapped-On Lighting!
 *
 * Lighting Architecture:
 * - **Rotating Layer**: Contains the vinyl/optical substrate, subtle physical groove micro-variations,
 *   and the **Circular Album Cover Picture** in the center with clamping ring + spindle hole.
 * - **Stationary Studio Optical Layer (World-Space Fixed!)**: Overhead studio light sheen stays anchored
 *   in physical space while groove micro-glints shimmer dynamically with `rotationDegrees` — so the
 *   lighting never looks like a static PNG glued to the spinning record!
 * - **Liquid Glass Vinyl Option**: When `vinylLiquidGlass == true`, renders a refractive frosted smoked-glass
 *   vinyl rim and specular lens sheen!
 */
@Composable
private fun TurntableDeckWithInteractiveTonearm(
    album: CdAlbum?,
    finishStyle: DiscFinishStyle,
    vinylLiquidGlass: Boolean,
    isPlaying: Boolean,
    trackProgress: Float,
    rotationDegrees: Float,
    accentColor: Color,
    deckWidth: Dp,
    platterSize: Dp,
    onScratchStart: (Offset, Offset) -> Unit,
    onScratchEnd: () -> Unit,
    onScratchDrag: (Offset, Offset) -> Unit,
    onTonearmSeekAndPlay: (Float) -> Unit,
    onTonearmParkOffRecord: () -> Unit,
    onTonearmTapToggle: () -> Unit
) {
    val view = LocalView.current
    val glassConfig = LocalLiquidGlassConfig.current
    val isSilverCd = finishStyle == DiscFinishStyle.SILVER_OPTICAL

    val latestSeekPlay by rememberUpdatedState(onTonearmSeekAndPlay)
    val latestPark by rememberUpdatedState(onTonearmParkOffRecord)
    val latestTapToggle by rememberUpdatedState(onTonearmTapToggle)

    // Tonearm angle: 0° = parked on rest; 19.5° = outer lead-in groove; 36.5° = inner run-out groove
    var isDraggingArm by remember { mutableStateOf(false) }
    var draggedArmAngle by remember { mutableFloatStateOf(0f) }

    val targetArmAngle = when {
        isDraggingArm -> draggedArmAngle
        isPlaying && album != null -> 19.5f + trackProgress.coerceIn(0f, 1f) * 17.0f
        else -> 0f
    }

    val animatedArmAngle by animateFloatAsState(
        targetValue = targetArmAngle,
        animationSpec = if (isDraggingArm) {
            spring(dampingRatio = 0.95f, stiffness = 900f)
        } else {
            spring(dampingRatio = 0.78f, stiffness = 240f)
        },
        label = "TonearmSwingAngle"
    )

    Box(
        modifier = Modifier
            .width(deckWidth)
            .height(platterSize + 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // =====================================================================
        // 1. PLATTER + ROTATING CIRCULAR ALBUM COVER + WORLD-SPACE STUDIO LIGHTING
        // =====================================================================
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(platterSize)
                .shadow(
                    elevation = if (vinylLiquidGlass) 20.dp else 26.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.72f)
                )
                .clip(CircleShape)
                .background(
                    when {
                        vinylLiquidGlass && isSilverCd -> lerp(Color(0xFFDCE3EC), glassConfig.tintColor, 0.22f).copy(alpha = 0.72f)
                        vinylLiquidGlass -> lerp(Color(0xFF101520), glassConfig.tintColor, 0.18f).copy(alpha = 0.68f)
                        isSilverCd -> Color(0xFFD0D5DD)
                        else -> Color(0xFF0A0C10)
                    }
                )
                .border(
                    width = if (vinylLiquidGlass) 1.4.dp else 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (vinylLiquidGlass) 0.62f else 0.26f),
                            Color.White.copy(alpha = if (vinylLiquidGlass) 0.14f else 0.06f),
                            Color.White.copy(alpha = if (vinylLiquidGlass) 0.42f else 0.16f)
                        )
                    ),
                    shape = CircleShape
                )
                .pointerInput(album?.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onScratchStart(offset, Offset(size.width / 2f, size.height / 2f))
                        },
                        onDragEnd = onScratchEnd,
                        onDragCancel = onScratchEnd,
                        onDrag = { change, _ ->
                            change.consume()
                            onScratchDrag(change.position, Offset(size.width / 2f, size.height / 2f))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // A) PHYSICALLY SPINNING LAYER: Micro-groove texture & Center Circular Album Cover!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationDegrees),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val grooveBaseAlpha = if (isSilverCd) 0.06f else if (vinylLiquidGlass) 0.075f else 0.048f
                    for (g in 1..22) {
                        val radiusFrac = 0.41f + g * 0.025f
                        drawCircle(
                            color = if (isSilverCd) Color.Black.copy(alpha = grooveBaseAlpha)
                            else Color.White.copy(alpha = if (g % 5 == 0) grooveBaseAlpha * 1.45f else grooveBaseAlpha),
                            radius = r * radiusFrac,
                            style = Stroke(width = 0.75.dp.toPx())
                        )
                    }
                }

                // Center Circular Album Cover Picture (Spins cleanly with the record!)
                val labelDiameter = platterSize * 0.40f
                Box(
                    modifier = Modifier
                        .size(labelDiameter)
                        .clip(CircleShape)
                        .background(Color(0xFF161A23))
                        .border(1.2.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (album?.coverArt != null) {
                        Image(
                            bitmap = album.coverArt,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MultiOsIcons.MusicNote(
                            modifier = Modifier.size(22.dp),
                            tint = Color.White.copy(alpha = 0.35f)
                        )
                    }

                    // Clamping Ring + Authentic Center Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(platterSize * 0.09f)
                            .clip(CircleShape)
                            .background(Color(0xFF090B0F).copy(alpha = 0.55f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.36f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(platterSize * 0.038f)
                                .clip(CircleShape)
                                .background(Color(0xFF050608))
                                .border(0.75.dp, Color.White.copy(alpha = 0.48f), CircleShape)
                        )
                    }
                }
            }

            // B) STATIONARY WORLD-SPACE STUDIO OPTICAL LIGHTING + GROOVE MICRO-SHIMMER
            // Stays fixed relative to overhead studio light while subtle groove glints breathe with motion!
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                val radPhase = (rotationDegrees * PI / 180.0)
                val microShimmer = (0.86f + 0.14f * sin(radPhase * 3.0).toFloat())

                // Soft anisotropic studio sheen across the groove band only (does NOT obscure center album cover!)
                for (band in 0..7) {
                    val bandFrac = 0.44f + band * 0.068f
                    val bandRadius = r * bandFrac
                    val strokeW = r * 0.055f
                    val topLeft = Offset(c.x - bandRadius, c.y - bandRadius)
                    val arcSize = Size(bandRadius * 2f, bandRadius * 2f)
                    val shimmerShift = sin(radPhase + band * 0.7).toFloat() * 4.5f

                    val highlightAlpha = (if (vinylLiquidGlass) 0.13f else if (isSilverCd) 0.18f else 0.085f) * microShimmer
                    // Upper-left overhead specular arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.00f to Color.Transparent,
                            0.54f to Color.Transparent,
                            0.62f to Color.White.copy(alpha = highlightAlpha),
                            0.70f to Color.Transparent,
                            1.00f to Color.Transparent,
                            center = c
                        ),
                        startAngle = 195f + shimmerShift,
                        sweepAngle = 62f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Lower-right counter-reflection arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.00f to Color.Transparent,
                            0.05f to Color.White.copy(alpha = highlightAlpha * 0.72f),
                            0.14f to Color.Transparent,
                            1.00f to Color.Transparent,
                            center = c
                        ),
                        startAngle = 15f - shimmerShift,
                        sweepAngle = 58f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Liquid Glass Refractive Lens Rim when Vinyl Liquid Glass is ON
                if (vinylLiquidGlass) {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.Transparent,
                                glassConfig.tintColor.copy(alpha = 0.14f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        radius = r * 0.985f,
                        center = c,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // =====================================================================
        // 2. PRECISION INTERACTIVE TURNTABLE TONEARM (DRAG OR TAP TO PLAY/SEEK/PARK!)
        // =====================================================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(album?.id) {
                    detectTapGestures { tapPos ->
                        // Respond to taps near the pivot/arm/rest zone on the right side
                        if (tapPos.x > size.width * 0.64f) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            latestTapToggle()
                        }
                    }
                }
                .pointerInput("tonearm_drag_${album?.id}") {
                    detectDragGestures(
                        onDragStart = { startPos ->
                            val pivot = Offset(size.width * 0.88f, size.height * 0.15f)
                            val distFromRightZone = startPos.x > size.width * 0.56f || hypot(startPos.x - pivot.x, startPos.y - pivot.y) < size.height * 0.85f
                            if (distFromRightZone && startPos.x > size.width * 0.54f) {
                                isDraggingArm = true
                                draggedArmAngle = touchToTonearmAngle(startPos, pivot)
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        },
                        onDragEnd = {
                            if (isDraggingArm) {
                                val finalAngle = draggedArmAngle
                                isDraggingArm = false
                                if (finalAngle >= 15.5f) {
                                    val prog = ((finalAngle - 19.5f) / 17.0f).coerceIn(0f, 0.99f)
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    latestSeekPlay(prog)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    latestPark()
                                }
                            }
                        },
                        onDragCancel = { isDraggingArm = false },
                        onDrag = { change, _ ->
                            if (isDraggingArm) {
                                change.consume()
                                val pivot = Offset(size.width * 0.88f, size.height * 0.15f)
                                val nextAngle = touchToTonearmAngle(change.position, pivot)
                                if (abs(nextAngle - draggedArmAngle) > 2.2f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                draggedArmAngle = nextAngle
                                if (nextAngle >= 18.0f) {
                                    val liveProg = ((nextAngle - 19.5f) / 17.0f).coerceIn(0f, 0.99f)
                                    latestSeekPlay(liveProg)
                                }
                            }
                        }
                    )
                }
        ) {
            val pivot = Offset(size.width * 0.88f, size.height * 0.15f)
            val armLength = size.height * 0.67f

            // Tonearm Rest Clip (Parked Cradle)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(pivot.x - 5.dp.toPx(), pivot.y + armLength * 0.52f),
                size = Size(10.dp.toPx(), 16.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Precision Gimbal Base Assembly
            drawCircle(
                color = Color(0xFF141821),
                radius = 22.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                radius = 22.dp.toPx(),
                center = pivot,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Rotating Tonearm Tube + Counterweight + Headshell + Glowing Stylus Tip
            rotate(degrees = animatedArmAngle, pivot = pivot) {
                // Counterweight cylinder above pivot
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF64748B), Color(0xFFE2E8F0), Color(0xFF475569))
                    ),
                    topLeft = Offset(pivot.x - 9.dp.toPx(), pivot.y - 30.dp.toPx()),
                    size = Size(18.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Soft drop shadow under tonearm
                drawLine(
                    color = Color.Black.copy(alpha = 0.48f),
                    start = Offset(pivot.x + 3.dp.toPx(), pivot.y + 4.dp.toPx()),
                    end = Offset(pivot.x + 3.dp.toPx(), pivot.y + armLength + 4.dp.toPx()),
                    strokeWidth = 5.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Brushed Titanium Tonearm Shaft
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF94A3B8), Color(0xFFF8FAFC), Color(0xFF64748B))
                    ),
                    start = Offset(pivot.x, pivot.y - 14.dp.toPx()),
                    end = Offset(pivot.x, pivot.y + armLength),
                    strokeWidth = 4.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Angled Headshell + Cartridge + Stylus Needle Tip
                val headshellStart = Offset(pivot.x, pivot.y + armLength)
                val headshellEnd = Offset(pivot.x - 14.dp.toPx(), pivot.y + armLength + 22.dp.toPx())
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = headshellStart,
                    end = headshellEnd,
                    strokeWidth = 6.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Finger-lift cueing lever on headshell
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(headshellStart.x + 1.dp.toPx(), headshellStart.y + 6.dp.toPx()),
                    end = Offset(headshellStart.x + 10.dp.toPx(), headshellStart.y + 3.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Glowing Stylus Indicator Dot (Lights up with Accent Color when tracking vinyl grooves!)
                drawCircle(
                    color = if (animatedArmAngle >= 16f) accentColor else Color.White.copy(alpha = 0.55f),
                    radius = 3.2.dp.toPx(),
                    center = headshellEnd
                )
            }

            // Center Gimbal Cap
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color(0xFF64748B)),
                    center = pivot,
                    radius = 10.dp.toPx()
                ),
                radius = 9.dp.toPx(),
                center = pivot
            )
        }
    }
}

/**
 * Converts a touch point near the tonearm into an accurate clockwise swing angle (`0°` parked .. `40°` inner label).
 */
private fun touchToTonearmAngle(touch: Offset, pivot: Offset): Float {
    val dx = pivot.x - touch.x
    val dy = (touch.y - pivot.y).coerceAtLeast(12f)
    val deg = (atan2(dx, dy) * (180.0 / PI)).toFloat()
    return deg.coerceIn(0f, 39.5f)
}

@Composable
private fun VectorTransportRow(
    isPlaying: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCircleIconButton(size = 42.dp, onClick = onPrev) {
            MultiOsIcons.SkipBack(Modifier.size(17.dp), themeColors.iconTint)
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(accentColor)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                MultiOsIcons.Pause(Modifier.size(21.dp), onAccentColor)
            } else {
                MultiOsIcons.Play(Modifier.size(21.dp), onAccentColor)
            }
        }
        GlassCircleIconButton(size = 42.dp, onClick = onNext) {
            MultiOsIcons.SkipForward(Modifier.size(17.dp), themeColors.iconTint)
        }
    }
}

@Composable
private fun GlassCircleIconButton(
    size: Dp = 32.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(themeColors.subtleFill)
            .border(0.75.dp, themeColors.subtleBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MinimalRecordRowItem(
    cd: CdAlbum,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColors = LocalMultiOsColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) themeColors.accent.copy(alpha = if (themeColors.isLight) 0.22f else 0.18f)
                else themeColors.subtleFill
            )
            .border(
                width = 0.75.dp,
                color = if (isSelected) themeColors.accent.copy(alpha = 0.65f) else themeColors.subtleBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Mini Vinyl Disc with Circular Album Cover
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D1016))
                .border(0.75.dp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (cd.coverArt != null) {
                Image(
                    bitmap = cd.coverArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cd.title,
                color = themeColors.textPrimary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cd.artist,
                color = themeColors.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            MultiOsIcons.Trash(Modifier.size(13.dp), themeColors.textSecondary)
        }
    }
}

@Composable
private fun MinimalStudioPad(
    label: String,
    isActive: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val themeColors = LocalMultiOsColors.current
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) accentColor else themeColors.subtleFill)
            .border(
                width = 0.75.dp,
                color = if (isActive) accentColor else themeColors.subtleBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) onAccentColor else themeColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}

private fun computeDiscAngle(touch: Offset, center: Offset): Float {
    val dx = touch.x - center.x
    val dy = touch.y - center.y
    val rad = atan2(dy, dx)
    var deg = (rad * (180f / PI)).toFloat()
    if (deg < 0f) deg += 360f
    return deg
}
