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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.ContentDescriptionLabel
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.client.DefaultComplicationProviderPolicyAndType
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.client.WatchUiState
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.samples.BLUE_STYLE
import androidx.wear.watchface.samples.COLOR_STYLE_SETTING
import androidx.wear.watchface.samples.COMPLICATIONS_STYLE_SETTING
import androidx.wear.watchface.samples.DRAW_HOUR_PIPS_STYLE_SETTING
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.GREEN_STYLE
import androidx.wear.watchface.samples.NO_COMPLICATIONS
import androidx.wear.watchface.samples.WATCH_HAND_LENGTH_STYLE_SETTING
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_MILLIS = 500L
private const val DESTROY_TIMEOUT_MILLIS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
public class WatchFaceControlClientTest {
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
    private lateinit var surfaceHolder: SurfaceHolder

    @Mock
    private lateinit var surface: Surface
    private lateinit var engine: WatchFaceService.EngineWrapper
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wallpaperService: TestExampleCanvasAnalogWatchFaceService

    @Before
    public fun setUp() {
        MockitoAnnotations.initMocks(this)
        WatchFaceControlTestService.apiVersionOverride = null
        wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
    }

    @After
    public fun tearDown() {
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
    public val screenshotRule: AndroidXScreenshotTestRule =
        AndroidXScreenshotTestRule("wear/wear-watchface-client")

    private val exampleWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.samples.test",
        "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
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
                .build(),
        EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("ID").build(),
                ComplicationText.EMPTY
            ).setTitle(PlainComplicationText.Builder("Right").build())
                .build()
    )

    private fun createEngine() {
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
            engine.onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
        }
    }

    @Test
    public fun headlessScreenshot() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
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
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "headlessScreenshot")

        headlessInstance.close()
    }

    @Test
    public fun yellowComplicationHighlights() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
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
                    RenderParameters.HighlightedElement.AllComplications,
                    Color.YELLOW,
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "yellowComplicationHighlights")

        headlessInstance.close()
    }

    @Test
    public fun highlightOnlyLayer() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
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
                    RenderParameters.HighlightedElement.AllComplications,
                    Color.YELLOW,
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "highlightOnlyLayer")

        headlessInstance.close()
    }

    @Test
    public fun headlessComplicationDetails() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
            deviceConfig,
            400,
            400
        )!!

        assertThat(headlessInstance.complicationsState.size).isEqualTo(2)

        val leftComplicationDetails = headlessInstance.complicationsState[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(leftComplicationDetails.boundsType).isEqualTo(ComplicationBoundsType.ROUND_RECT)
        assertThat(leftComplicationDetails.defaultProviderPolicy.systemProviderFallback).isEqualTo(
            SystemProviders.PROVIDER_DAY_OF_WEEK
        )
        assertThat(leftComplicationDetails.defaultProviderType).isEqualTo(
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

        val rightComplicationDetails = headlessInstance.complicationsState[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(rightComplicationDetails.boundsType).isEqualTo(ComplicationBoundsType.ROUND_RECT)
        assertThat(rightComplicationDetails.defaultProviderPolicy.systemProviderFallback).isEqualTo(
            SystemProviders.PROVIDER_STEP_COUNT
        )
        assertThat(rightComplicationDetails.defaultProviderType).isEqualTo(
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
    public fun headlessUserStyleSchema() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
            deviceConfig,
            400,
            400
        )!!

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(4)
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

        headlessInstance.close()
    }

    @Test
    public fun headlessToBundleAndCreateFromBundle() {
        val headlessInstance = HeadlessWatchFaceClient.createFromBundle(
            service.createHeadlessWatchFaceClient(
                exampleWatchFaceComponentName,
                deviceConfig,
                400,
                400
            )!!.toBundle()
        )

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(4)
    }

    @Test
    public fun getOrCreateInteractiveWatchFaceClient(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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

        val interactiveInstance = withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }

        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            1234567,
            null,
            complications
        )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "interactiveScreenshot")
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    public fun getOrCreateInteractiveWatchFaceClient_initialStyle(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                // An incomplete map which is OK.
                UserStyleData(
                    mapOf(
                        "color_style_setting" to "green_style".encodeToByteArray(),
                        "draw_hour_pips_style_setting" to BooleanOption(false).id.value,
                        "watch_hand_length_style_setting" to DoubleRangeOption(0.8).id.value
                    )
                ),
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        val interactiveInstance = withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }

        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            1234567,
            null,
            complications
        )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "initialStyle")
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    public fun interactiveWatchFaceClient_ComplicationDetails(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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

        val interactiveInstance = withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }

        interactiveInstance.updateComplicationData(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("Test").build(),
                        ComplicationText.EMPTY
                    ).build(),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("Test").build(),
                        ComplicationText.EMPTY
                    ).build()
            )
        )

        assertThat(interactiveInstance.complicationsState.size).isEqualTo(2)

        val leftComplicationDetails = interactiveInstance.complicationsState[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(leftComplicationDetails.boundsType).isEqualTo(ComplicationBoundsType.ROUND_RECT)
        assertThat(leftComplicationDetails.defaultProviderPolicy.systemProviderFallback).isEqualTo(
            SystemProviders.PROVIDER_DAY_OF_WEEK
        )
        assertThat(leftComplicationDetails.defaultProviderType).isEqualTo(
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

        val rightComplicationDetails = interactiveInstance.complicationsState[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(rightComplicationDetails.boundsType).isEqualTo(ComplicationBoundsType.ROUND_RECT)
        assertThat(rightComplicationDetails.defaultProviderPolicy.systemProviderFallback).isEqualTo(
            SystemProviders.PROVIDER_STEP_COUNT
        )
        assertThat(rightComplicationDetails.defaultProviderType).isEqualTo(
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
            ComplicationType.LONG_TEXT
        )

        interactiveInstance.close()
    }

    @Test
    public fun getOrCreateInteractiveWatchFaceClient_existingOpenInstance(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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

        withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }

        withTimeout(CONNECT_TIMEOUT_MILLIS) {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )
        }
    }

    @Test
    public fun getOrCreateInteractiveWatchFaceClient_existingClosedInstance(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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
        val interactiveInstance = withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }
        // Closing this interface means the subsequent
        // getOrCreateInteractiveWatchFaceClient won't immediately return
        // a resolved future.
        interactiveInstance.close()

        val deferredExistingInstance = async {
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
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
            engine.onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
        }

        withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredExistingInstance.await()
        }
    }

    @Test
    public fun getInteractiveWatchFaceInstance(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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
        withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }

        val sysUiInterface =
            service.getInteractiveWatchFaceClientInstance("testId")!!

        val contentDescriptionLabels = sysUiInterface.contentDescriptionLabels
        assertThat(contentDescriptionLabels.size).isEqualTo(3)
        // Central clock element. Note we don't know the timezone this test will be running in
        // so we can't assert the contents of the clock's test.
        assertThat(contentDescriptionLabels[0].bounds).isEqualTo(Rect(100, 100, 300, 300))
        assertThat(contentDescriptionLabels[0].getTextAt(context.resources, 0).isNotEmpty())

        // Left complication.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(contentDescriptionLabels[1].getTextAt(context.resources, 0))
            .isEqualTo("ID Left")

        // Right complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(contentDescriptionLabels[2].getTextAt(context.resources, 0))
            .isEqualTo("ID Right")

        sysUiInterface.close()
    }

    @Test
    public fun additionalContentDescriptionLabels(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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
        withTimeout(CONNECT_TIMEOUT_MILLIS) {
            val instance = deferredInteractiveInstance.await()

            // We need to wait for watch face init to have completed before lateinit
            // wallpaperService.watchFace will be assigned. To do this we issue an arbitrary API
            // call which by necessity awaits full initialization.
            instance.complicationsState
        }

        // Add some additional ContentDescriptionLabels
        wallpaperService.watchFace.renderer.additionalContentDescriptionLabels = listOf(
            Pair(
                0,
                ContentDescriptionLabel(
                    PlainComplicationText.Builder("Before").build(),
                    Rect(10, 10, 20, 20),
                    null
                )
            ),
            Pair(
                20000,
                ContentDescriptionLabel(
                    PlainComplicationText.Builder("After").build(),
                    Rect(30, 30, 40, 40),
                    null
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
        assertThat(contentDescriptionLabels[0].getTextAt(context.resources, 0).isNotEmpty())

        // First additional ContentDescriptionLabel.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(10, 10, 20, 20))
        assertThat(contentDescriptionLabels[1].getTextAt(context.resources, 0))
            .isEqualTo("Before")

        // Left complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(contentDescriptionLabels[2].getTextAt(context.resources, 0))
            .isEqualTo("ID Left")

        // Right complication.
        assertThat(contentDescriptionLabels[3].bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(contentDescriptionLabels[3].getTextAt(context.resources, 0))
            .isEqualTo("ID Right")

        // Second additional ContentDescriptionLabel.
        assertThat(contentDescriptionLabels[4].bounds).isEqualTo(Rect(30, 30, 40, 40))
        assertThat(contentDescriptionLabels[4].getTextAt(context.resources, 0))
            .isEqualTo("After")
    }

    @Test
    public fun updateInstance(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
            service.getOrCreateInteractiveWatchFaceClient(
                "testId",
                deviceConfig,
                systemState,
                UserStyleData(
                    mapOf(
                        COLOR_STYLE_SETTING to GREEN_STYLE.encodeToByteArray(),
                        WATCH_HAND_LENGTH_STYLE_SETTING to DoubleRangeOption(0.25).id.value,
                        DRAW_HOUR_PIPS_STYLE_SETTING to BooleanOption(false).id.value,
                        COMPLICATIONS_STYLE_SETTING to NO_COMPLICATIONS.encodeToByteArray()
                    )
                ),
                complications
            )
        }

        // Create the engine which triggers creation of InteractiveWatchFaceClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance = runBlocking {
            withTimeout(CONNECT_TIMEOUT_MILLIS) {
                deferredInteractiveInstance.await()
            }
        }

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

        // The complications should have been cleared.
        val leftComplication =
            interactiveInstance.complicationsState[EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID]!!
        val rightComplication =
            interactiveInstance.complicationsState[EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID]!!
        assertThat(leftComplication.currentType).isEqualTo(ComplicationType.NO_DATA)
        assertThat(rightComplication.currentType).isEqualTo(ComplicationType.NO_DATA)

        // It should be possible to create an instance with the updated id.
        val instance =
            service.getInteractiveWatchFaceClientInstance("testId2")
        assertThat(instance).isNotNull()
        instance?.close()

        interactiveInstance.updateComplicationData(complications)
        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                null
            ),
            1234567,
            null,
            complications
        )

        try {
            // Note the hour hand pips and both complications should be visible in this image.
            bitmap.assertAgainstGolden(screenshotRule, "setUserStyle")
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    public fun getComplicationIdAt(): Unit = runBlocking {
        val deferredInteractiveInstance = async {
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

        val interactiveInstance = withTimeout(CONNECT_TIMEOUT_MILLIS) {
            deferredInteractiveInstance.await()
        }

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
    public fun crashingWatchFace(): Unit = runBlocking {
        val wallpaperService = TestCrashingWatchFaceServiceWithBaseContext()

        // Create the engine which triggers the crashing watchface.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
            engine.onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
        }

        val client = service.getOrCreateInteractiveWatchFaceClient(
            "testId",
            deviceConfig,
            systemState,
            null,
            complications
        )

        try {
            // The first call on the interface should report the crash.
            client.complicationsState
            fail("Expected an exception to be thrown because the watchface crashed on init")
        } catch (e: Exception) {
            assertThat(e.toString()).contains("Deliberately crashing")
        }
    }

    @Test
    public fun getDefaultProviderPolicies() {
        assertThat(
            service.getDefaultComplicationProviderPoliciesAndType(exampleWatchFaceComponentName)
        ).containsExactlyEntriesIn(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    DefaultComplicationProviderPolicyAndType(
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
                        ComplicationType.SHORT_TEXT
                    ),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    DefaultComplicationProviderPolicyAndType(
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_STEP_COUNT),
                        ComplicationType.SHORT_TEXT
                    )
            )
        )
    }

    @Test
    public fun getDefaultProviderPoliciesOldApi() {
        WatchFaceControlTestService.apiVersionOverride = 1
        assertThat(
            service.getDefaultComplicationProviderPoliciesAndType(exampleWatchFaceComponentName)
        ).containsExactlyEntriesIn(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    DefaultComplicationProviderPolicyAndType(
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
                        ComplicationType.SHORT_TEXT
                    ),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    DefaultComplicationProviderPolicyAndType(
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_STEP_COUNT),
                        ComplicationType.SHORT_TEXT
                    )
            )
        )
    }

    @Test
    public fun getDefaultProviderPolicies_with_TestCrashingWatchFaceService() {
        // Tests that we can retrieve the DefaultComplicationProviderPolicy without invoking any
        // parts of TestCrashingWatchFaceService that deliberately crash.
        assertThat(
            service.getDefaultComplicationProviderPoliciesAndType(
                ComponentName(
                    "androidx.wear.watchface.client.test",
                    "androidx.wear.watchface.client.test.TestCrashingWatchFaceService"

                )
            )
        ).containsExactlyEntriesIn(
            mapOf(
                TestCrashingWatchFaceService.COMPLICATION_ID to
                    DefaultComplicationProviderPolicyAndType(
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_SUNRISE_SUNSET),
                        ComplicationType.LONG_TEXT
                    )
            )
        )
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
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        watchFace = super.createWatchFace(
            surfaceHolder,
            watchState,
            complicationsManager,
            currentUserStyleRepository
        )
        return watchFace
    }
}

internal open class TestCrashingWatchFaceService : WatchFaceService() {

    companion object {
        const val COMPLICATION_ID = 123
    }

    override fun createComplicationsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationsManager {
        return ComplicationsManager(
            listOf(
                Complication.createRoundRectComplicationBuilder(
                    COMPLICATION_ID,
                    { _, _ -> throw Exception("Deliberately crashing") },
                    listOf(ComplicationType.LONG_TEXT),
                    DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_SUNRISE_SUNSET),
                    ComplicationBounds(RectF(0.1f, 0.1f, 0.4f, 0.4f))
                ).setDefaultProviderType(ComplicationType.LONG_TEXT).build()
            ),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        throw Exception("Deliberately crashing")
    }
}

internal class TestCrashingWatchFaceServiceWithBaseContext : TestCrashingWatchFaceService() {
    init {
        attachBaseContext(ApplicationProvider.getApplicationContext<Context>())
    }
}
