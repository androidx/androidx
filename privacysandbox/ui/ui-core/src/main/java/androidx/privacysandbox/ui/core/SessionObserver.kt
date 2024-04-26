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

package androidx.privacysandbox.ui.core

/**
 * An interface that can be used by the client of a
 * [androidx.privacysandbox.ui.core.SandboxedUiAdapter.Session] to receive updates about its state.
 * One session may be associated with multiple session observers.
 *
 * When a new UI session is started, [onSessionOpened] will be invoked for all registered observers.
 * This callback will contain a [SessionObserverContext] object containing data which will be
 * constant for the lifetime of the UI session.
 *
 * When the UI session has completed, [onSessionClosed] will be sent. After this point, no more
 * callbacks will be sent and it is safe to free any resources associated with this session
 * observer.
 */
@SuppressWarnings("CallbackName")
interface SessionObserver {

    /**
     * Called exactly once per session, when the
     * [androidx.privacysandbox.ui.core.SandboxedUiAdapter.Session] associated with the session
     * observer is created. [sessionObserverContext] contains data which will be constant for the
     * lifetime of the UI session. The resources associated with the [sessionObserverContext] will
     * be released when [onSessionClosed] is called.
     */
    fun onSessionOpened(sessionObserverContext: SessionObserverContext)

    /**
     * Called exactly once per session when the
     * [androidx.privacysandbox.ui.core.SandboxedUiAdapter.Session] associated with this session
     * observer closes. No more callbacks will be made on this observer after this point, and any
     * resources associated with it can be freed.
     */
    fun onSessionClosed()
}
