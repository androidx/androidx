/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.WebTextInputService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

internal class WebTextInputSession(
    coroutineScope: CoroutineScope,
    private val webTextInputService: WebTextInputService
) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {

    override suspend fun startInputMethod(
        request: PlatformTextInputMethodRequest
    ): Nothing = coroutineScope {
        // TODO: Adopt PlatformTextInputService2 (https://youtrack.jetbrains.com/issue/CMP-7831/Web-Adopt-PlatformTextInputService2)
        launch {
            snapshotFlow { request.value() }.collect {
                webTextInputService.updateState(oldValue = null, newValue = it)
            }
        }
        launch {
            snapshotFlow { request.focusedRectInRoot() }.filterNotNull().collect {
                // Skip Rect.Zero, because it's an initial value. The real value is never Zero
                if (it != Rect.Zero) {
                    webTextInputService.notifyFocusedRect(it)
                }
            }
        }
        suspendCancellableCoroutine<Nothing> { continuation ->
            webTextInputService.startInput(
                value = request.value(),
                imeOptions = request.imeOptions,
                onEditCommand = request.onEditCommand,
                onImeActionPerformed = request.onImeAction ?: {}
            )

            continuation.invokeOnCancellation {
                webTextInputService.stopInput()
            }
        }
    }
}