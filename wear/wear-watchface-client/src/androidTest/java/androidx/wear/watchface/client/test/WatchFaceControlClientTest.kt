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
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_MILLIS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceControlClientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val service = WatchFaceControlClient(
        context,
        Intent(context, WatchFaceControlTestService::class.java).apply {
            action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
        }
    )

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface-client")

    private val exampleWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.samples.test",
        "androidx.wear.watchface.samples.ExampleCanvasWatchFaceService"
    )

    private val deviceConfig = DeviceConfig(
        false,
        false,
        1,
        0,
        0
    )

    @Test
    fun headlessScreenshot() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            ComponentName(
                "androidx.wear.watchface.samples.test",
                "androidx.wear.watchface.samples.ExampleCanvasWatchFaceService"
            ),
            DeviceConfig(
                false,
                false,
                1,
                0,
                0
            ),
            400,
            400
        ).get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val bitmap = headlessInstance!!.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
            1234567,
            null,
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    ShortTextComplicationData.Builder(ComplicationText.plain("ID"))
                        .setTitle(ComplicationText.plain("Left"))
                        .build(),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    ShortTextComplicationData.Builder(ComplicationText.plain("ID"))
                        .setTitle(ComplicationText.plain("Right"))
                        .build()
            )
        )

        bitmap.assertAgainstGolden(screenshotRule, "headlessScreenshot")

        headlessInstance.close()
        service.close()
    }

    @Test
    fun complicationDetails() {
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
        service.close()
    }
}
