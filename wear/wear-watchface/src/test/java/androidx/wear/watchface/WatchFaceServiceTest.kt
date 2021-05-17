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
import android.graphics.Insets
import android.graphics.Rect
import android.graphics.RectF
import android.os.BatteryManager
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
import android.view.WindowInsets
import androidx.annotation.Px
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IPendingInteractiveWatchFace
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationsOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
private const val EDGE_COMPLICATION_ID = 1002
private const val BACKGROUND_COMPLICATION_ID = 1111
private const val NO_COMPLICATIONS = "NO_COMPLICATIONS"
private const val LEFT_COMPLICATION = "LEFT_COMPLICATION"
private const val RIGHT_COMPLICATION = "RIGHT_COMPLICATION"
private const val LEFT_AND_RIGHT_COMPLICATIONS = "LEFT_AND_RIGHT_COMPLICATIONS"

@Config(manifest = Config.NONE)
@RunWith(WatchFaceTestRunner::class)
public class WatchFaceServiceTest {

    private val handler = mock<Handler>()
    private val iWatchFaceService = mock<IWatchFaceService>()
    private val surfaceHolder = mock<SurfaceHolder>()
    private val tapListener = mock<WatchFace.TapListener>()
    private val watchState = MutableWatchState()

    init {
        `when`(surfaceHolder.surfaceFrame).thenReturn(ONE_HUNDRED_BY_ONE_HUNDRED_RECT)
    }

    private companion object {
        val ONE_HUNDRED_BY_ONE_HUNDRED_RECT = Rect(0, 0, 100, 100)
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val complicationDrawableLeft = ComplicationDrawable(context)
    private val complicationDrawableRight = ComplicationDrawable(context)
    private val complicationDrawableEdge = ComplicationDrawable(context)
    private val complicationDrawableBackground = ComplicationDrawable(context)

    private val redStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("red_style"), "Red", icon = null)

    private val greenStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("green_style"), "Green", icon = null)

    private val blueStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("bluestyle"), "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = ListUserStyleSetting(
        UserStyleSetting.Id("color_style_setting"),
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(WatchFaceLayer.BASE)
    )

    private val classicStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("classic_style"), "Classic", icon = null)

    private val modernStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("modern_style"), "Modern", icon = null)

    private val gothicStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("gothic_style"), "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = ListUserStyleSetting(
        UserStyleSetting.Id("hand_style_setting"),
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
    )

    private val badStyleOption =
        ListUserStyleSetting.ListOption(Option.Id("bad_option"), "Bad", icon = null)

    private val leftComplication =
        Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            { watchState, listener ->
                CanvasComplicationDrawable(
                    complicationDrawableLeft,
                    watchState,
                    listener
                ).apply {
                    loadData(createComplicationData(), false)
                }
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_SUNRISE_SUNSET),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val rightComplication =
        Complication.createRoundRectComplicationBuilder(
            RIGHT_COMPLICATION_ID,
            { watchState, listener ->
                CanvasComplicationDrawable(
                    complicationDrawableRight,
                    watchState,
                    listener
                ).apply {
                    loadData(createComplicationData(), false)
                }
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
            ComplicationBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val edgeComplicationHitTester = mock<ComplicationTapFilter>()
    private val edgeComplication =
        Complication.createEdgeComplicationBuilder(
            EDGE_COMPLICATION_ID,
            { watchState, listener ->
                CanvasComplicationDrawable(
                    complicationDrawableEdge,
                    watchState,
                    listener
                ).apply {
                    loadData(createComplicationData(), false)
                }
            },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
            ComplicationBounds(RectF(0.0f, 0.4f, 0.4f, 0.6f)),
            edgeComplicationHitTester
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val backgroundComplication =
        Complication.createBackgroundComplicationBuilder(
            BACKGROUND_COMPLICATION_ID,
            { watchState, listener ->
                CanvasComplicationDrawable(
                    complicationDrawableBackground,
                    watchState,
                    listener
                ).apply {
                    loadData(createComplicationData(), false)
                }
            },
            listOf(
                ComplicationType.PHOTO_IMAGE
            ),
            DefaultComplicationProviderPolicy()
        ).setDefaultProviderType(ComplicationType.PHOTO_IMAGE)
            .build()

    private val leftAndRightComplicationsOption = ComplicationsOption(
        Option.Id(LEFT_AND_RIGHT_COMPLICATIONS),
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
        Option.Id(NO_COMPLICATIONS),
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
        Option.Id(LEFT_COMPLICATION),
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
        Option.Id(RIGHT_COMPLICATION),
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
        UserStyleSetting.Id("complications_style_setting"),
        "AllComplications",
        "Number and position",
        icon = null,
        complicationConfig = listOf(
            leftAndRightComplicationsOption,
            noComplicationsOption,
            leftComplicationsOption,
            rightComplicationsOption
        ),
        affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS)
    )

    private lateinit var renderer: TestRenderer
    private lateinit var complicationsManager: ComplicationsManager
    private lateinit var currentUserStyleRepository: CurrentUserStyleRepository
    private lateinit var watchFaceImpl: WatchFaceImpl
    private lateinit var testWatchFaceService: TestWatchFaceService
    private lateinit var engineWrapper: WatchFaceService.EngineWrapper
    private lateinit var interactiveWatchFaceInstance: IInteractiveWatchFace

    private class Task(val runTimeMillis: Long, val runnable: Runnable) : Comparable<Task> {
        override fun compareTo(other: Task) = runTimeMillis.compareTo(other.runTimeMillis)
    }

    private var looperTimeMillis = 0L
    private val pendingTasks = PriorityQueue<Task>()

    /**
     * Runs any pending DispatchedContinuation tasks.  Care should be taken to ensure there's
     * not other tasks in the queue ahead of these or they won't get run.
     */
    private fun runPendingPostedDispatchedContinuationTasks() {
        while (pendingTasks.isNotEmpty() &&
            pendingTasks.peek()!!.runTimeMillis <= looperTimeMillis &&
            pendingTasks.peek()!!.runnable.toString().startsWith("DispatchedContinuation")
        ) {
            pendingTasks.remove().runnable.run()
        }
    }

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
        testWatchFaceService = TestWatchFaceService(
            watchFaceType,
            complications,
            { _, currentUserStyleRepository, watchState ->
                renderer = TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
                renderer
            },
            userStyleSchema,
            watchState,
            handler,
            tapListener,
            true,
            null
        )
        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)

        // Trigger watch face creation by sending the SurfceHolder, setting the binder and the
        // immutable properties.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, apiVersion)
        sendImmutableProperties(engineWrapper, hasLowBitAmbient, hasBurnInProtection)

        watchFaceImpl = engineWrapper.watchFaceImpl
        currentUserStyleRepository = watchFaceImpl.currentUserStyleRepository
        complicationsManager = watchFaceImpl.complicationsManager

        testWatchFaceService.setIsVisible(true)
    }

    private fun initWallpaperInteractiveWatchFaceInstance(
        @WatchFaceType watchFaceType: Int,
        complications: List<Complication>,
        userStyleSchema: UserStyleSchema,
        wallpaperInteractiveWatchFaceInstanceParams: WallpaperInteractiveWatchFaceInstanceParams
    ) {
        testWatchFaceService = TestWatchFaceService(
            watchFaceType,
            complications,
            { _, currentUserStyleRepository, watchState ->
                renderer = TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
                renderer
            },
            userStyleSchema,
            watchState,
            handler,
            null,
            false,
            null
        )

        InteractiveInstanceManager
            .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                    wallpaperInteractiveWatchFaceInstanceParams,
                    object : IPendingInteractiveWatchFace.Stub() {
                        override fun getApiVersion() =
                            IPendingInteractiveWatchFace.API_VERSION

                        override fun onInteractiveWatchFaceCreated(
                            iInteractiveWatchFace: IInteractiveWatchFace
                        ) {
                            interactiveWatchFaceInstance = iInteractiveWatchFace
                        }

                        override fun onInteractiveWatchFaceCrashed(exception: CrashInfoParcel?) {
                            fail("WatchFace crashed: $exception")
                        }
                    }
                )
            )

        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)

        // [WatchFaceService.createWatchFace] is a suspend function backed by a handler coroutine
        // dispatcher. We need to execute posted tasks inorder for the engine to get created.
        runPendingPostedDispatchedContinuationTasks()

        // The [SurfaceHolder] must be sent before binding.
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        watchFaceImpl = engineWrapper.watchFaceImpl
        currentUserStyleRepository = watchFaceImpl.currentUserStyleRepository
        complicationsManager = watchFaceImpl.complicationsManager
        testWatchFaceService.setIsVisible(true)
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
        engine.wslFlow.onPropertiesChanged(
            Bundle().apply {
                putBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT, hasLowBitAmbient)
                putBoolean(Constants.PROPERTY_BURN_IN_PROTECTION, hasBurnInProtection)
            }
        )

        // [WatchFaceService.createWatchFace] is a suspend function backed by a handler coroutine
        // dispatcher. We need to execute posted tasks for the engine to get created. We assume this
        // is the last call made before the test needs to do something with the watch face, so we
        // force execution here.
        runPendingPostedDispatchedContinuationTasks()
    }

    private fun sendRequestStyle() {
        engineWrapper.onCommand(Constants.COMMAND_REQUEST_STYLE, 0, 0, 0, null, false)
    }

    private fun setComplicationViaWallpaperCommand(
        complicationId: Int,
        complicationData: ComplicationData
    ) {
        engineWrapper.onCommand(
            Constants.COMMAND_COMPLICATION_DATA,
            0,
            0,
            0,
            Bundle().apply {
                putInt(Constants.EXTRA_COMPLICATION_ID, complicationId)
                putParcelable(Constants.EXTRA_COMPLICATION_DATA, complicationData)
            },
            false
        )
    }

    @Before
    public fun setUp() {
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
    public fun validate() {
        if (this::interactiveWatchFaceInstance.isInitialized) {
            interactiveWatchFaceInstance.release()
        }

        if (this::engineWrapper.isInitialized && !engineWrapper.destroyed) {
            engineWrapper.onDestroy()
        }

        validateMockitoUsage()
    }

    @Test
    public fun maybeUpdateDrawMode_setsCorrectDrawMode() {
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

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_ALL
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_PRIORITY
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.MUTE)

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        watchFaceImpl.maybeUpdateDrawMode()
        assertThat(renderer.renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)

        watchState.interruptionFilter.value = NotificationManager.INTERRUPTION_FILTER_ALARMS
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
    public fun onDraw_calendar_setFromSystemTime() {
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
    public fun onDraw_calendar_affectedCorrectly_with2xMockTime() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L

        watchFaceImpl.broadcastsReceiver!!.mockTimeReceiver.onReceive(
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
    public fun onDraw_calendar_affectedCorrectly_withMockTimeWrapping() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        watchState.isAmbient.value = false
        testWatchFaceService.mockSystemTimeMillis = 1000L

        watchFaceImpl.broadcastsReceiver!!.mockTimeReceiver.onReceive(
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
        watchFaceImpl.onTapCommand(TapType.DOWN, x, y)
        watchFaceImpl.onTapCommand(TapType.UP, x, y)
    }

    private fun tapCancelAt(x: Int, y: Int) {
        watchFaceImpl.onTapCommand(TapType.DOWN, x, y)
        watchFaceImpl.onTapCommand(TapType.CANCEL, x, y)
    }

    @Test
    public fun singleTaps_correctlyDetected_and_highlightComplications() {
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
        assertThat(testWatchFaceService.tappedComplicationIds)
            .isEqualTo(listOf(LEFT_COMPLICATION_ID))

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap right complication.
        testWatchFaceService.reset()
        tapAt(70, 50)
        assertThat(complicationDrawableRight.isHighlighted).isTrue()
        assertThat(testWatchFaceService.tappedComplicationIds)
            .isEqualTo(listOf(RIGHT_COMPLICATION_ID))

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()

        // Tap on blank space.
        testWatchFaceService.reset()
        tapAt(1, 1)
        assertThat(testWatchFaceService.tappedComplicationIds).isEmpty()

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(testWatchFaceService.tappedComplicationIds).isEmpty()
    }

    @Test
    public fun singleTaps_onDifferentComplications() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Rapidly tap left then right complication.
        tapAt(30, 50)
        tapAt(70, 50)

        // Both complications get temporarily highlighted.
        assertThat(complicationDrawableLeft.isHighlighted).isTrue()
        assertThat(complicationDrawableRight.isHighlighted).isTrue()

        // And the highlight goes away after a delay.
        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableLeft.isHighlighted).isFalse()
        assertThat(complicationDrawableRight.isHighlighted).isFalse()

        // Taps are registered on both complications.
        assertThat(testWatchFaceService.tappedComplicationIds)
            .isEqualTo(listOf(LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID))
    }

    @Test
    public fun tapCancel_after_tapDown_CancelsTap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        testWatchFaceService.reset()
        // Tap/Cancel left complication
        tapCancelAt(30, 50)
        assertThat(testWatchFaceService.tappedComplicationIds).isEmpty()
    }

    @Test
    public fun edgeComplication_tap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(edgeComplication),
            UserStyleSchema(emptyList()),
            tapListener = tapListener
        )

        assertThat(complicationDrawableEdge.isHighlighted).isFalse()

        `when`(
            edgeComplicationHitTester.hitTest(
                edgeComplication,
                ONE_HUNDRED_BY_ONE_HUNDRED_RECT,
                0,
                50
            )
        ).thenReturn(true)

        // Tap the edge complication.
        tapAt(0, 50)
        assertThat(complicationDrawableEdge.isHighlighted).isTrue()
        assertThat(testWatchFaceService.tappedComplicationIds)
            .isEqualTo(listOf(EDGE_COMPLICATION_ID))

        runPostedTasksFor(WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS)
        assertThat(complicationDrawableEdge.isHighlighted).isFalse()
    }

    @Test
    public fun tapListener_tap() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            tapListener = tapListener
        )

        // Tap on nothing.
        tapAt(1, 1)

        verify(tapListener).onTap(TapType.DOWN, 1, 1)
        verify(tapListener).onTap(TapType.UP, 1, 1)
    }

    @Test
    public fun tapListener_tapComplication() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            tapListener = tapListener
        )

        // Tap right complication.
        tapAt(70, 50)

        verify(tapListener, times(0)).onTap(TapType.DOWN, 70, 50)
        verify(tapListener, times(0)).onTap(TapType.UP, 70, 50)
    }

    @Test
    public fun interactiveFrameRate_reducedWhenBatteryLow() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            INTERACTIVE_UPDATE_RATE_MS
        )

        // The delay should change when battery is low.
        watchFaceImpl.broadcastsReceiver!!.actionBatteryLowReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_LOW)
        )
        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            WatchFaceImpl.MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS
        )

        // And go back to normal when battery is OK.
        watchFaceImpl.broadcastsReceiver!!.actionBatteryOkayReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_OKAY)
        )
        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            INTERACTIVE_UPDATE_RATE_MS
        )
    }

    @Test
    public fun interactiveFrameRate_restoreWhenPowerConnectedAfterBatteryLow() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            INTERACTIVE_UPDATE_RATE_MS
        )

        // The delay should change when battery is low.
        watchFaceImpl.broadcastsReceiver!!.actionBatteryLowReceiver.onReceive(
            context,
            Intent(Intent.ACTION_BATTERY_LOW)
        )
        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            WatchFaceImpl.MAX_LOW_POWER_INTERACTIVE_UPDATE_RATE_MS
        )

        // And go back to normal when power is connected.
        watchFaceImpl.broadcastsReceiver!!.actionPowerConnectedReceiver.onReceive(
            context,
            Intent(Intent.ACTION_POWER_CONNECTED)
        )
        assertThat(watchFaceImpl.computeDelayTillNextFrame(0, 0)).isEqualTo(
            INTERACTIVE_UPDATE_RATE_MS
        )
    }

    @Test
    public fun computeDelayTillNextFrame_accountsForSlowDraw() {
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
    public fun computeDelayTillNextFrame_dropsFramesForVerySlowDraw() {
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
    public fun computeDelayTillNextFrame_perservesPhaseForVerySlowDraw() {
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
    public fun computeDelayTillNextFrame_beginFrameTimeInTheFuture() {
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
    public fun getComplicationIdAt_returnsCorrectComplications() {
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
    public fun getBackgroundComplicationId_returnsNull() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )
        // Flush pending tasks posted as a result of initEngine.
        runPendingPostedDispatchedContinuationTasks()
        assertThat(complicationsManager.getBackgroundComplication()).isNull()
        engineWrapper.onDestroy()
    }

    @Test
    public fun getBackgroundComplicationId_returnsCorrectId() {
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
    public fun getStoredUserStyleNotSupported_userStyle_isPersisted() {
        // The style should get persisted in a file because this test is set up using the legacy
        // Wear 2.0 APIs.
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            2
        )

        // This should get persisted.
        currentUserStyleRepository.userStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to blueStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )
        engineWrapper.onDestroy()

        // Flush pending tasks posted as a result of initEngine.
        runPostedTasksFor(0)

        val service2 = TestWatchFaceService(
            WatchFaceType.ANALOG,
            emptyList(),
            { _, currentUserStyleRepository, watchState ->
                TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
            },
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            watchState,
            handler,
            null,
            true,
            null
        )

        // Trigger watch face creation.
        val engine2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
        engine2.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engine2, apiVersion = 2)
        sendImmutableProperties(engine2, false, false)

        val userStyleRepository2 = engine2.watchFaceImpl.currentUserStyleRepository
        assertThat(userStyleRepository2.userStyle.selectedOptions[colorStyleSetting]!!.id)
            .isEqualTo(
                blueStyleOption.id
            )
        assertThat(userStyleRepository2.userStyle.selectedOptions[watchHandStyleSetting]!!.id)
            .isEqualTo(
                gothicStyleOption.id
            )
    }

    @SdkSuppress(maxSdkVersion = 29)
    @Test
    public fun onApplyWindowInsetsBeforeR_setsChinHeight() {
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList())
        )
        // Initially the chin size is set to zero.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(0)
        // When window insets are delivered to the watch face.
        engineWrapper.onApplyWindowInsets(getChinWindowInsetsApi25(chinHeight = 12))
        // Then the chin size is updated.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(12)
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    public fun onApplyWindowInsetsRAndAbove_setsChinHeight() {
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList())
        )
        // Initially the chin size is set to zero.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(0)
        // When window insets are delivered to the watch face.
        engineWrapper.onApplyWindowInsets(getChinWindowInsetsApi30(chinHeight = 12))
        // Then the chin size is updated.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(12)
    }

    @Test
    public fun onApplyWindowInsetsBeforeR_multipleCallsIgnored() {
        initEngine(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList())
        )
        // Initially the chin size is set to zero.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(0)
        // When window insets are delivered to the watch face.
        engineWrapper.onApplyWindowInsets(getChinWindowInsetsApi25(chinHeight = 12))
        // Then the chin size is updated.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(12)
        // When the same window insets are delivered to the watch face again.
        engineWrapper.onApplyWindowInsets(getChinWindowInsetsApi25(chinHeight = 12))
        // Nothing happens.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(12)
        // When different window insets are delivered to the watch face again.
        engineWrapper.onApplyWindowInsets(getChinWindowInsetsApi25(chinHeight = 24))
        // Nothing happens and the size is unchanged.
        assertThat(engineWrapper.mutableWatchState.chinHeight).isEqualTo(12)
    }

    @Test
    public fun initWallpaperInteractiveWatchFaceInstanceWithUserStyle() {
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
                WatchUiState(false, 0),
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
        assertThat(currentUserStyleRepository.userStyle.selectedOptions[colorStyleSetting]!!.id)
            .isEqualTo(
                blueStyleOption.id
            )
        assertThat(currentUserStyleRepository.userStyle.selectedOptions[watchHandStyleSetting]!!.id)
            .isEqualTo(
                gothicStyleOption.id
            )
    }

    @Test
    public fun initWallpaperInteractiveWatchFaceInstanceWithUserStyleThatDoesntMatchSchema() {
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
                WatchUiState(false, 0),
                UserStyle(hashMapOf(watchHandStyleSetting to badStyleOption)).toWireFormat(),
                null
            )
        )

        assertThat(currentUserStyleRepository.userStyle.selectedOptions[watchHandStyleSetting])
            .isEqualTo(watchHandStyleList.first())
    }

    @Test
    public fun wear2ImmutablePropertiesSetCorrectly() {
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
    public fun wear2ImmutablePropertiesSetCorrectly2() {
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
    public fun wallpaperInteractiveWatchFaceImmutablePropertiesSetCorrectly() {
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
                WatchUiState(false, 0),
                UserStyle(hashMapOf(watchHandStyleSetting to badStyleOption)).toWireFormat(),
                null
            )
        )

        assertTrue(watchState.hasLowBitAmbient)
        assertFalse(watchState.hasBurnInProtection)
    }

    @Test
    public fun onCreate_calls_setActiveComplications_withCorrectIDs() {
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
    public fun onCreate_calls_setContentDescriptionLabels_withCorrectArgs() {
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
    public fun setActiveComplications_afterDisablingSeveralComplications() {
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
    public fun initial_setContentDescriptionLabels_afterDisablingSeveralComplications() {
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
    public fun moveComplications() {
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
        engineWrapper.updateContentDescriptionLabels()
        val contentDescriptionLabels = engineWrapper.contentDescriptionLabels
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
    public fun styleChangesAccessibilityTraversalIndex() {
        val leftAndRightComplicationsOptionIndexReversed = ComplicationsOption(
            Option.Id(LEFT_AND_RIGHT_COMPLICATIONS),
            "Both",
            null,
            listOf(
                ComplicationOverlay.Builder(LEFT_COMPLICATION_ID)
                    .setEnabled(true).setAccessibilityTraversalIndex(RIGHT_COMPLICATION_ID).build(),
                ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID)
                    .setEnabled(true).setAccessibilityTraversalIndex(LEFT_COMPLICATION_ID).build()
            )
        )

        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting"),
            "AllComplications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                leftAndRightComplicationsOption,
                leftAndRightComplicationsOptionIndexReversed
            ),
            affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS)
        )

        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(listOf(complicationsStyleSetting)),
            4
        )

        // Despite disabling the background complication we should still get a
        // ContentDescriptionLabel for the main clock element.
        engineWrapper.updateContentDescriptionLabels()
        val contentDescriptionLabels = engineWrapper.contentDescriptionLabels
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
                20,
                40,
                40,
                60
            )
        ) // Left complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(
            Rect(
                60,
                40,
                80,
                60
            )
        ) // Right complication.

        // Change the style
        engineWrapper.watchFaceImpl.currentUserStyleRepository.userStyle = UserStyle(
            hashMapOf(complicationsStyleSetting to leftAndRightComplicationsOptionIndexReversed)
        )
        runPostedTasksFor(0)

        val contentDescriptionLabels2 = engineWrapper.contentDescriptionLabels
        assertThat(contentDescriptionLabels2.size).isEqualTo(3)
        assertThat(contentDescriptionLabels2[0].bounds).isEqualTo(
            Rect(
                25,
                25,
                75,
                75
            )
        ) // Clock element.
        assertThat(contentDescriptionLabels2[1].bounds).isEqualTo(
            Rect(
                60,
                40,
                80,
                60
            )
        ) // Right complication.
        assertThat(contentDescriptionLabels2[2].bounds).isEqualTo(
            Rect(
                20,
                40,
                40,
                60
            )
        ) // Left complication.
    }

    @Test
    public fun getOptionForIdentifier_ListViewStyleSetting() {
        // Check the correct Options are returned for known option names.
        assertThat(colorStyleSetting.getOptionForId(redStyleOption.id.value)).isEqualTo(
            redStyleOption
        )
        assertThat(colorStyleSetting.getOptionForId(greenStyleOption.id.value)).isEqualTo(
            greenStyleOption
        )
        assertThat(colorStyleSetting.getOptionForId(blueStyleOption.id.value)).isEqualTo(
            blueStyleOption
        )

        // For unknown option names the first element in the list should be returned.
        assertThat(colorStyleSetting.getOptionForId("unknown".encodeToByteArray()))
            .isEqualTo(colorStyleList.first())
    }

    @Test
    public fun centerX_and_centerY_containUpToDateValues() {
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
    public fun requestStyleBeforeSetBinder() {
        testWatchFaceService = TestWatchFaceService(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication, backgroundComplication),
            { _, currentUserStyleRepository, watchState ->
                TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
            },
            UserStyleSchema(emptyList()),
            watchState,
            handler,
            null,
            true,
            null
        )
        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
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
    public fun defaultProvidersWithFallbacks_newApi() {
        val provider1 = ComponentName("com.app1", "com.app1.App1")
        val provider2 = ComponentName("com.app2", "com.app2.App2")
        val complication = Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            { watchState, listener ->
                CanvasComplicationDrawable(complicationDrawableLeft, watchState, listener)
            },
            emptyList(),
            DefaultComplicationProviderPolicy(
                provider1,
                provider2,
                SystemProviders.PROVIDER_SUNRISE_SUNSET
            ),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()
        initEngine(WatchFaceType.ANALOG, listOf(complication), UserStyleSchema(emptyList()))

        runPostedTasksFor(0)

        verify(iWatchFaceService).setDefaultComplicationProviderWithFallbacks(
            LEFT_COMPLICATION_ID,
            listOf(provider1, provider2),
            SystemProviders.PROVIDER_SUNRISE_SUNSET,
            ComplicationData.TYPE_SHORT_TEXT
        )
    }

    @Test
    public fun defaultProvidersWithFallbacks_oldApi() {
        val provider1 = ComponentName("com.app1", "com.app1.App1")
        val provider2 = ComponentName("com.app2", "com.app2.App2")
        val complication = Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            { watchState, listener ->
                CanvasComplicationDrawable(complicationDrawableLeft, watchState, listener)
            },
            emptyList(),
            DefaultComplicationProviderPolicy(
                provider1,
                provider2,
                SystemProviders.PROVIDER_SUNRISE_SUNSET
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
            LEFT_COMPLICATION_ID,
            SystemProviders.PROVIDER_SUNRISE_SUNSET,
            ComplicationData.TYPE_SHORT_TEXT
        )
    }

    @Test
    public fun previewReferenceTimeMillisAnalog() {
        val instanceParams = WallpaperInteractiveWatchFaceInstanceParams(
            "interactiveInstanceId",
            DeviceConfig(
                false,
                false,
                1000,
                2000,
            ),
            WatchUiState(false, 0),
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
    public fun previewReferenceTimeMillisDigital() {
        val instanceParams = WallpaperInteractiveWatchFaceInstanceParams(
            "interactiveInstanceId",
            DeviceConfig(
                false,
                false,
                1000,
                2000,
            ),
            WatchUiState(false, 0),
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
    public fun getComplicationDetails() {
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
    public fun shouldAnimateOverrideControlsEnteringAmbientMode() {
        lateinit var testRenderer: TestRendererWithShouldAnimate
        testWatchFaceService = TestWatchFaceService(
            WatchFaceType.ANALOG,
            emptyList(),
            { _, currentUserStyleRepository, watchState ->
                testRenderer = TestRendererWithShouldAnimate(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
                testRenderer
            },
            UserStyleSchema(emptyList()),
            watchState,
            handler,
            null,
            true,
            null
        )

        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
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
    public fun complicationsUserStyleSettingSelectionAppliesChanges() {
        initEngine(
            WatchFaceType.DIGITAL,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(listOf(complicationsStyleSetting)),
            apiVersion = 4
        )

        // Select a new style which turns off both complications.
        val newStyleA = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
        newStyleA[complicationsStyleSetting] = noComplicationsOption
        currentUserStyleRepository.userStyle = UserStyle(newStyleA)

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
        val newStyleB = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
        newStyleB[complicationsStyleSetting] = leftComplicationsOption
        currentUserStyleRepository.userStyle = UserStyle(newStyleB)

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
    public fun partialComplicationOverrides() {
        val bothComplicationsOption = ComplicationsOption(
            Option.Id(LEFT_AND_RIGHT_COMPLICATIONS),
            "Left And Right",
            null,
            // An empty list means use the initial config.
            emptyList()
        )
        val leftOnlyComplicationsOption = ComplicationsOption(
            Option.Id(LEFT_COMPLICATION),
            "Left",
            null,
            listOf(ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID).setEnabled(false).build())
        )
        val rightOnlyComplicationsOption = ComplicationsOption(
            Option.Id(RIGHT_COMPLICATION),
            "Right",
            null,
            listOf(ComplicationOverlay.Builder(LEFT_COMPLICATION_ID).setEnabled(false).build())
        )
        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting"),
            "AllComplications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                bothComplicationsOption,
                leftOnlyComplicationsOption,
                rightOnlyComplicationsOption
            ),
            affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS)
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
        val newStyleA = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
        newStyleA[complicationsStyleSetting] = leftOnlyComplicationsOption
        currentUserStyleRepository.userStyle = UserStyle(newStyleA)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertFalse(rightComplication.enabled)

        // Select right complication only.
        val newStyleB = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
        newStyleB[complicationsStyleSetting] = rightOnlyComplicationsOption
        currentUserStyleRepository.userStyle = UserStyle(newStyleB)

        runPostedTasksFor(0)

        assertFalse(leftComplication.enabled)
        assertTrue(rightComplication.enabled)

        // Select both complications.
        val newStyleC = HashMap(currentUserStyleRepository.userStyle.selectedOptions)
        newStyleC[complicationsStyleSetting] = bothComplicationsOption
        currentUserStyleRepository.userStyle = UserStyle(newStyleC)

        runPostedTasksFor(0)

        assertTrue(leftComplication.enabled)
        assertTrue(rightComplication.enabled)
    }

    @Test
    public fun partialComplicationOverrideAppliedToInitialStyle() {
        val bothComplicationsOption = ComplicationsOption(
            Option.Id(LEFT_AND_RIGHT_COMPLICATIONS),
            "Left And Right",
            null,
            // An empty list means use the initial config.
            emptyList()
        )
        val leftOnlyComplicationsOption = ComplicationsOption(
            Option.Id(LEFT_COMPLICATION),
            "Left",
            null,
            listOf(ComplicationOverlay.Builder(RIGHT_COMPLICATION_ID).setEnabled(false).build())
        )
        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting"),
            "AllComplications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                leftOnlyComplicationsOption, // The default value which should be applied.
                bothComplicationsOption,
            ),
            affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS)
        )

        initEngine(
            WatchFaceType.DIGITAL,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(listOf(complicationsStyleSetting)),
            apiVersion = 4
        )

        assertTrue(leftComplication.enabled)
        assertFalse(rightComplication.enabled)
    }

    public fun UserStyleManager_init_applies_ComplicationsUserStyleSetting() {
        val complicationId1 = 101
        val complicationId2 = 102

        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            UserStyleSetting.Id("ID"),
            "",
            "",
            icon = null,
            complicationConfig = listOf(
                ComplicationsOption(
                    Option.Id("one"),
                    "one",
                    null,
                    listOf(
                        ComplicationOverlay(
                            complicationId1,
                            enabled = true
                        ),
                    )
                ),
                ComplicationsOption(
                    Option.Id("two"),
                    "teo",
                    null,
                    listOf(
                        ComplicationOverlay(
                            complicationId2,
                            enabled = true
                        ),
                    )
                )
            ),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )

        val currentUserStyleRepository =
            CurrentUserStyleRepository(UserStyleSchema(listOf(complicationsStyleSetting)))

        val manager = ComplicationsManager(
            listOf(
                Complication.createRoundRectComplicationBuilder(
                    complicationId1,
                    { watchState, listener ->
                        CanvasComplicationDrawable(complicationDrawableLeft, watchState, listener)
                    },
                    listOf(
                        ComplicationType.RANGED_VALUE,
                    ),
                    DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
                    ComplicationBounds(RectF(0.2f, 0.7f, 0.4f, 0.9f))
                ).setDefaultProviderType(ComplicationType.RANGED_VALUE)
                    .setEnabled(false)
                    .build(),

                Complication.createRoundRectComplicationBuilder(
                    complicationId2,
                    { watchState, listener ->
                        CanvasComplicationDrawable(complicationDrawableRight, watchState, listener)
                    },
                    listOf(
                        ComplicationType.LONG_TEXT,
                    ),
                    DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
                    ComplicationBounds(RectF(0.2f, 0.7f, 0.4f, 0.9f))
                ).setDefaultProviderType(ComplicationType.LONG_TEXT)
                    .setEnabled(false)
                    .build()
            ),
            currentUserStyleRepository
        )

        // The init function of ComplicationsManager should enable complicationId1.
        assertThat(manager[complicationId1]!!.enabled).isTrue()
    }

    @Test
    public fun observeComplicationData() {
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
                WatchUiState(false, 0),
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

        interactiveWatchFaceInstance.updateComplicationData(
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
    public fun complication_isActiveAt() {
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
                WatchUiState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                null
            )
        )

        interactiveWatchFaceInstance.updateComplicationData(
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
        interactiveWatchFaceInstance.updateComplicationData(
            listOf(
                IdAndComplicationDataWireFormat(
                    LEFT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_EMPTY).build()
                )
            )
        )

        assertThat(leftComplication.isActiveAt(0)).isFalse()

        // Send a complication that is active for a time range.
        interactiveWatchFaceInstance.updateComplicationData(
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
    public fun updateInvalidCOmpliationIdDoesNotCrash() {
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
                WatchUiState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                null
            )
        )

        // Send a complication with an invalid id - this should get ignored.
        interactiveWatchFaceInstance.updateComplicationData(
            listOf(
                IdAndComplicationDataWireFormat(
                    RIGHT_COMPLICATION_ID,
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("TYPE_SHORT_TEXT"))
                        .build()
                )
            )
        )
    }

    @Test
    public fun invalidateRendererBeforeFullInit() {
        renderer = TestRenderer(
            surfaceHolder,
            CurrentUserStyleRepository(UserStyleSchema(emptyList())),
            watchState.asWatchState(),
            INTERACTIVE_UPDATE_RATE_MS
        )

        // This should not throw an exception.
        renderer.invalidate()
    }

    @Test
    public fun watchStateObservableWatchDataMembersHaveValues() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList()),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                null
            )
        )

        assertTrue(watchState.interruptionFilter.hasValue())
        assertTrue(watchState.isAmbient.hasValue())
        assertTrue(watchState.isBatteryLowAndNotCharging.hasValue())
        assertTrue(watchState.isVisible.hasValue())
    }

    @Test
    public fun setIsBatteryLowAndNotChargingFromBatteryStatus() {
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(emptyList()),
            WallpaperInteractiveWatchFaceInstanceParams(
                "interactiveInstanceId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                null
            )
        )

        watchFaceImpl.setIsBatteryLowAndNotChargingFromBatteryStatus(
            Intent().apply {
                putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING)
                putExtra(BatteryManager.EXTRA_LEVEL, 0)
                putExtra(BatteryManager.EXTRA_SCALE, 100)
            }
        )
        assertTrue(watchState.isBatteryLowAndNotCharging.value)

        watchFaceImpl.setIsBatteryLowAndNotChargingFromBatteryStatus(
            Intent().apply {
                putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
                putExtra(BatteryManager.EXTRA_LEVEL, 0)
                putExtra(BatteryManager.EXTRA_SCALE, 100)
            }
        )
        assertFalse(watchState.isBatteryLowAndNotCharging.value)

        watchFaceImpl.setIsBatteryLowAndNotChargingFromBatteryStatus(
            Intent().apply {
                putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING)
                putExtra(BatteryManager.EXTRA_LEVEL, 80)
                putExtra(BatteryManager.EXTRA_SCALE, 100)
            }
        )
        assertFalse(watchState.isBatteryLowAndNotCharging.value)

        watchFaceImpl.setIsBatteryLowAndNotChargingFromBatteryStatus(Intent())
        assertFalse(watchState.isBatteryLowAndNotCharging.value)

        watchFaceImpl.setIsBatteryLowAndNotChargingFromBatteryStatus(null)
        assertFalse(watchState.isBatteryLowAndNotCharging.value)
    }

    @Test
    public fun isAmbientInitalisedEvenWithoutPropertiesSent() {
        testWatchFaceService = TestWatchFaceService(
            WatchFaceType.ANALOG,
            emptyList(),
            { _, currentUserStyleRepository, watchState ->
                TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
            },
            UserStyleSchema(emptyList()),
            watchState,
            handler,
            null,
            true,
            null
        )

        engineWrapper = testWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onCreate(surfaceHolder)

        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        sendBinder(engineWrapper, 1)

        // At this stage we haven't sent properties such as isAmbient, we expect it to be
        // initialized to false (as opposed to null).
        assertThat(watchState.isAmbient.value).isFalse()
    }

    @Test
    public fun onDestroy_clearsInstanceRecord() {
        val instanceId = "interactiveInstanceId"
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            emptyList(),
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            WallpaperInteractiveWatchFaceInstanceParams(
                instanceId,
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(hashMapOf(colorStyleSetting to blueStyleOption)).toWireFormat(),
                null
            )
        )
        engineWrapper.onDestroy()

        assertNull(InteractiveInstanceManager.getAndRetainInstance(instanceId))
    }

    @Test
    public fun sendComplicationWallpaperCommandPreRFlow() {
        initEngine(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList())
        )

        setComplicationViaWallpaperCommand(
            LEFT_COMPLICATION_ID,
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("Override"))
                .build()
        )

        val complication =
            watchFaceImpl.complicationsManager[LEFT_COMPLICATION_ID]!!.complicationData.value as
                ShortTextComplicationData
        assertThat(
            complication.text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("Override")
    }

    @Test
    public fun sendComplicationWallpaperCommandIgnoredPostRFlow() {
        val instanceId = "interactiveInstanceId"
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            WallpaperInteractiveWatchFaceInstanceParams(
                instanceId,
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                listOf(
                    IdAndComplicationDataWireFormat(
                        LEFT_COMPLICATION_ID,
                        ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("INITIAL_VALUE"))
                            .build()
                    )
                )
            )
        )

        // This should be ignored because we're on the R flow.
        setComplicationViaWallpaperCommand(
            LEFT_COMPLICATION_ID,
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortText(ComplicationText.plainText("Override"))
                .build()
        )

        val complication =
            watchFaceImpl.complicationsManager[LEFT_COMPLICATION_ID]!!.complicationData.value as
                ShortTextComplicationData
        assertThat(
            complication.text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("INITIAL_VALUE")
    }

    @Test
    public fun directBoot() {
        val instanceId = "DirectBootInstance"
        testWatchFaceService = TestWatchFaceService(
            WatchFaceType.ANALOG,
            emptyList(),
            { _, currentUserStyleRepository, watchState ->
                TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
            },
            UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting)),
            watchState,
            handler,
            null,
            false, // Allows DirectBoot
            WallpaperInteractiveWatchFaceInstanceParams(
                instanceId,
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(
                    hashMapOf(
                        colorStyleSetting to blueStyleOption,
                        watchHandStyleSetting to gothicStyleOption
                    )
                ).toWireFormat(),
                null
            )
        )

        testWatchFaceService.onCreateEngine().onSurfaceChanged(surfaceHolder, 0, 100, 100)

        runPendingPostedDispatchedContinuationTasks()

        val instance = InteractiveInstanceManager.getAndRetainInstance(instanceId)
        assertThat(instance).isNotNull()
        val userStyle = instance!!.engine.watchFaceImpl.currentUserStyleRepository.userStyle
        assertThat(userStyle[colorStyleSetting]).isEqualTo(blueStyleOption)
        assertThat(userStyle[watchHandStyleSetting]).isEqualTo(gothicStyleOption)

        InteractiveInstanceManager.deleteInstance(instanceId)
    }

    @Test
    public fun headlessFlagPreventsDirectBoot() {
        val instanceId = "DirectBootInstance"
        testWatchFaceService = TestWatchFaceService(
            WatchFaceType.ANALOG,
            emptyList(),
            { _, currentUserStyleRepository, watchState ->
                TestRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    INTERACTIVE_UPDATE_RATE_MS
                )
            },
            UserStyleSchema(emptyList()),
            watchState,
            handler,
            null,
            false, // Allows DirectBoot
            WallpaperInteractiveWatchFaceInstanceParams(
                instanceId,
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(
                    hashMapOf(
                        colorStyleSetting to blueStyleOption,
                        watchHandStyleSetting to gothicStyleOption
                    )
                ).toWireFormat(),
                null
            )
        )

        testWatchFaceService.createHeadlessEngine()

        runPendingPostedDispatchedContinuationTasks()

        val instance = InteractiveInstanceManager.getAndRetainInstance(instanceId)
        assertThat(instance).isNull()
    }

    @Test
    public fun firstOnVisibilityChangedIgnoredPostRFlow() {
        val instanceId = "interactiveInstanceId"
        initWallpaperInteractiveWatchFaceInstance(
            WatchFaceType.ANALOG,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(emptyList()),
            WallpaperInteractiveWatchFaceInstanceParams(
                instanceId,
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                UserStyle(emptyMap()).toWireFormat(),
                listOf(
                    IdAndComplicationDataWireFormat(
                        LEFT_COMPLICATION_ID,
                        ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(ComplicationText.plainText("INITIAL_VALUE"))
                            .build()
                    )
                )
            )
        )

        val observer = mock<Observer<Boolean>>()

        // This should be ignored.
        engineWrapper.onVisibilityChanged(true)
        watchState.isVisible.addObserver(observer)
        verify(observer, times(0)).onChanged(false)

        // This should trigger the observer.
        engineWrapper.onVisibilityChanged(false)
        verify(observer).onChanged(true)
    }

    @Test
    public fun complicationsUserStyleSetting_with_setComplicationBounds() {
        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting"),
            "AllComplications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                ComplicationsOption(
                    Option.Id(RIGHT_COMPLICATION),
                    "Right",
                    null,
                    listOf(
                        ComplicationOverlay.Builder(LEFT_COMPLICATION_ID)
                            .setComplicationBounds(
                                ComplicationBounds(RectF(10f, 10f, 20f, 20f))
                            ).build()
                    )
                )
            ),
            affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS)
        )

        // This should not crash.
        initEngine(
            WatchFaceType.DIGITAL,
            listOf(leftComplication, rightComplication),
            UserStyleSchema(listOf(complicationsStyleSetting)),
            apiVersion = 4
        )
    }

    @Suppress("DEPRECATION")
    private fun getChinWindowInsetsApi25(@Px chinHeight: Int): WindowInsets =
        WindowInsets.Builder().setSystemWindowInsets(
            Insets.of(0, 0, 0, chinHeight)
        ).build()

    private fun getChinWindowInsetsApi30(@Px chinHeight: Int): WindowInsets =
        WindowInsets.Builder().setInsets(
            WindowInsets.Type.systemBars(),
            Insets.of(Rect().apply { bottom = chinHeight })
        ).build()
}
