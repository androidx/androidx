/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.AnchorFunctions.baselineAnchorFunction
import androidx.constraintlayout.core.state.ConstraintReference

/**
 * Scope that can be used to constrain a layout.
 *
 * Used within `Modifier.constrainAs` from the inline DSL API. And within `constrain` from the
 * ConstraintSet-based API.
 */
@LayoutScopeMarker
@Stable
class ConstrainScope internal constructor(internal val id: Any) {
    internal val tasks = mutableListOf<(State) -> Unit>()
    internal fun applyTo(state: State) = tasks.forEach { it(state) }

    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference(SolverState.PARENT)

    /**
     * The start anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val start: VerticalAnchorable = ConstraintVerticalAnchorable(id, -2, tasks)

    /**
     * The left anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteLeft: VerticalAnchorable = ConstraintVerticalAnchorable(id, 0, tasks)

    /**
     * The top anchor of the layout - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val top: HorizontalAnchorable = ConstraintHorizontalAnchorable(id, 0, tasks)

    /**
     * The end anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val end: VerticalAnchorable = ConstraintVerticalAnchorable(id, -1, tasks)

    /**
     * The right anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteRight: VerticalAnchorable = ConstraintVerticalAnchorable(id, 1, tasks)

    /**
     * The bottom anchor of the layout - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val bottom: HorizontalAnchorable = ConstraintHorizontalAnchorable(id, 1, tasks)

    /**
     * The [FirstBaseline] of the layout - can be constrained using [BaselineAnchorable.linkTo].
     */
    val baseline: BaselineAnchorable = ConstraintBaselineAnchorable(id, tasks)

    /**
     * The width of the [ConstraintLayout] child.
     */
    var width: Dimension = Dimension.wrapContent
        set(value) {
            field = value
            tasks.add { state ->
                state.constraints(id).width(
                    (value as DimensionDescription).toSolverDimension(state)
                )
            }
        }

    /**
     * The height of the [ConstraintLayout] child.
     */
    var height: Dimension = Dimension.wrapContent
        set(value) {
            field = value
            tasks.add { state ->
                state.constraints(id).height(
                    (value as DimensionDescription).toSolverDimension(state)
                )
            }
        }

    /**
     * The overall visibility of the [ConstraintLayout] child.
     *
     * [Visibility.Visible] by default.
     */
    var visibility: Visibility = Visibility.Visible
        set(value) {
            field = value
            tasks.add { state ->
                with(state.constraints(id)) {
                    visibility(value.solverValue)
                    if (value == Visibility.Invisible) {
                        // A bit of a hack, this behavior is not defined in :core
                        // Invisible should override alpha
                        alpha(0f)
                    }
                }
            }
        }

    /**
     * The transparency value when rendering the content.
     */
    @FloatRange(from = 0.0, to = 1.0)
    var alpha: Float = 1.0f
        set(value) {
            field = value
            addTransform {
                if (visibility != Visibility.Invisible) {
                    // A bit of a hack, this behavior is not defined in :core
                    // Invisible should override alpha
                    alpha(value)
                }
            }
        }

    /**
     * The percent scaling value on the horizontal axis. Where 1 is 100%.
     */
    var scaleX: Float = 1.0f
        set(value) {
            field = value
            addTransform { scaleX(value) }
        }

    /**
     * The percent scaling value on the vertical axis. Where 1 is 100%.
     */
    var scaleY: Float = 1.0f
        set(value) {
            field = value
            addTransform { scaleY(value) }
        }

    /**
     * The degrees to rotate the content over the horizontal axis.
     */
    var rotationX: Float = 0.0f
        set(value) {
            field = value
            addTransform { rotationX(value) }
        }

    /**
     * The degrees to rotate the content over the vertical axis.
     */
    var rotationY: Float = 0.0f
        set(value) {
            field = value
            addTransform { rotationY(value) }
        }

    /**
     * The degrees to rotate the content on the screen plane.
     */
    var rotationZ: Float = 0.0f
        set(value) {
            field = value
            addTransform { rotationZ(value) }
        }

    /**
     * The distance to offset the content over the X axis.
     */
    var translationX: Dp = 0.dp
        set(value) {
            field = value
            addFloatTransformFromDp(value) { floatValue -> translationX(floatValue) }
        }

    /**
     * The distance to offset the content over the Y axis.
     */
    var translationY: Dp = 0.dp
        set(value) {
            field = value
            addFloatTransformFromDp(value) { floatValue -> translationY(floatValue) }
        }

    /**
     * The distance to offset the content over the Z axis.
     */
    var translationZ: Dp = 0.dp
        set(value) {
            field = value
            addFloatTransformFromDp(value) { floatValue -> translationZ(floatValue) }
        }

    /**
     * The X axis offset percent where the content is rotated and scaled.
     *
     * @see [TransformOrigin]
     */
    var pivotX: Float = 0.5f
        set(value) {
            field = value
            addTransform { pivotX(value) }
        }

    /**
     * The Y axis offset percent where the content is rotated and scaled.
     *
     * @see [TransformOrigin]
     */
    var pivotY: Float = 0.5f
        set(value) {
            field = value
            addTransform { pivotY(value) }
        }

    /**
     * Whenever the width is not fixed, this weight may be used by an horizontal Chain to decide how
     * much space assign to this widget.
     */
    var horizontalChainWeight: Float = Float.NaN
        set(value) {
            field = value
            tasks.add { state ->
                state.constraints(id).horizontalChainWeight = value
            }
        }

    /**
     * Whenever the height is not fixed, this weight may be used by a vertical Chain to decide how
     * much space assign to this widget.
     */
    var verticalChainWeight: Float = Float.NaN
        set(value) {
            field = value
            tasks.add { state ->
                state.constraints(id).verticalChainWeight = value
            }
        }

    private fun addTransform(change: ConstraintReference.() -> Unit) =
        tasks.add { state -> change(state.constraints(id)) }

    private fun addFloatTransformFromDp(dpValue: Dp, change: ConstraintReference.(Float) -> Unit) =
        tasks.add { state ->
            (state as? State)?.also {
                state.constraints(id).change(state.convertDimension(dpValue).toFloat())
            }
        }

    /**
     * Adds both start and end links towards other [ConstraintLayoutBaseScope.VerticalAnchor]s.
     */
    fun linkTo(
        start: ConstraintLayoutBaseScope.VerticalAnchor,
        end: ConstraintLayoutBaseScope.VerticalAnchor,
        startMargin: Dp = 0.dp,
        endMargin: Dp = 0.dp,
        startGoneMargin: Dp = 0.dp,
        endGoneMargin: Dp = 0.dp,
        @FloatRange(from = 0.0, to = 1.0) bias: Float = 0.5f
    ) {
        this@ConstrainScope.start.linkTo(
            anchor = start,
            margin = startMargin,
            goneMargin = startGoneMargin
        )
        this@ConstrainScope.end.linkTo(anchor = end, margin = endMargin, goneMargin = endGoneMargin)
        tasks.add { state ->
            val resolvedBias = if (state.layoutDirection == LayoutDirection.Rtl) 1 - bias else bias
            state.constraints(id).horizontalBias(resolvedBias)
        }
    }

    /**
     * Adds both top and bottom links towards other [ConstraintLayoutBaseScope.HorizontalAnchor]s.
     */
    fun linkTo(
        top: ConstraintLayoutBaseScope.HorizontalAnchor,
        bottom: ConstraintLayoutBaseScope.HorizontalAnchor,
        topMargin: Dp = 0.dp,
        bottomMargin: Dp = 0.dp,
        topGoneMargin: Dp = 0.dp,
        bottomGoneMargin: Dp = 0.dp,
        @FloatRange(from = 0.0, to = 1.0) bias: Float = 0.5f
    ) {
        this@ConstrainScope.top.linkTo(anchor = top, margin = topMargin, goneMargin = topGoneMargin)
        this@ConstrainScope.bottom.linkTo(
            anchor = bottom,
            margin = bottomMargin,
            goneMargin = bottomGoneMargin
        )
        tasks.add { state ->
            state.constraints(id).verticalBias(bias)
        }
    }

    /**
     * Adds all start, top, end, bottom links towards
     * other [ConstraintLayoutBaseScope.HorizontalAnchor]s.
     */
    fun linkTo(
        start: ConstraintLayoutBaseScope.VerticalAnchor,
        top: ConstraintLayoutBaseScope.HorizontalAnchor,
        end: ConstraintLayoutBaseScope.VerticalAnchor,
        bottom: ConstraintLayoutBaseScope.HorizontalAnchor,
        startMargin: Dp = 0.dp,
        topMargin: Dp = 0.dp,
        endMargin: Dp = 0.dp,
        bottomMargin: Dp = 0.dp,
        startGoneMargin: Dp = 0.dp,
        topGoneMargin: Dp = 0.dp,
        endGoneMargin: Dp = 0.dp,
        bottomGoneMargin: Dp = 0.dp,
        @FloatRange(from = 0.0, to = 1.0) horizontalBias: Float = 0.5f,
        @FloatRange(from = 0.0, to = 1.0) verticalBias: Float = 0.5f
    ) {
        linkTo(
            start = start,
            end = end,
            startMargin = startMargin,
            endMargin = endMargin,
            startGoneMargin = startGoneMargin,
            endGoneMargin = endGoneMargin,
            bias = horizontalBias
        )
        linkTo(
            top = top,
            bottom = bottom,
            topMargin = topMargin,
            bottomMargin = bottomMargin,
            topGoneMargin = topGoneMargin,
            bottomGoneMargin = bottomGoneMargin,
            bias = verticalBias
        )
    }

    /**
     * Adds all start, top, end, bottom links towards the corresponding anchors of [other].
     * This will center the current layout inside or around (depending on size) [other].
     */
    fun centerTo(other: ConstrainedLayoutReference) {
        linkTo(other.start, other.top, other.end, other.bottom)
    }

    /**
     * Adds start and end links towards the corresponding anchors of [other].
     * This will center horizontally the current layout inside or around (depending on size)
     * [other].
     */
    fun centerHorizontallyTo(
        other: ConstrainedLayoutReference,
        @FloatRange(from = 0.0, to = 1.0) bias: Float = 0.5f
    ) {
        linkTo(start = other.start, end = other.end, bias = bias)
    }

    /**
     * Adds top and bottom links towards the corresponding anchors of [other].
     * This will center vertically the current layout inside or around (depending on size)
     * [other].
     */
    fun centerVerticallyTo(
        other: ConstrainedLayoutReference,
        @FloatRange(from = 0.0, to = 1.0) bias: Float = 0.5f
    ) {
        linkTo(other.top, other.bottom, bias = bias)
    }

    /**
     * Adds start and end links towards a vertical [anchor].
     * This will center the current layout around the vertical [anchor].
     */
    fun centerAround(anchor: ConstraintLayoutBaseScope.VerticalAnchor) {
        linkTo(anchor, anchor)
    }

    /**
     * Adds top and bottom links towards a horizontal [anchor].
     * This will center the current layout around the horizontal [anchor].
     */
    fun centerAround(anchor: ConstraintLayoutBaseScope.HorizontalAnchor) {
        linkTo(anchor, anchor)
    }

    /**
     * Set a circular constraint relative to the center of [other].
     * This will position the current widget at a relative angle and distance from [other].
     */
    fun circular(other: ConstrainedLayoutReference, angle: Float, distance: Dp) {
        tasks.add { state ->
            state.constraints(id)
                .circularConstraint(other.id, angle, state.convertDimension(distance).toFloat())
        }
    }

    /**
     * Clear the constraints on the horizontal axis (left, right, start, end).
     *
     * Useful when extending another [ConstraintSet] with unwanted constraints on this axis.
     */
    fun clearHorizontal() {
        tasks.add { state ->
            state.constraints(id).clearHorizontal()
        }
    }

    /**
     * Clear the constraints on the vertical axis (top, bottom, baseline).
     *
     * Useful when extending another [ConstraintSet] with unwanted constraints on this axis.
     */
    fun clearVertical() {
        tasks.add { state ->
            state.constraints(id).clearVertical()
        }
    }

    /**
     * Clear all constraints (vertical, horizontal, circular).
     *
     * Useful when extending another [ConstraintSet] with unwanted constraints applied.
     */
    fun clearConstraints() {
        tasks.add { state ->
            state.constraints(id).clearAll()
        }
    }

    /**
     * Resets the [width] and [height] to their default values.
     *
     * Useful when extending another [ConstraintSet] with unwanted dimensions.
     */
    fun resetDimensions() {
        tasks.add { state ->
            val defaultDimension =
                (Dimension.wrapContent as DimensionDescription).toSolverDimension(state)
            state.constraints(id)
                .width(defaultDimension)
                .height(defaultDimension)
        }
    }

    /**
     * Reset all render-time transforms of the content to their default values.
     *
     * Does not modify the [visibility] property.
     *
     * Useful when extending another [ConstraintSet] with unwanted transforms applied.
     */
    fun resetTransforms() {
        tasks.add { state ->
            state.constraints(id)
                .alpha(Float.NaN)
                .scaleX(Float.NaN)
                .scaleY(Float.NaN)
                .rotationX(Float.NaN)
                .rotationY(Float.NaN)
                .rotationZ(Float.NaN)
                .translationX(Float.NaN)
                .translationY(Float.NaN)
                .translationZ(Float.NaN)
                .pivotX(Float.NaN)
                .pivotY(Float.NaN)
        }
    }
}

/**
 * Represents a vertical side of a layout (i.e start and end) that can be anchored using
 * [linkTo] in their `Modifier.constrainAs` blocks.
 */
private class ConstraintVerticalAnchorable constructor(
    val id: Any,
    index: Int,
    tasks: MutableList<(State) -> Unit>
) : BaseVerticalAnchorable(tasks, index) {
    override fun getConstraintReference(state: State): ConstraintReference =
        state.constraints(id)
}

/**
 * Represents a horizontal side of a layout (i.e top and bottom) that can be anchored using
 * [linkTo] in their `Modifier.constrainAs` blocks.
 */
private class ConstraintHorizontalAnchorable constructor(
    val id: Any,
    index: Int,
    tasks: MutableList<(State) -> Unit>
) : BaseHorizontalAnchorable(tasks, index) {
    override fun getConstraintReference(state: State): ConstraintReference =
        state.constraints(id)
}

/**
 * Represents the [FirstBaseline] of a layout that can be anchored
 * using [linkTo] in their `Modifier.constrainAs` blocks.
 */
private class ConstraintBaselineAnchorable constructor(
    val id: Any,
    val tasks: MutableList<(State) -> Unit>
) : BaselineAnchorable {
    /**
     * Adds a link towards a [ConstraintLayoutBaseScope.BaselineAnchor].
     */
    override fun linkTo(
        anchor: ConstraintLayoutBaseScope.BaselineAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        tasks.add { state ->
            (state as? State)?.let {
                it.baselineNeededFor(id)
                it.baselineNeededFor(anchor.id)
            }
            with(state.constraints(id)) {
                baselineAnchorFunction.invoke(this, anchor.id).margin(margin).marginGone(goneMargin)
            }
        }
    }
}