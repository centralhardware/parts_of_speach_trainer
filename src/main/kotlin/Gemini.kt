import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable data class ResponseDTO(val candidates: List<Candidate>)

@Serializable data class Candidate(val content: Content)

@Serializable data class Content(val parts: List<Part>)

@Serializable data class Part(val text: String)

object Gemini {

    val json = Json { ignoreUnknownKeys = true }

    fun sendPromptToGeminiAI(prompt: String): String {
        val client = OkHttpClient()

        // URL для обращения к API (включает API-ключ)
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.0-pro:generateContent?key=${System.getenv("GEMINI_TOKEN")}"

        val requestBody =
            """
        {
          "contents": [{
            "parts":[{
              "text": "$prompt",
            }]
          }]
        }
        """

        val request =
            Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.body?.string() == null)
            throw IOException("Unexpected code $response")
        val responseDTO = json.decodeFromString<ResponseDTO>(response.body?.string()!!)

        return responseDTO.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text!!
    }

    fun isValid(word: String): Result<Pair<Boolean, IgnoreReason?>> =
        runCatching {
                val res =
                    sendPromptToGeminiAI(
                            "Необходимо определить, является ли слово профессионализмом, архаизмом (включая устаревшие формы слова) или матом. Ответь одним из вариантов: архаизм | профессионализм | мат. Если слово не относится ни к одной категории, отвечай словом '-'. Имена собственные всегда записывай в профессионализм. Слово: $word"
                        )
                        .replace("\n", "")
                        .replace(" ", "")
                KSLog.info("gemini >> $word $res")

                return@runCatching when {
                    res.equals("архаизм", ignoreCase = true) -> Pair(true, IgnoreReason.ARCHAISM)
                    res.equals("профессионализм", ignoreCase = true) ->
                        Pair(true, IgnoreReason.PROFESSIONALISM)
                    res.equals("мат", ignoreCase = true) -> Pair(true, IgnoreReason.SWEAR)
                    else -> Pair(false, null)
                }
            }
            .onFailure { KSLog.error(it) }
}
