/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Size
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class PreviewPixelHDRnetQuirkTest(
    private val manufacturer: String,
    private val device: String,
    private val shouldApplyQuirk: Boolean,
) {

    @get:Rule
    val immediateExecutorRule = object : TestWatcher() {
        override fun starting(description: Description) {
            super.starting(description)
            ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) {
                    runnable.run()
                }

                override fun postToMainThread(runnable: Runnable) {
                    runnable.run()
                }

                override fun isMainThread(): Boolean {
                    return true
                }
            })
        }

        override fun finished(description: Description) {
            super.finished(description)
            ArchTaskExecutor.getInstance().setDelegate(null)
        }
    }

    private val resolutionHD: Size = Size(1280, 720)
    private val resolutionVGA: Size = Size(640, 480)

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter

    @Before
    fun setup() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", manufacturer)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", device)
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
        }
    }

    @Test
    fun previewShouldApplyToneModeForHDRNet() {
        // Arrange
        cameraUseCaseAdapter = configureCameraUseCaseAdapter(
            resolutionVGA,
            configType = PreviewConfig::class.java
        )
        val preview = Preview.Builder().build()

        // Act. Update UseCase to create SessionConfig
        cameraUseCaseAdapter.addUseCases(setOf<UseCase>(preview))

        // Assert.
        if (shouldApplyQuirk) {
            assertThat(
                Camera2ImplConfig(
                    preview.sessionConfig.repeatingCaptureConfig.implementationOptions
                ).getCaptureRequestOption(CaptureRequest.TONEMAP_MODE)
            ).isEqualTo(CaptureRequest.TONEMAP_MODE_HIGH_QUALITY)
        } else {
            assertThat(
                Camera2ImplConfig(
                    preview.sessionConfig.repeatingCaptureConfig.implementationOptions
                ).getCaptureRequestOption(CaptureRequest.TONEMAP_MODE)
            ).isNull()
        }
    }

    @Test
    fun otherUseCasesNotApplyHDRNet() {
        // Arrange
        cameraUseCaseAdapter = configureCameraUseCaseAdapter(
            resolutionVGA,
            configType = ImageCaptureConfig::class.java
        )

        // Act. Update UseCase to create SessionConfig
        val imageCapture = ImageCapture.Builder().build()
        cameraUseCaseAdapter.addUseCases(setOf<UseCase>(imageCapture))

        assertThat(
            Camera2ImplConfig(
                imageCapture.sessionConfig.repeatingCaptureConfig.implementationOptions
            ).getCaptureRequestOption(CaptureRequest.TONEMAP_MODE)
        ).isNull()
    }

    @Test
    fun resolution16x9NotApplyHDRNet() {
        // Arrange
        cameraUseCaseAdapter = configureCameraUseCaseAdapter(
            resolutionHD,
            configType = PreviewConfig::class.java
        )

        // Act. Update UseCase to create SessionConfig
        val preview = Preview.Builder().build()
        cameraUseCaseAdapter.addUseCases(setOf<UseCase>(preview))

        assertThat(
            Camera2ImplConfig(
                preview.sessionConfig.repeatingCaptureConfig.implementationOptions
            ).getCaptureRequestOption(CaptureRequest.TONEMAP_MODE)
        ).isNull()
    }

    private fun configureCameraUseCaseAdapter(
        resolution: Size,
        fakeCameraId: String = "0",
        configType: Class<out UseCaseConfig<*>?>,
    ): CameraUseCaseAdapter {
        return CameraUseCaseAdapter(
            LinkedHashSet<CameraInternal>(setOf(FakeCamera(fakeCameraId))),
            FakeCameraCoordinator(),
            FakeCameraDeviceSurfaceManager().apply {
                setSuggestedStreamSpec(
                    fakeCameraId,
                    configType,
                    StreamSpec.builder(resolution).build()
                )
            },
            androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter(
                ApplicationProvider.getApplicationContext()
            )
        )
    }

    companion object {
        private const val FAKE_OEM = "fake_oem"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "manufacturer={0}, device={1}, shouldApplyQuirk={2}"
        )
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf("Google", "sunfish", true))
            add(arrayOf("Google", "barbet", true))
            add(arrayOf(FAKE_OEM, "barbet", false))
            add(arrayOf(FAKE_OEM, "not_a_real_device", false))
        }
    }
}
