package info.ankurpandya.smartedittext

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class TextEnhancer {

    private val IS_OFFLINE = false

    private lateinit var interpreter: Interpreter
    private lateinit var tokenizer: HuggingFaceTokenizer
    private var startToken: Long = 0
    private var endToken: Long = 0
    private var isInitialized = false

    companion object {
        private const val MODEL_FILE = "t5-small-dailycnn.tflite"
        private const val TOKENIZER_FILE = "tokenizer.json"
        private const val MAX_OUTPUT_TOKENS = 32
        private const val VOCAB_SIZE = 32128
    }

    fun init(context: Context) {
        if (IS_OFFLINE) {
            try {
                val descriptor = context.assets.openFd(MODEL_FILE)
                FileInputStream(descriptor.fileDescriptor).use { stream ->
                    val mapped: MappedByteBuffer = stream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        descriptor.startOffset,
                        descriptor.declaredLength
                    )
                    val options = Interpreter.Options().apply {
                        addDelegate(FlexDelegate())
                    }
                    interpreter = Interpreter(mapped, options)
                    context.assets.open(TOKENIZER_FILE).use { stream ->
                        tokenizer = HuggingFaceTokenizer.newInstance(stream, emptyMap<String, Any>())
                    }
                    startToken = tokenizer.encode("<pad>").ids.first().toLong()
                    endToken = tokenizer.encode("</s>").ids.first().toLong()
                    isInitialized = true
                }
            } catch (e: Exception) {
                isInitialized = false
                Log.e("TextEnhancer", "Offline init failed: ${e.message}")
            }
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
        if (!isInitialized) {
            onEnhanced.invoke(originalText)
            return
        }

        try {
            val encoding = tokenizer.encode(originalText)
            val inputIds = encoding.ids
            val attention = LongArray(inputIds.size) { 1L }
            val decoded = mutableListOf(startToken)

            repeat(MAX_OUTPUT_TOKENS) {
                val decArr = decoded.toLongArray()
                val decMask = LongArray(decArr.size) { 1L }
                val outputs = HashMap<String, Any>()
                outputs["logits"] = Array(1) { Array(decArr.size) { FloatArray(VOCAB_SIZE) } }

                interpreter.runSignature(
                    mapOf(
                        "attention_mask" to arrayOf(attention),
                        "decoder_attention_mask" to arrayOf(decMask),
                        "decoder_input_ids" to arrayOf(decArr),
                        "input_ids" to arrayOf(inputIds)
                    ),
                    outputs,
                    "int64_serving"
                )

                val logits = (outputs["logits"] as Array<Array<FloatArray>>)[0][decArr.size - 1]
                var next = 0
                var max = Float.NEGATIVE_INFINITY
                for (i in logits.indices) {
                    if (logits[i] > max) {
                        max = logits[i]
                        next = i
                    }
                }
                if (next.toLong() == endToken) return onEnhanced(tokenizer.decode(decoded.drop(1).toLongArray()))
                decoded.add(next.toLong())
            }
            val result = tokenizer.decode(decoded.drop(1).toLongArray())
            onEnhanced.invoke(result)
        } catch (e: Exception) {
            Log.e("TextEnhancer", "Offline inference failed: ${e.message}")
            onEnhanced.invoke(originalText)
        }
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