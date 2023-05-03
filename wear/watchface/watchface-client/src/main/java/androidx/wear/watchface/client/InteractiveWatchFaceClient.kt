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

package androidx.wear.watchface.client

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.RemoteException
import android.support.wearable.watchface.SharedMemoryImage
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import androidx.annotation.AnyThread
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.ContentDescriptionLabel
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFaceColors
import androidx.wear.watchface.WatchFaceExperimental
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicy
import androidx.wear.watchface.complications.data.toApiComplicationText
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IWatchfaceListener
import androidx.wear.watchface.control.IWatchfaceReadyListener
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchFaceColorsWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.toApiFormat
import androidx.wear.watchface.utility.TraceEvent
import java.time.Instant
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
@IntDef(value = [DisconnectReasons.ENGINE_DIED, DisconnectReasons.ENGINE_DETACHED])
public annotation class DisconnectReason

/**
 * Disconnect reasons for
 * [InteractiveWatchFaceClient.ClientDisconnectListener.onClientDisconnected].
 */
public object DisconnectReasons {

    /**
     * The underlying engine died, probably because the watch face was killed or crashed. Sometimes
     * this is due to memory pressure and it's not the watch face's fault. Usually in response a new
     * [InteractiveWatchFaceClient] should be created (see
     * [WatchFaceControlClient.getOrCreateInteractiveWatchFaceClient]), however if this new client
     * also disconnects due to [ENGINE_DIED] within a few seconds the watchface is probably bad and
     * it's recommended to switch to a safe system default watch face.
     */
    public const val ENGINE_DIED: Int = 1

    /**
     * Wallpaper service detached from the engine, which is now defunct. The watch face itself has
     * no control over this. Usually in response a new [InteractiveWatchFaceClient] should be
     * created (see [WatchFaceControlClient.getOrCreateInteractiveWatchFaceClient]).
     */
    public const val ENGINE_DETACHED: Int = 2
}

/**
 * Intended for use by watch face editors, a RemoteWatchFaceViewHost allows the watch face to send a
 * [SurfaceControlViewHost.SurfacePackage] to the client, which the client can attach to a
 * [SurfaceView] with [SurfaceView.setChildSurfacePackage]. The client can request an updated screen
 * shot by calling [renderWatchFace].
 */
public interface RemoteWatchFaceViewHost : AutoCloseable {
    /**
     * Renders the watchface into the view associated with [surfacePackage].
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param instant The [Instant] render with.
     * @param userStyle Optional [UserStyle] to render with, if null the current style is used.
     * @param idAndComplicationData Map of complication ids to [ComplicationData] to render with, or
     *   if null then the existing complication data if any is used.
     */
    @Throws(RemoteException::class)
    public fun renderWatchFace(
        renderParameters: RenderParameters,
        instant: Instant,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?,
    )

    /**
     * The [SurfaceControlViewHost.SurfacePackage] the client should attach to a [SurfaceView] via
     * [SurfaceView.setChildSurfacePackage]. The watch face will render into this view when
     * [renderWatchFace] is called.
     */
    val surfacePackage: SurfaceControlViewHost.SurfacePackage
}

/**
 * Controls a stateful remote interactive watch face. Typically this will be used for the current
 * active watch face.
 *
 * Note clients should call [close] when finished.
 */
public interface InteractiveWatchFaceClient : AutoCloseable {

    /**
     * Sends new [ComplicationData] to the watch face. Note this doesn't have to be a full update,
     * it's possible to update just one complication at a time, but doing so may result in a less
     * visually clean transition.
     *
     * @param slotIdToComplicationData The [ComplicationData] for each
     *   [androidx.wear.watchface.ComplicationSlot].
     */
    @Throws(RemoteException::class)
    public fun updateComplicationData(slotIdToComplicationData: Map<Int, ComplicationData>)

    /**
     * Renders the watchface to a shared memory backed [Bitmap] with the given settings. Note this
     * will be fairly slow since either software canvas or glReadPixels will be invoked.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param instant The [Instant] render with.
     * @param userStyle Optional [UserStyle] to render with, if null the current style is used.
     * @param idAndComplicationData Map of complication ids to [ComplicationData] to render with, or
     *   if null then the existing complication data if any is used.
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch face with the
     *   given settings.
     */
    @RequiresApi(27)
    @Throws(RemoteException::class)
    public fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /** Whether or not the watch face supports [RemoteWatchFaceViewHost]. */
    public val isRemoteWatchFaceViewHostSupported: Boolean
        @get:JvmName("isRemoteWatchFaceViewHostSupported") get() = false

    /**
     * Constructs a [RemoteWatchFaceViewHost] whose [RemoteWatchFaceViewHost.surfacePackage] can be
     * attached to a [SurfaceView] owned by the client with [SurfaceView.setChildSurfacePackage].
     * The watch face will render into this view upon demand (see
     * [RemoteWatchFaceViewHost.renderWatchFace]).
     *
     * This is more efficient than calling [renderWatchFaceToBitmap] multiple times, although there
     * is some overhead (memory and cpu) to setting up a RemoteWatchFaceViewHost.
     *
     * Requires the watchface to be compiled with a compatible library, to check if that's the case
     * use [isRemoteWatchFaceViewHostSupported].
     *
     * @param hostToken The return value of [View.getHostToken()]
     * @param width The width of the view in pixels
     * @param height The height of the view in pixels
     * @return The [RemoteWatchFaceViewHost] or null if the client has already been closed or if the
     *   watch face is not compatible.
     */
    @Throws(RemoteException::class)
    @RequiresApi(Build.VERSION_CODES.R)
    public fun createRemoteWatchFaceViewHost(
        hostToken: IBinder,
        @Px width: Int,
        @Px height: Int
    ): RemoteWatchFaceViewHost? = null

    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    @get:Throws(RemoteException::class) public val previewReferenceInstant: Instant

    /**
     * The watchface's [OverlayStyle] which configures the system status overlay on Wear 3.0 and
     * beyond. Note for older watch faces which don't support this, the default value will be
     * returned.
     */
    @get:Throws(RemoteException::class)
    public val overlayStyle: OverlayStyle
        // Default implementation, overridden below.
        get() = OverlayStyle()

    /**
     * Renames this instance to [newInstanceId] (must be unique, usually this would be different
     * from the old ID but that's not a requirement). Sets the current [UserStyle] and clears any
     * complication data. Setting the new UserStyle may have a side effect of enabling or disabling
     * complicationSlots, which will be visible via [ComplicationSlotState.isEnabled].
     *
     * NB [setWatchUiState] and [updateWatchFaceInstance] can be called in any order.
     */
    @Throws(RemoteException::class)
    public fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyle)

    /**
     * Renames this instance to [newInstanceId] (must be unique, usually this would be different
     * from the old ID but that's not a requirement). Sets the current [UserStyle] represented as a
     * [UserStyleData> and clears any complication data. Setting the new UserStyle may have a side effect of enabling or disabling complicationSlots, which will be visible via [ComplicationSlotState.isEnabled].
     */
    @Throws(RemoteException::class)
    public fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyleData)

    /** Returns the ID of this watch face instance. */
    @get:Throws(RemoteException::class) public val instanceId: String

    /** The watch face's [UserStyleSchema]. */
    @get:Throws(RemoteException::class) public val userStyleSchema: UserStyleSchema

    /**
     * Map of [androidx.wear.watchface.ComplicationSlot] ids to [ComplicationSlotState] for each
     * [ComplicationSlot] registered with the watch face's [ComplicationSlotsManager]. The
     * ComplicationSlotState is based on the initial state of each
     * [androidx.wear.watchface.ComplicationSlot] plus any overrides from a
     * [ComplicationSlotsUserStyleSetting]. As a consequence ComplicationSlotState may update based
     * on style changes.
     */
    @get:Throws(RemoteException::class)
    public val complicationSlotsState: Map<Int, ComplicationSlotState>

    /**
     * Returns the ID of the [androidx.wear.watchface.ComplicationSlot] at the given coordinates or
     * `null` if there isn't one.
     *
     * Note this currently doesn't support Edge complications.
     */
    @SuppressWarnings("AutoBoxing")
    @Throws(RemoteException::class)
    public fun getComplicationIdAt(@Px x: Int, @Px y: Int): Int? =
        complicationSlotsState
            .asSequence()
            .firstOrNull {
                it.value.isEnabled &&
                    when (it.value.boundsType) {
                        ComplicationSlotBoundsType.ROUND_RECT -> it.value.bounds.contains(x, y)
                        ComplicationSlotBoundsType.BACKGROUND -> false
                        ComplicationSlotBoundsType.EDGE -> false
                        else -> false
                    }
            }
            ?.key

    public companion object {
        /** Indicates a "down" touch event on the watch face. */
        public const val TAP_TYPE_DOWN: Int = IInteractiveWatchFace.TAP_TYPE_DOWN

        /**
         * Indicates that a previous [TAP_TYPE_DOWN] event has been canceled. This generally happens
         * when the watch face is touched but then a move or long press occurs.
         */
        public const val TAP_TYPE_CANCEL: Int = IInteractiveWatchFace.TAP_TYPE_CANCEL

        /**
         * Indicates that an "up" event on the watch face has occurred that has not been consumed by
         * another activity. A [TAP_TYPE_DOWN] always occur first. This event will not occur if a
         * [TAP_TYPE_CANCEL] is sent.
         */
        public const val TAP_TYPE_UP: Int = IInteractiveWatchFace.TAP_TYPE_UP
    }

    /**
     * Sends a tap event to the watch face for processing.
     *
     * @param xPosition The x-coordinate of the tap in pixels
     * @param yPosition The y-coordinate of the tap in pixels
     * @param tapType The [TapType] of the event
     */
    @Throws(RemoteException::class)
    public fun sendTouchEvent(@Px xPosition: Int, @Px yPosition: Int, @TapType tapType: Int)

    /**
     * Returns the [ContentDescriptionLabel]s describing the watch face, for the use by screen
     * readers.
     */
    @get:Throws(RemoteException::class)
    public val contentDescriptionLabels: List<ContentDescriptionLabel>

    /**
     * Updates the watch faces [WatchUiState]. NB [setWatchUiState] and [updateWatchFaceInstance]
     * can be called in any order.
     */
    @Throws(RemoteException::class)
    public fun setWatchUiState(watchUiState: androidx.wear.watchface.client.WatchUiState)

    /** Triggers watch face rendering into the surface when in ambient mode. */
    @Throws(RemoteException::class) public fun performAmbientTick()

    /**
     * Callback that observes when the client disconnects. Use [addClientDisconnectListener] to
     * register a ClientDisconnectListener.
     */
    @JvmDefaultWithCompatibility
    public interface ClientDisconnectListener {
        /**
         * The client disconnected, typically due to the server side crashing. Note this is not
         * called in response to [close] being called on [InteractiveWatchFaceClient].
         */
        @Deprecated(
            "Deprecated, use an overload that passes the disconnectReason",
            ReplaceWith("onClientDisconnected(Int)")
        )
        public fun onClientDisconnected() {}

        /** The client disconnected, due to [disconnectReason]. */
        public fun onClientDisconnected(@DisconnectReason disconnectReason: Int) {
            @Suppress("DEPRECATION") onClientDisconnected()
        }
    }

    /** Registers a [ClientDisconnectListener]. */
    @AnyThread
    public fun addClientDisconnectListener(listener: ClientDisconnectListener, executor: Executor)

    /**
     * Removes a [ClientDisconnectListener] previously registered by [addClientDisconnectListener].
     */
    @AnyThread public fun removeClientDisconnectListener(listener: ClientDisconnectListener)

    /** Returns true if the connection to the server side is alive. */
    @AnyThread public fun isConnectionAlive(): Boolean

    /**
     * Interface passed to [addOnWatchFaceReadyListener] which calls
     * [OnWatchFaceReadyListener.onWatchFaceReady] when the watch face is ready to render. Use
     * [addOnWatchFaceReadyListener] to register a OnWatchFaceReadyListener.
     */
    public fun interface OnWatchFaceReadyListener {
        /**
         * Called when the watchface is ready to render.
         *
         * Note in the event of the watch face disconnecting (e.g. due to a crash) this callback
         * will never fire. Use [ClientDisconnectListener] to observe disconnects.
         */
        public fun onWatchFaceReady()
    }

    /**
     * Registers a [OnWatchFaceReadyListener] which gets called when the watch face is ready to
     * render.
     *
     * Note in the event of the watch face disconnecting (e.g. due to a crash) the listener will
     * never get called. Use [ClientDisconnectListener] to observe disconnects.
     *
     * @param executor The [Executor] on which to run [OnWatchFaceReadyListener].
     * @param listener The [OnWatchFaceReadyListener] to run when the watchface is ready to render.
     */
    public fun addOnWatchFaceReadyListener(executor: Executor, listener: OnWatchFaceReadyListener)

    /** Stops listening for events registered by [addOnWatchFaceReadyListener]. */
    public fun removeOnWatchFaceReadyListener(listener: OnWatchFaceReadyListener)

    /**
     * Registers a [Consumer] which gets called initially with the current
     * [Renderer.watchfaceColors] if known or `null` if not, and subsequently whenever the watch
     * face's [Renderer.watchfaceColors] change.
     *
     * @param executor The [Executor] on which to run [listener].
     * @param listener The [Consumer] to run whenever the watch face's [Renderer.watchfaceColors]
     *   change.
     */
    @OptIn(WatchFaceExperimental::class)
    @WatchFaceClientExperimental
    public fun addOnWatchFaceColorsListener(
        executor: Executor,
        listener: Consumer<WatchFaceColors?>
    ) {}

    /** Stops listening for events registered by [addOnWatchFaceColorsListener]. */
    @OptIn(WatchFaceExperimental::class)
    @WatchFaceClientExperimental
    public fun removeOnWatchFaceColorsListener(listener: Consumer<WatchFaceColors?>) {}

    /**
     * Whether or not the watch face supports [ComplicationDisplayPolicy]. If it doesn't then the
     * client is responsible for emulating it by observing the state of the keyguard and sending
     * NoData complications when the device becomes locked and subsequently restoring them when it
     * becomes unlocked for affected complications.
     */
    public fun isComplicationDisplayPolicySupported() = false
}

/** Controls a stateful remote interactive watch face. */
@OptIn(WatchFaceExperimental::class)
internal class InteractiveWatchFaceClientImpl
internal constructor(
    private val iInteractiveWatchFace: IInteractiveWatchFace,
    private val previewImageUpdateRequestedExecutor: Executor?,
    private val previewImageUpdateRequestedListener: Consumer<String>?
) : InteractiveWatchFaceClient {

    private val lock = Any()
    private val disconnectListeners =
        HashMap<InteractiveWatchFaceClient.ClientDisconnectListener, Executor>()
    private val readyListeners =
        HashMap<InteractiveWatchFaceClient.OnWatchFaceReadyListener, Executor>()
    private val watchFaceColorsChangeListeners = HashMap<Consumer<WatchFaceColors?>, Executor>()
    private var watchFaceReady = false
    private var watchfaceReadyListenerRegistered = false
    private var lastWatchFaceColors: WatchFaceColors? = null
    private var disconnectReason: Int? = null
    private var closed = false

    private val iWatchFaceListener =
        object : IWatchfaceListener.Stub() {
            override fun getApiVersion() = IWatchfaceListener.API_VERSION

            override fun onWatchfaceReady() {
                this@InteractiveWatchFaceClientImpl.onWatchFaceReady()
            }

            override fun onWatchfaceColorsChanged(watchFaceColors: WatchFaceColorsWireFormat?) {
                var listenerCopy: HashMap<Consumer<WatchFaceColors?>, Executor>

                synchronized(lock) {
                    listenerCopy = HashMap(watchFaceColorsChangeListeners)
                    lastWatchFaceColors = watchFaceColors?.toApiFormat()
                }

                for ((listener, executor) in listenerCopy) {
                    executor.execute { listener.accept(lastWatchFaceColors) }
                }
            }

            override fun onPreviewImageUpdateRequested(watchFaceId: String) {
                previewImageUpdateRequestedExecutor?.execute {
                    previewImageUpdateRequestedListener!!.accept(watchFaceId)
                }
            }

            override fun onEngineDetached() {
                sendDisconnectNotification(DisconnectReasons.ENGINE_DETACHED)
            }
        }

    init {
        iInteractiveWatchFace
            .asBinder()
            .linkToDeath({ sendDisconnectNotification(DisconnectReasons.ENGINE_DIED) }, 0)

        if (iInteractiveWatchFace.apiVersion >= 6) {
            iInteractiveWatchFace.addWatchFaceListener(iWatchFaceListener)
        }
    }

    internal fun sendDisconnectNotification(reason: Int) {
        val listenersCopy =
            synchronized(lock) {
                // Don't send more than one notification.
                if (disconnectReason != null) {
                    return
                }
                disconnectReason = reason
                HashMap(disconnectListeners)
            }
        for ((listener, executor) in listenersCopy) {
            executor.execute { listener.onClientDisconnected(reason) }
        }
    }

    override fun updateComplicationData(slotIdToComplicationData: Map<Int, ComplicationData>) =
        TraceEvent("InteractiveWatchFaceClientImpl.updateComplicationData").use {
            iInteractiveWatchFace.updateComplicationData(
                slotIdToComplicationData.map {
                    IdAndComplicationDataWireFormat(it.key, it.value.asWireComplicationData())
                }
            )
        }

    @RequiresApi(27)
    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap =
        TraceEvent("InteractiveWatchFaceClientImpl.renderWatchFaceToBitmap").use {
            SharedMemoryImage.ashmemReadImageBundle(
                iInteractiveWatchFace.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        renderParameters.toWireFormat(),
                        instant.toEpochMilli(),
                        userStyle?.toWireFormat(),
                        idAndComplicationData?.map {
                            IdAndComplicationDataWireFormat(
                                it.key,
                                it.value.asWireComplicationData()
                            )
                        }
                    )
                )
            )
        }

    override val isRemoteWatchFaceViewHostSupported = iInteractiveWatchFace.apiVersion >= 9

    @RequiresApi(Build.VERSION_CODES.R)
    override fun createRemoteWatchFaceViewHost(
        hostToken: IBinder,
        @Px width: Int,
        @Px height: Int
    ): RemoteWatchFaceViewHost? {
        if (iInteractiveWatchFace.apiVersion < 8) {
            throw UnsupportedOperationException()
        }
        val remoteWatchFaceView =
            iInteractiveWatchFace.createRemoteWatchFaceView(hostToken, width, height) ?: return null
        return object : RemoteWatchFaceViewHost {
            override fun renderWatchFace(
                renderParameters: RenderParameters,
                instant: Instant,
                userStyle: UserStyle?,
                idAndComplicationData: Map<Int, ComplicationData>?
            ) {
                remoteWatchFaceView.renderWatchFace(
                    WatchFaceRenderParams(
                        renderParameters.toWireFormat(),
                        instant.toEpochMilli(),
                        userStyle?.toWireFormat(),
                        idAndComplicationData?.map {
                            IdAndComplicationDataWireFormat(
                                it.key,
                                it.value.asWireComplicationData()
                            )
                        }
                    )
                )
            }

            override val surfacePackage: SurfaceControlViewHost.SurfacePackage
                get() = remoteWatchFaceView.surfacePackage

            override fun close() {
                remoteWatchFaceView.close()
            }
        }
    }

    override val previewReferenceInstant: Instant
        get() = Instant.ofEpochMilli(iInteractiveWatchFace.previewReferenceTimeMillis)

    override val overlayStyle: OverlayStyle
        get() {
            if (iInteractiveWatchFace.apiVersion >= 4) {
                iInteractiveWatchFace.watchFaceOverlayStyle?.let {
                    return OverlayStyle(it.backgroundColor, it.foregroundColor)
                }
            }
            return OverlayStyle(null, null)
        }

    override fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyle) =
        TraceEvent("InteractiveWatchFaceClientImpl.updateInstance").use {
            iInteractiveWatchFace.updateWatchfaceInstance(newInstanceId, userStyle.toWireFormat())
        }

    override fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyleData) =
        TraceEvent("InteractiveWatchFaceClientImpl.updateInstance").use {
            iInteractiveWatchFace.updateWatchfaceInstance(newInstanceId, userStyle.toWireFormat())
        }

    override val instanceId: String
        get() = iInteractiveWatchFace.instanceId

    override val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iInteractiveWatchFace.userStyleSchema)

    override val complicationSlotsState: Map<Int, ComplicationSlotState>
        get() =
            iInteractiveWatchFace.complicationDetails.associateBy(
                { it.id },
                { ComplicationSlotState(it.complicationState) }
            )

    override fun close() =
        TraceEvent("InteractiveWatchFaceClientImpl.close").use {
            if (iInteractiveWatchFace.apiVersion >= 6) {
                iInteractiveWatchFace.removeWatchFaceListener(iWatchFaceListener)
            }
            iInteractiveWatchFace.release()
            synchronized(lock) { closed = true }
        }

    override fun sendTouchEvent(xPosition: Int, yPosition: Int, @TapType tapType: Int) =
        TraceEvent("InteractiveWatchFaceClientImpl.sendTouchEvent").use {
            iInteractiveWatchFace.sendTouchEvent(xPosition, yPosition, tapType)
        }

    override val contentDescriptionLabels: List<ContentDescriptionLabel>
        get() =
            iInteractiveWatchFace.contentDescriptionLabels?.map {
                ContentDescriptionLabel(it.text.toApiComplicationText(), it.bounds, it.tapAction)
            }
                ?: emptyList()

    override fun setWatchUiState(watchUiState: androidx.wear.watchface.client.WatchUiState) =
        TraceEvent("InteractiveWatchFaceClientImpl.setSystemState").use {
            iInteractiveWatchFace.setWatchUiState(
                WatchUiState(watchUiState.inAmbientMode, watchUiState.interruptionFilter)
            )
        }

    override fun performAmbientTick() =
        TraceEvent("InteractiveWatchFaceClientImpl.performAmbientTick").use {
            iInteractiveWatchFace.ambientTickUpdate()
        }

    override fun addClientDisconnectListener(
        listener: InteractiveWatchFaceClient.ClientDisconnectListener,
        executor: Executor
    ) {
        val disconnectReasonCopy =
            synchronized(lock) {
                require(!disconnectListeners.contains(listener)) {
                    "Don't call addClientDisconnectListener multiple times for the same listener"
                }
                disconnectListeners.put(listener, executor)
                disconnectReason
            }
        disconnectReasonCopy?.let { listener.onClientDisconnected(it) }
    }

    override fun removeClientDisconnectListener(
        listener: InteractiveWatchFaceClient.ClientDisconnectListener
    ) {
        synchronized(lock) { disconnectListeners.remove(listener) }
    }

    override fun isConnectionAlive() =
        iInteractiveWatchFace.asBinder().isBinderAlive && synchronized(lock) { !closed }

    private fun maybeRegisterWatchfaceReadyListener() {
        if (watchfaceReadyListenerRegistered) {
            return
        }
        when {
            // From version 6 we want to use IWatchFaceListener instead.
            iInteractiveWatchFace.apiVersion >= 6 -> return
            iInteractiveWatchFace.apiVersion >= 2 -> {
                iInteractiveWatchFace.addWatchfaceReadyListener(
                    object : IWatchfaceReadyListener.Stub() {
                        override fun getApiVersion(): Int = IWatchfaceReadyListener.API_VERSION

                        override fun onWatchfaceReady() {
                            this@InteractiveWatchFaceClientImpl.onWatchFaceReady()
                        }
                    }
                )
            }
            else -> {
                // We can emulate this on an earlier API by using a call to get userStyleSchema that
                // will block until the watch face is ready. to Avoid blocking the current thread we
                // spin up a temporary thread.
                val thread = HandlerThread("addWatchFaceReadyListener")
                thread.start()
                val handler = Handler(thread.looper)
                handler.post {
                    iInteractiveWatchFace.userStyleSchema
                    this@InteractiveWatchFaceClientImpl.onWatchFaceReady()
                    thread.quitSafely()
                }
            }
        }
        watchfaceReadyListenerRegistered = true
    }

    internal fun onWatchFaceReady() {
        var listenerCopy: HashMap<InteractiveWatchFaceClient.OnWatchFaceReadyListener, Executor>

        synchronized(lock) {
            listenerCopy = HashMap(readyListeners)
            watchFaceReady = true
        }

        for ((listener, executor) in listenerCopy) {
            executor.execute { listener.onWatchFaceReady() }
        }
    }

    override fun addOnWatchFaceReadyListener(
        executor: Executor,
        listener: InteractiveWatchFaceClient.OnWatchFaceReadyListener
    ) {
        synchronized(lock) {
            if (watchFaceReady) {
                executor.execute { listener.onWatchFaceReady() }
                return
            }

            require(!readyListeners.contains(listener)) {
                "Don't call addWatchFaceReadyListener multiple times for the same listener"
            }
            maybeRegisterWatchfaceReadyListener()
            readyListeners.put(listener, executor)
        }
    }

    override fun removeOnWatchFaceReadyListener(
        listener: InteractiveWatchFaceClient.OnWatchFaceReadyListener
    ) {
        synchronized(lock) { readyListeners.remove(listener) }
    }

    @WatchFaceClientExperimental
    override fun addOnWatchFaceColorsListener(
        executor: Executor,
        listener: Consumer<WatchFaceColors?>
    ) {
        val colors =
            synchronized(lock) {
                require(!watchFaceColorsChangeListeners.contains(listener)) {
                    "Don't call addOnWatchFaceColorsListener multiple times for the same listener"
                }
                maybeRegisterWatchfaceReadyListener()
                watchFaceColorsChangeListeners.put(listener, executor)

                lastWatchFaceColors
            }

        listener.accept(colors)
    }

    @WatchFaceClientExperimental
    override fun removeOnWatchFaceColorsListener(listener: Consumer<WatchFaceColors?>) {
        synchronized(lock) { watchFaceColorsChangeListeners.remove(listener) }
    }

    override fun getComplicationIdAt(@Px x: Int, @Px y: Int): Int? =
        TraceEvent("getComplicationIdAt").use {
            if (iInteractiveWatchFace.apiVersion >= 7) {
                val longId = iInteractiveWatchFace.getComplicationIdAt(x, y)
                if (longId == Long.MIN_VALUE) {
                    null
                } else {
                    longId.toInt()
                }
            } else {
                complicationSlotsState
                    .asSequence()
                    .firstOrNull {
                        it.value.isEnabled &&
                            when (it.value.boundsType) {
                                ComplicationSlotBoundsType.ROUND_RECT ->
                                    it.value.bounds.contains(x, y)
                                ComplicationSlotBoundsType.BACKGROUND -> false
                                ComplicationSlotBoundsType.EDGE -> false
                                else -> false
                            }
                    }
                    ?.key
            }
        }

    override fun isComplicationDisplayPolicySupported() = iInteractiveWatchFace.apiVersion >= 8
}
