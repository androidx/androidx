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
import android.support.wearable.complications.ComplicationData
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.WatchFaceColors
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * An interactive watch face instance with an API for WCS and SysUI.
 *
 * @param instanceId The system's id for this instance
 * @param uiThreadCoroutineScope A UI thread [CoroutineScope], this must must have the same lifetime
 * as the [WatchFaceService].
 * @param initialComplications The initial complications for this instance, may be empty.
 */
internal class InteractiveWatchFaceImpl(
    internal var instanceId: String,
    private val uiThreadCoroutineScope: CoroutineScope,
    initialComplications: HashMap<Int, ComplicationData>
) : IInteractiveWatchFace.Stub() {
    private companion object {
        private const val TAG = "InteractiveWatchFaceImpl"
    }

    private class PendingUpdateWatchfaceInstance(
        val newInstanceId: String,
        var userStyle: UserStyleWireFormat
    )

    // You can't have non null pendingUpdateWatchfaceInstance and a non null pendingStyleChange
    private var pendingUpdateWatchfaceInstance: PendingUpdateWatchfaceInstance? = null
    private var pendingStyleChange: UserStyleWireFormat? = null

    // Members after this are protected by the lock.
    private val lock = Any()

    /** Protected by [lock]. */
    private val listeners = HashSet<IWatchfaceListener>()
    private var lastWatchFaceColors: WatchFaceColors? = null
    @VisibleForTesting
    internal var complications = initialComplications
    /** This is deprecated, instead we prefer [IWatchfaceListener.onWatchfaceReady]. */
    private var watchfaceReadyListener: IWatchfaceReadyListener? = null
    private var watchFaceReady = false
    private var pendingPreviewImageUpdateRequested: String? = null

    // UiThread: write needs a lock, read doesn't.
    // Other threads: Read access only which needs a lock.
    internal var engine: WatchFaceService.EngineWrapper? = null
    private var watchUiState: WatchUiState? = null

    internal fun onWatchFaceColorsChanged(watchFaceColors: WatchFaceColors?) {
        val listenersCopy = synchronized(lock) {
            lastWatchFaceColors = watchFaceColors
            HashSet<IWatchfaceListener>(listeners)
        }

        listenersCopy.forEach {
            it.onWatchfaceColorsChanged(lastWatchFaceColors?.toWireFormat())
        }
    }

    @UiThread
    fun attachEngine(engine: WatchFaceService.EngineWrapper) {
        Log.d(TAG, "attachEngine id $instanceId")
        require(this@InteractiveWatchFaceImpl.engine == null) {
            "attachEngine failed because ${this@InteractiveWatchFaceImpl.engine} was" +
                " already attached"
        }

        synchronized(lock) {
            this.engine = engine
            watchUiState
        }?.let {
            engine.setWatchUiState(it, fromSysUi = true)
        }
    }

    @UiThread
    fun detachEngine() {
        Log.d(TAG, "detachEngine id $instanceId")
        require(engine != null) { "detachEngine called but engine is null" }

        synchronized(lock) {
            engine = null
            watchFaceReady = false
        }
    }

    /** Fires any watchfaceReadyListener  */
    @UiThread
    fun onWatchFaceReady() {
        val readyListener: IWatchfaceReadyListener?
        val listenersCopy = synchronized(lock) {
            watchFaceReady = true
            readyListener = watchfaceReadyListener
            HashSet(listeners)
        }

        readyListener?.onWatchfaceReady()

        for (listener in listenersCopy) {
            listener.onWatchfaceReady()
        }

        val complicationsCopy = synchronized(lock) {
            HashMap(complications)
        }

        // NB this won't actually block since the watchface is ready.
        runBlocking {
            require(pendingUpdateWatchfaceInstance == null || pendingStyleChange == null)

            // In the case where updateWatchfaceInstance and complications were delivered while the
            // engine was null, we need to load any cached complications and apply updates.
            pendingUpdateWatchfaceInstance?.let {
                updateWatchfaceInstanceImpl(it.newInstanceId, it.userStyle)
                pendingUpdateWatchfaceInstance = null
            }

            // Similarly we need to apply any pending style changes.
            pendingStyleChange?.let {
                engine!!.setUserStyle(it)
            }
        }

        engine!!.setComplications(complicationsCopy)
    }

    override fun getApiVersion() = IInteractiveWatchFace.API_VERSION

    override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
        WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            synchronized(lock) { engine },
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
        val watchFaceIsReadyCopy: Boolean
        val pendingPreviewImageUpdateRequestedCopy: String?
        val colors = synchronized(lock) {
            if (listeners.add(listener)) {
                Log.d(TAG, "addWatchFaceListener $listener")
            } else {
                Log.w(TAG, "addWatchFaceListener $listener failed because its already registered")
                return
            }
            pendingPreviewImageUpdateRequestedCopy = pendingPreviewImageUpdateRequested
            pendingPreviewImageUpdateRequested = null
            watchFaceIsReadyCopy = watchFaceReady
            lastWatchFaceColors
        }

        listener.onWatchfaceColorsChanged(colors?.toWireFormat())

        if (watchFaceIsReadyCopy) {
            listener.onWatchfaceReady()
        }

        if (pendingPreviewImageUpdateRequestedCopy != null) {
            listener.onPreviewImageUpdateRequested(pendingPreviewImageUpdateRequestedCopy)
        }
    }

    override fun removeWatchFaceListener(listener: IWatchfaceListener) {
        synchronized(lock) {
           if (listeners.remove(listener)) {
               Log.d(TAG, "removeWatchFaceListener $listener")
           } else {
               Log.w(TAG, "removeWatchFaceListener $listener failed because it's not registered")
           }
        }
    }

    override fun getWatchFaceOverlayStyle(): WatchFaceOverlayStyleWireFormat? =
        WatchFaceService.awaitDeferredWatchFaceAndComplicationManagerThenRunOnBinderThread(
            synchronized(lock) { engine },
            "InteractiveWatchFaceImpl.getWatchFaceOverlayStyle"
        ) { watchFaceInitDetails ->
            WatchFaceOverlayStyleWireFormat(
                watchFaceInitDetails.watchFace.overlayStyle.backgroundColor,
                watchFaceInitDetails.watchFace.overlayStyle.foregroundColor
            )
        }

    override fun getContentDescriptionLabels(): Array<ContentDescriptionLabel>? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            synchronized(lock) { engine },
            "InteractiveWatchFaceImpl.getContentDescriptionLabels"
        ) { engine?.contentDescriptionLabels }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams): Bundle? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            synchronized(lock) { engine },
            "InteractiveWatchFaceImpl.renderWatchFaceToBitmap"
        ) { watchFaceImpl -> watchFaceImpl.renderWatchFaceToBitmap(params) }
    }

    override fun getPreviewReferenceTimeMillis(): Long {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            synchronized(lock) { engine },
            "InteractiveWatchFaceImpl.getPreviewReferenceTimeMillis"
        ) { watchFaceImpl -> watchFaceImpl.previewReferenceInstant.toEpochMilli() } ?: 0
    }

    override fun setWatchUiState(watchUiState: WatchUiState) {
        val engineCopy = synchronized(lock) {
            this.watchUiState = watchUiState
            engine
        }

        WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engineCopy,
            "InteractiveWatchFaceImpl.setWatchUiState"
        ) {
            engine?.setWatchUiState(watchUiState, fromSysUi = true)
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
                }
                InteractiveInstanceManager.releaseInstance(instanceId)
            }
        }
    }

    override fun updateComplicationData(
        complicationDatumWireFormats: List<IdAndComplicationDataWireFormat>
    ) {
        if ("user" != Build.TYPE) {
            Log.d(TAG, "updateComplicationData " + complicationDatumWireFormats.joinToString())
        }

        synchronized(lock) {
            for (complication in complicationDatumWireFormats) {
                complications[complication.id] = complication.complicationData
            }
        }

        uiThreadCoroutineScope.launch {
            TraceEvent("InteractiveWatchFaceImpl.updateComplicationData").use {
                engine?.setComplications(
                    complicationDatumWireFormats.associateBy(
                        { it.id },
                        { it.complicationData }
                    )
                )
            }
        }
    }

    // This function doesn't use runBlockingWithTracing since that deadlocks some of our tests.
    fun updateStyle(userStyle: UserStyleWireFormat) {
        uiThreadCoroutineScope.launch {
            TraceEvent("InteractiveWatchFaceImpl.updateComplicationData").use {
                val engineCopy = engine
                if (engineCopy != null) {
                    engineCopy.setUserStyle(userStyle)
                } else {
                    // If there's a pending pendingUpdateWatchfaceInstance, then override the
                    // userStyle
                    if (pendingUpdateWatchfaceInstance != null) {
                        pendingUpdateWatchfaceInstance!!.userStyle = userStyle
                    } else {
                        pendingStyleChange = userStyle
                    }
                }
            }
        }
    }

    override fun updateWatchfaceInstance(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ) {
        // If the instance ID has changed we need to clear the complications.
        // NB updateWatchfaceInstanceImpl does this too, but we're doing this here in case the
        // engine is null, which simplifies complication handling in onWatchFaceReady.
        if (newInstanceId != instanceId) {
            synchronized(lock) {
                complications = HashMap()
            }
        }

        /**
         * This is blocking to ensure ordering with respect to any subsequent [getInstanceId] and
         * [getPreviewReferenceTimeMillis] calls.
         */
        uiThreadCoroutineScope.runBlockingWithTracing(
            "InteractiveWatchFaceImpl.updateWatchfaceInstance"
        ) {
            if (engine != null) {
                updateWatchfaceInstanceImpl(newInstanceId, userStyle)
            } else {
                // No engine means one of two things, either the WF is shutting down or we're
                // transitioning to a new engine (very much a corner case). We
                //
                // This is very much a corner case, and we handle it in a best effort way. We don't
                // attempt to
                pendingUpdateWatchfaceInstance =
                    PendingUpdateWatchfaceInstance(newInstanceId, userStyle)

                // We don't need a pendingUpdateWatchfaceInstance and a pendingStyleChange.
                pendingStyleChange = null
            }
        }
    }

    @UiThread
    private suspend fun updateWatchfaceInstanceImpl(
        newInstanceId: String,
        userStyle: UserStyleWireFormat
    ) {
        if (instanceId != newInstanceId) {
            // Complications are tied to the newInstanceId, if it's changed we need to load any
            // cached ones for newInstanceId.
            complications = engine!!.updateInstanceAndLoadCachedComplications(newInstanceId)
            instanceId = newInstanceId
        }
        engine!!.setUserStyle(userStyle)
    }

    override fun getComplicationDetails(): List<IdAndComplicationStateWireFormat>? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            synchronized(lock) { engine },
            "InteractiveWatchFaceImpl.getComplicationDetails"
        ) { watchFaceImpl -> watchFaceImpl.getComplicationState() }
    }

    override fun getUserStyleSchema(): UserStyleSchemaWireFormat? {
        return WatchFaceService.awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            synchronized(lock) { engine },
            "InteractiveWatchFaceImpl.getUserStyleSchema"
        ) { watchFaceImpl -> watchFaceImpl.currentUserStyleRepository.schema.toWireFormat() }
    }

    override fun bringAttentionToComplication(id: Int) {
        // Unsupported.
    }

    override fun addWatchfaceReadyListener(listener: IWatchfaceReadyListener) {
        val ready = synchronized(lock) {
            watchfaceReadyListener = listener
            watchFaceReady
        }
        if (ready) {
            listener.onWatchfaceReady()
        }
    }

    fun sendPreviewImageNeedsUpdateRequest() {
        val listenersCopy = synchronized(lock) {
            if (listeners.isEmpty()) {
                pendingPreviewImageUpdateRequested = instanceId
            }
            HashSet<IWatchfaceListener>(listeners)
        }

        listenersCopy.forEach {
            it.onPreviewImageUpdateRequested(instanceId)
        }
    }

    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("WatchFaceEngine:")
        writer.increaseIndent()
        synchronized(lock) {
            writer.print("engine = $engine")
            writer.print("pendingPreviewImageUpdateRequested = $pendingPreviewImageUpdateRequested")
            writer.print(
                "complications = " + complications.map {
                    "{id = ${it.key}, value = ${it.value}}"
                }.joinToString(",")
            )
            writer.println("listeners = " + listeners.joinToString(", "))
            writer.println("watchfaceReadyListener = $watchfaceReadyListener")
        }
        writer.decreaseIndent()
    }
}
