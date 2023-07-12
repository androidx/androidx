/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric.FrameDurationCpuNs
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric.FrameDurationUiNs
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric.FrameOverrunNs
import androidx.benchmark.macro.perfetto.FrameTimingQuery.getFrameSubMetrics
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameTimingQueryTest {
    @MediumTest
    @Test
    fun fixedTrace28() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api28_scroll", ".perfetto-trace")

        val frameSubMetrics = PerfettoTraceProcessor.runSingleSessionServer(
            traceFile.absolutePath
        ) {
            FrameTimingQuery.getFrameData(
                session = this,
                captureApiLevel = 28,
                packageName = "androidx.benchmark.integration.macrobenchmark.target"
            ).getFrameSubMetrics(captureApiLevel = 28)
        }

        assertEquals(
            expected = mapOf(
                FrameDurationCpuNs to listOf(9907605L, 6038595L, 4812136L, 3938490L),
                FrameDurationUiNs to listOf(3086979L, 2868490L, 2232709L, 1889479L)
            ),
            actual = frameSubMetrics.mapValues {
                it.value.subList(0, 4)
            }
        )
        assertEquals(
            expected = List(2) { 62 },
            actual = frameSubMetrics.map { it.value.size },
            message = "Expect same number of frames for each metric"
        )
    }

    @MediumTest
    @Test
    fun fixedTrace31() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_scroll", ".perfetto-trace")

        val frameSubMetrics = PerfettoTraceProcessor.runSingleSessionServer(
            traceFile.absolutePath
        ) {
            FrameTimingQuery.getFrameData(
                session = this,
                captureApiLevel = 31,
                packageName = "androidx.benchmark.integration.macrobenchmark.target"
            ).getFrameSubMetrics(captureApiLevel = 31)
        }

        assertEquals(
            expected = mapOf(
                FrameDurationCpuNs to listOf(6881407L, 5648542L, 3830261L, 4343438L),
                FrameDurationUiNs to listOf(2965052L, 3246407L, 1562188L, 1945469L),
                FrameOverrunNs to listOf(-5207137L, -11699862L, -14025295L, -12300155L)
            ),
            actual = frameSubMetrics.mapValues {
                it.value.subList(0, 4)
            }
        )
        assertEquals(
            expected = List(3) { 96 },
            actual = frameSubMetrics.map { it.value.size },
            message = "Expect same number of frames for each metric"
        )
    }

    /**
     * This validates that we're able to see all frames, even if expected/actual frame IDs don't
     * match UI thread and Renderthread (b/279088460)
     */
    @MediumTest
    @Test
    fun fixedTrace33_mismatchExpectedActualFrameIds() {
        assumeTrue(isAbiSupported())
        val traceFile =
            createTempFileFromAsset("api33_motionlayout_messagejson", ".perfetto-trace")

        val frameData = PerfettoTraceProcessor.runSingleSessionServer(
            traceFile.absolutePath
        ) {
            FrameTimingQuery.getFrameData(
                session = this,
                captureApiLevel = 33,
                packageName = "androidx.constraintlayout.compose.integration.macrobenchmark.target"
            )
        }

        // although there are 58 frames in the trace, the last 4
        // don't have associated complete expected/actual events
        assertEquals(54, frameData.size)

        // first frame, with matching IDs
        frameData.single {
            it.rtSlice.frameId == 1370854
        }.run {
            assertEquals(1370854, this.uiSlice.frameId)
            assertEquals(1370854, this.expectedSlice!!.frameId)
            assertEquals(1370854, this.actualSlice!!.frameId)
        }

        // second frame, where IDs don't match
        frameData.single {
            it.rtSlice.frameId == 1370869
        }.run {
            assertEquals(1370869, this.uiSlice.frameId) // matches
            assertEquals(1370876, this.expectedSlice!!.frameId) // doesn't match!
            assertEquals(1370876, this.actualSlice!!.frameId) // doesn't match!
        }

        assertEquals(
            // Note: it's correct for UI to be > CPU in cases below,
            // since UI is be sleeping after RT is done
            expected = mapOf(
                FrameDurationCpuNs to listOf(7304479L, 7567188L, 8064897L, 8434115L),
                FrameDurationUiNs to listOf(4253646L, 7592761L, 8088855L, 8461876L),
                FrameOverrunNs to listOf(-9009770L, -12199949L, -11299378L, -11708522L)
            ),
            actual = frameData.getFrameSubMetrics(captureApiLevel = 33).mapValues {
                it.value.subList(0, 4)
            }
        )
    }
}