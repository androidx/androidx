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

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.AudioUtil
import androidx.camera.testing.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@LargeTest
@RunWith(Parameterized::class)
class VideoCaptureRotationTest(
    private var cameraSelector: CameraSelector,
    private var targetRotation: Int
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            val result: MutableList<Array<Any>> = ArrayList()
            result.add(arrayOf(CameraSelector.DEFAULT_BACK_CAMERA, Surface.ROTATION_90))
            result.add(arrayOf(CameraSelector.DEFAULT_BACK_CAMERA, Surface.ROTATION_180))
            result.add(arrayOf(CameraSelector.DEFAULT_FRONT_CAMERA, Surface.ROTATION_90))
            result.add(arrayOf(CameraSelector.DEFAULT_FRONT_CAMERA, Surface.ROTATION_180))
            return result
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var videoUseCase: VideoCapture
    private lateinit var callback: VideoCapture.OnVideoSavedCallback
    private lateinit var outputFileResultsArgumentCaptor:
        ArgumentCaptor<VideoCapture.OutputFileResults>

    @Before
    fun setUp() {
        // TODO(b/168175357): Fix VideoCaptureTest problems on CuttleFish API 29
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        // TODO(b/168187087): Video: Unable to record Video on Pixel 1 API 26,27 when only
        // VideoCapture is bound
        Assume.assumeFalse(
            "Pixel running API 26 has CameraDevice.onError when set repeating request",
            Build.DEVICE == "sailfish" &&
                (Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27)
        )
        Assume.assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        context = ApplicationProvider.getApplicationContext()
        CameraX.initialize(context, Camera2Config.defaultConfig())
        Assume.assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                cameraSelector.lensFacing!!
            )
        )
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        contentResolver = context.contentResolver

        callback = Mockito.mock(
            VideoCapture.OnVideoSavedCallback::class.java
        )
        outputFileResultsArgumentCaptor = ArgumentCaptor.forClass(
            VideoCapture.OutputFileResults::class.java
        )
    }

    @After
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun tearDown() {
        instrumentation.runOnMainSync {
            if (this::cameraUseCaseAdapter.isInitialized) {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }
        CameraX.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    @Throws(IOException::class)
    fun metadataGetCorrectRotation_afterVideoCaptureRecording() {
        // Sets the device rotation.
        videoUseCase = VideoCapture.Builder()
            .setTargetRotation(targetRotation)
            .build()
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(setOf(videoUseCase))
            } catch (e: CameraUseCaseAdapter.CameraException) {
                e.printStackTrace()
            }
        }

        // Start recording
        videoUseCase.startRecording(
            VideoCapture.OutputFileOptions.Builder(savedFile).build(),
            CameraXExecutors.mainThreadExecutor(), callback
        )
        // The way to control recording length might not be applicable in the new VideoCapture.
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        // Assert.
        // Checks the target rotation is correct when the use case is bound.
        videoUseCase.stopRecording()

        Mockito.verify(callback, Mockito.timeout(2000)).onVideoSaved(any())

        val targetRotationDegree = CameraOrientationUtil.surfaceRotationToDegrees(targetRotation)
        val videoRotation: Int
        val mediaRetriever = MediaMetadataRetriever()

        mediaRetriever.apply {
            setDataSource(context, Uri.fromFile(savedFile))
            videoRotation = extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )?.toInt()!!
        }

        val sensorRotation = CameraUtil.getSensorOrientation(cameraSelector.lensFacing!!)
        // Whether the camera lens and display are facing opposite directions.
        val isOpposite = cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK
        val relativeRotation = CameraOrientationUtil.getRelativeImageRotation(
            targetRotationDegree,
            sensorRotation!!,
            isOpposite
        )

        // Checks the rotation from video file's metadata is matched with the relative rotation.
        assertThat(videoRotation).isEqualTo(relativeRotation)
    }
}