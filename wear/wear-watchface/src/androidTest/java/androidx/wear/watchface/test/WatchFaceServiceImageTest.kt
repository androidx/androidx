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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.ashmemCompressedImageBundleToBitmap
import android.view.Surface
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.LayerMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IWallpaperWatchFaceControlService
import androidx.wear.watchface.control.IWallpaperWatchFaceControlServiceRequest
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.IdAndComplicationData
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
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

private const val API_VERSION = 3
private const val BITMAP_WIDTH = 400
private const val BITMAP_HEIGHT = 400
private const val TIMEOUT_MS = 800L

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceServiceImageTest {

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder

    private val handler = Handler(Looper.getMainLooper())

    private val complicationProviders = mapOf(
        SystemProviders.DAY_OF_WEEK to
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("23rd"))
                .setShortText(ComplicationText.plainText("Mon"))
                .build(),
        SystemProviders.STEP_COUNT to
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("Steps"))
                .setShortText(ComplicationText.plainText("100"))
                .build()
    )

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface")

    private val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val renderDoneLatch = CountDownLatch(1)
    private var initLatch = CountDownLatch(1)

    private val surfaceTexture = SurfaceTexture(false)

    private lateinit var canvasWatchFaceService: TestCanvasWatchFaceService
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
        canvasWatchFaceService = TestCanvasWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            surfaceHolder
        )

        engineWrapper = canvasWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderDoneLatch.countDown()
        }

        setBinderAndSendComplicationData(
            WallpaperInteractiveWatchFaceInstanceParams(
                "InteractiveTestInstance",
                DeviceConfig(
                    false,
                    false,
                    DeviceConfig.SCREEN_SHAPE_ROUND
                ),
                SystemState(false, 0),
                null,
                null
            )
        )
    }

    private fun initGles2WatchFace() {
        glesWatchFaceService = TestGlesWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            surfaceHolder
        )
        engineWrapper = glesWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper

        surfaceTexture.setDefaultBufferSize(BITMAP_WIDTH, BITMAP_HEIGHT)

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.surface).thenReturn(Surface(surfaceTexture))

        setBinderAndSendComplicationData(
            WallpaperInteractiveWatchFaceInstanceParams(
                "InteractiveTestInstance",
                DeviceConfig(
                    false,
                    false,
                    DeviceConfig.SCREEN_SHAPE_ROUND
                ),
                SystemState(false, 0),
                null,
                null
            )
        )
    }

    private fun setBinderAndSendComplicationData(
        wallpaperInteractiveWatchFaceInstanceParams: WallpaperInteractiveWatchFaceInstanceParams
    ) {
        val serviceRequest = object : IWallpaperWatchFaceControlServiceRequest.Stub() {
            override fun getApiVersion() = IWallpaperWatchFaceControlServiceRequest.API_VERSION

            override fun registerWallpaperWatchFaceControlService(
                service: IWallpaperWatchFaceControlService
            ) {
                interactiveWatchFaceInstanceWCS = service.createInteractiveWatchFaceInstance(
                    wallpaperInteractiveWatchFaceInstanceParams
                )
                sendComplications()
                initLatch.countDown()
            }
        }

        engineWrapper.onCommand(
            Constants.COMMAND_BIND_WALLPAPER_WATCH_FACE_CONTROL_SERVICE_REQUEST,
            0,
            0,
            0,
            Bundle().apply { putBinder(Constants.EXTRA_BINDER, serviceRequest.asBinder()) },
            false
        )
    }

    private fun sendComplications() {
        interactiveWatchFaceInstanceWCS.updateComplicationData(
            interactiveWatchFaceInstanceWCS.complicationDetails.map {
                IdAndComplicationData(
                    it.id,
                    complicationProviders[it.complicationDetails.fallbackSystemProvider]!!
                )
            }
        )
    }

    private fun setAmbient(ambient: Boolean) {
        val interactiveWatchFaceInstanceSysUi =
            InteractiveInstanceManager.getAndRetainInstance(
                interactiveWatchFaceInstanceWCS.instanceId
            )!!.sysUiApi

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
            bitmap = interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                WatchfaceScreenshotParams(
                    RenderParameters(
                        DrawMode.AMBIENT,
                        RenderParameters.DRAW_ALL_LAYERS
                    ).toWireFormat(),
                    100,
                    123456789,
                    null,
                    null
                )
            ).ashmemCompressedImageBundleToBitmap()
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
            bitmap = interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                WatchfaceScreenshotParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        RenderParameters.DRAW_ALL_LAYERS
                    ).toWireFormat(),
                    100,
                    123456789,
                    null,
                    null
                )
            ).ashmemCompressedImageBundleToBitmap()!!
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
                UserStyleWireFormat(mapOf("color_style_category" to "green_style"))
            )
            engineWrapper.draw()
        }

        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap.assertAgainstGolden(screenshotRule, "green_screenshot")
    }

    @Test
    fun testHighlightComplicationsInScreenshot() {
        val latch = CountDownLatch(1)

        handler.post(this::initCanvasWatchFace)
        initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                WatchfaceScreenshotParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE,
                        mapOf(
                            Layer.BASE_LAYER to LayerMode.DRAW,
                            Layer.COMPLICATIONS to LayerMode.DRAW_HIGHLIGHTED,
                            Layer.TOP_LAYER to LayerMode.DRAW
                        )
                    ).toWireFormat(),
                    100,
                    123456789,
                    null,
                    null
                )
            ).ashmemCompressedImageBundleToBitmap()
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "highlight_complications"
        )
    }

    @Test
    fun testScreenshotWithPreviewComplicationData() {
        val latch = CountDownLatch(1)
        val previewComplicationData = listOf(
            IdAndComplicationData(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortTitle(ComplicationText.plainText("Preview"))
                    .setShortText(ComplicationText.plainText("A"))
                    .build()
            ),
            IdAndComplicationData(
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortTitle(ComplicationText.plainText("Preview"))
                    .setShortText(ComplicationText.plainText("B"))
                    .build()
            )
        )

        handler.post(this::initCanvasWatchFace)
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = interactiveWatchFaceInstanceWCS.takeWatchFaceScreenshot(
                WatchfaceScreenshotParams(
                    RenderParameters(
                        DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS
                    ).toWireFormat(),
                    100,
                    123456789,
                    null,
                    previewComplicationData
                )
            ).ashmemCompressedImageBundleToBitmap()
            latch.countDown()
        }

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "preview_complications"
        )
    }
}
