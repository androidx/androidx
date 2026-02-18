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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import java.awt.Window

/**
 * Compose [Window] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [Window] to be composed, and it is also guaranteed to be invoked on
 * the UI thread (Event Dispatch Thread).
 *
 * Once [AwtWindow] leaves the composition, [dispose] will be called to free resources that were
 * obtained by the [Window].
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [Window] properties depending on state.
 * When state changes, the block will be re-executed to set the new properties.
 * Note the block will also be run once right after the [create] block completes.
 *
 * [AwtWindow] is needed for creating windows / dialogs that can't be created with the default
 * Compose functions [androidx.compose.ui.window.Window] or
 * [androidx.compose.ui.window.DialogWindow].
 *
 * @param visible Is [Window] visible to user.
 * Note that if we set `false` - native resources will not be released. They will be released
 * only when [Window] will leave the composition.
 * @param create The block creating the [Window] to be composed.
 * @param dispose The block to dispose [Window] and free native resources. Usually it is simple
 * `Window::dispose`
 * @param update The callback to be invoked to update window properties.
 */
@Suppress("unused")
@Deprecated(
    message = "Moved to androidx.compose.ui.awt",
    replaceWith = ReplaceWith(
        expression = "androidx.compose.ui.awt.AwtWindow(visible, create, dispose, update)",
    )
)
@Composable
@ComposableOpenTarget(-1)
fun <T : Window> AwtWindow(
    visible: Boolean = true,
    create: () -> T,
    dispose: (T) -> Unit,
    update: (T) -> Unit = {}
) {
    androidx.compose.ui.awt.AwtWindow(visible, create, dispose, update)
}
