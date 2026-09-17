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

import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.TrackTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Covers [TrackSizeSelector]'s own responsibility: mapping a container onto the breakpoints of its
 * surface.
 *
 * Which slots each breakpoint then enables belongs to [TrackSlots] and is covered by
 * [TrackSlotsTest]. The assertions here deliberately probe a single tier-sensitive property —
 * [TrackSlots.stackTitle], which is on at `W1` and off everywhere else — so that they fail for a
 * misplaced breakpoint rather than for a redesigned slot.
 */
class TrackSizeSelectorTest {

    private val template =
        TrackTemplate(
            title = "Morning Run",
            subtitle = "Cardio Workout",
            progress = 0.75f,
            statusText = "5.2 km",
        )

    @Test
    fun select_homeScreen_atW1Boundary_switchesAt132Dp() {
        assertThat(selectOnHomeScreen(widthDp = 131).stackTitle).isTrue()
        assertThat(selectOnHomeScreen(widthDp = 132).stackTitle).isFalse()
    }

    @Test
    fun select_lockScreen_atW1Boundary_switchesAt100Dp() {
        // The lock screen has its own, narrower breakpoints.
        val surface = AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN

        assertThat(select(surface, widthDp = 99, heightDp = 50).stackTitle).isTrue()
        assertThat(select(surface, widthDp = 100, heightDp = 50).stackTitle).isFalse()
    }

    @Test
    fun select_tabletHomeScreen_usesHomeScreenBreakpoints() {
        val surface = AppWidgetGlanceSurface.TABLET_HOME_SCREEN

        assertThat(select(surface, widthDp = 131).stackTitle).isTrue()
        assertThat(select(surface, widthDp = 132).stackTitle).isFalse()
    }

    @Test
    fun select_titleOrientation_isIndependentOfHeight() {
        for (heightDp in listOf(40, 90, 150, 250, 400)) {
            assertThat(selectOnHomeScreen(widthDp = 100, heightDp = heightDp).stackTitle).isTrue()
            assertThat(selectOnHomeScreen(widthDp = 300, heightDp = heightDp).stackTitle).isFalse()
        }
    }

    @Test
    fun select_sizesHeroFromContainer_notFromTier() {
        // Both are W4/H4, so a tier-only plan would size their charts identically.
        val short = selectOnHomeScreen(widthDp = 360, heightDp = 300)
        val tall = selectOnHomeScreen(widthDp = 360, heightDp = 420)

        assertThat(tall.barMaxHeight).isGreaterThan(short.barMaxHeight)
    }

    @Test
    fun select_isIndependentOfOptionalData() {
        // Selection may consult the payload, but must not currently depend on it.
        val sparseTemplate = TrackTemplate(title = "Morning Run")
        val constraints =
            HostConstraints(Dimensions(300, 200), AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)

        assertThat(TrackSizeSelector.select(sparseTemplate, constraints))
            .isEqualTo(TrackSizeSelector.select(template, constraints))
    }

    private fun select(
        surface: AppWidgetGlanceSurface,
        widthDp: Int,
        heightDp: Int = 200,
    ): TrackSlots =
        TrackSizeSelector.select(template, HostConstraints(Dimensions(widthDp, heightDp), surface))

    private fun selectOnHomeScreen(widthDp: Int, heightDp: Int = 200): TrackSlots =
        select(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, widthDp, heightDp)
}
