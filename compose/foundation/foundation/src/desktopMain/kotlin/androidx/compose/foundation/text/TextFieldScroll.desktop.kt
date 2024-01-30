package androidx.compose.foundation.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

internal actual fun Modifier.textFieldScroll(
    scrollerPosition: TextFieldScrollerPosition,
    textFieldValue: TextFieldValue,
    visualTransformation: VisualTransformation,
    textLayoutResultProvider: () -> TextLayoutResultProxy?
): Modifier = defaultTextFieldScroll(
    scrollerPosition,
    textFieldValue,
    visualTransformation,
    textLayoutResultProvider,
)
