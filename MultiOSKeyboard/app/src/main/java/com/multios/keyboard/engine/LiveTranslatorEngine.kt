package com.multios.keyboard.engine

/**
 * High-speed offline bidirectional English <-> Arabic dictionary translator.
 * Contains common phrases, greetings, questions, technology terms, and daily vocabulary.
 */
object LiveTranslatorEngine {

    private val enToAr: Map<String, String> = mapOf(
        "hello" to "مرحباً",
        "hi" to "أهلاً",
        "hey" to "مرحباً",
        "good morning" to "صباح الخير",
        "good evening" to "مساء الخير",
        "how are you" to "كيف حالك",
        "i am fine" to "أنا بخير",
        "thank you" to "شكراً لك",
        "thanks" to "شكراً",
        "you are welcome" to "عفواً",
        "please" to "من فضلك",
        "yes" to "نعم",
        "no" to "لا",
        "ok" to "حسناً",
        "okay" to "تمام",
        "peace be upon you" to "السلام عليكم",
        "goodbye" to "مع السلامة",
        "bye" to "إلى اللقاء",
        "see you" to "أراك لاحقاً",
        "what is your name" to "ما اسمك",
        "my name is" to "اسمي",
        "where are you" to "أين أنت",
        "i love you" to "أحبك",
        "what" to "ماذا",
        "why" to "لماذا",
        "how" to "كيف",
        "when" to "متى",
        "where" to "أين",
        "who" to "من",
        "friend" to "صديق",
        "brother" to "أخي",
        "sister" to "أختي",
        "family" to "عائلة",
        "home" to "البيت",
        "work" to "العمل",
        "phone" to "هاتف",
        "keyboard" to "لوحة المفاتيح",
        "screen" to "شاشة",
        "love" to "حب",
        "good" to "جيد",
        "great" to "رائع",
        "bad" to "سيء",
        "beautiful" to "جميل",
        "today" to "اليوم",
        "tomorrow" to "غداً",
        "yesterday" to "أمس",
        "now" to "الآن",
        "later" to "لاحقاً",
        "help" to "مساعدة",
        "sorry" to "آسف",
        "excuse me" to "عذراً",
        "awesome" to "رهيب",
        "cool" to "حلو",
        "god willing" to "إن شاء الله",
        "praise god" to "الحمد لله",
        "glory to god" to "سبحان الله"
    )

    private val arToEn: Map<String, String> by lazy {
        val reversed = HashMap<String, String>(enToAr.size * 2)
        enToAr.forEach { (en, ar) ->
            reversed.putIfAbsent(normalize(ar), en)
        }
        reversed["مرحبا"] = "hello"
        reversed["اهلا"] = "hi"
        reversed["شكرا"] = "thanks"
        reversed["عفوا"] = "you are welcome"
        reversed["كيفك"] = "how are you"
        reversed["شخبارك"] = "how are you"
        reversed["تمام"] = "fine / okay"
        reversed["حبيبي"] = "my love / bro"
        reversed["يعطيك العافية"] = "may God give you wellness"
        reversed["ان شاء الله"] = "god willing"
        reversed["الحمد لله"] = "praise god"
        reversed["والله"] = "honestly / by God"
        reversed["يلا"] = "let's go"
        reversed["يلا باي"] = "bye"
        reversed
    }

    private fun normalize(s: String): String = s.trim().lowercase()
        .replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")

    fun translate(input: String, fromEnglish: Boolean): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (fromEnglish) {
            val direct = enToAr[trimmed.lowercase()]
            if (direct != null) return direct

            // Word-by-word fallback
            val words = trimmed.split(' ')
            val translated = words.map { w ->
                val clean = w.lowercase().filter { it.isLetter() }
                enToAr[clean] ?: w
            }
            return translated.joinToString(" ")
        } else {
            val norm = normalize(trimmed)
            val direct = arToEn[norm] ?: arToEn[trimmed]
            if (direct != null) return direct

            val words = trimmed.split(' ')
            val translated = words.map { w ->
                val n = normalize(w)
                arToEn[n] ?: w
            }
            return translated.joinToString(" ")
        }
    }
}
