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

import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import android.os.Build
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.interfaces.Detector
import com.google.mlkit.vision.interfaces.Detector.TYPE_BARCODE_SCANNING
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
        private val CROP_RECT = Rect(0, 0, 640, 480)
    }

    @Test
    fun analyze_detectorsReceiveValues() {
        // Arrange: 2 detectors, one succeeds and one fails.
        val failDetector = FakeDetector(null, Exception(), TYPE_BARCODE_SCANNING)
        val successDetector = FakeDetector(RETURN_VALUE, null, TYPE_BARCODE_SCANNING)
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
            listOf(FakeDetector(RETURN_VALUE, null, Detector.TYPE_SEGMENTATION)),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            directExecutor()
        ) {}
    }

    @Test
    fun transformationAndRotationIsCorrect() {
        // Arrange.
        val additionalTransform = Matrix()
        additionalTransform.setScale(2F, 2F)
        val detector = FakeDetector(RETURN_VALUE, null, TYPE_BARCODE_SCANNING)
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
        val expected = floatArrayOf(-0.00625F, 0F, 2.0F, 0F, -0.0083333F, 2.0F, 0.0F, 0.0F, 1.0F)
        val actual = FloatArray(9)
        detector.latestMatrix!!.getValues(actual)
        actual.forEachIndexed { i, element ->
            // Assert allowing float rounding error.
            assertThat(expected[i]).isWithin(1E-4F).of(element)
        }
    }

    private fun createFakeImageProxy(): ImageProxy {
        val imageInfo = FakeImageInfo()
        imageInfo.timestamp = TIMESTAMP
        imageInfo.rotationDegrees = ROTATION_DEGREES

        val imageProxy = FakeImageProxy(imageInfo)
        imageProxy.image = mock(Image::class.java)
        imageProxy.width = CROP_RECT.width()
        imageProxy.height = CROP_RECT.height()
        imageProxy.setCropRect(CROP_RECT)

        return imageProxy
    }
}
