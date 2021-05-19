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
import android.os.Build
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.extensions.ExtensionMode.Mode
import androidx.camera.extensions.ExtensionsManager.EffectMode
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
class ExtensionsTest(
    @field:Mode @param:Mode private val extensionMode: Int,
    @field:LensFacing @param:LensFacing private val lensFacing: Int
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private val effectMode: EffectMode =
        ExtensionsTestUtil.extensionModeToEffectMode(extensionMode)

    private lateinit var extensions: Extensions
    private lateinit var cameraProvider: ProcessCameraProvider

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())

        cameraProvider =
            ProcessCameraProvider.getInstance(context).get(10000, TimeUnit.MILLISECONDS)

        assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                lensFacing
            )
        )
        assumeTrue(ExtensionsTestUtil.initExtensions(context))
        extensions = ExtensionsManager.getExtensions(context)
    }

    @After
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        TimeoutException::class
    )
    fun cleanUp() {
        cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        ExtensionsManager.deinit().get(10000, TimeUnit.MILLISECONDS)
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    // TODO: Can be removed after the Extensions class is fully implemented.
    @Test
    fun isExtensionAvailable() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assertThat(ExtensionsManager.isExtensionAvailable(effectMode, lensFacing)).isEqualTo(
            extensions.isExtensionAvailable(cameraProvider, cameraSelector, extensionMode)
        )
    }

    @Test
    fun setExtensionSucceedsIfAvailable() {
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeTrue(
            extensions.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector =
            extensions.getExtensionCameraSelector(baseCameraSelector, extensionMode)

        lateinit var camera: Camera
        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
        }

        assertThat(extensions.getExtension(camera)).isEqualTo(extensionMode)
    }

    @Test
    fun setExtensionFailsIfNotAvailable() {
        val baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        assumeFalse(
            extensions.isExtensionAvailable(
                cameraProvider,
                baseCameraSelector,
                extensionMode
            )
        )

        val extensionCameraSelector =
            extensions.getExtensionCameraSelector(baseCameraSelector, extensionMode)

        instrumentation.runOnMainSync {
            assertThrows<IllegalArgumentException> {
                cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector)
            }
        }
    }
}
