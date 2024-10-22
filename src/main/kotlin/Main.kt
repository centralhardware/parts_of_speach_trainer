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
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.row

@OptIn(RiskFeature::class)
suspend fun main() {
    AppConfig.init("PartsOfSpeachTrainer")
    longPolling {
        setMyCommands(
            BotCommand("stat", "Вывести статистику сессии"),
            BotCommand("easy", "Легкая сложность"),
            BotCommand("medium", "Средний уровень сложности"),
            BotCommand("hard", "Тяжелая сложность"),
            BotCommand("ignore", "добавить слово в игнор")
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
                WordMapper.markWord(args[0], WordStatus.IGNORE, IgnoreReason.BLOCKED_BY_ADMIN)
                sendTextMessage(msg.chat, "Сохранено")
                sendWord(this, msg.from)
            } else {
                sendTextMessage(msg.chat, "Неверный формат", replyMarkup = keyboards[Storage.getDifficult(msg.from!!)])
            }
        }
        onCommand("stat") {
            sendStatistic(this, it.from?.id!!)
        }
        onText(initialFilter = CommonMessageFilterExcludeCommand()) {
            val text = it.text!!

            val next = Storage.getNext(it.from!!)
            val correct = WordType.fromFullName(text) == next.second
            Statistic.add(it.from!!, next.first, correct)
            if (correct) {
                Storage.appendCorrect(it.from!!, next.first)
                sendTextMessage(it.chat, "Правильно")
                val size = Storage.correctSize(it.from!!)
                if (Achievement.isAchievement(size)) {
                    sendTextMessage(it.chat, "Серия из $size слов")
                }
                sendWord(this, it.from)
            } else {
                sendTextMessage(it.chat, "Неправильно")
                Storage.clearCorrect(it.from!!)
            }
        }
        Statistic.addListener { id ->
            sendStatistic(this, id)
        }
    }.second.join()
}

suspend fun sendWord(bot: TelegramBot, chat: User?) {
    bot.sendActionTyping(chat!!)
    val difficult = Storage.getDifficult(chat)
    var next: Pair<String, WordType>? = WordMapper.getRandomWord(difficult)
    if (WordMapper.isNotValid(next!!.first)) {
        var isIgnore = Gemini.isValid(next.first)
        while (isIgnore.getOrNull()?.first == true) {
            isIgnore.onSuccess {
                WordMapper.markWord(next!!.first, WordStatus.IGNORE, isIgnore.getOrThrow().second!!)
                KSLog.info("mark word ${next.first} as ignored")
            }

            next = WordMapper.getRandomWord(difficult)
            isIgnore = Gemini.isValid(next.first)
        }
    }
    WordMapper.markWord(next.first, WordStatus.APPROVED)
    KSLog.info("mark word ${next.first} as approved")

    Storage.setNext(chat, next)
    val word = next.first
    bot.send(chat, text = word, replyMarkup = keyboards[difficult])
    KSLog.info("${chat.id.chatId.long} $word ${next.second.fullName} $difficult")
}

suspend fun sendStatistic(bot: TelegramBot, chat: ChatId) {
    val stat = Statistic.getStatistic(chat)
    val percent = if (stat.second != 0) {
        (stat.first / stat.second * 100).coerceAtMost(100)
    } else {
        0
    }
    bot.sendTextMessage(chat, """
               правильно: ${stat.first}
               неправильно: ${stat.second}
               процент правильных: $percent%
            """.trimIndent())
}

val easy = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName); simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName) }
}
val medium = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName);         simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName);         simpleButton(WordType.ADVERB.fullName) }
    row { simpleButton(WordType.PRONOUN.fullName);      simpleButton(WordType.CONJUNCTION.fullName) }
    row { simpleButton(WordType.PREPOSITION.fullName);  simpleButton(WordType.PARTICLE.fullName) }
    row { simpleButton(WordType.INTERJECTION.fullName) }
}
val hard = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName);         simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName);         simpleButton(WordType.ADVERB.fullName) }
    row { simpleButton(WordType.PRONOUN.fullName);      simpleButton(WordType.CONJUNCTION.fullName) }
    row { simpleButton(WordType.PREPOSITION.fullName);  simpleButton(WordType.PARTICLE.fullName) }
    row { simpleButton(WordType.PARTICIPLE.fullName);   simpleButton(WordType.INTERJECTION.fullName) }
    row { simpleButton(WordType.PARTICIPLE_ADJECTIVE.fullName) }
}

val keyboards = mapOf(
    Difficult.EASY to easy,
    Difficult.MEDIUM to medium,
    Difficult.HARD to hard,
)