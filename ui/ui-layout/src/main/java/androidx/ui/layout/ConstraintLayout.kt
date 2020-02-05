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

package androidx.ui.layout.constraintlayout

import android.annotation.SuppressLint
import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.compose.remember
import androidx.constraintlayout.solver.state.ConstraintReference
import androidx.constraintlayout.solver.state.Dimension
import androidx.constraintlayout.solver.state.State
import androidx.constraintlayout.solver.state.helpers.BarrierReference
import androidx.constraintlayout.solver.widgets.ConstraintWidget
import androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer
import androidx.constraintlayout.solver.widgets.Optimizer
import androidx.constraintlayout.solver.widgets.analyzer.BasicMeasure
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Constraints
import androidx.ui.core.FirstBaseline
import androidx.ui.core.Layout
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.ParentDataModifier
import androidx.ui.core.Placeable
import androidx.ui.core.Placeable.PlacementScope.place
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.tag
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

/**
 * Layout that positions its children according to the constraints between them.
 */
@Composable
fun ConstraintLayout(constraintSet: ConstraintSet, children: @Composable() () -> Unit) {
    val measurer = remember { Measurer() }
    Layout(children) { measurables, constraints ->
        val layoutSize = measurer.performMeasure(
            constraints,
            constraintSet,
            measurables,
            this
        )
        layout(layoutSize.width, layoutSize.height) {
            measurer.performLayout()
        }
    }
}

/**
 * Immutable description of the constraints used to layout the children of a [ConstraintLayout].
 */
@Immutable
data class ConstraintSet(internal val description: ConstraintSetBuilderScope.() -> Unit)

/**
 * Builder scope for a [ConstraintSet]. The scope should not be created directly - the
 * [ConstraintSet] function should be used instead.
 */
// TODO(popam): support RTL
class ConstraintSetBuilderScope internal constructor(internal val state: State) {
    /**
     * Creates a reference corresponding to the constraint layout children with a specific tag,
     * which can be used to define the constraints to be imposed to those children.
     */
    fun tag(tag: Any) = tags.getOrPut(tag, { ConstrainedLayoutReference(state, tag) })
    private val tags = mutableMapOf<Any, ConstrainedLayoutReference>()

    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference(state, State.PARENT)

    class ConstrainedLayoutReference internal constructor(val state: State, val tag: Any) {
        val left = VerticalAnchor.ConstrainedLayoutAnchor(state, this, 0)
        val top = HorizontalAnchor.ConstrainedLayoutAnchor(state, this, 0)
        var right = VerticalAnchor.ConstrainedLayoutAnchor(state, this, 1)
        var bottom = HorizontalAnchor.ConstrainedLayoutAnchor(state, this, 1)
        var baseline = ConstrainedLayoutBaselineAnchor(state, this)

        /**
         * Adds constraints between left, top, right and bottom corresponding anchors of
         * two layout references.
         */
        infix fun constrainTo(other: ConstrainedLayoutReference) {
            left constrainTo other.left
            top constrainTo other.top
            right constrainTo other.right
            bottom constrainTo other.bottom
        }

        /**
         * Adds constraints between left and right corresponding anchors of two layout references.
         */
        infix fun constrainHorizontallyTo(other: ConstrainedLayoutReference) {
            left constrainTo other.left
            right constrainTo other.right
        }

        /**
         * Adds constraints between top and bottom corresponding anchors of two layout references.
         */
        infix fun constrainVerticallyTo(other: ConstrainedLayoutReference) {
            top constrainTo other.top
            bottom constrainTo other.bottom
        }

        /**
         * The horizontal bias of the current layout reference.
         */
        // TODO(popam): keep the source of truth in ConstraintReference
        var horizontalBias: Float = 0.5f
            set(value) {
                field = value
                state.constraints(tag).horizontalBias(value)
            }

        /**
         * The vertical bias of the current layout reference.
         */
        // TODO(popam): keep the source of truth in ConstraintReference
        var verticalBias: Float = 0.5f
            set(value) {
                field = value
                state.constraints(tag).verticalBias(value)
            }

        /**
         * Centers the layout horizontally in its [parent].
         */
        fun centerHorizontally() = state.centerHorizontally(tag).also { it.bias(0.5f) }.apply()

        /**
         * Centers the layout vertically in its [parent].
         */
        fun centerVertically() = state.centerVertically(tag).also { it.bias(0.5f) }.apply()

        /**
         * Centers the layout in its [parent].
         */
        fun center() {
            centerHorizontally()
            centerVertically()
        }
    }

    /**
     * Represents a horizontal chain.
     */
    class HorizontalChain internal constructor(
        internal val first: ConstrainedLayoutReference,
        internal val last: ConstrainedLayoutReference
    ) {
        val left: VerticalAnchor.ConstrainedLayoutAnchor get() = first.left
        val right: VerticalAnchor.ConstrainedLayoutAnchor get() = last.right

        infix fun constrainTo(other: ConstrainedLayoutReference) {
            left constrainTo other.left
            right constrainTo other.right
        }
    }

    /**
     * Represents a vertical chain.
     */
    class VerticalChain internal constructor(
        internal val first: ConstrainedLayoutReference,
        internal val last: ConstrainedLayoutReference
    ) {
        val top: HorizontalAnchor.ConstrainedLayoutAnchor get() = first.top
        val bottom: HorizontalAnchor.ConstrainedLayoutAnchor get() = last.bottom

        infix fun constrainTo(other: ConstrainedLayoutReference) {
            first.top constrainTo other.top
            last.bottom constrainTo other.bottom
        }
    }

    /**
     * Defines a vertical anchor which can be used for defining constraints. It can correspond:
     * - to a side or baseline of a child of the [ConstraintLayout]
     * - to a left barrier or right barrier
     * - to a vertical guideline
     */
    sealed class VerticalAnchor {
        internal abstract val state: State
        internal abstract val tag: Any
        internal abstract val index: Int

        /**
         * Anchor corresponding to the left or right of a child of the [ConstraintLayout].
         */
        class ConstrainedLayoutAnchor internal constructor(
            override val state: State,
            internal val constrainedLayoutReference: ConstrainedLayoutReference,
            override val index: Int
        ) : VerticalAnchor() {
            override val tag: Any get() = constrainedLayoutReference.tag

            // TODO(popam): keep the source of truth in ConstraintReference
            /**
             * The margin to be applied to the current [ConstrainedLayoutAnchor].
             */
            var margin: Dp = 0.dp
                set(value) {
                    field = value
                    state.constraints(tag)
                        .let { if (index == 0) it.start() else it.end() }
                        .margin(value)
                }

            /**
             * Adds a constraint between a [ConstrainedLayoutAnchor] and a [VerticalAnchor].
             */
            infix fun constrainTo(other: VerticalAnchor) {
                with(state.constraints(this.constrainedLayoutReference.tag)) {
                    val thisIndex = this@ConstrainedLayoutAnchor.index
                    val otherIndex = other.index
                    verticalAnchorFunctions[thisIndex][otherIndex].invoke(this, other.tag)
                }
            }
        }

        /**
         * Anchor corresponding to a vertical guideline.
         */
        class GuidelineAnchor internal constructor(
            override val state: State,
            override val tag: Any,
            override val index: Int = 0
        ) : VerticalAnchor()

        /**
         * Anchor corresponding to a left or right barrier.
         */
        class BarrierAnchor internal constructor(
            override val state: State,
            override val tag: Any,
            private val barrierReference: BarrierReference,
            override val index: Int = 0
        ) : VerticalAnchor() {
            // TODO(popam): keep the source of truth in ConstraintReference
            /**
             * The margin to be applied to the current [BarrierAnchor], in the
             * direction of the barrier.
             */
            var margin: Dp = 0.dp
                set(value) {
                    field = value
                    barrierReference.margin(value)
                }
        }
    }

    /**
     * Defines an horizontal anchor which can be used for defining constraints. It can correspond:
     * - to a side or baseline of a child of the [ConstraintLayout]
     * - to a top or bottom barrier
     * - to a horizontal guideline
     */
    sealed class HorizontalAnchor {
        internal abstract val state: State
        internal abstract val tag: Any
        internal abstract val index: Int

        /**
         * Anchor corresponding to the top or bottom of a child of the [ConstraintLayout].
         */
        class ConstrainedLayoutAnchor internal constructor(
            override val state: State,
            internal val constrainedLayoutReference: ConstrainedLayoutReference,
            override val index: Int
        ) : HorizontalAnchor() {
            override val tag: Any get() = constrainedLayoutReference.tag

            // TODO(popam): keep the source of truth in ConstraintReference
            /**
             * The margin to be applied to the current [ConstrainedLayoutAnchor].
             */
            var margin: Dp = 0.dp
                set(value) {
                    field = value
                    state.constraints(tag)
                        .let { if (index == 0) it.top() else it.bottom() }
                        .margin(value)
                }

            /**
             * Adds a constraint between a [ConstrainedLayoutAnchor] and a [HorizontalAnchor].
             */
            infix fun constrainTo(other: HorizontalAnchor) {
                with(state.constraints(this.constrainedLayoutReference.tag)) {
                    val thisIndex = this@ConstrainedLayoutAnchor.index
                    val otherIndex = other.index
                    horizontalAnchorFunctions[thisIndex][otherIndex].invoke(this, other.tag)
                }
            }
        }

        /**
         * Anchor corresponding to a horizontal guideline.
         */
        class GuidelineAnchor internal constructor(
            override val state: State,
            override val tag: Any,
            override val index: Int = 0
        ) : HorizontalAnchor()

        /**
         * Anchor corresponding to a top or bottom barrier.
         */
        class BarrierAnchor internal constructor(
            override val state: State,
            override val tag: Any,
            private val barrierReference: BarrierReference,
            override val index: Int = 0
        ) : HorizontalAnchor() {
            // TODO(popam): keep the source of truth in ConstraintReference
            var margin: Dp = 0.dp
                set(value) {
                    field = value
                    barrierReference.margin(value)
                }
        }
    }

    /**
     * Anchor corresponding to the baseline of a [ConstraintLayout] child.
     */
    class ConstrainedLayoutBaselineAnchor internal constructor(
        val state: State,
        val tag: Any
    ) {
        /**
         * Adds a constraint between two [ConstrainedLayoutBaselineAnchor] anchors.
         */
        infix fun constrainTo(other: ConstrainedLayoutBaselineAnchor) {
            with(state.constraints(tag)) {
                baselineAnchorFunction.invoke(this, other.tag)
            }
        }
    }

    /**
     * Creates a horizontal chain including the referenced layouts.
     */
    fun createHorizontalChain(
        // Suppress lint here to allow vararg parameter for elements. API likely to change.
        @SuppressLint("ArrayReturn")
        vararg elements: ConstrainedLayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): HorizontalChain {
        state.horizontalChain(*(elements.map { it.tag }.toTypedArray()))
            .also { it.style(chainStyle.style) }
            .apply()
        if (chainStyle.bias != null) elements[0].horizontalBias = chainStyle.bias
        return HorizontalChain(elements.first(), elements.last())
    }

    /**
     * Creates a vertical chain including the referenced layouts.
     */
    fun createVerticalChain(
        // Suppress lint here to allow vararg parameter for elements. API likely to change.
        @SuppressLint("ArrayReturn")
        vararg elements: ConstrainedLayoutReference,
        chainStyle: ChainStyle = ChainStyle.Spread
    ): VerticalChain {
        state.verticalChain(*(elements.map { it.tag }.toTypedArray()))
            .also { it.style(chainStyle.style) }
            .apply()
        if (chainStyle.bias != null) elements[0].verticalBias = chainStyle.bias
        return VerticalChain(elements.first(), elements.last())
    }

    /**
     * The style of a horizontal or vertical chain.
     */
    class ChainStyle internal constructor(
        internal val style: State.Chain,
        internal val bias: Float? = null
    ) {
        companion object {
            val Spread = ChainStyle(State.Chain.SPREAD)
            val SpreadInside = ChainStyle(State.Chain.SPREAD_INSIDE)
            val Packed = Packed(0.5f)
            fun Packed(bias: Float) = ChainStyle(State.Chain.PACKED, bias)
        }
    }

    /**
     * Creates a guideline at a specific offset from the left of the [ConstraintLayout].
     */
    fun createGuidelineFromLeft(offset: Dp): VerticalAnchor.GuidelineAnchor {
        val tag = object : Any() {}
        state.verticalGuideline(tag).apply { start(offset) }
        return VerticalAnchor.GuidelineAnchor(state, tag)
    }

    /**
     * Creates a guideline at a width percentage from the left of the [ConstraintLayout].
     */
    fun createGuidelineFromLeft(percent: Float): VerticalAnchor.GuidelineAnchor {
        val tag = object : Any() {}
        state.verticalGuideline(tag).apply { percent(percent) }
        return VerticalAnchor.GuidelineAnchor(state, tag)
    }

    /**
     * Creates a guideline at a specific offset from the right of the [ConstraintLayout].
     */
    fun createGuidelineFromRight(offset: Dp): VerticalAnchor.GuidelineAnchor {
        val tag = object : Any() {}
        state.verticalGuideline(tag).apply { end(offset) }
        return VerticalAnchor.GuidelineAnchor(state, tag)
    }

    /**
     * Creates a guideline at a width percentage from the right of the [ConstraintLayout].
     */
    fun createGuidelineFromRight(percent: Float): VerticalAnchor.GuidelineAnchor {
        return createGuidelineFromLeft(1f - percent)
    }

    /**
     * Creates a guideline at a specific offset from the top of the [ConstraintLayout].
     */
    fun createGuidelineFromTop(offset: Dp): HorizontalAnchor.GuidelineAnchor {
        val tag = object : Any() {}
        state.horizontalGuideline(tag).apply { start(offset) }
        return HorizontalAnchor.GuidelineAnchor(state, tag)
    }

    /**
     * Creates a guideline at a height percentage from the top of the [ConstraintLayout].
     */
    fun createGuidelineFromTop(percent: Float): HorizontalAnchor.GuidelineAnchor {
        val tag = object : Any() {}
        state.horizontalGuideline(tag).apply { percent(percent) }
        return HorizontalAnchor.GuidelineAnchor(state, tag)
    }

    /**
     * Creates a guideline at a specific offset from the bottom of the [ConstraintLayout].
     */
    fun createGuidelineFromBottom(offset: Dp): HorizontalAnchor.GuidelineAnchor {
        val tag = object : Any() {}
        state.horizontalGuideline(tag).apply { end(offset) }
        return HorizontalAnchor.GuidelineAnchor(state, tag)
    }

    /**
     * Creates a guideline at a height percentage from the bottom of the [ConstraintLayout].
     */
    fun createGuidelineFromBottom(percent: Float): HorizontalAnchor.GuidelineAnchor {
        return createGuidelineFromTop(1f - percent)
    }

    /**
     * Creates and returns a top barrier, containing the specified elements.
     */
    fun createTopBarrier(
        vararg elements: ConstrainedLayoutReference
    ): HorizontalAnchor.BarrierAnchor {
        val tag = object : Any() {}
        val barrier = state.barrier(tag, State.Direction.TOP).apply {
            add(*(elements.map { it.tag }.toTypedArray()))
        }
        return HorizontalAnchor.BarrierAnchor(state, tag, barrier)
    }

    /**
     * Creates and returns a bottom barrier, containing the specified elements.
     */
    fun createBottomBarrier(
        vararg elements: ConstrainedLayoutReference
    ): HorizontalAnchor.BarrierAnchor {
        val tag = object : Any() {}
        val barrier = state.barrier(tag, State.Direction.BOTTOM).apply {
            add(*(elements.map { it.tag }.toTypedArray()))
        }
        return HorizontalAnchor.BarrierAnchor(state, tag, barrier)
    }

    /**
     * Creates and returns a left barrier, containing the specified elements.
     */
    fun createLeftBarrier(
        vararg elements: ConstrainedLayoutReference
    ): VerticalAnchor.BarrierAnchor {
        val tag = object : Any() {}
        val barrier = state.barrier(tag, State.Direction.START).apply {
            add(*(elements.map { it.tag }.toTypedArray()))
        }
        return VerticalAnchor.BarrierAnchor(state, tag, barrier)
    }

    /**
     * Creates and returns a right barrier, containing the specified elements.
     */
    fun createRightBarrier(
        vararg elements: ConstrainedLayoutReference
    ): VerticalAnchor.BarrierAnchor {
        val tag = object : Any() {}
        val barrier = state.barrier(tag, State.Direction.END).apply {
            add(*(elements.map { it.tag }.toTypedArray()))
        }
        return VerticalAnchor.BarrierAnchor(state, tag, barrier)
    }

    internal companion object {
        val verticalAnchorFunctions: Array<Array<ConstraintReference.(Any) -> Unit>> = arrayOf(
            arrayOf(
                { other -> startToStart(other) },
                { other -> startToEnd(other) }
            ),
            arrayOf(
                { other -> endToStart(other) },
                { other -> endToEnd(other) }
            )
        )
        val horizontalAnchorFunctions: Array<Array<ConstraintReference.(Any) -> Unit>> = arrayOf(
            arrayOf(
                { other -> topToTop(other) },
                { other -> topToBottom(other) }
            ),
            arrayOf(
                { other -> bottomToTop(other) },
                { other -> bottomToBottom(other) }
            )
        )
        val baselineAnchorFunction: ConstraintReference.(Any) -> Unit =
            { other -> baselineToBaseline(other) }
    }
}

private class Measurer internal constructor() : BasicMeasure.Measurer {
    private val root = ConstraintWidgetContainer(0, 0).also { it.measurer = this }
    private val placeables = mutableMapOf<Measurable, Placeable>()
    private lateinit var density: Density
    private val state = object : State() {
        override fun convertDimension(value: Any?): Int {
            return if (value is Dp) {
                with(density) { value.toIntPx().value }
            } else {
                super.convertDimension(value)
            }
        }
    }

    override fun measure(constraintWidget: ConstraintWidget, measure: BasicMeasure.Measure) {
        val measurable = constraintWidget.companionWidget
        if (measurable !is Measurable) return

        var measuredWidth = constraintWidget.width
        var measuredHeight = constraintWidget.height

        val constraints = Constraints(
            if (measure.wrapsHorizontal) 0.ipx else constraintWidget.width.ipx,
            if (measure.wrapsHorizontal) IntPx.Infinity else constraintWidget.width.ipx,
            if (measure.wrapsVertical) 0.ipx else constraintWidget.height.ipx,
            if (measure.wrapsVertical) IntPx.Infinity else constraintWidget.height.ipx
        )

        val placeable = measurable.measure(constraints).also { placeables[measurable] = it }

        if (measure.wrapsHorizontal) {
            measuredWidth = placeable.width.value
        }
        if (measure.wrapsVertical) {
            measuredHeight = placeable.height.value
        }

        measure.measuredWidth = measuredWidth
        measure.measuredHeight = measuredHeight
        val baseline = placeable[FirstBaseline]
        if (baseline != null) {
            measure.measuredBaseline = baseline.value
        } else {
            measure.measuredBaseline = measuredHeight
        }
    }

    fun performMeasure(
        constraints: Constraints,
        constraintSet: ConstraintSet,
        measurables: List<Measurable>,
        density: Density
    ): IntPxSize {
        this.density = density
        state.reset()
        measurables.forEach { measurable ->
            state.map(measurable.tag ?: object : Any() {}, measurable)
        }
        state.width(if (constraints.hasBoundedWidth) {
            Dimension.Fixed(constraints.maxWidth.value)
        } else {
            Dimension.Wrap()
        })
        state.height(if (constraints.hasBoundedHeight) {
            Dimension.Fixed(constraints.maxHeight.value)
        } else {
            Dimension.Wrap()
        })
        constraintSet.description(ConstraintSetBuilderScope(state))
        state.apply(root)

        root.minWidth = constraints.minWidth.value
        root.minHeight = constraints.minHeight.value
        root.measure(
            Optimizer.OPTIMIZATION_NONE,
            if (constraints.hasBoundedWidth) BasicMeasure.EXACTLY else BasicMeasure.WRAP_CONTENT,
            if (constraints.hasBoundedWidth) constraints.maxWidth.value else 0,
            if (constraints.hasBoundedHeight) BasicMeasure.EXACTLY else BasicMeasure.WRAP_CONTENT,
            if (constraints.hasBoundedHeight) constraints.maxHeight.value else 0,
            0,
            0,
            0,
            0
        )
        return IntPxSize(root.width.ipx, root.height.ipx)
    }

    fun performLayout() {
        for (child in root.children) {
            val measurable = child.companionWidget
            if (measurable !is Measurable) continue
            placeables[measurable]?.place(IntPx(child.x), IntPx(child.y))
        }
    }

    override fun didMeasures() { }

    private val BasicMeasure.Measure.wrapsHorizontal
        get() = horizontalBehavior == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
    private val BasicMeasure.Measure.wrapsVertical
        get() = verticalBehavior == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
}

private data class TagModifier(val tag: Any) : LayoutModifier, ParentDataModifier {
    override fun Density.modifyConstraints(constraints: Constraints) = constraints
    override fun Density.modifySize(constraints: Constraints, childSize: IntPxSize) = childSize
    override fun Density.minIntrinsicWidthOf(measurable: Measurable, height: IntPx) =
        measurable.minIntrinsicWidth(height)
    override fun Density.maxIntrinsicWidthOf(measurable: Measurable, height: IntPx) =
        measurable.maxIntrinsicWidth(height)
    override fun Density.minIntrinsicHeightOf(measurable: Measurable, width: IntPx) =
        measurable.minIntrinsicHeight(width)
    override fun Density.maxIntrinsicHeightOf(measurable: Measurable, width: IntPx) =
        measurable.maxIntrinsicHeight(width)
    override fun Density.modifyAlignmentLine(line: AlignmentLine, value: IntPx?) = value
    override fun Density.modifyParentData(parentData: Any?) = this@TagModifier
}
