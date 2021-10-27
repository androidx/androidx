
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

package androidx.camera.integration.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.view.CameraController.TAP_TO_FOCUS_FAILED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_FOCUSED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_NOT_FOCUSED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_NOT_STARTED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_STARTED
import androidx.camera.view.PreviewView
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

const val TIMEOUT_SECONDS = 3L

/**
 * Instrument tests for [CameraControllerFragment].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraControllerFragmentTest {

    companion object {
        // The right shift needed to get color component from a Int color, in the order of R, G
        // and B.
        private val RGB_SHIFTS = ImmutableList.of(/*R*/16, /*G*/ 8, /*B*/0)
        private const val COLOR_MASK = 0xFF

        // The minimum luminance for comparing pictures. Arbitrarily chosen.
        private const val MIN_LUMINANCE = 50F

        @JvmField
        val testCameraRule = CameraUtil.PreTestCamera()
    }

    @get:Rule
    val thrown: ExpectedException = ExpectedException.none()

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest(testCameraRule)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fragment: CameraControllerFragment
    private lateinit var fragmentScenario: FragmentScenario<CameraControllerFragment>
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        cameraProvider = ProcessCameraProvider.getInstance(
            ApplicationProvider.getApplicationContext()
        )[10000, TimeUnit.MILLISECONDS]
        fragmentScenario = createFragmentScenario()
        fragment = fragmentScenario.getFragment()
        uiDevice = UiDevice.getInstance(instrumentation)
    }

    @After
    fun tearDown() {
        if (::fragmentScenario.isInitialized) {
            fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
        }

        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun controllerBound_canGetCameraControl() {
        fragment.assertPreviewIsStreaming()
        instrumentation.runOnMainSync {
            assertThat(fragment.cameraController.cameraControl).isNotNull()
        }
    }

    @Test
    fun onPreviewViewTapped_previewIsFocused() {
        Assume.assumeFalse(
            "Ignore Cuttlefish",
            Build.MODEL.contains("Cuttlefish")
        )
        // Arrange: listens to LiveData updates.
        fragment.assertPreviewIsStreaming()
        val focused = Semaphore(0)
        var started = false
        var finalState = TAP_TO_FOCUS_NOT_STARTED
        instrumentation.runOnMainSync {
            fragment.cameraController.tapToFocusState.observe(
                fragment,
                {
                    // Make sure the LiveData receives STARTED first and then another update.
                    if (it == TAP_TO_FOCUS_STARTED) {
                        started = true
                        return@observe
                    }
                    if (started) {
                        finalState = it
                        focused.release()
                    }
                }
            )
        }

        // Act: click PreviewView.
        val previewViewId = "androidx.camera.integration.view:id/preview_view"
        uiDevice.findObject(UiSelector().resourceId(previewViewId)).click()

        // Assert: got a LiveData update
        assertThat(focused.tryAcquire(6 /* focus time out is 5s */, TimeUnit.SECONDS)).isTrue()
        assertThat(finalState).isAnyOf(
            TAP_TO_FOCUS_FOCUSED,
            TAP_TO_FOCUS_FAILED,
            TAP_TO_FOCUS_NOT_FOCUSED
        )
    }

    @Test
    fun controllerBound_canGetCameraInfo() {
        fragment.assertPreviewIsStreaming()
        instrumentation.runOnMainSync {
            assertThat(fragment.cameraController.cameraInfo).isNotNull()
        }
    }

    @UiThreadTest
    @Test
    fun controllerNotBound_cameraInfoIsNull() {
        fragment.previewView.controller = null
        assertThat(fragment.cameraController.cameraInfo).isNull()
    }

    @Test
    fun controllerHasCameraResult_sameAsUtilResult() {
        fragment.assertPreviewIsStreaming()
        instrumentation.runOnMainSync {
            assertThat(fragment.cameraController.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                .isEqualTo(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
            assertThat(fragment.cameraController.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
                .isEqualTo(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        }
    }

    @Test
    fun fragmentLaunch_cameraInitializationCompletes() {
        val semaphore = Semaphore(0)
        Futures.addCallback(
            fragment.cameraController.initializationFuture,
            object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                    semaphore.release()
                }

                override fun onFailure(t: Throwable) {}
            },
            CameraXExecutors.directExecutor()
        )
        assertThat(semaphore.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun fragmentLaunch_receiveAnalysisFrames() {
        fragment.assertAnalysisStreaming(true)
    }

    @Test
    fun imageAnalysisDisabled_isNotStreaming() {
        fragment.assertAnalysisStreaming(true)

        onView(withId(R.id.analysis_enabled)).perform(click())

        fragment.assertAnalysisStreaming(false)
    }

    @Test
    fun imageAnalysisDisabledAndEnabled_isStreaming() {
        fragment.assertAnalysisStreaming(true)

        onView(withId(R.id.analysis_enabled)).perform(click())
        onView(withId(R.id.analysis_enabled)).perform(click())

        fragment.assertAnalysisStreaming(true)
    }

    @Ignore
    @Test
    fun analyzerCleared_isNotStreaming() {
        fragment.assertAnalysisStreaming(true)

        instrumentation.runOnMainSync {
            fragment.cameraController.clearImageAnalysisAnalyzer()
        }

        fragment.assertAnalysisStreaming(false)
    }

    @Test
    fun canSetAnalysisImageDepth() {
        var currentDepth = 0

        // Act.
        instrumentation.runOnMainSync {
            currentDepth = fragment.cameraController.imageAnalysisImageQueueDepth
            fragment.cameraController.imageAnalysisImageQueueDepth = currentDepth + 1
        }
        fragment.assertAnalysisStreaming(true)

        // Assert.
        instrumentation.runOnMainSync {
            assertThat(fragment.cameraController.imageAnalysisImageQueueDepth)
                .isEqualTo(currentDepth + 1)
        }
    }

    @Test
    fun canSetAnalysisBackpressureStrategy() {
        // Act.
        instrumentation.runOnMainSync {
            fragment.cameraController.imageAnalysisBackpressureStrategy =
                ImageAnalysis.STRATEGY_BLOCK_PRODUCER
        }
        fragment.assertAnalysisStreaming(true)

        // Assert.
        instrumentation.runOnMainSync {
            assertThat(fragment.cameraController.imageAnalysisBackpressureStrategy)
                .isEqualTo(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
        }
    }

    @Test
    @Ignore
    fun capturedImage_sameAsPreviewSnapshot() {
        // TODO(b/147448711) Add back in once cuttlefish has correct user cropping functionality.
        Assume.assumeFalse(
            "Cuttlefish does not correctly handle crops. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        // Arrange.
        fragment.assertPreviewIsStreaming()
        // Scaled down images to 10x10 bitmap to normalize and reduce computation.
        val width = 10
        val height = 10

        // Act.
        // Get the capture bitmap and the preview bitmap.
        val captureTargetDegrees = rotationValueToRotationDegrees(fragment.sensorRotation)
        val captureResult = fragment.assertCanTakePicture()
        var captureBitmap = Bitmap.createScaledBitmap(captureResult.bitmap, width, height, true)

        val previewTargetDegrees =
            rotationValueToRotationDegrees(fragment.previewView.display.rotation)

        lateinit var previewBitmap: Bitmap
        instrumentation.runOnMainSync {
            previewBitmap = fragment.previewView.bitmap!!
        }
        previewBitmap = Bitmap.createScaledBitmap(previewBitmap, width, height, true)

        // Rotate capture bitmap to match preview orientation
        val captureToPreviewDegrees =
            captureTargetDegrees - previewTargetDegrees + captureResult.rotationDegrees
        val transformCapture = Matrix()
        transformCapture.postRotate(
            captureToPreviewDegrees.toFloat(),
            width.toFloat() / 2,
            height.toFloat() / 2
        )
        if (captureResult.isFlippedHorizontally) {
            transformCapture.postScale(-1F, 1F, width / 2F, height / 2F)
        } else if (captureResult.isFlippedVertically) {
            transformCapture.postScale(1F, -1F, width / 2F, height / 2F)
        }
        captureBitmap =
            Bitmap.createBitmap(captureBitmap, 0, 0, width, height, transformCapture, true)

        // Assert.
        val captureLuminance = getLuminance(captureBitmap)
        val previewLuminance = getLuminance(previewBitmap)
        // Skip test if any of the picture is too dark. The phone is likely to be in a low light
        // environment (e.g. in a unlit test box). In that case the noise is too high to be
        // useful. The test will be skipped.
        assumeTrue(
            "Test skipped. Device most likely in low light environment.",
            captureLuminance > MIN_LUMINANCE && previewLuminance > MIN_LUMINANCE
        )

        val captureMoment = getRgbMoments(captureBitmap)
        val previewMoment = getRgbMoments(previewBitmap)
        // For a 10x10 image, we allow an 1px error. The 2 bitmaps are different due to
        // dynamic range processing, especially in a high contrast environment. The error
        // tolerance is purposely high to avoid false positive.
        val errorTolerance = 1F
        for ((i, colorShift) in RGB_SHIFTS.withIndex()) {
            val errorMsg = "Color $i Capture\n" +
                colorComponentToReadableString(captureBitmap, colorShift) + "Preview\n" +
                colorComponentToReadableString(previewBitmap, colorShift)
            assertWithMessage(errorMsg).that(captureMoment[i].x).isWithin(errorTolerance)
                .of(previewMoment[i].x)
            assertWithMessage(errorMsg).that(captureMoment[i].y).isWithin(errorTolerance)
                .of(previewMoment[i].y)
        }
    }

    @Test
    fun fragmentLaunched_canTakePicture() {
        fragment.assertPreviewIsStreaming()
        fragment.assertCanTakePicture()
    }

    @Test
    fun captureDisabled_cannotTakePicture() {
        // Arrange.
        thrown.expectMessage("ImageCapture disabled")

        // Act.
        onView(withId(R.id.capture_enabled)).perform(click())

        // Assert.
        fragment.assertCanTakePicture()
    }

    @Test
    fun captureDisabledAndEnabled_canTakePicture() {
        // Arrange.
        fragment.assertPreviewIsStreaming()

        // Act.
        onView(withId(R.id.capture_enabled)).perform(click())
        onView(withId(R.id.capture_enabled)).perform(click())
        fragment.assertPreviewIsStreaming()

        // Assert.
        fragment.assertCanTakePicture()
    }

    @Test
    fun previewViewRemoved_previewIsIdle() {
        onView(withId(R.id.remove_or_add)).perform(click())
        fragment.assertPreviewIsIdle()
    }

    @Test
    fun previewViewRemovedAndAdded_previewIsStreaming() {
        onView(withId(R.id.remove_or_add)).perform(click())
        onView(withId(R.id.remove_or_add)).perform(click())
        fragment.assertPreviewIsStreaming()
    }

    @Test
    fun cameraToggled_previewIsStreaming() {
        onView(withId(R.id.camera_toggle)).perform(click())
        fragment.assertPreviewIsStreaming()
    }

    @Test
    fun cameraToggled_canTakePicture() {
        onView(withId(R.id.camera_toggle)).perform(click())
        fragment.assertPreviewIsStreaming()
        fragment.assertCanTakePicture()
    }

    /**
     * Calculates the 1st order moment (center of mass) of the R, G and B of the bitmap.
     */
    private fun getLuminance(bitmap: Bitmap): Float {
        var totals = 0F
        for (colorShift in RGB_SHIFTS) {
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    val color = bitmap.getPixel(x, y)
                    val colorComponent = color shr colorShift and COLOR_MASK
                    totals += colorComponent
                }
            }
        }
        return totals / bitmap.width / bitmap.height / RGB_SHIFTS.size
    }

    /**
     * Calculates the 1st order moment (center of mass) of the R, G and B of the bitmap.
     */
    private fun getRgbMoments(bitmap: Bitmap): Array<PointF> {
        val rgbMoments = arrayOf(PointF(0F, 0F), PointF(0F, 0F), PointF(0F, 0F))
        val totals = arrayOf(0F, 0F, 0F)
        for ((i, colorShift) in RGB_SHIFTS.withIndex()) {
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    val color = bitmap.getPixel(x, y)
                    val colorComponent = color shr colorShift and COLOR_MASK
                    rgbMoments[i].x += colorComponent * x
                    rgbMoments[i].y += colorComponent * y
                    totals[i] += colorComponent.toFloat()
                }
            }
            if (totals[i] == 0F) {
                // Check for divide by 0 error.
                rgbMoments[i].x = 0F
                rgbMoments[i].y = 0F
            } else {
                rgbMoments[i].x /= totals[i]
                rgbMoments[i].y /= totals[i]
            }
        }
        return rgbMoments
    }

    /**
     * Converts the R, G or B component of the bitmap to a readable string table with fixed
     * column width.
     *
     * <p> Example:
     * <pre>
     * 255 255 200
     * 200 10  100
     * 0   1   10
     * </pre>
     *
     * @param colorShift: color component in the format of right shift on Int color.
     */
    private fun colorComponentToReadableString(bitmap1: Bitmap, colorShift: Int): String {
        var result = ""
        for (x in 0 until bitmap1.width) {
            for (y in 0 until bitmap1.height) {
                var color = (bitmap1.getPixel(x, y) shr colorShift and 0xFF).toString()
                // 10x10 table Each column is a fixed size of 4.
                color += " ".repeat((3 - color.length))
                result += "$color "
            }
            result += "\n"
        }
        return result
    }

    private fun rotationValueToRotationDegrees(rotationValue: Int): Int {
        return when (rotationValue) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw IllegalStateException("Unexpected rotation value $rotationValue")
        }
    }

    /**
     * Takes a picture and assert the URI exists.
     *
     * <p> Also cleans up the saved picture afterwards.
     */
    private fun CameraControllerFragment.assertCanTakePicture(): CaptureResult {
        val imageCallbackSemaphore = Semaphore(0)
        var uri: Uri? = null
        instrumentation.runOnMainSync {
            this.takePicture(object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    uri = outputFileResults.savedUri
                    imageCallbackSemaphore.release()
                }

                override fun onError(exception: ImageCaptureException) {
                    imageCallbackSemaphore.release()
                }
            })
        }
        assertThat(imageCallbackSemaphore.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

        // Read bitmap and exif rotation to return.
        val bitmap = this.activity!!.contentResolver.openInputStream(uri!!)!!.use {
            BitmapFactory.decodeStream(it)
        }
        val rotationAndFlip = this.activity!!.contentResolver.openInputStream(uri!!)!!.use {
            val exif = Exif.createFromInputStream(it)
            Triple(exif.rotation, exif.isFlippedHorizontally, exif.isFlippedVertically)
        }

        // Delete the saved picture. Assert 1 row was deleted.
        assertThat(this.activity!!.contentResolver.delete(uri!!, null, null)).isEqualTo(1)
        return CaptureResult(
            bitmap, rotationAndFlip.first, rotationAndFlip.second, rotationAndFlip.third
        )
    }

    private fun createFragmentScenario(): FragmentScenario<CameraControllerFragment> {
        return FragmentScenario.launchInContainer(
            CameraControllerFragment::class.java, null, R.style.AppTheme,
            null
        )
    }

    private fun FragmentScenario<CameraControllerFragment>.getFragment(): CameraControllerFragment {
        var fragment: CameraControllerFragment? = null
        this.onFragment { newValue: CameraControllerFragment -> fragment = newValue }
        return fragment!!
    }

    private fun CameraControllerFragment.assertPreviewIsStreaming() {
        assertPreviewState(PreviewView.StreamState.STREAMING)
    }

    private fun CameraControllerFragment.assertPreviewIsIdle() {
        assertPreviewState(PreviewView.StreamState.IDLE)
    }

    private fun CameraControllerFragment.assertPreviewState(state: PreviewView.StreamState) {
        val previewStreaming = Semaphore(0)
        instrumentation.runOnMainSync {
            previewView.previewStreamState.observe(
                this,
                Observer {
                    if (it == state) {
                        previewStreaming.release()
                    }
                }
            )
        }
        assertThat(previewStreaming.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    private fun CameraControllerFragment.assertAnalysisStreaming(streaming: Boolean) {
        val analysisStreaming = Semaphore(0)
        instrumentation.runOnMainSync {
            setWrappedAnalyzer {
                analysisStreaming.release()
            }
        }
        // Wait for 2 analysis frames. It's necessary because even after the analyzer is removed on
        // the main thread, there could already be a frame posted on user call back thread. For the
        // default non-blocking mode, the max number of frame posted on user thread at the same
        // time is 1. So we wait for one additional frame to make sure the analyzer has stopped.
        assertThat(analysisStreaming.tryAcquire(2, TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(
            streaming
        )
    }

    /**
     * Return value of [CameraControllerFragment.assertCanTakePicture].
     */
    private data class CaptureResult(
        val bitmap: Bitmap,
        val rotationDegrees: Int,
        val isFlippedHorizontally: Boolean,
        val isFlippedVertically: Boolean
    )
}