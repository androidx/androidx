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
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.SurfaceControlViewHost
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.privacysandbox.ui.core.ClientAdapterWrapper
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IDelegateChangeListener
import androidx.privacysandbox.ui.core.IDelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.IDelegatorCallback
import androidx.privacysandbox.ui.core.IMotionEventTransferCallback
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.LocalUiAdapter
import androidx.privacysandbox.ui.core.ProtocolConstants
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import androidx.privacysandbox.ui.core.SdkRuntimeUiLibVersions
import androidx.privacysandbox.ui.core.SessionData
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.provider.impl.DeferredSessionClient
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Provides a [Bundle] containing a Binder which represents a [SandboxedUiAdapter]. The Bundle is
 * shuttled to the host app in order for the [SandboxedUiAdapter] to be used to retrieve content.
 */
@OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
public fun SandboxedUiAdapter.toCoreLibInfo(@Suppress("ContextFirst") context: Context): Bundle {
    // If the ui adapter has already been wrapped as a client SandboxedUiAdapter
    // at some point it needs no further wrapping
    if (this is ClientAdapterWrapper) {
        return this.getSourceBundle()
    }

    val bundle = Bundle()
    bundle.putInt(
        ProtocolConstants.uiProviderVersionKey,
        SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
    )
    val binderAdapter =
        if (this is DelegatingSandboxedUiAdapter) {
            bundle.putBundle(ProtocolConstants.delegateKey, this.getDelegate())
            BinderDelegatingAdapter(this)
        } else {
            BinderAdapterDelegate(context, this)
        }
    // Bundle key is a binary compatibility requirement
    bundle.putBinder(ProtocolConstants.uiAdapterBinderKey, binderAdapter)
    return bundle
}

@OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
private class BinderDelegatingAdapter(private var adapter: DelegatingSandboxedUiAdapter) :
    IDelegatingSandboxedUiAdapter.Stub() {
    private class RemoteDelegateChangeListener(val binder: IDelegateChangeListener) :
        DelegatingSandboxedUiAdapter.DelegateChangeListener {

        override suspend fun onDelegateChanged(delegate: Bundle) {
            suspendCancellableCoroutine { continuation ->
                tryToCallRemoteObject(binder) {
                    onDelegateChanged(
                        delegate,
                        object : IDelegatorCallback.Stub() {
                            override fun onDelegateChangeResult(success: Boolean) {
                                if (success) {
                                    continuation.resume(Unit)
                                } else {
                                    continuation.resumeWithException(
                                        IllegalStateException("Client failed to switch")
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    override fun addDelegateChangeListener(binder: IDelegateChangeListener) {
        val listener = RemoteDelegateChangeListener(binder)
        adapter.addDelegateChangeListener(listener)
        binder.asBinder().linkToDeath({ adapter.removeDelegateChangeListener(listener) }, 0)
    }

    override fun removeDelegateChangeListener(listener: IDelegateChangeListener) {
        adapter.removeDelegateChangeListener(RemoteDelegateChangeListener(listener))
    }
}

private class BinderAdapterDelegate(
    private val sandboxContext: Context,
    private val adapter: SandboxedUiAdapter,
) : LocalUiAdapter, ISandboxedUiAdapter.Stub() {

    companion object {
        private const val TAG = "BinderAdapterDelegate"
        private const val FRAME_TIMEOUT_MILLIS = 1000.toLong()
    }

    /** Called in local mode via reflection. */
    override fun openLocalSession(
        clientVersion: Int,
        context: Context,
        sessionData: SessionData,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SessionClient,
    ) {
        MainThreadExecutor.execute {
            val displayManager =
                sandboxContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext = sandboxContext.createDisplayContext(display)
            openSessionInternal(
                displayContext,
                sessionData,
                initialWidth,
                initialHeight,
                isZOrderOnTop,
                clientExecutor,
                LocalSessionClient(clientVersion, client),
            )
        }
    }

    /** Called in remote mode via binder call. */
    override fun openRemoteSession(
        clientVersion: Int,
        sessionData: Bundle,
        displayId: Int,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        remoteSessionClient: IRemoteSessionClient,
    ) {
        val remoteSessionClientWithVersionCheck =
            RemoteSessionClient(clientVersion, remoteSessionClient)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            remoteSessionClientWithVersionCheck.onRemoteSessionError(
                "openRemoteSession() requires API34+"
            )
            return
        }
        val constants = SessionData.fromBundle(sessionData)

        MainThreadExecutor.execute {
            try {
                val displayManager =
                    sandboxContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(displayId)
                val displayContext = sandboxContext.createDisplayContext(display)

                val deferredClient =
                    DeferredSessionClient.create(
                        clientFactory = {
                            RemoteCompatImpl.createSessionClientProxy(
                                displayContext,
                                display,
                                constants,
                                isZOrderOnTop,
                                remoteSessionClientWithVersionCheck,
                            )
                        },
                        clientInit = { it.initialize(initialWidth, initialHeight) },
                        errorHandler = {
                            remoteSessionClientWithVersionCheck.onRemoteSessionError(it.message)
                        },
                    )

                openSessionInternal(
                    displayContext,
                    constants,
                    initialWidth,
                    initialHeight,
                    isZOrderOnTop,
                    MainThreadExecutor,
                    deferredClient,
                )

                deferredClient.preloadClient()
            } catch (exception: Throwable) {
                remoteSessionClientWithVersionCheck.onRemoteSessionError(exception.message)
            }
        }
    }

    private fun openSessionInternal(
        context: Context,
        sessionData: SessionData,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient,
    ) {
        adapter.openSession(
            context,
            sessionData,
            initialWidth,
            initialHeight,
            isZOrderOnTop,
            clientExecutor,
            SessionClientForObservers(client),
        )
    }

    /** Avoiding all potential concurrency issues by executing callback only on main thread. */
    private object MainThreadExecutor : Executor {
        private val mainHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            if (mainHandler.looper == Looper.myLooper()) {
                command.run()
            } else {
                mainHandler.post(command)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class SessionClientProxy(
        private val providerViewWrapper: ProviderViewWrapper,
        private val surfaceControlViewHost: SurfaceControlViewHost,
        private val isZOrderOnTop: Boolean,
        private val remoteSessionClient: androidx.privacysandbox.ui.provider.IRemoteSessionClient,
    ) : SandboxedUiAdapter.SessionClient {

        /**
         * Split SurfaceControlViewHost creation and calling setView() into 2 steps to minimize each
         * step duration and interference with actual openSession() logic (reduce potential delays).
         */
        fun initialize(initialWidth: Int, initialHeight: Int) {
            surfaceControlViewHost.setView(providerViewWrapper, initialWidth, initialHeight)
        }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            val view = session.view

            if (providerViewWrapper.childCount > 0) {
                providerViewWrapper.removeAllViews()
            }
            providerViewWrapper.addView(view)

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
                    FRAME_TIMEOUT_MILLIS,
                )
        }

        override fun onSessionError(throwable: Throwable) {
            remoteSessionClient.onRemoteSessionError(throwable.message)
        }

        override fun onResizeRequested(width: Int, height: Int) {
            remoteSessionClient.onResizeRequested(width, height)
        }

        private fun sendRemoteSessionOpened(session: SandboxedUiAdapter.Session) {
            val remoteSessionController = RemoteSessionController(surfaceControlViewHost, session)
            surfaceControlViewHost.surfacePackage?.let { surfacePackage ->
                remoteSessionClient.onRemoteSessionOpened(
                    surfacePackage,
                    remoteSessionController,
                    isZOrderOnTop,
                    session.signalOptions.toList(),
                )
            }
        }

        private fun sendSurfacePackage() {
            surfaceControlViewHost.surfacePackage?.let { surfacePackage ->
                remoteSessionClient.onSessionUiFetched(surfacePackage)
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

            override fun notifySessionRendered(supportedSignalOptions: List<String>) {
                session.notifySessionRendered(supportedSignalOptions.toSet())
            }

            override fun notifyMotionEvent(
                motionEvent: MotionEvent,
                eventTargetFrameTime: Long,
                eventTransferCallback: IMotionEventTransferCallback?,
            ) {
                providerViewWrapper.scheduleMotionEventProcessing(
                    motionEvent,
                    eventTargetFrameTime,
                    eventTransferCallback,
                )
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
            val signalOptions: MutableSet<String> = mutableSetOf()
            if (adapter is SessionObserverFactoryRegistry) {
                adapter.sessionObserverFactories.forEach {
                    sessionObservers.add(it.create())
                    signalOptions.addAll(it.signalOptions)
                }
            }
            client.onSessionOpened(SessionForObservers(session, sessionObservers, signalOptions))
        }
    }

    /**
     * Wrapper class of a [SandboxedUiAdapter.Session] that handles the sending of events to any
     * [SessionObserver]s attached to the session.
     */
    private class SessionForObservers(
        val session: SandboxedUiAdapter.Session,
        val sessionObservers: List<SessionObserver>,
        override val signalOptions: Set<String>,
    ) : SandboxedUiAdapter.Session by session {

        override val view: View
            get() = session.view

        override fun notifySessionRendered(supportedSignalOptions: Set<String>) {
            sessionObservers.forEach {
                it.onSessionOpened(SessionObserverContext(view, supportedSignalOptions))
            }
        }

        override fun notifyUiChanged(uiContainerInfo: Bundle) {
            // Copy the bundle in case [SandboxedSdkViewUiInfo.pruneBundle] alters the bundle.
            sessionObservers.forEach { it.onUiContainerChanged(Bundle(uiContainerInfo)) }
        }

        override fun close() {
            session.close()
            sessionObservers.forEach { it.onSessionClosed() }
        }
    }

    /**
     * Provides backward compat support for APIs.
     *
     * If the API is available, it's called from a version-specific static inner class gated with
     * version check, otherwise a fallback action is taken depending on the situation.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object RemoteCompatImpl {

        fun createSessionClientProxy(
            displayContext: Context,
            display: Display,
            sessionData: SessionData,
            isZOrderOnTop: Boolean,
            remoteSessionClient: androidx.privacysandbox.ui.provider.IRemoteSessionClient,
        ): SessionClientProxy {
            val surfaceControlViewHost =
                checkNotNull(createSurfaceControlViewHost(displayContext, display, sessionData)) {
                    "Failed to create SurfaceControlViewHost"
                }
            return SessionClientProxy(
                ProviderViewWrapper(displayContext),
                surfaceControlViewHost,
                isZOrderOnTop,
                remoteSessionClient,
            )
        }

        fun createSurfaceControlViewHost(
            displayContext: Context,
            display: Display,
            sessionData: SessionData,
        ): SurfaceControlViewHost? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Api35PlusImpl.createSurfaceControlViewHost(displayContext, display, sessionData)
            } else Api34PlusImpl.createSurfaceControlViewHost(displayContext, display, sessionData)
        }

        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        private object Api35PlusImpl {

            @JvmStatic
            fun createSurfaceControlViewHost(
                context: Context,
                display: Display,
                sessionData: SessionData,
            ): SurfaceControlViewHost {
                return SurfaceControlViewHost(context, display, sessionData.inputTransferToken)
            }
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private object Api34PlusImpl {

            @JvmStatic
            fun createSurfaceControlViewHost(
                context: Context,
                display: Display,
                sessionData: SessionData,
            ): SurfaceControlViewHost {
                return SurfaceControlViewHost(context, display, sessionData.windowInputToken)
            }
        }
    }
}
