package androidx.compose.mpp.demo.textfield

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EmojiExample() {
    Column {
        TextFieldExample(
            "Compound Emoji",
            mutableStateOf("Family emoji: \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66, and some text at the end")
        )

        TextFieldExample(
            "Compound Emoji",
            mutableStateOf("Split family emoji: \uD83D\uDC68 \uD83D\uDC69 \uD83D\uDC67 \uD83D\uDC66, and some text at the end")
        )

    }

}

@Composable
private fun TextFieldExample(title: String, state: MutableState<String>) {
    Column(Modifier.fillMaxWidth().padding(4.dp).border(1.dp, Color.Black).padding(4.dp)) {
        Text(title)
        TextField(
            value = state.value,
            onValueChange = {
                state.value = it
            },
            keyboardOptions = KeyboardOptions(autoCorrect = false),
        )
    }
}
