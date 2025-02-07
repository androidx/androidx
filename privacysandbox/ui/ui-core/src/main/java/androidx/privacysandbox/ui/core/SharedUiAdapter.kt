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

package androidx.privacysandbox.ui.core

import android.annotation.SuppressLint
import java.util.concurrent.Executor

/**
 * An adapter that provides a communication channel between a UI provider and a client app, while
 * the client is displaying shared UI, i.e. UI that can contain both client-owned and provider-owned
 * elements.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
interface SharedUiAdapter {

    /**
     * Opens a new session to maintain connection with a UI provider. [client] will receive all
     * incoming communication from the provider. All incoming calls to [client] will be made through
     * the provided [clientExecutor].
     */
    fun openSession(clientExecutor: Executor, client: SessionClient)

    /** A single session with the UI provider. */
    interface Session : AutoCloseable {
        /**
         * Closes this session, indicating that the remote provider should dispose of associated
         * resources and that the [SessionClient] should not receive further callback events.
         */
        override fun close()
    }

    /** The client of a single session that will receive callback events from an active session. */
    interface SessionClient {
        /**
         * Called to report that the session was opened successfully, delivering the [Session]
         * handle that should be used to communicate with the provider.
         */
        fun onSessionOpened(session: Session)

        /**
         * Called to report a terminal error in the session. No further events will be reported to
         * this [SessionClient] and any further or currently pending calls to the [Session] that may
         * have been in flight may be ignored.
         */
        fun onSessionError(throwable: Throwable)
    }
}
