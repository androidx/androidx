/*
 * Copyright 2022 The Android Open Source Project
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
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.AudioUtil
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@LargeTest
@RunWith(Parameterized::class)
class AudioVideoSyncTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule val labTest: LabTestRule = LabTestRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Suppress("UNCHECKED_CAST")
    private val videoRecordEventListener =
        Mockito.mock(Consumer::class.java) as Consumer<VideoRecordEvent>

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var recorder: Recorder
    private lateinit var preview: Preview
    private lateinit var surfaceTexturePreview: Preview

    @Before
    fun setUp() {
        // Skip for b/264902324
        Assume.assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        // Skip for b/168175357, b/233661493
        Assume.assumeFalse(
            "Skip tests for Cuttlefish MediaCodec issues",
            Build.MODEL.contains("Cuttlefish") &&
                (Build.VERSION.SDK_INT == 29 || Build.VERSION.SDK_INT == 33),
        )
        Assume.assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        CameraXUtil.initialize(context, cameraConfig).get()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)

        recorder = Recorder.Builder().build()

        // Using Preview so that the surface provider could be set to control when to issue the
        // surface request.
        preview = Preview.Builder().build()

        // Add another Preview to provide an additional surface for b/168187087.
        surfaceTexturePreview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            surfaceTexturePreview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(
                    object : SurfaceTextureProvider.SurfaceTextureCallback {
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
            )
        }

        Assume.assumeTrue(
            "This combination (preview, surfaceTexturePreview) is not supported.",
            cameraUseCaseAdapter.isUseCasesCombinationSupported(preview, surfaceTexturePreview),
        )

        cameraUseCaseAdapter =
            CameraUtil.createCameraAndAttachUseCase(
                context,
                cameraSelector,
                // Must put surfaceTexturePreview before preview while addUseCases, otherwise
                // an issue on Samsung device will occur. See b/196755459.
                surfaceTexturePreview,
                preview,
            )
        recorder.onSourceStateChanged(VideoOutput.SourceState.ACTIVE_NON_STREAMING)
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
            recorder.onSourceStateChanged(VideoOutput.SourceState.INACTIVE)
        }

        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun canRecord_withAvSyncInStart() {
        val diffThresholdUs = 50000L // 50,000 is about 0.05 second

        Mockito.clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest(recorder)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val recording =
            recorder
                .prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withAudioEnabled()
                .start(CameraXExecutors.directExecutor(), videoRecordEventListener)

        val inOrder = Mockito.inOrder(videoRecordEventListener)
        inOrder
            .verify(videoRecordEventListener, Mockito.timeout(5000L))
            .accept(ArgumentMatchers.any(VideoRecordEvent.Start::class.java))
        inOrder
            .verify(videoRecordEventListener, Mockito.timeout(15000L).atLeast(5))
            .accept(ArgumentMatchers.any(VideoRecordEvent.Status::class.java))

        // check if the time difference between the first video and audio data is within a threshold
        val firstAudioTime = recorder.mFirstRecordingAudioDataTimeUs
        val firstVideoTime = recorder.mFirstRecordingVideoDataTimeUs
        val timeDiff = abs(firstAudioTime - firstVideoTime)
        assertThat(timeDiff).isLessThan(diffThresholdUs)

        recording.stop()
        inOrder
            .verify(videoRecordEventListener, Mockito.timeout(5000L))
            .accept(ArgumentMatchers.any(VideoRecordEvent.Finalize::class.java))
        file.delete()
    }

    private fun invokeSurfaceRequest(recorder: Recorder) {
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request: SurfaceRequest ->
                recorder.onSurfaceRequested(request)
            }
            recorder.onSourceStateChanged(VideoOutput.SourceState.ACTIVE_STREAMING)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }
}
