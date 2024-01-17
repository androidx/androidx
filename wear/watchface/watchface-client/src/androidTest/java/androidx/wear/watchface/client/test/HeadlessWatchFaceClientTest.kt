/*
 * Copyright 2022 The Android Open Source Project
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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.client.test.TestServicesHelpers.componentOf
import androidx.wear.watchface.client.test.TestServicesHelpers.createTestComplications
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleOpenGLBackgroundInitWatchFaceService
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresApi(Build.VERSION_CODES.O_MR1)
abstract class HeadlessWatchFaceClientTestBase {
    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val service = runBlocking {
        WatchFaceControlClient.createWatchFaceControlClientImpl(
            context,
            Intent(context, WatchFaceControlTestService::class.java).apply {
                action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
            },
            resourceOnlyWatchFacePackageName = null
        )
    }

    protected fun createHeadlessWatchFaceClient(
        componentName: ComponentName = exampleCanvasAnalogWatchFaceComponentName
    ): HeadlessWatchFaceClient {
        return service.createHeadlessWatchFaceClient("id", componentName, deviceConfig, 400, 400)!!
    }

    protected val exampleCanvasAnalogWatchFaceComponentName =
        componentOf<ExampleCanvasAnalogWatchFaceService>()

    protected val exampleOpenGLWatchFaceComponentName =
        componentOf<ExampleOpenGLBackgroundInitWatchFaceService>()

    protected val deviceConfig =
        DeviceConfig(
            hasLowBitAmbient = false,
            hasBurnInProtection = false,
            analogPreviewReferenceTimeMillis = 0,
            digitalPreviewReferenceTimeMillis = 0
        )
}

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
class HeadlessWatchFaceClientTest : HeadlessWatchFaceClientTestBase() {
    @Suppress("DEPRECATION", "NewApi") // defaultDataSourceType
    @Test
    fun headlessComplicationDetails() {
        val headlessInstance = createHeadlessWatchFaceClient()

        Truth.assertThat(headlessInstance.complicationSlotsState.size).isEqualTo(2)

        val leftComplicationDetails =
            headlessInstance.complicationSlotsState[EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID]!!
        Truth.assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        Truth.assertThat(leftComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        Truth.assertThat(leftComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback)
            .isEqualTo(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK)
        Truth.assertThat(leftComplicationDetails.defaultDataSourceType)
            .isEqualTo(ComplicationType.SHORT_TEXT)
        Truth.assertThat(leftComplicationDetails.supportedTypes)
            .containsExactly(
                ComplicationType.RANGED_VALUE,
                ComplicationType.GOAL_PROGRESS,
                ComplicationType.WEIGHTED_ELEMENTS,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            )
        Assert.assertTrue(leftComplicationDetails.isEnabled)

        val rightComplicationDetails =
            headlessInstance.complicationSlotsState[
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID]!!
        Truth.assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        Truth.assertThat(rightComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        Truth.assertThat(rightComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback)
            .isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
        Truth.assertThat(rightComplicationDetails.defaultDataSourceType)
            .isEqualTo(ComplicationType.SHORT_TEXT)
        Truth.assertThat(rightComplicationDetails.supportedTypes)
            .containsExactly(
                ComplicationType.RANGED_VALUE,
                ComplicationType.GOAL_PROGRESS,
                ComplicationType.WEIGHTED_ELEMENTS,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            )

        Truth.assertThat(rightComplicationDetails.isEnabled).isTrue()

        headlessInstance.close()
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    fun headlessUserStyleSchema() {
        val headlessInstance = createHeadlessWatchFaceClient()

        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(5)
        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings[0].id.value)
            .isEqualTo("color_style_setting")
        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings[1].id.value)
            .isEqualTo("draw_hour_pips_style_setting")
        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings[2].id.value)
            .isEqualTo("watch_hand_length_style_setting")
        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings[3].id.value)
            .isEqualTo("complications_style_setting")
        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings[4].id.value)
            .isEqualTo("hours_draw_freq_style_setting")

        headlessInstance.close()
    }

    @Test
    fun headlessUserStyleFlavors() {
        val headlessInstance = createHeadlessWatchFaceClient()

        Truth.assertThat(headlessInstance.getUserStyleFlavors().flavors.size).isEqualTo(1)
        val flavorA = headlessInstance.getUserStyleFlavors().flavors[0]
        Truth.assertThat(flavorA.id).isEqualTo("exampleFlavor")
        Truth.assertThat(flavorA.style.userStyleMap.containsKey("color_style_setting"))
        Truth.assertThat(flavorA.style.userStyleMap.containsKey("watch_hand_length_style_setting"))
        Truth.assertThat(
            flavorA.complications.containsKey(EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)
        )
        Truth.assertThat(
            flavorA.complications.containsKey(EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID)
        )

        headlessInstance.close()
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    fun headlessToBundleAndCreateFromBundle() {
        val headlessInstance =
            HeadlessWatchFaceClient.createFromBundle(
                service
                    .createHeadlessWatchFaceClient(
                        "id",
                        exampleCanvasAnalogWatchFaceComponentName,
                        deviceConfig,
                        400,
                        400
                    )!!
                    .toBundle()
            )

        Truth.assertThat(headlessInstance.userStyleSchema.userStyleSettings.size).isEqualTo(5)
    }

    @Test
    fun computeUserStyleSchemaDigestHash() {
        val headlessInstance1 =
            createHeadlessWatchFaceClient(exampleCanvasAnalogWatchFaceComponentName)

        val headlessInstance2 = createHeadlessWatchFaceClient(exampleOpenGLWatchFaceComponentName)

        Truth.assertThat(headlessInstance1.getUserStyleSchemaDigestHash())
            .isNotEqualTo(headlessInstance2.getUserStyleSchemaDigestHash())
    }

    @Test
    fun headlessLifeCycle() {
        val headlessInstance =
            createHeadlessWatchFaceClient(componentOf<TestLifeCycleWatchFaceService>())

        // Blocks until the headless instance has been fully constructed.
        headlessInstance.previewReferenceInstant
        headlessInstance.close()

        Truth.assertThat(TestLifeCycleWatchFaceService.lifeCycleEvents)
            .containsExactly(
                "WatchFaceService.onCreate",
                "Renderer.constructed",
                "Renderer.onDestroy",
                "WatchFaceService.onDestroy"
            )
    }
}

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
class HeadlessWatchFaceClientScreenshotTest : HeadlessWatchFaceClientTestBase() {
    @get:Rule
    val screenshotRule: AndroidXScreenshotTestRule =
        AndroidXScreenshotTestRule("wear/wear-watchface-client")

    private val complications = createTestComplications(context)

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun headlessScreenshot() {
        val headlessInstance = createHeadlessWatchFaceClient()

        val bitmap =
            headlessInstance.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
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
        val headlessInstance = createHeadlessWatchFaceClient()

        val bitmap =
            headlessInstance.renderWatchFaceToBitmap(
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
        val headlessInstance = createHeadlessWatchFaceClient()

        val bitmap =
            headlessInstance.renderWatchFaceToBitmap(
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
}
