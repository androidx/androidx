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

import androidx.annotation.RequiresApi
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.WatchFaceImpl
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.runBlockingWithTracing
import androidx.wear.watchface.style.data.UserStyleWireFormat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** An interactive watch face instance with SysUI and WCS facing interfaces.*/
@RequiresApi(27)
internal class InteractiveWatchFaceImpl(
    internal val engine: WatchFaceService.EngineWrapper,
    internal var instanceId: String
) : IInteractiveWatchFace.Stub() {

    override fun getApiVersion() = IInteractiveWatchFace.API_VERSION

    private fun <R> awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
        traceName: String,
        task: (watchFaceImpl: WatchFaceImpl) -> R
    ): R = TraceEvent(traceName).use {
        runBlocking {
            val watchFaceImpl = engine.deferredWatchFaceImpl.await()
            withContext(engine.uiThreadCoroutineScope.coroutineContext) {
                task(watchFaceImpl)
            }
        }
    }

    override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.sendTouchEvent"
        ) { watchFaceImpl -> watchFaceImpl.onTapCommand(tapType, xPos, yPos) }

    override fun getContentDescriptionLabels() =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.getContentDescriptionLabels"
        ) { engine.contentDescriptionLabels }

    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams) =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.renderWatchFaceToBitmap"
        ) { watchFaceImpl -> watchFaceImpl.renderWatchFaceToBitmap(params) }

    override fun getPreviewReferenceTimeMillis() =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.getPreviewReferenceTimeMillis"
        ) { watchFaceImpl -> watchFaceImpl.previewReferenceTimeMillis }

    override fun setWatchUiState(watchUiState: WatchUiState) =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.setWatchUiState"
        ) { engine.setWatchUiState(watchUiState) }

    override fun getInstanceId(): String = instanceId

    override fun ambientTickUpdate() {
        engine.uiThreadCoroutineScope.runBlockingWithTracing(
            "InteractiveWatchFaceImpl.ambientTickUpdate"
        ) { engine.ambientTickUpdate() }
    }

    override fun release() = TraceEvent("InteractiveWatchFaceImpl.release").use {
        runBlocking {
            try {
                engine.deferredWatchFaceImpl.await()
            } catch (e: Exception) {
                // deferredWatchFaceImpl may have completed with an exception. This will have
                // already been reported so we can ignore it.
            }
            withContext(engine.uiThreadCoroutineScope.coroutineContext) {
                InteractiveInstanceManager.releaseInstance(instanceId)
            }
        }
    }

    override fun updateComplicationData(
        complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
    ) = engine.uiThreadCoroutineScope.runBlockingWithTracing(
        "InteractiveWatchFaceImpl.updateComplicationData"
    ) { engine.setComplicationDataList(complicationDatumWireFormats) }

    override fun updateWatchfaceInstance(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ) {
        /**
         * This is blocking to ensure ordering with respect to any subsequent [getInstanceId] and
         * [getPreviewReferenceTimeMillis] calls.
         */
        engine.uiThreadCoroutineScope.runBlockingWithTracing(
            "InteractiveWatchFaceImpl.updateWatchfaceInstance"
        ) {
            if (instanceId != newInstanceId) {
                InteractiveInstanceManager.renameInstance(instanceId, newInstanceId)
                instanceId = newInstanceId
            }
            engine.setUserStyle(userStyle)
            engine.clearComplicationData()
        }
    }

    override fun getComplicationDetails() =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.getComplicationDetails"
        ) { watchFaceImpl -> watchFaceImpl.getComplicationState() }

    override fun getUserStyleSchema() =
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.getUserStyleSchema"
        ) { watchFaceImpl -> watchFaceImpl.currentUserStyleRepository.schema.toWireFormat() }

    override fun bringAttentionToComplication(id: Int) {
        awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            "InteractiveWatchFaceImpl.getUserStyleSchema"
        ) { watchFaceImpl -> watchFaceImpl.complicationsManager.displayPressedAnimation(id) }
    }
}