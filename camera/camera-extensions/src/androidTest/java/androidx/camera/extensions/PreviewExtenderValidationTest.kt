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

package androidx.camera.extensions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode.Mode
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType
import androidx.camera.extensions.impl.PreviewImageProcessorImpl
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewExtenderValidationTest(
    @field:Mode @param:Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraId: String
    private lateinit var cameraCharacteristics: CameraCharacteristics

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())
        assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                lensFacing
            )
        )

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                extensionMode
            )
        )

        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraProvider,
            baseCameraSelector,
            extensionMode
        )

        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
        cameraCharacteristics = Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)
    }

    @After
    fun cleanUp() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        fun initParameters(): Collection<Array<Any>> =
            ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    @Test
    fun getSupportedResolutionsImplementationTest() {
        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version.compareTo(Version.VERSION_1_1) >= 0)

        // Creates the ImageCaptureExtenderImpl to retrieve the target format/resolutions pair list
        // from vendor library for the target effect mode.
        val impl = ExtensionsTestUtil.createPreviewExtenderImpl(
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
        val impl = ExtensionsTestUtil.createPreviewExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )
        assertThat(impl.onPresetSession()).isNull()
    }

    @Test
    fun returnCorrectProcessor() {
        val impl = ExtensionsTestUtil.createPreviewExtenderImpl(
            extensionMode,
            cameraId,
            cameraCharacteristics
        )

        when (val processorType = impl.processorType) {
            ProcessorType.PROCESSOR_TYPE_NONE -> assertThat(impl.processor).isNull()
            ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY ->
                assertThat(impl.processor).isInstanceOf(RequestUpdateProcessorImpl::class.java)
            ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR ->
                assertThat(impl.processor).isInstanceOf(PreviewImageProcessorImpl::class.java)
            else ->
                throw IllegalArgumentException("Unexpected ProcessorType: $processorType")
        }
    }
}
