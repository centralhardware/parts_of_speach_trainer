import korlibs.time.milliseconds
import korlibs.time.seconds
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.LinkedList
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class TimedList<K1, K2, T>(private val expirationTime: Duration) {
    private val list = LinkedList<Pair<Pair<K1, K2>, Pair<T, Instant>>>()

    init {
        thread(start = true) {
            while (true) {
                Thread.sleep(10.seconds.toJavaDuration())
                synchronized(list) {
                    runBlocking{
                        getFirstKeys().forEach { key ->
                            dropLastRecords(key)
                        }
                    }
                }
            }
        }
    }

    val listeners = mutableListOf<suspend (id: K1) -> Unit>()
    fun addListener(listener: suspend (id: K1) -> Unit) = listeners.add(listener)

    suspend fun add(key1: K1, key2: K2, element: T) {
        dropLastRecords(key1)
        val currentTime = Instant.now()
        list.add(Pair(Pair(key1, key2), Pair(element, currentTime)))
    }

    fun getElementsByKey1(key1: K1): List<Pair<K2, T>> {
        return list
            .filter { it.first.first == key1 }
            .map { Pair(it.first.second, it.second.first) }
    }

    fun getFirstKeys(): Set<K1> {
        val res = mutableSetOf<K1>()
        list.forEach { res.add(it.first.first) }
        return res.toSet()
    }

    suspend fun dropLastRecords(key: K1) {
        val elem = list.filter{ it.first.first == key }
        if (elem.isNotEmpty() &&
            (Instant.now().toEpochMilli() - elem.last().second.second.toEpochMilli() > expirationTime.milliseconds)) {
            listeners.forEach { it.invoke(key) }
            list.removeIf{
                it.first.first == key
            }
        }
    }

}