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
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.complications.ComplicationData
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.ComplicationDetails
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
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

    private val handler = mock<Handler>()
    private val iWatchFaceService = mock<IWatchFaceService>()
    private val surfaceHolder = mock<SurfaceHolder>()
    private val watchState = MutableWatchState()

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
        colorStyleList,
        UserStyleCategory.LAYER_WATCH_FACE_BASE
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
        watchHandStyleList,
        UserStyleCategory.LAYER_WATCH_FACE_UPPER
    )

    private val badStyleOption =
        ListUserStyleCategory.ListOption("bad_option", "Bad", icon = null)

    private val leftComplication =
        Complication.Builder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(
                complicationDrawableLeft,
                watchState.asWatchState()
            ).apply {
                setData(createComplicationData())
            },
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            Complication.DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET)
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()

    private val rightComplication =
        Complication.Builder(
            RIGHT_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                setData(createComplicationData())
            },
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            Complication.DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK)
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
            .build()

    private val backgroundComplication =
        Complication.Builder(
            BACKGROUND_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                setData(createComplicationData())
            },
            intArrayOf(
                ComplicationData.TYPE_LARGE_IMAGE
            ),
            Complication.DefaultComplicationProviderPolicy()
        ).setDefaultProviderType(ComplicationData.TYPE_LARGE_IMAGE)
            .setBackgroundComplication()
            .build()

    private lateinit var renderer: TestRenderer
    private lateinit var complicationsManager: ComplicationsManager
    private lateinit var userStyleRepository: UserStyleRepository
    private lateinit var watchFace: WatchFace
    private lateinit var testWatchFaceService: TestWatchFaceService
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
        this.complicationsManager = ComplicationsManager(complications)
        userStyleRepository =
            UserStyleRepository(userStyleCategories)
        renderer = TestRenderer(surfaceHolder, userStyleRepository, watchState.asWatchState())
        testWatchFaceService = TestWatchFaceService(
            watchFaceType,
            this.complicationsManager,
            renderer,
            userStyleRepository,
            watchState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )
        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)
        // Trigger watch face creation.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion)
        watchFace = testWatchFaceService.watchFace
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
        `when`(handler.getLooper()).thenReturn(Looper.myLooper())

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
    fun maybeUpdateDrawMode_setsCorrectDrawMode() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        watchState.isAmbient.value = false

        assertThat(renderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.isBatteryLowAndNotCharging.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.LOW_BATTERY_INTERACTIVE)

        watchState.isBatteryLowAndNotCharging.value = false
        watchState.isAmbient.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.AMBIENT)

        watchState.isAmbient.value = false
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_NONE
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.MUTE)

        // Ambient takes precidence over interruption filter.
        watchState.isAmbient.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.AMBIENT)

        watchState.isAmbient.value = false
        watchState.interruptionFilter.value = 0
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)
    }

    @Test
    fun onDraw_calendar_setFromSystemTime() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)
    }

    @Test
    fun onDraw_calendar_affectedCorrectly_with2xMockTime() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L

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
        testWatchFaceService.mockSystemTimeMillis = 2000L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(3000L)
    }

    @Test
    fun onDraw_calendar_affectedCorrectly_withMockTimeWrapping() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L

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

        testWatchFaceService.mockSystemTimeMillis = 1250L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1500L)

        testWatchFaceService.mockSystemTimeMillis = 1499L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1998L)

        testWatchFaceService.mockSystemTimeMillis = 1500L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1000L)

        testWatchFaceService.mockSystemTimeMillis = 1750L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1500L)

        testWatchFaceService.mockSystemTimeMillis = 1999L
        watchFace.onDraw()
        assertThat(watchFace.calendar.timeInMillis).isEqualTo(1998L)

        testWatchFaceService.mockSystemTimeMillis = 2000L
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
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        tapAt(70, 50)
        assertThat(complicationDrawableRight.highlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        tapAt(1, 1)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()

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
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDrawableLeft.highlighted).isTrue()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        doubleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDrawableRight.highlighted).isTrue()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        doubleTapAt(1, 1, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()

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
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()

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

        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()
    }

    @Test
    fun tripleTap_recogisedAsDoubleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Quickly tap left complication thrice.
        tripleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)

        assertThat(testWatchFaceService.complicationSingleTapped).isNull()
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
    }

    @Test
    fun tapCancel_after_tapDown_at_same_location_HandledAsSingleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        testWatchFaceService.reset()
        // Tap/Cancel left complication
        tapCancelAt(30, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
    }

    @Test
    fun tapDown_then_tapDown_tapCancel_HandledAsSingleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        testWatchFaceService.reset()
        // Tap down left Complication
        watchFace.onTapCommand(TapType.TOUCH, 30, 50)

        // Tap down at right complication
        watchFace.onTapCommand(TapType.TOUCH, 70, 50)

        // Now Tap cancel at the second position
        watchFace.onTapCommand(TapType.TOUCH_CANCEL, 70, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
    }

    @Test
    fun tapDown_tapCancel_different_positions_CancelsTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        testWatchFaceService.reset()
        // Tap down at a position in left Complication
        watchFace.onTapCommand(TapType.TOUCH, 30, 50)
        // Tap cancel at different position stillin left Complication
        watchFace.onTapCommand(TapType.TOUCH_CANCEL, 32, 50)

        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()
    }

    @Test
    fun singleTap_recognisedAfterTripleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        // Quickly tap right complication thrice.
        tripleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)

        // Wait a bit for the condition to reset and clear our detection state.
        testWatchFaceService.clearTappedState()
        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.highlighted).isFalse()
        assertThat(complicationDrawableRight.highlighted).isFalse()

        // Tap right complication.
        tapAt(70, 50)
        assertThat(complicationDrawableRight.highlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()
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
        ).isEqualTo(INTERACTIVE_UPDATE_RATE_MS)
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
        ).isEqualTo(INTERACTIVE_UPDATE_RATE_MS - 1)
    }

    @Test
    fun computeDelayTillNextFrame_beginFrameTimeInTheFuture() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(
            watchFace.computeDelayTillNextFrame(
                beginFrameTimeMillis = 100,
                currentTimeMillis = 10
            )
        ).isEqualTo(INTERACTIVE_UPDATE_RATE_MS)
    }

    @Test
    fun getComplicationIdAt_returnsCorrectComplications() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationsManager.getComplicationAt(30, 50)!!.id)
            .isEqualTo(LEFT_COMPLICATION_ID)
        leftComplication.enabled = false
        assertThat(complicationsManager.getComplicationAt(30, 50)).isNull()

        assertThat(complicationsManager.getComplicationAt(70, 50)!!.id)
            .isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationsManager.getComplicationAt(1, 1)).isNull()
    }

    @Test
    fun getBackgroundComplicationId_returnsCorrectId() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        assertThat(complicationsManager.getBackgroundComplication()).isNull()

        initEngine(WatchFaceType.ANALOG, listOf(leftComplication), emptyList())
        assertThat(complicationsManager.getBackgroundComplication()).isNull()

        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, backgroundComplication),
            emptyList()
        )
        assertThat(complicationsManager.getBackgroundComplication()!!.id).isEqualTo(
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
        userStyleRepository.userStyle = UserStyle(hashMapOf(
            colorStyleCategory to blueStyleOption,
            watchHandStyleCategory to gothicStyleOption
        ))

        val userStyleRepository2 = UserStyleRepository(
            listOf(colorStyleCategory, watchHandStyleCategory)
        )

        val testRenderer2 =
            TestRenderer(surfaceHolder, userStyleRepository2, watchState.asWatchState())
        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(emptyList()),
            testRenderer2,
            userStyleRepository2,
            watchState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )

        // Trigger watch face creation.
        val engine2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
        engine2.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engine2, apiVersion = 2)

        assertThat(userStyleRepository2.userStyle.options[colorStyleCategory]).isEqualTo(
            blueStyleOption
        )
        assertThat(userStyleRepository2.userStyle.options[watchHandStyleCategory]).isEqualTo(
            gothicStyleOption
        )
    }

    @Test
    fun getStoredUserStyleSupported_userStyle_isPersisted() {
        var persistedStyle: UserStyleWireFormat? = null

        // Mock the behavior of Home/SysUI which should persist the style.
        doAnswer {
            persistedStyle
        }.`when`(iWatchFaceService).getStoredUserStyle()

        `when`(iWatchFaceService.setCurrentUserStyle(any())).then {
            persistedStyle = it.arguments[0] as UserStyleWireFormat
            Unit
        }

        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            3
        )

        // This should get persisted.
        userStyleRepository.userStyle = UserStyle(hashMapOf(
            colorStyleCategory to blueStyleOption,
            watchHandStyleCategory to gothicStyleOption
        ))

        val userStyleRepository2 = UserStyleRepository(
            listOf(colorStyleCategory, watchHandStyleCategory)
        )

        val testRenderer2 =
            TestRenderer(surfaceHolder, userStyleRepository2, watchState.asWatchState())
        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(emptyList()),
            testRenderer2,
            userStyleRepository2,
            watchState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )

        // Trigger watch face creation.
        val engine2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
        engine2.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engine2, apiVersion = 3)

        assertThat(userStyleRepository2.userStyle.options[colorStyleCategory]).isEqualTo(
            blueStyleOption
        )
        assertThat(userStyleRepository2.userStyle.options[watchHandStyleCategory]).isEqualTo(
            gothicStyleOption
        )
    }

    @Test
    fun persistedStyleOptionMismatchIgnored() {
        `when`(iWatchFaceService.getStoredUserStyle()).thenReturn(
            UserStyle(hashMapOf(watchHandStyleCategory to badStyleOption)).toWireFormat()
        )

        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            3
        )

        assertThat(testWatchFaceService.lastUserStyle!!.options[watchHandStyleCategory])
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

        val isChargingObserver = mock<Observer<Boolean>>()
        val inAirplaneModeObserver = mock<Observer<Boolean>>()
        val isConnectedToCompanionObserver = mock<Observer<Boolean>>()
        val isInTheaterModeObserver = mock<Observer<Boolean>>()
        val isGpsActiveObserver = mock<Observer<Boolean>>()
        val isKeyguardLockedObserver = mock<Observer<Boolean>>()
        watchState.isCharging.observe(isChargingObserver)
        watchState.inAirplaneMode.observe(inAirplaneModeObserver)
        watchState.isConnectedToCompanion.observe(isConnectedToCompanionObserver)
        watchState.isInTheaterMode.observe(isInTheaterModeObserver)
        watchState.isGpsActive.observe(isGpsActiveObserver)
        watchState.isKeyguardLocked.observe(isKeyguardLockedObserver)

        // Every indicator onXyz method should be called upon the initial update.
        engineWrapper.onBackgroundAction(Bundle().apply {
            putBundle(Constants.EXTRA_INDICATOR_STATUS, bundle)
        })

        verify(isChargingObserver).onChanged(true)
        verify(inAirplaneModeObserver).onChanged(false)
        verify(isConnectedToCompanionObserver).onChanged(true)
        verify(isInTheaterModeObserver).onChanged(false)
        verify(isGpsActiveObserver).onChanged(true)
        verify(isKeyguardLockedObserver).onChanged(false)

        reset(isChargingObserver)
        reset(inAirplaneModeObserver)
        reset(isConnectedToCompanionObserver)
        reset(isInTheaterModeObserver)
        reset(isGpsActiveObserver)
        reset(isKeyguardLockedObserver)

        // Check only the modified setIsCharging state leads to a call.
        bundle.putBoolean(Constants.STATUS_CHARGING, false)
        engineWrapper.onBackgroundAction(Bundle().apply {
            putBundle(Constants.EXTRA_INDICATOR_STATUS, bundle)
        })
        verify(isChargingObserver).onChanged(false)
        verify(inAirplaneModeObserver, times(0)).onChanged(anyBoolean())
        verify(isConnectedToCompanionObserver, times(0)).onChanged(anyBoolean())
        verify(isInTheaterModeObserver, times(0)).onChanged(anyBoolean())
        verify(isGpsActiveObserver, times(0)).onChanged(anyBoolean())
        verify(isKeyguardLockedObserver, times(0)).onChanged(anyBoolean())
    }

    @Test
    fun onPropertiesChanged_issuesCorrectApiCalls() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList())
        val bundle = Bundle().apply {
            putBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT, true)
            putBoolean(Constants.PROPERTY_BURN_IN_PROTECTION, false)
        }

        val hasLowBitAmbientObserver = mock<Observer<Boolean>>()
        val hasBurnInProtectionObserver = mock<Observer<Boolean>>()
        watchState.hasLowBitAmbient.observe(hasLowBitAmbientObserver)
        watchState.hasBurnInProtection.observe(hasBurnInProtectionObserver)

        // Check all the right methods are called on initial onPropertiesChanged call.
        engineWrapper.onPropertiesChanged(bundle)
        verify(hasLowBitAmbientObserver).onChanged(true)
        verify(hasBurnInProtectionObserver).onChanged(false)
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

        verify(iWatchFaceService).setActiveComplications(
            intArrayOf(rightComplication.id),
            true
        )

        // Despite disabling the background complication we should still get a
        // ContentDescriptionLabel for the main clock element.
        val argument = ArgumentCaptor.forClass(Array<ContentDescriptionLabel>::class.java)
        verify(iWatchFaceService).setContentDescriptionLabels(argument.capture())
        assertThat(argument.value.size).isEqualTo(2)
        assertThat(argument.value[0].bounds).isEqualTo(Rect(25, 25, 75, 75)) // Clock element.
        assertThat(argument.value[1].bounds).isEqualTo(Rect(60, 40, 80, 60)) // Right complication.
    }

    @Test
    fun moveComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            emptyList(),
            4
        )

        // Ignore initial setContentDescriptionLabels call.
        reset(iWatchFaceService)

        leftComplication.unitSquareBounds = RectF(0.3f, 0.3f, 0.5f, 0.5f)
        rightComplication.unitSquareBounds = RectF(0.7f, 0.75f, 0.9f, 0.95f)

        runPostedTasksFor(0)

        val complicationId = ArgumentCaptor.forClass(Int::class.java)
        val complicationDetails = ArgumentCaptor.forClass(ComplicationDetails::class.java)
        verify(iWatchFaceService, times(2)).setComplicationDetails(
            complicationId.capture(), complicationDetails.capture()
        )

        assertThat(complicationId.allValues[0]).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDetails.allValues[0].boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT)
        assertThat(complicationDetails.allValues[0].bounds).isEqualTo(
            Rect(30, 30, 50, 50))

        assertThat(complicationId.allValues[1]).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDetails.allValues[1].boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT)
        assertThat(complicationDetails.allValues[1].bounds).isEqualTo(
            Rect(70, 75, 90, 95))

        // Despite disabling the background complication we should still get a
        // ContentDescriptionLabel for the main clock element.
        val argument = ArgumentCaptor.forClass(Array<ContentDescriptionLabel>::class.java)
        verify(iWatchFaceService).setContentDescriptionLabels(argument.capture())
        assertThat(argument.value.size).isEqualTo(3)
        assertThat(argument.value[0].bounds).isEqualTo(Rect(25, 25, 75, 75)) // Clock element.
        assertThat(argument.value[1].bounds).isEqualTo(Rect(30, 30, 50, 50)) // Left complication.
        assertThat(argument.value[2].bounds).isEqualTo(Rect(70, 75, 90, 95)) // Right complication.
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
        var userStyleRepository =
            UserStyleRepository(emptyList())
        var testRenderer =
            TestRenderer(surfaceHolder, userStyleRepository, watchState.asWatchState())
        val service = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(
                listOf(leftComplication, rightComplication, backgroundComplication)
            ),
            testRenderer,
            UserStyleRepository(emptyList()),
            watchState,
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
        val complication = Complication.Builder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(complicationDrawableLeft, watchState.asWatchState()),
            intArrayOf(),
            Complication.DefaultComplicationProviderPolicy(
                listOf(provider1, provider2),
                SystemProviders.SUNRISE_SUNSET
            )
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()
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
        val complication = Complication.Builder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawableRenderer(complicationDrawableLeft, watchState.asWatchState()),
            intArrayOf(),
            Complication.DefaultComplicationProviderPolicy(
                listOf(provider1, provider2),
                SystemProviders.SUNRISE_SUNSET
            )
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()
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

    @Test
    fun setComplicationDetails_called() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            emptyList(),
            apiVersion = 4
        )

        runPostedTasksFor(0)

        val complicationId = ArgumentCaptor.forClass(Int::class.java)
        val complicationDetails = ArgumentCaptor.forClass(ComplicationDetails::class.java)
        verify(iWatchFaceService, times(3)).setComplicationDetails(
            complicationId.capture(), complicationDetails.capture()
        )

        assertThat(complicationId.allValues[0]).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDetails.allValues[0].boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT)
        assertThat(complicationDetails.allValues[0].bounds).isEqualTo(
            Rect(20, 40, 40, 60))
        assertThat(complicationDetails.allValues[0].supportedTypes).isEqualTo(
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ))

        assertThat(complicationId.allValues[1]).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDetails.allValues[1].boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT)
        assertThat(complicationDetails.allValues[1].bounds).isEqualTo(
            Rect(60, 40, 80, 60))
        assertThat(complicationDetails.allValues[0].supportedTypes).isEqualTo(
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ))

        assertThat(complicationId.allValues[2]).isEqualTo(BACKGROUND_COMPLICATION_ID)
        assertThat(complicationDetails.allValues[2].boundsType).isEqualTo(
            ComplicationBoundsType.BACKGROUND)
        assertThat(complicationDetails.allValues[2].bounds).isEqualTo(
            Rect(0, 0, 100, 100))
        assertThat(complicationDetails.allValues[2].supportedTypes).isEqualTo(
            intArrayOf(ComplicationData.TYPE_LARGE_IMAGE))
    }

    @Test
    fun shouldAnimateOverrideControlsEnteringAmbientMode() {
        var userStyleRepository = UserStyleRepository(emptyList())
        var testRenderer = object :
            TestRenderer(surfaceHolder, userStyleRepository, watchState.asWatchState()) {
            var animate = true
            override fun shouldAnimate() = animate
        }
        val service = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(emptyList()),
            testRenderer,
            UserStyleRepository(emptyList()),
            watchState,
            handler,
            INTERACTIVE_UPDATE_RATE_MS
        )

        engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)
        `when`(surfaceHolder.surfaceFrame).thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)

        // Trigger watch face creation.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion = 2)
        watchFace = service.watchFace

        // Enter ambient mode.
        watchState.isAmbient.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(testRenderer.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        // Simulate enter ambient animation finishing.
        testRenderer.animate = false
        watchFace.maybeUpdateDrawMode()
        assertThat(testRenderer.drawMode).isEqualTo(DrawMode.AMBIENT)
    }
}
