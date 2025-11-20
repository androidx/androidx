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

package androidx.privacysandbox.ui.provider

import android.view.SurfaceControlViewHost
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject

/**
 * Wrapper class to perform check on client version before delegating call to [IRemoteSessionClient]
 */
internal class RemoteSessionClient(
    private val clientVersion: Int,
    private val remoteSessionClient: IRemoteSessionClient,
) : androidx.privacysandbox.ui.provider.IRemoteSessionClient {
    override fun onRemoteSessionOpened(
        surfacePackage: SurfaceControlViewHost.SurfacePackage,
        remoteSessionController: IRemoteSessionController,
        isZOrderOnTop: Boolean,
        signalOptions: List<String>,
    ) {
        tryToCallRemoteObject(remoteSessionClient) {
            this.onRemoteSessionOpened(
                surfacePackage,
                remoteSessionController,
                isZOrderOnTop,
                signalOptions,
            )
        }
    }

    override fun onRemoteSessionError(exception: String?) {
        tryToCallRemoteObject(remoteSessionClient) { this.onRemoteSessionError(exception) }
    }

    override fun onResizeRequested(width: Int, height: Int) {
        tryToCallRemoteObject(remoteSessionClient) { this.onResizeRequested(width, height) }
    }

    override fun onSessionUiFetched(surfacePackage: SurfaceControlViewHost.SurfacePackage) {
        tryToCallRemoteObject(remoteSessionClient) { this.onSessionUiFetched(surfacePackage) }
    }
}
