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

import android.app.Instrumentation
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MacroBenchmarkTest {

    lateinit var instrumentation: Instrumentation

    @Before
    fun setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    @Test
    @LargeTest
    @Ignore("Not running the test in CI")
    fun basicTest() {
        val collectors = listOf<Collector<*>>(
            CpuUsageCollector(),
            AppStartupCollector(),
            PerfettoCollector("basicTest")
        )
        val loopManager = LoopManager(packageName, instrumentation, collectors)

        loopManager.measureRepeated(2) { _ ->
            runWithMeasurementDisabled {
                pressHome()
                compile(SPEED)
                dropCaches()
                killProcess()
            }
            val context = instrumentation.context
            val intent: Intent =
                context.packageManager.getLaunchIntentForPackage(packageName)!!
            // Clear out any previous instances
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            instrumentation.device().wait(
                Until.hasObject(By.pkg(Companion.packageName).depth(0)),
                5000 /* ms */
            )
        }
    }

    companion object {
        const val packageName = "com.android.settings"
    }
}
