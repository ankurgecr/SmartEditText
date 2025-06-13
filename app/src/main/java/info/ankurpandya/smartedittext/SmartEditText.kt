import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

@Composable
fun CustomEditTextWithActions(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    inputType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    maxChar: Int = Int.MAX_VALUE,
    maxLines: Int = 1,
    placeholder: String = "",
    onMicClick: () -> Unit,
    onAIEnhanceClick: () -> Unit
) {
    val visualTransformation =
        if (inputType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None

    OutlinedTextField(
        value = value,
        onValueChange = {
            if (it.length <= maxChar) onValueChange(it)
        },
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = inputType,
            imeAction = imeAction
        ),
        visualTransformation = visualTransformation,
        maxLines = maxLines,
        trailingIcon = {
            Row {
                IconButton(onClick = onMicClick) {
                    Icon(imageVector = Icons.Rounded.Call, contentDescription = "Mic")
                }
                IconButton(onClick = onAIEnhanceClick) {
                    Icon(imageVector = Icons.Rounded.Face, contentDescription = "AI Enhance")
                }
            }
        },
        singleLine = maxLines == 1
    )
}
