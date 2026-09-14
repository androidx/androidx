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

package androidx.glance.adaptive.appwidget.ui.templates

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Covers [TrackSlots] against the design's breakpoint matrix.
 *
 * Slot resolution is deliberately Compose-free, so the whole matrix is checked here rather than
 * through rendered output. Each cell is probed at the container size the design nominates for it.
 */
class TrackSlotsTest {

    @Test
    fun titleStacks_onlyAtTheNarrowestTier() {
        for (height in HeightTier.entries) {
            assertThat(slotsAt(WidthTier.W1, height).stackTitle).isTrue()
            assertThat(slotsAt(WidthTier.W2, height).stackTitle).isFalse()
            assertThat(slotsAt(WidthTier.W3, height).stackTitle).isFalse()
            assertThat(slotsAt(WidthTier.W4, height).stackTitle).isFalse()
        }
    }

    @Test
    fun tertiaryLabel_appearsFromW3() {
        for (height in HeightTier.entries) {
            assertThat(slotsAt(WidthTier.W2, height).showTertiary).isFalse()
            assertThat(slotsAt(WidthTier.W3, height).showTertiary).isTrue()
            assertThat(slotsAt(WidthTier.W4, height).showTertiary).isTrue()
        }
    }

    @Test
    fun tertiaryLabel_atW1_isDrivenByHeightInstead() {
        // A stacked title has only one column, so height takes over the role width plays elsewhere.
        assertThat(slotsAt(WidthTier.W1, HeightTier.H2).showTertiary).isFalse()
        assertThat(slotsAt(WidthTier.W1, HeightTier.H3).showTertiary).isTrue()
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).showTertiary).isTrue()
    }

    @Test
    fun supportingCount_growsWithWidth() {
        // H2 and taller all have room for the supporting row.
        for (height in listOf(HeightTier.H2, HeightTier.H3, HeightTier.H4)) {
            assertThat(slotsAt(WidthTier.W2, height).supportingCount).isEqualTo(1)
            assertThat(slotsAt(WidthTier.W3, height).supportingCount).isEqualTo(2)
            assertThat(slotsAt(WidthTier.W4, height).supportingCount).isEqualTo(3)
        }
    }

    @Test
    fun supportingRow_atW1_arrivesOnlyOnTheTallestTier() {
        assertThat(slotsAt(WidthTier.W1, HeightTier.H3).showSupporting).isFalse()
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).supportingCount).isEqualTo(1)
    }

    @Test
    fun supportingRow_isDroppedWhenTooShort() {
        for (width in listOf(WidthTier.W2, WidthTier.W3, WidthTier.W4)) {
            assertThat(slotsAt(width, HeightTier.H0).showSupporting).isFalse()
            assertThat(slotsAt(width, HeightTier.H1).showSupporting).isFalse()
            assertThat(slotsAt(width, HeightTier.H2).showSupporting).isTrue()
        }
    }

    @Test
    fun heroDensity_isDrivenByHeight() {
        for (width in listOf(WidthTier.W2, WidthTier.W3, WidthTier.W4)) {
            assertThat(slotsAt(width, HeightTier.H0).heroDensity).isEqualTo(TrackHeroDensity.NONE)
            assertThat(slotsAt(width, HeightTier.H1).heroDensity).isEqualTo(TrackHeroDensity.NONE)
            assertThat(slotsAt(width, HeightTier.H2).heroDensity).isEqualTo(TrackHeroDensity.NONE)
            assertThat(slotsAt(width, HeightTier.H3).heroDensity)
                .isEqualTo(TrackHeroDensity.COMPACT)
            assertThat(slotsAt(width, HeightTier.H4).heroDensity).isEqualTo(TrackHeroDensity.FULL)
        }
    }

    @Test
    fun hero_isNeverShownAtW1() {
        // A chart needs more horizontal room than the narrowest container has.
        for (height in HeightTier.entries) {
            assertThat(slotsAt(WidthTier.W1, height).showHero).isFalse()
        }
    }

    @Test
    fun heroDescription_isOnlyShownAtFullDensity() {
        assertThat(slotsAt(WidthTier.W4, HeightTier.H3).showHeroDescription).isFalse()
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).showHeroDescription).isTrue()
    }

    @Test
    fun headlineMetric_scalesWithWidth() {
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).primaryTextSize).isEqualTo(18.sp)
        assertThat(slotsAt(WidthTier.W2, HeightTier.H4).primaryTextSize).isEqualTo(30.sp)
        assertThat(slotsAt(WidthTier.W3, HeightTier.H4).primaryTextSize).isEqualTo(30.sp)
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).primaryTextSize).isEqualTo(40.sp)
    }

    @Test
    fun progressRing_shrinksSlightlyAtW1() {
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).ringSize).isEqualTo(54.dp)
        assertThat(slotsAt(WidthTier.W3, HeightTier.H4).ringSize).isEqualTo(56.dp)
    }

    @Test
    fun contentPadding_tightensOnTheShortestTiers() {
        assertThat(slotsAt(WidthTier.W3, HeightTier.H1).contentPadding).isEqualTo(16.dp)
        assertThat(slotsAt(WidthTier.W3, HeightTier.H2).contentPadding).isEqualTo(20.dp)
        assertThat(slotsAt(WidthTier.W3, HeightTier.H4).contentPadding).isEqualTo(20.dp)
    }

    @Test
    fun barSpacing_widensWithTheContainer() {
        assertThat(slotsAt(WidthTier.W2, HeightTier.H4).barSpacing).isEqualTo(6.dp)
        assertThat(slotsAt(WidthTier.W3, HeightTier.H4).barSpacing).isEqualTo(8.dp)
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).barSpacing).isEqualTo(16.dp)
    }

    @Test
    fun barMaxHeight_fillsTheSpaceTheOtherSlotsLeave() {
        // 348x264: 40 padding, 56 title, 2x14 slot spacing, 24 supporting, and 36 of chart
        // chrome leave 80, less a 4 dp safety margin.
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).barMaxHeight).isEqualTo(76.dp)
    }

    @Test
    fun barMaxHeight_growsWithTheContainer() {
        // Two containers in the same cell of the matrix, so only the measured height differs.
        val short = TrackSlots.from(SizeTiers(WidthTier.W4, HeightTier.H4), Dimensions(348, 300))
        val tall = TrackSlots.from(SizeTiers(WidthTier.W4, HeightTier.H4), Dimensions(348, 420))

        assertThat(tall.barMaxHeight - short.barMaxHeight).isEqualTo(120.dp)
    }

    @Test
    fun barMaxHeight_staysPositiveWhenTheHeroIsSqueezed() {
        // A container barely tall enough for its tier must not resolve to a zero-height chart.
        val cramped = TrackSlots.from(SizeTiers(WidthTier.W4, HeightTier.H3), Dimensions(348, 120))

        assertThat(cramped.barMaxHeight.value).isGreaterThan(0f)
    }

    @Test
    fun fullestCell_enablesEverything() {
        val slots = slotsAt(WidthTier.W4, HeightTier.H4)

        assertThat(slots.showTertiary).isTrue()
        assertThat(slots.showHero).isTrue()
        assertThat(slots.showHeroDescription).isTrue()
        assertThat(slots.supportingCount).isEqualTo(3)
    }

    @Test
    fun smallestCell_isTitleOnly() {
        for (width in WidthTier.entries) {
            val slots = slotsAt(width, HeightTier.H0)

            assertThat(slots.showHero).isFalse()
            assertThat(slots.showSupporting).isFalse()
        }
    }

    /** Resolves the plan for the container size the design nominates for a cell of the matrix. */
    private fun slotsAt(width: WidthTier, height: HeightTier): TrackSlots =
        nominalSlots(width, height)
}
