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

package androidx.compose.ui.text.input

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Represents a request to open an Android text input session via
 * `PlatformTextInputSession.startInputMethod`.
 */
actual fun interface PlatformTextInputMethodRequest {

    /**
     * Called when the platform requests an [InputConnection] via [View.onCreateInputConnection].
     *
     * This method makes stricter ordering guarantees about the lifetime of the returned
     * [InputConnection] than Android does, to make working with connections simpler. Namely, it
     * guarantees:
     *  - For a given [PlatformTextInputMethodRequest] instance, only one [InputConnection] will
     *    ever be active at a time.
     *  - References to an [InputConnection] will be cleared as soon as the connection becomes
     *    inactive. Even if Android leaks its reference to the connection, the connection returned
     *    from this method will not be leaked.
     *  - On API levels that support [InputConnection.closeConnection] (24+), a connection will
     *    always be closed before a new connection is requested.
     *
     * Android may call [View.onCreateInputConnection] multiple times for the same session – each
     * system call will result in a 1:1 call to this method, although the old connection will always
     * be closed first.
     *
     * @param outAttrs The [EditorInfo] from [View.onCreateInputConnection].
     *
     * @return The [InputConnection] that will be used to talk to the IME as long as the session is
     * active. This connection will not receive any calls after the requesting coroutine is
     * cancelled.
     */
    fun createInputConnection(outAttrs: EditorInfo): InputConnection
}