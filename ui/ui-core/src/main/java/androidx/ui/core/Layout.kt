/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import android.content.Context
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.Stable
import androidx.compose.Untracked
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.min
import androidx.ui.util.fastForEach

/**
 * [Layout] is the main core component for layout. It can be used to measure and position
 * zero or more children.
 *
 * Intrinsic measurement blocks define the intrinsic sizes of the current layout. These
 * can be queried by the parent in order to understand, in specific cases, what constraints
 * should the layout be measured with:
 * - [minIntrinsicWidthMeasureBlock] defines the minimum width this layout can take, given
 *   a specific height, such that the content of the layout will be painted correctly
 * - [minIntrinsicHeightMeasureBlock] defines the minimum height this layout can take, given
 *   a specific width, such that the content of the layout will be painted correctly
 * - [maxIntrinsicWidthMeasureBlock] defines the minimum width such that increasing it further
 *   will not decrease the minimum intrinsic height
 * - [maxIntrinsicHeightMeasureBlock] defines the minimum height such that increasing it further
 *   will not decrease the minimum intrinsic width
 *
 * For a composable able to define its content according to the incoming constraints,
 * see [WithConstraints].
 *
 * Example usage:
 * @sample androidx.ui.core.samples.LayoutWithProvidedIntrinsicsUsage
 *
 * @param children The children composable to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param minIntrinsicWidthMeasureBlock The minimum intrinsic width of the layout.
 * @param minIntrinsicHeightMeasureBlock The minimum intrinsic height of the layout.
 * @param maxIntrinsicWidthMeasureBlock The maximum intrinsic width of the layout.
 * @param maxIntrinsicHeightMeasureBlock The maximum intrinsic height of the layout.
 * @param measureBlock The block defining the measurement and positioning of the layout.
 *
 * @see Layout
 * @see WithConstraints
 */
@Composable
/*inline*/ fun Layout(
    /*crossinline*/
    children: @Composable () -> Unit,
    /*crossinline*/
    minIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    /*crossinline*/
    minIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    /*crossinline*/
    maxIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    /*crossinline*/
    maxIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    modifier: Modifier = Modifier,
    /*crossinline*/
    measureBlock: MeasureBlock
) {
    val measureBlocks = object : LayoutNode.MeasureBlocks {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ) = measureScope.measureBlock(measurables, constraints, layoutDirection)
        override fun minIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.minIntrinsicWidthMeasureBlock(measurables, h, layoutDirection)
        override fun minIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.minIntrinsicHeightMeasureBlock(measurables, w, layoutDirection)
        override fun maxIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.maxIntrinsicWidthMeasureBlock(measurables, h, layoutDirection)
        override fun maxIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.maxIntrinsicHeightMeasureBlock(measurables, w, layoutDirection)
    }
    Layout(children, measureBlocks, modifier)
}

/**
 * [Layout] is the main core component for layout. It can be used to measure and position
 * zero or more children.
 *
 * The intrinsic measurements of this layout will be calculated by running the measureBlock,
 * while swapping measure calls with appropriate intrinsic measurements. Note that these
 * provided implementations will not be accurate in all cases - when this happens, the other
 * overload of [Layout] should be used to provide correct measurements.
 *
 * For a composable able to define its content according to the incoming constraints,
 * see [WithConstraints].
 *
 * Example usage:
 * @sample androidx.ui.core.samples.LayoutUsage
 *
 * @param children The children composable to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param measureBlock The block defining the measurement and positioning of the layout.
 *
 * @see Layout
 * @see WithConstraints
 */
@Composable
/*inline*/ fun Layout(
    /*crossinline*/
    children: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    /*noinline*/
    measureBlock: MeasureBlock
) {

    val measureBlocks = remember(measureBlock) { MeasuringIntrinsicsMeasureBlocks(measureBlock) }
    Layout(children, measureBlocks, modifier)
}

/*@PublishedApi*/ @Composable internal /*inline*/ fun Layout(
    /*crossinline*/
    children: @Composable () -> Unit,
    measureBlocks: LayoutNode.MeasureBlocks,
    modifier: Modifier
) {
    LayoutNode(modifier = currentComposer.materialize(modifier), measureBlocks = measureBlocks) {
        children()
    }
}

@Composable
@Deprecated("This composable is temporary to enable quicker prototyping in ConstraintLayout. " +
        "It should not be used in app code directly.")
fun MultiMeasureLayout(
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit,
    measureBlock: MeasureBlock
) {
    val measureBlocks = remember(measureBlock) { MeasuringIntrinsicsMeasureBlocks(measureBlock) }
    LayoutNode(
        modifier = currentComposer.materialize(modifier),
        measureBlocks = measureBlocks,
        canMultiMeasure =
        true
    ) {
        children()
    }
}

@Composable
@Deprecated("This composable supports our transition from single child composables to modifiers. " +
        "It should not be used in app code directly.")
fun PassThroughLayout(
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    val measureBlocks = remember {
        val measureBlock: MeasureBlock = { measurables, constraints, _ ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = placeables.maxBy { it.width }?.width ?: constraints.minWidth
            val height = placeables.maxBy { it.height }?.height ?: constraints.minHeight
            layout(width, height) {
                placeables.fastForEach { it.place(IntPx.Zero, IntPx.Zero) }
            }
        }
        MeasuringIntrinsicsMeasureBlocks(measureBlock)
    }
    LayoutNode(
        modifier = currentComposer.materialize(modifier),
        measureBlocks = measureBlocks,
        handlesParentData = false,
        useChildZIndex = true
    ) {
        children()
    }
}

/**
 * Used to return a fixed sized item for intrinsics measurements in [Layout]
 */
private class DummyPlaceable(width: IntPx, height: IntPx) : Placeable() {
    override fun get(line: AlignmentLine): IntPx? = null
    override val measurementConstraints = Constraints()
    override val measuredSize = IntPxSize(width, height)
    override fun place(position: IntPxPosition) { }
}

/**
 * Identifies an [IntrinsicMeasurable] as a min or max intrinsic measurement.
 */
@PublishedApi
internal enum class IntrinsicMinMax {
    Min, Max
}

/**
 * Identifies an [IntrinsicMeasurable] as a width or height intrinsic measurement.
 */
@PublishedApi
internal enum class IntrinsicWidthHeight {
    Width, Height
}

/**
 * A wrapper around a [Measurable] for intrinsic measurments in [Layout]. Consumers of
 * [Layout] don't identify intrinsic methods, but we can give a reasonable implementation
 * by using their [measure], substituting the intrinsics gathering method
 * for the [Measurable.measure] call.
 */
@PublishedApi
internal class DefaultIntrinsicMeasurable(
    val measurable: IntrinsicMeasurable,
    val minMax: IntrinsicMinMax,
    val widthHeight: IntrinsicWidthHeight
) : Measurable {
    override val parentData: Any?
        get() = measurable.parentData

    override fun measure(constraints: Constraints, layoutDirection: LayoutDirection): Placeable {
        if (widthHeight == IntrinsicWidthHeight.Width) {
            val width = if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicWidth(constraints.maxHeight, layoutDirection)
            } else {
                measurable.minIntrinsicWidth(constraints.maxHeight, layoutDirection)
            }
            return DummyPlaceable(width, constraints.maxHeight)
        }
        val height = if (minMax == IntrinsicMinMax.Max) {
            measurable.maxIntrinsicHeight(constraints.maxWidth, layoutDirection)
        } else {
            measurable.minIntrinsicHeight(constraints.maxWidth, layoutDirection)
        }
        return DummyPlaceable(constraints.maxWidth, height)
    }

    override fun minIntrinsicWidth(height: IntPx, layoutDirection: LayoutDirection): IntPx {
        return measurable.minIntrinsicWidth(height, layoutDirection)
    }

    override fun maxIntrinsicWidth(height: IntPx, layoutDirection: LayoutDirection): IntPx {
        return measurable.maxIntrinsicWidth(height, layoutDirection)
    }

    override fun minIntrinsicHeight(width: IntPx, layoutDirection: LayoutDirection): IntPx {
        return measurable.minIntrinsicHeight(width, layoutDirection)
    }

    override fun maxIntrinsicHeight(width: IntPx, layoutDirection: LayoutDirection): IntPx {
        return measurable.maxIntrinsicHeight(width, layoutDirection)
    }
}

/**
 * Receiver scope for [Layout]'s layout lambda when used in an intrinsics call.
 */
@PublishedApi
internal class IntrinsicsMeasureScope(
    density: Density,
    override val layoutDirection: LayoutDirection
) : MeasureScope(), Density by density
/**
 * Default [LayoutNode.MeasureBlocks] object implementation, providing intrinsic measurements
 * that use the measure block replacing the measure calls with intrinsic measurement calls.
 */
fun MeasuringIntrinsicsMeasureBlocks(measureBlock: MeasureBlock) =
    object : LayoutNode.MeasureBlocks {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ) = measureScope.measureBlock(measurables, constraints, layoutDirection)
        override fun minIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.MeasuringMinIntrinsicWidth(
            measureBlock,
            measurables,
            h,
            layoutDirection
        )
        override fun minIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.MeasuringMinIntrinsicHeight(
            measureBlock,
            measurables,
            w,
            layoutDirection
        )
        override fun maxIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.MeasuringMaxIntrinsicWidth(
            measureBlock,
            measurables,
            h,
            layoutDirection
        )
        override fun maxIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx,
            layoutDirection: LayoutDirection
        ) = intrinsicMeasureScope.MeasuringMaxIntrinsicHeight(
            measureBlock,
            measurables,
            w,
            layoutDirection
        )

        override fun toString(): String {
            // this calls simpleIdentityToString on measureBlock because it is typically a lambda,
            // which has a useless toString that doesn't hint at the source location
            return simpleIdentityToString(
                this,
                "MeasuringIntrinsicsMeasureBlocks"
            ) + "{ measureBlock=${simpleIdentityToString(measureBlock)} }"
        }
    }

/**
 * Default implementation for the min intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMinIntrinsicWidth(
    measureBlock: MeasureBlock /*TODO: crossinline*/,
    measurables: List<IntrinsicMeasurable>,
    h: IntPx,
    layoutDirection: LayoutDirection
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
    }
    val constraints = Constraints(maxHeight = h)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints, layoutDirection)
    return layoutResult.width
}

/**
 * Default implementation for the min intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMinIntrinsicHeight(
    measureBlock: MeasureBlock /*TODO: crossinline*/,
    measurables: List<IntrinsicMeasurable>,
    w: IntPx,
    layoutDirection: LayoutDirection
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
    }
    val constraints = Constraints(maxWidth = w)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints, layoutDirection)
    return layoutResult.height
}

/**
 * Default implementation for the max intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMaxIntrinsicWidth(
    measureBlock: MeasureBlock /*TODO: crossinline*/,
    measurables: List<IntrinsicMeasurable>,
    h: IntPx,
    layoutDirection: LayoutDirection
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
    }
    val constraints = Constraints(maxHeight = h)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints, layoutDirection)
    return layoutResult.width
}

/**
 * Default implementation for the max intrinsic height of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMaxIntrinsicHeight(
    measureBlock: MeasureBlock /*TODO: crossinline*/,
    measurables: List<IntrinsicMeasurable>,
    w: IntPx,
    layoutDirection: LayoutDirection
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
    }
    val constraints = Constraints(maxWidth = w)
    val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints, layoutDirection)
    return layoutResult.height
}

/**
 * A composable that defines its own content according to the available space, based on the incoming
 * constraints or the current [LayoutDirection]. Example usage:
 * @sample androidx.ui.core.samples.WithConstraintsSample
 *
 * The composable will compose the given children, and will position the resulting layout composables
 * in a parent [Layout]. This layout will be as small as possible such that it can fit its
 * children. If the composition yields multiple layout children, these will be all placed at the
 * top left of the WithConstraints, so consider wrapping them in an additional common
 * parent if different positioning is preferred.
 *
 * @param modifier Modifier to be applied to the introduced layout.
 */
@Composable
fun WithConstraints(
    modifier: Modifier = Modifier,
    children: @Composable WithConstraintsScope.() -> Unit
) {
    val state = remember { WithConstrainsState() }
    state.children = children
    state.context = ContextAmbient.current
    state.recomposer = currentComposer.recomposer
    state.compositionRef = compositionReference()
    // if this code was executed subcomposition must be triggered as well
    state.forceRecompose = true

    LayoutNode(
        modifier = currentComposer.materialize(modifier),
        ref = state.nodeRef,
        measureBlocks = state.measureBlocks
    )

    // if LayoutNode scheduled the remeasuring no further steps are needed - subcomposition
    // will happen later on the measuring stage. otherwise we can assume the LayoutNode
    // already holds the final Constraints and we should subcompose straight away.
    // if owner is null this means we are not yet attached. once attached the remeasuring
    // will be scheduled which would cause subcomposition
    val layoutNode = state.nodeRef.value!!
    if (!layoutNode.needsRemeasure && layoutNode.owner != null) {
        state.subcompose()
    }
    onDispose {
        state.composition?.dispose()
    }
}

/**
 * Receiver scope being used by the children parameter of [WithConstraints]
 */
@Stable
interface WithConstraintsScope {
    /**
     * The constraints given by the parent layout in pixels.
     *
     * Use [minWidth], [maxWidth], [minHeight] or [maxHeight] if you need value in [Dp].
     */
    val constraints: Constraints
    /**
     * The current [LayoutDirection] to be used by this layout.
     */
    val layoutDirection: LayoutDirection
    /**
     * The minimum width in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val minWidth: Dp
    /**
     * The maximum width in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val maxWidth: Dp
    /**
     * The minimum height in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val minHeight: Dp
    /**
     * The minimum height in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val maxHeight: Dp
}

private class WithConstrainsState {
    lateinit var recomposer: Recomposer
    var compositionRef: CompositionReference? = null
    lateinit var context: Context
    val nodeRef = Ref<LayoutNode>()
    var children: @Composable WithConstraintsScope.() -> Unit = { }
    var forceRecompose = false
    var composition: Composition? = null

    private var scope: WithConstraintsScope = WithConstraintsScopeImpl(
        Density(1f),
        Constraints.fixed(0.ipx, 0.ipx),
        LayoutDirection.Ltr
    )

    val measureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks(
        error = "Intrinsic measurements are not supported by WithConstraints"
    ) {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            val root = nodeRef.value!!
            if (scope.constraints != constraints ||
                scope.layoutDirection != measureScope.layoutDirection ||
                forceRecompose
            ) {
                scope = WithConstraintsScopeImpl(measureScope, constraints, layoutDirection)
                root.ignoreModelReads { subcompose() }
                // if there were models created and read inside this subcomposition
                // and we are going to modify this models within the same frame
                // the composables which read this model will not be recomposed.
                // to make this possible we should switch to the next frame.
                FrameManager.nextFrame()
            }

            // Measure the obtained children and compute our size.
            val layoutChildren = root.children
            var maxWidth: IntPx = constraints.minWidth
            var maxHeight: IntPx = constraints.minHeight
            layoutChildren.fastForEach {
                it.measure(constraints, layoutDirection)
                maxWidth = max(maxWidth, it.width)
                maxHeight = max(maxHeight, it.height)
            }
            maxWidth = min(maxWidth, constraints.maxWidth)
            maxHeight = min(maxHeight, constraints.maxHeight)

            return measureScope.layout(maxWidth, maxHeight) {
                layoutChildren.fastForEach { it.place(IntPx.Zero, IntPx.Zero) }
            }
        }
    }

    fun subcompose() {
        // TODO(b/150390669): Review use of @Untracked
        composition =
            subcomposeInto(context, nodeRef.value!!, recomposer, compositionRef) @Untracked {
                scope.children()
            }
        forceRecompose = false
    }

    private data class WithConstraintsScopeImpl(
        private val density: Density,
        override val constraints: Constraints,
        override val layoutDirection: LayoutDirection
    ) : WithConstraintsScope {
        override val minWidth: Dp
            get() = with(density) { constraints.minWidth.toDp() }
        override val maxWidth: Dp
            get() = with(density) { constraints.maxWidth.toDp() }
        override val minHeight: Dp
            get() = with(density) { constraints.minHeight.toDp() }
        override val maxHeight: Dp
            get() = with(density) { constraints.maxHeight.toDp() }
    }
}