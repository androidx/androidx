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
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CamcorderProfileProxy
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
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
import androidx.camera.video.QualitySelector.QUALITY_FHD
import androidx.camera.video.QualitySelector.QUALITY_HD
import androidx.camera.video.QualitySelector.QUALITY_HIGHEST
import androidx.camera.video.QualitySelector.QUALITY_LOWEST
import androidx.camera.video.QualitySelector.QUALITY_SD
import androidx.camera.video.QualitySelector.QUALITY_UHD
import androidx.camera.video.VideoOutput.StreamState
import androidx.camera.video.impl.VideoCaptureConfig
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.TimeUnit

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
    private lateinit var surfaceManager: FakeCameraDeviceSurfaceManager

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            cameraUseCaseAdapter.apply {
                removeUseCases(useCases)
            }
        }
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun setTargetResolution_throwsException() {
        val videoOutput = createVideoOutput()

        assertThrows(UnsupportedOperationException::class.java) {
            VideoCapture.Builder(videoOutput)
                .setTargetResolution(ANY_SIZE)
                .build()
        }
    }

    @Test
    fun canGetVideoOutput() {
        // Arrange.
        val videoOutput = createVideoOutput()

        // Act.
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Assert.
        assertThat(videoCapture.output).isEqualTo(videoOutput)
    }

    @Test
    fun addUseCases_receiveOnSurfaceRequest() {
        // Arrange.
        setupCamera()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)

        var surfaceRequest: SurfaceRequest? = null
        val videoOutput = createVideoOutput(surfaceRequestListener = { surfaceRequest = it })
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

        // Assert.
        assertThat(surfaceRequest).isNotNull()
    }

    @Test
    fun addUseCases_withNullMediaSpec_throwException() {
        // Arrange.
        setupCamera()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)

        val videoOutput = createVideoOutput(mediaSpec = null)
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Assert.
        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            // Act.
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }
    }

    @Test
    fun setQualitySelector_sameResolutionAsQualitySelector() {
        // Arrange.
        setupCamera()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)

        // Camera 0 support 2160P(UHD) and 720P(HD)
        val qualityList = arrayOf(
            QUALITY_UHD to RESOLUTION_2160P,
            QUALITY_HD to RESOLUTION_720P,
            QUALITY_HIGHEST to RESOLUTION_2160P,
            QUALITY_LOWEST to RESOLUTION_720P,
        )
        qualityList.forEach { (quality, resolution) ->
            surfaceManager.setSuggestedResolution(
                CAMERA_ID_0,
                VideoCaptureConfig::class.java,
                resolution
            )

            val videoOutput = createVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.of(quality))
                }.build()
            )
            val videoCapture = VideoCapture.Builder(videoOutput)
                .setSessionOptionUnpacker { _, _ -> }
                .build()

            // Act.
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

            // Assert.
            assertThat(videoCapture.attachedSurfaceResolution).isEqualTo(resolution)
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
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)
        surfaceManager.setSuggestedResolution(
            CAMERA_ID_0,
            VideoCaptureConfig::class.java,
            RESOLUTION_1080P // the suggested resolution
        )

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(
                    QualitySelector.firstTry(QUALITY_UHD) // 2160P
                        .thenTry(QUALITY_SD) // 480P
                        .thenTry(QUALITY_HD) // 720P
                        .finallyTry(QUALITY_FHD) // 1080P
                )
            }.build()
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

        // Assert.
        assertSupportedResolutions(
            videoCapture, RESOLUTION_2160P, RESOLUTION_480P, RESOLUTION_720P, RESOLUTION_1080P
        )
        assertThat(videoCapture.attachedSurfaceResolution).isEqualTo(RESOLUTION_480P)
    }

    @Test
    fun setQualitySelector_notSupportedQuality_throwException() {
        // Arrange.
        setupCamera()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)

        // Camera 0 support 2160P(UHD) and 720P(HD)
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.of(QUALITY_FHD))
            }.build()
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Assert.
        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            // Act.
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }
    }

    @Test
    fun noSupportedQuality_supportedResolutionsIsNotSet() {
        // Arrange.
        setupCamera(profiles = emptyArray())
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)

        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.of(QUALITY_UHD))
            }.build()
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

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
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)

        var surfaceResult: SurfaceRequest.Result? = null
        val videoOutput = createVideoOutput { surfaceRequest ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                CameraXExecutors.directExecutor()
            ) { surfaceResult = it }
        }
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

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
        val videoCapture = VideoCapture.withOutput(createVideoOutput())

        // Act.
        videoCapture.targetRotation = Surface.ROTATION_180

        // Assert.
        assertThat(videoCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun addUseCases_transformationInfoUpdated() {
        // Arrange.
        setupCamera()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)
        val listener = mock(SurfaceRequest.TransformationInfoListener::class.java)
        val videoOutput = createVideoOutput(
            surfaceRequestListener = {
                it.setTransformationInfoListener(
                    CameraXExecutors.directExecutor(),
                    listener
                )
            }
        )

        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

        // Assert.
        verify(listener).onTransformationInfoUpdate(any())
    }

    @Test
    fun setTargetRotation_transformationInfoUpdated() {
        // Arrange.
        setupCamera()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)
        var transformationInfo: SurfaceRequest.TransformationInfo? = null
        val videoOutput = createVideoOutput(
            surfaceRequestListener = { surfaceRequest ->
                surfaceRequest.setTransformationInfoListener(
                    CameraXExecutors.directExecutor()
                ) {
                    transformationInfo = it
                }
            }
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setTargetRotation(Surface.ROTATION_90)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter.addUseCases(listOf(videoCapture))

        // Assert.
        assertThat(transformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_90)

        // Act.
        videoCapture.targetRotation = Surface.ROTATION_180

        // Assert.
        assertThat(transformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_180)
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

    private fun createVideoOutput(
        streamState: StreamState = StreamState.ACTIVE,
        mediaSpec: MediaSpec? = MediaSpec.builder().build(),
        surfaceRequestListener: Consumer<SurfaceRequest> = Consumer { it.willNotProvideSurface() }
    ): TestVideoOutput = TestVideoOutput(streamState, mediaSpec, surfaceRequestListener)

    private class TestVideoOutput constructor(
        streamState: StreamState,
        mediaSpec: MediaSpec?,
        val surfaceRequestCallback: Consumer<SurfaceRequest>
    ) : VideoOutput {

        private val streamStateObservable: MutableStateObservable<StreamState> =
            MutableStateObservable.withInitialState(streamState)

        private val mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(mediaSpec)

        override fun onSurfaceRequested(surfaceRequest: SurfaceRequest) {
            surfaceRequestCallback.accept(surfaceRequest)
        }

        override fun getStreamState(): Observable<StreamState> = streamStateObservable

        override fun getMediaSpec(): Observable<MediaSpec> = mediaSpecObservable

        fun setStreamState(streamState: StreamState) = streamStateObservable.setState(streamState)

        fun setMediaSpec(mediaSpec: MediaSpec) = mediaSpecObservable.setState(mediaSpec)
    }

    private fun setupCamera(
        cameraId: String = CAMERA_ID_0,
        vararg profiles: CamcorderProfileProxy = CAMERA_0_PROFILES
    ) {
        val cameraInfo = FakeCameraInfoInternal(cameraId).apply {
            camcorderProfileProvider =
                FakeCamcorderProfileProvider.Builder().addProfile(*profiles).build()
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
