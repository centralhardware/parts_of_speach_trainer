import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendActionTyping
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row
import kotliquery.queryOf
import kotliquery.sessionOf

val state: MutableMap<ChatId, WordType> = mutableMapOf()
suspend fun main() {
    AppConfig.init("PartsOfSpeachTrainer")
    longPolling {
        onCommand("start") {
            sendWord(this, it.from)
        }
        onText {
            val text = it.text!!
            if (text == "/start") return@onText

            if (WordType.fromFullName(text) == (state[it.from!!.id])) {
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
    val word = getRandomWord()
    state[chat!!.id] = word.second
    bot.send(chat, text = "${word.first}?", replyMarkup = keyboard)
    KSLog.info("${chat!!.id.chatId.long} ${word.first} ${word.second.fullName}")
}

val keyboard = replyKeyboard {
    row { simpleButton(WordType.VERB.fullName); simpleButton(WordType.GERUND.fullName) }
    row { simpleButton(WordType.INTERJECTION.fullName); simpleButton(WordType.PRONOUN.fullName) }
    row { simpleButton(WordType.ADVERB.fullName); simpleButton(WordType.ADVERB_PRONOUN.fullName) }
    row { simpleButton(WordType.ADJECTIVE.fullName); simpleButton(WordType.ADJECTIVE_PRONOUN.fullName) }
    row { simpleButton(WordType.PARTICIPLE.fullName); simpleButton(WordType.CONJUNCTION.fullName) }
    row { simpleButton(WordType.NOUN.fullName); simpleButton(WordType.NOUN_PRONOUN.fullName) }
    row { simpleButton(WordType.PARTICLE.fullName); simpleButton(WordType.NUMERAL.fullName) }
    row { simpleButton(WordType.PREPOSITION.fullName) }
}

val session = sessionOf(System.getenv("POSTGRES_URL"),
    System.getenv("POSTGRES_USERNAME"),
    System.getenv("POSTGRES_PASSWORD"))
fun getRandomWord(): Pair<String, WordType> = session.run(
    queryOf("""
            WITH RandomType AS (
                SELECT type
                FROM words
                WHERE type != 'предик'
                GROUP BY type
                ORDER BY RANDOM()
                LIMIT 1
            )
            SELECT word, max(type) as type
            FROM words
            WHERE type = (SELECT type FROM RandomType) 
                AND code_parent = 0
            GROUP BY word
            HAVING count(distinct type) = 1
            ORDER BY RANDOM()
            LIMIT 1;
        """).map { row -> Pair(row.string("word"), WordType.fromCode(row.string("type"))) }.asSingle
)!!