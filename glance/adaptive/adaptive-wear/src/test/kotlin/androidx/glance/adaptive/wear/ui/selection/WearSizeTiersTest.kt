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

package androidx.glance.adaptive.wear.ui.selection

import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearSizeTiersTest {

    @Test
    fun widthTier_fromDp_tileBreakpoints() {
        val surface = WearGlanceSurface.TILE
        assertThat(WearSizeTiers.resolveWidthTier(100, surface)).isEqualTo(WidthTier.W1)
        assertThat(WearSizeTiers.resolveWidthTier(139, surface)).isEqualTo(WidthTier.W1)
        assertThat(WearSizeTiers.resolveWidthTier(140, surface)).isEqualTo(WidthTier.W2)
        assertThat(WearSizeTiers.resolveWidthTier(160, surface)).isEqualTo(WidthTier.W2)
    }

    @Test
    fun heightTier_fromDp_tileBreakpoints() {
        assertThat(WearSizeTiers.resolveHeightTier(100, WearGlanceSurface.TILE))
            .isEqualTo(HeightTier.H1)
        assertThat(WearSizeTiers.resolveHeightTier(139, WearGlanceSurface.TILE))
            .isEqualTo(HeightTier.H1)
        assertThat(WearSizeTiers.resolveHeightTier(140, WearGlanceSurface.TILE))
            .isEqualTo(HeightTier.H2)
        assertThat(WearSizeTiers.resolveHeightTier(160, WearGlanceSurface.TILE))
            .isEqualTo(HeightTier.H2)
    }

    @Test
    fun heightTier_fromDp_complicationBreakpoints() {
        assertThat(WearSizeTiers.resolveHeightTier(50, WearGlanceSurface.COMPLICATION))
            .isEqualTo(HeightTier.H0)
        assertThat(WearSizeTiers.resolveHeightTier(150, WearGlanceSurface.COMPLICATION))
            .isEqualTo(HeightTier.H0)
    }

    @Test
    fun sizeTiers_from_matchesExpectedTiersAndDestructuring() {
        val (w, h) = WearSizeTiers.from(135, 160, WearGlanceSurface.TILE)
        assertThat(w).isEqualTo(WidthTier.W1)
        assertThat(h).isEqualTo(HeightTier.H2)

        val sizeTiers = WearSizeTiers.from(Dimensions(135, 160), WearGlanceSurface.TILE)
        assertThat(sizeTiers.width).isEqualTo(WidthTier.W1)
        assertThat(sizeTiers.height).isEqualTo(HeightTier.H2)

        val sizeTiersExt = SizeTiers.from(Dimensions(135, 160), WearGlanceSurface.TILE)
        assertThat(sizeTiersExt).isEqualTo(sizeTiers)

        // Wear runs its own, tighter scale, so the same container resolves differently against the
        // baseline: 135dp is W2 on mobile but still W1 on Wear, and 160dp is H3 on mobile but H2
        // on Wear.
        val baselineTiers = SizeTiers.from(Dimensions(135, 160))
        assertThat(baselineTiers.width).isEqualTo(WidthTier.W2)
        assertThat(baselineTiers.height).isEqualTo(HeightTier.H3)
    }
}
