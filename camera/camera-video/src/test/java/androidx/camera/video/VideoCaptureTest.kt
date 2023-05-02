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
import android.graphics.Rect
import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.arch.core.util.Function
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT
import androidx.camera.core.DynamicRange.FORMAT_HLG
import androidx.camera.core.MirrorMode.MIRROR_MODE_OFF
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.CameraOrientationUtil.surfaceRotationToDegrees
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.TransformUtils.within360
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_QHD
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_QVGA
import androidx.camera.testing.EncoderProfilesUtil.RESOLUTION_VGA
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.fakes.FakeSurfaceEffect
import androidx.camera.testing.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.fakes.FakeVideoEncoderInfo
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.HIGHEST
import androidx.camera.video.Quality.LOWEST
import androidx.camera.video.Quality.NONE
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.RecorderVideoCapabilities.CapabilitiesByQuality
import androidx.camera.video.StreamInfo.StreamState
import androidx.camera.video.impl.VideoCaptureConfig
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowLog

private val ANY_SIZE = Size(640, 480)
private const val CAMERA_ID_0 = "0"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoCaptureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var cameraFactory: CameraFactory
    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var surfaceManager: FakeCameraDeviceSurfaceManager
    private lateinit var camera: FakeCamera
    private var surfaceRequestsToRelease = mutableListOf<SurfaceRequest>()
    private val handlersToRelease = mutableListOf<Handler>()

    @Before
    fun setup() {
        ShadowLog.stream = System.out

        DefaultSurfaceProcessor.Factory.setSupplier { createFakeSurfaceProcessor() }
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            cameraUseCaseAdapter.apply {
                detachUseCases()
                removeUseCases(useCases)
                shadowOf(Looper.getMainLooper()).idle()
            }
        }
        surfaceRequestsToRelease.forEach {
            // If the request is already provided, then this is no-op.
            it.willNotProvideSurface()
        }
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
        for (handler in handlersToRelease) {
            handler.looper.quitSafely()
        }
    }

    @Test
    fun verifySupportedEffects() {
        val videoCapture = createVideoCapture(createVideoOutput())
        assertThat(videoCapture.isEffectTargetsSupported(VIDEO_CAPTURE)).isTrue()
        assertThat(videoCapture.isEffectTargetsSupported(PREVIEW or VIDEO_CAPTURE)).isTrue()
        assertThat(
            videoCapture.isEffectTargetsSupported(
                PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE
            )
        ).isTrue()
        assertThat(videoCapture.isEffectTargetsSupported(PREVIEW)).isFalse()
        assertThat(videoCapture.isEffectTargetsSupported(IMAGE_CAPTURE)).isFalse()
        assertThat(videoCapture.isEffectTargetsSupported(PREVIEW or IMAGE_CAPTURE)).isFalse()
    }

    @Test
    fun setNoCameraTransform_propagatesToCameraEdge() {
        // Arrange.
        setupCamera()
        val videoCapture = createVideoCapture(createVideoOutput())
        videoCapture.effect = createFakeEffect()
        camera.hasTransform = false
        // Act: set no transform and create pipeline.
        videoCapture.bindToCamera(camera, null, null)
        videoCapture.updateSuggestedStreamSpec(StreamSpec.builder(Size(640, 480)).build())
        videoCapture.onStateAttached()
        // Assert: camera edge does not have transform.
        assertThat(videoCapture.cameraEdge!!.hasCameraTransform()).isFalse()
        videoCapture.onStateDetached()
        videoCapture.unbindFromCamera(camera)
    }

    @Test
    fun cameraEdgeHasTransformByDefault() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))
        val videoCapture = createVideoCapture(createVideoOutput())
        // Act.
        addAndAttachUseCases(videoCapture)
        // Assert.
        assertThat(videoCapture.cameraEdge!!.hasCameraTransform()).isTrue()
    }

    @Test
    fun setTargetResolution_throwsException() {
        assertThrows(UnsupportedOperationException::class.java) {
            createVideoCapture(targetResolution = ANY_SIZE)
        }
    }

    @Test
    fun canGetVideoOutput() {
        // Arrange.
        val videoOutput = createVideoOutput()

        // Act.
        val videoCapture = createVideoCapture(videoOutput)

        // Assert.
        assertThat(videoCapture.output).isEqualTo(videoOutput)
    }

    @Test
    fun addUseCases_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution()
    }

    @Test
    fun enableProcessor_sensorRotationIs0AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 0,
            effect = createFakeEffect()
        )
    }

    @Test
    fun enableProcessor_sensorRotationIs90AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 90,
            effect = createFakeEffect()
        )
    }

    @Test
    fun enableProcessor_sensorRotationIs180AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 180,
            effect = createFakeEffect()
        )
    }

    @Test
    fun enableProcessor_sensorRotationIs270AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 270,
            effect = createFakeEffect()
        )
    }

    @Test
    fun invalidateAppSurfaceRequestWithProcessing_cameraNotReset() {
        // Arrange: create videoCapture with processing.
        setupCamera()
        createCameraUseCaseAdapter()
        val videoCapture = createVideoCapture(createVideoOutput())
        cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))
        addAndAttachUseCases(videoCapture)
        // Act: invalidate.
        videoCapture.surfaceRequest.invalidate()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert: videoCapture is not reset.
        assertThat(camera.useCaseResetHistory).isEmpty()
    }

    @Test
    fun invalidateNodeSurfaceRequest_cameraReset() {
        // Arrange: create videoCapture.
        setupCamera()
        createCameraUseCaseAdapter()
        val processor = createFakeSurfaceProcessor()
        val effect = createFakeEffect(processor)
        val videoCapture = createVideoCapture(createVideoOutput())
        cameraUseCaseAdapter.setEffects(listOf(effect))
        addAndAttachUseCases(videoCapture)
        // Act: invalidate.
        processor.surfaceRequest!!.invalidate()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert: videoCapture is reset.
        assertThat(camera.useCaseResetHistory).containsExactly(videoCapture)
    }

    @Test
    fun invalidateAppSurfaceRequestWithoutProcessing_cameraReset() {
        // Arrange: create videoCapture without processing.
        setupCamera()
        createCameraUseCaseAdapter()
        val videoCapture = createVideoCapture(createVideoOutput())
        addAndAttachUseCases(videoCapture)
        // Act: invalidate.
        videoCapture.surfaceRequest.invalidate()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert: videoCapture is reset.
        assertThat(camera.useCaseResetHistory).containsExactly(videoCapture)
    }

    @Test
    fun invalidateWhenDetached_appEdgeClosed() {
        // Arrange: create Preview with processing then detach.
        setupCamera()
        createCameraUseCaseAdapter()
        cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))
        val videoCapture = createVideoCapture(createVideoOutput())
        addAndAttachUseCases(videoCapture)
        val surfaceRequest = videoCapture.surfaceRequest
        detachAndRemoveUseCases(videoCapture)
        // Act: invalidate.
        surfaceRequest.invalidate()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert: camera is not reset.
        assertThat(camera.useCaseResetHistory).isEmpty()
        assertThat(surfaceRequest.deferrableSurface.isClosed).isTrue()
    }

    private fun testSetRotationWillSendCorrectResolution(
        sensorRotation: Int = 0,
        effect: CameraEffect? = null
    ) {
        setupCamera(sensorRotation = sensorRotation)
        createCameraUseCaseAdapter()
        val quality = HD

        listOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        ).forEach { targetRotation ->
            // Arrange.
            setSuggestedStreamSpec(quality)
            var surfaceRequest: SurfaceRequest? = null
            val videoOutput = createVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.from(quality))
                }.build(),
                surfaceRequestListener = { request, _ ->
                    surfaceRequest = request
                })
            val videoCapture = createVideoCapture(videoOutput)
            videoCapture.targetRotation = targetRotation
            effect?.apply { cameraUseCaseAdapter.setEffects(listOf(this)) }

            // Act.
            addAndAttachUseCases(videoCapture)

            // Assert.
            val resolution = CAMERA_0_QUALITY_SIZE[quality]!!
            val expectedResolution = if (effect != null) {
                rotateSize(resolution, cameraInfo.getSensorRotationDegrees(targetRotation))
            } else {
                resolution
            }
            assertThat(surfaceRequest).isNotNull()
            assertThat(surfaceRequest!!.resolution).isEqualTo(expectedResolution)

            // Clean-up.
            detachAndRemoveUseCases(videoCapture)
        }
    }

    @Test
    fun addUseCases_cameraIsUptime_requestIsUptime() {
        testTimebase(cameraTimebase = Timebase.UPTIME, expectedTimebase = Timebase.UPTIME)
    }

    @Test
    fun addUseCases_cameraIsRealtime_requestIsUptime() {
        testTimebase(cameraTimebase = Timebase.REALTIME, expectedTimebase = Timebase.UPTIME)
    }

    @Test
    fun addUseCasesWithoutCameraTransform_cameraIsRealtime_requestIsRealtime() {
        testTimebase(
            cameraTimebase = Timebase.REALTIME,
            expectedTimebase = Timebase.REALTIME,
            hasTransform = false
        )
    }

    @Test
    fun addUseCasesWithSurfaceProcessor_cameraIsUptime_requestIsUptime() {
        testTimebase(
            effect = createFakeEffect(),
            cameraTimebase = Timebase.UPTIME,
            expectedTimebase = Timebase.UPTIME
        )
    }

    @Test
    fun addUseCasesWithSurfaceProcessor_cameraIsRealtime_requestIsRealtime() {
        testTimebase(
            effect = createFakeEffect(),
            cameraTimebase = Timebase.REALTIME,
            expectedTimebase = Timebase.REALTIME
        )
    }

    private fun testTimebase(
        effect: CameraEffect? = null,
        cameraTimebase: Timebase,
        expectedTimebase: Timebase,
        hasTransform: Boolean = true,
    ) {
        // Arrange.
        setupCamera(timebase = cameraTimebase, hasTransform = hasTransform)
        createCameraUseCaseAdapter()

        var timebase: Timebase? = null
        val videoOutput = createVideoOutput(surfaceRequestListener = { _, tb ->
            timebase = tb
        })
        effect?.apply { cameraUseCaseAdapter.setEffects(listOf(this)) }
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(timebase).isEqualTo(expectedTimebase)
    }

    @Test
    fun addUseCases_withNullMediaSpec_throwException() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        val videoOutput = createVideoOutput(mediaSpec = null)
        val videoCapture = createVideoCapture(videoOutput)

        // Assert.
        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            // Act.
            addAndAttachUseCases(videoCapture)
        }
    }

    @Test
    fun setQualitySelector_sameResolutionAsQualitySelector() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        // Camera 0 support 2160P(UHD) and 720P(HD)
        arrayOf(UHD, HD, HIGHEST, LOWEST).forEach { quality ->
            setSuggestedStreamSpec(quality)

            val videoOutput = createVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.from(quality))
                }.build()
            )
            val videoCapture = createVideoCapture(videoOutput)

            // Act.
            addAndAttachUseCases(videoCapture)

            // Assert.
            assertThat(videoCapture.attachedSurfaceResolution)
                .isEqualTo(CAMERA_0_QUALITY_SIZE[quality]!!)

            // Clean up.
            detachAndRemoveUseCases(videoCapture)
        }
    }

    @Test
    fun setQualitySelector_sameCustomOrderedResolutions() {
        // Arrange.
        setupCamera(profiles = FULL_QUALITY_PROFILES_MAP)
        createCameraUseCaseAdapter()
        setSuggestedStreamSpec(StreamSpec.builder(RESOLUTION_480P).build())

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(
                    QualitySelector.fromOrderedList(
                        listOf(
                            UHD, // 2160P
                            SD, // 480P
                            HD, // 720P
                            FHD // 1080P
                        )
                    )
                )
            }.build(),
            videoCapabilities = FULL_QUALITY_VIDEO_CAPABILITIES
        )
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertCustomOrderedResolutions(
            videoCapture, RESOLUTION_2160P, RESOLUTION_480P, RESOLUTION_720P, RESOLUTION_1080P
        )
        assertThat(videoCapture.attachedSurfaceResolution).isEqualTo(RESOLUTION_480P)
    }

    @Test
    fun setAspectRatio_4by3() {
        // Arrange.
        setupCamera(profiles = FULL_QUALITY_PROFILES_MAP)
        createCameraUseCaseAdapter()

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.fromOrderedList(listOf(UHD, FHD, HD, SD)))
                it.setAspectRatio(RATIO_4_3)
            }.build(),
            videoCapabilities = FULL_QUALITY_VIDEO_CAPABILITIES
        )
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertCustomOrderedResolutions(
            videoCapture,
            // UHD
            Size(3120, 2340), Size(4000, 3000),
            // FHD
            Size(1440, 1080),
            // HD
            Size(960, 720), Size(1280, 960),
            // SD
            RESOLUTION_VGA,
        )
    }

    @Test
    fun setAspectRatio_16by9() {
        // Arrange.
        setupCamera(profiles = FULL_QUALITY_PROFILES_MAP)
        createCameraUseCaseAdapter()

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.fromOrderedList(listOf(UHD, FHD, HD, SD)))
                it.setAspectRatio(RATIO_16_9)
            }.build(),
            videoCapabilities = FULL_QUALITY_VIDEO_CAPABILITIES
        )
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertCustomOrderedResolutions(
            videoCapture,
            // UHD
            RESOLUTION_2160P,
            // FHD
            RESOLUTION_1080P,
            // HD
            RESOLUTION_720P,
            // SD
            Size(736, 412), Size(864, 480), Size(640, 360),
        )
    }

    @Test
    fun adjustInvalidResolution() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        setSuggestedStreamSpec(StreamSpec.builder(Size(639, 479)).build())

        val videoOutput = createVideoOutput()
        val videoCapture = createVideoCapture(
            videoOutput,
            videoEncoderInfoFinder = {
                createVideoEncoderInfo(widthAlignment = 16, heightAlignment = 16)
            })
        cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(rectToSize(videoCapture.cropRect!!)).isEqualTo(Size(624, 464))
    }

    @Test
    fun setQualitySelector_notSupportedQuality_throwException() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        // Camera 0 support 2160P(UHD) and 720P(HD)
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.from(FHD))
            }.build()
        )
        val videoCapture = createVideoCapture(videoOutput)

        // Assert.
        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            // Act.
            addAndAttachUseCases(videoCapture)
        }
    }

    @Test
    fun noSupportedQuality_supportedResolutionsIsNotSet() {
        // Arrange.
        setupCamera(profiles = emptyMap())
        createCameraUseCaseAdapter()

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.from(UHD))
            }.build()
        )
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        val supportedResolutionPairs = videoCapture.currentConfig.retrieveOption(
            ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS,
            null
        )
        assertThat(supportedResolutionPairs).isNull()
    }

    @Test
    fun removeUseCases_receiveResultOfSurfaceRequest() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        var surfaceResult: SurfaceRequest.Result? = null
        val videoOutput = createVideoOutput { surfaceRequest, _ ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                directExecutor()
            ) { surfaceResult = it }
        }
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        // Surface is in use, should not receive any result.
        assertThat(surfaceResult).isNull()

        // Act.
        cameraUseCaseAdapter.removeUseCases(listOf(videoCapture))

        // Assert.
        assertThat(surfaceResult!!.resultCode).isEqualTo(
            SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY
        )
    }

    @Test
    fun detachUseCases_receiveResultOfSurfaceRequest() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        var surfaceResult: SurfaceRequest.Result? = null
        val videoOutput = createVideoOutput { surfaceRequest, _ ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                directExecutor()
            ) { surfaceResult = it }
        }
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        // Surface is in use, should not receive any result.
        assertThat(surfaceResult).isNull()

        // Act.
        cameraUseCaseAdapter.detachUseCases()

        // Assert.
        assertThat(surfaceResult!!.resultCode).isEqualTo(
            SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY
        )
    }

    @Test
    fun hasSurfaceProcessingQuirk_nodeIsNeeded() {
        // Arrange.
        VideoCapture.sEnableSurfaceProcessingByQuirk = true
        setupCamera()
        createCameraUseCaseAdapter()

        // Act.
        val videoCapture = createVideoCapture()
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(videoCapture.node).isNotNull()

        // Clean-up.
        VideoCapture.sEnableSurfaceProcessingByQuirk = false
    }

    @Test
    fun hasSurfaceProcessingQuirkButNoCameraTransform_nodeIsNotNeeded() {
        // Arrange.
        VideoCapture.sEnableSurfaceProcessingByQuirk = true
        setupCamera(hasTransform = false)
        createCameraUseCaseAdapter()

        // Act.
        val videoCapture = createVideoCapture()
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(videoCapture.node).isNull()

        // Clean-up.
        VideoCapture.sEnableSurfaceProcessingByQuirk = false
    }

    @Test
    fun defaultDynamicRangeIsSdr() {
        val videoCapture = createVideoCapture()
        assertThat(videoCapture.dynamicRange).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun canSetDynamicRange() {
        val videoCapture = createVideoCapture(
            dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
        )
        assertThat(videoCapture.dynamicRange).isEqualTo(DynamicRange.HDR_UNSPECIFIED_10_BIT)
    }

    @Test
    fun defaultMirrorModeIsOff() {
        val videoCapture = createVideoCapture()
        assertThat(videoCapture.mirrorMode).isEqualTo(MIRROR_MODE_OFF)
    }

    @Test
    fun canGetSetMirrorMode() {
        val videoCapture = createVideoCapture(mirrorMode = MIRROR_MODE_ON_FRONT_ONLY)
        assertThat(videoCapture.mirrorMode).isEqualTo(MIRROR_MODE_ON_FRONT_ONLY)
    }

    @Test
    fun setMirrorMode_nodeIsNeeded() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        // Act.
        val videoCapture = createVideoCapture(mirrorMode = MIRROR_MODE_ON)
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(videoCapture.node).isNotNull()
    }

    @Test
    fun setMirrorMode_noCameraTransform_nodeIsNotNeeded() {
        // Arrange.
        setupCamera(hasTransform = false)
        createCameraUseCaseAdapter()

        // Act.
        val videoCapture = createVideoCapture(mirrorMode = MIRROR_MODE_ON)
        addAndAttachUseCases(videoCapture)

        // Assert: the input stream should already be mirrored.
        assertThat(videoCapture.node).isNull()
    }

    @Test
    fun setTargetRotationInBuilder_rotationIsChanged() {
        // Act.
        val videoCapture = createVideoCapture(targetRotation = Surface.ROTATION_180)

        // Assert.
        assertThat(videoCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun setTargetRotation_rotationIsChanged() {
        // Arrange.
        val videoCapture = createVideoCapture()

        // Act.
        videoCapture.targetRotation = Surface.ROTATION_180

        // Assert.
        assertThat(videoCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun setTargetRotationWithEffect_rotationChangesOnSurfaceEdge() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        val videoCapture = createVideoCapture()
        cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))
        addAndAttachUseCases(videoCapture)

        // Act: update target rotation
        videoCapture.targetRotation = Surface.ROTATION_0
        shadowOf(Looper.getMainLooper()).idle()
        // Assert that the rotation of the SettableFuture is updated based on ROTATION_0.
        assertThat(videoCapture.cameraEdge!!.rotationDegrees).isEqualTo(0)

        // Act: update target rotation again.
        videoCapture.targetRotation = Surface.ROTATION_180
        shadowOf(Looper.getMainLooper()).idle()
        // Assert: the rotation of the SettableFuture is updated based on ROTATION_90.
        assertThat(videoCapture.cameraEdge!!.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun setTargetRotationWithEffectOnBackground_rotationChangesOnSurfaceEdge() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        val videoCapture = createVideoCapture()
        cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))
        addAndAttachUseCases(videoCapture)
        val backgroundHandler = createBackgroundHandler()

        // Act: update target rotation
        backgroundHandler.post { videoCapture.targetRotation = Surface.ROTATION_0 }
        shadowOf(backgroundHandler.looper).idle()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert that the rotation of the SettableFuture is updated based on ROTATION_0.
        assertThat(videoCapture.cameraEdge!!.rotationDegrees).isEqualTo(0)

        // Act: update target rotation again.
        backgroundHandler.post { videoCapture.targetRotation = Surface.ROTATION_180 }
        shadowOf(backgroundHandler.looper).idle()
        shadowOf(Looper.getMainLooper()).idle()
        // Assert: the rotation of the SettableFuture is updated based on ROTATION_90.
        assertThat(videoCapture.cameraEdge!!.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun addUseCases_transformationInfoUpdated() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        val listener = mock(SurfaceRequest.TransformationInfoListener::class.java)
        val videoOutput = createVideoOutput(
            surfaceRequestListener = { surfaceRequest, _ ->
                surfaceRequest.setTransformationInfoListener(
                    directExecutor(),
                    listener
                )
            }
        )

        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        verify(listener).onTransformationInfoUpdate(any())
    }

    // Test setTargetRotation with common back and front camera properties and various conditions.
    @Test
    fun setTargetRotation_backCameraInitial0_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            initialTargetRotation = Surface.ROTATION_0,
        )
    }

    @Test
    fun setTargetRotation_backCameraInitial0MirrorOn_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            initialTargetRotation = Surface.ROTATION_0,
            mirrorMode = MIRROR_MODE_ON
        )
    }

    @Test
    fun setTargetRotation_backCameraInitial0NoCameraTransform_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            hasCameraTransform = false,
            initialTargetRotation = Surface.ROTATION_0,
        )
    }

    @Test
    fun setTargetRotation_backCameraInitial0NoCameraTransformMirrorOn_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            hasCameraTransform = false,
            initialTargetRotation = Surface.ROTATION_0,
            mirrorMode = MIRROR_MODE_ON,
        )
    }

    @Test
    fun setTargetRotation_backCameraInitial90_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            initialTargetRotation = Surface.ROTATION_90,
        )
    }

    @Test
    fun setTargetRotation_frontCameraInitial0_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_FRONT,
            sensorRotationDegrees = 270,
            initialTargetRotation = Surface.ROTATION_0,
        )
    }

    @Test
    fun setTargetRotation_frontCameraInitial0MirrorOn_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_FRONT,
            sensorRotationDegrees = 270,
            initialTargetRotation = Surface.ROTATION_0,
            mirrorMode = MIRROR_MODE_ON
        )
    }

    @Test
    fun setTargetRotation_frontCameraInitial90_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_FRONT,
            sensorRotationDegrees = 270,
            initialTargetRotation = Surface.ROTATION_90,
        )
    }

    @Test
    fun setTargetRotation_withEffectBackCameraInitial0_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            effect = createFakeEffect(),
            initialTargetRotation = Surface.ROTATION_0,
        )
    }

    @Test
    fun setTargetRotation_withEffectBackCameraInitial90_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_BACK,
            sensorRotationDegrees = 90,
            effect = createFakeEffect(),
            initialTargetRotation = Surface.ROTATION_90,
        )
    }

    @Test
    fun setTargetRotation_withEffectFrontCameraInitial0_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_FRONT,
            sensorRotationDegrees = 270,
            effect = createFakeEffect(),
            initialTargetRotation = Surface.ROTATION_90,
        )
    }

    @Test
    fun setTargetRotation_withEffectFrontCameraInitial90_transformationInfoUpdated() {
        testSetTargetRotation_transformationInfoUpdated(
            lensFacing = LENS_FACING_FRONT,
            sensorRotationDegrees = 270,
            effect = createFakeEffect(),
            initialTargetRotation = Surface.ROTATION_90,
        )
    }

    @Test
    fun suggestedStreamSpecFrameRate_isPropagatedToSurfaceRequest() {
        // [24, 24] is what will be chosen by the stream spec. By setting the target to another
        // value, this ensures the SurfaceRequest is getting what comes from the stream spec rather
        // than just from the target.
        testSurfaceRequestContainsExpected(
            targetFrameRate = FRAME_RATE_RANGE_FIXED_30,
            expectedFrameRate = FRAME_RATE_RANGE_FIXED_24
        )
    }

    @Test
    fun unspecifiedStreamSpecFrameRate_sendsDefaultFrameRateToSurfaceRequest() {
        // Currently we assume a fixed [30, 30] for VideoCapture since that is typically the fixed
        // frame rate that most devices will choose for a video template. In the future we may
        // try to query the device for this default frame rate.
        testSurfaceRequestContainsExpected(
            expectedFrameRate = FRAME_RATE_RANGE_FIXED_30
        )
    }

    @Test
    fun suggestedStreamSpecDynamicRange_isPropagatedToSurfaceRequest() {
        // This ensures the dynamic range set on the VideoCapture.Builder is not just directly
        // propagated to the SurfaceRequest. It should come from the StreamSpec.
        testSurfaceRequestContainsExpected(
            requestedDynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT,
            expectedDynamicRange = DynamicRange(FORMAT_HLG, BIT_DEPTH_10_BIT)
        )
    }

    private fun testSetTargetRotation_transformationInfoUpdated(
        lensFacing: Int = LENS_FACING_BACK,
        sensorRotationDegrees: Int = 0,
        hasCameraTransform: Boolean = true,
        effect: CameraEffect? = null,
        initialTargetRotation: Int = Surface.ROTATION_0,
        mirrorMode: Int? = null,
    ) {
        // Arrange.
        setupCamera(
            lensFacing = lensFacing,
            sensorRotation = sensorRotationDegrees,
            hasTransform = hasCameraTransform,
        )
        createCameraUseCaseAdapter(lensFacing = lensFacing)
        var transformationInfo: SurfaceRequest.TransformationInfo? = null
        val videoOutput = createVideoOutput(
            surfaceRequestListener = { surfaceRequest, _ ->
                surfaceRequest.setTransformationInfoListener(
                    directExecutor()
                ) {
                    transformationInfo = it
                }
            }
        )
        val videoCapture = createVideoCapture(
            videoOutput,
            targetRotation = initialTargetRotation,
            mirrorMode = mirrorMode
        )
        val requireMirroring = videoCapture.isMirroringRequired(camera)

        // Act.
        effect?.apply { cameraUseCaseAdapter.setEffects(listOf(this)) }
        addAndAttachUseCases(videoCapture)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert.
        var videoContentDegrees: Int
        var metadataDegrees: Int
        cameraInfo.getRelativeRotation(initialTargetRotation, requireMirroring).let {
            if (videoCapture.node != null) {
                // If effect is enabled, the rotation is applied on video content but not metadata.
                videoContentDegrees = it
                metadataDegrees = 0
            } else {
                videoContentDegrees = 0
                metadataDegrees = it
            }
        }
        assertThat(transformationInfo!!.rotationDegrees).isEqualTo(metadataDegrees)

        // Act: Test all 4 rotation degrees.
        for (targetRotation in listOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        )) {
            videoCapture.targetRotation = targetRotation
            shadowOf(Looper.getMainLooper()).idle()

            // Assert.
            val requiredDegrees = cameraInfo.getRelativeRotation(targetRotation, requireMirroring)
            val expectedDegrees = if (videoCapture.node != null) {
                // If effect is enabled, the rotation should eliminate the video content rotation.
                within360(requiredDegrees - videoContentDegrees)
            } else {
                requiredDegrees
            }
            val message = "lensFacing = $lensFacing" +
                ", sensorRotationDegrees = $sensorRotationDegrees" +
                ", initialTargetRotation = $initialTargetRotation" +
                ", targetRotation = ${surfaceRotationToDegrees(targetRotation)}" +
                ", effect = ${effect != null}" +
                ", requireMirroring = $requireMirroring" +
                ", videoContentDegrees = $videoContentDegrees" +
                ", metadataDegrees = $metadataDegrees" +
                ", requiredDegrees = $requiredDegrees" +
                ", expectedDegrees = $expectedDegrees" +
                ", transformationInfo.rotationDegrees = " + transformationInfo!!.rotationDegrees
            assertWithMessage(message).that(transformationInfo!!.rotationDegrees)
                .isEqualTo(expectedDegrees)
        }
    }

    @Test
    fun bindAndUnbind_surfacesPropagated() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        val processor = createFakeSurfaceProcessor(autoCloseSurfaceOutput = false)
        var appSurfaceReadyToRelease = false
        val videoOutput = createVideoOutput(surfaceRequestListener = { surfaceRequest, _ ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                mainThreadExecutor()
            ) {
                appSurfaceReadyToRelease = true
            }
        })

        val effect = createFakeEffect(processor)
        cameraUseCaseAdapter.setEffects(listOf(effect))
        val videoCapture = createVideoCapture(videoOutput)

        // Act: bind and provide Surface.
        addAndAttachUseCases(videoCapture)

        // Assert: surfaceOutput received.
        assertThat(processor.surfaceOutputs).hasSize(1)
        assertThat(processor.isReleased).isFalse()
        assertThat(processor.isOutputSurfaceRequestedToClose[VIDEO_CAPTURE]).isNull()
        assertThat(processor.isInputSurfaceReleased).isFalse()
        assertThat(appSurfaceReadyToRelease).isFalse()
        // processor surface is provided to camera.
        assertThat(videoCapture.sessionConfig.surfaces[0].surface.get())
            .isEqualTo(processor.inputSurface)

        // Act: unbind.
        detachAndRemoveUseCases(videoCapture)

        // Assert: processor and processor surface is released.
        assertThat(processor.isReleased).isTrue()
        assertThat(processor.isOutputSurfaceRequestedToClose[VIDEO_CAPTURE]).isTrue()
        assertThat(processor.isInputSurfaceReleased).isTrue()
        assertThat(appSurfaceReadyToRelease).isFalse()

        // Act: close SurfaceOutput
        processor.surfaceOutputs[VIDEO_CAPTURE]!!.close()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(appSurfaceReadyToRelease).isTrue()
    }

    @Test
    fun adjustCropRect_noAdjustment() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(widthAlignment = 8, heightAlignment = 8),
            cropRect = Rect(8, 8, 808, 608),
            expectedCropRect = Rect(8, 8, 808, 608),
        )
    }

    @Test
    fun adjustCropRect_toSmallerSize() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(widthAlignment = 8, heightAlignment = 8),
            cropRect = Rect(8, 8, 811, 608), // 803x600 -> 800x600
            expectedCropRect = Rect(9, 8, 809, 608),
        )
    }

    @Test
    fun adjustCropRect_toLargerSize() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(widthAlignment = 8, heightAlignment = 8),
            cropRect = Rect(8, 8, 805, 608), // 797x600 -> 800x600
            expectedCropRect = Rect(6, 8, 806, 608),
        )
    }

    @Test
    fun adjustCropRect_toLargerSize_fromTopLeft() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(widthAlignment = 8, heightAlignment = 8),
            cropRect = Rect(0, 0, 797, 600), // 797x600 -> 800x600
            expectedCropRect = Rect(0, 0, 800, 600),
        )
    }

    @Test
    fun adjustCropRect_toLargerSize_fromBottomRight() {
        testAdjustCropRectToValidSize(
            // Quality.HD maps to 1280x720 (4:3)
            videoEncoderInfo = createVideoEncoderInfo(widthAlignment = 8, heightAlignment = 8),
            cropRect = Rect(1280 - 797, 720 - 600, 1280, 720), // 797x600 -> 800x600
            expectedCropRect = Rect(1280 - 800, 720 - 600, 1280, 720),
        )
    }

    @Test
    fun adjustCropRect_clampBySupportedWidthsHeights() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(
                widthAlignment = 8,
                heightAlignment = 8,
                // 1280x720 is a valid size
                supportedWidths = Range(80, 1600),
                supportedHeights = Range(100, 1600),
            ),
            cropRect = Rect(8, 8, 48, 48), // 40x40
            expectedCropRect = Rect(0, 0, 80, 100),
        )
    }

    @Test
    fun adjustCropRect_notValidSize_ignoreSupportedSizeAndClampByWorkaroundSize() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(
                widthAlignment = 8,
                heightAlignment = 8,
                // 1280x720 is not a valid size, workaround size is [8-4096], [8-2160]
                supportedWidths = Range(80, 80),
                supportedHeights = Range(80, 80),
            ),
            cropRect = Rect(0, 0, 4, 4), // 4x4
            expectedCropRect = Rect(0, 0, 8, 8), // 8x8
        )
    }

    @Test
    fun adjustCropRect_toSmallestDimensionChange() {
        testAdjustCropRectToValidSize(
            videoEncoderInfo = createVideoEncoderInfo(widthAlignment = 8, heightAlignment = 8),
            cropRect = Rect(8, 8, 811, 607), // 803x599 -> 800x600
            expectedCropRect = Rect(9, 7, 809, 607),
        )
    }

    private fun testAdjustCropRectToValidSize(
        quality: Quality = HD, // HD maps to 1280x720 (4:3)
        videoEncoderInfo: VideoEncoderInfo = createVideoEncoderInfo(),
        cropRect: Rect? = null,
        expectedCropRect: Rect? = null
    ) {
        testSurfaceRequestContainsExpected(
            quality = quality,
            videoEncoderInfo = videoEncoderInfo,
            cropRect = cropRect,
            expectedCropRect = expectedCropRect
        )
    }

    private fun testSurfaceRequestContainsExpected(
        quality: Quality = HD, // HD maps to 1280x720 (4:3)
        videoEncoderInfo: VideoEncoderInfo = createVideoEncoderInfo(),
        cropRect: Rect? = null,
        expectedCropRect: Rect? = null,
        targetFrameRate: Range<Int>? = null,
        expectedFrameRate: Range<Int> = SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
        requestedDynamicRange: DynamicRange? = null,
        expectedDynamicRange: DynamicRange? = null
    ) {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        setSuggestedStreamSpec(
            quality,
            expectedFrameRate = expectedFrameRate,
            dynamicRange = expectedDynamicRange
        )
        var surfaceRequest: SurfaceRequest? = null
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.from(quality))
            }.build(),
            surfaceRequestListener = { request, _ -> surfaceRequest = request },
        )
        val videoCapture = createVideoCapture(
            videoOutput,
            videoEncoderInfoFinder = { videoEncoderInfo },
            targetFrameRate = targetFrameRate,
            dynamicRange = requestedDynamicRange
        )

        cropRect?.let {
            cameraUseCaseAdapter.setEffects(listOf(createFakeEffect()))
            videoCapture.setViewPortCropRect(it)
        }

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(surfaceRequest).isNotNull()
        expectedCropRect?.let {
            assertThat(surfaceRequest!!.resolution).isEqualTo(rectToSize(it))
            assertThat(videoCapture.cropRect).isEqualTo(it)
        }

        if (expectedFrameRate != StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED) {
            assertThat(surfaceRequest!!.expectedFrameRate).isEqualTo(expectedFrameRate)
        }

        expectedDynamicRange?.let {
            assertThat(surfaceRequest!!.dynamicRange).isEqualTo(expectedDynamicRange)
        }
    }

    private fun assertCustomOrderedResolutions(
        videoCapture: VideoCapture<out VideoOutput>,
        vararg expectedResolutions: Size
    ) {
        val resolutions = (videoCapture.currentConfig as ImageOutputConfig).customOrderedResolutions
        assertThat(resolutions).containsExactlyElementsIn(expectedResolutions).inOrder()
    }

    private fun createVideoEncoderInfo(
        widthAlignment: Int = 1,
        heightAlignment: Int = 1,
        supportedWidths: Range<Int> = Range.create(1, Integer.MAX_VALUE),
        supportedHeights: Range<Int> = Range.create(1, Integer.MAX_VALUE),
    ): VideoEncoderInfo {
        return FakeVideoEncoderInfo(
            _widthAlignment = widthAlignment,
            _heightAlignment = heightAlignment,
            _supportedWidths = supportedWidths,
            _supportedHeights = supportedHeights,
        )
    }

    private fun createVideoOutput(
        streamState: StreamState = StreamState.ACTIVE,
        mediaSpec: MediaSpec? = MediaSpec.builder().build(),
        videoCapabilities: VideoCapabilities = CAMERA_0_VIDEO_CAPABILITIES,
        surfaceRequestListener: (SurfaceRequest, Timebase) -> Unit = { surfaceRequest, _ ->
            surfaceRequest.willNotProvideSurface()
        },
    ): TestVideoOutput =
        TestVideoOutput(streamState, mediaSpec, videoCapabilities) { surfaceRequest, timebase ->
            surfaceRequestsToRelease.add(surfaceRequest)
            surfaceRequestListener.invoke(surfaceRequest, timebase)
        }

    private class TestVideoOutput constructor(
        streamState: StreamState,
        mediaSpec: MediaSpec?,
        val videoCapabilities: VideoCapabilities = CAMERA_0_VIDEO_CAPABILITIES,
        val surfaceRequestCallback: (SurfaceRequest, Timebase) -> Unit
    ) : VideoOutput {

        private val streamInfoObservable: MutableStateObservable<StreamInfo> =
            MutableStateObservable.withInitialState(
                StreamInfo.of(
                    StreamInfo.STREAM_ID_ANY,
                    streamState
                )
            )

        private val mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(mediaSpec)

        override fun onSurfaceRequested(surfaceRequest: SurfaceRequest) {
            surfaceRequestCallback.invoke(surfaceRequest, Timebase.UPTIME)
        }

        override fun onSurfaceRequested(surfaceRequest: SurfaceRequest, timebase: Timebase) {
            surfaceRequestCallback.invoke(surfaceRequest, timebase)
        }

        override fun getStreamInfo(): Observable<StreamInfo> = streamInfoObservable

        override fun getMediaSpec(): Observable<MediaSpec> = mediaSpecObservable

        override fun getMediaCapabilities(cameraInfo: CameraInfo): VideoCapabilities {
            return videoCapabilities
        }
    }

    private fun addAndAttachUseCases(vararg useCases: UseCase) {
        cameraUseCaseAdapter.addUseCases(useCases.asList())
        cameraUseCaseAdapter.attachUseCases()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun detachAndRemoveUseCases(vararg useCases: UseCase) {
        cameraUseCaseAdapter.detachUseCases()
        cameraUseCaseAdapter.removeUseCases(useCases.asList())
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun createCameraUseCaseAdapter(lensFacing: Int = LENS_FACING_BACK) {
        val cameraSelector = if (lensFacing == LENS_FACING_FRONT) DEFAULT_FRONT_CAMERA
        else DEFAULT_BACK_CAMERA
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
    }

    private fun createVideoCapture(
        videoOutput: VideoOutput = createVideoOutput(),
        targetRotation: Int? = null,
        mirrorMode: Int? = null,
        targetResolution: Size? = null,
        targetFrameRate: Range<Int>? = null,
        dynamicRange: DynamicRange? = null,
        videoEncoderInfoFinder: Function<VideoEncoderConfig, VideoEncoderInfo> =
            Function { createVideoEncoderInfo() },
    ): VideoCapture<VideoOutput> = VideoCapture.Builder(videoOutput)
        .setSessionOptionUnpacker { _, _, _ -> }
        .apply {
            targetRotation?.let { setTargetRotation(it) }
            mirrorMode?.let { setMirrorMode(it) }
            targetResolution?.let { setTargetResolution(it) }
            targetFrameRate?.let { setTargetFrameRate(it) }
            dynamicRange?.let { setDynamicRange(it) }
            setVideoEncoderInfoFinder(videoEncoderInfoFinder)
        }.build()

    private fun createFakeEffect(
        processor: FakeSurfaceProcessorInternal = createFakeSurfaceProcessor()
    ) = FakeSurfaceEffect(
        VIDEO_CAPTURE,
        processor
    )

    private fun createFakeSurfaceProcessor(
        executor: Executor = mainThreadExecutor(),
        autoCloseSurfaceOutput: Boolean = true
    ) = FakeSurfaceProcessorInternal(executor, autoCloseSurfaceOutput)

    private fun createBackgroundHandler(): Handler {
        val handler = Handler(HandlerThread("VideoCaptureTest").run {
            start()
            looper
        })
        handlersToRelease.add(handler)
        return handler
    }

    private fun setSuggestedStreamSpec(
        quality: Quality,
        expectedFrameRate: Range<Int> = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED,
        dynamicRange: DynamicRange? = null
    ) {
        setSuggestedStreamSpec(
            StreamSpec.builder(CAMERA_0_QUALITY_SIZE[quality]!!).apply {
                setExpectedFrameRateRange(expectedFrameRate)
                dynamicRange?.let { setDynamicRange(dynamicRange) }
            }.build()
        )
    }

    private fun setSuggestedStreamSpec(streamSpec: StreamSpec) {
        surfaceManager.setSuggestedStreamSpec(
            CAMERA_ID_0,
            VideoCaptureConfig::class.java,
            streamSpec
        )
    }

    private fun setupCamera(
        cameraId: String = CAMERA_ID_0,
        lensFacing: Int = LENS_FACING_BACK,
        sensorRotation: Int = 0,
        hasTransform: Boolean = true,
        supportedResolutions: Map<Int, List<Size>> = SUPPORTED_RESOLUTION_MAP,
        profiles: Map<Int, EncoderProfilesProxy> = CAMERA_0_PROFILES,
        timebase: Timebase = Timebase.UPTIME,
    ) {
        cameraInfo = FakeCameraInfoInternal(cameraId, sensorRotation, lensFacing).apply {
            supportedResolutions.forEach { (format, resolutions) ->
                setSupportedResolutions(format, resolutions)
            }
            encoderProfilesProvider = FakeEncoderProfilesProvider.Builder().addAll(profiles).build()
            setTimebase(timebase)
        }
        camera = FakeCamera(cameraId, null, cameraInfo)
        camera.hasTransform = hasTransform

        cameraFactory = FakeCameraFactory().apply {
            insertDefaultBackCamera(cameraId) { camera }
        }

        initCameraX()
    }

    private fun initCameraX() {
        surfaceManager = FakeCameraDeviceSurfaceManager()

        val cameraXConfig = CameraXConfig.Builder.fromConfig(FakeAppConfig.create())
            .setCameraFactoryProvider { _, _, _ -> cameraFactory }
            .setDeviceSurfaceManagerProvider { _, _, _ -> surfaceManager }
            .build()
        CameraXUtil.initialize(context, cameraXConfig).get()
    }

    private fun CameraInfoInternal.getRelativeRotation(
        targetRotation: Int,
        requireMirroring: Boolean
    ) = getSensorRotationDegrees(targetRotation).let {
        if (requireMirroring) {
            within360(-it)
        } else it
    }

    companion object {
        private val CAMERA_0_QUALITY_SIZE: Map<Quality, Size> = mapOf(
            SD to RESOLUTION_480P,
            HD to RESOLUTION_720P,
            FHD to RESOLUTION_1080P,
            UHD to RESOLUTION_2160P,
            LOWEST to RESOLUTION_720P,
            HIGHEST to RESOLUTION_2160P,
        )

        private val FRAME_RATE_RANGE_FIXED_24 = Range(24, 24)
        private val FRAME_RATE_RANGE_FIXED_30 = Range(30, 30)

        private val SUPPORTED_RESOLUTION_MAP = mapOf(
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE to listOf(
                // 4:3
                // UHD
                Size(4000, 3000), Size(3120, 2340),
                // FHD
                Size(1440, 1080),
                // HD
                Size(960, 720), Size(1280, 960),
                // SD
                RESOLUTION_VGA,

                // 16:9
                // UHD
                RESOLUTION_2160P,
                // FHD
                RESOLUTION_1080P,
                // HD
                RESOLUTION_720P,
                // SD
                Size(864, 480), Size(736, 412), Size(640, 360),

                // Other rations
                RESOLUTION_480P, RESOLUTION_QHD, RESOLUTION_QVGA
            ).apply {
                // Sort from large to small as default.
                Collections.sort(this, CompareSizesByArea(true))
            })

        private val FULL_QUALITY_PROFILES_MAP = mapOf(
            QUALITY_HIGH to PROFILES_2160P,
            QUALITY_2160P to PROFILES_2160P,
            QUALITY_1080P to PROFILES_1080P,
            QUALITY_720P to PROFILES_720P,
            QUALITY_480P to PROFILES_480P,
            QUALITY_LOW to PROFILES_480P
        )

        private val CAMERA_0_PROFILES = mapOf(
            QUALITY_HIGH to PROFILES_2160P,
            QUALITY_2160P to PROFILES_2160P,
            QUALITY_720P to PROFILES_720P,
            QUALITY_LOW to PROFILES_720P,
        )

        // TODO(b/278168212): Only SDR is checked by now. The default value of DynamicRange in
        //  VideoCapture is SDR.
        private val FULL_QUALITY_VIDEO_CAPABILITIES = createFakeVideoCapabilities(
            mapOf(DynamicRange.SDR to FULL_QUALITY_PROFILES_MAP)
        )

        private val CAMERA_0_VIDEO_CAPABILITIES = createFakeVideoCapabilities(
            mapOf(DynamicRange.SDR to CAMERA_0_PROFILES)
        )

        /**
         * Create a fake VideoCapabilities.
         */
        private fun createFakeVideoCapabilities(
            profilesMap: Map<DynamicRange, Map<Int, EncoderProfilesProxy>>
        ): VideoCapabilities {
            val videoCapabilitiesMap = profilesMap.mapValues {
                val provider = FakeEncoderProfilesProvider.Builder().addAll(it.value).build()
                CapabilitiesByQuality(provider)
            }

            return object : VideoCapabilities {

                override fun getSupportedDynamicRanges(): MutableSet<DynamicRange> {
                    return videoCapabilitiesMap.keys.toMutableSet()
                }

                override fun getSupportedQualities(
                    dynamicRange: DynamicRange
                ): MutableList<Quality> {
                    return videoCapabilitiesMap[dynamicRange]?.supportedQualities ?: mutableListOf()
                }

                override fun isQualitySupported(
                    quality: Quality,
                    dynamicRange: DynamicRange
                ): Boolean {
                    return videoCapabilitiesMap[dynamicRange]?.isQualitySupported(quality) ?: false
                }

                override fun getProfiles(
                    quality: Quality,
                    dynamicRange: DynamicRange
                ): VideoValidatedEncoderProfilesProxy? {
                    return videoCapabilitiesMap[dynamicRange]?.getProfiles(quality)
                }

                override fun findHighestSupportedEncoderProfilesFor(
                    size: Size,
                    dynamicRange: DynamicRange
                ): VideoValidatedEncoderProfilesProxy? {
                    return videoCapabilitiesMap[dynamicRange]
                        ?.findHighestSupportedEncoderProfilesFor(size)
                }

                override fun findHighestSupportedQualityFor(
                    size: Size,
                    dynamicRange: DynamicRange
                ): Quality {
                    return videoCapabilitiesMap[dynamicRange]
                        ?.findHighestSupportedQualityFor(size) ?: NONE
                }
            }
        }
    }
}
