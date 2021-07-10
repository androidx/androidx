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

package androidx.camera.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
class VideoRecordingTest(
    private var cameraSelector: CameraSelector
) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    companion object {
        private const val VIDEO_TIMEOUT = 10_000L
        private const val TAG = "VideoRecordingTest"
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(CameraSelector.DEFAULT_BACK_CAMERA),
                arrayOf(CameraSelector.DEFAULT_FRONT_CAMERA),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var preview: Preview
    private lateinit var cameraInfo: CameraInfo

    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch

    private lateinit var finalize: VideoRecordEvent.Finalize

    private val videoRecordEventListener = Consumer<VideoRecordEvent> {
        when (it.eventType) {
            VideoRecordEvent.EVENT_TYPE_START -> {
                // Recording start.
                Log.d(TAG, "Recording start")
            }
            VideoRecordEvent.EVENT_TYPE_FINALIZE -> {
                // Recording stop.
                Log.d(TAG, "Recording finalize")
                finalize = it as VideoRecordEvent.Finalize
                latchForVideoSaved.countDown()
            }
            VideoRecordEvent.EVENT_TYPE_STATUS -> {
                // Make sure the recording proceed for a while.
                latchForVideoRecording.countDown()
            }
            VideoRecordEvent.EVENT_TYPE_PAUSE, VideoRecordEvent.EVENT_TYPE_RESUME -> {
                // no op for this test, skip these event now.
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        // Skip for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        CameraX.initialize(context, Camera2Config.defaultConfig()).get()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraInfo = cameraUseCaseAdapter.cameraInfo

        // Add extra Preview to provide an additional surface for b/168187087.
        preview = Preview.Builder().build()
        // Sets surface provider to preview
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider()
            )
        }
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.apply {
                    removeUseCases(useCases)
                }
            }
        }
        CameraX.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun getMetadataRotation_when_setTargetRotation() {
        // Arrange.
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        // Just set one Surface.ROTATION_90 to verify the function work or not.
        val targetRotation = Surface.ROTATION_90
        videoCapture.targetRotation = targetRotation

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.addUseCases(listOf(preview, videoCapture))
        }

        // Act.
        completeVideoRecording(videoCapture, file)

        // Verify.
        verifyMetadataRotation(targetRotation, file)
        file.delete()
    }

    // TODO: Add other metadata info check, e.g. location, after Recorder add more metadata.

    @Test
    fun getCorrectResolution_when_setSupportedQuality() {
        Assume.assumeTrue(QualitySelector.getSupportedQualities(cameraInfo).isNotEmpty())

        val qualityList = QualitySelector.getSupportedQualities(cameraInfo)
        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.addUseCases(listOf(preview))
        }

        Log.d(TAG, "CameraSelector: ${cameraSelector.lensFacing}, QualityList: $qualityList ")
        qualityList.forEach loop@{ quality ->
            val targetResolution = QualitySelector.getResolution(cameraInfo, quality)
            if (targetResolution == null) {
                // If targetResolution is null, try next one
                Log.e(TAG, "Unable to get resolution for the quality: $quality")
                return@loop
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.of(quality)).build()

            val videoCapture = VideoCapture.withOutput(recorder)
            val file = File.createTempFile("video_$targetResolution", ".tmp")
                .apply { deleteOnExit() }

            latchForVideoSaved = CountDownLatch(1)
            latchForVideoRecording = CountDownLatch(5)

            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
            }

            // Act.
            completeVideoRecording(videoCapture, file)

            // Verify.
            verifyVideoResolution(targetResolution, file)

            // Cleanup.
            file.delete()
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.apply {
                    removeUseCases(listOf(videoCapture))
                }
            }
        }
    }

    private fun completeVideoRecording(videoCapture: VideoCapture<Recorder>, file: File) {
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val activeRecording = videoCapture.output
            .prepareRecording(outputOptions)
            .withEventListener(
                CameraXExecutors.directExecutor(),
                videoRecordEventListener
            )
            .start()

        // Wait for status event to proceed recording for a while.
        assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()

        activeRecording.stop()
        // Wait for finalize event to saved file.
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()

        // Check if any error after recording finalized
        assertWithMessage(TAG + "Finalize with error: ${finalize.error}, ${finalize.cause}.")
            .that(finalize.hasError()).isFalse()
    }

    private fun verifyMetadataRotation(targetRotation: Int, file: File) {
        // Whether the camera lens and display are facing opposite directions.
        val isOpposite = cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK
        val relativeRotation = CameraOrientationUtil.getRelativeImageRotation(
            CameraOrientationUtil.surfaceRotationToDegrees(targetRotation),
            CameraUtil.getSensorOrientation(cameraSelector.lensFacing!!)!!,
            isOpposite
        )
        val videoRotation = getRotationInMetadata(Uri.fromFile(file))

        // Checks the rotation from video file's metadata is matched with the relative rotation.
        assertWithMessage(
            TAG + ", $targetRotation rotation test failure:" +
                ", videoRotation: $videoRotation" +
                ", relativeRotation: $relativeRotation"
        ).that(videoRotation).isEqualTo(relativeRotation)
    }

    private fun verifyVideoResolution(targetResolution: Size, file: File) {
        val mediaRetriever = MediaMetadataRetriever()
        lateinit var resolution: Size
        mediaRetriever.apply {
            setDataSource(context, Uri.fromFile(file))
            val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                .toInt()
            val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                .toInt()
            resolution = Size(width, height)
        }

        // Compare with the resolution of video and the targetResolution in QualitySelector
        assertWithMessage(
            TAG + ", verifyVideoResolution failure:" +
                ", videoResolution: $resolution" +
                ", targetResolution: $targetResolution"
        ).that(resolution).isEqualTo(targetResolution)
    }

    private fun getRotationInMetadata(uri: Uri): Int {
        val mediaRetriever = MediaMetadataRetriever()
        return mediaRetriever.let {
            it.setDataSource(context, uri)
            it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()!!
        }
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // No-op
                }
                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                }
            }
        )
    }
}