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

import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.R4a

/**
 * A part of the composition that can be measured. This represents a [ComplexMeasureBox] somewhere
 * down the hierarchy.
 *
 * @return a [Placeable] that can be used within a [layoutResult] block
 */
// TODO(mount): Make this an inline class when private constructors are possible
internal class MeasurableImpl internal constructor(private val measureBox: ComplexMeasureBox) :
    Measurable {
    private fun runBlock() {
        measureBox.minIntrinsicWidthBlock = ComplexMeasureBox.IntrinsicMeasurementStub
        measureBox.maxIntrinsicWidthBlock = ComplexMeasureBox.IntrinsicMeasurementStub
        measureBox.minIntrinsicHeightBlock = ComplexMeasureBox.IntrinsicMeasurementStub
        measureBox.maxIntrinsicHeightBlock = ComplexMeasureBox.IntrinsicMeasurementStub

        measureBox.runBlock()

        if (measureBox.minIntrinsicWidthBlock == ComplexMeasureBox.IntrinsicMeasurementStub
            || measureBox.maxIntrinsicWidthBlock == ComplexMeasureBox.IntrinsicMeasurementStub
            || measureBox.minIntrinsicHeightBlock == ComplexMeasureBox.IntrinsicMeasurementStub
            || measureBox.maxIntrinsicHeightBlock == ComplexMeasureBox.IntrinsicMeasurementStub) {
            throw IllegalStateException("ComplexMeasureBox $measureBox should provide" +
                    "implementations for all min and max, width and height intrinsic" +
                    "measurements")
        }
    }

    internal fun measure(constraints: Constraints): Placeable {
        runBlock()
        measureBox.measure(constraints)
        return MeasuredPlaceable(measureBox)
    }

    internal fun minIntrinsicWidth(h: Px): Int {
        runBlock()
        return measureBox.minIntrinsicWidthBlock(h, IntrinsicMeasureOperations)
    }

    internal fun maxIntrinsicWidth(h: Px): Int {
        runBlock()
        return measureBox.maxIntrinsicWidthBlock(h, IntrinsicMeasureOperations)
    }

    internal fun minIntrinsicHeight(w: Px): Int {
        runBlock()
        return measureBox.minIntrinsicHeightBlock(w, IntrinsicMeasureOperations)
    }

    internal fun maxIntrinsicHeight(w: Px): Int {
        runBlock()
        return measureBox.maxIntrinsicHeightBlock(w, IntrinsicMeasureOperations)
    }
}

/**
 * A measured [ComplexMeasureBox] from [Measurable.measure]. The [place] call can be made within
 * [ComplexMeasureOperations.layout]. In the future, this will only be accessible within the
 * [layoutResult] call.
 */
internal class MeasuredPlaceable internal constructor(
    private val complexMeasureBox: ComplexMeasureBox
) : Placeable({ x, y -> place(complexMeasureBox, x, y)}) {
    override val width = complexMeasureBox.layoutNode.width
    override val height = complexMeasureBox.layoutNode.height
    companion object {
        internal fun place(complexMeasureBox: ComplexMeasureBox, x: Int, y: Int) {
            complexMeasureBox.moveTo(x, y)
            complexMeasureBox.placeChildren()
        }
    }
}

internal class DummyPlaceable(override val width: Int, override val height: Int)
    : Placeable({ _, _ -> })

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
class ComplexMeasureBox(@Children(composable = false) var block:
                            (complexMeasureOperations: ComplexMeasureOperations) -> Unit)
    : Component() {
    internal var layoutBlock: (
        Constraints,
        (Measurable, Constraints) -> Placeable,
        IntrinsicMeasureOperations,
        (Int, Int, () -> Unit) -> Unit
    ) -> Unit = { _, _ , _, _ -> }
    internal var positioningBlock: () -> Unit = {}
    internal var minIntrinsicWidthBlock = IntrinsicMeasurementStub
    internal var maxIntrinsicWidthBlock = IntrinsicMeasurementStub
    internal var minIntrinsicHeightBlock = IntrinsicMeasurementStub
    internal var maxIntrinsicHeightBlock = IntrinsicMeasurementStub
    private val ref = Ref<LayoutNode>()
    internal val layoutNode: LayoutNode get() = ref.value!!
    internal var ambients: Ambient.Reference? = null
    internal val coordinatesCallbacks = mutableListOf<(LayoutCoordinates) -> Unit>()
    internal var density: Density = Density(0f)

    override fun compose() {
        <Ambient.Portal> reference ->
            ambients = reference
            <DensityConsumer> density ->
                this.density = density
            </DensityConsumer>
            <LayoutNode ref measureBox=this />
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
        val measureOperations = ComplexMeasureOperations(this)
        block(measureOperations)
    }

    internal fun measure(constraints: Constraints) {
        layoutNode.constraints = constraints
        layoutBlock(
            constraints,
            { measurable, constraints ->
                measurable as MeasurableImpl
                measurable.measure(constraints)
            } /* measure lambda */,
            IntrinsicMeasureOperations,
            { width, height, positioningBlock -> /* layoutResult lambda */
                resize(width, height)
                this.positioningBlock = positioningBlock
            }
        )
    }

    internal fun placeChildren() {
        layoutNode.visible = true
        positioningBlock()
        if (coordinatesCallbacks.isNotEmpty()) {
            val coordinates = LayoutNodeCoordinates(layoutNode)
            coordinatesCallbacks.forEach { it.invoke(coordinates) }
        }
    }

    internal fun moveTo(x: Int, y: Int) {
        layoutNode.moveTo(x, y)
    }

    internal fun resize(width: Int, height: Int) {
        layoutNode.resize(width, height)
    }

    companion object {
        // Default stub for intrinsic measurements blocks.
        internal val IntrinsicMeasurementStub: (Px, IntrinsicMeasureOperations) -> Int
            = { _, _ -> throw NotImplementedError() }
    }
}

/**
 * Temporary class to pass intrinsic measurement methods in the appropriate scopes.
 */
// TODO(popam): remove this when receiver scopes for lambdas are available
object IntrinsicMeasureOperations {
    fun minIntrinsicWidth(m: Measurable, h: Px) = (m as MeasurableImpl).minIntrinsicWidth(h)
    fun maxIntrinsicWidth(m: Measurable, h: Px) = (m as MeasurableImpl).maxIntrinsicWidth(h)
    fun minIntrinsicHeight(m: Measurable, w: Px) =
        (m as MeasurableImpl).minIntrinsicHeight(w)

    fun maxIntrinsicHeight(m: Measurable, w: Px) =
        (m as MeasurableImpl).maxIntrinsicHeight(w)
}

internal var recomposeComplexMeasureBox: ComplexMeasureBox? = null

/**
 * Receiver scope for [ComplexMeasureBox]'s child lambda.
 */
class ComplexMeasureOperations internal constructor(
    private val complexMeasureBox: ComplexMeasureBox
) {
    // TODO(mount/popam): Remove this when we have reciever scopes
    val density: Density get() = complexMeasureBox.density

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
        val layoutNode = complexMeasureBox.layoutNode
        val boxesSoFar = if (collectedComposables.isEmpty()) 0 else
            layoutNode.childrenMeasureBoxes().size
        collectedComposables.add(children)

        val ambients = complexMeasureBox.ambients!!
        R4a.composeInto(layoutNode, ambients.getAmbient(ContextAmbient), ambients) {
            <CoordinatesCallbacksAmbient.Provider value=complexMeasureBox.coordinatesCallbacks>
                collectedComposables.forEach { children -> <children /> }
            </CoordinatesCallbacksAmbient.Provider>
        }

        return layoutNode.childrenMeasureBoxes()
            .drop(boxesSoFar)
            .map { measureBox ->
                measureBox as ComplexMeasureBox
                measureBox.layoutNode.visible = false
                MeasurableImpl(measureBox)
            }
    }

    /**
     * Define the layout of the current layout. The block is aware of constraints, intrinsic
     * dimensions and is able to measure and position its children.
     */
    fun layout(block: (Constraints, (Measurable, Constraints) -> Placeable,
                       IntrinsicMeasureOperations,
                       (Int, Int, () -> Unit) -> Unit) -> Unit) {
        complexMeasureBox.layoutBlock = block
    }

    /**
     * Set the min intrinsic width of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun minIntrinsicWidth(block: (Px, IntrinsicMeasureOperations) -> Int) {
        complexMeasureBox.minIntrinsicWidthBlock = block
    }

    /**
     * Set the max intrinsic width of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun maxIntrinsicWidth(block: (Px, IntrinsicMeasureOperations) -> Int) {
        complexMeasureBox.maxIntrinsicWidthBlock = block
    }

    /**
     * Set the min intrinsic height of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun minIntrinsicHeight(block: (Px, IntrinsicMeasureOperations) -> Int) {
        complexMeasureBox.minIntrinsicHeightBlock = block
    }

    /**
     * Set the max intrinsic height of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun maxIntrinsicHeight(block: (Px, IntrinsicMeasureOperations) -> Int) {
        complexMeasureBox.maxIntrinsicHeightBlock = block
    }
}

/**
 * A simpler version of [ComplexMeasureBox], intrinsic dimensions do not need to be defined.
 * If a layout of this [MeasureBox] queries the intrinsics, an exception will be thrown.
 * This [MeasureBox] is built using public API on top of [ComplexMeasureBox].
 */
@Composable
class MeasureBox(
    @Children(composable = false) var block:
        (constraints: Constraints, measureOperations: MeasureOperations) -> Unit
) : Component() {
    override fun compose() {
        <ComplexMeasureBox> measureOperations ->
            measureOperations.layout { constraints, measure, _, layoutResult ->
                block(constraints,
                    MeasureOperations(measureOperations, measure, layoutResult)
                )
            }

            measureOperations.minIntrinsicWidth { h, intrinsics ->
                var intrinsicWidth = 0
                val measureOperations = MeasureOperations(measureOperations, { m, c ->
                    val width = intrinsics.minIntrinsicWidth(m, c.minHeight)
                    DummyPlaceable(width, h.toRoundedPixels())
                }, { width, _, _ -> intrinsicWidth = width })
                val constraints = Constraints.tightConstraintsForHeight(h)
                block(constraints, measureOperations)
                intrinsicWidth
            }
            measureOperations.maxIntrinsicWidth { h, intrinsics ->
                var intrinsicWidth = 0
                val measureOperations = MeasureOperations(measureOperations, { m, c ->
                    val width = intrinsics.maxIntrinsicWidth(m, c.minHeight)
                    DummyPlaceable(width, h.toRoundedPixels())
                }, { width, _, _ -> intrinsicWidth = width })
                val constraints = Constraints.tightConstraintsForHeight(h)
                block(constraints, measureOperations)
                intrinsicWidth
            }
            measureOperations.minIntrinsicHeight { w, intrinsics ->
                var intrinsicHeight = 0
                val measureOperations = MeasureOperations(measureOperations, { m, c ->
                    val height = intrinsics.minIntrinsicHeight(m, c.minWidth)
                    DummyPlaceable(w.toRoundedPixels(), height)
                }, { _, height, _ -> intrinsicHeight = height })
                val constraints = Constraints.tightConstraintsForWidth(w)
                block(constraints, measureOperations)
                intrinsicHeight
            }
            measureOperations.maxIntrinsicHeight { w, intrinsics ->
                var intrinsicHeight = 0
                val measureOperations = MeasureOperations(measureOperations, { m, c ->
                    val height = intrinsics.maxIntrinsicHeight(m, c.minWidth)
                    DummyPlaceable(w.toRoundedPixels(), height)
                }, { _, height, _ -> intrinsicHeight = height })
                val constraints = Constraints.tightConstraintsForWidth(w)
                block(constraints, measureOperations)
                intrinsicHeight
            }
        </ComplexMeasureBox>
    }
}

/**
 * Measure operations to be used with [MeasureBox].
 * Used to mask away intrinsics inside [MeasureBox].
 */
class MeasureOperations(
    private val complexMeasureOperations: ComplexMeasureOperations,
    private val complexMeasure: (Measurable, Constraints) -> Placeable,
    private val complexLayoutResult: (Int, Int, () -> Unit) -> Unit
) {
    // TODO(mount/popam): remove this when receiver scopes exist
    val density: Density get() = complexMeasureOperations.density

    /**
     * Compose [children] into the [MeasureBox] and return a list of [Measurable]s within
     * the children. Composition stops at [MeasureBox] children. Further composition requires
     * calling [Measurable.measure].
     */
    // TODO(popam): prevent collect from happening before every intrinsic measurement
    fun collect(@Children children: () -> Unit): List<Measurable>
            = complexMeasureOperations.collect(children)

    /**
     * Measure the child [Measurable] with a specific set of [Constraints]. The result
     * is a [Placeable], which can be used inside the [layout] method to position the child.
     */
    fun measure(measurable: Measurable, constraints: Constraints): Placeable
            = complexMeasure(measurable, constraints)

    /**
     * Sets the width and height of the current layout. The lambda is used to perform the
     * calls to [Placeable.place], defining the positions of the children relative to the current
     * layout.
     */
    fun layout(width: Int, height: Int, block: () -> Unit) {
        complexLayoutResult(width, height, block)
    }
}

internal val CoordinatesCallbacksAmbient = Ambient.of<MutableList<(LayoutCoordinates) -> Unit>>()

/**
 * The children callback will be called with the final LayoutCoordinates of the current MeasureBox
 * after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * <Column>
 *     <Item1/>
 *     <Item2/>
 *     <OnPositioned> coordinates ->
 *         // This coordinates contain bounds of the Column within it's parent Layout.
 *         // Store it if you want to use it later f.e. when a touch happens.
 *     </OnPositioned>
 * </Column>
 */
class OnPositioned(
    @Children(composable = false) private var callback: (coordinates: LayoutCoordinates) -> Unit
) : Component() {

    private var firstCompose = true

    override fun compose() {
        <CoordinatesCallbacksAmbient.Consumer> callbacks ->
            // TODO(Andrey): replace with didCommit effect to execute only once
            if (firstCompose) {
                firstCompose = false
                callbacks.add(callback)
                // TODO(Andrey): remove the callback from the list in onDispose effect
            }
        </CoordinatesCallbacksAmbient.Consumer>
    }

}

/**
 * Temporary needed to be able to use the component from the adapter module. b/120971484
 */
@Composable
fun ComplexMeasureBoxComposable(
    @Children(composable = false) block:
        (complexMeasureOperations: ComplexMeasureOperations) -> Unit
) {
    <ComplexMeasureBox block=block/>
}

/**
 * Temporary needed to be able to use the component from the adapter module. b/120971484
 */
@Composable
fun MeasureBoxComposable(
    @Children(composable = false) block:
        (constraints: Constraints, measureOperations: MeasureOperations) -> Unit
) {
    <MeasureBox block=block/>
}

