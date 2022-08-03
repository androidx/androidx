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

import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.Surface
import androidx.camera.core.EffectBundle
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceEffect.PREVIEW
import androidx.camera.core.UseCase
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.SurfaceEffectWithExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeSurfaceEffect
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CameraUseCaseAdapter].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraUseCaseAdapterTest {

    private lateinit var surfaceEffect: FakeSurfaceEffect
    private lateinit var effectBundle: EffectBundle
    private lateinit var executor: ExecutorService

    private lateinit var fakeCameraDeviceSurfaceManager: FakeCameraDeviceSurfaceManager
    private lateinit var fakeCamera: FakeCamera
    private lateinit var useCaseConfigFactory: UseCaseConfigFactory
    private val fakeCameraSet = LinkedHashSet<CameraInternal>()

    @Before
    fun setUp() {
        fakeCameraDeviceSurfaceManager = FakeCameraDeviceSurfaceManager()
        fakeCamera = FakeCamera()
        useCaseConfigFactory = FakeUseCaseConfigFactory()
        fakeCameraSet.add(fakeCamera)
        surfaceEffect = FakeSurfaceEffect(mainThreadExecutor())
        executor = Executors.newSingleThreadExecutor()
        effectBundle = EffectBundle.Builder(executor).addEffect(PREVIEW, surfaceEffect).build()
    }

    @After
    fun tearDown() {
        surfaceEffect.cleanUp()
        executor.shutdown()
    }

    @Test
    fun detachUseCases() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val fakeUseCase = FakeUseCase()
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        cameraUseCaseAdapter.removeUseCases(listOf(fakeUseCase))
        assertThat(fakeUseCase.camera).isNull()
    }

    @Test
    fun attachUseCases_restoreInteropConfig() {
        // Set an config to CameraControl.
        val option = Config.Option.create<Int>(
            "OPTION_ID_1",
            Int::class.java
        )
        val value = 1
        val originalConfig = MutableOptionsBundle.create()
        originalConfig.insertOption(option, value)
        fakeCamera.cameraControlInternal.addInteropConfig(originalConfig)
        val cameraUseCaseAdapter1 = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val cameraUseCaseAdapter2 = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )

        // This caches the original config and clears it from CameraControl internally.
        cameraUseCaseAdapter1.detachUseCases()

        // Set a different config.
        val newConfig = MutableOptionsBundle.create()
        newConfig.insertOption(
            Config.Option.create(
                "OPTION_ID_2",
                Int::class.java
            ), 2
        )
        fakeCamera.cameraControlInternal.addInteropConfig(newConfig)

        // This caches the second config and clears it from CameraControl internally.
        cameraUseCaseAdapter2.detachUseCases()

        // This restores the cached config to CameraControl.
        cameraUseCaseAdapter1.attachUseCases()
        val finalConfig: Config =
            fakeCamera.cameraControlInternal.interopConfig
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
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )

        // This caches the original config and clears it from CameraControl internally.
        cameraUseCaseAdapter.detachUseCases()

        // Check the config in CameraControl is empty.
        assertThat(fakeCamera.cameraControlInternal.interopConfig.listOptions()).isEmpty()
    }

    @Test
    fun closeCameraUseCaseAdapter() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val fakeUseCase = FakeUseCase()
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        cameraUseCaseAdapter.detachUseCases()
        assertThat(fakeUseCase.camera).isEqualTo(fakeCamera)
        assertThat(fakeCamera.attachedUseCases).isEmpty()
    }

    @Test
    fun cameraIdEquals() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val otherCameraId = CameraUseCaseAdapter.generateCameraId(fakeCameraSet)
        assertThat(cameraUseCaseAdapter.cameraId == otherCameraId).isTrue()
    }

    @Test
    fun cameraEquivalent() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val otherCameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        assertThat(cameraUseCaseAdapter.isEquivalent(otherCameraUseCaseAdapter)).isTrue()
    }

    @Test
    fun useCase_onAttach() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val fakeUseCase = spy(FakeUseCase())
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        verify(fakeUseCase).onAttach(eq(fakeCamera), isNull(), any(FakeUseCaseConfig::class.java))
    }

    @Test
    fun useCase_onDetach() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val fakeUseCase = spy(FakeUseCase())
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        cameraUseCaseAdapter.removeUseCases(listOf(fakeUseCase))
        verify(fakeUseCase).onDetach(fakeCamera)
    }

    @Test
    fun eventCallbackOnBind() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val callback = mock(UseCase.EventCallback::class.java)
        val fakeUseCase = FakeUseCaseConfig.Builder().setUseCaseEventCallback(callback).build()
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        verify(callback).onAttach(fakeCamera.cameraInfoInternal)
    }

    @Test
    fun eventCallbackOnUnbind() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val callback = mock(UseCase.EventCallback::class.java)
        val fakeUseCase = FakeUseCaseConfig.Builder().setUseCaseEventCallback(callback).build()
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        cameraUseCaseAdapter.removeUseCases(listOf(fakeUseCase))
        verify(callback).onDetach()
    }

    @Test
    fun addExistingUseCase_viewPortUpdated() {
        val aspectRatio1 = Rational(1, 1)
        val aspectRatio2 = Rational(2, 1)

        // Arrange: set up adapter with aspect ratio 1.
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setViewPort(
            ViewPort.Builder(aspectRatio1, Surface.ROTATION_0).build()
        )
        val fakeUseCase = spy(FakeUseCase())
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        // Use case gets aspect ratio 1
        assertThat(fakeUseCase.viewPortCropRect).isNotNull()
        assertThat(
            Rational(
                fakeUseCase.viewPortCropRect!!.width(),
                fakeUseCase.viewPortCropRect!!.height()
            )
        ).isEqualTo(aspectRatio1)

        // Act: set aspect ratio 2 and attach the same use case.
        cameraUseCaseAdapter.setViewPort(
            ViewPort.Builder(aspectRatio2, Surface.ROTATION_0).build()
        )
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))

        // Assert: the viewport has aspect ratio 2.
        assertThat(fakeUseCase.viewPortCropRect).isNotNull()
        assertThat(
            Rational(
                fakeUseCase.viewPortCropRect!!.width(),
                fakeUseCase.viewPortCropRect!!.height()
            )
        ).isEqualTo(aspectRatio2)
    }

    @Test
    fun addExistingUseCase_setSensorToBufferMatrix() {
        val aspectRatio = Rational(1, 1)

        // Arrange: set up adapter with aspect ratio 1.
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setViewPort(ViewPort.Builder(aspectRatio, Surface.ROTATION_0).build())
        val fakeUseCase = spy(FakeUseCase())
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))
        verify(fakeUseCase).setViewPortCropRect(Rect(504, 0, 3528, 3024))
        verify(fakeUseCase).setSensorToBufferTransformMatrix(Matrix())
    }

    @Test
    fun canSetExtendedCameraConfig_whenNoUseCase() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(FakeCameraConfig())
    }

    @Test(expected = IllegalStateException::class)
    fun canNotSetExtendedCameraConfig_whenUseCaseHasExisted() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )

        // Adds use case first
        cameraUseCaseAdapter.addUseCases(listOf(FakeUseCase()))

        // Sets extended config after a use case is added
        cameraUseCaseAdapter.setExtendedConfig(FakeCameraConfig())
    }

    @Test
    fun canSetSameExtendedCameraConfig_whenUseCaseHasExisted() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val cameraConfig: CameraConfig = FakeCameraConfig()
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig)
        cameraUseCaseAdapter.addUseCases(listOf(FakeUseCase()))

        // Sets extended config with the same camera config
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig)
    }

    @Test
    fun canSwitchExtendedCameraConfig_afterUnbindUseCases() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val cameraConfig1: CameraConfig = FakeCameraConfig()
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig1)

        // Binds use case
        val fakeUseCase = FakeUseCase()
        cameraUseCaseAdapter.addUseCases(listOf(fakeUseCase))

        // Unbinds use case
        cameraUseCaseAdapter.removeUseCases(listOf(fakeUseCase))

        // Sets extended config with different camera config
        val cameraConfig2: CameraConfig = FakeCameraConfig()
        cameraUseCaseAdapter.setExtendedConfig(cameraConfig2)
    }

    @Test
    fun noExtraUseCase_whenBindEmptyUseCaseList() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
        cameraUseCaseAdapter.addUseCases(emptyList())
        val useCases = cameraUseCaseAdapter.useCases
        assertThat(useCases.size).isEqualTo(0)
    }

    @Test
    fun addExtraImageCapture_whenOnlyBindPreview() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
        val preview = Preview.Builder().build()

        // Adds a Preview only
        cameraUseCaseAdapter.addUseCases(listOf(preview))

        // Checks whether an extra ImageCapture is added.
        assertThat(containsImageCapture(cameraUseCaseAdapter.useCases)).isTrue()
    }

    @Test
    fun removeExtraImageCapture_afterBindImageCapture() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
        val preview = Preview.Builder().build()

        // Adds a Preview only
        cameraUseCaseAdapter.addUseCases(listOf(preview))

        // Checks whether an extra ImageCapture is added.
        assertThat(containsImageCapture(cameraUseCaseAdapter.useCases))
        val imageCapture = ImageCapture.Builder().build()

        // Adds an ImageCapture
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks the preview and the added imageCapture contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun addExtraImageCapture_whenUnbindImageCapture() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
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
        assertThat(containsImageCapture(cameraUseCaseAdapter.useCases)).isTrue()
    }

    @Test
    fun addExtraPreview_whenOnlyBindImageCapture() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
        val imageCapture = ImageCapture.Builder().build()

        // Adds an ImageCapture only
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks whether an extra Preview is added.
        assertThat(containsPreview(cameraUseCaseAdapter.useCases)).isTrue()
    }

    @Test
    fun removeExtraPreview_afterBindPreview() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
        val imageCapture = ImageCapture.Builder().build()

        // Adds a ImageCapture only
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks whether an extra Preview is added.
        assertThat(containsPreview(cameraUseCaseAdapter.useCases))
        val preview = Preview.Builder().build()

        // Adds an Preview
        cameraUseCaseAdapter.addUseCases(listOf(preview))
        // Checks the imageCapture and the added preview contained in the CameraUseCaseAdapter
        assertThat(cameraUseCaseAdapter.useCases).containsExactly(imageCapture, preview)
    }

    @Test
    fun addExtraPreview_whenUnbindPreview() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
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
        assertThat(containsPreview(cameraUseCaseAdapter.useCases)).isTrue()
    }

    @Test
    fun noExtraUseCase_whenUnbindBothPreviewAndImageCapture() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        cameraUseCaseAdapter.setExtendedConfig(createCoexistingRequiredRuleCameraConfig())
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
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val preview = Preview.Builder().build()

        // Adds a Preview only
        cameraUseCaseAdapter.addUseCases(listOf(preview))

        // Checks that no extra use case is added.
        assertThat(cameraUseCaseAdapter.useCases.size).isEqualTo(1)
    }

    @Test
    fun noExtraPreview_whenOnlyBindImageCaptureWithoutRule() {
        val cameraUseCaseAdapter = CameraUseCaseAdapter(
            fakeCameraSet,
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
        val imageCapture = ImageCapture.Builder().build()

        // Adds an ImageCapture only
        cameraUseCaseAdapter.addUseCases(listOf(imageCapture))

        // Checks that no extra use case is added.
        assertThat(cameraUseCaseAdapter.useCases.size).isEqualTo(1)
    }

    @Test
    fun updateEffects_effectsAddedAndRemoved() {
        // Arrange.
        val preview = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
        // Act: update use cases with effects bundle
        CameraUseCaseAdapter.updateEffects(effectBundle, listOf(preview))
        // Assert: preview has effect wrapped with the right executor.
        val previewEffect = preview.effect as SurfaceEffectWithExecutor
        assertThat(previewEffect.surfaceEffect).isEqualTo(surfaceEffect)
        assertThat(previewEffect.executor).isEqualTo(executor)
        // Act: update again with null effects bundle
        CameraUseCaseAdapter.updateEffects(null, listOf(preview))
        // Assert: preview no longer has effects.
        assertThat(preview.effect).isNull()
    }

    private fun createCoexistingRequiredRuleCameraConfig(): CameraConfig {
        return object : CameraConfig {
            private val mUseCaseConfigFactory =
                UseCaseConfigFactory { _, _ -> null }
            private val mIdentifier = Identifier.create(Any())
            override fun getUseCaseConfigFactory(): UseCaseConfigFactory {
                return mUseCaseConfigFactory
            }

            override fun getCompatibilityId(): Identifier {
                return mIdentifier
            }

            override fun getConfig(): Config {
                return OptionsBundle.emptyBundle()
            }

            override fun getUseCaseCombinationRequiredRule(): Int {
                return CameraConfig.REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE
            }
        }
    }

    private fun containsPreview(useCases: List<UseCase>): Boolean {
        for (useCase in useCases) {
            if (useCase is Preview) {
                return true
            }
        }
        return false
    }

    private fun containsImageCapture(useCases: List<UseCase>): Boolean {
        for (useCase in useCases) {
            if (useCase is ImageCapture) {
                return true
            }
        }
        return false
    }

    private class FakeCameraConfig : CameraConfig {
        private val mUseCaseConfigFactory =
            UseCaseConfigFactory { _, _ -> null }
        private val mIdentifier = Identifier.create(Any())
        override fun getUseCaseConfigFactory(): UseCaseConfigFactory {
            return mUseCaseConfigFactory
        }

        override fun getCompatibilityId(): Identifier {
            return mIdentifier
        }

        override fun getConfig(): Config {
            return OptionsBundle.emptyBundle()
        }
    }
}
