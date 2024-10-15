import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.utils.row
import kotliquery.queryOf
import kotliquery.sessionOf

val state: MutableMap<ChatId, WordType> = mutableMapOf()
suspend fun main() {
    AppConfig.init("PartsOfSpeachTrainer")
    longPolling {
        onCommand("start") {
            val word = getRandomWord()
            state[it.from!!.id] = word.second
            send(it.chat, text = "${word.first}?", replyMarkup = keyboard)
        }
        onText {
            val text = it.text!!
            if (text == "/start") return@onText

            if (text == (state[it.from!!.id]?.description ?: "")) {
                sendTextMessage(it.chat, "Правильно")
                val word = getRandomWord()
                state[it.from!!.id] = word.second
                send(it.chat, text = "${word.first}?", replyMarkup = keyboard)
            } else {
                sendTextMessage(it.chat, "Неправильно")
            }
        }
    }.second.join()
}

val keyboard = replyKeyboard {
    WordType.entries
        .forEach { type -> row { simpleButton(type.description) }  }
}

val session = sessionOf(System.getenv("POSTGRES_URL"),
    System.getenv("POSTGRES_USERNAME"),
    System.getenv("POSTGRES_PASSWORD"))
fun getRandomWord(): Pair<String, WordType> = session.run(
    queryOf("""
            SELECT word, type
            FROM words
            OFFSET floor(random() * 4159394)
            LIMIT 1;
        """).map { row -> Pair(row.string("word"), WordType.fromDescription(row.string("type"))) }.asSingle
)!!