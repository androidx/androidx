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

import android.os.Bundle

/**
 * An interface that can be used by the client of a
 * [androidx.privacysandbox.ui.core.SandboxedUiAdapter.Session] to receive updates about its state.
 * One session may be associated with multiple session observers.
 *
 * When a new UI session is started, [onSessionOpened] will be invoked for all registered observers.
 * This callback will contain a [SessionObserverContext] object containing data which will be
 * constant for the lifetime of the UI session. [onUiContainerChanged] will also be sent when a new
 * session is opened. This callback will contain a [Bundle] representing the UI presentation of the
 * session's view.
 *
 * During the entire lifetime of the UI session, [onUiContainerChanged] will be sent when the UI
 * presentation has changed. These updates will be throttled.
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
     * Called when the UI container has changed its presentation state. Note that these updates will
     * not be sent instantaneously, and will be throttled. This should not be used to react to UI
     * changes on the client side as it is not sent in real time.
     */
    // TODO(b/326942993): Decide on the correct data type to send.
    fun onUiContainerChanged(uiContainerInfo: android.os.Bundle)

    /**
     * Called exactly once per session when the
     * [androidx.privacysandbox.ui.core.SandboxedUiAdapter.Session] associated with this session
     * observer closes. No more callbacks will be made on this observer after this point, and any
     * resources associated with it can be freed.
     */
    fun onSessionClosed()
}
