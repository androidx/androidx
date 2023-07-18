/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntOffset

@Immutable
actual class DialogProperties actual constructor(
    actual val dismissOnBackPress: Boolean,
    actual val dismissOnClickOutside: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        return result
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit
) {
    val popupPositioner = remember {
        AlignmentOffsetPositionProvider(
            alignment = Alignment.Center,
            offset = IntOffset(0, 0)
        )
    }
    PopupLayout(
        popupPositionProvider = popupPositioner,
        focusable = true,
        if (properties.dismissOnClickOutside) onDismissRequest else null,
        modifier = Modifier.drawBehind {
            drawRect(Color.Black.copy(alpha = 0.4f))
        },
        onKeyEvent = {
            if (properties.dismissOnBackPress &&
                it.type == KeyEventType.KeyDown && it.key == Key.Escape
            ) {
                onDismissRequest()
                true
            } else {
                false
            }
        },
        content = content
    )
}
