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

import androidx.compose.remote.creation.compose.state.rc
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
import androidx.compose.ui.unit.TextUnitType
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
        assertThat(defaultRemoteStyle.textAlign).isEqualTo(TextAlign.Unspecified)
        assertThat(defaultRemoteStyle.textDecoration).isNull()
        assertThat(defaultRemoteStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
        assertThat(defaultRemoteStyle.hyphens).isEqualTo(Hyphens.Unspecified)
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

        assertThat(remoteStyle.fontSize?.type).isEqualTo(TextUnitType.Sp)
        assertThat(remoteStyle.fontSize?.constantValueOrNull).isEqualTo(16.sp)
        assertThat(remoteStyle.color?.constantValueOrNull).isEqualTo(Color.Red)
        assertThat(remoteStyle.letterSpacing?.constantValueOrNull).isEqualTo(1.2f.sp)
        assertThat(remoteStyle.lineHeight?.constantValueOrNull).isEqualTo(20.sp)
        assertThat(remoteStyle.background?.constantValueOrNull).isEqualTo(Color.Blue)
        assertThat(remoteStyle.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(remoteStyle.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(remoteStyle.fontFamily).isEqualTo(RemoteFontFamily.Serif)
        assertThat(remoteStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(remoteStyle.textDecoration).isEqualTo(TextDecoration.Underline)
        assertThat(remoteStyle.lineBreak).isEqualTo(LineBreak.Heading)
        assertThat(remoteStyle.hyphens).isEqualTo(Hyphens.Auto)
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
                            variationSettings =
                                FontVariation.Settings(
                                    FontVariation.Setting(
                                        "wdth",
                                        110f,
                                    )
                                ),
                        )
                    ),
            )
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontFeatureSettings).isEqualTo("smcp 1, tnum 1")
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(1)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("wdth")
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].toVariationValue(null))
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
        assertThat(remoteStyle.fontVariationSettings.settings[0].axisName).isEqualTo("ROND")
        assertThat(remoteStyle.fontVariationSettings.settings[0].toVariationValue(null))
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
                    FontVariation.Settings(
                        FontVariation.Setting("wdth", 110f),
                        FontVariation.Setting("wght", 450f),
                    ),
            )
        val fontFamily = FontFamily(fontWithoutSettings, fontWithSettings)

        val textStyle = TextStyle(fontFamily = fontFamily)
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontFamily).isNull()
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(2)
        assertThat(remoteStyle.fontVariationSettings.settings[0].axisName).isEqualTo("wdth")
        assertThat(remoteStyle.fontVariationSettings.settings[0].toVariationValue(null))
            .isEqualTo(110f)
        assertThat(remoteStyle.fontVariationSettings.settings[1].axisName).isEqualTo("wght")
        assertThat(remoteStyle.fontVariationSettings.settings[1].toVariationValue(null))
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
                fontWeight = FontWeight.Bold,
                lineHeight = 10.rsp,
                textDecoration = TextDecoration.Underline,
                lineBreak = LineBreak.Heading,
                hyphens = Hyphens.None,
            )

        val newStyle =
            style.copy(
                color = Color.Blue.rc,
                fontSize = 24.rsp,
                lineHeight = 20.rsp,
                textDecoration = TextDecoration.LineThrough,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
            )

        assertThat(newStyle.color?.constantValueOrNull).isEqualTo(Color.Blue.rc.constantValueOrNull)
        assertThat(newStyle.fontSize?.constantValueOrNull).isEqualTo(24.rsp.constantValueOrNull)
        assertThat(newStyle.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(newStyle.lineHeight?.constantValueOrNull).isEqualTo(20.rsp.constantValueOrNull)
        assertThat(newStyle.textDecoration).isEqualTo(TextDecoration.LineThrough)
        assertThat(newStyle.lineBreak).isEqualTo(LineBreak.Paragraph)
        assertThat(newStyle.hyphens).isEqualTo(Hyphens.Auto)
    }

    @Test
    fun noarg_copy_does_not_overrides_properties() {
        val style =
            RemoteTextStyle(
                color = Color.Red.rc,
                fontSize = 12.rsp,
                lineBreak = LineBreak.Heading,
                hyphens = Hyphens.Auto,
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
                fontWeight = FontWeight.Normal,
                lineBreak = LineBreak.Heading,
                hyphens = Hyphens.None,
            )

        val newStyle =
            style.merge(
                color = Color.Blue.rc,
                fontWeight = FontWeight.Bold,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
            )

        assertThat(newStyle.color?.constantValueOrNull).isEqualTo(Color.Blue.rc.constantValueOrNull)
        assertThat(newStyle.fontSize?.constantValueOrNull).isEqualTo(12.rsp.constantValueOrNull)
        assertThat(newStyle.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(newStyle.lineBreak).isEqualTo(LineBreak.Paragraph)
        assertThat(newStyle.hyphens).isEqualTo(Hyphens.Auto)
    }

    @Test
    fun fontFeatureSettings_and_fontVariationSettings_remainIndependent() {
        val style =
            RemoteTextStyle(
                fontFeatureSettings = "tnum",
                fontVariationSettings = FontVariation.Settings(FontVariation.weight(700)),
            )

        assertThat(style.fontFeatureSettings).isEqualTo("tnum")
        assertThat(style.fontVariationSettings).isNotNull()
        assertThat(style.fontVariationSettings!!.settings).hasSize(1)
        assertThat(style.fontVariationSettings.settings[0].axisName).isEqualTo("wght")
    }

    @Test
    fun combineFontSettings_combinesCorrectly() {
        val combined =
            combineFontSettings(
                fontFeatureSettings = "tnum, zero",
                fontVariationSettings = FontVariation.Settings(FontVariation.weight(700)),
            )

        assertThat(combined).isNotNull()
        assertThat(combined!!.settings).hasSize(3)
        assertThat(combined.settings[0].axisName).isEqualTo("wght")
        assertThat(combined.settings[0].toVariationValue(null)).isEqualTo(700f)
        assertThat(combined.settings[1].axisName).isEqualTo("tnum")
        assertThat(combined.settings[1].toVariationValue(null)).isEqualTo(1f)
        assertThat(combined.settings[2].axisName).isEqualTo("zero")
        assertThat(combined.settings[2].toVariationValue(null)).isEqualTo(1f)
    }

    @Test
    fun combineFontSettings_onlyFeatureSettings() {
        val combined =
            combineFontSettings(fontFeatureSettings = "tnum", fontVariationSettings = null)

        assertThat(combined).isNotNull()
        assertThat(combined!!.settings).hasSize(1)
        assertThat(combined.settings[0].axisName).isEqualTo("tnum")
        assertThat(combined.settings[0].toVariationValue(null)).isEqualTo(1f)
    }

    @Test
    fun combineFontSettings_nullOrEmpty_returnsOriginalOrNull() {
        val settings = FontVariation.Settings(FontVariation.weight(400))
        assertThat(combineFontSettings(null, settings)).isSameInstanceAs(settings)
        assertThat(combineFontSettings("", settings)).isSameInstanceAs(settings)
        assertThat(combineFontSettings(null, null)).isNull()
        assertThat(combineFontSettings("", null)).isNull()
    }

    @Test
    fun remoteTextStyle_exposesRemoteFontWeightAndFontStyle() {
        val style =
            RemoteTextStyle(
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
            )

        assertThat(style.remoteFontWeight).isEqualTo(RemoteFontWeight.SemiBold)
        assertThat(style.remoteFontStyle).isEqualTo(RemoteFontStyle.Italic)

        val defaultStyle = RemoteTextStyle()
        assertThat(defaultStyle.remoteFontWeight).isNull()
        assertThat(defaultStyle.remoteFontStyle).isNull()
    }
}
