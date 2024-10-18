import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient

object Storage {

    val redisClient = newClient(Endpoint.from(System.getenv("REDIS_URL")))

    suspend fun setDifficult(chatId: Long, difficult: Difficult) =
        redisClient.hset(chatId.toString(), Pair("difficult", difficult.name))
    suspend fun getDifficult(chatId: Long): Difficult =
        redisClient.hget(chatId.toString(), "difficult")?.let{ Difficult.valueOf(it) }?: run {
            setDifficult(chatId, Difficult.MEDIUM)
            return@run Difficult.MEDIUM
        }


    suspend fun setType(chatId: Long, type: WordType) =
        redisClient.hset(chatId.toString(), Pair("type", type.name))
    suspend fun getType(chatId: Long): WordType? =
        redisClient.hget(chatId.toString(), "type")?.let{ WordType.valueOf(it) }


}