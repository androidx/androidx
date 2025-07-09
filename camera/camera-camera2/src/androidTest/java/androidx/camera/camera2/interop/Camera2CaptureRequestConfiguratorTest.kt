/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.interop

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class Camera2CaptureRequestConfiguratorTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private lateinit var cameraSelector: CameraSelector

    @Before
    fun setUp() {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
    }

    @After
    fun tearDown(): Unit =
        runBlocking(Dispatchers.Main) {
            try {
                val provider = ProcessCameraProvider.getInstance(context).await()
                provider.shutdownAsync().await()
            } catch (e: IllegalStateException) {
                // ProcessCameraProvider may not be configured. Ignore.
            }
        }

    @Test
    fun setCaptureRequestConfigurator_canGetFromConfig() {
        val captureRequestConfigurator = Camera2CaptureRequestConfigurator {}
        val cameraXConfig: CameraXConfig =
            CameraXConfig.Builder()
                .setCamera2CaptureRequestConfigurator(captureRequestConfigurator)
                .build()

        assertThat(cameraXConfig.getCamera2CaptureRequestConfigurator())
            .isSameInstanceAs(captureRequestConfigurator)
    }

    @Test
    fun setCaptureRequestConfigurator_configuratorIsCalled(): Unit = runBlocking {
        val lifecycleOwner = FakeLifecycleOwner()
        val frameAvailableSemaphore = Semaphore(0)
        var targetFrameRate = Range(15, 15)
        val captureRequests: MutableList<CaptureRequest?> = mutableListOf()
        val cameraConfigWithConfigurator =
            CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                .setCamera2CaptureRequestConfigurator { captureRequest ->
                    captureRequests.add(captureRequest)
                }
                .build()
        ProcessCameraProvider.configureInstance(cameraConfigWithConfigurator)
        var preview: Preview
        withContext(Dispatchers.Main) {
            val provider = ProcessCameraProvider.awaitInstance(context)
            preview =
                Preview.Builder().setTargetFrameRate(targetFrameRate).build().apply {
                    surfaceProvider =
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                            frameAvailableSemaphore.release()
                        }
                }
            lifecycleOwner.startAndResume()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        // Wait for the preview to start.
        frameAvailableSemaphore.tryAcquire(10, TimeUnit.SECONDS)
        // Get the actual frame rate resulted from the stream spec.
        preview.attachedStreamSpec?.let { targetFrameRate = it.expectedFrameRateRange }

        var valueMatched = false
        captureRequests.forEach {
            if (it!!.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE) == targetFrameRate)
                valueMatched = true
        }
        assertThat(valueMatched).isTrue()
    }
}
