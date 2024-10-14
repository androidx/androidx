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

import android.annotation.SuppressLint
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.SpProp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.material3.ColorTokens.ColorToken
import androidx.wear.protolayout.material3.Shape.ShapeToken
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.material3.tokens.TextStyle

/**
 * MaterialTheme defines the styling principle from the Wear Material3 design specification which
 * extends the Material design specification.
 *
 * The default MaterialTheme provides:
 * * A baseline color theme and color set
 * * A typography set using a flex font
 * * A corner shape set
 *
 * Some of these attributes values are allowed to be customized with limitation. Any values that are
 * not customized will use default values.
 *
 * While a custom color scheme can be created manually, it is highly recommended to generate one
 * using source colors from your brand. The
 * [Material Theme Builder tool](https://material-foundation.github.io/material-theme-builder/)
 * allows you to do this.
 *
 * ProtoLayout Material3 components use the values provided here to style their looks.
 *
 * @param customColorScheme The map with the customized colors. The keys are predefined in
 *   [ColorToken] as color roles, and the values in the provided map will override the defaults
 *   values correspondingly.
 */
// TODO: b/369350414 - Rethink API for Color/Typography/Shape with value instead of an enum.
internal class MaterialTheme(private val customColorScheme: Map<Int, ColorProp> = emptyMap()) {
    /** Retrieves the [FontStyle.Builder] with the typography name. */
    internal fun getFontStyleBuilder(@TypographyToken typographyToken: Int) =
        createFontStyleBuilder(typographyToken)

    /** Retrieves the line height with the typography name. */
    internal fun getLineHeight(@TypographyToken typographyToken: Int) =
        Typography.fromToken(typographyToken).lineHeight

    /** Retrieves the [ColorProp] with the color name. */
    internal fun getColor(@ColorToken colorToken: Int): ColorProp =
        customColorScheme.getOrDefault(colorToken, ColorTokens.fromToken(colorToken))

    /** Retrieves the [Corner] with the shape name. */
    internal fun getCornerShape(@ShapeToken shapeToken: Int): Corner = Shape.fromToken(shapeToken)
}

/** MaterialTheme that uses predefined default values without any customization. */
@JvmField internal val DEFAULT_MATERIAL_THEME: MaterialTheme = MaterialTheme(emptyMap())

@SuppressLint("ResourceType")
@androidx.annotation.OptIn(ProtoLayoutExperimental::class)
internal fun createFontStyleBuilder(
    @TypographyToken typographyToken: Int,
    deviceConfiguration: DeviceParameters? = null,
    isScalable: Boolean = true
): FontStyle.Builder {
    val textStyle: TextStyle = Typography.fromToken(typographyToken)
    val sizeSp: SpProp = textStyle.size
    return FontStyle.Builder()
        .setSize(
            if (!isScalable && deviceConfiguration != null) {
                sp(sizeSp.value.dpToSp(deviceConfiguration.fontScale))
            } else {
                sizeSp
            }
        )
        .setLetterSpacing(textStyle.letterSpacing)
        .setSettings(*textStyle.fontSettings.toTypedArray())
        .setVariant(TypographyFontSelection.getFontVariant(typographyToken))
        .setWeight(TypographyFontSelection.getFontWeight(typographyToken))
}
