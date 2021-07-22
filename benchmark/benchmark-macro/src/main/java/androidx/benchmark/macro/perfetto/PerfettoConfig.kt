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

package androidx.benchmark.macro.perfetto

import perfetto.protos.DataSourceConfig
import perfetto.protos.FtraceConfig
import perfetto.protos.MeminfoCounters
import perfetto.protos.ProcessStatsConfig
import perfetto.protos.SysStatsConfig
import perfetto.protos.TraceConfig
import perfetto.protos.TraceConfig.BufferConfig
import perfetto.protos.TraceConfig.BufferConfig.FillPolicy

private val FTRACE_DATA_SOURCE = TraceConfig.DataSource(
    config = DataSourceConfig(
        name = "linux.ftrace",
        target_buffer = 0,
        ftrace_config = FtraceConfig(
            // These parameters affect only the kernel trace buffer size and how
            // frequently it gets moved into the userspace buffer defined above.
            buffer_size_kb = 16384,
            drain_period_ms = 250,
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
                // atrace categories currently come from default systrace tags
                "am",
                "binder_driver",
                "camera",
                "dalvik",
                "freq",
                "gfx",
                "hal",
                "idle",
                "input",
                "memreclaim",
                "res",
                "sched",
                "sync",
                "view",
                // "webview" not included to workaround b/190743595
                "wm",
                // "memory" not included as some Q devices requiring ftrace_event
                // configuration directly to collect this data. See b/171085599
            ),
            // Trace all apps for now
            atrace_apps = listOf("*")
        )
    )
)

private val PROCESS_STATS_DATASOURCE = TraceConfig.DataSource(
    config = DataSourceConfig(
        name = "linux.process_stats",
        target_buffer = 1,
        process_stats_config = ProcessStatsConfig(
            proc_stats_poll_ms = 10000
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

/**
 * Global config for perfetto.
 *
 * Eventually, this should be configurable.
 */
internal val PERFETTO_CONFIG = TraceConfig(
    buffers = listOf(
        BufferConfig(size_kb = 16384, FillPolicy.RING_BUFFER),
        BufferConfig(size_kb = 16384, FillPolicy.RING_BUFFER)
    ),
    data_sources = listOf(
        FTRACE_DATA_SOURCE,
        PROCESS_STATS_DATASOURCE,
        LINUX_SYS_STATS_DATASOURCE,
    ),
)