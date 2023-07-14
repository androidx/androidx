/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import android.content.res.Configuration
import android.os.IBinder
import android.view.View
import java.util.concurrent.Executor

/**
 * An Adapter that provides content from a SandboxedSdk to be displayed as part of a host app's UI.
 */

interface SandboxedUiAdapter {

    /**
     * Open a new session for displaying content with an initial size of
     * [initialWidth]x[initialHeight] pixels. [client] will receive all incoming communication from
     * the provider of content. All incoming calls to [client] will be made through the provided
     * [clientExecutor]. [isZOrderOnTop] tracks if the content surface will be placed on top of its
     * window
     */
    fun openSession(
        context: Context,
        windowInputToken: IBinder,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SessionClient
    )

    /**
     * A single session with the provider of remote content.
     */
    interface Session {

        /**
         * Return the [View] that presents content for this session. The same view will be returned
         * for the life of the session object. Accessing [view] after [close] may throw an
         * [IllegalStateException].
         */
        val view: View

        /**
         * Notify the provider that the size of the host presentation area has changed to a size of
         * [width] x [height] pixels.
         */
        fun notifyResized(width: Int, height: Int)

        /**
         * Notify the provider that there's a change in the intended z order of the session UI and
         * it is now set to [isZOrderOnTop].
         */
        fun notifyZOrderChanged(isZOrderOnTop: Boolean)

        /**
         * Notify the session that the host configuration has changed to [configuration].
         */
        fun notifyConfigurationChanged(configuration: Configuration)

        /**
         * Close this session, indicating that the remote provider of content should
         * dispose of associated resources and that the [SessionClient] should not
         * receive further callback events.
         */
        fun close()
    }

    /**
     * The client of a single session that will receive callback events from an active session.
     */
    interface SessionClient {
        /**
         * Called to report that the session was opened successfully, delivering the [Session]
         * handle that should be used to notify the session of UI events.
         */
        fun onSessionOpened(session: Session)

        /**
         * Called to report a terminal error in the session. No further events will be reported
         * to this [SessionClient] and any further or currently pending calls to the [Session]
         * that may have been in flight may be ignored.
         */
        fun onSessionError(throwable: Throwable)

        /**
         * Called when the provider of content would like the UI to be presented at [width] and
         * [height]. The library tries to get as close a fit as possible whilst staying within the
         * container's constraints.
         */
        fun onResizeRequested(width: Int, height: Int)
    }
}
