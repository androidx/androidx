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
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
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
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.toApiComplicationData
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.ObservableWatchData.MutableObservableWatchData
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.WatchFaceLayer
import kotlinx.coroutines.CompletableDeferred
import java.security.InvalidParameterException
import kotlin.math.max

// Human reaction time is limited to ~100ms.
private const val MIN_PERCEPTABLE_DELAY_MILLIS = 100

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

        /** The UTC reference time to use for previews in milliseconds since the epoch. */
        public val previewReferenceTimeMillis: Long

        /** The [Handler] for the background thread. */
        public val backgroundThreadHandler: Handler

        /** Renders the watchface to a [Bitmap] with the [CurrentUserStyleRepository]'s [UserStyle]. */
        public fun renderWatchFaceToBitmap(
            renderParameters: RenderParameters,
            calendarTimeMillis: Long,
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
         * @param xPos the horizontal position in pixels on the screen where the touch happened
         * @param yPos the vertical position in pixels on the screen where the touch happened
         */
        @UiThread
        public fun onTap(
            @TapType tapType: Int,
            @Px xPos: Int,
            @Px yPos: Int
        )
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

    /** The UTC preview time in milliseconds since the epoch, or null if not set. */
    @get:SuppressWarnings("AutoBoxing")
    @IntRange(from = 0)
    public var overridePreviewReferenceTimeMillis: Long? = null
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
    }

    /**
     * Overrides the reference time for editor preview images.
     *
     * @param previewReferenceTimeMillis The UTC preview time in milliseconds since the epoch
     */
    public fun setOverridePreviewReferenceTimeMillis(
        @IntRange(from = 0) previewReferenceTimeMillis: Long
    ): WatchFace = apply {
        overridePreviewReferenceTimeMillis = previewReferenceTimeMillis
    }

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

internal data class MockTime(var speed: Double, var minTime: Long, var maxTime: Long)

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("SyntheticAccessor")
public class WatchFaceImpl @UiThread constructor(
    watchface: WatchFace,
    private val watchFaceHostApi: WatchFaceHostApi,
    private val watchState: WatchState,
    internal val currentUserStyleRepository: CurrentUserStyleRepository,
    internal var complicationSlotsManager: ComplicationSlotsManager,

    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public val calendar: Calendar,
    private val broadcastsObserver: BroadcastsObserver,
    internal var broadcastsReceiver: BroadcastsReceiver?
) {
    internal companion object {
        internal const val NO_DEFAULT_PROVIDER = SystemProviders.NO_PROVIDER

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

        // Complications are highlighted when tapped and after this delay the highlight is removed.
        internal const val CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS = 300L

        // The threshold used to judge whether the battery is low during initialization.  Ideally
        // we would use the threshold for Intent.ACTION_BATTERY_LOW but it's not documented or
        // available programmatically. The value below is the default but it could be overridden
        // by OEMs.
        internal const val INITIAL_LOW_BATTERY_THRESHOLD = 15.0f

        internal val defaultRenderParametersForDrawMode: HashMap<DrawMode, RenderParameters> =
            hashMapOf(
                DrawMode.AMBIENT to
                    RenderParameters(
                        DrawMode.AMBIENT, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null
                    ),
                DrawMode.INTERACTIVE to
                    RenderParameters(
                        DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null
                    ),
                DrawMode.LOW_BATTERY_INTERACTIVE to
                    RenderParameters(
                        DrawMode.LOW_BATTERY_INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                        null
                    ),
                DrawMode.MUTE to
                    RenderParameters(
                        DrawMode.MUTE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null
                    ),
            )
    }

    private val systemTimeProvider = watchface.systemTimeProvider
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
        calendar.timeZone = TimeZone.getDefault()
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

    /** The UTC reference time for editor preview images in milliseconds since the epoch. */
    public val previewReferenceTimeMillis: Long =
        watchface.overridePreviewReferenceTimeMillis ?: when (watchface.watchFaceType) {
            WatchFaceType.ANALOG -> watchState.analogPreviewReferenceTimeMillis
            WatchFaceType.DIGITAL -> watchState.digitalPreviewReferenceTimeMillis
            else -> throw InvalidParameterException("Unrecognized watchFaceType")
        }

    private var inOnSetStyle = false
    internal var initComplete = false

    private val ambientObserver = Observer<Boolean> {
        TraceEvent("WatchFaceImpl.ambientObserver").use {
            // It's not safe to draw until initComplete because the ComplicationSlotManager init
            // may not have completed.
            if (initComplete) {
                onDraw()
            }
            scheduleDraw()
        }
    }

    private val interruptionFilterObserver = Observer<Int> {
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

    private val visibilityObserver = Observer<Boolean> { isVisible ->
        TraceEvent("WatchFaceImpl.visibilityObserver").use {
            if (isVisible) {
                registerReceivers()
                // Update time zone in case it changed while we weren't visible.
                calendar.timeZone = TimeZone.getDefault()
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
    private val batteryLowAndNotChargingObserver = Observer<Boolean> {
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
        renderer.uiThreadInitInternal()

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

        watchState.isAmbient.addObserver(ambientObserver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !watchState.isHeadless) {
            watchState.isBatteryLowAndNotCharging.addObserver(batteryLowAndNotChargingObserver)
        }
        watchState.interruptionFilter.addObserver(interruptionFilterObserver)
        watchState.isVisible.addObserver(visibilityObserver)
    }

    internal fun invalidateIfNotAnimating() {
        // Ensure we render a frame if the ComplicationSlot needs rendering, e.g.
        // because it loaded an image. However if we're animating there's no need
        // to trigger an extra invalidation.
        if (!renderer.shouldAnimate() || computeDelayTillNextFrame(
                nextDrawTimeMillis,
                systemTimeProvider.getSystemTimeMillis()
            ) > MIN_PERCEPTABLE_DELAY_MILLIS
        ) {
            watchFaceHostApi.invalidate()
        }
    }

    internal fun createWFEditorDelegate() = WFEditorDelegate() as WatchFace.EditorDelegate

    internal inner class WFEditorDelegate : WatchFace.EditorDelegate {
        override val userStyleSchema: UserStyleSchema
            get() = currentUserStyleRepository.schema

        override var userStyle: UserStyle
            get() = currentUserStyleRepository.userStyle
            set(value) {
                currentUserStyleRepository.userStyle = value
            }

        override val complicationSlotsManager: ComplicationSlotsManager
            get() = this@WatchFaceImpl.complicationSlotsManager

        override val screenBounds
            get() = renderer.screenBounds

        override val previewReferenceTimeMillis
            get() = this@WatchFaceImpl.previewReferenceTimeMillis

        override val backgroundThreadHandler
            get() = watchFaceHostApi.getBackgroundThreadHandler()

        override fun renderWatchFaceToBitmap(
            renderParameters: RenderParameters,
            calendarTimeMillis: Long,
            slotIdToComplicationData: Map<Int, ComplicationData>?
        ): Bitmap = TraceEvent("WFEditorDelegate.takeScreenshot").use {
            val oldComplicationData =
                complicationSlotsManager.complicationSlots.values.associateBy(
                    { it.id },
                    { it.renderer.getData() }
                )

            slotIdToComplicationData?.let {
                for ((id, complicationData) in it) {
                    complicationSlotsManager[id]!!.renderer.loadData(complicationData, false)
                }
            }
            val screenShot = renderer.takeScreenshot(
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = calendarTimeMillis
                },
                renderParameters
            )
            if (slotIdToComplicationData != null) {
                for ((id, data) in oldComplicationData) {
                    complicationSlotsManager[id]!!.renderer.loadData(data, false)
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
        val isBatteryLowAndNotCharging =
            watchState.isBatteryLowAndNotCharging as MutableObservableWatchData
        isBatteryLowAndNotCharging.value =
            (batteryPercent < INITIAL_LOW_BATTERY_THRESHOLD) && !isCharging
    }

    /**
     * Called by the system in response to remote configuration, on the main thread.
     */
    internal fun onSetStyleInternal(style: UserStyle) {
        // No need to echo the userStyle back.
        inOnSetStyle = true
        currentUserStyleRepository.userStyle = style
        inOnSetStyle = false
    }

    internal fun onDestroy() {
        pendingUpdateTime.cancel()
        renderer.onDestroy()
        watchState.isAmbient.removeObserver(ambientObserver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !watchState.isHeadless) {
            watchState.isBatteryLowAndNotCharging.removeObserver(batteryLowAndNotChargingObserver)
        }
        watchState.interruptionFilter.removeObserver(interruptionFilterObserver)
        watchState.isVisible.removeObserver(visibilityObserver)
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

        setCalendarTime(systemTimeProvider.getSystemTimeMillis())
        if (renderer.shouldAnimate()) {
            pendingUpdateTime.postUnique {
                watchFaceHostApi.invalidate()
            }
        }
    }

    /**
     * Sets the calendar's time in milliseconds adjusted by the mock time controls.
     */
    private fun setCalendarTime(timeMillis: Long) {
        // This adjustment allows time to be sped up or slowed down and to wrap between two
        // instants. This is useful when developing animations that occur infrequently (e.g.
        // hourly).
        val millis = (mockTime.speed * (timeMillis - mockTime.minTime).toDouble()).toLong()
        val range = mockTime.maxTime - mockTime.minTime
        var delta = millis % range
        if (delta < 0) {
            delta += range
        }
        calendar.timeInMillis = mockTime.minTime + delta
    }

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
        if (watchState.isAmbient.value && !renderer.shouldAnimate()) {
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
        setCalendarTime(systemTimeProvider.getSystemTimeMillis())
        maybeUpdateDrawMode()
        renderer.renderInternal(calendar)

        val currentTimeMillis = systemTimeProvider.getSystemTimeMillis()
        setCalendarTime(currentTimeMillis)
        if (renderer.shouldAnimate()) {
            val delayMillis = computeDelayTillNextFrame(nextDrawTimeMillis, currentTimeMillis)
            nextDrawTimeMillis = currentTimeMillis + delayMillis
            pendingUpdateTime.postDelayedUnique(delayMillis) { watchFaceHostApi.invalidate() }
        }
    }

    internal fun onSurfaceRedrawNeeded() {
        setCalendarTime(systemTimeProvider.getSystemTimeMillis())
        maybeUpdateDrawMode()
        renderer.renderInternal(calendar)
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
            //  beginFrameTimeMillis to keep the animation smooth.
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

    /** Clears all [ComplicationData]. */
    @UiThread
    internal fun clearComplicationData() {
        complicationSlotsManager.clearComplicationData()
        watchFaceHostApi.invalidate()
    }

    /**
     * Called when a tap or touch related event occurs. Detects taps on [ComplicationSlot]s and
     * triggers the associated action.
     *
     * @param tapType Value representing the event sent to the wallpaper
     * @param x X coordinate of the event
     * @param y Y coordinate of the event
     */
    @UiThread
    internal fun onTapCommand(
        @TapType tapType: Int,
        x: Int,
        y: Int
    ) {
        val tappedComplication = complicationSlotsManager.getComplicationSlotAt(x, y)
        if (tappedComplication == null) {
            // The event does not belong to any of the complicationSlots, pass to the listener.
            lastTappedComplicationId = null
            tapListener?.onTap(tapType, x, y)
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
                complicationSlotsManager.displayPressedAnimation(tappedComplication.id)
                complicationSlotsManager.onComplicationSlotSingleTapped(tappedComplication.id)
                watchFaceHostApi.invalidate()
                lastTappedComplicationId = null
            }
            TapType.DOWN -> {
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

    @UiThread
    @RequiresApi(27)
    internal fun renderWatchFaceToBitmap(
        params: WatchFaceRenderParams
    ): Bundle = TraceEvent("WatchFaceImpl.renderWatchFaceToBitmap").use {
        val oldStyle = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
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
                complicationSlotsManager[idAndData.id]!!.renderer
                    .loadData(idAndData.complicationData.toApiComplicationData(), false)
            }
        }

        val bitmap = renderer.takeScreenshot(
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = params.calendarTimeMillis
            },
            RenderParameters(params.renderParametersWireFormat)
        )

        // Restore previous style & complicationSlots if required.
        if (params.userStyle != null) {
            onSetStyleInternal(UserStyle(oldStyle))
        }

        if (params.idAndComplicationDatumWireFormats != null) {
            for ((id, data) in oldComplicationData) {
                complicationSlotsManager[id]!!.renderer.loadData(data, false)
            }
        }

        return SharedMemoryImage.ashmemWriteImageBundle(bitmap)
    }

    @UiThread
    @RequiresApi(27)
    internal fun renderComplicationToBitmap(
        params: ComplicationRenderParams
    ): Bundle? = TraceEvent("WatchFaceImpl.renderComplicationToBitmap").use {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = params.calendarTimeMillis
        }
        return complicationSlotsManager[params.complicationSlotId]?.let {
            val oldStyle = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
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
                it.renderer.loadData(
                    screenshotComplicationData.toApiComplicationData(),
                    false
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
                it.renderer.loadData(prevData, false)
            }

            if (newStyle != null) {
                onSetStyleInternal(UserStyle(oldStyle))
            }

            SharedMemoryImage.ashmemWriteImageBundle(complicationBitmap)
        }
    }

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("WatchFaceImpl ($componentName): ")
        writer.increaseIndent()
        writer.println("calendar=$calendar")
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
