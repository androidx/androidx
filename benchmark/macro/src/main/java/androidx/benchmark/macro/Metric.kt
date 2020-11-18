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

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.helpers.CpuUsageHelper
import com.android.helpers.JankCollectionHelper
import com.android.helpers.TotalPssHelper

/**
 * Metric interface.
 */
sealed class Metric {
    abstract fun configure(config: MacrobenchmarkConfig)

    abstract fun start()

    abstract fun stop()

    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a
     *  general config object coming into [start].
     */
    abstract fun getMetrics(packageName: String): Map<String, Long>
}

class StartupTimingMetric : Metric() {
    private val helper = AppStartupHelper()

    override fun configure(config: MacrobenchmarkConfig) {
        // does nothing
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String): Map<String, Long> {
        return helper.getMetrics(packageName)
    }
}

class CpuUsageMetric : Metric() {
    private val helper = CpuUsageHelper().also {
        it.setEnableCpuUtilization()
    }

    override fun configure(config: MacrobenchmarkConfig) {
        // does nothing
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String): Map<String, Long> {
        return helper.metrics
    }
}

class JankMetric : Metric() {
    private lateinit var packageName: String
    private val helper = JankCollectionHelper()

    override fun configure(config: MacrobenchmarkConfig) {
        packageName = config.packageName
        helper.addTrackedPackages(packageName)
    }

    override fun start() {
        try {
            helper.startCollecting()
        } catch (exception: RuntimeException) {
            // Ignore the exception that might result from trying to clear GfxInfo
            // The current implementation of JankCollectionHelper throws a RuntimeException
            // when that happens. This is safe to ignore because the app being benchmarked
            // is not showing any UI when this happens typically.

            // Once the MacroBenchmarkRule has the ability to setup the app in the right state via
            // a designated setup block, we can get rid of this.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            if (instrumentation != null) {
                val device = UiDevice.getInstance(instrumentation)
                val result = device.executeShellCommand("ps -A | grep $packageName")
                if (!result.isNullOrEmpty()) {
                    error(exception.message ?: "Assertion error (Found $packageName)")
                }
            }
        }
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String): Map<String, Long> {
        return helper.metrics.mapValues {
            it.value.toLong()
        }
    }
}

class TotalPssMetric : Metric() {
    private val helper = TotalPssHelper()

    override fun configure(config: MacrobenchmarkConfig) {
        helper.setUp(config.packageName)
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String): Map<String, Long> {
        return helper.metrics
    }
}
