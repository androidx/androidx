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
import android.os.Build
import android.os.Looper
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.arch.core.util.Function
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CamcorderProfileProxy
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.processing.SurfaceProcessorInternal
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_1080P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_480P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_1080P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_2160P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_480P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_720P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_QHD
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_QVGA
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_VGA
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeSurfaceProcessorInternal
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.HIGHEST
import androidx.camera.video.Quality.LOWEST
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.StreamInfo.StreamState
import androidx.camera.video.impl.VideoCaptureConfig
import androidx.camera.video.internal.encoder.FakeVideoEncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Collections
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
private val CAMERA_0_PROFILES = arrayOf(
    CamcorderProfileUtil.asHighQuality(PROFILE_2160P),
    PROFILE_2160P,
    PROFILE_720P,
    CamcorderProfileUtil.asLowQuality(PROFILE_720P)
)

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

    @Before
    fun setup() {
        ShadowLog.stream = System.out
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
    }

    @Test
    fun setNoCameraTransform_propagatesToCameraEdge() {
        // Arrange.
        setupCamera()
        val processor = createFakeSurfaceProcessor()
        val videoCapture = createVideoCapture(createVideoOutput(), processor = processor)
        // Act: set no transform and create pipeline.
        videoCapture.hasCameraTransform = false
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
        val processor = createFakeSurfaceProcessor()
        val videoCapture = createVideoCapture(createVideoOutput(), processor = processor)
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
            processor = createFakeSurfaceProcessor()
        )
    }

    @Test
    fun enableProcessor_sensorRotationIs90AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 90,
            processor = createFakeSurfaceProcessor()
        )
    }

    @Test
    fun enableProcessor_sensorRotationIs180AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 180,
            processor = createFakeSurfaceProcessor()
        )
    }

    @Test
    fun enableProcessor_sensorRotationIs270AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 270,
            processor = createFakeSurfaceProcessor()
        )
    }

    @Test
    fun invalidateAppSurfaceRequestWithProcessing_cameraNotReset() {
        // Arrange: create videoCapture with processing.
        setupCamera()
        createCameraUseCaseAdapter()
        val videoCapture =
            createVideoCapture(createVideoOutput(), processor = createFakeSurfaceProcessor())
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
        val videoCapture = createVideoCapture(createVideoOutput(), processor = processor)
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
        val videoCapture =
            createVideoCapture(createVideoOutput(), processor = createFakeSurfaceProcessor())
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
        processor: SurfaceProcessorInternal? = null
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
            val videoCapture = createVideoCapture(videoOutput, processor = processor)
            videoCapture.targetRotation = targetRotation

            // Act.
            addAndAttachUseCases(videoCapture)

            // Assert.
            val resolution = CAMERA_0_QUALITY_SIZE[quality]!!
            val expectedResolution = if (processor != null) {
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
    fun addUseCasesWithSurfaceProcessor_cameraIsUptime_requestIsUptime() {
        testTimebase(
            processor = createFakeSurfaceProcessor(),
            cameraTimebase = Timebase.UPTIME,
            expectedTimebase = Timebase.UPTIME
        )
    }

    @Test
    fun addUseCasesWithSurfaceProcessor_cameraIsRealtime_requestIsRealtime() {
        testTimebase(
            processor = createFakeSurfaceProcessor(),
            cameraTimebase = Timebase.REALTIME,
            expectedTimebase = Timebase.REALTIME
        )
    }

    private fun testTimebase(
        processor: SurfaceProcessorInternal? = null,
        cameraTimebase: Timebase,
        expectedTimebase: Timebase
    ) {
        // Arrange.
        setupCamera(timebase = cameraTimebase)
        createCameraUseCaseAdapter()

        var timebase: Timebase? = null
        val videoOutput = createVideoOutput(surfaceRequestListener = { _, tb ->
            timebase = tb
        })
        val videoCapture = createVideoCapture(videoOutput, processor = processor)

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
        setupCamera(
            profiles = arrayOf(
                CamcorderProfileUtil.asHighQuality(PROFILE_2160P),
                PROFILE_2160P,
                PROFILE_1080P,
                PROFILE_720P,
                PROFILE_480P,
                CamcorderProfileUtil.asLowQuality(PROFILE_480P)
            )
        )
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
            }.build()
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
        setupCamera(
            profiles = arrayOf(
                CamcorderProfileUtil.asHighQuality(PROFILE_2160P),
                PROFILE_2160P,
                PROFILE_1080P,
                PROFILE_720P,
                PROFILE_480P,
                CamcorderProfileUtil.asLowQuality(PROFILE_480P)
            )
        )
        createCameraUseCaseAdapter()

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.fromOrderedList(listOf(UHD, FHD, HD, SD)))
                it.setAspectRatio(RATIO_4_3)
            }.build()
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
        setupCamera(
            profiles = arrayOf(
                CamcorderProfileUtil.asHighQuality(PROFILE_2160P),
                PROFILE_2160P,
                PROFILE_1080P,
                PROFILE_720P,
                PROFILE_480P,
                CamcorderProfileUtil.asLowQuality(PROFILE_480P)
            )
        )
        createCameraUseCaseAdapter()

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.fromOrderedList(listOf(UHD, FHD, HD, SD)))
                it.setAspectRatio(RATIO_16_9)
            }.build()
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
            processor = createFakeSurfaceProcessor(),
            videoEncoderInfoFinder = {
                createVideoEncoderInfo(widthAlignment = 16, heightAlignment = 16)
            })

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
        setupCamera(profiles = emptyArray())
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
    fun setTargetRotation_rotationIsChanged() {
        // Arrange.
        val videoCapture = createVideoCapture()

        // Act.
        videoCapture.targetRotation = Surface.ROTATION_180

        // Assert.
        assertThat(videoCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
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

    @Test
    fun setTargetRotation_transformationInfoUpdated() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
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
        val videoCapture = createVideoCapture(videoOutput, targetRotation = Surface.ROTATION_90)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(transformationInfo!!.rotationDegrees).isEqualTo(270)

        // Act.
        videoCapture.targetRotation = Surface.ROTATION_180

        // Assert.
        assertThat(transformationInfo!!.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun bindAndUnbind_surfacesPropagated() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        val processor = FakeSurfaceProcessorInternal(
            mainThreadExecutor(),
            false
        )
        var appSurfaceReadyToRelease = false
        val videoOutput = createVideoOutput(surfaceRequestListener = { surfaceRequest, _ ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                mainThreadExecutor()
            ) {
                appSurfaceReadyToRelease = true
            }
        })
        val videoCapture = createVideoCapture(videoOutput, processor = processor)

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
        cropRect: Rect,
        expectedCropRect: Rect,
    ) {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        setSuggestedStreamSpec(quality)
        var surfaceRequest: SurfaceRequest? = null
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.from(quality))
            }.build(),
            surfaceRequestListener = { request, _ -> surfaceRequest = request }
        )
        val videoCapture = createVideoCapture(
            videoOutput,
            processor = createFakeSurfaceProcessor(),
            videoEncoderInfoFinder = { videoEncoderInfo }
        )
        videoCapture.setViewPortCropRect(cropRect)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(surfaceRequest).isNotNull()
        assertThat(surfaceRequest!!.resolution).isEqualTo(rectToSize(expectedCropRect))
        assertThat(videoCapture.cropRect).isEqualTo(expectedCropRect)
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
        surfaceRequestListener: (SurfaceRequest, Timebase) -> Unit = { surfaceRequest, _ ->
            surfaceRequest.willNotProvideSurface()
        }
    ): TestVideoOutput = TestVideoOutput(streamState, mediaSpec) { surfaceRequest, timebase ->
        surfaceRequestsToRelease.add(surfaceRequest)
        surfaceRequestListener.invoke(surfaceRequest, timebase)
    }

    private class TestVideoOutput constructor(
        streamState: StreamState,
        mediaSpec: MediaSpec?,
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

    private fun createCameraUseCaseAdapter() {
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)
    }

    private fun createVideoCapture(
        videoOutput: VideoOutput = createVideoOutput(),
        hasCameraTransform: Boolean = true,
        targetRotation: Int? = null,
        targetResolution: Size? = null,
        processor: SurfaceProcessorInternal? = null,
        videoEncoderInfoFinder: Function<VideoEncoderConfig, VideoEncoderInfo> =
            Function { createVideoEncoderInfo() },
    ): VideoCapture<VideoOutput> = VideoCapture.Builder(videoOutput)
        .setSessionOptionUnpacker { _, _ -> }
        .apply {
            targetRotation?.let { setTargetRotation(it) }
            targetResolution?.let { setTargetResolution(it) }
            setVideoEncoderInfoFinder(videoEncoderInfoFinder)
        }.build().apply {
            setProcessor(processor)
            setHasCameraTransform(hasCameraTransform)
        }

    private fun createFakeSurfaceProcessor() = FakeSurfaceProcessorInternal(mainThreadExecutor())

    private fun setSuggestedStreamSpec(quality: Quality) {
        setSuggestedStreamSpec(StreamSpec.builder(CAMERA_0_QUALITY_SIZE[quality]!!).build())
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
        sensorRotation: Int = 0,
        supportedResolutions: Map<Int, List<Size>> = CAMERA_0_SUPPORTED_RESOLUTION_MAP,
        vararg profiles: CamcorderProfileProxy = CAMERA_0_PROFILES,
        timebase: Timebase = Timebase.UPTIME,
    ) {
        cameraInfo = FakeCameraInfoInternal(cameraId, sensorRotation, LENS_FACING_BACK).apply {
            supportedResolutions.forEach { (format, resolutions) ->
                setSupportedResolutions(format, resolutions)
            }
            camcorderProfileProvider =
                FakeCamcorderProfileProvider.Builder().addProfile(*profiles).build()
            setTimebase(timebase)
        }
        camera = FakeCamera(cameraId, null, cameraInfo)

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

    companion object {
        private val CAMERA_0_QUALITY_SIZE: Map<Quality, Size> = mapOf(
            SD to RESOLUTION_480P,
            HD to RESOLUTION_720P,
            FHD to RESOLUTION_1080P,
            UHD to RESOLUTION_2160P,
            LOWEST to RESOLUTION_720P,
            HIGHEST to RESOLUTION_2160P,
        )

        private val CAMERA_0_SUPPORTED_RESOLUTION_MAP = mapOf(
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE to listOf(
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
    }
}
