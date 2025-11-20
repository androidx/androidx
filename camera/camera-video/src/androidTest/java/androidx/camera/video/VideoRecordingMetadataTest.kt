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

package androidx.camera.video

import android.content.Context
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraTaskTrackingExecutor
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.getColorStandard
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.testing.impl.video.RecordingSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class VideoRecordingMetadataTest(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig,
    private val forceEnableStreamSharing: Boolean,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName.contains(CameraPipeConfig::class.simpleName!!))

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    // Chain rule to not run WakelockEmptyActivityRule when the test is ignored.
    @get:Rule
    val skipAndWakelockRule: TestRule =
        RuleChain.outerRule(IgnoreVideoRecordingProblematicDeviceRule())
            .around(WakelockEmptyActivityRule())

    companion object {
        private const val TAG = "VideoRecordingMetadataTest"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            // TODO: consider reducing the number of tests (e.g. testing only the back camera)
            return listOf(
                arrayOf(
                    "back+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig(),
                    /*forceEnableStreamSharing=*/ false,
                ),
                arrayOf(
                    "front+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig(),
                    /*forceEnableStreamSharing=*/ false,
                ),
                arrayOf(
                    "external+" + Camera2Config::class.simpleName,
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
                        .build(),
                    Camera2Config.defaultConfig(),
                    /*forceEnableStreamSharing=*/ false,
                ),
                arrayOf(
                    "back+" + Camera2Config::class.simpleName + "+streamSharing",
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig(),
                    /*forceEnableStreamSharing=*/ true,
                ),
                arrayOf(
                    "front+" + Camera2Config::class.simpleName + "+streamSharing",
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig(),
                    /*forceEnableStreamSharing=*/ true,
                ),
                arrayOf(
                    "external+" + Camera2Config::class.simpleName + "+streamSharing",
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
                        .build(),
                    Camera2Config.defaultConfig(),
                    /*forceEnableStreamSharing=*/ true,
                ),
                arrayOf(
                    "back+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                    /*forceEnableStreamSharing=*/ false,
                ),
                arrayOf(
                    "front+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                    /*forceEnableStreamSharing=*/ false,
                ),
                arrayOf(
                    "external+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
                        .build(),
                    CameraPipeConfig.defaultConfig(),
                    /*forceEnableStreamSharing=*/ false,
                ),
                arrayOf(
                    "back+" + CameraPipeConfig::class.simpleName + "+streamSharing",
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                    /*forceEnableStreamSharing=*/ true,
                ),
                arrayOf(
                    "front+" + CameraPipeConfig::class.simpleName + "+streamSharing",
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                    /*forceEnableStreamSharing=*/ true,
                ),
                arrayOf(
                    "external+" + CameraPipeConfig::class.simpleName + "+streamSharing",
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
                        .build(),
                    CameraPipeConfig.defaultConfig(),
                    /*forceEnableStreamSharing=*/ true,
                ),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProviderWrapper
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var preview: Preview
    private lateinit var videoCapabilities: VideoCapabilities
    private lateinit var recordingSession: RecordingSession
    private lateinit var defaultRecorder: Recorder

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        val cameraExecutor = CameraTaskTrackingExecutor()
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(cameraConfig).setCameraExecutor(cameraExecutor).build()
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider =
            ProcessCameraProviderWrapper(
                ProcessCameraProvider.getInstance(context).get(),
                forceEnableStreamSharing,
            )
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        // Add extra Preview to provide an additional surface for b/168187087.
        preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            // Sets surface provider to preview
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()

            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            val cameraInfo = camera.cameraInfo
            videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        }

        defaultRecorder = Recorder.Builder().build()
        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
                    recorder = defaultRecorder,
                    outputOptionsProvider = {
                        FileOutputOptions.Builder(temporaryFolder.newFile()).build()
                    },
                    withAudio = false,
                )
            )
    }

    @After
    fun tearDown() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(timeoutMs = 5000)
        }
        if (this::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    @LabTestRule.LabTestOnly
    @Test
    fun colorStandardIsNotAbnormal() {
        val supportedQualities = videoCapabilities.getSupportedQualities(DynamicRange.SDR)
        assumeTrue(supportedQualities.isNotEmpty())

        for (quality in supportedQualities) {
            // Arrange.
            val recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
            val videoCapture = VideoCapture.Builder(recorder).build()
            checkAndBindUseCases(preview, videoCapture)

            // Act.
            val result =
                recordingSession.createRecording(recorder = videoCapture.output).recordAndVerify()

            // Verify. SDR video is not expected to have BT2020 color standard.
            verifyVideoColorStandard(
                unexpectedColorStandard = MediaFormat.COLOR_STANDARD_BT2020,
                file = result.file,
            )

            instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    @LabTestRule.LabTestOnly
    @Test
    fun colorStandardIsNotAbnormal_whenSurfaceProcessingIsEnabled() {
        val supportedQualities = videoCapabilities.getSupportedQualities(DynamicRange.SDR)
        assumeTrue(supportedQualities.isNotEmpty())

        for (quality in supportedQualities) {
            // Arrange.
            val recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
            val videoCapture =
                VideoCapture.Builder(recorder).setSurfaceProcessingForceEnabled().build()
            checkAndBindUseCases(preview, videoCapture)

            // Act.
            val result =
                recordingSession.createRecording(recorder = videoCapture.output).recordAndVerify()

            // Verify. SDR video is not expected to have BT2020 color standard.
            verifyVideoColorStandard(
                unexpectedColorStandard = MediaFormat.COLOR_STANDARD_BT2020,
                file = result.file,
            )

            instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        }
    }

    private fun isUseCasesCombinationSupported(vararg useCases: UseCase) =
        camera.isUseCasesCombinationSupported(*useCases)

    private fun checkAndBindUseCases(vararg useCases: UseCase) {
        assumeTrue(isUseCasesCombinationSupported(*useCases))

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases)
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    private fun verifyVideoColorStandard(unexpectedColorStandard: Int, file: File) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(file))
            val colorStandard = it.getColorStandard()

            assertWithMessage(
                    TAG +
                        ", verifyVideoColorStandard failure:" +
                        ", videoColorStandard: $colorStandard" +
                        ", unexpectedColorStandard: $unexpectedColorStandard"
                )
                .that(colorStandard)
                .isNotEqualTo(unexpectedColorStandard)
        }
    }
}
