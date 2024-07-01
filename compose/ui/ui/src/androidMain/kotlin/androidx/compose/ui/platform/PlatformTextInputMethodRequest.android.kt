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
     * This method makes relatively stricter ordering guarantees about the lifetime of the returned
     * [InputConnection] than Android does, to make working with connections simpler. Namely, it
     * guarantees:
     * - References to an [InputConnection] will be cleared as soon as the connection get closed via
     *   [InputConnection.closeConnection]. Even if Android leaks its reference to the connection,
     *   the connection returned from this method will not be leaked.
     *
     * However it does not guarantee that only one [InputConnection] will ever be active at a time
     * for a given [PlatformTextInputMethodRequest] instance. On the other hand Android platform
     * does guarantee that even though the platform may create multiple [InputConnection]s, only one
     * of them will ever communicate with the app, invalidating any other [InputConnection] that
     * remained open at the time of communication.
     *
     * Android may call [View.onCreateInputConnection] multiple times for the same session – each
     * system call will result in a 1:1 call to this method. Unfortunately Android platform may
     * decide to use an earlier [InputConnection] returned from this function, invalidating the ones
     * that were created later. Please do not rely on the order of calls to this function.
     *
     * @param outAttributes The [EditorInfo] from [View.onCreateInputConnection].
     * @return The [InputConnection] that will be used to talk to the IME as long as the session is
     *   active. This connection will not receive any calls after the requesting coroutine is
     *   cancelled.
     */
    // Please take a look at go/text-input-session-android-gotchas to learn more about corner cases
    // of Android InputConnection management
    fun createInputConnection(outAttributes: EditorInfo): InputConnection
}
