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
import androidx.core.uwb.RangingResult
import androidx.core.uwb.exceptions.UwbHardwareNotAvailableException
import com.google.android.gms.common.api.ApiException
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
        else -> RangingResult.RANGING_FAILURE_REASON_UNKNOWN
    }
}
