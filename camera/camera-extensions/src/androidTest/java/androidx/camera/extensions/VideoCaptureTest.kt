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

package androidx.camera.extensions

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.impl.ExtensionsTestlibControl
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.extensions.util.ExtensionsTestUtil.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
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
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class VideoCaptureTest(
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
    private val implType: ExtensionsTestlibControl.ImplementationType,
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraXConfig)
        )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionsCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var latchForVideoStarted: CountDownLatch
    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch
    private lateinit var finalize: VideoRecordEvent.Finalize
    private lateinit var recording: Recording
    private val videoRecordEventListener =
        Consumer<VideoRecordEvent> {
            when (it) {
                is VideoRecordEvent.Start -> {
                    Log.d(TAG, "Recording start")
                    latchForVideoStarted.countDown()
                }
                is VideoRecordEvent.Finalize -> {
                    Log.d(TAG, "Recording finalize")
                    finalize = it
                    latchForVideoSaved.countDown()
                }
                is VideoRecordEvent.Status -> {
                    Log.d(TAG, "Recording Status")
                    latchForVideoRecording.countDown()
                }
                is VideoRecordEvent.Pause,
                is VideoRecordEvent.Resume -> {
                    // Do nothing.
                }
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
        skipVideoRecordingTestIfNotSupportedByEmulator()

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        ExtensionsTestlibControl.getInstance().setImplementationType(implType)
        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionsCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)

        fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
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

    @UiThreadTest
    @Test
    @Ignore("b/331617278")
    fun canBindToLifeCycleAndRecordVideo() {
        // Arrange.
        val file = createTempFile()
        val recorder = Recorder.Builder().build()
        val videoCapture = VideoCapture.withOutput(recorder)

        // Act.
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                videoCapture
            )
        }
        videoCapture.recordTo(file)

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @UiThreadTest
    @Test
    @Ignore("b/331617278")
    fun canBindToLifeCycleAndRecordVideoWithPreviewAndImageCaptureBound() {
        // Arrange.
        val file = createTempFile()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val recorder = Recorder.Builder().build()
        val videoCapture = VideoCapture.withOutput(recorder)

        // Act.
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
            preview.setSurfaceProvider(createPreviewSurfaceProvider())
        }
        videoCapture.recordTo(file)

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    private fun createPreviewSurfaceProvider(): SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // No-op.
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    // No-op.
                }
            }
        )
    }

    private fun createTempFile(): File {
        return File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
    }

    private fun checkFileHasAudioAndVideo(uri: Uri) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val hasVideo = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            assertThat(hasVideo).isEqualTo("yes")
            val hasAudio = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            assertThat(hasAudio).isEqualTo("yes")
        }
    }

    private fun VideoCapture<Recorder>.recordTo(file: File) {
        latchForVideoStarted = CountDownLatch(1)
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        recording =
            output
                .prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withAudioEnabled()
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

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L
        private const val TAG = "VideoCaptureTest"
        val context: Context = ApplicationProvider.getApplicationContext()

        @JvmStatic
        @Parameterized.Parameters(
            name = "cameraXConfig = {0}, implType = {2}, mode = {3}, facing = {4}"
        )
        fun data(): Collection<Array<Any>> {
            return ExtensionsTestUtil.getAllImplExtensionsLensFacingCombinations(context, true)
        }
    }
}
