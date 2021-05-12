/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.NonNull
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.SurfaceTextureProvider.createSurfaceTextureProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import java.io.File
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
public class VideoCaptureTest {
    public companion object {
        @ClassRule
        @JvmField
        public val useRecordingResource: TestRule = CameraUtil.checkVideoRecordingResource()
    }

    @get:Rule
    public val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    public val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraSelector: CameraSelector

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter

    private lateinit var contentResolver: ContentResolver

    @Before
    public fun setUp() {
        // TODO(b/168175357): Fix VideoCaptureTest problems on CuttleFish API 29
        assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        assumeTrue(CameraUtil.deviceHasCamera())

        cameraSelector = if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        CameraX.initialize(context, Camera2Config.defaultConfig()).get()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)

        contentResolver = context.contentResolver
    }

    @After
    public fun tearDown() {
        instrumentation.runOnMainSync {
            if (this::cameraUseCaseAdapter.isInitialized) {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }

        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    public fun buildFileOutputOptionsWithFileDescriptor_throwExceptionWhenAPILevelSmallerThan26() {
        val file = File.createTempFile("CameraX", ".tmp").apply {
            deleteOnExit()
        }

        val fileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE).fileDescriptor

        assertThrows<IllegalArgumentException> {
            VideoCapture.OutputFileOptions.Builder(fileDescriptor).build()
        }

        file.delete()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public fun startRecordingWithFileDescriptor_whenAPILevelLargerThan26() {
        val file = File.createTempFile("CameraX", ".tmp").apply {
            deleteOnExit()
        }

        // It's needed to have a variable here to hold the parcel file descriptor reference which
        // returned from ParcelFileDescriptor.open(), the returned parcel descriptor reference might
        // be garbage collected unexpectedly. That will caused an "invalid file descriptor" issue.
        val parcelFileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        val fileDescriptor = parcelFileDescriptor.fileDescriptor

        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.Builder().build()

        assumeTrue(
            "This combination (videoCapture, preview) is not supported.",
            checkUseCasesCombinationSupported(videoCapture, preview)
        )

        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider()
            )
            // b/168187087 if there is only VideoCapture , VideoCapture will failed when setting the
            // repeating request with the surface, the workaround is binding one more useCase
            // Preview.
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture, preview))
        }

        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(fileDescriptor).build()

        val callback = mock(VideoCapture.OnVideoSavedCallback::class.java)

        // Start recording with FileDescriptor
        videoCapture.startRecording(
            outputFileOptions,
            CameraXExecutors.mainThreadExecutor(),
            callback
        )

        // Recording for seconds
        Thread.sleep(3000)

        // Stop recording
        videoCapture.stopRecording()

        verify(callback, timeout(10000)).onVideoSaved(any())
        parcelFileDescriptor.close()
        file.delete()
    }

    @FlakyTest // b/182165222
    @Test
    public fun unbind_shouldStopRecording() {
        val file = File.createTempFile("CameraX", ".tmp").apply {
            deleteOnExit()
        }

        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.Builder().build()

        assumeTrue(
            "This combination (videoCapture, preview) is not supported.",
            checkUseCasesCombinationSupported(videoCapture, preview)
        )
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider()
            )
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture, preview))
        }

        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(file).build()

        val callback = mock(VideoCapture.OnVideoSavedCallback::class.java)

        videoCapture.startRecording(
            outputFileOptions,
            CameraXExecutors.mainThreadExecutor(),
            callback
        )

        // Recording for seconds
        Thread.sleep(3000)

        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.removeUseCases(listOf(videoCapture, preview))
        }

        verify(callback, timeout(10000)).onVideoSaved(any())
        file.delete()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public fun startRecordingWithUri_whenAPILevelLargerThan26() {
        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.Builder().build()

        assumeTrue(
            "This combination (videoCapture, preview) is not supported.",
            checkUseCasesCombinationSupported(videoCapture, preview)
        )
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider()
            )
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture, preview))
        }

        val callback = mock(VideoCapture.OnVideoSavedCallback::class.java)
        videoCapture.startRecording(
            getNewVideoOutputFileOptions(contentResolver),
            CameraXExecutors.mainThreadExecutor(),
            callback
        )
        Thread.sleep(3000)

        videoCapture.stopRecording()

        // Assert: Wait for the signal that the image has been saved.
        val outputFileResultsArgumentCaptor =
            ArgumentCaptor.forClass(
                VideoCapture.OutputFileResults::class.java
            )
        verify(callback, timeout(10000)).onVideoSaved(outputFileResultsArgumentCaptor.capture())

        // get file path to remove it
        val saveLocationUri =
            outputFileResultsArgumentCaptor.value.savedUri
        assertThat(saveLocationUri).isNotNull()

        // Remove temp test file
        contentResolver.delete(saveLocationUri!!, null, null)
    }

    @Test
    public fun videoCapture_saveResultToFile() {
        val file = File.createTempFile("CameraX", ".tmp").apply {
            deleteOnExit()
        }

        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.Builder().build()

        assumeTrue(
            "This combination (videoCapture, preview) is not supported.",
            checkUseCasesCombinationSupported(videoCapture, preview)
        )
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider()
            )
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture, preview))
        }

        val callback = mock(VideoCapture.OnVideoSavedCallback::class.java)
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(file).build(),
            CameraXExecutors.mainThreadExecutor(),
            callback
        )

        Thread.sleep(3000)

        videoCapture.stopRecording()

        // Wait for the signal that the video has been saved.
        verify(callback, timeout(10000)).onVideoSaved(any())
        file.delete()
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

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return createSurfaceTextureProvider(object : SurfaceTextureCallback {
            override fun onSurfaceTextureReady(surfaceTexture: SurfaceTexture, resolution: Size) {
                // No-op
            }

            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                surfaceTexture.release()
            }
        })
    }

    private fun checkUseCasesCombinationSupported(@NonNull vararg useCases: UseCase): Boolean {
        val useCaseList = useCases.asList()

        try {
            cameraUseCaseAdapter.checkAttachUseCases(useCaseList)
        } catch (e: CameraUseCaseAdapter.CameraException) {
            // This use combination is not supported. on this device, abort this test.
            return false
        }
        return true
    }
}
