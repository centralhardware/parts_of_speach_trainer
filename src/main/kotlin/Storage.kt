import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.User
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient

object Storage {

    val redisClient = newClient(Endpoint.from(System.getenv("REDIS_URL")))

    suspend fun setDifficult(chatId: ChatId, difficult: Difficult) =
        redisClient.hset(chatId.rawId().toString(), Pair("difficult", difficult.name))
    suspend fun getDifficult(chatId: ChatId): Difficult =
        redisClient.hget(chatId.rawId().toString(), "difficult")?.let { Difficult.valueOf(it) }
            ?: run {
                setDifficult(chatId, Difficult.MEDIUM)
                return@run Difficult.MEDIUM
            }

    suspend fun setNext(chatId: ChatId, next: Pair<String, WordType>) {
        redisClient.hset(chatId.rawId().toString(), Pair("type", next.second.name))
        redisClient.hset(chatId.rawId().toString(), Pair("word", next.first))
    }
    suspend fun getNext(chatId: ChatId) =
        Pair(
            redisClient.hget(chatId.rawId().toString(), "word")!!,
            redisClient.hget(chatId.rawId().toString(), "type")?.let { WordType.valueOf(it) }!!
        )
    suspend fun clearNext(chatId: ChatId) {
        redisClient.hdel(chatId.rawId().toString(), "word")
        redisClient.hdel(chatId.rawId().toString(), "type")
    }

    suspend fun appendCorrect(user: User, word: String) =
        redisClient.lpush("${user.rawId()}_correct", word)
    suspend fun correctSize(user: User) = redisClient.llen("${user.rawId()}_correct").toInt()
    suspend fun clearCorrect(user: User) = redisClient.del("${user.rawId()}_correct")
}
