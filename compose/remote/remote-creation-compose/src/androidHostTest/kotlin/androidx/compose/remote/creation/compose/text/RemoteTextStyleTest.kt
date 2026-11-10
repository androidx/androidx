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

import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.compose.state.RemoteTextUnitType
import androidx.compose.remote.creation.compose.state.asConstantColorOrNull
import androidx.compose.remote.creation.compose.state.asConstantTextUnitOrNull
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteTextStyleTest {

    /** A variable font whose axes match its declared [weight] and [style]. */
    private fun flexFont(weight: FontWeight, style: FontStyle = FontStyle.Normal) =
        Font(
            DeviceFontFamilyName("roboto-flex"),
            weight = weight,
            style = style,
            variationSettings = FontVariation.Settings(weight, style),
        )

    private fun FontVariation.Settings?.toAxisMap(): Map<String, Float> =
        this?.settings?.associate { it.axisName to it.toVariationValue(null) } ?: emptyMap()

    private fun RemoteFontVariation.Settings?.toAxisMap(): Map<String, Float> =
        this?.settings?.associate { it.axisName to it.value.constantValue } ?: emptyMap()

    @Test
    fun fromTextStyle_createsCorrectly() {
        // Unspecified values map to null
        val defaultRemoteStyle = RemoteTextStyle.fromTextStyle(TextStyle.Default)

        assertThat(defaultRemoteStyle.fontSize).isNull()
        assertThat(defaultRemoteStyle.color).isNull()
        assertThat(defaultRemoteStyle.letterSpacing).isNull()
        assertThat(defaultRemoteStyle.lineHeight).isNull()
        assertThat(defaultRemoteStyle.background).isNull()
        assertThat(defaultRemoteStyle.fontWeight).isNull()
        assertThat(defaultRemoteStyle.fontStyle).isNull()
        assertThat(defaultRemoteStyle.fontFamily).isNull()
        assertThat(defaultRemoteStyle.textAlign).isEqualTo(RemoteTextAlign.Unspecified)
        assertThat(defaultRemoteStyle.textDecoration).isNull()
        assertThat(defaultRemoteStyle.lineBreak).isEqualTo(RemoteLineBreak.Unspecified)
        assertThat(defaultRemoteStyle.hyphens).isEqualTo(RemoteHyphens.Unspecified)
        assertThat(defaultRemoteStyle.fontFeatureSettings).isNull()
        assertThat(defaultRemoteStyle.fontVariationSettings).isNull()

        // Explicit values
        val remoteStyle =
            RemoteTextStyle.fromTextStyle(
                TextStyle(
                    fontSize = 16.sp,
                    color = Color.Red,
                    letterSpacing = 1.2f.sp,
                    lineHeight = 20.sp,
                    background = Color.Blue,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    lineBreak = LineBreak.Heading,
                    hyphens = Hyphens.Auto,
                    fontFeatureSettings = "tnum",
                )
            )

        assertThat(remoteStyle.fontSize?.type).isEqualTo(RemoteTextUnitType.Sp)
        assertThat(remoteStyle.fontSize?.asConstantTextUnitOrNull()).isEqualTo(16.sp)
        assertThat(remoteStyle.color?.asConstantColorOrNull()).isEqualTo(Color.Red)
        assertThat(remoteStyle.letterSpacing?.asConstantTextUnitOrNull()).isEqualTo(1.2f.sp)
        assertThat(remoteStyle.lineHeight?.asConstantTextUnitOrNull()).isEqualTo(20.sp)
        assertThat(remoteStyle.background?.asConstantColorOrNull()).isEqualTo(Color.Blue)
        assertThat(remoteStyle.fontWeight).isEqualTo(RemoteFontWeight.Bold)
        assertThat(remoteStyle.fontStyle).isEqualTo(RemoteFontStyle.Italic)
        assertThat(remoteStyle.fontFamily).isEqualTo(RemoteFontFamily.Serif)
        assertThat(remoteStyle.textAlign).isEqualTo(RemoteTextAlign.Center)
        assertThat(remoteStyle.textDecoration).isEqualTo(RemoteTextDecoration.Underline)
        assertThat(remoteStyle.lineBreak).isEqualTo(RemoteLineBreak.Heading)
        assertThat(remoteStyle.hyphens).isEqualTo(RemoteHyphens.Auto)
        assertThat(remoteStyle.fontFeatureSettings).isEqualTo("tnum")
        assertThat(remoteStyle.fontVariationSettings).isNull()
    }

    @Test
    fun fromTextStyle_withFontFeatureSettingsAndFontVariationSettings_doesNotCombine() {
        val textStyle =
            TextStyle(
                fontFeatureSettings = "smcp 1, tnum 1",
                fontFamily =
                    FontFamily(
                        Font(
                            DeviceFontFamilyName("roboto-flex"),
                            variationSettings = FontVariation.Settings(FontVariation.width(110f)),
                        )
                    ),
            )
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontFeatureSettings).isEqualTo("smcp 1, tnum 1")
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(1)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("wdth")
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].value.constantValue)
            .isEqualTo(110f)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun fromTextStyle_withResourceFontAndFontFeatureSettings_extractsFontVariationSettings() {
        val style =
            TextStyle(
                fontFamily =
                    FontFamily(
                        Font(
                            resId = 1,
                            variationSettings =
                                FontVariation.Settings(FontVariation.Setting("ROND", 100f)),
                        )
                    ),
                fontFeatureSettings = "pnum",
            )
        val remoteStyle = RemoteTextStyle.fromTextStyle(style)

        assertThat(remoteStyle.fontFeatureSettings).isEqualTo("pnum")
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(1)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("ROND")
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].value.constantValue)
            .isEqualTo(100f)
    }

    @Test
    fun fromTextStyle_variableFontWithFontFeatureSettings_keepsFontAxes() {
        // Repro from b/568283088: a variable font with axes plus fontFeatureSettings.
        val textStyle =
            TextStyle(
                fontFamily =
                    FontFamily(
                        Font(
                            DeviceFontFamilyName("google-sans-flex"),
                            variationSettings =
                                FontVariation.Settings(FontVariation.Setting("ROND", 100f)),
                        )
                    ),
                fontFeatureSettings = "pnum",
            )

        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontVariationSettings.toAxisMap()).containsExactly("ROND", 100f)
        assertThat(remoteStyle.fontFeatureSettings).isEqualTo("pnum")
    }

    @Test
    fun fromTextStyle_multiWeightFamily_usesVariationSettingsOfMatchingWeight() {
        val fontFamily = FontFamily(flexFont(FontWeight.Normal), flexFont(FontWeight.Bold))

        val bold =
            RemoteTextStyle.fromTextStyle(
                TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            )
        val normal = RemoteTextStyle.fromTextStyle(TextStyle(fontFamily = fontFamily))

        assertThat(bold.fontVariationSettings.toAxisMap()).containsEntry("wght", 700f)
        assertThat(normal.fontVariationSettings.toAxisMap()).containsEntry("wght", 400f)
    }

    @Test
    fun fromTextStyle_multiStyleFamily_usesVariationSettingsOfMatchingStyle() {
        val fontFamily =
            FontFamily(
                flexFont(FontWeight.Normal, FontStyle.Normal),
                flexFont(FontWeight.Normal, FontStyle.Italic),
            )

        val italic =
            RemoteTextStyle.fromTextStyle(
                TextStyle(fontFamily = fontFamily, fontStyle = FontStyle.Italic)
            )

        assertThat(italic.fontVariationSettings.toAxisMap()).containsEntry("ital", 1f)
    }

    @Test
    fun fromTextStyle_noExactWeight_followsCssFontMatching() {
        val fontFamily = FontFamily(flexFont(FontWeight.W300), flexFont(FontWeight.W700))

        fun wghtFor(weight: FontWeight) =
            RemoteTextStyle.fromTextStyle(TextStyle(fontFamily = fontFamily, fontWeight = weight))
                .fontVariationSettings
                .toAxisMap()["wght"]

        // < 400: prefer lighter, then heavier.
        assertThat(wghtFor(FontWeight.W200)).isEqualTo(300f)
        assertThat(wghtFor(FontWeight(350))).isEqualTo(300f)
        // 400..500: heavier up to 500, then lighter, then heavier than 500.
        assertThat(wghtFor(FontWeight(450))).isEqualTo(300f)
        // > 500: prefer heavier, then lighter.
        assertThat(wghtFor(FontWeight.W600)).isEqualTo(700f)
        assertThat(wghtFor(FontWeight.W900)).isEqualTo(700f)
    }

    @Test
    fun fromTextStyle_withFontListFontFamily_extractsFirstFontVariationSettings() {
        val fontWithoutSettings = Font(DeviceFontFamilyName("google-sans"))
        val fontWithSettings =
            Font(
                DeviceFontFamilyName("roboto-flex"),
                weight = FontWeight(450),
                variationSettings =
                    FontVariation.Settings(FontVariation.width(110f), FontVariation.weight(450)),
            )
        val fontFamily = FontFamily(fontWithoutSettings, fontWithSettings)

        val textStyle = TextStyle(fontFamily = fontFamily)
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontFamily).isNull()
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(2)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("wdth")
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].value.constantValue)
            .isEqualTo(110f)
        assertThat(remoteStyle.fontVariationSettings!!.settings[1].axisName).isEqualTo("wght")
        assertThat(remoteStyle.fontVariationSettings!!.settings[1].value.constantValue)
            .isEqualTo(450f)
    }

    @Test
    fun fromTextStyle_withFontListFontFamily_noVariationSettings_returnsNull() {
        val fontFamily = FontFamily(Font(DeviceFontFamilyName("google-sans")))
        val textStyle = TextStyle(fontFamily = fontFamily)
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontVariationSettings).isNull()
    }

    @Test
    fun fromTextStyle_withNonFontListFontFamily_returnsNullVariationSettings() {
        val textStyle = TextStyle(fontFamily = FontFamily.SansSerif)
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontVariationSettings).isNull()
    }

    @Test
    fun fromTextStyle_withNullFontFamily_returnsNullVariationSettings() {
        val textStyle = TextStyle(fontFamily = null)
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontVariationSettings).isNull()
    }

    @Test
    fun copy_overrides_properties() {
        val style =
            RemoteTextStyle(
                color = Color.Red.rc,
                fontSize = 12.rsp,
                fontWeight = RemoteFontWeight.Bold,
                lineHeight = 10.rsp,
                textDecoration = RemoteTextDecoration.Underline,
                lineBreak = RemoteLineBreak.Heading,
                hyphens = RemoteHyphens.None,
            )

        val newStyle =
            style.copy(
                color = Color.Blue.rc,
                fontSize = 24.rsp,
                lineHeight = 20.rsp,
                textDecoration = RemoteTextDecoration.LineThrough,
                lineBreak = RemoteLineBreak.Paragraph,
                hyphens = RemoteHyphens.Auto,
            )

        assertThat(newStyle.color?.constantValueOrNull).isEqualTo(Color.Blue.rc.constantValueOrNull)
        assertThat(newStyle.fontSize?.constantValueOrNull).isEqualTo(24.rsp.constantValueOrNull)
        assertThat(newStyle.fontWeight).isEqualTo(RemoteFontWeight.Bold)
        assertThat(newStyle.lineHeight?.constantValueOrNull).isEqualTo(20.rsp.constantValueOrNull)
        assertThat(newStyle.textDecoration).isEqualTo(RemoteTextDecoration.LineThrough)
        assertThat(newStyle.lineBreak).isEqualTo(RemoteLineBreak.Paragraph)
        assertThat(newStyle.hyphens).isEqualTo(RemoteHyphens.Auto)
    }

    @Test
    fun noarg_copy_does_not_overrides_properties() {
        val style =
            RemoteTextStyle(
                color = Color.Red.rc,
                fontSize = 12.rsp,
                lineBreak = RemoteLineBreak.Heading,
                hyphens = RemoteHyphens.Auto,
            )

        val newStyle = style.copy()

        assertThat(newStyle.color).isEqualTo(style.color)
        assertThat(newStyle.fontSize).isEqualTo(style.fontSize)
        assertThat(newStyle.lineBreak).isEqualTo(style.lineBreak)
        assertThat(newStyle.hyphens).isEqualTo(style.hyphens)
    }

    @Test
    fun merge_overrides_properties() {
        val style =
            RemoteTextStyle(
                color = Color.Red.rc,
                fontSize = 12.rsp,
                fontWeight = RemoteFontWeight.Normal,
                lineBreak = RemoteLineBreak.Heading,
                hyphens = RemoteHyphens.None,
            )

        val newStyle =
            style.merge(
                color = Color.Blue.rc,
                fontWeight = RemoteFontWeight.Bold,
                lineBreak = RemoteLineBreak.Paragraph,
                hyphens = RemoteHyphens.Auto,
            )

        assertThat(newStyle.color?.constantValueOrNull).isEqualTo(Color.Blue.rc.constantValueOrNull)
        assertThat(newStyle.fontSize?.constantValueOrNull).isEqualTo(12.rsp.constantValueOrNull)
        assertThat(newStyle.fontWeight).isEqualTo(RemoteFontWeight.Bold)
        assertThat(newStyle.lineBreak).isEqualTo(RemoteLineBreak.Paragraph)
        assertThat(newStyle.hyphens).isEqualTo(RemoteHyphens.Auto)
    }

    @Test
    fun copy_clears_nullable_properties_and_handles_fontVariationSettings() {
        val initialSettings = RemoteFontVariation.Settings(RemoteFontVariation.weight(400.rf))
        val overrideSettings = RemoteFontVariation.Settings(RemoteFontVariation.weight(700.rf))
        val style =
            RemoteTextStyle(
                color = Color.Red.rc,
                fontSize = 12.rsp,
                fontWeight = RemoteFontWeight.Bold,
                fontStyle = RemoteFontStyle.Italic,
                fontFamily = RemoteFontFamily.Serif,
                letterSpacing = 1.rsp,
                background = Color.Blue.rc,
                textAlign = RemoteTextAlign.Center,
                lineHeight = 16.rsp,
                textDecoration = RemoteTextDecoration.Underline,
                lineBreak = RemoteLineBreak.Heading,
                hyphens = RemoteHyphens.Auto,
                fontFeatureSettings = "tnum",
                fontVariationSettings = initialSettings,
            )

        // Preserves fontVariationSettings when not specified, and clears nullable properties when
        // explicitly passed null.
        val cleared =
            style.copy(
                fontWeight = null,
                fontStyle = null,
                textAlign = RemoteTextAlign.Unspecified,
                textDecoration = null,
                lineBreak = RemoteLineBreak.Unspecified,
                hyphens = RemoteHyphens.Unspecified,
            )
        assertThat(cleared.fontWeight).isNull()
        assertThat(cleared.fontStyle).isNull()
        assertThat(cleared.textAlign).isEqualTo(RemoteTextAlign.Unspecified)
        assertThat(cleared.textDecoration).isNull()
        assertThat(cleared.lineBreak).isEqualTo(RemoteLineBreak.Unspecified)
        assertThat(cleared.hyphens).isEqualTo(RemoteHyphens.Unspecified)
        assertThat(cleared.fontVariationSettings).isEqualTo(initialSettings)

        // Overrides fontVariationSettings while clearing
        // nullable properties.
        val overridden =
            style.copy(
                fontWeight = null,
                fontStyle = null,
                textAlign = RemoteTextAlign.Unspecified,
                textDecoration = null,
                lineBreak = RemoteLineBreak.Unspecified,
                hyphens = RemoteHyphens.Unspecified,
                fontVariationSettings = overrideSettings,
            )
        assertThat(overridden.fontWeight).isNull()
        assertThat(overridden.fontStyle).isNull()
        assertThat(overridden.textAlign).isEqualTo(RemoteTextAlign.Unspecified)
        assertThat(overridden.textDecoration).isNull()
        assertThat(overridden.lineBreak).isEqualTo(RemoteLineBreak.Unspecified)
        assertThat(overridden.hyphens).isEqualTo(RemoteHyphens.Unspecified)
        assertThat(overridden.fontVariationSettings).isEqualTo(overrideSettings)

        // Clears fontVariationSettings when explicitly passed null.
        val clearedVariation = style.copy(fontVariationSettings = null)
        assertThat(clearedVariation.fontVariationSettings).isNull()
        assertThat(clearedVariation.fontWeight).isEqualTo(RemoteFontWeight.Bold)
    }

    @Test
    fun merge_preserves_and_overrides_fontVariationSettings() {
        val initialSettings = RemoteFontVariation.Settings(RemoteFontVariation.weight(400.rf))
        val overrideSettings = RemoteFontVariation.Settings(RemoteFontVariation.weight(700.rf))
        val style =
            RemoteTextStyle(
                fontWeight = RemoteFontWeight.Normal,
                fontVariationSettings = initialSettings,
            )

        val preserved =
            style.merge(fontWeight = RemoteFontWeight.Bold, fontVariationSettings = null)
        assertThat(preserved.fontWeight).isEqualTo(RemoteFontWeight.Bold)
        assertThat(preserved.fontVariationSettings).isEqualTo(initialSettings)

        val overridden =
            style.merge(
                fontWeight = RemoteFontWeight.Bold,
                fontVariationSettings = overrideSettings,
            )
        assertThat(overridden.fontWeight).isEqualTo(RemoteFontWeight.Bold)
        assertThat(overridden.fontVariationSettings).isEqualTo(overrideSettings)
    }

    @Test
    fun fontFeatureSettings_and_fontVariationSettings_remainIndependent() {
        val style =
            RemoteTextStyle(
                fontFeatureSettings = "tnum",
                fontVariationSettings =
                    RemoteFontVariation.Settings(RemoteFontVariation.weight(700.rf)),
            )

        assertThat(style.fontFeatureSettings).isEqualTo("tnum")
        assertThat(style.fontVariationSettings).isNotNull()
        assertThat(style.fontVariationSettings!!.settings).hasSize(1)
        assertThat(style.fontVariationSettings!!.settings[0].axisName).isEqualTo("wght")
    }

    @Test
    fun combineFontSettings_combinesCorrectly() {
        val combined =
            combineFontSettings(
                fontFeatureSettings = "tnum, zero",
                fontVariationSettings =
                    RemoteFontVariation.Settings(RemoteFontVariation.weight(700.rf)),
            )

        assertThat(combined).isNotNull()
        assertThat(combined!!.settings).hasSize(3)
        assertThat(combined.settings[0].axisName).isEqualTo("wght")
        assertThat(combined.settings[0].value.constantValue).isEqualTo(700f)
        assertThat(combined.settings[1].axisName).isEqualTo("tnum")
        assertThat(combined.settings[1].value.constantValue).isEqualTo(1f)
        assertThat(combined.settings[2].axisName).isEqualTo("zero")
        assertThat(combined.settings[2].value.constantValue).isEqualTo(1f)
    }

    @Test
    fun combineFontSettings_onlyFeatureSettings() {
        val combined =
            combineFontSettings(fontFeatureSettings = "tnum", fontVariationSettings = null)

        assertThat(combined).isNotNull()
        assertThat(combined!!.settings).hasSize(1)
        assertThat(combined.settings[0].axisName).isEqualTo("tnum")
        assertThat(combined.settings[0].value.constantValue).isEqualTo(1f)
    }

    @Test
    fun combineFontSettings_nullOrEmpty_returnsOriginalOrNull() {
        val settings = RemoteFontVariation.Settings(RemoteFontVariation.weight(400.rf))
        assertThat(combineFontSettings(null, settings)).isSameInstanceAs(settings)
        assertThat(combineFontSettings("", settings)).isSameInstanceAs(settings)
        assertThat(combineFontSettings(null, null)).isNull()
        assertThat(combineFontSettings("", null)).isNull()
    }

    @Test
    fun fontVariationSettings_convertsFontVariationSettings() {
        val style =
            RemoteTextStyle(
                fontVariationSettings =
                    FontVariation.Settings(FontVariation.weight(700), FontVariation.width(80f))
                        .toRemoteFontVariationSettings()
            )

        assertThat(style.fontVariationSettings)
            .isEqualTo(
                RemoteFontVariation.Settings(
                    RemoteFontVariation.weight(700.rf),
                    RemoteFontVariation.width(80f.rf),
                )
            )
        assertThat(RemoteTextStyle().fontVariationSettings).isNull()
    }

    @Test
    fun remoteTextStyle_exposesRemoteFontWeightAndFontStyle() {
        val style =
            RemoteTextStyle(
                fontWeight = RemoteFontWeight.SemiBold,
                fontStyle = RemoteFontStyle.Italic,
                textAlign = RemoteTextAlign.Center,
                textDecoration = RemoteTextDecoration.Underline + RemoteTextDecoration.LineThrough,
                lineBreak = RemoteLineBreak.Heading,
                hyphens = RemoteHyphens.Auto,
            )

        assertThat(style.fontWeight).isEqualTo(RemoteFontWeight.SemiBold)
        assertThat(style.fontStyle).isEqualTo(RemoteFontStyle.Italic)
        assertThat(style.textAlign).isEqualTo(RemoteTextAlign.Center)
        assertThat(style.textDecoration)
            .isEqualTo(RemoteTextDecoration.Underline + RemoteTextDecoration.LineThrough)
        assertThat(style.textDecoration!!.toString())
            .isEqualTo("RemoteTextDecoration[Underline, LineThrough]")
        assertThat(RemoteTextDecoration.None.toString()).isEqualTo("RemoteTextDecoration.None")
        assertThat(RemoteTextDecoration.Underline.toString())
            .isEqualTo("RemoteTextDecoration.Underline")
        assertThat(RemoteTextDecoration.LineThrough.toString())
            .isEqualTo("RemoteTextDecoration.LineThrough")
        assertThat(RemoteTextDecoration.Underline in style.textDecoration!!).isTrue()
        assertThat(RemoteTextDecoration.LineThrough in style.textDecoration!!).isTrue()
        assertThat(style.lineBreak).isEqualTo(RemoteLineBreak.Heading)
        assertThat(style.lineBreak.value).isEqualTo(CoreText.BREAK_STRATEGY_BALANCED)
        assertThat(style.hyphens).isEqualTo(RemoteHyphens.Auto)
        assertThat(style.hyphens.value).isEqualTo(CoreText.HYPHENATION_FREQUENCY_FULL)

        val defaultStyle = RemoteTextStyle()
        assertThat(defaultStyle.fontWeight).isNull()
        assertThat(defaultStyle.fontStyle).isNull()
        assertThat(defaultStyle.textAlign).isEqualTo(RemoteTextAlign.Unspecified)
        assertThat(
                RemoteTextStyle.fromTextStyle(TextStyle(textAlign = TextAlign.Unspecified))
                    .textAlign
            )
            .isEqualTo(RemoteTextAlign.Unspecified)
        assertThat(defaultStyle.textDecoration).isNull()
        assertThat(defaultStyle.lineBreak).isEqualTo(RemoteLineBreak.Unspecified)
        assertThat(defaultStyle.hyphens).isEqualTo(RemoteHyphens.Unspecified)

        assertThat(RemoteTextAlign.Start.toString()).isEqualTo("RemoteTextAlign.Start")
        assertThat(RemoteTextOverflow.Ellipsis.value).isEqualTo(3)
        assertThat(RemoteTextDecoration.None.plus(RemoteTextDecoration.Underline))
            .isEqualTo(RemoteTextDecoration.Underline)
    }
}
