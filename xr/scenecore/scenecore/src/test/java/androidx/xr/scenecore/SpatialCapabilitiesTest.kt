/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpatialCapabilitiesTest {

    @Test
    fun hasCapability_singleCapability_returnsTrue() {
        val caps = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue()
    }

    @Test
    fun hasCapability_singleCapability_returnsFalseForMissing() {
        val caps = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse()
    }

    @Test
    fun hasCapability_multipleCapabilities_returnsTrueForAll() {
        val capabilities =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT
        val caps = SpatialCapabilities(capabilities)
        assertThat(caps.hasCapability(capabilities)).isTrue()
    }

    @Test
    fun hasCapability_multipleCapabilities_returnsTrueForSubset() {
        val capabilities =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT
        val caps = SpatialCapabilities(capabilities)
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue()
    }

    @Test
    fun hasCapability_multipleCapabilities_returnsFalseForSuperset() {
        val capabilities = SpatialCapabilities.SPATIAL_CAPABILITY_UI
        val caps = SpatialCapabilities(capabilities)
        val check =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT
        assertThat(caps.hasCapability(check)).isFalse()
    }

    @Test
    fun hasCapability_noCapabilities_returnsFalse() {
        val caps = SpatialCapabilities(0)
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse()
    }

    @Test
    fun toString_noCapabilities_returnsNone() {
        val caps = SpatialCapabilities(0)
        assertThat(caps.toString()).isEqualTo("SpatialCapabilities(NONE)")
    }

    @Test
    fun toString_singleCapability_returnsCorrectString() {
        val caps = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        assertThat(caps.toString()).isEqualTo("SpatialCapabilities(SPATIAL_CAPABILITY_UI)")
    }

    @Test
    fun toString_multipleCapabilities_returnsCorrectString() {
        val capabilities =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT
        val caps = SpatialCapabilities(capabilities)
        assertThat(caps.toString())
            .isEqualTo(
                "SpatialCapabilities(SPATIAL_CAPABILITY_UI | SPATIAL_CAPABILITY_APP_ENVIRONMENT)"
            )
    }

    @Test
    fun toString_allCapabilities_returnsCorrectString() {
        val allCapabilities =
            SpatialCapabilities.SPATIAL_CAPABILITY_UI or
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO or
                SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY
        val caps = SpatialCapabilities(allCapabilities)
        assertThat(caps.toString())
            .isEqualTo(
                "SpatialCapabilities(SPATIAL_CAPABILITY_UI | SPATIAL_CAPABILITY_3D_CONTENT | " +
                    "SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL | SPATIAL_CAPABILITY_APP_ENVIRONMENT | " +
                    "SPATIAL_CAPABILITY_SPATIAL_AUDIO | SPATIAL_CAPABILITY_EMBED_ACTIVITY)"
            )
    }

    @Test
    fun equals_sameCapabilities_returnsTrue() {
        val caps1 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        val caps2 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        assertThat(caps1).isEqualTo(caps2)
    }

    @Test
    fun equals_differentCapabilities_returnsFalse() {
        val caps1 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        val caps2 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
        assertThat(caps1).isNotEqualTo(caps2)
    }

    @Test
    fun equals_differentType_returnsFalse() {
        val caps = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        val other = "a string"
        @Suppress("SuspiciousEqualsCombination") // Testing the implementation
        assertThat(caps.equals(other)).isFalse()
    }

    @Test
    fun equals_null_returnsFalse() {
        val caps = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        assertThat(caps.equals(null)).isFalse()
    }

    @Test
    fun hashCode_sameCapabilities_areEqual() {
        val caps1 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        val caps2 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        assertThat(caps1.hashCode()).isEqualTo(caps2.hashCode())
    }

    @Test
    fun hashCode_differentCapabilities_areNotEqual() {
        val caps1 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_UI)
        val caps2 = SpatialCapabilities(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
        assertThat(caps1.hashCode()).isNotEqualTo(caps2.hashCode())
    }
}
