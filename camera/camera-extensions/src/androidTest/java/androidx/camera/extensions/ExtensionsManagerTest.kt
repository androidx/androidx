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
import android.hardware.camera2.CaptureRequest
import android.util.Pair
import android.util.Range
import android.util.Size
import androidx.annotation.NonNull
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.RestrictedCameraInfo
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.extensions.impl.ExtensionsTestlibControl
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceConfigurationImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceImpl
import androidx.camera.extensions.impl.advanced.RequestProcessorImpl
import androidx.camera.extensions.impl.advanced.SessionProcessorImpl
import androidx.camera.extensions.internal.ClientVersion
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.extensions.internal.VendorExtender
import androidx.camera.extensions.internal.Version
import androidx.camera.extensions.internal.sessionprocessor.AdvancedSessionProcessor
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.extensions.util.ExtensionsTestUtil.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.video.MediaSpec
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoOutput
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ExtensionsManagerTest(
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
    private val implType: ExtensionsTestlibControl.ImplementationType,
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

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

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        ExtensionsTestlibControl.getInstance().setImplementationType(implType)
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
        @Parameterized.Parameters(
            name = "cameraXConfig = {0}, implType = {2}, mode = {3}, facing = {4}"
        )
        fun data(): Collection<Array<Any>> {
            return ExtensionsTestUtil.getAllImplExtensionsLensFacingCombinations(context, false)
        }
    }

    @Test
    fun getInstanceSuccessfully_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider, ClientVersion("99.0.0"))[
                    10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability !=
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )
        assertThat(extensionsManager).isNotNull()
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider, ClientVersion("99.0.0"))[
                    10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability !=
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assertThrows<IllegalArgumentException> {
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)
        }
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionModeIsNotSupported() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeFalse(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        assertThrows<IllegalArgumentException> {
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)
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
    fun correctAvailability_whenExtensionIsNotAvailable() {
        // Skips the test if extensions availability is disabled by quirk.
        assumeFalse(
            ExtensionsTestUtil.extensionsDisabledByQuirk(
                CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
            )
        )

        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]
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
    fun getEstimatedCaptureLatencyRange_returnValueFromExtender() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(
                    context,
                    cameraProvider,
                )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability ==
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )
        // Skips the test when the extension version is 1.1 or below. It is the case that the
        // device has its own implementation and ExtensionsInfo will directly return null to impact
        // the test result.
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_2)

        val estimatedCaptureLatency = Range(100L, 1000L)

        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ): Boolean {
                    return true
                }

                override fun getEstimatedCaptureLatencyRange(size: Size?): Range<Long> {
                    return estimatedCaptureLatency
                }
            }
        extensionsManager.setVendorExtenderFactory { fakeVendorExtender }

        assertThat(
                extensionsManager.getEstimatedCaptureLatencyRange(baseCameraSelector, extensionMode)
            )
            .isEqualTo(estimatedCaptureLatency)
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnNull_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider, ClientVersion("99.0.0"))[
                    10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability !=
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(
                extensionsManager.getEstimatedCaptureLatencyRange(baseCameraSelector, extensionMode)
            )
            .isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnNull_belowVersion1_2() {
        assumeTrue(ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) < 0)

        checkExtensionAvailabilityAndInit()

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo =
            extensionsManager.getEstimatedCaptureLatencyRange(baseCameraSelector, extensionMode)

        assertThat(latencyInfo).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnsNull_whenNoCameraCanBeFound() {
        checkExtensionAvailabilityAndInit()

        val emptyCameraSelector =
            CameraSelector.Builder().addCameraFilter { _ -> ArrayList<CameraInfo>() }.build()

        assertThat(
                extensionsManager.getEstimatedCaptureLatencyRange(
                    emptyCameraSelector,
                    extensionMode
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
    fun canSwitchExtendedCameraConfig_afterUnbindUseCases(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        withContext(Dispatchers.Main) {
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
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector, FakeUseCase())
        }
    }

    @Test
    fun isImageAnalysisSupportedReturnsFalse_whenHasNoAnalysisSizes() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(
                    context,
                    cameraProvider,
                )[10000, TimeUnit.MILLISECONDS]

        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ): Boolean {
                    return true
                }

                override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
                    return emptyArray()
                }
            }
        extensionsManager.setVendorExtenderFactory { fakeVendorExtender }

        assumeTrue(
            extensionsManager.extensionsAvailability ==
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(baseCameraSelector, extensionMode))
            .isFalse()
    }

    @Test
    fun isImageAnalysisSupportedReturnsTrue_whenHasAnalysisSizes() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(
                    context,
                    cameraProvider,
                )[10000, TimeUnit.MILLISECONDS]

        val fakeVendorExtender =
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ): Boolean {
                    return true
                }

                override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
                    return arrayOf(Size(1920, 1080))
                }
            }
        extensionsManager.setVendorExtenderFactory { fakeVendorExtender }

        assumeTrue(
            extensionsManager.extensionsAvailability ==
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(baseCameraSelector, extensionMode))
            .isTrue()
    }

    @Test
    fun isImageAnalysisSupportedIsFalse_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider, ClientVersion("99.0.0"))[
                    10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability !=
                ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(baseCameraSelector, extensionMode))
            .isFalse()
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
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ): Boolean {
                    return true
                }

                override fun isPostviewAvailable(): Boolean {
                    return true
                }
            }
        extensionsManager.setVendorExtenderFactory { fakeVendorExtender }

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
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ): Boolean {
                    return true
                }

                override fun isCaptureProcessProgressAvailable(): Boolean {
                    return true
                }
            }
        extensionsManager.setVendorExtenderFactory { fakeVendorExtender }

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
                    RestrictedCameraInfo.CAMERA_OPERATION_EXTENSION_STRENGTH
                )
            )
    }

    @Test
    fun returnsCorrectCurrentExtensionTypeAvailabilityFromCameraExtensionsInfo() = runBlocking {
        assumeTrue(ExtensionVersion.isAdvancedExtenderSupported())
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        // Inject fake VendorExtenderFactory to provide custom VendorExtender
        extensionsManager.setVendorExtenderFactory {
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ): Boolean {
                    return true
                }

                override fun isCurrentExtensionModeAvailable(): Boolean {
                    return true
                }

                override fun createSessionProcessor(context: Context): SessionProcessor? {
                    return AdvancedSessionProcessor(
                        FakeSessionProcessorImpl(),
                        Collections.emptyList(),
                        this,
                        context,
                        extensionMode
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
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        return extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
    }

    @Test
    fun returnsCorrectExtensionStrengthFromCameraExtensionsInfoForNormalMode() = runBlocking {
        // Runs the test only when the parameterized extension mode is BOKEH to avoid wasting time
        assumeTrue(extensionMode == ExtensionMode.BOKEH)
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

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

    private fun isExtensionAvailableByCameraInfo(cameraInfo: CameraInfo): Boolean {
        var vendorExtender = ExtensionsTestUtil.createVendorExtender(extensionMode)
        val cameraId = (cameraInfo as CameraInfoInternal).cameraId

        return vendorExtender.isExtensionAvailable(
            cameraId,
            ExtensionsUtils.getCameraCharacteristicsMap(cameraInfo)
        )
    }

    private fun createVideoCapture(): VideoCapture<TestVideoOutput> {
        val mediaSpec = MediaSpec.builder().build()
        val videoOutput = TestVideoOutput()
        videoOutput.mediaSpecObservable.setState(mediaSpec)
        return VideoCapture.withOutput(videoOutput)
    }

    /** A fake implementation of VideoOutput */
    private class TestVideoOutput : VideoOutput {
        val mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(MediaSpec.builder().build())
        var surfaceRequest: SurfaceRequest? = null
        var sourceState: VideoOutput.SourceState? = null

        override fun onSurfaceRequested(@NonNull request: SurfaceRequest) {
            surfaceRequest = request
        }

        override fun getMediaSpec() = mediaSpecObservable

        override fun onSourceStateChanged(@NonNull sourceState: VideoOutput.SourceState) {
            this.sourceState = sourceState
        }
    }

    private class FakeSessionProcessorImpl : SessionProcessorImpl {
        override fun initSession(
            cameraId: String,
            cameraCharacteristicsMap: MutableMap<String, CameraCharacteristics>,
            context: Context,
            surfaceConfigs: OutputSurfaceConfigurationImpl
        ): Camera2SessionConfigImpl = FakeCamera2SessionConfigImpl()

        override fun initSession(
            cameraId: String,
            cameraCharacteristicsMap: MutableMap<String, CameraCharacteristics>,
            context: Context,
            previewSurfaceConfig: OutputSurfaceImpl,
            imageCaptureSurfaceConfig: OutputSurfaceImpl,
            imageAnalysisSurfaceConfig: OutputSurfaceImpl?
        ): Camera2SessionConfigImpl = FakeCamera2SessionConfigImpl()

        override fun deInitSession() {}

        override fun setParameters(parameters: MutableMap<CaptureRequest.Key<*>, Any>) {}

        override fun startTrigger(
            triggers: MutableMap<CaptureRequest.Key<*>, Any>,
            callback: SessionProcessorImpl.CaptureCallback
        ): Int = 0

        override fun onCaptureSessionStart(requestProcessor: RequestProcessorImpl) {}

        override fun onCaptureSessionEnd() {}

        override fun startRepeating(callback: SessionProcessorImpl.CaptureCallback): Int = 0

        override fun stopRepeating() {}

        override fun startCapture(callback: SessionProcessorImpl.CaptureCallback): Int = 0

        override fun startCaptureWithPostview(callback: SessionProcessorImpl.CaptureCallback): Int =
            0

        override fun abortCapture(captureSequenceId: Int) {}

        override fun getRealtimeCaptureLatency(): Pair<Long, Long>? = null
    }

    private class FakeCamera2SessionConfigImpl : Camera2SessionConfigImpl {
        override fun getOutputConfigs(): MutableList<Camera2OutputConfigImpl> = mutableListOf()

        override fun getSessionParameters(): MutableMap<CaptureRequest.Key<*>, Any> = mutableMapOf()

        override fun getSessionTemplateId(): Int = 0

        override fun getSessionType(): Int = 0
    }
}
