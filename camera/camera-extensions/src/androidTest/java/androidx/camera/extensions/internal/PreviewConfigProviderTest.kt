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

package androidx.camera.extensions.internal

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionSessionConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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

@MediumTest
@RunWith(AndroidJUnit4::class)
class PreviewConfigProviderTest {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())

        cameraProvider = ProcessCameraProvider.awaitInstance(context)
        extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 33)
    fun canSetSupportedResolutionsToConfigTest(): Unit = runBlocking {
        val cameraXExtensionMode = ExtensionMode.NIGHT
        assumeTrue(
            ExtensionsTestUtil.isExtensionAvailable(
                extensionsManager,
                cameraSelector.lensFacing!!,
                cameraXExtensionMode,
            )
        )
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val camera2ExtensionMode =
            Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode(cameraXExtensionMode)
        val expectedPrivOutputSizes =
            cameraManager
                .getCameraExtensionCharacteristics(
                    CameraUtil.getCameraIdWithLensFacing(cameraSelector.lensFacing!!)!!
                )
                .getExtensionSupportedSizes(camera2ExtensionMode, SurfaceTexture::class.java)

        val preview = Preview.Builder().build()

        val sessionConfig = ExtensionSessionConfig(cameraXExtensionMode, extensionsManager, preview)

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, sessionConfig)
        }

        val resultFormatResolutionsPairList =
            (preview.currentConfig as ImageOutputConfig).supportedResolutions

        // Checks the result and target pair lists are the same
        for (resultPair in resultFormatResolutionsPairList) {
            if (resultPair.first == ImageFormat.PRIVATE) {
                assertThat(resultPair.second.toList())
                    .containsExactlyElementsIn(expectedPrivOutputSizes.toTypedArray())
            }
        }
    }
}
