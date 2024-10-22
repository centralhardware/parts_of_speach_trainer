import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.Chat

fun Chat.rawId() = id.chatId.long
fun ChatId.rawId() = chatId.long