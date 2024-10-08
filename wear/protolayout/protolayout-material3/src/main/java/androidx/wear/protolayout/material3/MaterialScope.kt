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
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.ImageDimension
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment
import androidx.wear.protolayout.LayoutElementBuilders.TextOverflow
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.material3.ColorTokens.ColorToken
import androidx.wear.protolayout.material3.ColorTokens.fromToken
import androidx.wear.protolayout.material3.Shape.ShapeToken
import androidx.wear.protolayout.material3.Typography.TypographyToken

/**
 * Receiver scope which is used by all ProtoLayout Material3 components and layout to support
 * opinionated defaults and to provide the global information for styling Material3 components.
 *
 * The MaterialScope includes:
 * * theme, which is used to retrieve the color, typography or shape values.
 * * [DeviceParameters], which contains screen size, font scale, renderer schema version etc.
 * * Default usage of system theme, with the option to opt out.
 */
// TODO: b/352308384 - Add helper to read the exported Json or XML file from the Material Theme
//    Builder tool.
// TODO: b/350927030 - Customization setters of shape and typography, which are not fully
// TODO: b/352308384 - Add helper to read the exported Json or XML file from the Material Theme
//    Builder tool.
// TODO: b/350927030 - Customization setters of shape and typography, which are not fully
// customizable.
// TODO: b/369116159 - Add samples on usage.
@MaterialScopeMarker
public open class MaterialScope
/**
 * @param context The Android Context for the Tile service
 * @param deviceConfiguration The device parameters for where the components will be rendered
 * @param theme The theme to be used. If not set, default Material theme will be applied
 * @param allowDynamicTheme If dynamic colors theme should be used on components, meaning that
 * @param defaultTextElementStyle The opinionated text style that text component can use as defaults
 */
internal constructor(
    internal val context: Context,
    /** The device parameters for where the components will be rendered. */
    public val deviceConfiguration: DeviceParameters,
    internal val theme: MaterialTheme = DEFAULT_MATERIAL_THEME,
    internal val allowDynamicTheme: Boolean = true,
    internal val defaultTextElementStyle: TextElementStyle = TextElementStyle(),
    internal val defaultIconStyle: IconStyle = IconStyle()
) {
    internal fun withStyle(
        defaultTextElementStyle: TextElementStyle = this.defaultTextElementStyle,
        defaultIconStyle: IconStyle = this.defaultIconStyle
    ): MaterialScope =
        MaterialScope(
            context = context,
            deviceConfiguration = deviceConfiguration,
            theme = theme,
            allowDynamicTheme = allowDynamicTheme,
            defaultTextElementStyle = defaultTextElementStyle,
            defaultIconStyle = defaultIconStyle
        )
}

/**
 * Retrieves the [Corner] shape from the default Material theme with shape token name.
 *
 * @throws IllegalArgumentException if the token name is not recognized as one of the constants in
 *   [Shape]
 */
public fun MaterialScope.getCorner(@ShapeToken shapeToken: Int): Corner =
    theme.getCornerShape(shapeToken)

/**
 * Retrieves the [ColorProp] from the customized or default Material theme or dynamic system theme
 * with the given color token name.
 *
 * @throws IllegalArgumentException if the token name is not recognized as one of the constants in
 *   [ColorTokens]
 */
public fun MaterialScope.getColorProp(@ColorToken colorToken: Int): ColorProp =
    if (isDynamicThemeEnabled(context) && allowDynamicTheme) {
        DynamicMaterialTheme.getColorProp(context, colorToken) ?: theme.getColor(colorToken)
    } else {
        theme.getColor(colorToken)
    }

/**
 * Returns whether the dynamic colors theme (colors following the system theme) is enabled.
 *
 * If enabled, and elements or [MaterialScope] are opted in to using dynamic theme, colors will
 * change whenever system theme changes.
 */
public fun isDynamicThemeEnabled(context: Context): Boolean {
    val overlaySetting: String? =
        Settings.Secure.getString(context.contentResolver, THEME_CUSTOMIZATION_OVERLAY_PACKAGES)
    return (!overlaySetting.isNullOrEmpty() && overlaySetting != "{}")
}

/**
 * Creates a top-level receiver scope [MaterialScope] that calls the given [layout] to support for
 * opinionated defaults and building Material3 components and layout.
 *
 * @param context The Android Context for the Tile service
 * @param deviceConfiguration The device parameters for where the components will be rendered
 * @param allowDynamicTheme If dynamic colors theme should be used on components, meaning that
 *   colors will follow the system theme if enabled on the device. If not set, defaults to using the
 *   system theme
 * @param layout Scoped slot for the content of layout to be displayed
 */
// TODO: b/369350414 - Allow for overriding colors theme.
// TODO: b/370976767 - Specify in docs that MaterialTileService should be used instead of using this
// directly.
public fun materialScope(
    context: Context,
    deviceConfiguration: DeviceParameters,
    allowDynamicTheme: Boolean = true,
    layout: MaterialScope.() -> LayoutElement
): LayoutElement =
    MaterialScope(
            context = context,
            deviceConfiguration = deviceConfiguration,
            allowDynamicTheme = allowDynamicTheme
        )
        .layout()

@DslMarker public annotation class MaterialScopeMarker

/** This maps to `android.provider.Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES`. */
@VisibleForTesting
internal const val THEME_CUSTOMIZATION_OVERLAY_PACKAGES: String =
    "theme_customization_overlay_packages"

internal class TextElementStyle(
    @TypographyToken val typography: Int = Typography.BODY_MEDIUM,
    val color: ColorProp = fromToken(ColorTokens.PRIMARY),
    val italic: Boolean = false,
    val underline: Boolean = false,
    val scalable: Boolean = true,
    val maxLines: Int = 1,
    @TextAlignment val multilineAlignment: Int = TEXT_ALIGN_CENTER,
    @TextOverflow val overflow: Int = TEXT_OVERFLOW_ELLIPSIZE,
)

internal class IconStyle(
    val size: ImageDimension = 24.toDp(),
    val tintColor: ColorProp = fromToken(ColorTokens.PRIMARY),
)
