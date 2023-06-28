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

package androidx.work

import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW
import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CHARGING
import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY
import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW
import androidx.annotation.RestrictTo

@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
value class StopReason internal constructor(val value: Int) {
    companion object {
        val ConstraintBatteryNotLow = StopReason(STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW)
        val ConstraintCharging = StopReason(STOP_REASON_CONSTRAINT_CHARGING)
        val ConstraintConnectivity = StopReason(STOP_REASON_CONSTRAINT_CONNECTIVITY)
        val ConstraintStorageNotLow = StopReason(STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW)
        val Unknown = StopReason(WorkInfo.STOP_REASON_UNKNOWN)
    }
}