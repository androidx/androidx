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

package androidx.compose.ui.test

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DrawOnlyInvalidationTest {

    /**
     * Paused clock: the first frame is drawn by `waitForIdle`, and a subsequent draw-only
     * invalidation is also flushed by `waitForIdle`. Android-verified:
     * `afterSetContent=[0] afterFirstWait=[0] afterMutate=[0] afterWaitAfterMutate=[0, 1]`.
     */
    @Test
    fun pausedClock_drawInvalidationIsFlushedByWaitForIdle() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = mutableStateOf(0)
        val drawn = mutableListOf<Int>()
        setContent {
            Layout(
                modifier = Modifier.drawBehind { drawn.add(state.value) },
                measurePolicy = { _, _ -> layout(10, 10) {} }
            )
        }
        waitForIdle()
        assertEquals(listOf(0), drawn, "first frame should be drawn by waitForIdle")

        state.value = 1 // draw-only invalidation: no measure/layout/recomposition
        waitForIdle() // no clock advance
        assertEquals(listOf(0, 1), drawn, "draw-only update should be flushed by waitForIdle")
    }

    /**
     * Paused clock, measure invalidation (state read in measure): flushed by `waitForIdle`.
     * Android-verified: draw count 1 -> 2.
     */
    @Test
    fun pausedClock_measureInvalidationIsFlushedByWaitForIdle() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = mutableStateOf(0)
        val drawn = mutableListOf<Int>()
        setContent {
            Layout(
                modifier = Modifier.drawBehind { drawn.add(-1) },
                measurePolicy = { _, _ ->
                    val s = state.value // read in measure -> measure invalidation
                    layout(10 + s, 10) {}
                }
            )
        }
        waitForIdle()
        assertEquals(1, drawn.size)
        state.value = 1
        waitForIdle()
        assertEquals(2, drawn.size, "measure invalidation should trigger a redraw")
    }

    /**
     * Paused clock, recomposition invalidation (state read in composition) is NOT flushed without
     * advancing the clock: recomposition is driven by the frame clock, so a paused clock leaves it
     * pending (and, per [MainTestClock], pending recompositions are not awaited when autoAdvance is
     * false). Android-verified: `[0] -> [0]`.
     */
    @Test
    fun pausedClock_recompositionIsNotFlushedWithoutAdvancingClock() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = mutableStateOf(0)
        val drawn = mutableListOf<Int>()
        setContent {
            val v = state.value // read in composition -> recomposition invalidation
            Layout(
                modifier = Modifier.drawBehind { drawn.add(v) },
                measurePolicy = { _, _ -> layout(10, 10) {} }
            )
        }
        waitForIdle()
        assertEquals(listOf(0), drawn)
        state.value = 1
        waitForIdle()
        assertEquals(listOf(0), drawn, "recomposition should stay pending under a paused clock")
    }

    /** Running clock (autoAdvance=true, the default): draw-only invalidation flushed. */
    @Test
    fun runningClock_drawOnlyInvalidationIsFlushed() = runComposeUiTest {
        val state = mutableStateOf(0)
        val drawn = mutableListOf<Int>()
        setContent {
            Layout(
                modifier = Modifier.drawBehind { drawn.add(state.value) },
                measurePolicy = { _, _ -> layout(10, 10) {} }
            )
        }
        waitForIdle()
        assertEquals(listOf(0), drawn)
        state.value = 1
        waitForIdle()
        assertEquals(listOf(0, 1), drawn)
    }
}