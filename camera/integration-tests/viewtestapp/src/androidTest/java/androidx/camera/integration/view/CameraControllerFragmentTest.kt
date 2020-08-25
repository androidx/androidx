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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.CameraUtil
import androidx.camera.view.PreviewView
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume
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
    }

    @get:Rule
    val thrown: ExpectedException = ExpectedException.none()

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun fragmentLaunch_cameraInitializationCompletes() {
        val semaphore = Semaphore(0)
        Futures.addCallback(
            createFragmentScenario().getFragment().cameraController.initializationFuture,
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
        createFragmentScenario().getFragment().assertAnalysisStreaming(true)
    }

    @Test
    fun imageAnalysisDisabled_isNotStreaming() {
        val fragment = createFragmentScenario().getFragment()
        fragment.assertAnalysisStreaming(true)

        instrumentation.runOnMainSync {
            fragment.cameraController.isImageAnalysisEnabled = false
        }

        fragment.assertAnalysisStreaming(false)
    }

    @Test
    fun imageAnalysisDisabledAndEnabled_isStreaming() {
        val fragment = createFragmentScenario().getFragment()
        fragment.assertAnalysisStreaming(true)

        instrumentation.runOnMainSync {
            fragment.cameraController.isImageAnalysisEnabled = false
            fragment.cameraController.isImageAnalysisEnabled = true
        }

        fragment.assertAnalysisStreaming(true)
    }

    @Test
    fun analyzerCleared_isNotStreaming() {
        val fragment = createFragmentScenario().getFragment()
        fragment.assertAnalysisStreaming(true)

        instrumentation.runOnMainSync {
            fragment.cameraController.clearImageAnalysisAnalyzer()
        }

        fragment.assertAnalysisStreaming(false)
    }

    @Test
    fun canSetAnalysisImageDepth() {
        // Arrange.
        val fragment = createFragmentScenario().getFragment()
        var currentDepth: Int = 0

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
        // Arrange.
        val fragment = createFragmentScenario().getFragment()

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
    fun capturedImage_sameAsPreviewSnapshot() {
        // TODO(b/147448711) Add back in once cuttlefish has correct user cropping functionality.
        Assume.assumeFalse(
            "Cuttlefish does not correctly handle crops. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        // Arrange.
        val fragment = createFragmentScenario().getFragment()
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
        var previewBitmap = fragment.previewView.bitmap!!
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
        val fragment = createFragmentScenario().getFragment()
        fragment.assertPreviewIsStreaming()
        fragment.assertCanTakePicture()
    }

    @Test
    fun captureDisabled_cannotTakePicture() {
        // Arrange.
        thrown.expectMessage("ImageCapture disabled")
        val fragment = createFragmentScenario().getFragment()
        fragment.assertPreviewIsStreaming()
        instrumentation.runOnMainSync {
            fragment.cameraController.isImageCaptureEnabled = false
        }

        // Act & assert.
        fragment.assertCanTakePicture()
    }

    @Test
    fun captureDisabledAndEnabled_canTakePicture() {
        // Arrange.
        val fragment = createFragmentScenario().getFragment()
        fragment.assertPreviewIsStreaming()

        // Act.
        instrumentation.runOnMainSync {
            fragment.cameraController.isImageCaptureEnabled = false
            fragment.cameraController.isImageCaptureEnabled = true
        }
        fragment.assertPreviewIsStreaming()

        // Assert.
        fragment.assertCanTakePicture()
    }

    @Test
    fun previewViewRemoved_previewIsIdle() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.remove_or_add)).perform(click())
        fragment.assertPreviewIsIdle()
    }

    @Test
    fun previewViewRemovedAndAdded_previewIsStreaming() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.remove_or_add)).perform(click())
        onView(withId(R.id.remove_or_add)).perform(click())
        fragment.assertPreviewIsStreaming()
    }

    @Test
    fun cameraToggled_previewIsStreaming() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.camera_toggle)).perform(click())
        fragment.assertPreviewIsStreaming()
    }

    @Test
    fun cameraToggled_canTakePicture() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.camera_toggle)).perform(click())
        fragment.assertPreviewIsStreaming()
        fragment.assertCanTakePicture()
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
            rgbMoments[i].x /= totals[i]
            rgbMoments[i].y /= totals[i]
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
    private fun colorComponentToReadableString(bitmap1: Bitmap, colorShift: Int):
        String {
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
        ).also {
            it.moveToState(Lifecycle.State.CREATED)
            it.moveToState(Lifecycle.State.RESUMED)
        }
    }

    private fun FragmentScenario<CameraControllerFragment>.getFragment():
        CameraControllerFragment {
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
                it.close()
                analysisStreaming.release()
            }
        }
        assertThat(analysisStreaming.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(
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