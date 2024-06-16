/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.junit4

import android.annotation.SuppressLint
import android.os.Looper
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BenchmarkRuleTest {
    @get:Rule val benchmarkRule: BenchmarkRule = BenchmarkRule()

    @SuppressLint("BanThreadSleep") // doesn't affect runtime, since we have target time
    @Test
    fun runWithTimingDisabled() {
        benchmarkRule.measureRepeated { runWithTimingDisabled { Thread.sleep(5) } }
        val min = benchmarkRule.getState().getMinTimeNanos()
        Assert.assertTrue(
            "minimum $min should be less than 1ms",
            min < TimeUnit.MILLISECONDS.toNanos(1)
        )
    }

    @Test
    fun measureRepeatedMainThread() {
        var scheduledOnMain = false

        // validate rethrow behavior
        assertFailsWith<IllegalStateException> {
            benchmarkRule.measureRepeatedOnMainThread {
                scheduledOnMain = Looper.myLooper() == Looper.getMainLooper()

                throw IllegalStateException("just a test")
            }
        }

        // validate work done on main thread
        assertTrue(scheduledOnMain)

        // let a benchmark actually run, so "benchmark hasn't finished" isn't thrown
        benchmarkRule.measureRepeatedOnMainThread {}
    }

    @SmallTest
    @Test
    @UiThreadTest
    fun measureRepeatedOnMainThread_throwOnMain() {
        assertEquals(Looper.myLooper(), Looper.getMainLooper())
        // validate rethrow behavior
        val exception =
            assertFailsWith<IllegalStateException> {
                benchmarkRule.measureRepeatedOnMainThread {
                    // Doesn't matter
                }
            }
        assertTrue(
            exception.message!!.contains(
                "Cannot invoke measureRepeatedOnMainThread from the main thread"
            )
        )
    }
}
