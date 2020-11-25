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

package androidx.wear.watchface.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.wearable.watchface.SharedMemoryImage
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IHeadlessWatchFace
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.ComplicationScreenshotParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val API_VERSION = 3

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceControlServiceTest {

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface")

    private fun createInstance(width: Int, height: Int): IHeadlessWatchFace {
        val instanceService = IWatchFaceControlService.Stub.asInterface(
            WatchFaceControlService().apply {
                setContext(ApplicationProvider.getApplicationContext<Context>())
            }.onBind(
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE)
            )
        )
        return instanceService.createHeadlessWatchFaceInstance(
            HeadlessWatchFaceInstanceParams(
                ComponentName(
                    ApplicationProvider.getApplicationContext<Context>(),
                    ExampleCanvasAnalogWatchFaceService::class.java
                ),
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                width,
                height
            )
        )
    }

    @Test
    fun createHeadlessWatchFaceInstance() {
        val instance = createInstance(100, 100)
        val bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
            instance.takeWatchFaceScreenshot(
                WatchfaceScreenshotParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        RenderParameters.DRAW_ALL_LAYERS,
                        null
                    ).toWireFormat(),
                    100,
                    1234567890,
                    null,
                    listOf(
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(ComplicationText.plain("Mon"))
                                .setTitle(ComplicationText.plain("23rd"))
                                .build()
                                .asWireComplicationData()
                        ),
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(ComplicationText.plain("100"))
                                .setTitle(ComplicationText.plain("Steps"))
                                .build()
                                .asWireComplicationData()
                        )
                    )
                )
            )
        )

        bitmap.assertAgainstGolden(screenshotRule, "service_interactive")

        instance.release()
    }

    @Test
    fun testCommandTakeComplicationScreenShot() {
        val instance = createInstance(400, 400)
        val bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
            instance.takeComplicationScreenshot(
                ComplicationScreenshotParams(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                    RenderParameters(
                        DrawMode.AMBIENT,
                        RenderParameters.DRAW_ALL_LAYERS,
                        null
                    ).toWireFormat(),
                    100,
                    123456789,
                    ShortTextComplicationData.Builder(ComplicationText.plain("Mon"))
                        .setTitle(ComplicationText.plain("23rd"))
                        .build()
                        .asWireComplicationData(),
                    null
                )
            )
        )

        bitmap.assertAgainstGolden(
            screenshotRule,
            "leftComplication"
        )

        instance.release()
    }
}
