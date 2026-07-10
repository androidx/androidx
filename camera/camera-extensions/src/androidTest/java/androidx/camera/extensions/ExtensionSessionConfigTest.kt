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

package androidx.camera.extensions

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraConfigProvider
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
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
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@LargeTest
@RunWith(Parameterized::class)
class ExtensionSessionConfigTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int,
) {

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule
    val wakelockEmptyActivityRule = WakelockEmptyActivityRule(brandsToEnable = listOf("vivo"))

    @get:Rule val temporaryFolder = TemporaryFolder(context.cacheDir)

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch
    private lateinit var finalize: VideoRecordEvent.Finalize
    private lateinit var recording: Recording
    private val videoRecordEventListener =
        Consumer<VideoRecordEvent> {
            when (it) {
                is VideoRecordEvent.Start -> {}
                is VideoRecordEvent.Finalize -> {
                    finalize = it
                    latchForVideoSaved.countDown()
                }
                is VideoRecordEvent.Status -> {
                    latchForVideoRecording.countDown()
                }
                is VideoRecordEvent.Pause,
                is VideoRecordEvent.Resume -> {}
                else -> {
                    throw IllegalStateException()
                }
            }
        }

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(lensFacing, extensionMode)
        )
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        assumeTrue(
            ExtensionsTestUtil.isExtensionAvailable(extensionsManager, lensFacing, extensionMode)
        )

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
        }
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.unbindAll() }
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L
        val TAG = "ExtensionSessionConfigTest"
        val context: Context = ApplicationProvider.getApplicationContext()

        @JvmStatic
        @Parameterized.Parameters(name = "mode = {0}, facing = {1}")
        fun data(): Collection<Array<Any>> {
            return ExtensionsTestUtil.getAllExtensionsLensFacingCombinations(context, true)
        }
    }

    @Test
    fun canBindToLifeCycleAndTakeJpegPicture(): Unit = runBlocking {
        val cameraInfo =
            cameraProvider.getCameraInfo(
                baseCameraSelector,
                ExtensionSessionConfig(extensionMode, extensionsManager),
            )

        val isPostviewSupported =
            ImageCapture.getImageCaptureCapabilities(cameraInfo).isPostviewSupported

        val imageCaptureBuilder = ImageCapture.Builder()
        if (isPostviewSupported) {
            imageCaptureBuilder.setPostviewEnabled(true)
        }
        val imageCapture = imageCaptureBuilder.build()
        val preview = Preview.Builder().build()
        // Uses constructor to create the ExtensionSessionConfig
        val sessionConfig =
            ExtensionSessionConfig(extensionMode, extensionsManager, preview, imageCapture)
        val previewReady = CountDownLatch(1)

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    previewReady.countDown()
                }
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector, sessionConfig)
        }

        // Wait for preview to be ready
        assertThat(previewReady.await(5000, TimeUnit.MILLISECONDS)).isTrue()

        val mockOnImageCapturedCallback =
            Mockito.mock(ImageCapture.OnImageCapturedCallback::class.java)

        imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(), mockOnImageCapturedCallback)

        if (isPostviewSupported) {
            val bitmap = ArgumentCaptor.forClass(Bitmap::class.java)
            Mockito.verify(mockOnImageCapturedCallback, Mockito.timeout(10000))
                .onPostviewBitmapAvailable(bitmap.capture())
            assertThat(bitmap.value).isNotNull()
        }

        val imageProxy = ArgumentCaptor.forClass(ImageProxy::class.java)
        Mockito.verify(mockOnImageCapturedCallback, Mockito.timeout(15000))
            .onCaptureSuccess(imageProxy.capture())
        assertThat(imageProxy.value).isNotNull()
        imageProxy.value.close()

        Mockito.verify(mockOnImageCapturedCallback, Mockito.never())
            .onError(ArgumentMatchers.any(ImageCaptureException::class.java))
    }

    @UiThreadTest
    @Test
    fun canBindToLifeCycleAndRecordVideo(): Unit = runBlocking {
        val recorder = Recorder.Builder().build()
        val videoCapture = VideoCapture.withOutput(recorder)
        val preview = Preview.Builder().build()
        // Uses Builder to create the ExtensionSessionConfig
        val sessionConfig =
            ExtensionSessionConfig.Builder(extensionMode, extensionsManager)
                .addUseCase(preview)
                .addUseCase(videoCapture)
                .build()
        val previewReady = CountDownLatch(1)

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    previewReady.countDown()
                }
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector, sessionConfig)
        }

        // Wait for preview to be ready
        assertThat(previewReady.await(5000, TimeUnit.MILLISECONDS)).isTrue()

        val file = temporaryFolder.newFile("video.mp4")
        recordVideo(recorder, file)

        assertThat(file.exists() && file.length() > 0).isTrue()
    }

    @Test
    fun cameraFilterIsAvailableAndProviderInjected_afterCreation() {
        val imageCapture = ImageCapture.Builder().build()
        // Uses constructor to create the ExtensionSessionConfig
        val sessionConfig = ExtensionSessionConfig(extensionMode, extensionsManager, imageCapture)
        assertThat(sessionConfig.cameraFilter).isNotNull()

        val id = ExtensionsInfo.getExtendedCameraConfigProviderId(extensionMode)
        val provider = ExtendedCameraConfigProviderStore.getConfigProvider(Identifier.create(id))
        assertThat(provider).isNotEqualTo(CameraConfigProvider.EMPTY)
    }

    @Test
    fun canCreateWithEmptyUseCaseList() {
        // Uses Builder to create the ExtensionSessionConfig
        val sessionConfig = ExtensionSessionConfig.Builder(extensionMode, extensionsManager).build()
        assertThat(sessionConfig).isNotNull()
        assertThat(sessionConfig.useCases).isEmpty()
    }

    @Test
    fun isSessionConfigSupported_returnsTrue_forSupportedExtension() = runBlocking {
        val cameraInfo = cameraProvider.getCameraInfo(baseCameraSelector)
        val sessionConfig = ExtensionSessionConfig.Builder(extensionMode, extensionsManager).build()

        assertThat(cameraInfo.isSessionConfigSupported(sessionConfig)).isTrue()
    }

    @Test
    fun isSessionConfigSupported_returnsFalse_forUnsupportedExtension() = runBlocking {
        val allModes =
            setOf(
                ExtensionMode.BOKEH,
                ExtensionMode.HDR,
                ExtensionMode.NIGHT,
                ExtensionMode.FACE_RETOUCH,
                ExtensionMode.AUTO,
            )
        // Finds an unsupported extension type for the test
        val unsupportedMode =
            allModes.firstOrNull { !extensionsManager.isExtensionAvailable(baseCameraSelector, it) }

        assumeTrue("No unsupported extension mode found on this device.", unsupportedMode != null)

        val cameraInfo = cameraProvider.getCameraInfo(baseCameraSelector)
        val sessionConfig = ExtensionSessionConfig(unsupportedMode!!, extensionsManager)

        assertThat(cameraInfo.isSessionConfigSupported(sessionConfig)).isFalse()
    }

    private fun recordVideo(recorder: Recorder, file: File) {
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        recording =
            recorder
                .prepareRecording(context, FileOutputOptions.Builder(file).build())
                .start(CameraXExecutors.directExecutor(), videoRecordEventListener)

        try {
            // Wait for status event to proceed recording for a while.
            assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        } finally {
            recording.stop()
        }

        // Wait for finalize event to saved file.
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Check if any error after recording finalized
        assertWithMessage(TAG + "Finalize with error: ${finalize.error}, ${finalize.cause}.")
            .that(finalize.hasError())
            .isFalse()
    }
}
