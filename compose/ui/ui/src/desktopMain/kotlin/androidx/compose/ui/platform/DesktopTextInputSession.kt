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

package androidx.compose.ui.platform

import androidx.compose.ui.SessionMutex
import androidx.compose.ui.text.input.PlatformTextInputMethodRequest
import java.awt.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

internal class DesktopTextInputSession(
    coroutineScope: CoroutineScope,
    private val inputComponent: PlatformInputComponent,
    private val component: Component
) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {

    private val innerSessionMutex = SessionMutex<Nothing?>()

    override suspend fun startInputMethod(
        request: PlatformTextInputMethodRequest
    ): Nothing = innerSessionMutex.withSessionCancellingPrevious(
        // This session has no data, just init/dispose tasks.
        sessionInitializer = { null }
    ) {
        @Suppress("RemoveExplicitTypeArguments")
        (suspendCancellableCoroutine<Nothing> { continuation ->
            inputComponent.enableInput(request.inputMethodRequests)
            component.addInputMethodListener(request.inputMethodListener)

            continuation.invokeOnCancellation {
                component.removeInputMethodListener(request.inputMethodListener)
                inputComponent.disableInput(request.inputMethodRequests)
            }
        })
    }
}
