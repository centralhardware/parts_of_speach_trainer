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
import dev.inmo.tgbotapi.utils.row
import kotliquery.queryOf
import kotliquery.sessionOf

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
            Storage.setDifficult(it.from!!.id.chatId.long, Dificult.EASY)
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommand("medium") {
            Storage.setDifficult(it.from!!.id.chatId.long, Dificult.MEDIUM)
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommand("hard") {
            Storage.setDifficult(it.from!!.id.chatId.long, Dificult.HARD)
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommandWithArgs("ignore") { msg, args ->
            if (args.size == 1) {
                addToIgnore(args[0])
                sendTextMessage(msg.chat, "Сохранено")
            } else {
                sendTextMessage(msg.chat, "Неверный формат")
            }
        }
        onText(initialFilter = CommonMessageFilterExcludeCommand()) {
            val text = it.text!!

            if (WordType.fromFullName(text) == (Storage.getType(it.from!!.id.chatId.long))) {
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
    val difficult = Storage.getDifficult(chat.id.chatId.long)
    val word = getRandomWord(difficult)
    Storage.setType(chat!!.id.chatId.long, word.second)
    bot.send(chat, text = "${word.first}?", replyMarkup = keyboards[difficult])
    KSLog.info("${chat!!.id.chatId.long} ${word.first} ${word.second.fullName} $difficult")
}

val easy = replyKeyboard {
    row { simpleButton(WordType.NOUN.fullName); simpleButton(WordType.ADJECTIVE.fullName) }
    row { simpleButton(WordType.VERB.fullName) }
}
val media = replyKeyboard {
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
    Dificult.EASY to easy,
    Dificult.MEDIUM to media,
    Dificult.HARD to hard,
)

val session = sessionOf(System.getenv("POSTGRES_URL"),
    System.getenv("POSTGRES_USERNAME"),
    System.getenv("POSTGRES_PASSWORD"))
fun getRandomWord(difficult: Dificult): Pair<String, WordType> = session.run(
    queryOf("""
            WITH RandomType AS (
                SELECT part_of_speech as type
                FROM entry
                WHERE (CASE 
                        WHEN :difficult = 'EASY' THEN part_of_speech IN ('noun', 'adjective', 'verb')
                        WHEN :difficult = 'MEDIUM' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection')
                        WHEN :difficult = 'HARd' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection', 'participle', 'participleAdjective')
                END)
                GROUP BY part_of_speech
                ORDER BY RANDOM()
                LIMIT 1
            )
            SELECT written_rep, part_of_speech
            FROM entry
            WHERE part_of_speech = (SELECT type FROM RandomType) 
                AND (
                CASE 
                        WHEN :difficult = 'EASY' THEN part_of_speech IN ('noun', 'adjective', 'verb')
                        WHEN :difficult = 'MEDIUM' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection')
                        WHEN :difficult = 'HARD' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection', 'participle', 'participleAdjective')
                END
                )
                AND written_rep NOT IN (SELECT word FROM ignore_words)
            ORDER BY RANDOM()
            LIMIT 1;
        """, mapOf("difficult" to difficult.name))
        .map { row -> Pair(row.string("written_rep"), WordType.fromCode(row.string("part_of_speech"))) }.asSingle
)!!

fun addToIgnore(word: String) = session.execute(queryOf(
    """
        INSERT INTO ignore_words (word) VALUES (:word)
    """, mapOf("word" to word)
))