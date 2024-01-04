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
@file:RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
import android.view.SurfaceControlViewHost
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import java.util.concurrent.Executor

/**
 * Provides a [Bundle] containing a Binder which represents a [SandboxedUiAdapter]. The Bundle
 * is shuttled to the host app in order for the [SandboxedUiAdapter] to be used to retrieve
 * content.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun SandboxedUiAdapter.toCoreLibInfo(@Suppress("ContextFirst") context: Context): Bundle {
    val binderAdapter = BinderAdapterDelegate(context, this)
    // TODO: Add version info
    val bundle = Bundle()
    // Bundle key is a binary compatibility requirement
    bundle.putBinder("uiAdapterBinder", binderAdapter)
    return bundle
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
            context, windowInputToken, initialWidth, initialHeight, isZOrderOnTop, clientExecutor,
            client
        )
    }

    override fun openRemoteSession(
        windowInputToken: IBinder,
        displayId: Int,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        remoteSessionClient: IRemoteSessionClient
    ) {
        val mHandler = Handler(Looper.getMainLooper())
        mHandler.post {
            try {
                val mDisplayManager: DisplayManager =
                    sandboxContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val windowContext =
                    sandboxContext.createDisplayContext(mDisplayManager.getDisplay(displayId))
                val surfaceControlViewHost = SurfaceControlViewHost(
                    windowContext,
                    mDisplayManager.getDisplay(displayId), windowInputToken
                )
                val sessionClient = SessionClientProxy(
                    surfaceControlViewHost, initialWidth, initialHeight, isZOrderOnTop,
                    remoteSessionClient
                )
                openSession(
                    windowContext, windowInputToken, initialWidth, initialHeight, isZOrderOnTop,
                    Runnable::run, sessionClient
                )
            } catch (exception: Throwable) {
                remoteSessionClient.onRemoteSessionError(exception.message)
            }
        }
    }

    private inner class SessionClientProxy(
        private val surfaceControlViewHost: SurfaceControlViewHost,
        private val initialWidth: Int,
        private val initialHeight: Int,
        private val isZOrderOnTop: Boolean,
        private val remoteSessionClient: IRemoteSessionClient
    ) : SandboxedUiAdapter.SessionClient {

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            val view = session.view
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val touchTransferringView = TouchFocusTransferringView(
                    sandboxContext, surfaceControlViewHost)
                touchTransferringView.addView(view)
                surfaceControlViewHost.setView(touchTransferringView, initialWidth, initialHeight)
            } else {
                surfaceControlViewHost.setView(view, initialWidth, initialHeight)
            }

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
            Handler(Looper.getMainLooper()).postDelayed({
                if (!alreadyOpenedSession) {
                    Log.w(TAG, "Frame not committed within $FRAME_TIMEOUT_MILLIS ms.")
                    alreadyOpenedSession = true
                    sendRemoteSessionOpened(session)
                }
            }, FRAME_TIMEOUT_MILLIS)
        }

        override fun onSessionError(throwable: Throwable) {
            remoteSessionClient.onRemoteSessionError(throwable.message)
        }

        override fun onResizeRequested(width: Int, height: Int) {
            remoteSessionClient.onResizeRequested(width, height)
        }

        private fun sendRemoteSessionOpened(session: SandboxedUiAdapter.Session) {
            val surfacePackage = surfaceControlViewHost.surfacePackage
            val remoteSessionController =
                RemoteSessionController(surfaceControlViewHost, session)
            remoteSessionClient.onRemoteSessionOpened(
                surfacePackage, remoteSessionController,
                isZOrderOnTop
            )
        }

        @VisibleForTesting
        private inner class RemoteSessionController(
            val surfaceControlViewHost: SurfaceControlViewHost,
            val session: SandboxedUiAdapter.Session
        ) : IRemoteSessionController.Stub() {

            override fun notifyConfigurationChanged(configuration: Configuration) {
                surfaceControlViewHost.surfacePackage?.notifyConfigurationChanged(configuration)
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

            override fun close() {
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.post {
                    session.close()
                    surfaceControlViewHost.release()
                }
            }
        }
    }
}
