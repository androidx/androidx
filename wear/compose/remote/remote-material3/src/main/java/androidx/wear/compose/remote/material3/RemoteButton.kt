/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.linearGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonDefaults.scrimGradientEndColor
import androidx.wear.compose.material3.ButtonDefaults.scrimGradientStartColor
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.TextConfiguration

/**
 * Base level Wear Material3 [RemoteButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [RemoteButton] components that take specific content such as
 * icons and labels.
 *
 * [RemoteButton] takes the [RemoteButtonDefaults.buttonColors] color scheme by default, with
 * colored background, contrasting content color and no border. This is a high-emphasis button for
 * the primary, most important or most common action on a screen.
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteButtonSimpleSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states. See [RemoteButtonDefaults.buttonColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content Slot for composable body content displayed on the Button
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    shape: RemoteShape = RemoteButtonDefaults.shape,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        border = border,
        borderColor = borderColor,
        shape = shape,
        contentPadding = contentPadding,
        labelFont = LocalRemoteTypography.current.labelMedium,
        containerPainter = null,
        disabledContainerPainter = null,
        content = content,
    )
}

/**
 * Base level Wear Material3 [RemoteButton] that offers parameters for container image backgrounds,
 * with a single slot to take any content.
 *
 * An Image background is a means to reinforce the meaning of information in a Button. Buttons
 * should have a content color that contrasts with the background image and scrim.
 *
 * [RemoteButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param containerPainter The background image of this [RemoteButton] when enabled
 * @param disabledContainerPainter The background image of this [RemoteButton] when disabled
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states. See [RemoteButtonDefaults.buttonColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content Slot for composable body content displayed on the Button
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
public fun RemoteButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    containerPainter: RemotePainter,
    disabledContainerPainter: RemotePainter =
        RemoteButtonDefaults.disabledContainerPainter(containerPainter),
    colors: RemoteButtonColors =
        RemoteButtonDefaults.buttonWithNullableContainerPainterColors(containerPainter),
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    shape: RemoteShape = RemoteButtonDefaults.shape,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        shape = shape,
        contentPadding = contentPadding,
        border = border,
        borderColor = borderColor,
        labelFont = LocalRemoteTypography.current.labelMedium,
        content = content,
    )
}

/**
 * Wear Material3 [RemoteButton] that offers parameters for optional container image backgrounds,
 * with three slots and a specific layout for an icon, label and secondaryLabel. The icon and
 * secondaryLabel are optional. The items are laid out with the icon, if provided, at the start of a
 * row, with a column next containing the two label slots.
 *
 * An image background is a means to reinforce the meaning of information in a Button. Buttons
 * should have a content color that contrasts with the background image and scrim.
 *
 * This [RemoteButton] takes [containerPainter] for the container image background to be drawn when
 * the button is enabled and [disabledContainerPainter] for the image background when the button is
 * disabled (the [RemoteButtonColors] containerColor and disabledContainerColor properties are
 * ignored). It is recommended to use [RemoteButtonDefaults.containerPainter] to create the painters
 * so that a scrim is drawn on top of the container image, ensuring that any content above the
 * background is legible. If painters are not provided, a tonal color shape would be used as the
 * button background.
 *
 * The [RemoteButton] is stadium-shaped by default and its standard height is designed to take 2
 * lines of text - either a two-line label or both a single line label and a secondary label. With
 * localisation and/or large font sizes, the [RemoteButton] height adjusts to accommodate the
 * contents. The label and secondary label should be consistently aligned.
 *
 * [RemoteButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [RemoteButton] with an image background, an icon and a secondary label:
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned if there is an icon preset and "start" or "center"
 *   aligned if not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [RemoteButtonDefaults.IconSize] or
 *   [RemoteButtonDefaults.LargeIconSize].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable
 * @param containerPainter The [RemotePainter] to use to draw the container image of the
 *   [RemoteButton], such as returned by [RemoteButtonDefaults.containerPainter].
 * @param disabledContainerPainter [RemotePainter] to use to draw the container of the
 *   [RemoteButton] when not enabled, such as returned by
 *   [RemoteButtonDefaults.disabledContainerPainter].
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states (the containerColor and disabledContainerColor are
 *   overridden by containerPainter and disabledContainerPainter respectively). See
 *   [RemoteButtonDefaults.buttonWithContainerPainterColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
public fun RemoteButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    containerPainter: RemotePainter? = null,
    disabledContainerPainter: RemotePainter? = null,
    colors: RemoteButtonColors =
        RemoteButtonDefaults.buttonWithNullableContainerPainterColors(containerPainter),
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    shape: RemoteShape = RemoteButtonDefaults.shape,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
): Unit =
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier,
        secondaryLabelContent =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled),
                textStyle = RemoteMaterialTheme.typography.labelSmall,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
                content = secondaryLabel,
            ),
        icon = icon,
        enabled = enabled,
        shape = shape,
        labelFont = LocalRemoteTypography.current.labelMedium,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        border = border,
        borderColor = borderColor,
        contentPadding = contentPadding,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = LocalRemoteTypography.current.labelMedium,
                textConfiguration =
                    TextConfiguration(
                        textAlign =
                            if (icon != null || secondaryLabel != null) TextAlign.Start
                            else TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label,
            ),
    )

/**
 * A Wear Material3 [RemoteCompactButton] that offers two slots and a specific layout for an icon
 * and label. Both the icon and label are optional however it is expected that at least one will be
 * provided.
 *
 * The [RemoteCompactButton] is Stadium shaped and has a max height designed to take no more than
 * one line of text and/or one icon. The default max height is [ButtonDefaults.CompactButtonHeight].
 * This includes a visible button height of 32.dp and 8.dp of padding above and below the button in
 * order to meet accessibility guidelines that request a minimum of 48.dp height and width of
 * tappable area.
 *
 * If an icon is provided then the labels should be "start" aligned, e.g. left aligned in
 * left-to-right mode so that the text starts next to the icon.
 *
 * The items are laid out as follows.
 * 1. If a label is provided then the button will be laid out with the optional icon at the start of
 *    a row followed by the label with a default max height of
 *    [RemoteButtonDefaults.CompactButtonHeight].
 * 2. If only an icon is provided it will be laid out vertically and horizontally centered with a
 *    default height of [RemoteButtonDefaults.CompactButtonHeight] and the default width of
 *    [RemoteButtonDefaults.IconOnlyCompactButtonWidth]
 *
 * If neither icon nor label is provided then the button will displayed like an icon only button but
 * with no contents or background color.
 *
 * [RemoteCompactButton] takes the [RemoteButtonDefaults.buttonColors] color scheme by default, with
 * colored background, contrasting content color and no border. This is a high-emphasis button for
 * the primary, most important or most common action on a screen.
 *
 * [RemoteCompactButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCompactButtonSimpleSample
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 *   horizontally and vertically aligned icon of size [RemoteButtonDefaults.ExtraSmallIconSize] when
 *   used with a label or [RemoteButtonDefaults.SmallIconSize] when used as the only content in the
 *   button.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states. See [RemoteButtonDefaults.buttonColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param label Slot for composable body content displayed on the Button
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
public fun RemoteCompactButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    icon: (@Composable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteButtonDefaults.compactButtonShape,
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.CompactButtonContentPadding,
    label: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)?,
) {
    val tapPadding = RemoteButtonDefaults.CompactButtonTapTargetPadding

    RemoteBox(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { role = Role.Button }
                .compactButtonModifier()
                .padding(tapPadding)
                .clickable(onClick, enabled = enabled.constantValueOrNull ?: false)
    ) {
        if (label != null) {
            RemoteButtonImpl(
                modifier = RemoteModifier.height(RemoteButtonDefaults.CompactButtonVisibleHeight),
                secondaryLabelContent = null,
                icon = icon,
                enabled = enabled,
                shape = shape,
                labelFont = LocalRemoteTypography.current.labelSmall,
                containerPainter = null,
                disabledContainerPainter = null,
                colors = colors,
                border = border,
                borderColor = borderColor,
                contentPadding = contentPadding,
                labelContent =
                    provideScopeContent(
                        contentColor = colors.contentColor(enabled),
                        textStyle = LocalRemoteTypography.current.labelSmall,
                        textConfiguration =
                            TextConfiguration(
                                textAlign = if (icon != null) TextAlign.Start else TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                            ),
                        content = label,
                    ),
            )
        } else {
            // Icon only compact buttons have their own layout with a specific width and center
            // aligned
            // content. We use the base simple single slot Button under the covers.
            RemoteButtonImpl(
                modifier =
                    RemoteModifier.height(RemoteButtonDefaults.CompactButtonVisibleHeight)
                        .width(RemoteButtonDefaults.IconOnlyCompactButtonWidth),
                enabled = enabled,
                shape = shape,
                labelFont = LocalRemoteTypography.current.labelSmall,
                containerPainter = null,
                disabledContainerPainter = null,
                colors = colors,
                border = border,
                borderColor = borderColor,
                contentPadding = contentPadding,
            ) {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxSize().wrapContentSize(),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    if (icon != null) {
                        icon()
                    }
                }
            }
        }
    }
}

/**
 * Button with label. This allows to use the token values for individual buttons instead of relying
 * on common values.
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
private fun RemoteButtonImpl(
    onClick: Action? = null,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteButtonColors,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    enabled: RemoteBoolean,
    border: RemoteDp?,
    borderColor: RemoteColor?,
    shape: RemoteShape,
    contentPadding: RemotePaddingValues,
    labelFont: RemoteTextStyle,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val containerModifier =
        RemoteModifier.clickable(
                actions = buildList { onClick?.let { add(it) } },
                enabled = enabled.constantValueOrNull ?: false && onClick != null,
            )
            .padding(contentPadding)
            .semantics(mergeDescendants = true) { role = Role.Button }

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        horizontalArrangement = RemoteArrangement.Center,
        modifier =
            modifier
                .drawWithContent {
                    drawShapedBackground(
                        shape = shape,
                        color = colors.containerColor(enabled),
                        enabled = enabled,
                        containerPainter = containerPainter,
                        disabledContainerPainter = disabledContainerPainter,
                        borderColor = borderColor,
                        borderStrokeWidth = border?.value,
                    )
                    drawContent()
                }
                .then(containerModifier),
        content = provideScopeContent(colors.contentColor(enabled = enabled), labelFont, content),
    )
}

/**
 * Button with icon, label and secondary label. This allows to use the token values for individual
 * buttons instead of relying on common values.
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
private fun RemoteButtonImpl(
    onClick: Action? = null,
    modifier: RemoteModifier = RemoteModifier,
    secondaryLabelContent: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)?,
    icon: (@Composable @RemoteComposable () -> Unit)?,
    colors: RemoteButtonColors,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    enabled: RemoteBoolean,
    border: RemoteDp?,
    borderColor: RemoteColor?,
    shape: RemoteShape,
    contentPadding: RemotePaddingValues,
    labelFont: RemoteTextStyle,
    labelContent: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        labelFont = labelFont,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        border = border,
        borderColor = borderColor,
        contentPadding = contentPadding,
    ) {
        if (icon != null) {
            RemoteBox(
                modifier = RemoteModifier.wrapContentSize(),
                contentAlignment = RemoteAlignment.Center,
                content = icon,
            )
            RemoteBox(RemoteModifier.size(RemoteButtonDefaults.IconSpacing))
        }
        RemoteColumn(modifier = RemoteModifier) {
            RemoteRow(content = labelContent)
            if (secondaryLabelContent != null) {
                RemoteBox(RemoteModifier.size(1.rdp))
                RemoteRow(content = secondaryLabelContent)
            }
        }
    }
}

/** Contains the default values used by [RemoteButton] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteButtonDefaults {
    /** Recommended [RemoteRoundedCornerShape] for [RemoteButton]. */
    @Suppress("RestrictedApiAndroidX")
    public val shape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(16.rdp)

    /** Recommended [RemoteRoundedCornerShape] for [RemoteCompactButton]. */
    @Suppress("RestrictedApiAndroidX")
    public val compactButtonShape: RemoteRoundedCornerShape
        get() = RemoteRoundedCornerShape(18.rdp)

    /**
     * Creates a [RemoteButtonColors] that represents the default background and content colors used
     * in a [RemoteButton].
     */
    @Composable
    public fun buttonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonColors

    /**
     * Creates a [RemoteButtonColors] that represents the default background and content colors used
     * in a [RemoteButton].
     *
     * @param containerColor The background color of this [RemoteButton] when enabled
     * @param contentColor The content color of this [RemoteButton] when enabled
     * @param secondaryContentColor The content color of this [RemoteButton] when enabled
     * @param iconColor The content color of this [RemoteButton] when enabled
     * @param disabledContainerColor The background color of this [RemoteButton] when not enabled
     * @param disabledContentColor The content color of this [RemoteButton] when not enabled
     * @param disabledSecondaryContentColor The content color of this [RemoteButton] when not
     *   enabled
     * @param disabledIconColor The content color of this [RemoteButton] when not enabled
     */
    @Composable
    public fun buttonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultButtonColors
        return default.copy(
            containerColor = containerColor ?: default.containerColor,
            contentColor = contentColor ?: default.contentColor,
            secondaryContentColor = secondaryContentColor ?: default.secondaryContentColor,
            iconColor = iconColor ?: default.iconColor,
            disabledContainerColor = disabledContainerColor ?: default.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
            disabledSecondaryContentColor =
                disabledSecondaryContentColor ?: default.disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor ?: default.disabledIconColor,
        )
    }

    /**
     * Creates a [RemoteButtonColors] for the content in a [RemoteButton] with an image container
     * painter.
     */
    @Composable
    public fun buttonWithContainerPainterColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonWithContainerPainterColors

    /**
     * Creates a [RemoteButtonColors] for the content in a [RemoteButton], returns default
     * [buttonColors] if painter is null, else return [defaultButtonWithContainerPainterColors]
     */
    @Suppress("RestrictedApiAndroidX")
    @Composable
    internal fun buttonWithNullableContainerPainterColors(
        containerPainter: RemotePainter?
    ): RemoteButtonColors =
        if (containerPainter == null) {
            buttonColors()
        } else {
            buttonWithContainerPainterColors()
        }

    /** The default minimum height applied for the [RemoteButton]. */
    public val Height: RemoteDp = 52.rdp

    /** The default minimum width applied for the [RemoteButton]. */
    public val Width: RemoteDp = 12.rdp

    /**
     * The default size of the spacing between an icon and a text when they are used inside a
     * [RemoteButton].
     */
    public val IconSpacing: RemoteDp = 6.rdp

    /**
     * The recommended icon size when used in [RemoteCompactButton]s containing both icon and text.
     */
    public val ExtraSmallIconSize: RemoteDp = 20.rdp

    /** The recommended icon size when used in [CompactButton]s containing icon-only content. */
    public val SmallIconSize: RemoteDp = 24.rdp

    /** The recommended default size for icons when used inside a [RemoteButton]. */
    public val IconSize: RemoteDp = 26.rdp

    /** The recommended icon size when used in [RemoteButton]s for icons such as an app icon */
    public val LargeIconSize: RemoteDp = 32.rdp

    /** The recommended horizontal padding used by [RemoteButton] by default */
    public val ButtonHorizontalPadding: RemoteDp = 14.rdp

    /** The recommended vertical padding used by [RemoteButton] by default */
    public val ButtonVerticalPadding: RemoteDp = 6.rdp

    /** The default content padding used by [RemoteButton] */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = ButtonHorizontalPadding, vertical = ButtonVerticalPadding)

    /**
     * The height applied for the [RemoteCompactButton]. This includes a visible button height of
     * 32.dp and 8.dp of padding above and below the button in order to meet accessibility
     * guidelines that request a minimum of 48.dp height and width of tappable area.
     *
     * Note that you can override it by adjusting Modifier.height and Modifier.padding directly on
     * [RemoteCompactButton].
     */
    public val CompactButtonHeight: RemoteDp = 48.rdp
    internal val CompactButtonVisibleHeight: RemoteDp = 32.rdp

    /**
     * The default padding to be provided around a [RemoteCompactButton] in order to ensure that its
     * tappable area meets minimum UX guidance.
     */
    internal val CompactButtonTapTargetPadding: RemotePaddingValues =
        RemotePaddingValues(topPadding = 8.rdp, bottomPadding = 8.rdp)

    internal val IconOnlyCompactButtonWidth = 52.rdp

    internal val CompactButtonHorizontalPadding: RemoteDp = 12.rdp
    internal val CompactButtonVerticalPadding: RemoteDp = 0.rdp

    /** The default content padding used by [CompactButton] */
    public val CompactButtonContentPadding: RemotePaddingValues =
        RemotePaddingValues(
            leftPadding = CompactButtonHorizontalPadding,
            topPadding = CompactButtonVerticalPadding,
            rightPadding = CompactButtonHorizontalPadding,
            bottomPadding = CompactButtonVerticalPadding,
        )

    /** The default alpha applied to the container when the button is disabled. */
    public val DisabledContainerAlpha: Float = 0.12f

    private val RemoteColorScheme.defaultButtonColors: RemoteButtonColors
        @Composable
        get() {
            return RemoteButtonColors(
                containerColor = primary,
                contentColor = onPrimary,
                secondaryContentColor = onPrimary.copy(alpha = 0.8f.rf),
                iconColor = onPrimary,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    private val RemoteColorScheme.defaultButtonWithContainerPainterColors: RemoteButtonColors
        @Composable
        get() {
            return RemoteButtonColors(
                containerColor = primary,
                contentColor = onBackground,
                secondaryContentColor = onBackground.copy(alpha = 0.8f.rf),
                iconColor = onBackground,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    /**
     * Creates a [RemotePainter] for the background of a [RemoteButton] with container painter, that
     * displays an image with a scrim on top to make sure that any content above the background will
     * be legible.
     *
     * An Image background is a means to reinforce the meaning of information in a Button. Buttons
     * should have a content color that contrasts with the background image and scrim.
     *
     * @param image The [RemotePainter] to use to draw the container background of the
     *   [RemoteButton].
     * @param scrim The [RemoteBrush] to use to paint a scrim over the container image to ensure
     *   that any text drawn over the image is legible.
     * @param alpha Opacity of the container image painter and scrim.
     */
    @Suppress("RestrictedApiAndroidX")
    @Composable
    public fun containerPainter(
        image: RemotePainter,
        scrim: RemoteBrush? = image.intrinsicSize?.let { scrimBrush(it) },
        alpha: RemoteFloat = DefaultAlpha.rf,
    ): RemotePainter {
        return remoteContainerPainter(image, scrim, alpha)
    }

    /**
     * Creates a [RemotePainter] for the disabled background of a [RemoteButton] with container
     * painter - draws the containerPainter with an alpha applied to achieve a disabled effect.
     *
     * An Image background is a means to reinforce the meaning of information in a Button. Buttons
     * should have a content color that contrasts with the background image and scrim.
     *
     * @param containerPainter The [RemotePainter] to use to draw the container background of the
     *   [RemoteButton].
     */
    @Suppress("RestrictedApiAndroidX")
    @Composable
    public fun disabledContainerPainter(containerPainter: RemotePainter): RemotePainter {
        return disabledRemoteContainerPainter(
            painter = containerPainter,
            alpha = DisabledContainerAlpha.rf,
        )
    }

    /**
     * Creates a [RemoteBrush] for the recommended scrim drawn on top of image container
     * backgrounds.
     */
    @Suppress("RestrictedApiAndroidX")
    @Composable
    public fun scrimBrush(size: RemoteSize): RemoteBrush {
        val startColor = scrimGradientStartColor.rc
        val endColor = scrimGradientEndColor.rc
        return RemoteBrush.linearGradient(
            colors = listOf(startColor, endColor),
            RemoteOffset.Zero,
            RemoteOffset(size.width, size.height),
        )
    }
}

/**
 * Represents the container and content colors used in buttons in different states.
 *
 * @param containerColor The background color of this [RemoteButton] when enabled (overridden by the
 *   containerPainter parameter on Buttons with image backgrounds).
 * @param contentColor The content color of this [RemoteButton] when enabled.
 * @param secondaryContentColor The content color of this [RemoteButton] when enabled.
 * @param iconColor The content color of this [RemoteButton] when enabled.
 * @param disabledContainerColor The background color of this [RemoteButton] when not enabled
 *   (overridden by the disabledContainerPainter parameter on Buttons with image backgrounds)
 * @param disabledContentColor The content color of this [RemoteButton] when not enabled.
 * @param disabledSecondaryContentColor The content color of this [RemoteButton] when not enabled.
 * @param disabledIconColor The content color of this [RemoteButton] when not enabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteButtonColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val secondaryContentColor: RemoteColor,
    public val iconColor: RemoteColor,
    public val disabledContainerColor: RemoteColor,
    public val disabledContentColor: RemoteColor,
    public val disabledSecondaryContentColor: RemoteColor,
    public val disabledIconColor: RemoteColor,
) {
    @Stable
    internal fun contentColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = contentColor, ifFalse = disabledContentColor)
    }

    @Stable
    internal fun containerColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = containerColor, ifFalse = disabledContainerColor)
    }

    @Stable
    internal fun secondaryContentColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(
            ifTrue = secondaryContentColor,
            ifFalse = disabledSecondaryContentColor,
        )
    }

    /** Returns a copy of this RemoteButtonColors optionally overriding some of the values. */
    public fun copy(
        containerColor: RemoteColor? = this.containerColor,
        contentColor: RemoteColor? = this.contentColor,
        secondaryContentColor: RemoteColor? = this.secondaryContentColor,
        iconColor: RemoteColor? = this.iconColor,
        disabledContainerColor: RemoteColor? = this.disabledContainerColor,
        disabledContentColor: RemoteColor? = this.disabledContentColor,
        disabledSecondaryContentColor: RemoteColor? = this.disabledSecondaryContentColor,
        disabledIconColor: RemoteColor? = this.disabledIconColor,
    ): RemoteButtonColors =
        RemoteButtonColors(
            containerColor = containerColor ?: this.containerColor,
            contentColor = contentColor ?: this.contentColor,
            secondaryContentColor = secondaryContentColor ?: this.secondaryContentColor,
            iconColor = iconColor ?: this.iconColor,
            disabledContainerColor = disabledContainerColor ?: this.disabledContainerColor,
            disabledContentColor = disabledContentColor ?: this.disabledContentColor,
            disabledSecondaryContentColor =
                disabledSecondaryContentColor ?: this.disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor ?: this.disabledIconColor,
        )
}

/** Draws a colored and shaped background with when clipping is not supported. */
@Suppress("RestrictedApiAndroidX")
internal fun RemoteDrawScope.drawShapedBackground(
    shape: RemoteShape,
    color: RemoteColor,
    enabled: RemoteBoolean,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    borderColor: RemoteColor?,
    borderStrokeWidth: RemoteFloat?,
) {
    val w = width
    val h = height

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        drawRect(paint = RemotePaint { this.color = color })
        return
    }

    if (!enabled.hasConstantValue) {
        TODO("Dynamic clickable enabled value is not supported.")
    }

    val backgroundImagePainter =
        if (enabled.constantValue == true) containerPainter else disabledContainerPainter

    if (backgroundImagePainter != null) {
        // Draws solid shape as destination
        drawSolidColorShape(shape, w, h)

        // TODO: Fix BlendMode.SRC_IN so it draws an shaped image
        with(backgroundImagePainter) { draw() }
    } else {
        // Draws solid color shape
        drawSolidColorShape(shape, w, h, color)
    }

    // Draw border if specified
    if (borderColor != null && borderStrokeWidth != null) {
        drawBorder(borderColor, borderStrokeWidth, shape, w, h)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawBorder(
    borderColor: RemoteColor,
    borderStrokeWidth: RemoteFloat,
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
) {
    with(shape.createOutline(RemoteSize(w, h), remoteDensity, layoutDirection)) {
        drawOutline(
            RemotePaint {
                color = borderColor
                strokeWidth = borderStrokeWidth
                style = PaintingStyle.Stroke
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawSolidColorShape(
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
    color: RemoteColor? = null,
) {
    with(shape.createOutline(RemoteSize(w, h), remoteDensity, layoutDirection)) {
        drawOutline(
            RemotePaint {
                style = PaintingStyle.Fill
                color?.let { this.color = it }
            }
        )
    }
}

// TODO(b/459724215): Constraint shouldn't be enforced when there is not enough space.
public fun RemoteModifier.buttonSizeModifier(): RemoteModifier =
    this.heightIn(min = RemoteButtonDefaults.Height).widthIn(min = RemoteButtonDefaults.Width)

private fun RemoteModifier.compactButtonModifier(): RemoteModifier {
    return this.height(RemoteButtonDefaults.CompactButtonHeight)
}

internal fun RemoteColor.toDisabledColor(
    disabledAlpha: RemoteFloat = DisabledContentAlpha.rf
): RemoteColor = this.copy(alpha = this.alpha * disabledAlpha)
