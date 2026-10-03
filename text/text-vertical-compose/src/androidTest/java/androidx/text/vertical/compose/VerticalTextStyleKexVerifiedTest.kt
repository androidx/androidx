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

package androidx.text.vertical.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in [VerticalTextStyle] derived from Kex symbolic and concolic execution paths.
 *
 * Covers [VerticalTextStyle.Default], [VerticalTextStyle.copy], [VerticalTextStyle.merge],
 * [VerticalTextStyle.plus], [VerticalTextStyle.equals], [VerticalTextStyle.hashCode], and
 * [VerticalTextStyle.toString].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class VerticalTextStyleKexVerifiedTest {

    // =========================================================================
    // Target 1: Constructors and Default
    // =========================================================================

    // Branch: when all nine parameters use their default values, initializes Color.Unspecified,
    // TextUnit.Unspecified, and null for all six nullable properties, matching
    // VerticalTextStyle.Default.
    @Test
    fun kex_init_withDefaultArguments_matchesDefaultCompanionInstance() {
        val style = VerticalTextStyle()

        assertThat(style).isEqualTo(VerticalTextStyle.Default)
        assertThat(style.color).isEqualTo(Color.Unspecified)
        assertThat(style.fontSize).isEqualTo(TextUnit.Unspecified)
        assertThat(style.fontWeight).isNull()
        assertThat(style.fontStyle).isNull()
        assertThat(style.fontSynthesis).isNull()
        assertThat(style.fontFamily).isNull()
        assertThat(style.fontFeatureSettings).isNull()
        assertThat(style.background).isEqualTo(Color.Unspecified)
        assertThat(style.localeList).isNull()
    }

    // Branch: primary constructor stores all nine explicitly provided properties.
    @Test
    fun kex_init_withAllArgumentsSpecified_storesAllProperties() {
        val locales = LocaleList("ja-JP")
        val style =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = locales,
            )

        assertThat(style.color).isEqualTo(Color.Red)
        assertThat(style.fontSize).isEqualTo(18.sp)
        assertThat(style.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(style.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(style.fontSynthesis).isEqualTo(FontSynthesis.Weight)
        assertThat(style.fontFamily).isEqualTo(FontFamily.Serif)
        assertThat(style.fontFeatureSettings).isEqualTo("smcp")
        assertThat(style.background).isEqualTo(Color.Yellow)
        assertThat(style.localeList).isEqualTo(locales)
    }

    // =========================================================================
    // Target 2: copy
    // =========================================================================

    // Branch: copy when all nine parameters use their default values preserves all nine properties
    // from the receiver.
    @Test
    fun kex_copy_withDefaultArguments_preservesAllReceiverProperties() {
        val original =
            VerticalTextStyle(
                color = Color.Blue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Style,
                fontFamily = FontFamily.Monospace,
                fontFeatureSettings = "liga",
                background = Color.Cyan,
                localeList = LocaleList("zh-TW"),
            )

        val copied = original.copy()

        assertThat(copied).isEqualTo(original)
        assertNotSame(original, copied)
    }

    // Branch: copy with explicit arguments replaces all nine properties on the returned instance.
    @Test
    fun kex_copy_withAllArgumentsOverridden_replacesAllProperties() {
        val original =
            VerticalTextStyle(
                color = Color.Blue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.None,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "liga",
                background = Color.Cyan,
                localeList = LocaleList("en-US"),
            )
        val newLocales = LocaleList("ja-JP")

        val copied =
            original.copy(
                color = Color.Green,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Cursive,
                fontFeatureSettings = "palt",
                background = Color.Magenta,
                localeList = newLocales,
            )

        assertThat(copied.color).isEqualTo(Color.Green)
        assertThat(copied.fontSize).isEqualTo(28.sp)
        assertThat(copied.fontWeight).isEqualTo(FontWeight.Black)
        assertThat(copied.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(copied.fontSynthesis).isEqualTo(FontSynthesis.All)
        assertThat(copied.fontFamily).isEqualTo(FontFamily.Cursive)
        assertThat(copied.fontFeatureSettings).isEqualTo("palt")
        assertThat(copied.background).isEqualTo(Color.Magenta)
        assertThat(copied.localeList).isEqualTo(newLocales)
    }

    // =========================================================================
    // Target 3: merge and plus
    // =========================================================================

    // Branch: other == null -> returns this without allocating a new VerticalTextStyle.
    @Test
    fun kex_merge_whenOtherIsNull_returnsSameInstance() {
        val base = VerticalTextStyle(color = Color.Red, fontSize = 16.sp)

        val merged = base.merge(null)

        assertSame(base, merged)
    }

    // Branch: other != null && other == Default -> returns this without allocating a new instance.
    @Test
    fun kex_merge_whenOtherEqualsDefault_returnsSameInstance() {
        val base = VerticalTextStyle(color = Color.Red, fontSize = 16.sp)

        assertSame(base, base.merge(VerticalTextStyle.Default))
        assertSame(base, base.merge(VerticalTextStyle()))
    }

    // Branch: other has all nine properties specified (isSpecified == true and non-null).
    // Replaces every property from this style with the property from other.
    @Test
    fun kex_merge_whenOtherHasAllPropertiesSpecified_usesAllOtherProperties() {
        val base =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.None,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "kern",
                background = Color.White,
                localeList = LocaleList("en-US"),
            )
        val otherLocales = LocaleList("ja-JP")
        val other =
            VerticalTextStyle(
                color = Color.Blue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "vpal",
                background = Color.Yellow,
                localeList = otherLocales,
            )

        val merged = base.merge(other)

        assertThat(merged).isEqualTo(other)
    }

    // Branch: other != Default (color is specified), while all other eight properties on other are
    // Unspecified or null.
    // Falls back to this style for the eight unspecified properties.
    @Test
    fun kex_merge_whenOtherSpecifiesOnlyColor_retainsRemainingReceiverProperties() {
        val baseLocales = LocaleList("ja-JP")
        val base =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "vpal",
                background = Color.Yellow,
                localeList = baseLocales,
            )
        val other = VerticalTextStyle(color = Color.Green)

        val merged = base.merge(other)

        assertThat(merged.color).isEqualTo(Color.Green)
        assertThat(merged.fontSize).isEqualTo(18.sp)
        assertThat(merged.fontWeight).isEqualTo(FontWeight.SemiBold)
        assertThat(merged.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(merged.fontSynthesis).isEqualTo(FontSynthesis.Weight)
        assertThat(merged.fontFamily).isEqualTo(FontFamily.Serif)
        assertThat(merged.fontFeatureSettings).isEqualTo("vpal")
        assertThat(merged.background).isEqualTo(Color.Yellow)
        assertThat(merged.localeList).isEqualTo(baseLocales)
    }

    // Branch: other.color.isSpecified == false (Color.Unspecified) while other.fontSize.isSpecified
    // == true.
    // Retains this.color and uses other.fontSize.
    @Test
    fun kex_merge_whenOtherColorIsUnspecified_retainsReceiverColor() {
        val base = VerticalTextStyle(color = Color.Red, fontSize = 14.sp)
        val other = VerticalTextStyle(color = Color.Unspecified, fontSize = 22.sp)

        val merged = base.merge(other)

        assertThat(merged.color).isEqualTo(Color.Red)
        assertThat(merged.fontSize).isEqualTo(22.sp)
    }

    // Branch: plus delegates to merge(other) for both Default and specified styles.
    @Test
    fun kex_plus_delegatesToMerge() {
        val base = VerticalTextStyle(color = Color.Red, fontWeight = FontWeight.Bold)
        val other = VerticalTextStyle(fontSize = 20.sp, background = Color.Yellow)

        assertSame(base, base + VerticalTextStyle.Default)
        val combined = base + other
        assertThat(combined.color).isEqualTo(Color.Red)
        assertThat(combined.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(combined.fontSize).isEqualTo(20.sp)
        assertThat(combined.background).isEqualTo(Color.Yellow)
    }

    // =========================================================================
    // Target 4: equals (all 12 branches)
    // =========================================================================

    // Branch 1: this === other -> returns true.
    @Test
    fun kex_equals_whenSameReference_returnsTrue() {
        val style = VerticalTextStyle(color = Color.Red, fontSize = 16.sp)

        assertThat(style.equals(style)).isTrue()
    }

    // Branch 2: other !is VerticalTextStyle (null or unrelated type) -> returns false.
    @Test
    fun kex_equals_whenOtherIsNotVerticalTextStyle_returnsFalse() {
        val style = VerticalTextStyle()

        assertThat(style.equals(null)).isFalse()
        assertThat(style.equals(Any())).isFalse()
    }

    // Branch 3: color != other.color -> returns false.
    @Test
    fun kex_equals_whenColorDiffers_returnsFalse() {
        val style1 = VerticalTextStyle(color = Color.Red)
        val style2 = VerticalTextStyle(color = Color.Blue)

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 4: fontSize != other.fontSize -> returns false.
    @Test
    fun kex_equals_whenFontSizeDiffers_returnsFalse() {
        val style1 = VerticalTextStyle(color = Color.Red, fontSize = 14.sp)
        val style2 = VerticalTextStyle(color = Color.Red, fontSize = 16.sp)

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 5: fontWeight != other.fontWeight -> returns false.
    @Test
    fun kex_equals_whenFontWeightDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val style2 = VerticalTextStyle(color = Color.Red, fontSize = 14.sp, fontWeight = null)

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 6: fontStyle != other.fontStyle -> returns false.
    @Test
    fun kex_equals_whenFontStyleDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            )

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 7: fontSynthesis != other.fontSynthesis -> returns false.
    @Test
    fun kex_equals_whenFontSynthesisDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.None,
            )

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 8: fontFamily != other.fontFamily -> returns false.
    @Test
    fun kex_equals_whenFontFamilyDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Monospace,
            )

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 9: fontFeatureSettings != other.fontFeatureSettings -> returns false.
    @Test
    fun kex_equals_whenFontFeatureSettingsDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = null,
            )

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 10: background != other.background -> returns false.
    @Test
    fun kex_equals_whenBackgroundDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Cyan,
            )

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 11: localeList != other.localeList -> returns false.
    @Test
    fun kex_equals_whenLocaleListDiffers_returnsFalse() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = LocaleList("ja-JP"),
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = LocaleList("ko-KR"),
            )

        assertThat(style1).isNotEqualTo(style2)
    }

    // Branch 12: all nine properties match across two distinct instances -> returns true.
    @Test
    fun kex_equals_whenAllNinePropertiesMatch_returnsTrue() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = LocaleList("ja-JP"),
            )
        val style2 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = LocaleList("ja-JP"),
            )

        assertThat(style1).isEqualTo(style2)
    }

    // =========================================================================
    // Target 5: hashCode and toString
    // =========================================================================

    // Branch: all six nullable properties are null (?: 0 branch taken for each).
    @Test
    fun kex_hashCode_whenAllNullablePropertiesAreNull_isConsistentWithDefault() {
        val style1 = VerticalTextStyle()
        val style2 = VerticalTextStyle()

        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
        assertThat(style1.hashCode()).isEqualTo(VerticalTextStyle.Default.hashCode())
    }

    // Branch: all six nullable properties are non-null (?.hashCode() branch taken for each).
    @Test
    fun kex_hashCode_whenAllPropertiesAreNonNull_matchesEqualInstanceAndDiffersFromDefault() {
        val style1 =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = LocaleList("ja-JP"),
            )
        val style2 = style1.copy()

        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
        assertThat(style1.hashCode()).isNotEqualTo(VerticalTextStyle.Default.hashCode())
    }

    // Formats all nine properties in VerticalTextStyle.toString().
    @Test
    fun kex_toString_formatsAllNineProperties() {
        val locales = LocaleList("ja-JP")
        val style =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = locales,
            )

        val text = style.toString()

        assertThat(text).startsWith("VerticalTextStyle(")
        assertThat(text).contains("color=${Color.Red}")
        assertThat(text).contains("fontSize=${16.sp}")
        assertThat(text).contains("fontWeight=${FontWeight.Bold}")
        assertThat(text).contains("fontStyle=${FontStyle.Italic}")
        assertThat(text).contains("fontSynthesis=${FontSynthesis.All}")
        assertThat(text).contains("fontFamily=${FontFamily.Serif}")
        assertThat(text).contains("fontFeatureSettings=smcp")
        assertThat(text).contains("background=${Color.Yellow}")
        assertThat(text).contains("localeList=$locales")
    }
}
