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
@file:JvmName("SandboxedUiAdapterProxy")

package androidx.privacysandbox.ui.provider

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.SurfaceControlViewHost
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory
import androidx.privacysandbox.ui.provider.impl.DeferredSessionClient
import java.util.concurrent.Executor

/**
 * Provides a [Bundle] containing a Binder which represents a [SandboxedUiAdapter]. The Bundle is
 * shuttled to the host app in order for the [SandboxedUiAdapter] to be used to retrieve content.
 */
fun SandboxedUiAdapter.toCoreLibInfo(@Suppress("ContextFirst") context: Context): Bundle {
    val binderAdapter = BinderAdapterDelegate(context, this)
    // TODO: Add version info
    val bundle = Bundle()
    // Bundle key is a binary compatibility requirement
    bundle.putBinder("uiAdapterBinder", binderAdapter)
    return bundle
}

private class BinderAdapterDelegate(
    private val sandboxContext: Context,
    private val adapter: SandboxedUiAdapter
) : ISandboxedUiAdapter.Stub(), SandboxedUiAdapter {

    companion object {
        private const val TAG = "BinderAdapterDelegate"
        private const val FRAME_TIMEOUT_MILLIS = 1000.toLong()
    }

    override fun openSession(
        context: Context,
        windowInputToken: IBinder,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient
    ) {
        adapter.openSession(
            context,
            windowInputToken,
            initialWidth,
            initialHeight,
            isZOrderOnTop,
            clientExecutor,
            SessionClientForObservers(client)
        )
    }

    override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

    override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

    override fun openRemoteSession(
        windowInputToken: IBinder,
        displayId: Int,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        remoteSessionClient: IRemoteSessionClient
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            remoteSessionClient.onRemoteSessionError("openRemoteSession() requires API34+")
            return
        }

        val mHandler = Handler(Looper.getMainLooper())
        mHandler.post {
            try {
                val displayManager =
                    sandboxContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(displayId)
                val displayContext = sandboxContext.createDisplayContext(display)

                val deferredClient =
                    DeferredSessionClient.create(
                        clientFactory = {
                            Api34PlusImpl.createSessionClientProxy(
                                displayContext,
                                display,
                                windowInputToken,
                                isZOrderOnTop,
                                remoteSessionClient
                            )
                        },
                        clientInit = { it.initialize(initialWidth, initialHeight) },
                        errorHandler = { remoteSessionClient.onRemoteSessionError(it.message) }
                    )

                openSession(
                    displayContext,
                    windowInputToken,
                    initialWidth,
                    initialHeight,
                    isZOrderOnTop,
                    MainThreadExecutor(mHandler),
                    deferredClient
                )

                deferredClient.preloadClient()
            } catch (exception: Throwable) {
                remoteSessionClient.onRemoteSessionError(exception.message)
            }
        }
    }

    /** Avoiding all potential concurrency issues by executing callback only on main thread. */
    private class MainThreadExecutor(private val mainHandler: Handler) : Executor {
        override fun execute(command: Runnable) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                command.run()
            } else {
                mainHandler.post(command)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class SessionClientProxy(
        private val touchTransferringView: TouchFocusTransferringView,
        private val surfaceControlViewHost: SurfaceControlViewHost,
        private val isZOrderOnTop: Boolean,
        private val remoteSessionClient: IRemoteSessionClient
    ) : SandboxedUiAdapter.SessionClient {

        /**
         * Split SurfaceControlViewHost creation and calling setView() into 2 steps to minimize each
         * step duration and interference with actual openSession() logic (reduce potential delays).
         */
        fun initialize(initialWidth: Int, initialHeight: Int) {
            surfaceControlViewHost.setView(touchTransferringView, initialWidth, initialHeight)
        }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            val view = session.view

            if (touchTransferringView.childCount > 0) {
                touchTransferringView.removeAllViews()
            }
            touchTransferringView.addView(view)

            // This var is not locked as it will be set to false by the first event that can trigger
            // sending the remote session opened callback.
            var alreadyOpenedSession = false
            view.viewTreeObserver.registerFrameCommitCallback {
                if (!alreadyOpenedSession) {
                    alreadyOpenedSession = true
                    sendRemoteSessionOpened(session)
                }
            }

            // If a frame commit callback is not triggered within the timeout (such as when the
            // screen is off), open the session anyway.
            Handler(Looper.getMainLooper())
                .postDelayed(
                    {
                        if (!alreadyOpenedSession) {
                            Log.w(TAG, "Frame not committed within $FRAME_TIMEOUT_MILLIS ms.")
                            alreadyOpenedSession = true
                            sendRemoteSessionOpened(session)
                        }
                    },
                    FRAME_TIMEOUT_MILLIS
                )
        }

        override fun onSessionError(throwable: Throwable) {
            remoteSessionClient.onRemoteSessionError(throwable.message)
        }

        override fun onResizeRequested(width: Int, height: Int) {
            remoteSessionClient.onResizeRequested(width, height)
        }

        private fun sendRemoteSessionOpened(session: SandboxedUiAdapter.Session) {
            val surfacePackage = surfaceControlViewHost.surfacePackage
            val remoteSessionController = RemoteSessionController(surfaceControlViewHost, session)
            remoteSessionClient.onRemoteSessionOpened(
                surfacePackage,
                remoteSessionController,
                isZOrderOnTop,
                session.signalOptions.isNotEmpty()
            )
        }

        private fun sendSurfacePackage() {
            if (surfaceControlViewHost.surfacePackage != null) {
                remoteSessionClient.onSessionUiFetched(surfaceControlViewHost.surfacePackage)
            }
        }

        @VisibleForTesting
        private inner class RemoteSessionController(
            val surfaceControlViewHost: SurfaceControlViewHost,
            val session: SandboxedUiAdapter.Session,
        ) : IRemoteSessionController.Stub() {

            override fun notifyConfigurationChanged(configuration: Configuration) {
                session.notifyConfigurationChanged(configuration)
            }

            override fun notifyResized(width: Int, height: Int) {
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.post {
                    surfaceControlViewHost.relayout(width, height)
                    session.notifyResized(width, height)
                }
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                session.notifyZOrderChanged(isZOrderOnTop)
            }

            override fun notifyFetchUiForSession() {
                sendSurfacePackage()
            }

            override fun notifyUiChanged(uiContainerInfo: Bundle) {
                session.notifyUiChanged(uiContainerInfo)
            }

            override fun close() {
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.post {
                    session.close()
                    surfaceControlViewHost.release()
                }
            }
        }
    }

    /**
     * Wrapper class to handle the creation of [SessionObserver] instances when the session is
     * opened.
     */
    private inner class SessionClientForObservers(val client: SandboxedUiAdapter.SessionClient) :
        SandboxedUiAdapter.SessionClient by client {

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            val sessionObservers: MutableList<SessionObserver> = mutableListOf()
            if (adapter is AbstractSandboxedUiAdapter) {
                adapter.sessionObserverFactories.forEach { sessionObservers.add(it.create()) }
            }
            client.onSessionOpened(SessionForObservers(session, sessionObservers))
        }
    }

    /**
     * Wrapper class of a [SandboxedUiAdapter.Session] that handles the sending of events to any
     * [SessionObserver]s attached to the session.
     */
    private class SessionForObservers(
        val session: SandboxedUiAdapter.Session,
        val sessionObservers: List<SessionObserver>
    ) : SandboxedUiAdapter.Session by session {

        init {
            if (sessionObservers.isNotEmpty()) {
                val sessionObserverContext = SessionObserverContext(view)
                sessionObservers.forEach { it.onSessionOpened(sessionObserverContext) }
            }
        }

        override val view: View
            get() = session.view

        override val signalOptions: Set<String>
            get() =
                if (sessionObservers.isEmpty()) {
                    setOf()
                } else {
                    setOf("someOptions")
                }

        override fun notifyUiChanged(uiContainerInfo: Bundle) {
            sessionObservers.forEach { it.onUiContainerChanged(uiContainerInfo) }
        }

        override fun close() {
            session.close()
            sessionObservers.forEach { it.onSessionClosed() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object Api34PlusImpl {
        fun createSessionClientProxy(
            displayContext: Context,
            display: Display,
            windowInputToken: IBinder,
            isZOrderOnTop: Boolean,
            remoteSessionClient: IRemoteSessionClient
        ): SessionClientProxy {
            val surfaceControlViewHost =
                SurfaceControlViewHost(displayContext, display, windowInputToken)
            val touchTransferringView =
                TouchFocusTransferringView(displayContext, surfaceControlViewHost)
            return SessionClientProxy(
                touchTransferringView,
                surfaceControlViewHost,
                isZOrderOnTop,
                remoteSessionClient
            )
        }
    }
}
