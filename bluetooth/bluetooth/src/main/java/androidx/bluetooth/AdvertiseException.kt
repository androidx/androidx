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

import android.bluetooth.le.AdvertiseCallback as FwkAdvertiseCallback
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Exception indicating a failure to start an advertise operation.
 *
 * @property errorCode the error code for indicating the reason why the exception is thrown.
 */
class AdvertiseException(errorCode: Int) : BluetoothException(errorCode) {

    companion object {
        /** Advertise failed to start because the data is too large. */
        const val DATA_TOO_LARGE: Int = 10101

        /** Advertise failed to start because of too many advertisers. */
        const val TOO_MANY_ADVERTISERS: Int = 10102

        /** Advertise failed to start because of an internal error. */
        const val INTERNAL_ERROR: Int = 10103

        /** Advertise failed to start because the advertise feature is not supported. */
        const val UNSUPPORTED: Int = 10104
    }

    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(DATA_TOO_LARGE, TOO_MANY_ADVERTISERS, INTERNAL_ERROR, UNSUPPORTED)
    annotation class AdvertiseFail

    /** The error code associated with this exception. */
    override val errorCode: @AdvertiseFail Int =
        when (errorCode) {
            FwkAdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> DATA_TOO_LARGE
            FwkAdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> TOO_MANY_ADVERTISERS
            FwkAdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> INTERNAL_ERROR
            FwkAdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> UNSUPPORTED
            else -> ERROR_UNKNOWN
        }
}
