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
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.RemoteException
import android.os.Trace
import android.service.wallpaper.WallpaperService
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationData.TYPE_NO_DATA
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceCommand
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.support.wearable.watchface.toAshmemCompressedImageBundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.annotation.IntDef
import androidx.wear.complications.SystemProviders.ProviderId
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.ComplicationDetails
import androidx.wear.watchface.data.IdAndComplicationData
import androidx.wear.watchface.data.ImmutableSystemState
import androidx.wear.watchface.data.IndicatorState
import androidx.wear.watchface.data.RenderParametersWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import java.util.concurrent.CountDownLatch

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
        /** Used in onTapCommand to indicate a "down" touch event on the watch face. */
        public const val TOUCH: Int = 0

        /**
         * Used in onTapCommand to indicate that a previous TapType.TOUCH touch event has been
         * canceled. This generally happens when the watch face is touched but then a move or long
         * press occurs.
         */
        public const val TOUCH_CANCEL: Int = 1

        /**
         * Used in onTapCommaned to indicate that an "up" event on the watch face has occurred that
         * has not been consumed by another activity. A TapType.TOUCH will always occur first.
         * This event will not occur if a TapType.TOUCH_CANCEL is sent.
         */
        public const val TAP: Int = 2
    }
}

private class PendingComplicationData(val complicationId: Int, val data: ComplicationData)

/**
 * WatchFaceService and [WatchFace] are a pair of base classes intended to handle much of
 * the boilerplate needed to implement a watch face without being too opinionated. The suggested
 * structure of an WatchFaceService based watch face is:
 *
 * @sample androidx.wear.watchface.samples.kDocCreateExampleWatchFaceService
 *
 * Base classes for complications and styles are provided along with a default UI for configuring
 * them. Complications are optional, however if required, WatchFaceService assumes all
 * complications can be enumerated up front and passed as a collection into WatchFace's constructor.
 * Some watch faces support different configurations (number & position) of complications and this
 * can be achieved by rendering a subset and only marking the ones you need as active. Most watch
 * faces will not animate the location of complications so its recommended to use the
 * WatchFace.UnitSquareBoundsProvider helper.
 *
 * Many watch faces support styles, typically controlling the color and visual look of watch face
 * elements such as numeric fonts, watch hands and ticks. WatchFaceService doesn't take an
 * an opinion on what comprises a style beyond it should be representable as a map of categories to
 * options.
 *
 * It's recommended to avoid putting business logic in sub classes of WatchFaceService, rather
 * that should go in sub classes of WatchFace.
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
@TargetApi(26)
public abstract class WatchFaceService : WallpaperService() {

    /** @hide */
    private companion object {
        private const val TAG = "WatchFaceService"

        /** Whether to log every frame. */
        private const val LOG_VERBOSE = false

        /** Whether to enable tracing for each call to [Engine.onDraw]. */
        private const val TRACE_DRAW = false
    }

    /** Override this factory method to create your WatchFace. */
    protected abstract fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchFaceHost: WatchFaceHost,
        watchState: WatchState
    ): WatchFace

    final override fun onCreateEngine(): Engine = EngineWrapper(getHandler())

    // This is open to allow mocking.
    internal open fun getHandler() = Handler(Looper.getMainLooper())

    // This is open to allow mocking.
    internal open fun getMutableWatchState() = MutableWatchState()

    // This is open for use by tests.
    internal open fun allowWatchFaceToAnimate() = true

    internal fun setContext(context: Context) {
        attachBaseContext(context)
    }

    internal inner class EngineWrapper(
        private val uiThreadHandler: Handler
    ) : WallpaperService.Engine(), WatchFaceHostApi {
        private val _context = this@WatchFaceService as Context

        private lateinit var currentSurfaceHolder: SurfaceHolder
        private var currentSurfaceFormat = 0
        private var currentSurfaceWidth = 0
        private var currentSurfaceHeight = 0

        internal lateinit var iWatchFaceService: IWatchFaceService
        internal lateinit var watchFace: WatchFace

        internal val mutableWatchState = getMutableWatchState().apply {
            isVisible.value = this@EngineWrapper.isVisible
        }

        private var timeTickRegistered = false
        private val timeTickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            @SuppressWarnings("SyntheticAccessor")
            override fun onReceive(context: Context, intent: Intent) {
                watchFace.invalidate()
            }
        }

        /**
         * Whether or not we allow watchfaces to animate. In some tests or for headless
         * rendering (for remote config) we don't want this.
         */
        internal var allowWatchfaceToAnimate = allowWatchFaceToAnimate()

        private var destroyed = false

        internal lateinit var ambientUpdateWakelock: PowerManager.WakeLock

        private val choreographer = Choreographer.getInstance()

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

        // TODO(alexclarke): Migrate all pending logic to operate inside watchFaceCommand.
        private var pendingBackgroundAction: Bundle? = null
        private var pendingProperties: Bundle? = null
        private var pendingSetWatchFaceStyle = false
        private var pendingVisibilityChanged: Boolean? = null
        private var pendingComplicationDataUpdates = ArrayList<PendingComplicationData>()
        private var complicationsActivated = false

        // Only valid after onSetBinder has been called.
        private var systemApiVersion = -1

        internal var firstSetSystemState = true
        internal var firstIndicatorState = true
        internal var immutableSystemStateDone = false

        internal var lastActiveComplications: IntArray? = null
        internal var lastA11yLabels: Array<ContentDescriptionLabel>? = null

        private var watchFaceInitStarted = false

        private val watchFaceCommand = object : IWatchFaceCommand.Stub() {
            override fun getApiVersion() = IWatchFaceCommand.WATCHFACE_COMMAND_API_VERSION

            override fun ambientUpdate() {
                uiThreadHandler.runOnHandler {
                    if (mutableWatchState.isAmbient.value) {
                        ambientUpdateWakelock.acquire()
                        watchFace.invalidate()
                        ambientUpdateWakelock.acquire(SURFACE_DRAW_TIMEOUT_MS)
                    }
                }
            }

            override fun setSystemState(systemState: SystemState) {
                uiThreadHandler.runOnHandler {
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

                    if (firstSetSystemState ||
                        systemState.unreadCount != mutableWatchState.unreadNotificationCount.value
                    ) {
                        mutableWatchState.unreadNotificationCount.value = systemState.unreadCount
                    }

                    if (firstSetSystemState ||
                        systemState.notificationCount != mutableWatchState.notificationCount.value
                    ) {
                        mutableWatchState.notificationCount.value = systemState.notificationCount
                    }

                    firstSetSystemState = false
                }
            }

            override fun setIndicatorState(indicatorState: IndicatorState) {
                uiThreadHandler.runOnHandler {
                    if (firstIndicatorState ||
                        indicatorState.isCharging != mutableWatchState.isCharging.value
                    ) {
                        mutableWatchState.isCharging.value = indicatorState.isCharging
                    }

                    if (firstIndicatorState ||
                        indicatorState.inAirplaneMode != mutableWatchState.inAirplaneMode.value
                    ) {
                        mutableWatchState.inAirplaneMode.value = indicatorState.inAirplaneMode
                    }

                    if (firstIndicatorState ||
                        indicatorState.isConnectedToCompanion !=
                        mutableWatchState.isConnectedToCompanion.value
                    ) {
                        mutableWatchState.isConnectedToCompanion.value =
                            indicatorState.isConnectedToCompanion
                    }

                    if (firstIndicatorState ||
                        indicatorState.inTheaterMode != mutableWatchState.isInTheaterMode.value
                    ) {
                        mutableWatchState.isInTheaterMode.value = indicatorState.inTheaterMode
                    }

                    if (firstIndicatorState ||
                        indicatorState.isGpsActive != mutableWatchState.isGpsActive.value
                    ) {
                        mutableWatchState.isGpsActive.value = indicatorState.isGpsActive
                    }

                    if (firstIndicatorState ||
                        indicatorState.isKeyguardLocked != mutableWatchState.isKeyguardLocked.value
                    ) {
                        mutableWatchState.isKeyguardLocked.value = indicatorState.isKeyguardLocked
                    }

                    firstIndicatorState = false
                }
            }

            override fun setUserStyle(userStyle: UserStyleWireFormat) {
                uiThreadHandler.runOnHandler {
                    watchFace.onSetStyleInternal(
                        UserStyle(userStyle, watchFace.userStyleRepository.userStyleCategories)
                    )
                }
            }

            override fun setImmutableSystemState(immutableSystemState: ImmutableSystemState) {
                uiThreadHandler.runOnHandler {
                    // These properties never change so set them once only.
                    if (!immutableSystemStateDone) {
                        mutableWatchState.hasLowBitAmbient = immutableSystemState.hasLowBitAmbient
                        mutableWatchState.hasBurnInProtection =
                            immutableSystemState.hasBurnInProtection

                        immutableSystemStateDone = true
                    }
                }
            }

            @SuppressLint("SyntheticAccessor")
            override fun setComplicationData(complicationId: Int, data: ComplicationData) {
                uiThreadHandler.runOnHandler {
                    if (watchFaceCreated()) {
                        watchFace.onComplicationDataUpdate(complicationId, data)
                    } else {
                        pendingComplicationDataUpdates.add(
                            PendingComplicationData(complicationId, data)
                        )
                    }
                }
            }

            override fun requestWatchFaceStyle() {
                uiThreadHandler.runOnHandler {
                    try {
                        iWatchFaceService.setStyle(watchFace.watchFaceStyle)
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
            }

            override fun takeWatchfaceScreenshot(
                rendererParametersWireFormat: RenderParametersWireFormat,
                compressionQuality: Int,
                calendarTimeMillis: Long,
                idAndComplicationData: List<IdAndComplicationData>?,
                userStyle: UserStyleWireFormat?
            ): Bundle {
                return uiThreadHandler.runOnHandler {
                    val oldStyle = HashMap(watchFace.userStyleRepository.userStyle.options)
                    if (userStyle != null) {
                        watchFace.onSetStyleInternal(
                            UserStyle(userStyle, watchFace.userStyleRepository.userStyleCategories)
                        )
                    }

                    val oldComplicationData =
                        watchFace.complicationsManager.complications.mapValues {
                            it.value.renderer.getData() ?: ComplicationData.Builder(TYPE_NO_DATA)
                                .build()
                        }
                    if (idAndComplicationData != null) {
                        for (idAndData in idAndComplicationData) {
                            watchFace.onComplicationDataUpdate(
                                idAndData.id, idAndData.complicationData
                            )
                        }
                    }

                    val bitmap = watchFace.renderer.takeScreenshot(
                        Calendar.getInstance().apply {
                            timeInMillis = calendarTimeMillis
                        },
                        RenderParameters(rendererParametersWireFormat)
                    )

                    // Restore previous style & complications if required.
                    if (userStyle != null) {
                        watchFace.onSetStyleInternal(UserStyle(oldStyle))
                    }

                    if (idAndComplicationData != null) {
                        for ((id, data) in oldComplicationData) {
                            watchFace.onComplicationDataUpdate(id, data)
                        }
                    }

                    bitmap
                }.toAshmemCompressedImageBundle(
                    compressionQuality
                )
            }

            override fun takeComplicationScreenshot(
                complicationId: Int,
                rendererParametersWireFormat: RenderParametersWireFormat,
                compressionQuality: Int,
                calendarTimeMillis: Long,
                complicationData: ComplicationData?,
                userStyle: UserStyleWireFormat?
            ): Bundle? {
                return uiThreadHandler.runOnHandler {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = calendarTimeMillis
                    }
                    val complication = watchFace.complicationsManager[complicationId]
                    if (complication != null) {
                        val oldStyle = HashMap(watchFace.userStyleRepository.userStyle.options)
                        if (userStyle != null) {
                            watchFace.onSetStyleInternal(
                                UserStyle(
                                    userStyle,
                                    watchFace.userStyleRepository.userStyleCategories
                                )
                            )
                        }

                        val bounds = complication.computeBounds(watchFace.renderer.screenBounds)
                        val complicationBitmap =
                            Bitmap.createBitmap(
                                bounds.width(), bounds.height(),
                                Bitmap.Config.ARGB_8888
                            )

                        var prevComplicationData: ComplicationData? = null
                        if (complicationData != null) {
                            prevComplicationData = complication.renderer.getData()
                            complication.renderer.setData(complicationData)
                        }

                        complication.renderer.render(
                            Canvas(complicationBitmap),
                            Rect(0, 0, bounds.width(), bounds.height()),
                            calendar,
                            RenderParameters(rendererParametersWireFormat)
                        )

                        // Restore previous ComplicationData & style if required.
                        if (complicationData != null) {
                            complication.renderer.setData(prevComplicationData)
                        }

                        if (userStyle != null) {
                            watchFace.onSetStyleInternal(UserStyle(oldStyle))
                        }

                        complicationBitmap.toAshmemCompressedImageBundle(
                            compressionQuality
                        )
                    } else {
                        null
                    }
                }
            }

            override fun sendTouchEvent(xPos: Int, yPos: Int, tapType: Int) {
                uiThreadHandler.runOnHandler {
                    if (watchFaceCreated()) {
                        watchFace.onTapCommand(tapType, xPos, yPos)
                    }
                }
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
        }

        override fun onDestroy() {
            destroyed = true
            uiThreadHandler.removeCallbacks(invalidateRunnable)
            choreographer.removeFrameCallback(frameCallback)

            if (timeTickRegistered) {
                timeTickRegistered = false
                unregisterReceiver(timeTickReceiver)
            }

            if (!watchFaceCreated()) {
                watchFace.onDestroy()
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
                Constants.COMMAND_AMBIENT_UPDATE -> watchFaceCommand.ambientUpdate()
                Constants.COMMAND_BACKGROUND_ACTION -> onBackgroundAction(extras!!)
                Constants.COMMAND_COMPLICATION_DATA -> onComplicationDataUpdate(extras!!)
                Constants.COMMAND_REQUEST_STYLE -> onRequestStyle()
                Constants.COMMAND_SET_BINDER -> onSetBinder(extras!!)
                Constants.COMMAND_SET_PROPERTIES -> onPropertiesChanged(extras!!)
                Constants.COMMAND_TAP -> watchFaceCommand.sendTouchEvent(x, y, TapType.TAP)
                Constants.COMMAND_TOUCH -> watchFaceCommand.sendTouchEvent(x, y, TapType.TOUCH)
                Constants.COMMAND_TOUCH_CANCEL -> watchFaceCommand.sendTouchEvent(
                    x,
                    y,
                    TapType.TOUCH_CANCEL
                )
                else -> {
                }
            }
            return null
        }

        fun onBackgroundAction(extras: Bundle) {
            // We can't guarantee the binder has been set and onSurfaceChanged called before this
            // command.
            if (!watchFaceCreated()) {
                pendingBackgroundAction = extras
                return
            }

            watchFaceCommand.setSystemState(
                SystemState(
                    extras.getBoolean(
                        Constants.EXTRA_AMBIENT_MODE,
                        mutableWatchState.isAmbient.getValueOr(false)
                    ),
                    extras.getInt(
                        Constants.EXTRA_INTERRUPTION_FILTER,
                        mutableWatchState.interruptionFilter.getValueOr(0)
                    ),
                    extras.getInt(
                        Constants.EXTRA_UNREAD_COUNT,
                        mutableWatchState.unreadNotificationCount.getValueOr(0)
                    ),
                    extras.getInt(
                        Constants.EXTRA_NOTIFICATION_COUNT,
                        mutableWatchState.notificationCount.getValueOr(0)
                    )
                )
            )

            val statusBundle = extras.getBundle(Constants.EXTRA_INDICATOR_STATUS)
            if (statusBundle != null) {
                watchFaceCommand.setIndicatorState(
                    IndicatorState(
                        statusBundle.getBoolean(Constants.STATUS_CHARGING),
                        statusBundle.getBoolean(Constants.STATUS_AIRPLANE_MODE),
                        statusBundle.getBoolean(Constants.STATUS_CONNECTED),
                        statusBundle.getBoolean(Constants.STATUS_THEATER_MODE),
                        statusBundle.getBoolean(Constants.STATUS_GPS_ACTIVE),
                        statusBundle.getBoolean(Constants.STATUS_KEYGUARD_LOCKED)
                    )
                )
            }

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

            if (systemApiVersion >= 3) {
                val bundle = Bundle().apply {
                    putBinder(
                        Constants.EXTRA_WATCH_FACE_COMMAND_BINDER,
                        watchFaceCommand.asBinder()
                    )
                }
                iWatchFaceService.registerIWatchFaceCommand(bundle)
            }

            maybeCreateWatchFace()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            currentSurfaceHolder = holder
            currentSurfaceFormat = format
            currentSurfaceWidth = width
            currentSurfaceHeight = height

            if (watchFaceCreated()) {
                watchFace.onSurfaceChanged(holder, format, width, height)
            } else {
                maybeCreateWatchFace()
            }
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            if (watchFaceCreated()) {
                watchFace.onSurfaceRedrawNeeded()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            if (watchFaceCreated()) {
                watchFace.renderer.onSurfaceDestroyed(holder)
            }
        }

        private fun maybeCreateWatchFace() {
            // To simplify handling of watch face state, we only construct the [WatchFace]
            // once both currentSurfaceHolder and iWatchFaceService have been initialized.
            if (this::currentSurfaceHolder.isInitialized &&
                this::iWatchFaceService.isInitialized && pendingProperties != null &&
                !watchFaceCreated()
            ) {
                watchFaceInitStarted = true

                // Apply immutable properties to mutableWatchState before creating the watch face.
                onPropertiesChanged(pendingProperties!!)
                pendingProperties = null

                val host = WatchFaceHost()
                host.api = this
                watchFace = createWatchFace(
                    currentSurfaceHolder,
                    host,
                    mutableWatchState.asWatchState()
                )

                // Watchfaces especially OpenGL ones often do initialization in
                // onSurfaceChanged, make sure we send the initial one.
                watchFace.renderer.onSurfaceChanged(
                    currentSurfaceHolder,
                    currentSurfaceFormat,
                    currentSurfaceWidth,
                    currentSurfaceHeight
                )

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
                    watchFaceCommand.setComplicationData(
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
            watchFaceCommand.requestWatchFaceStyle()
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
                watchFace.invalidate()
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
                watchFace.onDraw()
            } finally {
                if (TRACE_DRAW) {
                    Trace.endSection()
                }
            }
        }

        private fun onComplicationDataUpdate(extras: Bundle) {
            extras.classLoader = ComplicationData::class.java.classLoader
            watchFaceCommand.setComplicationData(
                extras.getInt(Constants.EXTRA_COMPLICATION_ID),
                (extras.getParcelable(Constants.EXTRA_COMPLICATION_DATA) as ComplicationData?)!!
            )
        }

        internal fun onPropertiesChanged(properties: Bundle) {
            if (!watchFaceInitStarted) {
                pendingProperties = properties
                maybeCreateWatchFace()
                return
            }

            watchFaceCommand.setImmutableSystemState(
                ImmutableSystemState(
                    properties.getBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT),
                    properties.getBoolean(Constants.PROPERTY_BURN_IN_PROTECTION)
                )
            )
        }

        internal fun watchFaceCreated() = this::watchFace.isInitialized

        override fun setDefaultComplicationProviderWithFallbacks(
            watchFaceComplicationId: Int,
            providers: List<ComponentName>?,
            @ProviderId fallbackSystemProvider: Int,
            type: Int
        ) {
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
                if (fallbackSystemProvider != WatchFace.NO_DEFAULT_PROVIDER) {
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
            lastA11yLabels = labels
            try {
                iWatchFaceService.setContentDescriptionLabels(labels)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to set accessibility labels: ", e)
            }
        }

        override fun registerWatchFaceType(@WatchFaceType watchFaceType: Int) {
            if (systemApiVersion >= 3) {
                iWatchFaceService.registerWatchFaceType(watchFaceType)
            }
        }

        override fun registerUserStyleSchema(userStyleSchema: UserStyleSchemaWireFormat) {
            if (systemApiVersion >= 3) {
                iWatchFaceService.registerUserStyleSchema(userStyleSchema)
            }
        }

        override fun setCurrentUserStyle(
            userStyle: UserStyleWireFormat
        ) {
            if (systemApiVersion >= 3) {
                iWatchFaceService.setCurrentUserStyle(userStyle)
            }
        }

        override fun getStoredUserStyle(): UserStyleWireFormat? {
            if (systemApiVersion < 3) {
                return null
            }
            return iWatchFaceService.storedUserStyle
        }

        @SuppressLint("WrongConstant")
        override fun setComplicationDetails(
            complicationId: Int,
            bounds: Rect,
            @ComplicationBoundsType boundsType: Int,
            types: IntArray
        ) {
            if (systemApiVersion >= 3) {
                iWatchFaceService.setComplicationDetails(
                    complicationId,
                    ComplicationDetails(bounds, boundsType, types)
                )
            }
        }
    }
}

/**
 * Runs the supplied task on the handler thread. If we're not on the handler thread a task is posted
 * and we block until it's been processed.
 *
 * AIDL calls are dispatched from a thread pool, but for simplicity WatchFace code is
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
