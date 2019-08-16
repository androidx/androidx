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
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.memo
import androidx.compose.onPreCommit
import androidx.compose.trace
import androidx.compose.unaryPlus

internal typealias LayoutBlock =
        LayoutBlockReceiver.(List<Measurable>, Constraints) -> LayoutResult
internal typealias ComplexLayoutBlock =
        ComplexLayoutBlockReceiver.(List<Measurable>, Constraints) -> LayoutResult
internal typealias IntrinsicMeasurementBlock =
        IntrinsicMeasurementReceiver.(List<Measurable>, IntPx) -> IntPx

internal val ComplexLayoutBlockStub: ComplexLayoutBlock = { _, _ -> LayoutResult.instance }
internal val IntrinsicMeasurementBlockStub: IntrinsicMeasurementBlock = { _, _ -> 0.ipx }

/**
 * Object returned from [LayoutBlockReceiver.layout] and [ComplexLayoutBlockReceiver.layoutResult]
 * to deliver proof the method was called in [LayoutBlock] and [ComplexLayoutBlock], respectively.
 */
class LayoutResult private constructor() {
    companion object {
        internal val instance = LayoutResult()
    }
}

internal class ComplexLayoutState(
    internal var layoutBlock: ComplexLayoutBlock = ComplexLayoutBlockStub,
    internal var minIntrinsicWidthBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal var maxIntrinsicWidthBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal var minIntrinsicHeightBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal var maxIntrinsicHeightBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal val density: Density
) : Measurable, Placeable(), MeasurableLayout {
    override val parentData: Any?
        get() {
            var node = layoutNode.parent
            val parentLayoutNode = layoutNode.parentLayoutNode
            while (node != null && node !== parentLayoutNode) {
                if (node is DataNode<*> && node.key === ParentDataKey) {
                    return node.value
                }
                node = node.parent
            }
            return null
        }

    internal var block: ComplexLayoutReceiver.() -> Unit = {}
    internal var positioningBlock: PositioningBlockReceiver.() -> Unit = {}

    internal val layoutBlockReceiver = ComplexLayoutBlockReceiver(this)
    internal val intrinsicMeasurementsReceiver =
        IntrinsicMeasurementReceiver(this)
    internal val positioningBlockReceiver = PositioningBlockReceiver()

    internal val layoutNodeRef = Ref<LayoutNode>()
    internal val layoutNode: LayoutNode
        get() = layoutNodeRef.value!!

    // TODO(mount/popam): This is inefficient
    internal val childrenMeasurables: List<Measurable>
        get() = ComplexLayoutStateMeasurablesList(layoutNode)

    private var measureIteration = 0L

    override fun callMeasure(constraints: Constraints) { measure(constraints) }
    override fun callLayout() {
        placeChildren()
    }

    fun measure(constraints: Constraints): Placeable {
        val owner = layoutNode.owner
        val iteration = if (owner == null) 0L else owner.measureIteration
        if (measureIteration == iteration) {
            throw IllegalStateException("measure() may not be called multiple times " +
                    "on the same Measurable")
        }
        measureIteration = iteration
        if (layoutNode.constraints == constraints && !layoutNode.needsRemeasure) {
            layoutNode.resize(layoutNode.width, layoutNode.height)
            return this // we're already measured to this size, don't do anything
        }
        layoutNode.startMeasure()
        layoutNode.constraints = constraints
        layoutBlockReceiver.layoutBlock(childrenMeasurables, constraints)
        layoutNode.endMeasure()
        return this
    }

    fun minIntrinsicWidth(h: IntPx) =
        minIntrinsicWidthBlock(intrinsicMeasurementsReceiver, childrenMeasurables, h)

    fun maxIntrinsicWidth(h: IntPx) =
        maxIntrinsicWidthBlock(intrinsicMeasurementsReceiver, childrenMeasurables, h)

    fun minIntrinsicHeight(w: IntPx) =
        minIntrinsicHeightBlock(intrinsicMeasurementsReceiver, childrenMeasurables, w)

    fun maxIntrinsicHeight(w: IntPx) =
        maxIntrinsicHeightBlock(intrinsicMeasurementsReceiver, childrenMeasurables, w)

    internal fun placeChildren() {
        if (layoutNode.needsRelayout) {
            layoutNode.startLayout()
            positioningBlockReceiver.apply { positioningBlock() }
            dispatchOnPositionedCallbacks()
            layoutNode.endLayout()
        }
    }

    private fun dispatchOnPositionedCallbacks() {
        // There are two types of callbacks:
        // a) when the Layout is positioned - `onPositioned`
        // b) when the child of the Layout is positioned - `onChildPositioned`
        // To create LayoutNodeCoordinates only once here we will call callbacks from
        // both `onPositioned` and 'onChildPositioned'.
        val coordinates = LayoutNodeCoordinates(layoutNode)
        walkOnPosition(layoutNode, coordinates)
        walkOnChildPositioned(layoutNode, coordinates)
    }

    internal fun resize(width: IntPx, height: IntPx) {
        layoutNode.resize(width, height)
    }

    private fun moveTo(x: IntPx, y: IntPx) {
        layoutNode.moveTo(x, y)
    }

    override val width: IntPx get() = layoutNode.width
    override val height: IntPx get() = layoutNode.height
    override fun place(x: IntPx, y: IntPx) {
        moveTo(x, y)
        placeChildren()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun walkOnPosition(node: ComponentNode, coordinates: LayoutCoordinates) {
            node.visitChildren { child ->
                if (child !is LayoutNode) {
                    if (child is DataNode<*> && child.key === OnPositionedKey) {
                        val method = child.value as (LayoutCoordinates) -> Unit
                        method(coordinates)
                    }
                    walkOnPosition(child, coordinates)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun walkOnChildPositioned(layoutNode: LayoutNode, coordinates: LayoutCoordinates) {
            var node = layoutNode.parent
            while (node != null && node !is LayoutNode) {
                if (node is DataNode<*> && node.key === OnChildPositionedKey) {
                    val method = node.value as (LayoutCoordinates) -> Unit
                    method(coordinates)
                }
                node = node.parent
            }
        }
    }
}

internal class ComplexLayoutStateMeasurablesList(
    layoutNode: LayoutNode
) : MutableList<Measurable> by mutableListOf() {
    private val composableRanges = mutableMapOf<@Composable() () -> Unit, IntRange>()
    fun composables(composable: @Composable() () -> Unit): List<Measurable> {
        val range = composableRanges[composable] ?: return emptyList()
        return subList(range.first, range.last)
    }

    init {
        // var start = 0 doesn't work because of IR
        val start = intArrayOf(0)
        layoutNode.visitLayoutChildren { child ->
            val layout = child.layout
            if (layout != null) {
                layout as Measurable
                val parentData = layout.parentData
                if (parentData is ChildrenEndParentData) {
                    composableRanges[parentData.children] = IntRange(start[0], size)
                    start[0] = size
                } else {
                    this += layout
                }
            }
        }
    }
}

/**
 * Receiver scope for the [ComplexLayout] lambda.
 */
class ComplexLayoutReceiver internal constructor(internal val layoutState: ComplexLayoutState) {
    fun measure(layoutBlock: ComplexLayoutBlock) {
        layoutState.layoutBlock = layoutBlock
    }
    fun minIntrinsicWidth(minIntrinsicWidthBlock: IntrinsicMeasurementBlock) {
        layoutState.minIntrinsicWidthBlock = minIntrinsicWidthBlock
    }
    fun maxIntrinsicWidth(maxIntrinsicWidthBlock: IntrinsicMeasurementBlock) {
        layoutState.maxIntrinsicWidthBlock = maxIntrinsicWidthBlock
    }
    fun minIntrinsicHeight(minIntrinsicHeightBlock: IntrinsicMeasurementBlock) {
        layoutState.minIntrinsicHeightBlock = minIntrinsicHeightBlock
    }
    fun maxIntrinsicHeight(maxIntrinsicHeightBlock: IntrinsicMeasurementBlock) {
        layoutState.maxIntrinsicHeightBlock = maxIntrinsicHeightBlock
    }

    internal fun runBlock(block: ComplexLayoutReceiver.() -> Unit) {
        layoutState.layoutBlock = ComplexLayoutBlockStub
        layoutState.minIntrinsicWidthBlock = IntrinsicMeasurementBlockStub
        layoutState.maxIntrinsicWidthBlock = IntrinsicMeasurementBlockStub
        layoutState.minIntrinsicHeightBlock = IntrinsicMeasurementBlockStub
        layoutState.maxIntrinsicHeightBlock = IntrinsicMeasurementBlockStub

        block()

        val noLambdaMessage = { subject: String ->
            { "No $subject lambda provided in ComplexLayout" }
        }
        require(layoutState.layoutBlock != ComplexLayoutBlockStub, noLambdaMessage("layout"))
        require(
            layoutState.minIntrinsicWidthBlock != IntrinsicMeasurementBlockStub,
            noLambdaMessage("minIntrinsicWidth")
        )
        require(
            layoutState.maxIntrinsicWidthBlock != IntrinsicMeasurementBlockStub,
            noLambdaMessage("maxIntrinsicWidth")
        )
        require(
            layoutState.minIntrinsicHeightBlock != IntrinsicMeasurementBlockStub,
            noLambdaMessage("minIntrinsicHeight")
        )
        require(
            layoutState.maxIntrinsicHeightBlock != IntrinsicMeasurementBlockStub,
            noLambdaMessage("maxIntrinsicHeight")
        )
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
    block: ComplexLayoutReceiver.() -> Unit
) {
    val density = +ambientDensity()
    val layoutState = +memo { ComplexLayoutState(density = density) }
    layoutState.block = block

    +onPreCommit {
        layoutState.layoutNode.requestRemeasure()
    }

    <LayoutNode ref = layoutState.layoutNodeRef layout = layoutState>
        children()
    </LayoutNode>
    ComplexLayoutReceiver(layoutState).runBlock(block)
}

/**
 * Receiver scope for [ComplexLayout]'s intrinsic measurements lambdas.
 */
class IntrinsicMeasurementReceiver internal constructor(
    internal val layoutState: ComplexLayoutState
) : DensityReceiver {
    override val density: Density
        get() = layoutState.density
    fun Measurable.minIntrinsicWidth(h: IntPx) =
        (this as ComplexLayoutState).minIntrinsicWidth(h)
    fun Measurable.maxIntrinsicWidth(h: IntPx) =
        (this as ComplexLayoutState).maxIntrinsicWidth(h)
    fun Measurable.minIntrinsicHeight(w: IntPx) =
        (this as ComplexLayoutState).minIntrinsicHeight(w)
    fun Measurable.maxIntrinsicHeight(w: IntPx) =
        (this as ComplexLayoutState).maxIntrinsicHeight(w)
}

/**
 * Receiver scope for [ComplexLayout]'s layout lambda.
 */
class ComplexLayoutBlockReceiver internal constructor(
    internal val layoutState: ComplexLayoutState
) : DensityReceiver {
    override val density: Density
        get() = layoutState.density

    fun Measurable.measure(constraints: Constraints): Placeable {
        this as ComplexLayoutState
        return this.measure(constraints)
    }
    fun layout(
        width: IntPx,
        height: IntPx,
        positioningBlock: PositioningBlockReceiver.() -> Unit
    ): LayoutResult {
        layoutState.resize(width, height)
        layoutState.positioningBlock = positioningBlock
        return LayoutResult.instance
    }
    fun Measurable.minIntrinsicWidth(h: IntPx) =
        (this as ComplexLayoutState).minIntrinsicWidth(h)
    fun Measurable.maxIntrinsicWidth(h: IntPx) =
        (this as ComplexLayoutState).maxIntrinsicWidth(h)
    fun Measurable.minIntrinsicHeight(w: IntPx) =
        (this as ComplexLayoutState).minIntrinsicHeight(w)
    fun Measurable.maxIntrinsicHeight(w: IntPx) =
        (this as ComplexLayoutState).maxIntrinsicHeight(w)
}

internal class DummyPlaceable(override val width: IntPx, override val height: IntPx) : Placeable() {
    override fun place(x: IntPx, y: IntPx) { }
}

// Measure lambda used in Layout
private val LayoutMeasure = { m: Measurable, c: Constraints -> (m as ComplexLayoutState).measure(c) }

/**
 * A simpler version of [ComplexLayout], intrinsic dimensions do not need to be defined.
 * If a layout of this [Layout] queries the intrinsics, an exception will be thrown.
 * This [Layout] is built using public API on top of [ComplexLayout].
 */
@Composable
fun Layout(
    children: @Composable() () -> Unit,
    measureBlock: LayoutBlock
) {
    trace("UI:Layout") {
        ComplexLayout(children = children, block = {
            val layoutReceiver = LayoutBlockReceiver(
                layoutState,
                LayoutMeasure, /* measure lambda */
                { _, _, _ -> }
            )
            measure { measurables, constraints ->
                layoutReceiver.complexMeasure = LayoutMeasure
                layoutReceiver.complexLayoutResult = { w, h, block -> layout(w, h, block) }
                layoutReceiver.measureBlock(measurables, constraints)
            }

            minIntrinsicWidth { measurables, h ->
                var intrinsicWidth = IntPx.Zero
                layoutReceiver.complexMeasure = { m, c ->
                    val width = m.minIntrinsicWidth(c.maxHeight)
                    DummyPlaceable(width, h)
                }
                layoutReceiver.complexLayoutResult = { width, _, _ -> intrinsicWidth = width }
                val constraints = Constraints(maxHeight = h)
                layoutReceiver.measureBlock(measurables, constraints)
                intrinsicWidth
            }

            maxIntrinsicWidth { measurables, h ->
                var intrinsicWidth = IntPx.Zero
                layoutReceiver.complexMeasure = { m, c ->
                    val width = m.maxIntrinsicWidth(c.maxHeight)
                    DummyPlaceable(width, h)
                }
                layoutReceiver.complexLayoutResult = { width, _, _ -> intrinsicWidth = width }
                val constraints = Constraints(maxHeight = h)
                layoutReceiver.measureBlock(measurables, constraints)
                intrinsicWidth
            }

            minIntrinsicHeight { measurables, w ->
                var intrinsicHeight = IntPx.Zero
                layoutReceiver.complexMeasure = { m, c ->
                    val height = m.minIntrinsicHeight(c.maxWidth)
                    DummyPlaceable(w, height)
                }
                layoutReceiver.complexLayoutResult = { _, height, _ -> intrinsicHeight = height }
                val constraints = Constraints(maxWidth = w)
                layoutReceiver.measureBlock(measurables, constraints)
                intrinsicHeight
            }

            maxIntrinsicHeight { measurables, w ->
                var intrinsicHeight = IntPx.Zero
                layoutReceiver.complexMeasure = { m, c ->
                    val height = m.maxIntrinsicHeight(c.maxWidth)
                    DummyPlaceable(w, height)
                }
                layoutReceiver.complexLayoutResult = { _, height, _ -> intrinsicHeight = height }
                val constraints = Constraints(maxWidth = w)
                layoutReceiver.measureBlock(measurables, constraints)
                intrinsicHeight
            }
        })
    }
}

/**
 * Used by multi child [Layout] as parent data for the dummy [Layout] instances that mark
 * the end of the [Measurable]s sequence corresponding to a particular child.
 */
internal data class ChildrenEndParentData(val children: @Composable() () -> Unit)

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
    measureBlock: LayoutBlock
) {
    val ChildrenEndMarker = @Composable { children: @Composable() () -> Unit ->
        ParentData(data = ChildrenEndParentData(children)) {
            Layout(measureBlock={_, _ -> layout(0.ipx, 0.ipx){}}, children = {})
        }
    }
    val children = @Composable {
        val addMarkers = childrenArray.size > 1
        childrenArray.forEach { childrenComposable ->
            childrenComposable()
            if (addMarkers) ChildrenEndMarker(childrenComposable)
        }
    }

    Layout(measureBlock = measureBlock, children = children)
}

/**
 * Receiver scope for [Layout]'s layout lambda.
 * Used to mask away intrinsics inside [Layout].
 */
class LayoutBlockReceiver internal constructor(
    internal val layoutState: ComplexLayoutState,
    internal var complexMeasure: (Measurable, Constraints) -> Placeable,
    internal var complexLayoutResult: (IntPx, IntPx, PositioningBlockReceiver.() -> Unit) -> Unit
) : DensityReceiver {
    override val density: Density
        get() = layoutState.density

    /**
     * Returns all the [Measurable]s emitted for a particular children lambda.
     * TODO(popam): finding measurables for each individual composable is O(n^2), consider improving
     */
    operator fun List<Measurable>.get(children: @Composable() () -> Unit): List<Measurable> {
        if (this !is ComplexLayoutStateMeasurablesList) error("Invalid list of measurables")

        return composables(children)
    }
    /**
     * Measure the child [Measurable] with a specific set of [Constraints]. The result
     * is a [Placeable], which can be used inside the [layout] method to position the child.
     */
    fun Measurable.measure(constraints: Constraints): Placeable = complexMeasure(this, constraints)
    /**
     * Sets the width and height of the current layout. The lambda is used to perform the
     * calls to [Placeable.place], defining the positions of the children relative to the current
     * layout.
     */
    fun layout(
        width: IntPx,
        height: IntPx,
        positioningBlock: PositioningBlockReceiver.() -> Unit
    ): LayoutResult {
        complexLayoutResult(width, height, positioningBlock)
        return LayoutResult.instance
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

    Layout(children = {}) { _, constraints ->
        val root = layoutState.layoutNode
        // Start subcomposition from the current node.
        Compose.composeInto(
            root,
            context,
            ref
        ) {
            children(p1 = constraints)
        }

        // Measure the obtained children and compute our size.
        val measurables = layoutState.childrenMeasurables
        val placeables = measurables.map { it.measure(constraints) }
        val layoutSize = constraints.constrain(IntPxSize(
            placeables.map { it.width }.maxBy { it.value } ?: IntPx.Zero,
            placeables.map { it.height }.maxBy { it.value } ?: IntPx.Zero
        ))

        layout(layoutSize.width, layoutSize.height) {
            placeables.forEach { placeable ->
                placeable.place(IntPx.Zero, IntPx.Zero)
            }
        }
    }
}

private val OnPositionedKey = DataNodeKey<(LayoutCoordinates) -> Unit>("Compose:OnPositioned")
private val OnChildPositionedKey =
    DataNodeKey<(LayoutCoordinates) -> Unit>("Compose:OnChildPositioned")

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
