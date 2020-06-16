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
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Bundle
import android.support.wearable.complications.ComplicationData
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.VisibleForTesting
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.ui.WatchFaceConfigActivity
import androidx.wear.watchface.ui.WatchFaceConfigDelegate
import androidx.wear.watchfacestyle.UserStyleCategory
import androidx.wear.watchfacestyle.UserStyleManager
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

private fun readPrefs(context: Context, fileName: String): Map<String, String> {
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
    return hashMap
}

private fun writePrefs(
    context: Context,
    fileName: String,
    style: Map<UserStyleCategory, UserStyleCategory.Option>
) {
    val writer = context.openFileOutput(fileName, Context.MODE_PRIVATE).bufferedWriter()
    for ((key, value) in style) {
        writer.write(key.id)
        writer.newLine()
        writer.write(value.id)
        writer.newLine()
    }
    writer.close()
}

/** A WatchFace is constructed by a user's {@link WatchFaceService). */
@SuppressLint("SyntheticAccessor")
open class WatchFace(
    /** The type of watch face, whether it's digital or analog. */
    @WatchFaceType private val watchFaceType: Int,

    /**
     * The interval in milliseconds between frames in interactive mode. To render at 60hz pass in
     * 16. Note when battery is low, the framerate will be clamped to 10fps. Watch faces are
     * recommended to use lower frame rates if possible for better battery life.
     */
    private var interactiveUpdateRateMillis: Long,

    /** The {@UserStyleManager} for this WatchFace. */
    internal val userStyleManager: UserStyleManager,

    /** The {@link ComplicationSlots} for this WatchFace. */
    internal var complicationSlots: ComplicationSlots,

    /** The {@link Renderer} for this WatchFace. */
    internal val renderer: Renderer,

    /** The {@link SystemApi} for this WatchFace. */
    private val systemApi: SystemApi,

    /** The {@link SystemStateListener} for this WatchFace. */
    private val systemState: SystemState
) {
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
        CancellableUniqueTask(systemApi.getHandler())
    private val pendingUpdateTime: CancellableUniqueTask =
        CancellableUniqueTask(systemApi.getHandler())
    private val pendingPostDoubleTap: CancellableUniqueTask =
        CancellableUniqueTask(systemApi.getHandler())
    private val componentName: ComponentName by lazy {
        ComponentName(
            systemApi.getContext().packageName,
            systemApi.getContext().javaClass.typeName
        )
    }
    private val timeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            calendar.timeZone = TimeZone.getDefault()
            invalidate()
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val batteryLevelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_LOW -> systemState.onIsBatteryLowAndNotCharging(true)
                Intent.ACTION_BATTERY_OKAY -> systemState.onIsBatteryLowAndNotCharging(
                    false
                )
                Intent.ACTION_POWER_CONNECTED -> systemState.onIsBatteryLowAndNotCharging(
                    false
                )
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
                mockTime.minTime = getSystemTimeMillis()
            }
            mockTime.maxTime =
                intent.getLongExtra(EXTRA_MOCK_TIME_WRAPPING_MAX_TIME, Long.MAX_VALUE)
        }
    }

    init {
        // If the system has a stored user style then Home/SysUI is in charge of style
        // persistence, otherwise we need to do our own.
        val storedUserStyle = systemApi.getStoredUserStyle(userStyleManager.userStyleCategories)
        if (storedUserStyle != null) {
            userStyleManager.userStyle = storedUserStyle
        } else {
            // The system doesn't support preference persistence we need to do it ourselves.
            val preferencesFile = "watchface_prefs_${systemApi.getContext().javaClass.typeName}.txt"

            userStyleManager.userStyle = UserStyleCategory.idMapToStyleMap(
                readPrefs(systemApi.getContext(), preferencesFile),
                userStyleManager.userStyleCategories
            )

            userStyleManager.addUserStyleListener(object : UserStyleManager.UserStyleListener {
                @SuppressLint("SyntheticAccessor")
                override fun onUserStyleChanged(
                    userStyle: Map<UserStyleCategory, UserStyleCategory.Option>
                ) {
                    writePrefs(systemApi.getContext(), preferencesFile, userStyle)
                }
            })
        }
    }

    private var inOnSetStyle = false

    private inner class WfUserStyleListener : UserStyleManager.UserStyleListener {
        @SuppressWarnings("SyntheticAccessor")
        override fun onUserStyleChanged(
            userStyle: Map<UserStyleCategory, UserStyleCategory.Option>
        ) {
            // No need to echo the userStyle back.
            if (!inOnSetStyle) {
                sendCurrentUserStyle(userStyle)
            }
        }
    }

    private val styleListener = WfUserStyleListener()

    private fun sendCurrentUserStyle(userStyle: Map<UserStyleCategory, UserStyleCategory.Option>) {
        // Sync the user style with the system.
        systemApi.setCurrentUserStyle(userStyle)
    }

    private inner class SystemStateListener : SystemState.Listener {
        @SuppressWarnings("SyntheticAccessor")
        override fun onAmbientModeChanged(isAmbient: Boolean) {
            scheduleDraw()
            invalidate()
        }

        @SuppressWarnings("SyntheticAccessor")
        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            val inMuteMode = interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                invalidate()
            }
        }

        @SuppressWarnings("SyntheticAccessor")
        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                registerReceivers()
                // Update time zone in case it changed while we weren't visible.
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceivers()
            }

            scheduleDraw()
        }
    }

    private val systemStateListener = SystemStateListener()

    init {

        // We need to inhibit an immediate callback during initialization because members are not
        // fully constructed and it will fail. It's also superfluous because we're going to render
        // anyway.
        var initFinished = false
        complicationSlots.init(systemApi, calendar, renderer,
            object : ComplicationRenderer.InvalidateCallback {
                @SuppressWarnings("SyntheticAccessor")
                override fun invalidate() {
                    if (initFinished) {
                        this@WatchFace.invalidate()
                    }
                    // Ensure we render a frame if the Complication needs rendering, e.g. because it
                    // loaded an image. However if we're animating there's no need to trigger an extra
                    // invalidation.
                    if (shouldAnimate() &&
                        computeDelayTillNextFrame(nextDrawTimeMillis, getSystemTimeMillis()) <
                        MIN_PERCEPTABLE_DELAY_MILLIS
                    ) {
                        return
                    }
                }
            }
        )

        WatchFaceConfigActivity.registerWatchFace(componentName, object : WatchFaceConfigDelegate {
            override fun getUserStyleSchema() =
                UserStyleCategory.userStyleCategoryListToBundles(
                    userStyleManager.userStyleCategories
                )

            override fun getUserStyle() =
                UserStyleCategory.styleMapToBundle(userStyleManager.userStyle)

            override fun setUserStyle(style: Bundle) {
                userStyleManager.userStyle =
                    UserStyleCategory.bundleToStyleMap(style, userStyleManager.userStyleCategories)
            }

            override fun getBackgroundComplicationId() =
                complicationSlots.getBackgroundComplication()?.id

            override fun getComplicationsMap() = complicationSlots.complications

            override fun getCalendar() = calendar

            override fun getComplicationIdAt(tapX: Int, tapY: Int, calendar: Calendar) =
                complicationSlots.getComplicationAt(tapX, tapY, calendar)?.id

            override fun brieflyHighlightComplicationId(complicationId: Int) {
                complicationSlots.brieflyHighlightComplication(complicationId)
            }

            override fun drawComplicationSelect(
                canvas: Canvas,
                drawRect: Rect,
                calendar: Calendar
            ) {
                renderer.drawComplicationSelect(canvas, drawRect, calendar)
            }
        })

        systemApi.registerWatchFaceType(watchFaceType)
        systemApi.registerUserStyleSchema(userStyleManager.userStyleCategories)

        systemState.addListener(systemStateListener)
        userStyleManager.addUserStyleListener(styleListener)
        sendCurrentUserStyle(userStyleManager.userStyle)

        initFinished = true
    }

    /**
     * Called by the system in response to remote configuration, on the main thread.
     */
    internal fun onSetStyleInternal(style: Map<UserStyleCategory, UserStyleCategory.Option>) {
        // No need to echo the userStyle back.
        inOnSetStyle = true
        userStyleManager.userStyle = style
        inOnSetStyle = false
    }

    @CallSuper
    open fun onDestroy() {
        pendingSingleTap.cancel()
        pendingUpdateTime.cancel()
        pendingPostDoubleTap.cancel()
        renderer.onDestroy()
        systemState.removeListener(systemStateListener)
        userStyleManager.removeUserStyleListener(styleListener)
        WatchFaceConfigActivity.unregisterWatchFace(componentName)
    }

    /**
     * Called during initialization, allows customization of the {@link WatchFaceStyle}.
     *
     * @param watchFaceStyleBuilder The {@link WatchFaceStyle.Builder} to modify
     */
    open fun onCreateWatchFaceStyle(watchFaceStyleBuilder: WatchFaceStyle.Builder) {}

    private fun registerReceivers() {
        if (registeredReceivers) {
            return
        }
        registeredReceivers = true
        systemApi.getContext().registerReceiver(
            timeZoneReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        )
        systemApi.getContext().registerReceiver(
            batteryLevelReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        systemApi.getContext().registerReceiver(
            mockTimeReceiver,
            IntentFilter(MOCK_TIME_INTENT)
        )
    }

    private fun unregisterReceivers() {
        if (!registeredReceivers) {
            return
        }
        registeredReceivers = false
        systemApi.getContext().unregisterReceiver(timeZoneReceiver)
        systemApi.getContext().unregisterReceiver(batteryLevelReceiver)
        systemApi.getContext().unregisterReceiver(mockTimeReceiver)
    }

    private fun scheduleDraw() {
        setCalendarTime(getSystemTimeMillis())
        if (shouldAnimate()) {
            pendingUpdateTime.postUnique {
                invalidate()
            }
        }
    }

    /**
     * Convenience for {@link SurfaceHolder.Callback#surfaceChanged}. Called when the
     * {@link SurfaceHolder} containing the display surface changes.
     *
     * @param holder The new {@link SurfaceHolder} containing the display surface
     * @param format The new {@link android.graphics.PixelFormat} of the surface
     * @param width The width of the new display surface
     * @param height The height of the new display surface
     */
    internal fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.onSurfaceChanged(holder, format, width, height)
        invalidate()
    }

    /**
     * The system periodically (at least once per minute) calls onTimeTick() to trigger a display
     * update. If the watch face needs to animate with an interactive frame rate, calls to
     * invalidate must be scheduled. This method controls whether or not we should do that.
     *
     * By default we remain at an interactive frame rate when the watch face is visible and we're
     * not in ambient mode. Watchfaces with animated transitions for entering ambient mode may
     * need to override this to ensure they play smoothly.
     *
     * @return Whether we should schedule an onDraw call to maintain an interactive frame rate
     */
    open fun shouldAnimate() = systemState.isVisible && !systemState.isAmbient

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

    /**
     * The {@link DrawMode} is recomputed before every onDraw call. This method allows the subclass
     * to override the {@link DrawMode}. e.g. to support enter/exit ambient animations which may
     * wish to defer rendering changes.
     *
     * @param drawMode The proposed {@link DrawMode} to use for rendering based on default logic
     * @return The {@link DrawMode} to use for rendering
     */
    open fun maybeOverrideDrawMode(@DrawMode drawMode: Int) = drawMode

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun maybeUpdateDrawMode() {
        var newDrawMode = if (systemState.isBatteryLowAndNotCharging) {
            DrawMode.LOW_BATTERY_INTERACTIVE
        } else {
            DrawMode.INTERACTIVE
        }
        if (systemState.isAmbient) {
            newDrawMode = DrawMode.AMBIENT
        } else if (muteMode) {
            newDrawMode = DrawMode.MUTE
        }
        renderer.drawMode = maybeOverrideDrawMode(newDrawMode)
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    fun onDraw() {
        setCalendarTime(getSystemTimeMillis())
        maybeUpdateDrawMode()
        renderer.onDrawInternal(calendar)

        val currentTimeMillis = getSystemTimeMillis()
        setCalendarTime(currentTimeMillis)
        if (shouldAnimate()) {
            val delayMillis = computeDelayTillNextFrame(nextDrawTimeMillis, currentTimeMillis)
            nextDrawTimeMillis = currentTimeMillis + delayMillis
            pendingUpdateTime.postDelayedUnique(delayMillis) { invalidate() }
        }
    }

    internal fun onSurfaceRedrawNeeded() {
        setCalendarTime(getSystemTimeMillis())
        maybeUpdateDrawMode()
        renderer.onDrawInternal(calendar)
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun computeDelayTillNextFrame(beginFrameTimeMillis: Long, currentTimeMillis: Long): Long {
        // Limit update rate to conserve power when the battery is low and not charging.
        val updateRateMillis =
            if (systemState.isBatteryLowAndNotCharging) {
                max(interactiveUpdateRateMillis, MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS)
            } else {
                interactiveUpdateRateMillis
            }
        var nextFrameTimeMillis = beginFrameTimeMillis + updateRateMillis
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

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    open fun getSystemTimeMillis() = System.currentTimeMillis()

    /**
     * Called when new complication data is received.
     *
     * @param watchFaceComplicationId The id of the complication that the data relates to. This will
     *     be an id that was previously sent in a call to {@link #setActiveComplications}.
     * @param data The {@link ComplicationData} that should be displayed in the complication.
     */
    internal fun onComplicationDataUpdate(watchFaceComplicationId: Int, data: ComplicationData) {
        complicationSlots.onComplicationDataUpdate(watchFaceComplicationId, data)
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
    @CallSuper
    fun onTapCommand(
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
        val tappedComplication = complicationSlots.getComplicationAt(x, y, calendar)
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
                    complicationSlots.onComplicationDoubleTapped(tappedComplication.id)
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
                    complicationSlots.brieflyHighlightComplication(tappedComplication.id)

                    lastTappedComplicationId = tappedComplication.id

                    // This could either be a single or a double tap, post a task to process the
                    // single tap which will get canceled if a double tap gets there first
                    pendingSingleTap.postDelayedUnique(
                        ViewConfiguration.getDoubleTapTimeout().toLong()
                    ) {
                        complicationSlots.onComplicationSingleTapped(tappedComplication.id)
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

    /**
     * Schedules a call to {@link #onDraw} to draw the next frame. Must be called on the main
     * thread.
     */
    open fun invalidate() {
        systemApi.invalidate()
    }

    /**
     * Posts a message to schedule a call to {@link #onDraw} to draw the next frame. Unlike {@link
     * #invalidate}, this method is thread-safe and may be called on any thread.
     */
    fun postInvalidate() {
        systemApi.getHandler().post { systemApi.invalidate() }
    }
}
