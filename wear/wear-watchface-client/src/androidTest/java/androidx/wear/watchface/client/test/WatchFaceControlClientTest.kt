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
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.SystemState
import androidx.wear.watchface.client.WatchFaceControlClientImpl
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
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertFalse
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
    private val service = WatchFaceControlClientImpl(
        context,
        Intent(context, WatchFaceControlTestService::class.java).apply {
            action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
        }
    )

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var engine: WallpaperService.Engine
    private val handler = Handler(Looper.getMainLooper())
    private val engineLatch = CountDownLatch(1)
    private lateinit var wallpaperService: TestExampleCanvasAnalogWatchFaceService

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
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
            ShortTextComplicationData.Builder(ComplicationText.plain("ID"))
                .setTitle(ComplicationText.plain("Left"))
                .build(),
        EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(ComplicationText.plain("ID"))
                .setTitle(ComplicationText.plain("Right"))
                .build()
    )

    private fun createEngine() {
        handler.post {
            engine = wallpaperService.onCreateEngine()
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
        ).get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!
        val bitmap = headlessInstance.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "headlessScreenshot")

        headlessInstance.close()
    }

    @Test
    fun headlessComplicationDetails() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
            deviceConfig,
            400,
            400
        ).get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        assertThat(headlessInstance.complicationState.size).isEqualTo(2)

        val leftComplicationDetails = headlessInstance.complicationState[
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

        val rightComplicationDetails = headlessInstance.complicationState[
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
        ).get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

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
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        val bitmap = interactiveInstance.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
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
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
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

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        val bitmap = interactiveInstance.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
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
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        assertThat(interactiveInstance.complicationState.size).isEqualTo(2)

        val leftComplicationDetails = interactiveInstance.complicationState[
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

        val rightComplicationDetails = interactiveInstance.complicationState[
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

        interactiveInstance.close()
    }

    @Test
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_existingOpenInstance() {
        service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
            "testId",
            deviceConfig,
            systemState,
            null,
            complications
        )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        val existingInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        assertTrue(existingInstance.isDone)
    }

    @Test
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_existingClosedInstance() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        // Closing this interface means the subsequent
        // getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient won't immediately return
        // a resolved future.
        interactiveInstance.close()

        val existingInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        assertFalse(existingInstance.isDone)

        // We don't want to leave a pending request or it'll mess up subsequent tests.
        handler.post { engine = wallpaperService.onCreateEngine() }
        existingInstance.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!
    }

    @Test
    fun getInteractiveWatchFaceInstanceSysUi() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        // Wait for the instance to be created.
        interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        val sysUiInterface = service.getInteractiveWatchFaceSysUiClientInstance("testId")
            .get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

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
    }

    @Test
    fun setUserStyle() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
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

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        createEngine()

        // Wait for the instance to be created.
        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        // Note this map doesn't include all the categories, which is fine the others will be set
        // to their defaults.
        interactiveInstance.setUserStyle(
            mapOf(
                COLOR_STYLE_SETTING to BLUE_STYLE,
                WATCH_HAND_LENGTH_STYLE_SETTING to "0.9",
            )
        )

        val bitmap = interactiveInstance.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
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
