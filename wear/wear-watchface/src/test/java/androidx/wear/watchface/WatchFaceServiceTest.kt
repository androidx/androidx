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
import android.support.wearable.complications.ComplicationText
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IPendingInteractiveWatchFaceWCS
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationsOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
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
    private val tapListener = mock<WatchFace.TapListener>()
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
    private val complicationDrawableBackground = ComplicationDrawable(context)

    private val redStyleOption =
        ListUserStyleSetting.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleSetting.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleSetting.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = ListUserStyleSetting(
        "color_style_setting",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(Layer.BASE_LAYER)
    )

    private val classicStyleOption =
        ListUserStyleSetting.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleSetting.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleSetting.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = ListUserStyleSetting(
        "hand_style_setting",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(Layer.TOP_LAYER)
    )

    private val badStyleOption =
        ListUserStyleSetting.ListOption("bad_option", "Bad", icon = null)

    private val leftComplication =
        Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableLeft,
                watchState.asWatchState()
            ).apply {
                idAndData = createIdAndComplicationData(LEFT_COMPLICATION_ID)
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val rightComplication =
        Complication.createRoundRectComplicationBuilder(
            RIGHT_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableRight,
                watchState.asWatchState()
            ).apply {
                idAndData = createIdAndComplicationData(RIGHT_COMPLICATION_ID)
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK),
            ComplicationBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val backgroundComplication =
        Complication.createBackgroundComplicationBuilder(
            BACKGROUND_COMPLICATION_ID,
            CanvasComplicationDrawable(
                complicationDrawableBackground,
                watchState.asWatchState()
            ).apply {
                idAndData = createIdAndComplicationData(BACKGROUND_COMPLICATION_ID)
            },
            listOf(
                ComplicationType.PHOTO_IMAGE
            ),
            DefaultComplicationProviderPolicy()
        ).setDefaultProviderType(ComplicationType.PHOTO_IMAGE)
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
    private val complicationsStyleSetting = ComplicationsUserStyleSetting(
        "complications_style_setting",
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
    private lateinit var watchFaceImpl: WatchFaceImpl
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
        userStyleSchema: UserStyleSchema,
        apiVersion: Int = 2,
        hasLowBitAmbient: Boolean = false,
        hasBurnInProtection: Boolean = false,
        tapListener: WatchFace.TapListener? = null
    ) {
        userStyleRepository = UserStyleRepository(userStyleSchema)
        complicationsManager = ComplicationsManager(complications, userStyleRepository)
        renderer = TestRenderer(
            surfaceHolder,
            userStyleRepository,
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        )
        testWatchFaceService = TestWatchFaceService(
            watchFaceType,
            complicationsManager,
            renderer,
            userStyleRepository,
            watchState,
            handler,
            tapListener
        )
        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)

        // Trigger watch face creation by sending the SurfceHolder, setting the binder and the
        // immutable properties.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion)
        sendImmutableProperties(engineWrapper, hasLowBitAmbient, hasBurnInProtection)

        watchFaceImpl = engineWrapper.watchFaceImpl
    }

    private fun initWallpaperInteractiveWatchFaceInstance(
        @WatchFaceType watchFaceType: Int,
        complications: List<Complication>,
        userStyleSchema: UserStyleSchema,
        wallpaperInteractiveWatchFaceInstanceParams: WallpaperInteractiveWatchFaceInstanceParams
    ) {
        userStyleRepository = UserStyleRepository(userStyleSchema)
        complicationsManager = ComplicationsManager(complications, userStyleRepository)
        renderer = TestRenderer(
            surfaceHolder,
            userStyleRepository,
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        )
        testWatchFaceService = TestWatchFaceService(
            watchFaceType,
            complicationsManager,
            renderer,
            userStyleRepository,
            watchState,
            handler,
            null
        )

        InteractiveInstanceManager
            .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                    wallpaperInteractiveWatchFaceInstanceParams,
                    object : IPendingInteractiveWatchFaceWCS.Stub() {
                        override fun getApiVersion() =
                            IPendingInteractiveWatchFaceWCS.API_VERSION

                        override fun onInteractiveWatchFaceWcsCreated(
                            iInteractiveWatchFaceWcs: IInteractiveWatchFaceWCS
                        ) {
                            interactiveWatchFaceInstanceWCS = iInteractiveWatchFaceWcs
                        }
                    }
                )
            )

        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)

        // The [SurfaceHolder] must be sent before binding.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        watchFaceImpl = engineWrapper.watchFaceImpl
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
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        watchState.isAmbient.value = false

        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.isBatteryLowAndNotCharging.value = true
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.LOW_BATTERY_INTERACTIVE)

        watchState.isBatteryLowAndNotCharging.value = false
        watchState.isAmbient.value = true
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)

        watchState.isAmbient.value = false
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_NONE
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.MUTE)

        // Ambient takes precidence over interruption filter.
        watchState.isAmbient.value = true
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)

        watchState.isAmbient.value = false
        watchState.interruptionFilter.value = 0
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)
    }

    @Test
    fun onDraw_calendar_setFromSystemTime() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1000L)
    }

    @Test
    fun onDraw_calendar_affectedCorrectly_with2xMockTime() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L

        watchFaceImpl.mockTimeReceiver.onReceive(
            context,
            Intent(WatchFaceImpl.MOCK_TIME_INTENT).apply {
                putExtra(WatchFaceImpl.EXTRA_MOCK_TIME_SPEED_MULTIPLIER, 2.0f)
                putExtra(WatchFaceImpl.EXTRA_MOCK_TIME_WRAPPING_MIN_TIME, -1L)
            }
        )

        // Time should not diverge initially.
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1000L)

        // However 1000ms of real time should result in 2000ms observed by onDraw.
        testWatchFaceService.mockSystemTimeMillis = 2000L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(3000L)
    }

    @Test
    fun onDraw_calendar_affectedCorrectly_withMockTimeWrapping() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L

        watchFaceImpl.mockTimeReceiver.onReceive(
            context,
            Intent(WatchFaceImpl.MOCK_TIME_INTENT).apply {
                putExtra(WatchFaceImpl.EXTRA_MOCK_TIME_SPEED_MULTIPLIER, 2.0f)
                putExtra(WatchFaceImpl.EXTRA_MOCK_TIME_WRAPPING_MIN_TIME, 1000L)
                putExtra(WatchFaceImpl.EXTRA_MOCK_TIME_WRAPPING_MAX_TIME, 2000L)
            }
        )

        // Time in millis observed by onDraw should wrap betwween 1000 and 2000.
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1000L)

        testWatchFaceService.mockSystemTimeMillis = 1250L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1500L)

        testWatchFaceService.mockSystemTimeMillis = 1499L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1998L)

        testWatchFaceService.mockSystemTimeMillis = 1500L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1000L)

        testWatchFaceService.mockSystemTimeMillis = 1750L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1500L)

        testWatchFaceService.mockSystemTimeMillis = 1999L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1998L)

        testWatchFaceService.mockSystemTimeMillis = 2000L
        watchFaceImpl.onDraw()
        assertThat(watchFaceImpl.calendar.timeInMillis).isEqualTo(1000L)
    }

    private fun tapAt(x: Int, y: Int) {
        // The eventTime is ignored.
        watchFaceImpl.onTapCommand(TapType.TOUCH, x, y)
        watchFaceImpl.onTapCommand(TapType.TAP, x, y)
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
        watchFaceImpl.onTapCommand(TapType.TOUCH, x, y)
        watchFaceImpl.onTapCommand(TapType.TOUCH_CANCEL, x, y)
    }

    @Test
    fun singleTaps_correctlyDetected_and_highlightComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap left complication.
        tapAt(30, 50)
        assertThat(complicationDrawableLeft.isHighlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        tapAt(70, 50)
        assertThat(complicationDrawableRight.isHighlighted).isTrue()
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        tapAt(1, 1)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
    }

    @Test
    fun doubleTaps_correctlyDetected_and_highlightComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap left complication.
        doubleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDrawableLeft.isHighlighted).isTrue()

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        doubleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDrawableRight.isHighlighted).isTrue()

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        doubleTapAt(1, 1, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
    }

    @Test
    fun fastTap_onDifferentComplications_ignored() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

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

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()
    }

    @Test
    fun slow_doubleTap_recogisedAsSingleTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Slowly tap left complication twice.
        doubleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() * 2)

        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()
    }

    @Test
    fun tripleTap_recogisedAsDoubleTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Quickly tap left complication thrice.
        tripleTapAt(30, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)

        assertThat(testWatchFaceService.complicationSingleTapped).isNull()
        assertThat(testWatchFaceService.complicationDoubleTapped).isEqualTo(LEFT_COMPLICATION_ID)
    }

    @Test
    fun tapCancel_after_tapDown_at_same_location_HandledAsSingleTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        testWatchFaceService.reset()
        // Tap/Cancel left complication
        tapCancelAt(30, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(LEFT_COMPLICATION_ID)
    }

    @Test
    fun tapDown_then_tapDown_tapCancel_HandledAsSingleTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        testWatchFaceService.reset()
        // Tap down left Complication
        watchFaceImpl.onTapCommand(TapType.TOUCH, 30, 50)

        // Tap down at right complication
        watchFaceImpl.onTapCommand(TapType.TOUCH, 70, 50)

        // Now Tap cancel at the second position
        watchFaceImpl.onTapCommand(TapType.TOUCH_CANCEL, 70, 50)
        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isEqualTo(RIGHT_COMPLICATION_ID)
    }

    @Test
    fun tapDown_tapCancel_different_positions_CancelsTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        testWatchFaceService.reset()
        // Tap down at a position in left Complication
        watchFaceImpl.onTapCommand(TapType.TOUCH, 30, 50)
        // Tap cancel at different position stillin left Complication
        watchFaceImpl.onTapCommand(TapType.TOUCH_CANCEL, 32, 50)

        runPostedTasksFor(ViewConfiguration.getDoubleTapTimeout().toLong())
        assertThat(testWatchFaceService.complicationSingleTapped).isNull()
        assertThat(testWatchFaceService.complicationDoubleTapped).isNull()
    }

    @Test
    fun singleTap_recognisedAfterTripleTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        // Quickly tap right complication thrice.
        tripleTapAt(70, 50, ViewConfiguration.getDoubleTapTimeout().toLong() / 2)

        // Wait a bit for the condition to reset and clear our detection state.
        testWatchFaceService.clearTappedState()
        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
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
    fun tapListener_tap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            tapListener = tapListener
        )

        // Tap on nothing.
        tapAt(1, 1)

        verify(tapListener).onTap(TapType.TOUCH, 1, 1)
        verify(tapListener).onTap(TapType.TAP, 1, 1)
    }

    @Test
    fun tapListener_tapComplication() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            tapListener = tapListener
        )

        // Tap right complication.
        tapAt(70, 50)

        verify(tapListener, times(0)).onTap(TapType.TOUCH, 70, 50)
        verify(tapListener, times(0)).onTap(TapType.TAP, 70, 50)
    }

    @Test
    fun interactiveFrameRate_reducedWhenBatteryLow() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            INTERACTIVE_UPDATE_RATE_MS
        )

        // The delay should change when battery is low.
        watchFaceImpl.batteryLevelReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_LOW)
        )
        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            WatchFaceImpl.MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS
        )

        // And go back to normal when battery is OK.
        watchFaceImpl.batteryLevelReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_OKAY)
        )
        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            INTERACTIVE_UPDATE_RATE_MS
        )
    }

    @Test
    fun computeDelayTillNextFrame_accountsForSlowDraw() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(
            watchFaceImpl.computeDelayTillNextFrame(
                beginFrameTimeMillis = 0,
                currentTimeMillis = 2
            )
        )
            .isEqualTo(INTERACTIVE_UPDATE_RATE_MS - 2)
    }

    @Test
    fun computeDelayTillNextFrame_dropsFramesForVerySlowDraw() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(
            watchFaceImpl.computeDelayTillNextFrame(
                beginFrameTimeMillis = 0,
                currentTimeMillis = INTERACTIVE_UPDATE_RATE_MS
            )
        ).isEqualTo(INTERACTIVE_UPDATE_RATE_MS)
    }

    @Test
    fun computeDelayTillNextFrame_perservesPhaseForVerySlowDraw() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        // The phase of beginFrameTimeMillis % INTERACTIVE_UPDATE_RATE_MS is 2, but the phase of
        // currentTimeMillis % INTERACTIVE_UPDATE_RATE_MS is 3, so we expect to delay
        // INTERACTIVE_UPDATE_RATE_MS - 1 to preserve the phase while dropping a frame.
        assertThat(
            watchFaceImpl.computeDelayTillNextFrame(
                beginFrameTimeMillis = 2,
                currentTimeMillis = INTERACTIVE_UPDATE_RATE_MS + 3
            )
        ).isEqualTo(INTERACTIVE_UPDATE_RATE_MS - 1)
    }

    @Test
    fun computeDelayTillNextFrame_beginFrameTimeInTheFuture() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(
            watchFaceImpl.computeDelayTillNextFrame(
                beginFrameTimeMillis = 100,
                currentTimeMillis = 10
            )
        ).isEqualTo(INTERACTIVE_UPDATE_RATE_MS)
    }

    @Test
    fun getComplicationIdAt_returnsCorrectComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

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
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        assertThat(complicationsManager.getBackgroundComplication()).isNull()

        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication),
            UserStyleSchema(emptyList())
        )
        assertThat(complicationsManager.getBackgroundComplication()).isNull()

        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, backgroundComplication),
            UserStyleSchema(emptyList())
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
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            2
        )

        // This should get persisted.
        userStyleRepository.userStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to blueStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )

        val userStyleRepository2 = UserStyleRepository(
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting))
        )

        val testRenderer2 = TestRenderer(
            surfaceHolder,
            userStyleRepository2,
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        )
        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(emptyList(), userStyleRepository2),
            testRenderer2,
            userStyleRepository2,
            watchState,
            handler,
            null
        )

        // Trigger watch face creation.
        val engine2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
        engine2.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engine2, apiVersion = 2)
        sendImmutableProperties(engine2, false, false)

        assertThat(userStyleRepository2.userStyle.selectedOptions[colorStyleSetting]!!.id)
            .isEqualTo(
                blueStyleOption.id
            )
        assertThat(userStyleRepository2.userStyle.selectedOptions[watchHandStyleSetting]!!.id)
            .isEqualTo(
                gothicStyleOption.id
            )
    }

    @Test
    fun initWallpaperInteractiveWatchFaceInstanceWithUserStyle() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                SystemState(false, 0),
                UserStyle(
                    hashMapOf(
                        colorStyleSetting to blueStyleOption,
                        watchHandStyleSetting to gothicStyleOption
                    )
                ).toWireFormat(),
                null
            )
        )

        // The style option above should get applied during watch face creation.
        assertThat(userStyleRepository.userStyle.selectedOptions[colorStyleSetting]!!.id)
            .isEqualTo(
                blueStyleOption.id
            )
        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleSetting]!!.id)
            .isEqualTo(
                gothicStyleOption.id
            )
    }

    @Test
    fun initWallpaperInteractiveWatchFaceInstanceWithUserStyleThatDoesntMatchSchema() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                SystemState(false, 0),
                UserStyle(hashMapOf(watchHandStyleSetting to badStyleOption)).toWireFormat(),
                null
            )
        )

        assertThat(userStyleRepository.userStyle.selectedOptions[watchHandStyleSetting])
            .isEqualTo(watchHandStyleList.first())
    }

    @Test
    fun wear2ImmutablePropertiesSetCorrectly() {
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList()),
            2,
            true,
            false
        )

        assertTrue(watchState.hasLowBitAmbient)
        assertFalse(watchState.hasBurnInProtection)
    }

    @Test
    fun wear2ImmutablePropertiesSetCorrectly2() {
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList()),
            2,
            false,
            true
        )

        assertFalse(watchState.hasLowBitAmbient)
        assertTrue(watchState.hasBurnInProtection)
    }

    @Test
    fun wallpaperInteractiveWatchFaceImmutablePropertiesSetCorrectly() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    true,
                    false,
                    0,
                    0
                ),
                SystemState(false, 0),
                UserStyle(hashMapOf(watchHandStyleSetting to badStyleOption)).toWireFormat(),
                null
            )
        )

        assertTrue(watchState.hasLowBitAmbient)
        assertFalse(watchState.hasBurnInProtection)
    }

    @Test
    fun onCreate_calls_setActiveComplications_withCorrectIDs() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            UserStyleSchema(emptyList())
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
            UserStyleSchema(emptyList())
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
            UserStyleSchema(emptyList())
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
            UserStyleSchema(emptyList())
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
            UserStyleSchema(emptyList()),
            4
        )

        leftComplication.complicationBounds = ComplicationBounds(RectF(0.3f, 0.3f, 0.5f, 0.5f))
        rightComplication.complicationBounds = ComplicationBounds(RectF(0.7f, 0.75f, 0.9f, 0.95f))

        val complicationDetails = engineWrapper.getComplicationState()
        assertThat(complicationDetails[0].id).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDetails[0].complicationState.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[0].complicationState.bounds).isEqualTo(
            Rect(30, 30, 50, 50)
        )

        assertThat(complicationDetails[1].id).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDetails[1].complicationState.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[1].complicationState.bounds).isEqualTo(
            Rect(70, 75, 90, 95)
        )

        // Despite disabling the background complication we should still get a
        // ContentDescriptionLabel for the main clock element.
        val contentDescriptionLabels =
            watchFaceImpl.complicationsManager.getContentDescriptionLabels()
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
    fun getOptionForIdentifier_ListViewStyleSetting() {
        // Check the correct Options are returned for known option names.
        assertThat(colorStyleSetting.getOptionForId(redStyleOption.id)).isEqualTo(
            redStyleOption
        )
        assertThat(colorStyleSetting.getOptionForId(greenStyleOption.id)).isEqualTo(
            greenStyleOption
        )
        assertThat(colorStyleSetting.getOptionForId(blueStyleOption.id)).isEqualTo(
            blueStyleOption
        )

        // For unknown option names the first element in the list should be returned.
        assertThat(colorStyleSetting.getOptionForId("unknown")).isEqualTo(colorStyleList.first())
    }

    @Test
    fun centerX_and_centerY_containUpToDateValues() {
        initEngine(WatchFaceType.ANALOG, emptyList(), UserStyleSchema(emptyList()))

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
            UserStyleRepository(UserStyleSchema(emptyList()))
        var testRenderer = TestRenderer(
            surfaceHolder,
            userStyleRepository,
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        )
        val service = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(
                listOf(leftComplication, rightComplication, backgroundComplication),
                userStyleRepository
            ),
            testRenderer,
            UserStyleRepository(UserStyleSchema(emptyList())),
            watchState,
            handler,
            null
        )
        engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)
        `when`(surfaceHolder.surfaceFrame).thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)

        sendRequestStyle()

        // Trigger watch face creation.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion = 2)
        sendImmutableProperties(engineWrapper, false, false)
        watchFaceImpl = engineWrapper.watchFaceImpl

        val argument = ArgumentCaptor.forClass(WatchFaceStyle::class.java)
        verify(iWatchFaceService).setStyle(argument.capture())
        assertThat(argument.value.acceptsTapEvents).isEqualTo(true)
    }

    @Test
    fun defaultProvidersWithFallbacks_newApi() {
        val provider1 = ComponentName("com.app1", "com.app1.App1")
        val provider2 = ComponentName("com.app2", "com.app2.App2")
        val complication = Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawable(complicationDrawableLeft, watchState.asWatchState()),
            emptyList(),
            DefaultComplicationProviderPolicy(
                provider1,
                provider2,
                SystemProviders.SUNRISE_SUNSET
            ),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()
        initEngine(WatchFaceType.ANALOG, listOf(complication), UserStyleSchema(emptyList()))

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
        val complication = Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            CanvasComplicationDrawable(complicationDrawableLeft, watchState.asWatchState()),
            emptyList(),
            DefaultComplicationProviderPolicy(
                provider1,
                provider2,
                SystemProviders.SUNRISE_SUNSET
            ),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()
        initEngine(
            WatchFaceType.ANALOG,
            listOf(complication),
            UserStyleSchema(emptyList()),
            apiVersion = 0
        )

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
        val instanceParams = WallpaperInteractiveWatchFaceInstanceParams(
            "interactiveInstanceId",
            DeviceConfig(
                false,
                false,
                1000,
                2000,
            ),
            SystemState(false, 0),
            UserStyle(
                hashMapOf(
                    colorStyleSetting to blueStyleOption,
                    watchHandStyleSetting to gothicStyleOption
                )
            ).toWireFormat(),
            null
        )

        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            instanceParams
        )

        assertThat(watchFaceImpl.previewReferenceTimeMillis).isEqualTo(1000)
    }

    @Test
    fun previewReferenceTimeMillisDigital() {
        val instanceParams = WallpaperInteractiveWatchFaceInstanceParams(
            "interactiveInstanceId",
            DeviceConfig(
                false,
                false,
                1000,
                2000,
            ),
            SystemState(false, 0),
            UserStyle(
                hashMapOf(
                    colorStyleSetting to blueStyleOption,
                    watchHandStyleSetting to gothicStyleOption
                )
            ).toWireFormat(),
            null
        )

        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.DIGITAL,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            instanceParams
        )

        assertThat(watchFaceImpl.previewReferenceTimeMillis).isEqualTo(2000)
    }

    @Test
    fun getComplicationDetails() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            UserStyleSchema(emptyList()),
            apiVersion = 4
        )

        val complicationDetails = engineWrapper.getComplicationState()
        assertThat(complicationDetails[0].id).isEqualTo(LEFT_COMPLICATION_ID)
        assertThat(complicationDetails[0].complicationState.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[0].complicationState.bounds).isEqualTo(
            Rect(20, 40, 40, 60)
        )
        assertThat(complicationDetails[0].complicationState.supportedTypes).isEqualTo(
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            )
        )

        assertThat(complicationDetails[1].id).isEqualTo(RIGHT_COMPLICATION_ID)
        assertThat(complicationDetails[1].complicationState.boundsType).isEqualTo(
            ComplicationBoundsType.ROUND_RECT
        )
        assertThat(complicationDetails[1].complicationState.bounds).isEqualTo(
            Rect(60, 40, 80, 60)
        )
        assertThat(complicationDetails[1].complicationState.supportedTypes).isEqualTo(
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE
            )
        )

        assertThat(complicationDetails[2].id).isEqualTo(BACKGROUND_COMPLICATION_ID)
        assertThat(complicationDetails[2].complicationState.boundsType).isEqualTo(
            ComplicationBoundsType.BACKGROUND
        )
        assertThat(complicationDetails[2].complicationState.bounds).isEqualTo(
            Rect(0, 0, 100, 100)
        )
        assertThat(complicationDetails[2].complicationState.supportedTypes).isEqualTo(
            intArrayOf(ComplicationData.TYPE_LARGE_IMAGE)
        )
    }

    @Test
    fun shouldAnimateOverrideControlsEnteringAmbientMode() {
        var userStyleRepository = UserStyleRepository(UserStyleSchema(emptyList()))
        var testRenderer = object : TestRenderer(
            surfaceHolder,
            userStyleRepository,
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        ) {
            var animate = true
            override fun shouldAnimate() = animate
        }
        val service = TestWatchFaceService(
            WatchFaceType.ANALOG,
            ComplicationsManager(emptyList(), userStyleRepository),
            testRenderer,
            UserStyleRepository(UserStyleSchema(emptyList())),
            watchState,
            handler,
            null
        )

        engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)
        `when`(surfaceHolder.surfaceFrame).thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)

        // Trigger watch face creation.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion = 2)
        sendImmutableProperties(engineWrapper, false, false)
        watchFaceImpl = engineWrapper.watchFaceImpl

        // Enter ambient mode.
        watchState.isAmbient.value = true
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(testRenderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        // Simulate enter ambient animation finishing.
        testRenderer.animate = false
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(testRenderer.renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)
    }

    @Test
    fun complicationsUserStyleSettingSelectionAppliesChanges() {
        initEngine(
            WatchFaceType.DIGITAL,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(listOf(complicationsStyleSetting)),
            apiVersion = 4
        )

        // Select a new style which turns off both complications.
        val newStyleA = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleA[complicationsStyleSetting] = noComplicationsOption
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
        newStyleB[complicationsStyleSetting] = leftComplicationsOption
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
        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            "complications_style_setting",
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
            UserStyleSchema(listOf(complicationsStyleSetting)),
            apiVersion = 4
        )

        assertTrue(leftComplication.enabled)
        assertTrue(rightComplication.enabled)

        // Select left complication only.
        val newStyleA = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleA[complicationsStyleSetting] = leftOnlyComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleA)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertFalse(rightComplication.enabled)

        // Select right complication only.
        val newStyleB = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleB[complicationsStyleSetting] = rightOnlyComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleB)

        runPostedTasksFor(0)

        assertFalse(leftComplication.enabled)
        assertTrue(rightComplication.enabled)

        // Select both complications.
        val newStyleC = HashMap(userStyleRepository.userStyle.selectedOptions)
        newStyleC[complicationsStyleSetting] = bothComplicationsOption
        userStyleRepository.userStyle = UserStyle(newStyleC)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertTrue(rightComplication.enabled)
    }

    @Test
    fun observeComplicationData() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                SystemState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                null
            )
        )

        lateinit var leftComplicationData: ComplicationData
        lateinit var rightComplicationData: ComplicationData

        leftComplication.complicationData.addObserver {
            leftComplicationData = it.asWireComplicationData()
        }

        rightComplication.complicationData.addObserver {
            rightComplicationData = it.asWireComplicationData()
        }

        interactiveWatchFaceInstanceWCS.updateComplicationData(
            listOf(
                IdAndComplicationDataWireFormat(
                    LEFT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText("TYPE_LONG_TEXT")).build()
                ),
                IdAndComplicationDataWireFormat(
                    RIGHT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("TYPE_SHORT_TEXT")).build()
                )
            )
        )

        assertThat(leftComplicationData.type).isEqualTo(ComplicationData.TYPE_LONG_TEXT)
        assertThat(leftComplicationData.longText?.getTextAt(context.resources, 0))
            .isEqualTo("TYPE_LONG_TEXT")
        assertThat(rightComplicationData.type).isEqualTo(ComplicationData.TYPE_SHORT_TEXT)
        assertThat(rightComplicationData.shortText?.getTextAt(context.resources, 0))
            .isEqualTo("TYPE_SHORT_TEXT")
    }

    @Test
    fun complication_isActiveAt() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            listOf(leftComplication),
            UserStyleSchema(emptyList()),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                SystemState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                null
            )
        )

        interactiveWatchFaceInstanceWCS.updateComplicationData(
            listOf(
                IdAndComplicationDataWireFormat(
                    LEFT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("TYPE_SHORT_TEXT"))
                        .build()
                )
            )
        )

        // Initially the complication should be active.
        assertThat(leftComplication.isActiveAt(0)).isTrue()

        // Send empty data.
        interactiveWatchFaceInstanceWCS.updateComplicationData(
            listOf(
                IdAndComplicationDataWireFormat(
                    LEFT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_EMPTY).build()
                )
            )
        )

        assertThat(leftComplication.isActiveAt(0)).isFalse()

        // Send a complication that is active for a time range.
        interactiveWatchFaceInstanceWCS.updateComplicationData(
            listOf(
                IdAndComplicationDataWireFormat(
                    LEFT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("TYPE_SHORT_TEXT"))
                        .setStartDateTimeMillis(1000000)
                        .setEndDateTimeMillis(2000000)
                        .build()
                )
            )
        )

        assertThat(leftComplication.isActiveAt(999999)).isFalse()
        assertThat(leftComplication.isActiveAt(1000000)).isTrue()
        assertThat(leftComplication.isActiveAt(2000000)).isTrue()
        assertThat(leftComplication.isActiveAt(2000001)).isFalse()
    }

    @Test
    fun invalidateRendererBeforeFullInit() {
        renderer = TestRenderer(
            surfaceHolder,
            UserStyleRepository(UserStyleSchema(emptyList())),
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        )

        // This should not throw an exception.
        renderer.invalidate()
    }
}
