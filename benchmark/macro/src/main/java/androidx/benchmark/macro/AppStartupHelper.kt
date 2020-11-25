/*
 * Copyright 2018 The Android Open Source Project
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

import android.util.Log
import com.android.helpers.StatsdHelper
import com.android.os.nano.AtomsProto
import java.util.ArrayList

/**
 * AppStartupHelper consist of helper methods to set the app
 * startup configs in statsd to track the app startup related
 * performance metrics and retrieve the necessary information from
 * statsd using the config id.
 */
class AppStartupHelper {
    private var isProcStartDetailsDisabled = false
    private val statsdHelper = StatsdHelper()

    /**
     * Set up the app startup statsd config to track the metrics during the app start occurred.
     */
    fun startCollecting() {
        Log.i("AppStartupHelper", "Adding app startup configs to statsd.")
        val atomIdList: MutableList<Int> = ArrayList()
        atomIdList.add(AtomsProto.Atom.APP_START_OCCURRED_FIELD_NUMBER)
        atomIdList.add(AtomsProto.Atom.APP_START_FULLY_DRAWN_FIELD_NUMBER)
        if (!isProcStartDetailsDisabled) {
            atomIdList.add(AtomsProto.Atom.PROCESS_START_TIME_FIELD_NUMBER)
        }
        if (!statsdHelper.addEventConfig(atomIdList)) {
            throw IllegalStateException("Unable to add event config to statsd")
        }
    }

    /**
     * Captures all startup events as a List<AppStartupMetrics>.
     *
     * Note - this function supports capturing multiple startups for a given package, but
     * [getMetrics] does not, to support only one value per metric, per iteration.
     */
    internal fun getMetricStructure(
        packageName: String
    ): List<AppStartupMetrics> {
        val list = statsdHelper.eventMetrics
        val appStartOccurredList = list
            .filter { it.atom.hasAppStartOccurred() }
            .map { it.atom.appStartOccurred }
            .filter { it.pkgName == packageName }

        val appStartFullyDrawnList = list
            .filter { it.atom.hasAppStartFullyDrawn() }
            .map { it.atom.appStartFullyDrawn }
            .filter { it.pkgName == packageName }

        val processStartTimeList = list
            .filter { it.atom.hasProcessStartTime() }
            .map { it.atom.processStartTime }
            .filter { it.processName == packageName }

        // Each startup may be split up into 3 events, based on what is recorded and when.
        // we merge these three lists, though tolerate if items are missing
        val eventLists = listOf(appStartOccurredList, appStartFullyDrawnList, processStartTimeList)
        val expectedEventCount = eventLists.map { it.size }.maxOrNull() ?: 0

        eventLists.forEach {
            if (it.isNotEmpty() && it.size != expectedEventCount) {
                throw AssertionError(
                    "Saw inconsistent number of startup events between" +
                        " occurred ${appStartOccurredList.size}" +
                        " fullyDrawn ${appStartFullyDrawnList.size}" +
                        " processStart ${processStartTimeList.size}"
                )
            }
        }
        return List(expectedEventCount) { index ->
            val appStart = appStartOccurredList.getOrNull(index)
            AppStartupMetrics(
                transitionType = appStart?.type?.toStartupMode(),
                windowDrawnDelayMs = appStart?.windowsDrawnDelayMillis?.toLong(),
                transitionDelayMs = appStart?.transitionDelayMillis?.toLong(),
                appStartupTimeMs = appStartFullyDrawnList.getOrNull(index)?.appStartupTimeMillis,
                processStartDelayMs = processStartTimeList.getOrNull(index)
                    ?.processStartDelayMillis?.toLong()
            )
        }
    }

    fun getMetrics(packageName: String): Map<String, Long> {
        val results = getMetricStructure(packageName)

        if (results.isEmpty()) {
            throw IllegalStateException(
                "Saw no launches of package $packageName, did you launch an activity?"
            )
        }
        if (results.size > 1) {
            throw IllegalStateException(
                "Saw more than one startup for package $packageName, this is not supported"
            )
        }
        val result = results.first()

        Log.d(
            "AppStartupHelper",
            "saw startup of package $packageName, type ${result.transitionType}"
        )
        return mapOf(
            // AppStartupHelper originally reports this as simply startup time, so we do the same
            "startupMs" to result.windowDrawnDelayMs,
            // Though the proto calls this appStartupTime, we clarify this is "fully drawn" startup
            "startupFullyDrawnMs" to result.appStartupTimeMs,

            // The following metrics are useful for platform devs, but disabled for now for brevity
            // "transitionDelayMs" to result.transitionDelayMs,
            // "processStartDelayMs" to result.processStartDelayMs,
        )
            .filterValues { it != null } // doesn't drop nullability for values...
            .mapValues { it.value!! } // ...so we do that explicitly here
    }

    /**
     * Remove the statsd config used to track the app startup metrics.
     */
    fun stopCollecting(): Boolean {
        return statsdHelper.removeStatsConfig()
    }

    /**
     * Disable process start detailed metrics.
     */
    fun setDisableProcStartDetails() {
        isProcStartDetailsDisabled = true
    }

    private fun Int.toStartupMode(): StartupMode? {
        return when (this) {
            AtomsProto.AppStartOccurred.COLD -> StartupMode.COLD
            AtomsProto.AppStartOccurred.WARM -> StartupMode.WARM
            AtomsProto.AppStartOccurred.HOT -> StartupMode.HOT
            else -> null
        }
    }

    data class AppStartupMetrics(
        val transitionType: StartupMode?,
        val windowDrawnDelayMs: Long?,
        val transitionDelayMs: Long?,
        val appStartupTimeMs: Long?,
        val processStartDelayMs: Long?,
    )
}
