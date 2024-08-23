/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastRoundToInt

/**
 * Adds padding so that the content doesn't enter [insets] space.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from [insets]. [insets] will be [consumed][consumeWindowInsets] for child
 * layouts as well.
 *
 * For example, if an ancestor uses [statusBarsPadding] and this modifier uses
 * [WindowInsets.Companion.systemBars], the portion of the system bars that the status bars uses
 * will not be padded again by this modifier.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsPaddingSample
 * @see WindowInsets
 */
@Stable
fun Modifier.windowInsetsPadding(insets: WindowInsets): Modifier =
    composed(
        debugInspectorInfo {
            name = "windowInsetsPadding"
            properties["insets"] = insets
        }
    ) {
        remember(insets) { InsetsPaddingModifier(insets) }
    }

/**
 * Consume insets that haven't been consumed yet by other insets Modifiers similar to
 * [windowInsetsPadding] without adding any padding.
 *
 * This can be useful when content offsets are provided by [WindowInsets.asPaddingValues]. This
 * should be used further down the hierarchy than the [PaddingValues] is used so that the values
 * aren't consumed before the padding is added.
 *
 * @sample androidx.compose.foundation.layout.samples.consumedInsetsSample
 */
@Stable
fun Modifier.consumeWindowInsets(insets: WindowInsets): Modifier =
    composed(
        debugInspectorInfo {
            name = "consumeWindowInsets"
            properties["insets"] = insets
        }
    ) {
        remember(insets) { UnionInsetsConsumingModifier(insets) }
    }

/**
 * Consume [paddingValues] as insets as if the padding was added irrespective of insets. Layouts
 * further down the hierarchy that use [windowInsetsPadding], [safeContentPadding], and other insets
 * padding Modifiers won't pad for the values that [paddingValues] provides. This can be useful when
 * content offsets are provided by layout rather than [windowInsetsPadding] modifiers.
 *
 * This method consumes all of [paddingValues] in addition to whatever has been consumed by other
 * [windowInsetsPadding] modifiers by ancestors. [consumeWindowInsets] accepting a [WindowInsets]
 * argument ensures that its insets are consumed and doesn't consume more if they have already been
 * consumed by ancestors.
 *
 * @sample androidx.compose.foundation.layout.samples.consumedInsetsPaddingSample
 */
@Stable
fun Modifier.consumeWindowInsets(paddingValues: PaddingValues): Modifier =
    composed(
        debugInspectorInfo {
            name = "consumeWindowInsets"
            properties["paddingValues"] = paddingValues
        }
    ) {
        remember(paddingValues) { PaddingValuesConsumingModifier(paddingValues) }
    }

/**
 * Calls [block] with the [WindowInsets] that have been consumed, either by [consumeWindowInsets] or
 * one of the padding Modifiers, such as [imePadding].
 *
 * @sample androidx.compose.foundation.layout.samples.withConsumedInsetsSample
 */
@Stable
fun Modifier.onConsumedWindowInsetsChanged(block: (consumedWindowInsets: WindowInsets) -> Unit) =
    composed(
        debugInspectorInfo {
            name = "onConsumedWindowInsetsChanged"
            properties["block"] = block
        }
    ) {
        remember(block) { ConsumedInsetsModifier(block) }
    }

/**
 * Adds padding to accommodate the [safe drawing][WindowInsets.Companion.safeDrawing] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.safeDrawing] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [statusBarsPadding], the area that the parent pads for the
 * status bars will not be padded again by this [safeDrawingPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.safeDrawingPaddingSample
 */
expect fun Modifier.safeDrawingPadding(): Modifier

/**
 * Adds padding to accommodate the [safe gestures][WindowInsets.Companion.safeGestures] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.safeGestures] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [navigationBarsPadding], the area that the parent layout
 * pads for the status bars will not be padded again by this [safeGesturesPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.safeGesturesPaddingSample
 */
expect fun Modifier.safeGesturesPadding(): Modifier

/**
 * Adds padding to accommodate the [safe content][WindowInsets.Companion.safeContent] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.safeContent] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [navigationBarsPadding], the area that the parent layout
 * pads for the status bars will not be padded again by this [safeContentPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.safeContentPaddingSample
 */
expect fun Modifier.safeContentPadding(): Modifier

/**
 * Adds padding to accommodate the [system bars][WindowInsets.Companion.systemBars] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.systemBars] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [statusBarsPadding], the area that the parent layout pads
 * for the status bars will not be padded again by this [systemBarsPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.systemBarsPaddingSample
 */
expect fun Modifier.systemBarsPadding(): Modifier

/**
 * Adds padding to accommodate the [display cutout][WindowInsets.Companion.displayCutout].
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.displayCutout] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [statusBarsPadding], the area that the parent layout pads
 * for the status bars will not be padded again by this [displayCutoutPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.displayCutoutPaddingSample
 */
expect fun Modifier.displayCutoutPadding(): Modifier

/**
 * Adds padding to accommodate the [status bars][WindowInsets.Companion.statusBars] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.statusBars] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [displayCutoutPadding], the area that the parent layout pads
 * for the status bars will not be padded again by this [statusBarsPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.statusBarsAndNavigationBarsPaddingSample
 */
expect fun Modifier.statusBarsPadding(): Modifier

/**
 * Adds padding to accommodate the [ime][WindowInsets.Companion.ime] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.ime] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [navigationBarsPadding], the area that the parent layout
 * pads for the status bars will not be padded again by this [imePadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.imePaddingSample
 */
expect fun Modifier.imePadding(): Modifier

/**
 * Adds padding to accommodate the [navigation bars][WindowInsets.Companion.navigationBars] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.navigationBars] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [systemBarsPadding], the area that the parent layout pads
 * for the status bars will not be padded again by this [navigationBarsPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.statusBarsAndNavigationBarsPaddingSample
 */
expect fun Modifier.navigationBarsPadding(): Modifier

/**
 * Adds padding to accommodate the [caption bar][WindowInsets.Companion.captionBar] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.captionBar] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [displayCutoutPadding], the area that the parent layout pads
 * for the status bars will not be padded again by this [captionBarPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.captionBarPaddingSample
 */
expect fun Modifier.captionBarPadding(): Modifier

/**
 * Adds padding to accommodate the [waterfall][WindowInsets.Companion.waterfall] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.waterfall] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [systemGesturesPadding], the area that the parent layout
 * pads for the status bars will not be padded again by this [waterfallPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.waterfallPaddingSample
 */
expect fun Modifier.waterfallPadding(): Modifier

/**
 * Adds padding to accommodate the [system gestures][WindowInsets.Companion.systemGestures] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.systemGestures] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [waterfallPadding], the area that the parent layout pads for
 * the status bars will not be padded again by this [systemGesturesPadding] modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.systemGesturesPaddingSample
 */
expect fun Modifier.systemGesturesPadding(): Modifier

/**
 * Adds padding to accommodate the
 * [mandatory system gestures][WindowInsets.Companion.mandatorySystemGestures] insets.
 *
 * Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a parent layout
 * will be excluded from the padding. [WindowInsets.Companion.mandatorySystemGestures] will be
 * [consumed][consumeWindowInsets] for child layouts as well.
 *
 * For example, if a parent layout uses [navigationBarsPadding], the area that the parent layout
 * pads for the status bars will not be padded again by this [mandatorySystemGesturesPadding]
 * modifier.
 *
 * When used, the [WindowInsets] will be consumed.
 *
 * @sample androidx.compose.foundation.layout.samples.mandatorySystemGesturesPaddingSample
 */
expect fun Modifier.mandatorySystemGesturesPadding(): Modifier

/**
 * This recalculates the [WindowInsets] based on the size and position. This only works when
 * [Constraints] have [fixed width][Constraints.hasFixedWidth] and
 * [fixed height][Constraints.hasFixedHeight]. This can be accomplished, for example, by having
 * [Modifier.size], or [Modifier.fillMaxSize], or other size modifier before
 * [recalculateWindowInsets]. If the [Constraints] sizes aren't fixed, [recalculateWindowInsets]
 * won't adjust the [WindowInsets] and won't have any affect on layout.
 *
 * [recalculateWindowInsets] is useful when the parent does not call [consumeWindowInsets] when it
 * aligns a child. For example, a [Column] with two children should have different [WindowInsets]
 * for each child. The top item should exclude insets below its bottom and the bottom item should
 * exclude the top insets, but the Column can't assign different insets for different children.
 *
 * @sample androidx.compose.foundation.layout.samples.recalculateWindowInsetsSample
 *
 * Another use is when a parent doesn't properly [consumeWindowInsets] for all space that it
 * consumes. For example, a 3rd-party container has padding that doesn't properly use
 * [consumeWindowInsets].
 *
 * @sample androidx.compose.foundation.layout.samples.unconsumedWindowInsetsWithPaddingSample
 *
 * In most cases you should not need to use this API, and the parent should instead use
 * [consumeWindowInsets] to provide the correct values
 *
 * @sample androidx.compose.foundation.layout.samples.consumeWindowInsetsWithPaddingSample
 */
fun Modifier.recalculateWindowInsets(): Modifier = this.then(RecalculateWindowInsetsModifierElement)

internal val ModifierLocalConsumedWindowInsets = modifierLocalOf { WindowInsets(0, 0, 0, 0) }

internal class InsetsPaddingModifier(private val insets: WindowInsets) :
    LayoutModifier, ModifierLocalConsumer, ModifierLocalProvider<WindowInsets> {
    private var unconsumedInsets: WindowInsets by mutableStateOf(insets)
    private var consumedInsets: WindowInsets by mutableStateOf(insets)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val left = unconsumedInsets.getLeft(this, layoutDirection)
        val top = unconsumedInsets.getTop(this)
        val right = unconsumedInsets.getRight(this, layoutDirection)
        val bottom = unconsumedInsets.getBottom(this)

        val horizontal = left + right
        val vertical = top + bottom

        val childConstraints = constraints.offset(-horizontal, -vertical)
        val placeable = measurable.measure(childConstraints)

        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)
        return layout(width, height) { placeable.place(left, top) }
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) {
            val consumed = ModifierLocalConsumedWindowInsets.current
            unconsumedInsets = insets.exclude(consumed)
            consumedInsets = consumed.union(insets)
        }
    }

    override val key: ProvidableModifierLocal<WindowInsets>
        get() = ModifierLocalConsumedWindowInsets

    override val value: WindowInsets
        get() = consumedInsets

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InsetsPaddingModifier) {
            return false
        }

        return other.insets == insets
    }

    override fun hashCode(): Int = insets.hashCode()
}

/** Base class for arbitrary insets consumption modifiers. */
@Stable
private sealed class InsetsConsumingModifier :
    ModifierLocalConsumer, ModifierLocalProvider<WindowInsets> {
    private var consumedInsets: WindowInsets by mutableStateOf(WindowInsets(0, 0, 0, 0))

    abstract fun calculateInsets(modifierLocalInsets: WindowInsets): WindowInsets

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        with(scope) {
            val current = ModifierLocalConsumedWindowInsets.current
            consumedInsets = calculateInsets(current)
        }
    }

    override val key: ProvidableModifierLocal<WindowInsets>
        get() = ModifierLocalConsumedWindowInsets

    override val value: WindowInsets
        get() = consumedInsets
}

@Stable
private class PaddingValuesConsumingModifier(private val paddingValues: PaddingValues) :
    InsetsConsumingModifier() {
    override fun calculateInsets(modifierLocalInsets: WindowInsets): WindowInsets =
        paddingValues.asInsets().add(modifierLocalInsets)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PaddingValuesConsumingModifier) {
            return false
        }

        return other.paddingValues == paddingValues
    }

    override fun hashCode(): Int = paddingValues.hashCode()
}

@Stable
private class ConsumedInsetsModifier(private val block: (WindowInsets) -> Unit) :
    ModifierLocalConsumer {

    private var oldWindowInsets: WindowInsets? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ConsumedInsetsModifier) {
            return false
        }

        return other.block === block
    }

    override fun hashCode(): Int = block.hashCode()

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) =
        with(scope) {
            val consumed = ModifierLocalConsumedWindowInsets.current
            if (consumed != oldWindowInsets) {
                oldWindowInsets = consumed
                block(consumed)
            }
        }
}

@Stable
private class UnionInsetsConsumingModifier(private val insets: WindowInsets) :
    InsetsConsumingModifier() {
    override fun calculateInsets(modifierLocalInsets: WindowInsets): WindowInsets =
        insets.union(modifierLocalInsets)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is UnionInsetsConsumingModifier) {
            return false
        }

        return other.insets == insets
    }

    override fun hashCode(): Int = insets.hashCode()
}

private object RecalculateWindowInsetsModifierElement :
    ModifierNodeElement<RecalculateWindowInsetsModifierNode>() {
    override fun create(): RecalculateWindowInsetsModifierNode =
        RecalculateWindowInsetsModifierNode()

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other === this

    override fun update(node: RecalculateWindowInsetsModifierNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "recalculateWindowInsets"
    }
}

private class RecalculateWindowInsetsModifierNode :
    Modifier.Node(),
    ModifierLocalModifierNode,
    LayoutModifierNode,
    GlobalPositionAwareModifierNode {
    val insets = ValueInsets(InsetsValues(0, 0, 0, 0), "reset")
    var oldPosition = IntOffset.Zero

    override val providedValues: ModifierLocalMap =
        modifierLocalMapOf(ModifierLocalConsumedWindowInsets to insets)

    override val shouldAutoInvalidate: Boolean
        get() = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return if (!constraints.hasFixedWidth || !constraints.hasFixedHeight) {
            // We can't provide the modifier local value.
            // We'll fall back to measuring the contents without providing the value
            provide(ModifierLocalConsumedWindowInsets, ModifierLocalConsumedWindowInsets.current)
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            layout(width, height) {
                val coordinates = coordinates
                coordinates?.let { oldPosition = it.positionInRoot().round() }
                val windowInsets =
                    if (coordinates == null) {
                        // We don't know where we are, so can't reset the value. Use the old value.
                        ModifierLocalConsumedWindowInsets.current
                    } else {
                        val topLeft = coordinates.positionInRoot()
                        val size = coordinates.size
                        val bottomRight =
                            coordinates.localToRoot(
                                Offset(size.width.toFloat(), size.height.toFloat())
                            )
                        val root = coordinates.findRootCoordinates()
                        val rootSize = root.size
                        val left = topLeft.x.fastRoundToInt()
                        val top = topLeft.y.fastRoundToInt()
                        val right = rootSize.width - bottomRight.x.fastRoundToInt()
                        val bottom = rootSize.height - bottomRight.y.fastRoundToInt()
                        val oldValues = insets.value
                        if (
                            oldValues.left != left ||
                                oldValues.top != top ||
                                oldValues.right != right ||
                                oldValues.bottom != bottom
                        ) {
                            insets.value = InsetsValues(left, top, right, bottom)
                        }
                        insets
                    }
                provide(ModifierLocalConsumedWindowInsets, windowInsets)
                val placeable = measurable.measure(Constraints.fixed(width, height))
                placeable.place(0, 0)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = measurable.maxIntrinsicWidth(height)

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val newPosition = coordinates.positionInRoot().round()
        val hasMoved = oldPosition != newPosition
        oldPosition = newPosition
        if (hasMoved) {
            invalidatePlacement()
        }
    }
}
