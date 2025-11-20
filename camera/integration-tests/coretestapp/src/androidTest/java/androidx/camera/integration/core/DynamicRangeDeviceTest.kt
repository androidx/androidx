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

package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.hardware.DataSpace
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule.Companion.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.getColorStandard
import androidx.camera.testing.impl.getColorTransfer
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
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
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class DynamicRangeDeviceTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
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

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule val labTestRule = LabTestRule()

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        // Enumerate possible SDR transfer functions. This may need to be updated if more transfer
        // functions are added to the DataSpace class. This set is notably missing the HLG and PQ
        // transfer functions, though HLG could technically be used with 8-bit for SDR. We also
        // exclude LINEAR as most devices should at least apply gamma for SDR.
        private val POSSIBLE_COLOR_TRANSFERS_SDR: Set<Int> =
            setOf<Int>(
                DataSpace.TRANSFER_UNSPECIFIED, // Some devices may use this as a default for SDR
                DataSpace.TRANSFER_GAMMA2_2,
                DataSpace.TRANSFER_GAMMA2_6,
                DataSpace.TRANSFER_GAMMA2_8,
                DataSpace.TRANSFER_SMPTE_170M,
                DataSpace.TRANSFER_SRGB,
            )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var cameraInfo: CameraInfo
    private lateinit var videoCapabilities: VideoCapabilities
    private lateinit var recordingSession: RecordingSession

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        instrumentation.runOnMainSync {
            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
            videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        }

        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
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

    @LabTestRule.LabTestOnly
    @SdkSuppress(minSdkVersion = 33) // Can only verify data space on API 33+
    @Test
    fun bindPreviewWithDefault_sdrDynamicRangeAppliedToCamera() {
        bindPreviewAndVerifyDynamicRangeAppliedToCamera(
            dynamicRange = null, // Should default to SDR
            possibleColorStandards = null, // Do not check ColorSpace for SDR; could be many.
            possibleColorTransfers = POSSIBLE_COLOR_TRANSFERS_SDR,
        )
    }

    @LabTestRule.LabTestOnly
    @SdkSuppress(minSdkVersion = 33) // HLG dynamic range only supported since API 33
    @Test
    fun bindPreviewWithHlg_hlgDynamicRangeAppliedToCamera() {
        bindPreviewAndVerifyDynamicRangeAppliedToCamera(
            dynamicRange = HLG_10_BIT,
            possibleColorStandards = setOf(DataSpace.STANDARD_BT2020),
            possibleColorTransfers = setOf(DataSpace.TRANSFER_HLG),
        )
    }

    @LabTestRule.LabTestOnly
    @SdkSuppress(minSdkVersion = 30) // Can only verify data space on API 30+
    @Test
    fun recordVideoWithDefault_sdrDynamicRangeAppliedToRecordedFile() {
        recordVideoAndVerifyDynamicRangeAppliedToRecordedFile(
            dynamicRange = null, // Should default to SDR
            possibleColorStandards =
                setOf(
                    MediaFormat.COLOR_STANDARD_BT709,
                    MediaFormat.COLOR_STANDARD_BT601_PAL,
                    MediaFormat.COLOR_STANDARD_BT601_NTSC,
                ),
            possibleColorTransfers = setOf(MediaFormat.COLOR_TRANSFER_SDR_VIDEO),
        )
    }

    @LabTestRule.LabTestOnly
    @SdkSuppress(minSdkVersion = 33) // HLG dynamic range only supported since API 33
    @Test
    fun recordVideoWithHlg_hlgDynamicRangeAppliedToRecordedFile() {
        recordVideoAndVerifyDynamicRangeAppliedToRecordedFile(
            dynamicRange = HLG_10_BIT,
            possibleColorStandards = setOf(MediaFormat.COLOR_STANDARD_BT2020),
            possibleColorTransfers = setOf(MediaFormat.COLOR_TRANSFER_HLG),
        )
    }

    @RequiresApi(33) // SurfaceTexture.getDataSpace() was added in API 33
    private fun bindPreviewAndVerifyDynamicRangeAppliedToCamera(
        dynamicRange: DynamicRange?,
        possibleColorStandards: Set<Int>?,
        possibleColorTransfers: Set<Int>?,
    ) {
        if (dynamicRange != null) {
            assumeTrue(
                cameraProvider
                    .getCameraInfo(cameraSelector)
                    .querySupportedDynamicRanges(setOf(dynamicRange))
                    .contains(dynamicRange)
            )
        }

        // 1. Arrange
        val latch = CountDownLatch(1)
        val dataSpace = AtomicInteger(DataSpace.DATASPACE_UNKNOWN)
        val preview =
            Preview.Builder()
                .apply {
                    if (dynamicRange != null) {
                        setDynamicRange(dynamicRange)
                    }
                }
                .build()

        // 2. Act
        // Since there is no direct way to check dynamic range in camera's output config, check the
        // data space in SurfaceTexture instead.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { surfaceRequest ->
                runBlocking {
                    val surfaceTextureHolder =
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
                                surfaceRequest.resolution.width,
                                surfaceRequest.resolution.height,
                            ) { surfaceTexture ->
                                dataSpace.set(surfaceTexture.dataSpace)
                                latch.countDown()
                            }
                            .await()
                    val surface = Surface(surfaceTextureHolder!!.surfaceTexture)
                    surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) { _ ->
                        surfaceTextureHolder.close()
                        surface.release()
                    }
                }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // 3. Assert
        assertWithMessage("Timed out while waiting for frame to be produced.")
            .that(latch.await(2, TimeUnit.SECONDS))
            .isTrue()
        if (possibleColorStandards != null) {
            assertThat(DataSpace.getStandard(dataSpace.get())).isIn(possibleColorStandards)
        }
        if (possibleColorTransfers != null) {
            assertThat(DataSpace.getTransfer(dataSpace.get())).isIn(possibleColorTransfers)
        }
    }

    @RequiresApi(30)
    private fun recordVideoAndVerifyDynamicRangeAppliedToRecordedFile(
        dynamicRange: DynamicRange?,
        possibleColorStandards: Set<Int>?,
        possibleColorTransfers: Set<Int>?,
    ) {
        skipVideoRecordingTestIfNotSupportedByEmulator()

        val supportedQualities = videoCapabilities.getSupportedQualities(dynamicRange ?: SDR)
        assumeTrue(supportedQualities.isNotEmpty())

        // 1. Arrange
        val preview = Preview.Builder().build()
        val recorder = Recorder.Builder().build()
        val videoCapture =
            VideoCapture.Builder(recorder)
                .apply {
                    if (dynamicRange != null) {
                        setDynamicRange(dynamicRange)
                    }
                }
                .build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        // 2. Act
        val result =
            recordingSession.createRecording(recorder = videoCapture.output).recordAndVerify()

        // 3. Assert
        verifyVideoDataSpace(
            file = result.file,
            possibleColorStandards = possibleColorStandards,
            possibleColorTransfers = possibleColorTransfers,
        )
    }

    @RequiresApi(30) // MediaMetadataRetriever data space keys were added in API 30
    private fun verifyVideoDataSpace(
        file: File,
        possibleColorStandards: Set<Int>?,
        possibleColorTransfers: Set<Int>?,
    ) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(file))
            val colorStandard = it.getColorStandard()
            val colorTransfer = it.getColorTransfer()

            if (possibleColorStandards != null) {
                assertThat(colorStandard).isIn(possibleColorStandards)
            }
            if (possibleColorTransfers != null) {
                assertThat(colorTransfer).isIn(possibleColorTransfers)
            }
        }
    }
}
