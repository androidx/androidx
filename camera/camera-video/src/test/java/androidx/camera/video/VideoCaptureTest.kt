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
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CamcorderProfileProxy
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.processing.SurfaceEffectInternal
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_1080P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_480P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_1080P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_2160P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_480P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_720P
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeSurfaceEffectInternal
import androidx.camera.video.StreamInfo.StreamState
import androidx.camera.video.impl.VideoCaptureConfig
import androidx.camera.video.internal.encoder.FakeVideoEncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

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
    private var surfaceRequestsToRelease = mutableListOf<SurfaceRequest>()

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
    fun enableEffect_sensorRotationIs0AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 0,
            effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor())
        )
    }

    @Test
    fun enableEffect_sensorRotationIs90AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 90,
            effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor())
        )
    }

    @Test
    fun enableEffect_sensorRotationIs180AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 180,
            effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor())
        )
    }

    @Test
    fun enableEffect_sensorRotationIs270AndSetTargetRotation_sendCorrectResolution() {
        testSetRotationWillSendCorrectResolution(
            sensorRotation = 270,
            effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor())
        )
    }

    private fun testSetRotationWillSendCorrectResolution(
        sensorRotation: Int = 0,
        effect: SurfaceEffectInternal? = null
    ) {
        setupCamera(sensorRotation = sensorRotation)
        createCameraUseCaseAdapter()

        listOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        ).forEach { targetRotation ->
            // Arrange.
            var surfaceRequest: SurfaceRequest? = null
            val videoOutput = createVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.from(Quality.HD))
                }.build(),
                surfaceRequestListener = { request, _ ->
                    surfaceRequest = request
                })
            val videoCapture = createVideoCapture(videoOutput)
            effect?.let { videoCapture.setEffect(it) }
            videoCapture.targetRotation = targetRotation

            // Act.
            addAndAttachUseCases(videoCapture)

            // Assert.
            val expectedResolution = if (effect != null) {
                rotateSize(RESOLUTION_720P, cameraInfo.getSensorRotationDegrees(targetRotation))
            } else {
                RESOLUTION_720P
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
    fun addUseCasesWithSurfaceEffect_cameraIsUptime_requestIsUptime() {
        testTimebase(
            effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor()),
            cameraTimebase = Timebase.UPTIME,
            expectedTimebase = Timebase.UPTIME
        )
    }

    @Test
    fun addUseCasesWithSurfaceEffect_cameraIsRealtime_requestIsRealtime() {
        testTimebase(
            effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor()),
            cameraTimebase = Timebase.REALTIME,
            expectedTimebase = Timebase.REALTIME
        )
    }

    private fun testTimebase(
        effect: SurfaceEffectInternal? = null,
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
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()
        effect?.let { videoCapture.setEffect(it) }

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
        val qualityList = arrayOf(
            Quality.UHD to RESOLUTION_2160P,
            Quality.HD to RESOLUTION_720P,
            Quality.HIGHEST to RESOLUTION_2160P,
            Quality.LOWEST to RESOLUTION_720P,
        )
        qualityList.forEach { (quality, resolution) ->
            surfaceManager.setSuggestedResolution(
                CAMERA_ID_0,
                VideoCaptureConfig::class.java,
                resolution
            )

            val videoOutput = createVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.from(quality))
                }.build()
            )
            val videoCapture = createVideoCapture(videoOutput)

            // Act.
            addAndAttachUseCases(videoCapture)

            // Assert.
            assertThat(videoCapture.attachedSurfaceResolution).isEqualTo(resolution)

            // Clean up.
            detachAndRemoveUseCases(videoCapture)
        }
    }

    @Test
    fun setQualitySelector_limitedBySurfaceManager_findHighestPriorityQuality() {
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
        surfaceManager.setSuggestedResolution(
            CAMERA_ID_0,
            VideoCaptureConfig::class.java,
            RESOLUTION_1080P // the suggested resolution
        )

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(
                    QualitySelector.fromOrderedList(
                        listOf(
                            Quality.UHD, // 2160P
                            Quality.SD, // 480P
                            Quality.HD, // 720P
                            Quality.FHD // 1080P
                        )
                    )
                )
            }.build()
        )
        val videoCapture = createVideoCapture(videoOutput)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertSupportedResolutions(
            videoCapture, RESOLUTION_2160P, RESOLUTION_480P
            // RESOLUTION_720P, RESOLUTION_1080P is filtered out
        )
        assertThat(videoCapture.attachedSurfaceResolution).isEqualTo(RESOLUTION_480P)
    }

    @Test
    fun setQualitySelector_notSupportedQuality_throwException() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()

        // Camera 0 support 2160P(UHD) and 720P(HD)
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.from(Quality.FHD))
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
                it.setQualitySelector(QualitySelector.from(Quality.UHD))
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
                CameraXExecutors.directExecutor()
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
                    CameraXExecutors.directExecutor(),
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
                    CameraXExecutors.directExecutor()
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
    fun filterOutResolutions() {
        // Arrange.
        val inputs = listOf(
            listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P), // 0
            listOf(RESOLUTION_2160P, RESOLUTION_720P, RESOLUTION_1080P), // 1
            listOf(RESOLUTION_1080P, RESOLUTION_2160P, RESOLUTION_720P), // 2
            listOf(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_2160P), // 3
            listOf(RESOLUTION_720P, RESOLUTION_2160P, RESOLUTION_1080P), // 4
            listOf(RESOLUTION_720P, RESOLUTION_1080P, RESOLUTION_2160P), // 5
            listOf(RESOLUTION_1080P, RESOLUTION_1080P, RESOLUTION_720P), // 6 contain duplicate
        )

        val expected = listOf(
            listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P), // 0
            listOf(RESOLUTION_2160P, RESOLUTION_720P), // 1
            listOf(RESOLUTION_1080P, RESOLUTION_720P), // 2
            listOf(RESOLUTION_1080P, RESOLUTION_720P), // 3
            listOf(RESOLUTION_720P), // 4
            listOf(RESOLUTION_720P), // 5
            listOf(RESOLUTION_1080P, RESOLUTION_720P), // 6
        )

        inputs.zip(expected).forEachIndexed { index, (input, exp) ->
            // Act.
            val result = VideoCapture.filterOutResolutions(input)

            // Assert.
            assertWithMessage("filterOutResolutions fails on index: $index")
                .that(result)
                .isEqualTo(exp)
        }
    }

    @Test
    fun bindAndUnbind_surfacesPropagated() {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        val effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor(), false)
        var appSurfaceReadyToRelease = false
        val videoOutput = createVideoOutput(surfaceRequestListener = { surfaceRequest, _ ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                CameraXExecutors.mainThreadExecutor()
            ) {
                appSurfaceReadyToRelease = true
            }
        })
        val videoCapture = createVideoCapture(videoOutput)

        // Act: bind and provide Surface.
        videoCapture.setEffect(effect)
        addAndAttachUseCases(videoCapture)

        // Assert: surfaceOutput received.
        assertThat(effect.surfaceOutput).isNotNull()
        assertThat(effect.isReleased).isFalse()
        assertThat(effect.isOutputSurfaceRequestedToClose).isFalse()
        assertThat(effect.isInputSurfaceReleased).isFalse()
        assertThat(appSurfaceReadyToRelease).isFalse()
        // effect surface is provided to camera.
        assertThat(videoCapture.sessionConfig.surfaces[0].surface.get())
            .isEqualTo(effect.inputSurface)

        // Act: unbind.
        detachAndRemoveUseCases(videoCapture)

        // Assert: effect and effect surface is released.
        assertThat(effect.isReleased).isTrue()
        assertThat(effect.isOutputSurfaceRequestedToClose).isTrue()
        assertThat(effect.isInputSurfaceReleased).isTrue()
        assertThat(appSurfaceReadyToRelease).isFalse()

        // Act: close SurfaceOutput
        effect.surfaceOutput!!.close()
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
                supportedWidths = Range(80, 800),
                supportedHeights = Range(100, 800),
            ),
            cropRect = Rect(8, 8, 48, 48), // 40x40
            expectedCropRect = Rect(0, 0, 80, 100),
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
        quality: Quality = Quality.HD, // Quality.HD maps to 1280x720 (4:3)
        videoEncoderInfo: VideoEncoderInfo = createVideoEncoderInfo(),
        cropRect: Rect,
        expectedCropRect: Rect,
    ) {
        // Arrange.
        setupCamera()
        createCameraUseCaseAdapter()
        var surfaceRequest: SurfaceRequest? = null
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.from(quality))
            }.build(),
            surfaceRequestListener = { request, _ -> surfaceRequest = request }
        )
        val videoCapture = createVideoCapture(
            videoOutput, videoEncoderInfoFinder = { videoEncoderInfo }
        )
        val effect = FakeSurfaceEffectInternal(CameraXExecutors.mainThreadExecutor())
        videoCapture.setEffect(effect)
        videoCapture.setViewPortCropRect(cropRect)

        // Act.
        addAndAttachUseCases(videoCapture)

        // Assert.
        assertThat(surfaceRequest).isNotNull()
        assertThat(surfaceRequest!!.resolution).isEqualTo(rectToSize(expectedCropRect))
        assertThat(videoCapture.cameraSettableSurface.cropRect).isEqualTo(expectedCropRect)
    }

    private fun assertSupportedResolutions(
        videoCapture: VideoCapture<out VideoOutput>,
        vararg expectedResolutions: Size
    ) {
        val supportedResolutionPairs = videoCapture.currentConfig.retrieveOption(
            ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS
        )
        supportedResolutionPairs!!.first { it.first == videoCapture.imageFormat }.second.let {
            assertThat(it).isEqualTo(expectedResolutions)
        }
    }

    private fun createVideoEncoderInfo(
        widthAlignment: Int = 2,
        heightAlignment: Int = 2,
        supportedWidths: Range<Int> = Range.create(0, Integer.MAX_VALUE),
        supportedHeights: Range<Int> = Range.create(0, Integer.MAX_VALUE),
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
        targetRotation: Int? = null,
        targetResolution: Size? = null,
        videoEncoderInfoFinder: Function<VideoEncoderConfig, VideoEncoderInfo>? = null,
    ): VideoCapture<VideoOutput> = VideoCapture.Builder(videoOutput)
        .setSessionOptionUnpacker { _, _ -> }
        .apply {
            targetRotation?.let { setTargetRotation(it) }
            targetResolution?.let { setTargetResolution(it) }
            videoEncoderInfoFinder?.let { setVideoEncoderInfoFinder(it) }
        }.build()

    private fun setupCamera(
        cameraId: String = CAMERA_ID_0,
        sensorRotation: Int = 0,
        vararg profiles: CamcorderProfileProxy = CAMERA_0_PROFILES,
        timebase: Timebase = Timebase.UPTIME,
    ) {
        cameraInfo = FakeCameraInfoInternal(cameraId, sensorRotation, LENS_FACING_BACK).apply {
            camcorderProfileProvider =
                FakeCamcorderProfileProvider.Builder().addProfile(*profiles).build()
            setTimebase(timebase)
        }
        val camera = FakeCamera(cameraId, null, cameraInfo)

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
}
