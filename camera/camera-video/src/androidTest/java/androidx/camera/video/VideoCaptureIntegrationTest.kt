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
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.NonNull
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCase
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.CameraUseCaseAdapter.CameraException
import androidx.camera.testing.AudioUtil
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.abs

@LargeTest
@RunWith(Parameterized::class)
class VideoCaptureIntegrationTest(
    private var cameraSelector: CameraSelector
) {
    companion object {
        private const val TAG = "VideoCaptureTest"

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

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var contentResolver: ContentResolver
    private lateinit var videoUseCase: VideoCapture
    private lateinit var callback: VideoCapture.OnVideoSavedCallback
    private lateinit var outputFileResultsArgumentCaptor:
        ArgumentCaptor<VideoCapture.OutputFileResults>

    @Before
    fun setUp() {
        // TODO(b/168175357): Fix VideoCaptureTest problems on CuttleFish API 29
        assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        // TODO(b/168187087): Video: Unable to record Video on Pixel 1 API 26,27 when only
        // VideoCapture is bound
        // On API 28 the muxer started lately just after receive the stop command, that will cause a
        // failure.
        assumeFalse(
            "Pixel running API 26 has CameraDevice.onError when set repeating request",
            Build.DEVICE == "sailfish" &&
                (
                    Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27 || Build.VERSION
                        .SDK_INT == 28
                    )
        )
        assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        CameraX.initialize(context, Camera2Config.defaultConfig())
        assumeTrue(
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
    fun canRecordingThreeVideosToFilesInARow() {
        // Arrange.
        videoUseCase = VideoCapture.Builder().build()

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(setOf(videoUseCase))
            } catch (e: CameraException) {
                e.printStackTrace()
            }
        }

        // Act.
        // Recording 1st video
        val savedFirstFile = File.createTempFile("CameraX00", ".tmp")
        savedFirstFile.deleteOnExit()
        startRecordingWithUriAndVerifyCallback(savedFirstFile)

        // Recording 2nd video
        val savedSecondFile = File.createTempFile("CameraX01", ".tmp")
        savedSecondFile.deleteOnExit()
        startRecordingWithUriAndVerifyCallback(savedSecondFile)

        // Recording 3rd video
        val savedThirdFile = File.createTempFile("CameraX02", ".tmp")
        savedThirdFile.deleteOnExit()
        startRecordingWithUriAndVerifyCallback(savedThirdFile)

        val firstUri = Uri.fromFile(savedFirstFile)
        val secondUri = Uri.fromFile(savedSecondFile)
        val thirdUri = Uri.fromFile(savedThirdFile)

        verifyRecordingResult(firstUri)
        verifyRecordingResult(secondUri)
        verifyRecordingResult(thirdUri)
    }

    @Ignore
    @Test
    fun canRecordingToFileAndStopImmediately() {
        // Arrange.
        videoUseCase = VideoCapture.Builder().build()
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(setOf(videoUseCase))
            } catch (e: CameraException) {
                e.printStackTrace()
            }
        }

        // Start recording
        videoUseCase.startRecording(
            VideoCapture.OutputFileOptions.Builder(savedFile).build(),
            CameraXExecutors.mainThreadExecutor(), callback
        )

        videoUseCase.stopRecording()
        // Assert.
        verify(callback, Mockito.timeout(2000))
            .onVideoSaved(outputFileResultsArgumentCaptor.capture())
        val savedUri = outputFileResultsArgumentCaptor.value.savedUri
        assertThat(savedUri).isNotNull()
        assertThat(Uri.fromFile(savedFile)).isEqualTo(savedUri)
        verifyRecordingResult(savedUri!!)
    }

    @Test
    @Throws(IOException::class)
    fun canRecordingWithAspectRatio4By3() {
        // Arrange.
        videoUseCase = VideoCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(setOf(videoUseCase))
            } catch (e: CameraException) {
                e.printStackTrace()
            }
        }

        // Start recording
        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()

        // Assert.
        verifyOnSavedCallback()

        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.setDataSource(context, Uri.fromFile(savedFile))
        val height =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                .toInt().toFloat()
        val width =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                .toInt().toFloat()
        Logger.i(TAG, "width: $width height:$height")
        // Checks the aspect ration with a tolerance, because some devices have mod16 resolution.
        assertThat(abs((width / height) - 4.0f / 3.0f) < 0.01).isTrue()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun canRecordingWithAspectRatio16By9() {
        // Arrange.
        videoUseCase = VideoCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(setOf(videoUseCase))
            } catch (e: CameraException) {
                e.printStackTrace()
            }
        }

        // Start recording
        startRecordingThreeSecondsWithFile(savedFile)
        videoUseCase.stopRecording()

        // Assert.
        verifyOnSavedCallback()

        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.setDataSource(context, Uri.fromFile(savedFile))
        val height =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                .toInt().toFloat()
        val width =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                .toInt().toFloat()
        Logger.i(TAG, "width: $width height:$height")
        // Checks the aspect ration with a tolerance, because some devices have mod16 resolution.
        assertThat(abs((width / height) - 16.0f / 9.0f) < 0.03).isTrue()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun recordingWithPreview() {
        // Arrange.
        val preview: Preview = Preview.Builder().build()
        // Sets surface provider to preview
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider()
            )
        }

        videoUseCase = VideoCapture.Builder().build()

        assumeTrue(checkUseCasesCombinationSupported(videoUseCase, preview))

        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        addUseCaseToCamera(preview, videoUseCase)

        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()
        // Assert.
        verifyOnSavedCallback()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun recordingWithImageCapture() {
        // Arrange.
        val imageCapture: ImageCapture = ImageCapture.Builder().build()
        videoUseCase = VideoCapture.Builder().build()

        assumeTrue(checkUseCasesCombinationSupported(videoUseCase, imageCapture))

        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        addUseCaseToCamera(imageCapture, videoUseCase)

        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()
        // Assert.
        verifyOnSavedCallback()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun recordingWithImageAnalysis() {
        // Arrange.
        val analysis = ImageAnalysis.Builder().build()

        // Make ImageAnalysis active.
        analysis.setAnalyzer(
            CameraXExecutors.mainThreadExecutor(),
            { obj: ImageProxy -> obj.close() }
        )

        videoUseCase = VideoCapture.Builder().build()

        assumeTrue(checkUseCasesCombinationSupported(videoUseCase, analysis))

        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        addUseCaseToCamera(analysis, videoUseCase)

        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()
        // Assert.
        verifyOnSavedCallback()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun recordingWithPreviewAndImageAnalysis() {
        // Arrange.
        val preview = Preview.Builder().build()
        // Sets surface provider to preview
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider()
            )
        }

        val analysis = ImageAnalysis.Builder().build()
        // Make ImageAnalysis active.
        analysis.setAnalyzer(
            CameraXExecutors.mainThreadExecutor(),
            { obj: ImageProxy -> obj.close() }
        )

        videoUseCase = VideoCapture.Builder().build()

        assumeTrue(checkUseCasesCombinationSupported(videoUseCase, preview, analysis))

        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        addUseCaseToCamera(preview, analysis, videoUseCase)

        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()
        // Assert.
        verifyOnSavedCallback()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun recordingWithPreviewAndImageCapture() {
        // Arrange.
        val preview = Preview.Builder().build()
        // Sets surface provider to preview
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider()
            )
        }

        val imageCapture = ImageCapture.Builder().build()

        videoUseCase = VideoCapture.Builder().build()

        assumeTrue(checkUseCasesCombinationSupported(videoUseCase, preview, imageCapture))

        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        addUseCaseToCamera(preview, imageCapture, videoUseCase)

        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()
        // Assert.
        verifyOnSavedCallback()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun recordingWithImageAnalysisAndImageCapture() {
        // Arrange.
        val imageCapture = ImageCapture.Builder().build()
        val analysis = ImageAnalysis.Builder().build()
        // Make ImageAnalysis active.
        analysis.setAnalyzer(
            CameraXExecutors.mainThreadExecutor(),
            { obj: ImageProxy -> obj.close() }
        )

        videoUseCase = VideoCapture.Builder().build()

        assumeTrue(checkUseCasesCombinationSupported(videoUseCase, analysis, imageCapture))

        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        addUseCaseToCamera(analysis, imageCapture, videoUseCase)

        startRecordingThreeSecondsWithFile(savedFile)

        videoUseCase.stopRecording()
        // Assert.
        verifyOnSavedCallback()
        verifyRecordingResult(Uri.fromFile(savedFile))
    }

    @Test
    @Throws(IOException::class)
    fun unbind_shouldStopRecording() {
        val file = File.createTempFile("CameraX", "tmp")
        file.deleteOnExit()

        // Arrange.
        videoUseCase = VideoCapture.Builder().build()
        addUseCaseToCamera(videoUseCase)

        startRecordingThreeSecondsWithFile(file)

        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.removeUseCases(
                cameraUseCaseAdapter.useCases
            )
        }

        verify(callback, Mockito.timeout(2000))
            .onVideoSaved(outputFileResultsArgumentCaptor.capture())
        verifyRecordingResult(Uri.fromFile(file))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun startRecordingWithUri_whenAPILevelLargerThan26() {
        val useCase = VideoCapture.Builder().build()

        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.addUseCases(Collections.singleton<UseCase>(useCase))
        }

        val callback = Mockito.mock(VideoCapture.OnVideoSavedCallback::class.java)
        useCase.startRecording(
            getNewVideoOutputFileOptions(contentResolver),
            CameraXExecutors.mainThreadExecutor(),
            callback
        )
        Thread.sleep(3000)

        useCase.stopRecording()

        // Assert: Wait for the signal that the image has been saved.
        val outputFileResultsArgumentCaptor =
            ArgumentCaptor.forClass(
                VideoCapture.OutputFileResults::class.java
            )
        verify(
            callback,
            Mockito.timeout(10000)
        ).onVideoSaved(outputFileResultsArgumentCaptor.capture())

        // get file path to remove it
        val saveLocationUri =
            outputFileResultsArgumentCaptor.value.savedUri
        assertThat(saveLocationUri).isNotNull()
        verifyRecordingResult(saveLocationUri!!)

        // Remove temp test file
        contentResolver.delete(saveLocationUri, null, null)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun startRecordingWithFileDescriptor_whenAPILevelLargerThan26() {
        // Arrange.
        videoUseCase = VideoCapture.Builder()
            .build()
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        // It's needed to have a variable here to hold the parcel file descriptor reference which
        // returned from ParcelFileDescriptor.open(), the returned parcel descriptor reference might
        // be garbage collected unexpectedly. That will caused an "invalid file descriptor" issue.
        val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(
            savedFile,
            ParcelFileDescriptor.MODE_READ_WRITE
        )
        val fd: FileDescriptor = pfd.fileDescriptor

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(setOf(videoUseCase))
            } catch (e: CameraException) {
                e.printStackTrace()
            }
        }

        // Start recording
        videoUseCase.startRecording(
            VideoCapture.OutputFileOptions.Builder(fd).build(),
            CameraXExecutors.mainThreadExecutor(), callback
        )

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        videoUseCase.stopRecording()

        // Assert.
        verify(callback, Mockito.timeout(2000))
            .onVideoSaved(outputFileResultsArgumentCaptor.capture())
        verifyRecordingResult(Uri.fromFile(savedFile))
        pfd.close()
    }

    /** Return a VideoOutputFileOption which is used to save a video.  */
    private fun getNewVideoOutputFileOptions(
        resolver: ContentResolver
    ): VideoCapture.OutputFileOptions {
        val videoFileName = "video_" + System.currentTimeMillis()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, videoFileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
        }

        return VideoCapture.OutputFileOptions.Builder(
            resolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()
    }

    private fun startRecordingWithUriAndVerifyCallback(file: File?) {
        // callback need to reset since it is called multiple in same test.
        reset(callback)
        startRecordingThreeSecondsWithFile(file)

        videoUseCase.stopRecording()
        // Assert.
        verify(callback, Mockito.timeout(2000))
            .onVideoSaved(outputFileResultsArgumentCaptor.capture())
    }

    private fun addUseCaseToCamera(@NonNull vararg useCases: UseCase) {
        val caseList: MutableList<UseCase> = ArrayList()
        for (case in useCases) {
            caseList.add(case)
        }

        instrumentation.runOnMainSync {
            try {
                cameraUseCaseAdapter.addUseCases(caseList)
            } catch (e: CameraException) {
                e.printStackTrace()
            }
        }
    }

    private fun checkUseCasesCombinationSupported(@NonNull vararg useCases: UseCase): Boolean {
        val useCaseList: MutableList<UseCase> = ArrayList()
        for (case in useCases) {
            useCaseList.add(case)
        }

        try {
            cameraUseCaseAdapter.checkAttachUseCases(useCaseList)
        } catch (e: CameraException) {
            // This use combination is not supported on this device, abort this test.
            Logger.i(TAG, "This combination is not supported: $useCaseList .")
            return false
        }
        return true
    }

    private fun startRecordingThreeSecondsWithFile(file: File?) {
        // Start recording
        videoUseCase.startRecording(
            VideoCapture.OutputFileOptions.Builder(file!!).build(),
            CameraXExecutors.mainThreadExecutor(), callback
        )

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun verifyOnSavedCallback() {
        verify(callback, Mockito.timeout(2000))
            .onVideoSaved(outputFileResultsArgumentCaptor.capture())

        val savedUri = outputFileResultsArgumentCaptor.value.savedUri
        assertThat(savedUri).isNotNull()
    }

    private fun getSurfaceProvider(): SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(object : SurfaceTextureCallback {
            override fun onSurfaceTextureReady(surfaceTexture: SurfaceTexture, resolution: Size) {
                // No-op
            }

            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                surfaceTexture.release()
            }
        })
    }

    private fun verifyRecordingResult(uri: Uri) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val hasAudio = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val hasVideo = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)

            assertThat(hasAudio).isEqualTo("yes")
            assertThat(hasVideo).isEqualTo("yes")
        }
    }
}