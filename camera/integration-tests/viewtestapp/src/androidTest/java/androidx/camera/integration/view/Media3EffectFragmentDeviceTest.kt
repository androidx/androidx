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

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors.ioExecutor
import androidx.camera.integration.view.CameraControllerFragmentTest.Companion.TIMEOUT_SECONDS
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.view.CameraController.IMAGE_CAPTURE
import androidx.camera.view.CameraController.VIDEO_CAPTURE
import androidx.camera.view.PreviewView
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
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
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val requireForegroundRule = RequireForegroundRule {
        assumeFalse("Test fails on API 24 emulator (b/539514196)", AndroidUtil.isEmulator(24))
        CoreAppTestUtil.assumeCompatibleDevice()
    }

    @get:Rule
    val useCameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            testCameraRule,
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
    private lateinit var fragment: Media3EffectsFragment
    private lateinit var fragmentScenario: FragmentScenario<Media3EffectsFragment>

    @Before
    fun setup() {
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10000, TimeUnit.MILLISECONDS]
        requireForegroundRule.deferCleanup {
            try {
                if (::fragmentScenario.isInitialized) {
                    fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
                }
            } finally {
                if (::cameraProvider.isInitialized) {
                    cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
                }
            }
        }
        fragmentScenario = createFragmentScenario()
        fragment = fragmentScenario.getFragment()
    }

    @Test
    fun takePictureWithMaxBrightness_resultIsWhite() {
        adjustSliderAndAssertColor(MAX_PROGRESS, Color.WHITE)
    }

    @Test
    fun takePictureWithMinBrightness_resultIsBlack() {
        adjustSliderAndAssertColor(MIN_PROGRESS, Color.BLACK)
    }

    private fun adjustSliderAndAssertColor(progress: Int, color: Int) {
        // Skip for emulators.
        assumeFalse(AndroidUtil.isEmulator())
        fragment.cameraController.initializationFuture[TIMEOUT_SECONDS, TimeUnit.SECONDS]

        // Arrange: configure slider before enabling IMAGE_CAPTURE so Media3 initializes with the
        // target effect active on its initial input stream, then wait for PreviewView to stream.
        instrumentation.runOnMainSync {
            fragment.slider.progress = progress
            fragment.cameraController.setEnabledUseCases(VIDEO_CAPTURE or IMAGE_CAPTURE)
        }
        instrumentation.waitForIdleSync()
        fragment.assertPreviewStreaming()

        // Assert: Take a picture and assert the color.
        val bitmap = takePictureAsBitmap()
        try {
            assertThat(getAverageColorDiff(bitmap, color)).isEqualTo(0)
        } finally {
            bitmap.recycle()
        }
    }

    private fun takePictureAsBitmap(): Bitmap {
        val imageCallbackSemaphore = Semaphore(0)
        val captureErrorRef = AtomicReference<ImageCaptureException?>()
        var bitmap: Bitmap? = null
        instrumentation.runOnMainSync {
            fragment.cameraController.takePicture(
                ioExecutor(),
                object : OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        image.use { imageProxy ->
                            bitmap = imageProxy.toBitmap()
                            // Unblock the test
                            imageCallbackSemaphore.release()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        captureErrorRef.set(exception)
                        imageCallbackSemaphore.release()
                    }
                },
            )
        }
        assertWithMessage("Timed out waiting for image capture callback")
            .that(imageCallbackSemaphore.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .isTrue()
        assertWithMessage("Image capture failed").that(captureErrorRef.get()).isNull()
        return checkNotNull(bitmap)
    }

    private fun Media3EffectsFragment.assertPreviewStreaming() {
        val previewStreaming = Semaphore(0)
        val observer =
            Observer<PreviewView.StreamState> { state ->
                if (state == PreviewView.StreamState.STREAMING) {
                    previewStreaming.release()
                }
            }
        instrumentation.runOnMainSync {
            previewView.previewStreamState.observe(viewLifecycleOwner, observer)
        }
        try {
            val isStreaming = previewStreaming.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!isStreaming) {
                var diagnosticState = ""
                instrumentation.runOnMainSync {
                    diagnosticState =
                        "lifecycle=${lifecycle.currentState}, " +
                            "streamState=${previewView.previewStreamState.value}, " +
                            "windowFocus=${previewView.hasWindowFocus()}"
                }
                assertWithMessage("Timed out waiting for PreviewView STREAMING ($diagnosticState)")
                    .that(isStreaming)
                    .isTrue()
            }
        } finally {
            instrumentation.runOnMainSync {
                previewView.previewStreamState.removeObserver(observer)
            }
        }
    }

    private fun getAverageColorDiff(bitmap: Bitmap, expectedColor: Int): Int {
        require(bitmap.width > 0 && bitmap.height > 0) {
            "Bitmap dimensions must be positive: ${bitmap.width}x${bitmap.height}"
        }
        val expectedR = (expectedColor shr 16) and 0xFF
        val expectedG = (expectedColor shr 8) and 0xFF
        val expectedB = expectedColor and 0xFF
        val width = bitmap.width
        val height = bitmap.height
        val rowPixels = IntArray(width)
        var totalDiff = 0L
        for (y in 0 until height) {
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
            for (pixel in rowPixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalDiff +=
                    ((abs(r - expectedR) + abs(g - expectedG) + abs(b - expectedB)) / 3).toLong()
            }
        }
        return (totalDiff / (width.toLong() * height.toLong())).toInt()
    }

    private fun createFragmentScenario(): FragmentScenario<Media3EffectsFragment> {
        val fragmentArgs =
            Bundle().apply {
                putString(Media3EffectsFragment.ARG_EFFECT, Media3EffectsFragment.EFFECT_BRIGHTNESS)
            }
        return FragmentScenario.launchInContainer(
            Media3EffectsFragment::class.java,
            fragmentArgs,
            R.style.AppTheme,
            null,
        )
    }

    private fun FragmentScenario<Media3EffectsFragment>.getFragment(): Media3EffectsFragment {
        var fragment: Media3EffectsFragment? = null
        this.onFragment { newValue: Media3EffectsFragment -> fragment = newValue }
        return checkNotNull(fragment)
    }

    companion object {
        @JvmField val testCameraRule = CameraUtil.PreTestCamera()

        const val MAX_PROGRESS = 200
        const val MIN_PROGRESS = 0

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }
}
