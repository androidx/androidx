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

class TrackSizeSelectorTest {

    private val template =
        TrackTemplate(
            title = "Morning Run",
            subtitle = "Cardio Workout",
            progress = 0.75f,
            statusText = "5.2 km",
        )

    @Test
    fun select_homeScreen_w1_isThin() {
        assertThat(selectOnHomeScreen(widthDp = 120)).isEqualTo(TrackArchetype.THIN)
    }

    @Test
    fun select_homeScreen_w2AndWider_isStandard() {
        assertThat(selectOnHomeScreen(widthDp = 180)).isEqualTo(TrackArchetype.STANDARD)
        assertThat(selectOnHomeScreen(widthDp = 260)).isEqualTo(TrackArchetype.STANDARD)
        assertThat(selectOnHomeScreen(widthDp = 400)).isEqualTo(TrackArchetype.STANDARD)
    }

    @Test
    fun select_homeScreen_atW1Boundary_switchesAt130Dp() {
        assertThat(selectOnHomeScreen(widthDp = 129)).isEqualTo(TrackArchetype.THIN)
        assertThat(selectOnHomeScreen(widthDp = 130)).isEqualTo(TrackArchetype.STANDARD)
    }

    @Test
    fun select_lockScreen_atW1Boundary_switchesAt100Dp() {
        assertThat(select(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN, widthDp = 99, heightDp = 50))
            .isEqualTo(TrackArchetype.THIN)
        assertThat(select(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN, widthDp = 100, heightDp = 50))
            .isEqualTo(TrackArchetype.STANDARD)
    }

    @Test
    fun select_tabletHomeScreen_usesHomeScreenBreakpoints() {
        assertThat(select(AppWidgetGlanceSurface.TABLET_HOME_SCREEN, widthDp = 129))
            .isEqualTo(TrackArchetype.THIN)
        assertThat(select(AppWidgetGlanceSurface.TABLET_HOME_SCREEN, widthDp = 130))
            .isEqualTo(TrackArchetype.STANDARD)
    }

    @Test
    fun select_isIndependentOfHeight() {
        for (heightDp in listOf(40, 90, 150, 250, 400)) {
            assertThat(selectOnHomeScreen(widthDp = 100, heightDp = heightDp))
                .isEqualTo(TrackArchetype.THIN)
            assertThat(selectOnHomeScreen(widthDp = 300, heightDp = heightDp))
                .isEqualTo(TrackArchetype.STANDARD)
        }
    }

    @Test
    fun select_isIndependentOfOptionalData() {
        val sparseTemplate = TrackTemplate(title = "Morning Run")

        assertThat(
                TrackSizeSelector.select(
                    sparseTemplate,
                    HostConstraints(
                        Dimensions(100, 200),
                        AppWidgetGlanceSurface.MOBILE_HOME_SCREEN,
                    ),
                )
            )
            .isEqualTo(TrackArchetype.THIN)
        assertThat(
                TrackSizeSelector.select(
                    sparseTemplate,
                    HostConstraints(
                        Dimensions(300, 200),
                        AppWidgetGlanceSurface.MOBILE_HOME_SCREEN,
                    ),
                )
            )
            .isEqualTo(TrackArchetype.STANDARD)
    }

    private fun select(
        surface: AppWidgetGlanceSurface,
        widthDp: Int,
        heightDp: Int = 200,
    ): TrackArchetype =
        TrackSizeSelector.select(template, HostConstraints(Dimensions(widthDp, heightDp), surface))

    private fun selectOnHomeScreen(widthDp: Int, heightDp: Int = 200): TrackArchetype =
        select(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, widthDp, heightDp)
}
