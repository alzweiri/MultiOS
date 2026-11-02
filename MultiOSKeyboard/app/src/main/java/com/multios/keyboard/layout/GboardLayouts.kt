package com.multios.keyboard.layout

import com.multios.keyboard.data.KeyboardLanguage

enum class KeyboardLayer {
    LETTERS,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    CLIPBOARD,
    TEXT_EDIT,
    TRANSLATOR
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

enum class KeyRole {
    CHAR,
    SHIFT,
    BACKSPACE,
    SYMBOLS_TOGGLE,
    SECONDARY_SYMBOLS,
    COMMA,
    PERIOD,
    LANGUAGE_GLOBE,
    EMOJI,
    SPACE,
    ENTER_ACTION
}

data class KeySpec(
    val label: String,
    val commitText: String = label,
    val topHint: String? = null,
    val alternates: List<String> = emptyList(),
    val weight: Float = 1f,
    val role: KeyRole = KeyRole.CHAR,
    val isModifierStyle: Boolean = role != KeyRole.CHAR && role != KeyRole.SPACE && role != KeyRole.COMMA && role != KeyRole.PERIOD
)

/**
 * Exact Gboard Key Geometry & Multi-Option Hold Alternates for English (`en_US`) and Arabic (`ar`).
 */
object GboardLayouts {

    fun buildRows(
        language: KeyboardLanguage,
        layer: KeyboardLayer,
        shiftState: ShiftState,
        showNumberRow: Boolean
    ): List<List<KeySpec>> {
        return when (layer) {
            KeyboardLayer.LETTERS -> {
                if (language == KeyboardLanguage.ARABIC) {
                    buildArabicLetterRows(showNumberRow)
                } else {
                    buildEnglishLetterRows(shiftState, showNumberRow)
                }
            }
            KeyboardLayer.SYMBOLS_1 -> buildSymbols1Rows(language)
            KeyboardLayer.SYMBOLS_2 -> buildSymbols2Rows(language)
            KeyboardLayer.EMOJI, KeyboardLayer.CLIPBOARD, KeyboardLayer.TEXT_EDIT, KeyboardLayer.TRANSLATOR -> emptyList()
        }
    }

    // =========================================================================
    // ENGLISH QWERTY — EXACT GBOARD FORMAT & LONG-PRESS ALTERNATES
    // =========================================================================
    private fun buildEnglishLetterRows(
        shiftState: ShiftState,
        showNumberRow: Boolean
    ): List<List<KeySpec>> {
        val upper = shiftState != ShiftState.OFF
        fun c(ch: String) = if (upper) ch.uppercase() else ch
        fun alts(vararg items: String): List<String> =
            items.map { if (upper && it.length == 1 && it[0].isLetter()) it.uppercase() else it }

        val rows = mutableListOf<List<KeySpec>>()

        if (showNumberRow) {
            rows.add(
                listOf(
                    KeySpec("1", alternates = listOf("1", "¹", "½", "⅓", "¼", "⅛")),
                    KeySpec("2", alternates = listOf("2", "²", "⅔")),
                    KeySpec("3", alternates = listOf("3", "³", "¾", "⅜")),
                    KeySpec("4", alternates = listOf("4", "⁴")),
                    KeySpec("5", alternates = listOf("5", "⁵", "⅝")),
                    KeySpec("6", alternates = listOf("6", "⁶")),
                    KeySpec("7", alternates = listOf("7", "⁷", "⅞")),
                    KeySpec("8", alternates = listOf("8", "⁸", "∞")),
                    KeySpec("9", alternates = listOf("9", "⁹")),
                    KeySpec("0", alternates = listOf("0", "⁰", "ⁿ", "∅"))
                )
            )
        }

        // Row 1: Q W E R T Y U I O P (with Gboard 1..0 hints & hold alternates)
        rows.add(
            listOf(
                KeySpec(c("q"), topHint = if (!showNumberRow) "1" else null, alternates = alts("1", "%", "\\")),
                KeySpec(c("w"), topHint = if (!showNumberRow) "2" else null, alternates = alts("2", "~")),
                KeySpec(c("e"), topHint = if (!showNumberRow) "3" else null, alternates = alts("3", "è", "é", "ê", "ë", "ē", "€")),
                KeySpec(c("r"), topHint = if (!showNumberRow) "4" else null, alternates = alts("4", "|")),
                KeySpec(c("t"), topHint = if (!showNumberRow) "5" else null, alternates = alts("5", "{", "[")),
                KeySpec(c("y"), topHint = if (!showNumberRow) "6" else null, alternates = alts("6", "}", "]", "¥")),
                KeySpec(c("u"), topHint = if (!showNumberRow) "7" else null, alternates = alts("7", "ù", "ú", "û", "ü", "ū", "<")),
                KeySpec(c("i"), topHint = if (!showNumberRow) "8" else null, alternates = alts("8", "ì", "í", "î", "ï", "ī", ">")),
                KeySpec(c("o"), topHint = if (!showNumberRow) "9" else null, alternates = alts("9", "ò", "ó", "ô", "õ", "ö", "ø", "ō")),
                KeySpec(c("p"), topHint = if (!showNumberRow) "0" else null, alternates = alts("0", "=", "£"))
            )
        )

        // Row 2: A S D F G H J K L (with Gboard symbol & accent hold alternates)
        rows.add(
            listOf(
                KeySpec(c("a"), topHint = "@", alternates = alts("@", "à", "á", "â", "ä", "æ", "ã", "å", "ā")),
                KeySpec(c("s"), topHint = "#", alternates = alts("#", "$", "ß", "ś", "š")),
                KeySpec(c("d"), topHint = "$", alternates = alts("$", "ď", "ð")),
                KeySpec(c("f"), topHint = "_", alternates = alts("_")),
                KeySpec(c("g"), topHint = "&", alternates = alts("&")),
                KeySpec(c("h"), topHint = "-", alternates = alts("-")),
                KeySpec(c("j"), topHint = "+", alternates = alts("+")),
                KeySpec(c("k"), topHint = "(", alternates = alts("(")),
                KeySpec(c("l"), topHint = ")", alternates = alts(")"))
            )
        )

        // Row 3: SHIFT + Z X C V B N M + BACKSPACE
        rows.add(
            listOf(
                KeySpec("⇧", commitText = "", weight = 1.48f, role = KeyRole.SHIFT),
                KeySpec(c("z"), topHint = "*", alternates = alts("*", "ž", "ź", "ż")),
                KeySpec(c("x"), topHint = "\"", alternates = alts("\"")),
                KeySpec(c("c"), topHint = "'", alternates = alts("'", "ç", "ć", "č")),
                KeySpec(c("v"), topHint = ":", alternates = alts(":")),
                KeySpec(c("b"), topHint = ";", alternates = alts(";")),
                KeySpec(c("n"), topHint = "!", alternates = alts("!", "ñ", "ń")),
                KeySpec(c("m"), topHint = "?", alternates = alts("?")),
                KeySpec("⌫", commitText = "", weight = 1.48f, role = KeyRole.BACKSPACE)
            )
        )

        // Row 4: ?123 | , | Globe | Emoji | Spacebar | . | Action
        rows.add(
            listOf(
                KeySpec("?123", commitText = "", weight = 1.38f, role = KeyRole.SYMBOLS_TOGGLE),
                KeySpec(",", commitText = ",", alternates = listOf(",", "'", "\"", ";", "/"), weight = 0.92f, role = KeyRole.COMMA),
                KeySpec("🌐", commitText = "", weight = 0.92f, role = KeyRole.LANGUAGE_GLOBE),
                KeySpec("English", commitText = " ", weight = 3.85f, role = KeyRole.SPACE),
                KeySpec(".", commitText = ".", alternates = listOf(".", "?", "!", ",", "'", "\"", ":", ";", "@", "-"), weight = 0.92f, role = KeyRole.PERIOD),
                KeySpec("↵", commitText = "\n", weight = 1.42f, role = KeyRole.ENTER_ACTION)
            )
        )

        return rows
    }

    // =========================================================================
    // ARABIC — EXACT GBOARD ARABIC FORMAT & TASHKEEL / HAMZA ALTERNATES
    // =========================================================================
    private fun buildArabicLetterRows(showNumberRow: Boolean): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()

        if (showNumberRow) {
            rows.add(
                listOf(
                    KeySpec("١", alternates = listOf("1", "١")),
                    KeySpec("٢", alternates = listOf("2", "٢")),
                    KeySpec("٣", alternates = listOf("3", "٣")),
                    KeySpec("٤", alternates = listOf("4", "٤")),
                    KeySpec("٥", alternates = listOf("5", "٥")),
                    KeySpec("٦", alternates = listOf("6", "٦")),
                    KeySpec("٧", alternates = listOf("7", "٧")),
                    KeySpec("٨", alternates = listOf("8", "٨")),
                    KeySpec("٩", alternates = listOf("9", "٩")),
                    KeySpec("٠", alternates = listOf("0", "٠"))
                )
            )
        }

        // Row 1 (11 keys): ض ص ث ق ف غ ع ه خ ح ج
        rows.add(
            listOf(
                KeySpec("ض", topHint = "َ", alternates = listOf("َ", "ً", "١", "1")),
                KeySpec("ص", topHint = "ُ", alternates = listOf("ُ", "ٌ", "٢", "2")),
                KeySpec("ث", topHint = "ِ", alternates = listOf("ِ", "ٍ", "٣", "3")),
                KeySpec("ق", topHint = "ّ", alternates = listOf("ّ", "ْ", "٤", "4", "ڤ")),
                KeySpec("ف", alternates = listOf("٥", "5", "ڤ", "پ")),
                KeySpec("غ", alternates = listOf("٦", "6")),
                KeySpec("ع", alternates = listOf("٧", "7")),
                KeySpec("ه", alternates = listOf("٨", "8", "ھ", "ة")),
                KeySpec("خ", alternates = listOf("٩", "9")),
                KeySpec("ح", alternates = listOf("٠", "0")),
                KeySpec("ج", alternates = listOf("چ", "ـ"))
            )
        )

        // Row 2 (11 keys): ش س ي ب ل ا ت ن م ك ط
        rows.add(
            listOf(
                KeySpec("ش", alternates = listOf("ِ", "ٍ")),
                KeySpec("س", alternates = listOf("ُ", "ٌ")),
                KeySpec("ي", alternates = listOf("ئ", "ى", "ی")),
                KeySpec("ب", alternates = listOf("پ")),
                KeySpec("ل", alternates = listOf("لا", "لأ", "لإ", "لآ")),
                KeySpec("ا", topHint = "أ", alternates = listOf("أ", "إ", "آ", "ٱ", "ء")),
                KeySpec("ت", alternates = listOf("ة")),
                KeySpec("ن"),
                KeySpec("م"),
                KeySpec("ك", alternates = listOf("گ", ":")),
                KeySpec("ط", alternates = listOf("ظ", "\""))
            )
        )

        // Row 3 (11 items): ذ ء ؤ ر لا ى ة و ز ظ + Backspace
        rows.add(
            listOf(
                KeySpec("ذ", topHint = "ّ", alternates = listOf("ّ", "ْ", "ٰ")),
                KeySpec("ء", alternates = listOf("أ", "إ", "ؤ", "ئ")),
                KeySpec("ؤ", alternates = listOf("و", "ء")),
                KeySpec("ر", alternates = listOf("ژ")),
                KeySpec("لا", alternates = listOf("لأ", "لإ", "لآ")),
                KeySpec("ى", alternates = listOf("ي", "ئ", "ٰ")),
                KeySpec("ة", alternates = listOf("ه", "ت")),
                KeySpec("و", alternates = listOf("ؤ")),
                KeySpec("ز", alternates = listOf("ژ")),
                KeySpec("ظ", alternates = listOf("ط")),
                KeySpec("⌫", commitText = "", weight = 1.42f, role = KeyRole.BACKSPACE)
            )
        )

        // Row 4: ؟١٢٣ | ، | Globe | العربية Spacebar | . | Action
        rows.add(
            listOf(
                KeySpec("؟١٢٣", commitText = "", weight = 1.38f, role = KeyRole.SYMBOLS_TOGGLE),
                KeySpec("،", commitText = "،", alternates = listOf("،", "؛", "؟", "!"), weight = 0.92f, role = KeyRole.COMMA),
                KeySpec("🌐", commitText = "", weight = 0.92f, role = KeyRole.LANGUAGE_GLOBE),
                KeySpec("العربية", commitText = " ", weight = 3.85f, role = KeyRole.SPACE),
                KeySpec(".", commitText = ".", alternates = listOf(".", "؟", "!", "،", "؛", "\"", "'", ":"), weight = 0.92f, role = KeyRole.PERIOD),
                KeySpec("↵", commitText = "\n", weight = 1.42f, role = KeyRole.ENTER_ACTION)
            )
        )

        return rows
    }

    // =========================================================================
    // SYMBOLS LAYER 1 (?123 / ؟١٢٣)
    // =========================================================================
    private fun buildSymbols1Rows(language: KeyboardLanguage): List<List<KeySpec>> {
        val isAr = language == KeyboardLanguage.ARABIC
        val nums = if (isAr) {
            listOf(
                KeySpec("١", alternates = listOf("1")), KeySpec("٢", alternates = listOf("2")),
                KeySpec("٣", alternates = listOf("3")), KeySpec("٤", alternates = listOf("4")),
                KeySpec("٥", alternates = listOf("5")), KeySpec("٦", alternates = listOf("6")),
                KeySpec("٧", alternates = listOf("7")), KeySpec("٨", alternates = listOf("8")),
                KeySpec("٩", alternates = listOf("9")), KeySpec("٠", alternates = listOf("0"))
            )
        } else {
            listOf(
                KeySpec("1", alternates = listOf("¹", "½", "¼")),
                KeySpec("2", alternates = listOf("²", "⅔")),
                KeySpec("3", alternates = listOf("³", "¾")),
                KeySpec("4", alternates = listOf("⁴")),
                KeySpec("5", alternates = listOf("⁵")),
                KeySpec("6", alternates = listOf("⁶")),
                KeySpec("7", alternates = listOf("⁷")),
                KeySpec("8", alternates = listOf("⁸")),
                KeySpec("9", alternates = listOf("⁹")),
                KeySpec("0", alternates = listOf("⁰", "∅"))
            )
        }

        val row2 = listOf(
            KeySpec("@"), KeySpec("#"), KeySpec("$", alternates = listOf("€", "£", "¥", "¢", "﷼", "د.إ")),
            KeySpec("_"), KeySpec("&"), KeySpec("-", alternates = listOf("–", "—", "•")),
            KeySpec("+", alternates = listOf("±")), KeySpec("(", alternates = listOf("[", "{", "<")),
            KeySpec(")", alternates = listOf("]", "}", ">")), KeySpec("/", alternates = listOf("\\"))
        )

        val qMark = if (isAr) KeySpec("؟", alternates = listOf("?", "¿")) else KeySpec("?", alternates = listOf("¿", "؟"))
        val semi = if (isAr) KeySpec("؛", alternates = listOf(";")) else KeySpec(";")

        val row3 = listOf(
            KeySpec("=\\<", commitText = "", weight = 1.48f, role = KeyRole.SECONDARY_SYMBOLS),
            KeySpec("*", alternates = listOf("★", "†", "‡")),
            KeySpec("\"", alternates = listOf("“", "”", "«", "»")),
            KeySpec("'", alternates = listOf("‘", "’", "`")),
            KeySpec(":"),
            semi,
            KeySpec("!", alternates = listOf("¡")),
            qMark,
            KeySpec("⌫", commitText = "", weight = 1.48f, role = KeyRole.BACKSPACE)
        )

        val abcLabel = if (isAr) "أ ب ج" else "ABC"
        val commaLabel = if (isAr) "،" else ","

        val row4 = listOf(
            KeySpec(abcLabel, commitText = "", weight = 1.38f, role = KeyRole.SYMBOLS_TOGGLE),
            KeySpec(commaLabel, commitText = commaLabel, weight = 0.92f, role = KeyRole.COMMA),
            KeySpec("🌐", commitText = "", weight = 0.92f, role = KeyRole.LANGUAGE_GLOBE),
            KeySpec(" ", commitText = " ", weight = 3.85f, role = KeyRole.SPACE),
            KeySpec(".", commitText = ".", weight = 0.92f, role = KeyRole.PERIOD),
            KeySpec("↵", commitText = "\n", weight = 1.42f, role = KeyRole.ENTER_ACTION)
        )

        return listOf(nums, row2, row3, row4)
    }

    // =========================================================================
    // SYMBOLS LAYER 2 (=\<)
    // =========================================================================
    private fun buildSymbols2Rows(language: KeyboardLanguage): List<List<KeySpec>> {
        val isAr = language == KeyboardLanguage.ARABIC
        val row1 = listOf(
            KeySpec("~"), KeySpec("`"), KeySpec("|"), KeySpec("•"), KeySpec("√"),
            KeySpec("π"), KeySpec("÷"), KeySpec("×"), KeySpec("§"), KeySpec("∆")
        )
        val row2 = listOf(
            KeySpec("£"), KeySpec("¢"), KeySpec("€"), KeySpec("¥"), KeySpec("^"),
            KeySpec("°"), KeySpec("="), KeySpec("{"), KeySpec("}"), KeySpec("\\")
        )
        val row3 = listOf(
            KeySpec(if (isAr) "؟١٢٣" else "?123", commitText = "", weight = 1.48f, role = KeyRole.SECONDARY_SYMBOLS),
            KeySpec("%", alternates = listOf("‰")), KeySpec("©"), KeySpec("®"), KeySpec("™"),
            KeySpec("✓"), KeySpec("["), KeySpec("]"),
            KeySpec("⌫", commitText = "", weight = 1.48f, role = KeyRole.BACKSPACE)
        )
        val abcLabel = if (isAr) "أ ب ج" else "ABC"
        val row4 = listOf(
            KeySpec(abcLabel, commitText = "", weight = 1.38f, role = KeyRole.SYMBOLS_TOGGLE),
            KeySpec("<", commitText = "<", weight = 0.92f),
            KeySpec("🌐", commitText = "", weight = 0.92f, role = KeyRole.LANGUAGE_GLOBE),
            KeySpec(" ", commitText = " ", weight = 3.85f, role = KeyRole.SPACE),
            KeySpec(">", commitText = ">", weight = 0.92f),
            KeySpec("↵", commitText = "\n", weight = 1.42f, role = KeyRole.ENTER_ACTION)
        )
        return listOf(row1, row2, row3, row4)
    }
}
