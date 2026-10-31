/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.CompositionSettings
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.AdapterCameraInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.core.streamsharing.VirtualCameraInfo
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Verifies [preAppliedSourceRotation] and [toViewfinderTransformationInfo] against the rotation
 * calculated by CameraX itself across stream-sharing, effect, and combined pipelines.
 */
@SuppressLint("RestrictedApiAndroidX")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument // Needed for Robolectric to correctly instrument classes in the same module
class PreAppliedRotationConsistencyTest {

    private enum class Pipeline {
        /** The Preview is a child of stream sharing, which copies the camera stream. */
        STREAM_SHARING,
        /** The stream is copied by a CameraEffect applied to the Preview. */
        EFFECT,
        /** Stream sharing copies the camera stream and a child CameraEffect copies it again. */
        STREAM_SHARING_AND_EFFECT,
    }

    private data class CameraConfig(val lensFacing: Int, val sensorRotation: Int)

    private val rotations =
        listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)

    private lateinit var context: Context
    private lateinit var useCaseConfigFactory: UseCaseConfigFactory
    private val processors = mutableListOf<FakeSurfaceProcessorInternal>()
    private val unbinders = mutableListOf<() -> Unit>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val cameraXConfig = FakeAppConfig.create()
        CameraXUtil.initialize(context, cameraXConfig).get()
        useCaseConfigFactory =
            cameraXConfig.getUseCaseConfigFactoryProvider(null)!!.newInstance(context, false)
        DefaultSurfaceProcessor.Factory.setSupplier {
            FakeSurfaceProcessorInternal(mainThreadExecutor()).also { processors.add(it) }
        }
    }

    @After
    fun tearDown() {
        unbinders.forEach { it() }
        processors.forEach {
            it.release()
            it.cleanUp()
        }
        shadowOf(getMainLooper()).idle()
        CameraXUtil.shutdown().get()
    }

    @Test fun streamSharing_correctionMatchesCameraX() = verifyConsistency(Pipeline.STREAM_SHARING)

    @Test fun effect_correctionMatchesCameraX() = verifyConsistency(Pipeline.EFFECT)

    @Test
    fun streamSharingAndEffect_correctionMatchesCameraX() =
        verifyConsistency(Pipeline.STREAM_SHARING_AND_EFFECT)

    @Test
    fun streamSharing_withoutAppTargetRotation_correctionMatchesCameraX() =
        verifyConsistency(Pipeline.STREAM_SHARING, appSetsTargetRotation = false)

    @Test
    fun streamSharing_physicalCameraId_usesPhysicalSensorRotationAndMatchesCameraX() {
        val adapterCameraInfo = createPhysicalAdapterCameraInfo()
        val adaptedCamera =
            AdapterCameraInternal(
                FakeCamera("0", null, adapterCameraInfo.implementation),
                adapterCameraInfo,
            )

        for (initialRotation in rotations) {
            val (virtualCameraInfo, preview, _, latestInfo) =
                bindPreview(
                    Pipeline.STREAM_SHARING,
                    adaptedCamera,
                    MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
                    initialRotation,
                )
            assertWithMessage("StreamSharing wraps AdapterCameraInfo in VirtualCameraInfo")
                .that(virtualCameraInfo)
                .isInstanceOf(VirtualCameraInfo::class.java)
            val initialSnapshot = latestInfo()

            for (displayRotation in rotations) {
                preview.targetRotation = displayRotation
                shadowOf(getMainLooper()).idle()

                val derived =
                    preAppliedSourceRotation(
                        sensorToBufferTransform = initialSnapshot.sensorToBufferTransform,
                        cameraInfo = virtualCameraInfo,
                        displayRotation = displayRotation,
                    )
                assertWithMessage("initial=$initialRotation, display=$displayRotation")
                    .that(derived)
                    .isEqualTo(latestInfo().rotationDegrees)
            }
        }
    }

    /**
     * Binds the preview once per camera and mirror mode, then steps its target rotation through all
     * rotations, checking that the source rotation derived from the TransformationInfo of the
     * displayed buffers matches the rotation that CameraX reports for the new target rotation.
     *
     * @param appSetsTargetRotation Whether the app sets the initial target rotation of the
     *   `Preview`. If not, the `Preview` uses the default [Surface.ROTATION_0], and CameraX doesn't
     *   report a target rotation.
     */
    private fun verifyConsistency(pipeline: Pipeline, appSetsTargetRotation: Boolean = true) {
        val configs =
            listOf(
                CameraConfig(CameraSelector.LENS_FACING_BACK, 90),
                CameraConfig(CameraSelector.LENS_FACING_FRONT, 270),
            )
        val mirrorModes = listOf(MirrorMode.MIRROR_MODE_OFF, MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        for ((lensFacing, sensorRotation) in configs) {
            for (mirrorMode in mirrorModes) {
                val camera =
                    FakeCamera("0", null, FakeCameraInfoInternal(sensorRotation, lensFacing))
                val initialRotation = if (appSetsTargetRotation) Surface.ROTATION_0 else null
                val (cameraInfo, preview, latestRequest, latestInfo) =
                    bindPreview(pipeline, camera, mirrorMode, initialRotation)
                var prevRequest = latestRequest()
                var prevInfo = latestInfo()
                if (!appSetsTargetRotation) {
                    assertWithMessage("targetRotation")
                        .that(prevInfo.targetRotation)
                        .isEqualTo(ImageOutputConfig.ROTATION_NOT_SPECIFIED)
                }
                assertWithMessage("hasCameraTransform")
                    .that(prevInfo.hasCameraTransform())
                    .isFalse()

                for (newRotation in rotations) {
                    preview.targetRotation = newRotation
                    shadowOf(getMainLooper()).idle()
                    val expected = latestInfo()
                    // When surface processing (e.g. an effect) applies the rotation, CameraX
                    // recreates the output for the new target rotation, and the new request
                    // describes its own buffers. Otherwise, the displayed buffers are unchanged.
                    val bufferInfo = if (latestRequest() === prevRequest) prevInfo else expected
                    val derived =
                        bufferInfo
                            .toViewfinderTransformationInfo(
                                cameraInfo,
                                newRotation,
                                bufferInfo.sensorToBufferTransform,
                            )
                            .sourceRotation
                    assertWithMessage(
                            "$pipeline lens=$lensFacing sensor=$sensorRotation " +
                                "mirror=$mirrorMode new=$newRotation"
                        )
                        .that(derived)
                        .isEqualTo(expected.rotationDegrees)
                    prevRequest = latestRequest()
                    prevInfo = expected
                }
            }
        }
    }

    private data class BoundPreview(
        /** The camera info of the camera producing the stream, as reported by the request. */
        val cameraInfo: CameraInfoInternal,
        val preview: Preview,
        val latestRequest: () -> SurfaceRequest,
        val latestInfo: () -> SurfaceRequest.TransformationInfo,
    )

    private fun bindPreview(
        pipeline: Pipeline,
        camera: CameraInternal,
        mirrorMode: Int,
        targetRotation: Int?,
    ): BoundPreview {
        val preview =
            Preview.Builder()
                .setMirrorMode(mirrorMode)
                .apply { targetRotation?.let { setTargetRotation(it) } }
                .build()
        var latestInfo: SurfaceRequest.TransformationInfo? = null
        var latestRequest: SurfaceRequest? = null
        var requestCameraInfo: CameraInfoInternal? = null
        preview.setSurfaceProvider(directExecutor()) { request ->
            latestRequest = request
            requestCameraInfo = request.camera.cameraInfoInternal
            request.setTransformationInfoListener(directExecutor()) { latestInfo = it }
        }
        val sensorToBufferTransform = Matrix().apply { setScale(0.5f, 0.5f) }
        val streamSpec = StreamSpec.builder(Size(640, 480)).build()

        when (pipeline) {
            Pipeline.STREAM_SHARING,
            Pipeline.STREAM_SHARING_AND_EFFECT -> {
                if (pipeline == Pipeline.STREAM_SHARING_AND_EFFECT) {
                    val processor = FakeSurfaceProcessorInternal(mainThreadExecutor())
                    processors.add(processor)
                    preview.effect = FakeSurfaceEffect(processor)
                }
                val sibling =
                    FakeUseCase(
                        FakeUseCaseConfig.Builder().setSurfaceOccupancyPriority(1).useCaseConfig
                    )
                val fakeUseCaseConfigFactory = FakeUseCaseConfigFactory()
                val streamSharing =
                    StreamSharing(
                        camera,
                        null,
                        CompositionSettings.DEFAULT,
                        CompositionSettings.DEFAULT,
                        setOf(preview, sibling),
                        emptyList(),
                        fakeUseCaseConfigFactory,
                    )
                streamSharing.bindToCamera(
                    camera,
                    null,
                    null,
                    streamSharing.getDefaultConfig(true, fakeUseCaseConfigFactory),
                )
                streamSharing.sensorToBufferTransformMatrix = sensorToBufferTransform
                streamSharing.updateSuggestedStreamSpec(streamSpec, null)
                unbinders.add { streamSharing.unbindFromCamera(camera) }
            }
            Pipeline.EFFECT -> {
                val processor = FakeSurfaceProcessorInternal(mainThreadExecutor())
                processors.add(processor)
                preview.effect = FakeSurfaceEffect(processor)
                preview.bindToCamera(
                    camera,
                    null,
                    null,
                    preview.getDefaultConfig(true, useCaseConfigFactory),
                )
                preview.sensorToBufferTransformMatrix = sensorToBufferTransform
                preview.updateSuggestedStreamSpec(streamSpec, null)
                unbinders.add { preview.unbindFromCamera(camera) }
            }
        }
        shadowOf(getMainLooper()).idle()
        return BoundPreview(
            checkNotNull(requestCameraInfo),
            preview,
            { checkNotNull(latestRequest) },
            { checkNotNull(latestInfo) },
        )
    }
}
