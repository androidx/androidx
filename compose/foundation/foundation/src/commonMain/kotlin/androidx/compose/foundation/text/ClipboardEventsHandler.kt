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
import androidx.compose.ui.text.AnnotatedString

/**
 * Some platforms (e.g. web) can produce Clipboard events. See
 * https://developer.mozilla.org/en-US/docs/Web/API/ClipboardEvent The handlers should be integrated
 * with the Text Fields and Selection container. On all platforms but web, this is NoOp.
 *
 * The Clipboard events are triggered by an app user. The callbacks [onPaste], [onCopy], [onCut]
 * must satisfy the user's intention in the Compose app:
 * - onPaste should add/replace the pasted text in a TextField
 * - onCopy should return the selected text and clear the selection if necessary (TextField and
 *   SelectionContainer)
 * - onCut - same as onCopy, but also should remove the selected text in a TextField
 *
 * @param onPaste - invoked in a handler of a platform Clipboard 'paste' event. The lambda value
 *   parameter is the text from 'paste' event (most recent value in the Clipboard).
 * @param onCopy - invoked in a handler of a platform Clipboard 'copy' event. The lambda returns a
 *   value to be copied, it will be set into the 'copy' event.
 * @param onCut - invoked in a handler of a platform Clipboard 'cut' event. The lambda returns a
 *   value to be cut (copied), it will set into the 'cut' event.
 * @param isEnabled - whether the events handling is active (e.g. when a TextField is focused).
 */
@Suppress("ComposableNaming")
@Composable
internal expect inline fun rememberClipboardEventsHandler(
    crossinline onPaste: (AnnotatedString) -> Unit = {},
    crossinline onCopy: () -> AnnotatedString? = { null },
    crossinline onCut: () -> AnnotatedString? = { null },
    isEnabled: Boolean,
)
