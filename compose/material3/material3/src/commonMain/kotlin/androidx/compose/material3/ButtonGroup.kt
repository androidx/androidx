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

package androidx.compose.material3

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.tokens.ButtonGroupSmallTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * TODO link to mio page when available.
 *
 * A layout composable that places its children in a horizontal sequence. When a child uses
 * [Modifier.interactionSourceData] with a relevant [MutableInteractionSource], this button group
 * can listen to the interactions and expand the width of the pressed child element as well as
 * compress the neighboring child elements. Material3 components already use
 * [Modifier.interactionSourceData] and will behave as expected.
 *
 * TODO link to an image when available
 *
 * @sample androidx.compose.material3.samples.ButtonGroupSample
 * @param modifier the [Modifier] to be applied to the button group.
 * @param animateFraction the percentage, represented by a float, of the width of the interacted
 *   child element that will be used to expand the interacted child element as well as compress the
 *   neighboring children.
 * @param horizontalArrangement The horizontal arrangement of the button group's children.
 * @param content the content displayed in the button group, expected to use a Material3 component
 *   or a composable that is tagged with [Modifier.interactionSourceData].
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun ButtonGroup(
    modifier: Modifier = Modifier,
    @FloatRange(0.0) animateFraction: Float = ButtonGroupDefaults.animateFraction,
    horizontalArrangement: Arrangement.Horizontal =
        Arrangement.spacedBy(ButtonGroupDefaults.spaceBetween),
    content: @Composable ButtonGroupScope.() -> Unit
) {
    val anim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var pressedIndex by remember { mutableIntStateOf(-1) }
    val scope = remember {
        object : ButtonGroupScope {
            override fun Modifier.weight(weight: Float, fill: Boolean): Modifier {
                require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
                return this.then(
                    LayoutWeightElement(
                        // Coerce Float.POSITIVE_INFINITY to Float.MAX_VALUE to avoid errors
                        weight = weight.coerceAtMost(Float.MAX_VALUE),
                        fill = fill
                    )
                )
            }
        }
    }

    val interactionSourceFlow: MutableStateFlow<List<InteractionSource>> = remember {
        MutableStateFlow(emptyList())
    }

    LaunchedEffect(Unit) {
        interactionSourceFlow.collectLatest { sources ->
            sources.fastForEachIndexed { index, interactionSource ->
                launch {
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                pressedIndex = index
                                coroutineScope.launch { anim.animateTo(animateFraction) }
                            }
                            is PressInteraction.Release,
                            is PressInteraction.Cancel -> {
                                coroutineScope.launch {
                                    anim.animateTo(0f)
                                    pressedIndex = -1
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val measurePolicy =
        remember(horizontalArrangement) {
            ButtonGroupMeasurePolicy(
                horizontalArrangement = horizontalArrangement,
                anim = anim,
                pressedIndex = { pressedIndex }
            )
        }

    Layout(
        measurePolicy = measurePolicy,
        modifier =
            modifier.onChildrenInteractionSourceChange { interactionSource ->
                if (interactionSourceFlow.value != interactionSource) {
                    coroutineScope.launch { interactionSourceFlow.emit(interactionSource) }
                }
            },
        content = { scope.content() }
    )
}

/** Default values used by [ButtonGroup] */
object ButtonGroupDefaults {
    /**
     * The default percentage, represented as a float, of the width of the interacted child element
     * that will be used to expand the interacted child element as well as compress the neighboring
     * children.
     */
    val animateFraction = 0.15f

    /** The default spacing used between children. */
    val spaceBetween = ButtonGroupSmallTokens.BetweenSpace
}

private class ButtonGroupMeasurePolicy(
    val horizontalArrangement: Arrangement.Horizontal,
    val anim: Animatable<Float, AnimationVector1D>,
    val pressedIndex: () -> Int,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val arrangementSpacingInt = horizontalArrangement.spacing.roundToPx()
        val arrangementSpacingPx = arrangementSpacingInt.toLong()
        val size = measurables.size
        var totalWeight = 0f
        var fixedSpace = 0
        var weightChildrenCount = 0
        val placeables: List<Placeable>
        val childrenMainAxisSize = IntArray(size)
        val childrenConstraints: Array<Constraints?> = arrayOfNulls(size)

        val mainAxisMin = constraints.minWidth
        val mainAxisMax = constraints.maxWidth

        // First obtain constraints of children with zero weight
        var spaceAfterLastNoWeight = 0
        for (i in 0 until size) {
            val child = measurables[i]
            val parentData = child.buttonGroupParentData
            val weight = parentData.weight

            if (weight > 0f) {
                totalWeight += weight
                ++weightChildrenCount
            } else {
                val remaining = mainAxisMax - fixedSpace
                val desiredWidth = child.maxIntrinsicWidth(constraints.maxHeight)
                childrenConstraints[i] =
                    constraints.copy(
                        minWidth = 0,
                        maxWidth =
                            if (mainAxisMax == Constraints.Infinity) {
                                Constraints.Infinity
                            } else {
                                desiredWidth.coerceAtLeast(0)
                            }
                    )

                childrenMainAxisSize[i] = desiredWidth

                spaceAfterLastNoWeight =
                    min(arrangementSpacingInt, (remaining - desiredWidth).coerceAtLeast(0))

                fixedSpace += desiredWidth + spaceAfterLastNoWeight
            }
        }

        var weightedSpace = 0
        if (weightChildrenCount == 0) {
            // fixedSpace contains an extra spacing after the last non-weight child.
            fixedSpace -= spaceAfterLastNoWeight
        } else {
            // obtain the constraints of the rest according to their weights.
            val targetSpace =
                if (mainAxisMax != Constraints.Infinity) {
                    mainAxisMax
                } else {
                    mainAxisMin
                }

            val arrangementSpacingTotal = arrangementSpacingPx * (weightChildrenCount - 1)
            val remainingToTarget =
                (targetSpace - fixedSpace - arrangementSpacingTotal).coerceAtLeast(0)

            val weightUnitSpace = remainingToTarget / totalWeight
            var remainder = remainingToTarget
            for (i in 0 until size) {
                val measurable = measurables[i]
                val itemWeight = measurable.buttonGroupParentData.weight
                val weightedSize = (weightUnitSpace * itemWeight)
                remainder -= weightedSize.fastRoundToInt()
            }

            for (i in 0 until size) {
                if (childrenConstraints[i] == null) {
                    val child = measurables[i]
                    val parentData = child.buttonGroupParentData
                    val weight = parentData.weight

                    // After the weightUnitSpace rounding, the total space going to be occupied
                    // can be smaller or larger than remainingToTarget. Here we distribute the
                    // loss or gain remainder evenly to the first children.
                    val remainderUnit = remainder.sign
                    remainder -= remainderUnit
                    val weightedSize = (weightUnitSpace * weight)

                    val childMainAxisSize = max(0, weightedSize.fastRoundToInt() + remainderUnit)

                    childrenConstraints[i] =
                        constraints.copy(
                            minWidth =
                                if (parentData.fill && childMainAxisSize != Constraints.Infinity) {
                                    childMainAxisSize
                                } else {
                                    0
                                },
                            maxWidth = childMainAxisSize
                        )

                    childrenMainAxisSize[i] = childMainAxisSize
                    weightedSpace += childMainAxisSize
                }
                weightedSpace =
                    (weightedSpace + arrangementSpacingTotal)
                        .toInt()
                        .coerceIn(0, mainAxisMax - fixedSpace)
            }
        }

        val pressedIdx = pressedIndex.invoke()
        if (pressedIdx == -1 || pressedIdx >= measurables.size) {
            placeables =
                measurables.fastMapIndexed { index, measurable ->
                    measurable.measure(childrenConstraints[index] ?: constraints)
                }
        } else {
            val adjacent = buildList {
                measurables.getOrNull(pressedIdx - 1)?.let { add(it) }
                measurables.getOrNull(pressedIdx + 1)?.let { add(it) }
            }

            val pressedMeasurable = measurables[pressedIdx]
            val pressedWidth = (childrenConstraints[pressedIdx] ?: constraints).maxWidth
            val additionFactor = anim.value
            val subtractFactor =
                if (pressedIdx == 0 || pressedIdx == size - 1) anim.value else anim.value / 2f

            placeables =
                measurables.fastMapIndexed { index, measurable ->
                    val desiredWidth = (childrenConstraints[index] ?: constraints).maxWidth
                    if (measurable == pressedMeasurable) {
                        measurable.measure(
                            Constraints.fixedWidth(
                                (desiredWidth + (pressedWidth * additionFactor)).roundToInt()
                            )
                        )
                    } else if (measurable in adjacent) {
                        measurable.measure(
                            Constraints.fixedWidth(
                                (desiredWidth - (pressedWidth * subtractFactor)).roundToInt()
                            )
                        )
                    } else {
                        measurable.measure(childrenConstraints[index] ?: constraints)
                    }
                }
        }

        // Compute the row size and position the children.
        val mainAxisLayoutSize = max((fixedSpace + weightedSpace).coerceAtLeast(0), mainAxisMin)
        val mainAxisPositions = IntArray(size) { 0 }
        val measureScope = this
        with(horizontalArrangement) {
            measureScope.arrange(
                mainAxisLayoutSize,
                childrenMainAxisSize,
                measureScope.layoutDirection,
                mainAxisPositions
            )
        }

        val height = placeables.fastMaxBy { it.height }?.height ?: constraints.minHeight
        return layout(mainAxisLayoutSize, height) {
            var currentX = 0
            placeables.fastForEach {
                it.place(currentX, 0)
                currentX += it.width + arrangementSpacingInt
            }
        }
    }
}

/** Button group scope used to indicate a [Modifier.weight] of a child element. */
@ExperimentalMaterial3ExpressiveApi
interface ButtonGroupScope {
    /**
     * Size the element's width proportional to its [weight] relative to other weighted sibling
     * elements in the [ButtonGroup]. The parent will divide the horizontal space remaining after
     * measuring unweighted child elements and distribute it according to this weight. When [fill]
     * is true, the element will be forced to occupy the whole width allocated to it. Otherwise, the
     * element is allowed to be smaller - this will result in [ButtonGroup] being smaller, as the
     * unused allocated width will not be redistributed to other siblings.
     *
     * @param weight The proportional width to give to this element, as related to the total of all
     *   weighted siblings. Must be positive.
     * @param fill When `true`, the element will occupy the whole width allocated.
     */
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true
    ): Modifier
}

internal val IntrinsicMeasurable.buttonGroupParentData: ButtonGroupParentData?
    get() = parentData as? ButtonGroupParentData

internal val ButtonGroupParentData?.fill: Boolean
    get() = this?.fill ?: true

internal val ButtonGroupParentData?.weight: Float
    get() = this?.weight ?: 0f

internal data class ButtonGroupParentData(var weight: Float = 0f, var fill: Boolean = true)

internal class LayoutWeightElement(
    val weight: Float,
    val fill: Boolean,
) : ModifierNodeElement<LayoutWeightNode>() {
    override fun create(): LayoutWeightNode {
        return LayoutWeightNode(weight, fill)
    }

    override fun update(node: LayoutWeightNode) {
        node.weight = weight
        node.fill = fill
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "weight"
        value = weight
        properties["weight"] = weight
        properties["fill"] = fill
    }

    override fun hashCode(): Int {
        var result = weight.hashCode()
        result = 31 * result + fill.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? LayoutWeightElement ?: return false
        return weight == otherModifier.weight && fill == otherModifier.fill
    }
}

internal class LayoutWeightNode(
    var weight: Float,
    var fill: Boolean,
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? ButtonGroupParentData) ?: ButtonGroupParentData()).also {
            it.weight = weight
            it.fill = fill
        }
}
