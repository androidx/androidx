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
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Example of a [ToggleChip] with an icon, label and secondary label (defaults to switch toggle):
 * @sample androidx.wear.compose.material.samples.ToggleChipWithIcon
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked/selected status is
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned.
 * @param modifier Modifier to be applied to the chip
 * @param toggleIcon A slot for providing the chip's toggle icon(s). The contents are expected to be
 * a horizontally and vertically centre aligned icon of size [ToggleChipDefaults.IconSize]. Three
 * types of toggle icon are supported and can be obtained from
 * [ToggleChipDefaults.SwitchIcon], [ToggleChipDefaults.RadioIcon] and
 * [ToggleChipDefaults.CheckboxIcon]
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
    modifier: Modifier = Modifier,
    toggleIcon: @Composable () -> Unit = { ToggleChipDefaults.CheckboxIcon(checked = checked) },
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
 * Example of a [SplitToggleChip] with a label and the toggle icon changed to checkbox:
 * @sample androidx.wear.compose.material.samples.SplitToggleChipWithCheckbox
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked/selected status is
 * changed.
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned.
 * @param onClick Click listener called when the user clicks the main body of the chip, the area
 * behind the labels.
 * @param modifier Modifier to be applied to the chip
 * @param toggleIcon A slot for providing the chip's toggle icon(s). The contents are expected to be
 * a horizontally and vertically centre aligned icon of size [ToggleChipDefaults.IconSize]. Three
 * types of toggle icon are supported and can be obtained from
 * [ToggleChipDefaults.SwitchIcon], [ToggleChipDefaults.RadioIcon] and
 * [ToggleChipDefaults.CheckboxIcon]
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    toggleIcon: @Composable () -> Unit = { ToggleChipDefaults.CheckboxIcon(checked = checked) },
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
 * Contains the default values used by [ToggleChip]s
 */
public object ToggleChipDefaults {

    /**
     * Creates a [ToggleChipColors] for use in a ToggleChip or SplitToggleChip.
     * [ToggleChip]s are expected to have a linear gradient background when
     * checked/selected, similar to a [ChipDefaults.gradientBackgroundChipColors] and a solid
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
        checkedStartBackgroundColor: Color = MaterialTheme.colors.surface.copy(alpha = 0.75f),
        checkedEndBackgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.325f),
        checkedContentColor: Color = MaterialTheme.colors.onSurface,
        checkedSecondaryContentColor: Color = MaterialTheme.colors.onSurfaceVariant,
        checkedToggleIconTintColor: Color = MaterialTheme.colors.secondary,
        uncheckedStartBackgroundColor: Color = MaterialTheme.colors.surface,
        uncheckedEndBackgroundColor: Color = MaterialTheme.colors.surface,
        uncheckedContentColor: Color = contentColorFor(checkedEndBackgroundColor),
        uncheckedSecondaryContentColor: Color = uncheckedContentColor,
        uncheckedToggleIconTintColor: Color = uncheckedContentColor,
        splitBackgroundOverlayColor: Color = Color.White.copy(alpha = 0.05f),
        gradientDirection: LayoutDirection = LocalLayoutDirection.current
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
     * Creates switch style toggle [Icon]s for use in the toggleIcon slot of a [ToggleChip].
     * Depending on [checked] will return either an 'on' (checked) or 'off' (unchecked) switch icon.
     *
     * @param checked whether the [ToggleChip] or [SplitToggleChip] is currently 'on' (checked/true)
     * or 'off' (unchecked/false)
     */
    @Composable
    public fun SwitchIcon(
        checked: Boolean,
    ) {
        if (checked) {
            Icon(
                imageVector = SwitchOn,
                contentDescription = "Switch selector",
                modifier = Modifier.size(24.dp),
            )
        } else {
            Icon(
                imageVector = SwitchOff,
                contentDescription = "Switch selector",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colors.onSurface.copy(0.6f)
            )
        }
    }

    /**
     * Creates a radio button style toggle [Icon]s for use in the toggleIcon slot of a [ToggleChip].
     * Depending on [checked] will return either an 'on' (checked) or 'off' (unchecked) radio button
     * icon.
     *
     * @param checked whether the [ToggleChip] or [SplitToggleChip] is currently 'on' (checked/true)
     * or 'off' (unchecked/false)
     */
    @Composable
    public fun RadioIcon(checked: Boolean) {
        Icon(
            imageVector = if (checked) RadioOn else RadioOff,
            contentDescription = "Radio selector",
            modifier = Modifier.size(24.dp)
        )
    }

    /**
     * Creates a checkbox style toggle [Icon]s for use in the toggleIcon slot of a [ToggleChip].
     * Depending on [checked] will return either an 'on' (ticked/checked) or 'off'
     * (unticked/unchecked) checkbox icon.
     *
     * @param checked whether the [ToggleChip] or [SplitToggleChip] is currently 'on' (checked/true)
     * or 'off' (unchecked/false)
     */
    @Composable
    public fun CheckboxIcon(checked: Boolean) {
        Icon(
            imageVector = if (checked) CheckboxOn else CheckboxOff,
            contentDescription = "Checkbox selector",
            modifier = Modifier.size(24.dp)
        )
    }

    /**
     * The default height applied for the [ToggleChip].
     * Note that you can override it by applying Modifier.heightIn directly on [ToggleChip].
     */
    internal val Height = 52.dp

    /**
     * The default size of app or toggle icons when used inside a [ToggleChip].
     */
    public val IconSize: Dp = 24.dp

    /**
     * The default size of the spacing between an icon and a text when they are used inside a
     * [ToggleChip].
     */
    internal val IconSpacing = 6.dp

    /**
     * The default size of the spacing between a toggle icon and text when they are used
     * inside a [ToggleChip].
     */
    internal val ToggleIconSpacing = 4.dp

    private val SwitchOn: ImageVector
        get() {
            if (_switchOn != null) {
                return _switchOn!!
            }
            _switchOn = materialIcon(name = "SwitchOn") {
                materialPath(fillAlpha = 0.38f, strokeAlpha = 0.38f) {
                    moveTo(5.0f, 7.0f)
                    lineTo(19.0f, 7.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 24.0f, 12.0f)
                    lineTo(24.0f, 12.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 19.0f, 17.0f)
                    lineTo(5.0f, 17.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 0.0f, 12.0f)
                    lineTo(0.0f, 12.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 5.0f, 7.0f)
                    close()
                }
                materialPath(pathFillType = PathFillType.EvenOdd) {
                    moveTo(17.0f, 19.0f)
                    curveTo(20.866f, 19.0f, 24.0f, 15.866f, 24.0f, 12.0f)
                    curveTo(24.0f, 8.134f, 20.866f, 5.0f, 17.0f, 5.0f)
                    curveTo(13.134f, 5.0f, 10.0f, 8.134f, 10.0f, 12.0f)
                    curveTo(10.0f, 15.866f, 13.134f, 19.0f, 17.0f, 19.0f)
                    close()
                }
            }
            return _switchOn!!
        }

    private var _switchOn: ImageVector? = null

    private val SwitchOff: ImageVector
        get() {
            if (_switchOff != null) {
                return _switchOff!!
            }
            _switchOff = materialIcon(name = "SwitchOff") {
                materialPath(fillAlpha = 0.38f, strokeAlpha = 0.38f) {
                    moveTo(5.0f, 7.0f)
                    lineTo(19.0f, 7.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 24.0f, 12.0f)
                    lineTo(24.0f, 12.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 19.0f, 17.0f)
                    lineTo(5.0f, 17.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 0.0f, 12.0f)
                    lineTo(0.0f, 12.0f)
                    arcTo(5.0f, 5.0f, 0.0f, false, true, 5.0f, 7.0f)
                    close()
                }
                materialPath(pathFillType = PathFillType.EvenOdd) {
                    moveTo(7.0f, 19.0f)
                    curveTo(10.866f, 19.0f, 14.0f, 15.866f, 14.0f, 12.0f)
                    curveTo(14.0f, 8.134f, 10.866f, 5.0f, 7.0f, 5.0f)
                    curveTo(3.134f, 5.0f, 0.0f, 8.134f, 0.0f, 12.0f)
                    curveTo(0.0f, 15.866f, 3.134f, 19.0f, 7.0f, 19.0f)
                    close()
                }
            }
            return _switchOff!!
        }

    private var _switchOff: ImageVector? = null

    public val RadioOn: ImageVector
        get() {
            if (_radioOn != null) {
                return _radioOn!!
            }
            _radioOn = materialIcon(name = "RadioOn") {
                materialPath {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    curveTo(2.0f, 17.52f, 6.48f, 22.0f, 12.0f, 22.0f)
                    curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                    curveTo(22.0f, 6.48f, 17.52f, 2.0f, 12.0f, 2.0f)
                    close()
                    moveTo(12.0f, 20.0f)
                    curveTo(7.58f, 20.0f, 4.0f, 16.42f, 4.0f, 12.0f)
                    curveTo(4.0f, 7.58f, 7.58f, 4.0f, 12.0f, 4.0f)
                    curveTo(16.42f, 4.0f, 20.0f, 7.58f, 20.0f, 12.0f)
                    curveTo(20.0f, 16.42f, 16.42f, 20.0f, 12.0f, 20.0f)
                    close()
                }
                materialPath {
                    moveTo(12.0f, 12.0f)
                    moveToRelative(-5.0f, 0.0f)
                    arcToRelative(5.0f, 5.0f, 0.0f, true, true, 10.0f, 0.0f)
                    arcToRelative(5.0f, 5.0f, 0.0f, true, true, -10.0f, 0.0f)
                }
            }
            return _radioOn!!
        }

    private var _radioOn: ImageVector? = null

    public val RadioOff: ImageVector
        get() {
            if (_radioOff != null) {
                return _radioOff!!
            }
            _radioOff = materialIcon(name = "RadioOff") {
                materialPath {
                    moveTo(12.0f, 2.0f)
                    curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                    curveTo(2.0f, 17.52f, 6.48f, 22.0f, 12.0f, 22.0f)
                    curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                    curveTo(22.0f, 6.48f, 17.52f, 2.0f, 12.0f, 2.0f)
                    close()
                    moveTo(12.0f, 20.0f)
                    curveTo(7.58f, 20.0f, 4.0f, 16.42f, 4.0f, 12.0f)
                    curveTo(4.0f, 7.58f, 7.58f, 4.0f, 12.0f, 4.0f)
                    curveTo(16.42f, 4.0f, 20.0f, 7.58f, 20.0f, 12.0f)
                    curveTo(20.0f, 16.42f, 16.42f, 20.0f, 12.0f, 20.0f)
                    close()
                }
            }
            return _radioOff!!
        }

    private var _radioOff: ImageVector? = null

    public val CheckboxOn: ImageVector
        get() {
            if (_checkboxOn != null) {
                return _checkboxOn!!
            }
            _checkboxOn = materialIcon(name = "CheckboxOn") {
                materialPath {
                    moveTo(19.0f, 3.0f)
                    horizontalLineTo(5.0f)
                    curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                    verticalLineTo(19.0f)
                    curveTo(3.0f, 20.1f, 3.9f, 21.0f, 5.0f, 21.0f)
                    horizontalLineTo(19.0f)
                    curveTo(20.1f, 21.0f, 21.0f, 20.1f, 21.0f, 19.0f)
                    verticalLineTo(5.0f)
                    curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
                    close()
                    moveTo(19.0f, 19.0f)
                    horizontalLineTo(5.0f)
                    verticalLineTo(5.0f)
                    horizontalLineTo(19.0f)
                    verticalLineTo(19.0f)
                    close()
                    moveTo(18.0f, 9.0f)
                    lineTo(16.6f, 7.6f)
                    lineTo(13.3f, 10.9f)
                    lineTo(10.0f, 14.2f)
                    lineTo(7.4f, 11.6f)
                    lineTo(6.0f, 13.0f)
                    lineTo(10.0f, 17.0f)
                    lineTo(18.0f, 9.0f)
                    close()
                }
            }
            return _checkboxOn!!
        }

    private var _checkboxOn: ImageVector? = null

    private val CheckboxOff: ImageVector
        get() {
            if (_checkboxOff != null) {
                return _checkboxOff!!
            }
            _checkboxOff = materialIcon(name = "CheckboxOff") {
                materialPath {
                    moveTo(19.0f, 5.0f)
                    verticalLineTo(19.0f)
                    horizontalLineTo(5.0f)
                    verticalLineTo(5.0f)
                    horizontalLineTo(19.0f)
                    close()
                    moveTo(19.0f, 3.0f)
                    horizontalLineTo(5.0f)
                    curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                    verticalLineTo(19.0f)
                    curveTo(3.0f, 20.1f, 3.9f, 21.0f, 5.0f, 21.0f)
                    horizontalLineTo(19.0f)
                    curveTo(20.1f, 21.0f, 21.0f, 20.1f, 21.0f, 19.0f)
                    verticalLineTo(5.0f)
                    curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
                    close()
                }
            }
            return _checkboxOff!!
        }

    private var _checkboxOff: ImageVector? = null
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
