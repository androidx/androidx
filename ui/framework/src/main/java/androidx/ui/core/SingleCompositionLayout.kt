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

import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.onCommit
import com.google.r4a.unaryPlus

internal typealias LayoutBlock =
        SingleCompositionLayoutBlockReceiver.(List<Measurable>, Constraints) -> Unit
internal typealias IntrinsicMeasurementBlock =
        SingleCompositionIntrinsicMeasurementsReceiver.(List<Measurable>, IntPx) -> IntPx
internal val LayoutBlockStub: LayoutBlock = { _, _ -> }
internal val IntrinsicMeasurementBlockStub: IntrinsicMeasurementBlock = { _, _ -> 0.ipx }

internal class ComplexLayoutState(
    internal var layoutBlock: LayoutBlock = LayoutBlockStub,
    internal var minIntrinsicWidthBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal var maxIntrinsicWidthBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal var minIntrinsicHeightBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal var maxIntrinsicHeightBlock: IntrinsicMeasurementBlock = IntrinsicMeasurementBlockStub,
    internal val density: Density
) : Measurable, Placeable({ _, _ -> }), InternalMeasurable {
    init {
        // TODO(popam): redefine API of Placeable when ComplexMeasureBox is gone
        placeBlock = { x: IntPx, y: IntPx ->
            moveTo(x, y)
            placeChildren()
        }
    }

    internal var positioningBlock: PositioningBlockReceiver.() -> Unit = {}

    internal val layoutBlockReceiver = SingleCompositionLayoutBlockReceiver(this)
    internal val intrinsicMeasurementsReceiver =
        SingleCompositionIntrinsicMeasurementsReceiver(this)
    internal val positioningBlockReceiver = PositioningBlockReceiver()

    internal val layoutNodeRef = Ref<LayoutNode>()
    internal val layoutNode: LayoutNode
        get() = layoutNodeRef.value!!

    private val childrenMeasurables: List<Measurable>
        get() = layoutNode.childrenMeasureBoxes().map {
            when (it) {
                is ComplexLayoutState -> it as Measurable
                is ComplexMeasureBox -> MeasurableImpl(it)
                else -> error("Invalid child type in ComplexLayoutState#childrenMeasurables")
            }
        }

    internal val onChildPositioned = mutableListOf<(LayoutCoordinates) -> Unit>()
    internal val onPositioned = mutableListOf<(LayoutCoordinates) -> Unit>()

    override fun measure(constraints: Constraints): Placeable {
        layoutNode.constraints = constraints
        childrenMeasurables.forEach {
            when (it) {
                is ComplexLayoutState -> it.layoutNode.visible = false
                is MeasurableImpl -> it.measureBox.layoutNode.visible = false
            }
        }
        layoutBlockReceiver.layoutBlock(childrenMeasurables, constraints)
        return this
    }

    override fun minIntrinsicWidth(h: IntPx) =
        minIntrinsicWidthBlock(intrinsicMeasurementsReceiver, childrenMeasurables, h)

    override fun maxIntrinsicWidth(h: IntPx) =
        maxIntrinsicWidthBlock(intrinsicMeasurementsReceiver, childrenMeasurables, h)

    override fun minIntrinsicHeight(w: IntPx) =
        minIntrinsicHeightBlock(intrinsicMeasurementsReceiver, childrenMeasurables, w)

    override fun maxIntrinsicHeight(w: IntPx) =
        maxIntrinsicHeightBlock(intrinsicMeasurementsReceiver, childrenMeasurables, w)

    internal fun placeChildren() {
        layoutNode.visible = true
        positioningBlockReceiver.apply { positioningBlock() }
        dispatchOnPositionedCallbacks()
    }

    private fun dispatchOnPositionedCallbacks() {
        // There are two types of callbacks:
        // a) when the MeasureBox is positioned - `onPositioned`
        // b) when the child of the MeasureBox is positioned - `onChildPositioned`
        // To create LayoutNodeCoordinates only once here we will call callbacks from
        // both `onPositioned` and our parent MeasureBox's `onChildPositioned`.
        val parentMeasureBox = layoutNode.parentLayoutNode?.measureBox
        val parentOnChildPositioned = when (parentMeasureBox) {
            is ComplexLayoutState -> parentMeasureBox.onChildPositioned
            is ComplexMeasureBox -> parentMeasureBox.onChildPositioned
            else -> null
        }
        if (onPositioned.isNotEmpty() || !parentOnChildPositioned.isNullOrEmpty()) {
            val coordinates = LayoutNodeCoordinates(layoutNode)
            onPositioned.forEach { it.invoke(coordinates) }
            parentOnChildPositioned?.forEach { it.invoke(coordinates) }
        }
    }

    internal fun resize(width: IntPx, height: IntPx) {
        layoutNode.resize(width, height)
    }

    private fun moveTo(x: IntPx, y: IntPx) {
        layoutNode.moveTo(x, y)
    }

    override val width: IntPx get() = layoutNode.width
    override val height: IntPx get() = layoutNode.height
}

/**
 * [ComplexMeasureBox] which composes its children during its own composition, so the tree of
 * component nodes will be built in one composition pass. Since it composes its children
 * non-lazily, the children composable has to be known beforehand, which is the case for most
 * use cases. TODO(popam): add support for subcompositions
 * TODO(popam): improve this doc when the component is finalized
 */
@Composable
fun ComplexLayout(
    layoutBlock: LayoutBlock,
    minIntrinsicWidthBlock: IntrinsicMeasurementBlock,
    maxIntrinsicWidthBlock: IntrinsicMeasurementBlock,
    minIntrinsicHeightBlock: IntrinsicMeasurementBlock,
    maxIntrinsicHeightBlock: IntrinsicMeasurementBlock,
    @Children children: () -> Unit
) {
    val density = +ambientDensity()
    val complexMeasureBox = +memo { ComplexLayoutState(density = density) }
    complexMeasureBox.apply {
        this.layoutBlock = layoutBlock
        this.minIntrinsicWidthBlock = minIntrinsicWidthBlock
        this.maxIntrinsicWidthBlock = maxIntrinsicWidthBlock
        this.minIntrinsicHeightBlock = minIntrinsicHeightBlock
        this.maxIntrinsicHeightBlock = maxIntrinsicHeightBlock
    }

    +onCommit {
        complexMeasureBox.layoutNode.requestLayout()
    }

    <LayoutNode ref=complexMeasureBox.layoutNodeRef measureBox=complexMeasureBox>
        <OnChildPositionedAmbient.Provider value=complexMeasureBox.onChildPositioned>
            <OnPositionedAmbient.Provider value=complexMeasureBox.onPositioned>
                <children />
            </OnPositionedAmbient.Provider>
        </OnChildPositionedAmbient.Provider>
    </LayoutNode>
}

/**
 * Receiver scope for [SingleCompositionComplexMeasureBoxReceiver] intrinsic measurements lambdas.
 */
class SingleCompositionIntrinsicMeasurementsReceiver internal constructor(
    private val measureBox: ComplexLayoutState
) : DensityReceiver {
    override val density: Density
        get() = measureBox.density
    fun Measurable.minIntrinsicWidth(h: IntPx) =
        (this as InternalMeasurable).minIntrinsicWidth(h)
    fun Measurable.maxIntrinsicWidth(h: IntPx) =
        (this as InternalMeasurable).maxIntrinsicWidth(h)
    fun Measurable.minIntrinsicHeight(w: IntPx) =
        (this as InternalMeasurable).minIntrinsicHeight(w)
    fun Measurable.maxIntrinsicHeight(w: IntPx) =
        (this as InternalMeasurable).maxIntrinsicHeight(w)
}

/**
 * Receiver scope for [SingleCompositionComplexMeasureBoxReceiver#layout]'s lambda.
 */
class SingleCompositionLayoutBlockReceiver internal constructor(
    private val complexMeasureBox: ComplexLayoutState
) : DensityReceiver {
    override val density: Density
        get() = complexMeasureBox.density

    fun Measurable.measure(constraints: Constraints): Placeable {
        this as InternalMeasurable
        return this.measure(constraints)
    }
    fun layoutResult(
        width: IntPx,
        height: IntPx,
        block: PositioningBlockReceiver.() -> Unit
    ) {
        complexMeasureBox.resize(width, height)
        complexMeasureBox.positioningBlock = block
    }
    fun Measurable.minIntrinsicWidth(h: IntPx) =
        (this as InternalMeasurable).minIntrinsicWidth(h)
    fun Measurable.maxIntrinsicWidth(h: IntPx) =
        (this as InternalMeasurable).maxIntrinsicWidth(h)
    fun Measurable.minIntrinsicHeight(w: IntPx) =
        (this as InternalMeasurable).minIntrinsicHeight(w)
    fun Measurable.maxIntrinsicHeight(w: IntPx) =
        (this as InternalMeasurable).maxIntrinsicHeight(w)
}

/**
 * A simpler version of [ComplexMeasureBox], intrinsic dimensions do not need to be defined.
 * If a layout of this [MeasureBox] queries the intrinsics, an exception will be thrown.
 * This [MeasureBox] is built using public API on top of [ComplexMeasureBox].
 */
@Composable
fun Layout(
    layoutBlock: SingleCompositionMeasureBoxReceiver
        .(measurables: List<Measurable>, constraints: Constraints) -> Unit,
    @Children children: () -> Unit
) {
    val complexLayoutBlock: LayoutBlock = { measurables, constraints: Constraints ->
        val measureBoxReceiver = SingleCompositionMeasureBoxReceiver(
            { m, c -> m.measure(c) }, /* measure lambda */
            this::layoutResult,
            density
        )
        measureBoxReceiver.layoutBlock(measurables, constraints)
    }

    val minIntrinsicWidthBlock: IntrinsicMeasurementBlock = { measurables, h ->
        var intrinsicWidth = IntPx.Zero
        val measureBoxReceiver = SingleCompositionMeasureBoxReceiver({ m, c ->
            val width = m.minIntrinsicWidth(c.minHeight)
            DummyPlaceable(width, h)
        }, { width, _, _ -> intrinsicWidth = width }, density)
        val constraints = Constraints.tightConstraintsForHeight(h)
        layoutBlock(measureBoxReceiver, measurables, constraints)
        intrinsicWidth
    }

    val maxIntrinsicWidthBlock: IntrinsicMeasurementBlock = { measurables, h ->
        var intrinsicWidth = IntPx.Zero
        val measureBoxReceiver = SingleCompositionMeasureBoxReceiver({ m, c ->
            val width = m.maxIntrinsicWidth(c.minHeight)
            DummyPlaceable(width, h)
        }, { width, _, _ -> intrinsicWidth = width }, density)
        val constraints = Constraints.tightConstraintsForHeight(h)
        layoutBlock(measureBoxReceiver, measurables, constraints)
        intrinsicWidth
    }

    val minIntrinsicHeightBlock: IntrinsicMeasurementBlock = { measurables, w ->
        var intrinsicHeight = IntPx.Zero
        val measureBoxReceiver = SingleCompositionMeasureBoxReceiver({ m, c ->
            val height = m.minIntrinsicHeight(c.minWidth)
            DummyPlaceable(w, height)
        }, { _, height, _ -> intrinsicHeight = height }, density)
        val constraints = Constraints.tightConstraintsForWidth(w)
        layoutBlock(measureBoxReceiver, measurables, constraints)
        intrinsicHeight
    }
    val maxIntrinsicHeightBlock: IntrinsicMeasurementBlock = { measurables, w ->
        var intrinsicHeight = IntPx.Zero
        val measureBoxReceiver = SingleCompositionMeasureBoxReceiver({ m, c ->
            val height = m.maxIntrinsicHeight(c.minWidth)
            DummyPlaceable(w, height)
        }, { _, height, _ -> intrinsicHeight = height }, density)
        val constraints = Constraints.tightConstraintsForWidth(w)
        layoutBlock(measureBoxReceiver, measurables, constraints)
        intrinsicHeight
    }
    <ComplexLayout
        layoutBlock=complexLayoutBlock
        minIntrinsicWidthBlock
        maxIntrinsicWidthBlock
        minIntrinsicHeightBlock
        maxIntrinsicHeightBlock
        children />
}

/**
 * Receiver scope for the lambda of [Layout].
 * Used to mask away intrinsics inside [Layout].
 */
class SingleCompositionMeasureBoxReceiver internal constructor(
    private val complexMeasure: (Measurable, Constraints) -> Placeable,
    private val complexLayoutResult: (IntPx, IntPx, PositioningBlockReceiver.() -> Unit) -> Unit,
    override val density: Density
) : DensityReceiver {
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
    fun layout(width: IntPx, height: IntPx, block: PositioningBlockReceiver.() -> Unit) {
        complexLayoutResult(width, height, block)
    }
}
