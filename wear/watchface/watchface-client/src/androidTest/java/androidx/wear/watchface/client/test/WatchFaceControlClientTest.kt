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

package androidx.wear.watchface.client.test

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.ContentDescriptionLabel
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.InteractiveWatchFaceClient
import androidx.wear.watchface.client.InteractiveWatchFaceClientImpl
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.client.WatchUiState
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.samples.BLUE_STYLE
import androidx.wear.watchface.samples.COLOR_STYLE_SETTING
import androidx.wear.watchface.samples.COMPLICATIONS_STYLE_SETTING
import androidx.wear.watchface.samples.DRAW_HOUR_PIPS_STYLE_SETTING
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.ExampleOpenGLBackgroundInitWatchFaceService
import androidx.wear.watchface.samples.GREEN_STYLE
import androidx.wear.watchface.samples.NO_COMPLICATIONS
import androidx.wear.watchface.samples.WATCH_HAND_LENGTH_STYLE_SETTING
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val CONNECT_TIMEOUT_MILLIS = 500L
private const val DESTROY_TIMEOUT_MILLIS = 500L
private const val UPDATE_TIMEOUT_MILLIS = 500L

const val CONFIGURABLE_DATA_SOURCE_PKG = "androidx.wear.watchface.complications.datasource.samples"
const val CONFIGURABLE_DATA_SOURCE = "$CONFIGURABLE_DATA_SOURCE_PKG.ConfigurableDataSourceService"

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceControlClientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val service = runBlocking {
        WatchFaceControlClient.createWatchFaceControlClientImpl(
            context,
            Intent(context, WatchFaceControlTestService::class.java).apply {
                action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
            }
        )
    }

    @Mock
    private lateinit var mockBinder: IBinder

    @Mock
    private lateinit var iInteractiveWatchFace: IInteractiveWatchFace

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder

    @Mock
    private lateinit var surfaceHolder2: SurfaceHolder

    @Mock
    private lateinit var surface: Surface
    private lateinit var engine: WatchFaceService.EngineWrapper
    private val handler = Handler(Looper.getMainLooper())
    private val handlerCoroutineScope =
        CoroutineScope(Handler(handler.looper).asCoroutineDispatcher())
    private lateinit var wallpaperService: WatchFaceService

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        WatchFaceControlTestService.apiVersionOverride = null
        wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)
    }

    @After
    fun tearDown() {
        // Interactive instances are not currently shut down when all instances go away. E.g. WCS
        // crashing does not cause the watch face to stop. So we need to shut down explicitly.
        if (this::engine.isInitialized) {
            val latch = CountDownLatch(1)
            handler.post {
                engine.onDestroy()
                latch.countDown()
            }
            latch.await(DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        }
        service.close()
    }

    @get:Rule
    val screenshotRule: AndroidXScreenshotTestRule =
        AndroidXScreenshotTestRule("wear/wear-watchface-client")

    private val exampleCanvasAnalogWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.samples.test",
        "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
    )

    private val exampleOpenGLWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.samples.test",
        "androidx.wear.watchface.samples.ExampleOpenGLBackgroundInitWatchFaceService"
    )

    private val deviceConfig = DeviceConfig(
        false,
        false,
        0,
        0
    )

    private val systemState = WatchUiState(false, 0)

    private val complications = mapOf(
        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("ID").build(),
                ComplicationText.EMPTY
            ).setTitle(PlainComplicationText.Builder("Left").build())
                .setTapAction(
                    PendingIntent.getActivity(context, 0, Intent("left"), FLAG_IMMUTABLE)
                )
                .build(),
        EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("ID").build(),
                ComplicationText.EMPTY
            ).setTitle(PlainComplicationText.Builder("Right").build())
                .setTapAction(
                    PendingIntent.getActivity(context, 0, Intent("right"), FLAG_IMMUTABLE)
                )
                .build()
    )

    private fun createEngine() {
        // onCreateEngine must run after getOrCreateInteractiveWatchFaceClient. To ensure the
        // ordering relationship both calls should run on the same handler.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }
    }

    private fun <X> awaitWithTimeout(
        thing: Deferred<X>,
        timeoutMillis: Long = CONNECT_TIMEOUT_MILLIS
    ): X {
        var value: X? = null
        val latch = CountDownLatch(1)
        handlerCoroutineScope.launch {
            value = thing.await()
            latch.countDown()
        }
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Timeout waiting for thing!")
        }
        return value!!
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun headlessScreenshot() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            "id",
            exampleCanvasAnalogWatchFaceComponentName,
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!
        val bitmap = headlessInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "headlessScreenshot")

        headlessInstance.close()
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun yellowComplicationHighlights() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            "id",
            exampleCanvasAnalogWatchFaceComponentName,
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!
        val bitmap = headlessInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.YELLOW,
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "yellowComplicationHighlights")

        headlessInstance.close()
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun highlightOnlyLayer() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            "id",
            exampleCanvasAnalogWatchFaceComponentName,
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!
        val bitmap = headlessInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                emptySet(),
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.YELLOW,
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "highlightOnlyLayer")

        headlessInstance.close()
    }

    @Suppress("DEPRECATION") // defaultDataSourceType
    @Test
    fun headlessComplicationDetails() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            "id",
            exampleCanvasAnalogWatchFaceComponentName,
            deviceConfig,
            400,
            400
        )!!

        assertThat(headlessInstance.complicationSlotsState.size).isEqualTo(2)

        val leftComplicationDetails = headlessInstance.complicationSlotsState[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(leftComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(
            leftComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(
            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK
        )
        assertThat(leftComplicationDetails.defaultDataSourceType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(leftComplicationDetails.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        assertTrue(leftComplicationDetails.isEnabled)

        val rightComplicationDetails = headlessInstance.complicationSlotsState[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(rightComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(
            rightComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(
            SystemDataSources.DATA_SOURCE_STEP_COUNT
        )
        assertThat(rightComplicationDetails.defaultDataSourceType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(rightComplicationDetails.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        assertTrue(rightComplicationDetails.isEnabled)

        headlessInstance.close()
    }

    @Test
    fun complicationProviderDefaults() {
        val wallpaperService = TestComplicationProviderDefaultsWatchFaceService(
            context,
            surfaceHolder
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }
        // Create the engine which triggers construction of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            assertThat(interactiveInstance.complicationSlotsState.keys).containsExactly(123)

            val slot = interactiveInstance.complicationSlotsState[123]!!
            assertThat(slot.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package1", "com.app1"))
            assertThat(slot.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.PHOTO_IMAGE)

            assertThat(slot.defaultDataSourcePolicy.secondaryDataSource)
                .isEqualTo(ComponentName("com.package2", "com.app2"))
            assertThat(slot.defaultDataSourcePolicy.secondaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.LONG_TEXT)

            assertThat(slot.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
            assertThat(slot.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun unspecifiedComplicationSlotNames() {
        val wallpaperService = TestComplicationProviderDefaultsWatchFaceService(
            context,
            surfaceHolder
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }
        // Create the engine which triggers construction of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            assertThat(interactiveInstance.complicationSlotsState.keys).containsExactly(123)

            val slot = interactiveInstance.complicationSlotsState[123]!!
            assertThat(slot.nameResourceId).isNull()
            assertThat(slot.screenReaderNameResourceId).isNull()
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun headlessUserStyleSchema() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            "id",
            exampleCanvasAnalogWatchFaceComponentName,
            deviceConfig,
            400,
            400
        )!!

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(5)
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[0].id.value).isEqualTo(
            "color_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[1].id.value).isEqualTo(
            "draw_hour_pips_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[2].id.value).isEqualTo(
            "watch_hand_length_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[3].id.value).isEqualTo(
            "complications_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[4].id.value).isEqualTo(
            "hours_draw_freq_style_setting"
        )

        headlessInstance.close()
    }

    @Test
    fun headlessToBundleAndCreateFromBundle() {
        val headlessInstance = HeadlessWatchFaceClient.createFromBundle(
            service.createHeadlessWatchFaceClient(
                "id",
                exampleCanvasAnalogWatchFaceComponentName,
                deviceConfig,
                400,
                400
            )!!.toBundle()
        )

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(5)
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun getOrCreateInteractiveWatchFaceClient() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "interactiveScreenshot")
        } finally {
            interactiveInstance.close()
        }
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun getOrCreateInteractiveWatchFaceClient_initialStyle() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                // An incomplete map which is OK.
                UserStyleData(
                    mapOf(
                        "color_style_setting" to "green_style".encodeToByteArray(),
                        "draw_hour_pips_style_setting" to BooleanOption.FALSE.id.value,
                        "watch_hand_length_style_setting" to DoubleRangeOption(0.8).id.value
                    )
                ),
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "initialStyle")
        } finally {
            interactiveInstance.close()
        }
    }

    @Suppress("DEPRECATION") // defaultDataSourceType
    @Test
    fun interactiveWatchFaceClient_ComplicationDetails() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        assertThat(interactiveInstance.complicationSlotsState.size).isEqualTo(2)

        val leftComplicationDetails = interactiveInstance.complicationSlotsState[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(leftComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(
            leftComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(
            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK
        )
        assertThat(leftComplicationDetails.defaultDataSourceType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(leftComplicationDetails.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        assertTrue(leftComplicationDetails.isEnabled)
        assertThat(leftComplicationDetails.currentType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(leftComplicationDetails.nameResourceId)
            .isEqualTo(androidx.wear.watchface.samples.R.string.left_complication_screen_name)
        assertThat(leftComplicationDetails.screenReaderNameResourceId).isEqualTo(
            androidx.wear.watchface.samples.R.string.left_complication_screen_reader_name
        )

        val rightComplicationDetails = interactiveInstance.complicationSlotsState[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(rightComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(
            rightComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
        assertThat(rightComplicationDetails.defaultDataSourceType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(rightComplicationDetails.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        assertTrue(rightComplicationDetails.isEnabled)
        assertThat(rightComplicationDetails.currentType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(rightComplicationDetails.nameResourceId)
            .isEqualTo(androidx.wear.watchface.samples.R.string.right_complication_screen_name)
        assertThat(rightComplicationDetails.screenReaderNameResourceId).isEqualTo(
            androidx.wear.watchface.samples.R.string.right_complication_screen_reader_name
        )

        interactiveInstance.close()
    }

    @Test
    public fun updateComplicationData() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        // Under the hood updateComplicationData is a oneway aidl method so we need to perform some
        // additional synchronization to ensure it's side effects have been applied before
        // inspecting complicationSlotsState otherwise we risk test flakes.
        val updateCountDownLatch = CountDownLatch(1)
        var leftComplicationSlot: ComplicationSlot

        runBlocking {
            leftComplicationSlot = engine.deferredWatchFaceImpl.await()
                .complicationSlotsManager.complicationSlots[
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
            ]!!
        }

        var isFirstCall = true
        handlerCoroutineScope.launch {
            leftComplicationSlot.complicationData.collect {
                if (!isFirstCall) {
                    updateCountDownLatch.countDown()
                } else {
                    isFirstCall = false
                }
            }
        }

        interactiveInstance.updateComplicationData(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    RangedValueComplicationData.Builder(
                        50.0f,
                        10.0f,
                        100.0f,
                        ComplicationText.EMPTY
                    ).build(),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("Test").build(),
                        ComplicationText.EMPTY
                    ).build()
            )
        )
        assertTrue(updateCountDownLatch.await(UPDATE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

        assertThat(interactiveInstance.complicationSlotsState.size).isEqualTo(2)

        val leftComplicationDetails = interactiveInstance.complicationSlotsState[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        val rightComplicationDetails = interactiveInstance.complicationSlotsState[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!

        assertThat(leftComplicationDetails.currentType).isEqualTo(
            ComplicationType.RANGED_VALUE
        )
        assertThat(rightComplicationDetails.currentType).isEqualTo(
            ComplicationType.LONG_TEXT
        )
    }

    @Test
    fun getOrCreateInteractiveWatchFaceClient_existingOpenInstance() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        awaitWithTimeout(deferredInteractiveInstance)

        val deferredInteractiveInstance2 = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        assertThat(awaitWithTimeout(deferredInteractiveInstance2).instanceId).isEqualTo("testId")
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun getOrCreateInteractiveWatchFaceClient_existingOpenInstance_styleChange() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        awaitWithTimeout(deferredInteractiveInstance)

        val deferredInteractiveInstance2 = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                UserStyleData(
                    mapOf(
                        "color_style_setting" to "blue_style".encodeToByteArray(),
                        "draw_hour_pips_style_setting" to BooleanOption.FALSE.id.value,
                        "watch_hand_length_style_setting" to DoubleRangeOption(0.25).id.value
                    )
                ),
                complications
            )
        }

        val interactiveInstance2 = awaitWithTimeout(deferredInteractiveInstance2)
        assertThat(interactiveInstance2.instanceId).isEqualTo("testId")

        val bitmap = interactiveInstance2.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        try {
            // Note the hour hand pips and both complicationSlots should be visible in this image.
            bitmap.assertAgainstGolden(screenshotRule, "existingOpenInstance_styleChange")
        } finally {
            interactiveInstance2.close()
        }
    }

    @Test
    fun getOrCreateInteractiveWatchFaceClient_existingClosedInstance() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        // Closing this interface means the subsequent
        // getOrCreateInteractiveWatchFaceClient won't immediately return
        // a resolved future.
        interactiveInstance.close()

        val deferredExistingInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        assertFalse(deferredExistingInstance.isCompleted)

        // We don't want to leave a pending request or it'll mess up subsequent tests.
        handler.post {
            wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        awaitWithTimeout(deferredExistingInstance)
    }

    @Test
    fun getInteractiveWatchFaceInstance() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        // Wait for the instance to be created.
        awaitWithTimeout(deferredInteractiveInstance)

        val sysUiInterface =
            service.getInteractiveWatchFaceClientInstance("testId")!!

        val contentDescriptionLabels = sysUiInterface.contentDescriptionLabels
        assertThat(contentDescriptionLabels.size).isEqualTo(3)
        // Central clock element. Note we don't know the timezone this test will be running in
        // so we can't assert the contents of the clock's test.
        assertThat(contentDescriptionLabels[0].bounds).isEqualTo(Rect(100, 100, 300, 300))
        assertThat(
            contentDescriptionLabels[0].getTextAt(context.resources, Instant.EPOCH)
        ).isNotEqualTo("")

        // Left complication.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(
            contentDescriptionLabels[1].getTextAt(context.resources, Instant.EPOCH)
        ).isEqualTo("ID Left")

        // Right complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(
            contentDescriptionLabels[2].getTextAt(context.resources, Instant.EPOCH)
        ).isEqualTo("ID Right")

        assertThat(sysUiInterface.overlayStyle.backgroundColor).isNull()
        assertThat(sysUiInterface.overlayStyle.foregroundColor).isNull()

        sysUiInterface.close()
    }

    @Test
    fun additionalContentDescriptionLabels() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        // We need to wait for watch face init to have completed before lateinit
        // wallpaperService.watchFace will be assigned. To do this we issue an arbitrary API
        // call which by necessity awaits full initialization.
        interactiveInstance.complicationSlotsState

        // Add some additional ContentDescriptionLabels
        val pendingIntent1 = PendingIntent.getActivity(
            context, 0, Intent("One"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val pendingIntent2 = PendingIntent.getActivity(
            context, 0, Intent("Two"),
            PendingIntent.FLAG_IMMUTABLE
        )
        (wallpaperService as TestExampleCanvasAnalogWatchFaceService)
            .watchFace.renderer.additionalContentDescriptionLabels = listOf(
                Pair(
                    0,
                    ContentDescriptionLabel(
                        PlainComplicationText.Builder("Before").build(),
                        Rect(10, 10, 20, 20),
                        pendingIntent1
                    )
                ),
                Pair(
                    20000,
                    ContentDescriptionLabel(
                        PlainComplicationText.Builder("After").build(),
                        Rect(30, 30, 40, 40),
                        pendingIntent2
                    )
                )
            )

        val sysUiInterface =
            service.getInteractiveWatchFaceClientInstance("testId")!!

        val contentDescriptionLabels = sysUiInterface.contentDescriptionLabels
        assertThat(contentDescriptionLabels.size).isEqualTo(5)

        // Central clock element. Note we don't know the timezone this test will be running in
        // so we can't assert the contents of the clock's test.
        assertThat(contentDescriptionLabels[0].bounds).isEqualTo(Rect(100, 100, 300, 300))
        assertThat(
            contentDescriptionLabels[0].getTextAt(context.resources, Instant.EPOCH)
        ).isNotEqualTo("")

        // First additional ContentDescriptionLabel.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(10, 10, 20, 20))
        assertThat(
            contentDescriptionLabels[1].getTextAt(context.resources, Instant.EPOCH)
        ).isEqualTo("Before")
        assertThat(contentDescriptionLabels[1].tapAction).isEqualTo(pendingIntent1)

        // Left complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(
            contentDescriptionLabels[2].getTextAt(context.resources, Instant.EPOCH)
        ).isEqualTo("ID Left")

        // Right complication.
        assertThat(contentDescriptionLabels[3].bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(
            contentDescriptionLabels[3].getTextAt(context.resources, Instant.EPOCH)
        ).isEqualTo("ID Right")

        // Second additional ContentDescriptionLabel.
        assertThat(contentDescriptionLabels[4].bounds).isEqualTo(Rect(30, 30, 40, 40))
        assertThat(
            contentDescriptionLabels[4].getTextAt(context.resources, Instant.EPOCH)
        ).isEqualTo("After")
        assertThat(contentDescriptionLabels[4].tapAction).isEqualTo(pendingIntent2)
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun updateInstance() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                UserStyleData(
                    mapOf(
                        COLOR_STYLE_SETTING to GREEN_STYLE.encodeToByteArray(),
                        WATCH_HAND_LENGTH_STYLE_SETTING to DoubleRangeOption(0.25).id.value,
                        DRAW_HOUR_PIPS_STYLE_SETTING to BooleanOption.FALSE.id.value,
                        COMPLICATIONS_STYLE_SETTING to NO_COMPLICATIONS.encodeToByteArray()
                    )
                ),
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        assertThat(interactiveInstance.instanceId).isEqualTo("testId")

        // Note this map doesn't include all the categories, which is fine the others will be set
        // to their defaults.
        interactiveInstance.updateWatchFaceInstance(
            "testId2",
            UserStyleData(
                mapOf(
                    COLOR_STYLE_SETTING to BLUE_STYLE.encodeToByteArray(),
                    WATCH_HAND_LENGTH_STYLE_SETTING to DoubleRangeOption(0.9).id.value,
                )
            )
        )

        assertThat(interactiveInstance.instanceId).isEqualTo("testId2")

        // It should be possible to create an instance with the updated id.
        val instance =
            service.getInteractiveWatchFaceClientInstance("testId2")
        assertThat(instance).isNotNull()
        instance?.close()

        // The previous instance should still be usable despite the new instance being closed.
        interactiveInstance.updateComplicationData(complications)
        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            complications
        )

        try {
            // Note the hour hand pips and both complicationSlots should be visible in this image.
            bitmap.assertAgainstGolden(screenshotRule, "setUserStyle")
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun getComplicationIdAt() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        assertNull(interactiveInstance.getComplicationIdAt(0, 0))
        assertThat(interactiveInstance.getComplicationIdAt(85, 165)).isEqualTo(
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        )
        assertThat(interactiveInstance.getComplicationIdAt(255, 165)).isEqualTo(
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        )
        interactiveInstance.close()
    }

    @Test
    fun crashingWatchFace() {
        val wallpaperService = TestCrashingWatchFaceServiceWithBaseContext(surfaceHolder)
        val client = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testCrashingId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers the crashing watchface.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        try {
            // The first call on the interface should report the crash.
            awaitWithTimeout(client).complicationSlotsState
            fail("Expected an exception to be thrown because the watchface crashed on init")
        } catch (e: Exception) {
            assertThat(e.toString()).contains("Deliberately crashing")
        }
    }

    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Test
    fun getDefaultProviderPolicies() {
        assertThat(
            service.getDefaultComplicationDataSourcePoliciesAndType(
                exampleCanvasAnalogWatchFaceComponentName
            )
        ).containsExactlyEntriesIn(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                        DefaultComplicationDataSourcePolicy(
                            ComponentName(
                                androidx.wear.watchface.samples.CONFIGURABLE_DATA_SOURCE_PKG,
                                androidx.wear.watchface.samples.CONFIGURABLE_DATA_SOURCE
                            ),
                            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK
                        ),
                        ComplicationType.SHORT_TEXT
                    ),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                        DefaultComplicationDataSourcePolicy(
                            SystemDataSources.DATA_SOURCE_STEP_COUNT
                        ),
                        ComplicationType.SHORT_TEXT
                    )
            )
        )
    }

    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Test
    fun getDefaultProviderPoliciesOldApi() {
        WatchFaceControlTestService.apiVersionOverride = 1
        assertThat(
            service.getDefaultComplicationDataSourcePoliciesAndType(
                exampleCanvasAnalogWatchFaceComponentName
            )
        ).containsExactlyEntriesIn(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                        DefaultComplicationDataSourcePolicy(
                            ComponentName(
                                androidx.wear.watchface.samples.CONFIGURABLE_DATA_SOURCE_PKG,
                                androidx.wear.watchface.samples.CONFIGURABLE_DATA_SOURCE
                            ),
                            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK
                        ),
                        ComplicationType.SHORT_TEXT
                    ),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                        DefaultComplicationDataSourcePolicy(
                            SystemDataSources.DATA_SOURCE_STEP_COUNT
                        ),
                        ComplicationType.SHORT_TEXT
                    )
            )
        )
    }

    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Test
    fun getDefaultProviderPolicies_with_TestCrashingWatchFaceService() {
        // Tests that we can retrieve the DefaultComplicationDataSourcePolicy without invoking any
        // parts of TestCrashingWatchFaceService that deliberately crash.
        assertThat(
            service.getDefaultComplicationDataSourcePoliciesAndType(
                ComponentName(
                    "androidx.wear.watchface.client.test",
                    "androidx.wear.watchface.client.test.TestCrashingWatchFaceService"

                )
            )
        ).containsExactlyEntriesIn(
            mapOf(
                TestCrashingWatchFaceService.COMPLICATION_ID to
                    androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                        DefaultComplicationDataSourcePolicy(
                            SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET
                        ),
                        ComplicationType.LONG_TEXT
                    )
            )
        )
    }

    @Test
    fun addWatchFaceReadyListener_canvasRender() {
        val initCompletableDeferred = CompletableDeferred<Unit>()
        val wallpaperService = TestAsyncCanvasRenderInitWatchFaceService(
            context,
            surfaceHolder,
            initCompletableDeferred
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)

        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            val wfReady = CompletableDeferred<Unit>()
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                { wfReady.complete(Unit) }
            )
            assertThat(wfReady.isCompleted).isFalse()

            initCompletableDeferred.complete(Unit)

            // This should not timeout.
            awaitWithTimeout(wfReady)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun removeWatchFaceReadyListener_canvasRender() {
        val initCompletableDeferred = CompletableDeferred<Unit>()
        val wallpaperService = TestAsyncCanvasRenderInitWatchFaceService(
            context,
            surfaceHolder,
            initCompletableDeferred
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)

        val renderLatch = CountDownLatch(1)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderLatch.countDown()
        }

        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            var listenerCalled = false
            val listener =
                InteractiveWatchFaceClient.OnWatchFaceReadyListener { listenerCalled = true }
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                listener
            )
            interactiveInstance.removeOnWatchFaceReadyListener(listener)
            assertThat(listenerCalled).isFalse()

            initCompletableDeferred.complete(Unit)

            assertTrue(renderLatch.await(DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

            assertThat(listenerCalled).isFalse()
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun addWatchFaceReadyListener_glesRender() {
        val surfaceTexture = SurfaceTexture(false)
        surfaceTexture.setDefaultBufferSize(10, 10)
        Mockito.`when`(surfaceHolder2.surface).thenReturn(Surface(surfaceTexture))
        Mockito.`when`(surfaceHolder2.surfaceFrame)
            .thenReturn(Rect(0, 0, 10, 10))

        val onUiThreadGlSurfaceCreatedCompletableDeferred = CompletableDeferred<Unit>()
        val onBackgroundThreadGlContextCreatedCompletableDeferred = CompletableDeferred<Unit>()
        val wallpaperService = TestAsyncGlesRenderInitWatchFaceService(
            context,
            surfaceHolder2,
            onUiThreadGlSurfaceCreatedCompletableDeferred,
            onBackgroundThreadGlContextCreatedCompletableDeferred
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }
        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            val wfReady = CompletableDeferred<Unit>()
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                { wfReady.complete(Unit) }
            )
            assertThat(wfReady.isCompleted).isFalse()

            onUiThreadGlSurfaceCreatedCompletableDeferred.complete(Unit)
            onBackgroundThreadGlContextCreatedCompletableDeferred.complete(Unit)

            // This can be a bit slow.
            awaitWithTimeout(wfReady, 2000)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun isConnectionAlive_false_after_close() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)
        assertThat(interactiveInstance.isConnectionAlive()).isTrue()

        interactiveInstance.close()
        assertThat(interactiveInstance.isConnectionAlive()).isFalse()
    }

    @Test
    fun hasComplicationCache_oldApi() {
        WatchFaceControlTestService.apiVersionOverride = 3
        assertFalse(service.hasComplicationDataCache())
    }

    @Test
    fun hasComplicationCache_currentApi() {
        assertTrue(service.hasComplicationDataCache())
    }

    @Test
    fun getPendingIntentForTouchEvent() {
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        assertNull(interactiveInstance.getPendingIntentForTouchEvent(0, 0, TapType.DOWN))
        assertNull(interactiveInstance.getPendingIntentForTouchEvent(0, 0, TapType.UP))

        assertNull(interactiveInstance.getPendingIntentForTouchEvent(85, 165, TapType.DOWN))
        // Due to PendingIntent's opaque nature we can't really assert much else.
        assertNotNull(
            interactiveInstance.getPendingIntentForTouchEvent(85, 165, TapType.UP)
        )

        assertNull(interactiveInstance.getPendingIntentForTouchEvent(255, 165, TapType.DOWN))
        assertNotNull(
            interactiveInstance.getPendingIntentForTouchEvent(255, 165, TapType.UP)
        )
        interactiveInstance.close()
    }

    @Test
    fun getPendingIntentForTouchEvent_pendingIntentTapListener() {
        val wallpaperService = TestPendingIntentTapListenerWatchFaceService(
            context,
            surfaceHolder
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)

        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        assertThat(
            interactiveInstance.getPendingIntentForTouchEvent(0, 0, TapType.DOWN)
        ).isNull()

        assertThat(
            interactiveInstance.getPendingIntentForTouchEvent(20, 50, TapType.UP)
        ).isNotNull()

        interactiveInstance.close()
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    @Test
    fun interactiveAndHeadlessOpenGlWatchFaceInstances() {
        val surfaceTexture = SurfaceTexture(false)
        surfaceTexture.setDefaultBufferSize(400, 400)
        Mockito.`when`(surfaceHolder2.surface).thenReturn(Surface(surfaceTexture))
        Mockito.`when`(surfaceHolder2.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        wallpaperService = TestExampleOpenGLBackgroundInitWatchFaceService(context, surfaceHolder2)

        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                emptyMap()
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)
        val headlessInstance = HeadlessWatchFaceClient.createFromBundle(
            service.createHeadlessWatchFaceClient(
                "id",
                exampleOpenGLWatchFaceComponentName,
                deviceConfig,
                200,
                200
            )!!.toBundle()
        )

        // Take screenshots from both instances to confirm rendering works as expected despite the
        // watch face using shared SharedAssets.
        val interactiveBitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            null
        )

        interactiveBitmap.assertAgainstGolden(screenshotRule, "opengl_interactive")

        val headlessBitmap = headlessInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            Instant.ofEpochMilli(1234567),
            null,
            null
        )

        headlessBitmap.assertAgainstGolden(screenshotRule, "opengl_headless")

        headlessInstance.close()
        interactiveInstance.close()
    }

    @Test
    fun supportsPendingIntentForTouchEvent_oldApi() {
        `when`(iInteractiveWatchFace.apiVersion).thenReturn(2)
        `when`(iInteractiveWatchFace.asBinder()).thenReturn(mockBinder)
        val client = InteractiveWatchFaceClientImpl(iInteractiveWatchFace)
        assertFalse(client.supportsPendingIntentForTouchEvent())
    }

    @Test
    fun supportsPendingIntentForTouchEvent_currentApi() {
        `when`(iInteractiveWatchFace.apiVersion).thenReturn(IInteractiveWatchFace.API_VERSION)
        `when`(iInteractiveWatchFace.asBinder()).thenReturn(mockBinder)
        val client = InteractiveWatchFaceClientImpl(iInteractiveWatchFace)
        assertTrue(client.supportsPendingIntentForTouchEvent())
    }

    @Test
    fun watchfaceOverlayStyle() {
        val wallpaperService = TestWatchfaceOverlayStyleWatchFaceService(
            context,
            surfaceHolder,
            WatchFace.OverlayStyle(Color.valueOf(Color.RED), Color.valueOf(Color.BLACK))
        )
        val deferredInteractiveInstance = handlerCoroutineScope.async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }

        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        assertThat(interactiveInstance.overlayStyle.backgroundColor)
            .isEqualTo(Color.valueOf(Color.RED))
        assertThat(interactiveInstance.overlayStyle.foregroundColor)
            .isEqualTo(Color.valueOf(Color.BLACK))

        interactiveInstance.close()
    }
}

internal class TestExampleCanvasAnalogWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleCanvasAnalogWatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        watchFace = super.createWatchFace(
            surfaceHolder,
            watchState,
            complicationSlotsManager,
            currentUserStyleRepository
        )
        return watchFace
    }
}

internal class TestExampleOpenGLBackgroundInitWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleOpenGLBackgroundInitWatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        watchFace = super.createWatchFace(
            surfaceHolder,
            watchState,
            complicationSlotsManager,
            currentUserStyleRepository
        )
        return watchFace
    }
}

internal class TestPendingIntentTapListenerWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleCanvasAnalogWatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.DIGITAL,
        object : Renderer.CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            16
        ) {
            override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

            override fun renderHighlightLayer(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime
            ) {
            }
        }
    ).setPendingIntentTapListener(
        object : WatchFace.PendingIntentTapListener {
            override fun onTapEvent(
                tapType: Int,
                tapEvent: TapEvent,
                complicationSlot: ComplicationSlot?
            ) = if (tapType == TapType.UP && tapEvent.xPos >= 20 && tapEvent.xPos < 40 &&
                tapEvent.yPos >= 50 && tapEvent.yPos < 90
            ) {
                PendingIntent.getActivity(
                    this@TestPendingIntentTapListenerWatchFaceService,
                    0,
                    Intent("Test"),
                    FLAG_IMMUTABLE
                )
            } else {
                null
            }
        }
    )
}

internal open class TestCrashingWatchFaceService : WatchFaceService() {

    companion object {
        const val COMPLICATION_ID = 123
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    COMPLICATION_ID,
                    { _, _ -> throw Exception("Deliberately crashing") },
                    listOf(ComplicationType.LONG_TEXT),
                    DefaultComplicationDataSourcePolicy(
                        SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET,
                        ComplicationType.LONG_TEXT
                    ),
                    ComplicationSlotBounds(RectF(0.1f, 0.1f, 0.4f, 0.4f))
                ).build()
            ),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        throw Exception("Deliberately crashing")
    }
}

internal class TestCrashingWatchFaceServiceWithBaseContext(
    private var surfaceHolderOverride: SurfaceHolder
) : TestCrashingWatchFaceService() {
    init {
        attachBaseContext(ApplicationProvider.getApplicationContext<Context>())
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride
}

internal class TestWatchfaceOverlayStyleWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var watchFaceOverlayStyle: WatchFace.OverlayStyle
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.DIGITAL,
        object : Renderer.CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            16
        ) {
            override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
                // Actually rendering something isn't required.
            }

            override fun renderHighlightLayer(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime
            ) {
                // Actually rendering something isn't required.
            }
        }
    ).setOverlayStyle(watchFaceOverlayStyle)
}

internal class TestAsyncCanvasRenderInitWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var initCompletableDeferred: CompletableDeferred<Unit>
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.DIGITAL,
        object : Renderer.CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            16
        ) {
            override suspend fun init() {
                initCompletableDeferred.await()
            }

            override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
                // Actually rendering something isn't required.
            }

            override fun renderHighlightLayer(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime
            ) {
                TODO("Not yet implemented")
            }
        }
    ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
        override fun getSystemTimeMillis() = 123456789L

        override fun getSystemTimeZoneId() = ZoneId.of("UTC")
    })
}

internal class TestAsyncGlesRenderInitWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var onUiThreadGlSurfaceCreatedCompletableDeferred: CompletableDeferred<Unit>,
    private var onBackgroundThreadGlContextCreatedCompletableDeferred: CompletableDeferred<Unit>
) : WatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.DIGITAL,
        object : Renderer.GlesRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            16
        ) {
            override suspend fun onUiThreadGlSurfaceCreated(width: Int, height: Int) {
                onUiThreadGlSurfaceCreatedCompletableDeferred.await()
            }

            override suspend fun onBackgroundThreadGlContextCreated() {
                onBackgroundThreadGlContextCreatedCompletableDeferred.await()
            }

            override fun render(zonedDateTime: ZonedDateTime) {
                // GLES rendering is complicated and not strictly necessary for our test.
            }

            override fun renderHighlightLayer(zonedDateTime: ZonedDateTime) {
                TODO("Not yet implemented")
            }
        }
    )
}

internal class TestComplicationProviderDefaultsWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    123,
                    { _, _ ->
                        object : CanvasComplication {
                            override fun render(
                                canvas: Canvas,
                                bounds: Rect,
                                zonedDateTime: ZonedDateTime,
                                renderParameters: RenderParameters,
                                slotId: Int
                            ) {
                            }

                            override fun drawHighlight(
                                canvas: Canvas,
                                bounds: Rect,
                                boundsType: Int,
                                zonedDateTime: ZonedDateTime,
                                color: Int
                            ) {
                            }

                            override fun getData() = NoDataComplicationData()

                            override fun loadData(
                                complicationData: ComplicationData,
                                loadDrawablesAsynchronous: Boolean
                            ) {
                            }
                        }
                    },
                    listOf(
                        ComplicationType.PHOTO_IMAGE,
                        ComplicationType.LONG_TEXT,
                        ComplicationType.SHORT_TEXT
                    ),
                    DefaultComplicationDataSourcePolicy(
                        ComponentName("com.package1", "com.app1"),
                        ComplicationType.PHOTO_IMAGE,
                        ComponentName("com.package2", "com.app2"),
                        ComplicationType.LONG_TEXT,
                        SystemDataSources.DATA_SOURCE_STEP_COUNT,
                        ComplicationType.SHORT_TEXT
                    ),
                    ComplicationSlotBounds(
                        RectF(0.1f, 0.2f, 0.3f, 0.4f)
                    )
                )
                    .build()
            ),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.DIGITAL,
        object : Renderer.CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            16
        ) {
            override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

            override fun renderHighlightLayer(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime
            ) {
            }
        }
    )
}
