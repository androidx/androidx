/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.contextmenu

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAll

/** Unique key to avoid [Unit] clashes in [pointerInput]. */
private object ContextMenuKey

/**
 * Track right click events and update the [state] to [ContextMenuState.Status.Open] with the click
 * offset.
 *
 * @param state the state that will have its status set to open on a right click
 */
internal fun Modifier.contextMenuGestures(state: ContextMenuState): Modifier =
    pointerInput(ContextMenuKey) { onRightClickDown { state.status = Status.Open(offset = it) } }

/** Similar to PointerInputScope.detectTapAndPress, but for right clicks. */
@VisibleForTesting
internal suspend fun PointerInputScope.onRightClickDown(onDown: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstRightClickDown()
        down.consume()
        onDown(down.position)
        waitForUpOrCancellation()?.consume()
    }
}

/**
 * Similar to AwaitPointerEventScope.awaitFirstDown, but with an additional check to ensure it is a
 * right click.
 */
private suspend fun AwaitPointerEventScope.awaitFirstRightClickDown(): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        if (event.buttons.isSecondaryPressed && event.changes.fastAll { it.changedToDown() }) {
            return event.changes[0]
        }
    }
}
