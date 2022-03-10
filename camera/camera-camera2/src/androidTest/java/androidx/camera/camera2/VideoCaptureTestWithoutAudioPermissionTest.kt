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

package androidx.camera.camera2

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class VideoCaptureTestWithoutAudioPermissionTest {
    companion object {
        const val TAG: String = "VideoCaptureTestWithoutAudioPermission"
    }
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
            // Don't grant Manifest.permission.RECORD_AUDIO
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraSelector: CameraSelector

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        // TODO(b/168175357): Fix VideoCaptureTest problems on CuttleFish API 29
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        assumeTrue(CameraUtil.deviceHasCamera())

        cameraSelector = if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        CameraXUtil.initialize(
            context,
            Camera2Config.defaultConfig()
        ).get()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)

        contentResolver = context.contentResolver
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            if (this::cameraUseCaseAdapter.isInitialized) {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }

        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS)
    }

    /**
     * This test intends to test recording features without audio permission (RECORD_AUDIO).
     * Currently we cannot guarantee test cases' running sequence, the audio permission might be
     * granted by previous tests.
     * And if we revoke audio permission on the runtime it will cause the test crash.
     * That makes it necessary to check if the audio permission is denied or not before the test.
     * It's conceivable this test will be skipped because it's not the first case to test.
     */
    @Test
    fun videoCapture_saveResultToFileWithoutAudioPermission() {
        val checkPermissionResult =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)

        Logger.i(TAG, "checkSelfPermission RECORD_AUDIO: $checkPermissionResult")

        // This test is only for audio permission does not granted case.
        assumeTrue(checkPermissionResult == PackageManager.PERMISSION_DENIED)

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.Builder().build()

        assumeTrue(
            "This combination (videoCapture, preview) is not supported.",
            cameraUseCaseAdapter.isUseCasesCombinationSupported(videoCapture, preview)
        )
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider()
            )
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture, preview))
        }

        val callback = Mockito.mock(VideoCapture.OnVideoSavedCallback::class.java)
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(file).build(),
            CameraXExecutors.mainThreadExecutor(),
            callback
        )

        Thread.sleep(3000)

        videoCapture.stopRecording()

        // Wait for the signal that the video has been saved.
        Mockito.verify(callback, Mockito.timeout(10000)).onVideoSaved(ArgumentMatchers.any())

        val mediaRetriever = MediaMetadataRetriever()

        mediaRetriever.apply {
            setDataSource(context, Uri.fromFile(file))
            val hasAudio = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val numOfTracks = extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)

            // In most of case and test environment, the RECORD_AUDIO permission is granted.
            // But if there is any audio permission denied cases, the recording should be keeps
            // going and only video recorded.
            assertThat(hasAudio).isNull()
            assertThat(numOfTracks).isEqualTo("1")
        }

        file.delete()
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(object :
                SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size,
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