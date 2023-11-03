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
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.MeteringRegionCorrection
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl.OperationCanceledException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraControlInternal
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

/**
 * Implementation of focus and metering controls exposed by [CameraControlInternal].
 */
@CameraScope
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FocusMeteringControl @Inject constructor(
    private val cameraProperties: CameraProperties,
    private val meteringRegionCorrection: MeteringRegionCorrection,
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
    private val zoomCompat: ZoomCompat,
) : UseCaseCameraControl, UseCaseCamera.RunningUseCasesChangeListener {
    private var _useCaseCamera: UseCaseCamera? = null

    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
        }

    override fun onRunningUseCasesChanged() {
        // reset to null since preview use case may not be active for current runningUseCases
        previewAspectRatio = null

        _useCaseCamera?.runningUseCases?.forEach { useCase ->
            if (useCase is Preview) {
                useCase.attachedSurfaceResolution?.apply {
                    previewAspectRatio = Rational(width, height)
                }
            }
        }
    }

    override fun reset() {
        previewAspectRatio = null
        cancelFocusAndMeteringAsync()
    }

    private var previewAspectRatio: Rational? = null
    private val cropSensorRegion
        get() = zoomCompat.getCropSensorRegion()

    private val defaultAspectRatio: Rational
        get() = previewAspectRatio ?: Rational(cropSensorRegion.width(), cropSensorRegion.height())

    private val maxAfRegionCount =
        cameraProperties.metadata.getOrDefault(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0)
    private val maxAeRegionCount =
        cameraProperties.metadata.getOrDefault(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0)
    private val maxAwbRegionCount =
        cameraProperties.metadata.getOrDefault(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0)
    private var updateSignal: CompletableDeferred<FocusMeteringResult>? = null
    private var cancelSignal: CompletableDeferred<Result3A?>? = null

    fun startFocusAndMetering(
        action: FocusMeteringAction,
        autoFocusTimeoutMs: Long = AUTO_FOCUS_TIMEOUT_DURATION,
    ): ListenableFuture<FocusMeteringResult> {
        val signal = CompletableDeferred<FocusMeteringResult>()

        useCaseCamera?.let { useCaseCamera ->
            threads.sequentialScope.launch {
                cancelSignal?.setCancelException("Cancelled by another startFocusAndMetering()")
                updateSignal?.setCancelException("Cancelled by another startFocusAndMetering()")
                updateSignal = signal

                val aeRectangles = meteringRegionsFromMeteringPoints(
                    action.meteringPointsAe,
                    maxAeRegionCount,
                    cropSensorRegion,
                    defaultAspectRatio,
                    FocusMeteringAction.FLAG_AE,
                    meteringRegionCorrection,
                )
                val afRectangles = meteringRegionsFromMeteringPoints(
                    action.meteringPointsAf,
                    maxAfRegionCount,
                    cropSensorRegion,
                    defaultAspectRatio,
                    FocusMeteringAction.FLAG_AF,
                    meteringRegionCorrection,
                )
                val awbRectangles = meteringRegionsFromMeteringPoints(
                    action.meteringPointsAwb,
                    maxAwbRegionCount,
                    cropSensorRegion,
                    defaultAspectRatio,
                    FocusMeteringAction.FLAG_AWB,
                    meteringRegionCorrection,
                )
                if (aeRectangles.isEmpty() && afRectangles.isEmpty() && awbRectangles.isEmpty()) {
                    signal.completeExceptionally(
                        IllegalArgumentException(
                            "None of the specified AF/AE/AWB MeteringPoints is supported on" +
                                " this camera."
                        )
                    )
                    return@launch
                }
                if (afRectangles.isNotEmpty()) {
                    state3AControl.preferredFocusMode = CaptureRequest.CONTROL_AF_MODE_AUTO
                }
                val (isCancelEnabled, timeout) = if (action.isAutoCancelEnabled &&
                    action.autoCancelDurationInMillis < autoFocusTimeoutMs
                ) {
                    (true to action.autoCancelDurationInMillis)
                } else {
                    (false to autoFocusTimeoutMs)
                }

                /**
                 * If device does not support a 3A region, we should not update it at all.
                 * If device does support but a region list is empty, it means any previously
                 * set region should be removed, so the no-op METERING_REGIONS_DEFAULT is used.
                 */
                val result3A = useCaseCamera.requestControl.startFocusAndMeteringAsync(
                    aeRegions = if (maxAeRegionCount > 0)
                        aeRectangles.ifEmpty { METERING_REGIONS_DEFAULT.toList() }
                    else null,
                    afRegions = if (maxAfRegionCount > 0)
                        afRectangles.ifEmpty { METERING_REGIONS_DEFAULT.toList() }
                    else null,
                    awbRegions = if (maxAwbRegionCount > 0)
                        awbRectangles.ifEmpty { METERING_REGIONS_DEFAULT.toList() }
                    else null,
                    aeLockBehavior = if (maxAeRegionCount > 0)
                        Lock3ABehavior.AFTER_NEW_SCAN
                    else null,
                    afLockBehavior = if (maxAfRegionCount > 0)
                        Lock3ABehavior.AFTER_NEW_SCAN
                    else null,
                    awbLockBehavior = if (maxAwbRegionCount > 0)
                        Lock3ABehavior.AFTER_NEW_SCAN
                    else null,
                    afTriggerStartAeMode = cameraProperties.getSupportedAeMode(AeMode.ON),
                    timeLimitNs = TimeUnit.NANOSECONDS.convert(
                        timeout,
                        TimeUnit.MILLISECONDS
                    )
                ).await()

                if (result3A.status == Result3A.Status.SUBMIT_FAILED) {
                    signal.completeExceptionally(
                        OperationCanceledException("Camera is not active.")
                    )
                } else if (result3A.status == Result3A.Status.TIME_LIMIT_REACHED) {
                    if (isCancelEnabled) {
                        if (signal.isActive) {
                            cancelFocusAndMeteringNowAsync(useCaseCamera, signal)
                        }
                    } else {
                        signal.complete(FocusMeteringResult.create(false))
                    }
                } else {
                    signal.complete(result3A.toFocusMeteringResult(
                        shouldTriggerAf = afRectangles.isNotEmpty()
                    ))
                }
            }
        } ?: run {
            signal.completeExceptionally(
                OperationCanceledException("Camera is not active.")
            )
        }

        return signal.asListenableFuture()
    }

    fun isFocusMeteringSupported(
        action: FocusMeteringAction
    ): Boolean {
        val rectanglesAe = meteringRegionsFromMeteringPoints(
            action.meteringPointsAe,
            maxAeRegionCount,
            cropSensorRegion,
            defaultAspectRatio,
            FocusMeteringAction.FLAG_AE,
            meteringRegionCorrection,
        )
        val rectanglesAf = meteringRegionsFromMeteringPoints(
            action.meteringPointsAf,
            maxAfRegionCount,
            cropSensorRegion,
            defaultAspectRatio,
            FocusMeteringAction.FLAG_AF,
            meteringRegionCorrection,
        )
        val rectanglesAwb = meteringRegionsFromMeteringPoints(
            action.meteringPointsAwb,
            maxAwbRegionCount,
            cropSensorRegion,
            defaultAspectRatio,
            FocusMeteringAction.FLAG_AWB,
            meteringRegionCorrection,
        )
        return rectanglesAe.isNotEmpty() || rectanglesAf.isNotEmpty() || rectanglesAwb.isNotEmpty()
    }

    // TODO: Move this to a lower level so it is automatically checked for all requests
    private fun CameraProperties.getSupportedAeMode(preferredMode: AeMode): AeMode {
        val modes = metadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]?.map {
            AeMode.fromIntOrNull(it)
        } ?: return AeMode.OFF

        // if preferredMode is supported, use it
        if (modes.contains(preferredMode)) {
            return preferredMode
        }

        // if not found, priority is AE_ON > AE_OFF
        return if (modes.contains(AeMode.ON)) {
            AeMode.ON
        } else AeMode.OFF
    }

    fun cancelFocusAndMeteringAsync(): Deferred<Result3A?> {
        val signal = CompletableDeferred<Result3A?>()
        useCaseCamera?.let { useCaseCamera ->
            threads.sequentialScope.launch {
                cancelSignal?.setCancelException("Cancelled by another cancelFocusAndMetering()")
                cancelSignal = signal
                cancelFocusAndMeteringNowAsync(useCaseCamera, updateSignal).propagateTo(signal)
            }
        } ?: run {
            signal.completeExceptionally(OperationCanceledException("Camera is not active."))
        }

        return signal
    }

    private suspend fun cancelFocusAndMeteringNowAsync(
        useCaseCamera: UseCaseCamera,
        signalToCancel: CompletableDeferred<FocusMeteringResult>?,
    ): Deferred<Result3A> {
        signalToCancel?.setCancelException("Cancelled by cancelFocusAndMetering()")
        state3AControl.preferredFocusMode = null
        return useCaseCamera.requestControl.cancelFocusAndMeteringAsync()
    }

    private fun <T> CompletableDeferred<T>.setCancelException(message: String) {
        completeExceptionally(OperationCanceledException(message))
    }

    /**
     * Give whether auto focus trigger was desired, this method transforms a [Result3A] into
     * [FocusMeteringResult] by checking if the auto focus was locked in a focused state.
     */
    private fun Result3A.toFocusMeteringResult(shouldTriggerAf: Boolean): FocusMeteringResult {
        if (this.status != Result3A.Status.OK) {
            return FocusMeteringResult.create(false)
        }

        val resultAfState = frameMetadata?.get(CaptureResult.CONTROL_AF_STATE)

        /**
         * The sequence in which the conditions for isFocusSuccessful are checked is important,
         * since they represent the priorities for the conditions.
         *
         * For example, if isAfModeSupported is false, CameraX documentation dictates that
         * isFocusSuccessful will be true in result. However, CameraPipe will set
         * frameMetadata = null in this case as a kind of operation not allowed by camera.
         *
         * So we have to check isAfModeSupported first as it is a more specific case and higher
         * in priority. On the other hand, resultAfState == null matters only if the result comes
         * from a submitted request, so it should be checked after frameMetadata == null.
         *
         * @see FocusMeteringAction
         * @see androidx.camera.camera2.pipe.graph.Controller3A.lock3A
         */
        val isFocusSuccessful = when {
            !shouldTriggerAf -> false
            !cameraProperties.isAfModeSupported(AfMode.AUTO) -> true
            frameMetadata == null -> false
            resultAfState == null -> true
            else -> resultAfState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
        }

        return FocusMeteringResult.create(isFocusSuccessful)
    }

    private fun CameraProperties.isAfModeSupported(afMode: AfMode): Boolean {
        val modes = metadata[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES]?.map {
            AfMode.fromIntOrNull(it)
        } ?: return false

        return modes.contains(afMode)
    }

    companion object {
        const val METERING_WEIGHT_DEFAULT = MeteringRectangle.METERING_WEIGHT_MAX
        const val AUTO_FOCUS_TIMEOUT_DURATION = 5000L

        fun meteringRegionsFromMeteringPoints(
            meteringPoints: List<MeteringPoint>,
            maxRegionCount: Int,
            cropSensorRegion: Rect,
            defaultAspectRatio: Rational,
            @FocusMeteringAction.MeteringMode meteringMode: Int,
            meteringRegionCorrection: MeteringRegionCorrection,
        ): List<MeteringRectangle> {
            if (meteringPoints.isEmpty() || maxRegionCount == 0) {
                return emptyList()
            }
            val meteringRegions: MutableList<MeteringRectangle> = ArrayList()
            val cropRegionAspectRatio =
                Rational(cropSensorRegion.width(), cropSensorRegion.height())

            for (meteringPoint in meteringPoints) {
                // Only enable at most maxRegionCount.
                if (meteringRegions.size >= maxRegionCount) {
                    break
                }
                if (!isValid(meteringPoint)) {
                    continue
                }
                val adjustedPoint: PointF = getFovAdjustedPoint(
                    meteringPoint,
                    cropRegionAspectRatio,
                    defaultAspectRatio,
                    meteringMode,
                    meteringRegionCorrection
                )
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
            defaultAspectRatio: Rational,
            @FocusMeteringAction.MeteringMode meteringMode: Int,
            meteringRegionCorrection: MeteringRegionCorrection,
        ): PointF {
            // Use default aspect ratio unless there is a custom aspect ratio in MeteringPoint.
            val fovAspectRatio = meteringPoint.surfaceAspectRatio ?: defaultAspectRatio
            val correctedPoint = meteringRegionCorrection.getCorrectedPoint(
                meteringPoint,
                meteringMode
            )
            if (fovAspectRatio != cropRegionAspectRatio) {
                if (fovAspectRatio.compareTo(cropRegionAspectRatio) > 0) {
                    val adjustedPoint = PointF(correctedPoint.x, correctedPoint.y)
                    // The crop region is taller than the FOV, top and bottom of the crop region is
                    // cropped.
                    val verticalPadding =
                        (fovAspectRatio.toDouble() / cropRegionAspectRatio.toDouble()).toFloat()
                    val topPadding = ((verticalPadding - 1.0) / 2).toFloat()
                    adjustedPoint.y = (topPadding + adjustedPoint.y) * (1f / verticalPadding)
                    return adjustedPoint
                } else {
                    val adjustedPoint = PointF(correctedPoint.x, correctedPoint.y)
                    // The crop region is wider than the FOV, left and right side of crop region is
                    // cropped
                    val horizontalPadding =
                        (cropRegionAspectRatio.toDouble() / fovAspectRatio.toDouble()).toFloat()
                    val leftPadding = ((horizontalPadding - 1.0) / 2).toFloat()
                    adjustedPoint.x = (leftPadding + adjustedPoint.x) * (1f / horizontalPadding)
                    return adjustedPoint
                }
            }
            return PointF(correctedPoint.x, correctedPoint.y)
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

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(
            focusMeteringControl: FocusMeteringControl
        ): UseCaseCameraControl
    }
}
