/*
 * Copyright 2024 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.integration.view.CameraControllerFragmentTest.Companion.TIMEOUT_SECONDS
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.CameraController.VIDEO_CAPTURE
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
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Instrument tests for [Media3EffectsFragment]. */
@LargeTest
@RunWith(Parameterized::class)
class Media3EffectFragmentDeviceTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val useCameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            testCameraRule,
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fragment: Media3EffectsFragment
    private lateinit var fragmentScenario: FragmentScenario<Media3EffectsFragment>

    @Before
    fun setup() {
        CoreAppTestUtil.assumeCompatibleDevice()
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10000, TimeUnit.MILLISECONDS]
        fragmentScenario = createFragmentScenario()
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
    fun takePictureWithMaxBrightness_resultIsWhite() {
        adjustSliderAndAssertColor(MAX_PROGRESS, Color.WHITE)
    }

    @Test
    fun takePictureWithMinBrightness_resultIsBlack() {
        adjustSliderAndAssertColor(MIN_PROGRESS, Color.BLACK)
    }

    @SuppressLint("BanThreadSleep")
    private fun adjustSliderAndAssertColor(progress: Int, color: Int) {
        // Skip for emulators.
        assumeFalse(AndroidUtil.isEmulator())
        // Arrange.
        instrumentation.runOnMainSync {
            fragment.cameraController.setEnabledUseCases(VIDEO_CAPTURE or IMAGE_CAPTURE)
        }
        fragment.assertPreviewStreaming()

        // Act.
        instrumentation.runOnMainSync { fragment.slider.progress = progress }
        Thread.sleep(SET_EFFECT_DELAY_MILLIS)

        // Assert: Take a picture and assert the color.
        val bitmap = takePictureAsBitmap()

        assertThat(bitmap).isNotNull()
        assertThat(getAverageDiff(bitmap!!, Rect(0, 0, bitmap.width, bitmap.height), color))
            .isEqualTo(0)
    }

    private fun takePictureAsBitmap(): Bitmap? {
        val imageCallbackSemaphore = Semaphore(0)
        var bitmap: Bitmap? = null
        instrumentation.runOnMainSync {
            fragment.cameraController.takePicture(
                mainThreadExecutor(),
                object : OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        bitmap = image.toBitmap()
                        image.close()
                        // Unblock the test
                        imageCallbackSemaphore.release()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        imageCallbackSemaphore.release()
                    }
                }
            )
        }
        assertThat(imageCallbackSemaphore.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        return bitmap
    }

    private fun Media3EffectsFragment.assertPreviewStreaming() {
        val previewStreaming = Semaphore(0)
        instrumentation.runOnMainSync {
            previewView.previewStreamState.observe(this) {
                if (it == PreviewView.StreamState.STREAMING) {
                    previewStreaming.release()
                }
            }
        }
        assertThat(previewStreaming.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun createFragmentScenario(): FragmentScenario<Media3EffectsFragment> {
        return FragmentScenario.launchInContainer(
            Media3EffectsFragment::class.java,
            null,
            R.style.AppTheme,
            null
        )
    }

    private fun FragmentScenario<Media3EffectsFragment>.getFragment(): Media3EffectsFragment {
        var fragment: Media3EffectsFragment? = null
        this.onFragment { newValue: Media3EffectsFragment -> fragment = newValue }
        return fragment!!
    }

    companion object {
        @JvmField val testCameraRule = CameraUtil.PreTestCamera()

        const val MAX_PROGRESS = 100
        const val MIN_PROGRESS = 100

        // Timeout for waiting the effect to be effective.
        const val SET_EFFECT_DELAY_MILLIS = 200L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }
}
