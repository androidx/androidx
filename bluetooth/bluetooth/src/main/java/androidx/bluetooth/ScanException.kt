/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.bluetooth

import android.bluetooth.le.ScanCallback as FwkScanCallback
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Exception indicating a failure to start a scan operation.
 *
 * @property errorCode the error code for indicating the reason why the exception is thrown.
 */
class ScanException(errorCode: Int) : BluetoothException(errorCode) {

    companion object {
        /** Fails to start scan as app cannot be registered. */
        const val APPLICATION_REGISTRATION_FAILED: Int = 10201

        /** Fails to start scan due an internal error. */
        const val INTERNAL_ERROR: Int = 10202

        /** Fails to start power optimized scan as this feature is not supported. */
        const val UNSUPPORTED: Int = 10203

        /** Fails to start scan as it is out of hardware resources. */
        const val OUT_OF_HARDWARE_RESOURCES: Int = 10204

        /** Fails to start scan as application tries to scan too frequently. */
        const val SCANNING_TOO_FREQUENTLY: Int = 10205
    }

    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        APPLICATION_REGISTRATION_FAILED,
        INTERNAL_ERROR,
        UNSUPPORTED,
        OUT_OF_HARDWARE_RESOURCES,
        SCANNING_TOO_FREQUENTLY
    )
    annotation class ScanFail

    /** The error code associated with this exception. */
    override val errorCode: @ScanFail Int =
        when (errorCode) {
            FwkScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                APPLICATION_REGISTRATION_FAILED
            FwkScanCallback.SCAN_FAILED_INTERNAL_ERROR -> INTERNAL_ERROR
            FwkScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> UNSUPPORTED
            FwkScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> OUT_OF_HARDWARE_RESOURCES
            FwkScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> SCANNING_TOO_FREQUENTLY
            else -> ERROR_UNKNOWN
        }
}
