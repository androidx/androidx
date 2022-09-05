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

import android.os.Build
import android.os.Bundle
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.data.WatchFaceOverlayStyleWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.runBlockingWithTracing
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import kotlinx.coroutines.launch
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** An interactive watch face instance with SysUI and WCS facing interfaces.*/
internal class InteractiveWatchFaceImpl(
    internal var engine: WatchFaceService.EngineWrapper?,
    internal var instanceId: String
) : IInteractiveWatchFace.Stub() {
    private companion object {
        private const val TAG = "InteractiveWatchFaceImpl"
    }

    private val uiThreadCoroutineScope = engine!!.uiThreadCoroutineScope

    override fun getApiVersion() = IInteractiveWatchFace.API_VERSION

    override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
        val engineCopy = engine ?: return
        WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engineCopy,
            "InteractiveWatchFaceImpl.sendTouchEvent"
        ) { watchFaceImpl ->
            watchFaceImpl.onTapCommand(
                tapType,
                TapEvent(
                    xPos,
                    yPos,
                    Instant.ofEpochMilli(
                        watchFaceImpl.systemTimeProvider.getSystemTimeMillis()
                    )
                )
            )
        }
    }

    override fun unused18() {}

    override fun unused20() {}

    override fun addWatchFaceListener(listener: IWatchfaceListener) {
        engine?.addWatchFaceListener(listener) ?: Log.w(
            TAG,
            "addWatchFaceListener ignored due to null engine"
        )
    }

    override fun removeWatchFaceListener(listener: IWatchfaceListener) {
        engine?.removeWatchFaceListener(listener) ?: Log.w(
            TAG,
            "removeWatchFaceListener ignored due to null engine"
        )
    }

    override fun getWatchFaceOverlayStyle(): WatchFaceOverlayStyleWireFormat? =
        WatchFaceService.awaitDeferredWatchFaceThenRunOnBinderThread(
            engine,
            "InteractiveWatchFaceImpl.getWatchFaceOverlayStyle"
        ) { WatchFaceOverlayStyleWireFormat(
                it.overlayStyle.backgroundColor,
                it.overlayStyle.foregroundColor
            )
        }

    override fun getContentDescriptionLabels(): Array<ContentDescriptionLabel>? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engine,
            "InteractiveWatchFaceImpl.getContentDescriptionLabels"
        ) { engine?.contentDescriptionLabels }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams): Bundle? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engine,
            "InteractiveWatchFaceImpl.renderWatchFaceToBitmap"
        ) { watchFaceImpl -> watchFaceImpl.renderWatchFaceToBitmap(params) }
    }

    override fun getPreviewReferenceTimeMillis(): Long {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engine,
            "InteractiveWatchFaceImpl.getPreviewReferenceTimeMillis"
        ) { watchFaceImpl -> watchFaceImpl.previewReferenceInstant.toEpochMilli() } ?: 0
    }

    override fun setWatchUiState(watchUiState: WatchUiState) {
        WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engine,
            "InteractiveWatchFaceImpl.setWatchUiState"
        ) {
            engine?.let {
                it.setWatchUiState(watchUiState, fromSysUi = true)
            } ?: Log.d(TAG, "setWatchUiState ignored due to null engine id $instanceId")
        }
    }

    override fun getInstanceId(): String = instanceId

    override fun ambientTickUpdate() {
        uiThreadCoroutineScope.runBlockingWithTracing(
            "InteractiveWatchFaceImpl.ambientTickUpdate"
        ) {
            engine?.ambientTickUpdate()
                ?: Log.d(TAG, "ambientTickUpdate ignored due to null engine id $instanceId")
        }
    }

    override fun release(): Unit = TraceEvent("InteractiveWatchFaceImpl.release").use {
        // Note this is a one way method called on a binder thread, so it shouldn't matter if we
        // block.
        runBlocking {
            withContext(uiThreadCoroutineScope.coroutineContext) {
                engine?.let {
                    try {
                        it.deferredWatchFaceImpl.await()
                    } catch (e: Exception) {
                        // deferredWatchFaceImpl may have completed with an exception. This will
                        // have already been reported so we can ignore it.
                    }
                    InteractiveInstanceManager.releaseInstance(instanceId)
                }
            }
        }
    }

    override fun updateComplicationData(
        complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
    ): Unit = uiThreadCoroutineScope.runBlockingWithTracing(
        "InteractiveWatchFaceImpl.updateComplicationData"
    ) {
        if ("user" != Build.TYPE) {
            Log.d(TAG, "updateComplicationData " + complicationDatumWireFormats.joinToString())
        }

        engine?.setComplicationDataList(complicationDatumWireFormats)
            ?: Log.d(TAG, "updateComplicationData ignored due to null engine id $instanceId")
    }

    override fun updateWatchfaceInstance(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ) {
        /**
         * This is blocking to ensure ordering with respect to any subsequent [getInstanceId] and
         * [getPreviewReferenceTimeMillis] calls.
         */
        uiThreadCoroutineScope.runBlockingWithTracing(
            "InteractiveWatchFaceImpl.updateWatchfaceInstance"
        ) {
            if (instanceId != newInstanceId) {
                engine?.updateInstance(newInstanceId)
                instanceId = newInstanceId
            }
            engine?.setUserStyle(userStyle)
        }
    }

    override fun getComplicationDetails(): List<IdAndComplicationStateWireFormat>? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engine,
            "InteractiveWatchFaceImpl.getComplicationDetails"
        ) { it.complicationSlotsManager.getComplicationsState(it.renderer.screenBounds) }
    }

    override fun getUserStyleSchema(): UserStyleSchemaWireFormat? {
        return WatchFaceService.awaitDeferredEarlyInitDetailsThenRunOnBinderThread(
            engine,
            "InteractiveWatchFaceImpl.getUserStyleSchema"
        ) { it.userStyleRepository.schema.toWireFormat() }
    }

    override fun bringAttentionToComplication(id: Int) {
        // Unsupported.
    }

    override fun addWatchfaceReadyListener(listener: IWatchfaceReadyListener) {
        uiThreadCoroutineScope.launch {
            engine?.addWatchfaceReadyListener(listener)
                ?: Log.d(TAG, "addWatchfaceReadyListener ignored due to null engine id $instanceId")
        }
    }

    fun onDestroy() {
        // Note this is almost certainly called on the ui thread, from release() above.
        runBlocking {
            withContext(uiThreadCoroutineScope.coroutineContext) {
                Log.d(TAG, "onDestroy id $instanceId")
                engine = null
            }
        }
    }
}
