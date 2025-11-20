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

package androidx.camera.view

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.LayoutDirection
import android.util.Size
import androidx.camera.core.SurfaceRequest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Instrument test for [PreviewViewMeteringPointFactory]. */
@SmallTest
@RunWith(Parameterized::class)
class PreviewViewMeteringPointFactoryDeviceTest(
    private val cropRect: Rect,
    private val rotationDegrees: Int,
    private val surfaceSize: Size,
    private val scaleType: PreviewView.ScaleType,
    private val previewViewSize: Size,
    private val layoutDirection: Int,
    private val isFrontCamera: Boolean,
    private val uiPoint: PointF,
    private val expectedMeteringPoint: PointF,
) {

    companion object {

        private const val FRONT_CAMERA = true
        private const val BACK_CAMERA = false

        private const val FLOAT_ERROR = 1E-4F

        // A fake target rotation IntDef. Target rotation is already included in rotationDegrees.
        // The IntRef value convenience for PreviewTransformation to correct TextureView and
        // it doesn't affect the metering point calculation.
        private const val FAKE_TARGET_ROTATION = -999

        private val VIEW_SIZE: Size by lazy { Size(48, 36) }

        // View size rotated 90°.
        private val VIEW_SIZE_90: Size by lazy { Size(36, 48) }
        private val FULL_VIEW_CROP_RECT: Rect by lazy {
            Rect(0, 0, VIEW_SIZE.width, VIEW_SIZE.height)
        }
        private val FULL_VIEW_SURFACE_SIZE: Size by lazy { VIEW_SIZE }

        private val SQUARE_SURFACE_SIZE: Size by lazy { Size(36, 36) }
        private val SQUARE_SURFACE_RECT: Rect by lazy {
            Rect(0, 0, SQUARE_SURFACE_SIZE.width, SQUARE_SURFACE_SIZE.height)
        }

        // Crop rect with the same aspect ratio as the view.
        private val VIEW_CROP_RECT = Rect(15, 10, 39, 28)
        private val SENSOR_RECT = Rect(0, 0, 4000, 3000)

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {

            return listOf(
                // Device in sensor orientation without crop rect.
                arrayOf(
                    FULL_VIEW_CROP_RECT,
                    0,
                    FULL_VIEW_SURFACE_SIZE,
                    PreviewView.ScaleType.FILL_CENTER,
                    VIEW_SIZE,
                    LayoutDirection.LTR,
                    BACK_CAMERA,
                    PointF(0F, 0F),
                    PointF(0F, 0F),
                ),

                // Device with front camera. The metering point is flipped.
                arrayOf(
                    FULL_VIEW_CROP_RECT,
                    0,
                    FULL_VIEW_SURFACE_SIZE,
                    PreviewView.ScaleType.FILL_CENTER,
                    VIEW_SIZE,
                    LayoutDirection.LTR,
                    FRONT_CAMERA,
                    PointF(0F, 0F),
                    PointF(1F, 0F),
                ),

                // Device in sensor orientation with crop rect.
                arrayOf(
                    VIEW_CROP_RECT,
                    0,
                    FULL_VIEW_SURFACE_SIZE,
                    PreviewView.ScaleType.FILL_CENTER,
                    VIEW_SIZE,
                    LayoutDirection.LTR,
                    BACK_CAMERA,
                    PointF(0F, 0F),
                    PointF(
                        VIEW_CROP_RECT.left.toFloat() / VIEW_SIZE.width,
                        VIEW_CROP_RECT.top.toFloat() / VIEW_SIZE.height,
                    ),
                ),

                // Device in natural orientation with crop rect.
                arrayOf(
                    VIEW_CROP_RECT,
                    90,
                    FULL_VIEW_SURFACE_SIZE,
                    PreviewView.ScaleType.FILL_CENTER,
                    VIEW_SIZE_90,
                    LayoutDirection.LTR,
                    BACK_CAMERA,
                    PointF(VIEW_SIZE_90.width.toFloat(), 0F),
                    PointF(
                        VIEW_CROP_RECT.left.toFloat() / VIEW_SIZE.width,
                        VIEW_CROP_RECT.top.toFloat() / VIEW_SIZE.height,
                    ),
                ),

                // Device in locked portrait mode with 270° rotation and crop rect.
                arrayOf(
                    VIEW_CROP_RECT,
                    270,
                    FULL_VIEW_SURFACE_SIZE,
                    PreviewView.ScaleType.FILL_CENTER,
                    VIEW_SIZE_90,
                    LayoutDirection.LTR,
                    BACK_CAMERA,
                    PointF(0F, VIEW_SIZE_90.height.toFloat()),
                    PointF(
                        VIEW_CROP_RECT.left.toFloat() / VIEW_SIZE.width,
                        VIEW_CROP_RECT.top.toFloat() / VIEW_SIZE.height,
                    ),
                ),

                // FIT type.
                arrayOf(
                    SQUARE_SURFACE_RECT,
                    0,
                    SQUARE_SURFACE_SIZE,
                    PreviewView.ScaleType.FIT_END,
                    VIEW_SIZE,
                    LayoutDirection.LTR,
                    BACK_CAMERA,
                    PointF(12F, 0F),
                    PointF(0F, 0F),
                ),

                // FIT type with RTL
                arrayOf(
                    SQUARE_SURFACE_RECT,
                    0,
                    SQUARE_SURFACE_SIZE,
                    PreviewView.ScaleType.FIT_END,
                    VIEW_SIZE,
                    LayoutDirection.RTL,
                    BACK_CAMERA,
                    PointF(0F, 0F),
                    PointF(0F, 0F),
                ),

                // FILL type with mismatched crop rect. (viewport not set)
                arrayOf(
                    SQUARE_SURFACE_RECT,
                    0,
                    SQUARE_SURFACE_SIZE,
                    PreviewView.ScaleType.FILL_CENTER,
                    VIEW_SIZE,
                    LayoutDirection.LTR,
                    BACK_CAMERA,
                    PointF(0F, 0F),
                    PointF(0F, 0.125F),
                ),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun getSurfaceNormalizedPt(sensorNormalizedPt: PointF): PointF {
        val matrix = getSensorToBufferTransformMatrix(surfaceSize)

        val normalization = Matrix()
        normalization.setRectToRect(
            RectF(0f, 0f, surfaceSize.width.toFloat(), surfaceSize.height.toFloat()),
            RectF(0f, 0f, 1f, 1f),
            Matrix.ScaleToFit.FILL,
        )
        matrix.postConcat(normalization)
        val pointArray =
            floatArrayOf(
                sensorNormalizedPt.x * SENSOR_RECT.width(),
                sensorNormalizedPt.y * SENSOR_RECT.height(),
            )
        matrix.mapPoints(pointArray)
        return PointF(pointArray[0], pointArray[1])
    }

    private fun getSensorToBufferTransformMatrix(surfaceSize: Size): Matrix {
        val fullSensorRectF = RectF(SENSOR_RECT)
        val sensorToUseCaseTransformation = Matrix()
        val srcRect = RectF(0f, 0f, surfaceSize.width.toFloat(), surfaceSize.height.toFloat())
        sensorToUseCaseTransformation.setRectToRect(
            srcRect,
            fullSensorRectF,
            Matrix.ScaleToFit.CENTER,
        )
        sensorToUseCaseTransformation.invert(sensorToUseCaseTransformation)
        return sensorToUseCaseTransformation
    }

    @Test
    fun verifyMeteringPoint() {
        // Arrange.
        val previewTransformation = PreviewTransformation()
        previewTransformation.scaleType = scaleType
        previewTransformation.setTransformationInfo(
            SurfaceRequest.TransformationInfo.of(
                cropRect,
                rotationDegrees,
                FAKE_TARGET_ROTATION,
                /*hasCameraTransform=*/ true,
                /*sensorToBufferTransform=*/ getSensorToBufferTransformMatrix(surfaceSize),
                /*mirroring=*/ false,
            ),
            surfaceSize,
            isFrontCamera,
        )
        val meteringPointFactory = PreviewViewMeteringPointFactory(previewTransformation)
        meteringPointFactory.setSensorRect(SENSOR_RECT)

        // Act.
        instrumentation.runOnMainSync {
            meteringPointFactory.recalculate(previewViewSize, layoutDirection)
        }
        val meteringPoint =
            getSurfaceNormalizedPt(meteringPointFactory.convertPoint(uiPoint.x, uiPoint.y))

        // Assert.
        assertThat(meteringPoint.x).isWithin(FLOAT_ERROR).of(expectedMeteringPoint.x)
        assertThat(meteringPoint.y).isWithin(FLOAT_ERROR).of(expectedMeteringPoint.y)
    }
}
