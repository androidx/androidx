/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.client.guava

import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.ListenableWatchFaceControlClient
import androidx.wear.watchface.client.WatchUiState
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.truth.Truth.assertThat
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit

private const val TIMEOUT_MS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
public class ListenableWatchFaceControlClientTest {

    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var surfaceHolder: SurfaceHolder
    @Mock private lateinit var surface: Surface

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    public fun setUp() {
        Mockito.`when`(surfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 400, 400))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun headlessSchemaSettingIds() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val headlessInstance =
            client.createHeadlessWatchFaceClient(
                "id",
                ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
                DeviceConfig(false, false, 0, 0),
                400,
                400
            )!!

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.map { it.id.value })
            .containsExactly(
                "color_style_setting",
                "draw_hour_pips_style_setting",
                "watch_hand_length_style_setting",
                "complications_style_setting",
                "hours_draw_freq_style_setting"
            )

        headlessInstance.close()
        client.close()
    }

    @Test
    public fun createHeadlessWatchFaceClient_nonExistentWatchFaceComponentName() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        assertNull(
            client.createHeadlessWatchFaceClient(
                "id",
                ComponentName("?", "i.do.not.exist"),
                DeviceConfig(false, false, 0, 0),
                400,
                400
            )
        )
        client.close()
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun listenableGetOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        @Suppress("deprecation")
        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(false, false, 0, 0),
                WatchUiState(false, 0),
                null,
                null
            )

        val service =
            object : ExampleCanvasAnalogWatchFaceService() {
                init {
                    attachBaseContext(context)
                }
            }
        service
            .onCreateEngine()
            .onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )

        val interactiveInstance = interactiveInstanceFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(interactiveInstance.userStyleSchema.userStyleSettings.map { it.id.value })
            .containsExactly(
                "color_style_setting",
                "draw_hour_pips_style_setting",
                "watch_hand_length_style_setting",
                "complications_style_setting",
                "hours_draw_freq_style_setting"
            )

        interactiveInstance.close()
        client.close()
    }

    @Test
    public fun createMultipleHeadlessInstances() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val headlessInstance1 =
            client.createHeadlessWatchFaceClient(
                "id1",
                ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
                DeviceConfig(false, false, 0, 0),
                400,
                400
            )!!

        val headlessInstance2 =
            client.createHeadlessWatchFaceClient(
                "id2",
                ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
                DeviceConfig(false, false, 0, 0),
                400,
                400
            )!!

        val headlessInstance3 =
            client.createHeadlessWatchFaceClient(
                "id3",
                ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
                DeviceConfig(false, false, 0, 0),
                400,
                400
            )!!

        headlessInstance3.close()
        headlessInstance2.close()
        headlessInstance1.close()
        client.close()
    }

    @Test
    public fun createInteractiveAndHeadlessInstances() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        @Suppress("deprecation")
        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(false, false, 0, 0),
                WatchUiState(false, 0),
                null,
                null
            )

        val service =
            object : ExampleCanvasAnalogWatchFaceService() {
                init {
                    attachBaseContext(context)
                }
            }
        service
            .onCreateEngine()
            .onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )

        val interactiveInstance = interactiveInstanceFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        val headlessInstance1 =
            client.createHeadlessWatchFaceClient(
                "id",
                ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
                DeviceConfig(false, false, 0, 0),
                400,
                400
            )!!

        headlessInstance1.close()
        interactiveInstance.close()
        client.close()
    }

    @Test
    public fun getInteractiveWatchFaceInstanceSysUI_notExist() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        assertNull(client.getInteractiveWatchFaceClientInstance("I do not exist"))
    }

    @Test
    public fun createWatchFaceControlClient_cancel() {
        ListenableWatchFaceControlClient.createWatchFaceControlClient(context, context.packageName)
            .cancel(true)

        // Canceling should not prevent a subsequent createWatchFaceControlClient.
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(client).isNotNull()
        client.close()
    }

    @Test
    public fun hasComplicationDataCache_oldApi_returnsFalse() {
        WatchFaceControlTestService.apiVersionOverride = 3
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        assertFalse(client.hasComplicationDataCache())
    }

    @Test
    public fun hasComplicationDataCache_newApi_returnsTrue() {
        WatchFaceControlTestService.apiVersionOverride = 4
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        assertTrue(client.hasComplicationDataCache())
    }

    @Test
    public fun listenableGetOrCreateInteractiveWatchFaceClient_cancel() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        @Suppress("deprecation")
        client
            .listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(false, false, 0, 0),
                WatchUiState(false, 0),
                null,
                null
            )
            .cancel(true)

        // Canceling should not prevent a subsequent listenableGetOrCreateInteractiveWatchFaceClient
        @Suppress("deprecation")
        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(false, false, 0, 0),
                WatchUiState(false, 0),
                null,
                null
            )
        val service =
            object : ExampleCanvasAnalogWatchFaceService() {
                init {
                    attachBaseContext(context)
                }
            }
        service
            .onCreateEngine()
            .onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )

        val interactiveInstance = interactiveInstanceFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(interactiveInstance).isNotNull()
        interactiveInstance.close()
        client.close()
    }

    @Test
    public fun previewImageUpdateRequestedListener() {
        val client =
            ListenableWatchFaceControlClient.createWatchFaceControlClient(
                    context,
                    context.packageName
                )
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        var lastPreviewImageUpdateRequestedId = ""
        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(false, false, 0, 0),
                WatchUiState(false, 0),
                null,
                null,
                { runnable -> runnable.run() },
                { lastPreviewImageUpdateRequestedId = it }
            )

        val service = TestWatchFaceServiceWithPreviewImageUpdateRequest(context, surfaceHolder)
        service
            .onCreateEngine()
            .onSurfaceChanged(
                surfaceHolder,
                0,
                surfaceHolder.surfaceFrame.width(),
                surfaceHolder.surfaceFrame.height()
            )

        val interactiveInstance = interactiveInstanceFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        Assert.assertTrue(service.rendererInitializedLatch.await(500, TimeUnit.MILLISECONDS))

        assertThat(lastPreviewImageUpdateRequestedId).isEmpty()
        service.triggerPreviewImageUpdateRequest()
        assertThat(lastPreviewImageUpdateRequestedId).isEqualTo("listenableTestId")

        interactiveInstance.close()
    }
}

internal class TestWatchFaceServiceWithPreviewImageUpdateRequest(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
) : WatchFaceService() {
    val rendererInitializedLatch = CountDownLatch(1)

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    @Suppress("deprecation") private lateinit var renderer: Renderer.CanvasRenderer

    fun triggerPreviewImageUpdateRequest() {
        renderer.sendPreviewImageNeedsUpdateRequest()
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        @Suppress("deprecation")
        renderer =
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16
                ) {
                override suspend fun init() {
                    rendererInitializedLatch.countDown()
                }

                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {}
            }
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}
