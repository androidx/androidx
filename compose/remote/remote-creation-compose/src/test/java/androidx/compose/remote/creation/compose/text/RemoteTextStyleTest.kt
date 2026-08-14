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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteTextStyleTest {

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
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(1)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("tnum")
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].toVariationValue(null))
            .isEqualTo(1f)
    }

    @Test
    fun fromTextStyle_withFontFeatureSettings_usesParsedFeatures() {
        val textStyle =
            TextStyle(
                fontFeatureSettings = "smcp 1, tnum 1",
                fontFamily =
                    FontFamily(
                        androidx.compose.ui.text.font.Font(
                            androidx.compose.ui.text.font.DeviceFontFamilyName("roboto-flex"),
                            variationSettings =
                                androidx.compose.ui.text.font.FontVariation.Settings(
                                    androidx.compose.ui.text.font.FontVariation.Setting(
                                        "wdth",
                                        110f,
                                    )
                                ),
                        )
                    ),
            )
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        // fontFeatureSettings takes precedence over Font.variationSettings
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(2)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("smcp")
        assertThat(remoteStyle.fontVariationSettings!!.settings[1].axisName).isEqualTo("tnum")
    }

    @Test
    fun fromTextStyle_withFontListFontFamily_extractsFirstFontVariationSettings() {
        val fontWithoutSettings =
            androidx.compose.ui.text.font.Font(
                androidx.compose.ui.text.font.DeviceFontFamilyName("google-sans")
            )
        val fontWithSettings =
            androidx.compose.ui.text.font.Font(
                androidx.compose.ui.text.font.DeviceFontFamilyName("roboto-flex"),
                weight = FontWeight(450),
                variationSettings =
                    androidx.compose.ui.text.font.FontVariation.Settings(
                        androidx.compose.ui.text.font.FontVariation.Setting("wdth", 110f),
                        androidx.compose.ui.text.font.FontVariation.Setting("wght", 450f),
                    ),
            )
        val fontFamily = FontFamily(fontWithoutSettings, fontWithSettings)

        val textStyle = TextStyle(fontFamily = fontFamily)
        val remoteStyle = RemoteTextStyle.fromTextStyle(textStyle)

        assertThat(remoteStyle.fontFamily).isNull()
        assertThat(remoteStyle.fontVariationSettings).isNotNull()
        assertThat(remoteStyle.fontVariationSettings!!.settings).hasSize(2)
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].axisName).isEqualTo("wdth")
        assertThat(remoteStyle.fontVariationSettings!!.settings[0].toVariationValue(null))
            .isEqualTo(110f)
        assertThat(remoteStyle.fontVariationSettings!!.settings[1].axisName).isEqualTo("wght")
        assertThat(remoteStyle.fontVariationSettings!!.settings[1].toVariationValue(null))
            .isEqualTo(450f)
    }

    @Test
    fun fromTextStyle_withFontListFontFamily_noVariationSettings_returnsNull() {
        val fontFamily =
            FontFamily(
                androidx.compose.ui.text.font.Font(
                    androidx.compose.ui.text.font.DeviceFontFamilyName("google-sans")
                )
            )
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
    fun fontFeatureSettings_combinesCorrectly() {
        val style = RemoteTextStyle(fontFeatureSettings = "tnum, zero")
        val combined = style.combinedFontVariationSettings

        assertThat(combined).isNotNull()
        assertThat(combined!!.settings).hasSize(2)
        assertThat(combined.settings[0].axisName).isEqualTo("tnum")
        assertThat(combined.settings[0].toVariationValue(null)).isEqualTo(1f)
        assertThat(combined.settings[1].axisName).isEqualTo("zero")
        assertThat(combined.settings[1].toVariationValue(null)).isEqualTo(1f)
    }

    @Test
    fun fontFeatureSettings_and_fontVariationSettings_merge() {
        val style =
            RemoteTextStyle(
                fontFeatureSettings = "tnum",
                fontVariationSettings =
                    androidx.compose.ui.text.font.FontVariation.Settings(
                        androidx.compose.ui.text.font.FontVariation.weight(700)
                    ),
            )
        val combined = style.combinedFontVariationSettings

        assertThat(combined).isNotNull()
        assertThat(combined!!.settings).hasSize(2)
        assertThat(combined.settings[0].axisName).isEqualTo("wght")
        assertThat(combined.settings[1].axisName).isEqualTo("tnum")
    }
}
