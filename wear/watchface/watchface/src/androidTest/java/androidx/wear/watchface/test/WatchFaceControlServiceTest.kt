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
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.support.wearable.watchface.SharedMemoryImage
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.control.IHeadlessWatchFace
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleOpenGLWatchFaceService
import androidx.wear.watchface.samples.ExampleOpenGLWatchFaceService.Companion.EXAMPLE_OPENGL_COMPLICATION_ID
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// This service constructs a WatchFace with a task that's posted on the UI thread.
internal class AsyncInitWithUiThreadTaskWatchFace : WatchFaceService() {
    private val mainThreadCoroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace =
        withContext(mainThreadCoroutineScope.coroutineContext) {
            WatchFace(
                WatchFaceType.DIGITAL,
                @Suppress("deprecation")
                object :
                    Renderer.CanvasRenderer(
                        surfaceHolder,
                        currentUserStyleRepository,
                        watchState,
                        CanvasType.SOFTWARE,
                        16
                    ) {
                    override fun render(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime
                    ) {}

                    override fun renderHighlightLayer(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime
                    ) {}
                }
            )
        }
}

const val TIME_MILLIS: Long = 123456789
val DEVICE_CONFIG =
    DeviceConfig(
        /* hasLowBitAmbient = */ false,
        /* hasBurnInProtection = */ false,
        /* analogPreviewReferenceTimeMillis = */ 0,
        /* digitalPreviewReferenceTimeMillis = */ 0
    )

@SdkSuppress(maxSdkVersion = 32) // b/271922712
@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.O_MR1)
@MediumTest
public class WatchFaceControlServiceTest {

    @get:Rule internal val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface")

    private lateinit var instance: IHeadlessWatchFace

    @Before
    public fun setUp() {
        Assume.assumeTrue("This test suite assumes API 27", Build.VERSION.SDK_INT >= 27)
    }

    @After
    public fun tearDown() {
        if (this::instance.isInitialized) {
            instance.release()
        }
        InteractiveInstanceManager.setParameterlessEngine(null)
    }

    private fun createInstance(width: Int, height: Int) {
        val instanceService =
            IWatchFaceControlService.Stub.asInterface(
                WatchFaceControlService()
                    .apply { setContext(ApplicationProvider.getApplicationContext<Context>()) }
                    .onBind(Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE))
            )
        instance =
            instanceService.createHeadlessWatchFaceInstance(
                HeadlessWatchFaceInstanceParams(
                    ComponentName(
                        ApplicationProvider.getApplicationContext<Context>(),
                        ExampleCanvasAnalogWatchFaceService::class.java
                    ),
                    DEVICE_CONFIG,
                    width,
                    height,
                    null
                )
            )
    }

    private fun createOpenGlInstance(width: Int, height: Int) {
        val instanceService =
            IWatchFaceControlService.Stub.asInterface(
                WatchFaceControlService()
                    .apply { setContext(ApplicationProvider.getApplicationContext<Context>()) }
                    .onBind(Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE))
            )
        instance =
            instanceService.createHeadlessWatchFaceInstance(
                HeadlessWatchFaceInstanceParams(
                    ComponentName(
                        ApplicationProvider.getApplicationContext<Context>(),
                        ExampleOpenGLWatchFaceService::class.java
                    ),
                    DEVICE_CONFIG,
                    width,
                    height,
                    null
                )
            )
    }

    @Test
    public fun createWatchFaceInstanceWithRangedValueComplications() {
        createInstance(width = 400, height = 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
                        null,
                        listOf(
                            IdAndComplicationDataWireFormat(
                                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                                RangedValueComplicationData.Builder(
                                        value = 100.0f,
                                        min = 0.0f,
                                        max = 100.0f,
                                        ComplicationText.EMPTY
                                    )
                                    .setText(PlainComplicationText.Builder("100%").build())
                                    .build()
                                    .asWireComplicationData()
                            ),
                            IdAndComplicationDataWireFormat(
                                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                                RangedValueComplicationData.Builder(
                                        value = 75.0f,
                                        min = 0.0f,
                                        max = 100.0f,
                                        ComplicationText.EMPTY
                                    )
                                    .setText(PlainComplicationText.Builder("75%").build())
                                    .build()
                                    .asWireComplicationData()
                            )
                        )
                    )
                )
            )

        bitmap.assertAgainstGolden(screenshotRule, "ranged_value_complications")
    }

    @Test
    public fun createHeadlessWatchFaceInstance() {
        createInstance(width = 100, height = 100)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
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
    }

    @Test
    public fun createHeadlessOpenglWatchFaceInstance() {
        createOpenGlInstance(width = 400, height = 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
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
    }

    @Test
    public fun testCommandTakeComplicationScreenShot() {
        createInstance(width = 400, height = 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderComplicationToBitmap(
                    ComplicationRenderParams(
                        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null,
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
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

        bitmap.assertAgainstGolden(screenshotRule, "leftComplication")
    }

    @Test
    @Suppress("NewApi")
    public fun testGoalProgressComplication() {
        createInstance(width = 400, height = 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderComplicationToBitmap(
                    ComplicationRenderParams(
                        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null,
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
                        GoalProgressComplicationData.Builder(
                                value = 12345.0f,
                                targetValue = 10000.0f,
                                PlainComplicationText.Builder("12345 steps").build()
                            )
                            .setText(PlainComplicationText.Builder("12345").build())
                            .setTitle(PlainComplicationText.Builder("Steps").build())
                            .build()
                            .asWireComplicationData(),
                        null
                    )
                )
            )

        bitmap.assertAgainstGolden(screenshotRule, "goalProgressComplication")
    }

    @Test
    public fun testColorRampRangedValueComplication() {
        createInstance(400, 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderComplicationToBitmap(
                    ComplicationRenderParams(
                        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null,
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
                        RangedValueComplicationData.Builder(
                                value = 75f,
                                min = 0.0f,
                                max = 100.0f,
                                PlainComplicationText.Builder("Rainbow colors").build()
                            )
                            .setText(PlainComplicationText.Builder("Colors").build())
                            .setValueType(RangedValueComplicationData.TYPE_RATING)
                            .setColorRamp(
                                ColorRamp(
                                    intArrayOf(
                                        Color.GREEN,
                                        Color.YELLOW,
                                        Color.argb(255, 255, 255, 0),
                                        Color.RED,
                                        Color.argb(255, 255, 0, 255),
                                        Color.argb(255, 92, 64, 51)
                                    ),
                                    interpolated = true
                                )
                            )
                            .build()
                            .asWireComplicationData(),
                        null
                    )
                )
            )

        bitmap.assertAgainstGolden(screenshotRule, "colorRampRangedValueComplication")
    }

    @Test
    public fun testNonInterpolatedColorRampRangedValueComplication() {
        createInstance(width = 400, height = 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderComplicationToBitmap(
                    ComplicationRenderParams(
                        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null,
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
                        RangedValueComplicationData.Builder(
                                value = 75f,
                                min = 0.0f,
                                max = 100.0f,
                                PlainComplicationText.Builder("Rainbow colors").build()
                            )
                            .setText(PlainComplicationText.Builder("Colors").build())
                            .setValueType(RangedValueComplicationData.TYPE_RATING)
                            .setColorRamp(
                                ColorRamp(
                                    intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
                                    interpolated = false
                                )
                            )
                            .build()
                            .asWireComplicationData(),
                        null
                    )
                )
            )

        bitmap.assertAgainstGolden(
            screenshotRule,
            "nonInterpolatedColorRampRangedValueComplication"
        )
    }

    @Test
    @Suppress("NewApi")
    public fun testWeightedElementComplication() {
        createInstance(width = 400, height = 400)
        val bitmap =
            SharedMemoryImage.ashmemReadImageBundle(
                instance.renderComplicationToBitmap(
                    ComplicationRenderParams(
                        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                        RenderParameters(
                                DrawMode.INTERACTIVE,
                                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                                null,
                            )
                            .toWireFormat(),
                        TIME_MILLIS,
                        WeightedElementsComplicationData.Builder(
                                listOf(
                                    WeightedElementsComplicationData.Element(
                                        weight = 1.0f,
                                        Color.RED
                                    ),
                                    WeightedElementsComplicationData.Element(
                                        weight = 1.0f,
                                        Color.GREEN
                                    ),
                                    WeightedElementsComplicationData.Element(
                                        weight = 2.0f,
                                        Color.BLUE
                                    ),
                                    WeightedElementsComplicationData.Element(
                                        weight = 3.0f,
                                        Color.YELLOW
                                    )
                                ),
                                PlainComplicationText.Builder("Example").build()
                            )
                            .setText(PlainComplicationText.Builder("Calories").build())
                            .build()
                            .asWireComplicationData(),
                        null
                    )
                )
            )

        bitmap.assertAgainstGolden(screenshotRule, "weightedElementComplication")
    }

    @Test
    public fun asyncInitWithUiThreadTaskWatchFace() {
        val instanceService =
            IWatchFaceControlService.Stub.asInterface(
                WatchFaceControlService()
                    .apply { setContext(ApplicationProvider.getApplicationContext<Context>()) }
                    .onBind(Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE))
            )
        // This shouldn't hang.
        instance =
            instanceService.createHeadlessWatchFaceInstance(
                HeadlessWatchFaceInstanceParams(
                    ComponentName(
                        ApplicationProvider.getApplicationContext<Context>(),
                        AsyncInitWithUiThreadTaskWatchFace::class.java
                    ),
                    DEVICE_CONFIG,
                    /* width = */ 100,
                    /* height = */ 100,
                    /* instanceId = */ null
                )
            )

        assertThat(instance.userStyleSchema.mSchema).isEmpty()
    }

    @Test
    public fun createWatchFaceService_throwsOnInvalidClass() {
        assertThat(
                WatchFaceControlService()
                    .createWatchFaceService(
                        ComponentName(
                            ApplicationProvider.getApplicationContext(),
                            WatchFaceControlServiceTest::class.java
                        )
                    )
            )
            .isNull()
    }
}
