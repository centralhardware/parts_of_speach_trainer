import java.time.Instant
import java.util.LinkedList

class TimedList<K1, K2, T>(private val expirationTimeMs: Long) {
    private val list = LinkedList<Pair<Pair<K1, K2>, Pair<T, Instant>>>()

    fun add(key1: K1, key2: K2, element: T) {
        val currentTime = Instant.now()
        list.add(Pair(Pair(key1, key2), Pair(element, currentTime)))
        removeExpiredElements(currentTime)
    }

    private fun removeExpiredElements(currentTime: Instant) {
        list.removeIf { (_, value) ->
            currentTime.toEpochMilli() - value.second.toEpochMilli() > expirationTimeMs
        }
    }

    fun getElementsByKey1(key1: K1): List<Pair<K2, T>> {
        val currentTime = Instant.now()
        removeExpiredElements(currentTime)
        return list.filter { it.first.first == key1 }.map { Pair(it.first.second, it.second.first) }
    }
}