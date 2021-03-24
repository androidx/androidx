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
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.runOnHandlerWithTracing
import androidx.wear.watchface.style.data.UserStyleWireFormat

/** An interactive watch face instance with SysUI and WCS facing interfaces.*/
@RequiresApi(27)
internal class InteractiveWatchFaceImpl(
    internal val engine: WatchFaceService.EngineWrapper,
    internal var instanceId: String,
    private val uiThreadHandler: Handler
) {
    fun createSysUiApi() = SysUiApi(engine, this, uiThreadHandler)

    fun createWCSApi() = WCSApi(engine, this, uiThreadHandler)
}

/** The interface for SysUI. */
@RequiresApi(27)
internal class SysUiApi(
    private val engine: WatchFaceService.EngineWrapper,
    private val instance: InteractiveWatchFaceImpl,
    private val uiThreadHandler: Handler
) : IInteractiveWatchFaceSysUI.Stub() {
    override fun getApiVersion() = IInteractiveWatchFaceSysUI.API_VERSION

    override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
        uiThreadHandler.runOnHandlerWithTracing("SysUiApi.sendTouchEvent") {
            engine.sendTouchEvent(xPos, yPos, tapType)
        }
    }

    override fun getContentDescriptionLabels(): Array<ContentDescriptionLabel> =
        uiThreadHandler.runOnHandlerWithTracing("SysUiApi.getContentDescriptionLabels") {
            engine.watchFaceImpl.complicationsManager.getContentDescriptionLabels()
        }

    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams) =
        uiThreadHandler.runOnHandlerWithTracing("SysUiApi.renderWatchFaceToBitmap") {
            engine.renderWatchFaceToBitmap(params)
        }

    override fun getPreviewReferenceTimeMillis() = engine.watchFaceImpl.previewReferenceTimeMillis

    override fun setSystemState(systemState: SystemState) {
        uiThreadHandler.runOnHandlerWithTracing("SysUiApi.setSystemState") {
            engine.setSystemState(systemState)
        }
    }

    override fun getInstanceId(): String = instance.instanceId

    override fun ambientTickUpdate() {
        uiThreadHandler.runOnHandlerWithTracing("SysUiApi.ambientTickUpdate") {
            engine.ambientTickUpdate()
        }
    }

    override fun release() {
        uiThreadHandler.runOnHandlerWithTracing("SysUiApi.release") {
            InteractiveInstanceManager.releaseInstance(instanceId)
        }
    }
}

/** The interface for WCS. */
@RequiresApi(27)
internal class WCSApi(
    private val engine: WatchFaceService.EngineWrapper,
    private val instance: InteractiveWatchFaceImpl,
    private val uiThreadHandler: Handler
) : IInteractiveWatchFaceWCS.Stub() {
    override fun getApiVersion() = IInteractiveWatchFaceWCS.API_VERSION

    override fun updateComplicationData(
        complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
    ) {
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.updateComplicationData") {
            engine.setComplicationDataList(complicationDatumWireFormats)
        }
    }

    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams) =
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.renderWatchFaceToBitmap") {
            engine.renderWatchFaceToBitmap(params)
        }

    override fun getPreviewReferenceTimeMillis() = engine.watchFaceImpl.previewReferenceTimeMillis

    override fun updateWatchfaceInstance(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ) {
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.updateInstance") {
            if (instance.instanceId != newInstanceId) {
                InteractiveInstanceManager.renameInstance(instance.instanceId, newInstanceId)
                instance.instanceId = newInstanceId
            }
            engine.setUserStyle(userStyle)
            engine.clearComplicationData()
        }
    }

    override fun getInstanceId(): String = instance.instanceId

    override fun release() {
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.release") {
            InteractiveInstanceManager.releaseInstance(instanceId)
        }
    }

    override fun getComplicationDetails() =
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.getComplicationDetails") {
            engine.getComplicationState()
        }

    override fun getUserStyleSchema() =
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.getUserStyleSchema") {
            engine.watchFaceImpl.userStyleRepository.schema.toWireFormat()
        }

    override fun bringAttentionToComplication(id: Int) {
        uiThreadHandler.runOnHandlerWithTracing("WCSApi.bringAttentionToComplication") {
            engine.watchFaceImpl.complicationsManager.displayPressedAnimation(id)
        }
    }
}
