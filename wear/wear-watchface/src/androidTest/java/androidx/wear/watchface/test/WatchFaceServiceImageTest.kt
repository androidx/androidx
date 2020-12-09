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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.support.wearable.watchface.SharedMemoryImage
import android.view.Surface
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.LayerMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IPendingInteractiveWatchFaceWCS
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.samples.COLOR_STYLE_SETTING
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.GREEN_STYLE
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.data.UserStyleWireFormat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val BITMAP_WIDTH = 400
private const val BITMAP_HEIGHT = 400
private const val TIMEOUT_MS = 800L

private const val INTERACTIVE_INSTANCE_ID = "InteractiveTestInstance"

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceServiceImageTest {

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder

    private val handler = Handler(Looper.getMainLooper())

    private val complicationProviders = mapOf(
        SystemProviders.DAY_OF_WEEK to
            ShortTextComplicationData.Builder(ComplicationText.plain("Mon"))
                .setTitle(ComplicationText.plain("23rd"))
                .build()
                .asWireComplicationData(),
        SystemProviders.STEP_COUNT to
            ShortTextComplicationData.Builder(ComplicationText.plain("100"))
                .setTitle(ComplicationText.plain("Steps"))
                .build()
                .asWireComplicationData()
    )

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface")

    private val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val renderDoneLatch = CountDownLatch(1)
    private var initLatch = CountDownLatch(1)

    private val surfaceTexture = SurfaceTexture(false)

    private lateinit var canvasAnalogWatchFaceService: TestCanvasAnalogWatchFaceService
    private lateinit var glesWatchFaceService: TestGlesWatchFaceService
    private lateinit var engineWrapper: WatchFaceService.EngineWrapper
    private lateinit var interactiveWatchFaceInstanceWCS: IInteractiveWatchFaceWCS

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @After
    fun shutDown() {
        interactiveWatchFaceInstanceWCS.release()
    }

    private fun initCanvasWatchFace() {
        canvasAnalogWatchFaceService = TestCanvasAnalogWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            surfaceHolder,
            true // Not direct boot.
        )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderDoneLatch.countDown()
        }

        setPendingWallpaperInteractiveWatchFaceInstance()

        engineWrapper =
            canvasAnalogWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
    }

    private fun initGles2WatchFace() {
        glesWatchFaceService = TestGlesWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            surfaceHolder
        )

        surfaceTexture.setDefaultBufferSize(BITMAP_WIDTH, BITMAP_HEIGHT)

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.surface).thenReturn(Surface(surfaceTexture))

        setPendingWallpaperInteractiveWatchFaceInstance()

        engineWrapper = glesWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
    }

    private fun setPendingWallpaperInteractiveWatchFaceInstance() {
        InteractiveInstanceManager
            .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                    WallpaperInteractiveWatchFaceInstanceParams(
                        INTERACTIVE_INSTANCE_ID,
                        DeviceConfig(
                            false,
                            false,
                            0,
                            0
                        ),
                        SystemState(false, 0),
                        UserStyleWireFormat(emptyMap()),
                        null
                    ),
                    object : IPendingInteractiveWatchFaceWCS.Stub() {
                        override fun getApiVersion() =
                            IPendingInteractiveWatchFaceWCS.API_VERSION

                        override fun onInteractiveWatchFaceWcsCreated(
                            iInteractiveWatchFaceWcs: IInteractiveWatchFaceWCS
                        ) {
                            interactiveWatchFaceInstanceWCS = iInteractiveWatchFaceWcs
                            sendComplications()
                            initLatch.countDown()
                        }
                    }
                )
            )
    }

    private fun sendComplications() {
        interactiveWatchFaceInstanceWCS.updateComplicationData(
            interactiveWatchFaceInstanceWCS.complicationDetails.map {
                IdAndComplicationDataWireFormat(
                    it.id,
                    complicationProviders[it.complicationState.fallbackSystemProvider]!!
                )
            }
        )
    }

    private fun setAmbient(ambient: Boolean) {
        val interactiveWatchFaceInstanceSysUi =
            InteractiveInstanceManager.getAndRetainInstance(
                interactiveWatchFaceInstanceWCS.instanceId
            )!!.createSysUiApi()

        interactiveWatchFaceInstanceSysUi.setSystemState(SystemState(ambient, 0))
        interactiveWatchFaceInstanceSysUi.release()
    }

    @Test
    fun testActiveScreenshot() {
        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        handler.post {
            engineWrapper.draw()
        }

        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap.assertAgainstGolden(screenshotRule, "active_screenshot")
    }

    @Test
    fun testAmbientScreenshot() {
        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        handler.post {
            setAmbient(true)
            engineWrapper.draw()
        }

        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap.assertAgainstGolden(screenshotRule, "ambient_screenshot2")
    }

    @Test
    fun testCommandTakeScreenShot() {
        val latch = CountDownLatch(1)
        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
                interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                    WatchfaceScreenshotParams(
                        RenderParameters(
                            DrawMode.AMBIENT,
                            RenderParameters.DRAW_ALL_LAYERS,
                            null
                        ).toWireFormat(),
                        100,
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "ambient_screenshot"
        )
    }

    @Test
    fun testCommandTakeOpenGLScreenShot() {
        val latch = CountDownLatch(1)

        handler.post(this::initGles2WatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
                interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                    WatchfaceScreenshotParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            RenderParameters.DRAW_ALL_LAYERS,
                            null
                        ).toWireFormat(),
                        100,
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "ambient_gl_screenshot"
        )
    }

    @Test
    fun testSetGreenStyle() {
        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        handler.post {
            interactiveWatchFaceInstanceWCS.setCurrentUserStyle(
                UserStyleWireFormat(mapOf(COLOR_STYLE_SETTING to GREEN_STYLE))
            )
            engineWrapper.draw()
        }

        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap.assertAgainstGolden(screenshotRule, "green_screenshot")
    }

    @Test
    fun testHighlightAllComplicationsInScreenshot() {
        val latch = CountDownLatch(1)

        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
                interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                    WatchfaceScreenshotParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            mapOf(
                                Layer.BASE_LAYER to LayerMode.DRAW,
                                Layer.COMPLICATIONS to LayerMode.DRAW_HIGHLIGHTED,
                                Layer.TOP_LAYER to LayerMode.DRAW
                            ),
                            null
                        ).toWireFormat(),
                        100,
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "highlight_complications"
        )
    }

    @Test
    fun testHighlightRightComplicationInScreenshot() {
        val latch = CountDownLatch(1)

        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
                interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                    WatchfaceScreenshotParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            mapOf(
                                Layer.BASE_LAYER to LayerMode.DRAW,
                                Layer.COMPLICATIONS to LayerMode.DRAW_HIGHLIGHTED,
                                Layer.TOP_LAYER to LayerMode.DRAW
                            ),
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
                        ).toWireFormat(),
                        100,
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "highlight_right_complication"
        )
    }

    @Test
    fun testScreenshotWithPreviewComplicationData() {
        val latch = CountDownLatch(1)
        val previewComplicationData = listOf(
            IdAndComplicationDataWireFormat(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                ShortTextComplicationData.Builder(ComplicationText.plain("A"))
                    .setTitle(ComplicationText.plain("Preview"))
                    .build()
                    .asWireComplicationData()
            ),
            IdAndComplicationDataWireFormat(
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                ShortTextComplicationData.Builder(ComplicationText.plain("B"))
                    .setTitle(ComplicationText.plain("Preview"))
                    .build()
                    .asWireComplicationData()
            )
        )

        handler.post(this::initCanvasWatchFace)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
                interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                    WatchfaceScreenshotParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            RenderParameters.DRAW_ALL_LAYERS,
                            null
                        ).toWireFormat(),
                        100,
                        123456789,
                        null,
                        previewComplicationData
                    )
                )
            )
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "preview_complications"
        )
    }

    @Test
    fun directBoot() {
        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        handler.post {
            // Change the style
            interactiveWatchFaceInstanceWCS.setCurrentUserStyle(
                UserStyleWireFormat(mapOf(COLOR_STYLE_SETTING to GREEN_STYLE))
            )

            // Simulate device shutting down.
            InteractiveInstanceManager.deleteInstance(INTERACTIVE_INSTANCE_ID)

            // Simulate a direct boot scenario where a new service is created with a locked user
            // but there's no pending PendingWallpaperInteractiveWatchFaceInstance and no
            // wallpaper command. This should load the direct boot parameters which get saved.
            val service2 = TestCanvasAnalogWatchFaceService(
                ApplicationProvider.getApplicationContext<Context>(),
                handler,
                100000,
                surfaceHolder,
                false // Direct boot.
            )

            val engineWrapper2 = service2.onCreateEngine() as WatchFaceService.EngineWrapper
            engineWrapper2.draw()
        }

        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap.assertAgainstGolden(screenshotRule, "direct_boot")
    }
}
