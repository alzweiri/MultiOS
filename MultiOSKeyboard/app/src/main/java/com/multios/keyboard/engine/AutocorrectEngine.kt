package com.multios.keyboard.engine

import android.content.Context
import com.multios.keyboard.data.KeyboardLanguage
import kotlin.math.abs
import kotlin.math.min

data class SuggestionResult(
    val typedWord: String,
    val primaryCorrection: String?,
    val candidates: List<String>
)

/**
 * On-device English & Arabic speller / autocorrect / glide decoder.
 *
 * Lexicon sources (bundled in assets, open-source):
 * - `en_words.txt`  — curated core list (highest priority)
 * - `en_50k.txt`    — hermitdave/FrequencyWords (OpenSubtitles 2018, CC-BY-SA 4.0), frequency ordered
 * - `ar_50k.txt`    — hermitdave/FrequencyWords Arabic list, frequency ordered
 *
 * Rules:
 * 1. A word that is valid (lexicon, learned user word or a regular inflection) is NEVER auto-replaced.
 *    `made`, `hell`, `were`, `form`, `knob` stay exactly as typed.
 * 2. The suggestion bar is EMPTY until the user has typed at least one letter.
 * 3. Prefix completions are only offered as tap candidates — never forced on Space.
 * 4. Words the user keeps typing (or restores via backspace-undo) are learned and ranked first.
 */
object AutocorrectEngine {

    @Volatile
    private var initialized = false

    private var validEnglishSet: HashSet<String> = HashSet()
    private var wordsByLength: Map<Int, List<IndexedWord>> = emptyMap()
    private var wordsByFirstChar: Map<Char, List<IndexedWord>> = emptyMap()

    private var arabicLexicon: List<String> = emptyList()
    private var arabicValidSet: HashSet<String> = HashSet()
    private var arabicBareIndex: Map<String, String> = emptyMap()

    @Volatile
    private var userLearnedSet: Set<String> = emptySet()

    @Volatile
    private var userFrequency: Map<String, Int> = emptyMap()

    private data class IndexedWord(val word: String, val lower: String, val rank: Int)

    /** Real words that look like missing-apostrophe contractions — never touched. */
    private val protectedRealWords = setOf(
        "wont", "cant", "well", "were", "ill", "id", "its", "hell", "shell", "lets", "whore", "wed", "hed", "shed"
    )

    /** Unambiguous non-word fixes (missing apostrophes, transpositions, classic misspellings). */
    private val englishDirectFixes: Map<String, String> = mapOf(
        "i" to "I", "im" to "I'm", "ive" to "I've", "id've" to "I'd've",
        "dont" to "don't", "didnt" to "didn't", "doesnt" to "doesn't", "isnt" to "isn't",
        "arent" to "aren't", "wasnt" to "wasn't", "werent" to "weren't", "havent" to "haven't",
        "hasnt" to "hasn't", "hadnt" to "hadn't", "wouldnt" to "wouldn't", "couldnt" to "couldn't",
        "shouldnt" to "shouldn't", "mustnt" to "mustn't", "youre" to "you're", "theyre" to "they're",
        "youve" to "you've", "theyve" to "they've", "weve" to "we've", "youll" to "you'll",
        "theyll" to "they'll", "thats" to "that's", "whats" to "what's", "whos" to "who's",
        "heres" to "here's", "theres" to "there's", "itll" to "it'll", "yall" to "y'all",
        "aint" to "ain't", "shouldve" to "should've", "wouldve" to "would've", "couldve" to "could've",
        "teh" to "the", "hte" to "the", "adn" to "and", "nad" to "and", "taht" to "that",
        "thsi" to "this", "wiht" to "with", "waht" to "what", "whta" to "what", "jsut" to "just",
        "knwo" to "know", "konw" to "know", "ahve" to "have", "hvae" to "have", "fomr" to "from",
        "abotu" to "about", "abuot" to "about", "becuase" to "because", "beacuse" to "because",
        "recieve" to "receive", "seperate" to "separate", "definately" to "definitely",
        "definitly" to "definitely", "occured" to "occurred", "untill" to "until", "wich" to "which",
        "wierd" to "weird", "thier" to "their", "freind" to "friend", "tommorow" to "tomorrow",
        "tomorow" to "tomorrow", "adress" to "address", "alot" to "a lot", "beleive" to "believe",
        "goverment" to "government", "enviroment" to "environment", "realy" to "really",
        "acheive" to "achieve", "accomodate" to "accommodate", "begining" to "beginning",
        "calender" to "calendar", "existance" to "existence", "noticable" to "noticeable",
        "persue" to "pursue", "succesful" to "successful", "truely" to "truly", "wether" to "whether"
    )

    private val qwertyCoords: Map<Char, Pair<Float, Float>> = mapOf(
        'q' to (0.0f to 0f), 'w' to (1.0f to 0f), 'e' to (2.0f to 0f), 'r' to (3.0f to 0f), 't' to (4.0f to 0f),
        'y' to (5.0f to 0f), 'u' to (6.0f to 0f), 'i' to (7.0f to 0f), 'o' to (8.0f to 0f), 'p' to (9.0f to 0f),
        'a' to (0.5f to 1f), 's' to (1.5f to 1f), 'd' to (2.5f to 1f), 'f' to (3.5f to 1f), 'g' to (4.5f to 1f),
        'h' to (5.5f to 1f), 'j' to (6.5f to 1f), 'k' to (7.5f to 1f), 'l' to (8.5f to 1f),
        'z' to (1.5f to 2f), 'x' to (2.5f to 2f), 'c' to (3.5f to 2f), 'v' to (4.5f to 2f), 'b' to (5.5f to 2f),
        'n' to (6.5f to 2f), 'm' to (7.5f to 2f)
    )

    private val fallbackCoreEnglish = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I", "it", "for", "not", "on", "with",
        "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if",
        "about", "who", "get", "which", "go", "me", "when", "make", "made", "makes", "making", "can", "like",
        "time", "no", "just", "him", "know", "knew", "take", "took", "into", "your", "good", "some", "could",
        "them", "see", "saw", "other", "than", "then", "now", "look", "only", "come", "came", "over", "think",
        "thought", "also", "back", "after", "use", "used", "how", "work", "well", "way", "even", "new", "want",
        "because", "any", "give", "gave", "day", "hell", "hello", "hey", "were", "was", "are", "is", "been",
        "said", "went", "got", "told", "found", "felt", "left", "kept", "let", "lets", "put", "set",
        "knob", "knobs", "widget", "widgets", "launcher", "desktop", "screen", "keyboard", "custom", "music",
        "sound", "volume", "clock", "lock", "settings", "home", "phone", "display", "color", "glass", "blur",
        "status", "bar", "quick", "panel", "drawer", "root", "power", "battery", "wifi", "bluetooth", "torch",
        "app", "apps", "device", "android", "system", "file", "text", "space", "enter", "gemini", "magisk",
        "shit", "fuck", "fucking", "damn", "bro", "bruh", "nah", "yeah", "okay", "awesome", "lowkey", "highkey"
    )

    private val arabicDirectFixes: Map<String, String> = mapOf(
        "انا" to "أنا", "انت" to "أنت", "انتم" to "أنتم", "انتي" to "أنتِ", "الى" to "إلى", "الا" to "إلا",
        "اذن" to "إذن", "اذا" to "إذا", "او" to "أو", "اين" to "أين", "امس" to "أمس", "الان" to "الآن",
        "اكثر" to "أكثر", "اقل" to "أقل", "افضل" to "أفضل", "اجمل" to "أجمل", "احسن" to "أحسن", "اول" to "أول",
        "اهلا" to "أهلاً", "سهلا" to "سهلاً", "شكرا" to "شكراً", "عفوا" to "عفواً", "جدا" to "جداً",
        "ايضا" to "أيضاً", "طبعا" to "طبعاً", "فعلا" to "فعلاً", "مثلا" to "مثلاً", "دائما" to "دائماً",
        "لان" to "لأن", "لانه" to "لأنه", "لانها" to "لأنها", "هاذا" to "هذا", "هاذه" to "هذه", "لاكن" to "لكن",
        "انشاءالله" to "إن شاء الله", "انشالله" to "إن شاء الله", "ماشاءالله" to "ما شاء الله",
        "الحمدلله" to "الحمد لله", "سبحانالله" to "سبحان الله", "استغفرالله" to "أستغفر الله", "باذن" to "بإذن"
    )

    private val arabicCore: List<String> = listOf(
        "في", "من", "على", "إلى", "عن", "مع", "هذا", "هذه", "التي", "الذي", "كان", "كل", "لا", "ما", "هو", "هي",
        "هم", "نحن", "أنا", "أنت", "الله", "مرحباً", "أهلاً", "شكراً", "جداً", "اليوم", "غداً", "كيف", "حالك",
        "بخير", "نعم", "طبعاً", "ممكن", "تمام", "جميل", "رائع", "ممتاز", "أفضل", "أكثر", "الآن", "هنا", "هناك",
        "قبل", "بعد", "عند", "حتى", "بين", "غير", "بدون", "فقط", "أيضاً", "لكن", "لأن", "إذا", "متى", "أين",
        "لماذا", "ماذا", "هل", "والله", "أكيد", "يلا", "خلاص", "شوي", "كثير", "أحبك", "حبيبي"
    )

    @Synchronized
    fun ensureInitialized(context: Context?) {
        if (initialized && validEnglishSet.size > 2000) return
        val loaded = ArrayList<String>(52_000)
        val seen = HashSet<String>(64_000)

        fun addWord(raw: String) {
            val w = raw.trim()
            if (w.isEmpty() || w.length > 24) return
            if (!w.all { it.isLetter() || it == '\'' }) return
            if (w.first() == '\'' || w.last() == '\'') return
            val lower = w.lowercase()
            if (lower.length == 1 && lower != "a" && lower != "i") return
            if (seen.add(lower)) loaded.add(if (lower == "i") "I" else w)
        }

        if (context != null) {
            runCatching {
                context.assets.open("en_words.txt").bufferedReader().useLines { lines -> lines.forEach { addWord(it) } }
            }
            runCatching {
                context.assets.open("en_50k.txt").bufferedReader().useLines { lines ->
                    lines.forEach { line -> addWord(line.substringBefore(' ')) }
                }
            }
        }
        fallbackCoreEnglish.forEach { addWord(it) }

        val indexed = loaded.mapIndexed { idx, w -> IndexedWord(w, w.lowercase(), idx) }
        validEnglishSet = seen
        wordsByLength = indexed.groupBy { it.lower.length }
        wordsByFirstChar = indexed.groupBy { it.lower[0] }

        // Arabic lexicon
        val arList = ArrayList<String>(50_000)
        val arSeen = HashSet<String>(60_000)
        arabicCore.forEach { if (arSeen.add(it)) arList.add(it) }
        if (context != null) {
            runCatching {
                context.assets.open("ar_50k.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val w = line.substringBefore(' ').trim()
                        if (w.length in 2..20 && w.all { it in '\u0600'..'\u06FF' && it != '،' && it != '؟' } && arSeen.add(w)) {
                            arList.add(w)
                        }
                    }
                }
            }
        }
        arabicLexicon = arList
        arabicValidSet = arSeen
        val bare = HashMap<String, String>(arList.size)
        for (w in arList) bare.putIfAbsent(normalizeArabicBare(w), w)
        arabicBareIndex = bare

        initialized = true
    }

    fun setUserWords(words: Set<String>) {
        userLearnedSet = words.map { it.lowercase() }.toSet()
    }

    fun setUserFrequencies(freq: Map<String, Int>) {
        userFrequency = freq
    }

    fun isKnownWord(word: String, language: KeyboardLanguage): Boolean {
        val lower = word.lowercase()
        return if (language == KeyboardLanguage.ARABIC) {
            lower in arabicValidSet || lower in userLearnedSet
        } else {
            isKnownValidEnglishWord(lower)
        }
    }

    fun isKnownValidEnglishWord(lower: String): Boolean {
        if (lower.isEmpty()) return true
        if (lower.length == 1 && lower[0].isLetter()) return lower == "a"
        if (lower in validEnglishSet || lower in userLearnedSet) return true
        if (lower.any { it.isDigit() }) return true

        fun known(s: String) = s in validEnglishSet || s in userLearnedSet

        if (lower.endsWith("'s") && lower.length > 3 && known(lower.dropLast(2))) return true
        if (lower.endsWith("s") && lower.length > 3 && !lower.endsWith("ss")) {
            if (known(lower.dropLast(1))) return true
            if (lower.endsWith("ies") && lower.length > 4 && known(lower.dropLast(3) + "y")) return true
            if (lower.endsWith("es") && lower.length > 4 && known(lower.dropLast(2))) return true
        }
        if (lower.endsWith("ed") && lower.length > 4) {
            val s1 = lower.dropLast(2)
            if (known(s1) || known(lower.dropLast(1))) return true
            if (lower.endsWith("ied") && known(lower.dropLast(3) + "y")) return true
            if (s1.length >= 3 && s1.last() == s1[s1.length - 2] && known(s1.dropLast(1))) return true
        }
        if (lower.endsWith("ing") && lower.length > 5) {
            val s1 = lower.dropLast(3)
            if (known(s1) || known(s1 + "e")) return true
            if (s1.length >= 3 && s1.last() == s1[s1.length - 2] && known(s1.dropLast(1))) return true
        }
        if (lower.endsWith("ly") && lower.length > 4) {
            if (known(lower.dropLast(2))) return true
            if (lower.endsWith("ily") && known(lower.dropLast(3) + "y")) return true
        }
        if (lower.endsWith("er") && lower.length > 4 && (known(lower.dropLast(2)) || known(lower.dropLast(1)))) return true
        if (lower.endsWith("est") && lower.length > 5 && (known(lower.dropLast(3)) || known(lower.dropLast(2)))) return true
        if (lower.endsWith("ness") && lower.length > 6 && known(lower.dropLast(4))) return true
        if (lower.startsWith("un") && lower.length > 4 && known(lower.drop(2))) return true
        if (lower.startsWith("re") && lower.length > 4 && known(lower.drop(2))) return true
        return false
    }

    fun computeSuggestions(
        currentWord: String,
        previousWord: String,
        language: KeyboardLanguage,
        autocorrectEnabled: Boolean
    ): SuggestionResult {
        if (!initialized) ensureInitialized(null)
        val trimmed = currentWord.trim()
        // Idle (nothing typed for the current word) → keep the strip completely blank.
        if (trimmed.isEmpty()) {
            return SuggestionResult(typedWord = "", primaryCorrection = null, candidates = emptyList())
        }
        return if (language == KeyboardLanguage.ARABIC) {
            computeArabic(trimmed, autocorrectEnabled)
        } else {
            computeEnglish(trimmed, autocorrectEnabled)
        }
    }

    private fun userBoost(lower: String): Int = userFrequency[lower] ?: 0

    private fun computeEnglish(word: String, autocorrectEnabled: Boolean): SuggestionResult {
        val lower = word.lowercase()
        val isRealWord = isKnownValidEnglishWord(lower) && lower != "i"

        // Curated fixes only contain non-words (missing apostrophes / misspellings); real look-alikes are protected.
        val directFix = if (lower !in protectedRealWords && lower !in userLearnedSet) {
            englishDirectFixes[lower]?.let { matchCase(word, it) }
        } else null

        // Prefix completions (learned words first, then frequency)
        val firstCh = lower.first()
        val learnedPrefix = userFrequency.entries
            .asSequence()
            .filter { it.key.length > lower.length && it.key.startsWith(lower) }
            .sortedByDescending { it.value }
            .map { matchCase(word, it.key) }
            .take(2)
            .toList()
        val dictPrefix = wordsByFirstChar[firstCh].orEmpty()
            .asSequence()
            .filter { it.lower.length > lower.length && it.lower.startsWith(lower) }
            .take(4)
            .map { matchCase(word, it.word) }
            .toList()
        val prefixMatches = (learnedPrefix + dictPrefix).distinctBy { it.lowercase() }.take(4)

        val fuzzyMatches: List<String> = if (!isRealWord && lower.length >= 2) {
            findTypoCandidates(lower).map { matchCase(word, it) }
        } else {
            emptyList()
        }

        val primary: String? = when {
            !autocorrectEnabled -> null
            directFix != null && directFix != word -> directFix
            isRealWord -> null
            lower.length >= 3 && fuzzyMatches.isNotEmpty() -> fuzzyMatches.first()
            else -> null
        }

        val centerSlot = when {
            primary != null -> primary
            isRealWord -> word
            lower == "i" -> "I"
            prefixMatches.isNotEmpty() -> prefixMatches.first()
            fuzzyMatches.isNotEmpty() -> fuzzyMatches.first()
            else -> word
        }
        val rightSlot = (prefixMatches + fuzzyMatches)
            .firstOrNull { !it.equals(centerSlot, ignoreCase = true) && !it.equals(word, ignoreCase = true) }
            ?: ""

        return SuggestionResult(
            typedWord = word,
            primaryCorrection = primary,
            candidates = listOf("\"$word\"", centerSlot, rightSlot).filter { it.isNotBlank() }
        )
    }

    private fun findTypoCandidates(lower: String): List<String> {
        val maxDist = if (lower.length >= 6) 2 else 1
        val pool = ArrayList<IndexedWord>(8_000)
        for (len in (lower.length - maxDist)..(lower.length + maxDist)) {
            wordsByLength[len]?.let { pool.addAll(it) }
        }
        val scored = ArrayList<Pair<String, Float>>(16)
        for (cand in pool) {
            if (cand.lower == lower) continue
            val d = boundedDamerau(lower, cand.lower, maxDist)
            if (d > maxDist) continue
            // Distance-2 candidates must be reasonably common words
            if (d == 2 && cand.rank > 20_000) continue
            val score = scoreTypoCandidate(lower, cand.lower, cand.rank) + (d - 1) * 9f - userBoost(cand.lower) * 2f
            scored.add(cand.word to score)
        }
        for ((learned, count) in userFrequency) {
            if (learned == lower) continue
            val d = boundedDamerau(lower, learned, maxDist)
            if (d <= maxDist) scored.add(learned to (d * 4f - count * 2f))
        }
        return scored.sortedBy { it.second }.map { it.first }.distinctBy { it.lowercase() }.take(3)
    }

    private fun scoreTypoCandidate(typed: String, candidate: String, rank: Int): Float {
        var penalty = rank * 0.0009f
        if (typed.first() != candidate.first()) {
            val d = keyDistance(typed.first(), candidate.first())
            penalty += if (d <= 1.5f) 3.5f else 14f
        }
        if (typed.length == candidate.length) {
            for (i in typed.indices) {
                if (typed[i] != candidate[i]) {
                    penalty += if (keyDistance(typed[i], candidate[i]) <= 1.5f) 0.8f else 6.0f
                }
            }
        } else {
            penalty += 2.5f
        }
        return penalty
    }

    private fun keyDistance(a: Char, b: Char): Float {
        val ca = qwertyCoords[a] ?: return 5f
        val cb = qwertyCoords[b] ?: return 5f
        val dx = ca.first - cb.first
        val dy = ca.second - cb.second
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun decodeGlideSequence(visitedKeys: List<String>, language: KeyboardLanguage): List<String> {
        if (!initialized) ensureInitialized(null)
        if (visitedKeys.size < 2) return emptyList()
        if (language == KeyboardLanguage.ARABIC) {
            val raw = visitedKeys.joinToString("")
            val first = raw.firstOrNull() ?: return emptyList()
            val last = raw.lastOrNull() ?: return emptyList()
            return arabicLexicon.asSequence()
                .filter { it.length >= 2 && it.first() == first && it.last() == last && it.length <= raw.length + 2 }
                .take(3).toList()
        }
        val collapsed = buildString {
            for (k in visitedKeys) {
                val c = k.lowercase().firstOrNull() ?: continue
                if (c.isLetter() && (isEmpty() || last() != c)) append(c)
            }
        }
        if (collapsed.length < 2) return emptyList()
        val firstChar = collapsed.first()
        val lastChar = collapsed.last()
        return wordsByFirstChar[firstChar].orEmpty().asSequence()
            .filter { it.rank < 25_000 }
            .filter { cand ->
                cand.lower.length >= 2 && cand.lower.length <= collapsed.length + 3 &&
                    (cand.lower.last() == lastChar || keyDistance(cand.lower.last(), lastChar) <= 1.2f)
            }
            .mapNotNull { cand ->
                val ratio = subsequenceCoverage(cand.lower, collapsed)
                if (ratio < 0.62f) null
                else {
                    val endBonus = if (cand.lower.last() == lastChar) 0f else 4f
                    val lenDiff = abs(cand.lower.length - collapsed.length) * 0.35f
                    cand.word to ((1f - ratio) * 20f + endBonus + lenDiff + cand.rank * 0.0006f - userBoost(cand.lower))
                }
            }
            .sortedBy { it.second }
            .take(3)
            .map { it.first }
            .toList()
    }

    private fun subsequenceCoverage(target: String, path: String): Float {
        var pIdx = 0
        var matched = 0
        for (ch in target) {
            var foundAt = -1
            for (k in pIdx until path.length) {
                if (path[k] == ch || keyDistance(path[k], ch) <= 1.15f) {
                    foundAt = k
                    break
                }
            }
            if (foundAt >= 0) {
                matched++
                pIdx = foundAt
            }
        }
        return matched.toFloat() / target.length.toFloat()
    }

    private fun computeArabic(word: String, autocorrectEnabled: Boolean): SuggestionResult {
        val normalized = normalizeArabicBare(word)
        val direct = arabicDirectFixes[word] ?: arabicDirectFixes[normalized]
        val isValid = word in arabicValidSet || word.lowercase() in userLearnedSet
        val normalizedMatch = arabicBareIndex[normalized]?.takeIf { it != word }

        val prefixMatches = arabicLexicon.asSequence()
            .filter { it != word && it.length > word.length && (it.startsWith(word) || normalizeArabicBare(it).startsWith(normalized)) }
            .take(3)
            .toList()

        val primary: String? = when {
            !autocorrectEnabled -> null
            direct != null && direct != word -> direct
            isValid -> null
            normalizedMatch != null -> normalizedMatch
            else -> null
        }
        val centerSlot = primary ?: if (isValid) word else prefixMatches.firstOrNull() ?: word
        val rightSlot = prefixMatches.firstOrNull { it != centerSlot && it != word } ?: ""
        return SuggestionResult(
            typedWord = word,
            primaryCorrection = primary,
            candidates = listOf("\"$word\"", centerSlot, rightSlot).filter { it.isNotBlank() }
        )
    }

    private fun normalizeArabicBare(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                in '\u064B'..'\u0652' -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun matchCase(source: String, target: String): String {
        if (target == "I" || target.startsWith("I'")) return target
        if (source.length > 1 && source.all { it.isUpperCase() }) return target.uppercase()
        if (source.firstOrNull()?.isUpperCase() == true) return target.replaceFirstChar { it.uppercaseChar() }
        return target
    }

    /** Optimal-string-alignment (Damerau) distance with early exit above [max]. */
    private fun boundedDamerau(a: String, b: String, max: Int): Int {
        val la = a.length
        val lb = b.length
        if (abs(la - lb) > max) return max + 1
        var prevPrev = IntArray(lb + 1)
        var prev = IntArray(lb + 1) { it }
        var cur = IntArray(lb + 1)
        for (i in 1..la) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = min(min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = min(v, prevPrev[j - 2] + 1)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val tmp = prevPrev
            prevPrev = prev
            prev = cur
            cur = tmp
        }
        return prev[lb]
    }
}
