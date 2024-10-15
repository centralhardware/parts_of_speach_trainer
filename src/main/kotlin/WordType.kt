enum class WordType(val description: String) {
    VERB("гл"),
    GERUND("дееп"),
    INTERJECTION("межд"),
    PRONOUN("мест"),
    ADVERB("нар"),
    ADVERB_PRONOUN("нар,мест"),
    PREDICATE("предик"),
    PREPOSITION("предл"),
    ADJECTIVE("прл"),
    ADJECTIVE_PRONOUN("прл,мест"),
    PARTICIPLE("прч"),
    CONJUNCTION("союз"),
    NOUN("сущ"),
    NOUN_PRONOUN("сущ,мест"),
    PARTICLE("част"),
    NUMERAL("числ");

    companion object {
        fun fromDescription(description: String): WordType {
            return values().find { it.description == description }!!
        }
    }
}