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

package androidx.wear.watchface

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.ListenableWatchFaceControlClient
import androidx.wear.watchface.client.WatchUiState
import androidx.wear.watchface.control.WatchFaceControlServiceFactory
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

private const val TIMEOUT_MS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
public class ListenableWatchFaceControlClientTest {

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder
    @Mock
    private lateinit var surface: Surface

    @Before
    public fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
    }

    @Test
    public fun headlessSchemaSettingIds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val headlessInstance = client.createHeadlessWatchFaceClient(
            ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!

        assertThat(headlessInstance.userStyleSchema.userStyleSettings.map { it.id.value })
            .containsExactly(
                "color_style_setting",
                "draw_hour_pips_style_setting",
                "watch_hand_length_style_setting",
                "complications_style_setting"
            )

        headlessInstance.close()
        client.close()
    }

    @Test
    public fun createHeadlessWatchFaceClient_nonExistentWatchFaceComponentName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        assertNull(
            client.createHeadlessWatchFaceClient(
                ComponentName("?", "i.do.not.exist"),
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                400,
                400
            )
        )
        client.close()
    }

    @Test
    public fun listenableGetOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                null,
                null
            )

        val service = object : ExampleCanvasAnalogWatchFaceService() {
            init {
                attachBaseContext(context)
            }
        }
        service.onCreateEngine().onSurfaceChanged(
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
                "complications_style_setting"
            )

        interactiveInstance.close()
        client.close()
    }

    @Test
    public fun createMultipleHeadlessInstances() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val headlessInstance1 = client.createHeadlessWatchFaceClient(
            ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!

        val headlessInstance2 = client.createHeadlessWatchFaceClient(
            ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!

        val headlessInstance3 = client.createHeadlessWatchFaceClient(
            ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                null,
                null
            )

        val service = object : ExampleCanvasAnalogWatchFaceService() {
            init {
                attachBaseContext(context)
            }
        }
        service.onCreateEngine().onSurfaceChanged(
            surfaceHolder,
            0,
            surfaceHolder.surfaceFrame.width(),
            surfaceHolder.surfaceFrame.height()
        )

        val interactiveInstance = interactiveInstanceFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        val headlessInstance1 = client.createHeadlessWatchFaceClient(
            ComponentName(context, ExampleCanvasAnalogWatchFaceService::class.java),
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            400,
            400
        )!!

        headlessInstance1.close()
        interactiveInstance.close()
        client.close()
    }

    @Test
    public fun getInteractiveWatchFaceInstanceSysUI_notExist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        assertNull(client.getInteractiveWatchFaceClientInstance("I do not exist"))
    }

    @Test
    public fun createWatchFaceControlClient_cancel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).cancel(true)

        // Canceling should not prevent a subsequent createWatchFaceControlClient.
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(client).isNotNull()
        client.close()
    }

    @Test
    public fun listenableGetOrCreateInteractiveWatchFaceClient_cancel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ListenableWatchFaceControlClient.createWatchFaceControlClient(
            context,
            context.packageName
        ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        client.listenableGetOrCreateInteractiveWatchFaceClient(
            "listenableTestId",
            DeviceConfig(
                false,
                false,
                0,
                0
            ),
            WatchUiState(false, 0),
            null,
            null
        ).cancel(true)

        // Canceling should not prevent a subsequent listenableGetOrCreateInteractiveWatchFaceClient
        val interactiveInstanceFuture =
            client.listenableGetOrCreateInteractiveWatchFaceClient(
                "listenableTestId",
                DeviceConfig(
                    false,
                    false,
                    0,
                    0
                ),
                WatchUiState(false, 0),
                null,
                null
            )
        val service = object : ExampleCanvasAnalogWatchFaceService() {
            init {
                attachBaseContext(context)
            }
        }
        service.onCreateEngine().onSurfaceChanged(
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
}

public class TestWatchFaceControlService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return WatchFaceControlServiceFactory.createWatchFaceControlService(
            ApplicationProvider.getApplicationContext(),
            Handler(Looper.getMainLooper())
        ).asBinder()
    }
}
