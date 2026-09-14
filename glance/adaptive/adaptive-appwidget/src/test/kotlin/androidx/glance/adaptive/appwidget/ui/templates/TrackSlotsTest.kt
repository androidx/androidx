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
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).primaryTextSize).isEqualTo(20.sp)
        assertThat(slotsAt(WidthTier.W2, HeightTier.H4).primaryTextSize).isEqualTo(30.sp)
        assertThat(slotsAt(WidthTier.W3, HeightTier.H4).primaryTextSize).isEqualTo(30.sp)
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).primaryTextSize).isEqualTo(40.sp)
    }

    @Test
    fun progressRing_isTheSameSizeWhereverItSitsBesideContent() {
        for (width in WidthTier.entries) {
            for (height in listOf(HeightTier.H2, HeightTier.H3, HeightTier.H4)) {
                assertThat(slotsAt(width, height).ringSize).isEqualTo(56.dp)
            }
        }
    }

    @Test
    fun containedRing_isResolvedAtTheShortestNarrowTiers() {
        assertThat(slotsAt(WidthTier.W1, HeightTier.H0).containedRing).isTrue()
        assertThat(slotsAt(WidthTier.W1, HeightTier.H1).containedRing).isTrue()
        assertThat(slotsAt(WidthTier.W1, HeightTier.H2).containedRing).isFalse()
    }

    @Test
    fun containedRing_isNeverResolvedOnAWiderTier() {
        for (width in listOf(WidthTier.W2, WidthTier.W3, WidthTier.W4)) {
            for (height in HeightTier.entries) {
                assertThat(slotsAt(width, height).containedRing).isFalse()
            }
        }
    }

    @Test
    fun containedRing_fillsItsContainer() {
        val slots = slotsAt(WidthTier.W1, HeightTier.H1)

        // An 88x88 circle with the design's 4 dp inset on each side.
        assertThat(slots.containedRingContainer).isEqualTo(88.dp)
        assertThat(slots.ringSize).isEqualTo(80.dp)
    }

    @Test
    fun containedRing_staysCircularInAnOblongContainer() {
        // Both are bounded by the shorter axis, so neither can spill out of an oblong container —
        // and the circle stays a circle rather than picking up a flat run along the longer axis.
        val oblong = TrackSlots.from(SizeTiers(WidthTier.W1, HeightTier.H1), Dimensions(128, 96))

        assertThat(oblong.containedRingContainer).isEqualTo(96.dp)
        assertThat(oblong.ringSize).isEqualTo(88.dp)
    }

    @Test
    fun containedRing_shrinksRatherThanOverflowAShortContainer() {
        val short = TrackSlots.from(SizeTiers(WidthTier.W1, HeightTier.H0), Dimensions(88, 52))

        assertThat(short.containedRingContainer).isEqualTo(52.dp)
        assertThat(short.ringSize).isEqualTo(44.dp)
    }

    @Test
    fun containedRingContainer_isUnsetWhereThereIsNoContainedRing() {
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).containedRingContainer).isEqualTo(0.dp)
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).containedRingContainer).isEqualTo(0.dp)
    }

    @Test
    fun ringBesideTheMetric_shrinksToTheContentBoxRatherThanOverflowIt() {
        // A launcher rarely hands over the design's exact row: this device reports the 88 dp row as
        // 85 dp, which leaves 53 dp once 16 dp of padding comes off each side. A 56 dp ring would
        // overflow and be clipped flat top and bottom, so it has to come down to the content box.
        val cell = TrackSlots.from(SizeTiers(WidthTier.W3, HeightTier.H1), Dimensions(289, 85))

        assertThat(cell.containedRing).isFalse()
        assertThat(cell.ringSize).isEqualTo(53.dp)
    }

    @Test
    fun ringAboveTheMetric_shrinksToTheContentWidthRatherThanOverflowIt() {
        // Stacked, the ring is bounded by the width instead: an 88 dp column reported as 85 dp
        // leaves 53 dp of content box, so a 56 dp ring would be clipped flat left and right.
        val cell = TrackSlots.from(SizeTiers(WidthTier.W1, HeightTier.H3), Dimensions(85, 176))

        assertThat(cell.stackTitle).isTrue()
        assertThat(cell.ringSize).isEqualTo(53.dp)
    }

    @Test
    fun ringBesideTheMetric_keepsItsNominalSizeWhereTheContentBoxAllowsIt() {
        val roomy = TrackSlots.from(SizeTiers(WidthTier.W3, HeightTier.H2), Dimensions(264, 132))

        assertThat(roomy.ringSize).isEqualTo(56.dp)
    }

    @Test
    fun primaryMaxWidth_isBoundedByTheRingWhenTheMetricSitsInsideIt() {
        // 80 dp ring, less the stroke either side.
        assertThat(slotsAt(WidthTier.W1, HeightTier.H1).primaryMaxWidth).isEqualTo(68.8f.dp)
    }

    @Test
    fun primaryMaxWidth_isTheWholeContentWidthWhenTheTitleStacks() {
        // 88 dp wide less 16 dp padding either side; the unit label sits underneath, not beside.
        assertThat(slotsAt(WidthTier.W1, HeightTier.H4).primaryMaxWidth).isEqualTo(56f.dp)
    }

    @Test
    fun primaryMaxWidth_leavesRoomForTheRingWhenTheTitleRunsAcross() {
        // 348 dp wide, less 20 dp padding either side, the 56 dp ring and the 8 dp gap after it.
        assertThat(slotsAt(WidthTier.W4, HeightTier.H4).primaryMaxWidth).isEqualTo(244f.dp)
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
