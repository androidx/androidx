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
import com.google.r4a.R4a

/**
 * A part of the composition that can be measured. This represents a [MeasureBox] somewhere
 * down the hierarchy.
 *
 * @return a [Placeable] that can be used within a [MeasureOperations.layout] block
 */
// TODO(mount): Make this an inline class when private constructors are possible
class Measurable internal constructor(private val measureBox: MeasureBox) {
    fun measure(constraints: Constraints): Placeable {
        measureBox.measure(constraints)
        return Placeable(measureBox)
    }
}

/**
 * A measured [MeasureBox] from [Measurable.measure]. The [place] call can be made within
 * [MeasureOperations.layout]. In the future, this will only be accessible within the [layout]
 * call.
 */
// TODO(mount): Make this an inline class when private constructors are possible
class Placeable internal constructor(private val measureBox: MeasureBox) {
    val width = measureBox.layoutNode.size.width
    val height = measureBox.layoutNode.size.height

    fun place(x: Dimension, y: Dimension) {
        measureBox.moveTo(x, y)
        measureBox.layout()
    }
}

/**
 * MeasureBox is a tag that can be used to measure and position zero or more children.
 * The MeasureBox accepts a [Constraints] argument to use to determine its measurements.
 * For example, to get several children to draw on top of each other:
 *     @Composable fun Stack(@Children children: () -> Unit) {
 *         <MeasureBox> constraints ->
 *             val measurables = collect(children)
 *             val placeables = measurables.map { it.measure(constraints) }
 *             val width = placeables.maxBy { it.width }
 *             val height = placeables.maxBy { it.height }
 *             layout(width, height) {
 *                 placeables.forEach { placeable ->
 *                     placeable.place((width - placeable.width) / 2,
 *                                     (height - placeable.height) / 2))
 *                 }
 *             }
 *         </MeasureBox>
 *     }
 *
 * Until receiver scopes work, [MeasureOperations] is passed as an extra argument.
 */
class MeasureBox(@Children(composable = false) var block: (constraints: Constraints, measureOperations: MeasureOperations) -> Unit) :
    Component() {
    internal var layoutBlock: () -> Unit = {}
    private val ref = Ref<LayoutNode>()
    internal val layoutNode: LayoutNode get() = ref.value!!
    internal var ambients: Ambient.Reference? = null
    internal val coordinatesCallbacks = mutableListOf<(LayoutCoordinates) -> Unit>()

    override fun compose() {
        <Ambient.Portal> reference ->
            ambients = reference
            <LayoutNode ref measureBox=this />
        </Ambient.Portal>
        if (recomposeMeasureBox == null) {
            recomposeMeasureBox = this
            measure(layoutNode.constraints)
            recomposeMeasureBox = null
            layout()
        }
    }

    internal fun measure(constraints: Constraints) {
        layoutNode.constraints = constraints
        val measureOperations = MeasureOperations(this)
        block(constraints, measureOperations)
    }

    internal fun layout() {
        layoutNode.visible = true
        layoutBlock()
        if (coordinatesCallbacks.isNotEmpty()) {
            val coordinates = LayoutNodeCoordinates(layoutNode)
            coordinatesCallbacks.forEach { it.invoke(coordinates) }
        }
    }

    internal fun moveTo(x: Dimension, y: Dimension) {
        layoutNode.moveTo(x, y)
    }

    internal fun resize(width: Dimension, height: Dimension) {
        layoutNode.resize(width, height)
    }

}

internal var recomposeMeasureBox: MeasureBox? = null

/**
 * Receiver scope for [MeasureBox]'s child lambda to add [collect] and [layout] functions.
 */
class MeasureOperations internal constructor(private val measureBox: MeasureBox) {

    /**
     * We store previously provided composable children to allow multiple calls
     * of collect() function during the one measuring pass.
     * Example:
     * val header = collect(headerChildren)
     * val body = collect(bodyChildren)
     */
    private val collectedComposables = mutableListOf<() -> Unit>()

    /**
     * Compose [children] into the [MeasureBox] and return a list of [Measurable]s within
     * the children. Composition stops at [MeasureBox] children. Further composition requires
     * calling [Measurable.measure].
     */
    fun collect(@Children children: () -> Unit): List<Measurable> {
        val layoutNode = measureBox.layoutNode
        val boxesSoFar = if (collectedComposables.isEmpty()) 0 else
            layoutNode.childrenMeasureBoxes().size
        collectedComposables.add(children)

        val ambients = measureBox.ambients!!
        R4a.composeInto(layoutNode, ambients.getAmbient(ContextAmbient), ambients) {
            <CoordinatesCallbacksAmbient.Provider value=measureBox.coordinatesCallbacks>
                collectedComposables.forEach { children -> <children /> }
            </CoordinatesCallbacksAmbient.Provider>
        }

        return layoutNode.childrenMeasureBoxes()
            .drop(boxesSoFar)
            .map { measureBox ->
                measureBox.layoutNode.visible = false
                Measurable(measureBox)
            }
    }

    /**
     * Set the size of the [MeasureBox] to [width] x [height] and set the [block] used to
     * position children with [Placeable.place]. It is possible to call [Measurable.measure]
     * inside [block] for any [Measurables] that aren't needed to calculate [MeasureBox]'s size.
     */
    fun layout(width: Dimension, height: Dimension, block: () -> Unit) {
        measureBox.resize(width, height)
        measureBox.layoutBlock = block
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
    @Children(composable = false) private val callback: (coordinates: LayoutCoordinates) -> Unit
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
fun MeasureBoxComposable(@Children(composable = false) block: (constraints: Constraints, measureOperations: MeasureOperations) -> Unit) {
    <MeasureBox block=block/>
}