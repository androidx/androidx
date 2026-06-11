/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.analytics.impl.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.work.analytics.WorkMetricsInfo
import java.util.UUID

/**
 * This class persists state transitions, tracking metadata, and timing details (such as enqueue and
 * execution durations) for analyzing WorkManager execution trends.
 */
@Entity(primaryKeys = ["work_spec_id", "generation", "period_count"])
internal data class WorkMetricsSpec(
    @ColumnInfo(name = "work_spec_id") val workSpecId: String,
    @ColumnInfo(name = "generation") val generation: Int,
    @ColumnInfo(name = "period_count") val periodCount: Int = 0,
    @ColumnInfo(name = "worker_class_name") val workerClassName: String?,
    @ColumnInfo(name = "state") var state: WorkMetricsInfo.State,
    @ColumnInfo(name = "tags") val tags: List<String>,
    @ColumnInfo(name = "enqueue_time_ms") var enqueueTimeMillis: Long = TIME_NOT_SET,
    @ColumnInfo(name = "unblock_time_ms") var unblockTimeMillis: Long = TIME_NOT_SET,
    @ColumnInfo(name = "first_start_time_ms") var firstStartTimeMillis: Long = TIME_NOT_SET,
    @ColumnInfo(name = "finish_time_ms") var finishTimeMillis: Long = TIME_NOT_SET,
    @ColumnInfo(name = "worker_duration_ms") var workerDurationMillis: Long = 0,
    @ColumnInfo(name = "total_runtime_ms") var totalRuntimeMillis: Long = 0,
    @ColumnInfo(name = "run_attempt_count") var runAttemptCount: Int = 0,
    @ColumnInfo(name = "explicit_retry_count") var explicitRetryCount: Int = 0,
    @ColumnInfo(name = "stop_reasons") var stopReasonCounts: Map<Int, Int> = emptyMap(),
) {
    companion object {
        const val TIME_NOT_SET: Long = -1L
    }

    fun toWorkMetricsInfo(): WorkMetricsInfo {
        return WorkMetricsInfo(
            workSpecId = UUID.fromString(workSpecId),
            generation = generation,
            workerClassName = workerClassName,
            tags = tags.toSet(),
            state = state,
            enqueueTimeMillis = enqueueTimeMillis,
            unblockTimeMillis = unblockTimeMillis,
            firstStartTimeMillis = firstStartTimeMillis,
            finishTimeMillis = finishTimeMillis,
            runAttemptCount = runAttemptCount,
            explicitRetryCount = explicitRetryCount,
            stopReasonCounts = stopReasonCounts,
        )
    }
}
