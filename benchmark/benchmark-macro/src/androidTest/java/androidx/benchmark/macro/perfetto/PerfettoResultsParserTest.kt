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

import androidx.benchmark.macro.IterationResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@SmallTest
@RunWith(AndroidJUnit4::class)
class PerfettoResultsParserTest {
    private fun startupJson(fullyDrawn: Boolean): String {
        val fullyDrawnString = if (fullyDrawn) {
            """
            "report_fully_drawn": {
                "dur_ns": 204445333,
                "dur_ms": 204.445333
            },
            """
        } else {
            ""
        }
        return """
    {
      "android_startup": {
        "startup": [
          {
            "startup_id": 2,
            "package_name": "androidx.benchmark.macro.test",
            "process_name": "androidx.benchmark.macro.test",
            "zygote_new_process": 0,
            "to_first_frame": {
              "dur_ns": 149438504,
              "main_thread_by_task_state": {
                "running_dur_ns": 66840634,
                "runnable_dur_ns": 13585470,
                "uninterruptible_sleep_dur_ns": 2215416,
                "interruptible_sleep_dur_ns": 45290784
              },
              "other_processes_spawned_count": 0,
              "time_activity_manager": {
                "dur_ns": 12352501,
                "dur_ms": 12.352501
              },
              "time_activity_start": {
                "dur_ns": 53247818,
                "dur_ms": 53.247818
              },
              "time_activity_resume": {
                "dur_ns": 11945314,
                "dur_ms": 11.945314
              },
              "time_choreographer": {
                "dur_ns": 45386619,
                "dur_ms": 45.386619
              },
              "dur_ms": 149.438504,
              "time_inflate": {
                "dur_ns": 8330678,
                "dur_ms": 8.330678
              },
              "time_get_resources": {
                "dur_ns": 1426719,
                "dur_ms": 1.426719
              },
              "time_verify_class": {
                "dur_ns": 7012711,
                "dur_ms": 7.012711
              },
              "mcycles_by_core_type": {
                "little": 415,
                "big": 446,
                "bigger": 152
              },
              "jit_compiled_methods": 19,
              "time_jit_thread_pool_on_cpu": {
                "dur_ns": 6647968,
                "dur_ms": 6.647968
              }
            },
            "activity_hosting_process_count": 1,
            "process": {
              "name": "androidx.benchmark.macro.test",
              "uid": 10327
            },
            $fullyDrawnString
            "activities": [
              {
                "name": "androidx.benchmark.macro.ConfigurableActivity",
                "method": "performCreate",
                "ts_method_start": 345883126877037
              },
              {
                "name": "androidx.benchmark.macro.ConfigurableActivity",
                "method": "performResume",
                "ts_method_start": 345883158971676
              }
            ],
            "event_timestamps": {
              "intent_received": 345883080735887,
              "first_frame": 345883230174391
            }
          }
        ]
      }
    }
    """
    }

    @Test
    fun parseStartupResult_notFullyDrawn() {
        assertEquals(
            PerfettoResultsParser.parseStartupResult(
                startupJson(fullyDrawn = false),
                "androidx.benchmark.macro.test"
            ),
            IterationResult(
                singleMetrics = mapOf("startupMs" to 149.438504),
                sampledMetrics = emptyMap(),
                timelineRangeNs = 345883080735887..345883230174391
            )
        )
    }

    @Test
    fun parseStartupResult_fullyDrawn() {
        assertEquals(
            PerfettoResultsParser.parseStartupResult(
                startupJson(fullyDrawn = true),
                "androidx.benchmark.macro.test"
            ),
            IterationResult(
                singleMetrics = mapOf(
                    "startupMs" to 149.438504,
                    "fullyDrawnMs" to 204.445333
                ),
                sampledMetrics = emptyMap(),
                timelineRangeNs = 345883080735887..345883230174391
            )
        )
    }
}
