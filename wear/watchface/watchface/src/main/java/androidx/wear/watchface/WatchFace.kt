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
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.support.wearable.watchface.SharedMemoryImage
import android.support.wearable.watchface.WatchFaceStyle
import android.view.Gravity
import android.view.Surface
import android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.control.HeadlessWatchFaceImpl
import androidx.wear.watchface.control.RemoteWatchFaceView
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.utility.TraceEvent
import java.lang.Long.min
import java.security.InvalidParameterException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Human reaction time is limited to ~100ms.
private const val MIN_PERCEPTIBLE_DELAY_MILLIS = 100

// Zero is a special value meaning we will accept the system's choice for the
// display frame rate, which is the default behavior if this function isn't called.
private const val SYSTEM_DECIDES_FRAME_RATE = 0f

/**
 * The type of watch face, whether it's digital or analog. This influences the time displayed for
 * remote previews.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@IntDef(value = [WatchFaceType.DIGITAL, WatchFaceType.ANALOG])
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
 *   the default time for editor preview screenshots.
 * @param renderer The [Renderer] for this WatchFace.
 */
public class WatchFace(
    @WatchFaceType public var watchFaceType: Int,
    public val renderer: Renderer
) {
    internal var tapListener: TapListener? = null
    internal var complicationDeniedDialogIntent: Intent? = null
    internal var complicationRationaleDialogIntent: Intent? = null

    public companion object {
        /** Returns whether [LegacyWatchFaceOverlayStyle] is supported on this device. */
        @JvmStatic
        public fun isLegacyWatchFaceOverlayStyleSupported(): Boolean = Build.VERSION.SDK_INT <= 27

        private val componentNameToEditorDelegate = HashMap<ComponentName, EditorDelegate>()

        private var pendingComponentName: ComponentName? = null
        private var pendingEditorDelegateCB: CompletableDeferred<EditorDelegate>? = null

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

        @JvmStatic
        @UiThread
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        public fun clearAllEditorDelegates() {
            componentNameToEditorDelegate.clear()
        }

        /** For use by on watch face editors. */
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

        @UiThread
        internal fun createWatchFaceServiceOld(componentName: ComponentName): WatchFaceService {
            // Attempt to construct the class for the specified watchFaceName, failing if it either
            // doesn't exist or isn't a [WatchFaceService].
            val watchFaceServiceClass =
                Class.forName(componentName.className)
                    ?: throw IllegalArgumentException("Can't create ${componentName.className}")
            if (!WatchFaceService::class.java.isAssignableFrom(watchFaceServiceClass)) {
                throw IllegalArgumentException(
                    "${componentName.className} is not a WatchFaceService"
                )
            } else {
                return watchFaceServiceClass.getConstructor().newInstance() as WatchFaceService
            }
        }

        @SuppressLint("NewApi")
        @Suppress("DEPRECATION") // queryIntentServices
        @UiThread
        internal fun createWatchFaceService(
            componentName: ComponentName,
            context: Context
        ): WatchFaceService {
            // Resolve the WatchFaceControlService and construct WatchFaceService using its API
            val services =
                context.packageManager.queryIntentServices(
                    Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                        setPackage(context.packageName)
                    },
                    0
                )

            if (services.size != 1)
                throw IllegalArgumentException(
                    "WatchFaceControlService cannot be uniquely resolved (${services.size}) for " +
                        context.packageName
                )

            val watchFaceControlServiceClass =
                Class.forName(services[0].serviceInfo.name)
                    ?: throw IllegalArgumentException("Can't find ${services[0].serviceInfo.name}")

            val watchFaceControlService =
                watchFaceControlServiceClass.getConstructor().newInstance()
                    as WatchFaceControlService

            return watchFaceControlService.createWatchFaceService(componentName)
                ?: throw IllegalArgumentException("Can't create ${componentName.className}")
        }

        /** For use by on watch face editors. */
        @SuppressLint("NewApi")
        @JvmStatic
        @UiThread
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public suspend fun createHeadlessSessionDelegate(
            componentName: ComponentName,
            params: HeadlessWatchFaceInstanceParams,
            context: Context
        ): EditorDelegate {
            val watchFaceService =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        createWatchFaceService(componentName, context)
                    } else {
                        createWatchFaceServiceOld(componentName)
                    }
                    .apply { setContext(context) }

            val engine = watchFaceService.createHeadlessEngine() as WatchFaceService.EngineWrapper
            val headlessWatchFaceImpl = engine.createHeadlessInstance(params)
            return engine.deferredWatchFaceImpl.await().WFEditorDelegate(headlessWatchFaceImpl)
        }
    }

    /** Delegate used by on watch face editors. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface EditorDelegate {
        /** The [WatchFace]'s [UserStyleSchema]. */
        public val userStyleSchema: UserStyleSchema

        /** The watch face's [UserStyle]. */
        public var userStyle: UserStyle

        /** The [WatchFace]'s [ComplicationSlotsManager]. */
        public val complicationSlotsManager: ComplicationSlotsManager

        /** The [WatchFace]'s screen bounds [Rect]. */
        public val screenBounds: Rect

        /** Th reference [Instant] for previews. */
        public val previewReferenceInstant: Instant

        /** The [Handler] for the background thread. */
        public val backgroundThreadHandler: Handler

        /** [Intent] to launch the complication permission denied dialog. */
        public val complicationDeniedDialogIntent: Intent?

        /** [Intent] to launch the complication permission request rationale dialog. */
        public val complicationRationaleDialogIntent: Intent?

        /**
         * Renders the watchface to a [Bitmap] with the [CurrentUserStyleRepository]'s [UserStyle].
         */
        public fun renderWatchFaceToBitmap(
            renderParameters: RenderParameters,
            instant: Instant,
            slotIdToComplicationData: Map<Int, ComplicationData>?
        ): Bitmap

        /** Signals that the activity is going away and resources should be released. */
        public fun onDestroy()

        /** Sets a callback to observe an y changes to [ComplicationSlot.configExtras]. */
        public fun setComplicationSlotConfigExtrasChangeCallback(
            callback: ComplicationSlotConfigExtrasChangeCallback?
        )
    }

    /** Used to inform EditorSession about changes to [ComplicationSlot.configExtras]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface ComplicationSlotConfigExtrasChangeCallback {
        public fun onComplicationSlotConfigExtrasChanged()
    }

    /** Listens for taps on the watchface. */
    public interface TapListener {
        /**
         * Called whenever the user taps on the watchface.
         *
         * The watch face receives three different types of touch events:
         * - [TapType.DOWN] when the user puts the finger down on the touchscreen
         * - [TapType.UP] when the user lifts the finger from the touchscreen
         * - [TapType.CANCEL] when the system detects that the user is performing a gesture other
         *   than a tap
         *
         * Note that the watch face is only given tap events, i.e., events where the user puts the
         * finger down on the screen and then lifts it at the position. If the user performs any
         * other type of gesture while their finger in on the touchscreen, the watch face will be
         * receive a cancel, as all other gestures are reserved by the system.
         *
         * Therefore, a [TapType.DOWN] event and the successive [TapType.UP] event are guaranteed to
         * be close enough to be considered a tap according to the value returned by
         * [android.view.ViewConfiguration.getScaledTouchSlop].
         *
         * If the watch face receives a [TapType.CANCEL] event, it should not trigger any action, as
         * the system is already processing the gesture.
         *
         * @param tapType The type of touch event sent to the watch face
         * @param tapEvent The received [TapEvent]
         * @param complicationSlot The [ComplicationSlot] tapped if any or `null` otherwise
         */
        @UiThread
        public fun onTapEvent(
            @TapType tapType: Int,
            tapEvent: TapEvent,
            complicationSlot: ComplicationSlot?
        )
    }

    /**
     * Legacy Wear 2.0 watch face styling. These settings will be ignored on Wear 3.0 devices.
     *
     * @param viewProtectionMode The view protection mode bit field, must be a combination of zero
     *   or more of [WatchFaceStyle.PROTECT_STATUS_BAR], [WatchFaceStyle.PROTECT_HOTWORD_INDICATOR],
     *   [WatchFaceStyle.PROTECT_WHOLE_SCREEN].
     * @param statusBarGravity Controls the position of status icons (battery state, lack of
     *   connection) on the screen. This must be any combination of horizontal Gravity constant:
     *   ([Gravity.LEFT], [Gravity.CENTER_HORIZONTAL], [Gravity.RIGHT]) and vertical Gravity
     *   constants ([Gravity.TOP], [Gravity.CENTER_VERTICAL], [Gravity.BOTTOM]), e.g.
     *   `[Gravity.LEFT] | [Gravity.BOTTOM]`. On circular screens, only the vertical gravity is
     *   respected.
     * @param tapEventsAccepted Controls whether this watch face accepts tap events. Watchfaces that
     *   set this `true` are indicating they are prepared to receive [TapType.DOWN],
     *   [TapType.CANCEL], and [TapType.UP] events.
     * @param accentColor The accent color which will be used when drawing the unread notification
     *   indicator. Default color is white.
     * @throws IllegalArgumentException if [viewProtectionMode] has an unexpected value
     */
    public class LegacyWatchFaceOverlayStyle
    @JvmOverloads
    constructor(
        public val viewProtectionMode: Int,
        public val statusBarGravity: Int,
        @get:JvmName("isTapEventsAccepted") public val tapEventsAccepted: Boolean,
        @ColorInt public val accentColor: Int = WatchFaceStyle.DEFAULT_ACCENT_COLOR
    ) {
        init {
            if (
                viewProtectionMode < 0 ||
                    viewProtectionMode >
                        WatchFaceStyle.PROTECT_STATUS_BAR +
                            WatchFaceStyle.PROTECT_HOTWORD_INDICATOR +
                            WatchFaceStyle.PROTECT_WHOLE_SCREEN
            ) {
                throw IllegalArgumentException(
                    "View protection must be combination " +
                        "PROTECT_STATUS_BAR, PROTECT_HOTWORD_INDICATOR or PROTECT_WHOLE_SCREEN"
                )
            }
        }
    }

    /** The legacy [LegacyWatchFaceOverlayStyle] which only affects Wear 2.0 devices. */
    public var legacyWatchFaceStyle: LegacyWatchFaceOverlayStyle =
        LegacyWatchFaceOverlayStyle(0, 0, true)
        private set

    /** Sets the legacy [LegacyWatchFaceOverlayStyle] which only affects Wear 2.0 devices. */
    public fun setLegacyWatchFaceStyle(
        legacyWatchFaceStyle: LegacyWatchFaceOverlayStyle
    ): WatchFace = apply { this.legacyWatchFaceStyle = legacyWatchFaceStyle }

    /**
     * This class allows the watch face to configure the status overlay which is rendered by the
     * system on top of the watch face. These settings are applicable from Wear 3.0 and will be
     * ignored on earlier devices.
     */
    public class OverlayStyle(
        /**
         * The background color of the status indicator tray. This can be any color, including
         * [Color.TRANSPARENT]. If this is `null` then the system default will be used.
         */
        val backgroundColor: Color?,

        /**
         * The background color of items rendered in the status indicator tray. If not `null` then
         * this must be either [Color.BLACK] or [Color.WHITE]. If this is `null` then the system
         * default will be used.
         */
        val foregroundColor: Color?
    ) {

        public constructor() : this(null, null)

        init {
            require(
                foregroundColor == null ||
                    foregroundColor.toArgb() == Color.BLACK ||
                    foregroundColor.toArgb() == Color.WHITE
            ) {
                "foregroundColor must be one of null, Color.BLACK or Color.WHITE"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OverlayStyle

            if (backgroundColor != other.backgroundColor) return false
            if (foregroundColor != other.foregroundColor) return false

            return true
        }

        override fun hashCode(): Int {
            var result = backgroundColor?.hashCode() ?: 0
            result = 31 * result + (foregroundColor?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "OverlayStyle(backgroundColor=$backgroundColor, " +
                "foregroundColor=$foregroundColor)"
        }

        @UiThread
        internal fun dump(writer: IndentingPrintWriter) {
            writer.println("OverlayStyle:")
            writer.increaseIndent()
            writer.println("backgroundColor=$backgroundColor")
            writer.println("foregroundColor=$foregroundColor")
            writer.decreaseIndent()
        }
    }

    /** The [OverlayStyle] which affects Wear 3.0 devices and beyond. */
    public var overlayStyle: OverlayStyle = OverlayStyle()
        private set

    /** Sets the [OverlayStyle] which affects Wear 3.0 devices and beyond. */
    public fun setOverlayStyle(watchFaceOverlayStyle: OverlayStyle): WatchFace = apply {
        this.overlayStyle = watchFaceOverlayStyle
    }

    /**
     * The [Instant] to use for preview rendering, or `null` if not set in which case the system
     * chooses the Instant to use.
     */
    public var overridePreviewReferenceInstant: Instant? = null
        private set

    /**
     * Overrides the reference time for editor preview images.
     *
     * @param previewReferenceTimeMillis The UTC preview time in milliseconds since the epoch
     */
    public fun setOverridePreviewReferenceInstant(previewReferenceTimeMillis: Instant): WatchFace =
        apply {
            overridePreviewReferenceInstant = previewReferenceTimeMillis
        }

    /**
     * Sets an optional [TapListener] which if not `null` gets called on the ui thread whenever the
     * user taps on the watchface.
     */
    @SuppressWarnings("ExecutorRegistration")
    public fun setTapListener(tapListener: TapListener?): WatchFace = apply {
        this.tapListener = tapListener
    }

    /**
     * Sets the [Intent] to launch an activity which explains the watch face needs permission to
     * display complications. It is recommended the activity have a button which launches an intent
     * with [Settings.ACTION_APPLICATION_DETAILS_SETTINGS] to allow the user to grant permissions if
     * they wish.
     *
     * This [complicationDeniedDialogIntent] is launched when the user tries to configure a
     * complication slot when the `com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA`
     * permission has been denied. If the intent is not set or is `null` then no dialog will be
     * displayed.
     */
    public fun setComplicationDeniedDialogIntent(
        complicationDeniedDialogIntent: Intent?
    ): WatchFace = apply { this.complicationDeniedDialogIntent = complicationDeniedDialogIntent }

    /**
     * Sets the [Intent] to launch an activity that explains the rational for the requesting the
     * com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA` permission prior to
     * requesting it, if [Activity.shouldShowRequestPermissionRationale] returns `true`.
     *
     * If the intent is not set or is `null` then no dialog will be displayed.
     */
    public fun setComplicationRationaleDialogIntent(
        complicationRationaleDialogIntent: Intent?
    ): WatchFace = apply {
        this.complicationRationaleDialogIntent = complicationRationaleDialogIntent
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("SyntheticAccessor")
public class WatchFaceImpl
@UiThread
constructor(
    watchface: WatchFace,
    private val watchFaceHostApi: WatchFaceHostApi,
    private val watchState: WatchState,
    internal val currentUserStyleRepository: CurrentUserStyleRepository,
    @get:VisibleForTesting public var complicationSlotsManager: ComplicationSlotsManager,
    internal val broadcastsObserver: BroadcastsObserver,
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

        // Number of milliseconds before the target draw time for the delayed task to run and post a
        // choreographer frame. This is necessary when rendering at less than 60 fps to make sure we
        // post the choreographer frame in time to for us to render in the desired frame.
        // NOTE this value must be less than 16 or we'll render too early.
        internal const val POST_CHOREOGRAPHER_FRAME_MILLIS_BEFORE_DEADLINE = 10
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

    internal val systemTimeProvider = watchFaceHostApi.systemTimeProvider
    private val legacyWatchFaceStyle = watchface.legacyWatchFaceStyle
    internal val renderer = watchface.renderer
    private val tapListener = watchface.tapListener
    internal var complicationDeniedDialogIntent = watchface.complicationDeniedDialogIntent
    internal var complicationRationaleDialogIntent = watchface.complicationRationaleDialogIntent
    internal var overlayStyle = watchface.overlayStyle

    private var mockTime = MockTime(1.0, 0, Long.MAX_VALUE)

    private var lastTappedComplicationId: Int? = null

    // True if 'Do Not Disturb' mode is on.
    private var muteMode = false
    internal var lastDrawTimeMillis: Long = 0
    internal var nextDrawTimeMillis: Long = 0

    internal val componentName =
        ComponentName(
            watchFaceHostApi.getContext().packageName,
            watchFaceHostApi.getContext().javaClass.name
        )

    internal fun getWatchFaceStyle() =
        WatchFaceStyle(
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
        mockTime.speed =
            intent
                .getFloatExtra(EXTRA_MOCK_TIME_SPEED_MULTIPLIER, MOCK_TIME_DEFAULT_SPEED_MULTIPLIER)
                .toDouble()
        mockTime.minTime =
            intent.getLongExtra(
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
        watchface.overridePreviewReferenceInstant
            ?: Instant.ofEpochMilli(
                when (watchface.watchFaceType) {
                    WatchFaceType.ANALOG -> watchState.analogPreviewReferenceTimeMillis
                    WatchFaceType.DIGITAL -> watchState.digitalPreviewReferenceTimeMillis
                    else -> throw InvalidParameterException("Unrecognized watchFaceType")
                }
            )

    internal var initComplete = false

    private fun interruptionFilter(it: Int) {
        // We are in mute mode in any of the following modes. The specific mode depends on the
        // device's implementation of "Do Not Disturb".
        val inMuteMode =
            it == NotificationManager.INTERRUPTION_FILTER_NONE ||
                it == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                it == NotificationManager.INTERRUPTION_FILTER_ALARMS
        if (muteMode != inMuteMode) {
            muteMode = inMuteMode
            watchFaceHostApi.invalidate()
        }
    }

    internal fun onVisibility(isVisible: Boolean) {
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
                // We want to avoid a glimpse of a stale time when transitioning from hidden to
                // visible, so we render two black frames to clear the buffers.
                renderer.renderBlackFrame()
                renderer.renderBlackFrame()
                unregisterReceivers()
            }
        }
    }

    // Only installed if Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun batteryLowAndNotCharging(it: Boolean) {
        // To save power we request a lower hardware display frame rate when the battery is low
        // and not charging.
        if (renderer.surfaceHolder.surface.isValid) {
            SetFrameRateHelper.setFrameRate(
                renderer.surfaceHolder.surface,
                if (it) {
                    1000f / MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS.toFloat()
                } else {
                    SYSTEM_DECIDES_FRAME_RATE
                }
            )
        }
    }

    init {
        renderer.watchFaceHostApi = watchFaceHostApi

        if (
            renderer.additionalContentDescriptionLabels.isNotEmpty() ||
                complicationSlotsManager.complicationSlots.isEmpty()
        ) {
            watchFaceHostApi.updateContentDescriptionLabels()
        }

        if (!watchState.isHeadless) {
            WatchFace.registerEditorDelegate(
                componentName,
                WFEditorDelegate(headlessWatchFaceImpl = null)
            )
            registerReceivers()
        }

        val mainScope = CoroutineScope(Dispatchers.Main.immediate)

        mainScope.launch {
            watchState.isAmbient.collect {
                TraceEvent("WatchFaceImpl.ambient").use {
                    // It's not safe to draw until initComplete because the ComplicationSlotManager
                    // init may not have completed.
                    if (initComplete) {
                        onDraw()
                    }
                    scheduleDraw()
                }
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
    }

    internal fun invalidateIfNotAnimating() {
        // Ensure we render a frame if the ComplicationSlot needs rendering, e.g. because it loaded
        // an image. However if we're animating there's no need to trigger an extra invalidation.
        if (
            !renderer.shouldAnimate() ||
                computeDelayTillNextFrame(
                    nextDrawTimeMillis,
                    systemTimeProvider.getSystemTimeMillis(),
                    Instant.now()
                ) > MIN_PERCEPTIBLE_DELAY_MILLIS
        ) {
            watchFaceHostApi.invalidate()
        }
    }

    internal inner class WFEditorDelegate(
        private val headlessWatchFaceImpl: HeadlessWatchFaceImpl?
    ) : WatchFace.EditorDelegate {
        override val userStyleSchema
            get() = currentUserStyleRepository.schema

        override var userStyle: UserStyle
            get() = currentUserStyleRepository.userStyle.value
            set(value) {
                currentUserStyleRepository.updateUserStyle(value)
            }

        override val complicationSlotsManager
            get() = this@WatchFaceImpl.complicationSlotsManager

        override val screenBounds
            get() = renderer.screenBounds

        override val previewReferenceInstant
            get() = this@WatchFaceImpl.previewReferenceInstant

        override val backgroundThreadHandler
            get() = watchFaceHostApi.getBackgroundThreadHandler()

        override val complicationDeniedDialogIntent
            get() = watchFaceHostApi.getComplicationDeniedIntent()

        override val complicationRationaleDialogIntent
            get() = watchFaceHostApi.getComplicationRationaleIntent()

        override fun renderWatchFaceToBitmap(
            renderParameters: RenderParameters,
            instant: Instant,
            slotIdToComplicationData: Map<Int, ComplicationData>?
        ): Bitmap =
            TraceEvent("WFEditorDelegate.takeScreenshot").use {
                val oldComplicationData =
                    complicationSlotsManager.complicationSlots.values.associateBy(
                        { it.id },
                        { it.renderer.getData() }
                    )

                slotIdToComplicationData?.let {
                    for ((id, complicationData) in it) {
                        complicationSlotsManager.setComplicationDataUpdateSync(
                            id,
                            complicationData,
                            instant
                        )
                    }
                }
                val screenShot =
                    renderer.takeScreenshot(
                        ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")),
                        renderParameters
                    )
                slotIdToComplicationData?.let {
                    val now = getNow()
                    for ((id, complicationData) in oldComplicationData) {
                        complicationSlotsManager.setComplicationDataUpdateSync(
                            id,
                            complicationData,
                            now
                        )
                    }
                }
                return screenShot
            }

        override fun setComplicationSlotConfigExtrasChangeCallback(
            callback: WatchFace.ComplicationSlotConfigExtrasChangeCallback?
        ) {
            complicationSlotsManager.configExtrasChangeCallback = callback
        }

        @SuppressLint("NewApi") // release
        override fun onDestroy(): Unit =
            TraceEvent("WFEditorDelegate.onDestroy").use {
                if (watchState.isHeadless) {
                    headlessWatchFaceImpl!!.release()
                    this@WatchFaceImpl.onDestroy()
                }
            }
    }

    internal fun onDestroy() {
        renderer.onDestroyInternal()
        if (!watchState.isHeadless) {
            WatchFace.unregisterEditorDelegate(componentName)
        }
        unregisterReceivers()
    }

    @UiThread
    private fun registerReceivers() {
        // Looper can be null in some tests.
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
        // Looper can be null in some tests.
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
            watchFaceHostApi.postInvalidate()
        }
    }

    /** Gets the [ZonedDateTime] from [systemTimeProvider] adjusted by the mock time controls. */
    @UiThread
    private fun getZonedDateTime() =
        ZonedDateTime.ofInstant(getNow(), systemTimeProvider.getSystemTimeZoneId())

    /** Returns the current system time as provided by [systemTimeProvider] as an [Instant]. */
    internal fun getNow(): Instant =
        Instant.ofEpochMilli(mockTime.applyMockTime(systemTimeProvider.getSystemTimeMillis()))

    @UiThread
    internal fun maybeUpdateDrawMode() {
        var newDrawMode =
            if (watchState.isBatteryLowAndNotCharging.getValueOr(false)) {
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

    @UiThread
    fun onDraw() {
        val startTime = getZonedDateTime()
        val startInstant = startTime.toInstant()
        val startTimeMillis = systemTimeProvider.getSystemTimeMillis()
        maybeUpdateDrawMode()
        complicationSlotsManager.selectComplicationDataForInstant(startInstant)
        renderer.renderInternal(startTime)
        lastDrawTimeMillis = startTimeMillis

        if (renderer.shouldAnimate()) {
            val currentTimeMillis = systemTimeProvider.getSystemTimeMillis()
            var delayMillis =
                computeDelayTillNextFrame(startTimeMillis, currentTimeMillis, Instant.now())
            nextDrawTimeMillis = currentTimeMillis + delayMillis

            // We want to post our delayed task to post the choreographer frame a bit earlier than
            // the deadline because if we post it too close to the deadline we'll miss it. If we're
            // close to the deadline we post the choreographer frame immediately.
            delayMillis -= POST_CHOREOGRAPHER_FRAME_MILLIS_BEFORE_DEADLINE

            if (delayMillis <= 0) {
                watchFaceHostApi.invalidate()
            } else {
                watchFaceHostApi.postInvalidate(Duration.ofMillis(delayMillis))
            }
        }
    }

    internal fun onSurfaceRedrawNeeded() {
        maybeUpdateDrawMode()
        renderer.renderInternal(getZonedDateTime())
    }

    /**
     * @param startTimeMillis The SystemTime in milliseconds at which we started rendering
     * @param currentTimeMillis The current SystemTime in milliseconds
     * @param nowInstant The current [Instant].
     */
    @UiThread
    internal fun computeDelayTillNextFrame(
        startTimeMillis: Long,
        currentTimeMillis: Long,
        nowInstant: Instant
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

        var previousRequestedFrameTimeMillis = nextDrawTimeMillis

        // Its possible for nextDrawTimeMillis to be in the past (it's initialized to 0) or the
        // future (the user might have changed the system time) which we need to account for.
        val earliestSensiblePreviousRequestedFrameTimeMillis = startTimeMillis - updateRateMillis
        if (previousRequestedFrameTimeMillis < earliestSensiblePreviousRequestedFrameTimeMillis) {
            previousRequestedFrameTimeMillis = startTimeMillis
        }
        if (previousRequestedFrameTimeMillis > startTimeMillis) {
            previousRequestedFrameTimeMillis = startTimeMillis
        }

        // If the delay is long then round to the beginning of the next period.
        var nextFrameTimeMillis =
            if (updateRateMillis >= 500) {
                val nextUnroundedTime = previousRequestedFrameTimeMillis + updateRateMillis
                val delay = updateRateMillis - (nextUnroundedTime % updateRateMillis)
                previousRequestedFrameTimeMillis + delay
            } else {
                previousRequestedFrameTimeMillis + updateRateMillis
            }

        // If updateRateMillis is a multiple of 1 minute then align rendering to the beginning of
        // the minute.
        if ((updateRateMillis % 60000) == 0L) {
            nextFrameTimeMillis += (60000 - (nextFrameTimeMillis % 60000)) % 60000
        }

        var delayMillis = nextFrameTimeMillis - currentTimeMillis

        // Check if we need to render a frame sooner to support scheduled complication updates, e.g.
        // the stop watch complication.
        val nextComplicationChange = complicationSlotsManager.getNextChangeInstant(nowInstant)
        if (nextComplicationChange != Instant.MAX) {
            val nextComplicationChangeDelayMillis =
                max(0, nextComplicationChange.toEpochMilli() - nowInstant.toEpochMilli())
            delayMillis = min(delayMillis, nextComplicationChangeDelayMillis)
        }

        return delayMillis
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
        tapListener?.onTapEvent(tapType, tapEvent, tappedComplication)
        if (tappedComplication == null) {
            lastTappedComplicationId = null
            return
        }

        when (tapType) {
            TapType.UP -> {
                if (
                    tappedComplication.id != lastTappedComplicationId &&
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
    @RequiresApi(27)
    internal fun renderWatchFaceToBitmap(params: WatchFaceRenderParams): Bundle =
        TraceEvent("WatchFaceImpl.renderWatchFaceToBitmap").use {
            val oldStyle = currentUserStyleRepository.userStyle.value
            val instant = Instant.ofEpochMilli(params.calendarTimeMillis)

            params.userStyle?.let {
                currentUserStyleRepository.updateUserStyle(
                    UserStyle(UserStyleData(it), currentUserStyleRepository.schema)
                )
            }

            val oldComplicationData =
                complicationSlotsManager.complicationSlots.values.associateBy(
                    { it.id },
                    { it.renderer.getData() }
                )

            params.idAndComplicationDatumWireFormats?.let {
                for (idAndData in it) {
                    complicationSlotsManager.setComplicationDataUpdateSync(
                        idAndData.id,
                        idAndData.complicationData.toApiComplicationData(),
                        instant
                    )
                }
            }

            val bitmap =
                renderer.takeScreenshot(
                    ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")),
                    RenderParameters(params.renderParametersWireFormat)
                )

            // Restore previous style & complicationSlots if required.
            if (params.userStyle != null) {
                currentUserStyleRepository.updateUserStyle(oldStyle)
            }

            if (params.idAndComplicationDatumWireFormats != null) {
                val now = getNow()
                for ((id, complicationData) in oldComplicationData) {
                    complicationSlotsManager.setComplicationDataUpdateSync(
                        id,
                        complicationData,
                        now
                    )
                }
            }

            return SharedMemoryImage.ashmemWriteImageBundle(bitmap)
        }

    @UiThread
    internal fun createRemoteWatchFaceView(
        hostToken: IBinder,
        width: Int,
        height: Int
    ): RemoteWatchFaceView? =
        TraceEvent("WatchFaceImpl.createRemoteWatchFaceView").use {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return CreateRemoteWatchFaceViewHelper.createRemoteWatchFaceView(
                    watchFaceHostApi,
                    this,
                    hostToken,
                    width,
                    height
                )
            } else {
                return null
            }
        }

    @UiThread
    @RequiresApi(27)
    internal fun renderComplicationToBitmap(params: ComplicationRenderParams): Bundle? =
        TraceEvent("WatchFaceImpl.renderComplicationToBitmap").use {
            val zonedDateTime =
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(params.calendarTimeMillis),
                    ZoneId.of("UTC")
                )
            return complicationSlotsManager[params.complicationSlotId]?.let {
                val oldStyle = currentUserStyleRepository.userStyle.value
                val instant = Instant.ofEpochMilli(params.calendarTimeMillis)

                val newStyle = params.userStyle
                if (newStyle != null) {
                    currentUserStyleRepository.updateUserStyle(
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
                        screenshotComplicationData.toApiComplicationData(),
                        instant
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
                    val now = getNow()
                    complicationSlotsManager.setComplicationDataUpdateSync(
                        params.complicationSlotId,
                        prevData,
                        now
                    )
                }

                if (newStyle != null) {
                    currentUserStyleRepository.updateUserStyle(oldStyle)
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
        writer.println("lastDrawTimeMillis=$lastDrawTimeMillis")
        writer.println("nextDrawTimeMillis=$nextDrawTimeMillis")
        writer.println("muteMode=$muteMode")
        writer.println("lastTappedComplicationId=$lastTappedComplicationId")
        writer.println(
            "currentUserStyleRepository.userStyle=${currentUserStyleRepository.userStyle.value}"
        )
        writer.println("currentUserStyleRepository.schema=${currentUserStyleRepository.schema}")
        overlayStyle.dump(writer)
        watchState.dump(writer)
        complicationSlotsManager.dump(writer)
        renderer.dumpInternal(writer)
        broadcastsObserver.dump(writer)
        writer.decreaseIndent()
    }
}

@RequiresApi(Build.VERSION_CODES.R)
internal object CreateRemoteWatchFaceViewHelper {
    @Suppress("deprecation") // defaultDisplay
    internal fun createRemoteWatchFaceView(
        watchFaceHostApi: WatchFaceHostApi,
        watchFaceImpl: WatchFaceImpl,
        hostToken: IBinder,
        width: Int,
        height: Int
    ): RemoteWatchFaceView {
        val context = watchFaceHostApi.getContext()
        val host =
            SurfaceControlViewHost(
                context,
                context.getSystemService(WindowManager::class.java).defaultDisplay,
                hostToken
            )
        val view = SurfaceView(context)
        view.layoutParams =
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                )
                .apply { title = "RemoteWatchFaceView" }
        host.setView(view, width, height)
        return RemoteWatchFaceView(view, host, watchFaceHostApi.getUiThreadCoroutineScope()) {
            surfaceHolder,
            params ->
            val oldStyle = watchFaceImpl.currentUserStyleRepository.userStyle.value
            val instant = Instant.ofEpochMilli(params.calendarTimeMillis)

            params.userStyle?.let {
                watchFaceImpl.currentUserStyleRepository.updateUserStyle(
                    UserStyle(UserStyleData(it), watchFaceImpl.currentUserStyleRepository.schema)
                )
            }

            val oldComplicationData =
                watchFaceImpl.complicationSlotsManager.complicationSlots.values.associateBy(
                    { it.id },
                    { it.renderer.getData() }
                )

            params.idAndComplicationDatumWireFormats?.let {
                for (idAndData in it) {
                    watchFaceImpl.complicationSlotsManager.setComplicationDataUpdateSync(
                        idAndData.id,
                        idAndData.complicationData.toApiComplicationData(),
                        instant
                    )
                }
            }

            watchFaceImpl.renderer.renderScreenshotToSurface(
                ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")),
                RenderParameters(params.renderParametersWireFormat),
                surfaceHolder
            )

            // Restore previous style & complicationSlots if required.
            if (params.userStyle != null) {
                watchFaceImpl.currentUserStyleRepository.updateUserStyle(oldStyle)
            }

            if (params.idAndComplicationDatumWireFormats != null) {
                val now = watchFaceImpl.getNow()
                for ((id, complicationData) in oldComplicationData) {
                    watchFaceImpl.complicationSlotsManager.setComplicationDataUpdateSync(
                        id,
                        complicationData,
                        now
                    )
                }
            }
        }
    }
}

internal class SetFrameRateHelper {
    @RequiresApi(Build.VERSION_CODES.R)
    companion object {
        fun setFrameRate(surface: Surface, frameRate: Float) {
            surface.setFrameRate(frameRate, FRAME_RATE_COMPATIBILITY_DEFAULT)
        }
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
