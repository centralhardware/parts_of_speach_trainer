import com.github.michaelbull.retry.policy.stopAtRetries
import com.github.michaelbull.retry.retry
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
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
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row
import kotliquery.queryOf
import kotliquery.sessionOf

val state: MutableMap<ChatId, WordType> = mutableMapOf()
val difficults: MutableMap<ChatId, String> = mutableMapOf()
fun getDifficult(chat: User?) = difficults[chat!!.id]?: "medium"

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
            difficults[it.from!!.id] = "easy"
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommand("medium") {
            difficults[it.from!!.id] = "medium"
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onCommand("hard") {
            difficults[it.from!!.id] = "hard"
            sendTextMessage(it.chat, "Установлено")
            sendWord(this, it.from)
        }
        onText {
            val text = it.text!!
            if (text.startsWith("/")) return@onText

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
    val difficult = getDifficult(chat)
    val word = retry(stopAtRetries(4)) {
        getRandomWord(difficult)
    }
    state[chat!!.id] = word.second
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
    row { simpleButton(WordType.PARTICIPLE.fullName); simpleButton(WordType.GERUND.fullName) }
}
val keyboards = mapOf(
    "easy" to easy,
    "medium" to media,
    "hard" to hard,
)

val session = sessionOf(System.getenv("POSTGRES_URL"),
    System.getenv("POSTGRES_USERNAME"),
    System.getenv("POSTGRES_PASSWORD"))
fun getRandomWord(difficult: String): Pair<String, WordType> = session.run(
    queryOf("""
            WITH RandomType AS (
                SELECT DISTINCT type
                FROM words
                WHERE type NOT IN ('предик', 'ввод', 'нар,мест', 'прл,мест', 'сущ,мест') 
                GROUP BY type
                ORDER BY RANDOM()
                LIMIT 1
            )
            SELECT word, max(type) as type
            FROM words
            WHERE type = (SELECT type FROM RandomType) 
                AND code_parent = 0
                AND (
                CASE 
                        WHEN :difficult = 'easy' THEN type IN ('сущ', 'прл', 'гл')
                        WHEN :difficult = 'medium' THEN type IN ('сущ', 'прл', 'гл', 'нар', 'числ', 'мест', 'союз', 'предл', 'част', 'межд')
                        WHEN :difficult = 'hard' THEN type IN ('сущ', 'прл', 'гл', 'нар', 'числ', 'мест', 'союз', 'предл', 'част', 'межд', 'дееп', 'прч')
                    END
                )
            GROUP BY word
            HAVING count(distinct type) = 1
            ORDER BY RANDOM()
            LIMIT 1;
        """, mapOf("difficult" to difficult))
        .map { row -> Pair(row.string("word"), WordType.fromCode(row.string("type"))) }.asSingle
)!!