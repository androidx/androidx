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
class PanelClippingConfigTest {

    @Test
    fun constructor_defaultValue_isTrue() {
        val config = PanelClippingConfig()
        assertThat(config.isDepthTestEnabled).isTrue()
    }

    @Test
    fun constructor_withFalse_setsToFalse() {
        val config = PanelClippingConfig(isDepthTestEnabled = false)
        assertThat(config.isDepthTestEnabled).isFalse()
    }

    @Test
    fun constructor_withTrue_setsToTrue() {
        val config = PanelClippingConfig(isDepthTestEnabled = true)
        assertThat(config.isDepthTestEnabled).isTrue()
    }

    @Test
    fun copy_noArgs_createsEqualInstance() {
        val originalConfig = PanelClippingConfig(isDepthTestEnabled = false)
        val copiedConfig = originalConfig.copy()

        assertThat(copiedConfig).isEqualTo(originalConfig)
        assertThat(copiedConfig).isNotSameInstanceAs(originalConfig)
    }

    @Test
    fun copy_withNewValue_updatesValue() {
        val originalConfig = PanelClippingConfig(isDepthTestEnabled = false)
        val copiedConfig = originalConfig.copy(isDepthTestEnabled = true)

        assertThat(copiedConfig.isDepthTestEnabled).isTrue()
        assertThat(copiedConfig).isNotEqualTo(originalConfig)
    }

    @Test
    fun equals_sameValue_returnsTrue() {
        val config1 = PanelClippingConfig(isDepthTestEnabled = true)
        val config2 = PanelClippingConfig(isDepthTestEnabled = true)
        assertThat(config1).isEqualTo(config2)

        val config3 = PanelClippingConfig(isDepthTestEnabled = false)
        val config4 = PanelClippingConfig(isDepthTestEnabled = false)
        assertThat(config3).isEqualTo(config4)
    }

    @Test
    fun equals_differentValue_returnsFalse() {
        val config1 = PanelClippingConfig(isDepthTestEnabled = true)
        val config2 = PanelClippingConfig(isDepthTestEnabled = false)
        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentType_returnsFalse() {
        val config = PanelClippingConfig()
        val otherObject = "not a config"

        assertThat(config.equals(otherObject)).isFalse()
    }

    @Test
    fun equals_null_returnsFalse() {
        val config = PanelClippingConfig()
        assertThat(config.equals(null)).isFalse()
    }

    @Test
    fun hashCode_equalObjects_haveSameHashCode() {
        val config1 = PanelClippingConfig(isDepthTestEnabled = true)
        val config2 = PanelClippingConfig(isDepthTestEnabled = true)
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())

        val config3 = PanelClippingConfig(isDepthTestEnabled = false)
        val config4 = PanelClippingConfig(isDepthTestEnabled = false)
        assertThat(config3.hashCode()).isEqualTo(config4.hashCode())
    }

    @Test
    fun hashCode_unequalObjects_haveDifferentHashCodes() {
        val config1 = PanelClippingConfig(isDepthTestEnabled = true)
        val config2 = PanelClippingConfig(isDepthTestEnabled = false)
        assertThat(config1.hashCode()).isNotEqualTo(config2.hashCode())
    }

    @Test
    fun toString_returnsCorrectString() {
        val configTrue = PanelClippingConfig(isDepthTestEnabled = true)
        assertThat(configTrue.toString()).isEqualTo("PanelClippingConfig(isDepthTestEnabled=true)")

        val configFalse = PanelClippingConfig(isDepthTestEnabled = false)
        assertThat(configFalse.toString())
            .isEqualTo("PanelClippingConfig(isDepthTestEnabled=false)")
    }
}
