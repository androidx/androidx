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
    internal class TestMetricCapture(
        names: List<String>,
        private val data: List<LongArray>
    ) : MetricCapture(names) {
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
        val container = MetricsContainer(
            arrayOf(
                TestMetricCapture(
                    names = listOf("foo", "bar"), data = listOf(
                        longArrayOf(0, 6),
                        longArrayOf(2, 8),
                        longArrayOf(4, 10)
                    )
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
        val container = MetricsContainer(
            arrayOf(
                TestMetricCapture(
                    names = listOf("foo", "bar"), data = listOf(
                        longArrayOf(0, 6),
                        longArrayOf(2, 8),
                        longArrayOf(4, 10)
                    )
                ),
                TestMetricCapture(
                    names = listOf("baz"), data = listOf(
                        longArrayOf(12),
                        longArrayOf(14),
                        longArrayOf(16)
                    )
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
}
