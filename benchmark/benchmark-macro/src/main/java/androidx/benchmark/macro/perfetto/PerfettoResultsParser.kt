/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.benchmark.macro.MetricsWithUiState
import org.json.JSONObject

internal object PerfettoResultsParser {
    fun parseStartupResult(jsonMetricResults: String, packageName: String): MetricsWithUiState {
        val json = JSONObject(jsonMetricResults)
        json.optJSONObject("android_startup")?.let { androidStartup ->
            androidStartup.optJSONArray("startup")?.let { startup ->
                for (i in 0 until startup.length()) {
                    val startupResult = startup.getJSONObject(i)
                    if (startupResult.optString("package_name") == packageName) {
                        // NOTE: we return the startup for this process, and ignore any more
                        return startupResult.parseStartupMetricsWithUiState()
                    }
                }
            }
        }

        return MetricsWithUiState.EMPTY
    }

    private fun JSONObject.parseStartupMetricsWithUiState(): MetricsWithUiState {
        val durMs = getJSONObject("to_first_frame").getDouble("dur_ms")

        val eventTimestamps = optJSONObject("event_timestamps")
        val timelineStart = eventTimestamps?.optLong("intent_received")
        val timelineEnd = eventTimestamps?.optLong("first_frame")
        return MetricsWithUiState(
            metrics = mapOf("startupMs" to durMs.toLong()),
            timelineStart = timelineStart,
            timelineEnd = timelineEnd
        )
    }
}
