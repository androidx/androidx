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

package androidx.xr.projected

import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.ProjectedDisplayController.PresentationModeFlags
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalProjectedApi::class)
class PresentationModeFlagsTest {

    @Test
    fun equals_sameModes_isEqual() {
        val flags = PresentationModeFlags(setOf(PresentationMode.VISUALS_ON))
        val other = PresentationModeFlags(setOf(PresentationMode.VISUALS_ON))

        assertThat(flags).isEqualTo(other)
        assertThat(flags.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_differentModes_isNotEqual() {
        assertThat(PresentationModeFlags(setOf(PresentationMode.VISUALS_ON)))
            .isNotEqualTo(PresentationModeFlags(setOf(PresentationMode.AUDIO_ON)))
    }

    @Test
    fun toString_listsModes() {
        val flags =
            PresentationModeFlags(setOf(PresentationMode.VISUALS_ON, PresentationMode.AUDIO_ON))

        assertThat(flags.toString()).isEqualTo("PresentationModeFlags([VISUALS_ON, AUDIO_ON])")
    }

    @Test
    fun toString_noModes_isEmpty() {
        assertThat(PresentationModeFlags(emptySet()).toString())
            .isEqualTo("PresentationModeFlags([])")
    }
}
