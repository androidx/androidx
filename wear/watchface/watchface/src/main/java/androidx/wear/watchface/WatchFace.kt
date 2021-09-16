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
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.wearable.watchface.SharedMemoryImage
import android.support.wearable.watchface.WatchFaceStyle
import android.view.Gravity
import android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.WatchFaceLayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max

// Human reaction time is limited to ~100ms.
private const val MIN_PERCEPTIBLE_DELAY_MILLIS = 100

// Zero is a special value meaning we will accept the system's choice for the
// display frame rate, which is the default behavior if this function isn't called.
private const val SYSTEM_DECIDES_FRAME_RATE = 0f

/**
 * The type of watch face, whether it's digital or analog. This influences the time displayed for
 * remote previews.
 *
 * @hide
 */
@IntDef(
    value = [
        WatchFaceType.DIGITAL,
        WatchFaceType.ANALOG
    ]
)
public annotation class WatchFaceType {
    public companion object {
        /* The WatchFace has an analog time display. */
        public const val ANALOG: Int = 0

        /* The WatchFace has a digital time display. */
        public const val DIGITAL: Int = 1
    }
}

/**
 * The return value of [WatchFaceService.createWatchFace] which brings together rendering, styling,
 * complicationSlots and state observers.
 *
 * @param watchFaceType The type of watch face, whether it's digital or analog. Used to determine
 * the default time for editor preview screenshots.
 * @param renderer The [Renderer] for this WatchFace.
 */
public class WatchFace(
    @WatchFaceType public var watchFaceType: Int,
    public val renderer: Renderer
) {
    internal var tapListener: TapListener? = null

    public companion object {
        /** Returns whether [LegacyWatchFaceOverlayStyle] is supported on this device. */
        @JvmStatic
        public fun isLegacyWatchFaceOverlayStyleSupported(): Boolean = Build.VERSION.SDK_INT <= 27

        private val componentNameToEditorDelegate = HashMap<ComponentName, EditorDelegate>()

        private var pendingComponentName: ComponentName? = null
        private var pendingEditorDelegateCB: CompletableDeferred<EditorDelegate>? = null

        /** @hide */
        @JvmStatic
        @UiThread
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun registerEditorDelegate(
            componentName: ComponentName,
            editorDelegate: EditorDelegate
        ) {
            componentNameToEditorDelegate[componentName] = editorDelegate

            if (componentName == pendingComponentName) {
                pendingEditorDelegateCB?.complete(editorDelegate)
            } else {
                pendingEditorDelegateCB?.completeExceptionally(
                    IllegalStateException(
                        "Expected $pendingComponentName to be created but got $componentName"
                    )
                )
            }
            pendingComponentName = null
            pendingEditorDelegateCB = null
        }

        internal fun unregisterEditorDelegate(componentName: ComponentName) {
            componentNameToEditorDelegate.remove(componentName)
        }

        /** @hide */
        @JvmStatic
        @UiThread
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public fun clearAllEditorDelegates() {
            componentNameToEditorDelegate.clear()
        }

        /**
         * For use by on watch face editors.
         * @hide
         */
        @JvmStatic
        @UiThread
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun getOrCreateEditorDelegate(
            componentName: ComponentName
        ): CompletableDeferred<EditorDelegate> {
            componentNameToEditorDelegate[componentName]?.let {
                return CompletableDeferred(it)
            }

            // There's no pre-existing watch face. We expect Home/SysUI to switch the watchface soon
            // so record a pending request...
            pendingComponentName = componentName
            pendingEditorDelegateCB = CompletableDeferred()
            return pendingEditorDelegateCB!!
        }

        /**
         * For use by on watch face editors.
         * @hide
         */
        @SuppressLint("NewApi")
        @JvmStatic
        @UiThread
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public suspend fun createHeadlessSessionDelegate(
            componentName: ComponentName,
            params: HeadlessWatchFaceInstanceParams,
            context: Context
        ): EditorDelegate {
            // Attempt to construct the class for the specified watchFaceName, failing if it either
            // doesn't exist or isn't a [WatchFaceService].
            val watchFaceServiceClass =
                Class.forName(componentName.className) ?: throw IllegalArgumentException(
                    "Can't create ${componentName.className}"
                )
            if (!WatchFaceService::class.java.isAssignableFrom(WatchFaceService::class.java)) {
                throw IllegalArgumentException(
                    "${componentName.className} is not a WatchFaceService"
                )
            } else {
                val watchFaceService =
                    watchFaceServiceClass.getConstructor().newInstance() as WatchFaceService
                watchFaceService.setContext(context)
                val engine = watchFaceService.createHeadlessEngine() as
                    WatchFaceService.EngineWrapper
                engine.createHeadlessInstance(params)
                return engine.deferredWatchFaceImpl.await().WFEditorDelegate()
            }
        }
    }

    /**
     * Delegate used by on watch face editors.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface EditorDelegate {
        /** The [WatchFace]'s [UserStyleSchema]. */
        public val userStyleSchema: UserStyleSchema

        /** The watch face's  [UserStyle]. */
        public var userStyle: UserStyle

        /** The [WatchFace]'s [ComplicationSlotsManager]. */
        public val complicationSlotsManager: ComplicationSlotsManager

        /** The [WatchFace]'s screen bounds [Rect]. */
        public val screenBounds: Rect

        /** Th reference [Instant] for previews. */
        public val previewReferenceInstant: Instant

        /** The [Handler] for the background thread. */
        public val backgroundThreadHandler: Handler

        /** Renders the watchface to a [Bitmap] with the [CurrentUserStyleRepository]'s [UserStyle]. */
        public fun renderWatchFaceToBitmap(
            renderParameters: RenderParameters,
            instant: Instant,
            slotIdToComplicationData: Map<Int, ComplicationData>?
        ): Bitmap

        /** Signals that the activity is going away and resources should be released. */
        public fun onDestroy()
    }

    /**
     * Interface for getting the current system time.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface SystemTimeProvider {
        /** Returns the current system time in milliseconds. */
        public fun getSystemTimeMillis(): Long

        /** Returns the current system [ZoneId]. */
        public fun getSystemTimeZoneId(): ZoneId
    }

    /** Listens for taps on the watchface which didn't land on [ComplicationSlot]s. */
    public interface TapListener {
        /**
         * Called whenever the user taps on the watchface but doesn't hit a [ComplicationSlot].
         *
         * The watch face receives three different types of touch events:
         * - [TapType.DOWN] when the user puts the finger down on the touchscreen
         * - [TapType.UP] when the user lifts the finger from the touchscreen
         * - [TapType.CANCEL] when the system detects that the user is performing a gesture other
         *   than a tap
         *
         * Note that the watch face is only given tap events, i.e., events where the user puts
         * the finger down on the screen and then lifts it at the position. If the user performs any
         * other type of gesture while their finger in on the touchscreen, the watch face will be
         * receive a cancel, as all other gestures are reserved by the system.
         *
         * Therefore, a [TapType.DOWN] event and the successive [TapType.UP] event are guaranteed
         * to be close enough to be considered a tap according to the value returned by
         * [android.view.ViewConfiguration.getScaledTouchSlop].
         *
         * If the watch face receives a [TapType.CANCEL] event, it should not trigger any action, as
         * the system is already processing the gesture.
         *
         * @param tapType the type of touch event sent to the watch face
         * @param tapEvent the received [TapEvent]
         */
        @UiThread
        public fun onTapEvent(@TapType tapType: Int, tapEvent: TapEvent)
    }

    /**
     * Legacy Wear 2.0 watch face styling. These settings will be ignored on Wear 3.0 devices.
     *
     * @param viewProtectionMode The view protection mode bit field, must be a combination of zero
     * or more of [WatchFaceStyle.PROTECT_STATUS_BAR], [WatchFaceStyle.PROTECT_HOTWORD_INDICATOR],
     * [WatchFaceStyle.PROTECT_WHOLE_SCREEN].
     * @param statusBarGravity Controls the position of status icons (battery state, lack of
     * connection) on the screen. This must be any combination of horizontal Gravity constant:
     * ([Gravity.LEFT], [Gravity.CENTER_HORIZONTAL], [Gravity.RIGHT]) and vertical Gravity
     * constants ([Gravity.TOP], [Gravity.CENTER_VERTICAL], [Gravity.BOTTOM]), e.g.
     * `[Gravity.LEFT] | [Gravity.BOTTOM]`. On circular screens, only the vertical gravity is
     * respected.
     * @param tapEventsAccepted Controls whether this watch face accepts tap events. Watchfaces
     * that set this `true` are indicating they are prepared to receive [TapType.DOWN],
     * [TapType.CANCEL], and [TapType.UP] events.
     * @param accentColor The accent color which will be used when drawing the unread notification
     * indicator. Default color is white.
     * @throws IllegalArgumentException if [viewProtectionMode] has an unexpected value
     */
    public class LegacyWatchFaceOverlayStyle @JvmOverloads constructor(
        public val viewProtectionMode: Int,
        public val statusBarGravity: Int,
        @get:JvmName("isTapEventsAccepted")
        public val tapEventsAccepted: Boolean,
        @ColorInt public val accentColor: Int = WatchFaceStyle.DEFAULT_ACCENT_COLOR
    ) {
        init {
            if (viewProtectionMode < 0 ||
                viewProtectionMode >
                WatchFaceStyle.PROTECT_STATUS_BAR + WatchFaceStyle.PROTECT_HOTWORD_INDICATOR +
                WatchFaceStyle.PROTECT_WHOLE_SCREEN
            ) {
                throw IllegalArgumentException(
                    "View protection must be combination " +
                        "PROTECT_STATUS_BAR, PROTECT_HOTWORD_INDICATOR or PROTECT_WHOLE_SCREEN"
                )
            }
        }
    }

    /**
     * The [Instant] to use for preview rendering, or `null` if not set in which case the system
     * chooses the Instant to use.
     */
    public var overridePreviewReferenceInstant: Instant? = null
        private set

    /** The legacy [LegacyWatchFaceOverlayStyle] which only affects Wear 2.0 devices. */
    public var legacyWatchFaceStyle: LegacyWatchFaceOverlayStyle = LegacyWatchFaceOverlayStyle(
        0,
        0,
        true
    )
        private set

    internal var systemTimeProvider: SystemTimeProvider = object : SystemTimeProvider {
        override fun getSystemTimeMillis() = System.currentTimeMillis()

        override fun getSystemTimeZoneId() = ZoneId.systemDefault()
    }

    /**
     * Overrides the reference time for editor preview images.
     *
     * @param previewReferenceTimeMillis The UTC preview time in milliseconds since the epoch
     */
    public fun setOverridePreviewReferenceInstant(previewReferenceTimeMillis: Instant): WatchFace =
        apply { overridePreviewReferenceInstant = previewReferenceTimeMillis }

    /**
     * Sets the legacy [LegacyWatchFaceOverlayStyle] which only affects Wear 2.0 devices.
     */
    public fun setLegacyWatchFaceStyle(
        legacyWatchFaceStyle: LegacyWatchFaceOverlayStyle
    ): WatchFace = apply {
        this.legacyWatchFaceStyle = legacyWatchFaceStyle
    }

    /**
     * Sets an optional [TapListener] which if not `null` gets called on the ui thread whenever
     * the user taps on the watchface but doesn't hit a [ComplicationSlot].
     */
    @SuppressWarnings("ExecutorRegistration")
    public fun setTapListener(tapListener: TapListener?): WatchFace = apply {
        this.tapListener = tapListener
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun setSystemTimeProvider(systemTimeProvider: SystemTimeProvider): WatchFace = apply {
        this.systemTimeProvider = systemTimeProvider
    }
}

internal class MockTime(var speed: Double, var minTime: Long, var maxTime: Long) {
    /** Apply mock time adjustments. */
    fun applyMockTime(timeMillis: Long): Long {
        // This adjustment allows time to be sped up or slowed down and to wrap between two
        // instants. This is useful when developing animations that occur infrequently (e.g.
        // hourly).
        val millis = (speed * (timeMillis - minTime).toDouble()).toLong()
        val range = maxTime - minTime
        var delta = millis % range
        if (delta < 0) {
            delta += range
        }
        return minTime + delta
    }
}

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("SyntheticAccessor")
public class WatchFaceImpl @UiThread constructor(
    watchface: WatchFace,
    private val watchFaceHostApi: WatchFaceHostApi,
    private val watchState: WatchState,
    internal val currentUserStyleRepository: CurrentUserStyleRepository,

    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public var complicationSlotsManager: ComplicationSlotsManager,

    private val broadcastsObserver: BroadcastsObserver,
    internal var broadcastsReceiver: BroadcastsReceiver?
) {
    internal companion object {
        internal const val NO_DEFAULT_DATA_SOURCE = SystemDataSources.NO_DATA_SOURCE

        internal const val MOCK_TIME_INTENT = "androidx.wear.watchface.MockTime"

        // For debug purposes we support speeding up or slowing down time, these pair of constants
        // configure reading the mock time speed multiplier from a mock time intent.
        internal const val EXTRA_MOCK_TIME_SPEED_MULTIPLIER =
            "androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIER"
        private const val MOCK_TIME_DEFAULT_SPEED_MULTIPLIER = 1.0f

        // We support wrapping time between two instants, e.g. to loop an infrequent animation.
        // These constants configure reading this from a mock time intent.
        internal const val EXTRA_MOCK_TIME_WRAPPING_MIN_TIME =
            "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MIN_TIME"
        private const val MOCK_TIME_WRAPPING_MIN_TIME_DEFAULT = -1L
        internal const val EXTRA_MOCK_TIME_WRAPPING_MAX_TIME =
            "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MAX_TIME"

        // Many devices will enter Time Only Mode to reduce power consumption when the battery is
        // low, in which case only the system watch face will be displayed. On others there is a
        // battery saver mode triggering at 5% battery using an SCR to draw the display. For these
        // there's a gap of 10% battery (Intent.ACTION_BATTERY_LOW gets sent when < 15%) where we
        // clamp the framerate to a maximum of 10fps to conserve power.
        internal const val MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS = 100L

        // The threshold used to judge whether the battery is low during initialization.  Ideally
        // we would use the threshold for Intent.ACTION_BATTERY_LOW but it's not documented or
        // available programmatically. The value below is the default but it could be overridden
        // by OEMs.
        internal const val INITIAL_LOW_BATTERY_THRESHOLD = 15.0f
    }

    private val defaultRenderParametersForDrawMode: HashMap<DrawMode, RenderParameters> =
        hashMapOf(
            DrawMode.AMBIENT to
                RenderParameters(
                    DrawMode.AMBIENT,
                    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                    null,
                    complicationSlotsManager.lastComplicationTapDownEvents
                ),
            DrawMode.INTERACTIVE to
                RenderParameters(
                    DrawMode.INTERACTIVE,
                    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                    null,
                    complicationSlotsManager.lastComplicationTapDownEvents
                ),
            DrawMode.LOW_BATTERY_INTERACTIVE to
                RenderParameters(
                    DrawMode.LOW_BATTERY_INTERACTIVE,
                    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                    null,
                    complicationSlotsManager.lastComplicationTapDownEvents
                ),
            DrawMode.MUTE to
                RenderParameters(
                    DrawMode.MUTE,
                    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                    null,
                    complicationSlotsManager.lastComplicationTapDownEvents
                ),
        )

    internal val systemTimeProvider = watchface.systemTimeProvider
    private val legacyWatchFaceStyle = watchface.legacyWatchFaceStyle
    internal val renderer = watchface.renderer
    private val tapListener = watchface.tapListener

    private var mockTime = MockTime(1.0, 0, Long.MAX_VALUE)

    private var lastTappedComplicationId: Int? = null

    // True if 'Do Not Disturb' mode is on.
    private var muteMode = false
    private var nextDrawTimeMillis: Long = 0

    private val pendingUpdateTime: CancellableUniqueTask =
        CancellableUniqueTask(watchFaceHostApi.getUiThreadHandler())

    internal val componentName =
        ComponentName(
            watchFaceHostApi.getContext().packageName,
            watchFaceHostApi.getContext().javaClass.name
        )

    internal fun getWatchFaceStyle() = WatchFaceStyle(
        componentName,
        legacyWatchFaceStyle.viewProtectionMode,
        legacyWatchFaceStyle.statusBarGravity,
        legacyWatchFaceStyle.accentColor,
        false,
        false,
        legacyWatchFaceStyle.tapEventsAccepted
    )

    internal fun onActionTimeZoneChanged() {
        renderer.invalidate()
    }

    internal fun onActionTimeChanged() {
        // System time has changed hence next scheduled draw is invalid.
        nextDrawTimeMillis = systemTimeProvider.getSystemTimeMillis()
        renderer.invalidate()
    }

    internal fun onMockTime(intent: Intent) {
        mockTime.speed = intent.getFloatExtra(
            EXTRA_MOCK_TIME_SPEED_MULTIPLIER,
            MOCK_TIME_DEFAULT_SPEED_MULTIPLIER
        ).toDouble()
        mockTime.minTime = intent.getLongExtra(
            EXTRA_MOCK_TIME_WRAPPING_MIN_TIME,
            MOCK_TIME_WRAPPING_MIN_TIME_DEFAULT
        )
        // If MOCK_TIME_WRAPPING_MIN_TIME_DEFAULT is specified then use the current time.
        if (mockTime.minTime == MOCK_TIME_WRAPPING_MIN_TIME_DEFAULT) {
            mockTime.minTime = systemTimeProvider.getSystemTimeMillis()
        }
        mockTime.maxTime = intent.getLongExtra(EXTRA_MOCK_TIME_WRAPPING_MAX_TIME, Long.MAX_VALUE)
    }

    /** The reference [Instant] time for editor preview images in milliseconds since the epoch. */
    public val previewReferenceInstant: Instant =
        watchface.overridePreviewReferenceInstant ?: Instant.ofEpochMilli(
            when (watchface.watchFaceType) {
                WatchFaceType.ANALOG -> watchState.analogPreviewReferenceTimeMillis
                WatchFaceType.DIGITAL -> watchState.digitalPreviewReferenceTimeMillis
                else -> throw InvalidParameterException("Unrecognized watchFaceType")
            }
        )

    private var inOnSetStyle = false
    internal var initComplete = false

    private fun ambient() {
        TraceEvent("WatchFaceImpl.ambient").use {
            // It's not safe to draw until initComplete because the ComplicationSlotManager init
            // may not have completed.
            if (initComplete) {
                onDraw()
            }
            scheduleDraw()
        }
    }

    private fun interruptionFilter(it: Int) {
        // We are in mute mode in any of the following modes. The specific mode depends on the
        // device's implementation of "Do Not Disturb".
        val inMuteMode = it == NotificationManager.INTERRUPTION_FILTER_NONE ||
            it == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            it == NotificationManager.INTERRUPTION_FILTER_ALARMS
        if (muteMode != inMuteMode) {
            muteMode = inMuteMode
            watchFaceHostApi.invalidate()
        }
    }

    private fun visibility(isVisible: Boolean) {
        TraceEvent("WatchFaceImpl.visibility").use {
            if (isVisible) {
                registerReceivers()
                watchFaceHostApi.invalidate()

                // It's not safe to draw until initComplete because the ComplicationSlotManager init
                // may not have completed.
                if (initComplete) {
                    onDraw()
                }
                scheduleDraw()
            } else {
                unregisterReceivers()
            }
        }
    }

    // Only installed if Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    @SuppressLint("NewApi")
    private fun batteryLowAndNotCharging(it: Boolean) {
        // To save power we request a lower hardware display frame rate when the battery is low
        // and not charging.
        if (renderer.surfaceHolder.surface.isValid) {
            renderer.surfaceHolder.surface.setFrameRate(
                if (it) {
                    1000f / MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS.toFloat()
                } else {
                    SYSTEM_DECIDES_FRAME_RATE
                },
                FRAME_RATE_COMPATIBILITY_DEFAULT
            )
        }
    }

    init {
        renderer.watchFaceHostApi = watchFaceHostApi

        if (renderer.additionalContentDescriptionLabels.isNotEmpty()) {
            watchFaceHostApi.updateContentDescriptionLabels()
        }

        setIsBatteryLowAndNotChargingFromBatteryStatus(
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { iFilter ->
                watchFaceHostApi.getContext().registerReceiver(null, iFilter)
            }
        )

        if (!watchState.isHeadless) {
            WatchFace.registerEditorDelegate(componentName, WFEditorDelegate())
        }

        val mainScope = CoroutineScope(Dispatchers.Main.immediate)

        mainScope.launch {
            watchState.isAmbient.collect {
                ambient()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !watchState.isHeadless) {
            mainScope.launch {
                watchState.isBatteryLowAndNotCharging.collect {
                    if (it != null) {
                        batteryLowAndNotCharging(it)
                    }
                }
            }
        }

        mainScope.launch {
            watchState.interruptionFilter.collect {
                if (it != null) {
                    interruptionFilter(it)
                }
            }
        }

        mainScope.launch {
            watchState.isVisible.collect {
                if (it != null) {
                    visibility(it)
                }
            }
        }
    }

    internal fun invalidateIfNotAnimating() {
        // Ensure we render a frame if the ComplicationSlot needs rendering, e.g. because it loaded
        // an image. However if we're animating there's no need to trigger an extra invalidation.
        if (!renderer.shouldAnimate() || computeDelayTillNextFrame(
                nextDrawTimeMillis,
                systemTimeProvider.getSystemTimeMillis()
            ) > MIN_PERCEPTIBLE_DELAY_MILLIS
        ) {
            watchFaceHostApi.invalidate()
        }
    }

    internal inner class WFEditorDelegate : WatchFace.EditorDelegate {
        override val userStyleSchema: UserStyleSchema
            get() = currentUserStyleRepository.schema

        override var userStyle: UserStyle
            get() = currentUserStyleRepository.userStyle.value
            set(value) {
                currentUserStyleRepository.userStyle.value = value
            }

        override val complicationSlotsManager: ComplicationSlotsManager
            get() = this@WatchFaceImpl.complicationSlotsManager

        override val screenBounds
            get() = renderer.screenBounds

        override val previewReferenceInstant
            get() = this@WatchFaceImpl.previewReferenceInstant

        override val backgroundThreadHandler
            get() = watchFaceHostApi.getBackgroundThreadHandler()

        override fun renderWatchFaceToBitmap(
            renderParameters: RenderParameters,
            instant: Instant,
            slotIdToComplicationData: Map<Int, ComplicationData>?
        ): Bitmap = TraceEvent("WFEditorDelegate.takeScreenshot").use {
            val oldComplicationData =
                complicationSlotsManager.complicationSlots.values.associateBy(
                    { it.id },
                    { it.renderer.getData() }
                )

            slotIdToComplicationData?.let {
                for ((id, complicationData) in it) {
                    complicationSlotsManager.setComplicationDataUpdateSync(id, complicationData)
                }
            }
            val screenShot = renderer.takeScreenshot(
                ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")),
                renderParameters
            )
            if (slotIdToComplicationData != null) {
                for ((id, complicationData) in oldComplicationData) {
                    complicationSlotsManager.setComplicationDataUpdateSync(id, complicationData)
                }
            }
            return screenShot
        }

        override fun onDestroy(): Unit = TraceEvent("WFEditorDelegate.onDestroy").use {
            if (watchState.isHeadless) {
                this@WatchFaceImpl.onDestroy()
            }
        }
    }

    internal fun setIsBatteryLowAndNotChargingFromBatteryStatus(batteryStatus: Intent?) {
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val batteryPercent: Float = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        } ?: 100.0f
        val isBatteryLowAndNotCharging = watchState.isBatteryLowAndNotCharging as MutableStateFlow
        isBatteryLowAndNotCharging.value =
            (batteryPercent < INITIAL_LOW_BATTERY_THRESHOLD) && !isCharging
    }

    /** Called by the system in response to remote configuration. */
    @UiThread
    internal fun onSetStyleInternal(style: UserStyle) {
        // No need to echo the userStyle back.
        inOnSetStyle = true
        currentUserStyleRepository.userStyle.value = style
        inOnSetStyle = false
    }

    internal fun onDestroy() {
        pendingUpdateTime.cancel()
        renderer.onDestroy()
        if (!watchState.isHeadless) {
            WatchFace.unregisterEditorDelegate(componentName)
        }
        unregisterReceivers()
    }

    @UiThread
    private fun registerReceivers() {
        require(watchFaceHostApi.getUiThreadHandler().looper.isCurrentThread) {
            "registerReceivers must be called the UiThread"
        }

        // There's no point registering BroadcastsReceiver for headless instances.
        if (broadcastsReceiver == null && !watchState.isHeadless) {
            broadcastsReceiver =
                BroadcastsReceiver(watchFaceHostApi.getContext(), broadcastsObserver)
        }
    }

    @UiThread
    private fun unregisterReceivers() {
        require(watchFaceHostApi.getUiThreadHandler().looper.isCurrentThread) {
            "unregisterReceivers must be called the UiThread"
        }
        broadcastsReceiver?.onDestroy()
        broadcastsReceiver = null
    }

    private fun scheduleDraw() {
        // Separate calls are issued to deliver the state of isAmbient and isVisible, so during init
        // we might not yet know the state of both (which is required by the shouldAnimate logic).
        if (!watchState.isAmbient.hasValue() || !watchState.isVisible.hasValue()) {
            return
        }

        if (renderer.shouldAnimate()) {
            pendingUpdateTime.postUnique {
                watchFaceHostApi.invalidate()
            }
        }
    }

    /** Gets the [ZonedDateTime] from [systemTimeProvider] adjusted by the mock time controls. */
    @UiThread
    private fun getZonedDateTime() = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(mockTime.applyMockTime(systemTimeProvider.getSystemTimeMillis())),
        systemTimeProvider.getSystemTimeZoneId()
    )

    /** @hide */
    @UiThread
    internal fun maybeUpdateDrawMode() {
        var newDrawMode = if (watchState.isBatteryLowAndNotCharging.getValueOr(false)) {
            DrawMode.LOW_BATTERY_INTERACTIVE
        } else {
            DrawMode.INTERACTIVE
        }
        // Watch faces may wish to run an animation while entering ambient mode and we let them
        // defer entering ambient mode.
        if (watchState.isAmbient.value!! && !renderer.shouldAnimate()) {
            newDrawMode = DrawMode.AMBIENT
        } else if (muteMode) {
            newDrawMode = DrawMode.MUTE
        }

        if (renderer.renderParameters.drawMode != newDrawMode) {
            renderer.renderParameters = defaultRenderParametersForDrawMode[newDrawMode]!!
        }
    }

    /** @hide */
    @UiThread
    internal fun onDraw() {
        maybeUpdateDrawMode()
        renderer.renderInternal(getZonedDateTime())

        val currentTimeMillis = systemTimeProvider.getSystemTimeMillis()
        if (renderer.shouldAnimate()) {
            val delayMillis = computeDelayTillNextFrame(nextDrawTimeMillis, currentTimeMillis)
            nextDrawTimeMillis = currentTimeMillis + delayMillis
            pendingUpdateTime.postDelayedUnique(delayMillis) { watchFaceHostApi.invalidate() }
        }
    }

    internal fun onSurfaceRedrawNeeded() {
        maybeUpdateDrawMode()
        renderer.renderInternal(getZonedDateTime())
    }

    /** @hide */
    @UiThread
    internal fun computeDelayTillNextFrame(
        beginFrameTimeMillis: Long,
        currentTimeMillis: Long
    ): Long {
        // Limit update rate to conserve power when the battery is low and not charging.
        val updateRateMillis =
            if (watchState.isBatteryLowAndNotCharging.getValueOr(false)) {
                max(
                    renderer.interactiveDrawModeUpdateDelayMillis,
                    MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS
                )
            } else {
                renderer.interactiveDrawModeUpdateDelayMillis
            }
        // Note beginFrameTimeMillis could be in the future if the user adjusted the time so we need
        // to compute min(beginFrameTimeMillis, currentTimeMillis).
        var nextFrameTimeMillis =
            Math.min(beginFrameTimeMillis, currentTimeMillis) + updateRateMillis
        // Drop frames if needed (happens when onDraw is slow).
        if (nextFrameTimeMillis <= currentTimeMillis) {
            // Compute the next runtime after currentTimeMillis with the same phase as
            // beginFrameTimeMillis to keep the animation smooth.
            val phaseAdjust =
                updateRateMillis +
                    ((nextFrameTimeMillis - currentTimeMillis) % updateRateMillis)
            nextFrameTimeMillis = currentTimeMillis + phaseAdjust
        }
        return nextFrameTimeMillis - currentTimeMillis
    }

    /**
     * Called when new complication data is received.
     *
     * @param complicationSlotId The id of the [ComplicationSlot] that the data relates to.
     * @param data The [ComplicationData] that should be displayed in the complication.
     */
    @UiThread
    internal fun onComplicationSlotDataUpdate(complicationSlotId: Int, data: ComplicationData) {
        complicationSlotsManager.onComplicationDataUpdate(complicationSlotId, data)
        watchFaceHostApi.invalidate()
    }

    /**
     * Called when a tap or touch related event occurs. Detects taps on [ComplicationSlot]s and
     * triggers the associated action.
     *
     * @param tapType The [TapType] of the event
     * @param tapEvent The received [TapEvent]
     */
    @UiThread
    internal fun onTapCommand(@TapType tapType: Int, tapEvent: TapEvent) {
        val tappedComplication =
            complicationSlotsManager.getComplicationSlotAt(tapEvent.xPos, tapEvent.yPos)
        if (tappedComplication == null) {
            // The event does not belong to any of the complicationSlots, pass to the listener.
            lastTappedComplicationId = null
            tapListener?.onTapEvent(tapType, tapEvent)
            return
        }

        when (tapType) {
            TapType.UP -> {
                if (tappedComplication.id != lastTappedComplicationId &&
                    lastTappedComplicationId != null
                ) {
                    // The UP event belongs to a different complication then the DOWN event,
                    // do not consider this a tap on either of them.
                    lastTappedComplicationId = null
                    return
                }
                complicationSlotsManager.onComplicationSlotSingleTapped(tappedComplication.id)
                watchFaceHostApi.invalidate()
                lastTappedComplicationId = null
            }
            TapType.DOWN -> {
                complicationSlotsManager.onTapDown(tappedComplication.id, tapEvent)
                lastTappedComplicationId = tappedComplication.id
            }
            else -> lastTappedComplicationId = null
        }
    }

    @UiThread
    internal fun getComplicationState() = complicationSlotsManager.complicationSlots.map {
        IdAndComplicationStateWireFormat(
            it.key,
            ComplicationStateWireFormat(
                it.value.computeBounds(renderer.screenBounds),
                it.value.boundsType,
                ComplicationType.toWireTypes(it.value.supportedTypes),
                it.value.defaultDataSourcePolicy.dataSourcesAsList(),
                it.value.defaultDataSourcePolicy.systemDataSourceFallback,
                it.value.defaultDataSourceType.toWireComplicationType(),
                it.value.enabled,
                it.value.initiallyEnabled,
                it.value.renderer.getData().type.toWireComplicationType(),
                it.value.fixedComplicationDataSource,
                it.value.configExtras
            )
        )
    }

    @UiThread
    @RequiresApi(27)
    internal fun renderWatchFaceToBitmap(
        params: WatchFaceRenderParams
    ): Bundle = TraceEvent("WatchFaceImpl.renderWatchFaceToBitmap").use {
        val oldStyle = currentUserStyleRepository.userStyle.value

        params.userStyle?.let {
            onSetStyleInternal(UserStyle(UserStyleData(it), currentUserStyleRepository.schema))
        }

        val oldComplicationData =
            complicationSlotsManager.complicationSlots.values.associateBy(
                { it.id },
                { it.renderer.getData() }
            )

        params.idAndComplicationDatumWireFormats?.let {
            for (idAndData in it) {
                complicationSlotsManager.setComplicationDataUpdateSync(
                    idAndData.id, idAndData.complicationData.toApiComplicationData()
                )
            }
        }

        val bitmap = renderer.takeScreenshot(
            ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(params.calendarTimeMillis),
                ZoneId.of("UTC")
            ),
            RenderParameters(params.renderParametersWireFormat)
        )

        // Restore previous style & complicationSlots if required.
        if (params.userStyle != null) {
            onSetStyleInternal(oldStyle)
        }

        if (params.idAndComplicationDatumWireFormats != null) {
            for ((id, complicationData) in oldComplicationData) {
                complicationSlotsManager.setComplicationDataUpdateSync(id, complicationData)
            }
        }

        return SharedMemoryImage.ashmemWriteImageBundle(bitmap)
    }

    @UiThread
    @RequiresApi(27)
    internal fun renderComplicationToBitmap(
        params: ComplicationRenderParams
    ): Bundle? = TraceEvent("WatchFaceImpl.renderComplicationToBitmap").use {
        val zonedDateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(params.calendarTimeMillis),
            ZoneId.of("UTC")
        )
        return complicationSlotsManager[params.complicationSlotId]?.let {
            val oldStyle = currentUserStyleRepository.userStyle.value

            val newStyle = params.userStyle
            if (newStyle != null) {
                onSetStyleInternal(
                    UserStyle(UserStyleData(newStyle), currentUserStyleRepository.schema)
                )
            }

            val bounds = it.computeBounds(renderer.screenBounds)
            val complicationBitmap =
                Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)

            var prevData: ComplicationData? = null
            val screenshotComplicationData = params.complicationData
            if (screenshotComplicationData != null) {
                prevData = it.renderer.getData()
                complicationSlotsManager.setComplicationDataUpdateSync(
                    params.complicationSlotId,
                    screenshotComplicationData.toApiComplicationData()
                )
            }

            it.renderer.render(
                Canvas(complicationBitmap),
                Rect(0, 0, bounds.width(), bounds.height()),
                zonedDateTime,
                RenderParameters(params.renderParametersWireFormat),
                params.complicationSlotId
            )

            // Restore previous ComplicationData & style if required.
            if (prevData != null) {
                complicationSlotsManager.setComplicationDataUpdateSync(
                    params.complicationSlotId,
                    prevData
                )
            }

            if (newStyle != null) {
                onSetStyleInternal(oldStyle)
            }

            SharedMemoryImage.ashmemWriteImageBundle(complicationBitmap)
        }
    }

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("WatchFaceImpl ($componentName): ")
        writer.increaseIndent()
        writer.println("mockTime.maxTime=${mockTime.maxTime}")
        writer.println("mockTime.minTime=${mockTime.minTime}")
        writer.println("mockTime.speed=${mockTime.speed}")
        writer.println("nextDrawTimeMillis=$nextDrawTimeMillis")
        writer.println("muteMode=$muteMode")
        writer.println("pendingUpdateTime=${pendingUpdateTime.isPending()}")
        writer.println("lastTappedComplicationId=$lastTappedComplicationId")
        writer.println(
            "currentUserStyleRepository.userStyle=${currentUserStyleRepository.userStyle}"
        )
        writer.println("currentUserStyleRepository.schema=${currentUserStyleRepository.schema}")
        watchState.dump(writer)
        complicationSlotsManager.dump(writer)
        renderer.dump(writer)
        writer.decreaseIndent()
    }
}

internal fun <Boolean> StateFlow<Boolean?>.getValueOr(default: Boolean): Boolean {
    return if (hasValue()) {
        value!!
    } else {
        default
    }
}

internal fun <T> StateFlow<T>.hasValue(): Boolean = value != null
