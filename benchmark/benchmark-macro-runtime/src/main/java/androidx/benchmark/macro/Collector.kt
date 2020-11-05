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

import androidx.benchmark.InstrumentationResults.instrumentationReport
import com.android.helpers.ICollectorHelper

/**
 * Represents an entity that can be used to collect macro benchmark data.
 * @param T the type of the metric being collected.
 */
internal interface Collector<T : Any> {
    fun start(): Boolean
    fun stop(): Boolean
    fun metrics(): Map<String, T>
}

internal fun List<Collector<*>>.start() {
    this.forEach { it.start() }
}

internal fun List<Collector<*>>.stop() {
    this.forEach { it.stop() }
}

internal fun List<Collector<*>>.report() {
    instrumentationReport {
        val summary = this@report.flatMap { collector ->
            collector.metrics().map { (key, metric) ->
                "$key: '$metric'"
            }
        }.joinToString(separator = "\n")
        this.ideSummaryRecord(summary)
    }
}

/**
 * Converts a [ICollectorHelper] to a [Collector].
 */
internal fun <T : Any> ICollectorHelper<T>.collector(): Collector<T> {
    return object : Collector<T> {
        override fun start() = startCollecting()

        override fun stop() = stopCollecting()

        override fun metrics() = metrics
    }
}
