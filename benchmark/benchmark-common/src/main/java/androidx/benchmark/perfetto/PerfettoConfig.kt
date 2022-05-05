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

package androidx.benchmark.perfetto

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Shell
import perfetto.protos.DataSourceConfig
import perfetto.protos.AndroidPowerConfig
import perfetto.protos.FtraceConfig
import perfetto.protos.MeminfoCounters
import perfetto.protos.ProcessStatsConfig
import perfetto.protos.SysStatsConfig
import perfetto.protos.TraceConfig
import perfetto.protos.TraceConfig.BufferConfig
import perfetto.protos.TraceConfig.BufferConfig.FillPolicy

private fun ftraceDataSource(
    atraceApps: List<String>
) = TraceConfig.DataSource(
    config = DataSourceConfig(
        name = "linux.ftrace",
        target_buffer = 0,
        ftrace_config = FtraceConfig(
            ftrace_events = listOf(
                // We need to do process tracking to ensure kernel ftrace events targeted at short-lived
                // threads are associated correctly
                "task/task_newtask",
                "task/task_rename",
                "sched/sched_process_exit",
                "sched/sched_process_free",

                // Memory events
                "mm_event/mm_event_record",
                "kmem/rss_stat",
                "kmem/ion_heap_shrink",
                "kmem/ion_heap_grow",
                "ion/ion_stat",
                "oom/oom_score_adj_update",

                // Old (kernel) LMK
                "lowmemorykiller/lowmemory_kill",
            ),
            atrace_categories = listOf(
                AtraceTag.ActivityManager,
                AtraceTag.Audio,
                AtraceTag.BinderDriver,
                AtraceTag.Camera,
                AtraceTag.Dalvik,
                AtraceTag.Frequency,
                AtraceTag.Graphics,
                AtraceTag.HardwareModules,
                AtraceTag.Idle,
                AtraceTag.Input,
                AtraceTag.MemReclaim,
                AtraceTag.Resources,
                AtraceTag.Scheduling,
                AtraceTag.Synchronization,
                AtraceTag.View,
                AtraceTag.WindowManager
                // "webview" not included to workaround b/190743595
                // "memory" not included as some Q devices requiring ftrace_event
                // configuration directly to collect this data. See b/171085599
            ).filter {
                // filter to only supported tags on unrooted build
                // TODO: use root-only tags as needed
                it.supported(api = Build.VERSION.SDK_INT, rooted = false)
            }.map {
                it.tag
            },
            atrace_apps = atraceApps,
            compact_sched = FtraceConfig.CompactSchedConfig(enabled = true)
        )
    )
)

private val PROCESS_STATS_DATASOURCE = TraceConfig.DataSource(
    config = DataSourceConfig(
        name = "linux.process_stats",
        target_buffer = 1,
        process_stats_config = ProcessStatsConfig(
            proc_stats_poll_ms = 10000,
            // This flag appears to be unreliable on API 29 unbundled perfetto, so to avoid very
            // frequent proc stats polling to name processes correctly, we currently use unbundled
            // perfetto on API 29, even though the bundled version exists. (b/218668335)
            scan_all_processes_on_start = true
        )
    )
)

private val LINUX_SYS_STATS_DATASOURCE = TraceConfig.DataSource(
    config = DataSourceConfig(
        name = "linux.sys_stats",
        target_buffer = 1,
        sys_stats_config = SysStatsConfig(
            meminfo_period_ms = 1000,
            meminfo_counters = listOf(
                MeminfoCounters.MEMINFO_MEM_TOTAL,
                MeminfoCounters.MEMINFO_MEM_FREE,
                MeminfoCounters.MEMINFO_MEM_AVAILABLE,
                MeminfoCounters.MEMINFO_BUFFERS,
                MeminfoCounters.MEMINFO_CACHED,
                MeminfoCounters.MEMINFO_SWAP_CACHED,
                MeminfoCounters.MEMINFO_ACTIVE,
                MeminfoCounters.MEMINFO_INACTIVE,
                MeminfoCounters.MEMINFO_ACTIVE_ANON,
                MeminfoCounters.MEMINFO_INACTIVE_ANON,
                MeminfoCounters.MEMINFO_ACTIVE_FILE,
                MeminfoCounters.MEMINFO_INACTIVE_FILE,
                MeminfoCounters.MEMINFO_UNEVICTABLE,
                MeminfoCounters.MEMINFO_SWAP_TOTAL,
                MeminfoCounters.MEMINFO_SWAP_FREE,
                MeminfoCounters.MEMINFO_DIRTY,
                MeminfoCounters.MEMINFO_WRITEBACK,
                MeminfoCounters.MEMINFO_ANON_PAGES,
                MeminfoCounters.MEMINFO_MAPPED,
                MeminfoCounters.MEMINFO_SHMEM,
            )
        )
    )
)

private val ANDROID_POWER_DATASOURCE = TraceConfig.DataSource(
    config = DataSourceConfig(
        name = "android.power",
        android_power_config = AndroidPowerConfig(
            battery_poll_ms = 250,
            battery_counters = listOf(
                AndroidPowerConfig.BatteryCounters.BATTERY_COUNTER_CAPACITY_PERCENT,
                AndroidPowerConfig.BatteryCounters.BATTERY_COUNTER_CHARGE,
                AndroidPowerConfig.BatteryCounters.BATTERY_COUNTER_CURRENT
            ),
            collect_power_rails = true
        )
    )
)

/**
 * Config for perfetto.
 *
 * Eventually, this should be more configurable.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun perfettoConfig(
    atraceApps: List<String>
) = TraceConfig(
    buffers = listOf(
        BufferConfig(size_kb = 32768, FillPolicy.RING_BUFFER),
        BufferConfig(size_kb = 4096, FillPolicy.RING_BUFFER)
    ),
    data_sources = listOf(
        ftraceDataSource(atraceApps),
        PROCESS_STATS_DATASOURCE,
        LINUX_SYS_STATS_DATASOURCE,
        ANDROID_POWER_DATASOURCE,
        TraceConfig.DataSource(DataSourceConfig("android.surfaceflinger.frametimeline")),
        TraceConfig.DataSource(DataSourceConfig("track_event")) // required by tracing-perfetto
    ),
    // periodically dump to file, so we don't overrun our ring buffer
    // buffers are expected to be big enough for 5 seconds, so conservatively set 2.5 dump
    write_into_file = true,
    file_write_period_ms = 2500,

    // multiple of file_write_period_ms, enables trace processor to work in batches
    flush_period_ms = 5000
)

@RequiresApi(21) // needed for shell access
internal fun TraceConfig.validateAndEncode(): ByteArray {
    val ftraceConfig = data_sources
        .mapNotNull { it.config?.ftrace_config }
        .first()

    // check tags against known-supported tags based on SDK_INT / root status
    val supportedTags = AtraceTag.supported(
        api = Build.VERSION.SDK_INT,
        rooted = Shell.isSessionRooted()
    ).map { it.tag }.toSet()

    val unsupportedTags = (ftraceConfig.atrace_categories - supportedTags)
    check(unsupportedTags.isEmpty()) {
        "Error - attempted to use unsupported atrace tags: $unsupportedTags"
    }

    if (Build.VERSION.SDK_INT < 28) {
        check(!ftraceConfig.atrace_apps.contains("*")) {
            "Support for wildcard (*) app matching in atrace added in API 28"
        }
    }

    if (Build.VERSION.SDK_INT < 24) {
        val packageList = ftraceConfig.atrace_apps.joinToString(",")
        check(packageList.length <= 91) {
            "Unable to trace package list (\"$packageList\").length = " +
                "${packageList.length} > 91 chars, which is the limit before API 24"
        }
    }
    return encode()
}
