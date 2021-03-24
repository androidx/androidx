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
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.LayerMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.SystemState
import androidx.wear.watchface.client.WatchFaceControlClient
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
import androidx.wear.watchface.style.Layer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private lateinit var surfaceHolder: SurfaceHolder
    @Mock
    private lateinit var surface: Surface
    private lateinit var engine: WallpaperService.Engine
    private val handler = Handler(Looper.getMainLooper())
    private val engineLatch = CountDownLatch(1)
    private lateinit var wallpaperService: TestExampleCanvasAnalogWatchFaceService

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
    }

    @After
    fun tearDown() {
        if (this::engine.isInitialized) {
            engine.onDestroy()
        }
        service.close()
    }

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface-client")

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

    private val systemState = SystemState(false, 0)

    private val complications = mapOf(
        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(PlainComplicationText.Builder("ID").build())
                .setTitle(PlainComplicationText.Builder("Left").build())
                .build(),
        EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(PlainComplicationText.Builder("ID").build())
                .setTitle(PlainComplicationText.Builder("Right").build())
                .build()
    )

    private fun createEngine() {
        handler.post {
            engine = wallpaperService.onCreateEngine()
            engine.onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
            engineLatch.countDown()
        }
        engineLatch.await(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    }

    @Test
    fun headlessScreenshot() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            ComponentName(
                "androidx.wear.watchface.samples.test",
                "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
            ),
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
                RenderParameters.DRAW_ALL_LAYERS,
                null,
                Color.RED
            ),
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "headlessScreenshot")

        headlessInstance.close()
    }

    @Test
    fun yellowComplicationHighlights() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            ComponentName(
                "androidx.wear.watchface.samples.test",
                "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
            ),
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
                mapOf(
                    Layer.BASE_LAYER to LayerMode.DRAW,
                    Layer.COMPLICATIONS to LayerMode.DRAW_OUTLINED,
                    Layer.TOP_LAYER to LayerMode.DRAW
                ),
                null,
                Color.YELLOW
            ),
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "yellowComplicationHighlights")

        headlessInstance.close()
    }

    @Test
    fun headlessComplicationDetails() {
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
            SystemProviders.DAY_OF_WEEK
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
            SystemProviders.STEP_COUNT
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
    fun headlessUserStyleSchema() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
            deviceConfig,
            400,
            400
        )!!

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(4)
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[0].id).isEqualTo(
            "color_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[1].id).isEqualTo(
            "draw_hour_pips_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[2].id).isEqualTo(
            "watch_hand_length_style_setting"
        )
        assertThat(headlessInstance.userStyleSchema.userStyleSettings[3].id).isEqualTo(
            "complications_style_setting"
        )

        headlessInstance.close()
    }

    @Test
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance =
            runBlocking {
                withTimeout(CONNECT_TIMEOUT_MILLIS) {
                    deferredInteractiveInstance.await()
                }
            }

        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                RenderParameters.DRAW_ALL_LAYERS,
                null,
                Color.RED
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
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_initialStyle() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                // An incomplete map which is OK.
                mapOf(
                    "color_style_setting" to "green_style",
                    "draw_hour_pips_style_setting" to "false",
                    "watch_hand_length_style_setting" to "0.8"
                ),
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance =
            runBlocking {
                withTimeout(CONNECT_TIMEOUT_MILLIS) {
                    deferredInteractiveInstance.await()
                }
            }

        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                RenderParameters.DRAW_ALL_LAYERS,
                null,
                Color.RED
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
    fun wallpaperServiceBackedInteractiveWatchFaceWcsClient_ComplicationDetails() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance =
            runBlocking {
                withTimeout(CONNECT_TIMEOUT_MILLIS) {
                    deferredInteractiveInstance.await()
                }
            }

        interactiveInstance.updateComplicationData(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("Test").build()
                    ).build(),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("Test").build()
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
            SystemProviders.DAY_OF_WEEK
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
            SystemProviders.STEP_COUNT
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
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_existingOpenInstance() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        runBlocking {
            withTimeout(CONNECT_TIMEOUT_MILLIS) {
                deferredInteractiveInstance.await()
            }
        }

        val existingInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        assertTrue(existingInstance.isCompleted)
    }

    @Test
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_existingClosedInstance() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance =
            runBlocking {
                withTimeout(CONNECT_TIMEOUT_MILLIS) {
                    deferredInteractiveInstance.await()
                }
            }

        // Closing this interface means the subsequent
        // getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient won't immediately return
        // a resolved future.
        interactiveInstance.close()

        val deferredExistingInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        assertFalse(deferredExistingInstance.isCompleted)

        // We don't want to leave a pending request or it'll mess up subsequent tests.
        handler.post {
            engine = wallpaperService.onCreateEngine()
            engine.onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
        }
        runBlocking {
            withTimeout(CONNECT_TIMEOUT_MILLIS) {
                deferredExistingInstance.await()
            }
        }
    }

    @Test
    fun getInteractiveWatchFaceInstanceSysUi() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        // Wait for the instance to be created.
        runBlocking {
            withTimeout(CONNECT_TIMEOUT_MILLIS) {
                deferredInteractiveInstance.await()
            }
        }

        val sysUiInterface =
            service.getInteractiveWatchFaceSysUiClientInstance("testId")!!

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
    fun updateInstance() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                mapOf(
                    COLOR_STYLE_SETTING to GREEN_STYLE,
                    WATCH_HAND_LENGTH_STYLE_SETTING to "0.25",
                    DRAW_HOUR_PIPS_STYLE_SETTING to "false",
                    COMPLICATIONS_STYLE_SETTING to NO_COMPLICATIONS
                ),
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
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
            mapOf(
                COLOR_STYLE_SETTING to BLUE_STYLE,
                WATCH_HAND_LENGTH_STYLE_SETTING to "0.9",
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

        // It should be possible to create a SysUI instance with the updated id.
        val sysUiInterface =
            service.getInteractiveWatchFaceSysUiClientInstance("testId2")
        assertThat(sysUiInterface).isNotNull()
        sysUiInterface?.close()

        interactiveInstance.updateComplicationData(complications)
        val bitmap = interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                RenderParameters.DRAW_ALL_LAYERS,
                null,
                Color.RED
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
    fun getComplicationIdAt() {
        val deferredInteractiveInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClientAsync(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance = runBlocking {
            withTimeout(CONNECT_TIMEOUT_MILLIS) {
                deferredInteractiveInstance.await()
            }
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
}

internal class TestExampleCanvasAnalogWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleCanvasAnalogWatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride
}
