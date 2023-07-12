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

package androidx.window.area

import androidx.window.core.ExperimentalWindowApi

/**
 *  Callback to update the client on the WindowArea Session being
 * started and ended.
 * TODO(b/207720511) Move to window-java module when Kotlin API Finalized
 */
@ExperimentalWindowApi
interface WindowAreaSessionCallback {

    /**
     * Notifies about a start of a session. Provides a reference to the current [WindowAreaSession]
     * the application the ability to close the session through [WindowAreaSession.close].
     */
    fun onSessionStarted(session: WindowAreaSession)

    /**
     * Notifies about an end of a [WindowAreaSession].
     *
     * @param t [Throwable] to provide information on if the session was ended due to an error.
     * This will only occur if a session is attempted to be enabled when it is not available, but
     * can be expanded to alert for more errors in the future.
     */
    fun onSessionEnded(t: Throwable?)
}
