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
import android.os.IBinder
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.data.WatchFaceOverlayStyleWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.runBlockingWithTracing
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.utility.aidlMethod
import java.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** An interactive watch face instance with SysUI and WCS facing interfaces. */
internal class InteractiveWatchFaceImpl(
    internal var engine: WatchFaceService.EngineWrapper?,
    internal var instanceId: String
) : IInteractiveWatchFace.Stub() {
    private companion object {
        private const val TAG = "InteractiveWatchFaceImpl"
    }

    private val uiThreadCoroutineScope = engine!!.uiThreadCoroutineScope
    private val systemTimeProvider = engine!!.systemTimeProvider

    override fun getApiVersion() =
        aidlMethod(TAG, "getApiVersion") { IInteractiveWatchFace.API_VERSION }

    override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int): Unit =
        aidlMethod(TAG, "sendTouchEvent") {
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
                        Instant.ofEpochMilli(systemTimeProvider.getSystemTimeMillis())
                    )
                )
            }
        }

    override fun unused18() = aidlMethod(TAG, "unused18") {}

    override fun unused20() = aidlMethod(TAG, "unused20") {}

    override fun addWatchFaceListener(listener: IWatchfaceListener): Unit =
        aidlMethod(TAG, "addWatchFaceListener") {
            engine?.addWatchFaceListener(listener)
                ?: Log.w(TAG, "addWatchFaceListener ignored due to null engine")
        }

    override fun removeWatchFaceListener(listener: IWatchfaceListener): Unit =
        aidlMethod(TAG, "removeWatchFaceListener") {
            engine?.removeWatchFaceListener(listener)
                ?: Log.w(TAG, "removeWatchFaceListener ignored due to null engine")
        }

    @Suppress("Deprecation")
    override fun getWatchFaceOverlayStyle(): WatchFaceOverlayStyleWireFormat? =
        aidlMethod(TAG, "getWatchFaceOverlayStyle") {
            WatchFaceService.awaitDeferredWatchFaceThenRunOnUiThread(
                engine,
                "InteractiveWatchFaceImpl.getWatchFaceOverlayStyle"
            ) {
                WatchFaceOverlayStyleWireFormat(
                    it.overlayStyle.backgroundColor,
                    it.overlayStyle.foregroundColor
                )
            }
        }

    override fun getContentDescriptionLabels(): Array<ContentDescriptionLabel>? =
        aidlMethod(TAG, "getContentDescriptionLabels") {
            return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
                engine,
                "InteractiveWatchFaceImpl.getContentDescriptionLabels"
            ) {
                engine?.contentDescriptionLabels
            }
        }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams): Bundle? =
        aidlMethod(TAG, "renderWatchFaceToBitmap") {
            return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
                engine,
                "InteractiveWatchFaceImpl.renderWatchFaceToBitmap"
            ) { watchFaceImpl ->
                watchFaceImpl.renderWatchFaceToBitmap(params)
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun createRemoteWatchFaceView(
        hostToken: IBinder,
        width: Int,
        height: Int
    ): IRemoteWatchFaceView? =
        aidlMethod(TAG, "createRemoteWatchFaceView") {
            return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
                engine,
                "InteractiveWatchFaceImpl.createRemoteWatchFaceView"
            ) { watchFaceImpl ->
                watchFaceImpl.createRemoteWatchFaceView(hostToken, width, height)
            }
        }

    override fun getPreviewReferenceTimeMillis(): Long =
        aidlMethod(TAG, "getPreviewReferenceTimeMillis") {
            return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
                engine,
                "InteractiveWatchFaceImpl.getPreviewReferenceTimeMillis"
            ) { watchFaceImpl ->
                watchFaceImpl.previewReferenceInstant.toEpochMilli()
            } ?: 0
        }

    override fun setWatchUiState(watchUiState: WatchUiState): Unit =
        aidlMethod(TAG, "setWatchUiState") {
            WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
                engine,
                "InteractiveWatchFaceImpl.setWatchUiState"
            ) {
                engine?.let { it.setWatchUiState(watchUiState, fromSysUi = true) }
                    ?: Log.d(TAG, "setWatchUiState ignored due to null engine id $instanceId")
            }
        }

    override fun getInstanceId(): String = aidlMethod(TAG, "getInstanceId") { instanceId }

    override fun ambientTickUpdate(): Unit =
        aidlMethod(TAG, "ambientTickUpdate") {
            uiThreadCoroutineScope.runBlockingWithTracing(
                "InteractiveWatchFaceImpl.ambientTickUpdate"
            ) {
                engine?.ambientTickUpdate()
                    ?: Log.d(TAG, "ambientTickUpdate ignored due to null engine id $instanceId")
            }
        }

    override fun release(): Unit =
        aidlMethod(TAG, "release") {
            TraceEvent("InteractiveWatchFaceImpl.release").use {
                // Note this is a one way method called on a binder thread, so it shouldn't matter
                // if we block.
                runBlocking {
                    try {
                        withContext(uiThreadCoroutineScope.coroutineContext) {
                            engine?.let {
                                it.watchFaceDetails?.deferredWatchFaceImpl?.await()
                                it.unpauseAnimation()
                            }
                            InteractiveInstanceManager.releaseInstance(instanceId)
                        }
                    } catch (e: Exception) {
                        // deferredWatchFaceImpl may have completed with an exception. This will
                        // have already been reported so we can ignore it.
                    }
                }
            }
        }

    override fun updateComplicationData(
        complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
    ): Unit =
        aidlMethod(TAG, "updateComplicationData") {
            TraceEvent("InteractiveWatchFaceImpl.updateComplicationData").use {
                if ("user" != Build.TYPE) {
                    Log.d(
                        TAG,
                        "updateComplicationData " + complicationDatumWireFormats.joinToString()
                    )
                }

                engine?.setComplicationDataList(complicationDatumWireFormats)
                    ?: Log.d(
                        TAG,
                        "updateComplicationData ignored due to null engine id $instanceId"
                    )
            }
        }

    override fun updateWatchfaceInstance(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ): Unit =
        aidlMethod(TAG, "updateWatchfaceInstance") {
            /**
             * This is blocking to ensure ordering with respect to any subsequent [getInstanceId]
             * and [getPreviewReferenceTimeMillis] calls.
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

    override fun updateWatchfaceInstanceSync(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ): Unit =
        aidlMethod(TAG, "updateWatchfaceInstanceSync") {
            /**
             * This is blocking to ensure ordering with respect to any subsequent [getInstanceId]
             * and [getPreviewReferenceTimeMillis] calls.
             */
            uiThreadCoroutineScope.runBlockingWithTracing(
                "InteractiveWatchFaceImpl.updateWatchfaceInstanceSync"
            ) {
                if (instanceId != newInstanceId) {
                    engine?.updateInstance(newInstanceId)
                    instanceId = newInstanceId
                }
                engine?.setUserStyle(userStyle)
            }
        }

    override fun getComplicationDetails(): List<IdAndComplicationStateWireFormat>? =
        aidlMethod(TAG, "getComplicationDetails") {
            val engineCopy = engine
            return WatchFaceService.awaitDeferredEarlyInitDetailsThenRunOnThread(
                engineCopy,
                "InteractiveWatchFaceImpl.getComplicationDetails",
                WatchFaceService.Companion.ExecutionThread.UI
            ) {
                it.complicationSlotsManager.getComplicationsState(engineCopy!!.screenBounds)
            }
        }

    override fun getUserStyleSchema(): UserStyleSchemaWireFormat? =
        aidlMethod(TAG, "getUserStyleSchema") {
            return WatchFaceService.awaitDeferredEarlyInitDetailsThenRunOnThread(
                engine,
                "InteractiveWatchFaceImpl.getUserStyleSchema",
                WatchFaceService.Companion.ExecutionThread.CURRENT
            ) {
                it.userStyleRepository.schema.toWireFormat()
            }
        }

    override fun bringAttentionToComplication(id: Int) =
        aidlMethod(TAG, "bringAttentionToComplication") {
            // Unsupported.
        }

    override fun addWatchfaceReadyListener(listener: IWatchfaceReadyListener): Unit =
        aidlMethod(TAG, "addWatchfaceReadyListener") {
            uiThreadCoroutineScope.launch {
                engine?.addWatchfaceReadyListener(listener)
                    ?: Log.d(
                        TAG,
                        "addWatchfaceReadyListener ignored due to null engine id $instanceId"
                    )
            }
        }

    override fun getComplicationIdAt(xPos: Int, yPos: Int): Long =
        aidlMethod(TAG, "getComplicationIdAt") {
            return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
                engine,
                "InteractiveWatchFaceImpl.getComplicationIdAt"
            ) {
                it.complicationSlotsManager.getComplicationSlotAt(xPos, yPos)?.id?.toLong()
            } ?: Long.MIN_VALUE
        }

    override fun getUserStyleFlavors() =
        aidlMethod(TAG, "getUserStyleFlavors") {
            WatchFaceService.awaitDeferredEarlyInitDetailsThenRunOnThread(
                engine,
                "InteractiveWatchFaceImpl.getUserStyleFlavors",
                WatchFaceService.Companion.ExecutionThread.CURRENT
            ) {
                it.userStyleFlavors.toWireFormat()
            }
        }

    override fun overrideComplicationData(
        complicationDatumWireFormats: List<IdAndComplicationDataWireFormat>
    ): Unit =
        aidlMethod(TAG, "overrideComplicationData") {
            engine?.overrideComplicationsForEditing(
                complicationDatumWireFormats.associateBy(
                    { it.id },
                    { it.complicationData.toApiComplicationData() }
                )
            )
        }

    override fun clearComplicationDataOverride(): Unit =
        aidlMethod(TAG, "overrideComplicationData") { engine?.onEditSessionFinished() }

    override fun pauseAnimation(binder: IBinder): Unit =
        aidlMethod(TAG, "pauseAnimation") { engine?.pauseAnimation(binder) }

    override fun unpauseAnimation(): Unit =
        aidlMethod(TAG, "unpauseAnimation") { engine?.unpauseAnimation() }

    fun onDestroy() {
        // Note this is almost certainly called on the ui thread, from release() above.
        runBlocking {
            try {
                withContext(uiThreadCoroutineScope.coroutineContext) {
                    Log.d(TAG, "onDestroy id $instanceId")
                    engine?.onEngineDetached()
                    engine = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "onDestroy failed to call onEngineDetached", e)
            }
        }
    }
}
