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

package androidx.glance.adaptive.core.ui.selection

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SizeTiersTest {

    @Test
    fun widthTier_enumEntries() {
        assertThat(WidthTier.values())
            .asList()
            .containsExactly(WidthTier.W1, WidthTier.W2, WidthTier.W3, WidthTier.W4)
            .inOrder()
    }

    @Test
    fun heightTier_enumEntries() {
        assertThat(HeightTier.values())
            .asList()
            .containsExactly(
                HeightTier.H0,
                HeightTier.H1,
                HeightTier.H2,
                HeightTier.H3,
                HeightTier.H4,
            )
            .inOrder()
    }

    @Test
    fun sizeTiers_propertiesAndDestructuring() {
        val tiers = SizeTiers(WidthTier.W3, HeightTier.H2)

        assertThat(tiers.width).isEqualTo(WidthTier.W3)
        assertThat(tiers.height).isEqualTo(HeightTier.H2)

        val (w, h) = tiers
        assertThat(w).isEqualTo(WidthTier.W3)
        assertThat(h).isEqualTo(HeightTier.H2)
    }

    @Test
    fun sizeTiers_equalsHashCodeToString() {
        val tiers1 = SizeTiers(WidthTier.W2, HeightTier.H1)
        val tiers2 = SizeTiers(WidthTier.W2, HeightTier.H1)
        val diffTiers = SizeTiers(WidthTier.W3, HeightTier.H1)

        assertThat(tiers1).isEqualTo(tiers2)
        assertThat(tiers1.hashCode()).isEqualTo(tiers2.hashCode())
        assertThat(tiers1).isNotEqualTo(diffTiers)
        assertThat(tiers1.toString()).isEqualTo("SizeTiers(width=W2, height=H1)")
    }

    @Test
    fun dimensions_equalsHashCodeToString() {
        val dim1 = Dimensions(100, 200)
        val dim2 = Dimensions(100, 200)
        val dimDiff = Dimensions(100, 300)

        assertThat(dim1).isEqualTo(dim2)
        assertThat(dim1.hashCode()).isEqualTo(dim2.hashCode())
        assertThat(dim1).isNotEqualTo(dimDiff)
        assertThat(dim1.toString()).isEqualTo("Dimensions(widthDp=100, heightDp=200)")
    }

    @Test
    fun widthTier_fromDp_baselineResolution() {
        assertThat(WidthTier.fromDp(100)).isEqualTo(WidthTier.W1)
        assertThat(WidthTier.fromDp(130)).isEqualTo(WidthTier.W2)
        assertThat(WidthTier.fromDp(220)).isEqualTo(WidthTier.W3)
        assertThat(WidthTier.fromDp(310)).isEqualTo(WidthTier.W4)
    }

    @Test
    fun heightTier_fromDp_baselineResolution() {
        assertThat(HeightTier.fromDp(50)).isEqualTo(HeightTier.H0)
        assertThat(HeightTier.fromDp(60)).isEqualTo(HeightTier.H1)
        assertThat(HeightTier.fromDp(120)).isEqualTo(HeightTier.H2)
        assertThat(HeightTier.fromDp(200)).isEqualTo(HeightTier.H3)
        assertThat(HeightTier.fromDp(290)).isEqualTo(HeightTier.H4)
    }

    @Test
    fun sizeTiers_from_baselineDimensions() {
        val tiers = SizeTiers.from(Dimensions(180, 150))
        assertThat(tiers.width).isEqualTo(WidthTier.W2)
        assertThat(tiers.height).isEqualTo(HeightTier.H2)
    }
}
