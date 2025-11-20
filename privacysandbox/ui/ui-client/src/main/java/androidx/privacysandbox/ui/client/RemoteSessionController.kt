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

package androidx.privacysandbox.ui.client

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import androidx.privacysandbox.ui.core.IMotionEventTransferCallback
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject

/**
 * Wrapper class to perform check on provider version before delegating call to
 * [androidx.privacysandbox.ui.core.IRemoteSessionController]
 */
internal class RemoteSessionController(
    private val uiProviderVersion: Int,
    private val remoteSessionController: IRemoteSessionController,
) : androidx.privacysandbox.ui.client.IRemoteSessionController {
    override fun close() {
        tryToCallRemoteObject(remoteSessionController) { close() }
    }

    override fun notifyConfigurationChanged(configuration: Configuration) {
        tryToCallRemoteObject(remoteSessionController) { notifyConfigurationChanged(configuration) }
    }

    override fun notifyResized(width: Int, height: Int) {
        tryToCallRemoteObject(remoteSessionController) { notifyResized(width, height) }
    }

    override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
        tryToCallRemoteObject(remoteSessionController) { notifyZOrderChanged(isZOrderOnTop) }
    }

    override fun notifyFetchUiForSession() {
        tryToCallRemoteObject(remoteSessionController) { notifyFetchUiForSession() }
    }

    override fun notifyUiChanged(uiContainerInfo: Bundle) {
        tryToCallRemoteObject(remoteSessionController) { notifyUiChanged(uiContainerInfo) }
    }

    override fun notifySessionRendered(supportedSignalOptions: List<String>) {
        tryToCallRemoteObject(remoteSessionController) {
            notifySessionRendered(supportedSignalOptions)
        }
    }

    override fun notifyMotionEvent(
        motionEvent: MotionEvent,
        eventTargetTime: Long,
        eventTransferCallback: IMotionEventTransferCallback?,
    ) {
        tryToCallRemoteObject(remoteSessionController) {
            notifyMotionEvent(motionEvent, eventTargetTime, eventTransferCallback)
        }
    }

    override fun notifyHoverEvent(hoverEvent: MotionEvent, eventTargetTime: Long) {
        tryToCallRemoteObject(remoteSessionController) {
            notifyHoverEvent(hoverEvent, eventTargetTime)
        }
    }
}
