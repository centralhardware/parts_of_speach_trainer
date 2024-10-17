enum class WordType(val code: String, val fullName: String) {
    VERB("verb", "глагол"),
    PARTICIPLE_ADJECTIVE("participleAdjective", "причастие"),
    INTERJECTION("interjection", "междометие"),
    PRONOUN("pronoun", "местоимение"),
    ADVERB("adverb", "наречие"),
    PREPOSITION("preposition", "предлог"),
    ADJECTIVE("adjective", "прилагательное"),
    PARTICIPLE("participle", "деепричастие"),
    CONJUNCTION("conjunction", "союз"),
    NOUN("noun", "существительное"),
    PARTICLE("particle", "частица"),
    NUMERAL("number", "числительное");

    companion object {
        fun fromCode(code: String): WordType {
            return values().find { it.code == code }?: throw IllegalArgumentException("$code not found")
        }

        fun fromFullName(fullName: String): WordType {
            return values().find { it.fullName == fullName }?: throw IllegalArgumentException("$fullName not found")
        }
    }
}
