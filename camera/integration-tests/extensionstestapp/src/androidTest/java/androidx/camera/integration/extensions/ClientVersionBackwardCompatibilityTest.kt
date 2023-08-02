/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * These tests ensures OEM Extensions implementation can work with earlier Extensions-Interface
 * clients. For example, OEM implements v1.4.0 Extensions-Interface, these tests ensure it works
 * well not only on CameraX app with v1.4.0 Extensions-Interface but also on apps with v1.3.0 and
 * prior versions.
 *
 * For app-supplied callback methods, OEMs should use default methods that was added from 2023/Jun
 * to prevent from crash in older client versions. Please note this test can't detect the failure if
 * OEMs didn't use default methods.
 *
 * For variants of the overloaded API methods, OEMs should implement all of it to ensure it works
 * well on older client versions.
 *
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ClientVersionBackwardCompatibilityTest(private val config: CameraIdExtensionModePair) {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraIdExtensionModePair>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking(Dispatchers.Main) {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()
        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(config.cameraId)
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            withContext(Dispatchers.Main) {
                extensionsManager.shutdown()[10, TimeUnit.SECONDS]
            }
        }
    }

    private suspend fun assertPreviewAndImageCaptureWorking(clientVersion: String) {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
            clientVersion
        )[10000, TimeUnit.MILLISECONDS]
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, config.extensionMode))
        extensionCameraSelector = extensionsManager
            .getExtensionEnabledCameraSelector(baseCameraSelector, config.extensionMode)

        val previewFrameLatch = CountDownLatch(1)
        val captureLatch = CountDownLatch(1)

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    previewFrameLatch.countDown()
                }
            )
            cameraProvider.bindToLifecycle(
                lifecycleOwner, extensionCameraSelector,
                preview, imageCapture
            )
        }

        assertThat(previewFrameLatch.await(3, TimeUnit.SECONDS)).isTrue()
        imageCapture.takePicture(CameraXExecutors.ioExecutor(),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    captureLatch.countDown()
                }
            })
        assertThat(captureLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun previewImageCaptureWork_clientVersion_1_1_0() = runBlocking {
        assertPreviewAndImageCaptureWorking(clientVersion = "1.1.0")
    }

    @Test
    fun previewImageCaptureWork_clientVersion_1_2_0() = runBlocking {
        assertPreviewAndImageCaptureWorking(clientVersion = "1.2.0")
    }

    @Test
    fun previewImageCaptureWork_clientVersion_1_3_0() = runBlocking {
        assertPreviewAndImageCaptureWorking(clientVersion = "1.3.0")
    }

    @Test
    fun previewImageCaptureWork_clientVersion_1_4_0() = runBlocking {
        assertPreviewAndImageCaptureWorking(clientVersion = "1.4.0")
    }
}
