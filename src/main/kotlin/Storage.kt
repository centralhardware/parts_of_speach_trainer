import dev.inmo.tgbotapi.types.chat.User
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient

object Storage {

    val redisClient = newClient(Endpoint.from(System.getenv("REDIS_URL")))

    suspend fun setDifficult(user: User, difficult: Difficult) =
        redisClient.hset(user.rowId().toString(), Pair("difficult", difficult.name))
    suspend fun getDifficult(user: User): Difficult =
        redisClient.hget(user.rowId().toString(), "difficult")?.let{ Difficult.valueOf(it) }?: run {
            setDifficult(user, Difficult.MEDIUM)
            return@run Difficult.MEDIUM
        }


    suspend fun setType(user: User, type: WordType) =
        redisClient.hset(user.rowId().toString(), Pair("type", type.name))
    suspend fun getType(user: User): WordType? =
        redisClient.hget(user.rowId().toString(), "type")?.let{ WordType.valueOf(it) }


}