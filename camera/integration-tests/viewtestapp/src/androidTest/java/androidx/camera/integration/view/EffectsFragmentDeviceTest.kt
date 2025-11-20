/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.view

import android.net.Uri
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.integration.view.TestUtil.assertPreviewStreamingState
import androidx.camera.integration.view.TestUtil.getFragment
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.view.PreviewView
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Instrument tests for [EffectsFragment]. */
@LargeTest
@RunWith(Parameterized::class)
class EffectsFragmentDeviceTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val useCameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraControllerFragmentTest.testCameraRule,
            CameraUtil.PreTestCameraIdList(cameraConfig),
        )

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
        )
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fragment: EffectsFragment
    private lateinit var fragmentScenario: FragmentScenario<EffectsFragment>

    @Before
    fun setup() {
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10000, TimeUnit.MILLISECONDS]
        fragmentScenario =
            FragmentScenario.launchInContainer(
                EffectsFragment::class.java,
                null,
                R.style.AppTheme,
                null,
            )
        fragment = fragmentScenario.getFragment()
    }

    @After
    fun tearDown() {
        if (::fragmentScenario.isInitialized) {
            fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
        }
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun launchFragment_surfaceProcessorIsActive() {
        // Arrange.
        fragment.assertPreviewStreamingState(PreviewView.StreamState.STREAMING, instrumentation)
        // Assert.
        assertThat(fragment.getSurfaceProcessor().isSurfaceRequestedAndProvided()).isTrue()
    }

    @Test
    fun takePicture_imageEffectInvoked() {
        // Arrange.
        fragment.run {
            assertPreviewStreamingState(PreviewView.StreamState.STREAMING, instrumentation)
            // Act.
            assertCanTakePicture()
        }
        // Assert.
        assertThat(fragment.getImageEffect()!!.isInvoked()).isTrue()
    }

    @Test
    fun shareToImageCapture_canTakePicture() {
        // Act.
        instrumentation.runOnMainSync { fragment.surfaceEffectForImageCapture.isChecked = true }
        // Assert.
        fragment.assertPreviewStreamingState(PreviewView.StreamState.STREAMING, instrumentation)
        fragment.assertCanTakePicture()
        assertThat(fragment.getImageEffect()).isNull()
    }

    private fun EffectsFragment.assertCanTakePicture() {
        val imageCallbackSemaphore = Semaphore(0)
        var uri: Uri? = null
        instrumentation.runOnMainSync {
            this.takePicture(
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        uri = outputFileResults.savedUri
                        imageCallbackSemaphore.release()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.d(TAG, "onError", exception)
                        imageCallbackSemaphore.release()
                    }
                }
            )
        }
        assertThat(imageCallbackSemaphore.tryAcquire(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .isTrue()
        assertThat(uri).isNotNull()
    }

    companion object {
        private const val TAG = "EffectsFragmentDeviceTest"
        private const val CAPTURE_TIMEOUT_SECONDS = 45L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }
}
