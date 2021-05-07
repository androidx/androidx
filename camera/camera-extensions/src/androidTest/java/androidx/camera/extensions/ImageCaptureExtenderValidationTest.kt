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
import android.hardware.camera2.CameraAccessException
import android.os.Build
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode.Mode
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@SmallTest
@RunWith(Parameterized::class)
class ImageCaptureExtenderValidationTest(
    @field:Mode @param:Mode private val extensionMode: Int,
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int
) {
    private val context =
        ApplicationProvider.getApplicationContext<Context>()

    private val effectMode: ExtensionsManager.EffectMode =
        ExtensionsTestUtil.extensionModeToEffectMode(extensionMode)

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var extensionsManager: ExtensionsManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ExtensionsTestUtil.assumeCompatibleDevice()
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        Assume.assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                lensFacing
            )
        )

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        Assume.assumeTrue(
            extensionsManager.isExtensionAvailable(
                cameraProvider,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                extensionMode
            )
        )
    }

    @After
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        TimeoutException::class
    )
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
    @Throws(
        CameraInfoUnavailableException::class,
        CameraAccessException::class
    )
    fun getSupportedResolutionsImplementationTest() {
        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        Assume.assumeTrue(version != null && version.compareTo(Version.VERSION_1_1) >= 0)

        // Creates the ImageCaptureExtenderImpl to retrieve the target format/resolutions pair list
        // from vendor library for the target effect mode.
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(effectMode, lensFacing)

        // NoSuchMethodError will be thrown if getSupportedResolutions is not implemented in
        // vendor library, and then the test will fail.
        impl.supportedResolutions
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
    @Throws(
        CameraInfoUnavailableException::class,
        CameraAccessException::class
    )
    fun returnsNullFromOnPresetSession_whenAPILevelOlderThan28() {
        // Creates the ImageCaptureExtenderImpl to check that onPresetSession() returns null when
        // API level is older than 28.
        val impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(effectMode, lensFacing)
        assertThat(impl.onPresetSession()).isNull()
    }
}
