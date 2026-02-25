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
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.adapter.awaitUntil
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.getImageCaptureCapabilities
import androidx.camera.core.Preview
import androidx.camera.core.Preview.getPreviewCapabilities
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.takePicture
import androidx.camera.integration.featurecombo.AppUseCase.IMAGE_ANALYSIS
import androidx.camera.integration.featurecombo.AppUseCase.IMAGE_CAPTURE
import androidx.camera.integration.featurecombo.AppUseCase.PREVIEW
import androidx.camera.integration.featurecombo.AppUseCase.VIDEO_CAPTURE
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.Camera2CaptureCallbackImpl
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.GLUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.SurfaceTextureProvider.createSurfaceTextureProvider
import androidx.camera.testing.impl.UltraHdrImageVerification.assertJpegUltraHdr
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.camera.video.GroupableFeatures.FHD_RECORDING
import androidx.camera.video.GroupableFeatures.HD_RECORDING
import androidx.camera.video.GroupableFeatures.SD_RECORDING
import androidx.camera.video.GroupableFeatures.UHD_RECORDING
import androidx.camera.video.GroupableFeatures.VIDEO_STABILIZATION
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule

open class FeatureGroupTestBase(
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    protected lateinit var cameraProvider: ProcessCameraProvider
    protected lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    private val sessionCaptureCallback = Camera2CaptureCallbackImpl()

    protected val surfaceTextureDeferred = CompletableDeferred<SurfaceTexture>()

    private fun createPreview(aspectRatio: Int) =
        Preview.Builder()
            .apply {
                applyResolutionSelector(
                    aspectRatio,
                    resolutionSelectorSetter = ::setResolutionSelector,
                )

                applySessionCaptureCallback()
            }
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

    private fun createImageCapture(aspectRatio: Int) =
        ImageCapture.Builder()
            .apply {
                applyResolutionSelector(
                    aspectRatio,
                    resolutionSelectorSetter = ::setResolutionSelector,
                )

                applySessionCaptureCallback()
            }
            .build()

    private fun createImageAnalysis(aspectRatio: Int) =
        ImageAnalysis.Builder()
            .apply {
                applyResolutionSelector(
                    aspectRatio,
                    resolutionSelectorSetter = ::setResolutionSelector,
                )

                applySessionCaptureCallback()
            }
            .build()
            .apply {
                setAnalyzer(Dispatchers.Default.asExecutor()) {
                    // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent
                    // the closing of the CameraDevice from being stuck.
                    it.close()
                }
            }

    private fun createVideoCapture(aspectRatio: Int) =
        VideoCapture.Builder(Recorder.Builder().setAspectRatio(aspectRatio).build())
            .apply { applySessionCaptureCallback() }
            .build()

    private fun applyResolutionSelector(
        aspectRatio: Int,
        resolutionSelectorSetter: (ResolutionSelector) -> Unit,
    ) {
        if (aspectRatio != AspectRatio.RATIO_DEFAULT) {
            resolutionSelectorSetter(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                    )
                    .build()
            )
        }
    }

    protected fun List<AppUseCase>.toUseCases(aspectRatio: Int = AspectRatio.RATIO_DEFAULT) = map {
        when (it) {
            PREVIEW -> createPreview(aspectRatio)
            IMAGE_CAPTURE -> createImageCapture(aspectRatio)
            IMAGE_ANALYSIS -> createImageAnalysis(aspectRatio)
            VIDEO_CAPTURE -> createVideoCapture(aspectRatio)
        }
    }

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

    @SuppressLint("NewApi")
    protected suspend fun Set<GroupableFeature>.verifyFeatures(
        useCases: List<UseCase>,
        cameraInfo: CameraInfo,
        aspectRatio: Int = AspectRatio.RATIO_DEFAULT,
    ) {
        Log.d(TAG, "verifyFeatures: $this, useCases = $useCases")

        forEach {
            when {
                it == HDR_HLG10 -> {
                    // Reaching this stage before API 33 means query API didn't work correctly
                    require(Build.VERSION.SDK_INT >= 33)
                    verifyHlg10Hdr(useCases, cameraInfo)
                }
                it == FPS_60 -> verify60Fps(cameraInfo)
                it == PREVIEW_STABILIZATION ->
                    verifyPreviewStabilization(cameraInfo as CameraInfoInternal)
                it == IMAGE_ULTRA_HDR -> {
                    // Reaching this stage before API 34 means query API didn't work correctly
                    require(Build.VERSION.SDK_INT >= 34)
                    verifyUltraHdr(useCases, cameraInfo)
                }
                it.featureTypeInternal == FeatureTypeInternal.RECORDING_QUALITY ->
                    verifyRecordingQuality(useCases, it, aspectRatio)
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

    private fun verifyRecordingQuality(
        useCases: List<UseCase>,
        feature: GroupableFeature,
        aspectRatio: Int = AspectRatio.RATIO_DEFAULT,
    ) {
        val videoCapture = checkNotNull(useCases.firstOrNull { it is VideoCapture<*> })

        val expectedHeightRange =
            when (feature) {
                UHD_RECORDING -> Range(2160, 4319)
                FHD_RECORDING -> Range(1080, 1439)
                HD_RECORDING -> Range(720, 1079)
                SD_RECORDING -> Range(241, 719)
                else -> throw IllegalStateException("Unknown recording quality feature: $feature")
            }

        checkNotNull(videoCapture.attachedStreamSpec?.resolution).apply {
            if (aspectRatio != AspectRatio.RATIO_DEFAULT) {
                assertThat(
                        AspectRatioUtil.hasMatchingAspectRatio(
                            this,
                            when (aspectRatio) {
                                AspectRatio.RATIO_16_9 -> AspectRatioUtil.ASPECT_RATIO_16_9
                                AspectRatio.RATIO_4_3 -> AspectRatioUtil.ASPECT_RATIO_4_3
                                else ->
                                    throw IllegalStateException(
                                        "Unknown aspect ratio: $aspectRatio"
                                    )
                            },
                        )
                    )
                    .isTrue()
            }

            assertThat(expectedHeightRange.contains(min(this.width, this.height))).isTrue()
        }
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
        /** The most common use case combinations expected for feature group API. */
        val useCaseCombinationsToTest =
            listOf(
                listOf(PREVIEW, IMAGE_CAPTURE),
                listOf(PREVIEW, VIDEO_CAPTURE),
                listOf(PREVIEW, IMAGE_CAPTURE, VIDEO_CAPTURE),
                listOf(PREVIEW, VIDEO_CAPTURE, IMAGE_ANALYSIS),
            )

        val allHighQualityFeatures =
            setOf(
                HDR_HLG10,
                FPS_60,
                PREVIEW_STABILIZATION,
                VIDEO_STABILIZATION,
                IMAGE_ULTRA_HDR,
                UHD_RECORDING,
            )

        val allFeatures =
            setOf(
                HDR_HLG10,
                FPS_60,
                PREVIEW_STABILIZATION,
                VIDEO_STABILIZATION,
                IMAGE_ULTRA_HDR,
                UHD_RECORDING,
                FHD_RECORDING,
                HD_RECORDING,
                SD_RECORDING,
            )

        private const val TAG = "FeatureGroupTestBase"
    }
}
