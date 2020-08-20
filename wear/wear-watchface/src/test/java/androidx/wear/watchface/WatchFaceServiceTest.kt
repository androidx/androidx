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

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.support.wearable.complications.ComplicationData
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.UserStyleManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.validateMockitoUsage
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import java.util.ArrayDeque
import java.util.PriorityQueue

private const val INTERACTIVE_UPDATE_RATE_MS = 16L
private const val LEFT_COMPLICATION_ID = 1000
private const val RIGHT_COMPLICATION_ID = 1001
private const val BACKGROUND_COMPLICATION_ID = 1111

@Config(manifest = Config.NONE)
@RunWith(WatchFaceTestRunner::class)
class WatchFaceServiceTest {

    private val handler = mock(Handler::class.java)
    private val iWatchFaceService = mock(IWatchFaceService::class.java)
    private val surfaceHolder = mock(SurfaceHolder::class.java)
    private val systemState = WatchState()

    init {
        `when`(surfaceHolder.surfaceFrame).thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)
    }

    companion object {
        val ONE_HUNDRED_BY_ONE_HUNDRED_RECT = Rect(0, 0, 100, 100)
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val complicationDrawableLeft = ComplicationDrawable(context)
    private val complicationDrawableRight = ComplicationDrawable(context)

    private val redStyleOption =
        ListUserStyleCategory.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleCategory.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleCategory.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleCategory = ListUserStyleCategory(
        "color_style_category",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList
    )

    private val classicStyleOption =
        ListUserStyleCategory.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleCategory.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleCategory.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleCategory = ListUserStyleCategory(
        "hand_style_category",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList
    )

    private val badStyleOption =
        ListUserStyleCategory.ListOption("bad_option", "Bad", icon = null)

    private val leftComplication =
        Complication(
            LEFT_COMPLICATION_ID,
            UnitSquareBoundsProvider(RectF(0.2f, 0.4f, 0.4f, 0.6f)),
            ComplicationDrawableRenderer(complicationDrawableLeft, systemState),
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            Complication.DefaultComplicationProvider(SystemProviders.SUNRISE_SUNSET),
            ComplicationData.TYPE_SHORT_TEXT
        ).apply { data = createComplicationData() }

    private val rightComplication =
        Complication(
            RIGHT_COMPLICATION_ID,
            UnitSquareBoundsProvider(RectF(0.6f, 0.4f, 0.8f, 0.6f)),
            ComplicationDrawableRenderer(complicationDrawableRight, systemState),
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            Complication.DefaultComplicationProvider(SystemProviders.DAY_OF_WEEK),
            ComplicationData.TYPE_SHORT_TEXT
        ).apply { data = createComplicationData() }

    private val backgroundComplication =
        Complication(
            BACKGROUND_COMPLICATION_ID,
            BackgroundComplicationBoundsProvider(),
            ComplicationDrawableRenderer(complicationDrawableRight, systemState),
            intArrayOf(
                ComplicationData.TYPE_LARGE_IMAGE
            ),
            Complication.DefaultComplicationProvider(),
            ComplicationData.TYPE_LARGE_IMAGE
        ).apply { data = createComplicationData() }

    private lateinit var renderer: TestRenderer
    private lateinit var complicationSet: ComplicationSet
    private lateinit var userStyleManager: UserStyleManager
    private lateinit var watchFace: TestWatchFace
    private lateinit var engineWrapper: WatchFaceService.EngineWrapper

    private class Task(val runTimeMillis: Long, val runnable: Runnable) : Comparable<Task> {
        override fun compareTo(other: Task) = runTimeMillis.compareTo(other.runTimeMillis)
    }

    private var looperTimeMillis = 0L
    private val pendingTasks = PriorityQueue<Task>()

    private fun runPostedTasksFor(durationMillis: Long) {
        looperTimeMillis += durationMillis
        while (pendingTasks.isNotEmpty() &&
            pendingTasks.peek()!!.runTimeMillis <= looperTimeMillis
        ) {
            pendingTasks.remove().runnable.run()
        }
    }

    private fun initEngine(
        @WatchFaceType watchFaceType: Int,
        complications: List<Complication>,
        userStyleCategories: List<UserStyleCategory>,
        apiVersion: Int = 2
    ) {
        this.complicationSet = ComplicationSet(complications)
        userStyleManager =
            UserStyleManager(userStyleCategories)
        renderer = TestRenderer(surfaceHolder, userStyleManager, systemState)
        val service = TestWatchFaceService(
            watchFaceType,
            this.complicationSet,
            renderer,
            userStyleManager,
            systemState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )
        engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)
        // Trigger watch face creation.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion)
        watchFace = service.watchFace
    }

    private fun sendBinder(engine: WatchFaceService.EngineWrapper, apiVersion: Int) {
        `when`(iWatchFaceService.apiVersion).thenReturn(apiVersion)
        engine.onCommand(
            Constants.COMMAND_SET_BINDER,
            0,
            0,
            0,
            Bundle().apply {
                putBinder(
                    Constants.EXTRA_BINDER,
                    WatchFaceServiceStub(iWatchFaceService).asBinder()
                )
            },
            false
        )
    }

    private fun sendRequestStyle() {
        engineWrapper.onCommand(Constants.COMMAND_REQUEST_STYLE, 0, 0, 0, null, false)
    }

    @Before
    fun setUp() {
        // Capture tasks posted to mHandler and insert in mPendingTasks which is under our control.
        doAnswer {
            pendingTasks.add(Task(looperTimeMillis, it.arguments[0] as Runnable))
        }.`when`(handler).post(any())

        doAnswer {
            pendingTasks.add(
                Task(looperTimeMillis + it.arguments[1] as Long, it.arguments[0] as Runnable)
            )
        }.`when`(handler).postDelayed(any(), anyLong())

        doAnswer {
            // Remove task from the priority queue.  There's no good way of doing this quickly.
            val queue = ArrayDeque<Task>()
            while (pendingTasks.isNotEmpty()) {
                val task = pendingTasks.remove()
                if (task.runnable != it.arguments[0]) {
                    queue.add(task)
                }
            }

            // Push filtered tasks back on the queue.
            while (queue.isNotEmpty()) {
                pendingTasks.add(queue.remove())
            }
        }.`when`(handler).removeCallbacks(any())
    }

    @After
    fun validate() {
        validateMockitoUsage()
    }

    @Test
    fun paintModesCreate_producesCorrectPaintObjects() {
        val paintModes = PaintModes.create(Paint().apply { style = Paint.Style.FILL }) {
            color = if (it == DrawMode.INTERACTIVE) {
                Color.GRAY
            } else {
                Color.DKGRAY
            }
        }

        for (drawMode in DrawMode.values()) {
            assertThat(paintModes[drawMode].style).isEqualTo(Paint.Style.FILL)
            assertThat(paintModes[drawMode].color).isEqualTo(
                if (drawMode == DrawMode.INTERACTIVE) Color.GRAY else Color.DKGRAY
            )
            assertThat(paintModes[drawMode].isAntiAlias).isEqualTo(drawMode != DrawMode.AMBIENT)
        }
    }

    @Test
    fun maybeUpdateDrawMode_setsCorrectDrawMode() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(renderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        systemState.onIsBatteryLowAndNotCharging(true)
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.LOW_BATTERY_INTERACTIVE)

        systemState.onIsBatteryLowAndNotCharging(false)
        systemState.onAmbientModeChanged(true)
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.AMBIENT)

        systemState.onAmbientModeChanged(false)
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        systemState.onInterruptionFilterChanged(
            NotificationManager.INTERRUPTION_FILTER_NONE
        )
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.MUTE)

        // Ambient takes precidence over interruption filter.
        systemState.onAmbientModeChanged(true)
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.AMBIENT)

        systemState.onAmbientModeChanged(false)
        systemState.onInterruptionFilterChanged(0)
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        // WatchFaceService.DrawMode.COMPLICATION_SELECT is tested below.
    }

    @Test
    fun drawComplicationSelect_setsCorrectDrawMode() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        renderer.drawComplicationSelect(
            Canvas(),
            ONE_HUNDRED_BY_ONE_HUNDRED_RECT,
            Calendar.getInstance().apply {
                timeInMillis = 1000L
            }
        )
        assertThat(renderer.drawMode).isEqualTo(
            DrawMode.COMPLICATION_SELECT
        )
    }

    @Test
    fun onDraw_calendar_setFromSystemTime() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        watchFace.mockSystemTimeMillis = 1000L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)
    }

    @Test
    fun onDraw_calendar_affectedCorrectly_with2xMockTime() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        watchFace.mockSystemTimeMillis = 1000L

        watchFace.mockTimeReceiver.onReceive(
            context,
            Intent(WatchFace.MOCK_TIME_INTENT).apply {
                putExtra(WatchFace.EXTRA_MOCK_TIME_SPEED_MULTIPLIER, 2.0f)
                putExtra(WatchFace.EXTRA_MOCK_TIME_WRAPPING_MIN_TIME, -1L)
            }
        )

        // Time should not diverge initially.
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)

        // However 1000ms of real time should result in 2000ms observed by onDraw.
        watchFace.mockSystemTimeMillis = 2000L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(3000L)
    }

    @Test
    fun onDraw_calendar_affectedCorrectly_withMockTimeWrapping() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        watchFace.mockSystemTimeMillis = 1000L

        watchFace.mockTimeReceiver.onReceive(
            context,
            Intent(WatchFace.MOCK_TIME_INTENT).apply {
                putExtra(WatchFace.EXTRA_MOCK_TIME_SPEED_MULTIPLIER, 2.0f)
                putExtra(WatchFace.EXTRA_MOCK_TIME_WRAPPING_MIN_TIME, 1000L)
                putExtra(WatchFace.EXTRA_MOCK_TIME_WRAPPING_MAX_TIME, 2000L)
            }
        )

        // Time in millis observed by onDraw should wrap betwween 1000 and 2000.
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)

        watchFace.mockSystemTimeMillis = 1250L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1500L)

        watchFace.mockSystemTimeMillis = 1499L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1998L)

        watchFace.mockSystemTimeMillis = 1500L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)

        watchFace.mockSystemTimeMillis = 1750L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1500L)

        watchFace.mockSystemTimeMillis = 1999L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1998L)

        watchFace.mockSystemTimeMillis = 2000L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)
    }

    private fun tapAt(x: Int, y: Int) {
        // The eventTime is ignored.
        watchFace.onTapCommand(TapType.TOUCH, x, y)
        watchFace.onTapCommand(TapType.TAP, x, y)
    }

    private fun doubleTapAt(x: Int, y: Int, delayMillis: Long) {
        tapAt(x, y)
        runPostedTasksFor(delayMillis)
        tapAt(x, y)
    }

    private fun tripleTapAt(x: Int, y: Int, delayMillis: Long) {
        tapAt(x, y)
        runPostedTasksFor(delayMillis)
        tapAt(x, y)
        runPostedTasksFor(delayMillis)
        tapAt(x, y)
    }

    private fun tapCancelAt(x: Int, y: Int) {
        watchFace.onTapCommand(TapType.TOUCH, x, y)
        watchFace.onTapCommand(TapType.TOUCH_CANCEL, x, y)
    }

    @Test
    fun singleTaps_correctlyDetected_and_highlightComplications() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Tap left complication.
        tapAt(30, 50)
        assertThat(complicationDrawableLeft.highlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()

        // Tap right complication.
        watchFace.reset()
        tapAt(70, 50)
        assertThat(complicationDrawableRight.highlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()

        // Tap on blank space.
        watchFace.reset()
        tapAt(1, 1)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isNull()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()
    }

    @Test
    fun doubleTaps_correctlyDetected_and_highlightComplications() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Tap left complication.
        doubleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(watchFace.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDrawableLeft.highlighted).isTrue()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()

        // Tap right complication.
        watchFace.reset()
        doubleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(watchFace.complicationDoubleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDrawableRight.highlighted).isTrue()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Tap on blank space.
        watchFace.reset()
        doubleTapAt(1, 1, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(watchFace.complicationDoubleTapped).isNull()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()
    }

    @Test
    fun fastTap_onDifferentComplications_ignored() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Rapidly tap left then right complication.
        tapAt(30, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        tapAt(70, 50)

        // Both complications get temporarily highlighted but neither onComplicationSingleTapped
        // nor onComplicationDoubleTapped fire.
        assertThat(complicationDrawableLeft.highlighted).isTrue()
        assertThat(complicationDrawableRight.highlighted).isTrue()
        assertThat(watchFace.complicationSingleTapped).isNull()
        assertThat(watchFace.complicationDoubleTapped).isNull()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()
    }

    @Test
    fun slow_doubleTap_recogisedAsSingleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Slowly tap left complication twice.
        doubleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() * 2)

        assertThat(watchFace.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(watchFace.complicationDoubleTapped).isNull()
    }

    @Test
    fun tripleTap_recogisedAsDoubleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Quickly tap left complication thrice.
        tripleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)

        assertThat(watchFace.complicationSingleTapped).isNull()
        assertThat(watchFace.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
    }

    @Test
    fun tapCancel_after_tapDown_at_same_location_HandledAsSingleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        watchFace.reset()
        // Tap/Cancel left complication
        tapCancelAt(30, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
    }

    @Test
    fun tapDown_then_tapDown_tapCancel_HandledAsSingleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        watchFace.reset()
        // Tap down left Complication
        watchFace.onTapCommand(TapType.TOUCH, 30, 50)

        // Tap down at right complication
        watchFace.onTapCommand(TapType.TOUCH, 70, 50)

        // Now Tap cancel at the second position
        watchFace.onTapCommand(TapType.TOUCH_CANCEL, 70, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
    }

    @Test
    fun tapDown_tapCancel_different_positions_CancelsTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        watchFace.reset()
        // Tap down at a position in left Complication
        watchFace.onTapCommand(TapType.TOUCH, 30, 50)
        // Tap cancel at different position stillin left Complication
        watchFace.onTapCommand(TapType.TOUCH_CANCEL, 32, 50)

        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isNull()
        assertThat(watchFace.complicationDoubleTapped).isNull()
    }

    @Test
    fun singleTap_recognisedAfterTripleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        // Quickly tap right complication thrice.
        tripleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)

        // Wait a bit for the condition to reset and clear our detection state.
        watchFace.clearTappedState()
        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Tap right complication.
        tapAt(70, 50)
        assertThat(complicationDrawableRight.highlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(watchFace.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(watchFace.complicationDoubleTapped).isNull()
    }

    @Test
    fun interactiveFrameRate_reducedWhenBatteryLow() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(watchFace.computeDelayTillNextFrame(0, 0)).isEqualTo(INTERACTIVE_UPDATE_RATE_MS)

        // The delay should change when battery is low.
        watchFace.batteryLevelReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_LOW)
        )
        assertThat(watchFace.computeDelayTillNextFrame(0, 0)).isEqualTo(
            WatchFace.MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS
        )

        // And go back to normal when battery is OK.
        watchFace.batteryLevelReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_OKAY)
        )
        assertThat(watchFace.computeDelayTillNextFrame(0, 0)).isEqualTo(INTERACTIVE_UPDATE_RATE_MS)
    }

    @Test
    fun computeDelayTillNextFrame_accountsForSlowDraw() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(
            watchFace.computeDelayTillNextFrame(
                beginFrameTimeMillis = 0,
                currentTimeMillis = 2
            )
        )
            .isEqualTo(INTERACTIVE_UPDATE_RATE_MS - 2)
    }

    @Test
    fun computeDelayTillNextFrame_dropsFramesForVerySlowDraw() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(
            watchFace.computeDelayTillNextFrame(
                beginFrameTimeMillis = 0,
                currentTimeMillis = INTERACTIVE_UPDATE_RATE_MS
            )
        )
            .isEqualTo(INTERACTIVE_UPDATE_RATE_MS)
    }

    @Test
    fun computeDelayTillNextFrame_perservesPhaseForVerySlowDraw() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        // The phase of beginFrameTimeMillis % INTERACTIVE_UPDATE_RATE_MS is 2, but the phase of
        // currentTimeMillis % INTERACTIVE_UPDATE_RATE_MS is 3, so we expect to delay
        // INTERACTIVE_UPDATE_RATE_MS - 1 to preserve the phase while dropping a frame.
        assertThat(
            watchFace.computeDelayTillNextFrame(
                beginFrameTimeMillis = 2,
                currentTimeMillis = INTERACTIVE_UPDATE_RATE_MS + 3
            )
        )
            .isEqualTo(INTERACTIVE_UPDATE_RATE_MS - 1)
    }

    @Test
    fun getComplicationIdAt_returnsCorrectComplications() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationSet.getComplicationAt(30, 50)!!.id)
            .isEqualTo(LEFT_COMPLICATION_ID)
        leftComplication.enabled = false
        assertThat(complicationSet.getComplicationAt(30, 50)).isNull()

        assertThat(complicationSet.getComplicationAt(70, 50)!!.id)
            .isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationSet.getComplicationAt(1, 1)).isNull()
    }

    @Test
    fun getBackgroundComplicationId_returnsCorrectId() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        assertThat(complicationSet.getBackgroundComplication()).isNull()

        initEngine(WatchFaceType.ANALOG, listOf(leftComplication), emptyList())
        assertThat(complicationSet.getBackgroundComplication()).isNull()

        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, backgroundComplication),
            emptyList()
        )
        assertThat(complicationSet.getBackgroundComplication()!!.id).isEqualTo(
            BACKGROUND_COMPLICATION_ID
        )
    }

    @Test
    fun getStoredUserStyleNotSupported_userStyle_isPersisted() {
        // The style should get persisted in a file because the API is old and
        // {@link WatchFaceHostApi#getStoredUserStyle} returns null.
        `when`(iWatchFaceService.getStoredUserStyle()).thenReturn(null)

        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            2
        )

        // This should get persisted.
        userStyleManager.userStyle = mapOf(
            colorStyleCategory to blueStyleOption,
            watchHandStyleCategory to gothicStyleOption
        )

        val styleManager2 = UserStyleManager(
            listOf(colorStyleCategory, watchHandStyleCategory)
        )

        val testRenderer2 = TestRenderer(surfaceHolder, styleManager2, systemState)
        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationSet(emptyList()),
            testRenderer2,
            styleManager2,
            systemState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )

        // Trigger watch face creation.
        val engine2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
        engine2.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engine2, apiVersion = 2)

        assertThat(styleManager2.userStyle[colorStyleCategory]).isEqualTo(
            blueStyleOption
        )
        assertThat(styleManager2.userStyle[watchHandStyleCategory]).isEqualTo(
            gothicStyleOption
        )
    }

    @Test
    fun getStoredUserStyleSupported_userStyle_isPersisted() {
        var persistedStyle = Bundle()

        // Mock the behavior of Home/SysUI which should persist the style.
        doAnswer {
            persistedStyle
        }.`when`(iWatchFaceService).getStoredUserStyle()

        `when`(iWatchFaceService.setCurrentUserStyle(any())).then {
            persistedStyle = it.arguments[0] as Bundle
            Unit
        }

        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            3
        )

        // This should get persisted.
        userStyleManager.userStyle = mapOf(
            colorStyleCategory to blueStyleOption,
            watchHandStyleCategory to gothicStyleOption
        )

        val styleManager2 = UserStyleManager(
            listOf(colorStyleCategory, watchHandStyleCategory)
        )

        val testRenderer2 = TestRenderer(surfaceHolder, styleManager2, systemState)
        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationSet(emptyList()),
            testRenderer2,
            styleManager2,
            systemState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )

        // Trigger watch face creation.
        val engine2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
        engine2.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engine2, apiVersion = 3)

        assertThat(styleManager2.userStyle[colorStyleCategory]).isEqualTo(
            blueStyleOption
        )
        assertThat(styleManager2.userStyle[watchHandStyleCategory]).isEqualTo(
            gothicStyleOption
        )
    }

    @Test
    fun persistedStyleOptionMismatchIgnored() {
        `when`(iWatchFaceService.getStoredUserStyle()).thenReturn(
            UserStyleCategory.styleMapToBundle(mapOf(watchHandStyleCategory to badStyleOption)))

        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            3
        )

        assertThat(watchFace.lastUserStyle!![watchHandStyleCategory])
            .isEqualTo(watchHandStyleList.first())
    }

    @Test
    fun maybeUpdateStatus_issuesCorrectApiCalls() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList())
        val bundle = Bundle().apply {
            putBoolean(Constants.STATUS_CHARGING, true)
            putBoolean(Constants.STATUS_AIRPLANE_MODE, false)
            putBoolean(Constants.STATUS_CONNECTED, true)
            putBoolean(Constants.STATUS_THEATER_MODE, false)
            putBoolean(Constants.STATUS_GPS_ACTIVE, true)
            putBoolean(Constants.STATUS_KEYGUARD_LOCKED, false)
        }

        val systemStateListener = mock(WatchState.Listener::class.java)
        systemState.addListener(systemStateListener)

        // Every indicator onXyz method should be called upon the initial update.
        engineWrapper.onBackgroundAction(Bundle().apply {
            putBundle(Constants.EXTRA_INDICATOR_STATUS, bundle)
        })

        verify(systemStateListener).onIsChargingChanged(true)
        verify(systemStateListener).onInAirplaneModeChanged(false)
        verify(systemStateListener).onIsConnectedToCompanionChanged(true)
        verify(systemStateListener).onInTheaterModeChanged(false)
        verify(systemStateListener).onIsGpsActiveChanged(true)
        verify(systemStateListener).onIsKeyguardLockedChanged(false)

        reset(systemStateListener)

        // Check only the modified setIsCharging state leads to a call.
        bundle.putBoolean(Constants.STATUS_CHARGING, false)
        engineWrapper.onBackgroundAction(Bundle().apply {
            putBundle(Constants.EXTRA_INDICATOR_STATUS, bundle)
        })
        verify(systemStateListener).onIsChargingChanged(false)
        verify(systemStateListener, times(0)).onInAirplaneModeChanged(anyBoolean())
        verify(systemStateListener, times(0)).onIsConnectedToCompanionChanged(anyBoolean())
        verify(systemStateListener, times(0)).onInTheaterModeChanged(anyBoolean())
        verify(systemStateListener, times(0)).onIsGpsActiveChanged(anyBoolean())
        verify(systemStateListener, times(0)).onIsKeyguardLockedChanged(anyBoolean())
    }

    @Test
    fun onPropertiesChanged_issuesCorrectApiCalls() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList())
        val bundle = Bundle().apply {
            putBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT, true)
            putBoolean(Constants.PROPERTY_BURN_IN_PROTECTION, false)
        }

        val systemStateListener = mock(WatchState.Listener::class.java)
        systemState.addListener(systemStateListener)

        // Check all the right methods are called on initial onPropertiesChanged call.
        engineWrapper.onPropertiesChanged(bundle)
        verify(systemStateListener).setHasLowBitAmbient(true)
        verify(systemStateListener).setHasBurnInProtection(false)
    }

    @Test
    fun onCreate_calls_setActiveComplications_withCorrectIDs() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            emptyList()
        )

        runPostedTasksFor(0)

        verify(iWatchFaceService).setActiveComplications(
            intArrayOf(LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID, BACKGROUND_COMPLICATION_ID),
            true
        )
    }

    @Test
    fun onCreate_calls_setContentDescriptionLabels_withCorrectArgs() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            emptyList()
        )

        runPostedTasksFor(0)

        val argument = ArgumentCaptor.forClass(Array<ContentDescriptionLabel>::class.java)
        verify(iWatchFaceService).setContentDescriptionLabels(argument.capture())
        assertThat(argument.value.size).isEqualTo(3)
        assertThat(argument.value[0].bounds).isEqualTo(Rect(25, 25, 75, 75)) // Clock element.
        assertThat(argument.value[1].bounds).isEqualTo(Rect(20, 40, 40, 60)) // Left complicaiton.
        assertThat(argument.value[2].bounds).isEqualTo(Rect(60, 40, 80, 60)) // Right complicaiton.
    }

    @Test
    fun setActiveComplications_afterDisablingSeveralComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            emptyList()
        )

        // Disabling complications should post a task which updates the active complications.
        leftComplication.enabled = false
        backgroundComplication.enabled = false
        runPostedTasksFor(0)
        verify(iWatchFaceService).setActiveComplications(intArrayOf(RIGHT_COMPLICATION_ID), true)
    }

    @Test
    fun initial_setContentDescriptionLabels_afterDisablingSeveralComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            emptyList()
        )

        // Ignore initial setContentDescriptionLabels call.
        reset(iWatchFaceService)

        // Disabling complications should post a task which updates the active complications.
        leftComplication.enabled = false
        backgroundComplication.enabled = false
        runPostedTasksFor(0)

        // Despite disabling the background complication we should still get a
        // ContentDescriptionLabel for the main clock element.
        val argument = ArgumentCaptor.forClass(Array<ContentDescriptionLabel>::class.java)
        verify(iWatchFaceService).setContentDescriptionLabels(argument.capture())
        assertThat(argument.value.size).isEqualTo(2)
        assertThat(argument.value[0].bounds).isEqualTo(Rect(25, 25, 75, 75)) // Clock element.
        assertThat(argument.value[1].bounds).isEqualTo(Rect(60, 40, 80, 60)) // Right complicaiton.
    }

    @Test
    fun getOptionForIdentifier_ListViewStyleCategory() {
        // Check the correct Options are returned for known option names.
        assertThat(colorStyleCategory.getOptionForId(redStyleOption.id)).isEqualTo(
            redStyleOption
        )
        assertThat(colorStyleCategory.getOptionForId(greenStyleOption.id)).isEqualTo(
            greenStyleOption
        )
        assertThat(colorStyleCategory.getOptionForId(blueStyleOption.id)).isEqualTo(
            blueStyleOption
        )

        // For unknown option names the first element in the list should be returned.
        assertThat(colorStyleCategory.getOptionForId("unknown")).isEqualTo(colorStyleList.first())
    }

    @Test
    fun centerX_and_centerY_containUpToDateValues() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList())

        assertThat(renderer.centerX).isEqualTo(50f)
        assertThat(renderer.centerY).isEqualTo(50f)

        reset(surfaceHolder)
        `when`(surfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 200, 300))
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 200, 300)

        assertThat(renderer.centerX).isEqualTo(100f)
        assertThat(renderer.centerY).isEqualTo(150f)
    }

    @Test
    fun requestStyleBeforeSetBinder() {
        var styleManager =
            UserStyleManager(emptyList())
        var testRenderer = TestRenderer(surfaceHolder, styleManager, systemState)
        val service = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationSet(
                listOf(leftComplication, rightComplication, backgroundComplication)
            ),
            testRenderer,
            UserStyleManager(emptyList()),
            systemState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )
        engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)
        `when`(surfaceHolder.surfaceFrame).thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)

        sendRequestStyle()

        // Trigger watch face creation.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion = 2)
        watchFace = service.watchFace

        val argument = ArgumentCaptor.forClass(WatchFaceStyle::class.java)
        verify(iWatchFaceService).setStyle(argument.capture())
        assertThat(argument.value.acceptsTapEvents).isEqualTo(true)
    }

    fun defaultProvidersWithFallbacks_newApi() {
        val provider1 = ComponentName("com.app1", "com.app1.App1")
        val provider2 = ComponentName("com.app2", "com.app2.App2")
        val complication = Complication(
            LEFT_COMPLICATION_ID,
            UnitSquareBoundsProvider(RectF(0.2f, 0.4f, 0.4f, 0.6f)),
            ComplicationDrawableRenderer(complicationDrawableLeft, systemState),
            intArrayOf(),
            Complication.DefaultComplicationProvider(
                listOf(provider1, provider2),
                SystemProviders.SUNRISE_SUNSET
            ),
            ComplicationData.TYPE_SHORT_TEXT
        )
        initEngine(WatchFaceType.ANALOG, listOf(complication), emptyList())

        verify(iWatchFaceService).setDefaultComplicationProviderWithFallbacks(
            LEFT_COMPLICATION_ID,
            listOf(provider1, provider2),
            SystemProviders.SUNRISE_SUNSET,
            ComplicationData.TYPE_SHORT_TEXT
        )
    }

    @Test
    fun defaultProvidersWithFallbacks_oldApi() {
        val provider1 = ComponentName("com.app1", "com.app1.App1")
        val provider2 = ComponentName("com.app2", "com.app2.App2")
        val complication = Complication(
            LEFT_COMPLICATION_ID,
            UnitSquareBoundsProvider(RectF(0.2f, 0.4f, 0.4f, 0.6f)),
            ComplicationDrawableRenderer(complicationDrawableLeft, systemState),
            intArrayOf(),
            Complication.DefaultComplicationProvider(
                listOf(provider1, provider2),
                SystemProviders.SUNRISE_SUNSET
            ),
            ComplicationData.TYPE_SHORT_TEXT
        )
        initEngine(WatchFaceType.ANALOG, listOf(complication), emptyList(), apiVersion = 0)

        verify(iWatchFaceService).setDefaultComplicationProvider(
            LEFT_COMPLICATION_ID, provider2, ComplicationData.TYPE_SHORT_TEXT
        )
        verify(iWatchFaceService).setDefaultComplicationProvider(
            LEFT_COMPLICATION_ID, provider1, ComplicationData.TYPE_SHORT_TEXT
        )
        verify(iWatchFaceService).setDefaultSystemComplicationProvider(
            LEFT_COMPLICATION_ID, SystemProviders.SUNRISE_SUNSET, ComplicationData.TYPE_SHORT_TEXT
        )
    }

    @Test
    fun registerWatchFaceType_called() {
        initEngine(WatchFaceType.DIGITAL, emptyList(), emptyList(), apiVersion = 4)
        verify(iWatchFaceService).registerWatchFaceType(WatchFaceType.DIGITAL)
    }

    @Test
    fun registerIWatchFaceCommand_called() {
        initEngine(WatchFaceType.DIGITAL, emptyList(), emptyList(), apiVersion = 4)
        verify(iWatchFaceService).registerIWatchFaceCommand(any())
    }
}
