enum class WordType(val code: String, val fullName: String) {
    VERB("гл", "глагол"),
    GERUND("дееп", "деепричастие"),
    INTERJECTION("межд", "междометие"),
    PRONOUN("мест", "местоимение"),
    ADVERB("нар", "наречие"),
    ADVERB_PRONOUN("нар,мест", "местоимение наречие"),
    PREDICATE("предик", "предикатив"),
    PREPOSITION("предл", "предлог"),
    ADJECTIVE("прл", "прилагательное"),
    ADJECTIVE_PRONOUN("прл,мест", "прилагательное местоимение"),
    PARTICIPLE("прч", "причастие"),
    CONJUNCTION("союз", "союз"),
    NOUN("сущ", "существительное"),
    NOUN_PRONOUN("сущ,мест", "существительное местоимение"),
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
