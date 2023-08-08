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

import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Size
import androidx.annotation.NonNull
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.extensions.internal.ClientVersion
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.VendorExtender
import androidx.camera.extensions.internal.Version
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.video.MediaSpec
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoOutput
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
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
@SdkSuppress(minSdkVersion = 21)
class ExtensionsManagerTest(
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    private lateinit var baseCameraSelector: CameraSelector

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(
                lensFacing,
                extensionMode
            )
        )

        cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                lensFacing
            )
        )

        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    @After
    fun teardown(): Unit = runBlocking {
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
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
            ClientVersion("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )
        assertThat(extensionsManager).isNotNull()
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
            ClientVersion("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assertThrows<IllegalArgumentException> {
            extensionsManager.getExtensionEnabledCameraSelector(
                baseCameraSelector,
                extensionMode
            )
        }
    }

    @Test
    fun getExtensionsCameraSelectorThrowsException_whenExtensionModeIsNotSupported() {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeFalse(
            extensionsManager.isExtensionAvailable(
                baseCameraSelector,
                extensionMode
            )
        )

        assertThrows<IllegalArgumentException> {
            extensionsManager.getExtensionEnabledCameraSelector(
                baseCameraSelector,
                extensionMode
            )
        }
    }

    @Test
    fun returnNewCameraSelector_whenExtensionModeIsSupprted() {
        checkExtensionAvailabilityAndInit()

        val resultCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
        assertThat(resultCameraSelector).isNotNull()
        assertThat(resultCameraSelector).isNotEqualTo(baseCameraSelector)
    }

    @Test
    fun correctAvailability_whenExtensionIsNotAvailable() {
        // Skips the test if extensions availability is disabled by quirk.
        assumeFalse(ExtensionsTestUtil.extensionsDisabledByQuirk())

        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeFalse(
            extensionsManager.isExtensionAvailable(
                baseCameraSelector,
                extensionMode
            )
        )

        for (cameraInfo in cameraProvider.availableCameraInfos) {
            val characteristics = Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)

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
            val characteristics = Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)

            // Checks lens facing first
            val currentLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (currentLensFacing != lensFacing) {
                continue
            }

            // Checks whether the specified extension mode is available by camera info
            val isSupported = isExtensionAvailableByCameraInfo(cameraInfo)
            val currentCameraId = (cameraInfo as CameraInfoInternal).cameraId

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
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                == ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )
        // Skips the test when the extension version is 1.1 or below. It is the case that the
        // device has its own implementation and ExtensionsInfo will directly return null to impact
        // the test result.
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_2)

        val estimatedCaptureLatency = Range(100L, 1000L)

        val fakeVendorExtender = object : VendorExtender {
            override fun isExtensionAvailable(
                cameraId: String,
                characteristicsMap: MutableMap<String, CameraCharacteristics>
            ): Boolean {
                return true
            }

            override fun getEstimatedCaptureLatencyRange(size: Size?): Range<Long>? {
                return estimatedCaptureLatency
            }
        }
        extensionsManager.setVendorExtenderFactory {
            fakeVendorExtender
        }

        assertThat(extensionsManager.getEstimatedCaptureLatencyRange(
            baseCameraSelector,
            extensionMode)
        ).isEqualTo(estimatedCaptureLatency)
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnNull_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
            ClientVersion("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.getEstimatedCaptureLatencyRange(
                baseCameraSelector,
                extensionMode)
        ).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnNull_belowVersion1_2() {
        assumeTrue(
            ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) < 0
        )

        checkExtensionAvailabilityAndInit()

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo = extensionsManager.getEstimatedCaptureLatencyRange(
            baseCameraSelector,
            extensionMode
        )

        assertThat(latencyInfo).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeReturnsNull_whenNoCameraCanBeFound() {
        checkExtensionAvailabilityAndInit()

        val emptyCameraSelector = CameraSelector.Builder()
            .addCameraFilter { _ -> ArrayList<CameraInfo>() }
            .build()

        assertThat(
            extensionsManager.getEstimatedCaptureLatencyRange(
                emptyCameraSelector,
                extensionMode
            )
        ).isNull()
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
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                baseCameraSelector,
                FakeUseCase()
            )
        }
    }

    @Test
    fun throwIllegalArgumentException_whenBindingVideoCapture(): Unit = runBlocking {
        val extensionCameraSelector = checkExtensionAvailabilityAndInit()

        withContext(Dispatchers.Main) {
            val fakeLifecycleOwner = FakeLifecycleOwner()

            assertThrows<IllegalArgumentException> {
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    extensionCameraSelector,
                    createVideoCapture()
                )
            }
        }
    }

    @Test
    fun isImageAnalysisSupportedReturnsFalse_whenHasNoAnalysisSizes() {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
        )[10000, TimeUnit.MILLISECONDS]

        val fakeVendorExtender = object : VendorExtender {
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
        extensionsManager.setVendorExtenderFactory {
            fakeVendorExtender
        }

        assumeTrue(
            extensionsManager.extensionsAvailability
                == ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(
            baseCameraSelector,
            extensionMode)
        ).isFalse()
    }

    @Test
    fun isImageAnalysisSupportedReturnsTrue_whenHasAnalysisSizes() {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
        )[10000, TimeUnit.MILLISECONDS]

        val fakeVendorExtender = object : VendorExtender {
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
        extensionsManager.setVendorExtenderFactory {
            fakeVendorExtender
        }

        assumeTrue(
            extensionsManager.extensionsAvailability
                == ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(
            baseCameraSelector,
            extensionMode)
        ).isTrue()
    }
    @Test
    fun isImageAnalysisSupportedIsFalse_whenExtensionAvailabilityIsNotAvailable() {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider,
            ClientVersion("99.0.0")
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.extensionsAvailability
                != ExtensionsManager.ExtensionsAvailability.LIBRARY_AVAILABLE
        )

        assertThat(extensionsManager.isImageAnalysisSupported(
            baseCameraSelector,
            extensionMode)
        ).isFalse()
    }

    @Test
    fun isImageAnalysisSupportedIsFalse_whenNoCameraCanBeFound() {
        checkExtensionAvailabilityAndInit()
        val emptyCameraSelector = CameraSelector.Builder()
            .addCameraFilter { _ -> ArrayList<CameraInfo>() }
            .build()

        assertThat(
            extensionsManager.isImageAnalysisSupported(
                emptyCameraSelector,
                extensionMode
            )
        ).isFalse()
    }

    private fun checkExtensionAvailabilityAndInit(): CameraSelector {
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        assumeTrue(
            extensionsManager.isExtensionAvailable(
                baseCameraSelector,
                extensionMode
            )
        )

        return extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
    }

    private fun isExtensionAvailableByCameraInfo(cameraInfo: CameraInfo): Boolean {
        var vendorExtender = ExtensionsTestUtil.createVendorExtender(extensionMode)
        vendorExtender.init(cameraInfo)
        val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
        val cameraId = camera2CameraInfo.cameraId

        return vendorExtender.isExtensionAvailable(cameraId,
            camera2CameraInfo.cameraCharacteristicsMap)
    }

    private fun createVideoCapture(): VideoCapture<TestVideoOutput> {
        val mediaSpec = MediaSpec.builder().build()
        val videoOutput = TestVideoOutput()
        videoOutput.mediaSpecObservable.setState(mediaSpec)
        return VideoCapture.withOutput(videoOutput)
    }

    /** A fake implementation of VideoOutput  */
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
}
