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

package androidx.compose.ui.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope

@Suppress("ComposableLambdaParameterPosition")
@Deprecated(
    "LookaheadLayout has been replaced with LookaheadScope that does not require" +
        " a Modifier or a MeasurePolicy.",
    replaceWith = ReplaceWith(
        "LookaheadScope { Layout(content = { content() }, \n" +
            " modifier = modifier, measurePolicy = measurePolicy) }"
    )
)
@ExperimentalComposeUiApi
@UiComposable
@Composable
fun LookaheadLayout(
    content: @Composable @UiComposable LookaheadScope.() -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    LookaheadScope {
        Layout(
            content = { content() },
            modifier = modifier,
            measurePolicy = measurePolicy
        )
    }
}

/**
 * [LookaheadLayoutScope] provides a receiver scope for all (direct and indirect) child layouts in
 * [LookaheadLayout]. In [LookaheadLayoutScope], the measurement and placement of any layout
 * calculated in the lookahead pass can be observed via [Modifier.intermediateLayout] and
 * [Modifier.onPlaced] respectively.
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 */
@Deprecated(
    "LookaheadLayoutScope has been renamed to LookaheadScope",
    ReplaceWith("LookaheadScope")
)
@ExperimentalComposeUiApi
interface LookaheadLayoutScope {
    @Deprecated(
        "onPlaced in LookaheadLayoutScope has been deprecated. It's replaced" +
            " with reading LookaheadLayoutCoordinates directly during placement in" +
            " IntermediateMeasureScope. See example below."
    )
    /**
     * [onPlaced] gets invoked after the parent [LayoutModifier] has been placed
     * and before child [LayoutModifier] is placed. This allows child [LayoutModifier] to adjust
     * its own placement based on its parent.
     *
     * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
     */
    @Suppress("DEPRECATION")
    fun Modifier.onPlaced(
        onPlaced: (
            lookaheadScopeCoordinates: LookaheadLayoutCoordinates,
            layoutCoordinates: LookaheadLayoutCoordinates
        ) -> Unit
    ): Modifier
}

/**
 * [LookaheadScope] starts a scope in which all layouts scope will receive a lookahead pass
 * preceding the main measure/layout pass. This lookahead pass will calculate the layout
 * size and position for all child layouts, and make the lookahead results available in
 * [Modifier.intermediateLayout]. [Modifier.intermediateLayout] gets invoked in the main
 * pass to allow transient layout changes in the main pass that gradually morph the layout
 * over the course of multiple frames until it catches up with lookahead.
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 *
 * @param content The child composable to be laid out.
 */
@ExperimentalComposeUiApi
@UiComposable
@Composable
fun LookaheadScope(content: @Composable @UiComposable LookaheadScope.() -> Unit) {
    val scope = remember { LookaheadScopeImpl() }
    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = { LayoutNode(isVirtual = true) },
        update = {
            init { isVirtualLookaheadRoot = true }
            set(scope) { scope ->
                // This internal lambda will be invoked during placement.
                scope.scopeCoordinates = {
                    parent!!.innerCoordinator.coordinates
                }
            }
        },
        content = {
            scope.content()
        }
    )
}

/**
 * Creates an intermediate layout intended to help morph the layout from the current layout
 * to the lookahead (i.e. pre-calculated future) layout.
 *
 * This modifier will be invoked _after_ lookahead pass and will have access to the lookahead
 * results in [measure]. Therefore:
 * 1) [intermediateLayout] measure/layout logic will not affect lookahead pass, but only be
 * invoked during the main measure/layout pass,
 * and 2) [measure] block can define intermediate changes that morphs the layout in the
 * main pass gradually until it converges lookahead pass.
 *
 * @sample androidx.compose.ui.samples.IntermediateLayoutSample
 */
@ExperimentalComposeUiApi
fun Modifier.intermediateLayout(
    measure: IntermediateMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
): Modifier = this then IntermediateLayoutElement(measure)

@OptIn(ExperimentalComposeUiApi::class)
private data class IntermediateLayoutElement(
    val measure: IntermediateMeasureScope.(
        measurable: Measurable,
        constraints: Constraints,
    ) -> MeasureResult,
) : ModifierNodeElement<IntermediateLayoutModifierNode>() {
    override fun create() = IntermediateLayoutModifierNode(measure)
    override fun update(node: IntermediateLayoutModifierNode): IntermediateLayoutModifierNode =
        node.apply { this.measureBlock = measure }

    override fun InspectorInfo.inspectableProperties() {
        name = "intermediateLayout"
        properties["measure"] = measure
    }
}

/**
 * [IntermediateMeasureScope] provides access to lookahead results to allow
 * [intermediateLayout] to leverage lookahead results to define intermediate measurements
 * and placements to gradually converge with lookahead.
 *
 * [IntermediateMeasureScope.lookaheadSize] provides the target size of the layout.
 * [IntermediateMeasureScope] is also a [LookaheadScope], thus allowing layouts to
 * read their [LookaheadLayoutCoordinates] during placement using
 * [LookaheadScope.toLookaheadCoordinates], as well as the [LookaheadLayoutCoordinates] of the
 * closest lookahead scope via [LookaheadScope.lookaheadScopeCoordinates].
 * By knowing the target size and position, layout adjustments such as animations can be defined
 * in [intermediateLayout] to morph the layout gradually in both size and position
 * to arrive at its precalculated bounds.
 *
 * Note that [IntermediateMeasureScope] is the closest lookahead scope in the tree.
 * This [LookaheadScope] enables convenient query of the layout's relative position to the
 * [LookaheadScope]. Hence it becomes straightforward to animate position relative to the closest
 * scope, which usually yields a natural looking animation, unless there are specific UX
 * requirements to change position relative to a particular [LookaheadScope].
 *
 * [IntermediateMeasureScope] is a CoroutineScope, as a convenient scope for all the
 * coroutine-based intermediate changes (e.g. animations) to be launched from.
 */
@ExperimentalComposeUiApi
sealed interface IntermediateMeasureScope : LookaheadScope, CoroutineScope, MeasureScope {
    /**
     * Indicates the target size of the [intermediateLayout].
     */
    val lookaheadSize: IntSize
}

/**
 * [LookaheadScope] provides a receiver scope for all (direct and indirect) child layouts in
 * [LookaheadScope]. This receiver scope allows access to [lookaheadScopeCoordinates] from
 * any child's [Placeable.PlacementScope]. It also allows any child to convert
 * [LayoutCoordinates] (which can be retrieved in [Placeable.PlacementScope]) to
 * [LookaheadLayoutCoordinates] using [toLookaheadCoordinates].
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 */
@ExperimentalComposeUiApi
interface LookaheadScope {
    /**
     * Converts a [LayoutCoordinates] into a [LayoutCoordinates] in the Lookahead coordinates space.
     * This is only applicable to child layouts within [LookaheadScope].
     */
    fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates

    /**
     * Returns the [LayoutCoordinates] of the [LookaheadScope]. This is
     * only accessible from [Placeable.PlacementScope] (i.e. during placement time).
     */
    val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates

    /**
     * Calculates the localPosition in the Lookahead coordinate space. This is a convenient
     * method for 1) converting the given [LayoutCoordinates] to lookahead coordinates using
     * [toLookaheadCoordinates], and 2) invoking [LayoutCoordinates.localPositionOf] with the
     * converted coordinates.
     */
    fun LayoutCoordinates.localLookaheadPositionOf(coordinates: LayoutCoordinates) =
        this.toLookaheadCoordinates().localPositionOf(
            coordinates.toLookaheadCoordinates(),
            Offset.Zero
        )

    @Suppress("DEPRECATION")
    @Deprecated(
        "onPlaced in LookaheadLayoutScope has been deprecated. It's replaced" +
            " with reading LookaheadLayoutCoordinates directly during placement in" +
            " IntermediateMeasureScope. See example below."
    )
        /**
         * [onPlaced] gets invoked after the parent [LayoutModifier] has been placed
         * and before child [LayoutModifier] is placed. This allows child [LayoutModifier] to adjust
         * its own placement based on its parent.
         *
         * [onPlaced] callback will be invoked with the [LookaheadLayoutCoordinates] of the LayoutNode
         * emitted by [LookaheadLayout] as the first parameter, and the [LookaheadLayoutCoordinates] of
         * this modifier as the second parameter. Given the [LookaheadLayoutCoordinates]s, both
         * lookahead position and current position of this modifier in the [LookaheadLayout]'s
         * coordinates system can be calculated using
         * [LookaheadLayoutCoordinates.localLookaheadPositionOf] and
         * [LookaheadLayoutCoordinates.localPositionOf], respectively.
         *
         * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
         */
    fun Modifier.onPlaced(
        onPlaced: (
            lookaheadScopeCoordinates: LookaheadLayoutCoordinates,
            layoutCoordinates: LookaheadLayoutCoordinates
        ) -> Unit
    ): Modifier

    @Deprecated(
        "",
        ReplaceWith(
            "intermediateLayout { measurable, constraints ->" +
                "measure.invoke(this, measurable, constraints, lookaheadSize)" +
                "}"
        )
    )
        /**
         * Creates an intermediate layout based on target size of the child layout calculated
         * in the lookahead. This allows the intermediate layout to morph the child layout
         * after lookahead through [measure], in which the size of the child layout calculated from the
         * lookahead is provided. [intermediateLayout] does _not_ participate in the lookahead. It is
         * only invoked for retroactively changing the layout based on the lookahead before the layout
         * is drawn.
         *
         * @sample androidx.compose.ui.samples.IntermediateLayoutSample
         */
    fun Modifier.intermediateLayout(
        measure: MeasureScope.(
            measurable: Measurable,
            constraints: Constraints,
            lookaheadSize: IntSize
        ) -> MeasureResult,
    ): Modifier =
        this.intermediateLayout { measurable: Measurable, constraints: Constraints ->
            measure(measurable, constraints, lookaheadSize)
        }
}

@OptIn(ExperimentalComposeUiApi::class)
internal class LookaheadScopeImpl(
    var scopeCoordinates: (() -> LayoutCoordinates)? = null
) : LookaheadScope {
    override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates {
        return this as? LookaheadLayoutCoordinatesImpl
            ?: (this as NodeCoordinator).let {
                // If the coordinator has no lookahead delegate. Its
                // lookahead coords is the same as its coords
                it.lookaheadDelegate?.lookaheadLayoutCoordinates ?: it
            }
    }

    override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
        get() = scopeCoordinates!!()

    @Suppress("DEPRECATION")
    @Deprecated(
        "onPlaced in LookaheadLayoutScope has been deprecated. It's replaced" +
            " with reading LookaheadLayoutCoordinates directly during placement in" +
            "IntermediateMeasureScope"
    )
    override fun Modifier.onPlaced(
        onPlaced: (
            lookaheadScopeCoordinates: LookaheadLayoutCoordinates,
            layoutCoordinates: LookaheadLayoutCoordinates
        ) -> Unit
    ): Modifier = this.onPlaced { coordinates ->
        onPlaced(
            scopeCoordinates!!().toLookaheadCoordinates() as LookaheadLayoutCoordinates,
            coordinates.toLookaheadCoordinates() as LookaheadLayoutCoordinates
        )
    }
}