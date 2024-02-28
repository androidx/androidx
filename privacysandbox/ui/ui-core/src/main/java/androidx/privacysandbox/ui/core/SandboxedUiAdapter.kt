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
import android.os.Bundle
import android.os.IBinder
import android.view.View
import java.lang.AutoCloseable
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
     * Adds a [SessionObserverFactory] with a [SandboxedUiAdapter] for tracking UI presentation
     * state across UI sessions. This has no effect on already open sessions.
     *
     * For each [SandboxedUiAdapter.Session] that is created for the adapter after registration is
     * complete, [SessionObserverFactory.create] will be invoked to allow a new [SessionObserver]
     * instance to be attached to the UI session. This [SessionObserver] will receive UI updates for
     * the lifetime of the session. There may be one or more UI sessions created for a
     * [SandboxedUiAdapter], and a separate [SessionObserverFactory.create] call will be made for
     * each one.
     */
    fun addObserverFactory(sessionObserverFactory: SessionObserverFactory)

    /**
     * Removes a [SessionObserverFactory] from a [SandboxedUiAdapter], if it has been previously
     * added with [addObserverFactory].
     *
     * If the [SessionObserverFactory] was not previously added, no action is performed. Any
     * existing [SessionObserver] instances that have been created by the [SessionObserverFactory]
     * will continue to receive updates until their corresponding [SandboxedUiAdapter.Session] has
     * been closed. For any subsequent sessions created for the [SandboxedUiAdapter], no call to
     * [SessionObserverFactory.create] will be made.
     */
    fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory)

    /** A single session with the provider of remote content. */
    interface Session : AutoCloseable {

        /**
         * Return the [View] that presents content for this session. The same view will be returned
         * for the life of the session object. Accessing [view] after [close] may throw an
         * [IllegalStateException].
         */
        val view: View

        /**
         * The set of options that will be used to determine what information is calculated and sent
         * to [SessionObserver]s attached to this session.
         *
         * This value should not be directly set by UI providers. Instead, the registration of any
         * [SessionObserverFactory] with [addObserverFactory] will indicate that information should
         * be calculated for this session.
         */
        val signalOptions: Set<String>

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

        /** Notify the session that the host configuration has changed to [configuration]. */
        fun notifyConfigurationChanged(configuration: Configuration)

        /**
         * Notify the session when the presentation state of its UI container has changed.
         *
         * [uiContainerInfo] contains a Bundle that represents the state of the container. The exact
         * details of this Bundle depend on the container this Bundle is describing. This
         * notification is not in real time and is throttled, so it should not be used to react to
         * UI changes on the client side.
         *
         * UI providers should use [addObserverFactory] to observe UI changes rather than using this
         * method.
         */
        fun notifyUiChanged(uiContainerInfo: Bundle)

        /**
         * Close this session, indicating that the remote provider of content should dispose of
         * associated resources and that the [SessionClient] should not receive further callback
         * events.
         */
        override fun close()
    }

    /** The client of a single session that will receive callback events from an active session. */
    interface SessionClient {
        /**
         * Called to report that the session was opened successfully, delivering the [Session]
         * handle that should be used to notify the session of UI events.
         */
        fun onSessionOpened(session: Session)

        /**
         * Called to report a terminal error in the session. No further events will be reported to
         * this [SessionClient] and any further or currently pending calls to the [Session] that may
         * have been in flight may be ignored.
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
