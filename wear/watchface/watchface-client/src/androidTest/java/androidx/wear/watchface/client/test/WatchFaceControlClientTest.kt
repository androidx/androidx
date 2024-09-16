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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.watchface.BoundingArc
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.ContentDescriptionLabel
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceColors
import androidx.wear.watchface.WatchFaceExperimental
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.DisconnectReason
import androidx.wear.watchface.client.DisconnectReasons
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.InteractiveWatchFaceClient
import androidx.wear.watchface.client.InteractiveWatchFaceClientImpl
import androidx.wear.watchface.client.WatchFaceClientExperimental
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.client.WatchUiState
import androidx.wear.watchface.client.test.TestServicesHelpers.componentOf
import androidx.wear.watchface.client.test.TestServicesHelpers.createTestComplications
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.BLUE_STYLE
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.COLOR_STYLE_SETTING
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.COMPLICATIONS_STYLE_SETTING
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.CONFIGURABLE_DATA_SOURCE
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.CONFIGURABLE_DATA_SOURCE_PKG
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.DRAW_HOUR_PIPS_STYLE_SETTING
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.GREEN_STYLE
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.NO_COMPLICATIONS
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.WATCH_HAND_LENGTH_STYLE_SETTING
import androidx.wear.watchface.samples.ExampleOpenGLBackgroundInitWatchFaceService
import androidx.wear.watchface.samples.R
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleFlavor
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit

private const val CONNECT_TIMEOUT_MILLIS = 500L
private const val DESTROY_TIMEOUT_MILLIS = 500L
private const val UPDATE_TIMEOUT_MILLIS = 500L

@RequiresApi(Build.VERSION_CODES.O_MR1)
abstract class WatchFaceControlClientTestBase {
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

    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock protected lateinit var surfaceHolder: SurfaceHolder

    @Mock protected lateinit var surfaceHolder2: SurfaceHolder

    @Mock private lateinit var surface: Surface

    protected val handler = Handler(Looper.getMainLooper())
    protected val handlerCoroutineScope =
        CoroutineScope(Handler(handler.looper).asCoroutineDispatcher())

    protected lateinit var engine: WatchFaceService.EngineWrapper

    protected val deviceConfig =
        DeviceConfig(
            hasLowBitAmbient = false,
            hasBurnInProtection = false,
            analogPreviewReferenceTimeMillis = 0,
            digitalPreviewReferenceTimeMillis = 0
        )

    protected val systemState = WatchUiState(false, 0)

    protected val complications = createTestComplications(context)

    @Before
    fun setUp() {
        WatchFaceControlTestService.apiVersionOverride = null
        Mockito.`when`(surfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 400, 400))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)
    }

    @After
    @CallSuper
    open fun tearDown() {
        // Interactive instances are not currently shut down when all instances go away. E.g. WCS
        // crashing does not cause the watch face to stop. So we need to shut down explicitly.
        if (this::engine.isInitialized) {
            val latch = CountDownLatch(1)
            handler.post {
                engine.onDestroy()
                latch.countDown()
            }
            latch.await(DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        }
        service.close()
    }

    protected fun getOrCreateTestSubject(
        watchFaceService: WatchFaceService =
            TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder),
        instanceId: String = "testId",
        userStyle: UserStyleData? = null,
        complications: Map<Int, ComplicationData>? = this.complications,
        previewExecutor: Executor? = null,
        previewListener: Consumer<String>? = null
    ): InteractiveWatchFaceClient {
        val deferredInteractiveInstance =
            handlerCoroutineScope.async {
                if (previewExecutor != null && previewListener != null) {
                    service.getOrCreateInteractiveWatchFaceClient(
                        instanceId,
                        deviceConfig,
                        systemState,
                        userStyle,
                        complications,
                        previewExecutor,
                        previewListener
                    )
                } else {
                    @Suppress("deprecation")
                    service.getOrCreateInteractiveWatchFaceClient(
                        instanceId,
                        deviceConfig,
                        systemState,
                        userStyle,
                        complications
                    )
                }
            }

        // Create the engine which triggers construction of the interactive instance.
        handler.post {
            engine = watchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        return awaitWithTimeout(deferredInteractiveInstance)
    }

    protected fun <X> awaitWithTimeout(
        thing: Deferred<X>,
        timeoutMillis: Long = CONNECT_TIMEOUT_MILLIS
    ): X {
        var value: X? = null
        val latch = CountDownLatch(1)
        handlerCoroutineScope.launch {
            value = thing.await()
            latch.countDown()
        }
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Timeout waiting for thing!")
        }
        return value!!
    }

    protected fun tapOnComplication(interactiveInstance: InteractiveWatchFaceClient, slotId: Int) {
        val leftClickX = interactiveInstance.complicationSlotsState[slotId]!!.bounds.centerX()
        val leftClickY = interactiveInstance.complicationSlotsState[slotId]!!.bounds.centerY()

        interactiveInstance.sendTouchEvent(leftClickX, leftClickY, TapType.DOWN)
        interactiveInstance.sendTouchEvent(leftClickX, leftClickY, TapType.UP)
    }

    protected fun waitForWatchFaceInstanceReady(interactiveInstance: InteractiveWatchFaceClient) {
        val wfReady = CompletableDeferred<Unit>()
        interactiveInstance.addOnWatchFaceReadyListener(
            { runnable -> runnable.run() },
            { wfReady.complete(Unit) }
        )
        awaitWithTimeout(wfReady)
    }
}

fun rangedValueComplicationBuilder() =
    RangedValueComplicationData.Builder(
            value = 50.0f,
            min = 10.0f,
            max = 100.0f,
            ComplicationText.EMPTY
        )
        .setText(PlainComplicationText.Builder("Battery").build())

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
class WatchFaceControlClientTest : WatchFaceControlClientTestBase() {

    private val exampleCanvasAnalogWatchFaceComponentName =
        componentOf<ExampleCanvasAnalogWatchFaceService>()

    @After
    override fun tearDown() {
        super.tearDown()
        ObservableServiceA.reset()
        ObservableServiceB.reset()
        ObservableServiceC.reset()
    }

    @Test
    fun complicationProviderDefaults() {
        val wallpaperService =
            TestComplicationProviderDefaultsWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        try {
            assertThat(interactiveInstance.complicationSlotsState.keys).containsExactly(123)
            val slot = interactiveInstance.complicationSlotsState[123]!!
            assertThat(slot.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package1", "com.app1"))
            assertThat(slot.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.PHOTO_IMAGE)

            assertThat(slot.defaultDataSourcePolicy.secondaryDataSource)
                .isEqualTo(ComponentName("com.package2", "com.app2"))
            assertThat(slot.defaultDataSourcePolicy.secondaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.LONG_TEXT)

            assertThat(slot.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
            assertThat(slot.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun unspecifiedComplicationSlotNames() {
        val wallpaperService =
            TestComplicationProviderDefaultsWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        try {
            assertThat(interactiveInstance.complicationSlotsState.keys).containsExactly(123)

            val slot = interactiveInstance.complicationSlotsState[123]!!
            assertThat(slot.nameResourceId).isNull()
            assertThat(slot.screenReaderNameResourceId).isNull()
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    fun specifiedComplicationSlotNamesThroughComplicationSlotOption() {
        val wallpaperService = TestComplicationStyleUpdateWatchFaceService(context, surfaceHolder)

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        // User style settings to be updated
        val userStyleSettings = interactiveInstance.userStyleSchema.userStyleSettings
        val leftComplicationUserStyleSetting = userStyleSettings[0]
        val optionWithNameOverride = leftComplicationUserStyleSetting.options[1]

        // Apply complication style option
        interactiveInstance.updateWatchFaceInstance(
            "testId",
            UserStyle(
                selectedOptions = mapOf(leftComplicationUserStyleSetting to optionWithNameOverride)
            )
        )

        try {
            assertThat(interactiveInstance.complicationSlotsState.keys).containsExactly(123)

            val slot = interactiveInstance.complicationSlotsState[123]!!
            assertThat(slot.nameResourceId).isEqualTo(R.string.left_complication_screen_name)
            assertThat(slot.screenReaderNameResourceId)
                .isEqualTo(R.string.left_complication_screen_reader_name)
        } finally {
            interactiveInstance.close()
        }
    }

    @Suppress("DEPRECATION", "newApi") // defaultDataSourceType & ComplicationType
    @Test
    fun interactiveWatchFaceClient_ComplicationDetails() {
        val interactiveInstance = getOrCreateTestSubject()

        assertThat(interactiveInstance.complicationSlotsState.size).isEqualTo(2)

        val leftComplicationDetails =
            interactiveInstance.complicationSlotsState[
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID]!!
        assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(leftComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(leftComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback)
            .isEqualTo(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK)
        assertThat(leftComplicationDetails.defaultDataSourceType)
            .isEqualTo(ComplicationType.SHORT_TEXT)
        assertThat(leftComplicationDetails.supportedTypes)
            .containsExactly(
                ComplicationType.RANGED_VALUE,
                ComplicationType.GOAL_PROGRESS,
                ComplicationType.WEIGHTED_ELEMENTS,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            )
        assertTrue(leftComplicationDetails.isEnabled)
        assertThat(leftComplicationDetails.currentType).isEqualTo(ComplicationType.SHORT_TEXT)
        assertThat(leftComplicationDetails.nameResourceId)
            .isEqualTo(R.string.left_complication_screen_name)
        assertThat(leftComplicationDetails.screenReaderNameResourceId)
            .isEqualTo(R.string.left_complication_screen_reader_name)

        val rightComplicationDetails =
            interactiveInstance.complicationSlotsState[
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID]!!
        assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(rightComplicationDetails.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(rightComplicationDetails.defaultDataSourcePolicy.systemDataSourceFallback)
            .isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
        assertThat(rightComplicationDetails.defaultDataSourceType)
            .isEqualTo(ComplicationType.SHORT_TEXT)
        assertThat(rightComplicationDetails.supportedTypes)
            .containsExactly(
                ComplicationType.RANGED_VALUE,
                ComplicationType.GOAL_PROGRESS,
                ComplicationType.WEIGHTED_ELEMENTS,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            )
        assertTrue(rightComplicationDetails.isEnabled)
        assertThat(rightComplicationDetails.currentType).isEqualTo(ComplicationType.SHORT_TEXT)
        assertThat(rightComplicationDetails.nameResourceId)
            .isEqualTo(R.string.right_complication_screen_name)
        assertThat(rightComplicationDetails.screenReaderNameResourceId)
            .isEqualTo(R.string.right_complication_screen_reader_name)

        interactiveInstance.close()
    }

    @Test
    fun updateComplicationData() {
        val interactiveInstance = getOrCreateTestSubject()

        interactiveInstance.updateComplicationData(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    rangedValueComplicationBuilder().build(),
                EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                    LongTextComplicationData.Builder(
                            PlainComplicationText.Builder("Test").build(),
                            ComplicationText.EMPTY
                        )
                        .build()
            )
        )

        assertThat(interactiveInstance.complicationSlotsState.size).isEqualTo(2)

        val leftComplicationDetails =
            interactiveInstance.complicationSlotsState[
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID]!!
        val rightComplicationDetails =
            interactiveInstance.complicationSlotsState[
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID]!!

        assertThat(leftComplicationDetails.currentType).isEqualTo(ComplicationType.RANGED_VALUE)
        assertThat(rightComplicationDetails.currentType).isEqualTo(ComplicationType.LONG_TEXT)
    }

    @Test
    fun getOrCreateInteractiveWatchFaceClient_existingOpenInstance() {
        val watchFaceService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        getOrCreateTestSubject(watchFaceService)

        val deferredInteractiveInstance2 =
            handlerCoroutineScope.async {
                @Suppress("deprecation")
                service.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    deviceConfig,
                    systemState,
                    null,
                    complications
                )
            }

        assertThat(awaitWithTimeout(deferredInteractiveInstance2).instanceId).isEqualTo("testId")
    }

    @Test
    fun getOrCreateInteractiveWatchFaceClient_existingClosedInstance() {
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        // Closing this interface means the subsequent
        // getOrCreateInteractiveWatchFaceClient won't immediately return
        // a resolved future.
        interactiveInstance.close()

        // Connect again to the same wallpaperService instance
        val deferredExistingInstance =
            handlerCoroutineScope.async {
                @Suppress("deprecation")
                service.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    deviceConfig,
                    systemState,
                    null,
                    complications
                )
            }

        assertFalse(deferredExistingInstance.isCompleted)

        // We don't want to leave a pending request or it'll mess up subsequent tests.
        handler.post { wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper }

        awaitWithTimeout(deferredExistingInstance)
    }

    @Test
    @Suppress("deprecation") // getOrCreateInteractiveWatchFaceClient
    fun resourceOnlyWatchFacePackageName() {
        val watchFaceService = TestWatchFaceRuntimeService(context, surfaceHolder)
        val service = runBlocking {
            WatchFaceControlClient.createWatchFaceControlClientImpl(
                context,
                Intent(context, WatchFaceControlTestService::class.java).apply {
                    action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
                },
                resourceOnlyWatchFacePackageName = "com.example.watchface"
            )
        }

        val deferredInteractiveInstance =
            handlerCoroutineScope.async {
                service.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    deviceConfig,
                    systemState,
                    userStyle = null,
                    complications
                )
            }

        // Create the engine which triggers construction of the interactive instance.
        handler.post {
            engine = watchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        // Make sure watch face init has completed.
        assertTrue(
            watchFaceService.lastResourceOnlyWatchFacePackageNameLatch.await(
                UPDATE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS
            )
        )

        assertThat(watchFaceService.lastResourceOnlyWatchFacePackageName)
            .isEqualTo("com.example.watchface")

        interactiveInstance.close()
    }

    @Test
    @Suppress("deprecation") // getOrCreateInteractiveWatchFaceClient
    fun resourceOnlyWatchFacePackageNameWithExtra() {
        val watchFaceService = TestStatefulWatchFaceRuntimeService(context, surfaceHolder)
        val service = runBlocking {
            WatchFaceControlClient.createWatchFaceControlClientImpl(
                context,
                Intent(context, WatchFaceControlTestService::class.java).apply {
                    action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
                },
                resourceOnlyWatchFacePackageName = "com.example.watchface"
            )
        }

        val deferredInteractiveInstance =
            handlerCoroutineScope.async {
                service.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    deviceConfig,
                    systemState,
                    userStyle = null,
                    complications
                )
            }

        // Create the engine which triggers construction of the interactive instance.
        handler.post {
            engine = watchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        // Make sure watch face init has completed.
        assertTrue(
            watchFaceService.lastResourceOnlyWatchFacePackageNameLatch.await(
                UPDATE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS
            )
        )

        assertThat(watchFaceService.lastResourceOnlyWatchFacePackageName)
            .isEqualTo("com.example.watchface")

        interactiveInstance.close()
    }

    @Test
    @Suppress("Deprecation")
    fun getInteractiveWatchFaceInstance() {
        val testId = "testId"
        // Create and wait for an interactive instance without capturing a reference to it
        getOrCreateTestSubject(instanceId = testId)

        // Get the instance created above
        val sysUiInterface = service.getInteractiveWatchFaceClientInstance(testId)!!

        val contentDescriptionLabels = sysUiInterface.contentDescriptionLabels
        assertThat(contentDescriptionLabels.size).isEqualTo(3)
        // Central clock element. Note we don't know the timezone this test will be running in
        // so we can't assert the contents of the clock's test.
        assertThat(contentDescriptionLabels[0].bounds).isEqualTo(Rect(100, 100, 300, 300))
        assertThat(contentDescriptionLabels[0].getTextAt(context.resources, Instant.EPOCH))
            .isNotEqualTo("")

        // Left complication.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(contentDescriptionLabels[1].getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("ID Left")

        // Right complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(contentDescriptionLabels[2].getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("ID Right")

        assertThat(sysUiInterface.overlayStyle.backgroundColor).isNull()
        assertThat(sysUiInterface.overlayStyle.foregroundColor).isNull()

        sysUiInterface.close()
    }

    @Test
    fun additionalContentDescriptionLabels() {
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        // We need to wait for watch face init to have completed before lateinit
        // wallpaperService.watchFace will be assigned. To do this we issue an arbitrary API
        // call which by necessity awaits full initialization.
        interactiveInstance.previewReferenceInstant

        // Add some additional ContentDescriptionLabels
        val pendingIntent1 =
            PendingIntent.getActivity(context, 0, Intent("One"), PendingIntent.FLAG_IMMUTABLE)
        val pendingIntent2 =
            PendingIntent.getActivity(context, 0, Intent("Two"), PendingIntent.FLAG_IMMUTABLE)
        (wallpaperService).watchFace.renderer.additionalContentDescriptionLabels =
            listOf(
                Pair(
                    0,
                    ContentDescriptionLabel(
                        PlainComplicationText.Builder("Before").build(),
                        Rect(10, 10, 20, 20),
                        pendingIntent1
                    )
                ),
                Pair(
                    20000,
                    ContentDescriptionLabel(
                        PlainComplicationText.Builder("After").build(),
                        Rect(30, 30, 40, 40),
                        pendingIntent2
                    )
                )
            )

        val sysUiInterface = service.getInteractiveWatchFaceClientInstance("testId")!!

        val contentDescriptionLabels = sysUiInterface.contentDescriptionLabels
        assertThat(contentDescriptionLabels.size).isEqualTo(5)

        // Central clock element. Note we don't know the timezone this test will be running in
        // so we can't assert the contents of the clock's test.
        assertThat(contentDescriptionLabels[0].bounds).isEqualTo(Rect(100, 100, 300, 300))
        assertThat(contentDescriptionLabels[0].getTextAt(context.resources, Instant.EPOCH))
            .isNotEqualTo("")

        // First additional ContentDescriptionLabel.
        assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(10, 10, 20, 20))
        assertThat(contentDescriptionLabels[1].getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("Before")
        assertThat(contentDescriptionLabels[1].tapAction).isEqualTo(pendingIntent1)

        // Left complication.
        assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(contentDescriptionLabels[2].getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("ID Left")

        // Right complication.
        assertThat(contentDescriptionLabels[3].bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(contentDescriptionLabels[3].getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("ID Right")

        // Second additional ContentDescriptionLabel.
        assertThat(contentDescriptionLabels[4].bounds).isEqualTo(Rect(30, 30, 40, 40))
        assertThat(contentDescriptionLabels[4].getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("After")
        assertThat(contentDescriptionLabels[4].tapAction).isEqualTo(pendingIntent2)
    }

    @Test
    fun contentDescriptionLabels_after_close() {
        val interactiveInstance = getOrCreateTestSubject()

        assertThat(interactiveInstance.contentDescriptionLabels).isNotEmpty()
        interactiveInstance.close()
        assertThat(interactiveInstance.contentDescriptionLabels).isEmpty()
    }

    @Test
    fun getComplicationIdAt() {
        val interactiveInstance = getOrCreateTestSubject()

        assertNull(interactiveInstance.getComplicationIdAt(0, 0))
        assertThat(interactiveInstance.getComplicationIdAt(85, 165))
            .isEqualTo(EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)
        assertThat(interactiveInstance.getComplicationIdAt(255, 165))
            .isEqualTo(EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID)
        interactiveInstance.close()
    }

    @Test
    fun getComplicationIdAt_customTapFilter() {
        val wallpaperService = TestCustomTapFilterWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        // When x & y are both even it's deemed to be a hit.
        assertThat(interactiveInstance.getComplicationIdAt(0, 0)).isEqualTo(123)
        assertNull(interactiveInstance.getComplicationIdAt(0, 1))
        assertNull(interactiveInstance.getComplicationIdAt(1, 0))
        assertNull(interactiveInstance.getComplicationIdAt(1, 1))
        assertThat(interactiveInstance.getComplicationIdAt(2, 2)).isEqualTo(123)

        interactiveInstance.close()
    }

    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Test
    fun getDefaultProviderPolicies() {
        assertThat(
                service.getDefaultComplicationDataSourcePoliciesAndType(
                    exampleCanvasAnalogWatchFaceComponentName
                )
            )
            .containsExactlyEntriesIn(
                mapOf(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                        androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                            DefaultComplicationDataSourcePolicy(
                                ComponentName(
                                    CONFIGURABLE_DATA_SOURCE_PKG,
                                    CONFIGURABLE_DATA_SOURCE
                                ),
                                ComplicationType.SHORT_TEXT,
                                SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
                                ComplicationType.SHORT_TEXT
                            ),
                            ComplicationType.SHORT_TEXT
                        ),
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                        androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                            DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_STEP_COUNT,
                                ComplicationType.SHORT_TEXT
                            ),
                            ComplicationType.SHORT_TEXT
                        )
                )
            )
    }

    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Test
    fun getDefaultProviderPoliciesOldApi() {
        WatchFaceControlTestService.apiVersionOverride = 1
        assertThat(
                service.getDefaultComplicationDataSourcePoliciesAndType(
                    exampleCanvasAnalogWatchFaceComponentName
                )
            )
            .containsExactlyEntriesIn(
                mapOf(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                        androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                            DefaultComplicationDataSourcePolicy(
                                ComponentName(
                                    CONFIGURABLE_DATA_SOURCE_PKG,
                                    CONFIGURABLE_DATA_SOURCE
                                ),
                                ComplicationType.SHORT_TEXT,
                                SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
                                ComplicationType.SHORT_TEXT
                            ),
                            ComplicationType.SHORT_TEXT
                        ),
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                        androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                            DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_STEP_COUNT,
                                ComplicationType.SHORT_TEXT
                            ),
                            ComplicationType.SHORT_TEXT
                        )
                )
            )
    }

    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Test
    fun getDefaultProviderPolicies_with_TestCrashingWatchFaceService() {
        // Tests that we can retrieve the DefaultComplicationDataSourcePolicy without invoking any
        // parts of TestCrashingWatchFaceService that deliberately crash.
        assertThat(
                service.getDefaultComplicationDataSourcePoliciesAndType(
                    ComponentName(
                        "androidx.wear.watchface.client.test",
                        "androidx.wear.watchface.client.test.TestCrashingWatchFaceService"
                    )
                )
            )
            .containsExactlyEntriesIn(
                mapOf(
                    TestCrashingWatchFaceService.COMPLICATION_ID to
                        androidx.wear.watchface.client.DefaultComplicationDataSourcePolicyAndType(
                            DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET,
                                ComplicationType.LONG_TEXT
                            ),
                            ComplicationType.LONG_TEXT
                        )
                )
            )
    }

    @Test
    fun addWatchFaceReadyListener_canvasRender() {
        val initCompletableDeferred = CompletableDeferred<Unit>()
        val wallpaperService =
            TestAsyncCanvasRenderInitWatchFaceService(
                context,
                surfaceHolder,
                initCompletableDeferred
            )
        val deferredInteractiveInstance =
            handlerCoroutineScope.async {
                @Suppress("deprecation")
                service.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    deviceConfig,
                    systemState,
                    null,
                    complications
                )
            }

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)

        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            val wfReady = CompletableDeferred<Unit>()
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                { wfReady.complete(Unit) }
            )
            assertThat(wfReady.isCompleted).isFalse()

            initCompletableDeferred.complete(Unit)

            // This should not timeout.
            awaitWithTimeout(wfReady)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun removeWatchFaceReadyListener_canvasRender() {
        val initCompletableDeferred = CompletableDeferred<Unit>()
        val wallpaperService =
            TestAsyncCanvasRenderInitWatchFaceService(
                context,
                surfaceHolder,
                initCompletableDeferred
            )
        val deferredInteractiveInstance =
            handlerCoroutineScope.async {
                @Suppress("deprecation")
                service.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    deviceConfig,
                    systemState,
                    null,
                    complications
                )
            }

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)

        val renderLatch = CountDownLatch(1)
        Mockito.`when`(surfaceHolder.unlockCanvasAndPost(canvas)).then { renderLatch.countDown() }

        // Create the engine which triggers creation of the interactive instance.
        handler.post {
            engine = wallpaperService.onCreateEngine() as WatchFaceService.EngineWrapper
        }

        // Wait for the instance to be created.
        val interactiveInstance = awaitWithTimeout(deferredInteractiveInstance)

        try {
            var listenerCalled = false
            val listener =
                InteractiveWatchFaceClient.OnWatchFaceReadyListener { listenerCalled = true }
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                listener
            )
            interactiveInstance.removeOnWatchFaceReadyListener(listener)
            assertThat(listenerCalled).isFalse()

            initCompletableDeferred.complete(Unit)

            assertTrue(renderLatch.await(DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

            assertThat(listenerCalled).isFalse()
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun addWatchFaceReadyListener_glesRender() {
        val surfaceTexture = SurfaceTexture(false)
        surfaceTexture.setDefaultBufferSize(10, 10)
        Mockito.`when`(surfaceHolder2.surface).thenReturn(Surface(surfaceTexture))
        Mockito.`when`(surfaceHolder2.surfaceFrame).thenReturn(Rect(0, 0, 10, 10))

        val onUiThreadGlSurfaceCreatedCompletableDeferred = CompletableDeferred<Unit>()
        val onBackgroundThreadGlContextCreatedCompletableDeferred = CompletableDeferred<Unit>()
        val wallpaperService =
            TestAsyncGlesRenderInitWatchFaceService(
                context,
                surfaceHolder2,
                onUiThreadGlSurfaceCreatedCompletableDeferred,
                onBackgroundThreadGlContextCreatedCompletableDeferred
            )

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        try {
            val wfReady = CompletableDeferred<Unit>()
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                { wfReady.complete(Unit) }
            )
            assertThat(wfReady.isCompleted).isFalse()

            onUiThreadGlSurfaceCreatedCompletableDeferred.complete(Unit)
            onBackgroundThreadGlContextCreatedCompletableDeferred.complete(Unit)

            // This can be a bit slow.
            awaitWithTimeout(wfReady, 2000)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun addWatchFaceReadyListener_alreadyReady() {
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        try {
            // Perform an action that will block until watch face init has completed.
            assertThat(interactiveInstance.complicationSlotsState).isNotEmpty()

            val wfReady = CompletableDeferred<Unit>()
            interactiveInstance.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                { wfReady.complete(Unit) }
            )

            // This should happen quickly, but it can sometimes be slow.
            awaitWithTimeout(wfReady, 1000)
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun addWatchFaceReadyListener_alreadyReady_newInstance() {
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService, instanceId = "abc")
        var sysUiInterface: InteractiveWatchFaceClient? = null

        try {
            // Perform an action that will block until watch face init has completed.
            assertThat(interactiveInstance.complicationSlotsState).isNotEmpty()

            // Get the instance created above
            sysUiInterface = service.getInteractiveWatchFaceClientInstance("abc")!!

            val wfReady = CompletableDeferred<Unit>()
            sysUiInterface.addOnWatchFaceReadyListener(
                { runnable -> runnable.run() },
                { wfReady.complete(Unit) }
            )

            // This should happen quickly, but it can sometimes be slow.
            awaitWithTimeout(wfReady, 1000)
        } finally {
            interactiveInstance.close()
            sysUiInterface?.close()
        }
    }

    @Test
    fun isConnectionAlive_false_after_close() {
        val interactiveInstance = getOrCreateTestSubject()

        assertThat(interactiveInstance.isConnectionAlive()).isTrue()

        interactiveInstance.close()
        assertThat(interactiveInstance.isConnectionAlive()).isFalse()
    }

    @Test
    fun hasComplicationCache_oldApi() {
        WatchFaceControlTestService.apiVersionOverride = 3
        assertFalse(service.hasComplicationDataCache())
    }

    @Test
    fun hasComplicationCache_currentApi() {
        assertTrue(service.hasComplicationDataCache())
    }

    @Test
    @Suppress("Deprecation")
    public fun isComplicationDisplayPolicySupported() {
        val wallpaperService =
            TestWatchfaceOverlayStyleWatchFaceService(
                context,
                surfaceHolder,
                WatchFace.OverlayStyle(Color.valueOf(Color.RED), Color.valueOf(Color.BLACK))
            )
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        assertThat(interactiveInstance.isComplicationDisplayPolicySupported()).isTrue()

        interactiveInstance.close()
    }

    @Test
    public fun isComplicationDisplayPolicySupported_oldApi() {
        val mockIInteractiveWatchFace = mock(IInteractiveWatchFace::class.java)
        val mockIBinder = mock(IBinder::class.java)
        `when`(mockIInteractiveWatchFace.asBinder()).thenReturn(mockIBinder)
        `when`(mockIInteractiveWatchFace.apiVersion).thenReturn(7)

        val interactiveInstance =
            InteractiveWatchFaceClientImpl(
                mockIInteractiveWatchFace,
                previewImageUpdateRequestedExecutor = null,
                previewImageUpdateRequestedListener = null
            )

        assertThat(interactiveInstance.isComplicationDisplayPolicySupported()).isFalse()
    }

    @Test
    @Suppress("Deprecation")
    fun watchfaceOverlayStyle() {
        val wallpaperService =
            TestWatchfaceOverlayStyleWatchFaceService(
                context,
                surfaceHolder,
                WatchFace.OverlayStyle(Color.valueOf(Color.RED), Color.valueOf(Color.BLACK))
            )

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        assertThat(interactiveInstance.overlayStyle.backgroundColor)
            .isEqualTo(Color.valueOf(Color.RED))
        assertThat(interactiveInstance.overlayStyle.foregroundColor)
            .isEqualTo(Color.valueOf(Color.BLACK))

        interactiveInstance.close()
    }

    @Test
    @Suppress("Deprecation")
    fun watchfaceOverlayStyle_after_close() {
        val wallpaperService =
            TestWatchfaceOverlayStyleWatchFaceService(
                context,
                surfaceHolder,
                WatchFace.OverlayStyle(Color.valueOf(Color.RED), Color.valueOf(Color.BLACK))
            )

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        interactiveInstance.close()

        assertThat(interactiveInstance.overlayStyle.backgroundColor).isNull()
        assertThat(interactiveInstance.overlayStyle.foregroundColor).isNull()
    }

    @Test
    @OptIn(ComplicationExperimental::class)
    fun edgeComplication_boundingArc() {
        val wallpaperService = TestEdgeComplicationWatchFaceService(context, surfaceHolder)

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        try {
            assertThat(interactiveInstance.complicationSlotsState.keys).containsExactly(123)

            val slot = interactiveInstance.complicationSlotsState[123]!!
            assertThat(slot.boundsType).isEqualTo(ComplicationSlotBoundsType.EDGE)
            assertThat(slot.getBoundingArc()).isEqualTo(BoundingArc(45f, 90f, 0.1f))
            assertThat(slot.bounds).isEqualTo(Rect(0, 0, 400, 400))
        } finally {
            interactiveInstance.close()
        }
    }

    @OptIn(WatchFaceClientExperimental::class, WatchFaceExperimental::class)
    @Test
    fun watchFaceColors() {
        val interactiveInstance = getOrCreateTestSubject()

        try {
            val watchFaceColorsLatch = CountDownLatch(1)
            var watchFaceColors: WatchFaceColors? = null

            interactiveInstance.addOnWatchFaceColorsListener({ runnable -> runnable.run() }) {
                watchFaceColors = it
                if (watchFaceColors != null) {
                    watchFaceColorsLatch.countDown()
                }
            }

            assertTrue(watchFaceColorsLatch.await(UPDATE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

            assertThat(watchFaceColors)
                .isEqualTo(
                    WatchFaceColors(
                        Color.valueOf(1.0f, 1.0f, 1.0f, 1.0f),
                        Color.valueOf(0.93333334f, 0.6313726f, 0.6039216f, 1.0f),
                        Color.valueOf(0.26666668f, 0.26666668f, 0.26666668f, 1.0f)
                    )
                )

            val watchFaceColorsLatch2 = CountDownLatch(1)
            var watchFaceColors2: WatchFaceColors? = null

            interactiveInstance.updateWatchFaceInstance(
                "testId",
                UserStyleData(
                    mapOf(
                        COLOR_STYLE_SETTING to BLUE_STYLE.encodeToByteArray(),
                        WATCH_HAND_LENGTH_STYLE_SETTING to DoubleRangeOption(0.9).id.value,
                    )
                )
            )

            interactiveInstance.addOnWatchFaceColorsListener({ runnable -> runnable.run() }) {
                watchFaceColors2 = it
                if (watchFaceColors2 != null) {
                    watchFaceColorsLatch2.countDown()
                }
            }

            assertTrue(watchFaceColorsLatch2.await(UPDATE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

            assertThat(watchFaceColors2)
                .isEqualTo(
                    WatchFaceColors(
                        Color.valueOf(0.30980393f, 0.7647059f, 0.96862745f, 1.0f),
                        Color.valueOf(0.08235294f, 0.39607844f, 0.7529412f, 1.0f),
                        Color.valueOf(0.26666668f, 0.26666668f, 0.26666668f, 1.0f)
                    )
                )
        } finally {
            interactiveInstance.close()
        }
    }

    @Test
    fun previewImageUpdateRequest() {
        val wallpaperService =
            TestWatchFaceServiceWithPreviewImageUpdateRequest(context, surfaceHolder)
        var lastPreviewImageUpdateRequestedId = ""

        val interactiveInstance =
            getOrCreateTestSubject(
                watchFaceService = wallpaperService,
                instanceId = "wfId-1",
                previewExecutor = { runnable -> runnable.run() },
                previewListener = { lastPreviewImageUpdateRequestedId = it }
            )

        assertTrue(
            wallpaperService.rendererInitializedLatch.await(
                UPDATE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS
            )
        )

        assertThat(lastPreviewImageUpdateRequestedId).isEmpty()
        wallpaperService.triggerPreviewImageUpdateRequest()
        assertThat(lastPreviewImageUpdateRequestedId).isEqualTo("wfId-1")

        interactiveInstance.close()
    }

    @Test
    fun engineDetached() {
        val wallpaperService =
            TestComplicationProviderDefaultsWatchFaceService(context, surfaceHolder)

        val interactiveInstance = getOrCreateTestSubject(wallpaperService)

        var lastDisconnectReason = 0
        interactiveInstance.addClientDisconnectListener(
            object : InteractiveWatchFaceClient.ClientDisconnectListener {
                override fun onClientDisconnected(@DisconnectReason disconnectReason: Int) {
                    lastDisconnectReason = disconnectReason
                }
            }
        ) {
            it.run()
        }

        // Simulate detach.
        engine.onDestroy()

        assertThat(lastDisconnectReason).isEqualTo(DisconnectReasons.ENGINE_DETACHED)
    }

    @Test
    fun tapComplication() {
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        interactiveInstance.updateComplicationData(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    rangedValueComplicationBuilder()
                        .setTapAction(ObservableServiceA.createPendingIntent(context))
                        .build()
            )
        )

        tapOnComplication(interactiveInstance, EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)

        assertTrue(ObservableServiceA.awaitForServiceToBeBound(UPDATE_TIMEOUT_MILLIS))
    }

    @Test
    fun tapTimelineComplication() {
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        val watchFaceImpl = runBlocking { engine.watchFaceDetails!!.deferredWatchFaceImpl.await() }

        // Create a timeline complication with three phases, each with their own tap actions leading
        // to ObservableServiceA, ObservableServiceB & ObservableServiceC getting bound.
        val timelineComplication =
            rangedValueComplicationBuilder()
                .setTapAction(ObservableServiceA.createPendingIntent(context))
                .build()
                .asWireComplicationData()

        timelineComplication.setTimelineEntryCollection(
            listOf(
                ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("B").build(),
                        ComplicationText.EMPTY
                    )
                    .setTapAction(ObservableServiceB.createPendingIntent(context))
                    .build()
                    .asWireComplicationData()
                    .apply {
                        timelineStartEpochSecond =
                            10 + TestExampleCanvasAnalogWatchFaceService.systemTimeMillis / 1000
                        timelineEndEpochSecond =
                            20 + TestExampleCanvasAnalogWatchFaceService.systemTimeMillis / 1000
                    },
                ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("C").build(),
                        ComplicationText.EMPTY
                    )
                    .setTapAction(ObservableServiceC.createPendingIntent(context))
                    .build()
                    .asWireComplicationData()
                    .apply {
                        timelineStartEpochSecond =
                            20 + TestExampleCanvasAnalogWatchFaceService.systemTimeMillis / 1000
                        timelineEndEpochSecond =
                            90 + TestExampleCanvasAnalogWatchFaceService.systemTimeMillis / 1000
                    }
            )
        )

        interactiveInstance.updateComplicationData(
            mapOf(
                EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                    timelineComplication.toApiComplicationData()
            )
        )

        // A tap should initially lead to TapTargetServiceA getting bound.
        tapOnComplication(interactiveInstance, EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)

        assertTrue(ObservableServiceA.awaitForServiceToBeBound(UPDATE_TIMEOUT_MILLIS))

        // Simulate the passage of time and force timeline entry selection by drawing.
        TestExampleCanvasAnalogWatchFaceService.systemTimeMillis += 15 * 1000
        watchFaceImpl.onDraw()

        // A tap should now lead to TapTargetServiceB getting bound.
        tapOnComplication(interactiveInstance, EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)

        assertTrue(ObservableServiceB.awaitForServiceToBeBound(UPDATE_TIMEOUT_MILLIS))

        // Simulate the passage of time and force timeline entry selection by drawing.
        TestExampleCanvasAnalogWatchFaceService.systemTimeMillis += 20 * 1000
        watchFaceImpl.onDraw()

        // A tap should now lead to TapTargetServiceC getting bound.
        tapOnComplication(interactiveInstance, EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)

        assertTrue(ObservableServiceC.awaitForServiceToBeBound(UPDATE_TIMEOUT_MILLIS))
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun overrideComplicationData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return
        }
        val wallpaperService =
            TestComplicationProviderDefaultsWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        interactiveInstance.updateComplicationData(
            mapOf(123 to rangedValueComplicationBuilder().build())
        )

        interactiveInstance.overrideComplicationData(
            mapOf(
                123 to
                    ShortTextComplicationData.Builder(
                            PlainComplicationText.Builder("TEST").build(),
                            ComplicationText.EMPTY
                        )
                        .build()
            )
        )

        interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
            Instant.ofEpochMilli(1234567),
            null,
            null
        )
        assertThat(wallpaperService.lastComplicationType).isEqualTo(ComplicationType.SHORT_TEXT)
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun clearComplicationDataOverride() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return
        }
        val wallpaperService =
            TestComplicationProviderDefaultsWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        interactiveInstance.updateComplicationData(
            mapOf(123 to rangedValueComplicationBuilder().build())
        )
        interactiveInstance.overrideComplicationData(
            mapOf(
                123 to
                    ShortTextComplicationData.Builder(
                            PlainComplicationText.Builder("TEST").build(),
                            ComplicationText.EMPTY
                        )
                        .build(),
            )
        )

        interactiveInstance.clearComplicationDataOverride()

        interactiveInstance.renderWatchFaceToBitmap(
            RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
            Instant.ofEpochMilli(1234567),
            null,
            null
        )
        assertThat(wallpaperService.lastComplicationType).isEqualTo(ComplicationType.RANGED_VALUE)
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun pauseAnimation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return
        }
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        waitForWatchFaceInstanceReady(interactiveInstance)

        interactiveInstance.pauseAnimation()

        // Not visible means paused.
        assertThat(wallpaperService.lastWatchState.isVisible.value).isFalse()
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun unpauseAnimation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return
        }
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        waitForWatchFaceInstanceReady(interactiveInstance)
        interactiveInstance.pauseAnimation()

        interactiveInstance.unpauseAnimation()

        // Visible means unpaused.
        assertThat(wallpaperService.lastWatchState.isVisible.value).isTrue()
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun closing_instance_unpausesAnimation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return
        }
        val wallpaperService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)
        val interactiveInstance = getOrCreateTestSubject(wallpaperService)
        waitForWatchFaceInstanceReady(interactiveInstance)
        interactiveInstance.pauseAnimation()

        interactiveInstance.close()

        // Visible means unpaused.
        assertThat(wallpaperService.lastWatchState.isVisible.value).isTrue()
    }
}

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
class WatchFaceControlClientScreenshotTest : WatchFaceControlClientTestBase() {
    @get:Rule
    val screenshotRule: AndroidXScreenshotTestRule =
        AndroidXScreenshotTestRule("wear/wear-watchface-client")

    private val exampleOpenGLWatchFaceComponentName =
        componentOf<ExampleOpenGLBackgroundInitWatchFaceService>()

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun getOrCreateInteractiveWatchFaceClient() {
        val interactiveInstance = getOrCreateTestSubject()

        val bitmap =
            interactiveInstance.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
                Instant.ofEpochMilli(1234567),
                null,
                complications
            )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "interactiveScreenshot")
        } finally {
            interactiveInstance.close()
        }
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun getOrCreateInteractiveWatchFaceClient_initialStyle() {
        val interactiveInstance =
            getOrCreateTestSubject(
                // An incomplete map which is OK.
                userStyle =
                    UserStyleData(
                        mapOf(
                            "color_style_setting" to "green_style".encodeToByteArray(),
                            "draw_hour_pips_style_setting" to BooleanOption.FALSE.id.value,
                            "watch_hand_length_style_setting" to DoubleRangeOption(0.8).id.value
                        )
                    )
            )

        val bitmap =
            interactiveInstance.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
                Instant.ofEpochMilli(1234567),
                null,
                complications
            )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "initialStyle")
        } finally {
            interactiveInstance.close()
        }
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun getOrCreateInteractiveWatchFaceClient_existingOpenInstance_styleChange() {
        val watchFaceService = TestExampleCanvasAnalogWatchFaceService(context, surfaceHolder)

        val testId = "testId"

        getOrCreateTestSubject(watchFaceService, instanceId = testId)

        val deferredInteractiveInstance2 =
            handlerCoroutineScope.async {
                @Suppress("deprecation")
                service.getOrCreateInteractiveWatchFaceClient(
                    testId,
                    deviceConfig,
                    systemState,
                    UserStyleData(
                        mapOf(
                            "color_style_setting" to "blue_style".encodeToByteArray(),
                            "draw_hour_pips_style_setting" to BooleanOption.FALSE.id.value,
                            "watch_hand_length_style_setting" to DoubleRangeOption(0.25).id.value
                        )
                    ),
                    complications
                )
            }

        val interactiveInstance2 = awaitWithTimeout(deferredInteractiveInstance2)
        assertThat(interactiveInstance2.instanceId).isEqualTo("testId")

        val bitmap =
            interactiveInstance2.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
                Instant.ofEpochMilli(1234567),
                null,
                complications
            )

        try {
            // Note the hour hand pips and both complicationSlots should be visible in this image.
            bitmap.assertAgainstGolden(screenshotRule, "existingOpenInstance_styleChange")
        } finally {
            interactiveInstance2.close()
        }
    }

    @SuppressLint("NewApi") // renderWatchFaceToBitmap
    @Test
    fun updateInstance() {
        val interactiveInstance =
            getOrCreateTestSubject(
                userStyle =
                    UserStyleData(
                        mapOf(
                            COLOR_STYLE_SETTING to GREEN_STYLE.encodeToByteArray(),
                            WATCH_HAND_LENGTH_STYLE_SETTING to DoubleRangeOption(0.25).id.value,
                            DRAW_HOUR_PIPS_STYLE_SETTING to BooleanOption.FALSE.id.value,
                            COMPLICATIONS_STYLE_SETTING to NO_COMPLICATIONS.encodeToByteArray()
                        )
                    )
            )

        assertThat(interactiveInstance.instanceId).isEqualTo("testId")

        // Note this map doesn't include all the categories, which is fine the others will be set
        // to their defaults.
        interactiveInstance.updateWatchFaceInstance(
            "testId2",
            UserStyleData(
                mapOf(
                    COLOR_STYLE_SETTING to BLUE_STYLE.encodeToByteArray(),
                    WATCH_HAND_LENGTH_STYLE_SETTING to DoubleRangeOption(0.9).id.value,
                )
            )
        )

        assertThat(interactiveInstance.instanceId).isEqualTo("testId2")

        // It should be possible to create an instance with the updated id.
        val instance = service.getInteractiveWatchFaceClientInstance("testId2")
        assertThat(instance).isNotNull()
        instance?.close()

        // The previous instance should still be usable despite the new instance being closed.
        interactiveInstance.updateComplicationData(complications)
        val bitmap =
            interactiveInstance.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
                Instant.ofEpochMilli(1234567),
                null,
                complications
            )

        try {
            // Note the hour hand pips and both complicationSlots should be visible in this image.
            bitmap.assertAgainstGolden(screenshotRule, "setUserStyle")
        } finally {
            interactiveInstance.close()
        }
    }

    @Ignore // b/225230182
    @Test
    fun interactiveAndHeadlessOpenGlWatchFaceInstances() {
        val surfaceTexture = SurfaceTexture(false)
        surfaceTexture.setDefaultBufferSize(400, 400)
        Mockito.`when`(surfaceHolder2.surface).thenReturn(Surface(surfaceTexture))
        Mockito.`when`(surfaceHolder2.surfaceFrame).thenReturn(Rect(0, 0, 400, 400))

        val wallpaperService =
            TestExampleOpenGLBackgroundInitWatchFaceService(context, surfaceHolder2)

        val interactiveInstance =
            getOrCreateTestSubject(wallpaperService, complications = emptyMap())

        val headlessInstance =
            HeadlessWatchFaceClient.createFromBundle(
                service
                    .createHeadlessWatchFaceClient(
                        "id",
                        exampleOpenGLWatchFaceComponentName,
                        deviceConfig,
                        200,
                        200
                    )!!
                    .toBundle()
            )

        // Take screenshots from both instances to confirm rendering works as expected despite the
        // watch face using shared SharedAssets.
        val interactiveBitmap =
            interactiveInstance.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
                Instant.ofEpochMilli(1234567),
                null,
                null
            )

        interactiveBitmap.assertAgainstGolden(screenshotRule, "opengl_interactive")

        val headlessBitmap =
            headlessInstance.renderWatchFaceToBitmap(
                RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
                Instant.ofEpochMilli(1234567),
                null,
                null
            )

        headlessBitmap.assertAgainstGolden(screenshotRule, "opengl_headless")

        headlessInstance.close()
        interactiveInstance.close()
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun userStyleFlavors() {
        val interactiveInstance = getOrCreateTestSubject()

        assertThat(interactiveInstance.getUserStyleFlavors().flavors)
            .contains(
                UserStyleFlavor(
                    "exampleFlavor",
                    UserStyleData(
                        mapOf(
                            "color_style_setting" to UserStyleSetting.Option.Id("blue_style").value,
                            "watch_hand_length_style_setting" to DoubleRangeOption(1.0).id.value
                        )
                    ),
                    mapOf(
                        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                            DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
                                ComplicationType.SHORT_TEXT
                            ),
                        EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                            DefaultComplicationDataSourcePolicy(
                                ComponentName(
                                    "androidx.wear.watchface.complications.datasource.samples",
                                    "androidx.wear.watchface.complications.datasource.samples" +
                                        ".ConfigurableDataSourceService"
                                ),
                                ComplicationType.SHORT_TEXT,
                                SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET,
                                ComplicationType.SHORT_TEXT
                            )
                    )
                )
            )
        interactiveInstance.close()
    }
}
