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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.node.Ref
import androidx.compose.ui.util.UpdateEffect
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.skiko.MainUIDispatcher
import java.awt.Window

/**
 * Compose [Window] obtained from [create]. The [create] block will be called
 * exactly once to obtain the [Window] to be composed, and it is also guaranteed to be invoked on
 * the UI thread (Event Dispatch Thread).
 *
 * Once [AwtWindow] leaves the composition, [dispose] will be called to free resources that
 * obtained by the [Window].
 *
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [Window] properties depending on state.
 * When state changes, the block will be re-executed to set the new properties.
 * Note the block will also be run once right after the [create] block completes.
 *
 * [AwtWindow] is needed for creating window's / dialog's that still can't be created with
 * the default Compose functions [androidx.compose.ui.window.Window] or
 * [androidx.compose.ui.window.DialogWindow].
 *
 * @param visible Is [Window] visible to user.
 * Note that if we set `false` - native resources will not be released. They will be released
 * only when [Window] will leave the composition.
 * @param create The block creating the [Window] to be composed.
 * @param dispose The block to dispose [Window] and free native resources. Usually it is simple
 * `Window::dispose`
 * @param update The callback to be invoked after the layout is inflated.
 */
@OptIn(DelicateCoroutinesApi::class)
@Suppress("unused")
@Composable
fun <T : Window> AwtWindow(
    visible: Boolean = true,
    create: () -> T,
    dispose: (T) -> Unit,
    update: (T) -> Unit = {}
) {
    val windowRef = remember { Ref<T>() }
    fun window() = windowRef.value!!

    DisposableEffect(Unit) {
        windowRef.value = create()
        onDispose {
            dispose(window())
        }
    }

    UpdateEffect {
        val window = window()
        update(window)
    }

    DisposableEffect(visible) {
        // Why we dispatch showing in the next AWT tick:
        //
        // 1.
        // window.isVisible = true can be a blocking operation.
        // So we have to schedule it outside the Compose render frame.
        //
        // This happens in when we create a modal dialog.
        // When we call `window.isVisible = true`, internally a new AWT event loop will be created,
        // which will handle all the future Swing events while dialog is visible.
        //
        // We can't show the window directly inside LaunchedEffect or rememberCoroutineScope because
        // their dispatcher is controlled by the Compose rendering loop (ComposeScene.dispatcher)
        // and we will block the coroutine.
        //
        // 2.
        // We achieve the correct order when we open nested window at the same time when we open the
        // parent window. If we had shown the window immediately we would have had this sequence in
        // case of nested windows:
        //
        // 1. window1.setContent
        // 2. window2.setContent
        // 3. window2.isVisible = true
        // 4. window1.isVisible = true
        //
        // So we will have the wrong window active (window1).
        val showJob = GlobalScope.launch(MainUIDispatcher) {
            window().isVisible = visible
        }

        onDispose {
            showJob.cancel()
        }
    }
}