import dev.inmo.tgbotapi.types.chat.User
import korlibs.time.milliseconds
import korlibs.time.minutes

object Statistic {

    val cache = TimedList<Long, String, Boolean>(5.minutes.milliseconds.toLong())

    fun add(user: User, word: String, correct: Boolean) {
        cache.add(user.rowId(), word, correct)
    }

    fun getStatistic(user: User): Pair<Int, Int> {
        val stat = cache.getElementsByKey1(user.rowId()).groupBy{ it.first }.mapValues { entry ->
            entry.value.any{ !it.second }
        }
        val correct = stat.count{ !it.value }
        val incorrect = stat.count{ it.value }
        return Pair(correct, incorrect)
    }

}