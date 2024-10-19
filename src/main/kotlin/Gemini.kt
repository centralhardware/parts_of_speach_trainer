import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

@Serializable
data class ResponseDTO(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<SafetyRating>
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int?,
    val totalTokenCount: Int
)

object Gemini {

    fun sendPromptToGeminiAI(prompt: String): String {
        val client = OkHttpClient()

        // URL для обращения к API (включает API-ключ)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=${System.getenv("GEMINI_TOKEN")}"

        // Тело запроса с промтом
        val requestBody = """
        {
          "contents": [{
            "parts":[{
              "text": "$prompt",
            }]
          }]
        }
    """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.body?.string() == null) throw IOException("Unexpected code $response")
        val responseDTO = Json.decodeFromString<ResponseDTO>(response.body?.string()!!)

        return responseDTO.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text!!
    }


    fun validatedWord(word: String): Boolean {
        val res = sendPromptToGeminiAI("Необходимо определить является ли слово профессионализм или архаизмом или матом. Отвечай одним словом. Отвечай - когда слово не подходит под эти категории. Отвечай только когда сто процентно уверен. Всегда отвечай вариантами: архаизм|профессионализм|мат Слово: $word")
            .replace("\n", "").replace(" ", "")
        KSLog.info("gemin answer for word $word $res")
        return !res.equals("архаизм", ignoreCase = true) &&
            !res.equals("профессионализм", ignoreCase = true) &&
            !res.equals("мат", ignoreCase = true)
    }

}