/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.camera.testing.waitForIdle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingResource
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
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

private const val BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.extensions"

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureExtenderValidationTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector

    private lateinit var activityScenario: ActivityScenario<CameraExtensionsActivity>

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(ExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )

        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        cameraCharacteristics = Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::activityScenario.isInitialized) {
            activityScenario.onActivity { it.finish() }
        }

        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Test
    fun getSupportedResolutionsImplementationTest() {
        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version.compareTo(Version.VERSION_1_1) >= 0)

        // Creates the ImageCaptureExtenderImpl to retrieve the target format/resolutions pair list
        // from vendor library for the target effect mode.
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )

        // NoSuchMethodError will be thrown if getSupportedResolutions is not implemented in
        // vendor library, and then the test will fail.
        impl.supportedResolutions
    }

    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = Build.VERSION_CODES.O_MR1)
    fun returnsNullFromOnPresetSession_whenAPILevelOlderThan28() {
        // Creates the ImageCaptureExtenderImpl to check that onPresetSession() returns null when
        // API level is older than 28.
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )
        assertThat(impl.onPresetSession()).isNull()
    }

    @Test
    fun getEstimatedCaptureLatencyRangeSameAsImplClass_aboveVersion1_2(): Unit = runBlocking {
        assumeTrue(
            ExtensionVersion.getRuntimeVersion()!!.compareTo(Version.VERSION_1_2) >= 0
        )

        // This call should not cause any exception even if the vendor library doesn't implement
        // the getEstimatedCaptureLatencyRange function.
        val latencyInfo = extensionsManager.getEstimatedCaptureLatencyRange(
            baseCameraSelector,
            extensionMode
        )

        // Calls bind to lifecycle to get the selected camera
        var camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        val cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
        val characteristics = Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)

        // Creates ImageCaptureExtenderImpl directly to retrieve the capture latency range info
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
            extensionMode,
            cameraId,
            characteristics
        )
        val expectedLatencyInfo = impl.getEstimatedCaptureLatencyRange(null)

        // Compares the values obtained from ExtensionsManager and ImageCaptureExtenderImpl are
        // the same.
        assertThat(latencyInfo).isEqualTo(expectedLatencyInfo)
    }

    @LargeTest
    @Test
    fun returnCaptureStages_whenCaptureProcessorIsNotNull(): Unit = runBlocking {
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())

        val intent = ApplicationProvider.getApplicationContext<Context>().packageManager
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)?.apply {
                putExtra(CameraExtensionsActivity.INTENT_EXTRA_CAMERA_ID, cameraId)
                putExtra(CameraExtensionsActivity.INTENT_EXTRA_EXTENSION_MODE, extensionMode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        activityScenario = ActivityScenario.launch(intent)
        var initializationIdlingResource: IdlingResource? = null
        var previewViewIdlingResource: IdlingResource? = null

        activityScenario.onActivity {
            initializationIdlingResource = it.mInitializationIdlingResource
            previewViewIdlingResource = it.mPreviewViewIdlingResource
        }

        // Wait for CameraExtensionsActivity's initialization to be complete
        initializationIdlingResource.waitForIdle()

        activityScenario.onActivity {
            assumeTrue(it.isExtensionModeSupported(cameraId, extensionMode))

            // Retrieves the CaptureProcessor from ImageCapture's config
            val captureProcessor =
                it.imageCapture!!.currentConfig.retrieveOption(
                    ImageCaptureConfig.OPTION_CAPTURE_PROCESSOR, null
                )

            // Only runs the test when CaptureProcessor is not null
            assumeTrue(captureProcessor != null)
        }

        // Waits for preview view turned to STREAMING state to make sure that the capture session
        // has been created and the capture stages can be retrieved from the vendor library
        // successfully.
        previewViewIdlingResource.waitForIdle()

        activityScenario.onActivity {
            // Checks that CameraExtensionsActivity's current extension mode is correct.
            assertThat(it.currentExtensionMode).isEqualTo(extensionMode)

            // Retrieves the CaptureBundle from ImageCapture's config
            val captureBundle =
                it.imageCapture!!.currentConfig.retrieveOption(
                    ImageCaptureConfig.OPTION_CAPTURE_BUNDLE
                )

            // Calls CaptureBundle#getCaptureStages() will call
            // ImageCaptureExtenderImpl#getCaptureStages(). Checks the returned value is not empty.
            assertThat(captureBundle!!.captureStages).isNotEmpty()
        }
    }
}
