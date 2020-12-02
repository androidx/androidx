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
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.RemoteException
import android.os.Trace
import android.os.UserManager
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
import androidx.wear.complications.data.IdAndComplicationData
import androidx.wear.complications.data.NoDataComplicationData
import androidx.wear.complications.data.asApiComplicationData
import androidx.wear.watchface.control.HeadlessWatchFaceImpl
import androidx.wear.watchface.control.IInteractiveWatchFaceSysUI
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.InteractiveWatchFaceImpl
import androidx.wear.watchface.control.data.ComplicationScreenshotParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.data.UserStyleWireFormat
import java.io.FileNotFoundException
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
        TapType.TOUCH,
        TapType.TOUCH_CANCEL,
        TapType.TAP
    ]
)
public annotation class TapType {
    public companion object {
        /** Used in [WatchFaceImpl#onTapCommand] to indicate a "down" touch event on the watch face. */
        public const val TOUCH: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_TOUCH

        /**
         * Used in [WatchFaceImpl#onTapCommand] to indicate that a previous TapType.TOUCH touch event
         * has been canceled. This generally happens when the watch face is touched but then a
         * move or long press occurs.
         */
        public const val TOUCH_CANCEL: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_TOUCH_CANCEL

        /**
         * Used in [WatchFaceImpl#onTapCommand] to indicate that an "up" event on the watch face has
         * occurred that has not been consumed by another activity. A TapType.TOUCH will always
         * occur first. This event will not occur if a TapType.TOUCH_CANCEL is sent.
         */
        public const val TAP: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_TAP
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

    /** Override this factory method to create your WatchFaceImpl. */
    protected abstract fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState
    ): WatchFace

    final override fun onCreateEngine(): Engine = EngineWrapper(getHandler())

    // This is open to allow mocking.
    internal open fun getHandler() = Handler(Looper.getMainLooper())

    // This is open to allow mocking.
    internal open fun getMutableWatchState() = MutableWatchState()

    // This is open for use by tests.
    internal open fun allowWatchFaceToAnimate() = true

    // This is open for use by tests.
    internal open fun isUserUnlocked() = getSystemService(UserManager::class.java).isUserUnlocked

    /**
     * This is open for use by tests, it allows them to inject a custom [SurfaceHolder].
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getWallpaperSurfaceHolderOverride(): SurfaceHolder? = null

    internal fun setContext(context: Context) {
        attachBaseContext(context)
    }

    internal inner class EngineWrapper(
        private val uiThreadHandler: Handler
    ) : WallpaperService.Engine(), WatchFaceHostApi {
        private val _context = this@WatchFaceService as Context

        internal lateinit var iWatchFaceService: IWatchFaceService
        internal lateinit var watchFaceImpl: WatchFaceImpl

        internal val mutableWatchState = getMutableWatchState().apply {
            isVisible.value = this@EngineWrapper.isVisible
        }

        private var timeTickRegistered = false
        private val timeTickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            @SuppressWarnings("SyntheticAccessor")
            override fun onReceive(context: Context, intent: Intent) {
                watchFaceImpl.renderer.invalidate()
            }
        }

        /**
         * Whether or not we allow watchfaces to animate. In some tests or for headless
         * rendering (for remote config) we don't want this.
         */
        internal var allowWatchfaceToAnimate = allowWatchFaceToAnimate()

        private var destroyed = false

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
                require(allowWatchfaceToAnimate)
                frameCallbackPending = false
                draw()
            }
        }

        private val invalidateRunnable = Runnable(this::invalidate)

        private val ambientTimeTickFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        private val interactiveTimeTickFilter = IntentFilter(ambientTimeTickFilter).apply {
            addAction(Intent.ACTION_TIME_TICK)
        }

        // TODO(alexclarke): Figure out if we can remove this.
        private var pendingBackgroundAction: Bundle? = null
        private var pendingProperties: Bundle? = null
        private var pendingSetWatchFaceStyle = false
        private var pendingVisibilityChanged: Boolean? = null
        private var pendingComplicationDataUpdates = ArrayList<PendingComplicationData>()
        private var complicationsActivated = false

        // Only valid after onSetBinder has been called.
        private var systemApiVersion = -1

        internal var firstSetSystemState = true
        internal var immutableSystemStateDone = false

        internal var lastActiveComplications: IntArray? = null
        internal var lastA11yLabels: Array<ContentDescriptionLabel>? = null

        private var watchFaceInitStarted = false

        private var initialUserStyle: UserStyleWireFormat? = null
        private lateinit var interactiveInstanceId: String

        init {
            maybeCreateWCSApi()
        }

        @SuppressWarnings("NewApi")
        private fun maybeCreateWCSApi() {
            val pendingWallpaperInstance =
                InteractiveInstanceManager.takePendingWallpaperInteractiveWatchFaceInstance()

            // In a direct boot scenario attempt to load the previously serialized parameters.
            if (pendingWallpaperInstance == null && !isUserUnlocked()) {
                val params = readDirectBootPrefs(_context, DIRECT_BOOT_PREFS)
                if (params != null) {
                    createInteractiveInstance(params).createWCSApi()
                    keepSerializedDirectBootParamsUpdated(params)
                }
            }

            // If there's a pending WallpaperInteractiveWatchFaceInstance then create it.
            if (pendingWallpaperInstance != null) {
                pendingWallpaperInstance.callback.onInteractiveWatchFaceWcsCreated(
                    createInteractiveInstance(pendingWallpaperInstance.params).createWCSApi()
                )

                interactiveInstanceId = pendingWallpaperInstance.params.instanceId
                keepSerializedDirectBootParamsUpdated(pendingWallpaperInstance.params)
            }
        }

        private fun keepSerializedDirectBootParamsUpdated(
            directBootParams: WallpaperInteractiveWatchFaceInstanceParams
        ) {
            // We don't want to display complications in direct boot mode so replace with an
            // empty list. NB we can't actually serialise complications anyway so that's just as
            // well...
            directBootParams.idAndComplicationDataWireFormats = emptyList()

            watchFaceImpl.userStyleRepository.addUserStyleListener(
                object : UserStyleRepository.UserStyleListener {
                    @SuppressLint("SyntheticAccessor")
                    override fun onUserStyleChanged(userStyle: UserStyle) {
                        directBootParams.userStyle = userStyle.toWireFormat()
                        writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, directBootParams)
                    }
                })

            writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, directBootParams)
        }

        @UiThread
        fun ambientTickUpdate() {
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
                updateTimeTickReceiver()
            }

            if (firstSetSystemState ||
                systemState.interruptionFilter != mutableWatchState.interruptionFilter.value
            ) {
                mutableWatchState.interruptionFilter.value = systemState.interruptionFilter
            }

            firstSetSystemState = false
        }

        @UiThread
        fun setUserStyle(userStyle: UserStyleWireFormat) {
            watchFaceImpl.onSetStyleInternal(
                UserStyle(userStyle, watchFaceImpl.userStyleRepository.schema)
            )
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
        fun setComplicationData(complicationId: Int, data: ComplicationData) {
            if (watchFaceCreated()) {
                watchFaceImpl.onComplicationDataUpdate(complicationId, data)
            } else {
                pendingComplicationDataUpdates.add(
                    PendingComplicationData(complicationId, data)
                )
            }
        }

        @UiThread
        fun getComplicationState(): List<IdAndComplicationStateWireFormat> =
            uiThreadHandler.runOnHandler {
                watchFaceImpl.complicationsManager.complications.map {
                    IdAndComplicationStateWireFormat(
                        it.key,
                        ComplicationStateWireFormat(
                            it.value.computeBounds(watchFaceImpl.renderer.screenBounds),
                            it.value.boundsType,
                            ComplicationType.toWireTypes(it.value.supportedTypes),
                            it.value.defaultProviderPolicy.providersAsList(),
                            it.value.defaultProviderPolicy.systemProviderFallback,
                            it.value.defaultProviderType.asWireComplicationType(),
                            it.value.enabled
                        )
                    )
                }
            }

        @UiThread
        fun setComplicationDataList(
            complicationDatumWireFormats: MutableList<IdAndComplicationDataWireFormat>
        ) {
            if (watchFaceCreated()) {
                for (idAndComplicationData in complicationDatumWireFormats) {
                    watchFaceImpl.onComplicationDataUpdate(
                        idAndComplicationData.id,
                        idAndComplicationData.complicationData.asApiComplicationData()
                    )
                }
            } else {
                for (idAndComplicationData in complicationDatumWireFormats) {
                    pendingComplicationDataUpdates.add(
                        PendingComplicationData(
                            idAndComplicationData.id,
                            idAndComplicationData.complicationData.asApiComplicationData()
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
        fun takeWatchFaceScreenshot(params: WatchfaceScreenshotParams): Bundle {
            val oldStyle = HashMap(watchFaceImpl.userStyleRepository.userStyle.selectedOptions)
            params.userStyle?.let {
                watchFaceImpl.onSetStyleInternal(
                    UserStyle(it, watchFaceImpl.userStyleRepository.schema)
                )
            }

            val oldComplicationData =
                watchFaceImpl.complicationsManager.complications.mapValues {
                    it.value.renderer.idAndData?.complicationData ?: NoDataComplicationData()
                }
            params.idAndComplicationDatumWireFormats?.let {
                for (idAndData in it) {
                    watchFaceImpl.onComplicationDataUpdate(
                        idAndData.id, idAndData.complicationData.asApiComplicationData()
                    )
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
                    watchFaceImpl.onComplicationDataUpdate(id, data)
                }
            }

            return SharedMemoryImage.ashmemCompressedImageBundle(
                bitmap,
                params.compressionQuality
            )
        }

        @UiThread
        @RequiresApi(27)
        fun takeComplicationScreenshot(params: ComplicationScreenshotParams): Bundle? {
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

                var prevIdAndComplicationData: IdAndComplicationData? = null
                var screenshotComplicationData = params.complicationData
                if (screenshotComplicationData != null) {
                    prevIdAndComplicationData = it.renderer.idAndData
                    it.renderer.idAndData =
                        IdAndComplicationData(
                            params.complicationId,
                            screenshotComplicationData
                        )
                }

                it.renderer.render(
                    Canvas(complicationBitmap),
                    Rect(0, 0, bounds.width(), bounds.height()),
                    calendar,
                    RenderParameters(params.renderParametersWireFormat)
                )

                // Restore previous ComplicationData & style if required.
                if (params.complicationData != null) {
                    it.renderer.idAndData = prevIdAndComplicationData
                }

                if (newStyle != null) {
                    watchFaceImpl.onSetStyleInternal(UserStyle(oldStyle))
                }

                SharedMemoryImage.ashmemCompressedImageBundle(
                    complicationBitmap,
                    params.compressionQuality
                )
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

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            ambientUpdateWakelock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:[AmbientUpdate]")
            // Disable reference counting for our wake lock so that we can use the same wake lock
            // for user code in invaliate() and after that for having canvas drawn.
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

        override fun onDestroy() {
            destroyed = true
            uiThreadHandler.removeCallbacks(invalidateRunnable)
            if (this::choreographer.isInitialized) {
                choreographer.removeFrameCallback(frameCallback)
            }

            if (timeTickRegistered) {
                timeTickRegistered = false
                unregisterReceiver(timeTickReceiver)
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
            when (action) {
                Constants.COMMAND_AMBIENT_UPDATE ->
                    uiThreadHandler.runOnHandler { ambientTickUpdate() }
                Constants.COMMAND_BACKGROUND_ACTION ->
                    uiThreadHandler.runOnHandler { onBackgroundAction(extras!!) }
                Constants.COMMAND_COMPLICATION_DATA ->
                    uiThreadHandler.runOnHandler { onComplicationDataUpdate(extras!!) }
                Constants.COMMAND_REQUEST_STYLE ->
                    uiThreadHandler.runOnHandler { onRequestStyle() }
                Constants.COMMAND_SET_BINDER ->
                    uiThreadHandler.runOnHandler { onSetBinder(extras!!) }
                Constants.COMMAND_SET_PROPERTIES ->
                    uiThreadHandler.runOnHandler { onPropertiesChanged(extras!!) }
                Constants.COMMAND_TAP ->
                    uiThreadHandler.runOnHandler { sendTouchEvent(x, y, TapType.TAP) }
                Constants.COMMAND_TOUCH ->
                    uiThreadHandler.runOnHandler { sendTouchEvent(x, y, TapType.TOUCH) }
                Constants.COMMAND_TOUCH_CANCEL ->
                    uiThreadHandler.runOnHandler { sendTouchEvent(x, y, TapType.TOUCH_CANCEL) }
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

            maybeCreateWatchFace()
        }

        override fun getInitialUserStyle(): UserStyleWireFormat? = initialUserStyle

        @RequiresApi(27)
        fun createHeadlessInstance(
            params: HeadlessWatchFaceInstanceParams
        ): HeadlessWatchFaceImpl {
            require(!watchFaceCreated())
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

            val watchState = mutableWatchState.asWatchState()
            watchFaceImpl = WatchFaceImpl(
                createWatchFace(fakeSurfaceHolder, watchState),
                this,
                watchState
            )

            allowWatchfaceToAnimate = false
            mutableWatchState.isVisible.value = true
            mutableWatchState.isAmbient.value = false

            watchFaceImpl.renderer.onPostCreate()
            return HeadlessWatchFaceImpl(this, uiThreadHandler)
        }

        @UiThread
        @RequiresApi(27)
        fun createInteractiveInstance(
            params: WallpaperInteractiveWatchFaceInstanceParams
        ): InteractiveWatchFaceImpl {
            require(!watchFaceCreated())

            setImmutableSystemState(params.deviceConfig)
            setSystemState(params.systemState)
            initialUserStyle = params.userStyle

            val watchState = mutableWatchState.asWatchState()
            watchFaceImpl = WatchFaceImpl(
                createWatchFace(getWallpaperSurfaceHolderOverride() ?: surfaceHolder, watchState),
                this,
                watchState
            )

            params.idAndComplicationDataWireFormats?.let { setComplicationDataList(it) }

            watchFaceImpl.renderer.onPostCreate()
            val visibility = pendingVisibilityChanged
            if (visibility != null) {
                onVisibilityChanged(visibility)
                pendingVisibilityChanged = null
            }

            val instance = InteractiveWatchFaceImpl(this, params.instanceId, uiThreadHandler)
            InteractiveInstanceManager.addInstance(instance)
            return instance
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            if (watchFaceCreated()) {
                watchFaceImpl.onSurfaceRedrawNeeded()
            }
        }

        private fun maybeCreateWatchFace() {
            // To simplify handling of watch face state, we only construct the [WatchFaceImpl]
            // once iWatchFaceService have been initialized and pending properties sent.
            if (this::iWatchFaceService.isInitialized && pendingProperties != null &&
                !watchFaceCreated()
            ) {
                watchFaceInitStarted = true

                // Apply immutable properties to mutableWatchState before creating the watch face.
                onPropertiesChanged(pendingProperties!!)
                pendingProperties = null

                val watchState = mutableWatchState.asWatchState()
                watchFaceImpl = WatchFaceImpl(
                    createWatchFace(surfaceHolder, watchState),
                    this,
                    watchState
                )
                watchFaceImpl.renderer.onPostCreate()

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

        /**
         * Registers [timeTickReceiver] if it should be registered and isn't currently, or
         * unregisters it if it shouldn't be registered but currently is. It also applies the right
         * intent filter depending on whether we are in ambient mode or not.
         */
        internal fun updateTimeTickReceiver() {
            // Separate calls are issued to deliver the state of isAmbient and isVisible, so during
            // init we might not yet know the state of both.
            if (!mutableWatchState.isAmbient.hasValue() ||
                !mutableWatchState.isVisible.hasValue()
            ) {
                return
            }

            if (timeTickRegistered) {
                unregisterReceiver(timeTickReceiver)
                timeTickRegistered = false
            }

            // We only register if we are visible, otherwise it doesn't make sense to waste cycles.
            if (mutableWatchState.isVisible.value) {
                if (mutableWatchState.isAmbient.value) {
                    registerReceiver(timeTickReceiver, ambientTimeTickFilter)
                } else {
                    registerReceiver(timeTickReceiver, interactiveTimeTickFilter)
                }
                timeTickRegistered = true

                // In case we missed a tick while transitioning from ambient to interactive, we
                // want to make sure the watch face doesn't show stale time when in interactive
                // mode.
                watchFaceImpl.renderer.invalidate()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            // We are requesting state every time the watch face changes its visibility because
            // wallpaper commands have a tendency to be dropped. By requesting it on every
            // visibility change, we ensure that we don't become a victim of some race condition.
            sendBroadcast(
                Intent(Constants.ACTION_REQUEST_STATE).apply {
                    putExtra(Constants.EXTRA_WATCH_FACE_VISIBLE, visible)
                }
            )

            // We can't guarantee the binder has been set and onSurfaceChanged called before this
            // command.
            if (!watchFaceCreated()) {
                pendingVisibilityChanged = visible
                return
            }

            mutableWatchState.isVisible.value = visible
            updateTimeTickReceiver()
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
                complicationData.asApiComplicationData()
            )
        }

        @UiThread
        internal fun onPropertiesChanged(properties: Bundle) {
            if (!watchFaceInitStarted) {
                pendingProperties = properties
                maybeCreateWatchFace()
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

        override fun setCurrentUserStyle(userStyle: UserStyleWireFormat) {
            // TODO(alexclarke): Report programmatic style changes to WCS.
        }

        override fun setComplicationDetails(
            complicationId: Int,
            bounds: Rect,
            @ComplicationBoundsType boundsType: Int,
            types: IntArray
        ) {
            // TODO(alexclarke): Report programmatic complication details changes to WCS.
        }
    }
}

/**
 * Runs the supplied task on the handler thread. If we're not on the handler thread a task is posted
 * and we block until it's been processed.
 *
 * AIDL calls are dispatched from a thread pool, but for simplicity WatchFaceImpl code is
 * largely single threaded so we need to post tasks to the UI thread and wait for them to
 * execute.
 */
internal fun <R> Handler.runOnHandler(task: () -> R) =
    if (looper == Looper.myLooper()) {
        task.invoke()
    } else {
        val latch = CountDownLatch(1)
        var returnVal: R? = null
        var exception: Exception? = null
        if (post {
            try {
                returnVal = task.invoke()
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
        returnVal!!
    }

internal fun readDirectBootPrefs(
    context: Context,
    fileName: String
): WallpaperInteractiveWatchFaceInstanceParams? =
    try {
        val reader = context.openFileInput(fileName)
        val result =
            ParcelUtils.fromInputStream<WallpaperInteractiveWatchFaceInstanceParams>(reader)
        reader.close()
        result
    } catch (e: FileNotFoundException) {
        null
    }

internal fun writeDirectBootPrefs(
    context: Context,
    fileName: String,
    prefs: WallpaperInteractiveWatchFaceInstanceParams
) {
    val writer = context.openFileOutput(fileName, Context.MODE_PRIVATE)
    ParcelUtils.toOutputStream(prefs, writer)
    writer.close()
}
