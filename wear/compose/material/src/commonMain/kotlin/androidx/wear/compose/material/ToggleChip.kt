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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A [ToggleChip] is a specialized type of [Chip] that includes a slot for a bi-state toggle icon
 * such as a radio button, toggle or checkbox.
 *
 * The Wear Material [ToggleChip] offers four slots and a specific layout for an application icon, a
 * label, a secondaryLabel and toggle icon. The application icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots in the middle and a slot for the toggle icons at the end.
 *
 * The [ToggleChip] is Stadium shaped and has a max height designed to take no more than
 * two lines of text of [Typography.button] style.
 *
 * The recommended set of [ToggleChipColors] can be obtained from
 * [ToggleChipDefaults], e.g. [ToggleChipDefaults.toggleChipColors].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked/selected status is
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned.
 * @param toggleIcon A slot for providing the chip's toggle icon(s). The
 * contents are expected to be a horizontally and vertically centre aligned icon of size
 * [ToggleChipDefaults.IconSize].
 * changed.
 * @param modifier Modifier to be applied to the chip
 * @param appIcon An optional slot for providing an icon to indicate the purpose of the chip. The
 * contents are expected to be a horizontally and vertically centre aligned icon of size
 * [ToggleChipDefaults.IconSize].
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param colors [ToggleChipColors] that will be used to resolve the background and
 * content color for this chip in different states, see
 * [ToggleChipDefaults.toggleChipColors].
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this chip's "toggleable" tap area. You can create and pass in your own
 * remembered [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 */
@Composable
public fun ToggleChip(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: @Composable () -> Unit,
    toggleIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    appIcon: @Composable (() -> Unit)? = null,
    secondaryLabel: @Composable (() -> Unit)? = null,
    colors: ToggleChipColors = ToggleChipDefaults.toggleChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ToggleChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier = modifier
            .height(ToggleChipDefaults.Height)
            .clip(shape = shape)
    ) {
        // TODO: Due to b/178201337 the paint() modifier on the box doesn't make a call to draw the
        //  box contents. As a result we need to have stacked boxes to enable us to paint the
        //  background
        val painterModifier =
            Modifier
                .paint(
                    painter = colors.background(enabled = enabled, checked = checked).value,
                )

        val contentBoxModifier =
            Modifier
                .toggleable(
                    enabled = enabled,
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Checkbox,
                    indication = rememberRipple(),
                    interactionSource = interactionSource
                )
                .fillMaxSize()

        Box(
            modifier = painterModifier
        )
        Box(
            modifier = contentBoxModifier
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    Box(
                        modifier = Modifier.wrapContentSize(align = Alignment.Center)
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.contentColor(
                                enabled = enabled,
                                checked = checked
                            ).value,
                            LocalContentAlpha provides
                                colors.contentColor(
                                    enabled = enabled,
                                    checked = checked
                                ).value.alpha,
                            content = appIcon
                        )
                    }
                    Spacer(modifier = Modifier.size(ToggleChipDefaults.IconSpacing))
                }
                Labels(
                    colors = colors,
                    label = label,
                    secondaryLabel = secondaryLabel,
                    enabled = enabled,
                    checked = checked,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .width(36.dp)
                        .wrapContentWidth(align = Alignment.End)
                ) {
                    Spacer(
                        modifier = Modifier.size(
                            ToggleChipDefaults.ToggleIconSpacing
                        )
                    )
                    CompositionLocalProvider(
                        LocalContentColor provides
                            colors.toggleIconTintColor(
                                enabled = enabled,
                                checked = checked
                            ).value,
                        LocalContentAlpha provides
                            colors.toggleIconTintColor(
                                enabled = enabled,
                                checked = checked
                            ).value.alpha,
                        content = toggleIcon
                    )
                }
            }
        }
    }
}

/**
 * A [SplitToggleChip] is a specialized type of [Chip] that includes a slot for a toggle control,
 * such as a radio button, toggle or checkbox. The [SplitToggleChip] differs from the
 * [ToggleChip] by having two "tappable" areas, one clickable and one toggleable.
 *
 * The Wear Material [SplitToggleChip] offers three slots and a specific layout for a label,
 * secondaryLabel and toggle icon. The secondaryLabel is optional. The items are laid out
 * with a column containing the two label slots and a slot for the toggle icon at the
 * end.
 *
 * A [SplitToggleChip] has two tappable areas, one tap area for the labels and another for the
 * toggle control. The [onClick] listener will be associated with the main body of the split toggle
 * chip with the [onCheckedChange] listener associated with the toggle icon area only.
 *
 * For a split toggle chip the background of the tappable background area behind the toggle icon
 * will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The [SplitToggleChip] is Stadium shaped and has a max height designed to take no more than
 * two lines of text of [Typography.button] style.
 *
 * The recommended set of [ToggleChipColors] can be obtained from
 * [ToggleChipDefaults], e.g. [ToggleChipDefaults.toggleChipColors].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked/selected status is
 * changed.
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned.
 * @param toggleIcon A slot for providing the chip's toggle icon(s). The
 * contents are expected to be a horizontally and vertically centre aligned icon of size
 * [ToggleChipDefaults.IconSize].
 * @param onClick Click listener called when the user clicks the main body of the chip, the area
 * behind the labels.
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be "start" or "center" aligned. label and secondaryLabel contents should be consistently
 * aligned.
 * @param colors [ToggleChipColors] that will be used to resolve the background and
 * content color for this chip in different states, see
 * [ToggleChipDefaults.toggleChipColors].
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param checkedInteractionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this chip's "toggleable" tap area. You can create and pass in your own
 * remembered [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param clickInteractionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this chip's "clickable" tap area. You can create and pass in your own
 * remembered [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 */
@Composable
public fun SplitToggleChip(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: @Composable () -> Unit,
    toggleIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: @Composable (() -> Unit)? = null,
    colors: ToggleChipColors = ToggleChipDefaults.toggleChipColors(),
    enabled: Boolean = true,
    checkedInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    clickInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ToggleChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier = modifier
            .height(ToggleChipDefaults.Height)
            .clip(shape = shape)
    ) {
        // TODO: Due to b/178201337 the paint() modifier on the box doesn't make a call to draw the
        //  box contents. As a result we need to have stacked boxes to enable us to paint the
        //  background
        val painterModifier =
            Modifier
                .paint(
                    painter = colors.background(enabled = enabled, checked = checked).value,
                )

        val contentBoxModifier = Modifier.fillMaxSize()

        Box(
            modifier = painterModifier
        )
        Box(
            modifier = contentBoxModifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            enabled = enabled,
                            onClick = onClick,
                            role = Role.Checkbox,
                            indication = rememberRipple(),
                            interactionSource = clickInteractionSource,
                        )
                        .fillMaxSize()
                        .padding(
                            start = contentPadding
                                .calculateStartPadding(LocalLayoutDirection.current),
                            end = 0.dp,
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding()
                        )
                        .weight(1.0f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Labels(
                        colors = colors,
                        label = label,
                        secondaryLabel = secondaryLabel,
                        enabled = enabled,
                        checked = checked,
                    )
                    Spacer(
                        modifier = Modifier
                            .size(ToggleChipDefaults.ToggleIconSpacing)
                    )
                }
                var splitBoxModifier = Modifier.toggleable(
                    enabled = enabled,
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Checkbox,
                    indication = rememberRipple(),
                    interactionSource = checkedInteractionSource,
                )
                val splitBackgroundOverlayColor = colors.splitBackgroundOverlay(
                    enabled = enabled,
                    checked = checked,
                ).value
                splitBoxModifier = splitBoxModifier
                    .fillMaxHeight()
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(color = splitBackgroundOverlayColor)
                        }
                    }
                    .align(Alignment.CenterVertically)
                    .width(52.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .wrapContentWidth(align = Alignment.End)
                    .padding(
                        start = 0.dp,
                        end = contentPadding
                            .calculateEndPadding(
                                layoutDirection = LocalLayoutDirection.current
                            ),
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding()
                    )
                Box(
                    modifier = splitBoxModifier
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            colors.toggleIconTintColor(
                                enabled = enabled,
                                checked = checked
                            ).value,
                        LocalContentAlpha provides
                            colors.toggleIconTintColor(
                                enabled = enabled,
                                checked = checked
                            ).value.alpha,
                        content = toggleIcon
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Labels(
    colors: ToggleChipColors,
    enabled: Boolean,
    checked: Boolean,
    label: @Composable () -> Unit,
    secondaryLabel: @Composable (() -> Unit)?
) {
    Column(modifier = Modifier.weight(1.0f)) {
        CompositionLocalProvider(
            LocalContentColor provides
                colors.contentColor(enabled = enabled, checked = checked).value,
            LocalTextStyle provides MaterialTheme.typography.button,
            LocalContentAlpha provides
                colors.contentColor(enabled = enabled, checked = checked).value.alpha,
            content = label
        )
        if (secondaryLabel != null) {
            CompositionLocalProvider(
                LocalContentColor provides
                    colors.secondaryContentColor(
                        enabled = enabled,
                        checked = checked
                    ).value,
                LocalTextStyle provides MaterialTheme.typography.caption2,
                LocalContentAlpha provides
                    colors.secondaryContentColor(
                        enabled = enabled,
                        checked = checked
                    ).value.alpha,
                content = secondaryLabel
            )
        }
    }
}

/**
 * Represents the background and content colors used in [ToggleChip]s and [SplitToggleChip]s
 * in different states.
 */
@Stable
public interface ToggleChipColors {
    /**
     * Represents the background treatment for this chip, depending on the [enabled] and [checked]
     * properties. Backgrounds are typically a linear gradient when the chip is checked/selected
     * and solid when it is not.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    public fun background(enabled: Boolean, checked: Boolean): State<Painter>

    /**
     * Represents the content color for this chip, depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    public fun contentColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the secondary content color for this chip, depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    public fun secondaryContentColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the icon tint color for the toggle icon for this chip, depending on the [enabled]
     * and [checked]properties.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    public fun toggleIconTintColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the overlay to apply to a split background SplitToggleChip to distinguish
     * between the two tappable areas. The overlay will be applied to "lighten" the background of
     * area under the toggle icons, depending on the [enabled] and [checked] properties.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    public fun splitBackgroundOverlay(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Contains the default values used by [Chip]
 */
public object ToggleChipDefaults {

    /**
     * Creates a [ToggleChipColors] for use in a ToggleChip or SplitToggleChip.
     * [ToggleChip]s are expected to have a linear gradient background when
     * checked/selected, similar to a [ChipDefaults.gradientBackgroundChipColors()] and a solid
     * neutral background when not checked/selected (similar to a
     * [ChipDefaults.secondaryChipColors])
     *
     * @param checkedStartBackgroundColor The background color used at the start of the gradient of
     * a [ToggleChip] or [SplitToggleChip] when enabled and checked/selected.
     * @param checkedEndBackgroundColor The background color used at the end of the gradient of a
     * [ToggleChip] or [SplitToggleChip] when enabled and checked/selected.
     * @param checkedContentColor The content color of a [ToggleChip] or [SplitToggleChip]
     * when enabled and checked/selected.
     * @param checkedSecondaryContentColor The secondary content color of this
     * [ToggleChip] or [SplitToggleChip] when enabled and checked/selected, used for
     * secondaryLabel content
     * @param checkedToggleIconTintColor The icon tint color of this
     * [ToggleChip] or [SplitToggleChip] when enabled and checked/selected, used for
     * ToggleIcon content
     * @param uncheckedStartBackgroundColor The background color used at the start of the gradient
     * of a [ToggleChip] or [SplitToggleChip] when enabled and unchecked/not selected.
     * @param uncheckedEndBackgroundColor The background color used at the end of the gradient of a
     * [ToggleChip] or [SplitToggleChip] when enabled and unchecked/not selected.
     * @param uncheckedContentColor The content color of a [ToggleChip] or [SplitToggleChip]
     * when enabled and checked/selected.
     * @param uncheckedSecondaryContentColor The secondary content color of this
     * [ToggleChip] or [SplitToggleChip] when enabled and unchecked/not selected, used for
     * secondaryLabel content
     * @param uncheckedToggleIconTintColor The icon tint color of this
     * [ToggleChip] or [SplitToggleChip] when enabled and unchecked/not selected, used for
     * ToggleIcon content
     * @param splitBackgroundOverlayColor The color to use to lighten/distinguish the background
     * behind the ToggleIcon for a split background chip. A split background chip has two
     * tappable areas, one for the main body of the chip and one for area around the toggle
     * control icon.
     * @param gradientDirection Whether the chips gradient should be start to end (indicated by
     * [LayoutDirection.Ltr]) or end to start (indicated by [LayoutDirection.Rtl]).
     */
    @Composable
    public fun toggleChipColors(
        checkedStartBackgroundColor: Color = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
        checkedEndBackgroundColor: Color = MaterialTheme.colors.surface,
        checkedContentColor: Color = contentColorFor(checkedEndBackgroundColor),
        checkedSecondaryContentColor: Color = MaterialTheme.colors.onSurfaceVariant,
        checkedToggleIconTintColor: Color = checkedContentColor,
        uncheckedStartBackgroundColor: Color = MaterialTheme.colors.surface,
        uncheckedEndBackgroundColor: Color = MaterialTheme.colors.surface,
        uncheckedContentColor: Color = contentColorFor(checkedEndBackgroundColor),
        uncheckedSecondaryContentColor: Color = uncheckedContentColor,
        uncheckedToggleIconTintColor: Color = uncheckedContentColor,
        splitBackgroundOverlayColor: Color = Color.White.copy(alpha = 0.05f),
        gradientDirection: LayoutDirection =
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) LayoutDirection.Rtl
            else LayoutDirection.Ltr
    ): ToggleChipColors {
        val checkedBackgroundColors: List<Color>
        val disabledCheckedBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            checkedBackgroundColors = listOf(
                checkedStartBackgroundColor,
                checkedEndBackgroundColor
            )
            disabledCheckedBackgroundColors = listOf(
                checkedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
                checkedEndBackgroundColor.copy(alpha = ContentAlpha.disabled)
            )
        } else {
            checkedBackgroundColors = listOf(
                checkedEndBackgroundColor,
                checkedStartBackgroundColor
            )
            disabledCheckedBackgroundColors = listOf(
                checkedEndBackgroundColor.copy(alpha = ContentAlpha.disabled),
                checkedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
            )
        }
        val uncheckedBackgroundColors: List<Color>
        val disabledUncheckedBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            uncheckedBackgroundColors = listOf(
                uncheckedStartBackgroundColor,
                uncheckedEndBackgroundColor
            )
            disabledUncheckedBackgroundColors = listOf(
                uncheckedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
                uncheckedEndBackgroundColor.copy(alpha = ContentAlpha.disabled)
            )
        } else {
            uncheckedBackgroundColors = listOf(
                uncheckedEndBackgroundColor,
                uncheckedStartBackgroundColor
            )
            disabledUncheckedBackgroundColors = listOf(
                uncheckedEndBackgroundColor.copy(alpha = ContentAlpha.disabled),
                uncheckedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
            )
        }

        return DefaultToggleChipColors(
            checkedBackgroundPainter = BrushPainter(Brush.linearGradient(checkedBackgroundColors)),
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconTintColor = checkedToggleIconTintColor,
            checkedSplitBackgroundOverlay = splitBackgroundOverlayColor,
            uncheckedBackgroundPainter = BrushPainter(
                Brush.linearGradient(uncheckedBackgroundColors)
            ),
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconTintColor = uncheckedToggleIconTintColor,
            uncheckedSplitBackgroundOverlay = splitBackgroundOverlayColor,
            disabledCheckedBackgroundPainter = BrushPainter(
                Brush.linearGradient(disabledCheckedBackgroundColors)
            ),
            disabledCheckedContentColor = checkedContentColor.copy(alpha = ContentAlpha.disabled),
            disabledCheckedSecondaryContentColor = checkedSecondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledCheckedIconTintColor = checkedToggleIconTintColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledCheckedSplitBackgroundOverlay = splitBackgroundOverlayColor,
            disabledUncheckedBackgroundPainter = BrushPainter(
                Brush.linearGradient(disabledUncheckedBackgroundColors)
            ),
            disabledUncheckedContentColor = uncheckedContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledUncheckedSecondaryContentColor = uncheckedSecondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledUncheckedIconTintColor = uncheckedToggleIconTintColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledUncheckedSplitBackgroundOverlay = splitBackgroundOverlayColor,
        )
    }

    private val ChipHorizontalPadding = 14.dp
    private val ChipVerticalPadding = 6.dp

    /**
     * The default content padding used by [ToggleChip] and [SplitToggleChip]
     */
    public val ContentPadding: PaddingValues = PaddingValues(
        start = ChipHorizontalPadding,
        top = ChipVerticalPadding,
        end = ChipHorizontalPadding,
        bottom = ChipVerticalPadding
    )

    /**
     * The default height applied for the [Chip].
     * Note that you can override it by applying Modifier.heightIn directly on [Chip].
     */
    internal val Height = 52.dp

    /**
     * The default size of the icon when used inside a [Chip].
     */
    public val IconSize: Dp = 24.dp

    /**
     * The default size of the spacing between an icon and a text when they are used inside a
     * [ToggleChip].
     */
    internal val IconSpacing = 8.dp

    /**
     * The default size of the spacing between a toggle icon and text when they are used
     * inside a [ToggleChip].
     */
    internal val ToggleIconSpacing = 4.dp
}

/**
 * Default [ToggleChipColors] implementation.
 */
@Immutable
private class DefaultToggleChipColors(
    private val checkedBackgroundPainter: Painter,
    private val checkedContentColor: Color,
    private val checkedSecondaryContentColor: Color,
    private val checkedIconTintColor: Color,
    private val checkedSplitBackgroundOverlay: Color,
    private val disabledCheckedBackgroundPainter: Painter,
    private val disabledCheckedContentColor: Color,
    private val disabledCheckedSecondaryContentColor: Color,
    private val disabledCheckedIconTintColor: Color,
    private val disabledCheckedSplitBackgroundOverlay: Color,
    private val uncheckedBackgroundPainter: Painter,
    private val uncheckedContentColor: Color,
    private val uncheckedSecondaryContentColor: Color,
    private val uncheckedIconTintColor: Color,
    private val uncheckedSplitBackgroundOverlay: Color,
    private val disabledUncheckedBackgroundPainter: Painter,
    private val disabledUncheckedContentColor: Color,
    private val disabledUncheckedSecondaryContentColor: Color,
    private val disabledUncheckedIconTintColor: Color,
    private val disabledUncheckedSplitBackgroundOverlay: Color,
) : ToggleChipColors {

    @Composable
    override fun background(enabled: Boolean, checked: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedBackgroundPainter else uncheckedBackgroundPainter
            } else {
                if (checked) disabledCheckedBackgroundPainter else
                    disabledUncheckedBackgroundPainter
            }
        )
    }

    @Composable
    override fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedContentColor else uncheckedContentColor
            } else {
                if (checked) disabledCheckedContentColor else disabledUncheckedContentColor
            }
        )
    }

    @Composable
    override fun secondaryContentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedSecondaryContentColor else uncheckedSecondaryContentColor
            } else {
                if (checked) disabledCheckedSecondaryContentColor else
                    disabledUncheckedSecondaryContentColor
            }
        )
    }

    @Composable
    override fun toggleIconTintColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedIconTintColor else uncheckedIconTintColor
            } else {
                if (checked) disabledCheckedIconTintColor else disabledUncheckedIconTintColor
            }
        )
    }

    @Composable
    override fun splitBackgroundOverlay(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedSplitBackgroundOverlay else uncheckedSplitBackgroundOverlay
            } else {
                if (checked) disabledCheckedSplitBackgroundOverlay else
                    disabledUncheckedSplitBackgroundOverlay
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultToggleChipColors

        if (checkedBackgroundPainter != other.checkedBackgroundPainter) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedIconTintColor != other.checkedIconTintColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedSplitBackgroundOverlay != other.checkedSplitBackgroundOverlay) return false
        if (uncheckedBackgroundPainter != other.uncheckedBackgroundPainter) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedIconTintColor != other.uncheckedIconTintColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedSplitBackgroundOverlay != other.uncheckedSplitBackgroundOverlay) return false
        if (disabledCheckedBackgroundPainter != other.disabledCheckedBackgroundPainter) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedIconTintColor != other.disabledCheckedIconTintColor) return false
        if (disabledCheckedSecondaryContentColor !=
            other.disabledCheckedSecondaryContentColor
        ) return false
        if (disabledCheckedSplitBackgroundOverlay !=
            other.disabledCheckedSplitBackgroundOverlay
        ) return false
        if (disabledUncheckedBackgroundPainter !=
            other.disabledUncheckedBackgroundPainter
        ) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedIconTintColor != other.disabledUncheckedIconTintColor) return false
        if (disabledUncheckedSecondaryContentColor !=
            other.disabledUncheckedSecondaryContentColor
        ) return false
        if (disabledUncheckedSplitBackgroundOverlay !=
            other.disabledUncheckedSplitBackgroundOverlay
        ) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedBackgroundPainter.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconTintColor.hashCode()
        result = 31 * result + checkedSplitBackgroundOverlay.hashCode()
        result = 31 * result + uncheckedBackgroundPainter.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconTintColor.hashCode()
        result = 31 * result + uncheckedSplitBackgroundOverlay.hashCode()
        result = 31 * result + disabledCheckedBackgroundPainter.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconTintColor.hashCode()
        result = 31 * result + disabledCheckedSplitBackgroundOverlay.hashCode()
        result = 31 * result + disabledUncheckedBackgroundPainter.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconTintColor.hashCode()
        result = 31 * result + disabledUncheckedSplitBackgroundOverlay.hashCode()
        return result
    }
}
