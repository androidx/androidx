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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable

/**
 * An ordered, immutable, collection of modifier elements that work with curved components, in a
 * polar coordinate space.
 *
 * This plays the same role as [androidx.compose.ui.Modifier], but for the Curved composables.
 */
@Stable
public sealed interface CurvedModifier {
    /**
     * Concatenates this curved modifier with another.
     *
     * Returns a [CurvedModifier] representing this curved modifier followed by [other] in sequence.
     */
    infix fun then(other: CurvedModifier): CurvedModifier =
        CurvedModifierImpl(elements() + other.elements())

    /**
     * The companion object `CurvedModifier` is the empty, default, or starter [CurvedModifier]
     * that contains no Elements.
     */
    companion object : CurvedModifier {
        override fun toString() = "CurvedModifier"
    }
}

internal fun CurvedModifier.elements() =
    if (this == CurvedModifier) emptyList() else (this as CurvedModifierImpl).elements

internal open class CurvedModifierImpl(internal val elements: List<Element>) : CurvedModifier

/**
 * A single element contained within a [CurvedModifier] chain.
 */
internal fun interface Element {
    abstract fun wrap(child: CurvedChild): CurvedChild
}

/**
 * Concatenates this curved modifier with an [Element].
 *
 * Returns a [CurvedModifier] representing this curved modifier followed by [other] in sequence.
 */
internal infix fun CurvedModifier.then(other: Element): CurvedModifier =
    CurvedModifierImpl(elements() + other)

/**
 * Create a chain of CurvedChild, one for each modifier, wrapping each other.
 */
internal fun CurvedModifier.wrap(child: CurvedChild) = elements().foldRight(child) { elem, acc ->
    elem.wrap(acc)
}

/**
 * Base class to implement a CurvedChild in a chain.
 * Forwards all calls to the wrapped CurvedChild.
 */
internal open class BaseCurvedChildWrapper(val wrapped: CurvedChild) : CurvedChild() {
    @Composable
    override fun SubComposition() { wrapped.SubComposition() }

    override fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int = with(wrapped) {
        initializeMeasure(measurables, index)
    }

    override fun doEstimateThickness(maxRadius: Float) = wrapped.estimateThickness(maxRadius)

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ) = wrapped.radialPosition(
        parentOuterRadius,
        parentThickness,
    )

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ) = wrapped.angularPosition(
        parentStartAngleRadians,
        parentSweepRadians,
        centerOffset
    )

    override fun (Placeable.PlacementScope).placeIfNeeded() = with(wrapped) { placeIfNeeded() }

    override fun DrawScope.draw() = with(wrapped) { draw() }
}
