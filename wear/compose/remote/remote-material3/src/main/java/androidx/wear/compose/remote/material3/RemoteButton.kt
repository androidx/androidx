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

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.capture.currentProfileSupportsPathClip
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
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
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
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.shapes.clipOutline
import androidx.compose.remote.creation.compose.shapes.drawOutline
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.asConstantColorOrNull
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.ButtonDefaults.scrimGradientEndColor
import androidx.wear.compose.material3.ButtonDefaults.scrimGradientStartColor
import androidx.wear.compose.material3.TextConfiguration
import androidx.wear.compose.remote.material3.RemoteButtonDefaults.buttonColors
import androidx.wear.compose.remote.material3.RemoteButtonDefaults.defaultButtonWithContainerPainterColors

/** @sample androidx.wear.compose.remote.material3.samples.RemoteButtonSimpleSample */
@Composable
@RemoteComposable
public fun RemoteButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteButtonDefaults.shape,
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        colors = colors,
        enabled = enabled,
        border = border,
        shape = shape,
        contentPadding = contentPadding,
        labelFont = RemoteMaterialTheme.typography.labelMedium,
        containerPainter = null,
        disabledContainerPainter = null,
        content = content,
    )
}

/** @sample androidx.wear.compose.remote.material3.samples.RemoteButtonWithContainerPainterSample */
@Composable
@RemoteComposable
public fun RemoteButton(
    onClick: RemoteAction,
    containerPainter: RemotePainter,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    disabledContainerPainter: RemotePainter =
        RemoteButtonDefaults.disabledContainerPainter(containerPainter),
    shape: RemoteShape = RemoteButtonDefaults.shape,
    colors: RemoteButtonColors =
        RemoteButtonDefaults.buttonWithNullableContainerPainterColors(containerPainter),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
        enabled = enabled,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        shape = shape,
        contentPadding = contentPadding,
        border = border,
        labelFont = RemoteMaterialTheme.typography.labelMedium,
        content = content,
    )
}

// TODO(b/261838497) Add Material3 UX guidance links
/**
 * @sample androidx.wear.compose.remote.material3.samples.RemoteButtonWithIconAndSecondaryLabelSample
 */
@Composable
@RemoteComposable
public fun RemoteButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    containerPainter: RemotePainter? = null,
    disabledContainerPainter: RemotePainter? = null,
    shape: RemoteShape = RemoteButtonDefaults.shape,
    colors: RemoteButtonColors =
        RemoteButtonDefaults.buttonWithNullableContainerPainterColors(containerPainter),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier.buttonSizeModifier(),
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
        labelFont = RemoteMaterialTheme.typography.labelMedium,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        labelContent =
            provideScopeContent(
                contentColor = colors.contentColor(enabled),
                textStyle = RemoteMaterialTheme.typography.labelMedium,
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
}

/** @sample androidx.wear.compose.remote.material3.samples.RemoteCompactButtonSimpleSample */
@Composable
@RemoteComposable
public fun RemoteCompactButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    shape: RemoteShape = RemoteButtonDefaults.compactButtonShape,
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    border: RemoteBorderStroke? = null,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.CompactButtonContentPadding,
    label: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
) {
    val tapPadding = RemoteButtonDefaults.CompactButtonTapTargetPadding

    RemoteBox(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { role = RemoteRole.Button }
                .compactButtonModifier()
                .padding(tapPadding)
                .clip(shape = shape)
                .clickable(
                    onClick,
                    enabled = enabled.constantValueOrNull ?: false && onClick != RemoteAction.None,
                ),
        contentAlignment = RemoteAlignment.Center,
    ) {
        if (label != null) {
            RemoteButtonImpl(
                onClick = RemoteAction.None,
                modifier =
                    RemoteModifier.height(RemoteButtonDefaults.CompactButtonVisibleHeight)
                        .widthIn(min = RemoteButtonDefaults.CompactButtonVisibleHeight),
                colors = colors,
                border = border,
                contentPadding = contentPadding,
                enabled = enabled,
                shape = shape,
                labelFont = RemoteMaterialTheme.typography.labelSmall,
                containerPainter = null,
                disabledContainerPainter = null,
                horizontalArrangement =
                    if (icon != null) RemoteArrangement.Start else RemoteArrangement.Center,
                role = null,
            ) {
                if (icon != null) {
                    RemoteBox(
                        modifier = RemoteModifier.wrapContentSize(),
                        contentAlignment = RemoteAlignment.Center,
                        content = provideScopeContent(colors.iconColor(enabled = enabled), icon),
                    )
                    RemoteBox(RemoteModifier.size(RemoteButtonDefaults.CompactButtonIconSpacing))
                }
                RemoteRow(
                    content =
                        provideScopeContent(
                            contentColor = colors.contentColor(enabled),
                            textStyle = RemoteMaterialTheme.typography.labelSmall,
                            textConfiguration =
                                TextConfiguration(
                                    textAlign =
                                        if (icon != null) TextAlign.Start else TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                ),
                            content = label,
                        )
                )
            }
        } else {
            RemoteButtonImpl(
                onClick = RemoteAction.None,
                modifier =
                    RemoteModifier.height(RemoteButtonDefaults.CompactButtonVisibleHeight)
                        .width(RemoteButtonDefaults.IconOnlyCompactButtonWidth),
                colors = colors,
                border = border,
                contentPadding = contentPadding,
                enabled = enabled,
                shape = shape,
                labelFont = RemoteMaterialTheme.typography.labelSmall,
                containerPainter = null,
                disabledContainerPainter = null,
                horizontalArrangement = RemoteArrangement.Center,
                role = null,
            ) {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxSize().wrapContentSize(),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    if (icon != null) {
                        provideScopeContent(colors.iconColor(enabled = enabled), icon)()
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
private fun RemoteButtonImpl(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteButtonColors,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    enabled: RemoteBoolean,
    border: RemoteBorderStroke?,
    shape: RemoteShape,
    contentPadding: RemotePaddingValues,
    labelFont: RemoteTextStyle,
    horizontalArrangement: RemoteArrangement.Horizontal = RemoteArrangement.Center,
    role: RemoteRole? = RemoteRole.Button,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val containerModifier =
        RemoteModifier.clip(shape = shape)
            .clickable(
                onClick = onClick,
                enabled = enabled.constantValueOrNull ?: false && onClick != RemoteAction.None,
            )
            .padding(contentPadding)
            .let {
                if (role != null) {
                    it.semantics(mergeDescendants = true) { this.role = role }
                } else {
                    it
                }
            }

    // When supported, clip the background to the shape's path so that container painters (e.g.
    // background images) and non-rounded shapes are clipped, rather than relying on the
    // rectangle and rounded rectangle clips supported by RemoteModifier.clip.
    val clipBackgroundToShape = currentProfileSupportsPathClip()

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier =
            modifier
                .drawWithContent {
                    val drawBackground: RemoteDrawScope.() -> Unit = {
                        drawShapedBackground(
                            shape = shape,
                            color = colors.containerColor(enabled),
                            enabled = enabled,
                            containerPainter = containerPainter,
                            disabledContainerPainter = disabledContainerPainter,
                            border = border,
                        )
                    }
                    if (clipBackgroundToShape) {
                        clipToShape(shape, drawBackground)
                    } else {
                        drawBackground()
                    }
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
private fun RemoteButtonImpl(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    secondaryLabelContent: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)?,
    icon: (@Composable @RemoteComposable () -> Unit)?,
    colors: RemoteButtonColors,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    enabled: RemoteBoolean,
    border: RemoteBorderStroke?,
    shape: RemoteShape,
    contentPadding: RemotePaddingValues,
    labelFont: RemoteTextStyle,
    iconSpacing: RemoteDp = RemoteButtonDefaults.IconSpacing,
    labelContent: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val hasIconOrSecondary = icon != null || secondaryLabelContent != null
    val arrangement = if (hasIconOrSecondary) RemoteArrangement.Start else RemoteArrangement.Center

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
        contentPadding = contentPadding,
        horizontalArrangement = arrangement,
    ) {
        if (icon != null) {
            RemoteBox(
                modifier = RemoteModifier.wrapContentSize(),
                contentAlignment = RemoteAlignment.Center,
                content = provideScopeContent(colors.iconColor(enabled = enabled), icon),
            )
            RemoteBox(RemoteModifier.size(iconSpacing))
        }
        if (hasIconOrSecondary) {
            RemoteColumn {
                RemoteRow(content = labelContent)
                if (secondaryLabelContent != null) {
                    RemoteBox(RemoteModifier.size(1.rdp))
                    RemoteRow(content = secondaryLabelContent)
                }
            }
        } else {
            RemoteRow(content = labelContent)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteButtonDefaults {
    public val shape: RemoteRoundedCornerShape = RemoteRoundedCornerShape(26.rdp)

    public val compactButtonShape: RemoteRoundedCornerShape = RemoteRoundedCornerShape(18.rdp)

    @Composable
    @RemoteComposable
    public fun buttonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonColors

    @Composable
    @RemoteComposable
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
            disabledContainerColor =
                disabledContainerColor
                    ?: if (containerColor?.asConstantColorOrNull() == Color.Transparent) {
                        Color.Transparent.rc
                    } else {
                        default.disabledContainerColor
                    },
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
            disabledSecondaryContentColor =
                disabledSecondaryContentColor ?: default.disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor ?: default.disabledIconColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun filledTonalButtonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledTonalButtonColors

    @Composable
    @RemoteComposable
    public fun filledTonalButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultFilledTonalButtonColors
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

    @Composable
    @RemoteComposable
    public fun filledVariantButtonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultFilledVariantButtonColors

    @Composable
    @RemoteComposable
    public fun filledVariantButtonColors(
        containerColor: RemoteColor? = null,
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultFilledVariantButtonColors
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

    @Composable
    @RemoteComposable
    public fun outlinedButtonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultOutlinedButtonColors

    @Composable
    @RemoteComposable
    public fun outlinedButtonColors(
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultOutlinedButtonColors
        return default.copy(
            contentColor = contentColor ?: default.contentColor,
            secondaryContentColor = secondaryContentColor ?: default.secondaryContentColor,
            iconColor = iconColor ?: default.iconColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
            disabledSecondaryContentColor =
                disabledSecondaryContentColor ?: default.disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor ?: default.disabledIconColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun childButtonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultChildButtonColors

    @Composable
    @RemoteComposable
    public fun childButtonColors(
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultChildButtonColors
        return default.copy(
            contentColor = contentColor ?: default.contentColor,
            secondaryContentColor = secondaryContentColor ?: default.secondaryContentColor,
            iconColor = iconColor ?: default.iconColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
            disabledSecondaryContentColor =
                disabledSecondaryContentColor ?: default.disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor ?: default.disabledIconColor,
        )
    }

    @Composable
    @RemoteComposable
    public fun buttonWithContainerPainterColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonWithContainerPainterColors

    @Composable
    @RemoteComposable
    public fun buttonWithContainerPainterColors(
        contentColor: RemoteColor? = null,
        secondaryContentColor: RemoteColor? = null,
        iconColor: RemoteColor? = null,
        disabledContentColor: RemoteColor? = null,
        disabledSecondaryContentColor: RemoteColor? = null,
        disabledIconColor: RemoteColor? = null,
    ): RemoteButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultButtonWithContainerPainterColors
        return default.copy(
            contentColor = contentColor ?: default.contentColor,
            secondaryContentColor = secondaryContentColor ?: default.secondaryContentColor,
            iconColor = iconColor ?: default.iconColor,
            disabledContentColor = disabledContentColor ?: default.disabledContentColor,
            disabledSecondaryContentColor =
                disabledSecondaryContentColor ?: default.disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor ?: default.disabledIconColor,
        )
    }

    /**
     * Creates a [RemoteButtonColors] for the content in a [RemoteButton], returns default
     * [buttonColors] if painter is null, else return [defaultButtonWithContainerPainterColors]
     *
     * @return a [RemoteButtonColors] for a [RemoteButton] with an optional [containerPainter]
     */
    @Composable
    @RemoteComposable
    internal fun buttonWithNullableContainerPainterColors(
        containerPainter: RemotePainter?
    ): RemoteButtonColors =
        if (containerPainter == null) {
            buttonColors()
        } else {
            buttonWithContainerPainterColors()
        }

    public val Height: RemoteDp = 52.rdp

    public val Width: RemoteDp = 12.rdp

    public val IconSpacing: RemoteDp = 6.rdp

    public val CompactButtonIconSpacing: RemoteDp = 4.rdp

    public val ExtraSmallIconSize: RemoteDp = 20.rdp

    public val SmallIconSize: RemoteDp = 24.rdp

    public val IconSize: RemoteDp = 26.rdp

    public val LargeIconSize: RemoteDp = 32.rdp

    public val ButtonHorizontalPadding: RemoteDp = 14.rdp

    public val ButtonVerticalPadding: RemoteDp = 6.rdp

    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = ButtonHorizontalPadding, vertical = ButtonVerticalPadding)

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

    public val CompactButtonContentPadding: RemotePaddingValues =
        RemotePaddingValues(
            leftPadding = CompactButtonHorizontalPadding,
            topPadding = CompactButtonVerticalPadding,
            rightPadding = CompactButtonHorizontalPadding,
            bottomPadding = CompactButtonVerticalPadding,
        )

    public val DisabledContainerAlpha: RemoteFloat = 0.12f.rf

    private val RemoteColorScheme.defaultButtonColors: RemoteButtonColors
        @Composable
        @RemoteComposable
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
        @RemoteComposable
        get() {
            return RemoteButtonColors(
                containerColor = Color.Transparent.rc,
                contentColor = onBackground,
                secondaryContentColor = onBackground.copy(alpha = 0.8f.rf),
                iconColor = onBackground,
                disabledContainerColor = Color.Transparent.rc,
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    private val RemoteColorScheme.defaultFilledTonalButtonColors: RemoteButtonColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteButtonColors(
                containerColor = surfaceContainer,
                contentColor = onSurface,
                secondaryContentColor = onSurfaceVariant,
                iconColor = primary,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    private val RemoteColorScheme.defaultFilledVariantButtonColors: RemoteButtonColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteButtonColors(
                containerColor = primaryContainer,
                contentColor = onPrimaryContainer,
                secondaryContentColor = onPrimaryContainer.copy(alpha = 0.8f.rf),
                iconColor = onPrimaryContainer,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    private val RemoteColorScheme.defaultOutlinedButtonColors: RemoteButtonColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteButtonColors(
                containerColor = Color.Transparent.rc,
                contentColor = onSurface,
                secondaryContentColor = onSurfaceVariant,
                iconColor = primary,
                disabledContainerColor = Color.Transparent.rc,
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    private val RemoteColorScheme.defaultChildButtonColors: RemoteButtonColors
        @Composable
        @RemoteComposable
        get() {
            return RemoteButtonColors(
                containerColor = Color.Transparent.rc,
                contentColor = onSurface,
                secondaryContentColor = onSurfaceVariant,
                iconColor = primary,
                disabledContainerColor = Color.Transparent.rc,
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    @Composable
    @RemoteComposable
    public fun containerPainter(
        image: RemoteImageBitmap,
        scrim: RemoteBrush? = scrimBrush(),
        alpha: RemoteFloat = DefaultAlpha.rf,
        shape: RemoteShape = this.shape,
        contentScale: ContentScale = ContentScale.Crop,
    ): RemotePainter {
        return remoteContainerPainter(image, alpha, shape, contentScale, scrim)
    }

    @Composable
    @RemoteComposable
    public fun disabledContainerPainter(containerPainter: RemotePainter): RemotePainter {
        return disabledRemoteContainerPainter(
            painter = containerPainter,
            alpha = DisabledContainerAlpha,
        )
    }

    @Composable
    @RemoteComposable
    public fun scrimBrush(): RemoteBrush {
        val startColor = scrimGradientStartColor.rc
        val endColor = scrimGradientEndColor.rc
        return RemoteBrush.linearGradient(colors = listOf(startColor, endColor))
    }

    @Composable
    @RemoteComposable
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
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

    @Stable
    internal fun iconColor(enabled: RemoteBoolean = true.rb): RemoteColor {
        return enabled.select(ifTrue = iconColor, ifFalse = disabledIconColor)
    }

    public fun copy(
        containerColor: RemoteColor = this.containerColor,
        contentColor: RemoteColor = this.contentColor,
        secondaryContentColor: RemoteColor = this.secondaryContentColor,
        iconColor: RemoteColor = this.iconColor,
        disabledContainerColor: RemoteColor = this.disabledContainerColor,
        disabledContentColor: RemoteColor = this.disabledContentColor,
        disabledSecondaryContentColor: RemoteColor = this.disabledSecondaryContentColor,
        disabledIconColor: RemoteColor = this.disabledIconColor,
    ): RemoteButtonColors =
        RemoteButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledSecondaryContentColor = disabledSecondaryContentColor,
            disabledIconColor = disabledIconColor,
        )
}

/** Draws a colored and shaped background when clipping is not supported. */
internal fun RemoteDrawScope.drawShapedBackground(
    shape: RemoteShape,
    color: RemoteColor,
    enabled: RemoteBoolean,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    border: RemoteBorderStroke? = null,
) {
    if (!enabled.hasConstantValue) {
        TODO("Dynamic clickable enabled value is not supported.")
    }

    val backgroundImagePainter =
        if (enabled.constantValue) containerPainter else disabledContainerPainter

    backgroundImagePainter?.let { with(it) { onDraw() } } ?: drawSolidColorShape(shape, color)

    // Draw border if specified
    if (border != null) {
        drawBorder(border, shape)
    }
}

private fun RemoteDrawScope.drawBorder(
    border: RemoteBorderStroke,
    shape: RemoteShape,
) {
    val strokeWidthPx = border.width.toPx()
    val outline =
        when (shape) {
            is RemoteCornerBasedShape -> {
                shape.createOutline(
                    size = RemoteSize(width, height),
                    layoutDirection = layoutDirection,
                    density = remoteDensity,
                    strokeWidth = strokeWidthPx,
                )
            }
            is RemoteEdgeButtonShape -> {
                shape.createOutline(
                    size = RemoteSize(width, height),
                    layoutDirection = layoutDirection,
                    density = remoteDensity,
                    strokeWidth = strokeWidthPx,
                )
            }
            else -> {
                shape.createOutline(
                    size = RemoteSize(width, height),
                    layoutDirection = layoutDirection,
                    density = remoteDensity,
                )
            }
        }
    val paint = RemotePaint {
        strokeWidth = strokeWidthPx
        style = RemotePaintingStyle.Stroke
    }
    with(border.brush) {
        applyTo(paint, RemoteSize(width, height))
    }
    drawOutline(outline, paint)
}

/**
 * Clips [block] to the outline of [shape]. Callers must check [currentProfileSupportsPathClip]
 * first.
 */
@Suppress("RestrictedApiAndroidX") // clipOutline
internal fun RemoteDrawScope.clipToShape(shape: RemoteShape, block: RemoteDrawScope.() -> Unit) =
    clipOutline(
        shape.createOutline(RemoteSize(width, height), layoutDirection, remoteDensity),
        block,
    )

private fun RemoteDrawScope.drawSolidColorShape(shape: RemoteShape, color: RemoteColor? = null) =
    drawOutline(
        shape.createOutline(
            size = RemoteSize(width, height),
            layoutDirection = layoutDirection,
            density = remoteDensity,
        ),
        RemotePaint {
            style = RemotePaintingStyle.Fill
            color?.let { this.color = it }
        },
    )

/**
 * Modifier to be applied to a [RemoteButton] to ensure that its size meets the recommended
 * minimums.
 *
 * @return a [RemoteModifier] that enforces the minimum recommended button size
 */
internal fun RemoteModifier.buttonSizeModifier(): RemoteModifier =
    this.defaultMinSize(
        minWidth = RemoteButtonDefaults.Width,
        minHeight = RemoteButtonDefaults.Height,
    )

private fun RemoteModifier.compactButtonModifier(): RemoteModifier {
    return this.height(RemoteButtonDefaults.CompactButtonHeight)
}

internal fun RemoteColor.toDisabledColor(
    disabledAlpha: RemoteFloat = DisabledContentAlpha.rf
): RemoteColor = this.copy(alpha = this.alpha * disabledAlpha)
