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

package androidx.benchmark.macro.test

import android.content.Intent
import androidx.benchmark.macro.AppStartupHelper
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.withPermissiveSeLinuxPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class AppStartupHelperTest {
    @MediumTest
    @Test
    fun noResults() = withPermissiveSeLinuxPolicy {
        val helper = AppStartupHelper()
        helper.startCollecting()
        helper.stopCollecting()
        // assert equals to get clearer error in exception
        assertEquals(
            listOf<AppStartupHelper.AppStartupMetrics>(),
            helper.getMetricStructure("fake.package.fiction.nostartups")
        )
    }

    /**
     * Launch [TrivialStartupActivity], and validate we see startupFullyDrawnMs if
     * [reportFullyDrawn] is `true`.
     *
     * Note that unlike an actual macrobenchmark, we're launching an activity in our own process,
     * since we don't care about perf or profiling or anything, just that we can observe the launch.
     */
    private fun validateStartupResults(reportFullyDrawn: Boolean) = withPermissiveSeLinuxPolicy {
        val packageName = "androidx.benchmark.macro.test"
        val helper = AppStartupHelper()
        helper.startCollecting()
        val scope = MacrobenchmarkScope(packageName, launchWithClearTask = false)
        // note: don't killProcess first, that's our process too!
        // additionally, skip pressHome, since it's not needed, and skipping saves significant time
        scope.launchPackageAndWait {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.putExtra(TrivialStartupActivity.EXTRA_REPORT_FULLY_DRAWN, reportFullyDrawn)
        }
        helper.stopCollecting()

        val metricsMap = helper.getMetrics(packageName)
        assertTrue(metricsMap.isNotEmpty())
        assertTrue(metricsMap.containsKey("startupMs"))
        assertEquals(reportFullyDrawn, metricsMap.containsKey("startupFullyDrawnMs"))
    }

    @LargeTest
    @Test
    fun startupMetrics_reportFullyDrawn_false() = validateStartupResults(false)

    @LargeTest
    @Test
    fun startupMetrics_reportFullyDrawn_true() = validateStartupResults(true)
}