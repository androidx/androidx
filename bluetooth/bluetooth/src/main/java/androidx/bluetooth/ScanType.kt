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

import android.bluetooth.le.ScanSettings
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Bluetooth LE scan type that defines which scan results to send into the scan flow.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ScanType.ALL_MATCHES,
    ScanType.FIRST_MATCH,
    ScanType.MATCH_LOST
)
public annotation class ScanType {
    companion object {
        /* Send scan result for every Bluetooth advertisement that matches the filter criteria. */
        public const val ALL_MATCHES: Int = ScanSettings.CALLBACK_TYPE_ALL_MATCHES

        /**
         * Send scan result for only the first advertisement packet received that matches the filter
         * criteria.
         */
        public const val FIRST_MATCH: Int = ScanSettings.CALLBACK_TYPE_FIRST_MATCH

        /**
         * Send scan result when advertisements are no longer received from a device that has been
         * previously reported by a first match scan type.
         */
        public const val MATCH_LOST: Int = ScanSettings.CALLBACK_TYPE_MATCH_LOST
    }
}