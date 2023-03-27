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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLNumber
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLString
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * Scope that can be used to constrain a layout.
 *
 * Used within `Modifier.constrainAs` from the inline DSL API. And within `constrain` from the
 * ConstraintSet-based API.
 */
@LayoutScopeMarker
@Stable
class ConstrainScope internal constructor(
    internal val id: Any,
    internal val containerObject: CLObject
) {
    /**
     * Reference to the [ConstraintLayout] itself, which can be used to specify constraints
     * between itself and its children.
     */
    val parent = ConstrainedLayoutReference("parent")

    /**
     * The start anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val start: VerticalAnchorable = ConstraintVerticalAnchorable(-2, containerObject)

    /**
     * The left anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteLeft: VerticalAnchorable = ConstraintVerticalAnchorable(0, containerObject)

    /**
     * The top anchor of the layout - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val top: HorizontalAnchorable = ConstraintHorizontalAnchorable(0, containerObject)

    /**
     * The end anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val end: VerticalAnchorable = ConstraintVerticalAnchorable(-1, containerObject)

    /**
     * The right anchor of the layout - can be constrained using [VerticalAnchorable.linkTo].
     */
    val absoluteRight: VerticalAnchorable = ConstraintVerticalAnchorable(1, containerObject)

    /**
     * The bottom anchor of the layout - can be constrained using [HorizontalAnchorable.linkTo].
     */
    val bottom: HorizontalAnchorable = ConstraintHorizontalAnchorable(1, containerObject)

    /**
     * The [FirstBaseline] of the layout - can be constrained using [BaselineAnchorable.linkTo].
     */
    val baseline: BaselineAnchorable = ConstraintBaselineAnchorable(containerObject)

    /**
     * The width of the [ConstraintLayout] child.
     */
    var width: Dimension by DimensionProperty(Dimension.wrapContent)

    /**
     * The height of the [ConstraintLayout] child.
     */
    var height: Dimension by DimensionProperty(Dimension.wrapContent)

    /**
     * The overall visibility of the [ConstraintLayout] child.
     *
     * [Visibility.Visible] by default.
     */
    var visibility: Visibility by object : ObservableProperty<Visibility>(Visibility.Visible) {
        override fun afterChange(
            property: KProperty<*>,
            oldValue: Visibility,
            newValue: Visibility
        ) {
            containerObject.putString(property.name, newValue.name)
        }
    }

    /**
     * The transparency value when rendering the content.
     */
    @FloatRange(from = 0.0, to = 1.0)
    var alpha: Float = 1.0f
        set(value) {
            // FloatRange annotation doesn't work with delegate objects
            field = value
            if (!value.isNaN()) {
                containerObject.putNumber("alpha", value)
            }
        }

    /**
     * The percent scaling value on the horizontal axis. Where 1 is 100%.
     */
    var scaleX: Float by FloatProperty(1.0f)

    /**
     * The percent scaling value on the vertical axis. Where 1 is 100%.
     */
    var scaleY: Float by FloatProperty(1.0f)

    /**
     * The degrees to rotate the content over the horizontal axis.
     */
    var rotationX: Float by FloatProperty(0.0f)

    /**
     * The degrees to rotate the content over the vertical axis.
     */
    var rotationY: Float by FloatProperty(0.0f)

    /**
     * The degrees to rotate the content on the screen plane.
     */
    var rotationZ: Float by FloatProperty(0.0f)

    /**
     * The distance to offset the content over the X axis.
     */
    var translationX: Dp by DpProperty(0.dp)

    /**
     * The distance to offset the content over the Y axis.
     */
    var translationY: Dp by DpProperty(0.dp)

    /**
     * The distance to offset the content over the Z axis.
     */
    var translationZ: Dp by DpProperty(0.dp)

    /**
     * The X axis offset percent where the content is rotated and scaled.
     *
     * @see [TransformOrigin]
     */
    var pivotX: Float by FloatProperty(0.5f)

    /**
     * The Y axis offset percent where the content is rotated and scaled.
     *
     * @see [TransformOrigin]
     */
    var pivotY: Float by FloatProperty(0.5f)

    /**
     * Whenever the width is not fixed, this weight may be used by an horizontal Chain to decide how
     * much space assign to this widget.
     */
    var horizontalChainWeight: Float by FloatProperty(Float.NaN, "hWeight")

    /**
     * Whenever the height is not fixed, this weight may be used by a vertical Chain to decide how
     * much space assign to this widget.
     */
    var verticalChainWeight: Float by FloatProperty(Float.NaN, "vWeight")

    /**
     * Applied when the widget has constraints on the [start] and [end] anchors. It defines the
     * position of the widget relative to the space within the constraints, where `0f` is the
     * left-most position and `1f` is the right-most position.
     *
     * &nbsp;
     *
     * When layout direction is RTL, the value of the bias is effectively inverted.
     *
     * E.g.: For `horizontalBias = 0.3f`, `0.7f` is used for RTL.
     *
     * &nbsp;
     *
     * Note that the bias may also be applied with calls such as [linkTo].
     */
    @FloatRange(from = 0.0, to = 1.0)
    var horizontalBias: Float = 0.5f
        set(value) {
            // FloatRange annotation doesn't work with delegate objects
            field = value
            if (!value.isNaN()) {
                containerObject.putNumber("hBias", value)
            }
        }

    /**
     * Applied when the widget has constraints on the [top] and [bottom] anchors. It defines the
     * position of the widget relative to the space within the constraints, where `0f` is the
     * top-most position and `1f` is the bottom-most position.
     */
    @FloatRange(from = 0.0, to = 1.0)
    var verticalBias: Float = 0.5f
        set(value) {
            // FloatRange annotation doesn't work with delegate objects
            field = value
            if (!value.isNaN()) {
                containerObject.putNumber("vBias", value)
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
        this@ConstrainScope.end.linkTo(
            anchor = end,
            margin = endMargin,
            goneMargin = endGoneMargin
        )
        containerObject.putNumber("hRtlBias", bias)
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
        this@ConstrainScope.top.linkTo(
            anchor = top,
            margin = topMargin,
            goneMargin = topGoneMargin
        )
        this@ConstrainScope.bottom.linkTo(
            anchor = bottom,
            margin = bottomMargin,
            goneMargin = bottomGoneMargin
        )
        containerObject.putNumber("vBias", bias)
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
        val circularParams = CLArray(charArrayOf()).apply {
            add(CLString.from(other.id.toString()))
            add(CLNumber(angle))
            add(CLNumber(distance.value))
        }
        containerObject.put("circular", circularParams)
    }

    /**
     * Clear the constraints on the horizontal axis (left, right, start, end).
     *
     * Useful when extending another [ConstraintSet] with unwanted constraints on this axis.
     */
    fun clearHorizontal() {
        containerObject.remove("left")
        containerObject.remove("right")
        containerObject.remove("start")
        containerObject.remove("end")
    }

    /**
     * Clear the constraints on the vertical axis (top, bottom, baseline).
     *
     * Useful when extending another [ConstraintSet] with unwanted constraints on this axis.
     */
    fun clearVertical() {
        containerObject.remove("top")
        containerObject.remove("bottom")
        containerObject.remove("baseline")
    }

    /**
     * Clear all constraints (vertical, horizontal, circular).
     *
     * Useful when extending another [ConstraintSet] with unwanted constraints applied.
     */
    fun clearConstraints() {
        clearHorizontal()
        clearVertical()
        containerObject.remove("circular")
    }

    /**
     * Resets the [width] and [height] to their default values.
     *
     * Useful when extending another [ConstraintSet] with unwanted dimensions.
     */
    fun resetDimensions() {
        width = Dimension.wrapContent
        height = Dimension.wrapContent
    }

    /**
     * Reset all render-time transforms of the content to their default values.
     *
     * Does not modify the [visibility] property.
     *
     * Useful when extending another [ConstraintSet] with unwanted transforms applied.
     */
    fun resetTransforms() {
        containerObject.remove("alpha")
        containerObject.remove("scaleX")
        containerObject.remove("scaleY")
        containerObject.remove("rotationX")
        containerObject.remove("rotationY")
        containerObject.remove("rotationZ")
        containerObject.remove("translationX")
        containerObject.remove("translationY")
        containerObject.remove("translationZ")
        containerObject.remove("pivotX")
        containerObject.remove("pivotY")
    }

    /**
     * Convenience extension variable to parse a [Dp] as a [Dimension] object.
     *
     * @see Dimension.value
     */
    val Dp.asDimension: Dimension
        get() = Dimension.value(this)

    private inner class DimensionProperty(initialValue: Dimension) :
        ObservableProperty<Dimension>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: Dimension, newValue: Dimension) {
            containerObject.put(property.name, (newValue as DimensionDescription).asCLElement())
        }
    }

    private inner class FloatProperty(
        initialValue: Float,
        private val nameOverride: String? = null
    ) : ObservableProperty<Float>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: Float, newValue: Float) {
            if (!newValue.isNaN()) {
                containerObject.putNumber(nameOverride ?: property.name, newValue)
            }
        }
    }

    private inner class DpProperty(
        initialValue: Dp,
        private val nameOverride: String? = null
    ) : ObservableProperty<Dp>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: Dp, newValue: Dp) {
            if (!newValue.value.isNaN()) {
                containerObject.putNumber(nameOverride ?: property.name, newValue.value)
            }
        }
    }
}

/**
 * Represents a vertical side of a layout (i.e start and end) that can be anchored using
 * [linkTo] in their `Modifier.constrainAs` blocks.
 */
private class ConstraintVerticalAnchorable constructor(
    index: Int,
    containerObject: CLObject
) : BaseVerticalAnchorable(containerObject, index)

/**
 * Represents a horizontal side of a layout (i.e top and bottom) that can be anchored using
 * [linkTo] in their `Modifier.constrainAs` blocks.
 */
private class ConstraintHorizontalAnchorable constructor(
    index: Int,
    containerObject: CLObject
) : BaseHorizontalAnchorable(containerObject, index)

/**
 * Represents the [FirstBaseline] of a layout that can be anchored
 * using [linkTo] in their `Modifier.constrainAs` blocks.
 */
private class ConstraintBaselineAnchorable constructor(
    private val containerObject: CLObject
) : BaselineAnchorable {
    /**
     * Adds a link towards a [ConstraintLayoutBaseScope.BaselineAnchor].
     */
    override fun linkTo(
        anchor: ConstraintLayoutBaseScope.BaselineAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        val constraintArray = CLArray(charArrayOf()).apply {
            add(CLString.from(anchor.id.toString()))
            add(CLString.from("baseline"))
            add(CLNumber(margin.value))
            add(CLNumber(goneMargin.value))
        }
        containerObject.put("baseline", constraintArray)
    }

    /**
     * Adds a link towards a [ConstraintLayoutBaseScope.HorizontalAnchor].
     */
    override fun linkTo(
        anchor: ConstraintLayoutBaseScope.HorizontalAnchor,
        margin: Dp,
        goneMargin: Dp
    ) {
        val targetAnchorName = AnchorFunctions.horizontalAnchorIndexToAnchorName(anchor.index)
        val constraintArray = CLArray(charArrayOf()).apply {
            add(CLString.from(anchor.id.toString()))
            add(CLString.from(targetAnchorName))
            add(CLNumber(margin.value))
            add(CLNumber(goneMargin.value))
        }
        containerObject.put("baseline", constraintArray)
    }
}