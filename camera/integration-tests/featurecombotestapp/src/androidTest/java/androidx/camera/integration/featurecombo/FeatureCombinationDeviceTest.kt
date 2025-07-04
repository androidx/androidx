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

package androidx.camera.integration.featurecombo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.DataSpace
import android.hardware.DataSpace.TRANSFER_HLG
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.awaitUntil
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.getImageCaptureCapabilities
import androidx.camera.core.Preview
import androidx.camera.core.Preview.getPreviewCapabilities
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.takePicture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.Camera2CaptureCallbackImpl
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.GLUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.SurfaceTextureProvider.createSurfaceTextureProvider
import androidx.camera.testing.impl.UltraHdrImageVerification.assertJpegUltraHdr
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalSessionConfig::class)
@LargeTest
@RunWith(Parameterized::class)
class FeatureCombinationDeviceTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(
                if (implName == Camera2Config::class.simpleName) {
                    Camera2Config.defaultConfig()
                } else {
                    CameraPipeConfig.defaultConfig()
                }
            )
        )

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    private val sessionCaptureCallback = Camera2CaptureCallbackImpl()

    private val surfaceTextureDeferred = CompletableDeferred<SurfaceTexture>()

    private val preview =
        Preview.Builder()
            .apply { applySessionCaptureCallback() }
            .build()
            .apply {
                runBlocking {
                    withContext(Dispatchers.Main) {
                        surfaceProvider =
                            createSurfaceTextureProvider(
                                object : SurfaceTextureProvider.SurfaceTextureCallback {
                                    override fun onSurfaceTextureReady(
                                        surfaceTexture: SurfaceTexture,
                                        resolution: Size,
                                    ) {
                                        surfaceTextureDeferred.complete(surfaceTexture)
                                    }

                                    override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                                        surfaceTexture.release()
                                    }
                                }
                            )
                    }
                }
            }

    private val imageCapture =
        ImageCapture.Builder().apply { applySessionCaptureCallback() }.build()

    private val videoCapture =
        VideoCapture.Builder(Recorder.Builder().build())
            .apply { applySessionCaptureCallback() }
            .build()

    @Before
    fun setUp() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
        }
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun bindToLifecycle_hlg10_bindResultMatchesQueryResult(): Unit = runBlocking {
        testIfBindAndQueryApiResultsMatch(listOf(preview, videoCapture), setOf(HDR_HLG10))
    }

    @Test
    fun bindToLifecycle_fps60_bindResultMatchesQueryResult(): Unit = runBlocking {
        testIfBindAndQueryApiResultsMatch(listOf(preview, videoCapture), setOf(FPS_60))
    }

    @Test
    fun bindToLifecycle_previewStabilization_bindResultMatchesQueryResult(): Unit = runBlocking {
        testIfBindAndQueryApiResultsMatch(
            listOf(preview, videoCapture),
            setOf(PREVIEW_STABILIZATION),
        )
    }

    @Test
    fun bindToLifecycle_jpegUltraHdr_bindResultMatchesQueryResult(): Unit = runBlocking {
        testIfBindAndQueryApiResultsMatch(listOf(preview, imageCapture), setOf(IMAGE_ULTRA_HDR))
    }

    @Test
    fun bindToLifecycle_bothHdrAndFps60Required_bindResultMatchesQueryResult(): Unit = runBlocking {
        testIfBindAndQueryApiResultsMatch(listOf(preview, videoCapture), setOf(HDR_HLG10, FPS_60))
    }

    @Test
    fun bindToLifecycle_bothHdrAndPrvwStabilizationRequired_bindResultMatchesQueryResult(): Unit =
        runBlocking {
            testIfBindAndQueryApiResultsMatch(
                listOf(preview, videoCapture),
                setOf(HDR_HLG10, PREVIEW_STABILIZATION),
            )
        }

    @Test
    fun bindToLifecycle_moreThanTwoFeaturesRequired_bindResultMatchesQueryResult(): Unit =
        runBlocking {
            testIfBindAndQueryApiResultsMatch(
                listOf(preview, videoCapture),
                setOf(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION),
            )
        }

    @Test
    fun bindToLifecycle_allPreferredFeatures_canBindSuccessfully(): Unit = runBlocking {
        val useCases = listOf(preview, videoCapture, imageCapture)
        val orderedFeatures = listOf(HDR_HLG10, FPS_60, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)
        val selectedFeatures = mutableSetOf<GroupableFeature>()

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    cameraSelector,
                    SessionConfig(useCases = useCases, preferredFeatureGroup = orderedFeatures)
                        .apply {
                            setFeatureSelectionListener { features ->
                                selectedFeatures.addAll(features)
                            }
                        },
                )
            }

        selectedFeatures.verifyFeatures(useCases, camera.cameraInfo)
    }

    // TODO: b/419766630 - Add tests where FCQ provides extra support compared to non-FCQ bind flow,
    //  e.g. UHD recording + Preview Stabilization (which is probably not supported right now due to
    //  Preview Stabilization guaranteed table not supporting UHD PRIV). But we first need to wait
    //  for adding FCQ-queryable config combinations supported in Baklava, as Android 15 doesn't
    //  support UHD PRIV for FCQ.

    private suspend fun testIfBindAndQueryApiResultsMatch(
        useCases: List<UseCase>,
        features: Set<GroupableFeature>,
    ) {
        val sessionConfig = SessionConfig(useCases = useCases, requiredFeatureGroup = features)

        val isSupported =
            cameraProvider.getCameraInfo(cameraSelector).isFeatureGroupSupported(sessionConfig)

        val camera = bindAndVerify(sessionConfig, isSupported)

        if (isSupported) {
            features.verifyFeatures(useCases, requireNotNull(camera?.cameraInfo))
        }
    }

    private suspend fun bindAndVerify(
        sessionConfig: SessionConfig,
        isExpectedToBeSupported: Boolean,
    ): Camera? {
        var caughtException: Exception? = null
        val camera =
            try {
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        fakeLifecycleOwner,
                        cameraSelector,
                        sessionConfig,
                    )
                }
            } catch (e: IllegalArgumentException) {
                caughtException = e
                null
            }

        // If binding is expected to be supported, there should be no exception
        assertThat(caughtException == null).isEqualTo(isExpectedToBeSupported)

        return camera
    }

    @SuppressLint("NewApi")
    private suspend fun Set<GroupableFeature>.verifyFeatures(
        useCases: List<UseCase>,
        cameraInfo: CameraInfo,
    ) {
        forEach {
            when (it) {
                HDR_HLG10 -> {
                    // Reaching this stage before API 33 means query API didn't work correctly
                    require(Build.VERSION.SDK_INT >= 33)
                    verifyHlg10Hdr(useCases, cameraInfo)
                }
                FPS_60 -> verify60Fps(cameraInfo)
                PREVIEW_STABILIZATION ->
                    verifyPreviewStabilization(cameraInfo as CameraInfoInternal)
                IMAGE_ULTRA_HDR -> {
                    // Reaching this stage before API 34 means query API didn't work correctly
                    require(Build.VERSION.SDK_INT >= 34)
                    verifyUltraHdr(useCases, cameraInfo)
                }
            }
        }
    }

    @RequiresApi(33)
    private suspend fun verifyHlg10Hdr(useCases: List<UseCase>, cameraInfo: CameraInfo) {
        assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.HLG_10_BIT)))
            .contains(DynamicRange.HLG_10_BIT)

        useCases.forEach {
            when (it) {
                is Preview -> {
                    assertThat(it.dynamicRange).isEqualTo(DynamicRange.HLG_10_BIT)

                    surfaceTextureDeferred.await().verifyHlg10Hdr()
                }
                is VideoCapture<*> -> {
                    assertThat(it.dynamicRange).isEqualTo(DynamicRange.HLG_10_BIT)

                    // TODO: Check the actual recording
                }
            }
        }
    }

    @RequiresApi(33)
    private fun SurfaceTexture.verifyHlg10Hdr() {
        // Wait for a few frames in order to ensure the surface texture is updated
        val countDownLatch = CountDownLatch(5)
        setOnFrameAvailableListener { countDownLatch.countDown() }
        countDownLatch.await(1, TimeUnit.SECONDS)

        // Ensure latest frame is updated to the texture image
        attachToGLContext(GLUtil.getTexIdFromGLContext())
        updateTexImage()

        val dataspaceTransfer = DataSpace.getTransfer(dataSpace)

        assertThat(dataspaceTransfer).isEqualTo(TRANSFER_HLG)
    }

    private suspend fun verify60Fps(cameraInfo: CameraInfo) {
        assertThat(cameraInfo.supportedFrameRateRanges).contains(Range(60, 60))

        verifyCaptureResult(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(60, 60))
    }

    private suspend fun verifyPreviewStabilization(cameraInfo: CameraInfo) {
        assertThat(getPreviewCapabilities(cameraInfo).isStabilizationSupported).isTrue()

        verifyCaptureResult(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
        )
    }

    @RequiresApi(34)
    private suspend fun verifyUltraHdr(useCases: List<UseCase>, cameraInfo: CameraInfo) {
        assertThat(getImageCaptureCapabilities(cameraInfo).supportedOutputFormats)
            .contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)

        val imageCapture = useCases.filterIsInstance<ImageCapture>().first()

        imageCapture.takePicture().assertJpegUltraHdr()
    }

    private suspend fun <T> verifyCaptureResult(
        captureKey: CaptureRequest.Key<T>,
        expectedValue: T,
    ) {
        var lastSubmittedValue: T? = null
        val result =
            sessionCaptureCallback.verify { captureRequest, _ ->
                captureRequest[captureKey]?.let { lastSubmittedValue = it }
                captureRequest[captureKey] == expectedValue
            }

        val isCompleted = result.awaitUntil(timeoutMillis = 10000)
        assertWithMessage(
                "Test failed while verifying a value of $expectedValue for $captureKey" +
                    ", lastSubmittedValue = $lastSubmittedValue"
            )
            .that(isCompleted)
            .isTrue()
    }

    private fun ExtendableBuilder<*>.applySessionCaptureCallback() {
        Camera2InteropUtil.setCameraCaptureSessionCallback(implName, this, sessionCaptureCallback)
    }

    companion object {
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
}
