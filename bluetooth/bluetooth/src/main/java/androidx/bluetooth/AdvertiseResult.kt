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

import androidx.annotation.IntDef
import kotlin.annotation.Retention

/**
 * An advertise result indicates the result of a request to start advertising, whether success
 * or failure.
 *
 * @hide
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    AdvertiseResult.ADVERTISE_STARTED,
    AdvertiseResult.ADVERTISE_FAILED_DATA_TOO_LARGE,
    AdvertiseResult.ADVERTISE_FAILED_FEATURE_UNSUPPORTED,
    AdvertiseResult.ADVERTISE_FAILED_INTERNAL_ERROR,
    AdvertiseResult.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
)
public annotation class AdvertiseResult {
    companion object {
        /* Advertise started successfully. */
        public const val ADVERTISE_STARTED: Int = 1

        /* Advertise failed to start because the data is too large. */
        public const val ADVERTISE_FAILED_DATA_TOO_LARGE: Int = 2

        /* Advertise failed to start because the advertise feature is not supported. */
        public const val ADVERTISE_FAILED_FEATURE_UNSUPPORTED: Int = 3

        /* Advertise failed to start because of an internal error. */
        public const val ADVERTISE_FAILED_INTERNAL_ERROR: Int = 4

        /* Advertise failed to start because of too many advertisers. */
        public const val ADVERTISE_FAILED_TOO_MANY_ADVERTISERS: Int = 5
    }
}
