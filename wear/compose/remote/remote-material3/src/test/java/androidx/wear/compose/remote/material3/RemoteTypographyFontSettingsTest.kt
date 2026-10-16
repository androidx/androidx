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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteTypographyFontSettingsTest {
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun fromWearTypography_keepsFontAxesAndFontFeatureSettings() {
        // Wear Material3 styles use variable fonts with axes and also set
        // fontFeatureSettings = "pnum". See b/568283088.
        val typography = Typography()
        val remoteTypography =
            RemoteTypography(typography, defaultFontFamily = RemoteFontFamily.Default)

        val styles: List<Triple<String, TextStyle, RemoteTextStyle>> =
            listOf(
                Triple("displayLarge", typography.displayLarge, remoteTypography.displayLarge),
                Triple("displayMedium", typography.displayMedium, remoteTypography.displayMedium),
                Triple("displaySmall", typography.displaySmall, remoteTypography.displaySmall),
                Triple("titleLarge", typography.titleLarge, remoteTypography.titleLarge),
                Triple("titleMedium", typography.titleMedium, remoteTypography.titleMedium),
                Triple("titleSmall", typography.titleSmall, remoteTypography.titleSmall),
                Triple("labelLarge", typography.labelLarge, remoteTypography.labelLarge),
                Triple("labelMedium", typography.labelMedium, remoteTypography.labelMedium),
                Triple("labelSmall", typography.labelSmall, remoteTypography.labelSmall),
                Triple("bodyLarge", typography.bodyLarge, remoteTypography.bodyLarge),
                Triple("bodyMedium", typography.bodyMedium, remoteTypography.bodyMedium),
                Triple("bodySmall", typography.bodySmall, remoteTypography.bodySmall),
                Triple(
                    "bodyExtraSmall",
                    typography.bodyExtraSmall,
                    remoteTypography.bodyExtraSmall,
                ),
                Triple(
                    "numeralExtraLarge",
                    typography.numeralExtraLarge,
                    remoteTypography.numeralExtraLarge,
                ),
                Triple("numeralLarge", typography.numeralLarge, remoteTypography.numeralLarge),
                Triple("numeralMedium", typography.numeralMedium, remoteTypography.numeralMedium),
                Triple("numeralSmall", typography.numeralSmall, remoteTypography.numeralSmall),
                Triple(
                    "numeralExtraSmall",
                    typography.numeralExtraSmall,
                    remoteTypography.numeralExtraSmall,
                ),
            )

        for ((name, textStyle, remoteStyle) in styles) {
            val font = (textStyle.fontFamily as FontListFontFamily).fonts.single() as AndroidFont
            assertWithMessage("$name has font axes")
                .that(font.variationSettings.settings)
                .isNotEmpty()
            assertWithMessage("$name fontVariationSettings")
                .that(remoteStyle.fontVariationSettings)
                .isEqualTo(font.variationSettings)
            assertWithMessage("$name fontFeatureSettings")
                .that(remoteStyle.fontFeatureSettings)
                .isEqualTo(textStyle.fontFeatureSettings)
        }
        assertThat(typography.labelMedium.fontFeatureSettings).isEqualTo("pnum")
    }

    @Test
    fun robotoFlex_withoutDefaultFontFamily_losesFontFamily() {
        val remoteTypography =
            RemoteTypography(RobotoFlexTypography, defaultFontFamily = RemoteFontFamily.Default)

        // The axes survive, but the device font family can't be converted.
        assertThat(remoteTypography.bodyLarge.fontVariationSettings)
            .isEqualTo(RobotoFlexVariationSettings)
        assertThat(remoteTypography.bodyLarge.fontFamily).isEqualTo(RemoteFontFamily.Default)

        val font = renderBodyLargeAndGetFont(remoteTypography)
        assertThat(font.isDeviceFont("sans-serif")).isTrue()
    }

    @Test
    fun robotoFlex_withDefaultFontFamily_keepsFontFamilyAndAxes() {
        val remoteTypography =
            RemoteTypography(
                RobotoFlexTypography,
                defaultFontFamily = RemoteFontFamily.Named("roboto-flex"),
            )

        assertThat(remoteTypography.bodyLarge.fontVariationSettings)
            .isEqualTo(RobotoFlexVariationSettings)
        assertThat(remoteTypography.bodyLarge.fontFamily)
            .isEqualTo(RemoteFontFamily.Named("roboto-flex"))

        val font = renderBodyLargeAndGetFont(remoteTypography)
        assertThat(font.isDeviceFont("roboto-flex")).isTrue()
        assertThat(font.axes()).containsAtLeast("wdth", 110f, "wght", 500f)
    }

    @Test
    fun googleSansFlex_withoutDefaultFontFamily_losesFontFamily() {
        val remoteTypography =
            RemoteTypography(GoogleSansFlexTypography, defaultFontFamily = RemoteFontFamily.Default)

        assertThat(remoteTypography.bodyLarge.fontVariationSettings)
            .isEqualTo(GoogleSansFlexVariationSettings)
        assertThat(remoteTypography.bodyLarge.fontFamily).isEqualTo(RemoteFontFamily.Default)

        val font = renderBodyLargeAndGetFont(remoteTypography)
        assertThat(font.isDeviceFont("sans-serif")).isTrue()
    }

    @Test
    fun googleSansFlex_withDefaultFontFamily_keepsFontFamilyAndAxes() {
        val remoteTypography =
            RemoteTypography(
                GoogleSansFlexTypography,
                defaultFontFamily = RemoteFontFamily.Named("device:google-sans-flex"),
            )

        assertThat(remoteTypography.bodyLarge.fontVariationSettings)
            .isEqualTo(GoogleSansFlexVariationSettings)
        assertThat(remoteTypography.bodyLarge.fontFamily)
            .isEqualTo(RemoteFontFamily.Named("device:google-sans-flex"))

        val font = renderBodyLargeAndGetFont(remoteTypography)
        assertThat(font.isDeviceFont("google-sans-flex")).isTrue()
        assertThat(font.axes()).containsAtLeast("ROND", 100f, "wght", 500f)
    }

    /**
     * Plays [remoteTypography]'s bodyLarge in the embedded player and returns the font used by its
     * text layout.
     */
    private fun renderBodyLargeAndGetFont(remoteTypography: RemoteTypography): AndroidFont {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking {
            val bytes =
                captureSingleRemoteDocument(context = context) {
                        RemoteMaterialTheme(typography = remoteTypography) {
                            RemoteText(
                                BodyLargeText.rs,
                                style = RemoteMaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    .bytes
            CoreDocument().apply {
                ByteArrayInputStream(bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        }
        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        val node = rule.onNodeWithText(BodyLargeText).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        val fontFamily = results.single().layoutInput.style.fontFamily
        return (fontFamily as FontListFontFamily).fonts.single() as AndroidFont
    }

    /** Whether this is a [DeviceFontFamilyName] font named [name]. */
    private fun AndroidFont.isDeviceFont(name: String): Boolean =
        this == Font(DeviceFontFamilyName(name), weight, style, variationSettings)

    private fun AndroidFont.axes(): Map<String, Float> =
        variationSettings.settings.associate { it.axisName to it.toVariationValue(Density(1f)) }

    private companion object {
        const val BodyLargeText = "bodyLarge 123"

        val RobotoFlexVariationSettings =
            FontVariation.Settings(FontVariation.width(110f), FontVariation.weight(500))

        val GoogleSansFlexVariationSettings =
            FontVariation.Settings(FontVariation.Setting("ROND", 100f), FontVariation.weight(500))

        val RobotoFlexTypography =
            typographyWithBodyLarge("roboto-flex", RobotoFlexVariationSettings)

        val GoogleSansFlexTypography =
            typographyWithBodyLarge("google-sans-flex", GoogleSansFlexVariationSettings)

        /** A Wear [Typography] whose bodyLarge uses the variable device font [familyName]. */
        fun typographyWithBodyLarge(
            familyName: String,
            variationSettings: FontVariation.Settings,
        ): Typography =
            Typography(
                bodyLarge =
                    TextStyle(
                        fontFamily =
                            FontFamily(
                                Font(
                                    DeviceFontFamilyName(familyName),
                                    weight = FontWeight.Medium,
                                    variationSettings = variationSettings,
                                )
                            ),
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                    )
            )
    }
}
