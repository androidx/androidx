/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.MeteringPoint
import androidx.camera.core.impl.CameraControlInternal
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import java.util.ArrayList

/**
 * Implementation of focus and metering controls exposed by [CameraControlInternal].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FocusMeteringControl(
    val cameraProperties: CameraProperties,
    val useCaseManager: UseCaseManager,
    val threads: UseCaseThreads,
) {
    private val sensorRect =
        cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!

    fun startFocusAndMetering(
        action: FocusMeteringAction,
        defaultAspectRatio: Rational
    ): ListenableFuture<FocusMeteringResult> {
        // TODO(sushilnath): Throw a proper exception when (a). camera is null, (b) all of ae, af
        return threads.scope.async(start = CoroutineStart.UNDISPATCHED) {
            useCaseManager.camera!!.startFocusAndMeteringAsync(
                aeRegions = meteringRegionsFromMeteringPoints(
                    action.meteringPointsAe,
                    sensorRect,
                    defaultAspectRatio
                ),
                afRegions = meteringRegionsFromMeteringPoints(
                    action.meteringPointsAf,
                    sensorRect,
                    defaultAspectRatio
                ),
                awbRegions = meteringRegionsFromMeteringPoints(
                    action.meteringPointsAwb,
                    sensorRect,
                    defaultAspectRatio
                )
            ).await().toFocusMeteringResult(true)
        }.asListenableFuture()
    }

    /**
     * Give whether auto focus trigger was desired, this method transforms a [Result3A] into
     * [FocusMeteringResult] by checking if the auto focus was locked in a focused state.
     */
    private fun Result3A.toFocusMeteringResult(shouldTriggerAf: Boolean): FocusMeteringResult {
        if (this.status != Result3A.Status.OK) {
            return FocusMeteringResult.create(false)
        }
        val isFocusSuccessful =
            if (shouldTriggerAf)
                this.frameMetadata?.get(CaptureResult.CONTROL_AF_STATE) ==
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
            else true

        return FocusMeteringResult.create(isFocusSuccessful)
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    companion object {
        const val METERING_WEIGHT_DEFAULT = MeteringRectangle.METERING_WEIGHT_MAX

        fun meteringRegionsFromMeteringPoints(
            meteringPoints: List<MeteringPoint>,
            cropSensorRegion: Rect,
            defaultAspectRatio: Rational,
        ): List<MeteringRectangle> {
            val meteringRegions: MutableList<MeteringRectangle> = ArrayList()
            val cropRegionAspectRatio =
                Rational(cropSensorRegion.width(), cropSensorRegion.height())

            // TODO(sushilnath@): limit the number of metering regions to what is supported by the
            // device.
            for (meteringPoint in meteringPoints) {
                if (!isValid(meteringPoint)) {
                    continue
                }
                // TODO(sushilnath@): Use the zoom based crop region aspect ratio instead of sensor
                // active array aspect ratio.
                val adjustedPoint: PointF =
                    getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio, defaultAspectRatio)
                val meteringRectangle: MeteringRectangle =
                    getMeteringRect(adjustedPoint, meteringPoint.size, cropSensorRegion)
                meteringRegions.add(meteringRectangle)
            }
            return meteringRegions
        }

        // Illustration ref : https://source.android.com/devices/camera/camera3_crop_reprocess
        private fun getFovAdjustedPoint(
            meteringPoint: MeteringPoint,
            cropRegionAspectRatio: Rational,
            defaultAspectRatio: Rational
        ): PointF {
            // Use default aspect ratio unless there is a custom aspect ratio in MeteringPoint.
            val fovAspectRatio = meteringPoint.surfaceAspectRatio ?: defaultAspectRatio
            if (fovAspectRatio != cropRegionAspectRatio) {
                if (fovAspectRatio.compareTo(cropRegionAspectRatio) > 0) {
                    val adjustedPoint = PointF(meteringPoint.x, meteringPoint.y)
                    // The crop region is taller than the FOV, top and bottom of the crop region is
                    // cropped.
                    val verticalPadding =
                        (fovAspectRatio.toDouble() / cropRegionAspectRatio.toDouble()).toFloat()
                    val topPadding = ((verticalPadding - 1.0) / 2).toFloat()
                    adjustedPoint.y = (topPadding + adjustedPoint.y) * (1f / verticalPadding)
                    return adjustedPoint
                } else {
                    val adjustedPoint = PointF(meteringPoint.x, meteringPoint.y)
                    // The crop region is wider than the FOV, left and right side of crop region is
                    // cropped
                    val horizontalPadding =
                        (cropRegionAspectRatio.toDouble() / fovAspectRatio.toDouble()).toFloat()
                    val leftPadding = ((horizontalPadding - 1.0) / 2).toFloat()
                    adjustedPoint.x = (leftPadding + adjustedPoint.x) * (1f / horizontalPadding)
                    return adjustedPoint
                }
            }
            return PointF(meteringPoint.x, meteringPoint.y)
        }

        // Given a normalized PointF and normalized size factor for width and height, calculate
        // the corresponding metering rectangle for the given crop region. The necessary
        // constraints are applies to make sure that the metering rectangle is bonded within the
        // crop region.
        private fun getMeteringRect(
            normalizedPointF: PointF,
            normalizedSize: Float,
            cropRegion: Rect
        ): MeteringRectangle {
            val centerX = (cropRegion.left + normalizedPointF.x * cropRegion.width()).toInt()
            val centerY = (cropRegion.top + normalizedPointF.y * cropRegion.height()).toInt()
            val width = (normalizedSize * cropRegion.width()).toInt()
            val height = (normalizedSize * cropRegion.height()).toInt()
            val focusRect = Rect(
                centerX - width / 2,
                centerY - height / 2,
                centerX + width / 2,
                centerY + height / 2
            )
            focusRect.left = focusRect.left.coerceIn(cropRegion.left, cropRegion.right)
            focusRect.right = focusRect.right.coerceIn(cropRegion.left, cropRegion.right)
            focusRect.top = focusRect.top.coerceIn(cropRegion.top, cropRegion.bottom)
            focusRect.bottom = focusRect.bottom.coerceIn(cropRegion.top, cropRegion.bottom)
            return MeteringRectangle(focusRect, METERING_WEIGHT_DEFAULT)
        }

        private fun isValid(pt: MeteringPoint): Boolean {
            return pt.x >= 0f && pt.x <= 1f && pt.y >= 0f && pt.y <= 1f
        }
    }
}