/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core

import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.processing.SurfaceEffectInternal
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.ExecutionException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val TEST_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA

/**
 * Unit tests for [Preview].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP
)
class PreviewTest {
    var cameraUseCaseAdapter: CameraUseCaseAdapter? = null

    private lateinit var appSurface: Surface
    private lateinit var appSurfaceTexture: SurfaceTexture
    private lateinit var effectSurface: Surface
    private lateinit var effectSurfaceTexture: SurfaceTexture

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        appSurfaceTexture = SurfaceTexture(0)
        appSurface = Surface(appSurfaceTexture)
        effectSurfaceTexture = SurfaceTexture(0)
        effectSurface = Surface(effectSurfaceTexture)
        val camera = FakeCamera()

        val cameraFactoryProvider =
            CameraFactory.Provider { _: Context?, _: CameraThreadConfig?, _: CameraSelector? ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(
                    camera.cameraInfoInternal.cameraId
                ) { camera }
                cameraFactory
            }
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).setCameraFactoryProvider(cameraFactoryProvider).build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        CameraXUtil.initialize(context, cameraXConfig).get()
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        appSurfaceTexture.release()
        appSurface.release()
        effectSurfaceTexture.release()
        effectSurface.release()
        with(cameraUseCaseAdapter) {
            this?.removeUseCases(useCases)
        }
        cameraUseCaseAdapter = null
        CameraXUtil.shutdown().get()
    }

    @Test
    fun viewPortSet_cropRectIsBasedOnViewPort() {
        val transformationInfo = bindToLifecycleAndGetTransformationInfo(
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )
        // The expected value is based on fitting the 1:1 view port into a rect with the size of
        // FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.
        val expectedPadding = (
            FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width -
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            ) / 2
        assertThat(transformationInfo.cropRect).isEqualTo(
            Rect(
                expectedPadding,
                0,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width - expectedPadding,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun viewPortNotSet_cropRectIsFullSurface() {
        val transformationInfo = bindToLifecycleAndGetTransformationInfo(
            null
        )

        assertThat(transformationInfo.cropRect).isEqualTo(
            Rect(
                0,
                0,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun surfaceRequestSize_isSurfaceSize() {
        assertThat(bindToLifecycleAndGetSurfaceRequest().resolution).isEqualTo(
            Size(
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width,
                FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height
            )
        )
    }

    @Test
    fun setTargetRotation_rotationIsChanged() {
        // Arrange.
        val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_0).build()

        // Act: set target rotation.
        preview.targetRotation = Surface.ROTATION_180

        // Assert: target rotation is updated.
        assertThat(preview.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun attachUseCase_transformationInfoUpdates() {
        // Arrange: attach Preview without a SurfaceProvider.
        // Build and bind use case.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(), TEST_CAMERA_SELECTOR
        )
        val rational1 = Rational(1, 1)
        cameraUseCaseAdapter!!.setViewPort(ViewPort.Builder(rational1, Surface.ROTATION_0).build())
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))

        // Set SurfaceProvider
        var receivedTransformationInfo: TransformationInfo? = null
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
        }
        shadowOf(getMainLooper()).idle()
        assertThat(receivedTransformationInfo!!.cropRect.getAspectRatio()).isEqualTo(rational1)

        // Act: bind another use case with a different viewport.
        val fakeUseCase = FakeUseCase()
        val rational2 = Rational(2, 1)
        cameraUseCaseAdapter!!.setViewPort(ViewPort.Builder(rational2, Surface.ROTATION_0).build())
        cameraUseCaseAdapter!!.addUseCases(listOf(preview, fakeUseCase))
        shadowOf(getMainLooper()).idle()

        // Assert: received viewport's aspect ratio is the latest one.
        assertThat(receivedTransformationInfo!!.cropRect.getAspectRatio()).isEqualTo(rational2)
    }

    private fun Rect.getAspectRatio(): Rational {
        return Rational(width(), height())
    }

    @Test
    fun bindAndUnbindPreview_surfacesPropagated() {
        // Arrange.
        var surfaceOutputReceived: SurfaceOutput? = null
        var effectSurfaceReadyToRelease = false
        var isEffectReleased = false
        val surfaceEffect = object : SurfaceEffectInternal {
            override fun onInputSurface(request: SurfaceRequest) {
                request.provideSurface(effectSurface, mainThreadExecutor()) {
                    effectSurfaceReadyToRelease = true
                }
            }

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                surfaceOutputReceived = surfaceOutput
            }

            override fun release() {
                isEffectReleased = true
            }
        }

        // Act: bind Preview and provide Surface.
        val surfaceRequest = bindToLifecycleWithEffectAndGetSurfaceRequest(surfaceEffect)
        val preview = cameraUseCaseAdapter!!.useCases[0]
        var appSurfaceReadyToRelease = false
        surfaceRequest.provideSurface(appSurface, mainThreadExecutor()) {
            appSurfaceReadyToRelease = true
        }
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput received.
        assertThat(surfaceOutputReceived).isNotNull()
        var requestedToReleaseOutputSurface = false
        surfaceOutputReceived!!.getSurface(mainThreadExecutor()) {
            requestedToReleaseOutputSurface = true
        }
        assertThat(isEffectReleased).isFalse()
        assertThat(requestedToReleaseOutputSurface).isFalse()
        assertThat(effectSurfaceReadyToRelease).isFalse()
        assertThat(appSurfaceReadyToRelease).isFalse()
        // effect surface is provided to camera.
        assertThat(preview.sessionConfig.surfaces[0].surface.get()).isEqualTo(effectSurface)

        // Act: unbind Preview.
        cameraUseCaseAdapter!!.removeUseCases(Collections.singletonList(preview))
        shadowOf(getMainLooper()).idle()

        // Assert: effect and effect surface is released.
        assertThat(isEffectReleased).isTrue()
        assertThat(requestedToReleaseOutputSurface).isTrue()
        assertThat(effectSurfaceReadyToRelease).isTrue()
        assertThat(appSurfaceReadyToRelease).isFalse()

        // Act: close SurfaceOutput
        surfaceOutputReceived!!.close()
        shadowOf(getMainLooper()).idle()
        assertThat(appSurfaceReadyToRelease).isTrue()
    }

    @Test
    fun invokedErrorListener_recreatePipeline() {
        // Arrange: create pipeline and get a reference of the SessionConfig.
        val surfaceEffect = object : SurfaceEffectInternal {
            override fun onInputSurface(request: SurfaceRequest) {}

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                surfaceOutput.getSurface(mainThreadExecutor()) {
                    surfaceOutput.close()
                }
            }

            override fun release() {}
        }
        bindToLifecycleWithEffectAndGetSurfaceRequest(surfaceEffect)
        val preview = cameraUseCaseAdapter!!.useCases[0]
        val originalSessionConfig = preview.sessionConfig

        // Act: invoke the error listener.
        preview.sessionConfig.errorListeners[0].onError(
            preview.sessionConfig, SessionConfig.SessionError.SESSION_ERROR_UNKNOWN
        )
        shadowOf(getMainLooper()).idle()

        // Assert: the SessionConfig changed.
        assertThat(preview.sessionConfig).isNotEqualTo(originalSessionConfig)
    }

    @Test
    fun setTargetRotation_transformationInfoUpdated() {
        // Arrange: set up preview and verify target rotation in TransformationInfo.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(), TEST_CAMERA_SELECTOR
        )
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        var receivedTransformationInfo: TransformationInfo? = null
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
        }
        shadowOf(getMainLooper()).idle()
        assertThat(receivedTransformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_0)

        // Act: set target rotation to a different value.
        preview.targetRotation = Surface.ROTATION_180
        shadowOf(getMainLooper()).idle()

        // Assert: target rotation changed.
        assertThat(receivedTransformationInfo!!.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun setSurfaceProviderAfterAttachment_receivesSurfaceProviderCallbacks() {
        // Arrange: attach Preview without a SurfaceProvider.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        val cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider
                .getApplicationContext(),
            TEST_CAMERA_SELECTOR
        )
        cameraUseCaseAdapter.addUseCases(Collections.singleton<UseCase>(preview))

        // Get pending SurfaceRequest created by pipeline.
        val pendingSurfaceRequest = preview.mCurrentSurfaceRequest
        var receivedSurfaceRequest: SurfaceRequest? = null
        var receivedTransformationInfo: TransformationInfo? = null

        // Act: set a SurfaceProvider after attachment.
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
            receivedSurfaceRequest = request
        }
        shadowOf(getMainLooper()).idle()

        // Assert: received SurfaceRequest is the pending SurfaceRequest.
        assertThat(receivedSurfaceRequest).isSameInstanceAs(pendingSurfaceRequest)
        assertThat(receivedTransformationInfo).isNotNull()

        // Act: set a different SurfaceProvider.
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    receivedTransformationInfo = it
                }
            )
            receivedSurfaceRequest = request
        }
        shadowOf(getMainLooper()).idle()

        // Assert: received a different SurfaceRequest.
        assertThat(receivedSurfaceRequest).isNotSameInstanceAs(pendingSurfaceRequest)
    }

    @Test
    fun setSurfaceProviderAfterDetach_receivesSurfaceRequestAfterAttach() {
        // Arrange: attach Preview without a SurfaceProvider.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider
                .getApplicationContext(),
            TEST_CAMERA_SELECTOR
        )
        // Attach
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        // Detach
        cameraUseCaseAdapter!!.removeUseCases(Collections.singleton<UseCase>(preview))

        // Act: set a SurfaceProvider after detaching
        var receivedSurfaceRequest = false
        preview.setSurfaceProvider { receivedSurfaceRequest = true }
        shadowOf(getMainLooper()).idle()

        val receivedWhileDetached = receivedSurfaceRequest

        // Attach
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        shadowOf(getMainLooper()).idle()

        val receivedAfterAttach = receivedSurfaceRequest

        // Assert: received a SurfaceRequest.
        assertThat(receivedWhileDetached).isFalse()
        assertThat(receivedAfterAttach).isTrue()
    }

    private fun bindToLifecycleWithEffectAndGetSurfaceRequest(
        surfaceEffect: SurfaceEffectInternal?
    ): SurfaceRequest {
        return bindToLifecycleAndGetResult(null, surfaceEffect).first
    }

    private fun bindToLifecycleAndGetSurfaceRequest(): SurfaceRequest {
        return bindToLifecycleAndGetResult(null, null).first
    }

    private fun bindToLifecycleAndGetTransformationInfo(viewPort: ViewPort?): TransformationInfo {
        return bindToLifecycleAndGetResult(viewPort, null).second
    }

    private fun bindToLifecycleAndGetResult(
        viewPort: ViewPort?,
        surfaceEffect: SurfaceEffectInternal?
    ): Pair<SurfaceRequest,
        TransformationInfo> {
        // Arrange.
        val sessionOptionUnpacker =
            { _: UseCaseConfig<*>?, _: SessionConfig.Builder? -> }
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setSessionOptionUnpacker(sessionOptionUnpacker)
            .build()
        var surfaceRequest: SurfaceRequest? = null
        var transformationInfo: TransformationInfo? = null
        preview.setSurfaceProvider { request ->
            request.setTransformationInfoListener(
                CameraXExecutors.directExecutor(),
                SurfaceRequest.TransformationInfoListener {
                    transformationInfo = it
                }
            )
            surfaceRequest = request
        }

        preview.setEffect(surfaceEffect)

        // Act.
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            ApplicationProvider.getApplicationContext(), TEST_CAMERA_SELECTOR
        )
        cameraUseCaseAdapter!!.setViewPort(viewPort)
        cameraUseCaseAdapter!!.addUseCases(Collections.singleton<UseCase>(preview))
        shadowOf(getMainLooper()).idle()
        return Pair(surfaceRequest!!, transformationInfo!!)
    }
}