package info.ankurpandya.smartedittext

import CustomEditTextWithActions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.ankurpandya.smartedittext.ui.theme.SmartEditTextTheme


class MainActivity : ComponentActivity() {
    private val textEnhancer = TextEnhancer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textEnhancer.init(this)
        enableEdgeToEdge()
        setContent {
            SmartEditTextTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        enhancer = textEnhancer
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    enhancer: TextEnhancer? = null
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column {
        CustomEditTextWithActions(
            value = text,
            onValueChange = { text = it },
            maxChar = 100,
            maxLines = 3,
            inputType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            placeholder = "Type or speak here",
            onMicClick = {
                SpeechUtils.startSpeechRecognition(context) { spokenText ->
                    text = spokenText
                }
            },
            onAIEnhanceClick = {
                enhancer?.let {
                    it.enhanceText(text){ enhanced ->
                        text = enhanced
                    }
                }
            }
        )

        Text(
            text = text,
            modifier = modifier.padding(top = 16.dp)
        )
    }
}




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartEditTextTheme {
        Greeting("Android")
    }
}