import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.utils.row

val easy = replyKeyboard {
    row {
        simpleButton(WordType.NOUN.fullName)
        simpleButton(WordType.ADJECTIVE.fullName)
    }
    row { simpleButton(WordType.VERB.fullName) }
}
val medium = replyKeyboard {
    row {
        simpleButton(WordType.NOUN.fullName)
        simpleButton(WordType.ADJECTIVE.fullName)
    }
    row {
        simpleButton(WordType.VERB.fullName)
        simpleButton(WordType.ADVERB.fullName)
    }
    row {
        simpleButton(WordType.PRONOUN.fullName)
        simpleButton(WordType.CONJUNCTION.fullName)
    }
    row {
        simpleButton(WordType.PREPOSITION.fullName)
        simpleButton(WordType.PARTICLE.fullName)
    }
    row { simpleButton(WordType.INTERJECTION.fullName) }
}
val hard = replyKeyboard {
    row {
        simpleButton(WordType.NOUN.fullName)
        simpleButton(WordType.ADJECTIVE.fullName)
    }
    row {
        simpleButton(WordType.VERB.fullName)
        simpleButton(WordType.ADVERB.fullName)
    }
    row {
        simpleButton(WordType.PRONOUN.fullName)
        simpleButton(WordType.CONJUNCTION.fullName)
    }
    row {
        simpleButton(WordType.PREPOSITION.fullName)
        simpleButton(WordType.PARTICLE.fullName)
    }
    row {
        simpleButton(WordType.PARTICIPLE.fullName)
        simpleButton(WordType.INTERJECTION.fullName)
    }
    row { simpleButton(WordType.PARTICIPLE_ADJECTIVE.fullName) }
}

val keyboards = mapOf(Difficult.EASY to easy, Difficult.MEDIUM to medium, Difficult.HARD to hard)
