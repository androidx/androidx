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

package androidx.compose.remote.creation.compose.text

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteFontVariation.Setting
import androidx.compose.remote.creation.compose.text.RemoteFontVariation.Settings
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteFontVariationTest {
    @Test
    fun setting_storesAxisNameAndValue() {
        val value = 450f.rf
        val setting = Setting("wght", value)

        assertThat(setting.axisName).isEqualTo("wght")
        assertThat(setting.value).isSameInstanceAs(value)
        assertThat(setting.toString())
            .isEqualTo("RemoteFontVariation.Setting(axisName='wght', value=450.0)")
    }

    @Test
    fun setting_requiresFourCharacterAxisName() {
        val exception = assertFailsWith<IllegalArgumentException> { Setting("wgh", 1f.rf) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Name must be exactly four characters. Actual: 'wgh'")
        assertFailsWith<IllegalArgumentException> { Setting("weight", 1f.rf) }
    }

    @Test
    fun setting_equalsComparesAxisNameAndValue() {
        assertThat(Setting("wght", 400f.rf)).isEqualTo(Setting("wght", 400.rf))
        assertThat(Setting("wght", 400f.rf).hashCode())
            .isEqualTo(Setting("wght", 400.rf).hashCode())
        assertThat(Setting("wght", 400f.rf)).isNotEqualTo(Setting("wght", 500f.rf))
        assertThat(Setting("wght", 400f.rf)).isNotEqualTo(Setting("wdth", 400f.rf))
    }

    @Test
    fun setting_negativeZeroEqualsZeroAndIsKept() {
        val negativeZero = Setting("slnt", (-0f).rf)
        val zero = Setting("slnt", 0f.rf)

        assertThat(negativeZero).isEqualTo(zero)
        assertThat(negativeZero.hashCode()).isEqualTo(zero.hashCode())
        assertThat(negativeZero.value.constantValue.toRawBits()).isEqualTo((-0f).toRawBits())
    }

    @Test
    fun setting_equalsComparesDynamicValuesStructurally() {
        val weight = Setting("wght", RemoteFloat.createNamedRemoteFloat("weight", 400f))
        val sameWeight = Setting("wght", RemoteFloat.createNamedRemoteFloat("weight", 400f))

        assertThat(weight).isEqualTo(sameWeight)
        assertThat(weight.hashCode()).isEqualTo(sameWeight.hashCode())
        assertThat(weight)
            .isNotEqualTo(Setting("wght", RemoteFloat.createNamedRemoteFloat("other", 400f)))
        assertThat(weight).isNotEqualTo(Setting("wght", 400f.rf))
    }

    @Test
    fun settings_keepsOrder() {
        val weight = Setting("wght", 700f.rf)
        val width = Setting("wdth", 80f.rf)

        assertThat(Settings(weight, width).settings).containsExactly(weight, width).inOrder()
        assertThat(Settings(listOf(width, weight)).settings)
            .containsExactly(width, weight)
            .inOrder()
        assertThat(Settings().settings).isEmpty()
    }

    @Test
    fun settings_requiresUniqueAxisNames() {
        assertFailsWith<IllegalArgumentException> {
            Settings(Setting("wght", 400f.rf), Setting("wdth", 80f.rf), Setting("wght", 700f.rf))
        }
    }

    @Test
    fun settings_equalsComparesSettingsInOrder() {
        val settings = Settings(Setting("wght", 700f.rf), Setting("wdth", 80f.rf))
        val sameSettings = Settings(Setting("wght", 700f.rf), Setting("wdth", 80f.rf))

        assertThat(settings).isEqualTo(sameSettings)
        assertThat(settings.hashCode()).isEqualTo(sameSettings.hashCode())
        assertThat(settings)
            .isNotEqualTo(Settings(Setting("wdth", 80f.rf), Setting("wght", 700f.rf)))
        assertThat(settings).isNotEqualTo(Settings(Setting("wght", 700f.rf)))
        assertThat(settings.toString())
            .isEqualTo(
                "RemoteFontVariation.Settings(settings=[" +
                    "RemoteFontVariation.Setting(axisName='wght', value=700.0), " +
                    "RemoteFontVariation.Setting(axisName='wdth', value=80.0)])"
            )
    }

    @Test
    fun toRemoteFontVariationSettings_convertsSettingsInOrder() {
        val settings =
            FontVariation.Settings(
                    FontVariation.weight(300),
                    FontVariation.width(70f),
                    FontVariation.Setting("ROND", 100f),
                )
                .toRemoteFontVariationSettings()

        assertThat(settings)
            .isEqualTo(
                Settings(
                    Setting("wght", 300f.rf),
                    Setting("wdth", 70f.rf),
                    Setting("ROND", 100f.rf),
                )
            )
        assertThat(FontVariation.Settings().toRemoteFontVariationSettings().settings).isEmpty()
    }

    @Test
    fun toRemoteFontVariationSettings_keepsNegativeZero() {
        val setting =
            FontVariation.Settings(FontVariation.Setting("slnt", -0f))
                .toRemoteFontVariationSettings()
                .settings
                .single()

        assertThat(setting.value.constantValue.toRawBits()).isEqualTo((-0f).toRawBits())
    }

    @Test
    fun toRemoteFontVariationSettings_resolvesDensityDependentSettingsWithDensity() {
        val settings = FontVariation.Settings(FontVariation.opticalSizing(16.sp))

        assertThat(settings.toRemoteFontVariationSettings(Density(density = 2f, fontScale = 1.5f)))
            .isEqualTo(Settings(Setting("opsz", 24f.rf)))
    }

    @Test
    fun toRemoteFontVariationSettings_defersDensityDependentSettingsWithoutDensity() {
        val settings =
            FontVariation.Settings(FontVariation.opticalSizing(16.sp))
                .toRemoteFontVariationSettings()
        val sameSettings =
            FontVariation.Settings(FontVariation.opticalSizing(16.sp))
                .toRemoteFontVariationSettings()
        val otherSettings =
            FontVariation.Settings(FontVariation.opticalSizing(12.sp))
                .toRemoteFontVariationSettings()

        val setting = settings.settings.single()
        assertThat(setting.axisName).isEqualTo("opsz")
        assertThat(setting.value.hasConstantValue).isFalse()
        assertThat(setting).isNotEqualTo(Setting("opsz", 16f.rf))
        assertThat(settings).isEqualTo(sameSettings)
        assertThat(settings.hashCode()).isEqualTo(sameSettings.hashCode())
        assertThat(settings).isNotEqualTo(otherSettings)
    }
}
