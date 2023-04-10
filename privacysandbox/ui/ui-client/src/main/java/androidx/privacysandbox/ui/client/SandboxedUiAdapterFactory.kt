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

package androidx.privacysandbox.ui.client

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import java.util.concurrent.Executor

/**
 * Provides an adapter created from a supplied Bundle which acts as a proxy between the host app and
 * the Binder provided by the provider of content.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object SandboxedUiAdapterFactory {

    // Bundle key is a binary compatibility requirement
    private const val UI_ADAPTER_BINDER = "uiAdapterBinder"

    /**
     * @throws IllegalArgumentException if {@code coreLibInfo} does not contain a Binder with the
     * key UI_ADAPTER_BINDER
     */
    fun createFromCoreLibInfo(coreLibInfo: Bundle): SandboxedUiAdapter {
        val uiAdapterBinder = requireNotNull(coreLibInfo.getBinder(UI_ADAPTER_BINDER)) {
            "Invalid bundle, missing $UI_ADAPTER_BINDER."
        }
        val adapterInterface = ISandboxedUiAdapter.Stub.asInterface(
            uiAdapterBinder
        )
        return RemoteAdapter(adapterInterface)
    }

    private class RemoteAdapter(private val adapterInterface: ISandboxedUiAdapter) :
        SandboxedUiAdapter {

        override fun openSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            val mDisplayManager =
                context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displayId = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY).displayId

            adapterInterface.openRemoteSession(
                Binder(), // Host Token
                displayId,
                initialWidth,
                initialHeight,
                isZOrderOnTop,
                RemoteSessionClient(context, client, clientExecutor)
            )
        }

        class RemoteSessionClient(
            val context: Context,
            val client: SandboxedUiAdapter.SessionClient,
            val clientExecutor: Executor
        ) : IRemoteSessionClient.Stub() {

            override fun onRemoteSessionOpened(
                surfacePackage: SurfaceControlViewHost.SurfacePackage,
                remoteSessionController: IRemoteSessionController,
                isZOrderOnTop: Boolean
            ) {
                val surfaceView = SurfaceView(context)
                surfaceView.setChildSurfacePackage(surfacePackage)
                surfaceView.setZOrderOnTop(isZOrderOnTop)

                clientExecutor.execute {
                    client.onSessionOpened(SessionImpl(surfaceView, remoteSessionController))
                }
            }

            override fun onRemoteSessionError(errorString: String) {
                clientExecutor.execute {
                    client.onSessionError(Throwable(errorString))
                }
            }

            override fun onResizeRequested(width: Int, height: Int) {
                clientExecutor.execute {
                    client.onResizeRequested(width, height)
                }
            }
        }

        private class SessionImpl(
            val surfaceView: SurfaceView,
            val remoteSessionController: IRemoteSessionController
        ) : SandboxedUiAdapter.Session {

            override val view: View = surfaceView

            override fun notifyConfigurationChanged(configuration: Configuration) {
                remoteSessionController.notifyConfigurationChanged(configuration)
            }

            override fun notifyResized(width: Int, height: Int) {
                remoteSessionController.notifyResized(width, height)
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                surfaceView.setZOrderOnTop(isZOrderOnTop)
            }

            override fun close() {
                remoteSessionController.close()
            }
        }
    }
}
