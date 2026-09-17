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

class HostConstraintsTest {

    private val homeScreen = GlanceSurface.of("home_screen")
    private val lockScreen = GlanceSurface.of("lock_screen")

    @Test
    fun equalsHashCode_sameValues_areEqual() {
        val first = HostConstraints(Dimensions(100, 200), homeScreen)
        val second = HostConstraints(Dimensions(100, 200), homeScreen)

        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun equals_differingSurface_areNotEqual() {
        val homeConstraints = HostConstraints(Dimensions(100, 200), homeScreen)
        val lockConstraints = HostConstraints(Dimensions(100, 200), lockScreen)

        assertThat(homeConstraints).isNotEqualTo(lockConstraints)
    }

    @Test
    fun equals_differingDimensions_areNotEqual() {
        val small = HostConstraints(Dimensions(100, 200), homeScreen)
        val large = HostConstraints(Dimensions(300, 200), homeScreen)

        assertThat(small).isNotEqualTo(large)
    }

    @Test
    fun toString_containsDimensionsAndSurface() {
        val constraints = HostConstraints(Dimensions(100, 200), homeScreen)

        assertThat(constraints.toString())
            .contains("dimensions=Dimensions(widthDp=100, heightDp=200)")
        assertThat(constraints.toString()).contains("surface=")
    }

    @Test
    fun isCovariantInSurfaceType() {
        val named: HostConstraints<NamedSurface> =
            HostConstraints(Dimensions(100, 200), NamedSurface("custom"))

        // Compiles only because S is declared `out`.
        val widened: HostConstraints<GlanceSurface> = named

        assertThat(widened.surface.tag).isEqualTo("custom")
    }

    private data class NamedSurface(override val tag: String) : GlanceSurface
}
