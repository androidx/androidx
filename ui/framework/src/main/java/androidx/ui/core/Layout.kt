/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.ui.core.MeasuredPlaceable.Companion.place
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.R4a
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.onCommit
import com.google.r4a.unaryPlus

// Temporary interface for MeasurableImpl and ComplexLayoutState, while we need to support
// both MeasureBox and Layout. TODO(popam): remove this when MeasureBox goes away
internal interface InternalMeasurable {
    fun measure(constraints: Constraints): Placeable
    fun minIntrinsicWidth(h: IntPx): IntPx
    fun maxIntrinsicWidth(h: IntPx): IntPx
    fun minIntrinsicHeight(w: IntPx): IntPx
    fun maxIntrinsicHeight(w: IntPx): IntPx
}

/**
 * A part of the composition that can be measured. This represents a [ComplexMeasureBox] somewhere
 * down the hierarchy.
 *
 * @return a [Placeable] that can be used within a [layoutResult] block
 */
// TODO(mount): Make this an inline class when private constructors are possible
internal class MeasurableImpl internal constructor(internal val measureBox: ComplexMeasureBox) :
    Measurable, InternalMeasurable {
    override val parentData: Any?
        get() = measureBox.layoutNode.parentData

    private fun runBlock() {
        measureBox.minIntrinsicWidthBlock = ComplexMeasureBox.IntrinsicMeasurementStub
        measureBox.maxIntrinsicWidthBlock = ComplexMeasureBox.IntrinsicMeasurementStub
        measureBox.minIntrinsicHeightBlock = ComplexMeasureBox.IntrinsicMeasurementStub
        measureBox.maxIntrinsicHeightBlock = ComplexMeasureBox.IntrinsicMeasurementStub

        measureBox.runBlock()

        if (measureBox.minIntrinsicWidthBlock == ComplexMeasureBox.IntrinsicMeasurementStub ||
            measureBox.maxIntrinsicWidthBlock == ComplexMeasureBox.IntrinsicMeasurementStub ||
            measureBox.minIntrinsicHeightBlock == ComplexMeasureBox.IntrinsicMeasurementStub ||
            measureBox.maxIntrinsicHeightBlock == ComplexMeasureBox.IntrinsicMeasurementStub
        ) {
            throw IllegalStateException(
                "ComplexMeasureBox $measureBox should provide" +
                        "implementations for all min and max, width and height intrinsic" +
                        "measurements"
            )
        }
    }

    override fun measure(constraints: Constraints): Placeable {
        runBlock()
        measureBox.measure(constraints)
        return MeasuredPlaceable(measureBox)
    }

    override fun minIntrinsicWidth(h: IntPx): IntPx {
        runBlock()
        return measureBox.minIntrinsicWidthBlock(measureBox.intrinsicMeasurementsReceiver, h)
    }

    override fun maxIntrinsicWidth(h: IntPx): IntPx {
        runBlock()
        return measureBox.maxIntrinsicWidthBlock(measureBox.intrinsicMeasurementsReceiver, h)
    }

    override fun minIntrinsicHeight(w: IntPx): IntPx {
        runBlock()
        return measureBox.minIntrinsicHeightBlock(measureBox.intrinsicMeasurementsReceiver, w)
    }

    override fun maxIntrinsicHeight(w: IntPx): IntPx {
        runBlock()
        return measureBox.maxIntrinsicHeightBlock(measureBox.intrinsicMeasurementsReceiver, w)
    }
}

/**
 * A measured [ComplexMeasureBox] from [Measurable.measure]. The [place] call can be made within
 * [ComplexMeasureOperations.layout]. In the future, this will only be accessible within the
 * [layoutResult] call.
 */
internal class MeasuredPlaceable internal constructor(
    private val complexMeasureBox: ComplexMeasureBox
) : Placeable({ x, y -> place(complexMeasureBox, x, y) }) {
    override val width = complexMeasureBox.layoutNode.width
    override val height = complexMeasureBox.layoutNode.height

    companion object {
        internal fun place(complexMeasureBox: ComplexMeasureBox, x: IntPx, y: IntPx) {
            complexMeasureBox.moveTo(x, y)
            complexMeasureBox.placeChildren()
        }
    }
}

internal class DummyPlaceable(override val width: IntPx, override val height: IntPx) :
    Placeable({ _, _ -> })

/**
 * ComplexMeasureBox is a tag that can be used to measure and position zero or more children.
 * The ComplexMeasureBox accepts a [Constraints] argument to use to determine its measurements.
 * For example, to get several children to draw on top of each other:
 *     @Composable fun Stack(@Children children: () -> Unit) {
 *         <ComplexMeasureBox>
 *             val measurables = collect(children)
 *             layout { constraints ->
 *                 val placeables = measurables.map { it.measure(constraints) }
 *                 val width = placeables.maxBy { it.width }
 *                 val height = placeables.maxBy { it.height }
 *                 layoutResult(width, height) {
 *                     placeables.forEach { placeable ->
 *                         placeable.place((width - placeable.width) / 2,
 *                                         (height - placeable.height) / 2))
 *                     }
 *                 }
 *             }
 *             minIntrinsicWidth { h -> measurables.map { it.minIntrinsicWidth(h) }.max() }
 *             maxIntrinsicWidth { h -> measurables.map { it.maxIntrinsicWidth(h) }.max() }
 *             minIntrinsicHeight { w -> measurables.map { it.minIntrinsicHeight(w) }.max() }
 *             maxIntrinsicHeight { w -> measurables.map { it.maxIntrinsicHeight(w) }.max() }
 *         </ComplexMeasureBox>
 *     }
 *
 * Until receiver scopes work, [ComplexMeasureOperations], a lambda performing measurement
 * and a lambda computing intrinsics are passed as extra arguments, to reflect the scopes where
 * they will be available.
 *
 * For a simpler version of [ComplexMeasureBox], see [MeasureBox]. [MeasureBox] is essentially
 * a [ComplexMeasureBox], but unaware of intrinsic measurements, therefore leading to a slightly
 * simpler API. [MeasureBox] is the recommended way of building custom layouts if your layout
 * hierarchy is not using intrinsic dimensions, or if the default behavior provided by [MeasureBox]
 * for its own intrinsic measurements is correct for your layout. To compute its own intrinsic
 * dimensions, [MeasureBox] reuses the lambda provided for the regular measurement and layout, but
 * replaces any occurence of [Measurable.measure] with a suitable intrinsic dimension calculation
 * of the child.
 * @see [MeasureBox]
 */
@Deprecated(
    message = "Please use androidx.ui.core.ComplexLayout instead when possible.",
    replaceWith = ReplaceWith("androidx.ui.core.ComplexLayout")
)
class ComplexMeasureBox(
    @Children(composable = false) var block: ComplexMeasureBoxReceiver.() -> Unit
) : Component() {
    internal var layoutBlock: LayoutBlockReceiver.(Constraints) -> Unit = { }
    internal var positioningBlock: PositioningBlockReceiver.() -> Unit = {}
    internal var minIntrinsicWidthBlock = IntrinsicMeasurementStub
    internal var maxIntrinsicWidthBlock = IntrinsicMeasurementStub
    internal var minIntrinsicHeightBlock = IntrinsicMeasurementStub
    internal var maxIntrinsicHeightBlock = IntrinsicMeasurementStub
    internal val complexMeasureBoxReceiver = ComplexMeasureBoxReceiver(this)
    internal val layoutBlockReceiver = LayoutBlockReceiver(this)
    internal val intrinsicMeasurementsReceiver = IntrinsicMeasurementsReceiver(this)
    internal val positioningBlockReceiver = PositioningBlockReceiver()
    private val ref = Ref<LayoutNode>()
    internal val layoutNode: LayoutNode get() = ref.value!!
    internal var ambients: Ambient.Reference? = null
    internal val onPositioned = mutableListOf<(LayoutCoordinates) -> Unit>()
    internal val onChildPositioned = mutableListOf<(LayoutCoordinates) -> Unit>()
    internal var density: Density = Density(0f)

    override fun compose() {
        <Ambient.Portal> reference ->
            ambients = reference
            density = reference.getAmbient(DensityAmbient)
            val parentData = reference.getAmbient(ParentDataAmbient)
            <LayoutNode ref layout=this parentData/>
        </Ambient.Portal>
        if (recomposeComplexMeasureBox == null) {
            recomposeComplexMeasureBox = this
            runBlock()
            measure(layoutNode.constraints)
            recomposeComplexMeasureBox = null
            placeChildren()
        }
    }

    internal fun runBlock() {
        complexMeasureBoxReceiver.clear()
        complexMeasureBoxReceiver.block()
    }

    internal fun measure(constraints: Constraints) {
        layoutNode.constraints = constraints
        layoutBlockReceiver.apply { layoutBlock(constraints) }
    }

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
        val parentMeasureBox = layoutNode.parentLayoutNode?.layout
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

    internal fun moveTo(x: IntPx, y: IntPx) {
        layoutNode.moveTo(x, y)
    }

    internal fun resize(width: IntPx, height: IntPx) {
        layoutNode.resize(width, height)
    }

    companion object {
        // Default stub for intrinsic measurements blocks.
        internal val IntrinsicMeasurementStub: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx =
            { throw NotImplementedError() }
    }
}

internal var recomposeComplexMeasureBox: ComplexMeasureBox? = null

@DslMarker private annotation class LayoutDsl

/**
 * Receiver scope for [ComplexMeasureBox]'s child lambda.
 */
@LayoutDsl
class ComplexMeasureBoxReceiver internal constructor(
    private val measureBox: ComplexMeasureBox
) : DensityReceiver {
    override val density
        get() = measureBox.density

    /**
     * We store previously provided composable children to allow multiple calls
     * of collect() function during the one measuring pass.
     * Example:
     * val header = collect(headerChildren)
     * val body = collect(bodyChildren)
     */
    private val collectedComposables = mutableListOf<() -> Unit>()

    /**
     * Compose [children] into the [ComplexMeasureBox] and return a list of [Measurable]s within
     * the children. Composition stops at [ComplexMeasureBox] children. Further composition requires
     * calling [Measurable.measure].
     */
    // TODO(popam): prevent collect from happening before every intrinsic measurement
    fun collect(@Children children: () -> Unit): List<Measurable> {
        val layoutNode = measureBox.layoutNode
        val boxesSoFar = if (collectedComposables.isEmpty()) 0 else
            layoutNode.childrenLayouts().size
        collectedComposables.add(children)

        val ambients = measureBox.ambients!!
        R4a.composeInto(layoutNode, ambients.getAmbient(ContextAmbient), ambients) {
            <OnChildPositionedAmbient.Provider value=measureBox.onChildPositioned>
                <OnPositionedAmbient.Provider value=measureBox.onPositioned>
                    <ParentDataAmbient.Provider value=null>
                        collectedComposables.forEach { children -> <children /> }
                    </ParentDataAmbient.Provider>
                </OnPositionedAmbient.Provider>
            </OnChildPositionedAmbient.Provider>
        }

        return layoutNode.childrenLayouts()
            .drop(boxesSoFar)
            .map { measureBox ->
                when (measureBox) {
                    is ComplexMeasureBox -> {
                        measureBox.layoutNode.visible = false
                        MeasurableImpl(measureBox) as Measurable
                    }
                    is ComplexLayoutState -> {
                        measureBox.layoutNode.visible = false
                        measureBox as Measurable
                    }
                    else -> error("Invalid ComplexMeasureBox child found")
                }
            }
    }

    /**
     * Define the layout of the current layout. The block is aware of constraints, intrinsic
     * dimensions and is able to measure and position its children.
     */
    fun layout(block: LayoutBlockReceiver.(Constraints) -> Unit) {
        measureBox.layoutBlock = block
    }

    /**
     * Set the min intrinsic width of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun minIntrinsicWidth(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        measureBox.minIntrinsicWidthBlock = block
    }

    /**
     * Set the max intrinsic width of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun maxIntrinsicWidth(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        measureBox.maxIntrinsicWidthBlock = block
    }

    /**
     * Set the min intrinsic height of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun minIntrinsicHeight(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        measureBox.minIntrinsicHeightBlock = block
    }

    /**
     * Set the max intrinsic height of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun maxIntrinsicHeight(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        measureBox.maxIntrinsicHeightBlock = block
    }

    internal fun clear() {
        collectedComposables.clear()
    }
}

/**
 * Receiver scope for [ComplexMeasureBoxReceiver] intrinsic measurements lambdas.
 */
@LayoutDsl
class IntrinsicMeasurementsReceiver internal constructor(
    private val measureBox: ComplexMeasureBox
) : DensityReceiver {
    override val density: Density
        get() = measureBox.density
    fun collect(@Children children: () -> Unit) =
        measureBox.complexMeasureBoxReceiver.collect(children)
    fun Measurable.minIntrinsicWidth(h: IntPx) = (this as InternalMeasurable).minIntrinsicWidth(h)
    fun Measurable.minIntrinsicHeight(w: IntPx) = (this as InternalMeasurable).minIntrinsicHeight(w)
    fun Measurable.maxIntrinsicWidth(h: IntPx) = (this as InternalMeasurable).maxIntrinsicWidth(h)
    fun Measurable.maxIntrinsicHeight(w: IntPx) = (this as InternalMeasurable).maxIntrinsicHeight(w)
}

/**
 * Receiver scope for [ComplexMeasureBoxReceiver#layout]'s lambda.
 */
@LayoutDsl
class LayoutBlockReceiver internal constructor(
    private val complexMeasureBox: ComplexMeasureBox
) : DensityReceiver {
    override val density: Density
        get() = complexMeasureBox.density

    fun Measurable.measure(constraints: Constraints): Placeable {
        this as InternalMeasurable
        return measure(constraints)
    }
    fun layoutResult(
        width: IntPx,
        height: IntPx,
        block: PositioningBlockReceiver.() -> Unit
    ) {
        complexMeasureBox.resize(width, height)
        complexMeasureBox.positioningBlock = block
    }
    fun collect(@Children children: () -> Unit) =
        complexMeasureBox.complexMeasureBoxReceiver.collect(children)
    fun Measurable.minIntrinsicWidth(h: IntPx) = (this as InternalMeasurable).minIntrinsicWidth(h)
    fun Measurable.minIntrinsicHeight(w: IntPx) = (this as InternalMeasurable).minIntrinsicHeight(w)
    fun Measurable.maxIntrinsicWidth(h: IntPx) = (this as InternalMeasurable).maxIntrinsicWidth(h)
    fun Measurable.maxIntrinsicHeight(w: IntPx) = (this as InternalMeasurable).maxIntrinsicHeight(w)
}

/**
 * A simpler version of [ComplexMeasureBox], intrinsic dimensions do not need to be defined.
 * If a layout of this [MeasureBox] queries the intrinsics, an exception will be thrown.
 * This [MeasureBox] is built using public API on top of [ComplexMeasureBox].
 */
@Deprecated(
    message = "Please use androidx.ui.core.Layout instead when possible.",
    replaceWith = ReplaceWith("androidx.ui.core.Layout")
)
@Composable
class MeasureBox(
    @Children(composable = false) var block: MeasureBoxReceiver.(constraints: Constraints) -> Unit
) : Component() {
    override fun compose() {
        <ComplexMeasureBox>
            val density = density

            layout { constraints ->
                val measureBoxReceiver = MeasureBoxReceiver(
                    this::collect,
                    { m, c -> m.measure(c) }, /* measure lambda */
                    this::layoutResult,
                    density
                )
                measureBoxReceiver.apply { block(constraints) }
            }

            minIntrinsicWidth { h ->
                var intrinsicWidth = IntPx.Zero
                val measureBoxReceiver = MeasureBoxReceiver(this::collect, { m, c ->
                    val width = m.minIntrinsicWidth(c.minHeight)
                    DummyPlaceable(width, h)
                }, { width, _, _ -> intrinsicWidth = width }, density)
                val constraints = Constraints.tightConstraintsForHeight(h)
                block(measureBoxReceiver, constraints)
                intrinsicWidth
            }
            maxIntrinsicWidth { h ->
                var intrinsicWidth = IntPx.Zero
                val measureBoxReceiver = MeasureBoxReceiver(this::collect, { m, c ->
                    val width = m.maxIntrinsicWidth(c.minHeight)
                    DummyPlaceable(width, h)
                }, { width, _, _ -> intrinsicWidth = width }, density)
                val constraints = Constraints.tightConstraintsForHeight(h)
                block(measureBoxReceiver, constraints)
                intrinsicWidth
            }
            minIntrinsicHeight { w ->
                var intrinsicHeight = IntPx.Zero
                val measureBoxReceiver = MeasureBoxReceiver(this::collect, { m, c ->
                    val height = m.minIntrinsicHeight(c.minWidth)
                    DummyPlaceable(w, height)
                }, { _, height, _ -> intrinsicHeight = height }, density)
                val constraints = Constraints.tightConstraintsForWidth(w)
                block(measureBoxReceiver, constraints)
                intrinsicHeight
            }
            maxIntrinsicHeight { w ->
                var intrinsicHeight = IntPx.Zero
                val measureBoxReceiver = MeasureBoxReceiver(this::collect, { m, c ->
                    val height = m.maxIntrinsicHeight(c.minWidth)
                    DummyPlaceable(w, height)
                }, { _, height, _ -> intrinsicHeight = height }, density)
                val constraints = Constraints.tightConstraintsForWidth(w)
                block(measureBoxReceiver, constraints)
                intrinsicHeight
            }
        </ComplexMeasureBox>
    }
}

/**
 * Receiver scope for the lambda of [MeasureBox].
 * Used to mask away intrinsics inside [MeasureBox].
 */
class MeasureBoxReceiver internal constructor(
    private val complexCollect: (() -> Unit) -> List<Measurable>,
    private val complexMeasure: (Measurable, Constraints) -> Placeable,
    private val complexLayoutResult: (IntPx, IntPx, PositioningBlockReceiver.() -> Unit) -> Unit,
    override val density: Density
) : DensityReceiver {
    /**
     * Compose [children] into the [MeasureBox] and return a list of [Measurable]s within
     * the children. Composition stops at [MeasureBox] children. Further composition requires
     * calling [Measurable.measure].
     */
    fun collect(@Children children: () -> Unit): List<Measurable> = complexCollect(children)

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

internal val OnPositionedAmbient =
    Ambient.of<MutableList<(LayoutCoordinates) -> Unit>>()

internal val OnChildPositionedAmbient =
    Ambient.of<MutableList<(LayoutCoordinates) -> Unit>>()

/**
 * [onPositioned] callback will be called with the final LayoutCoordinates of the parent
 * MeasureBox after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 *     <Column>
 *         <Item1/>
 *         <Item2/>
 *         <OnPositioned onPositioned={ coordinates ->
 *             // This coordinates contain bounds of the Column within it's parent Layout.
 *             // Store it if you want to use it later f.e. when a touch happens.
 *         } />
 *     </Column>
 */
@Composable
fun OnPositioned(
    onPositioned: (coordinates: LayoutCoordinates) -> Unit
) {
    val coordinatesCallbacks = +ambient(OnPositionedAmbient)
    +onCommit(onPositioned) {
        coordinatesCallbacks.add(onPositioned)
        onDispose {
            coordinatesCallbacks.remove(onPositioned)
        }
    }
}

/**
 * [onPositioned] callback will be called with the final LayoutCoordinates of the children
 * MeasureBox(es) after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 *     <OnChildPositioned onPositioned={ coordinates ->
 *         // This coordinates contain bounds of the Item within it's parent Layout.
 *         // Store it if you want to use it later f.e. when a touch happens.
 *     } >
 *         <Item/>
 *     </OnChildPositioned>
 * </Column>
 */
@Composable
fun OnChildPositioned(
    onPositioned: (coordinates: LayoutCoordinates) -> Unit,
    @Children children: () -> Unit
) {
    val coordinatesCallbacks = +ambient(OnChildPositionedAmbient)
    +onCommit(onPositioned) {
        coordinatesCallbacks.add(onPositioned)
        onDispose {
            coordinatesCallbacks.remove(onPositioned)
        }
    }
    <children/>
}
