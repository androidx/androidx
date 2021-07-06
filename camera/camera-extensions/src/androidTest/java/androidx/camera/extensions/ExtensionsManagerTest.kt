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

package androidx.camera.extensions

import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.extensions.internal.VersionName
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.camera.testing.fakes.FakeUseCase
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(Parameterized::class)
@Suppress("DEPRECATION")
class ExtensionsManagerTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {

    private val context = InstrumentationRegistry.getInstrumentation().context

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private val effectMode: ExtensionsManager.EffectMode =
        ExtensionsTestUtil.extensionModeToEffectMode(extensionMode)

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())

        cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                lensFacing
            )
        )
    }

    @After
    fun teardown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    @Test
    fun getInstanceSuccessfully_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager = ExtensionsManager.getInstance(
            context,
            VersionName("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )
        assertThat(extensionsManager).isNotNull()
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager = ExtensionsManager.getInstance(
            context,
            VersionName("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assertThrows<IllegalArgumentException> {
            extensionsManager.getExtensionEnabledCameraSelector(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        }
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionModeIsNotSupported() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeFalse(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        assertThrows<IllegalArgumentException> {
            extensionsManager.getExtensionEnabledCameraSelector(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        }
    }

    @Test
    fun returnNewCameraSelector_whenExtensionModeIsSupprted() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val resultCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )
        assertThat(resultCameraSelector).isNotNull()
        assertThat(resultCameraSelector).isNotEqualTo(baseCameraSelector)
    }

    // TODO: Can be removed after the Extensions class is fully implemented.
    @Test
    fun isExtensionAvailable() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assertThat(ExtensionsManager.isExtensionAvailable(effectMode, lensFacing)).isEqualTo(
            extensionsManager.isExtensionAvailable(cameraProvider, cameraSelector, extensionMode)
        )
    }

    @Test
    fun correctCameraConfigIsSet_withSupportedExtensionCameraSelector() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )

        lateinit var camera: Camera
        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        var extensionsConfig = camera.extendedConfig as ExtensionsConfig
        assertThat(extensionsConfig.extensionMode).isEqualTo(extensionMode)
    }

    @Test
    fun getEstimatedCaptureLatencyRangeThrowsException_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager = ExtensionsManager.getInstance(
            context,
            VersionName("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assertThrows<IllegalArgumentException> {
            extensionsManager.getEstimatedCaptureLatencyRange(
                cameraProvider,
                baseCameraSelector,
                extensionMode,
                null
            )
        }
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnNull_belowVersion1_2() {
        assumeTrue(
            ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) < 0
        )

        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo = extensionsManager.getEstimatedCaptureLatencyRange(
            cameraProvider,
            baseCameraSelector,
            extensionMode,
            null
        )

        assertThat(latencyInfo).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeSameAsImplClass_aboveVersion1_2() {
        assumeTrue(
            ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) >= 0
        )

        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo = extensionsManager.getEstimatedCaptureLatencyRange(
            cameraProvider,
            baseCameraSelector,
            extensionMode,
            null
        )

        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(effectMode, lensFacing)
        val expectedLatencyInfo = impl.getEstimatedCaptureLatencyRange(null)

        assertThat(latencyInfo).isEqualTo(expectedLatencyInfo)
    }

    @Test
    fun getEstimatedCaptureLatencyRangeThrowsException_whenNoCameraCanBeFound() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val emptyCameraSelector = CameraSelector.Builder()
            .addCameraFilter { _ -> ArrayList<CameraInfo>() }
            .build()

        assertThrows<IllegalArgumentException> {
            extensionsManager.getEstimatedCaptureLatencyRange(
                cameraProvider,
                emptyCameraSelector,
                extensionMode,
                null
            )
        }
    }

    @Test
    fun canSetExtensionsConfig_whenNoUseCase() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }
    }

    @Test
    fun canNotSetExtensionsConfig_whenUseCaseHasExisted() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )

        instrumentation.runOnMainSync {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            // This test works only if the camera is the same no matter running normal or
            // extension modes.
            val normalCamera =
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector)
            val extensionCamera = cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector
            )
            assumeTrue(extensionCamera == normalCamera)

            // Binds a use case with the basic camera selector first.
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector, FakeUseCase())

            // IllegalStateException should be thrown when bindToLifecycle is called with
            // different extension camera config
            assertThrows<IllegalStateException> {
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, extensionCameraSelector)
            }
        }
    }

    @Test
    fun canSetSameExtensionsConfig_whenUseCaseHasExisted() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )

        instrumentation.runOnMainSync {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            // Binds a use case with extension camera config first.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector,
                FakeUseCase()
            )

            // Binds another use case with the same extension camera config.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector,
                FakeUseCase()
            )
        }
    }

    @Test
    fun canSwitchExtendedCameraConfig_afterUnbindUseCases() {
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )

        instrumentation.runOnMainSync {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            // Binds a use case with extension camera config first.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector,
                FakeUseCase()
            )

            // Unbinds all use cases
            cameraProvider.unbindAll()

            // Binds another use case with the basic camera selector.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                baseCameraSelector,
                FakeUseCase()
            )
        }
    }
}