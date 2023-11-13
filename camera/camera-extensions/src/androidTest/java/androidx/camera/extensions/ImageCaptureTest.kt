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
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    private lateinit var baseCameraSelector: CameraSelector

    private lateinit var extensionsCameraSelector: CameraSelector

    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking {
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

        extensionsCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
        }
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
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

        bindAndTakePicture(mockOnImageCapturedCallback)

        // Verify the image captured.
        val imageProxy = ArgumentCaptor.forClass(
            ImageProxy::class.java
        )

        Mockito.verify(mockOnImageCapturedCallback, Mockito.timeout(5000).times(1))
            .onCaptureStarted()
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

    fun canBindToLifeCycleAndTakePicture_diskIo(): Unit = runBlocking {
        val mockOnImageSavedCallback = Mockito.mock(
            ImageCapture.OnImageSavedCallback::class.java
        )

        bindAndTakePicture(mockOnImageSavedCallback)

        // Verify the image captured.
        val outputFileResults = ArgumentCaptor.forClass(
            ImageCapture.OutputFileResults::class.java
        )

        Mockito.verify(mockOnImageSavedCallback, Mockito.timeout(5000).times(1))
            .onCaptureStarted()

        Mockito.verify(mockOnImageSavedCallback, Mockito.timeout(10000)).onImageSaved(
            outputFileResults.capture()
        )
        assertThat(outputFileResults.value).isNotNull()

        // Verify the take picture should not have any error happen.
        Mockito.verify(mockOnImageSavedCallback, Mockito.never()).onError(
            ArgumentMatchers.any(
                ImageCaptureException::class.java
            )
        )
    }

    private fun isCaptureProcessProgressSupported(): Boolean = runBlocking {
        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector
            )
        }

        val capabilities = ImageCapture.getImageCaptureCapabilities(camera.cameraInfo)
        capabilities.isCaptureProcessProgressSupported
    }

    private fun isPostviewSupported(): Boolean = runBlocking {
        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector
            )
        }

        val capabilities = ImageCapture.getImageCaptureCapabilities(camera.cameraInfo)
        capabilities.isPostviewSupported
    }

    private suspend fun bindAndTakePicture(
        onImageCaptureCallback: ImageCapture.OnImageCapturedCallback,
        enablePostview: Boolean = false
    ) {
        // To test bind/unbind and take picture.
        val imageCapture = ImageCapture.Builder()
            .setPostviewEnabled(enablePostview)
            .build()
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

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                preview,
                imageCapture
            )

            imageCapture.takePicture(
                CameraXExecutors.mainThreadExecutor(),
                onImageCaptureCallback
            )
        }
    }

    private suspend fun bindAndTakePicture(
        onImageSavedCallback: ImageCapture.OnImageSavedCallback,
        enablePostview: Boolean = false
    ) {
        // To test bind/unbind and take picture.
        val imageCapture = ImageCapture.Builder()
            .setPostviewEnabled(enablePostview)
            .build()
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

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                preview,
                imageCapture
            )

            val saveLocation = temporaryFolder.newFile("test.jpg")
            val outputFileOptions = ImageCapture.OutputFileOptions
                .Builder(saveLocation)
                .build()
            imageCapture.takePicture(
                outputFileOptions,
                CameraXExecutors.mainThreadExecutor(),
                onImageSavedCallback
            )
        }
    }

    @Test
    fun canBindToLifeCycleAndTakePictureWithCaptureProcessProgress(): Unit = runBlocking {
        assumeTrue(isCaptureProcessProgressSupported())

        val mockOnImageCapturedCallback = Mockito.mock(
            ImageCapture.OnImageCapturedCallback::class.java
        )

        bindAndTakePicture(mockOnImageCapturedCallback)

        // Verify the image captured.
        val imageProxy = ArgumentCaptor.forClass(
            ImageProxy::class.java
        )

        Mockito.verify(mockOnImageCapturedCallback, Mockito.timeout(5000).times(1))
            .onCaptureStarted()

        Mockito.verify(mockOnImageCapturedCallback, Mockito.timeout(8000).atLeastOnce())
            .onCaptureProcessProgressed(ArgumentMatchers.anyInt())

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

    @Test
    fun canBindToLifeCycleAndTakePictureWithCaptureProcessProgress_diskIo(): Unit = runBlocking {
        assumeTrue(isCaptureProcessProgressSupported())

        val mockOnImageSavedCallback = Mockito.mock(
            ImageCapture.OnImageSavedCallback::class.java
        )

        bindAndTakePicture(mockOnImageSavedCallback)

        // Verify the image captured.
        val outputFileResults = ArgumentCaptor.forClass(
            ImageCapture.OutputFileResults::class.java
        )

        Mockito.verify(mockOnImageSavedCallback, Mockito.timeout(5000).times(1))
            .onCaptureStarted()

        Mockito.verify(mockOnImageSavedCallback, Mockito.timeout(8000).atLeastOnce())
            .onCaptureProcessProgressed(ArgumentMatchers.anyInt())

        Mockito.verify(mockOnImageSavedCallback, Mockito.timeout(10000)).onImageSaved(
            outputFileResults.capture()
        )

        assertThat(outputFileResults.value).isNotNull()

        // Verify the take picture should not have any error happen.
        Mockito.verify(mockOnImageSavedCallback, Mockito.never()).onError(
            ArgumentMatchers.any(
                ImageCaptureException::class.java
            )
        )
    }

    private fun isRotationOptionSupportedDevice() =
        ExifRotationAvailability().isRotationOptionSupported

    @Test
    fun canBindToLifeCycleAndTakePictureWithPostview(): Unit = runBlocking {
        assumeTrue(isPostviewSupported())

        val captureStartedDeferred = CompletableDeferred<Boolean>()
        val captureSuccessDeferred = CompletableDeferred<ImageProxy>()
        val PostviewDeferred = CompletableDeferred<ImageProxy>()
        var hasError = false

        bindAndTakePicture(object : ImageCapture.OnImageCapturedCallback() {
            override fun onError(exception: ImageCaptureException) {
                hasError = true
            }
            override fun onCaptureStarted() {
                captureStartedDeferred.complete(true)
            }
            override fun onCaptureSuccess(image: ImageProxy) {
                captureSuccessDeferred.complete(image)
            }
            override fun onPostviewImageAvailable(image: ImageProxy) {
                PostviewDeferred.complete(image)
            }
        }, enablePostview = true)

        assertThat(withTimeoutOrNull(5000) { captureStartedDeferred.await() }).isTrue()

        withTimeoutOrNull(5000) { PostviewDeferred.await() }.use {
            assertThat(it).isNotNull()
            assertThat(it!!.format).isEqualTo(ImageFormat.JPEG)
            if (isRotationOptionSupportedDevice()) {
                val exif = ExifUtil.getExif(it)
                assertThat(exif!!.rotation).isEqualTo(it.imageInfo.rotationDegrees)
            }
        }

        withTimeoutOrNull(7000) { captureSuccessDeferred.await() }.use {
            assertThat(it).isNotNull()
            assertThat(it!!.format).isEqualTo(ImageFormat.JPEG)
            if (isRotationOptionSupportedDevice()) {
                val exif = ExifUtil.getExif(it)
                assertThat(exif!!.rotation).isEqualTo(it.imageInfo.rotationDegrees)
            }
        }

        assertThat(hasError).isFalse()
    }

    @Test
    fun canBindToLifeCycleAndTakePictureWithPostview_diskIo(): Unit = runBlocking {
        assumeTrue(isPostviewSupported())

        val captureStartedDeferred = CompletableDeferred<Boolean>()
        val imageSavedDeferred = CompletableDeferred<ImageCapture.OutputFileResults>()
        val PostviewDeferred = CompletableDeferred<ImageProxy>()
        var hasError = false

        bindAndTakePicture(object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                hasError = true
            }
            override fun onCaptureStarted() {
                captureStartedDeferred.complete(true)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                imageSavedDeferred.complete(outputFileResults)
            }
            override fun onPostviewImageAvailable(image: ImageProxy) {
                PostviewDeferred.complete(image)
            }
        }, enablePostview = true)

        assertThat(withTimeoutOrNull(5000) { captureStartedDeferred.await() }).isTrue()

        withTimeoutOrNull(5000) { PostviewDeferred.await() }.use {
            assertThat(it).isNotNull()
            assertThat(it!!.format).isEqualTo(ImageFormat.JPEG)
            if (isRotationOptionSupportedDevice()) {
                val exif = ExifUtil.getExif(it)
                assertThat(exif!!.rotation).isEqualTo(it.imageInfo.rotationDegrees)
            }
        }

        assertThat(withTimeoutOrNull(7000) { imageSavedDeferred.await() }).isNotNull()

        assertThat(hasError).isFalse()
    }

    @Test
    fun highResolutionDisabled_whenExtensionsEnabled(): Unit = runBlocking {
        val imageCapture = ImageCapture.Builder().build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                imageCapture)
        }

        assertThat(imageCapture.currentConfig.isHigResolutionDisabled(false)).isTrue()
    }
}
