/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wear Material [Button] that offers a single slot to take any content (text, icon or image).
 *
 * The [Button] is circular in shape. The recommended [Button] sizes can be obtained
 * from [ButtonDefaults] - see [ButtonDefaults.DefaultButtonSize], [ButtonDefaults.LargeButtonSize],
 * [ButtonDefaults.SmallButtonSize].
 * Icon content should be of size [ButtonDefaults.DefaultIconSize],
 * [ButtonDefaults.LargeIconSize] or [ButtonDefaults.SmallIconSize] respectively.
 *
 * The recommended set of [ButtonColors] styles can be obtained from [ButtonDefaults], e.g.
 * [ButtonDefaults.primaryButtonColors] to get a color scheme for a primary [Button] which by
 * default will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * [Button]s can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [Button] displaying an icon:
 * @sample androidx.wear.compose.material.samples.ButtonWithIcon
 *
 * Example of a large [Button] displaying an icon:
 * @sample androidx.wear.compose.material.samples.LargeButtonWithIcon
 *
 * Example of a [Button] with text content and size modified to LargeButtonSize:
 * @sample androidx.wear.compose.material.samples.ButtonWithText
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the [Button] such as text, icon or image.
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.0." +
    "A newer overload is available with an additional shape parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.primaryButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shape = CircleShape,
        border = ButtonDefaults.buttonBorder(),
        content = content
    )

/**
 * Wear Material [Button] that offers a single slot to take any content (text, icon or image).
 *
 * The recommended [Button] sizes can be obtained
 * from [ButtonDefaults] - see [ButtonDefaults.DefaultButtonSize], [ButtonDefaults.LargeButtonSize],
 * [ButtonDefaults.SmallButtonSize].
 * Icon content should be of size [ButtonDefaults.DefaultIconSize],
 * [ButtonDefaults.LargeIconSize] or [ButtonDefaults.SmallIconSize] respectively.
 *
 * The recommended set of [ButtonColors] styles can be obtained from [ButtonDefaults], e.g.
 * [ButtonDefaults.primaryButtonColors] to get a color scheme for a primary [Button] which by
 * default will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * [Button]s can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [Button] displaying an icon:
 * @sample androidx.wear.compose.material.samples.ButtonWithIcon
 *
 * Example of a large [Button] displaying an icon:
 * @sample androidx.wear.compose.material.samples.LargeButtonWithIcon
 *
 * Example of a [Button] with text content and size modified to LargeButtonSize:
 * @sample androidx.wear.compose.material.samples.ButtonWithText
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param border [ButtonBorder] that will be used to resolve the button border in different states.
 * See [ButtonDefaults.buttonBorder].
 * @param content The content displayed on the [Button] such as text, icon or image.
 */
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.primaryButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = CircleShape,
    border: ButtonBorder = ButtonDefaults.buttonBorder(),
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = { colors.backgroundColor(it) },
        interactionSource = interactionSource,
        shape = shape,
        border = { border.borderStroke(enabled = it) },
        buttonSize = ButtonDefaults.DefaultButtonSize,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.button,
            content
        )
    )
}

/**
 * Wear Material [OutlinedButton] that offers a single slot to take any content (text, icon or
 * image).
 *
 * The recommended [Button] sizes can be obtained
 * from [ButtonDefaults] - see [ButtonDefaults.DefaultButtonSize], [ButtonDefaults.LargeButtonSize],
 * [ButtonDefaults.SmallButtonSize].
 * Icon content should be of size [ButtonDefaults.DefaultIconSize],
 * [ButtonDefaults.LargeIconSize] or [ButtonDefaults.SmallIconSize] respectively.
 *
 * [Button]s can be enabled or disabled. A disabled button will not respond to click events.
 *
 * An [OutlinedButton] has a transparent background and a thin border by default with
 * content taking the theme primary color.
 *
 * Example of a [OutlinedButton] displaying an icon:
 * @sample androidx.wear.compose.material.samples.OutlinedButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param border [ButtonBorder] that will be used to resolve the button border in different states.
 * See [ButtonDefaults.outlinedButtonBorder].
 * @param content The content displayed on the [OutlinedButton] such as text, icon or image.
 */
@Composable
public fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = CircleShape,
    border: ButtonBorder = ButtonDefaults.outlinedButtonBorder(),
    content: @Composable BoxScope.() -> Unit,
) = Button(onClick, modifier, enabled, colors, interactionSource, shape, border, content)

/**
 * Wear Material [CompactButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * The [CompactButton] is circular in shape and has background size
 * [ButtonDefaults.ExtraSmallButtonSize]. There is an optional transparent padding around
 * the background, defaulted to [ButtonDefaults.CompactButtonBackgroundPadding],
 * which increases the clickable area. Icon content should have size [ButtonDefaults.SmallIconSize].
 *
 * The recommended set of [ButtonColors] styles can be obtained from [ButtonDefaults], e.g.
 * [ButtonDefaults.primaryButtonColors] to get a color scheme for a primary [Button] which by
 * default will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * [CompactButton]s can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.CompactButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
 * @param backgroundPadding Increases the transparent clickable area around the background,
 * defaults to [ButtonDefaults.CompactButtonBackgroundPadding]
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the [CompactButton] such as text, icon or image.
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.0." +
    "A newer overload is available with an additional shape parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.primaryButtonColors(),
    backgroundPadding: Dp = ButtonDefaults.CompactButtonBackgroundPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) = CompactButton(
    onClick,
    modifier,
    enabled,
    colors,
    backgroundPadding,
    interactionSource,
    CircleShape,
    ButtonDefaults.buttonBorder(),
    content)

/**
 * Wear Material [CompactButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * The [CompactButton] has background size [ButtonDefaults.ExtraSmallButtonSize].
 * There is an optional transparent padding around
 * the background, defaulted to [ButtonDefaults.CompactButtonBackgroundPadding],
 * which increases the clickable area. Icon content should have size [ButtonDefaults.SmallIconSize].
 *
 * The recommended set of [ButtonColors] styles can be obtained from [ButtonDefaults], e.g.
 * [ButtonDefaults.primaryButtonColors] to get a color scheme for a primary [Button] which by
 * default will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * [CompactButton]s can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.CompactButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
 * @param backgroundPadding Increases the transparent clickable area around the background,
 * defaults to [ButtonDefaults.CompactButtonBackgroundPadding]
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param border [ButtonBorder] that will be used to resolve the button border in different states.
 * See [ButtonDefaults.outlinedButtonBorder].
 * @param content The content displayed on the [CompactButton] such as text, icon or image.
 */
@Composable
public fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.primaryButtonColors(),
    backgroundPadding: Dp = ButtonDefaults.CompactButtonBackgroundPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = CircleShape,
    border: ButtonBorder = ButtonDefaults.buttonBorder(),
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Button(
        onClick = onClick,
        modifier = modifier
            .padding(backgroundPadding)
            .requiredSize(ButtonDefaults.ExtraSmallButtonSize),
        enabled = enabled,
        backgroundColor = { colors.backgroundColor(it) },
        interactionSource = interactionSource,
        shape = shape,
        border = { border.borderStroke(it) },
        buttonSize = ButtonDefaults.ExtraSmallButtonSize,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.button,
            content
        )
    )
}

/**
 * Wear Material [OutlinedCompactButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * The [OutlinedCompactButton] has background size [ButtonDefaults.ExtraSmallButtonSize].
 * There is an transparent padding around the background, defaulted to
 * [ButtonDefaults.CompactButtonBackgroundPadding], which increases the clickable area. Icon content
 * should have size [ButtonDefaults.SmallIconSize].
 *
 * An [OutlinedCompactButton] has a transparent background and a thin border by default with
 * content taking the theme primary color.
 *
 * [OutlinedCompactButton]s can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.OutlinedCompactButtonWithIcon
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param backgroundPadding Increases the transparent clickable area around the background,
 * defaults to [ButtonDefaults.CompactButtonBackgroundPadding]
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param border [ButtonBorder] that will be used to resolve the button border in different states.
 * See [ButtonDefaults.outlinedButtonBorder].
 * @param content The content displayed on the [OutlinedCompactButton] such as text, icon or image.
 */
@Composable
public fun OutlinedCompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    backgroundPadding: Dp = ButtonDefaults.CompactButtonBackgroundPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = CircleShape,
    border: ButtonBorder = ButtonDefaults.outlinedButtonBorder(),
    content: @Composable BoxScope.() -> Unit,
) =
    CompactButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        backgroundPadding = backgroundPadding,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        content = content
    )
/**
 * Represents the background and content colors used in a button in different states.
 *
 * See [ButtonDefaults.primaryButtonColors] for the default colors used in
 * a primary styled [Button].
 * See [ButtonDefaults.secondaryButtonColors] for the default colors used
 * in a secondary styled [Button].
 */
@Stable
public interface ButtonColors {
    /**
     * Represents the background color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    public fun backgroundColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    public fun contentColor(enabled: Boolean): State<Color>
}

/**
 * Represents the border stroke used in a [Button] in different states.
 */
@Stable
public interface ButtonBorder {
    @Composable
    /**
     * Represents the border stroke for this border, depending on [enabled] or null if no border
     *
     * @param enabled Whether the button is enabled
     */
    public fun borderStroke(enabled: Boolean): State<BorderStroke?>
}

/**
 * Contains the default values used by [Button].
 */
public object ButtonDefaults {
    /**
     * Creates a [ButtonColors] that represents the default background and content colors for a
     * primary [Button]. Primary buttons have a colored background with a contrasting content color.
     * If a button is disabled then the colors will have an alpha ([ContentAlpha.disabled]) value
     * applied.
     *
     * @param backgroundColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     */
    @Composable
    public fun primaryButtonColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor)
    ): ButtonColors {
        // For light background colors, the default disabled content colors do not provide
        // sufficient contrast. Instead, we default to using background for disabled content.
        // See b/254025377
        return buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledContentColor = MaterialTheme.colors.background
        )
    }

    /**
     * Creates a [ButtonColors] that represents the default background and content colors for a
     * secondary [Button]. Secondary buttons have a muted background with a contrasting content
     * color. If a button is disabled then the colors will have an alpha ([ContentAlpha.disabled])
     * value applied.
     *
     * @param backgroundColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     */
    @Composable
    public fun secondaryButtonColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = contentColorFor(backgroundColor)
    ): ButtonColors {
        return buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [ButtonColors] that represents the content colors for
     * an icon-only [Button]. If a button is disabled then the colors will have an alpha
     * ([ContentAlpha.disabled]) value applied.
     *
     * @param contentColor The content color of this [Button] when enabled
     */
    @Composable
    public fun iconButtonColors(
        contentColor: Color = MaterialTheme.colors.onSurface,
    ): ButtonColors {
        return buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
        )
    }

    /**
     * Creates a [ButtonColors] that represents the content colors for
     * an [OutlinedButton]. If a button is disabled then the colors will have an alpha
     * ([ContentAlpha.disabled]) value applied.
     *
     * @param contentColor The content color of this [OutlinedButton] when enabled
     */
    @Composable
    public fun outlinedButtonColors(
        contentColor: Color = MaterialTheme.colors.primary,
    ): ButtonColors {
        return buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [ButtonBorder] for the default border used in most [Button]
     *
     * @param borderStroke The border of this [Button] when enabled - or no border if null
     * @param disabledBorderStroke The border of this [Button] when disabled - or no border if null
     */
    @Composable
    public fun buttonBorder(
        borderStroke: BorderStroke? = null,
        disabledBorderStroke: BorderStroke? = borderStroke
    ): ButtonBorder {
        return DefaultButtonBorder(
            borderStroke = borderStroke,
            disabledBorderStroke = disabledBorderStroke
        )
    }

    /**
     * Creates a [ButtonBorder] for the [OutlinedButton]
     *
     * @param borderColor The color to use for the border for this [OutlinedButton] when enabled
     * @param disabledBorderColor The color to use for the border for this [OutlinedButton] when
     * disabled
     * @param borderWidth The width to use for the border for this [OutlinedButton]
     */
    @Composable
    public fun outlinedButtonBorder(
        borderColor: Color = MaterialTheme.colors.primaryVariant.copy(alpha = 0.6f),
        disabledBorderColor: Color = borderColor.copy(alpha = ContentAlpha.disabled),
        borderWidth: Dp = 1.dp
    ): ButtonBorder {
        return DefaultButtonBorder(
            borderStroke = BorderStroke(borderWidth, borderColor),
            disabledBorderStroke = BorderStroke(borderWidth, disabledBorderColor)
        )
    }

    /**
     * The default background size of a [CompactButton].
     */
    public val ExtraSmallButtonSize = 32.dp

    /**
     * The recommended size for a small [Button].
     * You can apply this value for the size by overriding Modifier.size directly on [Button].
     */
    public val SmallButtonSize = 48.dp

    /**
     * The default size applied for the [Button].
     * Note that you can override it by applying Modifier.size directly on [Button].
     */
    public val DefaultButtonSize = 52.dp

    /**
     * The recommended size for a large [Button].
     * You can apply this value for the size by overriding Modifier.size directly on [Button].
     */
    public val LargeButtonSize = 60.dp

    /**
     * The size of an icon when used inside a small-sized [Button] or a [CompactButton].
     */
    public val SmallIconSize = 24.dp

    /**
     * The default size of an icon when used inside a default-sized [Button].
     */
    public val DefaultIconSize = 26.dp

    /**
     * The size of an icon when used inside a large-sized [Button].
     */
    public val LargeIconSize = 30.dp

    /**
     * The default padding for a [CompactButton]. This will result in a larger tap area
     * than visible area.
     */
    public val CompactButtonBackgroundPadding = 8.dp

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in
     * a [Button].
     *
     * @param backgroundColor the background color of this [Button] when enabled
     * @param contentColor the content color of this [Button] when enabled
     * @param disabledBackgroundColor the background color of this [Button] when not enabled
     * @param disabledContentColor the content color of this [Button] when not enabled
     */
    @Composable
    public fun buttonColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        disabledBackgroundColor: Color = backgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledContentColor: Color = contentColor.copy(alpha = ContentAlpha.disabled)
    ): ButtonColors = DefaultButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor
    )
}

/**
 * Default [ButtonColors] implementation.
 */
@Immutable
private class DefaultButtonColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color
) : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) backgroundColor else disabledBackgroundColor
        )
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) contentColor else disabledContentColor
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultButtonColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}

/**
 * Default [ButtonBorder] implementation.
 */
@Immutable
private class DefaultButtonBorder(
    private val borderStroke: BorderStroke? = null,
    private val disabledBorderStroke: BorderStroke? = null
) : ButtonBorder {
    @Composable
    override fun borderStroke(enabled: Boolean): State<BorderStroke?> {
        return rememberUpdatedState(if (enabled) borderStroke else disabledBorderStroke)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultButtonBorder

        if (borderStroke != other.borderStroke) return false
        if (disabledBorderStroke != other.disabledBorderStroke) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderStroke.hashCode()
        result = 31 * result + disabledBorderStroke.hashCode()
        return result
    }
}
