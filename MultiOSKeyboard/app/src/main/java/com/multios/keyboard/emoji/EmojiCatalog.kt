package com.multios.keyboard.emoji

import android.content.Context
import android.graphics.Paint

data class EmojiCategory(
    val id: String,
    val icon: String,
    val emojis: List<String>
)

/**
 * Complete system emoji catalog.
 *
 * Source: the official Unicode `emoji-test.txt` (Emoji 16.0, bundled in assets) which lists every
 * fully-qualified emoji with its group and CLDR name. At runtime every entry is checked with
 * [Paint.hasGlyph] against the phone's own emoji font, so the picker shows EXACTLY the emoji set the
 * device ships with — nothing missing, no tofu boxes for emoji the font can't draw.
 *
 * Skin-tone variants are folded under their base emoji (long-press / [variantsOf]).
 */
object EmojiCatalog {

    private data class Group(val id: String, val icon: String, val unicodeGroup: String)

    private val groups = listOf(
        Group("smileys", "😀", "Smileys & Emotion"),
        Group("people", "👋", "People & Body"),
        Group("nature", "🐻", "Animals & Nature"),
        Group("food", "🍔", "Food & Drink"),
        Group("travel", "✈️", "Travel & Places"),
        Group("activities", "⚽", "Activities"),
        Group("objects", "💡", "Objects"),
        Group("symbols", "❤️", "Symbols"),
        Group("flags", "🏳️", "Flags")
    )

    private val kaomoji = EmojiCategory(
        id = "kaomoji",
        icon = "ツ",
        emojis = listOf(
            "¯\\_(ツ)_/¯", "(•‿•)", "(◕‿◕)", "(｡♥‿♥｡)", "(づ｡◕‿‿◕｡)づ", "(╯°□°)╯︵ ┻━┻",
            "┬─┬ノ( º _ ºノ)", "(☞ﾟヮﾟ)☞", "☜(ﾟヮﾟ☜)", "ಠ_ಠ", "(¬‿¬)", "(ಥ﹏ಥ)",
            "(ง'̀-'́)ง", "ʕ•ᴥ•ʔ", "(ᵔᴥᵔ)", "(•_•)", "( •_•)>⌐■-■", "(⌐■_■)",
            "✧･ﾟ: *✧･ﾟ:*", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(っ˘ڡ˘ς)", "(˘³˘)♥", "(^_−)☆", "≧◡≦"
        )
    )

    /** Minimal fallback used only until the asset has been parsed. */
    private val fallback = listOf(
        EmojiCategory(
            "smileys", "😀",
            listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😉", "😊", "😇", "🥰", "😍", "😘", "😭", "🔥", "❤️", "👍", "🙏")
        ),
        kaomoji
    )

    @Volatile
    var categories: List<EmojiCategory> = fallback
        private set

    @Volatile
    private var names: Map<String, String> = emptyMap()

    @Volatile
    private var toneVariants: Map<String, List<String>> = emptyMap()

    @Volatile
    private var loaded = false

    private val skinTones = 0x1F3FB..0x1F3FF

    /** Parses the bundled Unicode list and filters it by what the system emoji font can render. */
    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        val paint = Paint()
        val byGroup = LinkedHashMap<String, MutableList<String>>()
        val nameMap = HashMap<String, String>(4_000)
        val variants = HashMap<String, MutableList<String>>()
        var currentGroup = ""

        runCatching {
            context.assets.open("emoji-test.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("# group:")) {
                        currentGroup = line.substringAfter("# group:").trim()
                        return@forEach
                    }
                    if (line.isBlank() || line.startsWith("#")) return@forEach
                    val semi = line.indexOf(';')
                    val hash = line.indexOf('#')
                    if (semi < 0 || hash < 0) return@forEach
                    val status = line.substring(semi + 1, hash).trim()
                    if (status != "fully-qualified") return@forEach
                    if (currentGroup == "Component") return@forEach

                    val codePoints = line.substring(0, semi).trim().split(' ')
                        .mapNotNull { it.toIntOrNull(16) }
                    if (codePoints.isEmpty()) return@forEach
                    val emoji = String(codePoints.toIntArray(), 0, codePoints.size)
                    if (!paint.hasGlyph(emoji)) return@forEach

                    val name = line.substring(hash + 1).trim()
                        .replace(Regex("^\\S+\\s+E\\d+\\.\\d+\\s+"), "")
                    nameMap[emoji] = name

                    val hasTone = codePoints.any { it in skinTones }
                    if (hasTone) {
                        val baseCps = codePoints.filter { it !in skinTones }
                        // Re-qualify single-codepoint bases that need FE0F to render as emoji
                        val base = String(baseCps.toIntArray(), 0, baseCps.size)
                        val key = if (base in nameMap) base else base + "\uFE0F"
                        variants.getOrPut(if (key in nameMap) key else base) { mutableListOf() }.add(emoji)
                    } else {
                        byGroup.getOrPut(currentGroup) { mutableListOf() }.add(emoji)
                    }
                }
            }
        }

        if (byGroup.isNotEmpty()) {
            val built = groups.mapNotNull { g ->
                val list = byGroup[g.unicodeGroup] ?: return@mapNotNull null
                EmojiCategory(g.id, g.icon, list)
            }
            names = nameMap
            toneVariants = variants
            categories = built + kaomoji
        }
        loaded = true
    }

    fun variantsOf(emoji: String): List<String> = toneVariants[emoji].orEmpty()

    fun nameOf(emoji: String): String? = names[emoji]

    private val searchKeywords: Map<String, List<String>> = mapOf(
        "lol" to listOf("😂", "🤣", "💀", "😭"),
        "lmao" to listOf("🤣", "😂", "💀"),
        "dead" to listOf("💀", "☠️", "😭"),
        "lit" to listOf("🔥", "✨", "⚡", "💯"),
        "pray" to listOf("🙏", "🤲", "🕌", "🕋"),
        "ok" to listOf("👌", "👍", "✅", "🫡"),
        "yes" to listOf("✅", "👍", "💯", "🙌"),
        "no" to listOf("❌", "👎", "🙅", "🚫"),
        "shrug" to listOf("🤷", "¯\\_(ツ)_/¯"),
        "money" to listOf("💰", "💸", "🤑", "💳", "💎")
    )

    /** Searches slang keywords first, then the official CLDR emoji names (e.g. "cat", "flag", "heart"). */
    fun search(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val results = LinkedHashSet<String>()
        searchKeywords.forEach { (key, list) -> if (key.startsWith(q) || q == key) results.addAll(list) }
        val nameSnapshot = names
        if (nameSnapshot.isNotEmpty()) {
            val words = q.split(' ').filter { it.isNotBlank() }
            for (cat in categories) {
                for (e in cat.emojis) {
                    val n = nameSnapshot[e] ?: continue
                    if (words.all { w -> n.contains(w) }) results.add(e)
                    if (results.size >= 120) break
                }
            }
        } else {
            categories.forEach { cat -> if (cat.id.contains(q)) results.addAll(cat.emojis.take(24)) }
        }
        return results.toList()
    }
}
