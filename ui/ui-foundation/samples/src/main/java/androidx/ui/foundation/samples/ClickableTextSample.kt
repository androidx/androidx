/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.foundation.samples

import android.util.Log
import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.longPressGestureFilter
import androidx.ui.foundation.ClickableText
import androidx.ui.foundation.Text
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextOverflow

@Sampled
@Composable
fun ClickableText() {
    ClickableText(
        text = AnnotatedString("Click Me"),
        onClick = { offset ->
            Log.d("ClickableText", "$offset -th character is clicked.")
        }
    )
}

@Sampled
@Composable
fun LongClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = false,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onLongClick: (offset: Int) -> Unit
) {
    val layoutResult = state<TextLayoutResult?> { null }
    val gesture = Modifier.longPressGestureFilter(
        onLongPress = { pos ->
            layoutResult.value?.let { layout ->
                onLongClick(layout.getOffsetForPosition(pos))
            }
        }
    )

    Text(text = text,
        modifier = modifier + gesture,
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            onTextLayout(it)
            layoutResult.value = it
        }
    )
}
