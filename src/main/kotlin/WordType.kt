enum class WordType(val fullName: String) {
    VERB("глагол"),
    PARTICIPLE_ADJECTIVE("причастие"),
    INTERJECTION("междометие"),
    PRONOUN("местоимение"),
    ADVERB("наречие"),
    PREPOSITION("предлог"),
    ADJECTIVE("прилагательное"),
    PARTICIPLE("деепричастие"),
    CONJUNCTION("союз"),
    NOUN("существительное"),
    PARTICLE("частица");

    companion object {
        fun fromFullName(fullName: String): WordType {
            return values().find { it.fullName == fullName }
                ?: throw IllegalArgumentException("$fullName not found")
        }
    }
}
