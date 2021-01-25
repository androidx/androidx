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

package androidx.benchmark.perfetto

import org.json.JSONArray
import org.json.JSONObject

object PerfettoResultsParser {
    fun parseResult(jsonTrace: String, packageName: String): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        val json = JSONObject(jsonTrace)
        val androidStartup = json.optJSONObject(ANDROID_STARTUP)
        if (androidStartup != null) {
            val startup = androidStartup.optJSONArray(STARTUP)
            if (startup != null && startup.length() > 0) {
                parseStartupResult(startup, packageName, map)
            }
        }
        return map
    }

    private fun parseStartupResult(
        json: JSONArray,
        packageName: String,
        map: MutableMap<String, Long>
    ) {
        val length = json.length()
        for (i in 0 until length) {
            val startupResult = json.getJSONObject(i)
            val targetPackageName = startupResult.optString(PACKAGE_NAME)
            if (packageName == targetPackageName) {
                val firstFrameMetric = startupResult.optJSONObject(TO_FIRST_FRAME)
                if (firstFrameMetric != null) {
                    val duration = firstFrameMetric.optDouble(DUR_MS, 0.0)
                    map[STARTUP_MS] = duration.toLong()
                }
            }
        }
    }

    private const val ANDROID_STARTUP = "android_startup"
    private const val STARTUP = "startup"
    private const val PACKAGE_NAME = "package_name"
    private const val TO_FIRST_FRAME = "to_first_frame"
    private const val DUR_MS = "dur_ms"
    private const val TIME_ACTIVITY_START = "time_activity_start"
    private const val TIME_ACTIVITY_RESUME = "time_activity_resume"

    // Metric Keys
    private const val STARTUP_MS = "perfetto_startupMs"
}
