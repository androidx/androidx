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

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.icu.util.TimeZone
import android.os.Bundle
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
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.LayerMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IPendingInteractiveWatchFaceWCS
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.samples.COLOR_STYLE_SETTING
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.GREEN_STYLE
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
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

// Activity for testing complication taps.
public class ComplicationTapActivity : Activity() {
    internal companion object {
        private val lock = Any()
        private lateinit var theIntent: Intent
        private var countDown: CountDownLatch? = null

        fun newCountDown() {
            countDown = CountDownLatch(1)
        }

        fun awaitIntent(): Intent? {
            if (countDown!!.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                return theIntent
            } else {
                return null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        synchronized(lock) {
            theIntent = intent
        }
        countDown!!.countDown()
        finish()
    }
}

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceServiceImageTest {

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder

    @Mock
    private lateinit var surface: Surface

    private val handler = Handler(Looper.getMainLooper())

    private val complicationProviders = mapOf(
        SystemProviders.DAY_OF_WEEK to
            ShortTextComplicationData.Builder(PlainComplicationText.Builder("Mon").build())
                .setTitle(PlainComplicationText.Builder("23rd").build())
                .setTapAction(
                    PendingIntent.getActivity(
                        ApplicationProvider.getApplicationContext<Context>(),
                        123,
                        Intent(
                            ApplicationProvider.getApplicationContext<Context>(),
                            ComplicationTapActivity::class.java
                        ).apply {
                        },
                        PendingIntent.FLAG_ONE_SHOT
                    )
                )
                .build()
                .asWireComplicationData(),
        SystemProviders.STEP_COUNT to
            ShortTextComplicationData.Builder(PlainComplicationText.Builder("100").build())
                .setTitle(PlainComplicationText.Builder("Steps").build())
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
        if (this::interactiveWatchFaceInstanceWCS.isInitialized) {
            interactiveWatchFaceInstanceWCS.release()
        }
    }

    private fun initCanvasWatchFace() {
        canvasAnalogWatchFaceService = TestCanvasAnalogWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            surfaceHolder,
            true, // Not direct boot.
            null
        )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderDoneLatch.countDown()
        }
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)

        setPendingWallpaperInteractiveWatchFaceInstance()

        engineWrapper =
            canvasAnalogWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onSurfaceChanged(
            surfaceHolder,
            0,
            surfaceHolder.surfaceFrame.width(),
            surfaceHolder.surfaceFrame.height()
        )
    }

    private fun initGles2WatchFace() {
        glesWatchFaceService = TestGlesWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            surfaceHolder,
            null
        )

        surfaceTexture.setDefaultBufferSize(BITMAP_WIDTH, BITMAP_HEIGHT)

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.surface).thenReturn(Surface(surfaceTexture))

        setPendingWallpaperInteractiveWatchFaceInstance()

        engineWrapper = glesWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onSurfaceChanged(
            surfaceHolder,
            0,
            surfaceHolder.surfaceFrame.width(),
            surfaceHolder.surfaceFrame.height()
        )
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
                            // engineWrapper won't be initialized yet, so defer execution.
                            handler.post {
                                // Set the timezone so it doesn't matter where the bots are running.
                                engineWrapper.watchFaceImpl.calendar.timeZone =
                                    TimeZone.getTimeZone("UTC")
                                initLatch.countDown()
                            }
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

    private fun waitForPendingTaskToRunOnHandler() {
        val latch = CountDownLatch(1)
        handler.post {
            latch.countDown()
        }
        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
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
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstanceWCS.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.AMBIENT,
                            RenderParameters.DRAW_ALL_LAYERS,
                            null,
                            Color.RED
                        ).toWireFormat(),
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
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstanceWCS.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            RenderParameters.DRAW_ALL_LAYERS,
                            null,
                            Color.RED
                        ).toWireFormat(),
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
            interactiveWatchFaceInstanceWCS.updateWatchfaceInstance(
                "newId",
                UserStyleWireFormat(mapOf(COLOR_STYLE_SETTING to GREEN_STYLE))
            )
            sendComplications()
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
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstanceWCS.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            mapOf(
                                Layer.BASE_LAYER to LayerMode.DRAW,
                                Layer.COMPLICATIONS to LayerMode.DRAW_OUTLINED,
                                Layer.TOP_LAYER to LayerMode.DRAW
                            ),
                            null,
                            Color.RED
                        ).toWireFormat(),
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
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstanceWCS.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            mapOf(
                                Layer.BASE_LAYER to LayerMode.DRAW,
                                Layer.COMPLICATIONS to LayerMode.DRAW_OUTLINED,
                                Layer.TOP_LAYER to LayerMode.DRAW
                            ),
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                            Color.RED
                        ).toWireFormat(),
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
                ShortTextComplicationData.Builder(PlainComplicationText.Builder("A").build())
                    .setTitle(PlainComplicationText.Builder("Preview").build())
                    .build()
                    .asWireComplicationData()
            ),
            IdAndComplicationDataWireFormat(
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                ShortTextComplicationData.Builder(PlainComplicationText.Builder("B").build())
                    .setTitle(PlainComplicationText.Builder("Preview").build())
                    .build()
                    .asWireComplicationData()
            )
        )

        handler.post(this::initCanvasWatchFace)
        // Preview complication data results in additional tasks posted which we need to complete
        // before interactiveWatchFaceInstanceWCS is initialized.
        waitForPendingTaskToRunOnHandler()
        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstanceWCS.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            RenderParameters.DRAW_ALL_LAYERS,
                            null,
                            Color.RED
                        ).toWireFormat(),
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
        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderDoneLatch.countDown()
        }
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)

        lateinit var engineWrapper: WatchFaceService.EngineWrapper

        handler.post {
            // Simulate a R style direct boot scenario where a new service is created but there's no
            // pending PendingWallpaperInteractiveWatchFaceInstance and no wallpaper command. It
            // instead uses the WallpaperInteractiveWatchFaceInstanceParams which normally would be
            // read from disk, but provided directly in this test.
            val service = TestCanvasAnalogWatchFaceService(
                ApplicationProvider.getApplicationContext<Context>(),
                handler,
                100000,
                surfaceHolder,
                false, // Direct boot.
                WallpaperInteractiveWatchFaceInstanceParams(
                    INTERACTIVE_INSTANCE_ID,
                    DeviceConfig(
                        false,
                        false,
                        0,
                        0
                    ),
                    SystemState(false, 0),
                    UserStyleWireFormat(
                        mapOf(COLOR_STYLE_SETTING to GREEN_STYLE)
                    ),
                    null
                )
            )

            engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
            engineWrapper.onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )
            handler.post { engineWrapper.draw() }
        }

        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        try {
            bitmap.assertAgainstGolden(screenshotRule, "direct_boot")
        } finally {
            engineWrapper.onDestroy()
        }
    }

    @Test
    fun complicationTapLaunchesActivity() {
        handler.post(this::initCanvasWatchFace)

        ComplicationTapActivity.newCountDown()
        handler.post {
            val interactiveWatchFaceInstanceSysUi =
                InteractiveInstanceManager.getAndRetainInstance(
                    interactiveWatchFaceInstanceWCS.instanceId
                )!!.createSysUiApi()
            interactiveWatchFaceInstanceSysUi.sendTouchEvent(
                85,
                165,
                TapType.UP
            )
            interactiveWatchFaceInstanceSysUi.release()
        }

        assertThat(ComplicationTapActivity.awaitIntent()).isNotNull()
    }
}
