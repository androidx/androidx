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

package androidx.glance.adaptive.appwidget.ui.templates.track

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Covers [chunkBars], which keeps the hero chart inside Glance's ten-child container limit.
 *
 * The limit is enforced at translation time, not at composition time, so a composition test cannot
 * see a violation of it — an over-full container is truncated silently once on a real device. The
 * rule is therefore pinned here as the arithmetic it is.
 */
class BarChartBlockTest {

    @Test
    fun chunkBars_leavesASmallChartFlat() {
        // A week fits in one row, so nesting it would only add a container for nothing.
        assertThat(chunkBars(bars(7))).hasSize(1)
        assertThat(chunkBars(bars(7)).single()).hasSize(7)
    }

    @Test
    fun chunkBars_splitsAtTheContainerLimit() {
        assertThat(chunkBars(bars(10))).hasSize(1)
        assertThat(chunkBars(bars(11))).hasSize(2)
    }

    @Test
    fun chunkBars_balancesTheChunks() {
        // Not a row of ten and a row of four: chunks share the width equally, so an uneven split
        // would draw the first ten bars narrower than the last four.
        val chunks = chunkBars(bars(14))

        assertThat(chunks).hasSize(2)
        assertThat(chunks.map { it.size }).containsExactly(7, 7)
    }

    @Test
    fun chunkBars_padsAnUnevenRemainder() {
        // 13 bars split into two chunks of 7, the last of which is one short.
        val chunks = chunkBars(bars(13))

        assertThat(chunks.map { it.size }).containsExactly(7, 7)
        assertThat(chunks.flatten().count { it == null }).isEqualTo(1)
        assertThat(chunks.last().last()).isNull()
    }

    @Test
    fun chunkBars_neverExceedsTheContainerLimit() {
        // Both the outer row of chunks and every chunk itself have to stay under the limit.
        for (count in 1..100) {
            val chunks = chunkBars(bars(count))

            assertThat(chunks.size).isAtMost(MAX_CHILDREN)
            chunks.forEach { assertThat(it.size).isAtMost(MAX_CHILDREN) }
        }
    }

    @Test
    fun chunkBars_keepsEveryBarThatFits() {
        for (count in 1..100) {
            assertThat(chunkBars(bars(count)).flatten().filterNotNull()).hasSize(count)
        }
    }

    @Test
    fun chunkBars_dropsWhatCannotBeDrawnAtAll() {
        // Beyond a nested row of nested rows there is nowhere left to put a bar.
        assertThat(chunkBars(bars(140)).flatten().filterNotNull()).hasSize(100)
    }

    @Test
    fun chunkBars_handlesAnEmptyChart() {
        assertThat(chunkBars(emptyList())).isEmpty()
    }

    private fun bars(count: Int): List<TrackBar> =
        List(count) { TrackBar(label = "$it", value = 0.5f) }

    private companion object {
        /** Mirrors the production limit; see [chunkBars]. */
        const val MAX_CHILDREN = 10
    }
}
