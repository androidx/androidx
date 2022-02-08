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

package androidx.camera.extensions

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    private lateinit var baseCameraSelector: CameraSelector

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(
                lensFacing,
                extensionMode
            )
        )

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    @Test
    fun canBindToLifeCycleAndTakePicture(): Unit = runBlocking {
        val mockOnImageCapturedCallback = Mockito.mock(
            ImageCapture.OnImageCapturedCallback::class.java
        )

        // To test bind/unbind and take picture.
        val imageCapture = ImageCapture.Builder().build()
        val preview = Preview.Builder().build()
        withContext(Dispatchers.Main) {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(
                    object : SurfaceTextureCallback {
                        override fun onSurfaceTextureReady(
                            surfaceTexture: SurfaceTexture,
                            resolution: Size
                        ) {
                            // No-op.
                        }

                        override fun onSafeToRelease(
                            surfaceTexture: SurfaceTexture
                        ) {
                            // No-op.
                        }
                    })
            )

            val fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }

            val extensionsCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
                baseCameraSelector,
                extensionMode
            )

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                preview,
                imageCapture
            )

            imageCapture.takePicture(
                CameraXExecutors.mainThreadExecutor(),
                mockOnImageCapturedCallback
            )
        }

        // Verify the image captured.
        val imageProxy = ArgumentCaptor.forClass(
            ImageProxy::class.java
        )
        Mockito.verify(mockOnImageCapturedCallback, Mockito.timeout(10000)).onCaptureSuccess(
            imageProxy.capture()
        )
        assertThat(imageProxy.value).isNotNull()
        imageProxy.value.close() // Close the image after verification.

        // Verify the take picture should not have any error happen.
        Mockito.verify(mockOnImageCapturedCallback, Mockito.never()).onError(
            ArgumentMatchers.any(
                ImageCaptureException::class.java
            )
        )
    }
}