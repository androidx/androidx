/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.mlkit.vision

import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.os.Build
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.TransformUtils.getRectToRect
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.interfaces.Detector
import com.google.mlkit.vision.interfaces.Detector.TYPE_BARCODE_SCANNING
import com.google.mlkit.vision.interfaces.Detector.TYPE_FACE_DETECTION
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit test for [MlKitAnalyzer].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MlKitAnalyzerTest {

    companion object {
        private const val RETURN_VALUE = "return value"
        private const val TIMESTAMP = 100L
        private const val ROTATION_DEGREES = 180
        private val SENSOR_RECT = Rect(0, 0, 4000, 3000)
        private val VIEW_RECT = Rect(0, 0, 1024, 768)
        private val IMAGE_ANALYSIS_RECT = Rect(0, 0, 640, 480)
        private val SENSOR_TO_BUFFER = getRectToRect(
            RectF(SENSOR_RECT),
            RectF(IMAGE_ANALYSIS_RECT),
            0
        )
    }

    @Test
    fun detectorIsClosed_returnsException() {
        // Arrange: create 2 detectors, one is closed and one is open.
        val closedDetector = FakeDetector(RETURN_VALUE, TYPE_BARCODE_SCANNING)
        closedDetector.close()
        val openDetector = FakeDetector(RETURN_VALUE, TYPE_BARCODE_SCANNING)
        var result: MlKitAnalyzer.Result? = null
        val mlKitAnalyzer = MlKitAnalyzer(
            listOf(closedDetector, openDetector),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            directExecutor()
        ) {
            result = it
        }
        // Act.
        mlKitAnalyzer.analyze(createFakeImageProxy())
        // Assert: the closed detector contains a Exception. The open one contains the value.
        assertThat(result!!.getThrowable(closedDetector)).isInstanceOf(Exception::class.java)
        assertThat(result!!.getValue(openDetector)).isEqualTo(RETURN_VALUE)
    }

    @Test
    fun taskIsCanceled_returnsCancellationException() {
        // Arrange: create a detector that delivers canceled tasks.
        val fakeDetector = FakeDetector(RETURN_VALUE, TYPE_BARCODE_SCANNING)
        fakeDetector.taskCanceled = true
        var result: MlKitAnalyzer.Result? = null
        val mlKitAnalyzer = MlKitAnalyzer(
            listOf(fakeDetector),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            directExecutor()
        ) {
            result = it
        }
        // Act.
        mlKitAnalyzer.analyze(createFakeImageProxy())
        // Assert: the result has a CancellationException.
        assertThat(result!!.getThrowable(fakeDetector))
            .isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun createAnalyzerWith2Detectors_overridesWithHigherResolution() {
        val barcodeScanner = FakeDetector(RETURN_VALUE, TYPE_BARCODE_SCANNING)
        val faceDetector = FakeDetector(RETURN_VALUE, TYPE_FACE_DETECTION)
        val mlKitAnalyzer = MlKitAnalyzer(
            listOf(barcodeScanner, faceDetector),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            directExecutor()
        ) {}

        assertThat(mlKitAnalyzer.defaultTargetResolution).isEqualTo(Size(1280, 720))
    }

    @Test
    fun analyze_detectorsReceiveValues() {
        // Arrange: 2 detectors, one succeeds and one fails.
        val failDetector = FakeDetector(null, TYPE_BARCODE_SCANNING)
        failDetector.taskException = Exception()
        val successDetector = FakeDetector(RETURN_VALUE, TYPE_BARCODE_SCANNING)
        var result: MlKitAnalyzer.Result? = null
        val mlKitAnalyzer = MlKitAnalyzer(
            listOf(failDetector, successDetector),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            directExecutor()
        ) {
            result = it
        }

        // Act.
        mlKitAnalyzer.analyze(createFakeImageProxy())

        // Asset the detector received correct values.
        assertThat(result!!.timestamp).isEqualTo(TIMESTAMP)
        assertThat(failDetector.latestMatrix!!.isIdentity).isTrue()
        assertThat(successDetector.latestMatrix!!.isIdentity).isTrue()
        assertThat(failDetector.latestRotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(successDetector.latestRotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(result!!.getValue(failDetector)).isNull()
        assertThat(result!!.getThrowable(failDetector)).isNotNull()
        assertThat(result!!.getValue(successDetector)).isEqualTo(RETURN_VALUE)
        assertThat(result!!.getThrowable(successDetector)).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun segmentationAndPreviewView_throwsException() {
        MlKitAnalyzer(
            listOf(FakeDetector(RETURN_VALUE, Detector.TYPE_SEGMENTATION)),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            directExecutor()
        ) {}
    }

    @Test
    fun transformationAndRotationIsCorrect() {
        // Arrange.
        val additionalTransform = getRectToRect(RectF(SENSOR_RECT), RectF(VIEW_RECT), 0)
        additionalTransform.setScale(2F, 2F)
        val detector = FakeDetector(RETURN_VALUE, TYPE_BARCODE_SCANNING)
        val analyzer = MlKitAnalyzer(
            listOf(detector),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            directExecutor()
        ) {
        }
        analyzer.updateTransform(additionalTransform)

        // Act.
        analyzer.analyze(createFakeImageProxy())

        // Assert that the matrix is passed to the MLKit detector.
        val expected = floatArrayOf(-12.5F, 0F, 8000F, 0F, -12.5F, 6000F, 0F, 0F, 1F)
        val actual = FloatArray(9)
        detector.latestMatrix!!.getValues(actual)
        assertThat(actual).usingTolerance(1E-3).containsExactly(expected).inOrder()
    }

    private fun createFakeImageProxy(): ImageProxy {
        val imageInfo = FakeImageInfo()
        imageInfo.timestamp = TIMESTAMP
        imageInfo.rotationDegrees = ROTATION_DEGREES
        imageInfo.sensorToBufferTransformMatrix = SENSOR_TO_BUFFER

        val imageProxy =
            FakeImageProxy(imageInfo)
        imageProxy.image = mock(Image::class.java)
        imageProxy.width = IMAGE_ANALYSIS_RECT.width()
        imageProxy.height = IMAGE_ANALYSIS_RECT.height()
        imageProxy.setCropRect(IMAGE_ANALYSIS_RECT)

        return imageProxy
    }
}
