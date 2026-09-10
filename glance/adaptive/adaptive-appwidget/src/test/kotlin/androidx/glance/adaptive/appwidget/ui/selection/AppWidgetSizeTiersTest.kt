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

package androidx.glance.adaptive.appwidget.ui.selection

import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppWidgetSizeTiersTest {

    @Test
    fun widthTier_fromDp_homeScreenBreakpoints() {
        val surface = AppWidgetGlanceSurface.MOBILE_HOME_SCREEN
        assertThat(AppWidgetSizeTiers.resolveWidthTier(50, surface)).isEqualTo(WidthTier.W1)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(129, surface)).isEqualTo(WidthTier.W1)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(130, surface)).isEqualTo(WidthTier.W2)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(219, surface)).isEqualTo(WidthTier.W2)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(220, surface)).isEqualTo(WidthTier.W3)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(309, surface)).isEqualTo(WidthTier.W3)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(310, surface)).isEqualTo(WidthTier.W4)
        assertThat(AppWidgetSizeTiers.resolveWidthTier(600, surface)).isEqualTo(WidthTier.W4)
    }

    @Test
    fun widthTier_fromDp_tabletHomeScreenBreakpoints() {
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(100, AppWidgetGlanceSurface.TABLET_HOME_SCREEN)
            )
            .isEqualTo(WidthTier.W1)
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(200, AppWidgetGlanceSurface.TABLET_HOME_SCREEN)
            )
            .isEqualTo(WidthTier.W2)
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(250, AppWidgetGlanceSurface.TABLET_HOME_SCREEN)
            )
            .isEqualTo(WidthTier.W3)
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(400, AppWidgetGlanceSurface.TABLET_HOME_SCREEN)
            )
            .isEqualTo(WidthTier.W4)
    }

    @Test
    fun widthTier_fromDp_lockScreenBreakpoints() {
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(80, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
            )
            .isEqualTo(WidthTier.W1)
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(150, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
            )
            .isEqualTo(WidthTier.W2)
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(250, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
            )
            .isEqualTo(WidthTier.W3)
        assertThat(
                AppWidgetSizeTiers.resolveWidthTier(350, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
            )
            .isEqualTo(WidthTier.W4)
    }

    @Test
    fun heightTier_fromDp_homeScreenBreakpoints() {
        val surface = AppWidgetGlanceSurface.MOBILE_HOME_SCREEN
        assertThat(AppWidgetSizeTiers.resolveHeightTier(40, surface)).isEqualTo(HeightTier.H0)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(59, surface)).isEqualTo(HeightTier.H0)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(60, surface)).isEqualTo(HeightTier.H1)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(119, surface)).isEqualTo(HeightTier.H1)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(120, surface)).isEqualTo(HeightTier.H2)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(199, surface)).isEqualTo(HeightTier.H2)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(200, surface)).isEqualTo(HeightTier.H3)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(289, surface)).isEqualTo(HeightTier.H3)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(290, surface)).isEqualTo(HeightTier.H4)
        assertThat(AppWidgetSizeTiers.resolveHeightTier(500, surface)).isEqualTo(HeightTier.H4)
    }

    @Test
    fun heightTier_fromDp_lockScreenBreakpoints() {
        assertThat(
                AppWidgetSizeTiers.resolveHeightTier(50, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
            )
            .isEqualTo(HeightTier.H0)
        assertThat(
                AppWidgetSizeTiers.resolveHeightTier(200, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
            )
            .isEqualTo(HeightTier.H0)
    }

    @Test
    fun sizeTiers_from_matchesExpectedTiersAndDestructuring() {
        val (w, h) = AppWidgetSizeTiers.from(250, 150, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(w).isEqualTo(WidthTier.W3)
        assertThat(h).isEqualTo(HeightTier.H2)

        val sizeTiers =
            AppWidgetSizeTiers.from(Dimensions(250, 150), AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(sizeTiers.width).isEqualTo(WidthTier.W3)
        assertThat(sizeTiers.height).isEqualTo(HeightTier.H2)

        val sizeTiersExt =
            SizeTiers.from(Dimensions(250, 150), AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(sizeTiersExt).isEqualTo(sizeTiers)
    }
}
