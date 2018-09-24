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

package androidx.ui.widgets.basic

import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.painting.basictypes.Axis
import androidx.ui.painting.basictypes.VerticalDirection
import androidx.ui.rendering.flex.CrossAxisAlignment
import androidx.ui.rendering.flex.MainAxisAlignment
import androidx.ui.rendering.flex.MainAxisSize
import androidx.ui.rendering.flex.RenderFlex
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.MultiChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

/**
 * A widget that displays its children in a one-dimensional array.
 *
 * The [Flex] widget allows you to control the axis along which the children are
 * placed (horizontal or vertical). This is referred to as the _main axis_. If
 * you know the main axis in advance, then consider using a [Row] (if it's
 * horizontal) or [Column] (if it's vertical) instead, because that will be less
 * verbose.
 *
 * To cause a child to expand to fill the available vertical space, wrap the
 * child in an [Expanded] widget.
 *
 * The [Flex] widget does not scroll (and in general it is considered an error
 * to have more children in a [Flex] than will fit in the available room). If
 * you have some widgets and want them to be able to scroll if there is
 * insufficient room, consider using a [ListView].
 *
 * If you only have one child, then rather than using [Flex], [Row], or
 * [Column], consider using [Align] or [Center] to position the child.
 *
 * ## Layout algorithm
 *
 * _This section describes how a [Flex] is rendered by the framework._
 * _See [BoxConstraints] for an introduction to box layout models._
 *
 * Layout for a [Flex] proceeds in six steps:
 *
 * 1. Layout each child a null or zero flex factor (e.g., those that are not
 *    [Expanded]) with unbounded main axis constraints and the incoming
 *    cross axis constraints. If the [crossAxisAlignment] is
 *    [CrossAxisAlignment.STRETCH], instead use tight cross axis constraints
 *    that match the incoming max extent in the cross axis.
 * 2. Divide the remaining main axis space among the children with non-zero
 *    flex factors (e.g., those that are [Expanded]) according to their flex
 *    factor. For example, a child with a flex factor of 2.0 will receive twice
 *    the amount of main axis space as a child with a flex factor of 1.0.
 * 3. Layout each of the remaining children with the same cross axis
 *    constraints as in step 1, but instead of using unbounded main axis
 *    constraints, use max axis constraints based on the amount of space
 *    allocated in step 2. Children with [Flexible.fit] properties that are
 *    [FlexFit.TIGHT] are given tight constraints (i.e., forced to fill the
 *    allocated space), and children with [Flexible.fit] properties that are
 *    [FlexFit.LOOSE] are given loose constraints (i.e., not forced to fill the
 *    allocated space).
 * 4. The cross axis extent of the [Flex] is the maximum cross axis extent of
 *    the children (which will always satisfy the incoming constraints).
 * 5. The main axis extent of the [Flex] is determined by the [mainAxisSize]
 *    property. If the [mainAxisSize] property is [MainAxisAlignment.MAX], then the
 *    main axis extent of the [Flex] is the max extent of the incoming main
 *    axis constraints. If the [mainAxisSize] property is [MainAxisAlignment.MIN],
 *    then the main axis extent of the [Flex] is the sum of the main axis
 *    extents of the children (subject to the incoming constraints).
 * 6. Determine the position for each child according to the
 *    [mainAxisAlignment] and the [crossAxisAlignment]. For example, if the
 *    [mainAxisAlignment] is [MainAxisAlignment.SPACE_BETWEEN], any main axis
 *    space that has not been allocated to children is divided evenly and
 *    placed between the children.
 *
 * See also:
 *
 *  * [Row], for a version of this widget that is always horizontal.
 *  * [Column], for a version of this widget that is always vertical.
 *  * [Expanded], to indicate children that should take all the remaining room.
 *  * [Flexible], to indicate children that should share the remaining room but
 *  * [Spacer], a widget that takes up space proportional to it's flex value.
 *    that may be sized smaller (leaving some remaining room unused).
 *  * The [catalog of layout widgets](https://flutter.io/widgets/layout/).
 * In the primary constructor:
 *
 * The [direction] is required.
 *
 * The [direction], [mainAxisAlignment], [crossAxisAlignment], and
 * [verticalDirection] arguments must not be null. If [crossAxisAlignment] is
 * [CrossAxisAlignment.BASELINE], then [textBaseline] must not be null.
 *
 * The [textDirection] argument defaults to the ambient [Directionality], if
 * any. If there is no ambient directionality, and a text direction is going
 * to be necessary to decide which direction to lay the children in or to
 * disambiguate `start` or `end` values for the main or cross axis
 * directions, the [textDirection] must not be null.
 */
open class Flex(
    key: Key,
    /**
     * The direction to use as the main axis.
     *
     * If you know the axis in advance, then consider using a [Row] (if it's
     * horizontal) or [Column] (if it's vertical) instead of a [Flex], since that
     * will be less verbose. (For [Row] and [Column] this property is fixed to
     * the appropriate axis.)
     */
    private val direction: Axis,
    /**
     * How the children should be placed along the main axis.
     *
     * For example, [MainAxisAlignment.START], the default, places the children
     * at the start (i.e., the left for a [Row] or the top for a [Column]) of the
     * main axis.
     */
    private val mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.START,
    /**
     * How much space should be occupied in the main axis.
     *
     * After allocating space to children, there might be some remaining free
     * space. This value controls whether to maximize or minimize the amount of
     * free space, subject to the incoming layout constraints.
     *
     * If some children have a non-zero flex factors (and none have a fit of
     * [FlexFit.LOOSE]), they will expand to consume all the available space and
     * there will be no remaining free space to maximize or minimize, making this
     * value irrelevant to the final layout.
     */
    private val mainAxisSize: MainAxisSize = MainAxisSize.MAX,
    /**
     * How the children should be placed along the cross axis.
     *
     * For example, [CrossAxisAlignment.CENTER], the default, centers the
     * children in the cross axis (e.g., horizontally for a [Column]).
     */
    private val crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.CENTER,
    /**
     * Determines the order to lay children out horizontally and how to interpret
     * `start` and `end` in the horizontal direction.
     *
     * Defaults to the ambient [Directionality].
     *
     * If the [direction] is [Axis.HORIZONTAL], this controls the order in which
     * the children are positioned (left-to-right or right-to-left), and the
     * meaning of the [mainAxisAlignment] property's [MainAxisAlignment.START] and
     * [MainAxisAlignment.END] values.
     *
     * If the [direction] is [Axis.HORIZONTAL], and either the
     * [mainAxisAlignment] is either [MainAxisAlignment.START] or
     * [MainAxisAlignment.END], or there's more than one child, then the
     * [textDirection] (or the ambient [Directionality]) must not be null.
     *
     * If the [direction] is [Axis.VERTICAL], this controls the meaning of the
     * [crossAxisAlignment] property's [CrossAxisAlignment.START] and
     * [CrossAxisAlignment.END] values.
     *
     * If the [direction] is [Axis.VERTICAL], and the [crossAxisAlignment] is
     * either [CrossAxisAlignment.START] or [CrossAxisAlignment.END], then the
     * [textDirection] (or the ambient [Directionality]) must not be null.
     */
    private val textDirection: TextDirection?,
    /**
     * Determines the order to lay children out vertically and how to interpret
     * `start` and `end` in the vertical direction.
     *
     * Defaults to [VerticalDirection.DOWN].
     *
     * If the [direction] is [Axis.VERTICAL], this controls which order children
     * are painted in (down or up), the meaning of the [mainAxisAlignment]
     * property's [MainAxisAlignment.START] and [MainAxisAlignment.END] values.
     *
     * If the [direction] is [Axis.VERTICAL], and either the [mainAxisAlignment]
     * is either [MainAxisAlignment.START] or [MainAxisAlignment.END], or there's
     * more than one child, then the [verticalDirection] must not be null.
     *
     * If the [direction] is [Axis.HORIZONTAL], this controls the meaning of the
     * [crossAxisAlignment] property's [CrossAxisAlignment.START] and
     * [CrossAxisAlignment.END] values.
     *
     * If the [direction] is [Axis.HORIZONTAL], and the [crossAxisAlignment] is
     * either [CrossAxisAlignment.START] or [CrossAxisAlignment.END], then the
     * [verticalDirection] must not be null.
     */
    private val verticalDirection: VerticalDirection = VerticalDirection.DOWN,
    /** If aligning items according to their baseline, which baseline to use. */
    private val textBaseline: TextBaseline?,
    children: List<Widget>
) : MultiChildRenderObjectWidget(key, children) {

    private val _needTextDirection: Boolean
        get() {
            assert(direction != null)
            return when (direction) {
                Axis.HORIZONTAL -> true
                Axis.VERTICAL -> {
                    assert(crossAxisAlignment != null)
                    crossAxisAlignment == CrossAxisAlignment.START ||
                            crossAxisAlignment == CrossAxisAlignment.END
                }
            }
        }

    /**
     * The value to pass to [RenderFlex.textDirection].
     *
     * This value is derived from the [textDirection] property and the ambient
     * [Directionality]. The value is null if there is no need to specify the
     * text direction. In practice there's always a need to specify the direction
     * except for vertical flexes (e.g. [Column]s) whose [crossAxisAlignment] is
     * not dependent on the text direction (not `start` or `end`). In particular,
     * a [Row] always needs a text direction because the text direction controls
     * its layout order. (For [Column]s, the layout order is controlled by
     * [verticalDirection], which is always specified as it does not depend on an
     * inherited widget and defaults to [VerticalDirection.DOWN].)
     *
     * This method exists so that subclasses of [Flex] that create their own
     * render objects that are derived from [RenderFlex] can do so and still use
     * the logic for providing a text direction only when it is necessary.
     */
    protected fun getEffectiveTextDirection(context: BuildContext): TextDirection? {
        return textDirection ?: (if (_needTextDirection) Directionality.of(context) else null)
    }

    override fun createRenderObject(context: BuildContext): RenderFlex {
        return RenderFlex(
            _direction = direction,
            _mainAxisAlignment = mainAxisAlignment,
            _mainAxisSize = mainAxisSize,
            _crossAxisAlignment = crossAxisAlignment,
            _textDirection = getEffectiveTextDirection(context),
            _verticalDirection = verticalDirection,
            _textBaseline = textBaseline
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) {
        renderObject?.let {
            it as RenderFlex
            it.direction = direction
            it.mainAxisAlignment = mainAxisAlignment
            it.mainAxisSize = mainAxisSize
            it.crossAxisAlignment = crossAxisAlignment
            it.textDirection = getEffectiveTextDirection(context)
            it.verticalDirection = verticalDirection
            it.textBaseline = textBaseline
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty("direction", direction))
        properties.add(EnumProperty("mainAxisAlignment", mainAxisAlignment))
        properties.add(EnumProperty("mainAxisSize", mainAxisSize, defaultValue = MainAxisSize.MAX))
        properties.add(EnumProperty("crossAxisAlignment", crossAxisAlignment))
        properties.add(EnumProperty("textDirection", textDirection, defaultValue = null))
        properties.add(EnumProperty("verticalDirection", verticalDirection,
            defaultValue = VerticalDirection.DOWN))
        properties.add(EnumProperty("textBaseline", textBaseline, defaultValue = null))
    }
}