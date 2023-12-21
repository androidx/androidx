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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CpuCounterTest {
    private val perfHardenProp = PropOverride("security.perf_harden", "0")
    private var shouldResetEnforce1 = false

    @Before
    fun before() {
        // TODO: make this automatic
        if (Build.VERSION.SDK_INT > 29) {
            val blockedBySelinux = Shell.isSELinuxEnforced()
            assumeTrue(
                "blocked by selinux = $blockedBySelinux, rooted = ${DeviceInfo.isRooted}",
                !blockedBySelinux || DeviceInfo.isRooted
            )
            if (blockedBySelinux && DeviceInfo.isRooted) {
                Shell.executeScriptSilent("setenforce 0")
                shouldResetEnforce1 = true
            }
            perfHardenProp.forceValue()
        }
        val error = CpuCounter.checkPerfEventSupport()
        assumeTrue(error, error == null)
    }

    @After
    fun after() {
        perfHardenProp.resetIfOverridden()
        if (shouldResetEnforce1) {
            Shell.executeScriptSilent("setenforce 1")
        }
    }

    /**
     * Extremely basic validation of CPU counters.
     *
     * Note that we don't try and do more advanced validation (e.g. ensuring
     * instructions != cycles != l1 misses), since this may be brittle.
     */
    @Test
    fun basic() = CpuCounter().use { counter ->
        val values = CpuCounter.Values()

        counter.resetEvents(
            listOf(
                CpuCounter.Event.Instructions,
                CpuCounter.Event.CpuCycles,
                CpuCounter.Event.L1IReferences,
            )
        )
        counter.reset()
        counter.start()
        repeat(100) {
            System.nanoTime() // just something to do
        }
        counter.stop()

        counter.read(values)

        // NOTE: these expected number of counters may not be safe, will adjust as
        // needed based on CI results
        if (DeviceInfo.isEmulator) {
            assertTrue(
                values.numberOfCounters >= 1,
                "expect at least one counter enabled on emulator," +
                    " saw ${values.numberOfCounters}"
            )
        } else {
            assertTrue(
                values.numberOfCounters >= 3,
                "expect at least three counters on physical device," +
                    " saw ${values.numberOfCounters}"
            )
        }
        assertNotEquals(0, values.timeEnabled)
        assertNotEquals(0, values.timeRunning)
        assertTrue(values.timeEnabled >= values.timeRunning)

        // As counters are enabled in order of ID, these are in order of ID as well
        if (values.numberOfCounters >= 1) {
            assertNotEquals(0, values.getValue(CpuCounter.Event.Instructions))
        }
        if (values.numberOfCounters >= 2) {
            assertNotEquals(0, values.getValue(CpuCounter.Event.CpuCycles))
        }
        if (values.numberOfCounters >= 3) {
            assertNotEquals(0, values.getValue(CpuCounter.Event.L1IMisses))
        }
    }

    @Test
    fun instructions() = CpuCounter().use { counter ->
        val values = CpuCounter.Values()

        counter.resetEvents(
            listOf(
                CpuCounter.Event.Instructions,
                CpuCounter.Event.CpuCycles,
                CpuCounter.Event.L1IReferences
            )
        )

        val instructions = List(4) {
            counter.reset()
            counter.start()

            // factor chosen because small numbers will cause test to fail on an emulator,
            // likely due to warmup
            repeat(it * 100) {
                // Simple work designed to have minimum amount of Java code
                System.nanoTime()
            }
            counter.stop()
            counter.read(values)
            values.getValue(CpuCounter.Event.Instructions)
        }

        assertTrue(instructions.all { it != 0L })

        // note, we don't validate 1st, in case there's some amount of warmup happening
        assertTrue(
            instructions[3] > instructions[2] && instructions[2] > instructions[1],
            "expected increasing instruction counts (ignoring 1st): ${instructions.joinToString()}"
        )
    }

    @Test
    fun read_withoutReset(): Unit = CpuCounter().use { counter ->
        val values = CpuCounter.Values()

        // not yet reset, should fail...
        assertFailsWith<IllegalStateException> {
            counter.read(values)
        }.also { ise ->
            assertTrue(ise.message!!.contains("read counters without reset"))
        }
    }

    @Test
    fun read_afterClose(): Unit = CpuCounter().use { counter ->
        val values = CpuCounter.Values()
        // reset, but closed / deleted, should fail...
        counter.resetEvents(listOf(CpuCounter.Event.Instructions))
        counter.close()
        assertFailsWith<IllegalStateException> {
            counter.read(values)
        }.also { ise ->
            assertTrue(ise.message!!.contains("read counters after close"))
        }
    }
}
