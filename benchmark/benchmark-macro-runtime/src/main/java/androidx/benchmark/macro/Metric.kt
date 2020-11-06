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

import com.android.helpers.CpuUsageHelper

/**
 * Metric interface.
 */
sealed class Metric {
    abstract fun start()

    abstract fun stop()

    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a
     *  general config object coming into [start].
     */
    abstract fun getMetrics(packageName: String): Map<String, List<Long>>
}

class StartupTimingMetric : Metric() {
    private val helper = AppStartupHelper()

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String): Map<String, List<Long>> {
        return helper.getMetrics(packageName)
    }
}

class CpuUsageMetric : Metric() {
    private val helper = CpuUsageHelper().also {
        it.setEnableCpuUtilization()
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String): Map<String, List<Long>> {
        return helper.metrics.mapValues { listOf(it.value) }
    }
}
