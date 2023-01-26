/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.graphics.ImageFormat
import android.graphics.Rect
import android.util.LayoutDirection
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.LinkedHashSet

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class UseCaseTest {
    private var mockCameraInternal: CameraInternal? = null
    @Before
    fun setup() {
        mockCameraInternal = Mockito.mock(
            CameraInternal::class.java
        )
    }

    @Test
    fun getAttachedSessionConfig() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        val sessionToAttach = SessionConfig.Builder().build()
        testUseCase.updateSessionConfig(sessionToAttach)
        val attachedSession = testUseCase.sessionConfig
        Truth.assertThat(attachedSession).isEqualTo(sessionToAttach)
    }

    @Test
    fun removeListener() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.unbindFromCamera(mockCameraInternal!!)
        testUseCase.activate()
        Mockito.verify(mockCameraInternal, Mockito.never())!!.onUseCaseActive(
            ArgumentMatchers.any(
                UseCase::class.java
            )
        )
    }

    @Test
    fun notifyActiveState() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.activate()
        Mockito.verify(mockCameraInternal, Mockito.times(1))!!.onUseCaseActive(testUseCase)
    }

    @Test
    fun notifyInactiveState() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.deactivate()
        Mockito.verify(mockCameraInternal, Mockito.times(1))!!.onUseCaseInactive(testUseCase)
    }

    @Test
    fun notifyUpdatedSettings() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.update()
        Mockito.verify(mockCameraInternal, Mockito.times(1))!!.onUseCaseUpdated(testUseCase)
    }

    @Test
    fun notifyResetUseCase() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.notifyReset()
        Mockito.verify(mockCameraInternal, Mockito.times(1))!!.onUseCaseReset(testUseCase)
    }

    @Test
    fun useCaseConfig_keepOptionPriority() {
        val builder = FakeUseCaseConfig.Builder()
        val opt = Config.Option.create<Int>("OPT1", Int::class.java)
        builder.mutableConfig.insertOption(opt, Config.OptionPriority.ALWAYS_OVERRIDE, 1)
        val fakeUseCase = builder.build()
        val useCaseConfig = fakeUseCase.currentConfig
        Truth.assertThat(useCaseConfig.getOptionPriority(opt))
            .isEqualTo(Config.OptionPriority.ALWAYS_OVERRIDE)
    }

    @Test
    fun attachedSurfaceResolutionCanBeReset_whenOnDetach() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.updateSuggestedResolution(Size(640, 480))
        Truth.assertThat(testUseCase.attachedSurfaceResolution).isNotNull()
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.unbindFromCamera(mockCameraInternal!!)
        Truth.assertThat(testUseCase.attachedSurfaceResolution).isNull()
    }

    @Test
    fun viewPortCropRectCanBeReset_whenOnDetach() {
        val config = FakeUseCaseConfig.Builder().setTargetName(
            "UseCase"
        ).useCaseConfig
        val testUseCase = TestUseCase(config)
        testUseCase.setViewPortCropRect(Rect(0, 0, 640, 480))
        Truth.assertThat(testUseCase.viewPortCropRect).isNotNull()
        testUseCase.bindToCamera(mockCameraInternal!!, null, null)
        testUseCase.unbindFromCamera(mockCameraInternal!!)
        Truth.assertThat(testUseCase.viewPortCropRect).isNull()
    }

    @Test
    fun mergeConfigs() {
        val cameraDefaultPriority = 4
        val defaultConfig = FakeUseCaseConfig.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setBufferFormat(ImageFormat.RAW10)
            .setSurfaceOccupancyPriority(cameraDefaultPriority).useCaseConfig
        val useCaseImageFormat = ImageFormat.YUV_420_888
        val useCaseConfig = FakeUseCaseConfig.Builder()
            .setTargetRotation(Surface.ROTATION_90)
            .setBufferFormat(useCaseImageFormat).useCaseConfig
        val extendedConfig = FakeUseCaseConfig.Builder()
            .setTargetRotation(Surface.ROTATION_180).useCaseConfig
        val testUseCase = TestUseCase(useCaseConfig)
        val cameraInfo = FakeCameraInfoInternal()
        val mergedConfig = testUseCase.mergeConfigs(
            cameraInfo, extendedConfig,
            defaultConfig
        )
        Truth.assertThat(mergedConfig.surfaceOccupancyPriority).isEqualTo(cameraDefaultPriority)
        Truth.assertThat(mergedConfig.inputFormat).isEqualTo(useCaseImageFormat)
        val imageOutputConfig = mergedConfig as ImageOutputConfig
        Truth.assertThat(imageOutputConfig.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun returnNullResolutionInfo_beforeAddingToCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        Truth.assertThat(fakeUseCase.resolutionInfo).isNull()
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun returnResolutionInfo_afterAddingToCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfo
        Truth.assertThat(resolutionInfo).isNotNull()
        Truth.assertThat(resolutionInfo!!.resolution).isEqualTo(SURFACE_RESOLUTION)
        Truth.assertThat(resolutionInfo.cropRect).isEqualTo(
            Rect(
                0, 0,
                SURFACE_RESOLUTION.width, SURFACE_RESOLUTION.height
            )
        )
        Truth.assertThat(resolutionInfo.rotationDegrees).isEqualTo(0)
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun returnNullResolutionInfo_afterRemovedFromCameraUseCaseAdapter() {
        val fakeUseCase = FakeUseCase()
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        cameraUseCaseAdapter.removeUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfo
        Truth.assertThat(resolutionInfo).isNull()
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun correctRotationDegreesInResolutionInfo() {
        val fakeUseCase = FakeUseCase()
        fakeUseCase.targetRotationInternal = Surface.ROTATION_90
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfo
        Truth.assertThat(resolutionInfo!!.rotationDegrees).isEqualTo(270)
    }

    @Test
    @Throws(CameraUseCaseAdapter.CameraException::class)
    fun correctViewPortRectInResolutionInfo() {
        val fakeUseCase = FakeUseCase()
        val cameraUseCaseAdapter = createCameraUseCaseAdapter()
        cameraUseCaseAdapter.setViewPort(
            ViewPort(
                ViewPort.FILL_CENTER,
                Rational(16, 9), Surface.ROTATION_0, LayoutDirection.LTR
            )
        )
        cameraUseCaseAdapter.addUseCases(listOf<UseCase>(fakeUseCase))
        val resolutionInfo = fakeUseCase.resolutionInfo
        Truth.assertThat(resolutionInfo!!.cropRect).isEqualTo(Rect(0, 60, 640, 420))
    }

    private fun createCameraUseCaseAdapter(): CameraUseCaseAdapter {
        val cameraId = "fakeCameraId"
        val fakeCamera = FakeCamera(
            cameraId, null,
            FakeCameraInfoInternal(cameraId)
        )
        val fakeCameraDeviceSurfaceManager = FakeCameraDeviceSurfaceManager()
        fakeCameraDeviceSurfaceManager.setSuggestedResolution(
            cameraId,
            FakeUseCaseConfig::class.java,
            SURFACE_RESOLUTION
        )
        val useCaseConfigFactory: UseCaseConfigFactory = FakeUseCaseConfigFactory()
        return CameraUseCaseAdapter(
            LinkedHashSet(setOf(fakeCamera)),
            fakeCameraDeviceSurfaceManager,
            useCaseConfigFactory
        )
    }

    internal class TestUseCase(config: FakeUseCaseConfig?) : FakeUseCase(config!!) {
        fun activate() {
            notifyActive()
        }

        fun deactivate() {
            notifyInactive()
        }

        fun update() {
            notifyUpdated()
        }

        override fun onSuggestedResolutionUpdated(suggestedResolution: Size): Size {
            return suggestedResolution
        }
    }

    companion object {
        private val SURFACE_RESOLUTION: Size by lazy { Size(640, 480) }
    }
}