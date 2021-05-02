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
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.CamcorderProfileUtil.RESOLUTION_2160P
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.video.QualitySelector.QUALITY_FHD
import androidx.camera.video.QualitySelector.QUALITY_UHD
import androidx.camera.video.VideoOutput.StreamState
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val ANY_SIZE = Size(640, 480)
private const val CAMERA_ID_0 = "0"
private const val CAMERA_ID_1 = "1"
private val CAMERA_0_PROFILE_HIGH = CamcorderProfileUtil.asHighQuality(PROFILE_2160P)
private val CAMERA_0_PROFILE_LOW = CamcorderProfileUtil.asLowQuality(PROFILE_720P)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoCaptureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraUseCaseAdapter0: CameraUseCaseAdapter
    private lateinit var cameraUseCaseAdapter1: CameraUseCaseAdapter

    @Before
    fun setUp() {
        // Prepare camera0
        // Camera 0 support 2160P(UHD) and 720P(HD)
        val cameraInfo0 = FakeCameraInfoInternal(CAMERA_ID_0).apply {
            camcorderProfileProvider = FakeCamcorderProfileProvider.Builder()
                .addProfile(CAMERA_0_PROFILE_HIGH)
                .addProfile(PROFILE_2160P)
                .addProfile(PROFILE_720P)
                .addProfile(CAMERA_0_PROFILE_LOW)
                .build()
        }
        val camera0 = FakeCamera(CAMERA_ID_0, null, cameraInfo0)

        // Prepare camera 1
        // camera1 has no supported quality
        val cameraInfo1 = FakeCameraInfoInternal(CAMERA_ID_1, 0, CameraSelector.LENS_FACING_FRONT)
        val camera1 = FakeCamera(CAMERA_ID_1, null, cameraInfo1)

        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _ ->
                FakeCameraFactory().apply {
                    insertDefaultBackCamera(CAMERA_ID_0) { camera0 }
                    insertDefaultFrontCamera(CAMERA_ID_1) { camera1 }
                }
            }

        val cameraXConfig = CameraXConfig.Builder.fromConfig(FakeAppConfig.create())
            .setCameraFactoryProvider(cameraFactoryProvider)
            .build()
        CameraX.initialize(context, cameraXConfig).get()

        cameraUseCaseAdapter0 =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_BACK_CAMERA)
        cameraUseCaseAdapter1 =
            CameraUtil.createCameraUseCaseAdapter(context, CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter0.isInitialized) {
            cameraUseCaseAdapter0.apply {
                removeUseCases(useCases)
            }
        }
        if (this::cameraUseCaseAdapter1.isInitialized) {
            cameraUseCaseAdapter1.apply {
                removeUseCases(useCases)
            }
        }
        CameraX.shutdown().get()
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
        var surfaceRequest: SurfaceRequest? = null
        val videoOutput = createVideoOutput(surfaceRequestListener = { surfaceRequest = it })
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))

        // Assert.
        assertThat(surfaceRequest).isNotNull()
    }

    @Test
    fun addUseCases_withNullMediaSpec_throwException() {
        // Arrange.
        val videoOutput = createVideoOutput(mediaSpec = null)
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Assert.
        assertThrows(CameraUseCaseAdapter.CameraException::class.java) {
            // Act.
            cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))
        }
    }

    @Test
    fun setQualitySelector_sameResolutionAsQualitySelector() {
        // Arrange.
        // Camera 0 support 2160P(UHD) and 720P(HD)
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.of(QUALITY_UHD))
            }.build()
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))

        // Assert.
        val targetResolution = videoCapture.currentConfig.retrieveOption(
            ImageOutputConfig.OPTION_TARGET_RESOLUTION
        )
        assertThat(targetResolution).isEqualTo(RESOLUTION_2160P)
    }

    @Test
    fun setQualitySelector_notSupportedQuality_throwException() {
        // Arrange.
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
            cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))
        }
    }

    @Test
    fun noSupportedQuality_useDefaultResolution() {
        // Arrange.
        val videoOutput = createVideoOutput(
            mediaSpec = MediaSpec.builder().configureVideo {
                it.setQualitySelector(QualitySelector.of(QUALITY_UHD))
            }.build()
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        // camera1 has no supported quality
        cameraUseCaseAdapter1.addUseCases(listOf(videoCapture))

        // Assert.
        val targetResolution = videoCapture.currentConfig.retrieveOption(
            ImageOutputConfig.OPTION_TARGET_RESOLUTION
        )
        assertThat(targetResolution).isEqualTo(VideoCapture.Defaults.DEFAULT_RESOLUTION)
    }

    @Test
    fun removeUseCases_receiveResultOfSurfaceRequest() {
        // Arrange.
        var surfaceResult: SurfaceRequest.Result? = null
        val videoOutput = createVideoOutput { surfaceRequest ->
            surfaceRequest.provideSurface(
                mock(Surface::class.java),
                CameraXExecutors.directExecutor(),
                { surfaceResult = it }
            )
        }
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))

        // Assert.
        // Surface is in use, should not receive any result.
        assertThat(surfaceResult).isNull()

        // Act.
        cameraUseCaseAdapter0.removeUseCases(listOf(videoCapture))

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
        cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))

        // Assert.
        verify(listener).onTransformationInfoUpdate(any())
    }

    @Test
    fun setTargetRotation_transformationInfoUpdated() {
        // Arrange.
        var transformationInfo: SurfaceRequest.TransformationInfo? = null
        val videoOutput = createVideoOutput(
            surfaceRequestListener = { surfaceRequest ->
                surfaceRequest.setTransformationInfoListener(
                    CameraXExecutors.directExecutor(),
                    {
                        transformationInfo = it
                    }
                )
            }
        )
        val videoCapture = VideoCapture.Builder(videoOutput)
            .setTargetRotation(Surface.ROTATION_90)
            .setSessionOptionUnpacker { _, _ -> }
            .build()

        // Act.
        cameraUseCaseAdapter0.addUseCases(listOf(videoCapture))

        // Assert.
        assertThat(transformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_90)

        // Act.
        videoCapture.targetRotation = Surface.ROTATION_180

        // Assert.
        assertThat(transformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_180)
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
}
