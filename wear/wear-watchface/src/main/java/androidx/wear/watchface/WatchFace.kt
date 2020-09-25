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
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.support.wearable.complications.ComplicationData
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.ui.WatchFaceConfigActivity
import androidx.wear.watchface.ui.WatchFaceConfigDelegate
import java.io.FileNotFoundException
import java.io.InputStreamReader
import kotlin.math.max

// Human reaction time is limited to ~100ms.
private const val MIN_PERCEPTABLE_DELAY_MILLIS = 100

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
annotation class WatchFaceType {
    companion object {
        /* The WatchFace has an analog time display. */
        const val ANALOG = 0

        /* The WatchFace has a digital time display. */
        const val DIGITAL = 1
    }
}

private fun readPrefs(context: Context, fileName: String): UserStyleWireFormat {
    val hashMap = HashMap<String, String>()
    try {
        val reader = InputStreamReader(context.openFileInput(fileName)).buffered()
        while (true) {
            val key = reader.readLine() ?: break
            val value = reader.readLine() ?: break
            hashMap[key] = value
        }
        reader.close()
    } catch (e: FileNotFoundException) {
        // We don't need to do anything special here.
    }
    return UserStyleWireFormat(hashMap)
}

private fun writePrefs(context: Context, fileName: String, style: UserStyle) {
    val writer = context.openFileOutput(fileName, Context.MODE_PRIVATE).bufferedWriter()
    for ((key, value) in style.options) {
        writer.write(key.id)
        writer.newLine()
        writer.write(value.id)
        writer.newLine()
    }
    writer.close()
}

/**
 * A WatchFace is constructed by a user's [WatchFaceService] and brings together rendering,
 * styling, complications and state observers.
 */
@SuppressLint("SyntheticAccessor")
class WatchFace private constructor(
    @WatchFaceType private val watchFaceType: Int,
    private var interactiveUpdateRateMillis: Long,
    internal val userStyleRepository: UserStyleRepository,
    internal var complicationsManager: ComplicationsManager,
    internal val renderer: Renderer,
    private val watchFaceHostApi: WatchFaceHostApi,
    private val watchState: WatchState,
    // Not to be confused with a user style.
    internal val watchFaceStyle: WatchFaceStyle,
    private val componentName: ComponentName,
    private val systemTimeProvider: SystemTimeProvider
) {
    /**
     * Interface for getting the current system time.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    interface SystemTimeProvider {
        /** Returns the current system time in milliseconds. */
        fun getSystemTimeMillis(): Long
    }

    /**
     * Builder for a [WatchFace].
     *
     * If unreadCountIndicator or notificationIndicator are hidden then the WatchState class will
     * receive updates necessary for the watch to draw its own indicators.
     */
    class Builder(
        /** The type of watch face, whether it's digital or analog. */
        @WatchFaceType private val watchFaceType: Int,

        /**
         * The interval in milliseconds between frames in interactive mode. To render at 60hz pass in
         * 16. Note when battery is low, the framerate will be clamped to 10fps. Watch faces are
         * recommended to use lower frame rates if possible for better battery life.
         */
        private var interactiveUpdateRateMillis: Long,

        /** The {@UserStyleRepository} for this WatchFace. */
        internal val userStyleRepository: UserStyleRepository,

        /** The [ComplicationsManager] for this WatchFace. */
        internal var complicationsManager: ComplicationsManager,

        /** The [Renderer] for this WatchFace. */
        internal val renderer: Renderer,

        /** Holder for the internal API the WatchFace uses to communicate with the host service.  */
        private val watchFaceHost: WatchFaceHost,

        /**
         * The [WatchState] of the device we're running on. Contains data needed to draw
         * surface indicators if we've opted to draw them ourselves (see [onCreateWatchFaceStyle]).
         */
        private val watchState: WatchState
    ) {
        private var viewProtectionMode: Int = 0
        private var statusBarGravity: Int = 0

        @ColorInt
        private var accentColor: Int = WatchFaceStyle.DEFAULT_ACCENT_COLOR
        private var showUnreadCountIndicator: Boolean = false
        private var hideNotificationIndicator: Boolean = false
        private var acceptsTapEvents: Boolean = true
        private var systemTimeProvider: SystemTimeProvider = object : SystemTimeProvider {
            override fun getSystemTimeMillis() = System.currentTimeMillis()
        }

        /**
         * @param viewProtectionMode The view protection mode bit field, must be a combination of
         *     zero or more of [PROTECT_STATUS_BAR], [PROTECT_HOTWORD_INDICATOR],
         *     [PROTECT_WHOLE_SCREEN].
         * @throws IllegalArgumentException if viewProtectionMode has an unexpected value
         */
        fun setViewProtectionMode(viewProtectionMode: Int) = apply {
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
            this.viewProtectionMode = viewProtectionMode
        }

        /**
         * Sets position of status icons (battery state, lack of connection) on the screen.
         *
         * @param statusBarGravity This must be any combination of horizontal Gravity constant
         *     ([Gravity.LEFT], [Gravity.CENTER_HORIZONTAL], [Gravity.RIGHT])
         *     and vertical Gravity constants ([Gravity.TOP], [Gravity,CENTER_VERTICAL},
         *     [Gravity,BOTTOM]), e.g. {@code Gravity.LEFT | Gravity.BOTTOM}. On circular screens,
         *     only the vertical gravity is respected.
         */
        fun setStatusBarGravity(statusBarGravity: Int) = apply {
            this.statusBarGravity = statusBarGravity
        }

        /**
         * Sets the accent color which can be set by developers to customise watch face. It will be
         * used when drawing the unread notification indicator. Default color is white.
         */
        fun setAccentColor(@ColorInt accentColor: Int) = apply {
            this.accentColor = accentColor
        }

        /**
         * Sets whether to add an indicator of how many unread cards there are in the stream. The
         * indicator will be displayed next to status icons (battery state, lack of connection).
         *
         * @param showUnreadCountIndicator if true an indicator will be shown
         */
        fun setShowUnreadCountIndicator(showUnreadCountIndicator: Boolean) = apply {
            this.showUnreadCountIndicator = showUnreadCountIndicator
        }

        /**
         * Sets whether to hide the dot indicator that is displayed at the bottom of the watch face
         * if there are any unread notifications. The default value is false, but note that the
         * dot will not be displayed if the numerical unread count indicator is being shown (i.e.
         * if [getShowUnreadCountIndicator] is true).
         *
         * @param hideNotificationIndicator if true an indicator will be hidden
         * @hide
         */
        fun setHideNotificationIndicator(hideNotificationIndicator: Boolean) = apply {
            this.hideNotificationIndicator = hideNotificationIndicator
        }

        /**
         * Sets whether this watchface accepts tap events. The default is false.
         *
         * <p>Watchfaces that set this {@code true} are indicating they are prepared to receive
         * [android.support.wearable.watchface.WatchFaceService.TAP_TYPE_TOUCH],
         * [android.support.wearable.watchface.WatchFaceService.TAP_TYPE_TOUCH_CANCEL], and
         * [android.support.wearable.watchface.WatchFaceService.TAP_TYPE_TAP] events.
         *
         * @param acceptsTapEvents whether to receive touch events.
         */
        fun setAcceptsTapEvents(acceptsTapEvents: Boolean) = apply {
            this.acceptsTapEvents = acceptsTapEvents
        }

        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
        fun setSystemTimeProvider(systemTimeProvider: SystemTimeProvider) = apply {
            this.systemTimeProvider = systemTimeProvider
        }

        /** Constructs the [WatchFace]. */
        fun build(): WatchFace {
            val componentName =
                ComponentName(
                    watchFaceHost.api!!.getContext().packageName,
                    watchFaceHost.api!!.getContext().javaClass.typeName
                )
            return WatchFace(
                watchFaceType,
                interactiveUpdateRateMillis,
                userStyleRepository,
                complicationsManager,
                renderer,
                watchFaceHost.api!!,
                watchState,
                WatchFaceStyle(
                    componentName,
                    viewProtectionMode,
                    statusBarGravity,
                    accentColor,
                    showUnreadCountIndicator,
                    hideNotificationIndicator,
                    acceptsTapEvents
                ),
                componentName,
                systemTimeProvider
            )
        }
    }

    internal companion object {
        internal const val NO_DEFAULT_PROVIDER = SystemProviders.NO_PROVIDER
        internal const val DEFAULT_PROVIDER_TYPE_NONE = -2

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
    }

    private data class MockTime(var speed: Double, var minTime: Long, var maxTime: Long)

    private var mockTime = MockTime(1.0, 0, Long.MAX_VALUE)

    private var lastTappedComplicationId: Int? = null
    private var lastTappedPosition: Point? = null
    private var registeredReceivers = false

    // True if NotificationManager.INTERRUPTION_FILTER_NONE.
    private var muteMode = false
    private var nextDrawTimeMillis: Long = 0

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val calendar: Calendar = Calendar.getInstance()

    private val pendingSingleTap: CancellableUniqueTask =
        CancellableUniqueTask(watchFaceHostApi.getHandler())
    private val pendingUpdateTime: CancellableUniqueTask =
        CancellableUniqueTask(watchFaceHostApi.getHandler())
    private val pendingPostDoubleTap: CancellableUniqueTask =
        CancellableUniqueTask(watchFaceHostApi.getHandler())

    private val timeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            calendar.timeZone = TimeZone.getDefault()
            invalidate()
        }
    }

    private val timeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // System time has changed hence next scheduled draw is invalid.
            nextDrawTimeMillis = systemTimeProvider.getSystemTimeMillis()
            invalidate()
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val batteryLevelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            val isBatteryLowAndNotCharging =
                watchState.isBatteryLowAndNotCharging as MutableWatchData
            when (intent.action) {
                Intent.ACTION_BATTERY_LOW -> isBatteryLowAndNotCharging.value = true
                Intent.ACTION_BATTERY_OKAY -> isBatteryLowAndNotCharging.value = false
                Intent.ACTION_POWER_CONNECTED -> isBatteryLowAndNotCharging.value = false
            }
            invalidate()
        }
    }

    /**
     * We listen for MOCK_TIME_INTENTs which we interpret as a request to modify time. E.g. speeding
     * up or slowing down time, and providing support for making time loop between two instants.
     * This is intended to help implement animations which may occur infrequently (e.g. hourly).
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val mockTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
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
            mockTime.maxTime =
                intent.getLongExtra(EXTRA_MOCK_TIME_WRAPPING_MAX_TIME, Long.MAX_VALUE)
        }
    }

    init {
        // If the system has a stored user style then Home/SysUI is in charge of style
        // persistence, otherwise we need to do our own.
        val storedUserStyle = watchFaceHostApi.getStoredUserStyle()
        if (storedUserStyle != null) {
            userStyleRepository.userStyle =
                UserStyle(storedUserStyle, userStyleRepository.userStyleCategories)
        } else {
            // The system doesn't support preference persistence we need to do it ourselves.
            val preferencesFile =
                "watchface_prefs_${watchFaceHostApi.getContext().javaClass.typeName}.txt"

            userStyleRepository.userStyle = UserStyle(
                readPrefs(watchFaceHostApi.getContext(), preferencesFile),
                userStyleRepository.userStyleCategories
            )

            userStyleRepository.addUserStyleListener(
                object : UserStyleRepository.UserStyleListener {
                    @SuppressLint("SyntheticAccessor")
                    override fun onUserStyleChanged(userStyle: UserStyle) {
                        writePrefs(watchFaceHostApi.getContext(), preferencesFile, userStyle)
                    }
                })
        }
    }

    private var inOnSetStyle = false

    private inner class WfUserStyleListener : UserStyleRepository.UserStyleListener {
        @SuppressWarnings("SyntheticAccessor")
        override fun onUserStyleChanged(userStyle: UserStyle) {
            // No need to echo the userStyle back.
            if (!inOnSetStyle) {
                sendCurrentUserStyle(userStyle)
            }
        }
    }

    private val styleListener = WfUserStyleListener()

    private fun sendCurrentUserStyle(userStyle: UserStyle) {
        // Sync the user style with the system.
        watchFaceHostApi.setCurrentUserStyle(userStyle.toWireFormat())
    }

    private val ambientObserver = Observer<Boolean> {
        scheduleDraw()
        invalidate()
    }

    private val interruptionFilterObserver = Observer<Int> {
        val inMuteMode = it == NotificationManager.INTERRUPTION_FILTER_NONE
        if (muteMode != inMuteMode) {
            muteMode = inMuteMode
            invalidate()
        }
    }

    private val visibilityObserver = Observer<Boolean> {
        if (it) {
            registerReceivers()
            // Update time zone in case it changed while we weren't visible.
            calendar.timeZone = TimeZone.getDefault()
            invalidate()
        } else {
            unregisterReceivers()
        }

        scheduleDraw()
    }

    init {
        // We need to inhibit an immediate callback during initialization because members are not
        // fully constructed and it will fail. It's also superfluous because we're going to render
        // anyway.
        var initFinished = false
        complicationsManager.init(watchFaceHostApi, calendar, renderer,
            object : CanvasComplicationRenderer.InvalidateCallback {
                @SuppressWarnings("SyntheticAccessor")
                override fun onInvalidate() {
                    // Ensure we render a frame if the Complication needs rendering, e.g. because it
                    // loaded an image. However if we're animating there's no need to trigger an
                    // extra invalidation.
                    if (renderer.shouldAnimate() &&
                        computeDelayTillNextFrame(
                            nextDrawTimeMillis,
                            systemTimeProvider.getSystemTimeMillis()
                        )
                        < MIN_PERCEPTABLE_DELAY_MILLIS
                    ) {
                        return
                    }
                    if (initFinished) {
                        this@WatchFace.invalidate()
                    }
                }
            }
        )

        WatchFaceConfigActivity.registerWatchFace(componentName, object : WatchFaceConfigDelegate {
            override fun getUserStyleSchema() = userStyleRepository.toSchemaWireFormat()

            override fun getUserStyle() = userStyleRepository.userStyle.toWireFormat()

            override fun setUserStyle(userStyle: UserStyleWireFormat) {
                userStyleRepository.userStyle =
                    UserStyle(userStyle, userStyleRepository.userStyleCategories)
            }

            override fun getBackgroundComplicationId() =
                complicationsManager.getBackgroundComplication()?.id

            override fun getComplicationsMap() = complicationsManager.complications

            override fun getCalendar() = calendar

            override fun getComplicationIdAt(tapX: Int, tapY: Int) =
                complicationsManager.getComplicationAt(tapX, tapY)?.id

            override fun brieflyHighlightComplicationId(complicationId: Int) {
                complicationsManager.brieflyHighlightComplication(complicationId)
            }

            override fun takeScreenshot(
                drawRect: Rect,
                calendar: Calendar,
                drawMode: Int
            ): Bitmap {
                val oldDrawMode = renderer.drawMode
                renderer.drawMode = drawMode
                val bitmap = renderer.takeScreenshot(
                    calendar,
                    DrawMode.INTERACTIVE
                )
                renderer.drawMode = oldDrawMode
                return bitmap
            }
        })

        watchFaceHostApi.registerWatchFaceType(watchFaceType)
        watchFaceHostApi.registerUserStyleSchema(userStyleRepository.toSchemaWireFormat())
        watchState.isAmbient.observe(ambientObserver)
        watchState.interruptionFilter.observe(interruptionFilterObserver)
        watchState.isVisible.observe(visibilityObserver)
        userStyleRepository.addUserStyleListener(styleListener)
        sendCurrentUserStyle(userStyleRepository.userStyle)

        initFinished = true
    }

    /**
     * Called by the system in response to remote configuration, on the main thread.
     */
    internal fun onSetStyleInternal(style: UserStyle) {
        // No need to echo the userStyle back.
        inOnSetStyle = true
        userStyleRepository.userStyle = style
        inOnSetStyle = false
    }

    internal fun onDestroy() {
        pendingSingleTap.cancel()
        pendingUpdateTime.cancel()
        pendingPostDoubleTap.cancel()
        renderer.onDestroy()
        watchState.isAmbient.removeObserver(ambientObserver)
        watchState.interruptionFilter.removeObserver(interruptionFilterObserver)
        watchState.isVisible.removeObserver(visibilityObserver)
        userStyleRepository.removeUserStyleListener(styleListener)
        WatchFaceConfigActivity.unregisterWatchFace(componentName)
    }

    private fun registerReceivers() {
        if (registeredReceivers) {
            return
        }
        registeredReceivers = true
        watchFaceHostApi.getContext().registerReceiver(
            timeZoneReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        )
        watchFaceHostApi.getContext().registerReceiver(
            timeReceiver,
            IntentFilter(Intent.ACTION_TIME_CHANGED)
        )
        watchFaceHostApi.getContext().registerReceiver(
            batteryLevelReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        watchFaceHostApi.getContext().registerReceiver(
            mockTimeReceiver,
            IntentFilter(MOCK_TIME_INTENT)
        )
    }

    private fun unregisterReceivers() {
        if (!registeredReceivers) {
            return
        }
        registeredReceivers = false
        watchFaceHostApi.getContext().unregisterReceiver(timeZoneReceiver)
        watchFaceHostApi.getContext().unregisterReceiver(timeReceiver)
        watchFaceHostApi.getContext().unregisterReceiver(batteryLevelReceiver)
        watchFaceHostApi.getContext().unregisterReceiver(mockTimeReceiver)
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
                invalidate()
            }
        }
    }

    /**
     * Convenience for [SurfaceHolder.Callback.surfaceChanged]. Called when the
     * [SurfaceHolder] containing the display surface changes.
     *
     * @param holder The new [SurfaceHolder] containing the display surface
     * @param format The new [android.graphics.PixelFormat] of the surface
     * @param width The width of the new display surface
     * @param height The height of the new display surface
     */
    internal fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.onSurfaceChanged(holder, format, width, height)
        invalidate()
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
        renderer.drawMode = newDrawMode
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
            pendingUpdateTime.postDelayedUnique(delayMillis) { invalidate() }
        }
    }

    internal fun onSurfaceRedrawNeeded() {
        setCalendarTime(systemTimeProvider.getSystemTimeMillis())
        maybeUpdateDrawMode()
        renderer.renderInternal(calendar)
    }

    /** @hide */
    @UiThread
    internal fun computeDelayTillNextFrame(beginFrameTimeMillis: Long, currentTimeMillis: Long):
            Long {
        // Limit update rate to conserve power when the battery is low and not charging.
        val updateRateMillis =
            if (watchState.isBatteryLowAndNotCharging.getValueOr(false)) {
                max(interactiveUpdateRateMillis, MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS)
            } else {
                interactiveUpdateRateMillis
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
                updateRateMillis + ((nextFrameTimeMillis - currentTimeMillis) % updateRateMillis)
            nextFrameTimeMillis = currentTimeMillis + phaseAdjust
        }
        return nextFrameTimeMillis - currentTimeMillis
    }

    /**
     * Called when new complication data is received.
     *
     * @param watchFaceComplicationId The id of the complication that the data relates to. This will
     *     be an id that was previously sent in a call to [setActiveComplications].
     * @param data The [ComplicationData] that should be displayed in the complication.
     */
    @UiThread
    internal fun onComplicationDataUpdate(watchFaceComplicationId: Int, data: ComplicationData) {
        complicationsManager.onComplicationDataUpdate(watchFaceComplicationId, data)
        invalidate()
    }

    /**
     * Called when a tap or touch related event occurs. Detects double and single taps on
     * complications and triggers the associated action.
     *
     * @param originalTapType Value representing the event sent to the wallpaper
     * @param x X coordinate of the event
     * @param y Y coordinate of the event
     */
    @UiThread
    internal fun onTapCommand(
        @TapType originalTapType: Int,
        x: Int,
        y: Int
    ) {
        // Unfortunately we don't get MotionEvents so we can't directly use the GestureDetector
        // to distinguish between single and double taps. Currently we do that ourselves.
        // TODO(alexclarke): Revisit this

        var tapType = originalTapType
        when (tapType) {
            TapType.TOUCH -> {
                lastTappedPosition = Point(x, y)
            }
            TapType.TOUCH_CANCEL -> {
                lastTappedPosition?.let { safeLastTappedPosition ->
                    if ((safeLastTappedPosition.x == x) && (safeLastTappedPosition.y == y)) {
                        tapType = TapType.TAP
                    }
                }
                lastTappedPosition = null
            }
        }
        val tappedComplication = complicationsManager.getComplicationAt(x, y)
        if (tappedComplication == null) {
            clearGesture()
            return
        }

        when (tapType) {
            TapType.TAP -> {
                if (tappedComplication.id != lastTappedComplicationId &&
                    lastTappedComplicationId != null
                ) {
                    clearGesture()
                    return
                }
                if (pendingPostDoubleTap.isPending()) {
                    return
                }
                if (pendingSingleTap.isPending()) {
                    // The user tapped twice rapidly on the same complication so treat this as
                    // a double tap.
                    complicationsManager.onComplicationDoubleTapped(tappedComplication.id)
                    clearGesture()

                    // Block subsequent taps for a short time, to prevent accidental triple taps.
                    pendingPostDoubleTap.postDelayedUnique(
                        ViewConfiguration.getDoubleTapTimeout().toLong()
                    ) {
                        // NOP.
                    }
                } else {
                    // Give the user immediate visual feedback, the UI feels sluggish if we defer
                    // this.
                    complicationsManager.brieflyHighlightComplication(tappedComplication.id)

                    lastTappedComplicationId = tappedComplication.id

                    // This could either be a single or a double tap, post a task to process the
                    // single tap which will get canceled if a double tap gets there first
                    pendingSingleTap.postDelayedUnique(
                        ViewConfiguration.getDoubleTapTimeout().toLong()
                    ) {
                        complicationsManager.onComplicationSingleTapped(tappedComplication.id)
                        invalidate()
                        clearGesture()
                    }
                }
            }
            TapType.TOUCH -> {
                // Make sure the user isn't doing a swipe.
                if (tappedComplication.id != lastTappedComplicationId &&
                    lastTappedComplicationId != null
                ) {
                    clearGesture()
                }
                lastTappedComplicationId = tappedComplication.id
            }
            else -> clearGesture()
        }
    }

    private fun clearGesture() {
        lastTappedComplicationId = null
        pendingSingleTap.cancel()
    }

    /** Schedules a call to [onDraw] to draw the next frame. */
    @UiThread
    fun invalidate() {
        watchFaceHostApi.invalidate()
    }

    /**
     * Posts a message to schedule a call to [onDraw] to draw the next frame. Unlike
     * [invalidate], this method is thread-safe and may be called on any thread.
     */
    fun postInvalidate() {
        watchFaceHostApi.getHandler().post { watchFaceHostApi.invalidate() }
    }
}
