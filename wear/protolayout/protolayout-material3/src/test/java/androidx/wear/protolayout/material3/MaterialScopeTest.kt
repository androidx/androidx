/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.material3

import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION_CODES
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.material3.DynamicMaterialTheme.getColorProp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class MaterialScopeTest {
    @Test
    fun testDynamicThemeEnabled_returnsTrue() {
        enableDynamicTheme()

        assertThat(isDynamicThemeEnabled(ApplicationProvider.getApplicationContext())).isTrue()
    }

    @Test
    @Config(minSdk = VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun scopeWithDefaultTheme_defaultOptInDynamicColor_dynamicThemeEnabled_api34() {
        val scopeWithDefaultTheme =
            MaterialScope(ApplicationProvider.getApplicationContext(), DEVICE_PARAMETERS)
        enableDynamicTheme()

        assertThat(scopeWithDefaultTheme.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
        assertThat(scopeWithDefaultTheme.allowDynamicTheme).isTrue()
        assertThat(isDynamicThemeEnabled(scopeWithDefaultTheme.context)).isTrue()

        for (i in 0 until ColorTokens.TOKEN_COUNT) {
            assertThat(scopeWithDefaultTheme.getColorProp(i).argb)
                .isEqualTo(getColorProp(ApplicationProvider.getApplicationContext(), i)!!.argb)
        }

        for (i in 0 until Shape.TOKEN_COUNT) {
            assertThat(scopeWithDefaultTheme.getCorner(i).radius!!.value)
                .isEqualTo(DEFAULT_MATERIAL_THEME.getCornerShape(i).getRadius()!!.getValue())
        }

        for (i in 0 until Typography.TOKEN_COUNT) {
            val fontStyle1: LayoutElementBuilders.FontStyle =
                DEFAULT_MATERIAL_THEME.getFontStyleBuilder(i).build()
            val fontStyle2 = scopeWithDefaultTheme.theme.getFontStyleBuilder(i).build()
            assertThat(fontStyle1.preferredFontFamilies).isEmpty()
            assertThat(fontStyle1.variant!!.toProto())
                .isEqualTo(TypographyFontSelection.getFontVariant(i).toProto())
            assertThat(fontStyle1.size!!.value).isEqualTo(fontStyle2.size!!.value)
        }
    }

    @Test
    fun scopeWithCustomThemeAndOptOutDynamicColor() {
        val customErrorColor = Color.MAGENTA
        val customTertiaryColor = Color.CYAN

        val materialScope =
            MaterialScope(
                context = ApplicationProvider.getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS,
                theme =
                    MaterialTheme(
                        customColorScheme =
                            mapOf(
                                ColorTokens.ERROR to ColorBuilders.argb(customErrorColor),
                                ColorTokens.TERTIARY to ColorBuilders.argb(customTertiaryColor)
                            )
                    ),
                allowDynamicTheme = false
            )

        assertThat(materialScope.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
        assertThat(materialScope.allowDynamicTheme).isFalse()

        for (i in 0 until ColorTokens.TOKEN_COUNT) {
            when (i) {
                ColorTokens.ERROR ->
                    assertThat(materialScope.getColorProp(i).argb).isEqualTo(customErrorColor)
                ColorTokens.TERTIARY ->
                    assertThat(materialScope.getColorProp(i).argb).isEqualTo(customTertiaryColor)
                else ->
                    assertThat(materialScope.getColorProp(i).argb)
                        .isEqualTo(DEFAULT_MATERIAL_THEME.getColor(i).argb)
            }
        }

        assertThat(materialScope.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
    }

    @Test
    fun scopeWithCustomTheme_optInToDynamicColor_dynamicThemingDisabled() {
        val customErrorColor = Color.MAGENTA
        val customTertiaryColor = Color.CYAN

        val materialScope =
            MaterialScope(
                context = ApplicationProvider.getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS,
                theme =
                    MaterialTheme(
                        customColorScheme =
                            mapOf(
                                ColorTokens.ERROR to ColorBuilders.argb(customErrorColor),
                                ColorTokens.TERTIARY to ColorBuilders.argb(customTertiaryColor)
                            )
                    )
            )

        assertThat(isDynamicThemeEnabled(materialScope.context)).isFalse()
        assertThat(materialScope.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
        assertThat(materialScope.allowDynamicTheme).isTrue()

        for (i in 0 until ColorTokens.TOKEN_COUNT) {
            when (i) {
                ColorTokens.ERROR ->
                    assertThat(materialScope.getColorProp(i).argb).isEqualTo(customErrorColor)
                ColorTokens.TERTIARY ->
                    assertThat(materialScope.getColorProp(i).argb).isEqualTo(customTertiaryColor)
                else ->
                    assertThat(materialScope.getColorProp(i).argb)
                        .isEqualTo(DEFAULT_MATERIAL_THEME.getColor(i).argb)
            }
        }

        assertThat(materialScope.deviceConfiguration).isEqualTo(DEVICE_PARAMETERS)
    }

    companion object {
        internal val DEVICE_PARAMETERS =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .build()

        private fun enableDynamicTheme() {
            Settings.Secure.putString(
                ApplicationProvider.getApplicationContext<Context>().contentResolver,
                THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                "Placeholder text that enables theming"
            )
        }
    }
}
