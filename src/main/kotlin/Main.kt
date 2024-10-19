import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendActionTyping
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterExcludeCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.row

@OptIn(RiskFeature::class)
suspend fun main() {
    AppConfig.init("PartsOfSpeachTrainer")
    longPolling {
        setMyCommands(
            BotCommand("start", "Start testing"),
            BotCommand("easy", "Установить легкий уровень легкости"),
            BotCommand("medium", "Установить средний уровень легкости"),
            BotCommand("hard", "Установить сложный уровень легкости")
        )
        onCommand("start") {
            sendWord(this, it.from)
        }
        onCommand("easy") {
            Storage.setDifficult(it.from!!, Difficult.EASY)
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommand("medium") {
            Storage.setDifficult(it.from!!, Difficult.MEDIUM)
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommand("hard") {
            Storage.setDifficult(it.from!!, Difficult.HARD)
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommandWithArgs("ignore") { msg, args ->
            if (args.size == 1) {
                WordMapper.markWord(args[0], WordStatus.IGNORE)
                sendTextMessage(msg.chat, "Сохранено")
            } else {
                sendTextMessage(msg.chat, "Неверный формат")
            }
        }
        onText(initialFilter = CommonMessageFilterExcludeCommand()) {
            val text = it.text!!

            if (WordType.fromFullName(text) == (Storage.getType(it.from!!))) {
                sendTextMessage(it.chat, "Правильно")
                sendWord(this, it.from)
            } else {
                sendTextMessage(it.chat, "Неправильно")
            }
        }
    }.second.join()
}

suspend fun sendWord(bot: TelegramBot, chat: User?) {
    bot.sendActionTyping(chat!!)
    val difficult = Storage.getDifficult(chat)
    var next: Pair<String, WordType>? = WordMapper.getRandomWord(difficult)
    if (WordMapper.isNotValid(next!!.first)) {
        var isIgnore = Gemini.isValid(next.first)
        while (isIgnore.getOrElse{ true}) {
            if (isIgnore.isSuccess) {
                WordMapper.markWord(next!!.first, WordStatus.IGNORE)
                KSLog.info("mark word ${next.first} as ignored")
            }

            next = WordMapper.getRandomWord(difficult)
            isIgnore = Gemini.isValid(next.first)
        }
    }
    WordMapper.markWord(next.first, WordStatus.APPROVED)
    KSLog.info("mark word ${next.first} as approved")

    Storage.setType(chat, next.second)
    val word = next.first
    bot.send(chat, text = word, replyMarkup = keyboards[difficult])
    KSLog.info("${chat.id.chatId.long} $word ${next.second.fullName} $difficult")
}

val easy = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName); simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName) }
}
val medium = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName); simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName); simpleButton(WordType.ADVERB.fullName) }
    row { simpleButton(WordType.NUMERAL.fullName); simpleButton(WordType.PRONOUN.fullName) }
    row { simpleButton(WordType.CONJUNCTION.fullName); simpleButton(WordType.PREPOSITION.fullName) }
    row { simpleButton(WordType.PARTICLE.fullName); simpleButton(WordType.INTERJECTION.fullName) }
}
val hard = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName); simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName); simpleButton(WordType.ADVERB.fullName) }
    row { simpleButton(WordType.NUMERAL.fullName); simpleButton(WordType.PRONOUN.fullName) }
    row { simpleButton(WordType.CONJUNCTION.fullName); simpleButton(WordType.PREPOSITION.fullName) }
    row { simpleButton(WordType.PARTICLE.fullName); simpleButton(WordType.INTERJECTION.fullName) }
    row { simpleButton(WordType.PARTICIPLE.fullName); simpleButton(WordType.PARTICIPLE_ADJECTIVE.fullName) }
}
val keyboards = mapOf(
    Difficult.EASY to easy,
    Difficult.MEDIUM to medium,
    Difficult.HARD to hard,
)