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

package androidx.compose.foundation.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.browser.document
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener as EventListenerInterface

@Composable
@NonRestartableComposable
internal actual inline fun rememberClipboardEventsHandler(
    crossinline onPaste: (String) -> Unit,
    crossinline onCopy: () -> String?,
    crossinline onCut: () -> String?,
    isEnabled: Boolean
) {
    if (isEnabled) {
        DisposableEffect(Unit) {
            val copyListener = EventListener { event ->
                val textToCopy = onCopy()
                if (textToCopy != null && event is ClipboardEvent) {
                    event.clipboardData?.setData("text/plain", textToCopy)
                    event.preventDefault()
                }
            }

            val pasteListener = EventListener { event ->
                if (event is ClipboardEvent) {
                    val textToPaste = event.clipboardData?.getData("text/plain") ?: ""
                    onPaste(textToPaste)
                    event.preventDefault()
                }
            }

            val cutListener = EventListener { event ->
                val cutText = onCut()
                if (cutText != null && event is ClipboardEvent) {
                    event.clipboardData?.setData("text/plain", cutText)
                    event.preventDefault()
                }
            }

            document.addEventListener("copy", copyListener)
            document.addEventListener("paste", pasteListener)
            document.addEventListener("cut", cutListener)

            onDispose {
                document.removeEventListener("copy", copyListener)
                document.removeEventListener("paste", pasteListener)
                document.removeEventListener("cut", cutListener)
            }
        }
    }
}

private fun EventListener(handler: (Event) -> Unit): EventListenerInterface =
    js("(event) => { handler(event) }")
