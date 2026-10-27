/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.compose.remote.creation.compose.capture.headless

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HeadlessRenderFlowTest {

    /** Content emits no nodes; renders read snapshot state only. */
    private object NoopApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) {}

        override fun insertBottomUp(index: Int, instance: Unit) {}

        override fun remove(index: Int, count: Int) {}

        override fun move(from: Int, to: Int, count: Int) {}

        override fun onClear() {}
    }

    /** A render flow on the test scheduler's virtual time. */
    private fun <T : Any> TestScope.renderFlow(
        maxOutputs: Int = Int.MAX_VALUE,
        newRateLimiter: (TimeSource) -> UpdateRateLimiter? = { null },
        onNotQuiescent: (QuiescenceResult) -> Unit = {},
        onDispose: () -> Unit = {},
        content: @Composable () -> Unit = {},
        render: (renderIndex: Int) -> T,
    ): Flow<T> =
        headlessRenderFlow(
            name = "test",
            applier = NoopApplier,
            changeCount = { 0L },
            maxOutputs = maxOutputs,
            timeSource = testScheduler.timeSource,
            nanoTime = { testScheduler.currentTime * 1_000_000 },
            newRateLimiter = newRateLimiter,
            onNotQuiescent = onNotQuiescent,
            onDispose = onDispose,
            content = content,
            render = render,
        )

    @Test
    fun singleOutput_rendersOnceThenDisposes() = runTest {
        var disposed = false

        val outputs =
            renderFlow(maxOutputs = 1, onDispose = { disposed = true }) { "render $it" }.toList()

        assertThat(outputs).containsExactly("render 0")
        assertThat(disposed).isTrue()
    }

    @Test
    fun rerendersWhenStateReadByRenderChanges_andSkipsEqualOutputs() = runTest {
        val state = mutableStateOf(0)
        var renders = 0
        val outputs = mutableListOf<Int>()
        val collector = launch {
            renderFlow {
                renders++
                state.value / 2
            }
                .collect { outputs += it }
        }
        runCurrent()
        assertThat(outputs).containsExactly(0)

        // Renders again, but the output is unchanged, so nothing is emitted.
        Snapshot.withMutableSnapshot { state.value = 1 }
        runCurrent()
        assertThat(renders).isEqualTo(2)
        assertThat(outputs).containsExactly(0)

        Snapshot.withMutableSnapshot { state.value = 2 }
        runCurrent()
        assertThat(outputs).containsExactly(0, 1).inOrder()

        collector.cancel()
    }

    @Test
    fun unrelatedStateChange_doesNotRerender() = runTest {
        val read = mutableStateOf(0)
        val unrelated = mutableStateOf(0)
        var renders = 0
        val collector = launch {
            renderFlow {
                renders++
                read.value
            }
                .collect {}
        }
        runCurrent()

        repeat(5) {
            Snapshot.withMutableSnapshot { unrelated.value = it + 1 }
            runCurrent()
        }

        assertThat(renders).isEqualTo(1)
        collector.cancel()
    }

    @Test
    fun rateLimiter_holdsBackOutputsUntilAPermit() = runTest {
        val state = mutableStateOf(0)
        val emittedAt = mutableListOf<Long>()
        val collector = launch {
            renderFlow(newRateLimiter = { UpdateRateLimiter(1, 1_000.milliseconds, it) }) {
                    state.value
                }
                .collect { emittedAt += currentTime }
        }
        runCurrent()

        Snapshot.withMutableSnapshot { state.value = 1 }
        runCurrent()
        assertThat(emittedAt).containsExactly(0L)

        advanceTimeBy(1_001)
        runCurrent()
        assertThat(emittedAt).hasSize(2)
        assertThat(emittedAt[1]).isAtLeast(1_000L)

        collector.cancel()
    }

    @Test
    fun unsettledContent_reportsBeforeRendering() = runTest {
        val reported = mutableListOf<QuiescenceResult>()

        val outputs =
            renderFlow(
                    maxOutputs = 1,
                    onNotQuiescent = { reported += it },
                    content = { LaunchedEffect(Unit) { while (true) withFrameNanos {} } },
                ) {
                    "rendered"
                }
                .toList()

        assertThat(reported).containsExactly(QuiescenceResult.ContinuousFrames)
        assertThat(outputs).containsExactly("rendered")
    }
}
