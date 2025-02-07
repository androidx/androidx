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

import androidx.annotation.FloatRange
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.wear.compose.materialcore.screenHeightDp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Layout component to implement an expressive group of buttons in a row, that react to touch by
 * growing the touched button, (while the neighbor(s) shrink to accommodate and keep the group width
 * constant).
 *
 * Example of a [ButtonGroup]:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonGroupSample
 *
 * Example of 3 buttons, the middle one bigger [ButtonGroup]:
 *
 * @sample androidx.wear.compose.material3.samples.ButtonGroupThreeButtonsSample
 * @param modifier Modifier to be applied to the button group
 * @param spacing the amount of spacing between buttons
 * @param expansionWidth how much buttons grow when pressed
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param verticalAlignment the vertical alignment of the button group's children.
 * @param content the content and properties of each button. The Ux guidance is to use no more than
 *   3 buttons within a ButtonGroup. Note that this content is on the [ButtonGroupScope], to provide
 *   access to 3 new modifiers to configure the buttons.
 */
@Composable
public fun ButtonGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = ButtonGroupDefaults.Spacing,
    expansionWidth: Dp = ButtonGroupDefaults.ExpansionWidth,
    contentPadding: PaddingValues = ButtonGroupDefaults.fullWidthPaddings(),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable ButtonGroupScope.() -> Unit
) {
    val expandAmountPx = with(LocalDensity.current) { expansionWidth.toPx() }

    val downAnimSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>().faster(100f)
    val upAnimSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()

    val scope = remember {
        object : ButtonGroupScope {
            override fun Modifier.weight(weight: Float): Modifier {
                require(weight >= 0.0) {
                    "invalid weight $weight; must be greater or equal to zero"
                }
                return this.then(ButtonGroupElement(weight = weight))
            }

            override fun Modifier.minWidth(minWidth: Dp): Modifier {
                require(minWidth > 0.dp) { "invalid minWidth $minWidth; must be greater than zero" }
                return this.then(ButtonGroupElement(minWidth = minWidth))
            }

            override fun Modifier.animateWidth(interactionSource: InteractionSource) =
                this.then(
                    EnlargeOnPressElement(
                        interactionSource = interactionSource,
                        downAnimSpec,
                        upAnimSpec
                    )
                )
        }
    }

    Layout(modifier = modifier.padding(contentPadding), content = { scope.content() }) {
        measurables,
        constraints ->
        require(constraints.hasBoundedWidth) { "ButtonGroup width cannot be unbounded." }

        val width = constraints.maxWidth
        val spacingPx = spacing.roundToPx()

        val configs =
            Array(measurables.size) {
                measurables[it].parentData as? ButtonGroupParentData
                    ?: ButtonGroupParentData.DEFAULT
            }

        val animatedSizes = Array(measurables.size) { configs[it].pressedState.value }

        // TODO: Cache this if it proves to be computationally intensive.
        val widths =
            computeWidths(configs.map { it.minWidth.toPx() to it.weight }, spacingPx, width)

        // Add animated grow/shrink
        if (measurables.size > 1) {
            for (index in measurables.indices) {
                val value = animatedSizes[index] * expandAmountPx
                // How much we need to grow the pressed item.
                val growth: Int
                if (index in 1 until measurables.lastIndex) {
                    // index is in the middle. Ensure we keep the size of the middle item with
                    // the same parity, so its content remains in place.
                    growth = (value / 2).roundToInt() * 2
                    widths[index - 1] -= growth / 2
                    widths[index + 1] -= growth / 2
                } else {
                    growth = value.roundToInt()
                    if (index == 0) {
                        // index == 0, and we know there are at least 2 items.
                        widths[1] -= growth
                    } else {
                        // index == measurables.lastIndex, and we know there are at least 2 items.
                        widths[index - 1] -= growth
                    }
                }
                // Grow the pressed item
                widths[index] += growth
            }
        }

        // We know the width we want buttons to be, we can call measure now and pass that as a
        // constraint.
        val placeables =
            measurables.fastMapIndexed { ix, placeable ->
                placeable.measure(constraints.copy(minWidth = widths[ix], maxWidth = widths[ix]))
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
                x += widths[index] + spacingPx
                // TODO: rounding finalSizes & spacing means we may have a few extra pixels wasted
                //  or take more room than available.
            }
        }
    }
}

public interface ButtonGroupScope {
    /**
     * [ButtonGroup] uses a ratio of all sibling item [weight]s to assign a width to each item. The
     * horizontal space is distributed using [weight] first, and this will only be changed if any
     * item would be smaller than its [minWidth]. See also [Modifier.minWidth].
     *
     * @param weight The main way of distributing available space. This is a relative measure, and
     *   items with no weight specified will have a default of 1f.
     */
    public fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float
    ): Modifier

    /**
     * Specifies the minimum width this item can be, in Dp. This will only be used if distributing
     * the available space results in a item falling below its minimum width. Note that this is only
     * used before animations, pressing a button may result on neighbor button(s) going below their
     * minWidth. See also [Modifier.weight]
     *
     * @param minWidth the minimum width. If none is specified, minimumInteractiveComponentSize is
     *   used.
     */
    public fun Modifier.minWidth(minWidth: Dp = ButtonGroupDefaults.MinWidth): Modifier

    /**
     * Specifies the interaction source to use with this item. This is used to listen to events and
     * animate growing the pressed button and shrink the neighbor(s).
     */
    public fun Modifier.animateWidth(interactionSource: InteractionSource): Modifier
}

/** Contains the default values used by [ButtonGroup] */
public object ButtonGroupDefaults {
    /**
     * Return the recommended padding to use as the contentPadding of a [ButtonGroup], when it takes
     * the full width of the screen.
     */
    @Composable
    public fun fullWidthPaddings(): PaddingValues {
        val screenHeight = screenHeightDp().dp
        return PaddingValues(
            horizontal = screenHeight * FullWidthHorizontalPaddingPercentage / 100,
            vertical = 0.dp
        )
    }

    /** How much buttons grow (and neighbors shrink) when pressed. */
    public val ExpansionWidth: Dp = 24.dp

    /** Spacing between buttons. */
    public val Spacing: Dp = 4.dp

    /** Default for the minimum width of buttons in a ButtonGroup */
    public val MinWidth: Dp = minimumInteractiveComponentSize

    /** Padding at each side of the [ButtonGroup], as a percentage of the available space. */
    private const val FullWidthHorizontalPaddingPercentage: Float = 5.2f
}

/**
 * Data class to configure one item in a [ButtonGroup]
 *
 * @param weight the main way of distributing available space. In most cases, items will have a
 *   width assigned proportional to their weight (and available space). The exception is if that
 *   will make some item(s) width fall below its minWidth.
 * @param minWidth the minimum width this item can be. This will only be used if distributing the
 *   available space results on a item falling below its minimum width.
 * @param pressedState an animated float between 0f and 1f that captures an animated, continuous
 *   version of the item's interaction source pressed state.
 */
internal data class ButtonGroupParentData(
    val weight: Float,
    val minWidth: Dp,
    val pressedState: Animatable<Float, AnimationVector1D>,
) {
    companion object {
        val DEFAULT = ButtonGroupParentData(1f, minimumInteractiveComponentSize, Animatable(0f))
    }
}

internal class ButtonGroupElement(
    val weight: Float = Float.NaN,
    val minWidth: Dp = Dp.Unspecified,
) : ModifierNodeElement<ButtonGroupNode>() {

    override fun create(): ButtonGroupNode {
        return ButtonGroupNode(weight, minWidth)
    }

    override fun update(node: ButtonGroupNode) {
        node.weight = weight
        node.minWidth = minWidth
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ButtonGroupElement"
        properties["weight"] = weight
        properties["minWidth"] = minWidth
    }

    override fun hashCode() = weight.hashCode() * 31 + minWidth.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? ButtonGroupNode ?: return false
        return weight == otherModifier.weight && minWidth == otherModifier.minWidth
    }
}

internal class ButtonGroupNode(var weight: Float, var minWidth: Dp) :
    ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        (parentData as? ButtonGroupParentData ?: ButtonGroupParentData.DEFAULT).let { prev ->
            ButtonGroupParentData(
                if (weight.fastIsFinite()) weight else prev.weight,
                minWidth.takeOrElse { prev.minWidth },
                prev.pressedState
            )
        }
}

internal class EnlargeOnPressElement(
    val interactionSource: InteractionSource,
    val downAnimSpec: AnimationSpec<Float>,
    val upAnimSpec: AnimationSpec<Float>,
) : ModifierNodeElement<EnlargeOnPressNode>() {

    override fun create(): EnlargeOnPressNode {
        return EnlargeOnPressNode(interactionSource, downAnimSpec, upAnimSpec)
    }

    override fun update(node: EnlargeOnPressNode) {
        if (node.interactionSource != interactionSource) {
            node.interactionSource = interactionSource
            node.launchCollectionJob()
        }
        node.downAnimSpec = downAnimSpec
        node.upAnimSpec = upAnimSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "EnlargeOnPressElement"
        properties["interactionSource"] = interactionSource
        properties["downAnimSpec"] = downAnimSpec
        properties["upAnimSpec"] = upAnimSpec
    }

    override fun hashCode() =
        (interactionSource.hashCode() * 31 + downAnimSpec.hashCode()) * 31 + upAnimSpec.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? EnlargeOnPressNode ?: return false
        return interactionSource == otherModifier.interactionSource &&
            downAnimSpec == otherModifier.downAnimSpec &&
            upAnimSpec == otherModifier.upAnimSpec
    }
}

internal class EnlargeOnPressNode(
    var interactionSource: InteractionSource,
    var downAnimSpec: AnimationSpec<Float>,
    var upAnimSpec: AnimationSpec<Float>,
) : ParentDataModifierNode, Modifier.Node() {
    private val pressedAnimatable: Animatable<Float, AnimationVector1D> = Animatable(0f)

    private var collectionJob: Job? = null

    override fun onAttach() {
        super.onAttach()

        launchCollectionJob()
    }

    override fun onDetach() {
        super.onDetach()
        collectionJob = null
    }

    internal fun launchCollectionJob() {
        collectionJob?.cancel()
        collectionJob =
            coroutineScope.launch {
                val pressInteractions = mutableListOf<PressInteraction.Press>()

                launch {
                    // Use collect here to ensure we don't lose any events.
                    interactionSource.interactions
                        .map { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> pressInteractions.add(interaction)
                                is PressInteraction.Release ->
                                    pressInteractions.remove(interaction.press)
                                is PressInteraction.Cancel ->
                                    pressInteractions.remove(interaction.press)
                            }
                            pressInteractions.isNotEmpty()
                        }
                        .distinctUntilChanged()
                        .collectLatest { pressed ->
                            if (pressed) {
                                launch { pressedAnimatable.animateTo(1f, downAnimSpec) }
                            } else {
                                waitUntil { pressedAnimatable.value > 0.75f }
                                pressedAnimatable.animateTo(0f, upAnimSpec)
                            }
                        }
                }
            }
    }

    override fun Density.modifyParentData(parentData: Any?) =
        (parentData as? ButtonGroupParentData ?: ButtonGroupParentData.DEFAULT).let { prev ->
            ButtonGroupParentData(prev.weight, prev.minWidth, pressedAnimatable)
        }
}

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
): IntArray {
    val helper =
        Array(items.size) { index ->
            val pair = items[index]
            ComputeHelper(pair.first, pair.second, index, pair.first)
        }
    val totalSpacing = spacingPx * (helper.size - 1)
    val minSpaceNeeded = totalSpacing + helper.map { it.width }.sum()

    val totalWeight = helper.map { it.weight }.sum()

    val extraSpace = availableWidth - minSpaceNeeded
    // TODO: should we really handle the totalWeight <= 0 case? If so, we need to leave items
    // at their minWidth and center the whole thing?
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
    // * Items with weight > 0, sorted for the order in which they may get below their
    // minimum width
    //   as we take away space.
    // * Items with weight == 0 and enough width (This can only happen if totalWeight
    // == 0)
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
    val ret = IntArray(helper.size) { 0 }
    helper.forEach { ret[it.originalIndex] = it.width.roundToInt() }
    return ret
}
