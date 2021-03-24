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

package androidx.wear.watchface

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.RemoteException
import android.os.Trace
import android.service.wallpaper.WallpaperService
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.SharedMemoryImage
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.complications.SystemProviders.ProviderId
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.toApiComplicationData
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.control.HeadlessWatchFaceImpl
import androidx.wear.watchface.control.IInteractiveWatchFaceSysUI
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.InteractiveWatchFaceImpl
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.editor.EditorService
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.data.UserStyleWireFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch

/** The wire format for [ComplicationData]. */
internal typealias WireComplicationData = android.support.wearable.complications.ComplicationData

/**
 * After user code finishes, we need up to 100ms of wake lock holding for the drawing to occur. This
 * isn't the ideal approach, but the framework doesn't expose a callback that would tell us when our
 * Canvas was drawn. 100 ms should give us time for a few frames to be drawn, in case there is a
 * backlog. If we encounter issues with this approach, we should consider asking framework team to
 * expose a callback.
 */
internal const val SURFACE_DRAW_TIMEOUT_MS = 100L

/** @hide */
@IntDef(
    value = [
        TapType.DOWN,
        TapType.UP,
        TapType.CANCEL
    ]
)
public annotation class TapType {
    public companion object {
        /**
         * Used in [WatchFaceImpl#onTapCommand] to indicate a "down" touch event on the watch face.
         */
        public const val DOWN: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_DOWN

        /**
         * Used in [WatchFaceImpl#onTapCommand] to indicate that a previous [TapType.DOWN] touch
         * event has been canceled. This generally happens when the watch face is touched but then a
         * move or long press occurs.
         */
        public const val CANCEL: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_CANCEL

        /**
         * Used in [WatchFaceImpl#onTapCommand] to indicate that an "up" event on the watch face has
         * occurred that has not been consumed by another activity. A [TapType.DOWN] will always
         * occur first. This event will not occur if a [TapType.CANCEL] is sent.
         */
        public const val UP: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_UP
    }
}

private class PendingComplicationData(val complicationId: Int, val data: ComplicationData)

/**
 * WatchFaceService and [WatchFace] are a pair of classes intended to handle much of
 * the boilerplate needed to implement a watch face without being too opinionated. The suggested
 * structure of a WatchFaceService based watch face is:
 *
 * @sample androidx.wear.watchface.samples.kDocCreateExampleWatchFaceService
 *
 * Sub classes of WatchFaceService are expected to implement [createWatchFace] which is the
 * factory for making [WatchFace]s. All [Complication]s are assumed to be enumerated up upfront and
 * passed as a collection into [ComplicationsManager]'s constructor which is in turn passed to
 * [WatchFace]'s constructor. Complications can be enabled and disabled via [UserStyleSetting
 * .ComplicationsUserStyleSetting].
 *
 * Watch face styling (color and visual look of watch face elements such as numeric fonts, watch
 * hands and ticks, etc...) is directly supported via [UserStyleSetting] and
 * [UserStyleRepository].
 *
 * To aid debugging watch face animations, WatchFaceService allows you to speed up or slow down
 * time, and to loop between two instants.  This is controlled by MOCK_TIME_INTENT intents
 * with a float extra called "androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIE" and to long
 * extras called "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MIN_TIME" and
 * "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MAX_TIME" (which are UTC time in milliseconds).
 * If minTime is omitted or set to -1 then the current time is sampled as minTime.
 *
 * E.g, to make time go twice as fast:
 *  adb shell am broadcast -a androidx.wear.watchface.MockTime \
 *            --ef androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIER 2.0
 *
 *
 * To use the default watch face configuration UI add the following into your watch face's
 * AndroidManifest.xml:
 *
 * ```
 * <activity
 *   android:name="androidx.wear.watchface.ui.WatchFaceConfigActivity"
 *   android:exported="true"
 *   android:directBootAware="true"
 *   android:label="Config"
 *   android:theme="@android:style/Theme.Translucent.NoTitleBar">
 *   <intent-filter>
 *     <action android:name="com.google.android.clockwork.watchfaces.complication.CONFIG_DIGITAL" />
 *       <category android:name=
 *            "com.google.android.wearable.watchface.category.WEARABLE_CONFIGURATION" />
 *       <category android:name="android.intent.category.DEFAULT" />
 *    </intent-filter>
 * </activity>
 * ```
 *
 * To register a WatchFaceService with the system add a <service> tag to the <application> in your
 * watch face's AndroidManifest.xml:
 *
 * ```
 *  <service
 *    android:name=".MyWatchFaceServiceClass"
 *    android:exported="true"
 *    android:label="@string/watch_face_name"
 *    android:permission="android.permission.BIND_WALLPAPER">
 *    <intent-filter>
 *      <action android:name="android.service.wallpaper.WallpaperService" />
 *      <category android:name="com.google.android.wearable.watchface.category.WATCH_FACE" />
 *    </intent-filter>
 *    <meta-data
 *       android:name="com.google.android.wearable.watchface.preview"
 *       android:resource="@drawable/my_watch_preview" />
 *    <meta-data
 *      android:name="com.google.android.wearable.watchface.preview_circular"
 *      android:resource="@drawable/my_watch_circular_preview" />
 *    <meta-data
 *      android:name="com.google.android.wearable.watchface.wearableConfigurationAction"
 *      android:value="com.google.android.clockwork.watchfaces.complication.CONFIG_DIGITAL"/>
 *    <meta-data
 *      android:name="android.service.wallpaper"
 *      android:resource="@xml/watch_face" />
 *  </service>
 * ```
 *
 * Multiple watch faces can be defined in the same package, requiring multiple <service> tags.
 */
public abstract class WatchFaceService : WallpaperService() {

    /** @hide */
    private companion object {
        private const val TAG = "WatchFaceService"

        /** Whether to log every frame. */
        private const val LOG_VERBOSE = false

        /** Whether to enable tracing for each call to [Engine.onDraw]. */
        private const val TRACE_DRAW = false

        // Reference time for editor screenshots for analog watch faces.
        // 2020/10/10 at 09:30 Note the date doesn't matter, only the hour.
        private const val ANALOG_WATCHFACE_REFERENCE_TIME_MS = 1602318600000L

        // Reference time for editor screenshots for digital watch faces.
        // 2020/10/10 at 10:10 Note the date doesn't matter, only the hour.
        private const val DIGITAL_WATCHFACE_REFERENCE_TIME_MS = 1602321000000L

        // Filename for persisted preferences to be used in a direct boot scenario.
        private const val DIRECT_BOOT_PREFS = "directboot.prefs"
    }

    /**
     * Override this factory method to create your WatchFaceImpl. This method will be called by the
     * library on the UiThread. If possible any expensive initialization should be done on a
     * background thread.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @return A [WatchFace] whose [Renderer] uses the provided [surfaceHolder].
     */
    @UiThread
    protected abstract suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState
    ): WatchFace

    /** Creates an interactive engine for WallpaperService. */
    final override fun onCreateEngine(): Engine = EngineWrapper(getHandler(), false)

    /** Creates a headless engine. */
    internal fun createHeadlessEngine(): Engine = EngineWrapper(getHandler(), true)

    /** This is open to allow mocking. */
    internal open fun getHandler() = Handler(Looper.getMainLooper())

    /** This is open to allow mocking. */
    internal open fun getMutableWatchState() = MutableWatchState()

    /** This is open for use by tests. */
    internal open fun allowWatchFaceToAnimate() = true

    /**
     * Whether or not the pre R style init flow (SET_BINDER wallpaper command) is expected.
     * This is open for use by tests.
     */
    internal open fun expectPreRInitFlow() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    /**
     * This is open for use by tests, it allows them to inject a custom [SurfaceHolder].
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getWallpaperSurfaceHolderOverride(): SurfaceHolder? = null

    internal fun setContext(context: Context) {
        attachBaseContext(context)
    }

    internal open fun readDirectBootPrefs(
        context: Context,
        fileName: String
    ): WallpaperInteractiveWatchFaceInstanceParams? = TraceEvent(
        "WatchFaceService.readDirectBootPrefs"
    ).use {
        try {
            val reader = context.openFileInput(fileName)
            val result =
                ParcelUtils.fromInputStream<WallpaperInteractiveWatchFaceInstanceParams>(reader)
            reader.close()
            result
        } catch (e: FileNotFoundException) {
            null
        }
    }

    internal open fun writeDirectBootPrefs(
        context: Context,
        fileName: String,
        prefs: WallpaperInteractiveWatchFaceInstanceParams
    ): Unit = TraceEvent("WatchFaceService.writeDirectBootPrefs").use {
        val writer = context.openFileOutput(fileName, Context.MODE_PRIVATE)
        ParcelUtils.toOutputStream(prefs, writer)
        writer.close()
    }

    internal inner class EngineWrapper(
        private val uiThreadHandler: Handler,
        headless: Boolean
    ) : WallpaperService.Engine(), WatchFaceHostApi {
        internal val coroutineScope = CoroutineScope(getHandler().asCoroutineDispatcher().immediate)
        private val _context = this@WatchFaceService as Context

        internal lateinit var iWatchFaceService: IWatchFaceService
        internal lateinit var watchFaceImpl: WatchFaceImpl

        internal val mutableWatchState = getMutableWatchState().apply {
            isVisible.value = this@EngineWrapper.isVisible
            // Watch faces with the old [onSetBinder] init flow don't know whether the system
            // is ambient until they have received a background action wallpaper command.
            // That's supposed to get sent very quickly, but in case it doesn't we initially
            // assume we're not in ambient mode which should be correct most of the time.
            isAmbient.value = false
            isHeadless = headless
        }

        /**
         * Whether or not we allow watchfaces to animate. In some tests or for headless
         * rendering (for remote config) we don't want this.
         */
        internal var allowWatchfaceToAnimate = allowWatchFaceToAnimate()

        internal var destroyed = false

        internal lateinit var ambientUpdateWakelock: PowerManager.WakeLock

        private lateinit var choreographer: Choreographer

        /**
         * Whether we already have a [frameCallback] posted and waiting in the [Choreographer]
         * queue. This protects us from drawing multiple times in a single frame.
         */
        private var frameCallbackPending = false

        private val frameCallback = object : Choreographer.FrameCallback {
            @SuppressWarnings("SyntheticAccessor")
            override fun doFrame(frameTimeNs: Long) {
                if (destroyed) {
                    return
                }
                require(allowWatchfaceToAnimate) {
                    "Choreographer doFrame called but allowWatchfaceToAnimate is false"
                }
                frameCallbackPending = false
                draw()
            }
        }

        private val invalidateRunnable = Runnable(this::invalidate)

        // If non-null then changes to the style must be persisted.
        private var directBootParams: WallpaperInteractiveWatchFaceInstanceParams? = null

        // State to support the old WSL style initialzation flow.
        private var pendingBackgroundAction: Bundle? = null
        private var pendingProperties: Bundle? = null
        private var pendingSetWatchFaceStyle = false
        private var pendingVisibilityChanged: Boolean? = null
        private var pendingComplicationDataUpdates = ArrayList<PendingComplicationData>()
        private var complicationsActivated = false
        private var watchFaceInitStarted = false

        // Only valid after onSetBinder has been called.
        private var systemApiVersion = -1

        internal var firstSetSystemState = true
        internal var immutableSystemStateDone = false
        private var ignoreNextOnVisibilityChanged = false

        internal var lastActiveComplications: IntArray? = null
        internal var lastA11yLabels: Array<ContentDescriptionLabel>? = null

        private var firstOnSurfaceChangedReceived = false
        private var asyncWatchFaceConstructionPending = false

        private var initialUserStyle: UserStyleWireFormat? = null
        private lateinit var interactiveInstanceId: String

        private var createdBy = "?"

        /** Note this function should only be called once. */
        @SuppressWarnings("NewApi")
        private fun maybeCreateWCSApi(): Unit = TraceEvent("EngineWrapper.maybeCreateWCSApi").use {
            // If this is a headless instance then we don't want to create a WCS instance.
            if (mutableWatchState.isHeadless) {
                return
            }

            val pendingWallpaperInstance =
                InteractiveInstanceManager.takePendingWallpaperInteractiveWatchFaceInstance()

            // In a direct boot scenario attempt to load the previously serialized parameters.
            if (pendingWallpaperInstance == null && !expectPreRInitFlow()) {
                directBootParams = readDirectBootPrefs(_context, DIRECT_BOOT_PREFS)
                if (directBootParams != null) {
                    val asyncTraceEvent = AsyncTraceEvent("DirectBoot")
                    coroutineScope.launch {
                        // In tests a watchface may already have been created.
                        if (!watchFaceCreatedOrPending()) {
                            createInteractiveInstance(
                                directBootParams!!,
                                "DirectBoot"
                            ).createWCSApi()
                            asyncTraceEvent.close()
                        }
                    }
                }
            }

            // If there's a pending WallpaperInteractiveWatchFaceInstance then create it.
            if (pendingWallpaperInstance != null) {
                val asyncTraceEvent =
                    AsyncTraceEvent("Create PendingWallpaperInteractiveWatchFaceInstance")
                // The WallpaperService works around bugs in wallpapers (see b/5233826 and
                // b/5209847) by sending onVisibilityChanged(true), onVisibilityChanged(false)
                // after onSurfaceChanged during creation. This is unfortunate for us since we
                // perform work in response (see WatchFace.visibilityObserver). So here we
                // workaround the workaround...
                ignoreNextOnVisibilityChanged = true
                coroutineScope.launch {
                    pendingWallpaperInstance.callback.onInteractiveWatchFaceWcsCreated(
                        createInteractiveInstance(
                            pendingWallpaperInstance.params,
                            "Boot with pendingWallpaperInstance"
                        ).createWCSApi()
                    )
                    asyncTraceEvent.close()
                    val params = pendingWallpaperInstance.params
                    directBootParams = params
                    // We don't want to display complications in direct boot mode so replace with an
                    // empty list. NB we can't actually serialise complications anyway so that's
                    // just as well...
                    params.idAndComplicationDataWireFormats = emptyList()

                    // Writing even small amounts of data to storage is quite slow and if we did
                    // that immediately, we'd delay the first frame which is rendered via
                    // onSurfaceRedrawNeeded. By posting this task we expedite first frame
                    // rendering. There is a small window where the direct boot could be stale if
                    // the watchface crashed but this seems unlikely in practice.
                    uiThreadHandler.post {
                        writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, params)
                    }
                }
            }
        }

        override fun onUserStyleChanged() {
            val params = directBootParams
            if (!this::watchFaceImpl.isInitialized || params == null) {
                return
            }

            val currentStyle = watchFaceImpl.userStyleRepository.userStyle.toWireFormat()
            if (params.userStyle.equals(currentStyle)) {
                return
            }
            params.userStyle = currentStyle
            // We don't want to display complications in direct boot mode so replace with an
            // empty list. NB we can't actually serialise complications anyway so that's just as
            // well...
            params.idAndComplicationDataWireFormats = emptyList()
            writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, params)
        }

        @UiThread
        fun ambientTickUpdate(): Unit = TraceEvent("EngineWrapper.ambientTickUpdate").use {
            if (mutableWatchState.isAmbient.value) {
                ambientUpdateWakelock.acquire()
                watchFaceImpl.renderer.invalidate()
                ambientUpdateWakelock.acquire(SURFACE_DRAW_TIMEOUT_MS)
            }
        }

        @UiThread
        fun setSystemState(systemState: SystemState) {
            if (firstSetSystemState ||
                systemState.inAmbientMode != mutableWatchState.isAmbient.value
            ) {
                mutableWatchState.isAmbient.value = systemState.inAmbientMode
            }

            if (firstSetSystemState ||
                systemState.interruptionFilter != mutableWatchState.interruptionFilter.value
            ) {
                mutableWatchState.interruptionFilter.value = systemState.interruptionFilter
            }

            firstSetSystemState = false
        }

        @UiThread
        fun setUserStyle(
            userStyle: UserStyleWireFormat
        ): Unit = TraceEvent("EngineWrapper.setUserStyle").use {
            watchFaceImpl.onSetStyleInternal(
                UserStyle(userStyle, watchFaceImpl.userStyleRepository.schema)
            )
            onUserStyleChanged()
        }

        @UiThread
        fun setImmutableSystemState(deviceConfig: DeviceConfig) {
            // These properties never change so set them once only.
            if (!immutableSystemStateDone) {
                mutableWatchState.hasLowBitAmbient = deviceConfig.hasLowBitAmbient
                mutableWatchState.hasBurnInProtection =
                    deviceConfig.hasBurnInProtection
                mutableWatchState.analogPreviewReferenceTimeMillis =
                    deviceConfig.analogPreviewReferenceTimeMillis
                mutableWatchState.digitalPreviewReferenceTimeMillis =
                    deviceConfig.digitalPreviewReferenceTimeMillis

                immutableSystemStateDone = true
            }
        }

        @SuppressLint("SyntheticAccessor")
        fun setComplicationData(
            complicationId: Int,
            data: ComplicationData
        ): Unit = TraceEvent("EngineWrapper.setComplicationData").use {
            if (watchFaceCreated()) {
                watchFaceImpl.onComplicationDataUpdate(complicationId, data)
            } else {
                pendingComplicationDataUpdates.add(
                    PendingComplicationData(complicationId, data)
                )
            }
        }

        fun clearComplicationData() {
            require(watchFaceCreated()) {
                "WatchFace must have been created first"
            }
            watchFaceImpl.clearComplicationData()
        }

        @UiThread
        fun getComplicationState(): List<IdAndComplicationStateWireFormat> =
            uiThreadHandler.runOnHandlerWithTracing("EngineWrapper.getComplicationState") {
                watchFaceImpl.complicationsManager.complications.map {
                    IdAndComplicationStateWireFormat(
                        it.key,
                        ComplicationStateWireFormat(
                            it.value.computeBounds(watchFaceImpl.renderer.screenBounds),
                            it.value.boundsType,
                            ComplicationType.toWireTypes(it.value.supportedTypes),
                            it.value.defaultProviderPolicy.providersAsList(),
                            it.value.defaultProviderPolicy.systemProviderFallback,
                            it.value.defaultProviderType.toWireComplicationType(),
                            it.value.enabled,
                            it.value.initiallyEnabled,
                            it.value.renderer.getData()?.type?.toWireComplicationType()
                                ?: ComplicationType.NO_DATA.toWireComplicationType(),
                            it.value.fixedComplicationProvider,
                            it.value.configExtras
                        )
                    )
                }
            }

        @UiThread
        fun setComplicationDataList(
            complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
        ): Unit = TraceEvent("EngineWrapper.setComplicationDataList").use {
            if (watchFaceCreated()) {
                for (idAndComplicationData in complicationDatumWireFormats) {
                    watchFaceImpl.onComplicationDataUpdate(
                        idAndComplicationData.id,
                        idAndComplicationData.complicationData.toApiComplicationData()
                    )
                }
            } else {
                for (idAndComplicationData in complicationDatumWireFormats) {
                    pendingComplicationDataUpdates.add(
                        PendingComplicationData(
                            idAndComplicationData.id,
                            idAndComplicationData.complicationData.toApiComplicationData()
                        )
                    )
                }
            }
        }

        private fun requestWatchFaceStyle() {
            try {
                iWatchFaceService.setStyle(watchFaceImpl.getWatchFaceStyle())
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to set WatchFaceStyle: ", e)
            }

            val activeComplications = lastActiveComplications
            if (activeComplications != null) {
                setActiveComplications(activeComplications)
            }

            val a11yLabels = lastA11yLabels
            if (a11yLabels != null) {
                setContentDescriptionLabels(a11yLabels)
            }
        }

        @UiThread
        @RequiresApi(27)
        fun renderWatchFaceToBitmap(
            params: WatchFaceRenderParams
        ): Bundle = TraceEvent("EngineWrapper.renderWatchFaceToBitmap").use {
            val oldStyle = HashMap(watchFaceImpl.userStyleRepository.userStyle.selectedOptions)
            params.userStyle?.let {
                watchFaceImpl.onSetStyleInternal(
                    UserStyle(it, watchFaceImpl.userStyleRepository.schema)
                )
            }

            val oldComplicationData =
                watchFaceImpl.complicationsManager.complications.values.associateBy(
                    { it.id },
                    { it.renderer.getData() }
                )

            params.idAndComplicationDatumWireFormats?.let {
                for (idAndData in it) {
                    watchFaceImpl.complicationsManager[idAndData.id]!!.renderer
                        .loadData(idAndData.complicationData.toApiComplicationData(), false)
                }
            }

            val bitmap = watchFaceImpl.renderer.takeScreenshot(
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = params.calendarTimeMillis
                },
                RenderParameters(params.renderParametersWireFormat)
            )

            // Restore previous style & complications if required.
            if (params.userStyle != null) {
                watchFaceImpl.onSetStyleInternal(UserStyle(oldStyle))
            }

            if (params.idAndComplicationDatumWireFormats != null) {
                for ((id, data) in oldComplicationData) {
                    watchFaceImpl.complicationsManager[id]!!.renderer.loadData(data, false)
                }
            }

            return SharedMemoryImage.ashmemWriteImageBundle(bitmap)
        }

        @UiThread
        @RequiresApi(27)
        fun renderComplicationToBitmap(
            params: ComplicationRenderParams
        ): Bundle? = TraceEvent("EngineWrapper.renderComplicationToBitmap").use {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = params.calendarTimeMillis
            }
            return watchFaceImpl.complicationsManager[params.complicationId]?.let {
                val oldStyle = HashMap(watchFaceImpl.userStyleRepository.userStyle.selectedOptions)
                val newStyle = params.userStyle
                if (newStyle != null) {
                    watchFaceImpl.onSetStyleInternal(
                        UserStyle(newStyle, watchFaceImpl.userStyleRepository.schema)
                    )
                }

                val bounds = it.computeBounds(watchFaceImpl.renderer.screenBounds)
                val complicationBitmap =
                    Bitmap.createBitmap(
                        bounds.width(), bounds.height(),
                        Bitmap.Config.ARGB_8888
                    )

                var prevData: ComplicationData? = null
                val screenshotComplicationData = params.complicationData
                if (screenshotComplicationData != null) {
                    prevData = it.renderer.getData()
                    it.renderer.loadData(
                        screenshotComplicationData.toApiComplicationData(),
                        false
                    )
                }

                it.renderer.render(
                    Canvas(complicationBitmap),
                    Rect(0, 0, bounds.width(), bounds.height()),
                    calendar,
                    RenderParameters(params.renderParametersWireFormat),
                    params.complicationId
                )

                // Restore previous ComplicationData & style if required.
                if (params.complicationData != null) {
                    it.renderer.loadData(prevData, false)
                }

                if (newStyle != null) {
                    watchFaceImpl.onSetStyleInternal(UserStyle(oldStyle))
                }

                SharedMemoryImage.ashmemWriteImageBundle(complicationBitmap)
            }
        }

        @UiThread
        fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
            if (watchFaceCreated()) {
                watchFaceImpl.onTapCommand(tapType, xPos, yPos)
            }
        }

        override fun getContext() = _context

        override fun getHandler() = uiThreadHandler

        override fun onCreate(
            holder: SurfaceHolder
        ): Unit = TraceEvent("EngineWrapper.onCreate").use {
            super.onCreate(holder)

            ambientUpdateWakelock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:[AmbientUpdate]")
            // Disable reference counting for our wake lock so that we can use the same wake lock
            // for user code in invalidate() and after that for having canvas drawn.
            ambientUpdateWakelock.setReferenceCounted(false)

            // Rerender watch face if the surface changes.
            holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        // We can sometimes get this callback before the watchface has been created
                        // in which case it's safe to drop it.
                        if (this@EngineWrapper::watchFaceImpl.isInitialized) {
                            invalidate()
                        }
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                    }

                    override fun surfaceCreated(holder: SurfaceHolder) {
                    }
                }
            )
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ): Unit = TraceEvent("EngineWrapper.onSurfaceChanged").use {
            super.onSurfaceChanged(holder, format, width, height)

            // We can only call maybeCreateWCSApi once. For OpenGL watch faces we need to wait for
            // onSurfaceChanged before bootstrapping because the surface isn't valid for creating
            // an EGL context until then.
            if (!firstOnSurfaceChangedReceived) {
                maybeCreateWCSApi()
                firstOnSurfaceChangedReceived = true
            }
        }

        override fun onDestroy(): Unit = TraceEvent("EngineWrapper.onDestroy").use {
            destroyed = true
            uiThreadHandler.removeCallbacks(invalidateRunnable)
            if (this::choreographer.isInitialized) {
                choreographer.removeFrameCallback(frameCallback)
            }

            if (this::watchFaceImpl.isInitialized) {
                watchFaceImpl.onDestroy()
            }

            if (this::interactiveInstanceId.isInitialized) {
                InteractiveInstanceManager.deleteInstance(interactiveInstanceId)
            }

            super.onDestroy()
        }

        override fun onCommand(
            action: String?,
            x: Int,
            y: Int,
            z: Int,
            extras: Bundle?,
            resultRequested: Boolean
        ): Bundle? {
            // From android R onwards the integration changes and no wallpaper commands are allowed
            // or expected and can/should be ignored.
            if (!expectPreRInitFlow()) {
                return null
            }
            when (action) {
                Constants.COMMAND_AMBIENT_UPDATE ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_AMBIENT_UPDATE") {
                        ambientTickUpdate()
                    }
                Constants.COMMAND_BACKGROUND_ACTION ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_BACKGROUND_ACTION") {
                        onBackgroundAction(extras!!)
                    }
                Constants.COMMAND_COMPLICATION_DATA ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_COMPLICATION_DATA") {
                        onComplicationDataUpdate(extras!!)
                    }
                Constants.COMMAND_REQUEST_STYLE ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_REQUEST_STYLE") {
                        onRequestStyle()
                    }
                Constants.COMMAND_SET_BINDER ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_SET_BINDER") {
                        onSetBinder(extras!!)
                    }
                Constants.COMMAND_SET_PROPERTIES ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_SET_PROPERTIES") {
                        onPropertiesChanged(extras!!)
                    }
                Constants.COMMAND_TAP ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_TAP") {
                        sendTouchEvent(x, y, TapType.UP)
                    }
                Constants.COMMAND_TOUCH ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_TOUCH") {
                        sendTouchEvent(x, y, TapType.DOWN)
                    }
                Constants.COMMAND_TOUCH_CANCEL ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_TOUCH_CANCEL") {
                        sendTouchEvent(x, y, TapType.CANCEL)
                    }
                else -> {
                }
            }
            return null
        }

        @UiThread
        fun onBackgroundAction(extras: Bundle) {
            // We can't guarantee the binder has been set and onSurfaceChanged called before this
            // command.
            if (!watchFaceCreated()) {
                pendingBackgroundAction = extras
                return
            }

            setSystemState(
                SystemState(
                    extras.getBoolean(
                        Constants.EXTRA_AMBIENT_MODE,
                        mutableWatchState.isAmbient.getValueOr(false)
                    ),
                    extras.getInt(
                        Constants.EXTRA_INTERRUPTION_FILTER,
                        mutableWatchState.interruptionFilter.getValueOr(0)
                    )
                )
            )

            pendingBackgroundAction = null
        }

        private fun onSetBinder(extras: Bundle) {
            val binder = extras.getBinder(Constants.EXTRA_BINDER)
            if (binder == null) {
                Log.w(TAG, "Binder is null.")
                return
            }

            iWatchFaceService = IWatchFaceService.Stub.asInterface(binder)

            try {
                // Note if the implementation doesn't support getVersion this will return zero
                // rather than throwing an exception.
                systemApiVersion = iWatchFaceService.apiVersion
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to getVersion: ", e)
            }

            coroutineScope.launch { maybeCreateWatchFace() }
        }

        override fun getInitialUserStyle(): UserStyleWireFormat? = initialUserStyle

        @RequiresApi(27)
        suspend fun createHeadlessInstance(
            params: HeadlessWatchFaceInstanceParams
        ): HeadlessWatchFaceImpl = TraceEvent("EngineWrapper.createHeadlessInstance").use {
            require(!watchFaceCreatedOrPending()) {
                "WatchFace already exists! Created by $createdBy"
            }
            setImmutableSystemState(params.deviceConfig)

            // Fake SurfaceHolder with just enough methods implemented for headless rendering.
            val fakeSurfaceHolder = object : SurfaceHolder {
                val callbacks = HashSet<SurfaceHolder.Callback>()

                override fun setType(type: Int) {
                    throw NotImplementedError()
                }

                override fun getSurface(): Surface {
                    throw NotImplementedError()
                }

                override fun setSizeFromLayout() {
                    throw NotImplementedError()
                }

                override fun lockCanvas(): Canvas {
                    throw NotImplementedError()
                }

                override fun lockCanvas(dirty: Rect?): Canvas {
                    throw NotImplementedError()
                }

                override fun getSurfaceFrame() = Rect(0, 0, params.width, params.height)

                override fun setFixedSize(width: Int, height: Int) {
                    throw NotImplementedError()
                }

                override fun removeCallback(callback: SurfaceHolder.Callback) {
                    callbacks.remove(callback)
                }

                override fun isCreating(): Boolean {
                    throw NotImplementedError()
                }

                override fun addCallback(callback: SurfaceHolder.Callback) {
                    callbacks.add(callback)
                }

                override fun setFormat(format: Int) {
                    throw NotImplementedError()
                }

                override fun setKeepScreenOn(screenOn: Boolean) {
                    throw NotImplementedError()
                }

                override fun unlockCanvasAndPost(canvas: Canvas?) {
                    throw NotImplementedError()
                }
            }

            allowWatchfaceToAnimate = false
            require(mutableWatchState.isHeadless)
            val watchState = mutableWatchState.asWatchState()

            createWatchFaceInternal(
                watchState,
                fakeSurfaceHolder,
                "createHeadlessInstance"
            )

            mutableWatchState.isVisible.value = true
            mutableWatchState.isAmbient.value = false
            return HeadlessWatchFaceImpl(this, uiThreadHandler)
        }

        @UiThread
        @RequiresApi(27)
        suspend fun createInteractiveInstance(
            params: WallpaperInteractiveWatchFaceInstanceParams,
            _createdBy: String
        ): InteractiveWatchFaceImpl = TraceEvent(
            "EngineWrapper.createInteractiveInstance"
        ).use {
            require(!watchFaceCreatedOrPending()) {
                "WatchFace already exists! Created by $createdBy"
            }
            require(!mutableWatchState.isHeadless)

            setImmutableSystemState(params.deviceConfig)
            setSystemState(params.systemState)
            initialUserStyle = params.userStyle

            val watchState = mutableWatchState.asWatchState()

            createWatchFaceInternal(
                watchState,
                getWallpaperSurfaceHolderOverride() ?: surfaceHolder,
                _createdBy
            )

            asyncWatchFaceConstructionPending = false

            params.idAndComplicationDataWireFormats?.let { setComplicationDataList(it) }

            val visibility = pendingVisibilityChanged
            if (visibility != null) {
                onVisibilityChanged(visibility)
                pendingVisibilityChanged = null
            }

            val instance = InteractiveWatchFaceImpl(this, params.instanceId, uiThreadHandler)
            InteractiveInstanceManager.addInstance(instance)
            interactiveInstanceId = params.instanceId

            // WatchFace init is async so its possible we have a pending
            // WallpaperInteractiveWatchFaceInstance request.
            InteractiveInstanceManager.takePendingWallpaperInteractiveWatchFaceInstance()?.let {
                require(it.params.instanceId == params.instanceId) {
                    "Miss match between pendingWallpaperInstance id $it.params.instanceId and " +
                        "constructed instance id $params.instanceId"
                }
                it.callback.onInteractiveWatchFaceWcsCreated(instance.createWCSApi())
            }

            return instance
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            if (TRACE_DRAW) {
                Trace.beginSection("onSurfaceRedrawNeeded")
            }
            if (watchFaceCreated()) {
                watchFaceImpl.onSurfaceRedrawNeeded()
            }
            if (TRACE_DRAW) {
                Trace.endSection()
            }
        }

        private suspend fun createWatchFaceInternal(
            watchState: WatchState,
            surfaceHolder: SurfaceHolder,
            _createdBy: String
        ) {
            asyncWatchFaceConstructionPending = true
            createdBy = _createdBy
            val watchface = TraceEvent("WatchFaceService.createWatchFace").use {
                createWatchFace(surfaceHolder, watchState)
            }
            watchFaceImpl = TraceEvent("WatchFaceImpl.init").use {
                WatchFaceImpl(watchface, this, watchState)
            }
            asyncWatchFaceConstructionPending = false
        }

        private suspend fun maybeCreateWatchFace(): Unit = TraceEvent(
            "EngineWrapper.maybeCreateWatchFace"
        ).use {
            // To simplify handling of watch face state, we only construct the [WatchFaceImpl]
            // once iWatchFaceService have been initialized and pending properties sent.
            if (this::iWatchFaceService.isInitialized && pendingProperties != null &&
                !watchFaceCreatedOrPending()
            ) {
                watchFaceInitStarted = true

                // Apply immutable properties to mutableWatchState before creating the watch face.
                onPropertiesChanged(pendingProperties!!)
                pendingProperties = null

                val watchState = mutableWatchState.asWatchState()
                createWatchFaceInternal(watchState, surfaceHolder, "maybeCreateWatchFace")

                val backgroundAction = pendingBackgroundAction
                if (backgroundAction != null) {
                    onBackgroundAction(backgroundAction)
                    pendingBackgroundAction = null
                }
                if (pendingSetWatchFaceStyle) {
                    onRequestStyle()
                }
                val visibility = pendingVisibilityChanged
                if (visibility != null) {
                    onVisibilityChanged(visibility)
                    pendingVisibilityChanged = null
                }
                for (complicationDataUpdate in pendingComplicationDataUpdates) {
                    setComplicationData(
                        complicationDataUpdate.complicationId,
                        complicationDataUpdate.data
                    )
                }
            }
        }

        private fun onRequestStyle() {
            // We can't guarantee the binder has been set and onSurfaceChanged called before this
            // command.
            if (!watchFaceCreated()) {
                pendingSetWatchFaceStyle = true
                return
            }
            requestWatchFaceStyle()
            pendingSetWatchFaceStyle = false
        }

        override fun onVisibilityChanged(visible: Boolean): Unit = TraceEvent(
            "onVisibilityChanged"
        ).use {
            super.onVisibilityChanged(visible)

            if (ignoreNextOnVisibilityChanged) {
                ignoreNextOnVisibilityChanged = false
                return
            }

            // In the WSL flow Home doesn't know when WallpaperService has actually launched a
            // watchface after requesting a change. It used [Constants.ACTION_REQUEST_STATE] as a
            // signal to trigger the old boot flow (sending the binder etc). This is no longer
            // required from android R onwards. See (b/181965946).
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // We are requesting state every time the watch face changes its visibility because
                // wallpaper commands have a tendency to be dropped. By requesting it on every
                // visibility change, we ensure that we don't become a victim of some race
                // condition.
                sendBroadcast(
                    Intent(Constants.ACTION_REQUEST_STATE).apply {
                        putExtra(Constants.EXTRA_WATCH_FACE_VISIBLE, visible)
                    }
                )

                // We can't guarantee the binder has been set and onSurfaceChanged called before
                // this command.
                if (!watchFaceCreated()) {
                    pendingVisibilityChanged = visible
                    return
                }
            }

            mutableWatchState.isVisible.value = visible
            pendingVisibilityChanged = null
        }

        override fun invalidate() {
            if (!allowWatchfaceToAnimate) {
                return
            }
            if (!frameCallbackPending) {
                if (LOG_VERBOSE) {
                    Log.v(TAG, "invalidate: requesting draw")
                }
                frameCallbackPending = true
                if (!this::choreographer.isInitialized) {
                    choreographer = Choreographer.getInstance()
                }
                choreographer.postFrameCallback(frameCallback)
            } else {
                if (LOG_VERBOSE) {
                    Log.v(TAG, "invalidate: draw already requested")
                }
            }
        }

        internal fun draw() {
            try {
                if (TRACE_DRAW) {
                    Trace.beginSection("onDraw")
                }
                if (LOG_VERBOSE) {
                    Log.v(WatchFaceService.TAG, "drawing frame")
                }
                watchFaceImpl.onDraw()
            } finally {
                if (TRACE_DRAW) {
                    Trace.endSection()
                }
            }
        }

        private fun onComplicationDataUpdate(extras: Bundle) {
            extras.classLoader = WireComplicationData::class.java.classLoader
            val complicationData: WireComplicationData =
                extras.getParcelable(Constants.EXTRA_COMPLICATION_DATA)!!
            setComplicationData(
                extras.getInt(Constants.EXTRA_COMPLICATION_ID),
                complicationData.toApiComplicationData()
            )
        }

        @UiThread
        internal fun onPropertiesChanged(properties: Bundle) {
            if (!watchFaceInitStarted) {
                pendingProperties = properties
                coroutineScope.launch { maybeCreateWatchFace() }
                return
            }

            setImmutableSystemState(
                DeviceConfig(
                    properties.getBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT),
                    properties.getBoolean(Constants.PROPERTY_BURN_IN_PROTECTION),
                    ANALOG_WATCHFACE_REFERENCE_TIME_MS,
                    DIGITAL_WATCHFACE_REFERENCE_TIME_MS
                )
            )
        }

        internal fun watchFaceCreated() = this::watchFaceImpl.isInitialized

        internal fun watchFaceCreatedOrPending() =
            watchFaceCreated() || asyncWatchFaceConstructionPending

        override fun setDefaultComplicationProviderWithFallbacks(
            watchFaceComplicationId: Int,
            providers: List<ComponentName>?,
            @ProviderId fallbackSystemProvider: Int,
            type: Int
        ) {
            // For wear 3.0 watchfaces iWatchFaceService won't have been set.
            if (!this::iWatchFaceService.isInitialized) {
                return
            }

            if (systemApiVersion >= 2) {
                iWatchFaceService.setDefaultComplicationProviderWithFallbacks(
                    watchFaceComplicationId,
                    providers,
                    fallbackSystemProvider,
                    type
                )
            } else {
                // If the implementation doesn't support the new API we emulate its behavior by
                // setting complication providers in the reverse order. This works because if
                // setDefaultComplicationProvider attempts to set a non-existent or incompatible
                // provider it does nothing, which allows us to emulate the same semantics as
                // setDefaultComplicationProviderWithFallbacks albeit with more calls.
                if (fallbackSystemProvider != WatchFaceImpl.NO_DEFAULT_PROVIDER) {
                    iWatchFaceService.setDefaultSystemComplicationProvider(
                        watchFaceComplicationId, fallbackSystemProvider, type
                    )
                }

                if (providers != null) {
                    // Iterate in reverse order. This could be O(n^2) but n is expected to be small
                    // and the list is probably an ArrayList so it's probably O(n) in practice.
                    for (i in providers.size - 1 downTo 0) {
                        iWatchFaceService.setDefaultComplicationProvider(
                            watchFaceComplicationId, providers[i], type
                        )
                    }
                }
            }
        }

        override fun setActiveComplications(watchFaceComplicationIds: IntArray) {
            // For wear 3.0 watchfaces iWatchFaceService won't have been set.
            if (!this::iWatchFaceService.isInitialized) {
                return
            }

            lastActiveComplications = watchFaceComplicationIds

            try {
                iWatchFaceService.setActiveComplications(
                    watchFaceComplicationIds, /* updateAll= */ !complicationsActivated
                )
                complicationsActivated = true
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to set active complications: ", e)
            }
        }

        override fun setContentDescriptionLabels(labels: Array<ContentDescriptionLabel>) {
            // For wear 3.0 watchfaces iWatchFaceService won't have been set.
            if (!this::iWatchFaceService.isInitialized) {
                return
            }

            lastA11yLabels = labels
            try {
                iWatchFaceService.setContentDescriptionLabels(labels)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to set accessibility labels: ", e)
            }
        }

        @UiThread
        fun dump(writer: IndentingPrintWriter) {
            require(uiThreadHandler.looper.isCurrentThread) {
                "dump must be called from the UIThread"
            }
            writer.println("WatchFaceEngine:")
            writer.increaseIndent()
            when {
                this::iWatchFaceService.isInitialized -> writer.println("WSL style init flow")
                this::watchFaceImpl.isInitialized -> writer.println("Androidx style init flow")
                expectPreRInitFlow() -> writer.println("Expecting WSL style init")
                else -> writer.println("Expecting androidx style style init")
            }

            if (this::iWatchFaceService.isInitialized) {
                writer.println(
                    "iWatchFaceService.asBinder().isBinderAlive=" +
                        "${iWatchFaceService.asBinder().isBinderAlive}"
                )
                if (iWatchFaceService.asBinder().isBinderAlive) {
                    writer.println("iWatchFaceService.apiVersion=${iWatchFaceService.apiVersion}")
                }
            }
            writer.println("createdBy=$createdBy")
            writer.println("firstOnSurfaceChanged=$firstOnSurfaceChangedReceived")
            writer.println("watchFaceInitStarted=$watchFaceInitStarted")
            writer.println("asyncWatchFaceConstructionPending=$asyncWatchFaceConstructionPending")
            writer.println("ignoreNextOnVisibilityChanged=$ignoreNextOnVisibilityChanged")

            if (this::interactiveInstanceId.isInitialized) {
                writer.println("interactiveInstanceId=$interactiveInstanceId")
            }

            writer.println("frameCallbackPending=$frameCallbackPending")
            writer.println("destroyed=$destroyed")

            if (!destroyed && this::watchFaceImpl.isInitialized) {
                watchFaceImpl.dump(writer)
            }
            writer.decreaseIndent()
        }
    }

    @UiThread
    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<String>) {
        super.dump(fd, writer, args)
        val indentingPrintWriter = IndentingPrintWriter(writer)
        indentingPrintWriter.println("AndroidX WatchFaceService $packageName")
        InteractiveInstanceManager.dump(indentingPrintWriter)
        EditorService.globalEditorService.dump(indentingPrintWriter)
        HeadlessWatchFaceImpl.dump(indentingPrintWriter)
        indentingPrintWriter.flush()
    }
}

/**
 * Runs the supplied task on the handler thread. If we're not on the handler thread a task is posted
 * and we block until it's been processed.
 *
 * AIDL calls are dispatched from a thread pool, but for simplicity WatchFaceImpl code is largely
 * single threaded so we need to post tasks to the UI thread and wait for them to execute.
 *
 * @param traceEventName The name of the trace event to emit.
 * @param task The task to post on the handler.
 * @return [R] the return value of [task].
 */
internal fun <R> Handler.runOnHandlerWithTracing(
    traceEventName: String,
    task: () -> R
): R = TraceEvent(traceEventName).use {
    if (looper == Looper.myLooper()) {
        task.invoke()
    } else {
        val latch = CountDownLatch(1)
        var returnVal: R? = null
        var exception: Exception? = null
        if (post {
            try {
                returnVal = TraceEvent("$traceEventName invokeTask").use {
                    task.invoke()
                }
            } catch (e: Exception) {
                // Will rethrow on the calling thread.
                exception = e
            }
            latch.countDown()
        }
        ) {
            latch.await()
            if (exception != null) {
                throw exception as Exception
            }
        }
        // R might be nullable so we can't assert nullability here.
        @Suppress("UNCHECKED_CAST")
        returnVal as R
    }
}
