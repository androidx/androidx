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
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.support.wearable.watchface.SharedMemoryImage
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
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
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

// This service constructs a WatchFace with a task that's posted on the UI thread.
internal class AsyncInitWithUiThreadTaskWatchFace : WatchFaceService() {
    private val mainThreadCoroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace = withContext(mainThreadCoroutineScope.coroutineContext) {
        WatchFace(
            WatchFaceType.DIGITAL,
            object : Renderer.CanvasRenderer(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.SOFTWARE,
                16
            ) {
                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {}
            }
        )
    }
}

@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.O_MR1)
@MediumTest
public class WatchFaceControlServiceTest {

    @get:Rule
    internal val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface")

    @Before
    public fun setUp() {
        Assume.assumeTrue("This test suite assumes API 27", Build.VERSION.SDK_INT >= 27)
    }

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
    public fun createHeadlessWatchFaceInstance() {
        val instance = createInstance(100, 100)
        val bitmap = SharedMemoryImage.ashmemReadImageBundle(
            instance.renderWatchFaceToBitmap(
                WatchFaceRenderParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                        null
                    ).toWireFormat(),
                    1234567890,
                    null,
                    listOf(
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("Mon").build(),
                                ComplicationText.EMPTY
                            )
                                .setTitle(PlainComplicationText.Builder("23rd").build())
                                .build()
                                .asWireComplicationData()
                        ),
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("100").build(),
                                ComplicationText.EMPTY
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
    public fun createHeadlessOpenglWatchFaceInstance() {
        val instance = createOpenGlInstance(400, 400)
        val bitmap = SharedMemoryImage.ashmemReadImageBundle(
            instance.renderWatchFaceToBitmap(
                WatchFaceRenderParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                        null
                    ).toWireFormat(),
                    1234567890,
                    null,
                    listOf(
                        IdAndComplicationDataWireFormat(
                            EXAMPLE_OPENGL_COMPLICATION_ID,
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("Mon").build(),
                                ComplicationText.EMPTY
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
    public fun testCommandTakeComplicationScreenShot() {
        val instance = createInstance(400, 400)
        val bitmap = SharedMemoryImage.ashmemReadImageBundle(
            instance.renderComplicationToBitmap(
                ComplicationRenderParams(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                        null,
                    ).toWireFormat(),
                    123456789,
                    ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("Mon").build(),
                        ComplicationText.EMPTY
                    )
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

    @Test
    public fun asyncInitWithUiThreadTaskWatchFace() {
        val instanceService = IWatchFaceControlService.Stub.asInterface(
            WatchFaceControlService().apply {
                setContext(ApplicationProvider.getApplicationContext<Context>())
            }.onBind(
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE)
            )
        )
        // This shouldn't hang.
        val headlessInstance = instanceService.createHeadlessWatchFaceInstance(
            HeadlessWatchFaceInstanceParams(
                ComponentName(
                    ApplicationProvider.getApplicationContext<Context>(),
                    AsyncInitWithUiThreadTaskWatchFace::class.java
                ),
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                100,
                100
            )
        )

        assertThat(headlessInstance.userStyleSchema.mSchema).isEmpty()
    }
}
