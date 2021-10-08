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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.watchface.SharedMemoryImage
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IPendingInteractiveWatchFace
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.samples.COLOR_STYLE_SETTING
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.GREEN_STYLE
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
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

internal class SimpleDigitalWatchFaceRenderer(
    surfaceHolder: SurfaceHolder,
    watchState: WatchState
) : Renderer.CanvasRenderer(
    surfaceHolder,
    CurrentUserStyleRepository(UserStyleSchema(emptyList())),
    watchState,
    CanvasType.HARDWARE,
    UPDATE_DELAY_MILLIS
) {
    internal companion object {
        val UPDATE_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1)
        const val TEST_TIME = "08:00"
    }

    var mWatchState: WatchState? = watchState
    var mPaint: Paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        textSize = 64f
    }
    val mTimeText = TEST_TIME

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        mPaint.color = Color.BLACK
        canvas.drawRect(bounds, mPaint)
        mPaint.color = Color.WHITE
        canvas.drawText(
            mTimeText,
            0,
            5,
            bounds.centerX().toFloat(),
            (bounds.centerY() - mWatchState!!.chinHeight).toFloat(),
            mPaint
        )
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        renderParameters.highlightLayer?.backgroundTint?.let { canvas.drawColor(it) }
    }

    override fun shouldAnimate(): Boolean = true
}

internal class TestControllableWatchFaceService(
    private val handler: Handler,
    private var surfaceHolderOverride: SurfaceHolder,
    private val factory: TestWatchFaceFactory,
    private val watchState: MutableWatchState,
    private val directBootParams: WallpaperInteractiveWatchFaceInstanceParams?
) : WatchFaceService() {
    init {
        attachBaseContext(ApplicationProvider.getApplicationContext())
    }

    abstract class TestWatchFaceFactory {
        fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

        fun createComplicationsManager(
            currentUserStyleRepository: CurrentUserStyleRepository
        ): ComplicationSlotsManager =
            ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

        abstract fun createWatchFaceAsync(
            surfaceHolder: SurfaceHolder,
            watchState: WatchState,
            complicationSlotsManager: ComplicationSlotsManager,
            currentUserStyleRepository: CurrentUserStyleRepository
        ): Deferred<WatchFace>
    }

    override fun createUserStyleSchema() = factory.createUserStyleSchema()

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = factory.createComplicationsManager(currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = factory.createWatchFaceAsync(
        surfaceHolderOverride,
        watchState,
        complicationSlotsManager,
        currentUserStyleRepository
    ).await()

    override fun getUiThreadHandlerImpl() = handler

    override fun getBackgroundThreadHandlerImpl() = handler

    override fun getMutableWatchState() = watchState

    override fun readDirectBootPrefs(
        context: Context,
        fileName: String
    ) = directBootParams

    override fun writeDirectBootPrefs(
        context: Context,
        fileName: String,
        prefs: WallpaperInteractiveWatchFaceInstanceParams
    ) {
    }

    override fun expectPreRInitFlow() = false

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride
}

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
public class WatchFaceServiceImageTest {

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder

    @Mock
    private lateinit var surface: Surface

    private val handler = Handler(Looper.getMainLooper())

    private val complicationDataSources = mapOf(
        SystemDataSources.DATA_SOURCE_DAY_OF_WEEK to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("Mon").build(),
                ComplicationText.EMPTY
            )
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
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
                .asWireComplicationData(),
        SystemDataSources.DATA_SOURCE_STEP_COUNT to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("100").build(),
                ComplicationText.EMPTY
            )
                .setTitle(PlainComplicationText.Builder("Steps").build())
                .build()
                .asWireComplicationData()
    )

    @get:Rule
    public val screenshotRule: AndroidXScreenshotTestRule =
        AndroidXScreenshotTestRule("wear/wear-watchface")

    private val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private var renderDoneLatch = CountDownLatch(1)
    private var initLatch = CountDownLatch(1)

    private val surfaceTexture = SurfaceTexture(false)

    private lateinit var canvasAnalogWatchFaceService: TestCanvasAnalogWatchFaceService
    private lateinit var testControllableWatchFaceService: TestControllableWatchFaceService
    private lateinit var completableWatchFace: CompletableDeferred<WatchFace>
    private lateinit var glesWatchFaceService: TestGlesWatchFaceService
    private lateinit var engineWrapper: WatchFaceService.EngineWrapper
    private lateinit var interactiveWatchFaceInstance: IInteractiveWatchFace

    @Before
    public fun setUp() {
        Assume.assumeTrue("This test suite assumes API 27", Build.VERSION.SDK_INT >= 27)
        MockitoAnnotations.initMocks(this)
    }

    @After
    public fun shutDown() {
        val latch = CountDownLatch(1)
        handler.post {
            if (this::interactiveWatchFaceInstance.isInitialized) {
                interactiveWatchFaceInstance.release()
            }
            latch.countDown()
        }
        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun initCanvasWatchFace() {
        canvasAnalogWatchFaceService = TestCanvasAnalogWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            ZoneId.of("UTC"),
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
    }

    private fun initControllableWatchFace() {
        completableWatchFace = CompletableDeferred<WatchFace>()
        testControllableWatchFaceService = TestControllableWatchFaceService(
            handler,
            surfaceHolder,
            object : TestControllableWatchFaceService.TestWatchFaceFactory() {
                override fun createWatchFaceAsync(
                    surfaceHolder: SurfaceHolder,
                    watchState: WatchState,
                    complicationSlotsManager: ComplicationSlotsManager,
                    currentUserStyleRepository: CurrentUserStyleRepository
                ): Deferred<WatchFace> = completableWatchFace
            },
            MutableWatchState(),
            null
        )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderDoneLatch.countDown()
        }
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)

        setPendingWallpaperInteractiveWatchFaceInstance()

        engineWrapper =
            testControllableWatchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
    }

    private fun initGles2WatchFace() {
        glesWatchFaceService = TestGlesWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            ZoneId.of("UTC"),
            surfaceHolder,
            null
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
                        WatchUiState(false, 0),
                        UserStyleWireFormat(emptyMap()),
                        null
                    ),
                    object : IPendingInteractiveWatchFace.Stub() {
                        override fun getApiVersion() =
                            IPendingInteractiveWatchFace.API_VERSION

                        override fun onInteractiveWatchFaceCreated(
                            iInteractiveWatchFace: IInteractiveWatchFace
                        ) {
                            interactiveWatchFaceInstance = iInteractiveWatchFace
                            initLatch.countDown()
                        }

                        override fun onInteractiveWatchFaceCrashed(exception: CrashInfoParcel?) {
                            fail("WatchFace crashed: $exception")
                        }
                    }
                )
            )
    }

    private fun sendComplications() {
        interactiveWatchFaceInstance.updateComplicationData(
            interactiveWatchFaceInstance.complicationDetails.map {
                IdAndComplicationDataWireFormat(
                    it.id,
                    complicationDataSources[it.complicationState.fallbackSystemProvider]!!
                )
            }
        )
    }

    private fun setAmbient(ambient: Boolean) {
        val interactiveWatchFaceInstance =
            InteractiveInstanceManager.getAndRetainInstance(
                interactiveWatchFaceInstance.instanceId
            )!!

        interactiveWatchFaceInstance.setWatchUiState(
            WatchUiState(
                ambient,
                0
            )
        )
        interactiveWatchFaceInstance.release()
    }

    @Test
    public fun testActiveScreenshot() {
        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        handler.post {
            engineWrapper.draw()
        }

        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap.assertAgainstGolden(screenshotRule, "active_screenshot")
    }

    @Test
    @Ignore // TODO(b/189452267): Fix drawBlack and reinstate.
    public fun testNonBlockingDrawScreenshot() {
        handler.post(this::initControllableWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()

        renderDoneLatch = CountDownLatch(1)
        handler.post {
            engineWrapper.draw()
        }

        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        val bitmapBefore = bitmap.copy(bitmap.config, false)

        completableWatchFace.complete(
            WatchFace(
                WatchFaceType.DIGITAL,
                SimpleDigitalWatchFaceRenderer(
                    surfaceHolder,
                    MutableWatchState().apply {
                        isVisible.value = true
                    }.asWatchState()
                )
            )
        )

        renderDoneLatch = CountDownLatch(1)
        handler.post {
            engineWrapper.draw()
        }
        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()

        bitmapBefore.assertAgainstGolden(screenshotRule, "before_completeCreateWatchFace")
        bitmap.assertAgainstGolden(screenshotRule, "after_completeCreateWatchFace")
    }

    @Test
    public fun testAmbientScreenshot() {
        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        handler.post {
            setAmbient(true)
            engineWrapper.draw()
        }

        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap.assertAgainstGolden(screenshotRule, "ambient_screenshot2")
    }

    @SuppressLint("NewApi")
    @Test
    public fun testCommandTakeScreenShot() {
        val latch = CountDownLatch(1)
        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.AMBIENT,
                            WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                            null
                        ).toWireFormat(),
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "testCommandTakeScreenShot"
        )
    }

    @SuppressLint("NewApi")
    @Test
    public fun testCommandTakeOpenGLScreenShot() {
        val latch = CountDownLatch(1)

        handler.post(this::initGles2WatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                            null
                        ).toWireFormat(),
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "ambient_gl_screenshot"
        )
    }

    @Test
    public fun testSetGreenStyle() {
        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        // Note this will clear complicationSlots.
        interactiveWatchFaceInstance.updateWatchfaceInstance(
            "newId",
            UserStyleWireFormat(mapOf(COLOR_STYLE_SETTING to GREEN_STYLE.encodeToByteArray()))
        )
        sendComplications()

        handler.post {
            engineWrapper.draw()
        }

        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap.assertAgainstGolden(screenshotRule, "green_screenshot")
    }

    @Test
    public fun testSetGreenStyleButDontResendComplications() {
        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        // Note this will clear complicationSlots.
        interactiveWatchFaceInstance.updateWatchfaceInstance(
            "newId",
            UserStyleWireFormat(mapOf(COLOR_STYLE_SETTING to GREEN_STYLE.encodeToByteArray()))
        )

        handler.post {
            engineWrapper.draw()
        }

        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap.assertAgainstGolden(screenshotRule, "green_screenshot_no_complication_data")
    }

    @SuppressLint("NewApi")
    @Test
    public fun testHighlightAllComplicationsInScreenshot() {
        val latch = CountDownLatch(1)

        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                            RenderParameters.HighlightLayer(
                                RenderParameters.HighlightedElement.AllComplicationSlots,
                                Color.RED,
                                Color.argb(128, 0, 0, 0)
                            )
                        ).toWireFormat(),
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "highlight_complications"
        )
    }

    @SuppressLint("NewApi")
    @Test
    public fun testRenderLeftComplicationPressed() {
        val latch = CountDownLatch(1)

        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                            null,
                            mapOf(
                                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                                    TapEvent(1, 1, Instant.ofEpochMilli(123456789))
                            )
                        ).toWireFormat(),
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "left_complication_pressed"
        )
    }

    @SuppressLint("NewApi")
    @Test
    public fun testHighlightRightComplicationInScreenshot() {
        val latch = CountDownLatch(1)

        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                            RenderParameters.HighlightLayer(
                                RenderParameters.HighlightedElement.ComplicationSlot(
                                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
                                ),
                                Color.RED,
                                Color.argb(128, 0, 0, 0)
                            )
                        ).toWireFormat(),
                        123456789,
                        null,
                        null
                    )
                )
            )
            latch.countDown()
        }

        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "highlight_right_complication"
        )
    }

    @SuppressLint("NewApi")
    @Test
    public fun testScreenshotWithPreviewComplicationData() {
        val latch = CountDownLatch(1)
        val previewComplicationData = listOf(
            IdAndComplicationDataWireFormat(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("A").build(),
                    ComplicationText.EMPTY
                )
                    .setTitle(PlainComplicationText.Builder("Preview").build())
                    .build()
                    .asWireComplicationData()
            ),
            IdAndComplicationDataWireFormat(
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("B").build(),
                    ComplicationText.EMPTY
                )
                    .setTitle(PlainComplicationText.Builder("Preview").build())
                    .build()
                    .asWireComplicationData()
            )
        )

        handler.post(this::initCanvasWatchFace)
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        var bitmap: Bitmap? = null
        handler.post {
            bitmap = SharedMemoryImage.ashmemReadImageBundle(
                interactiveWatchFaceInstance.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        RenderParameters(
                            DrawMode.INTERACTIVE,
                            WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                            null
                        ).toWireFormat(),
                        123456789,
                        null,
                        previewComplicationData
                    )
                )
            )
            latch.countDown()
        }

        assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        bitmap!!.assertAgainstGolden(
            screenshotRule,
            "preview_complications"
        )
    }

    @Test
    public fun directBoot() {
        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then {
            renderDoneLatch.countDown()
        }
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)

        // Simulate a R style direct boot scenario where a new service is created but there's no
        // pending PendingWallpaperInteractiveWatchFaceInstance and no wallpaper command. It
        // instead uses the WallpaperInteractiveWatchFaceInstanceParams which normally would be
        // read from disk, but provided directly in this test.
        val service = TestCanvasAnalogWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>(),
            handler,
            100000,
            ZoneId.of("UTC"),
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
                WatchUiState(false, 0),
                UserStyleWireFormat(
                    mapOf(COLOR_STYLE_SETTING to GREEN_STYLE.encodeToByteArray())
                ),
                null
            )
        )

        val engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper

        // Make sure init has completed before trying to draw.
        runBlocking {
            engineWrapper.deferredWatchFaceImpl.await()
        }

        handler.post { engineWrapper.draw() }

        assertThat(renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        try {
            bitmap.assertAgainstGolden(screenshotRule, "direct_boot")
        } finally {
            val latch = CountDownLatch(1)
            handler.post {
                engineWrapper.onDestroy()
                latch.countDown()
            }
            assertThat(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @SuppressLint("NewApi")
    @Test
    public fun complicationTapLaunchesActivity() {
        handler.post(this::initCanvasWatchFace)

        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        sendComplications()

        ComplicationTapActivity.newCountDown()

        val interactiveWatchFaceInstance =
            InteractiveInstanceManager.getAndRetainInstance(
                interactiveWatchFaceInstance.instanceId
            )!!
        interactiveWatchFaceInstance.sendTouchEvent(
            85,
            165,
            TapType.UP
        )

        assertThat(ComplicationTapActivity.awaitIntent()).isNotNull()
    }
}
