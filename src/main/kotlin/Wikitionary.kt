import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Wikitionary {
    fun validateWord(word: String): Boolean {
        return runCatching {
            val url = "https://ru.wiktionary.org/wiki/$word"

            // Создаем HTTP клиент
            val client = OkHttpClient()

            // Создаем запрос
            val request = Request.Builder().url(url).build()

            // Отправляем запрос и получаем ответ
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Ошибка запроса: ${response.code}")

                // Парсим HTML ответа
                val html = response.body?.string() ?: throw Exception("Пустой ответ")
                val doc: Document = Jsoup.parse(html)

                // Находим элемент с определением слова
                val definitionElement = doc.select("ol > li").firstOrNull()

                // Возвращаем текст определения
                val res = definitionElement?.text()
                    ?.replace("Отсутствует пример употребления (см. рекомендации)", "")?: ""
                return !(res.contains("устар") && res.contains("мед."))
            }
        }.getOrNull()?: false
    }
}