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
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsAutoFocusTrigger
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
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
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Implementation of focus and metering controls exposed by [CameraControlInternal]. */
@OptIn(ExperimentalCoroutinesApi::class)
@CameraScope
public class FocusMeteringControl
@Inject
constructor(
    private val cameraProperties: CameraProperties,
    private val meteringRegionCorrection: MeteringRegionCorrection,
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
    private val zoomCompat: ZoomCompat,
) : UseCaseCameraControl, UseCaseManager.RunningUseCasesChangeListener {
    private var _requestControl: UseCaseCameraRequestControl? = null

    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
        }

    override fun onRunningUseCasesChanged(runningUseCases: Set<UseCase>) {
        // reset to null since preview use case may not be active for current runningUseCases
        previewAspectRatio = null

        for (useCase in runningUseCases) {
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

    private var focusTimeoutJob: Job? = null
    private var autoCancelJob: Job? = null

    public fun startFocusAndMetering(
        action: FocusMeteringAction,
        autoFocusTimeoutMs: Long = AUTO_FOCUS_TIMEOUT_DURATION,
    ): ListenableFuture<FocusMeteringResult> {
        val signal = CompletableDeferred<FocusMeteringResult>()

        requestControl?.let { requestControl ->
            focusTimeoutJob?.cancel()
            autoCancelJob?.cancel()
            cancelSignal?.setCancelException("Cancelled by another startFocusAndMetering()")
            updateSignal?.setCancelException("Cancelled by another startFocusAndMetering()")

            updateSignal = signal

            val aeRectangles =
                meteringRegionsFromMeteringPoints(
                    action.meteringPointsAe,
                    maxAeRegionCount,
                    cropSensorRegion,
                    defaultAspectRatio,
                    FocusMeteringAction.FLAG_AE,
                    meteringRegionCorrection,
                )
            val afRectangles =
                meteringRegionsFromMeteringPoints(
                    action.meteringPointsAf,
                    maxAfRegionCount,
                    cropSensorRegion,
                    defaultAspectRatio,
                    FocusMeteringAction.FLAG_AF,
                    meteringRegionCorrection,
                )
            val awbRectangles =
                meteringRegionsFromMeteringPoints(
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
                return signal.asListenableFuture()
            }
            if (afRectangles.isNotEmpty()) {
                state3AControl.preferredFocusMode = CaptureRequest.CONTROL_AF_MODE_AUTO
            }

            val aeRegions =
                if (maxAeRegionCount > 0) aeRectangles.ifEmpty { METERING_REGIONS_DEFAULT.toList() }
                else null
            val afRegions =
                if (maxAfRegionCount > 0) afRectangles.ifEmpty { METERING_REGIONS_DEFAULT.toList() }
                else null
            val awbRegions =
                if (maxAwbRegionCount > 0)
                    awbRectangles.ifEmpty { METERING_REGIONS_DEFAULT.toList() }
                else null

            val deferredResult3A =
                if (afRectangles.isEmpty() || !cameraProperties.metadata.supportsAutoFocusTrigger) {
                    /*
                     * Controller3A.lock3A() returns early in such cases without updating the 3A
                     * regions which conflicts with [CameraControl.startFocusAndMetering] doc.
                     * However, we should update the regions explicitly here only in these cases
                     * instead of all cases because Controller3A.update3A() will invalidate
                     * the CameraGraph and thus may cause extra requests to the camera.
                     */
                    debug { "startFocusAndMetering: updating 3A regions only" }
                    requestControl.update3aRegions(
                        aeRegions = aeRegions,
                        afRegions = afRegions,
                        awbRegions = awbRegions,
                    )
                } else {
                    // No need to keep trying to focus if auto-cancel is already triggered
                    val finalFocusTimeout =
                        if (
                            action.isAutoCancelEnabled &&
                                action.autoCancelDurationInMillis < autoFocusTimeoutMs
                        ) {
                            action.autoCancelDurationInMillis
                        } else {
                            autoFocusTimeoutMs
                        }

                    debug { "startFocusAndMetering: updating 3A regions & triggering AF" }
                    /*
                     * If device does not support a 3A region, we should not update it at all.
                     * If device does support but a region list is empty, it means any previously
                     * set region should be removed, so the no-op METERING_REGIONS_DEFAULT is used.
                     */
                    requestControl.startFocusAndMeteringAsync(
                        aeRegions = aeRegions,
                        afRegions = afRegions,
                        awbRegions = awbRegions,
                        afLockBehavior =
                            if (maxAfRegionCount > 0) Lock3ABehavior.IMMEDIATE else null,
                        afTriggerStartAeMode = cameraProperties.getSupportedAeMode(AeMode.ON),
                        timeLimitNs =
                            TimeUnit.NANOSECONDS.convert(finalFocusTimeout, TimeUnit.MILLISECONDS)
                    )
                }

            deferredResult3A.propagateToFocusMeteringResultDeferred(
                resultDeferred = signal,
                shouldTriggerAf = afRectangles.isNotEmpty(),
            )

            // camera-pipe core layer invokes timeout when there is a new frame result from
            // camera, this is not precise enough for CameraX since it may allow auto-cancel to
            // be triggered first for same or very close timeout values
            triggerFocusTimeout(autoFocusTimeoutMs, signal)

            if (action.isAutoCancelEnabled) {
                triggerAutoCancel(action.autoCancelDurationInMillis, signal, requestControl)
            }
        }
            ?: run {
                signal.completeExceptionally(OperationCanceledException("Camera is not active."))
            }

        return signal.asListenableFuture()
    }

    private fun triggerAutoCancel(
        delayMillis: Long,
        resultToCancel: CompletableDeferred<FocusMeteringResult>,
        requestControl: UseCaseCameraRequestControl,
    ) {
        autoCancelJob?.cancel()

        autoCancelJob =
            threads.sequentialScope.launch {
                delay(delayMillis)
                debug { "triggerAutoCancel: auto-canceling after $delayMillis ms" }
                cancelFocusAndMeteringNowAsync(requestControl, resultToCancel)
            }
    }

    private fun triggerFocusTimeout(
        delayMillis: Long,
        resultToComplete: CompletableDeferred<FocusMeteringResult>,
    ) {
        focusTimeoutJob?.cancel()

        focusTimeoutJob =
            threads.sequentialScope.launch {
                delay(delayMillis)
                debug {
                    "triggerFocusTimeout:" +
                        " completing with focus result unsuccessful after $delayMillis ms"
                }
                resultToComplete.complete(FocusMeteringResult.create(false))
            }
    }

    private fun Deferred<Result3A>.propagateToFocusMeteringResultDeferred(
        resultDeferred: CompletableDeferred<FocusMeteringResult>,
        shouldTriggerAf: Boolean,
    ) {
        invokeOnCompletion { throwable ->
            if (throwable != null) {
                warn(throwable) {
                    "propagateToFocusMeteringResultDeferred: completed exceptionally!"
                }
                resultDeferred.completeExceptionally(throwable)
            } else {
                val result3A = getCompleted()
                debug { "propagateToFocusMeteringResultDeferred: result3A = $result3A" }
                if (result3A.status == Result3A.Status.SUBMIT_FAILED) {
                    resultDeferred.completeExceptionally(
                        OperationCanceledException("Camera is not active.")
                    )
                } else if (result3A.status == Result3A.Status.TIME_LIMIT_REACHED) {
                    resultDeferred.complete(FocusMeteringResult.create(false))
                } else {
                    resultDeferred.complete(
                        result3A.toFocusMeteringResult(shouldTriggerAf = shouldTriggerAf)
                    )
                }
            }
        }
    }

    public fun isFocusMeteringSupported(action: FocusMeteringAction): Boolean {
        val rectanglesAe =
            meteringRegionsFromMeteringPoints(
                action.meteringPointsAe,
                maxAeRegionCount,
                cropSensorRegion,
                defaultAspectRatio,
                FocusMeteringAction.FLAG_AE,
                meteringRegionCorrection,
            )
        val rectanglesAf =
            meteringRegionsFromMeteringPoints(
                action.meteringPointsAf,
                maxAfRegionCount,
                cropSensorRegion,
                defaultAspectRatio,
                FocusMeteringAction.FLAG_AF,
                meteringRegionCorrection,
            )
        val rectanglesAwb =
            meteringRegionsFromMeteringPoints(
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
        val modes =
            metadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]?.map {
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

    public fun cancelFocusAndMeteringAsync(): Deferred<Result3A?> {
        val signal = CompletableDeferred<Result3A?>()
        requestControl?.let { requestControl ->
            focusTimeoutJob?.cancel()
            autoCancelJob?.cancel()
            cancelSignal?.setCancelException("Cancelled by another cancelFocusAndMetering()")
            cancelSignal = signal
            cancelFocusAndMeteringNowAsync(requestControl, updateSignal).propagateTo(signal)
        }
            ?: run {
                signal.completeExceptionally(OperationCanceledException("Camera is not active."))
            }

        return signal
    }

    private fun cancelFocusAndMeteringNowAsync(
        requestControl: UseCaseCameraRequestControl,
        signalToCancel: CompletableDeferred<FocusMeteringResult>?,
    ): Deferred<Result3A> {
        signalToCancel?.setCancelException("Cancelled by cancelFocusAndMetering()")
        state3AControl.preferredFocusMode = null
        return requestControl.cancelFocusAndMeteringAsync()
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
         * isFocusSuccessful will be true in result. However, CameraPipe will set frameMetadata =
         * null in this case as a kind of operation not allowed by camera.
         *
         * So we have to check isAfModeSupported first as it is a more specific case and higher in
         * priority. On the other hand, resultAfState == null matters only if the result comes from
         * a submitted request, so it should be checked after frameMetadata == null.
         *
         * @see FocusMeteringAction
         * @see androidx.camera.camera2.pipe.graph.Controller3A.lock3A
         */
        val isFocusSuccessful =
            when {
                !shouldTriggerAf -> false
                !cameraProperties.isAfModeSupported(AfMode.AUTO) -> true
                frameMetadata == null -> false
                resultAfState == null -> true
                else -> resultAfState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
            }

        return FocusMeteringResult.create(isFocusSuccessful)
    }

    private fun CameraProperties.isAfModeSupported(afMode: AfMode): Boolean {
        val modes =
            metadata[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES]?.map {
                AfMode.fromIntOrNull(it)
            } ?: return false

        return modes.contains(afMode)
    }

    public companion object {
        public const val METERING_WEIGHT_DEFAULT: Int = MeteringRectangle.METERING_WEIGHT_MAX
        public const val AUTO_FOCUS_TIMEOUT_DURATION: Long = 5000L

        public fun meteringRegionsFromMeteringPoints(
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
                val adjustedPoint: PointF =
                    getFovAdjustedPoint(
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
            val correctedPoint =
                meteringRegionCorrection.getCorrectedPoint(meteringPoint, meteringMode)
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
            val focusRect =
                Rect(
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
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(
            focusMeteringControl: FocusMeteringControl
        ): UseCaseCameraControl
    }
}
