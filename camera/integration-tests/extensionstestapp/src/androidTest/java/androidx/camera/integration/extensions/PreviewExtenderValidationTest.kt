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
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType
import androidx.camera.extensions.impl.PreviewImageProcessorImpl
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.CameraXExtensionTestParams
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
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

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewExtenderValidationTest(private val config: CameraXExtensionTestParams) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = config.implName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(config.cameraXConfig)
        )

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        assumeTrue(!ExtensionVersion.isAdvancedExtenderSupported())

        val (_, cameraXConfig, cameraId, extensionMode) = config
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(baseCameraSelector, extensionMode)

        val camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }

        cameraCharacteristics =
            (camera.cameraInfo as CameraInfoInternal).cameraCharacteristics as CameraCharacteristics
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraXExtensionTestParams>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Test
    fun getSupportedResolutionsImplementationTest() {
        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version.compareTo(Version.VERSION_1_1) >= 0)

        // Creates the ImageCaptureExtenderImpl to retrieve the target format/resolutions pair list
        // from vendor library for the target effect mode.
        val impl =
            CameraXExtensionsTestUtil.createPreviewExtenderImpl(
                config.extensionMode,
                config.cameraId,
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
        val impl =
            CameraXExtensionsTestUtil.createPreviewExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )
        assertThat(impl.onPresetSession()).isNull()
    }

    @Test
    fun returnCorrectProcessor() {
        val impl =
            CameraXExtensionsTestUtil.createPreviewExtenderImpl(
                config.extensionMode,
                config.cameraId,
                cameraCharacteristics
            )

        when (val processorType = impl.processorType) {
            ProcessorType.PROCESSOR_TYPE_NONE -> assertThat(impl.processor).isNull()
            ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY ->
                assertThat(impl.processor).isInstanceOf(RequestUpdateProcessorImpl::class.java)
            ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR ->
                assertThat(impl.processor).isInstanceOf(PreviewImageProcessorImpl::class.java)
            else -> throw IllegalArgumentException("Unexpected ProcessorType: $processorType")
        }
    }
}
