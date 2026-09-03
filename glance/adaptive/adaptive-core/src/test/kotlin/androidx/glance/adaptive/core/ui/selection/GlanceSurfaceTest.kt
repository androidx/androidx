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

class GlanceSurfaceTest {

    @Test
    fun glanceSurface_of_createsInstanceWithTag() {
        val surface1 = GlanceSurface.of("test_surface")
        val surface2 = GlanceSurface.of("test_surface")
        val surface3 = GlanceSurface.of("different_surface")

        assertThat(surface1.tag).isEqualTo("test_surface")
        assertThat(surface1).isEqualTo(surface2)
        assertThat(surface1.hashCode()).isEqualTo(surface2.hashCode())
        assertThat(surface1).isNotEqualTo(surface3)
    }

    @Test
    fun hostConstraints_equalsHashCodeToString() {
        val surface = GlanceSurface.of("home_screen")
        val diffSurface = GlanceSurface.of("lock_screen")

        val constraints1 = HostConstraints(Dimensions(100, 200), surface)
        val constraints2 = HostConstraints(Dimensions(100, 200), surface)
        val diffConstraints = HostConstraints(Dimensions(100, 200), diffSurface)
        val nullSurfaceConstraints = HostConstraints(Dimensions(100, 200), null)

        assertThat(constraints1).isEqualTo(constraints2)
        assertThat(constraints1.hashCode()).isEqualTo(constraints2.hashCode())
        assertThat(constraints1).isNotEqualTo(diffConstraints)
        assertThat(constraints1).isNotEqualTo(nullSurfaceConstraints)

        assertThat(constraints1.toString())
            .contains("dimensions=Dimensions(widthDp=100, heightDp=200)")
        assertThat(constraints1.toString()).contains("surface=")
    }

    @Test
    fun hostConstraints_defaultSurfaceIsNull() {
        val constraints = HostConstraints(Dimensions(50, 50))
        assertThat(constraints.surface).isNull()
    }
}
