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

import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.CompositionReference
import androidx.compose.Context
import androidx.compose.FrameManager
import androidx.compose.compositionReference
import androidx.compose.remember
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.max
import androidx.ui.unit.min

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
 * @sample androidx.ui.framework.samples.LayoutWithProvidedIntrinsicsUsage
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
    children: @Composable() () -> Unit,
    /*crossinline*/
    minIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    /*crossinline*/
    minIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    /*crossinline*/
    maxIntrinsicWidthMeasureBlock: IntrinsicMeasureBlock,
    /*crossinline*/
    maxIntrinsicHeightMeasureBlock: IntrinsicMeasureBlock,
    modifier: Modifier = Modifier.None,
    /*crossinline*/
    measureBlock: MeasureBlock
) {
    val measureBlocks = object : LayoutNode.MeasureBlocks {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ) = measureScope.measureBlock(measurables, constraints)
        override fun minIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ) = density.minIntrinsicWidthMeasureBlock(measurables, h)
        override fun minIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ) = density.minIntrinsicHeightMeasureBlock(measurables, w)
        override fun maxIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ) = density.maxIntrinsicWidthMeasureBlock(measurables, h)
        override fun maxIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ) = density.maxIntrinsicHeightMeasureBlock(measurables, w)
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
 * @sample androidx.ui.framework.samples.LayoutUsage
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
    children: @Composable() () -> Unit,
    modifier: Modifier = Modifier.None,
    /*noinline*/
    measureBlock: MeasureBlock
) {

    val measureBlocks = remember(measureBlock) { MeasuringIntrinsicsMeasureBlocks(measureBlock) }
    Layout(children, measureBlocks, modifier)
}

/*@PublishedApi*/ @Composable internal /*inline*/ fun Layout(
    /*crossinline*/
    children: @Composable() () -> Unit,
    measureBlocks: LayoutNode.MeasureBlocks,
    modifier: Modifier
) {
    LayoutNode(modifier = modifier, measureBlocks = measureBlocks) {
        children()
    }
}

/**
 * Used to return a fixed sized item for intrinsics measurements in [Layout]
 */
private class DummyPlaceable(width: IntPx, height: IntPx) : Placeable() {
    override fun get(line: AlignmentLine): IntPx? = null
    override val size = IntPxSize(width, height)
    override fun performPlace(position: IntPxPosition) { }
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

    override fun measure(constraints: Constraints): Placeable {
        if (widthHeight == IntrinsicWidthHeight.Width) {
            val width = if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicWidth(constraints.maxHeight)
            } else {
                measurable.minIntrinsicWidth(constraints.maxHeight)
            }
            return DummyPlaceable(width, constraints.maxHeight)
        }
        val height = if (minMax == IntrinsicMinMax.Max) {
            measurable.maxIntrinsicHeight(constraints.maxWidth)
        } else {
            measurable.minIntrinsicHeight(constraints.maxWidth)
        }
        return DummyPlaceable(constraints.maxWidth, height)
    }

    override fun minIntrinsicWidth(height: IntPx): IntPx {
        return measurable.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: IntPx): IntPx {
        return measurable.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: IntPx): IntPx {
        return measurable.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: IntPx): IntPx {
        return measurable.maxIntrinsicHeight(width)
    }
}

/**
 * Receiver scope for [Layout]'s layout lambda when used in an intrinsics call.
 */
@PublishedApi
internal class IntrinsicsMeasureScope(
    density: Density
) : MeasureScope(), Density by density {
    // TODO(popam): clean this up and prevent measuring inside intrinsics
}

/**
 * Default [LayoutNode.MeasureBlocks] object implementation, providing intrinsic measurements
 * that use the measure block replacing the measure calls with intrinsic measurement calls.
 */
@PublishedApi internal fun MeasuringIntrinsicsMeasureBlocks(measureBlock: MeasureBlock) =
    object : LayoutNode.MeasureBlocks {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ) = measureScope.measureBlock(measurables, constraints)
        override fun minIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ) = density.MeasuringMinIntrinsicWidth(measureBlock, measurables, h)
        override fun minIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ) = density.MeasuringMinIntrinsicHeight(measureBlock, measurables, w)
        override fun maxIntrinsicWidth(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            h: IntPx
        ) = density.MeasuringMaxIntrinsicWidth(measureBlock, measurables, h)
        override fun maxIntrinsicHeight(
            density: Density,
            measurables: List<IntrinsicMeasurable>,
            w: IntPx
        ) = density.MeasuringMaxIntrinsicHeight(measureBlock, measurables, w)

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
    measureBlock: MeasureBlock,
    measurables: List<IntrinsicMeasurable>,
    h: IntPx
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
    }
    val constraints = Constraints(maxHeight = h)
    val layoutReceiver = IntrinsicsMeasureScope(this)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.width
}

/**
 * Default implementation for the min intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMinIntrinsicHeight(
    measureBlock: MeasureBlock,
    measurables: List<IntrinsicMeasurable>,
    w: IntPx
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
    }
    val constraints = Constraints(maxWidth = w)
    val layoutReceiver = IntrinsicsMeasureScope(this)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.height
}

/**
 * Default implementation for the max intrinsic width of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMaxIntrinsicWidth(
    measureBlock: MeasureBlock,
    measurables: List<IntrinsicMeasurable>,
    h: IntPx
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
    }
    val constraints = Constraints(maxHeight = h)
    val layoutReceiver = IntrinsicsMeasureScope(this)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.width
}

/**
 * Default implementation for the max intrinsic height of a layout. This works by running the
 * measure block with measure calls replaced with intrinsic measurement calls.
 */
private inline fun Density.MeasuringMaxIntrinsicHeight(
    measureBlock: MeasureBlock,
    measurables: List<IntrinsicMeasurable>,
    w: IntPx
): IntPx {
    val mapped = measurables.map {
        DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
    }
    val constraints = Constraints(maxWidth = w)
    val layoutReceiver = IntrinsicsMeasureScope(this)
    val layoutResult = layoutReceiver.measureBlock(mapped, constraints)
    return layoutResult.height
}

/**
 * DataNodeKey for grouped child composables. When multiple composables are
 * passed to a `Layout`, they are identified by a `ChildGroupKey` with
 * the value of the composable. The direct children of the LayoutNode must be
 * a DataNodeKey with ChildGroupKey if they are provided.
 */
private val ChildGroupKey = DataNodeKey<@Composable() () -> Unit>("Compose: ChildGroup")

private val EmptyComposable: @Composable() () -> Unit = @Composable {}

/**
 * A [List] of [Measurable]s passed in as the argument to the [Layout] `measureBlock` when
 * using the `vararg` variant.
 */
class MultiComposableMeasurables internal constructor(private val layoutNode: LayoutNode) :
    List<Measurable> by layoutNode.layoutChildren {
    /**
     * When multiple [Composable] children are passed into `Layout`, the [Measurable]s
     * for each [Composable] can be retrieved using this method.
     */
    operator fun get(children: @Composable() () -> Unit): List<Measurable> {
        if (isEmpty()) return emptyList()
        for (i in 0 until layoutNode.count) {
            val child = layoutNode[i] as DataNode<*>
            if (child.key !== ChildGroupKey) {
                throw IllegalStateException("Malformed Layout. Must be a ChildGroupKey")
            }
            if (child.value === children) {
                val group = mutableListOf<LayoutNode>()
                child.visitLayoutChildren { node -> group += node }
                return group
            }
        }
        return emptyList()
    }
}

typealias MultiMeasureBlock =
        MeasureScope.(MultiComposableMeasurables, Constraints) -> MeasureScope.LayoutResult

/**
 * Temporary component that allows composing and indexing measurables of multiple composables.
 * The logic here will be moved back to Layout, which will accept vararg children argument.
 */
@Deprecated("Please use the LayoutTagParentData API instead.")
@Composable
fun Layout(
    vararg childrenArray: @Composable() () -> Unit,
    modifier: Modifier = Modifier.None,
    measureBlock: MultiMeasureBlock
) {
    val children: @Composable() () -> Unit = if (childrenArray.isEmpty()) {
        EmptyComposable
    } else {
        @Composable {
            childrenArray.forEach { childrenComposable ->
                DataNode(key = ChildGroupKey, value = childrenComposable) {
                    childrenComposable()
                }
            }
        }
    }

    Layout(children, modifier) { _, constraints ->
        measureBlock(
            MultiComposableMeasurables((this as LayoutNode.InnerMeasureScope).layoutNode),
            constraints
        )
    }
}

/**
 * A composable that defines its own content according to the available space, based on the incoming
 * constraints. Example usage:
 * @sample androidx.ui.framework.samples.WithConstraintsSample
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
    modifier: Modifier = Modifier.None,
    children: @Composable() (Constraints) -> Unit
) {
    val state = remember { WithConstrainsState() }
    state.children = children
    state.context = ContextAmbient.current
    state.compositionRef = compositionReference()
    // if this code was executed subcomposition must be triggered as well
    state.forceRecompose = true

    LayoutNode(modifier = modifier, ref = state.nodeRef, measureBlocks = state.measureBlocks)

    // if LayoutNode scheduled the remeasuring no further steps are needed - subcomposition
    // will happen later on the measuring stage. otherwise we can assume the LayoutNode
    // already holds the final Constraints and we should subcompose straight away.
    // if owner is null this means we are not yet attached. once attached the remeasuring
    // will be scheduled which would cause subcomposition
    val layoutNode = state.nodeRef.value!!
    if (!layoutNode.needsRemeasure && layoutNode.owner != null) {
        state.subcompose()
    }
}

private class WithConstrainsState {
    var compositionRef: CompositionReference? = null
    var context: Context? = null
    val nodeRef = Ref<LayoutNode>()
    var lastConstraints: Constraints? = null
    var children: @Composable() (Constraints) -> Unit = {}
    var forceRecompose = false
    val measureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks(
        error = "Intrinsic measurements are not supported by WithConstraints"
    ) {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureScope.LayoutResult {
            val root = nodeRef.value!!
            if (lastConstraints != constraints || forceRecompose) {
                lastConstraints = constraints
                root.ignoreModelReads { subcompose() }
                // if there were models created and read inside this subcomposition
                // and we are going to modify this models within the same frame
                // the composables which read this model will not be recomposed.
                // to make this possible we should switch to the next frame.
                FrameManager.nextFrame()
            }

            // Measure the obtained children and compute our size.
            val layoutChildren = root.layoutChildren
            var maxWidth: IntPx = constraints.minWidth
            var maxHeight: IntPx = constraints.minHeight
            layoutChildren.forEach {
                it.measure(constraints)
                maxWidth = max(maxWidth, it.width)
                maxHeight = max(maxHeight, it.height)
            }
            maxWidth = min(maxWidth, constraints.maxWidth)
            maxHeight = min(maxHeight, constraints.maxHeight)

            return measureScope.layout(maxWidth, maxHeight) {
                layoutChildren.forEach { it.place(IntPx.Zero, IntPx.Zero) }
            }
        }
    }

    fun subcompose() {
        val node = nodeRef.value!!
        val constraints = lastConstraints!!
        Compose.subcomposeInto(node, context!!, compositionRef) {
            children(constraints)
        }
        forceRecompose = false
    }
}

/**
 * [onPositioned] callback will be called with the final LayoutCoordinates of the parent
 * MeasureBox after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.ui.framework.samples.OnPositionedSample
 */
@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun OnPositioned(
    noinline onPositioned: (coordinates: LayoutCoordinates) -> Unit
) {
    DataNode(key = OnPositionedKey, value = onPositioned)
}

/**
 * [onPositioned] callback will be called with the final LayoutCoordinates of the children
 * MeasureBox(es) after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.ui.framework.samples.OnChildPositionedSample
 */
@Composable
inline fun OnChildPositioned(
    noinline onPositioned: (coordinates: LayoutCoordinates) -> Unit,
    crossinline children: @Composable() () -> Unit
) {
    DataNode(key = OnChildPositionedKey, value = onPositioned) {
        children()
    }
}
