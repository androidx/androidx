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

package androidx.benchmark.macro

/**
 * Metric interface.
 *
 * Duplicate of Collector interface, but avoids exposing helper impl detail.
 *
 * TODO: remove collector impl detail, use helpers more directly
 */
sealed class Metric constructor(internal val collector: Collector<*>) {
    fun start() = collector.start()

    fun stop() = collector.stop()
}

class StartupTimingMetric : Metric(AppStartupCollector())

class CpuUsageMetric : Metric(CpuUsageCollector())