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
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CpuEventCounterTest {
    @Before
    fun before() {
        // skip test if need root, or event fails to enable
        CpuEventCounter.forceEnable()?.let { errorMessage -> assumeTrue(errorMessage, false) }

        assumeFalse(DeviceInfo.isEmulator && Build.VERSION.SDK_INT == 28) // see b/357101113
    }

    @After
    fun after() {
        CpuEventCounter.reset()
    }

    /**
     * Extremely basic validation of CPU counters.
     *
     * Note that we don't try and do more advanced validation (e.g. ensuring instructions != cycles
     * != l1 misses), since this may be brittle.
     */
    @Test
    fun basic() =
        CpuEventCounter().use { counter ->
            val values = CpuEventCounter.Values()

            counter.resetEvents(
                listOf(
                    CpuEventCounter.Event.Instructions,
                    CpuEventCounter.Event.CpuCycles,
                    CpuEventCounter.Event.L1IReferences,
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
                        " saw ${values.numberOfCounters}",
                )
            } else {
                assertTrue(
                    values.numberOfCounters >= 3,
                    "expect at least three counters on physical device," +
                        " saw ${values.numberOfCounters}",
                )
            }
            assertNotEquals(0, values.timeEnabled)
            assertNotEquals(0, values.timeRunning)
            assertTrue(values.timeEnabled >= values.timeRunning)

            // As counters are enabled in order of ID, these are in order of ID as well
            if (values.numberOfCounters >= 1) {
                assertNotEquals(0, values.getValue(CpuEventCounter.Event.Instructions))
            }
            if (values.numberOfCounters >= 2) {
                assertNotEquals(0, values.getValue(CpuEventCounter.Event.CpuCycles))
            }
            if (values.numberOfCounters >= 3) {
                assertNotEquals(0, values.getValue(CpuEventCounter.Event.L1IReferences))
            }
        }

    @Test
    fun instructions() =
        CpuEventCounter().use { counter ->
            val values = CpuEventCounter.Values()

            counter.resetEvents(
                listOf(
                    CpuEventCounter.Event.Instructions,
                    CpuEventCounter.Event.CpuCycles,
                    CpuEventCounter.Event.L1IReferences,
                )
            )

            val instructions =
                List(4) {
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
                    values.getValue(CpuEventCounter.Event.Instructions)
                }

            assertTrue(instructions.all { it != 0L })

            // note, we don't validate 1st, in case there's some amount of warmup happening
            assertTrue(
                instructions[3] > instructions[2] && instructions[2] > instructions[1],
                "expected increasing instruction counts (ignoring 1st): ${instructions.joinToString()}",
            )
        }

    @Test
    fun tooManyEvents(): Unit =
        CpuEventCounter().use { counter ->
            val values = CpuEventCounter.Values()

            counter.resetEvents(
                listOf(
                    CpuEventCounter.Event.Instructions,
                    CpuEventCounter.Event.CpuCycles,
                    CpuEventCounter.Event.L1IReferences,
                    CpuEventCounter.Event.L1IMisses,
                    CpuEventCounter.Event.L1DReferences,
                    CpuEventCounter.Event.L1DMisses,
                )
            )

            counter.reset()
            counter.start()

            // factor chosen because small numbers will cause test to fail on an emulator,
            // likely due to warmup
            repeat(100) {
                // Simple work designed to have minimum amount of Java code
                System.nanoTime()
            }
            counter.stop()
            assertFailsWith<IllegalStateException> { counter.read(values) }
                .also { assertThat(it.message!!).contains("Observed 0 for instructions/cpuCycles") }
        }

    @Test
    fun read_withoutReset(): Unit =
        CpuEventCounter().use { counter ->
            val values = CpuEventCounter.Values()

            // not yet reset, should fail...
            assertFailsWith<IllegalStateException> { counter.read(values) }
                .also { ise -> assertTrue(ise.message!!.contains("read counters without reset")) }
        }

    @Test
    fun read_afterClose(): Unit =
        CpuEventCounter().use { counter ->
            val values = CpuEventCounter.Values()
            // reset, but closed / deleted, should fail...
            counter.resetEvents(listOf(CpuEventCounter.Event.Instructions))
            counter.close()
            assertFailsWith<IllegalStateException> { counter.read(values) }
                .also { ise -> assertTrue(ise.message!!.contains("read counters after close")) }
        }
}
