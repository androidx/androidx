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

package androidx.benchmark

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
internal fun String.toSnakeCase(): String = replace(Regex("([a-z])([A-Z0-9])")) {
    it.groups[1]!!.value + "_" + it.groups[2]!!.value.lowercase()
}

/**
 * Converts a metric name from the camelCase JSON output format to the snake_case version
 * used by AndroidX CI, and reducing abbreviations for clarity.
 *
 * This functionality is a stopgap as we migrate to actually using JSON in CI.
 */
internal fun String.toOutputMetricName() = this
    .toSnakeCase()
    .replace(Regex("_ns$"), "_nanos")
    .replace(Regex("_ms$"), "_millis")