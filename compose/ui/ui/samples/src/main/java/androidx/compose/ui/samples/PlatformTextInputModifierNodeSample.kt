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

@file:Suppress("unused")

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.textInputSession
import androidx.compose.ui.text.input.PlatformTextInputMethodRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Sampled
fun platformTextInputModifierNodeSample() {
    class PlatformTextInputModifierNodeSample : Modifier.Node(),
        FocusEventModifierNode,
        PlatformTextInputModifierNode {

        private var focusedJob: Job? = null

        override fun onFocusEvent(focusState: FocusState) {
            focusedJob?.cancel()
            focusedJob = if (focusState.isFocused) {
                // textInputSession is a suspend function, so it must be called from a coroutine.
                // Launching it into this modifier node's coroutine scope ensures the session will
                // automatically be torn down when the node is detached.
                coroutineScope.launch {
                    // This will automatically cancel any currently-active session.
                    textInputSession {
                        launch {
                            // TODO: Observe text field state, call into system to update it as required
                            //  by the platform.
                        }

                        // Call out to a platform-specific expect/actual function to create the
                        // platform-specific request.
                        val request: PlatformTextInputMethodRequest = createInputRequest()
                        startInputMethod(request)
                    }
                }
            } else {
                null
            }
        }

        // This would probably be an expect/actual function.
        private fun PlatformTextInputSession.createInputRequest(): PlatformTextInputMethodRequest {
            TODO("Create platform-specific request")
        }
    }
}