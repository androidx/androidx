/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import org.jetbrains.skiko.SkikoKey

// TODO(https://github.com/JetBrains/compose-multiplatform/issues/3341): support isComposeRootInScrollableContainer
internal actual fun CompositionLocalConsumerModifierNode
    .isComposeRootInScrollableContainer(): Boolean {
    return false
}

/**
 * Whether the specified [KeyEvent] represents a user intent to perform a click.
 * (eg. When you press Enter on a focused button, it should perform a click).
 */
internal actual val KeyEvent.isClick: Boolean
    get() = type == KeyEventType.KeyUp && when (nativeKeyEvent.key) {
        SkikoKey.KEY_ENTER -> true
        else -> false
    }

internal actual val KeyEvent.isPress: Boolean
    get() = type == KeyEventType.KeyDown && when (nativeKeyEvent.key) {
        SkikoKey.KEY_ENTER -> true
        else -> false
    }
