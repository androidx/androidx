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
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IWallpaperWatchFaceControlService
import androidx.wear.watchface.control.IWallpaperWatchFaceControlServiceRequest
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.style.ComplicationsUserStyleCategory
import androidx.wear.watchface.style.ComplicationsUserStyleCategory.ComplicationOverlay
import androidx.wear.watchface.style.ComplicationsUserStyleCategory.ComplicationsOption
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.UserStyleRepository
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.`when`
import org.mockito.Mockito.atLeastOnce
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
private const val NO_COMPLICATIONS = "NO_COMPLICATIONS"
private const val LEFT_COMPLICATION = "LEFT_COMPLICATION"
private const val RIGHT_COMPLICATION = "RIGHT_COMPLICATION"
private const val LEFT_AND_RIGHT_COMPLICATIONS = "LEFT_AND_RIGHT_COMPLICATIONS"

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
        listOf(Layer.BASE_LAYER)
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
        listOf(Layer.TOP_LAYER)
    )

    private val badStyleOption =
        ListUserStyleCategory.ListOption("bad_option", "Bad", icon = null)

    private val leftComplication =
        Complication.Builder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableLeft,
                watchState.asWatchState()
            ).apply {
                data = createComplicationData()
            },
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET)
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()

    private val rightComplication =
        Complication.Builder(
            RIGHT_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                data = createComplicationData()
            },
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK)
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
            .build()

    private val backgroundComplication =
        Complication.Builder(
            BACKGROUND_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                data = createComplicationData()
            },
            intArrayOf(
                ComplicationData.TYPE_LARGE_IMAGE
            ),
            DefaultComplicationProviderPolicy()
        ).setDefaultProviderType(ComplicationData.TYPE_LARGE_IMAGE)
            .setAsBackgroundComplication()
            .build()

    private val leftAndRightComplicationsOption = ComplicationsOption(
        LEFT_AND_RIGHT_COMPLICATIONS,
        "Both",
        null,
        listOf(
            ComplicationOverlay.Builder(LEFT_COMPLICATION_ID)
                .setEnabled(true).build(),
            ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID)
                .setEnabled(true).build()
        )
    )
    private val noComplicationsOption = ComplicationsOption(
        NO_COMPLICATIONS,
        "Both",
        null,
        listOf(
            ComplicationOverlay.Builder(LEFT_COMPLICATION_ID)
                .setEnabled(false).build(),
            ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID)
                .setEnabled(false).build()
        )
    )
    private val leftComplicationsOption = ComplicationsOption(
        LEFT_COMPLICATION,
        "Left",
        null,
        listOf(
            ComplicationOverlay.Builder(LEFT_COMPLICATION_ID)
                .setEnabled(true).build(),
            ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID)
                .setEnabled(false).build()
        )
    )
    private val rightComplicationsOption = ComplicationsOption(
        RIGHT_COMPLICATION,
        "Right",
        null,
        listOf(
            ComplicationOverlay.Builder(LEFT_COMPLICATION_ID)
                .setEnabled(false).build(),
            ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID)
                .setEnabled(true).build()
        )
    )
    private val complicationsStyleCategory = ComplicationsUserStyleCategory(
        "complications_style_category",
        "Complications",
        "Number and position",
        icon = null,
        complicationConfig = listOf(
            leftAndRightComplicationsOption,
            noComplicationsOption,
            leftComplicationsOption,
            rightComplicationsOption
        ),
        affectsLayers = listOf(Layer.COMPLICATIONS)
    )

    private lateinit var renderer: TestRenderer
    private lateinit var complicationsManager: ComplicationsManager
    private lateinit var userStyleRepository: UserStyleRepository
    private lateinit var watchFace: WatchFace
    private lateinit var testWatchFaceService: TestWatchFaceService
    private lateinit var engineWrapper: WatchFaceService.EngineWrapper
    private lateinit var interactiveWatchFaceInstanceWCS: IInteractiveWatchFaceWCS

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
        apiVersion: Int = 2,
        hasLowBitAmbient: Boolean = false,
        hasBurnInProtection: Boolean = false
    ) {
        userStyleRepository = UserStyleRepository(userStyleCategories)
        this.complicationsManager = ComplicationsManager(complications, userStyleRepository)
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

        // Trigger watch face creation by sending the SurfceHolder, setting the binder and the
        // immutable properties.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion)
        sendImmutableProperties(engineWrapper, hasLowBitAmbient, hasBurnInProtection)

        watchFace = testWatchFaceService.watchFace
    }

    private fun initWallpaperInteractiveWatchFaceInstance(
        @WatchFaceType watchFaceType: Int,
        complications: List<Complication>,
        userStyleCategories: List<UserStyleCategory>,
        wallpaperInteractiveWatchFaceInstanceParams: WallpaperInteractiveWatchFaceInstanceParams
    ) {
        userStyleRepository = UserStyleRepository(userStyleCategories)
        this.complicationsManager = ComplicationsManager(complications, userStyleRepository)
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

        // The [SurfaceHolder] must be sent before binding.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)

        val serviceRequest = object : IWallpaperWatchFaceControlServiceRequest.Stub() {
            override fun getApiVersion() = IWallpaperWatchFaceControlServiceRequest.API_VERSION

            override fun registerWallpaperWatchFaceControlService(
                service: IWallpaperWatchFaceControlService
            ) {
                interactiveWatchFaceInstanceWCS = service.createInteractiveWatchFaceInstance(
                    wallpaperInteractiveWatchFaceInstanceParams
                )
            }
        }

        engineWrapper.onCommand(
            Constants.COMMAND_BIND_WALLPAPER_WATCH_FACE_CONTROL_SERVICE_REQUEST,
            0,
            0,
            0,
            Bundle().apply { putBinder(Constants.EXTRA_BINDER, serviceRequest.asBinder()) },
            false
        )
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

    private fun sendImmutableProperties(
        engine: WatchFaceService.EngineWrapper,
        hasLowBitAmbient: Boolean,
        hasBurnInProtection: Boolean
    ) {
        engine.onPropertiesChanged(
            Bundle().apply {
                putBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT, hasLowBitAmbient)
                putBoolean(Constants.PROPERTY_BURN_IN_PROTECTION, hasBurnInProtection)
            }
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
        if (this::interactiveWatchFaceInstanceWCS.isInitialized) {
            interactiveWatchFaceInstanceWCS.release()
        }

        validateMockitoUsage()
    }

    @Test
    fun maybeUpdateDrawMode_setsCorrectDrawMode() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())
        watchState.isAmbient.value = false

        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.isBatteryLowAndNotCharging.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.LOW_BATTERY_INTERACTIVE)

        watchState.isBatteryLowAndNotCharging.value = false
        watchState.isAmbient.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)

        watchState.isAmbient.value = false
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_NONE
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.MUTE)

        // Ambient takes precidence over interruption filter.
        watchState.isAmbient.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)

        watchState.isAmbient.value = false
        watchState.interruptionFilter.value = 0
        watchFace.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)
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

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap left complication.
        tapAt(30, 50)
        assertThat(complicationDrawableLeft.isHighlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        tapAt(70, 50)
        assertThat(complicationDrawableRight.isHighlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        tapAt(1, 1)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
    }

    @Test
    fun doubleTaps_correctlyDetected_and_highlightComplications() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap left complication.
        doubleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDrawableLeft.isHighlighted).isTrue()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        doubleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDrawableRight.isHighlighted).isTrue()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        doubleTapAt(1, 1, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
    }

    @Test
    fun fastTap_onDifferentComplications_ignored() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Rapidly tap left then right complication.
        tapAt(30, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        tapAt(70, 50)

        // Both complications get temporarily highlighted but neither onComplicationSingleTapped
        // nor onComplicationDoubleTapped fire.
        assertThat(complicationDrawableLeft.isHighlighted).isTrue()
        assertThat(complicationDrawableRight.isHighlighted).isTrue()
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()

        runPostedTasksFor(WatchFace.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()
    }

    @Test
    fun slow_doubleTap_recogisedAsSingleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Slowly tap left complication twice.
        doubleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() * 2)

        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()
    }

    @Test
    fun tripleTap_recogisedAsDoubleTap() {
        initEngine(WatchFaceType.ANALOG, listOf(leftComplication, rightComplication), emptyList())

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

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
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap right complication.
        tapAt(70, 50)
        assertThat(complicationDrawableRight.isHighlighted).isTrue()
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
        // The style should get persisted in a file because this test is set up using the legacy
        // Wear 2.0 APIs.
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            2
        )

        // This should get persisted.
        userStyleRepository.userStyle = UserStyle(
            hashMapOf(
                colorStyleCategory to blueStyleOption,
                watchHandStyleCategory to gothicStyleOption
            )
        )

        val userStyleRepository2 = UserStyleRepository(
            listOf(colorStyleCategory, watchHandStyleCategory)
        )

        val testRenderer2 =
            TestRenderer(surfaceHolder, userStyleRepository2, watchState.asWatchState())
        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(emptyList(), userStyleRepository2),
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
        sendImmutableProperties(engine2, false, false)

        assertThat(userStyleRepository2.userStyle.selectedOptions[colorStyleCategory]!!.id)
            .isEqualTo(
                blueStyleOption.id
            )
        assertThat(userStyleRepository2.userStyle.selectedOptions[watchHandStyleCategory]!!.id)
            .isEqualTo(
                gothicStyleOption.id
            )
    }

    @Test
    fun initWallpaperInteractiveWatchFaceInstanceWithUserStyle() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    DeviceConfig.SCREEN_SHAPE_ROUND
                ),
                SystemState(false, 0),
                UserStyle(
                    hashMapOf(
                        colorStyleCategory to blueStyleOption,
                        watchHandStyleCategory to gothicStyleOption
                    )
                ).toWireFormat(),
                null
            )
        )

        // The style option above should get applied during watch face creation.
        assertThat(userStyleRepository.userStyle.selectedOptions[colorStyleCategory]!!.id)
            .isEqualTo(
                blueStyleOption.id
            )
        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleCategory]!!.id)
            .isEqualTo(
                gothicStyleOption.id
            )
    }

    @Test
    fun initWallpaperInteractiveWatchFaceInstanceWithUserStyleThatDoesntMatchSchema() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    DeviceConfig.SCREEN_SHAPE_ROUND
                ),
                SystemState(false, 0),
                UserStyle(hashMapOf(watchHandStyleCategory to badStyleOption)).toWireFormat(),
                null
            )
        )

        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleCategory])
            .isEqualTo(watchHandStyleList.first())
    }

    @Test
    fun wear2ImmutablePropertiesSetCorrectly() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList(), 2, true, false)

        assertTrue(watchState.hasLowBitAmbient)
        assertFalse(watchState.hasBurnInProtection)
    }

    @Test
    fun wear2ImmutablePropertiesSetCorrectly2() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList(), 2, false, true)

        assertFalse(watchState.hasLowBitAmbient)
        assertTrue(watchState.hasBurnInProtection)
    }

    @Test
    fun wallpaperInteractiveWatchFaceImmutablePropertiesSetCorrectly() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            listOf(colorStyleCategory, watchHandStyleCategory),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    true,
                    false,
                    DeviceConfig.SCREEN_SHAPE_RECTANGULAR
                ),
                SystemState(false, 0),
                UserStyle(hashMapOf(watchHandStyleCategory to badStyleOption)).toWireFormat(),
                null
            )
        )

        assertTrue(watchState.hasLowBitAmbient)
        assertFalse(watchState.hasBurnInProtection)
        assertThat(watchState.screenShape).isEqualTo(DeviceConfig.SCREEN_SHAPE_RECTANGULAR)
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

        leftComplication.unitSquareBounds = RectF(0.3f, 0.3f, 0.5f, 0.5f)
        rightComplication.unitSquareBounds = RectF(0.7f, 0.75f, 0.9f, 0.95f)

        val complicationDetails = engineWrapper.getComplicationDetails()
        assertThat(complicationDetails[0].id).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDetails[0].complicationDetails.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[0].complicationDetails.bounds).isEqualTo(
            Rect(30, 30, 50, 50)
        )

        assertThat(complicationDetails[1].id).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDetails[1].complicationDetails.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[1].complicationDetails.bounds).isEqualTo(
            Rect(70, 75, 90, 95)
        )

        // Despite disabling the background complication we should still get a
        // ContentDescriptionLabel for the main clock element.
        val contentDescriptionLabels = watchFace.complicationsManager.getContentDescriptionLabels()
        assertThat(contentDescriptionLabels.size).isEqualTo(3)
        assertThat(contentDescriptionLabels[0].bounds).isEqualTo(
            Rect(
                25,
                25,
                75,
                75
            )
        ) // Clock element.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(
            Rect(
                30,
                30,
                50,
                50
            )
        ) // Left complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(
            Rect(
                70,
                75,
                90,
                95
            )
        ) // Right complication.
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

        val argument = ArgumentCaptor.forClass(SurfaceHolder.Callback::class.java)
        verify(surfaceHolder, atLeastOnce()).addCallback(argument.capture())

        // Trigger an update to a larger surface which should move the center.
        reset(surfaceHolder)
        `when`(surfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 200, 300))

        for (value in argument.allValues) {
            value.surfaceChanged(surfaceHolder, 0, 200, 300)
        }

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
                listOf(leftComplication, rightComplication, backgroundComplication),
                userStyleRepository
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
        sendImmutableProperties(engineWrapper, false, false)
        watchFace = service.watchFace

        val argument = ArgumentCaptor.forClass(WatchFaceStyle::class.java)
        verify(iWatchFaceService).setStyle(argument.capture())
        assertThat(argument.value.acceptsTapEvents).isEqualTo(true)
    }

    @Test
    fun defaultProvidersWithFallbacks_newApi() {
        val provider1 = ComponentName("com.app1", "com.app1.App1")
        val provider2 = ComponentName("com.app2", "com.app2.App2")
        val complication = Complication.Builder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawable(complicationDrawableLeft, watchState.asWatchState()),
            intArrayOf(),
            DefaultComplicationProviderPolicy(
                provider1,
                provider2,
                SystemProviders.SUNRISE_SUNSET
            )
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()
        initEngine(WatchFaceType.ANALOG, listOf(complication), emptyList())

        runPostedTasksFor(0)

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
            CanvasComplicationDrawable(complicationDrawableLeft, watchState.asWatchState()),
            intArrayOf(),
            DefaultComplicationProviderPolicy(
                provider1,
                provider2,
                SystemProviders.SUNRISE_SUNSET
            )
        ).setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
            .setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            .build()
        initEngine(WatchFaceType.ANALOG, listOf(complication), emptyList(), apiVersion = 0)

        runPostedTasksFor(0)

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
    fun previewReferenceTimeMillisAnalog() {
        initEngine(WatchFaceType.ANALOG, emptyList(), emptyList(), apiVersion = 4)
        assertThat(watchFace.previewReferenceTimeMillis)
            .isEqualTo(WatchFace.ANALOG_WATCHFACE_REFERENCE_TIME_MS)
    }

    @Test
    fun previewReferenceTimeMillisDigital() {
        initEngine(WatchFaceType.DIGITAL, emptyList(), emptyList(), apiVersion = 4)
        assertThat(watchFace.previewReferenceTimeMillis)
            .isEqualTo(WatchFace.DIGITAL_WATCHFACE_REFERENCE_TIME_MS)
    }

    @Test
    fun getComplicationDetails() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            emptyList(),
            apiVersion = 4
        )

        val complicationDetails = engineWrapper.getComplicationDetails()
        assertThat(complicationDetails[0].id).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDetails[0].complicationDetails.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[0].complicationDetails.bounds).isEqualTo(
            Rect(20, 40, 40, 60)
        )
        assertThat(complicationDetails[0].complicationDetails.supportedTypes).isEqualTo(
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            )
        )

        assertThat(complicationDetails[1].id).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDetails[1].complicationDetails.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[1].complicationDetails.bounds).isEqualTo(
            Rect(60, 40, 80, 60)
        )
        assertThat(complicationDetails[1].complicationDetails.supportedTypes).isEqualTo(
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            )
        )

        assertThat(complicationDetails[2].id).isEqualTo(BACKGROUND_COMPLICATION_ID)
        assertThat(complicationDetails[2].complicationDetails.boundsType).isEqualTo(
            ComplicationBoundsType.BACKGROUND
        )
        assertThat(complicationDetails[2].complicationDetails.bounds).isEqualTo(
            Rect(0, 0, 100, 100)
        )
        assertThat(complicationDetails[2].complicationDetails.supportedTypes).isEqualTo(
            intArrayOf(ComplicationData.TYPE_LARGE_IMAGE)
        )
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
            ComplicationsManager(emptyList(), userStyleRepository),
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
        sendImmutableProperties(engineWrapper, false, false)
        watchFace = service.watchFace

        // Enter ambient mode.
        watchState.isAmbient.value = true
        watchFace.maybeUpdateDrawMode()
        assertThat(testRenderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        // Simulate enter ambient animation finishing.
        testRenderer.animate = false
        watchFace.maybeUpdateDrawMode()
        assertThat(testRenderer.renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)
    }

    @Test
    fun complicationsUserStyleCategorySelectionAppliesChanges() {
        initEngine(
            WatchFaceType.DIGITAL,
            listOf(leftComplication, rightComplication),
            listOf(complicationsStyleCategory),
            apiVersion = 4
        )

        // Select a new style which turns off both complications.
        val newStyleA = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleA[complicationsStyleCategory] = noComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleA)

        runPostedTasksFor(0)

        assertFalse(leftComplication.enabled)
        assertFalse(rightComplication.enabled)
        verify(iWatchFaceService).setActiveComplications(intArrayOf(), true)

        val argumentA = ArgumentCaptor.forClass(Array<ContentDescriptionLabel>::class.java)
        verify(iWatchFaceService).setContentDescriptionLabels(argumentA.capture())
        assertThat(argumentA.value.size).isEqualTo(1)
        assertThat(argumentA.value[0].bounds).isEqualTo(Rect(25, 25, 75, 75)) // Clock element.

        reset(iWatchFaceService)

        // Select a new style which turns on only the left complication.
        val newStyleB = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleB[complicationsStyleCategory] = leftComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleB)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertFalse(rightComplication.enabled)
        verify(iWatchFaceService).setActiveComplications(intArrayOf(LEFT_COMPLICATION_ID), false)

        val argumentB = ArgumentCaptor.forClass(Array<ContentDescriptionLabel>::class.java)
        verify(iWatchFaceService).setContentDescriptionLabels(argumentB.capture())
        assertThat(argumentB.value.size).isEqualTo(2)
        assertThat(argumentB.value[0].bounds).isEqualTo(Rect(25, 25, 75, 75)) // Clock element.
        assertThat(argumentB.value[1].bounds).isEqualTo(Rect(20, 40, 40, 60)) // Left complication.
    }

    @Test
    fun partialComplicationOverrides() {
        val bothComplicationsOption = ComplicationsOption(
            LEFT_AND_RIGHT_COMPLICATIONS,
            "Left And Right",
            null,
            // An empty list means use the initial config.
            emptyList()
        )
        val leftOnlyComplicationsOption = ComplicationsOption(
            LEFT_COMPLICATION,
            "Left",
            null,
            listOf(ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID).setEnabled(false).build())
        )
        val rightOnlyComplicationsOption = ComplicationsOption(
            RIGHT_COMPLICATION,
            "Right",
            null,
            listOf(ComplicationOverlay.Builder(LEFT_COMPLICATION_ID).setEnabled(false).build())
        )
        val complicationsStyleCategory = ComplicationsUserStyleCategory(
            "complications_style_category",
            "Complications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                bothComplicationsOption,
                leftOnlyComplicationsOption,
                rightOnlyComplicationsOption
            ),
            affectsLayers = listOf(Layer.COMPLICATIONS)
        )

        initEngine(
            WatchFaceType.DIGITAL,
            listOf(leftComplication, rightComplication),
            listOf(complicationsStyleCategory),
            apiVersion = 4
        )

        assertTrue(leftComplication.enabled)
        assertTrue(rightComplication.enabled)

        // Select left complication only.
        val newStyleA = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleA[complicationsStyleCategory] = leftOnlyComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleA)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertFalse(rightComplication.enabled)

        // Select right complication only.
        val newStyleB = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleB[complicationsStyleCategory] = rightOnlyComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleB)

        runPostedTasksFor(0)

        assertFalse(leftComplication.enabled)
        assertTrue(rightComplication.enabled)

        // Select both complications.
        val newStyleC = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleC[complicationsStyleCategory] = bothComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleC)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertTrue(rightComplication.enabled)
    }
}
