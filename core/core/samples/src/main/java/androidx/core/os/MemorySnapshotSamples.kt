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

package androidx.core.os

import androidx.annotation.Sampled

/** Sample demonstrating how to capture a [MemorySnapshot] and read memory statistics. */
@Sampled
fun captureMemorySnapshotSample() {
    val snapshot = MemorySnapshot.capture() ?: return

    // Resident memory metrics (in bytes)
    val rssBytes = snapshot.rssBytes
    val anonRssBytes = snapshot.anonRssBytes
    val fileRssBytes = snapshot.fileRssBytes
    val shmemRssBytes = snapshot.shmemRssBytes
    val peakRssBytes = snapshot.rssHwmBytes

    // Example: calculate total resident memory from resident memory of files, anonymous and shmem
    if (fileRssBytes >= 0 && anonRssBytes >= 0 && shmemRssBytes >= 0) {
        val sumRssBytes = anonRssBytes + fileRssBytes + shmemRssBytes
        // sumRssBytes should be equal (or close) to rssBytes
    }

    // Total virtual memory including swap (in bytes)
    val vssBytes = snapshot.vssBytes

    // Anonymous memory on swap (in bytes)
    val swapBytes = snapshot.swapBytes

    // Example: calculate total anonymous memory including swap
    if (anonRssBytes >= 0 && swapBytes >= 0) {
        val totalAnonBytes = anonRssBytes + swapBytes
    }

    // Cgroup memory usage (in bytes)
    val processCgroupBytes = snapshot.processMemoryUsageBytes
    val packageCgroupBytes = snapshot.packageMemoryUsageBytes

    // Example: Convert to megabytes and log or record metrics of resident anonymous memory
    val anonRssMb = if (anonRssBytes >= 0) anonRssBytes / (1024 * 1024) else -1
    recordAppMemoryMetric(anonRssMb)
}

/** Sample demonstrating how to measure the memory impact of an operation. */
@Sampled
fun trackMemoryUsageDeltaSample() {
    val before = MemorySnapshot.capture()

    performMemoryIntensiveOperation()

    val after = MemorySnapshot.capture()

    // On Android devices with zRAM enabled, memory pressure during an intensive
    // operation can cause the system to compress anonymous memory into swap.
    // Evaluating `after.anonRssBytes - before.anonRssBytes` alone can
    // underestimate the memory growth if anonymous pages were swapped out
    // during the operation. A better way is to combine both anonRssBytes and
    // swapBytes to evaluate the increase of anonymous memory usage.
    //
    // NOTE: the difference could be negative if GC happened during the operation,
    // which means more memory was released than allocated
    if (before != null && after != null) {
        // Track the difference in total anonymous memory
        if (
            before.anonRssBytes >= 0 &&
                after.anonRssBytes >= 0 &&
                before.swapBytes >= 0 &&
                after.swapBytes >= 0
        ) {
            val anonDeltaBytes =
                (after.anonRssBytes + after.swapBytes) - (before.anonRssBytes + before.swapBytes)
            recordMemoryDelta(anonDeltaBytes)
        }
    }
}

@Suppress("UNUSED_PARAMETER") private fun recordAppMemoryMetric(anonRssMb: Long) {}

@Suppress("UNUSED_PARAMETER") private fun recordMemoryDelta(deltaBytes: Long) {}

private fun performMemoryIntensiveOperation() {}
