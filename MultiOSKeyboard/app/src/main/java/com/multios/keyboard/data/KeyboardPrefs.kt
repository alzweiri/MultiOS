package com.multios.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.multios.keyboard.engine.AutocorrectEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The 3 exact design modes requested by the user:
 * 1. LIQUID_GLASS: Specular iOS Liquid Glass keys + Liquid Glass base tray
 * 2. BLURRED_KEYS: Frosted diffused blurred keys over a clean minimal base
 * 3. ALL_PASTEL: Soft tactile pastel keys + pastel base adapted to chosen tint hue
 */
enum class KeyboardMaterialMode(val title: String, val subtitle: String) {
    LIQUID_GLASS("Liquid Glass", "Liquid glass keys & liquid glass base"),
    BLURRED_KEYS("Blurred Keys", "Frosted blurred keys over clean dark base"),
    ALL_PASTEL("All Pastel", "Soft pastel keycaps & harmonious pastel base")
}

/**
 * Supported keyboard languages: strictly English and Arabic.
 */
enum class KeyboardLanguage(val code: String, val displayName: String, val spacebarLabel: String) {
    ENGLISH("en_US", "English", "English"),
    ARABIC("ar", "العربية", "العربية")
}

/**
 * Curated tint presets + Custom Hue option.
 */
enum class KeyboardTintPreset(
    val label: String,
    val color: Color,
    val defaultHue: Float,
    val defaultStrength: Float
) {
    PURE_FROST("Frost", Color(0xFFFFFFFF), 210f, 0.08f),
    ICE_BLUE("Ice Blue", Color(0xFF7DD3FC), 199f, 0.34f),
    ROSE_BLUSH("Rose", Color(0xFFFDA4AF), 352f, 0.34f),
    MINT_SAGE("Mint", Color(0xFF6EE7B7), 156f, 0.32f),
    LAVENDER("Lavender", Color(0xFFC4B5FD), 255f, 0.34f),
    PEACH_GOLD("Peach", Color(0xFFFDBA74), 28f, 0.34f),
    CUSTOM("Custom", Color(0xFF93C5FD), 215f, 0.38f)
}

/** Zero-latency key sound profiles (synthesised PCM played through SoundPool). */
enum class KeySoundProfile(val title: String) {
    SILENT("Silent"),
    IOS_TOCK("iOS Crisp Tock"),
    THOCKY_LINEAR("Thocky Linear"),
    BRAUN_CLICK("Braun Tactile Click")
}

/** One orientation's key-area geometry. The outer frame always stays flush with the screen edges. */
@Immutable
data class KeyboardGeometry(
    val heightScale: Float = 1.0f,
    val bottomOffsetDp: Float = 18f,
    val leftInsetDp: Float = 0f,
    val rightInsetDp: Float = 0f
)

@Immutable
data class KeyboardSettingsState(
    val materialMode: KeyboardMaterialMode = KeyboardMaterialMode.LIQUID_GLASS,
    val tintPreset: KeyboardTintPreset = KeyboardTintPreset.PURE_FROST,
    val customHue: Float = 210f,
    val tintStrength: Float = 0.12f,
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val showNumberRow: Boolean = true,
    val heightScale: Float = 1.0f,
    val bottomOffsetDp: Float = 18f,
    val leftInsetDp: Float = 0f,
    val rightInsetDp: Float = 0f,
    val showKeyBorders: Boolean = false,
    val autocorrectEnabled: Boolean = true,
    val glideTypingEnabled: Boolean = true,
    val swipeBackspaceWords: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val hapticStrengthMs: Int = 0, // 0 = Native Gboard Crisp Hardware Tick, 1..25 = Custom ms
    val recentEmojis: List<String> = listOf("😂", "❤️", "🔥", "✨", "🙏", "😭", "👍", "😍"),
    val clipboardHistory: List<String> = emptyList(),
    val pinnedClips: Set<String> = emptySet(),
    val userDictionary: Set<String> = emptySet(),
    val lastClipTimestampMs: Long = 0L,
    val isLandscape: Boolean = false,
    val portraitGeometry: KeyboardGeometry = KeyboardGeometry(),
    val landscapeGeometry: KeyboardGeometry = KeyboardGeometry(heightScale = 0.86f, bottomOffsetDp = 6f),
    val soundProfile: KeySoundProfile = KeySoundProfile.SILENT,
    val soundVolume: Float = 0.6f,
    val autoCapitalize: Boolean = true,
    val learnWords: Boolean = true,
    val translatorEnabled: Boolean = true,
    val cursorLoupeEnabled: Boolean = true,
    val keyRippleEnabled: Boolean = true,
    val userWordFrequency: Map<String, Int> = emptyMap()
) {
    val resolvedTintColor: Color
        get() = if (tintPreset == KeyboardTintPreset.CUSTOM) {
            Color.hsv(customHue.coerceIn(0f, 360f), 0.55f, 0.98f)
        } else {
            tintPreset.color
        }

    val resolvedHue: Float
        get() = if (tintPreset == KeyboardTintPreset.CUSTOM) {
            customHue.coerceIn(0f, 360f)
        } else {
            tintPreset.defaultHue
        }
}

/**
 * Process-wide reactive preferences repository shared seamlessly between
 * `KeyboardSettingsActivity` and `MultiOsInputMethodService`.
 */
object KeyboardPrefsRepository {
    private const val PREFS_NAME = "multios_keyboard_prefs"
    private const val CLIP_SEP = "|"

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(KeyboardSettingsState())
    val state: StateFlow<KeyboardSettingsState> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, _ ->
        val next = readFromPrefs(sp)
        _state.value = next
        AutocorrectEngine.setUserWords(next.userDictionary)
        AutocorrectEngine.setUserFrequencies(next.userWordFrequency)
    }

    @Volatile
    private var landscapeActive = false

    fun init(context: Context) {
        AutocorrectEngine.ensureInitialized(context.applicationContext)
        if (prefs != null) return
        val storageContext = context.applicationContext.createDeviceProtectedStorageContext()
            ?: context.applicationContext
        val sp = storageContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        val loaded = readFromPrefs(sp)
        _state.value = loaded
        AutocorrectEngine.setUserWords(loaded.userDictionary)
        AutocorrectEngine.setUserFrequencies(loaded.userWordFrequency)
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Switches the active geometry profile. Landscape and portrait are persisted under completely
     * separate keys, so resizing one orientation never touches the other.
     */
    fun setOrientation(landscape: Boolean) {
        landscapeActive = landscape
        val s = _state.value
        if (s.isLandscape == landscape) return
        _state.value = applyActiveGeometry(s.copy(isLandscape = landscape))
    }

    private fun applyActiveGeometry(s: KeyboardSettingsState): KeyboardSettingsState {
        val g = if (s.isLandscape) s.landscapeGeometry else s.portraitGeometry
        return s.copy(
            heightScale = g.heightScale,
            bottomOffsetDp = g.bottomOffsetDp,
            leftInsetDp = g.leftInsetDp,
            rightInsetDp = g.rightInsetDp
        )
    }

    private fun readGeometry(sp: SharedPreferences, suffix: String, def: KeyboardGeometry): KeyboardGeometry {
        return KeyboardGeometry(
            heightScale = sp.getFloat("height_scale_$suffix", def.heightScale).coerceIn(0.68f, 1.45f),
            bottomOffsetDp = sp.getFloat("bottom_offset_dp_$suffix", def.bottomOffsetDp).coerceIn(0f, 160f),
            leftInsetDp = sp.getFloat("left_inset_dp_$suffix", def.leftInsetDp).coerceIn(0f, 140f),
            rightInsetDp = sp.getFloat("right_inset_dp_$suffix", def.rightInsetDp).coerceIn(0f, 140f)
        )
    }

    private fun encodeFrequency(map: Map<String, Int>): String =
        map.entries.joinToString(CLIP_SEP) { (w, c) ->
            Base64.encodeToString(w.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) + ":" + c
        }

    private fun decodeFrequency(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = HashMap<String, Int>()
        raw.split(CLIP_SEP).forEach { token ->
            val idx = token.lastIndexOf(':')
            if (idx <= 0) return@forEach
            val word = runCatching {
                String(Base64.decode(token.substring(0, idx), Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull() ?: return@forEach
            val count = token.substring(idx + 1).toIntOrNull() ?: return@forEach
            out[word] = count
        }
        return out
    }

    private fun encodeStringList(items: Collection<String>): String {
        return items.joinToString(CLIP_SEP) { item ->
            Base64.encodeToString(item.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(CLIP_SEP).mapNotNull { token ->
            if (token.isBlank()) null
            else runCatching {
                String(Base64.decode(token, Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
    }

    private fun readFromPrefs(sp: SharedPreferences): KeyboardSettingsState {
        val modeName = sp.getString("material_mode", KeyboardMaterialMode.LIQUID_GLASS.name)
        val mode = KeyboardMaterialMode.entries.find { it.name == modeName }
            ?: KeyboardMaterialMode.LIQUID_GLASS

        val presetName = sp.getString("tint_preset", KeyboardTintPreset.PURE_FROST.name)
        val preset = KeyboardTintPreset.entries.find { it.name == presetName }
            ?: KeyboardTintPreset.PURE_FROST

        val langName = sp.getString("language", KeyboardLanguage.ENGLISH.name)
        val lang = KeyboardLanguage.entries.find { it.name == langName }
            ?: KeyboardLanguage.ENGLISH

        val recentsRaw = sp.getString("recent_emojis", "😂,❤️,🔥,✨,🙏,😭,👍,😍") ?: ""
        val recents = recentsRaw.split(",").filter { it.isNotBlank() }.ifEmpty {
            listOf("😂", "❤️", "🔥", "✨", "🙏", "😭", "👍", "😍")
        }

        val clips = decodeStringList(sp.getString("clipboard_history_b64", null))
        val pinned = decodeStringList(sp.getString("pinned_clips_b64", null)).toSet()
        val userWords = decodeStringList(sp.getString("user_dict_b64", null)).toSet()

        // Migrate legacy single-profile geometry into the portrait profile defaults
        val legacyPortrait = KeyboardGeometry(
            heightScale = sp.getFloat("height_scale", 1.0f),
            bottomOffsetDp = sp.getFloat("bottom_offset_dp", 18f),
            leftInsetDp = sp.getFloat("left_inset_dp", 0f),
            rightInsetDp = sp.getFloat("right_inset_dp", 0f)
        )
        val portraitGeo = readGeometry(sp, "port", legacyPortrait)
        val landscapeGeo = readGeometry(sp, "land", KeyboardGeometry(heightScale = 0.86f, bottomOffsetDp = 6f))
        val soundName = sp.getString("sound_profile", KeySoundProfile.SILENT.name)
        val sound = KeySoundProfile.entries.find { it.name == soundName } ?: KeySoundProfile.SILENT

        val base = KeyboardSettingsState(
            isLandscape = landscapeActive,
            portraitGeometry = portraitGeo,
            landscapeGeometry = landscapeGeo,
            soundProfile = sound,
            soundVolume = sp.getFloat("sound_volume", 0.6f).coerceIn(0f, 1f),
            autoCapitalize = sp.getBoolean("auto_capitalize", true),
            learnWords = sp.getBoolean("learn_words", true),
            translatorEnabled = sp.getBoolean("translator_enabled", true),
            cursorLoupeEnabled = sp.getBoolean("cursor_loupe", true),
            keyRippleEnabled = sp.getBoolean("key_ripple", true),
            userWordFrequency = decodeFrequency(sp.getString("user_word_freq", null)),
            materialMode = mode,
            tintPreset = preset,
            customHue = sp.getFloat("custom_hue", preset.defaultHue),
            tintStrength = sp.getFloat("tint_strength", preset.defaultStrength).coerceIn(0f, 0.85f),
            language = lang,
            showNumberRow = sp.getBoolean("show_number_row", true),
            heightScale = sp.getFloat("height_scale", 1.0f).coerceIn(0.68f, 1.45f),
            bottomOffsetDp = sp.getFloat("bottom_offset_dp", 18f).coerceIn(0f, 160f),
            leftInsetDp = sp.getFloat("left_inset_dp", 0f).coerceIn(0f, 140f),
            rightInsetDp = sp.getFloat("right_inset_dp", 0f).coerceIn(0f, 140f),
            showKeyBorders = sp.getBoolean("show_key_borders", false),
            autocorrectEnabled = sp.getBoolean("autocorrect_enabled", true),
            glideTypingEnabled = sp.getBoolean("glide_typing_enabled", true),
            swipeBackspaceWords = sp.getBoolean("swipe_backspace_words", true),
            doubleSpacePeriod = sp.getBoolean("double_space_period", true),
            hapticsEnabled = sp.getBoolean("haptics_enabled", true),
            hapticStrengthMs = sp.getInt("haptic_strength_ms", 0).coerceIn(0, 25),
            recentEmojis = recents,
            clipboardHistory = clips,
            pinnedClips = pinned,
            userDictionary = userWords,
            lastClipTimestampMs = sp.getLong("last_clip_ts", 0L)
        )
        return applyActiveGeometry(base)
    }

    fun setMaterialMode(mode: KeyboardMaterialMode) {
        prefs?.edit()?.putString("material_mode", mode.name)?.apply()
        _state.value = _state.value.copy(materialMode = mode)
    }

    fun setTintPreset(preset: KeyboardTintPreset) {
        prefs?.edit()
            ?.putString("tint_preset", preset.name)
            ?.putFloat("tint_strength", preset.defaultStrength)
            ?.apply()
        _state.value = _state.value.copy(
            tintPreset = preset,
            tintStrength = preset.defaultStrength
        )
    }

    fun setCustomHue(hue: Float) {
        val clamped = hue.coerceIn(0f, 360f)
        val strength = _state.value.tintStrength.coerceAtLeast(0.28f)
        prefs?.edit()
            ?.putString("tint_preset", KeyboardTintPreset.CUSTOM.name)
            ?.putFloat("custom_hue", clamped)
            ?.putFloat("tint_strength", strength)
            ?.apply()
        _state.value = _state.value.copy(
            tintPreset = KeyboardTintPreset.CUSTOM,
            customHue = clamped,
            tintStrength = strength
        )
    }

    fun setTintStrength(strength: Float) {
        val clamped = strength.coerceIn(0f, 0.85f)
        prefs?.edit()?.putFloat("tint_strength", clamped)?.apply()
        _state.value = _state.value.copy(tintStrength = clamped)
    }

    fun toggleLanguage() {
        val next = if (_state.value.language == KeyboardLanguage.ENGLISH) {
            KeyboardLanguage.ARABIC
        } else {
            KeyboardLanguage.ENGLISH
        }
        setLanguage(next)
    }

    fun setLanguage(lang: KeyboardLanguage) {
        prefs?.edit()?.putString("language", lang.name)?.apply()
        _state.value = _state.value.copy(language = lang)
    }

    fun setShowNumberRow(show: Boolean) {
        prefs?.edit()?.putBoolean("show_number_row", show)?.apply()
        _state.value = _state.value.copy(showNumberRow = show)
    }

    fun setHeightScale(scale: Float) = updateKeyboardGeometry(heightScale = scale)

    fun setBottomOffsetDp(offsetDp: Float) = updateKeyboardGeometry(bottomOffsetDp = offsetDp)

    fun setSideInsetsDp(leftDp: Float, rightDp: Float) =
        updateKeyboardGeometry(leftInsetDp = leftDp, rightInsetDp = rightDp)

    /** Edits a specific orientation profile (used by the settings screen to tune either one). */
    fun updateGeometryFor(landscape: Boolean, geometry: KeyboardGeometry) {
        val g = KeyboardGeometry(
            heightScale = geometry.heightScale.coerceIn(0.68f, 1.45f),
            bottomOffsetDp = geometry.bottomOffsetDp.coerceIn(0f, 160f),
            leftInsetDp = geometry.leftInsetDp.coerceIn(0f, 140f),
            rightInsetDp = geometry.rightInsetDp.coerceIn(0f, 140f)
        )
        val suffix = if (landscape) "land" else "port"
        prefs?.edit()
            ?.putFloat("height_scale_$suffix", g.heightScale)
            ?.putFloat("bottom_offset_dp_$suffix", g.bottomOffsetDp)
            ?.putFloat("left_inset_dp_$suffix", g.leftInsetDp)
            ?.putFloat("right_inset_dp_$suffix", g.rightInsetDp)
            ?.apply()
        val s = _state.value
        val next = if (landscape) s.copy(landscapeGeometry = g) else s.copy(portraitGeometry = g)
        _state.value = applyActiveGeometry(next)
    }

    fun setSoundProfile(profile: KeySoundProfile) {
        prefs?.edit()?.putString("sound_profile", profile.name)?.apply()
        _state.value = _state.value.copy(soundProfile = profile)
    }

    fun setSoundVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        prefs?.edit()?.putFloat("sound_volume", v)?.apply()
        _state.value = _state.value.copy(soundVolume = v)
    }

    fun setAutoCapitalize(enabled: Boolean) {
        prefs?.edit()?.putBoolean("auto_capitalize", enabled)?.apply()
        _state.value = _state.value.copy(autoCapitalize = enabled)
    }

    fun setLearnWords(enabled: Boolean) {
        prefs?.edit()?.putBoolean("learn_words", enabled)?.apply()
        _state.value = _state.value.copy(learnWords = enabled)
    }

    fun setTranslatorEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("translator_enabled", enabled)?.apply()
        _state.value = _state.value.copy(translatorEnabled = enabled)
    }

    fun setCursorLoupeEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("cursor_loupe", enabled)?.apply()
        _state.value = _state.value.copy(cursorLoupeEnabled = enabled)
    }

    fun setKeyRippleEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("key_ripple", enabled)?.apply()
        _state.value = _state.value.copy(keyRippleEnabled = enabled)
    }

    /**
     * Records a committed word. Frequencies boost suggestions; an unknown word typed twice is
     * automatically added to the personal dictionary so it is never "corrected" again.
     */
    fun recordTypedWord(rawWord: String) {
        val current = _state.value
        if (!current.learnWords) return
        val clean = rawWord.trim().lowercase()
        if (clean.length < 2 || clean.length > 32 || !clean.all { it.isLetter() || it == '\'' }) return
        val freq = HashMap(current.userWordFrequency)
        val count = (freq[clean] ?: 0) + 1
        freq[clean] = count
        val trimmed: Map<String, Int> = if (freq.size > 1500) {
            freq.entries.sortedByDescending { it.value }.take(1200).associate { it.key to it.value }
        } else freq
        AutocorrectEngine.setUserFrequencies(trimmed)
        prefs?.edit()?.putString("user_word_freq", encodeFrequency(trimmed))?.apply()
        _state.value = current.copy(userWordFrequency = trimmed)
        if (count >= 2 && !AutocorrectEngine.isKnownWord(clean, current.language)) {
            learnUserWord(clean)
        }
    }

    fun forgetUserWord(word: String) {
        val current = _state.value
        val lower = word.lowercase()
        val updated = current.userDictionary - lower
        val freq = current.userWordFrequency - lower
        AutocorrectEngine.setUserWords(updated)
        AutocorrectEngine.setUserFrequencies(freq)
        prefs?.edit()
            ?.putString("user_dict_b64", encodeStringList(updated))
            ?.putString("user_word_freq", encodeFrequency(freq))
            ?.apply()
        _state.value = current.copy(userDictionary = updated, userWordFrequency = freq)
    }

    /** Updates ONLY the currently active orientation profile. */
    fun updateKeyboardGeometry(
        heightScale: Float = _state.value.heightScale,
        bottomOffsetDp: Float = _state.value.bottomOffsetDp,
        leftInsetDp: Float = _state.value.leftInsetDp,
        rightInsetDp: Float = _state.value.rightInsetDp
    ) {
        updateGeometryFor(
            landscape = _state.value.isLandscape,
            geometry = KeyboardGeometry(heightScale, bottomOffsetDp, leftInsetDp, rightInsetDp)
        )
    }

    fun resetKeyboardGeometry() {
        val landscape = _state.value.isLandscape
        updateGeometryFor(
            landscape,
            if (landscape) KeyboardGeometry(heightScale = 0.86f, bottomOffsetDp = 6f) else KeyboardGeometry()
        )
    }

    fun setShowKeyBorders(show: Boolean) {
        prefs?.edit()?.putBoolean("show_key_borders", show)?.apply()
        _state.value = _state.value.copy(showKeyBorders = show)
    }

    fun setAutocorrectEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("autocorrect_enabled", enabled)?.apply()
        _state.value = _state.value.copy(autocorrectEnabled = enabled)
    }

    fun setGlideTypingEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("glide_typing_enabled", enabled)?.apply()
        _state.value = _state.value.copy(glideTypingEnabled = enabled)
    }

    fun setSwipeBackspaceWords(enabled: Boolean) {
        prefs?.edit()?.putBoolean("swipe_backspace_words", enabled)?.apply()
        _state.value = _state.value.copy(swipeBackspaceWords = enabled)
    }

    fun setDoubleSpacePeriod(enabled: Boolean) {
        prefs?.edit()?.putBoolean("double_space_period", enabled)?.apply()
        _state.value = _state.value.copy(doubleSpacePeriod = enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("haptics_enabled", enabled)?.apply()
        _state.value = _state.value.copy(hapticsEnabled = enabled)
    }

    fun setHapticStrengthMs(ms: Int) {
        val clamped = ms.coerceIn(0, 25)
        prefs?.edit()?.putInt("haptic_strength_ms", clamped)?.apply()
        _state.value = _state.value.copy(hapticStrengthMs = clamped)
    }

    fun learnUserWord(rawWord: String) {
        val clean = rawWord.trim().removeSurrounding("\"").lowercase()
        if (clean.length < 2 || clean.length > 32 || !clean.all { it.isLetter() || it == '\'' }) return
        val current = _state.value
        if (clean in current.userDictionary) return
        val updated = (listOf(clean) + current.userDictionary).take(500).toSet()
        AutocorrectEngine.setUserWords(updated)
        prefs?.edit()?.putString("user_dict_b64", encodeStringList(updated))?.apply()
        _state.value = current.copy(userDictionary = updated)
    }

    fun recordEmojiUsed(emoji: String) {
        val updated = (listOf(emoji) + _state.value.recentEmojis.filterNot { it == emoji }).take(24)
        prefs?.edit()?.putString("recent_emojis", updated.joinToString(","))?.apply()
        _state.value = _state.value.copy(recentEmojis = updated)
    }

    fun addClipboardItem(rawText: String) {
        val text = rawText.trim('\u0000')
        if (text.isBlank()) return
        val current = _state.value
        if (current.clipboardHistory.firstOrNull() == text) return
        val now = System.currentTimeMillis()
        val updated = (listOf(text) + current.clipboardHistory.filterNot { it == text }).take(30)
        prefs?.edit()
            ?.putString("clipboard_history_b64", encodeStringList(updated))
            ?.putLong("last_clip_ts", now)
            ?.apply()
        _state.value = current.copy(
            clipboardHistory = updated,
            lastClipTimestampMs = now
        )
    }

    fun togglePinClip(clip: String) {
        val current = _state.value
        val updatedPinned = if (clip in current.pinnedClips) {
            current.pinnedClips - clip
        } else {
            current.pinnedClips + clip
        }
        prefs?.edit()?.putString("pinned_clips_b64", encodeStringList(updatedPinned))?.apply()
        _state.value = current.copy(pinnedClips = updatedPinned)
    }

    fun removeClip(clip: String) {
        val current = _state.value
        val updatedHistory = current.clipboardHistory.filterNot { it == clip }
        val updatedPinned = current.pinnedClips - clip
        prefs?.edit()
            ?.putString("clipboard_history_b64", encodeStringList(updatedHistory))
            ?.putString("pinned_clips_b64", encodeStringList(updatedPinned))
            ?.apply()
        _state.value = current.copy(
            clipboardHistory = updatedHistory,
            pinnedClips = updatedPinned
        )
    }

    fun clearUnpinnedClips() {
        val current = _state.value
        val kept = current.clipboardHistory.filter { it in current.pinnedClips }
        prefs?.edit()?.putString("clipboard_history_b64", encodeStringList(kept))?.apply()
        _state.value = current.copy(clipboardHistory = kept)
    }
}
