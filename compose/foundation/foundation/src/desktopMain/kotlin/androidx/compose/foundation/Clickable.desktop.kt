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
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.DelegatableNode
import java.awt.event.KeyEvent.VK_ENTER

internal actual fun DelegatableNode.isComposeRootInScrollableContainer(): Boolean {
    return false
}

// TODO: b/168524931 - should this depend on the input device?
internal actual val TapIndicationDelay: Long = 0L

/**
 * Whether the specified [KeyEvent] should trigger a press for a clickable component, i.e. whether
 * it is associated with a press of the enter key.
 */
internal actual val KeyEvent.isPress: Boolean
    get() = type == KeyDown && key.nativeKeyCode == VK_ENTER

/**
 * Whether the specified [KeyEvent] should trigger a click for a clickable component, i.e. whether
 * it is associated with a release of the enter key.
 */
internal actual val KeyEvent.isClick: Boolean
    get() = type == KeyUp && key.nativeKeyCode == VK_ENTER
