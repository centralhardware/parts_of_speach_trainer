import com.github.michaelbull.retry.policy.stopAtAttempts
import com.github.michaelbull.retry.retry
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
import dev.inmo.tgbotapi.longPolling
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.utils.RiskFeature

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
            onCommand("start") { sendWord(this, it.from!!.id) }
            onCommand("easy") { changeMode(this, it.from!!.id, Difficult.EASY) }
            onCommand("medium") { changeMode(this, it.from!!.id, Difficult.MEDIUM) }
            onCommand("hard") { changeMode(this, it.from!!.id, Difficult.HARD) }
            onCommandWithArgs("ignore") { msg, args ->
                if (args.size == 1) {
                    WordMapper.markWord(args[0], WordStatus.IGNORE, IgnoreReason.BLOCKED_BY_ADMIN)
                    sendTextMessage(msg.chat, "Сохранено")
                    sendWord(this, msg.from!!.id)
                } else {
                    sendTextMessage(
                        msg.chat,
                        "Неверный формат",
                        replyMarkup = keyboards[Storage.getDifficult(msg.from!!.id)]
                    )
                }
            }
            onCommand("stat") { sendStatistic(this, it.from?.id!!) }
            onText(initialFilter = CommonMessageFilterExcludeCommand()) {
                val text = it.text!!

                val next = Storage.getNext(it.from!!.id)
                val correct = WordType.fromFullName(text) == next.second
                Statistic.add(it.from!!, next.first, correct)
                if (correct) {
                    Storage.appendCorrect(it.from!!, next.first)
                    val size = Storage.correctSize(it.from!!)
                    if (Achievement.isAchievement(size)) {
                        sendTextMessage(it.chat, "Правильно. Серия - $size слов")
                    } else {
                        sendTextMessage(it.chat, "Правильно")
                    }
                    sendWord(this, it.from!!.id)
                } else {
                    sendTextMessage(it.chat, "Неправильно")
                    Storage.clearCorrect(it.from!!)
                }
            }
            Statistic.addListener { id -> sendStatistic(this, id) }
        }
        .second
        .join()
}

suspend fun sendWord(bot: TelegramBot, chatId: ChatId) {
    bot.sendActionTyping(chatId)
    val difficult = Storage.getDifficult(chatId)
    var next: Pair<String, WordType>? =
        retry(stopAtAttempts(4)) {
            KSLog.info("trying to get random word")
            WordMapper.getRandomWord(difficult)
        }
    if (WordMapper.isNotValid(next!!.first)) {
        var isIgnore = Gemini.isValid(next.first)
        while (isIgnore.getOrNull()?.first == true) {
            isIgnore.onSuccess {
                WordMapper.markWord(next!!.first, WordStatus.IGNORE, isIgnore.getOrThrow().second!!)
                KSLog.info("mark word ${next.first} as ignored")
            }

            next =
                retry(stopAtAttempts(4)) {
                    KSLog.info("trying to get random word")
                    WordMapper.getRandomWord(difficult)
                }
            isIgnore = Gemini.isValid(next.first)
        }

        WordMapper.markWord(next.first, WordStatus.APPROVED)
        KSLog.info("mark word ${next.first} as approved")
    } else {
        KSLog.info("already approved ${next.first}")
    }
    Storage.setNext(chatId, next)
    val word = next.first
    bot.send(
        chatId,
        text = "$word.[знач](https://ru.wiktionary.org/wiki/$word)",
        parseMode = MarkdownParseMode,
        linkPreviewOptions = LinkPreviewOptions.Disabled,
        replyMarkup = keyboards[difficult]
    )
    KSLog.info("${chatId.chatId.long} $word ${next.second.fullName} $difficult")
}

suspend fun sendStatistic(bot: TelegramBot, chat: ChatId) {
    val stat = Statistic.getStatistic(chat)
    val percent =
        if (stat.second != 0) {
            (stat.second.toDouble() / stat.first * 100).toInt().coerceAtMost(100)
        } else {
            0
        }
    bot.sendTextMessage(
        chat,
        """
               правильно: ${stat.first}
               неправильно: ${stat.second}
               процент ошибок: $percent%
            """.trimIndent(
        )
    )
}

suspend fun changeMode(bot: TelegramBot, chat: ChatId, mode: Difficult) {
    Storage.setDifficult(chat, mode)
    bot.sendTextMessage(chat, "Установлено")
    sendWord(bot, chat)
}
