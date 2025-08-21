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

import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.foundation.text.contextmenu.gestures.onRightClickDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/** Unique key to avoid [Unit] clashes in [pointerInput]. */
private object ContextMenuKey

/**
 * Track right click events and update the [state] to [ContextMenuState.Status.Open] with the click
 * offset.
 *
 * @param state the state that will have its status set to open on a right click
 */
internal fun Modifier.contextMenuGestures(state: ContextMenuState): Modifier = contextMenuGestures {
    state.status = Status.Open(offset = it)
}

/**
 * Track right click events and invoke [onOpenGesture] callback
 *
 * @param onOpenGesture the callback that will be invoked on a right click
 */
internal fun Modifier.contextMenuGestures(onOpenGesture: (Offset) -> Unit): Modifier =
    pointerInput(ContextMenuKey) { onRightClickDown(onOpenGesture) }
