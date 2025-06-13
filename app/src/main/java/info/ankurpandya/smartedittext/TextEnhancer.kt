package info.ankurpandya.smartedittext

import android.content.Context
import android.util.Log
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class TextEnhancer {

    private val IS_OFFLINE = false

    fun init(context: Context) {
        if (IS_OFFLINE) {

        }
    }

    fun enhanceText(originalText: String, onEnhanced: (String) -> Unit) {
        if (IS_OFFLINE) {
            enhanceTextOffline(originalText, onEnhanced)
        } else {
            enhanceTextOnline(originalText, onEnhanced)
        }
    }

    private fun enhanceTextOffline(originalText: String, onEnhanced: (String) -> Unit) {
        onEnhanced.invoke(originalText)
    }

    private fun enhanceTextOnline(originalText: String, onEnhanced: (String) -> Unit) {
        val PROMPT =
            "Rephrase the following message in a professional tone. Return only the rephrased message without explanation:\\n\\n$originalText"
        val requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"$PROMPT\"}]}]}".trimIndent()
        val URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${BuildConfig.API_KEY}"

        val request = Request.Builder()
            .url(URL)
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("GeminiAI", "API failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("GeminiAI", "Raw Response: $responseBody")

                try {
                    val enhancedText = Regex("\"text\"\\s*:\\s*\"(.*?)\"")
                        .find(responseBody ?: "")?.groupValues?.get(1)?.replace("\\n", "\n")
                    enhancedText?.let { onEnhanced(it) }
                } catch (e: Exception) {
                    onEnhanced.invoke(originalText)
                    Log.e("GeminiAI", "Parsing failed: ${e.message}")
                }
            }
        })
    }
}