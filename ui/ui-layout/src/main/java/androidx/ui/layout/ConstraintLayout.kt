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

@file:Suppress("Deprecation")

package androidx.ui.layout

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.Composable
import androidx.compose.remember
import androidx.constraintlayout.core.state.ConstraintReference
import androidx.constraintlayout.core.state.Dimension.SPREAD_DIMENSION
import androidx.constraintlayout.core.state.Dimension.WRAP_DIMENSION
import androidx.constraintlayout.core.state.State
import androidx.constraintlayout.core.state.helpers.BarrierReference
import androidx.constraintlayout.core.widgets.ConstraintWidget
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
import androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED
import androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD
import androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer
import androidx.constraintlayout.core.widgets.Optimizer
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure
import androidx.compose.Immutable
import androidx.ui.core.Constraints
import androidx.ui.text.FirstBaseline
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.MultiMeasureLayout
import androidx.ui.core.Placeable
import androidx.ui.core.hasFixedHeight
import androidx.ui.core.hasFixedWidth
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
@ExperimentalLayout
@Composable
fun ConstraintLayout(
    constraintSet: ConstraintSet,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    val measurer = remember { Measurer() }
    @Suppress("Deprecation")
    MultiMeasureLayout(modifier = modifier, children = children) { measurables, constraints, _ ->
        val layoutSize = measurer.performMeasure(
            constraints,
            constraintSet,
            measurables,
            this
        )
        layout(layoutSize.width, layoutSize.height) {
            with(measurer) { performLayout() }
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
 * [ConstraintSet] class constructor should be used instead.
 */
// TODO(popam): support RTL
class ConstraintSetBuilderScope internal constructor(internal val state: State) {
    /**
     * Creates a reference corresponding to the constraint layout children with a specific tag,
     * which can be used to define the constraints to be imposed to those children.
     */
    fun tag(tag: Any) = tags.getOrPut(tag, { ConstrainedLayoutReference(tag) })
    private val tags = mutableMapOf<Any, ConstrainedLayoutReference>()

    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference(State.PARENT)

    /**
     * Represents a dimension that can be assigned to the width or height of a [ConstraintLayout]
     * [child][ConstrainedLayoutReference].
     */
    interface Dimension {
        /**
         * A [Dimension] that can be assigned both min and max bounds.
         */
        interface Coercible : Dimension

        /**
         * A [Dimension] that can be assigned a min bound.
         */
        interface MinCoercible : Dimension

        /**
         * A [Dimension] that can be assigned a max bound.
         */
        interface MaxCoercible : Dimension
    }

    /**
     * Sets the lower bound of the current [Dimension] to be the [Wrap] size of the child.
     */
    val Dimension.Coercible.minWrap: Dimension.MaxCoercible
        get() = (this as DimensionDescription).also { it.minSymbol = WRAP_DIMENSION }

    /**
     * Sets the lower bound of the current [Dimension] to a fixed [dp] value.
     */
    fun Dimension.Coercible.min(dp: Dp): Dimension.MaxCoercible =
        (this as DimensionDescription).also { it.min = state.convertDimension(dp).ipx }

    /**
     * Sets the upper bound of the current [Dimension] to a fixed [dp] value.
     */
    fun Dimension.Coercible.max(dp: Dp): Dimension.MinCoercible =
        (this as DimensionDescription).also { it.max = state.convertDimension(dp).ipx }

    /**
     * Sets the upper bound of the current [Dimension] to be the [Wrap] size of the child.
     */
    val Dimension.Coercible.maxWrap: Dimension.MinCoercible
        get() = (this as DimensionDescription).also { it.maxSymbol = WRAP_DIMENSION }

    /**
     * Sets the lower bound of the current [Dimension] to a fixed [dp] value.
     */
    fun Dimension.MinCoercible.min(dp: Dp): Dimension =
        (this as DimensionDescription).also { it.min = state.convertDimension(dp).ipx }

    /**
     * Sets the lower bound of the current [Dimension] to be the [Wrap] size of the child.
     */
    val Dimension.MinCoercible.minWrap: Dimension
        get() = (this as DimensionDescription).also { it.minSymbol = WRAP_DIMENSION }

    /**
     * Sets the upper bound of the current [Dimension] to a fixed [dp] value.
     */
    fun Dimension.MaxCoercible.max(dp: Dp): Dimension =
        (this as DimensionDescription).also { it.max = state.convertDimension(dp).ipx }

    /**
     * Sets the upper bound of the current [Dimension] to be the [Wrap] size of the child.
     */
    val Dimension.MaxCoercible.maxWrap: Dimension
        get() = (this as DimensionDescription).also { it.maxSymbol = WRAP_DIMENSION }

    /**
     * Describes a sizing behavior that can be applied to the width or height of a
     * [ConstraintLayout] child. The content of this class should not be instantiated
     * directly; helpers available in the scope such as [wrap], [value]
     * or [wrapFixed] should be used instead to create a [Dimension].
     */
    internal class DimensionDescription internal constructor(
        private val baseDimension: SolverDimension
    ) : Dimension.Coercible, Dimension.MinCoercible, Dimension.MaxCoercible, Dimension {
        var min: IntPx? = null
        var minSymbol: Any? = null
        var max: IntPx? = null
        var maxSymbol: Any? = null
        internal fun toSolverDimension() = baseDimension.also {
            if (minSymbol != null) {
                it.min(minSymbol)
            } else if (min != null) {
                it.min(min!!.value)
            }
            if (maxSymbol != null) {
                it.max(maxSymbol)
            } else if (max != null) {
                it.max(max!!.value)
            }
        }
    }

    /**
     * Creates a [Dimension] representing a suggested dp size. The requested size will
     * be respected unless the constraints in the [ConstraintSet] do not allow it. The min
     * and max bounds will be respected regardless of the constraints in the [ConstraintSet].
     * To make the value fixed (respected regardless the [ConstraintSet]), [valueFixed] should
     * be used instead.
     */
    fun value(dp: Dp): Dimension.Coercible =
        DimensionDescription(SolverDimension.Suggested(state.convertDimension(dp)))

    /**
     * Creates a [Dimension] representing a fixed dp size. The size will not change
     * according to the constraints in the [ConstraintSet].
     */
    fun valueFixed(dp: Dp): Dimension =
        DimensionDescription(SolverDimension.Fixed(state.convertDimension(dp)))

    /**
     * A [Dimension] with suggested wrap content behavior. The wrap content size
     * will be respected unless the constraints in the [ConstraintSet] do not allow it.
     * To make the value fixed (respected regardless the [ConstraintSet]), [wrapFixed]
     * should be used instead.
     */
    val wrap: Dimension.Coercible
        get() = DimensionDescription(SolverDimension.Suggested(WRAP_DIMENSION))

    /**
     * A [Dimension] with fixed wrap content behavior. The size will not change
     * according to the constraints in the [ConstraintSet].
     */
    val wrapFixed: Dimension
        get() = DimensionDescription(SolverDimension.Fixed(WRAP_DIMENSION))

    /**
     * A [Dimension] that spreads to match constraints.
     */
    val spread: Dimension
        get() = DimensionDescription(SolverDimension.Suggested(SPREAD_DIMENSION))

    /**
     * A [Dimension] that is a percent of the parent in the corresponding direction.
     */
    fun percent(percent: Float): Dimension =
        // TODO(popam): make this nicer when possible in future solver releases
        DimensionDescription(SolverDimension.Percent(0, percent).suggested(0))

    inner class ConstrainedLayoutReference internal constructor(val tag: Any) {
        val left = VerticalAnchor.ConstrainedLayoutAnchor(state, this, 0)
        val top = HorizontalAnchor.ConstrainedLayoutAnchor(state, this, 0)
        var right = VerticalAnchor.ConstrainedLayoutAnchor(state, this, 1)
        var bottom = HorizontalAnchor.ConstrainedLayoutAnchor(state, this, 1)
        var baseline = ConstrainedLayoutBaselineAnchor(state, this)

        /**
         * The width of the [ConstraintLayout] child.
         */
        var width: Dimension = wrap
            set(value) {
                field = value
                state.constraints(tag).width(
                    (value as DimensionDescription).toSolverDimension()
                ).apply()
            }

        /**
         * The height of the [ConstraintLayout] child.
         */
        var height: Dimension = wrap
            set(value) {
                field = value
                state.constraints(tag).height(
                    (value as DimensionDescription).toSolverDimension()
                ).apply()
            }

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
        // TODO(popam): keep the source of truth in ConstraintReference or make this write only
        var horizontalBias: Float = 0.5f
            set(value) {
                field = value
                state.constraints(tag).horizontalBias(value)
            }

        /**
         * The vertical bias of the current layout reference.
         */
        // TODO(popam): keep the source of truth in ConstraintReference or make this write only
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
    @SuppressLint
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
    private lateinit var measureScope: MeasureScope
    private val state = object : State() {
        lateinit var rootIncomingConstraints: Constraints

        override fun convertDimension(value: Any?): Int {
            return if (value is Dp) {
                with(density) { value.toIntPx().value }
            } else {
                super.convertDimension(value)
            }
        }
    }

    val widthConstraintsHolder = IntArray(2)
    val heightConstraintsHolder = IntArray(2)

    override fun measure(constraintWidget: ConstraintWidget, measure: BasicMeasure.Measure) {
        val measurable = constraintWidget.companionWidget
        if (measurable !is Measurable) return

        if (DEBUG) {
            Log.d("CCL", "Measuring ${measurable.tag} with: " +
                    constraintWidget.toDebugString() + "\n" + measure.toDebugString())
        }

        val initialPlaceable = placeables[measurable]
        val initialWidth = initialPlaceable?.width?.value ?: constraintWidget.width
        val initialHeight = initialPlaceable?.height?.value ?: constraintWidget.height
        val initialBaseline =
            initialPlaceable?.get(FirstBaseline) ?: constraintWidget.baselineDistance

        var constraints = run {
            obtainConstraints(
                constraintWidget.horizontalDimensionBehaviour,
                constraintWidget.width,
                constraintWidget.mMatchConstraintDefaultWidth,
                measure.useDeprecated,
                state.rootIncomingConstraints.maxWidth.value,
                widthConstraintsHolder
            )
            obtainConstraints(
                constraintWidget.verticalDimensionBehaviour,
                constraintWidget.height,
                constraintWidget.mMatchConstraintDefaultHeight,
                measure.useDeprecated,
                state.rootIncomingConstraints.maxHeight.value,
                heightConstraintsHolder
            )

            Constraints(
                widthConstraintsHolder[0].ipx,
                widthConstraintsHolder[1].ipx,
                heightConstraintsHolder[0].ipx,
                heightConstraintsHolder[1].ipx
            )
        }

        if (constraintWidget.horizontalDimensionBehaviour != MATCH_CONSTRAINT ||
                constraintWidget.mMatchConstraintDefaultWidth != MATCH_CONSTRAINT_SPREAD ||
                constraintWidget.verticalDimensionBehaviour != MATCH_CONSTRAINT ||
                constraintWidget.mMatchConstraintDefaultHeight != MATCH_CONSTRAINT_SPREAD) {
            if (DEBUG) {
                Log.d("CCL", "Measuring ${measurable.tag} with $constraints")
            }
            val placeable = with(measureScope) {
                measurable.measure(constraints).also { placeables[measurable] = it }
            }
            if (DEBUG) {
                Log.d("CCL", "${measurable.tag} is size ${placeable.width} ${placeable.height}")
            }

            val coercedWidth = placeable.width.value.coerceIn(
                constraintWidget.minWidth.takeIf { it > 0 },
                constraintWidget.maxWidth.takeIf { it > 0 }
            )
            val coercedHeight = placeable.height.value.coerceIn(
                constraintWidget.minHeight.takeIf { it > 0 },
                constraintWidget.maxHeight.takeIf { it > 0 }
            )

            var remeasure = false
            if (coercedWidth != placeable.width.value) {
                constraints = constraints.copy(
                    minWidth = coercedWidth.ipx,
                    maxWidth = coercedWidth.ipx
                )
                remeasure = true
            }
            if (coercedHeight != placeable.height.value) {
                constraints = constraints.copy(
                    minHeight = coercedHeight.ipx,
                    maxHeight = coercedHeight.ipx
                )
                remeasure = true
            }
            if (remeasure) {
                if (DEBUG) {
                    Log.d("CCL", "Remeasuring coerced ${measurable.tag} with $constraints")
                }
                with(measureScope) {
                    measurable.measure(constraints).also { placeables[measurable] = it }
                }
            }
        }

        val currentPlaceable = placeables[measurable]
        measure.measuredWidth = currentPlaceable?.width?.value ?: constraintWidget.width
        measure.measuredHeight = currentPlaceable?.height?.value ?: constraintWidget.height
        val baseline = currentPlaceable?.get(FirstBaseline)
        measure.measuredHasBaseline = baseline != null
        if (baseline != null) measure.measuredBaseline = baseline.value
        measure.measuredNeedsSolverPass = measure.measuredWidth != initialWidth ||
                measure.measuredHeight != initialHeight ||
                measure.measuredBaseline != initialBaseline
    }

    /**
     * Calculates the [Constraints] in one direction that should be used to measure a child,
     * based on the solver measure request.
     */
    private fun obtainConstraints(
        dimensionBehaviour: ConstraintWidget.DimensionBehaviour,
        dimension: Int,
        matchConstraintDefaultDimension: Int,
        useDeprecated: Boolean,
        rootMaxConstraint: Int,
        outConstraints: IntArray
    ) = when (dimensionBehaviour) {
        FIXED -> {
            outConstraints[0] = dimension
            outConstraints[1] = dimension
        }
        WRAP_CONTENT -> {
            outConstraints[0] = 0
            outConstraints[1] = rootMaxConstraint
        }
        MATCH_CONSTRAINT -> {
            val useDimension =
                useDeprecated && matchConstraintDefaultDimension == MATCH_CONSTRAINT_WRAP
            outConstraints[0] = if (useDimension) dimension else 0
            outConstraints[1] = if (useDimension) dimension else rootMaxConstraint
        }
        else -> {
            error("MATCH_PARENT is not supported")
        }
    }

    fun performMeasure(
        constraints: Constraints,
        constraintSet: ConstraintSet,
        measurables: List<Measurable>,
        measureScope: MeasureScope
    ): IntPxSize {
        this.density = measureScope
        this.measureScope = measureScope
        state.reset()
        // Add tags.
        measurables.forEach { measurable ->
            state.map(measurable.tag ?: object : Any() {}, measurable)
        }
        // Define the size of the ConstraintLayout.
        state.width(if (constraints.hasFixedWidth) {
            SolverDimension.Fixed(constraints.maxWidth.value)
        } else {
            SolverDimension.Wrap().min(constraints.minWidth.value)
        })
        state.height(if (constraints.hasFixedHeight) {
            SolverDimension.Fixed(constraints.maxHeight.value)
        } else {
            SolverDimension.Wrap().min(constraints.minHeight.value)
        })
        // Build constraint set and apply it to the state.
        constraintSet.description(ConstraintSetBuilderScope(state))
        state.apply(root)
        root.width = constraints.maxWidth.value
        root.height = constraints.maxHeight.value
        state.rootIncomingConstraints = constraints
        root.updateHierarchy()

        if (DEBUG) {
            root.debugName = "ConstraintLayout"
            root.children.forEach { child ->
                child.debugName = (child.companionWidget as? Measurable)?.tag?.toString() ?: "NOTAG"
            }
            Log.d("CCL", "ConstraintLayout is asked to measure with $constraints")
            Log.d("CCL", root.toDebugString())
            for (child in root.children) {
                Log.d("CCL", child.toDebugString())
            }
        }

        // No need to set sizes and size modes as we passed them to the state above.
        root.measure(Optimizer.OPTIMIZATION_NONE, 0, 0, 0, 0, 0, 0, 0, 0)

        for (child in root.children) {
            val measurable = child.companionWidget
            if (measurable !is Measurable) continue
            val placeable = placeables[measurable]
            val currentWidth = placeable?.width?.value
            val currentHeight = placeable?.height?.value
            if (child.width != currentWidth || child.height != currentHeight) {
                if (DEBUG) {
                    Log.d(
                        "CCL",
                        "Final measurement for ${measurable.tag} " +
                                "to confirm size ${child.width} ${child.height}"
                    )
                }
                with(measureScope) {
                    measurable.measure(
                        Constraints.fixed(child.width.ipx, child.height.ipx)
                    ).also { placeables[measurable] = it }
                }
            }
        }
        if (DEBUG) {
            Log.d("CCL", "ConstraintLayout is at the end ${root.width} ${root.height}")
        }

        return IntPxSize(root.width.ipx, root.height.ipx)
    }

    fun Placeable.PlacementScope.performLayout() {
        for (child in root.children) {
            val measurable = child.companionWidget
            if (measurable !is Measurable) continue
            // TODO(popam): check if measurer's rtl support should be used instead
            placeables[measurable]?.place(child.x.ipx, child.y.ipx)
        }
    }

    override fun didMeasures() { }
}

private typealias SolverDimension = androidx.constraintlayout.core.state.Dimension
private val DEBUG = true
private fun ConstraintWidget.toDebugString() =
    "$debugName " +
            "width $width minWidth $minWidth maxWidth $maxWidth " +
            "height $height minHeight $minHeight maxHeight maxHeight " +
            "HDB $horizontalDimensionBehaviour VDB $verticalDimensionBehaviour " +
            "percentH $mMatchConstraintPercentWidth $mMatchConstraintPercentHeight"
private fun BasicMeasure.Measure.toDebugString() =
    "use deprecated is $useDeprecated "
