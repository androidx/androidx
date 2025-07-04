/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.extensions.internal

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.impl.ExtensionsTestlibControl
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.extensions.util.ExtensionsTestUtil.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 30)
class AdvancedVendorExtenderTest(
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
    private val implType: ExtensionsTestlibControl.ImplementationType,
    @field:ExtensionMode.Mode @param:ExtensionMode.Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Before
    @Throws(Exception::class)
    fun setUp(): Unit = runBlocking {
        assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(lensFacing, extensionMode)
        )
        assumeTrue(
            implType == ExtensionsTestlibControl.ImplementationType.OEM_IMPL &&
                ExtensionVersion.isAdvancedExtenderSupported()
        )

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        extensionsManager =
            ExtensionsManager.getInstanceAsync(Companion.context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                extensionMode,
            )

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
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

    @Test
    fun getEstimatedCaptureLatencyRange_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when retrieving estimated capture latency range
        createAndInitAdvancedVendorExtender().apply {
            getEstimatedCaptureLatencyRange(null)
            getEstimatedCaptureLatencyRange(SizeUtil.RESOLUTION_1080P)
        }
    }

    @Test
    fun getPreviewOutputResolutions_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when retrieving supported preview output resolutions
        createAndInitAdvancedVendorExtender().supportedPreviewOutputResolutions
    }

    @Test
    fun getCaptureOutputResolutions_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when retrieving supported capture output resolutions
        createAndInitAdvancedVendorExtender().supportedCaptureOutputResolutions
    }

    @Test
    fun getSupportedPostviewResolutions_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when retrieving supported postview output resolutions
        createAndInitAdvancedVendorExtender()
            .getSupportedPostviewResolutions(SizeUtil.RESOLUTION_1080P)
    }

    @Test
    fun invokeIsPostviewAvailable_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when invoking isPostviewAvailable()
        createAndInitAdvancedVendorExtender().isPostviewAvailable
    }

    @Test
    fun invokeIsCaptureProcessProgressAvailable_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when invoking isCaptureProcessProgressAvailable()
        createAndInitAdvancedVendorExtender().isCaptureProcessProgressAvailable
    }

    @Test
    fun invokeIsExtensionStrengthAvailable_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when invoking isExtensionStrengthAvailable()
        createAndInitAdvancedVendorExtender().isExtensionStrengthAvailable
    }

    @Test
    fun invokeIsCurrentExtensionModeAvailable_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when invoking isCurrentExtensionModeAvailable()
        createAndInitAdvancedVendorExtender().isCurrentExtensionModeAvailable
    }

    @Test
    fun getSupportedCaptureResultKeys_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when retrieving supported capture result keys
        createAndInitAdvancedVendorExtender().supportedCaptureResultKeys
    }

    @Test
    fun getAvailableCharacteristicsKeyValues_wontThrowException(): Unit = runBlocking {
        // Checks no exception is thrown when retrieving available characteristics key-values
        // (b/401460276)
        createAndInitAdvancedVendorExtender().availableCharacteristicsKeyValues
    }

    private fun createAndInitAdvancedVendorExtender(): AdvancedVendorExtender = runBlocking {
        var cameraInfo: CameraInfoInternal
        withContext(Dispatchers.Main) {
            cameraInfo =
                cameraProvider
                    .bindToLifecycle(fakeLifecycleOwner, extensionCameraSelector)
                    .cameraInfo as CameraInfoInternal
        }

        val characteristicsMap = ExtensionsUtils.getCameraCharacteristicsMap(cameraInfo)

        val advancedExtender = AdvancedVendorExtender(extensionMode)
        advancedExtender.init(cameraInfo)
        assumeTrue(advancedExtender.isExtensionAvailable(cameraInfo.cameraId, characteristicsMap))

        advancedExtender
    }

    companion object {
        val context: Context = ApplicationProvider.getApplicationContext()

        @JvmStatic
        @Parameterized.Parameters(
            name = "cameraXConfig = {0}, impl = {2}, mode = {3}, facing = {4}"
        )
        fun data(): Collection<Array<Any>> {
            return ExtensionsTestUtil.getAllImplExtensionsLensFacingCombinations(context, true)
        }
    }
}
