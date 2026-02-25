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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.extensions.internal.VendorExtender
import androidx.camera.extensions.internal.sessionprocessor.Camera2ExtensionsSessionProcessor
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class ExtensionsManagerTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int,
) {

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    private lateinit var baseCameraSelector: CameraSelector

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(lensFacing, extensionMode)
        )

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        val context: Context = ApplicationProvider.getApplicationContext()

        @JvmStatic
        @Parameterized.Parameters(name = "mode = {0}, facing = {1}")
        fun data(): Collection<Array<Any>> {
            return ExtensionsTestUtil.getAllExtensionsLensFacingCombinations(context, false)
        }
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionModeIsNotSupported(): Unit =
        runBlocking {
            extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)
            val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            assumeFalse(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

            assertThrows<IllegalArgumentException> {
                extensionsManager.getExtensionEnabledCameraSelector(
                    baseCameraSelector,
                    extensionMode,
                )
            }
        }

    @Test
    fun returnNewCameraSelector_whenExtensionModeIsSupprted() {
        checkExtensionAvailabilityAndInit()

        val resultCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)
        assertThat(resultCameraSelector).isNotNull()
        assertThat(resultCameraSelector).isNotEqualTo(baseCameraSelector)
    }

    @Test
    fun correctAvailability_whenExtensionIsNotAvailable(): Unit = runBlocking {
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeFalse(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        for (cameraInfo in cameraProvider.availableCameraInfos) {
            val characteristics =
                (cameraInfo as CameraInfoInternal).cameraCharacteristics as CameraCharacteristics
            // Checks lens facing first
            val currentLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (currentLensFacing != lensFacing) {
                continue
            }

            // Checks whether the specified extension mode is available by camera info and it
            // must be false
            assertThat(isExtensionAvailableByCameraInfo(cameraInfo)).isFalse()
        }
    }

    @Test
    fun filterCorrectCamera_whenExtensionIsAvailable(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        // Calls bind to lifecycle to get the selected camera
        lateinit var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        val cameraId = (camera.cameraInfo as CameraInfoInternal).cameraId

        // Checks each camera in the available camera list that the selected camera must be the
        // first one supporting the specified extension mode in the same lens facing
        for (cameraInfo in cameraProvider.availableCameraInfos) {
            val characteristics =
                (cameraInfo as CameraInfoInternal).cameraCharacteristics as CameraCharacteristics

            // Checks lens facing first
            val currentLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (currentLensFacing != lensFacing) {
                continue
            }

            // Checks whether the specified extension mode is available by camera info
            val isSupported = isExtensionAvailableByCameraInfo(cameraInfo)
            val currentCameraId = cameraInfo.cameraId

            if (currentCameraId.equals(cameraId)) {
                assertThat(isSupported).isTrue()
                break
            } else {
                // Any other camera in front of the selected camera in the available cameras list
                // must not support the specified extension mode.
                assertThat(isSupported).isFalse()
            }
        }
    }

    @Test
    fun correctCameraConfigIsSet_withSupportedExtensionCameraSelector(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        lateinit var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        var extensionsConfig = camera.extendedConfig as ExtensionsConfig
        assertThat(extensionsConfig.extensionMode).isEqualTo(extensionMode)
    }

    @Test
    fun getEstimatedCaptureLatencyRange_returnValueFromExtender(): Unit = runBlocking {
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        assumeTrue(
            extensionsManager.extensionsAvailability ==
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        val estimatedCaptureLatency = Range(100L, 1000L)

        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>,
                ): Boolean {
                    return true
                }

                override fun getEstimatedCaptureLatencyRange(size: Size?): Range<Long> {
                    return estimatedCaptureLatency
                }
            }
        extensionsManager.setVendorExtenderFactory { _ -> fakeVendorExtender }

        assertThat(
                extensionsManager.getEstimatedCaptureLatencyRange(baseCameraSelector, extensionMode)
            )
            .isEqualTo(estimatedCaptureLatency)
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnsNull_whenNoCameraCanBeFound() {
        checkExtensionAvailabilityAndInit()

        val emptyCameraSelector =
            CameraSelector.Builder().addCameraFilter { _ -> ArrayList<CameraInfo>() }.build()

        assertThat(
                extensionsManager.getEstimatedCaptureLatencyRange(
                    emptyCameraSelector,
                    extensionMode,
                )
            )
            .isNull()
    }

    @Test
    fun canSetExtensionsConfig_whenNoUseCase(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }
    }

    @Test
    fun canNotSetExtensionsConfig_whenUseCaseHasExisted(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        withContext(Dispatchers.Main) {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            // This test works only if the camera is the same no matter running normal or
            // extension modes.
            val normalCamera =
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector)
            val extensionCamera =
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, extensionCameraSelector)
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
    fun canSetSameExtensionsConfig_whenUseCaseHasExisted(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        withContext(Dispatchers.Main) {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            // Binds a use case with extension camera config first.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector,
                FakeUseCase(),
            )

            // Binds another use case with the same extension camera config.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector,
                FakeUseCase(),
            )
        }
    }

    @Test
    fun canSwitchExtendedCameraConfig_afterUnbindUseCases(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        withContext(Dispatchers.Main) {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            // Binds a use case with extension camera config first.
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionCameraSelector,
                FakeUseCase(),
            )

            // Unbinds all use cases
            cameraProvider.unbindAll()

            // Binds another use case with the basic camera selector.
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector, FakeUseCase())
        }
    }

    @Test
    fun isImageAnalysisSupportedReturnsFalse_whenHasNoAnalysisSizes(): Unit = runBlocking {
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>,
                ): Boolean {
                    return true
                }

                override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
                    return emptyArray()
                }
            }
        extensionsManager.setVendorExtenderFactory { _ -> fakeVendorExtender }

        assumeTrue(
            extensionsManager.extensionsAvailability ==
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(baseCameraSelector, extensionMode))
            .isFalse()
    }

    @Test
    fun isImageAnalysisSupportedReturnsTrue_whenHasAnalysisSizes(): Unit = runBlocking {
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>,
                ): Boolean {
                    return true
                }

                override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
                    return arrayOf(Size(1920, 1080))
                }
            }
        extensionsManager.setVendorExtenderFactory { _ -> fakeVendorExtender }

        assumeTrue(
            extensionsManager.extensionsAvailability ==
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(baseCameraSelector, extensionMode))
            .isTrue()
    }

    @Test
    fun isImageAnalysisSupportedIsFalse_whenNoCameraCanBeFound() {
        checkExtensionAvailabilityAndInit()
        val emptyCameraSelector =
            CameraSelector.Builder().addCameraFilter { _ -> ArrayList<CameraInfo>() }.build()

        assertThat(extensionsManager.isImageAnalysisSupported(emptyCameraSelector, extensionMode))
            .isFalse()
    }

    @Test
    fun postviewSupportedIsSetCorrectlyOnCameraConfig() = runBlocking {
        // 1. Arrange
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()
        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>,
                ): Boolean {
                    return true
                }

                override fun isPostviewAvailable(): Boolean {
                    return true
                }
            }
        extensionsManager.setVendorExtenderFactory { _ -> fakeVendorExtender }

        // 2. Act
        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        // 3. Assert
        assertThat(camera.extendedConfig.isPostviewSupported).isTrue()
    }

    @Test
    fun captureProcessProgressSupportedIsSetCorrectlyOnCameraConfig() = runBlocking {
        // 1. Arrange
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()
        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>,
                ): Boolean {
                    return true
                }

                override fun isCaptureProcessProgressAvailable(): Boolean {
                    return true
                }
            }
        extensionsManager.setVendorExtenderFactory { _ -> fakeVendorExtender }

        // 2. Act
        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        // 3. Assert
        assertThat(camera.extendedConfig.isCaptureProcessProgressSupported).isTrue()
    }

    @Test
    fun returnsCorrectInitialTypeFromSessionProcessor() = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        val sessionProcessor = camera.extendedConfig.sessionProcessor
        val cameraExtensionsInfo = sessionProcessor as CameraExtensionsInfo
        val currentType = cameraExtensionsInfo.currentExtensionMode
        if (cameraExtensionsInfo.isCurrentExtensionModeAvailable) {
            assertThat(currentType!!.value).isEqualTo(extensionMode)
        } else {
            assertThat(currentType).isNull()
        }
    }

    @Test
    fun returnsCorrectExtensionTypeFromCameraExtensionsInfo() = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        val cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)

        if (cameraExtensionsInfo.isCurrentExtensionModeAvailable) {
            assertThat(cameraExtensionsInfo.currentExtensionMode!!.value).isEqualTo(extensionMode)
        } else {
            assertThat(cameraExtensionsInfo.currentExtensionMode).isNull()
        }
    }

    @Test
    fun returnsCorrectExtensionStrengthAvailabilityFromCameraExtensionsInfo() = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        val cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)

        assertThat(cameraExtensionsInfo.isExtensionStrengthAvailable)
            .isEqualTo(
                camera.extendedConfig.sessionProcessor.supportedCameraOperations.contains(
                    AdapterCameraInfo.CAMERA_OPERATION_EXTENSION_STRENGTH
                )
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun returnsCorrectCurrentExtensionTypeAvailabilityFromCameraExtensionsInfo() = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        // Inject fake VendorExtenderFactory to provide custom VendorExtender
        extensionsManager.setVendorExtenderFactory { _ ->
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>,
                ): Boolean {
                    return true
                }

                override fun isCurrentExtensionModeAvailable(): Boolean {
                    return true
                }

                override fun createSessionProcessor(context: Context): SessionProcessor? {
                    return Camera2ExtensionsSessionProcessor(
                        Collections.emptyList(),
                        extensionMode,
                        this,
                    )
                }
            }
        }

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }
        val cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)
        assertThat(cameraExtensionsInfo.isCurrentExtensionModeAvailable).isTrue()
    }

    @Test
    fun returnsCorrectInitialExtensionStrengthFromCameraExtensionsInfo() = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        val cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)
        if (cameraExtensionsInfo.isExtensionStrengthAvailable) {
            assertThat(cameraExtensionsInfo.extensionStrength!!.value).isEqualTo(100)
        } else {
            assertThat(cameraExtensionsInfo.extensionStrength).isNull()
        }
    }

    private fun checkExtensionAvailabilityAndInit(): CameraSelector {
        extensionsManager = runBlocking { ExtensionsManager.getInstance(context, cameraProvider) }

        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        return extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode,
        )
    }

    @Test
    fun returnsCorrectExtensionStrengthFromCameraExtensionsInfoForNormalMode() = runBlocking {
        // Runs the test only when the parameterized extension mode is BOKEH to avoid wasting time
        assumeTrue(extensionMode == ExtensionMode.BOKEH)
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), baseCameraSelector)
            }

        val cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)
        assertThat(cameraExtensionsInfo.isExtensionStrengthAvailable).isFalse()
        assertThat(cameraExtensionsInfo.extensionStrength).isNull()
    }

    @Test
    fun retrievesCameraExtensionsControlFromCameraControl(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        // Retrieves null CameraExtensionsControl from normal mode camera's CameraControl
        withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), baseCameraSelector)
            }
            .also {
                assertThat(extensionsManager.getCameraExtensionsControl(it.cameraControl)).isNull()
            }

        // Retrieves non-null CameraExtensionsControl from extensions-enabled camera's CameraControl
        withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }
            .also {
                assertThat(extensionsManager.getCameraExtensionsControl(it.cameraControl))
                    .isNotNull()
            }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun canProvideCorrectTypeOfSessionProcessor(): Unit = runBlocking {
        var extensionCameraSelector = checkExtensionAvailabilityAndInit()

        // Get and check the session processor type is correct
        (cameraProvider.getCameraInfo(extensionCameraSelector) as AdapterCameraInfo)
            .sessionProcessor
            ?.also { assertThat(it).isInstanceOf(Camera2ExtensionsSessionProcessor::class.java) }
    }

    private fun isExtensionAvailableByCameraInfo(cameraInfo: CameraInfo): Boolean {
        var vendorExtender = ExtensionsTestUtil.createVendorExtender(context, extensionMode)
        val cameraId = (cameraInfo as CameraInfoInternal).cameraId

        return vendorExtender.isExtensionAvailable(
            cameraId,
            ExtensionsUtils.getCameraCharacteristicsMap(cameraInfo),
        )
    }
}
