package androidx.compose.mpp.demo.textfield

import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun FastDelete() {
    val textState = remember {
        mutableStateOf(
            """
                Place carret at the end of this TextField. Press and hold delete previous button.
                Where are 21 digits at the end. After sequentially removing 21 symbols, iOS starts to remove bigger token.
                For example 2 words simultaneously.
                And some emoji 👨‍👩‍👧‍👦 👨 👩 👧 👦.
                Check    a    lot    of     spaces     in    once      sentence          here.
                Aaa aaa bbb bbb ccc ccc ddd ddd eee eee fff fff 012345678901234567890
            """.trimIndent()
        )
    }
    TextField(textState.value, { textState.value = it })
}
