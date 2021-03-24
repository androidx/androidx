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
import android.graphics.Color
import android.support.wearable.watchface.SharedMemoryImage
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IHeadlessWatchFace
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_OPENGL_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.ExampleOpenGLWatchFaceService
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

    private fun createOpenGlInstance(width: Int, height: Int): IHeadlessWatchFace {
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
                    ExampleOpenGLWatchFaceService::class.java
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
        val bitmap = SharedMemoryImage.ashmemReadImageBundle(
            instance.renderWatchFaceToBitmap(
                WatchFaceRenderParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        RenderParameters.DRAW_ALL_LAYERS,
                        null,
                        Color.RED
                    ).toWireFormat(),
                    1234567890,
                    null,
                    listOf(
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("Mon").build()
                            )
                                .setTitle(PlainComplicationText.Builder("23rd").build())
                                .build()
                                .asWireComplicationData()
                        ),
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("100").build()
                            )
                                .setTitle(PlainComplicationText.Builder("Steps").build())
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
    fun createHeadlessOpenglWatchFaceInstance() {
        val instance = createOpenGlInstance(400, 400)
        val bitmap = SharedMemoryImage.ashmemReadImageBundle(
            instance.renderWatchFaceToBitmap(
                WatchFaceRenderParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        RenderParameters.DRAW_ALL_LAYERS,
                        null,
                        Color.RED
                    ).toWireFormat(),
                    1234567890,
                    null,
                    listOf(
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_OPENGL_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("Mon").build()
                            )
                                .setTitle(PlainComplicationText.Builder("23rd").build())
                                .build()
                                .asWireComplicationData()
                        )
                    )
                )
            )
        )

        bitmap.assertAgainstGolden(screenshotRule, "opengl_headless")

        instance.release()
    }

    @Test
    fun testCommandTakeComplicationScreenShot() {
        val instance = createInstance(400, 400)
        val bitmap = SharedMemoryImage.ashmemReadImageBundle(
            instance.renderComplicationToBitmap(
                ComplicationRenderParams(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                    RenderParameters(
                        DrawMode.AMBIENT,
                        RenderParameters.DRAW_ALL_LAYERS,
                        null,
                        Color.RED
                    ).toWireFormat(),
                    123456789,
                    ShortTextComplicationData.Builder(PlainComplicationText.Builder("Mon").build())
                        .setTitle(PlainComplicationText.Builder("23rd").build())
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
