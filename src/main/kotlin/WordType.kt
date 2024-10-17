enum class WordType(val code: String, val fullName: String) {
    VERB("гл", "глагол"),
    GERUND("дееп", "деепричастие"),
    INTERJECTION("межд", "междометие"),
    PRONOUN("мест", "местоимение"),
    ADVERB("нар", "наречие"),
    PREPOSITION("предл", "предлог"),
    ADJECTIVE("прл", "прилагательное"),
    PARTICIPLE("прч", "причастие"),
    CONJUNCTION("союз", "союз"),
    NOUN("сущ", "существительное"),
    PARTICLE("част", "частица"),
    NUMERAL("числ", "числительное");

    companion object {
        fun fromCode(code: String): WordType {
            return values().find { it.code == code }?: throw IllegalArgumentException("$code not found")
        }

        fun fromFullName(fullName: String): WordType {
            return values().find { it.fullName == fullName }?: throw IllegalArgumentException("$fullName not found")
        }
    }
}
