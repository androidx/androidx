/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import android.util.SizeF
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 21)
class CameraMathTest {

    @Test
    fun cameraMathSensorToDisplayOrientationTable() {
        // Front facing table
        assertThat(relativeRotation(0, 0, true)).isEqualTo(0)
        assertThat(relativeRotation(0, 90, true)).isEqualTo(90)
        assertThat(relativeRotation(0, 180, true)).isEqualTo(180)
        assertThat(relativeRotation(0, 270, true)).isEqualTo(270)

        assertThat(relativeRotation(90, 0, true)).isEqualTo(90)
        assertThat(relativeRotation(90, 90, true)).isEqualTo(180)
        assertThat(relativeRotation(90, 180, true)).isEqualTo(270)
        assertThat(relativeRotation(90, 270, true)).isEqualTo(0)

        assertThat(relativeRotation(180, 0, true)).isEqualTo(180)
        assertThat(relativeRotation(180, 90, true)).isEqualTo(270)
        assertThat(relativeRotation(180, 180, true)).isEqualTo(0)
        assertThat(relativeRotation(180, 270, true)).isEqualTo(90)

        assertThat(relativeRotation(270, 0, true)).isEqualTo(270)
        assertThat(relativeRotation(270, 90, true)).isEqualTo(0)
        assertThat(relativeRotation(270, 180, true)).isEqualTo(90)
        assertThat(relativeRotation(270, 270, true)).isEqualTo(180)

        // Rear facing table
        assertThat(relativeRotation(0, 0, false)).isEqualTo(0)
        assertThat(relativeRotation(0, 90, false)).isEqualTo(90)
        assertThat(relativeRotation(0, 180, false)).isEqualTo(180)
        assertThat(relativeRotation(0, 270, false)).isEqualTo(270)

        assertThat(relativeRotation(90, 0, false)).isEqualTo(270)
        assertThat(relativeRotation(90, 90, false)).isEqualTo(0)
        assertThat(relativeRotation(90, 180, false)).isEqualTo(90)
        assertThat(relativeRotation(90, 270, false)).isEqualTo(180)

        assertThat(relativeRotation(180, 0, false)).isEqualTo(180)
        assertThat(relativeRotation(180, 90, false)).isEqualTo(270)
        assertThat(relativeRotation(180, 180, false)).isEqualTo(0)
        assertThat(relativeRotation(180, 270, false)).isEqualTo(90)

        assertThat(relativeRotation(270, 0, false)).isEqualTo(90)
        assertThat(relativeRotation(270, 90, false)).isEqualTo(180)
        assertThat(relativeRotation(270, 180, false)).isEqualTo(270)
        assertThat(relativeRotation(270, 270, false)).isEqualTo(0)
    }

    private fun relativeRotation(
        displayOrientation: Int,
        sensorOrientation: Int,
        isSensorFacingScreen: Boolean,
    ): Int {
        return CameraMath.computeSensorRotationToDisplayOrientation(
                displayRotation = displayOrientation,
                sensorOrientation = sensorOrientation,
                sensorIsFacingScreen = isSensorFacingScreen,
            )
            .degrees
    }

    @Test
    fun cameraMathSensorToJpegOrientationTable() {
        val jpegOrientation =
            { deviceOrientation: Int, sensorOrientation: Int, sensorIsFacingScreen: Boolean ->
                CameraMath.computeSensorRotationToJpegOrientation(
                        deviceOrientation = deviceOrientation,
                        sensorOrientation = sensorOrientation,
                        sensorIsFacingScreen = sensorIsFacingScreen,
                    )
                    .degrees
            }

        // Front facing table
        assertThat(jpegOrientation(0, 0, true)).isEqualTo(0)
        assertThat(jpegOrientation(0, 90, true)).isEqualTo(90)
        assertThat(jpegOrientation(0, 180, true)).isEqualTo(180)
        assertThat(jpegOrientation(0, 270, true)).isEqualTo(270)

        assertThat(jpegOrientation(90, 0, true)).isEqualTo(270)
        assertThat(jpegOrientation(90, 90, true)).isEqualTo(0)
        assertThat(jpegOrientation(90, 180, true)).isEqualTo(90)
        assertThat(jpegOrientation(90, 270, true)).isEqualTo(180)

        assertThat(jpegOrientation(180, 0, true)).isEqualTo(180)
        assertThat(jpegOrientation(180, 90, true)).isEqualTo(270)
        assertThat(jpegOrientation(180, 180, true)).isEqualTo(0)
        assertThat(jpegOrientation(180, 270, true)).isEqualTo(90)

        assertThat(jpegOrientation(270, 0, true)).isEqualTo(90)
        assertThat(jpegOrientation(270, 90, true)).isEqualTo(180)
        assertThat(jpegOrientation(270, 180, true)).isEqualTo(270)
        assertThat(jpegOrientation(270, 270, true)).isEqualTo(0)

        // Rear facing table
        assertThat(jpegOrientation(0, 0, false)).isEqualTo(0)
        assertThat(jpegOrientation(0, 90, false)).isEqualTo(90)
        assertThat(jpegOrientation(0, 180, false)).isEqualTo(180)
        assertThat(jpegOrientation(0, 270, false)).isEqualTo(270)

        assertThat(jpegOrientation(90, 0, false)).isEqualTo(90)
        assertThat(jpegOrientation(90, 90, false)).isEqualTo(180)
        assertThat(jpegOrientation(90, 180, false)).isEqualTo(270)
        assertThat(jpegOrientation(90, 270, false)).isEqualTo(0)

        assertThat(jpegOrientation(180, 0, false)).isEqualTo(180)
        assertThat(jpegOrientation(180, 90, false)).isEqualTo(270)
        assertThat(jpegOrientation(180, 180, false)).isEqualTo(0)
        assertThat(jpegOrientation(180, 270, false)).isEqualTo(90)

        assertThat(jpegOrientation(270, 0, false)).isEqualTo(270)
        assertThat(jpegOrientation(270, 90, false)).isEqualTo(0)
        assertThat(jpegOrientation(270, 180, false)).isEqualTo(90)
        assertThat(jpegOrientation(270, 270, false)).isEqualTo(180)
    }

    private val cropX = 10
    private val cropY = 10
    private val cropWidth = 4000
    private val cropHeight = 3000
    private val cropRect = Rect(cropX, cropY, cropX + cropWidth, cropY + cropHeight)

    private val stream1Width = 1600
    private val stream1Height = 900
    private val stream1Size = Size(stream1Width, stream1Height)
    private val stream1Rotation = 90

    private val stream2Width = 1000
    private val stream2Height = 1000
    private val stream2Size = Size(stream2Width, stream2Height)

    private val uiSize = Size(1800, 3200)

    @Test
    fun mapStreamRectToSensorRectTests() {
        val stream1SensorRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = RectF(0.0f, 0.0f, stream1Width.toFloat(), stream1Height.toFloat()),
                streamSize = stream1Size,
                sensorCrop = cropRect,
            )

        assertThat(stream1SensorRect).isEqualTo(Rect(10, 385, 4010, 2635))

        val stream2SensorRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = RectF(0.0f, 0.0f, stream2Width.toFloat(), stream2Height.toFloat()),
                streamSize = stream2Size,
                sensorCrop = cropRect,
            )
        assertThat(stream2SensorRect).isEqualTo(Rect(510, 10, 3510, 3010))
    }

    @Test
    fun uiRectToStreamRectTest() {
        val sensorRotationToDisplayOrientation =
            CameraMath.computeSensorRotationToDisplayOrientation(
                displayRotation = 0,
                sensorOrientation = stream1Rotation,
                sensorIsFacingScreen = true,
            )

        val stream1Rect =
            CameraMath.mapUiRectToStreamRectF(
                uiRect = RectF(100.0f, 100.0f, 200.0f, 200.0f),
                uiSize = uiSize,
                streamSize = stream1Size,
                streamMirroring = false,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation.degrees,
            )
        assertRectWithinTolerance(stream1Rect, RectF(50f, 800f, 100f, 850f))

        val stream1UiRect =
            CameraMath.mapStreamRectToUiRectF(
                streamRect = stream1Rect,
                uiSize = uiSize,
                streamSize = stream1Size,
                streamMirroring = false,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation.degrees,
            )
        assertRectWithinTolerance(stream1UiRect, RectF(100f, 100f, 200f, 200f))
    }

    @Test
    fun mapUiRectToSensorRect() {
        val sensorRotationToDisplayOrientation =
            CameraMath.computeSensorRotationToDisplayOrientation(
                displayRotation = 0,
                sensorOrientation = stream1Rotation,
                sensorIsFacingScreen = true,
            )

        val sensorRect =
            CameraMath.mapUiRectToSensorRect(
                uiRect = RectF(100.0f, 100.0f, 200.0f, 200.0f),
                uiSize = uiSize,
                streamSize = stream1Size,
                streamMirroring = false,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation.degrees,
                sensorCrop = cropRect,
            )

        assertThat(sensorRect).isEqualTo(Rect(135, 2385, 260, 2510))

        val uiRect =
            CameraMath.mapSensorRectToUiRectF(
                sensorRect = sensorRect,
                uiSize = uiSize,
                streamSize = stream1Size,
                streamMirroring = false,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation.degrees,
                sensorCrop = cropRect,
            )

        assertRectWithinTolerance(uiRect, RectF(100f, 100f, 200f, 200f))
    }

    @Test
    fun parameterizedUiRectToSensorRectAndBackTest() {
        val uiRect = Rect(100, 100, 200, 200)

        parameterizedCameraMathTest { streamMirroring, mapSensorRotationToDisplayOrientation ->
            val uiToSensorRect =
                CameraMath.mapUiRectToSensorRect(
                    uiRect =
                        RectF(
                            uiRect.left.toFloat(),
                            uiRect.top.toFloat(),
                            uiRect.right.toFloat(),
                            uiRect.bottom.toFloat(),
                        ),
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    streamMirroring = streamMirroring,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCrop = cropRect,
                )

            val sensorToUiRect: RectF =
                CameraMath.mapSensorRectToUiRectF(
                    sensorRect = uiToSensorRect,
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    streamMirroring = streamMirroring,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCrop = cropRect,
                )

            assertThat(sensorToUiRect.left).isWithin(0.9f).of(uiRect.left.toFloat())
            assertThat(sensorToUiRect.top).isWithin(0.9f).of(uiRect.top.toFloat())
            assertThat(sensorToUiRect.bottom).isWithin(0.9f).of(uiRect.bottom.toFloat())
            assertThat(sensorToUiRect.right).isWithin(0.9f).of(uiRect.right.toFloat())
        }
    }

    @Test
    fun parameterizedUiPointAndRectToSensorPointParityTest() {
        val uiPoint = Point(500, 500)
        val uiRectPoint = Rect(uiPoint.x, uiPoint.y, uiPoint.x, uiPoint.y)

        parameterizedCameraMathTest { streamMirroring, mapSensorRotationToDisplayOrientation ->
            val uiToSensorRect =
                CameraMath.mapUiRectToSensorRect(
                    uiRect =
                        RectF(
                            uiRectPoint.left.toFloat(),
                            uiRectPoint.top.toFloat(),
                            uiRectPoint.right.toFloat(),
                            uiRectPoint.bottom.toFloat(),
                        ),
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    streamMirroring = streamMirroring,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCrop = cropRect,
                    minimumSensorSize = 0,
                )

            assertThat(uiToSensorRect.left).isEqualTo(uiToSensorRect.right)
            assertThat(uiToSensorRect.top).isEqualTo(uiToSensorRect.bottom)

            val uiToSensorPoint =
                CameraMath.mapUiPointToSensorPoint(
                    uiPoint = uiPoint,
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    streamMirroring = streamMirroring,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCrop = cropRect,
                )

            assertThat(uiToSensorPoint.x).isEqualTo(uiToSensorRect.left)
            assertThat(uiToSensorPoint.y).isEqualTo(uiToSensorRect.top)

            val sensorToUiPoint: PointF =
                CameraMath.mapSensorPointToUiPointF(
                    sensorPoint = uiToSensorPoint,
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    streamMirroring = streamMirroring,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCrop = cropRect,
                )

            assertThat(sensorToUiPoint.x).isWithin(0.9f).of(uiPoint.x.toFloat())
            assertThat(sensorToUiPoint.y).isWithin(0.9f).of(uiPoint.y.toFloat())
        }
    }

    @Test
    fun parameterizedUiSizeAndRectToSensorPointParityTest() {
        val uiSize = Size(500, 500)
        val uiRectSize = Rect(100, 100, 100 + uiSize.width, 100 + uiSize.height)

        parameterizedCameraMathTest { streamMirroring, mapSensorRotationToDisplayOrientation ->
            val uiToSensorRect =
                CameraMath.mapUiRectToSensorRect(
                    uiRect =
                        RectF(
                            uiRectSize.left.toFloat(),
                            uiRectSize.top.toFloat(),
                            uiRectSize.right.toFloat(),
                            uiRectSize.bottom.toFloat(),
                        ),
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    streamMirroring = streamMirroring,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCrop = cropRect,
                )

            val uiToSensorSize =
                CameraMath.mapUiSizeToSensorSize(
                    size = uiSize,
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCropWidth = cropWidth,
                    sensorCropHeight = cropHeight,
                )

            assertThat(uiToSensorSize.width).isEqualTo(uiToSensorRect.width())
            assertThat(uiToSensorSize.height).isEqualTo(uiToSensorRect.height())

            val sensorToUiSize: SizeF =
                CameraMath.mapSensorSizeToUiSizeF(
                    size = uiToSensorSize,
                    uiSize = uiSize,
                    streamSize = stream1Size,
                    sensorRotationToDisplayOrientation = mapSensorRotationToDisplayOrientation,
                    sensorCropWidth = cropWidth,
                    sensorCropHeight = cropHeight,
                )

            assertThat(sensorToUiSize.width).isWithin(0.9f).of(uiSize.width.toFloat())
            assertThat(sensorToUiSize.height).isWithin(0.9f).of(uiSize.height.toFloat())
        }
    }

    @Test
    fun mapStreamPointToSensorPointCoercion() {
        val outsidePoint = PointF(-1f, -1f)
        val insidePoint = PointF(500f, 500f)

        val coercedOutsidePoint =
            CameraMath.mapStreamPointToSensorPoint(
                streamPoint = outsidePoint,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                coerceToCropRegion = true,
            )

        val coercedInsidePoint =
            CameraMath.mapStreamPointToSensorPoint(
                streamPoint = insidePoint,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                coerceToCropRegion = true,
            )

        val uncoercedOutsidePoint =
            CameraMath.mapStreamPointToSensorPoint(
                streamPoint = outsidePoint,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                coerceToCropRegion = false,
            )

        assertThat(coercedOutsidePoint.x).isAtLeast(cropX)
        assertThat(coercedOutsidePoint.x).isAtMost(cropX + cropWidth)
        assertThat(coercedOutsidePoint.y).isAtLeast(cropY)
        assertThat(coercedOutsidePoint.y).isAtMost(cropY + cropHeight)

        assertThat(coercedInsidePoint.x).isGreaterThan(cropX)
        assertThat(coercedInsidePoint.x).isLessThan(cropX + cropWidth)
        assertThat(coercedInsidePoint.y).isGreaterThan(cropY)
        assertThat(coercedInsidePoint.y).isLessThan(cropY + cropHeight)

        assertThat(uncoercedOutsidePoint.x).isLessThan(cropX)
        assertThat(uncoercedOutsidePoint.y).isLessThan(cropY)
    }

    @Test
    fun mapStreamRectToSensorRectCoercionAndMinimumSize() {
        val outsideRect = RectF(-100f, -100f, -50f, -50f)
        val insideRect = RectF(500f, 500f, 600f, 600f)
        val smallRect = RectF(0f, 0f, 1f, 1f)
        val largeRect = RectF(0f, 0f, 5000f, 4000f)

        val coercedOutsideRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = outsideRect,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                minimumSensorSize = 0,
                coerceToCropRegion = true,
            )

        val coercedInsideRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = insideRect,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                minimumSensorSize = 0,
                coerceToCropRegion = true,
            )

        val uncoercedOutsideRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = outsideRect,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                minimumSensorSize = 0,
                coerceToCropRegion = false,
            )

        val minimumSizeRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = smallRect,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                minimumSensorSize = 100,
                coerceToCropRegion = true,
            )

        val coercedLargeRect =
            CameraMath.mapStreamRectToSensorRect(
                streamRect = largeRect,
                streamSize = Size(cropWidth, cropHeight),
                sensorCrop = cropRect,
                minimumSensorSize = 0,
                coerceToCropRegion = true,
            )

        assertThat(coercedOutsideRect.left).isAtLeast(cropX)
        assertThat(coercedOutsideRect.right).isAtMost(cropX + cropWidth)
        assertThat(coercedOutsideRect.top).isAtLeast(cropY)
        assertThat(coercedOutsideRect.bottom).isAtMost(cropY + cropHeight)

        assertThat(coercedInsideRect.left).isGreaterThan(cropX)
        assertThat(coercedInsideRect.right).isLessThan(cropX + cropWidth)
        assertThat(coercedInsideRect.top).isGreaterThan(cropY)
        assertThat(coercedInsideRect.bottom).isLessThan(cropY + cropHeight)

        assertThat(uncoercedOutsideRect.left).isLessThan(cropX)
        assertThat(uncoercedOutsideRect.right).isLessThan(cropX)
        assertThat(uncoercedOutsideRect.top).isLessThan(cropY)
        assertThat(uncoercedOutsideRect.bottom).isLessThan(cropY)

        assertThat(minimumSizeRect.width()).isAtLeast(100)
        assertThat(minimumSizeRect.height()).isAtLeast(100)

        assertThat(coercedLargeRect.centerX()).isEqualTo(cropX + cropWidth / 2)
        assertThat(coercedLargeRect.centerY()).isEqualTo(cropY + cropHeight / 2)
    }

    private fun assertRectWithinTolerance(actual: RectF, expected: RectF, tolerance: Float = 0.9f) {
        assertThat(actual.left).isWithin(tolerance).of(expected.left)
        assertThat(actual.top).isWithin(tolerance).of(expected.top)
        assertThat(actual.right).isWithin(tolerance).of(expected.right)
        assertThat(actual.bottom).isWithin(tolerance).of(expected.bottom)
    }

    private fun parameterizedCameraMathTest(
        block: (streamMirroring: Boolean, mapSensorRotationToDisplayOrientation: Int) -> Unit
    ) {
        val streamMirroringParams = listOf(true, false)
        val mapSensorRotationToDisplayOrientationParams = listOf(0, 90, 180, 270)

        for (streamMirroring in streamMirroringParams) {
            for (mapSensorRotationToDisplayOrientation in
                mapSensorRotationToDisplayOrientationParams) {
                block(streamMirroring, mapSensorRotationToDisplayOrientation)
            }
        }
    }
}
