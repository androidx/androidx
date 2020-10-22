/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.control

import android.os.Handler
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.IdAndComplicationData
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.runOnHandler
import androidx.wear.watchface.style.data.UserStyleWireFormat

/** An interactive watchface instance with SysUI and WCS facing interfaces.*/
internal class InteractiveWatchFaceInstance(
    internal val engine: WatchFaceService.EngineWrapper,
    internal val instanceId: String,
    private val uiThreadHandler: Handler
) {
    val sysUiApi = SysUiApi()
    val wcsApi = WCSApi()

    /** The interface for SysUI. */
    inner class SysUiApi : IInteractiveWatchFaceSysUI.Stub() {
        override fun getApiVersion() = IInteractiveWatchFaceSysUI.API_VERSION

        override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
            uiThreadHandler.runOnHandler { engine.sendTouchEvent(xPos, yPos, tapType) }
        }

        override fun getContentDescriptionLabels(): Array<ContentDescriptionLabel> =
            uiThreadHandler.runOnHandler {
                engine.watchFace.complicationsManager.getContentDescriptionLabels()
            }

        override fun takeWatchFaceScreenshot(params: WatchfaceScreenshotParams) =
            uiThreadHandler.runOnHandler { engine.takeWatchFaceScreenshot(params) }

        override fun getPreviewReferenceTimeMillis() = engine.watchFace.previewReferenceTimeMillis

        override fun setSystemState(systemState: SystemState) {
            uiThreadHandler.runOnHandler { engine.setSystemState(systemState) }
        }

        override fun getInstanceId(): String = this@InteractiveWatchFaceInstance.instanceId

        override fun ambientTickUpdate() {
            uiThreadHandler.runOnHandler { engine.ambientTickUpdate() }
        }

        override fun release() {
            uiThreadHandler.runOnHandler { InteractiveInstanceManager.releaseInstance(instanceId) }
        }
    }

    /** The interface for WCS. */
    inner class WCSApi : IInteractiveWatchFaceWCS.Stub() {
        override fun getApiVersion() = IInteractiveWatchFaceWCS.API_VERSION

        override fun updateComplicationData(complicationData: MutableList<IdAndComplicationData>) {
            uiThreadHandler.runOnHandler { engine.setComplicationDataList(complicationData) }
        }

        override fun takeWatchFaceScreenshot(params: WatchfaceScreenshotParams) =
            uiThreadHandler.runOnHandler { engine.takeWatchFaceScreenshot(params) }

        override fun getPreviewReferenceTimeMillis() = engine.watchFace.previewReferenceTimeMillis

        override fun setCurrentUserStyle(userStyle: UserStyleWireFormat) {
            uiThreadHandler.runOnHandler { engine.setUserStyle(userStyle) }
        }

        override fun getInstanceId(): String = this@InteractiveWatchFaceInstance.instanceId

        override fun release() {
            uiThreadHandler.runOnHandler { InteractiveInstanceManager.releaseInstance(instanceId) }
        }

        override fun getComplicationDetails() =
            uiThreadHandler.runOnHandler { engine.getComplicationDetails() }

        override fun getUserStyleSchema() =
            uiThreadHandler.runOnHandler {
                engine.watchFace.userStyleRepository.toSchemaWireFormat()
            }
    }
}
