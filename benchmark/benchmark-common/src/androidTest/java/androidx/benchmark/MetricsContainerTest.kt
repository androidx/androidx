/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MetricsContainerTest {
    internal class FixedOutputCapture(names: List<String>, private val data: List<LongArray>) :
        MetricCapture(names) {
        private var repeatIndex = 0

        override fun captureStart(timeNs: Long) {}

        override fun capturePaused() {}

        override fun captureResumed() {}

        override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
            data[repeatIndex].copyInto(output, offset)
            repeatIndex++
        }
    }

    @Test
    fun basic() {
        val container =
            MetricsContainer(
                arrayOf(
                    FixedOutputCapture(
                        names = listOf("foo", "bar"),
                        data = listOf(longArrayOf(0, 6), longArrayOf(2, 8), longArrayOf(4, 10))
                    )
                ),
                repeatCount = 3
            )
        container.captureInit()
        repeat(3) {
            container.captureStart()
            container.captureStop()
        }
        assertEquals(
            listOf(
                MetricResult("foo", listOf(0.0, 1.0, 2.0)),
                MetricResult("bar", listOf(3.0, 4.0, 5.0))
            ),
            container.captureFinished(2) // divide measurements by 2
        )
    }

    @Test
    fun multiMetricCapture() {
        val container =
            MetricsContainer(
                arrayOf(
                    FixedOutputCapture(
                        names = listOf("foo", "bar"),
                        data = listOf(longArrayOf(0, 6), longArrayOf(2, 8), longArrayOf(4, 10))
                    ),
                    FixedOutputCapture(
                        names = listOf("baz"),
                        data = listOf(longArrayOf(12), longArrayOf(14), longArrayOf(16))
                    ),
                ),
                repeatCount = 3
            )
        container.captureInit()
        repeat(3) {
            container.captureStart()
            container.captureStop()
        }
        assertEquals(
            listOf(
                MetricResult("foo", listOf(0.0, 1.0, 2.0)),
                MetricResult("bar", listOf(3.0, 4.0, 5.0)),
                MetricResult("baz", listOf(6.0, 7.0, 8.0))
            ),
            container.captureFinished(2) // divide measurements by 2
        )
    }

    internal class CallOrderCapture(name: String) : MetricCapture(listOf(name)) {

        enum class Event {
            Start,
            Paused,
            Resumed,
            Stop
        }

        var lastEvent: Event? = null
            private set

        var lastOp: Int? = null
            private set

        override fun captureStart(timeNs: Long) {
            lastEvent = Event.Start
            lastOp = opOrder++
        }

        override fun capturePaused() {
            lastEvent = Event.Paused
            lastOp = opOrder++
        }

        override fun captureResumed() {
            lastEvent = Event.Resumed
            lastOp = opOrder++
        }

        override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
            lastEvent = Event.Stop
            lastOp = opOrder++
        }

        companion object {
            private var opOrder = 0
        }
    }

    @Test
    fun validatePriorityOrder() {
        // high should always be started first, and ended last - important behavior for more
        // sensitive metrics
        val high = CallOrderCapture(name = "highPriority")
        val low = CallOrderCapture(name = "lowPriority")

        val container = MetricsContainer(arrayOf(high, low), repeatCount = 1)
        container.captureInit()

        container.captureStart()
        assertEquals(CallOrderCapture.Event.Start, high.lastEvent)
        assertEquals(CallOrderCapture.Event.Start, low.lastEvent)
        assertEquals(high.lastOp!!, low.lastOp!! + 1) // start and resume, high is last

        container.capturePaused()
        assertEquals(CallOrderCapture.Event.Paused, high.lastEvent)
        assertEquals(CallOrderCapture.Event.Paused, low.lastEvent)
        assertEquals(high.lastOp!! + 1, low.lastOp!!) // pause and stop, high is first

        container.captureResumed()
        assertEquals(CallOrderCapture.Event.Resumed, high.lastEvent)
        assertEquals(CallOrderCapture.Event.Resumed, low.lastEvent)
        assertEquals(high.lastOp!!, low.lastOp!! + 1) // start and resume, high is last

        container.captureStop()
        assertEquals(CallOrderCapture.Event.Stop, high.lastEvent)
        assertEquals(CallOrderCapture.Event.Stop, low.lastEvent)
        assertEquals(high.lastOp!! + 1, low.lastOp!!) // pause and stop, high is first
    }
}
