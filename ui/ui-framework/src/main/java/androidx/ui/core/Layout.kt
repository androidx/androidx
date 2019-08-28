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
import androidx.compose.composer
import androidx.compose.ambient
import androidx.compose.compositionReference
import androidx.compose.memo
import androidx.compose.trace
import androidx.compose.unaryPlus

/**
 * Receiver scope for the [ComplexLayout] lambda.
 */
class ComplexLayoutScope internal constructor() {
    internal val layoutNodeRef = Ref<LayoutNode>()
    internal var measureBlock: (MeasureBlockScope.(List<Measurable>, Constraints) -> LayoutResult)? = null
    internal var minIntrinsicWidthBlock: (DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx)? = null
    internal var maxIntrinsicWidthBlock: (DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx)? = null
    internal var minIntrinsicHeightBlock: (DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx)? = null
    internal var maxIntrinsicHeightBlock: (DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx)? = null

    fun measure(measureBlock: MeasureBlockScope.(List<Measurable>, Constraints) -> LayoutResult) {
        this.measureBlock = measureBlock
    }
    fun minIntrinsicWidth(minIntrinsicWidthBlock: DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx) {
        this.minIntrinsicWidthBlock = minIntrinsicWidthBlock
    }
    fun maxIntrinsicWidth(maxIntrinsicWidthBlock: DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx) {
        this.maxIntrinsicWidthBlock = maxIntrinsicWidthBlock
    }
    fun minIntrinsicHeight(minIntrinsicHeightBlock: DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx) {
        this.minIntrinsicHeightBlock = minIntrinsicHeightBlock
    }
    fun maxIntrinsicHeight(maxIntrinsicHeightBlock: DensityReceiver.(List<IntrinsicMeasurable>, IntPx) -> IntPx) {
        this.maxIntrinsicHeightBlock = maxIntrinsicHeightBlock
    }

    internal fun runBlock(block: ComplexLayoutScope.() -> Unit) {
        measureBlock = null
        minIntrinsicWidthBlock = null
        maxIntrinsicWidthBlock = null
        minIntrinsicHeightBlock = null
        maxIntrinsicHeightBlock = null

        block()

        val noLambdaMessage = { subject: String ->
            { "No $subject lambda provided in ComplexLayout" }
        }
        require(measureBlock != null, noLambdaMessage("layout"))
        require(minIntrinsicWidthBlock != null, noLambdaMessage("minIntrinsicWidth"))
        require(maxIntrinsicWidthBlock != null, noLambdaMessage("maxIntrinsicWidth"))
        require(minIntrinsicHeightBlock != null, noLambdaMessage("minIntrinsicHeight"))
        require(maxIntrinsicHeightBlock != null, noLambdaMessage("maxIntrinsicHeight"))
    }
}

/**
 * [ComplexLayout] is the main core component for layout. It can be used to measure and position
 * zero or more children.
 *
 * For a simpler version of [ComplexLayout], unaware of intrinsic measurements, see [Layout].
 * For a widget able to define its content according to the incoming constraints,
 * see [WithConstraints].
 * @see Layout
 * @see WithConstraints
 */
@Composable
fun ComplexLayout(
    children: @Composable() () -> Unit,
    block: ComplexLayoutScope.() -> Unit
) {
    val scope = +memo { ComplexLayoutScope() }

    scope.runBlock(block)
    <LayoutNode
        ref=scope.layoutNodeRef
        measureBlock=scope.measureBlock!!
        minIntrinsicWidthBlock=scope.minIntrinsicWidthBlock!!
        maxIntrinsicWidthBlock=scope.maxIntrinsicWidthBlock!!
        minIntrinsicHeightBlock=scope.minIntrinsicHeightBlock!!
        maxIntrinsicHeightBlock=scope.maxIntrinsicHeightBlock!!
    >
        children()
    </LayoutNode>
}

/**
 * Used to return a fixed sized item for intrinsics measurements in [Layout]
 */
private class DummyPlaceable(override val width: IntPx, override val height: IntPx) : Placeable() {
    override fun get(line: AlignmentLine): IntPx? = null
    override fun place(x: IntPx, y: IntPx) { }
}

/**
 * Identifies an [IntrinsicMeasurable] as a min or max intrinsic measurement.
 */
private enum class IntrinsicMinMax {
    Min, Max
}

/**
 * Identifies an [IntrinsicMeasurable] as a width or height intrinsic measurement.
 */
private enum class IntrinsicWidthHeight {
    Width, Height
}

/**
 * A wrapper around a [Measurable] for intrinsic measurments in [Layout]. Consumers of
 * [Layout] don't identify intrinsic methods, but we can give a reasonable implementation
 * by using the [ComplexLayoutScope.measure], substituting the intrinsics gathering method
 * for the [Measurable.measure] call.
 */
private class DefaultIntrinsic(
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
private class IntrinsicsMeasureBlockScope (
    val layoutNode: LayoutNode,
    val widthHeight: IntrinsicWidthHeight
) : MeasureBlockScope {
    override val density: Density
        get() = layoutNode.density

    var measuredValue: IntPx = IntPx.Zero

    /**
     * Sets the width and height of the current layout. The lambda is used to perform the
     * calls to [Placeable.place], defining the positions of the children relative to the current
     * layout.
     */
    override fun layout(
        width: IntPx,
        height: IntPx,
        vararg alignmentLines: Pair<AlignmentLine, IntPx>,
        positioningBlock: PositioningBlockScope.() -> Unit
    ): LayoutResult {
        measuredValue = if (widthHeight == IntrinsicWidthHeight.Width) width else height
        return LayoutResult.Instance
    }
}

/**
 * A simpler version of [ComplexLayout], intrinsic dimensions do not need to be defined.
 * If a layout of this [Layout] queries the intrinsics, an exception will be thrown.
 * This [Layout] is built using public API on top of [ComplexLayout].
 */
@Composable
fun Layout(
    children: @Composable() () -> Unit,
    measureBlock: MeasureBlockScope.(List<Measurable>, Constraints) -> LayoutResult
) {
    trace("UI:Layout") {
        ComplexLayout(children) {
            measure(measureBlock)

            minIntrinsicWidth { measurables, h ->
                val mapped = measurables.map {
                    DefaultIntrinsic(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
                }
                val constraints = Constraints(maxHeight = h)
                val layoutReceiver = IntrinsicsMeasureBlockScope(layoutNodeRef.value!!, IntrinsicWidthHeight.Width)
                layoutReceiver.measureBlock(mapped, constraints)
                layoutReceiver.measuredValue
            }

            maxIntrinsicWidth { measurables, h ->
                val mapped = measurables.map {
                    DefaultIntrinsic(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
                }
                val constraints = Constraints(maxHeight = h)
                val layoutReceiver = IntrinsicsMeasureBlockScope(layoutNodeRef.value!!, IntrinsicWidthHeight.Width)
                layoutReceiver.measureBlock(mapped, constraints)
                layoutReceiver.measuredValue
            }

            minIntrinsicHeight { measurables, w ->
                val mapped = measurables.map {
                    DefaultIntrinsic(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
                }
                val constraints = Constraints(maxWidth = w)
                val layoutReceiver = IntrinsicsMeasureBlockScope(layoutNodeRef.value!!, IntrinsicWidthHeight.Height)
                layoutReceiver.measureBlock(mapped, constraints)
                layoutReceiver.measuredValue
            }

            maxIntrinsicHeight { measurables, w ->
                val mapped = measurables.map {
                    DefaultIntrinsic(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
                }
                val constraints = Constraints(maxWidth = w)
                val layoutReceiver = IntrinsicsMeasureBlockScope(layoutNodeRef.value!!, IntrinsicWidthHeight.Height)
                layoutReceiver.measureBlock(mapped, constraints)
                layoutReceiver.measuredValue
            }
        }
    }
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
class MultiComposableMeasurables internal constructor(private val layoutNode: LayoutNode):
    List<Measurable> by layoutNode.layoutChildren {
    /**
     * When multiple [Composable] children are passed into `Layout`, the [Measurable]s
     * for each [Composable] can be retrieved using this method.
     *
     * @sample androidx.ui.framework.samples.LayoutVarargsUsage
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

/**
 * Temporary component that allows composing and indexing measurables of multiple composables.
 * The logic here will be moved back to Layout, which will accept vararg children argument.
 * TODO(popam): remove this when the new syntax is available
 * With the new syntax, the API should support both:
 * Layout(children) { measurables, constraints ->
 *     val placeables = measurables.map { it.measure(...) }
 *     ...
 * }
 * and
 * Layout(header, cardContent, footer) { measurables, constraints ->
 *     val headerPlaceables = measurables[header].map { it.measure(...) }
 *     val cardContentPlaceables = measurables[cardContent].map { ... }
 *     val footerPlaceables = measurables[footer].map { ... }
 *     ...
 * }
 */
@Composable
fun Layout(
    vararg childrenArray: @Composable() () -> Unit,
    measureBlock: MeasureBlockScope.(MultiComposableMeasurables, Constraints) -> LayoutResult
) {
    val children: @Composable() () -> Unit = if (childrenArray.isEmpty()) {
        EmptyComposable
    } else {
        @Composable {
            childrenArray.forEach { childrenComposable ->
                <DataNode key=ChildGroupKey value=childrenComposable>
                    childrenComposable()
                </DataNode>
            }
        }
    }

    Layout(children) { _, constraints ->
        measureBlock(MultiComposableMeasurables(this as LayoutNode), constraints)
    }
}

/**
 * A widget that defines its own content according to the available space, based on the incoming
 * constraints. Example usage:
 *
 * WithConstraints { constraints ->
 *     if (constraints.maxWidth < 100.ipx) {
 *         Icon()
 *     } else {
 *         Row {
 *             Icon()
 *             IconDescription()
 *          }
 *     }
 * }
 *
 * The widget will compose the given children, and will position the resulting layout widgets
 * in a parent [Layout]. The widget will be as small as possible such that it can fit its
 * children. If the composition yields multiple layout children, these will be all placed at the
 * top left of the WithConstraints, so consider wrapping them in an additional common
 * parent if different positioning is preferred.
 *
 * Please note that using this widget might be a performance hit, so please use with care.
 */
@Composable
fun WithConstraints(children: @Composable() (Constraints) -> Unit) {
    val ref = +compositionReference()
    val context = +ambient(ContextAmbient)

    ComplexLayout(children = {}) {
        measure { _, constraints ->
            val root = layoutNodeRef.value!!
            // Start subcomposition from the current node.
            Compose.composeInto(
                root,
                context,
                ref
            ) {
                children(p1 = constraints)
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

            layout(maxWidth, maxHeight) {
                layoutChildren.forEach { placeable ->
                    placeable.place(IntPx.Zero, IntPx.Zero)
                }
            }
        }
        minIntrinsicWidth { _, _ -> throw UnsupportedOperationException("intrinsics aren't supported") }
        maxIntrinsicWidth { _, _ -> throw UnsupportedOperationException("intrinsics aren't supported") }
        minIntrinsicHeight { _, _ -> throw UnsupportedOperationException("intrinsics aren't supported") }
        maxIntrinsicHeight { _, _ -> throw UnsupportedOperationException("intrinsics aren't supported") }
    }
}

/**
 * [onPositioned] callback will be called with the final LayoutCoordinates of the parent
 * MeasureBox after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 *     Column {
 *         Item1()
 *         Item2()
 *         OnPositioned (onPositioned={ coordinates ->
 *             // This coordinates contain bounds of the Column within it's parent Layout.
 *             // Store it if you want to use it later f.e. when a touch happens.
 *         })
 *     }
 */
@Composable
fun OnPositioned(
    onPositioned: (coordinates: LayoutCoordinates) -> Unit
) {
    <DataNode key=OnPositionedKey value=onPositioned/>
}

/**
 * [onPositioned] callback will be called with the final LayoutCoordinates of the children
 * MeasureBox(es) after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 *     OnChildPositioned (onPositioned={ coordinates ->
 *         // This coordinates contain bounds of the Item within it's parent Layout.
 *         // Store it if you want to use it later f.e. when a touch happens.
 *     }) {
 *         Item()
 *     }
 * }
 */
@Composable
fun OnChildPositioned(
    onPositioned: (coordinates: LayoutCoordinates) -> Unit,
    children: @Composable() () -> Unit
) {
    <DataNode key=OnChildPositionedKey value=onPositioned>
        <children/>
    </DataNode>
}
