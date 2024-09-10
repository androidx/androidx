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

package androidx.wear.compose.material3

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.wear.compose.materialcore.screenHeightDp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Scope for the children of a [ButtonGroup] */
class ButtonGroupScope {
    internal val items = mutableListOf<ButtonGroupItem>()

    /**
     * Adds an item to a [ButtonGroup]
     *
     * @param interactionSource the interactionSource used to detect press/release events. Should be
     *   the same one used in the content in this slot, which is typically a [Button].
     * @param minWidth the minimum width this item can be. This will only be used if distributing
     *   the available space results on a item falling below it's minimum width.
     * @param weight the main way of distributing available space. In most cases, items will have a
     *   width assigned proportional to their width (and available space). The exception is if that
     *   will make some item(s) width fall below it's minWidth.
     * @param content the content to use for this item. Usually, this will be one of the [Button]
     *   variants.
     */
    fun buttonGroupItem(
        interactionSource: InteractionSource,
        minWidth: Dp = minimumInteractiveComponentSize,
        weight: Float = 1f,
        content: @Composable () -> Unit
    ) = items.add(ButtonGroupItem(interactionSource, minWidth, weight, content))
}

/**
 * Layout component to implement an expressive group of buttons, that react to touch by growing the
 * touched button, (while the neighbor(s) shrink to accommodate and keep the group width constant).
 *
 * Example of a [ButtonGroup]:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonGroupSample
 * @param modifier Modifier to be applied to the button group
 * @param spacing the amount of spacing between buttons
 * @param expansionWidth how much buttons grow when pressed
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param verticalAlignment the vertical alignment of the button group's children.
 * @param content the content and properties of each button. The Ux guidance is to use no more than
 *   3 buttons within a ButtonGroup.
 */
@Composable
fun ButtonGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = ButtonGroupDefaults.Spacing,
    expansionWidth: Dp = ButtonGroupDefaults.ExpansionWidth,
    contentPadding: PaddingValues = ButtonGroupDefaults.fullWidthPaddings(),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: ButtonGroupScope.() -> Unit
) {
    val actualContent = ButtonGroupScope().apply(block = content)

    val pressedStates = remember { Array(actualContent.items.size) { mutableStateOf(false) } }

    val animatedSizes = remember { Array(actualContent.items.size) { Animatable(0f) } }

    val expandAmountPx = with(LocalDensity.current) { expansionWidth.toPx() }

    val downAnimSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>().faster(100f)
    val upAnimSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()

    LaunchedEffect(actualContent.items) {
        launch {
            val pressInteractions =
                Array(actualContent.items.size) { mutableListOf<PressInteraction.Press>() }

            merge(
                    flows =
                        Array(actualContent.items.size) { index ->
                            // Annotate each flow with the item index it is related to.
                            actualContent.items[index].interactionSource.interactions.map {
                                interaction ->
                                index to interaction
                            }
                        }
                )
                .collect { (index, interaction) ->
                    when (interaction) {
                        is PressInteraction.Press -> pressInteractions[index].add(interaction)
                        is PressInteraction.Release ->
                            pressInteractions[index].remove(interaction.press)
                        is PressInteraction.Cancel ->
                            pressInteractions[index].remove(interaction.press)
                    }
                    pressedStates[index].value = pressInteractions[index].isNotEmpty()
                }
        }

        actualContent.items.indices.forEach { index ->
            launch {
                snapshotFlow { pressedStates[index].value }
                    .collectLatest { value ->
                        if (value) {
                            animatedSizes[index].animateTo(expandAmountPx, downAnimSpec)
                        } else {
                            animatedSizes[index].animateTo(0f, upAnimSpec)
                        }
                    }
            }
        }
    }

    Layout(
        modifier = modifier.padding(contentPadding),
        content = { actualContent.items.fastForEach { it.content() } }
    ) { measurables, constraints ->
        require(constraints.hasBoundedWidth) { "ButtonGroup width cannot be unbounded." }
        require(measurables.size == actualContent.items.size) {
            "ButtonGroup's items have to produce exactly one composable each."
        }

        val width = constraints.maxWidth
        val spacingPx = spacing.roundToPx()

        // TODO: Cache this if it proves to be computationally intensive.
        val widths =
            computeWidths(
                actualContent.items.fastMap { it.minWidth.toPx() to it.weight },
                spacingPx,
                width
            )

        // Add animated grow/shrink
        if (actualContent.items.size > 1) {
            animatedSizes.forEachIndexed { index, value ->
                // Grow the pressed item
                widths[index] += value.value

                // Shrink the neighbors.
                if (index == 0) {
                    // index == 0, and we know there are at least 2 items.
                    widths[1] -= value.value
                } else if (index < animatedSizes.lastIndex) {
                    // index is in the middle.
                    widths[index - 1] -= value.value / 2
                    widths[index + 1] -= value.value / 2
                } else {
                    // index == animatedSizes.lastIndex, and we know there are at least 2 items.
                    widths[index - 1] -= value.value
                }
            }
        }

        // We know the width we want buttons to be, we can call measure now and pass that as a
        // constraint.
        val finalSizes = IntArray(widths.size) { widths[it].roundToInt() }

        val placeables =
            measurables.fastMapIndexed { ix, placeable ->
                placeable.measure(
                    constraints.copy(minWidth = finalSizes[ix], maxWidth = finalSizes[ix])
                )
            }

        val height =
            (placeables.fastMap { it.height }.max()).coerceIn(
                constraints.minHeight,
                constraints.maxHeight
            )

        layout(width, height) {
            var x = 0
            placeables.fastForEachIndexed { index, placeable ->
                placeable.place(x, verticalAlignment.align(placeable.height, height))
                x += finalSizes[index] + spacingPx
                // TODO: rounding finalSizes & spacing means we may have a few extra pixels wasted
                //  or take more room than available.
            }
        }
    }
}

/** Contains the default values used by [ButtonGroup] */
object ButtonGroupDefaults {
    /**
     * Return the recommended padding to use as the contentPadding of a [ButtonGroup], when it takes
     * the full width of the screen.
     */
    @Composable
    fun fullWidthPaddings(): PaddingValues {
        val screenHeight = screenHeightDp().dp
        return PaddingValues(
            horizontal = screenHeight * FullWidthHorizontalPaddingPercentage / 100,
            vertical = 0.dp
        )
    }

    /** How much buttons grow (and neighbors shrink) when pressed. */
    val ExpansionWidth: Dp = 24.dp

    /** Spacing between buttons. */
    val Spacing: Dp = 4.dp

    /** Padding at each side of the [ButtonGroup], as a percentage of the available space. */
    private const val FullWidthHorizontalPaddingPercentage: Float = 5.2f
}

/**
 * Data class to configure one item in a [ButtonGroup]
 *
 * @param interactionSource the interactionSource used to detect press/release events. Should be the
 *   same one used in the content in this slot, which is typically a [Button].
 * @param minWidth the minimum width this item can be. This will only be used if distributing the
 *   available space results on a item falling below it's minimum width.
 * @param weight the main way of distributing available space. In most cases, items will have a
 *   width assigned proportional to their width (and available space). The exception is if that will
 *   make some item(s) width fall below it's minWidth.
 * @param content the content to use for this item. Usually, this will be one of the [Button]
 *   variants.
 */
internal data class ButtonGroupItem(
    val interactionSource: InteractionSource,
    val minWidth: Dp = minimumInteractiveComponentSize,
    val weight: Float = 1f,
    val content: @Composable () -> Unit
)

// TODO: Does it make sense to unify these 2 classes?
private data class ComputeHelper(
    var minWidth: Float,
    val weight: Float,
    val originalIndex: Int,
    var width: Float
)

/**
 * Computes the base widths of the items "at rest", i.e. when there is no user interaction.
 *
 * @param items the minimum width and weight of the items
 * @param spacingPx the spacing between items, in pixels
 * @param availableWidth the total available space.
 */
@VisibleForTesting
internal fun computeWidths(
    items: List<Pair<Float, Float>>,
    spacingPx: Int,
    availableWidth: Int
): FloatArray {
    val helper =
        Array(items.size) { index ->
            val pair = items[index]
            ComputeHelper(pair.first, pair.second, index, pair.first)
        }
    val totalSpacing = spacingPx * (helper.size - 1)
    val minSpaceNeeded = totalSpacing + helper.map { it.width }.sum()

    val totalWeight = helper.map { it.weight }.sum()

    val extraSpace = availableWidth - minSpaceNeeded
    // TODO: should we really handle the totalWeight <= 0 case? If so, we need to leave items at
    //  their minWidth and center the whole thing?
    if (totalWeight > 0) {
        for (ix in helper.indices) {
            // Initial distribution ignores minWidth.
            helper[ix].width = (availableWidth - totalSpacing) * helper[ix].weight / totalWeight
        }
    }

    // If we don't have extra space, ensure at least all sizes are >= 0
    if (extraSpace < 0) {
        helper.forEach { it.minWidth = 0f }
    }

    // Sort them. We will have:
    // * Items with weight == 0 and less width required (usually 0)
    // * Items with weight > 0 and less width required
    // * Items with weight > 0, sorted for the order in which they may get below their minimum width
    //   as we take away space.
    // * Items with weight == 0 and enough width (This can only happen if totalWeight == 0)
    helper.sortBy {
        if (it.weight == 0f) {
            if (it.width < it.minWidth) Float.MIN_VALUE else Float.MAX_VALUE
        } else (it.width - it.minWidth) / it.weight
    }

    // ** Redistribute width to match constraints
    // The total weight of the items we haven't processed yet
    var remainingWeight = totalWeight
    // How much width we added to the processed items and we need to take from the remaining ones.
    var owedWidth = 0f
    for (ix in helper.indices) {
        if (remainingWeight == 0f) break

        val item = helper[ix]
        if (item.width < item.minWidth) {
            // Item is too small, make it bigger.
            owedWidth += item.minWidth - item.width
            item.width = item.minWidth
        } else {
            // We have width to give, just need to be careful not to go below minWidth.
            val needToGive = owedWidth * item.weight / remainingWeight
            val canGive = needToGive.coerceAtMost(item.width - item.minWidth)
            item.width -= canGive
            owedWidth -= canGive
        }
        remainingWeight -= item.weight
    }
    // Check that things went as expected.
    require(abs(owedWidth) < 1e-4f || abs(remainingWeight) < 1e-4f) {
        "There was a problem computing the width of the button group's items, " +
            "owedWidth = $owedWidth, remainingWeight = $remainingWeight"
    }

    // Reconstruct the original order using the 'originalIndex'
    val ret = FloatArray(helper.size) { 0f }
    helper.forEach { ret[it.originalIndex] = it.width }
    return ret
}
