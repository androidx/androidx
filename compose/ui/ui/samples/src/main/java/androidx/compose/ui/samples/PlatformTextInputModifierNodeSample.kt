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

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.Sampled
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.establishTextInputSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Suppress("UnusedReceiverParameter")
@Sampled
fun platformTextInputModifierNodeSample() {
    class PlatformTextInputModifierNodeSample :
        Modifier.Node(), FocusEventModifierNode, PlatformTextInputModifierNode {

        private var focusedJob: Job? = null

        override fun onFocusEvent(focusState: FocusState) {
            focusedJob?.cancel()
            focusedJob =
                if (focusState.isFocused) {
                    // establishTextInputSession is a suspend function, so it must be called from a
                    // coroutine. Launching it into this modifier node's coroutine scope ensures the
                    // session will automatically be torn down when the node is detached.
                    coroutineScope.launch {
                        // This will automatically cancel any currently-active session.
                        establishTextInputSession {
                            launch {
                                // TODO: Observe text field state, call into system to update it as
                                //  required by the platform.
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

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun InterceptPlatformTextInputSample() {
    var text by remember { mutableStateOf("") }

    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            // Create a new request to wrap the incoming one with some custom logic.
            val modifiedRequest =
                object : PlatformTextInputMethodRequest {
                    override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                        val inputConnection = request.createInputConnection(outAttributes)
                        // After the original request finishes initializing the EditorInfo we can
                        // customize it. If we needed to we could also wrap the InputConnection
                        // before
                        // returning it.
                        updateEditorInfo(outAttributes)
                        return inputConnection
                    }

                    fun updateEditorInfo(outAttributes: EditorInfo) {
                        // Your code here, e.g. set some custom properties.
                    }
                }

            // Send our wrapping request to the next handler, which could be the system or another
            // interceptor up the tree.
            nextHandler.startInputMethod(modifiedRequest)
        }
    ) {
        BasicTextField(value = text, onValueChange = { text = it })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
fun disableSoftKeyboardSample() {
    /**
     * A function that disables the soft keyboard for any text field within its content.
     *
     * The keyboard is re-enabled by removing this modifier or passing `disable = false`.
     */
    @Composable
    fun DisableSoftKeyboard(disable: Boolean = true, content: @Composable () -> Unit) {
        InterceptPlatformTextInput(
            interceptor = { request, nextHandler ->
                // If this flag is changed while an input session is active, a new lambda instance
                // that captures the new value will be passed to InterceptPlatformTextInput, which
                // will automatically cancel the session upstream and restart it with this new
                // interceptor.
                if (!disable) {
                    // Forward the request to the system.
                    nextHandler.startInputMethod(request)
                } else {
                    // This function has to return Nothing, and since we don't have any work to do
                    // in this case, we just suspend until cancelled.
                    awaitCancellation()
                }
            },
            content = content
        )
    }
}
