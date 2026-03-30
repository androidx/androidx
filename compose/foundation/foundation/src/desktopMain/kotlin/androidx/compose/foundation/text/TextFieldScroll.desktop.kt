package androidx.compose.foundation.text

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@Composable
internal actual fun rememberTextFieldOverscrollEffect(): OverscrollEffect? = null

internal actual fun Modifier.textFieldScroll(
    scrollerPosition: TextFieldScrollerPosition,
    textFieldValue: TextFieldValue,
    visualTransformation: VisualTransformation,
    overscrollEffect: OverscrollEffect?,
    textLayoutResultProvider: () -> TextLayoutResultProxy?
): Modifier = defaultTextFieldScroll(
    scrollerPosition,
    textFieldValue,
    visualTransformation,
    overscrollEffect,
    textLayoutResultProvider,
)
