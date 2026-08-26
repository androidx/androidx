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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class SizeTiersTest {

    @Test
    fun widthTier_fromDp_homeScreenBreakpoints() {
        assertThat(WidthTier.fromDp(50)).isEqualTo(WidthTier.W1)
        assertThat(WidthTier.fromDp(129)).isEqualTo(WidthTier.W1)
        assertThat(WidthTier.fromDp(130)).isEqualTo(WidthTier.W2)
        assertThat(WidthTier.fromDp(219)).isEqualTo(WidthTier.W2)
        assertThat(WidthTier.fromDp(220)).isEqualTo(WidthTier.W3)
        assertThat(WidthTier.fromDp(309)).isEqualTo(WidthTier.W3)
        assertThat(WidthTier.fromDp(310)).isEqualTo(WidthTier.W4)
        assertThat(WidthTier.fromDp(600)).isEqualTo(WidthTier.W4)
    }

    @Test
    fun widthTier_fromDp_surfaceSpecificBreakpoints() {
        assertThat(WidthTier.fromDp(80, GlanceSurface.MOBILE_LOCK_SCREEN)).isEqualTo(WidthTier.W1)
        assertThat(WidthTier.fromDp(150, GlanceSurface.MOBILE_LOCK_SCREEN)).isEqualTo(WidthTier.W2)
        assertThat(WidthTier.fromDp(250, GlanceSurface.MOBILE_LOCK_SCREEN)).isEqualTo(WidthTier.W3)
        assertThat(WidthTier.fromDp(350, GlanceSurface.MOBILE_LOCK_SCREEN)).isEqualTo(WidthTier.W4)

        assertThat(WidthTier.fromDp(100, GlanceSurface.WEAR_TILE)).isEqualTo(WidthTier.W1)
        assertThat(WidthTier.fromDp(160, GlanceSurface.WEAR_TILE)).isEqualTo(WidthTier.W2)

        assertThat(WidthTier.fromDp(180, GlanceSurface.XR_GLASSES)).isEqualTo(WidthTier.W1)
        assertThat(WidthTier.fromDp(300, GlanceSurface.XR_GLASSES)).isEqualTo(WidthTier.W2)
        assertThat(WidthTier.fromDp(400, GlanceSurface.XR_GLASSES)).isEqualTo(WidthTier.W3)
        assertThat(WidthTier.fromDp(600, GlanceSurface.XR_GLASSES)).isEqualTo(WidthTier.W4)
    }

    @Test
    fun heightTier_fromDp_homeScreenBreakpoints() {
        assertThat(HeightTier.fromDp(40)).isEqualTo(HeightTier.H0)
        assertThat(HeightTier.fromDp(59)).isEqualTo(HeightTier.H0)
        assertThat(HeightTier.fromDp(60)).isEqualTo(HeightTier.H1)
        assertThat(HeightTier.fromDp(119)).isEqualTo(HeightTier.H1)
        assertThat(HeightTier.fromDp(120)).isEqualTo(HeightTier.H2)
        assertThat(HeightTier.fromDp(199)).isEqualTo(HeightTier.H2)
        assertThat(HeightTier.fromDp(200)).isEqualTo(HeightTier.H3)
        assertThat(HeightTier.fromDp(289)).isEqualTo(HeightTier.H3)
        assertThat(HeightTier.fromDp(290)).isEqualTo(HeightTier.H4)
        assertThat(HeightTier.fromDp(500)).isEqualTo(HeightTier.H4)
    }

    @Test
    fun heightTier_fromDp_surfaceSpecificBreakpoints() {
        assertThat(HeightTier.fromDp(50, GlanceSurface.MOBILE_LOCK_SCREEN)).isEqualTo(HeightTier.H0)
        assertThat(HeightTier.fromDp(200, GlanceSurface.MOBILE_LOCK_SCREEN))
            .isEqualTo(HeightTier.H0)

        assertThat(HeightTier.fromDp(100, GlanceSurface.WEAR_TILE)).isEqualTo(HeightTier.H1)
        assertThat(HeightTier.fromDp(150, GlanceSurface.WEAR_TILE)).isEqualTo(HeightTier.H2)

        assertThat(HeightTier.fromDp(100, GlanceSurface.XR_GLASSES)).isEqualTo(HeightTier.H1)
        assertThat(HeightTier.fromDp(250, GlanceSurface.XR_GLASSES)).isEqualTo(HeightTier.H2)
        assertThat(HeightTier.fromDp(400, GlanceSurface.XR_GLASSES)).isEqualTo(HeightTier.H3)
        assertThat(HeightTier.fromDp(600, GlanceSurface.XR_GLASSES)).isEqualTo(HeightTier.H4)
    }

    @Test
    fun sizeTiers_from_matchesExpectedTiersAndDestructuring() {
        val (w, h) = SizeTiers.from(250, 150, GlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(w).isEqualTo(WidthTier.W3)
        assertThat(h).isEqualTo(HeightTier.H2)

        val sizeTiers = SizeTiers.from(Dimensions(250, 150), GlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(sizeTiers.width).isEqualTo(WidthTier.W3)
        assertThat(sizeTiers.height).isEqualTo(HeightTier.H2)

        val sizeTiersFloat = SizeTiers.from(250f, 150f, GlanceSurface.MOBILE_HOME_SCREEN)
        assertThat(sizeTiersFloat).isEqualTo(sizeTiers)
        assertThat(sizeTiersFloat.hashCode()).isEqualTo(sizeTiers.hashCode())
        assertThat(sizeTiersFloat.toString()).isEqualTo("SizeTiers(width=W3, height=H2)")
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
}
