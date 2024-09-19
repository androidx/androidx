/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.internal

import android.graphics.ImageFormat.JPEG
import android.graphics.ImageFormat.JPEG_R
import android.graphics.ImageFormat.RAW_SENSOR
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CompositionSettings
import androidx.camera.core.DynamicRange.HDR_UNSPECIFIED_10_BIT
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringAction.FLAG_AE
import androidx.camera.core.FocusMeteringAction.FLAG_AF
import androidx.camera.core.FocusMeteringAction.FLAG_AWB
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.core.ViewPort
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.RestrictedCameraControl
import androidx.camera.core.impl.RestrictedCameraInfo
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter.CameraException
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.camera.testing.impl.fakes.GrayscaleImageEffect
import androidx.concurrent.futures.await
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

private const val CAMERA_ID = "0"
private const val SECONDARY_CAMERA_ID = "1"

/** Unit tests for [CameraUseCaseAdapter]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = ["androidx.camera.core"]
)
class CameraUseCaseAdapterTest {
    private lateinit var effects: List<CameraEffect>
    private lateinit var executor: ExecutorService
    private lateinit var fakeCameraDeviceSurfaceManager: FakeCameraDeviceSurfaceManager
    private lateinit var fakeCamera: FakeCamera
    private lateinit var fakeSecondaryCamera: FakeCamera
    private lateinit var useCaseConfigFactory: UseCaseConfigFactory
    private lateinit var previewEffect: FakeSurfaceEffect
    private lateinit var videoEffect: FakeSurfaceEffect
    private lateinit var sharedEffect: FakeSurfaceEffect
    private lateinit var cameraCoordinator: CameraCoordinator
    private lateinit var surfaceProcessorInternal: FakeSurfaceProcessorInternal
    private lateinit var fakeCameraControl: FakeCameraControl
    private lateinit var fakeCameraInfo: FakeCameraInfoInternal
    private lateinit var adapter: CameraUseCaseAdapter
    private val imageEffect = GrayscaleImageEffect()
    private val preview = Preview.Builder().build()
    private val video = createFakeVideoCaptureUseCase()
    private val image = ImageCapture.Builder().build()
    private val analysis = ImageAnalysis.Builder().build()
    private val adaptersToDetach = mutableListOf<CameraUseCaseAdapter>()

    @Before
    fun setUp() {
        fakeCameraDeviceSurfaceManager = FakeCameraDeviceSurfaceManager()
        fakeCameraControl = FakeCameraControl()
        fakeCameraInfo = FakeCameraInfoInternal()
        fakeCamera = FakeCamera(CAMERA_ID, fakeCameraControl, fakeCameraInfo)
        fakeSecondaryCamera = FakeCamera(SECONDARY_CAMERA_ID, fakeCameraControl, fakeCameraInfo)
        cameraCoordinator = FakeCameraCoordinator()
        useCaseConfigFactory = FakeUseCaseConfigFactory()
        executor = Executors.newSingleThreadExecutor()
        surfaceProcessorInternal = FakeSurfaceProcessorInternal(mainThreadExecutor())
        previewEffect = FakeSurfaceEffect(PREVIEW, surfaceProcessorInternal)
        videoEffect = FakeSurfaceEffect(VIDEO_CAPTURE, surfaceProcessorInternal)
        sharedEffect = FakeSurfaceEffect(PREVIEW or VIDEO_CAPTURE, surfaceProcessorInternal)
        effects = listOf(previewEffect, imageEffect, videoEffect)
        adapter = createCameraUseCaseAdapter(fakeCamera)
        DefaultSurfaceProcessor.Factory.setSupplier { surfaceProcessorInternal }
    }

    @After
    fun tearDown() {
        surfaceProcessorInternal.cleanUp()
        executor.shutdown()
        for (adapter in adaptersToDetach) {
            adapter.updateUseCases(emptySet())
        }
    }

    @Test(expected = CameraException::class)
    fun attachTwoPreviews_streamSharingNotEnabled() {
        // Arrange: bind 2 previews with an ImageCapture. Request fails without enabling
        // StreamSharing because StreamSharing only allows one use case per type.
        val preview2 = Preview.Builder().build()
        adapter.addUseCases(setOf(preview, preview2, image))
    }

    @Test
    fun attachOneVideoCapture_streamSharingNotEnabled() {
        adapter.addUseCases(setOf(video, image))
        // Assert: StreamSharing is not bound.
        adapter.cameraUseCases.hasExactTypes(FakeUseCase::class.java, ImageCapture::class.java)
    }

    @Test(expected = CameraException::class)
    fun attachTwoVideoCaptures_streamSharingNotEnabled() {
        // Arrange: bind 2 videos with an ImageCapture. Request fails without enabling StreamSharing
        // because StreamSharing only allows one use case per type.
        val video2 = createFakeVideoCaptureUseCase()
        adapter.addUseCases(setOf(video, video2, image))
    }

    @Test
    fun attachAndDetachUseCases_cameraUseCasesAttachedAndDetached() {
        // Arrange: bind UseCases that requires sharing.
        adapter.addUseCases(setOf(preview, video, image))
        val streamSharing = adapter.getStreamSharing()
        // Act: attach use cases.
        adapter.attachUseCases()
        // Assert: StreamSharing and image are attached.
        assertThat(fakeCamera.attachedUseCases).containsExactly(image, streamSharing)
        // Act: detach.
        adapter.detachUseCases()
        // Assert: use cases are detached.
        assertThat(fakeCamera.attachedUseCases).isEmpty()
    }

    @Test
    fun addUseCases_cameraConfigIsConfigured() {
        // Arrange: Prepare two sets of CameraConfig and CameraUseCaseAdapter.
        val cameraConfig = FakeCameraConfig()
        val adapter = createCameraUseCaseAdapter(fakeCamera, cameraConfig)

        // Act: Add use cases.
        adapter.addUseCases(setOf(preview, video, image))

        // Assert:  CameraConfig is configured to the underlying CameraInternal.
        assertThat(fakeCamera.extendedConfig).isSameInstanceAs(cameraConfig)
    }

    @Test
    fun addUseCases_notUpdateExistingUseCasesIfOptionNotChanged() {
        // Arrange.
        adapter.addUseCases(setOf(preview))
        adapter.attachUseCases()

        // Act.
        adapter.addUseCases(setOf(video))

        // Assert.
        assertThat(fakeCamera.useCaseUpdateHistory).doesNotContain(preview)
    }

    @Test
    fun addUseCases_updateExistingUseCasesWhenOptionChanged() {
        // Arrange.
        adapter.addUseCases(setOf(preview))
        adapter.attachUseCases()

        // Intentionally modify the implementation options for preview so that an update of use case
        // will be triggered.
        val optionsBundle = MutableOptionsBundle.create()
        optionsBundle.insertOption(OPTION_TARGET_NAME, "fakeName")
        fakeCameraDeviceSurfaceManager.setSuggestedStreamSpec(
            CAMERA_ID,
            PreviewConfig::class.java,
            StreamSpec.builder(Size(1920, 1080))
                .setImplementationOptions(Camera2ImplConfig(optionsBundle))
                .build()
        )

        // Act.
        adapter.addUseCases(setOf(video))

        // Assert.
        assertThat(fakeCamera.useCaseUpdateHistory).containsExactly(preview)
    }

    @Test
    fun attachUseCases_cameraConfigIsConfigured() {
        // Arrange: Prepare two sets of CameraConfig and CameraUseCaseAdapter.
        val cameraConfig1 = FakeCameraConfig()
        val adapter1 = createCameraUseCaseAdapter(fakeCamera, cameraConfig1)
        val cameraConfig2 = FakeCameraConfig()
        val adapter2 = createCameraUseCaseAdapter(fakeCamera, cameraConfig2)
        val preview2 = Preview.Builder().build()
        val video2 = createFakeVideoCaptureUseCase()
        val image2 = ImageCapture.Builder().build()

        // Act:  bind adapter1 and attach to camera, bind adapter 2 and attach to camera, and then
        // switch back to adapter1 to attach to camera (without unbinding).
        adapter1.detachUseCases()
        adapter1.addUseCases(setOf(preview, video, image))
        adapter1.attachUseCases()

        adapter1.detachUseCases()
        adapter2.detachUseCases()
        adapter2.addUseCases(setOf(preview2, video2, image2))
        adapter2.attachUseCases()

        adapter2.detachUseCases()
        adapter1.attachUseCases()

        // Assert: CameraConfig1 is configured because adapter1 is active (attached to camera)
        assertThat(fakeCamera.extendedConfig).isSameInstanceAs(cameraConfig1)
    }

    @RequiresApi(33) // 10-bit HDR only supported on API 33+
    @Test
    fun canUseHdrWithoutExtensions() {
        // Act: add UseCase that uses HDR.
        val hdrUseCase = FakeUseCaseConfig.Builder().setDynamicRange(HDR_UNSPECIFIED_10_BIT).build()
        adapter.addUseCases(setOf(hdrUseCase))
        // Assert: UseCase is added.
        adapter.cameraUseCases.hasExactTypes(
            FakeUseCase::class.java,
        )
    }

    @RequiresApi(33) // 10-bit HDR only supported on API 33+
    @Test(expected = CameraException::class)
    fun useHDRWithExtensions_throwsException() {
        // Arrange: enable extensions.
        val adapter =
            createCameraUseCaseAdapter(
                fakeCamera,
                createCoexistingRequiredRuleCameraConfig(FakeSessionProcessor())
            )
        // Act: add UseCase that uses HDR.
        val hdrUseCase = FakeUseCaseConfig.Builder().setDynamicRange(HDR_UNSPECIFIED_10_BIT).build()
        adapter.addUseCases(setOf(hdrUseCase))
    }

    @RequiresApi(34) // Ultra HDR only supported on API 34+
    @Test(expected = CameraException::class)
    fun useUltraHdrWithExtensions_throwsException() {
        // Arrange: enable extensions.
        val extensionsConfig = createCoexistingRequiredRuleCameraConfig(FakeSessionProcessor())
        val cameraId = "fakeCameraId"
        val fakeManager = FakeCameraDeviceSurfaceManager()
        fakeManager.setValidSurfaceCombos(
            setOf(listOf(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, JPEG_R))
        )
        val fakeCamera = FakeCamera(cameraId)
        val adapter =
            CameraUseCaseAdapter(
                fakeCamera,
                null,
                RestrictedCameraInfo(fakeCamera.cameraInfoInternal, extensionsConfig),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                FakeCameraCoordinator(),
                fakeManager,
                FakeUseCaseConfigFactory(),
            )

        // Act: add ImageCapture that sets Ultra HDR.
        val imageCapture =
            ImageCapture.Builder()
                .setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                .build()
        adapter.addUseCases(setOf(imageCapture))
    }

    @RequiresApi(23)
    @Test(expected = CameraException::class)
    fun useRawWithExtensions_throwsException() {
        // Arrange: enable extensions.
        val extensionsConfig = createCoexistingRequiredRuleCameraConfig(FakeSessionProcessor())
        val cameraId = "fakeCameraId"
        val fakeManager = FakeCameraDeviceSurfaceManager()
        fakeManager.setValidSurfaceCombos(
            setOf(listOf(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, RAW_SENSOR))
        )
        val fakeCamera = FakeCamera(cameraId)
        val adapter =
            CameraUseCaseAdapter(
                fakeCamera,
                null,
                RestrictedCameraInfo(fakeCamera.cameraInfoInternal, extensionsConfig),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                FakeCameraCoordinator(),
                fakeManager,
                FakeUseCaseConfigFactory(),
            )

        // Act: add ImageCapture that sets Ultra HDR.
        val imageCapture =
            ImageCapture.Builder().setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW).build()
        adapter.addUseCases(setOf(imageCapture))
    }

    @RequiresApi(34) // Ultra HDR only supported on API 34+
    @Test(expected = CameraException::class)
    fun useUltraHdrWithCameraEffect_throwsException() {
        // Arrange: add an image effect.
        val cameraId = "fakeCameraId"
        val fakeManager = FakeCameraDeviceSurfaceManager()
        fakeManager.setValidSurfaceCombos(
            setOf(listOf(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, JPEG_R))
        )
        val adapter =
            CameraUseCaseAdapter(
                FakeCamera(cameraId),
                FakeCameraCoordinator(),
                fakeManager,
                FakeUseCaseConfigFactory(),
            )
        adapter.setEffects(listOf(imageEffect))

        // Act: add ImageCapture that sets Ultra HDR.
        val imageCapture =
            ImageCapture.Builder()
                .setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                .build()
        adapter.addUseCases(setOf(imageCapture))
    }

    @Test(expected = CameraException::class)
    fun useRawWithCameraEffect_throwsException() {
        // Arrange: add an image effect.
        val cameraId = "fakeCameraId"
        val fakeManager = FakeCameraDeviceSurfaceManager()
        fakeManager.setValidSurfaceCombos(
            setOf(listOf(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, RAW_SENSOR))
        )
        val adapter =
            CameraUseCaseAdapter(
                FakeCamera(cameraId),
                FakeCameraCoordinator(),
                fakeManager,
                FakeUseCaseConfigFactory(),
            )
        adapter.setEffects(listOf(imageEffect))

        // Act: add ImageCapture that sets Ultra HDR.
        val imageCapture =
            ImageCapture.Builder().setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW).build()
        adapter.addUseCases(setOf(imageCapture))
    }

    @Test(expected = CameraException::class)
    fun addStreamSharing_throwsException() {
        val streamSharing =
            StreamSharing(
                fakeCamera,
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(preview, video),
                useCaseConfigFactory
            )
        // Act: add use cases that can only be supported with StreamSharing
        adapter.addUseCases(setOf(streamSharing, video, image))
    }

    @Test
    fun invalidUseCaseCombo_streamSharingOn() {
        // Act: add use cases that can only be supported with StreamSharing
        adapter.addUseCases(setOf(preview, video, image))
        // Assert: StreamSharing is connected to camera.
        adapter.cameraUseCases.hasExactTypes(StreamSharing::class.java, ImageCapture::class.java)
        // Assert: StreamSharing children are bound
        assertThat(preview.camera).isNotNull()
        assertThat(video.camera).isNotNull()
    }

    @Test
    fun validUseCaseCombo_streamSharingOff() {
        // Act: add use cases that do not need StreamSharing
        adapter.addUseCases(setOf(preview, video))
        // Assert: the app UseCase are connected to camera.
        adapter.cameraUseCases.hasExactTypes(Preview::class.java, FakeUseCase::class.java)
    }

    @Test
    fun isUseCasesCombinationSupported_returnTrueWhenSupported() {
        // Assert
        assertThat(adapter.isUseCasesCombinationSupported(preview, image)).isTrue()
    }

    @Test
    fun isUseCasesCombinationSupported_returnFalseWhenNotSupported() {
        // Arrange
        val preview2 = Preview.Builder().build()
        // Assert: double preview use cases should not be supported even with stream sharing.
        assertThat(adapter.isUseCasesCombinationSupported(preview, preview2, video, image))
            .isFalse()
    }

    @Test
    fun isUseCasesCombinationSupportedByFramework_returnTrueWhenSupported() {
        // Assert
        assertThat(adapter.isUseCasesCombinationSupportedByFramework(preview, image)).isTrue()
    }

    @Test
    fun isUseCasesCombinationSupportedByFramework_returnFalseWhenNotSupported() {
        // Assert
        assertThat(adapter.isUseCasesCombinationSupportedByFramework(preview, video, image))
            .isFalse()
    }

    @Test
    fun isUseCasesCombinationSupported_withStreamSharing() {
        // preview, video, image should not be supported if stream sharing is not enabled.
        assertThat(
                adapter.isUseCasesCombinationSupported(
                    /*withStreamSharing=*/ false,
                    preview,
                    video,
                    image
                )
            )
            .isFalse()

        // preview, video, image should be supported if stream sharing is enabled.
        assertThat(
                adapter.isUseCasesCombinationSupported(
                    /*withStreamSharing=*/ true,
                    preview,
                    video,
                    image
                )
            )
            .isTrue()
    }

    @Test(expected = CameraException::class)
    fun invalidUseCaseComboCantBeFixedByStreamSharing_throwsException() {
        // Arrange: create a camera that only support one JPEG stream.
        fakeCameraDeviceSurfaceManager.setValidSurfaceCombos(setOf(listOf(JPEG)))
        // Act: add PRIVATE and JPEG streams.
        adapter.addUseCases(setOf(preview, image))
    }

    @Test
    fun addChildThatRequiresStreamSharing_streamSharingOn() {
        // Act: add UseCase that do not need StreamSharing
        adapter.addUseCases(setOf(video, image))
        // Assert.
        adapter.cameraUseCases.hasExactTypes(FakeUseCase::class.java, ImageCapture::class.java)
        // Act: add a new UseCase that needs StreamSharing
        adapter.addUseCases(setOf(preview))
        // Assert: StreamSharing is created.
        adapter.cameraUseCases.hasExactTypes(StreamSharing::class.java, ImageCapture::class.java)
        // Assert: StreamSharing children are bound
        assertThat(preview.camera).isNotNull()
        assertThat(video.camera).isNotNull()
        assertThat(image.camera).isNotNull()
    }

    @Test
    fun removeChildThatRequiresStreamSharing_streamSharingOff() {
        // Act: add UseCases that need StreamSharing.
        adapter.addUseCases(setOf(preview, video, image))
        // Assert: StreamSharing exists and bound.
        adapter.cameraUseCases.hasExactTypes(StreamSharing::class.java, ImageCapture::class.java)
        val streamSharing = adapter.getStreamSharing()
        assertThat(streamSharing.camera).isNotNull()
        // Act: remove UseCase so that StreamSharing is no longer needed
        adapter.removeUseCases(setOf(video))
        // Assert: StreamSharing removed and unbound.
        adapter.cameraUseCases.hasExactTypes(Preview::class.java, ImageCapture::class.java)
        assertThat(streamSharing.camera).isNull()
    }

    @RequiresApi(23)
    @Test
    fun extensionEnabledAndVideoCaptureExisted_streamSharingOn() {
        // Arrange: enable extensions.
        val adapter =
            createCameraUseCaseAdapter(
                fakeCamera,
                createCoexistingRequiredRuleCameraConfig(FakeSessionProcessor())
            )
        // Act: add UseCases that require StreamSharing.
        adapter.addUseCases(setOf(preview, video, image))
        // Assert: StreamSharing exists and bound.
        adapter.cameraUseCases.hasExactTypes(StreamSharing::class.java, ImageCapture::class.java)
        val streamSharing = adapter.getStreamSharing()
        assertThat(streamSharing.camera).isNotNull()
    }

    @RequiresApi(23)
    @Test
    fun extensionEnabledAndOnlyVideoCaptureAttached_streamSharingOn() {
        // Arrange: enable extensions.
        val adapter =
            createCameraUseCaseAdapter(
                fakeCamera,
                createCoexistingRequiredRuleCameraConfig(FakeSessionProcessor())
            )
        // Act: add UseCases that require StreamSharing.
        adapter.addUseCases(setOf(video))
        // Assert: StreamSharing exists and bound.
        adapter.cameraUseCases.hasExactTypes(
            StreamSharing::class.java,
            ImageCapture::class.java // Placeholder
        )
        val streamSharing = adapter.getStreamSharing()
        assertThat(streamSharing.camera).isNotNull()
    }

    @Test
    fun addAdditionalUseCase_streamSharingReused() {
        // Act: add UseCases that require StreamSharing
        adapter.addUseCases(setOf(preview, video, image))
        // Assert: StreamSharing is used.
        val streamSharing = adapter.getStreamSharing()
        adapter.cameraUseCases.hasExactTypes(StreamSharing::class.java, ImageCapture::class.java)
        // Act: add another UseCase
        adapter.addUseCases(setOf(analysis))
        // Assert: the same StreamSharing instance is kept.
        assertThat(adapter.getStreamSharing()).isSameInstanceAs(streamSharing)
        adapter.cameraUseCases.hasExactTypes(
            StreamSharing::class.java,
            ImageCapture::class.java,
            ImageAnalysis::class.java
        )
    }

    private fun CameraUseCaseAdapter.getStreamSharing(): StreamSharing {
        return this.cameraUseCases.filterIsInstance(StreamSharing::class.java).single()
    }

    private fun Collection<UseCase>.hasExactTypes(vararg classTypes: Any) {
        assertThat(classTypes.size).isEqualTo(size)
        classTypes.forEach { assertThat(filterIsInstance(it as Class<*>)).hasSize(1) }
    }

    @Test
    fun detachUseCases() {
        val fakeUseCase = FakeUseCase()
        adapter.addUseCases(listOf(fakeUseCase))
        adapter.removeUseCases(listOf(fakeUseCase))
        assertThat(fakeUseCase.camera).isNull()
    }

    @Test
    fun attachUseCases_restoreInteropConfig() {
        // Set an config to CameraControl.
        val option = Config.Option.create<Int>("OPTION_ID_1", Int::class.java)
        val value = 1
        val originalConfig = MutableOptionsBundle.create()
        originalConfig.insertOption(option, value)
        fakeCamera.cameraControlInternal.addInteropConfig(originalConfig)
        val cameraUseCaseAdapter1 = createCameraUseCaseAdapter(fakeCamera)
        val cameraUseCaseAdapter2 = createCameraUseCaseAdapter(fakeCamera)

        // This caches the original config and clears it from CameraControl internally.
        cameraUseCaseAdapter1.detachUseCases()

        // Set a different config.
        val newConfig = MutableOptionsBundle.create()
        newConfig.insertOption(Config.Option.create("OPTION_ID_2", Int::class.java), 2)
        fakeCamera.cameraControlInternal.addInteropConfig(newConfig)

        // This caches the second config and clears it from CameraControl internally.
        cameraUseCaseAdapter2.detachUseCases()

        // This restores the cached config to CameraControl.
        cameraUseCaseAdapter1.attachUseCases()
        val finalConfig: Config = fakeCamera.cameraControlInternal.interopConfig
        // Check the final config in CameraControl has the same value as the original config.
        assertThat(finalConfig.listOptions().containsAll(originalConfig.listOptions())).isTrue()
        assertThat(finalConfig.retrieveOption(option)).isEqualTo(value)
        // Check the final config doesn't contain the options set before it's attached again.
        assertThat(finalConfig.listOptions().containsAll(newConfig.listOptions())).isFalse()
    }

    @Test
    fun detachUseCases_clearInteropConfig() {
        // Set an config to CameraControl.
        val config: Config = MutableOptionsBundle.create()
        fakeCamera.cameraControlInternal.addInteropConfig(config)

        // This caches the original config and clears it from CameraControl internally.
        adapter.detachUseCases()

        // Check the config in CameraControl is empty.
        assertThat(fakeCamera.cameraControlInternal.interopConfig.listOptions()).isEmpty()
    }

    @Test
    fun closeCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        adapter.addUseCases(listOf(fakeUseCase))
        adapter.detachUseCases()
        assertThat(fakeUseCase.camera).isEqualTo(fakeCamera)
        assertThat(fakeCamera.attachedUseCases).isEmpty()
    }

    @Test
    fun cameraIdEquals() {
        val otherCameraId = createCameraUseCaseAdapter(fakeCamera).cameraId
        assertThat(adapter.cameraId == otherCameraId).isTrue()
    }

    @Test
    fun cameraEquivalent() {
        val cameraUseCaseAdapter = createCameraUseCaseAdapter(fakeCamera)
        val otherCameraUseCaseAdapter = createCameraUseCaseAdapter(fakeCamera)
        assertThat(cameraUseCaseAdapter.isEquivalent(otherCameraUseCaseAdapter)).isTrue()
    }

    @Test
    fun useCase_onAttach() {
        val fakeUseCase = spy(FakeUseCase())
        adapter.addUseCases(listOf(fakeUseCase))
        verify(fakeUseCase)
            .bindToCamera(eq(fakeCamera), isNull(), isNull(), any(FakeUseCaseConfig::class.java))
    }

    @Test
    fun useCase_onDetach() {
        val fakeUseCase = spy(FakeUseCase())
        adapter.addUseCases(listOf(fakeUseCase))
        adapter.removeUseCases(listOf(fakeUseCase))
        verify(fakeUseCase).unbindFromCamera(fakeCamera)
    }

    @Test
    fun addExistingUseCase_viewPortUpdated() {
        val aspectRatio1 = Rational(1, 1)
        val aspectRatio2 = Rational(2, 1)

        // Arrange: set up adapter with aspect ratio 1.
        adapter.setViewPort(ViewPort.Builder(aspectRatio1, Surface.ROTATION_0).build())
        val fakeUseCase = spy(FakeUseCase())
        adapter.addUseCases(listOf(fakeUseCase))
        // Use case gets aspect ratio 1
        assertThat(fakeUseCase.viewPortCropRect).isNotNull()
        assertThat(
                Rational(
                    fakeUseCase.viewPortCropRect!!.width(),
                    fakeUseCase.viewPortCropRect!!.height()
                )
            )
            .isEqualTo(aspectRatio1)

        // Act: set aspect ratio 2 and attach the same use case.
        adapter.setViewPort(ViewPort.Builder(aspectRatio2, Surface.ROTATION_0).build())
        adapter.addUseCases(listOf(fakeUseCase))

        // Assert: the viewport has aspect ratio 2.
        assertThat(fakeUseCase.viewPortCropRect).isNotNull()
        assertThat(
                Rational(
                    fakeUseCase.viewPortCropRect!!.width(),
                    fakeUseCase.viewPortCropRect!!.height()
                )
            )
            .isEqualTo(aspectRatio2)
    }

    @Test
    fun addExistingUseCase_setSensorToBufferMatrix() {
        val aspectRatio = Rational(1, 1)

        // Arrange: set up adapter with aspect ratio 1.
        // The sensor size is 4032x3024 defined in FakeCameraDeviceSurfaceManager
        fakeCameraDeviceSurfaceManager.setSuggestedStreamSpec(
            CAMERA_ID,
            FakeUseCaseConfig::class.java,
            StreamSpec.builder(Size(4032, 3022)).build()
        )
        /*         Sensor to Buffer                 Crop on Buffer
         *        0               4032
         *      0 |-----------------|            0    505  3527  4032
         *      1 |-----------------|          0 |-----------------|
         *        |   Crop Inside   |            |     |Crop|      |
         *   3023 |-----------------|       3022 |-----------------|
         *   3024 |-----------------|
         */
        adapter.setViewPort(ViewPort.Builder(aspectRatio, Surface.ROTATION_0).build())
        val fakeUseCase = FakeUseCase()
        adapter.addUseCases(listOf(fakeUseCase))
        assertThat(fakeUseCase.viewPortCropRect).isEqualTo(Rect(505, 0, 3527, 3022))
        assertThat(fakeUseCase.sensorToBufferTransformMatrix)
            .isEqualTo(
                Matrix().apply {
                    // From 4032x3024 to 4032x3022 with Crop Inside, no scale and Y shift 1.
                    setValues(
                        floatArrayOf(
                            /*scaleX=*/ 1f,
                            0f,
                            /*translateX=*/ 0f,
                            0f,
                            /*scaleY=*/ 1f,
                            /*translateY=*/ -1f,
                            0f,
                            0f,
                            1f
                        )
                    )
                }
            )
    }

    @Test
    fun noExtraUseCase_whenBindEmptyUseCaseList() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        cameraUseCaseAdapter.addUseCases(emptyList())
        val useCases = cameraUseCaseAdapter.useCases
        assertThat(useCases.size).isEqualTo(0)
    }

    @Test
    fun addExtraImageCapture_whenOnlyBindPreview() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val preview = Preview.Builder().build()

        // Adds a Preview only
        cameraUseCaseAdapter.addUseCases(listOf(preview))

        // Checks whether an extra ImageCapture is added.
        assertThat(containsImageCapture(cameraUseCaseAdapter.cameraUseCases)).isTrue()
    }

    @Test
    fun removeExtraImageCapture_afterBindImageCapture() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val preview = Preview.Builder().build()

        // Adds a Preview only
        cameraUseCaseAdapter.addUseCases(listOf(preview))

        // Checks whether an extra ImageCapture is added.
        assertThat(containsImageCapture(cameraUseCaseAdapter.cameraUseCases)).isTrue()
        val imageCapture = ImageCapture.Builder().build()

        // Adds an ImageCapture
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks the preview and the added imageCapture contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun addExtraImageCapture_whenUnbindImageCapture() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val useCases = mutableListOf<UseCase>()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCases.add(preview)
        useCases.add(imageCapture)

        // Adds both Preview and ImageCapture
        cameraUseCaseAdapter.addUseCases(useCases)

        // Checks whether exactly two use cases contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases.size).isEqualTo(2)

        // Removes the ImageCapture
        cameraUseCaseAdapter.removeUseCases(listOf(imageCapture))

        // Checks whether an extra ImageCapture is added.
        assertThat(containsImageCapture(cameraUseCaseAdapter.cameraUseCases)).isTrue()
    }

    @Test
    fun addExtraPreview_whenOnlyBindImageCapture() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val imageCapture = ImageCapture.Builder().build()

        // Adds an ImageCapture only
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks whether an extra Preview is added.
        assertThat(containsPreview(cameraUseCaseAdapter.cameraUseCases)).isTrue()
    }

    @Test
    fun removeExtraPreview_afterBindPreview() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val imageCapture = ImageCapture.Builder().build()

        // Adds a ImageCapture only
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks whether an extra Preview is added.
        assertThat(containsPreview(cameraUseCaseAdapter.cameraUseCases)).isTrue()
        val preview = Preview.Builder().build()

        // Adds an Preview
        cameraUseCaseAdapter.addUseCases(listOf(preview))
        // Checks the imageCapture and the added preview contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases).containsExactly(imageCapture, preview)
    }

    @Test
    fun addExtraPreview_whenUnbindPreview() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val useCases = mutableListOf<UseCase>()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCases.add(preview)
        useCases.add(imageCapture)

        // Adds both Preview and ImageCapture
        cameraUseCaseAdapter.addUseCases(useCases)

        // Checks whether exactly two use cases contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases.size).isEqualTo(2)

        // Removes the Preview
        cameraUseCaseAdapter.removeUseCases(listOf(preview))

        // Checks whether an extra Preview is added.
        assertThat(containsPreview(cameraUseCaseAdapter.cameraUseCases)).isTrue()
    }

    @Test
    fun noExtraUseCase_whenUnbindBothPreviewAndImageCapture() {
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, createCoexistingRequiredRuleCameraConfig())
        val useCases = mutableListOf<UseCase>()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCases.add(preview)
        useCases.add(imageCapture)

        // Adds both Preview and ImageCapture
        cameraUseCaseAdapter.addUseCases(useCases)

        // Checks whether exactly two use cases contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases.size).isEqualTo(2)

        // Removes all use cases
        cameraUseCaseAdapter.removeUseCases(useCases)

        // Checks whether any extra use cases is added
        assertThat(cameraUseCaseAdapter.useCases.size).isEqualTo(0)
    }

    @Test
    fun noExtraImageCapture_whenOnlyBindPreviewWithoutRule() {
        val preview = Preview.Builder().build()

        // Adds a Preview only
        adapter.addUseCases(listOf(preview))

        // Checks that no extra use case is added.
        assertThat(adapter.useCases.size).isEqualTo(1)
    }

    @Test
    fun noExtraPreview_whenOnlyBindImageCaptureWithoutRule() {
        val imageCapture = ImageCapture.Builder().build()

        // Adds an ImageCapture only
        adapter.addUseCases(listOf(imageCapture))

        // Checks that no extra use case is added.
        assertThat(adapter.useCases.size).isEqualTo(1)
    }

    @Test(expected = IllegalStateException::class)
    fun updateEffectsWithDuplicateTargets_throwsException() {
        CameraUseCaseAdapter.updateEffects(
            listOf(previewEffect, previewEffect),
            listOf(preview),
            emptyList()
        )
    }

    @Test
    fun hasSharedEffect_enableStreamSharing() {
        // Arrange: add a shared effect and an image effect
        adapter.setEffects(listOf(sharedEffect, imageEffect))

        // Act: update use cases.
        adapter.updateUseCases(listOf(preview, video, image, analysis))

        // Assert: StreamSharing wraps preview and video with the shared effect.
        val streamSharing = adapter.getStreamSharing()
        assertThat(streamSharing.children).containsExactly(preview, video)
        assertThat(streamSharing.effect).isEqualTo(sharedEffect)
        assertThat(preview.effect).isNull()
        assertThat(video.effect).isNull()
        assertThat(analysis.effect).isNull()
        assertThat(image.effect).isEqualTo(imageEffect)
    }

    @Test
    fun hasSharedEffectButOnlyOneChild_theEffectIsEnabledOnTheChild() {
        // Arrange: add a shared effect.
        adapter.setEffects(listOf(sharedEffect))

        // Act: update use cases.
        adapter.updateUseCases(listOf(preview))

        // Assert: no StreamSharing and preview gets the shared effect.
        assertThat(adapter.cameraUseCases.filterIsInstance(StreamSharing::class.java)).isEmpty()
        assertThat(preview.effect).isEqualTo(sharedEffect)
    }

    @Test
    fun updateEffects_effectsAddedAndRemoved() {
        // Arrange.
        val useCases = listOf(preview, video, image)

        // Act: update use cases with effects.
        CameraUseCaseAdapter.updateEffects(effects, useCases, emptyList())
        // Assert: UseCase have effects
        assertThat(preview.effect).isEqualTo(previewEffect)
        assertThat(image.effect).isEqualTo(imageEffect)
        assertThat(video.effect).isEqualTo(videoEffect)

        // Act: update again with no effects.
        CameraUseCaseAdapter.updateEffects(listOf(), useCases, emptyList())
        // Assert: use cases no longer has effects.
        assertThat(preview.effect).isNull()
        assertThat(image.effect).isNull()
        assertThat(video.effect).isNull()
    }

    @RequiresApi(23)
    private fun createAdapterWithSupportedCameraOperations(
        @RestrictedCameraInfo.CameraOperation supportedOps: Set<Int>
    ): CameraUseCaseAdapter {
        val fakeSessionProcessor = FakeSessionProcessor()
        // no camera operations are supported.
        fakeSessionProcessor.restrictedCameraOperations = supportedOps
        val cameraConfig: CameraConfig = FakeCameraConfig(fakeSessionProcessor)

        return createCameraUseCaseAdapter(fakeCamera, cameraConfig)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraControlFailed_whenNoCameraOperationsSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(supportedOps = emptySet())

        // 2. Act && Assert
        assertThrows<IllegalStateException> {
            cameraUseCaseAdapter.cameraControl.setZoomRatio(1.0f).await()
        }
        assertThrows<IllegalStateException> {
            cameraUseCaseAdapter.cameraControl.setLinearZoom(1.0f).await()
        }
        assertThrows<IllegalStateException> {
            cameraUseCaseAdapter.cameraControl.enableTorch(true).await()
        }
        assertThrows<IllegalStateException> {
            cameraUseCaseAdapter.cameraControl
                .startFocusAndMetering(getFocusMeteringAction())
                .await()
        }
        assertThrows<IllegalStateException> {
            cameraUseCaseAdapter.cameraControl.setExposureCompensationIndex(0).await()
        }
    }

    private fun getFocusMeteringAction(
        meteringMode: Int = FLAG_AF or FLAG_AE or FLAG_AWB
    ): FocusMeteringAction {
        val pointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        return FocusMeteringAction.Builder(pointFactory.createPoint(0.5f, 0.5f), meteringMode)
            .build()
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun zoomEnabled_whenZoomOperationsSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps = setOf(RestrictedCameraInfo.CAMERA_OPERATION_ZOOM)
            )

        // 2. Act && Assert
        cameraUseCaseAdapter.cameraControl.setZoomRatio(2.0f).await()
        assertThat(fakeCameraControl.zoomRatio).isEqualTo(2.0f)
        cameraUseCaseAdapter.cameraControl.setLinearZoom(1.0f).await()
        assertThat(fakeCameraControl.linearZoom).isEqualTo(1.0f)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun torchEnabled_whenTorchOperationSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps = setOf(RestrictedCameraInfo.CAMERA_OPERATION_TORCH)
            )

        // 2. Act
        cameraUseCaseAdapter.cameraControl.enableTorch(true).await()

        // 3. Assert
        assertThat(fakeCameraControl.torchEnabled).isEqualTo(true)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun focusMetering_afEnabled_whenAfOperationSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps =
                    setOf(
                        RestrictedCameraInfo.CAMERA_OPERATION_AUTO_FOCUS,
                        RestrictedCameraInfo.CAMERA_OPERATION_AF_REGION,
                    )
            )

        // 2. Act
        cameraUseCaseAdapter.cameraControl.startFocusAndMetering(getFocusMeteringAction()).await()

        // 3. Assert
        // Only AF point remains, AE/AWB points removed.
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAf?.size)
            .isEqualTo(1)
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAe).isEmpty()
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAwb).isEmpty()
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun focusMetering_aeEnabled_whenAeOperationsSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps =
                    setOf(
                        RestrictedCameraInfo.CAMERA_OPERATION_AE_REGION,
                    )
            )

        // 2. Act
        cameraUseCaseAdapter.cameraControl.startFocusAndMetering(getFocusMeteringAction()).await()

        // 3. Assert
        // Only AE point remains, AF/AWB points removed.
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAe?.size)
            .isEqualTo(1)
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAf).isEmpty()
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAwb).isEmpty()
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun focusMetering_awbEnabled_whenAwbOperationsSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps =
                    setOf(
                        RestrictedCameraInfo.CAMERA_OPERATION_AWB_REGION,
                    )
            )

        // 2. Act
        cameraUseCaseAdapter.cameraControl.startFocusAndMetering(getFocusMeteringAction()).await()

        // 3. Assert
        // Only AWB point remains, AF/AE points removed.
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAwb?.size)
            .isEqualTo(1)
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAf).isEmpty()
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction?.meteringPointsAe).isEmpty()
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun focusMetering_disabled_whenNoneIsSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps =
                    setOf(
                        RestrictedCameraInfo.CAMERA_OPERATION_AE_REGION,
                    )
            )

        // 2. Act && Assert
        assertThrows<IllegalStateException> {
            cameraUseCaseAdapter.cameraControl
                .startFocusAndMetering(getFocusMeteringAction(FLAG_AF or FLAG_AWB))
                .await()
        }
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction).isNull()
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun exposureEnabled_whenExposureOperationSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps = setOf(RestrictedCameraInfo.CAMERA_OPERATION_EXPOSURE_COMPENSATION)
            )

        // 2. Act
        cameraUseCaseAdapter.cameraControl.setExposureCompensationIndex(0).await()

        // 3. Assert
        assertThat(fakeCameraControl.exposureCompensationIndex).isEqualTo(0)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraInfo_returnsDisabledState_AllOpsDisabled(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(supportedOps = emptySet())

        // 2. Act && Assert
        // Zoom is disabled
        val zoomState = cameraUseCaseAdapter.cameraInfo.zoomState.value!!
        assertThat(zoomState.minZoomRatio).isEqualTo(1f)
        assertThat(zoomState.maxZoomRatio).isEqualTo(1f)
        assertThat(zoomState.zoomRatio).isEqualTo(1f)
        assertThat(zoomState.linearZoom).isEqualTo(0f)

        // Flash is disabled
        assertThat(cameraUseCaseAdapter.cameraInfo.hasFlashUnit()).isFalse()

        // Torch is disabled.
        assertThat(cameraUseCaseAdapter.cameraInfo.torchState.value).isEqualTo(TorchState.OFF)

        // FocusMetering is disabled.
        assertThat(
                cameraUseCaseAdapter.cameraInfo.isFocusMeteringSupported(getFocusMeteringAction())
            )
            .isFalse()

        // ExposureCompensation is disabled.
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.isExposureCompensationSupported)
            .isFalse()
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.exposureCompensationRange)
            .isEqualTo(Range(0, 0))
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.exposureCompensationStep)
            .isEqualTo(Rational.ZERO)
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.exposureCompensationIndex)
            .isEqualTo(0)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraInfo_zoomEnabled(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps = setOf(RestrictedCameraInfo.CAMERA_OPERATION_ZOOM)
            )
        fakeCameraInfo.setZoom(10f, 0.6f, 10f, 1f)

        // 2. Act
        val zoomState = cameraUseCaseAdapter.cameraInfo.zoomState.value!!

        // 3. Assert
        val fakeZoomState = fakeCameraInfo.zoomState.value!!
        assertThat(zoomState.zoomRatio).isEqualTo(fakeZoomState.zoomRatio)
        assertThat(zoomState.minZoomRatio).isEqualTo(fakeZoomState.minZoomRatio)
        assertThat(zoomState.maxZoomRatio).isEqualTo(fakeZoomState.maxZoomRatio)
        assertThat(zoomState.linearZoom).isEqualTo(fakeZoomState.linearZoom)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraInfo_torchEnabled(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps = setOf(RestrictedCameraInfo.CAMERA_OPERATION_TORCH)
            )
        fakeCameraInfo.setTorch(TorchState.ON)

        // 2. Act && Assert
        assertThat(cameraUseCaseAdapter.cameraInfo.torchState.value)
            .isEqualTo(fakeCameraInfo.torchState.value)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraInfo_afEnabled(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps =
                    setOf(
                        RestrictedCameraInfo.CAMERA_OPERATION_AUTO_FOCUS,
                        RestrictedCameraInfo.CAMERA_OPERATION_AF_REGION
                    )
            )
        fakeCameraInfo.setIsFocusMeteringSupported(true)

        // 2. Act && Assert
        assertThat(
                cameraUseCaseAdapter.cameraInfo.isFocusMeteringSupported(getFocusMeteringAction())
            )
            .isTrue()
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraInfo_exposureExposureEnabled(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps =
                    setOf(
                        RestrictedCameraInfo.CAMERA_OPERATION_EXPOSURE_COMPENSATION,
                    )
            )
        fakeCameraInfo.setExposureState(2, Range.create(0, 10), Rational(1, 1), true)

        // 2. Act && Assert
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.exposureCompensationIndex)
            .isEqualTo(fakeCameraInfo.exposureState.exposureCompensationIndex)
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.exposureCompensationRange)
            .isEqualTo(fakeCameraInfo.exposureState.exposureCompensationRange)
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.exposureCompensationStep)
            .isEqualTo(fakeCameraInfo.exposureState.exposureCompensationStep)
        assertThat(cameraUseCaseAdapter.cameraInfo.exposureState.isExposureCompensationSupported)
            .isEqualTo(fakeCameraInfo.exposureState.isExposureCompensationSupported)
    }

    @org.robolectric.annotation.Config(minSdk = 23)
    @Test
    fun cameraInfo_flashEnabled(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createAdapterWithSupportedCameraOperations(
                supportedOps = setOf(RestrictedCameraInfo.CAMERA_OPERATION_FLASH)
            )

        // 2. Act && Assert
        assertThat(cameraUseCaseAdapter.cameraInfo.hasFlashUnit())
            .isEqualTo(fakeCameraInfo.hasFlashUnit())
    }

    @Test
    fun cameraInfo_postviewSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, FakeCameraConfig(postviewSupported = true))
        val cameraInfoInternal = cameraUseCaseAdapter.cameraInfo as CameraInfoInternal
        // 2. Act && Assert
        assertThat(cameraInfoInternal.isPostviewSupported).isTrue()
    }

    @Test
    fun cameraInfo_captureProcessProgressSupported(): Unit = runBlocking {
        // 1. Arrange
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(
                fakeCamera,
                FakeCameraConfig(captureProcessProgressSupported = true)
            )
        val cameraInfoInternal = cameraUseCaseAdapter.cameraInfo as CameraInfoInternal

        // 2. Act && Assert
        assertThat(cameraInfoInternal.isCaptureProcessProgressSupported).isTrue()
    }

    @RequiresApi(23)
    @Test
    fun returnsCorrectSessionProcessorFromRestrictedCameraControl() {
        val fakeSessionProcessor = FakeSessionProcessor()
        val cameraUseCaseAdapter =
            createCameraUseCaseAdapter(fakeCamera, FakeCameraConfig(fakeSessionProcessor))

        val cameraControl = cameraUseCaseAdapter.cameraControl
        assertThat(cameraControl).isInstanceOf(RestrictedCameraControl::class.java)
        assertThat((cameraControl as RestrictedCameraControl).sessionProcessor)
            .isSameInstanceAs(fakeSessionProcessor)
    }

    @Test
    fun generateCameraId_isDualCameraRecording() {
        val cameraUseCaseAdapter1 = createCameraUseCaseAdapter(fakeCamera)
        val cameraUseCaseAdapter2 =
            createCameraUseCaseAdapter(fakeCamera, secondaryCamera = fakeSecondaryCamera)
        assertThat(cameraUseCaseAdapter1.cameraId).isNotEqualTo(cameraUseCaseAdapter2.cameraId)
    }

    @Test
    fun generateCameraId_isNotDualCameraRecording() {
        val cameraUseCaseAdapter1 = createCameraUseCaseAdapter(fakeCamera)
        val cameraUseCaseAdapter2 = createCameraUseCaseAdapter(fakeCamera, secondaryCamera = null)
        assertThat(cameraUseCaseAdapter1.cameraId).isEqualTo(cameraUseCaseAdapter2.cameraId)
    }

    private fun createFakeVideoCaptureUseCase(): FakeUseCase {
        return FakeUseCaseConfig.Builder()
            .setCaptureType(CaptureType.VIDEO_CAPTURE)
            .setSurfaceOccupancyPriority(0)
            .build()
            .apply { this.supportedEffectTargets = setOf(VIDEO_CAPTURE) }
    }

    private fun createCoexistingRequiredRuleCameraConfig(
        sessionProcessor: FakeSessionProcessor? = null
    ): CameraConfig {
        return object : CameraConfig {
            private val useCaseConfigFactory = UseCaseConfigFactory { _, _ -> null }
            private val identifier = Identifier.create(Any())

            override fun getUseCaseConfigFactory(): UseCaseConfigFactory {
                return useCaseConfigFactory
            }

            override fun getCompatibilityId(): Identifier {
                return identifier
            }

            override fun getConfig(): Config {
                return OptionsBundle.emptyBundle()
            }

            override fun getUseCaseCombinationRequiredRule(): Int {
                return CameraConfig.REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE
            }

            override fun getSessionProcessor(valueIfMissing: SessionProcessor?): SessionProcessor? {
                return sessionProcessor ?: valueIfMissing
            }

            override fun getSessionProcessor(): SessionProcessor {
                return sessionProcessor!!
            }
        }
    }

    private fun containsPreview(useCases: Collection<UseCase>): Boolean {
        for (useCase in useCases) {
            if (useCase is Preview) {
                return true
            }
        }
        return false
    }

    private fun containsImageCapture(useCases: Collection<UseCase>): Boolean {
        for (useCase in useCases) {
            if (useCase is ImageCapture) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a [CameraUseCaseAdapter] with the given parameters and adds it to the list of
     * adapters to detach.
     */
    private fun createCameraUseCaseAdapter(
        camera: CameraInternal,
        cameraConfig: CameraConfig = CameraConfigs.defaultConfig(),
        secondaryCamera: CameraInternal? = null
    ): CameraUseCaseAdapter {
        val adapter =
            CameraUseCaseAdapter(
                camera,
                secondaryCamera,
                RestrictedCameraInfo(camera.cameraInfoInternal, cameraConfig),
                if (secondaryCamera == null) null
                else RestrictedCameraInfo(secondaryCamera.cameraInfoInternal, cameraConfig),
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                cameraCoordinator,
                fakeCameraDeviceSurfaceManager,
                useCaseConfigFactory
            )
        adaptersToDetach.add(adapter)
        return adapter
    }
}
