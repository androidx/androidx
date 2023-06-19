/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.DisabledLabelTextColor
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.DisabledLabelTextOpacity
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.DisabledOutlineOpacity
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.OutlineColor
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.SelectedContainerColor
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.SelectedLabelTextColor
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens.UnselectedLabelTextColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * <a href="https://m3.material.io/components/segmented-buttons/overview" class="external" target="_blank">Material Segmented Button</a>.
 * Segmented buttons help people select options, switch views, or sort elements.
 *
 * A default Segmented Button. Also known as Outlined Segmented Button.
 *
 * This should typically be used inside of a [SegmentedButtonRow], see the corresponding
 * documentation for example usage.
 *
 * @param onCheckedChange callback to be invoked when the button is clicked.
 * therefore the change of checked state in requested.
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param checked whether this button is checked or not
 * @param shape the shape for this button
 * @param border the border for this button, see [SegmentedButtonColors]
 * @param colors [SegmentedButtonColors] that will be used to resolve the colors used for this
 * Button in different states
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param content content to be rendered inside this button
 *
 * @sample androidx.compose.material3.samples.SegmentedButtonSingleSelectSample
 */
@Composable
@ExperimentalMaterial3Api
fun SegmentedButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: SegmentedButtonBorder = SegmentedButtonDefaults.Border,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    var interactionCount: Int by remember { mutableIntStateOf(0) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press,
                is FocusInteraction.Focus -> {
                    interactionCount++
                }

                is PressInteraction.Release,
                is FocusInteraction.Unfocus,
                is PressInteraction.Cancel -> {
                    interactionCount--
                }
            }
        }
    }

    val containerColor = colors.containerColor(enabled, checked)
    val contentColor = colors.contentColor(enabled, checked)
    val checkedState by rememberUpdatedState(checked)

    Surface(
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val zIndex = interactionCount + if (checkedState) CheckedZIndexFactor else 0f
                    placeable.place(0, 0, zIndex)
                }
            }
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            ),
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border.borderStroke(enabled, checked, colors),
        interactionSource = interactionSource
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            Row(
                Modifier.padding(ButtonDefaults.TextButtonContentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 *
 * <a href="https://m3.material.io/components/segmented-buttons/overview" class="external" target="_blank">Material Segmented Button</a>.
 *
 * A Layout to correctly position and size [SegmentedButton]s in a Row.
 * It handles overlapping items so that strokes of the item are correctly on top of each other.
 *
 * @param modifier the [Modifier] to be applied to this row
 * @param space the dimension of the overlap between buttons. Should be equal to the stroke width
 *  used on the items.
 * @param content the content of this Segmented Button Row, typically a sequence of
 * [SegmentedButtonRow]
 *
 * @sample androidx.compose.material3.samples.SegmentedButtonSingleSelectSample
 */
@Composable
@ExperimentalMaterial3Api
fun SegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.Border.width,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier
            .height(OutlinedSegmentedButtonTokens.ContainerHeight)
            .width(IntrinsicSize.Min)
            .selectableGroup(),
        content = content
    ) { measurables, constraints ->

        val width = measurables.fold(0) { curr, max ->
            maxOf(curr, max.maxIntrinsicWidth(constraints.maxHeight))
        }

        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = width))
        }

        layout(placeables.size * width, constraints.maxHeight) {
            val itemCount = placeables.size
            val startOffset = (itemCount - 1) * space.roundToPx() / 2
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(startOffset + index * (width - space.roundToPx()), 0)
            }
        }
    }
}

/* Contains defaults to be used with [SegmentedButtonRow] and [SegmentedButton] */
@ExperimentalMaterial3Api
object SegmentedButtonDefaults {
    /**
     * Creates a [SegmentedButtonColors] that represents the different colors
     * used in a [SegmentedButtonRow] in different states.
     *
     * @param checkedContainerColor the color used for the container when enabled and checked
     * @param checkedContentColor the color used for the content when enabled and checked
     * @param checkedBorderColor the color used for the border when enabled and checked
     * @param uncheckedContainerColor the color used for the container when enabled and unchecked
     * @param uncheckedContentColor the color used for the content when enabled and unchecked
     * @param uncheckedBorderColor the color used for the border when enabled and checked
     * @param disabledCheckedContainerColor the color used for the container
     * when disabled and checked
     * @param disabledCheckedContentColor the color used for the content when disabled and checked
     * @param disabledCheckedBorderColor the color used for the border when disabled and checked
     * @param disabledUncheckedContainerColor the color used for the container
     * when disabled and unchecked
     * @param disabledUncheckedContentColor the color used for the content when disabled and
     * unchecked
     * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
     */
    @Composable
    fun colors(
        checkedContainerColor: Color = SelectedContainerColor.toColor(),
        checkedContentColor: Color = SelectedLabelTextColor.toColor(),
        checkedBorderColor: Color = OutlineColor.toColor(),
        uncheckedContainerColor: Color = MaterialTheme.colorScheme.surface,
        uncheckedContentColor: Color = UnselectedLabelTextColor.toColor(),
        uncheckedBorderColor: Color = checkedBorderColor,
        disabledCheckedContainerColor: Color = checkedContainerColor,
        disabledCheckedContentColor: Color = DisabledLabelTextColor.toColor()
            .copy(alpha = DisabledLabelTextOpacity),
        disabledCheckedBorderColor: Color = OutlineColor.toColor()
            .copy(alpha = DisabledOutlineOpacity),
        disabledUncheckedContainerColor: Color = uncheckedContainerColor,
        disabledUncheckedContentColor: Color = disabledCheckedContentColor,
        disabledUncheckedBorderColor: Color = checkedBorderColor,
    ): SegmentedButtonColors = SegmentedButtonColors(
        checkedContainerColor = checkedContainerColor,
        checkedContentColor = checkedContentColor,
        checkedBorderColor = checkedBorderColor,
        uncheckedContainerColor = uncheckedContainerColor,
        uncheckedContentColor = uncheckedContentColor,
        uncheckedBorderColor = uncheckedBorderColor,
        disabledCheckedContainerColor = disabledCheckedContainerColor,
        disabledCheckedContentColor = disabledCheckedContentColor,
        disabledCheckedBorderColor = disabledCheckedBorderColor,
        disabledUncheckedContainerColor = disabledUncheckedContainerColor,
        disabledUncheckedContentColor = disabledUncheckedContentColor,
        disabledUncheckedBorderColor = disabledUncheckedBorderColor,

        )

    /** The default [BorderStroke] factory used by [SegmentedButton]. */
    val Border = SegmentedButtonBorder(width = OutlinedSegmentedButtonTokens.OutlineWidth)

    /** The default [Shape] for [SegmentedButton] insdie [SegmentedButtonRow]. */
    val Shape: CornerBasedShape
        @Composable
        @ReadOnlyComposable
        get() = OutlinedSegmentedButtonTokens.Shape.toShape() as CornerBasedShape

    /**
     * A shape constructor that the button in [position] should have when there are [count] buttons
     *
     * @param position the position for this button in the row
     * @param count the count of buttons in this row
     * @param shape the [CornerBasedShape] the base shape that should be used in buttons that are
     * not in the start or the end.
     */
    @Composable
    @ReadOnlyComposable
    fun shape(position: Int, count: Int, shape: CornerBasedShape = this.Shape): Shape {
        return when (position) {
            0 -> shape.start()
            count - 1 -> shape.end()
            else -> RectangleShape
        }
    }

    /**
     * Icon size to use for icons inside [SegmentedButtonRow] item
     */
    val IconSize = OutlinedSegmentedButtonTokens.IconSize
}

/**
 * Class to create border stroke for segmented button, see [SegmentedButtonColors], for
 * customization of colors.
 */
@ExperimentalMaterial3Api
@Immutable
class SegmentedButtonBorder(val width: Dp) {

    /** The default [BorderStroke] used by [SegmentedButton]. */
    fun borderStroke(
        enabled: Boolean,
        checked: Boolean,
        colors: SegmentedButtonColors
    ): BorderStroke = BorderStroke(
        width = width,
        color = colors.borderColor(enabled, checked)
    )
}

/**
 * The different colors used in parts of the [SegmentedButton] in different states
 *
 * @constructor create an instance with arbitrary colors, see [SegmentedButtonDefaults] for a
 * factory method using the default material3 spec
 *
 * @param checkedContainerColor the color used for the container when enabled and checked
 * @param checkedContentColor the color used for the content when enabled and checked
 * @param checkedBorderColor the color used for the border when enabled and checked
 * @param uncheckedContainerColor the color used for the container when enabled and unchecked
 * @param uncheckedContentColor the color used for the content when enabled and unchecked
 * @param uncheckedBorderColor the color used for the border when enabled and checked
 * @param disabledCheckedContainerColor the color used for the container when disabled and checked
 * @param disabledCheckedContentColor the color used for the content when disabled and checked
 * @param disabledCheckedBorderColor the color used for the border when disabled and checked
 * @param disabledUncheckedContainerColor the color used for the container
 * when disabled and unchecked
 * @param disabledUncheckedContentColor the color used for the content when disabled and unchecked
 * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
 */
@Immutable
@ExperimentalMaterial3Api
class SegmentedButtonColors constructor(
    // enabled & checked
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedBorderColor: Color,
    // enabled & unchecked
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedBorderColor: Color,
    // disable & checked
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedBorderColor: Color,
    // disable & unchecked
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedBorderColor: Color,

    ) {
    /**
     * Represents the color used for the SegmentedButton's border,
     * depending on [enabled] and [checked].
     *
     * @param enabled whether the [SegmentedButtonRow] is enabled or not
     * @param checked whether the [SegmentedButtonRow] item is checked or not
     */
    internal fun borderColor(enabled: Boolean, checked: Boolean): Color {
        return when {
            enabled && checked -> checkedBorderColor
            enabled && !checked -> uncheckedBorderColor
            !enabled && checked -> disabledCheckedContentColor
            else -> disabledUncheckedContentColor
        }
    }

    /**
     * Represents the content color passed to the items
     *
     * @param enabled whether the [SegmentedButtonRow] is enabled or not
     * @param checked whether the [SegmentedButtonRow] item is checked or not
     */
    internal fun contentColor(enabled: Boolean, checked: Boolean): Color {
        return when {
            enabled && checked -> checkedContentColor
            enabled && !checked -> uncheckedContentColor
            !enabled && checked -> disabledCheckedContentColor
            else -> disabledUncheckedContentColor
        }
    }

    /**
     * Represents the container color passed to the items
     *
     * @param enabled whether the [SegmentedButtonRow] is enabled or not
     * @param checked whether the [SegmentedButtonRow] item is checked or not
     */
    internal fun containerColor(enabled: Boolean, checked: Boolean): Color {
        return when {
            enabled && checked -> checkedContainerColor
            enabled && !checked -> uncheckedContainerColor
            !enabled && checked -> disabledCheckedContainerColor
            else -> disabledUncheckedContainerColor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentedButtonColors

        if (checkedBorderColor != other.checkedBorderColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedContainerColor != other.checkedContainerColor) return false
        if (uncheckedBorderColor != other.uncheckedBorderColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (disabledCheckedBorderColor != other.disabledCheckedBorderColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledUncheckedBorderColor != other.disabledUncheckedBorderColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedBorderColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedContainerColor.hashCode()
        result = 31 * result + uncheckedBorderColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedBorderColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedBorderColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        return result
    }
}

private const val CheckedZIndexFactor = 5f