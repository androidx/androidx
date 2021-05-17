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
import androidx.annotation.RequiresApi
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.runBlockingOnHandlerWithTracing
import androidx.wear.watchface.runOnHandlerWithTracing
import androidx.wear.watchface.style.data.UserStyleWireFormat

/** An interactive watch face instance with SysUI and WCS facing interfaces.*/
@RequiresApi(27)
internal class InteractiveWatchFaceImpl(
    internal val engine: WatchFaceService.EngineWrapper,
    internal var instanceId: String,
    private val uiThreadHandler: Handler
) : IInteractiveWatchFace.Stub() {

    override fun getApiVersion() = IInteractiveWatchFace.API_VERSION

    override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
        uiThreadHandler.runOnHandlerWithTracing("InteractiveWatchFaceImpl.sendTouchEvent") {
            engine.sendTouchEvent(xPos, yPos, tapType)
        }
    }

    override fun getContentDescriptionLabels(): Array<ContentDescriptionLabel> =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "InteractiveWatchFaceImpl.getContentDescriptionLabels"
        ) {
            engine.contentDescriptionLabels
        }

    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams) =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "InteractiveWatchFaceImpl.renderWatchFaceToBitmap"
        ) {
            engine.renderWatchFaceToBitmap(params)
        }

    override fun getPreviewReferenceTimeMillis() = engine.watchFaceImpl.previewReferenceTimeMillis

    override fun setWatchUiState(watchUiState: WatchUiState) {
        uiThreadHandler.runOnHandlerWithTracing("InteractiveWatchFaceImpl.setSystemState") {
            engine.setWatchUiState(watchUiState)
        }
    }

    override fun getInstanceId(): String = instanceId

    override fun ambientTickUpdate() {
        uiThreadHandler.runOnHandlerWithTracing("InteractiveWatchFaceImpl.ambientTickUpdate") {
            engine.ambientTickUpdate()
        }
    }

    override fun release() {
        uiThreadHandler.runOnHandlerWithTracing("InteractiveWatchFaceImpl.release") {
            InteractiveInstanceManager.releaseInstance(instanceId)
        }
    }

    override fun updateComplicationData(
        complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
    ) {
        uiThreadHandler.runOnHandlerWithTracing("InteractiveWatchFaceImpl.updateComplicationData") {
            engine.setComplicationDataList(complicationDatumWireFormats)
        }
    }

    override fun updateWatchfaceInstance(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ) {
        /**
         * This is blocking to ensure ordering with respect to any subsequent [getInstanceId] and
         * [getPreviewReferenceTimeMillis] calls.
         */
        uiThreadHandler.runBlockingOnHandlerWithTracing("InteractiveWatchFaceImpl.updateInstance") {
            if (instanceId != newInstanceId) {
                InteractiveInstanceManager.renameInstance(instanceId, newInstanceId)
                instanceId = newInstanceId
            }
            engine.setUserStyle(userStyle)
            engine.clearComplicationData()
        }
    }

    override fun getComplicationDetails() =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "InteractiveWatchFaceImpl.getComplicationDetails"
        ) {
            engine.getComplicationState()
        }

    override fun getUserStyleSchema() =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "InteractiveWatchFaceImpl.getUserStyleSchema"
        ) {
            engine.watchFaceImpl.currentUserStyleRepository.schema.toWireFormat()
        }

    override fun bringAttentionToComplication(id: Int) {
        uiThreadHandler.runOnHandlerWithTracing(
            "InteractiveWatchFaceImpl.bringAttentionToComplication"
        ) {
            engine.watchFaceImpl.complicationsManager.displayPressedAnimation(id)
        }
    }
}
