import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.toChatId
import korlibs.time.minutes

object Statistic {

    val cache = TimedList<Long, String, Boolean>(5.minutes)

    suspend fun add(user: User, word: String, correct: Boolean) {
        cache.add(user.rawId(), word, correct)
    }

    fun getStatistic(user: ChatId): Pair<Int, Int> {
        val stat =
            cache
                .getElementsByFirst(user.rawId())
                .groupBy { it.first }
                .mapValues { entry -> entry.value.any { !it.second } }
        val correct = stat.count { !it.value }
        val incorrect = stat.count { it.value }
        return Pair(correct, incorrect)
    }

    fun addListener(listener: suspend (id: ChatId) -> Unit) {
        cache.addListener { id -> listener.invoke(id.toChatId()) }
    }
}
