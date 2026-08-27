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

package androidx.core.uwb.helper

import android.content.Context
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingResult
import androidx.core.uwb.SensorFusionResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.exceptions.UwbHardwareNotAvailableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.uwb.PreciseEstimateInfo
import com.google.android.gms.nearby.uwb.RangingPosition
import com.google.android.gms.nearby.uwb.UwbDevice
import com.google.android.gms.nearby.uwb.UwbStatusCodes

internal const val UWB_FEATURE = "android.hardware.uwb"

/** Returns whether the Uwb System Feature is available on the device. */
internal fun isSystemFeatureAvailable(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(UWB_FEATURE)
}

/** Checks if the uwb system feature is supported and throws an UwbApiException otherwise. */
internal fun checkSystemFeature(context: Context) {
    if (!isSystemFeatureAvailable(context)) {
        throw UwbHardwareNotAvailableException("UWB Hardware is not available on this device.")
    }
}

internal fun getFailureReasonFromApiException(e: ApiException): Int {
    return when (e.statusCode) {
        UwbStatusCodes.INVALID_API_CALL -> RangingResult.RANGING_FAILURE_REASON_BAD_PARAMETERS
        UwbStatusCodes.RANGING_ALREADY_STARTED ->
            RangingResult.RANGING_FAILURE_REASON_FAILED_TO_START
        UwbStatusCodes.SERVICE_NOT_AVAILABLE -> RangingResult.RANGING_FAILURE_REASON_SYSTEM_POLICY
        UwbStatusCodes.UWB_SYSTEM_CALLBACK_FAILURE ->
            RangingResult.RANGING_FAILURE_REASON_SYSTEM_POLICY
        UwbStatusCodes.ARCORE_NOT_INSTALLED,
        UwbStatusCodes.ARCORE_APK_TOO_OLD ->
            RangingResult.RANGING_FAILURE_REASON_ARCORE_APK_INSTALL_NEEDED
        else -> RangingResult.RANGING_FAILURE_REASON_UNKNOWN
    }
}

/** @throws [SecurityException] if the provided [ApiException] indicates a missing permission. */
internal fun getFallbackReasonFromApiException(e: ApiException): Int? {
    return when (e.statusCode) {
        UwbStatusCodes.PRECISION_FINDING_NOT_AVAILABLE ->
            SensorFusionResult.FALLBACK_REASON_NOT_AVAILABLE
        UwbStatusCodes.PRECISION_FINDING_HARDWARE_AOA_PRECEDENCE ->
            SensorFusionResult.FALLBACK_REASON_HARDWARE_AOA_PRECEDENCE
        UwbStatusCodes.ARCORE_CAMERA_NOT_AVAILABLE ->
            SensorFusionResult.FALLBACK_REASON_ARCORE_CAMERA_NOT_AVAILABLE
        UwbStatusCodes.ARCORE_MISSING_GL_CONTEXT ->
            SensorFusionResult.FALLBACK_REASON_ARCORE_MISSING_GL_CONTEXT
        UwbStatusCodes.ARCORE_SDK_TOO_OLD -> SensorFusionResult.FALLBACK_REASON_ARCORE_SDK_TOO_OLD
        UwbStatusCodes.ARCORE_DEVICE_NOT_COMPATIBLE ->
            SensorFusionResult.FALLBACK_REASON_ARCORE_DEVICE_NOT_COMPATIBLE
        UwbStatusCodes.ARCORE_MISSING_CAMERA_PERMISSION ->
            throw SecurityException("Permission denied (missing CAMERA permission)", e)
        else -> null
    }
}

internal fun gmsRangingPositionToJetpack(
    device: UwbDevice,
    position: RangingPosition,
): RangingResult.RangingResultPosition {
    return RangingResult.RangingResultPosition(
        gmsDeviceToJetpack(device),
        androidx.core.uwb.RangingPosition(
            RangingMeasurement(position.distance.value),
            position.azimuth?.let { RangingMeasurement(it.value) },
            position.elevation?.let { RangingMeasurement(it.value) },
            position.elapsedRealtimeNanos,
        ),
    )
}

internal fun gmsDeviceToJetpack(device: UwbDevice): androidx.core.uwb.UwbDevice {
    return androidx.core.uwb.UwbDevice(UwbAddress(device.address.address))
}

internal fun gmsEstimateFailureReasonToJetpack(
    reason: @PreciseEstimateInfo.EstimateFailureReason Int
): Int {
    return when (reason) {
        PreciseEstimateInfo.EstimateFailureReason.NOT_AVAILABLE ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_NOT_AVAILABLE
        PreciseEstimateInfo.EstimateFailureReason.INSUFFICIENT_LIGHT ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_INSUFFICIENT_LIGHT
        PreciseEstimateInfo.EstimateFailureReason.EXCESSIVE_MOTION ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_EXCESSIVE_MOTION
        PreciseEstimateInfo.EstimateFailureReason.INSUFFICIENT_FEATURES ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_INSUFFICIENT_FEATURES
        PreciseEstimateInfo.EstimateFailureReason.CAMERA_NOT_AVAILABLE ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_CAMERA_UNAVAILABLE
        PreciseEstimateInfo.EstimateFailureReason.PEER_MOVING ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_PEER_MOVING
        PreciseEstimateInfo.EstimateFailureReason.INCONCLUSIVE_RESULT ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_INCONCLUSIVE_RESULT
        PreciseEstimateInfo.EstimateFailureReason.BAD_STATE ->
            SensorFusionResult.ESTIMATE_FAILURE_REASON_BAD_STATE
        else -> SensorFusionResult.ESTIMATE_FAILURE_REASON_NOT_AVAILABLE
    }
}
