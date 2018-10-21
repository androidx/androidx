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

package androidx.ui.rendering.flex

import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.painting.basictypes.Axis
import androidx.ui.painting.basictypes.VerticalDirection
import androidx.ui.painting.basictypes.flip
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.BoxParentData
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.ContainerParentDataMixin
import androidx.ui.rendering.obj.ContainerRenderObjectMixin
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.runtimeType
import kotlin.math.max

internal typealias ChildSizingFunction = (child: RenderBox, extent: Double) -> Double

internal fun startIsTopLeft(
    direction: Axis,
    textDirection: TextDirection?,
    verticalDirection: VerticalDirection?
) = when (direction) {
    // If the relevant value of textDirection or verticalDirection is null, this returns null too.
    Axis.HORIZONTAL -> if (textDirection != null) textDirection == TextDirection.LTR else null
    Axis.VERTICAL ->
        if (verticalDirection != null) verticalDirection == VerticalDirection.DOWN else null
}

/**
 * Displays its children in a one-dimensional array.
 *
 * ## Layout algorithm
 *
 * _This section describes how the framework causes [RenderFlex] to position
 * its children._
 * _See [BoxConstraints] for an introduction to box layout models._
 *
 * Layout for a [RenderFlex] proceeds in six steps:
 *
 * 1. Layout each child a null or zero flex factor with unbounded main axis
 *    constraints and the incoming cross axis constraints. If the
 *    [crossAxisAlignment] is [CrossAxisAlignment.STRETCH], instead use tight
 *    cross axis constraints that match the incoming max extent in the cross
 *    axis.
 * 2. Divide the remaining main axis space among the children with non-zero
 *    flex factors according to their flex factor. For example, a child with a
 *    flex factor of 2.0 will receive twice the amount of main axis space as a
 *    child with a flex factor of 1.0.
 * 3. Layout each of the remaining children with the same cross axis
 *    constraints as in step 1, but instead of using unbounded main axis
 *    constraints, use max axis constraints based on the amount of space
 *    allocated in step 2. Children with [Flexible.fit] properties that are
 *    [FlexFit.TIGHT] are given tight constraints (i.e., forced to fill the
 *    allocated space), and children with [Flexible.fit] properties that are
 *    [FlexFit.LOOSE] are given loose constraints (i.e., not forced to fill the
 *    allocated space).
 * 4. The cross axis extent of the [RenderFlex] is the maximum cross axis
 *    extent of the children (which will always satisfy the incoming
 *    constraints).
 * 5. The main axis extent of the [RenderFlex] is determined by the
 *    [mainAxisSize] property. If the [mainAxisSize] property is
 *    [MainAxisAlignment.MAX], then the main axis extent of the [RenderFlex] is the
 *    max extent of the incoming main axis constraints. If the [mainAxisSize]
 *    property is [MainAxisAlignment.MIN], then the main axis extent of the [Flex]
 *    is the sum of the main axis extents of the children (subject to the
 *    incoming constraints).
 * 6. Determine the position for each child according to the
 *    [mainAxisAlignment] and the [crossAxisAlignment]. For example, if the
 *    [mainAxisAlignment] is [MainAxisAlignment.SPACE_BETWEEN], any main axis
 *    space that has not been allocated to children is divided evenly and
 *    placed between the children.
 *
 * See also:
 *
 *  * [Flex], the widget equivalent.
 *  * [Row] and [Column], direction-specific variants of [Flex].
 * The primary constructor:
 *
 * By default, the flex layout is horizontal and children are aligned to the
 * start of the main axis and the center of the cross axis.
 */
// TODO(migration/Mihai): inheriting from ContainerRenderObjectMixin to workaround mixins
class RenderFlex(
    children: List<RenderBox>? = null,
    private var _direction: Axis = Axis.HORIZONTAL,
    private var _mainAxisSize: MainAxisSize = MainAxisSize.MAX,
    private var _mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.START,
    private var _crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.CENTER,
    private var _textDirection: TextDirection? = null,
    private var _verticalDirection: VerticalDirection = VerticalDirection.DOWN,
    private var _textBaseline: TextBaseline? = null
) : ContainerRenderObjectMixin<RenderBox, FlexParentData>()
    /* with RenderBoxContainerDefaultsMixin<RenderBox, FlexParentData> -- this was
     * copy pasted to ContainerRenderObjectMixin, so the logic exists */
    /* DebugOverflowIndicatorMixin*/ {

    /** The direction to use as the main axis. */
    var direction: Axis
        get() = _direction
        set(value) {
            if (_direction != value) {
                _direction = value
                markNeedsLayout()
            }
        }

    /**
     * How the children should be placed along the main axis.
     *
     * If the [direction] is [Axis.HORIZONTAL], and the [mainAxisAlignment] is
     * either [MainAxisAlignment.START] or [MainAxisAlignment.END], then the
     * [textDirection] must not be null.
     *
     * If the [direction] is [Axis.VERTICAL], and the [mainAxisAlignment] is
     * either [MainAxisAlignment.START] or [MainAxisAlignment.END], then the
     * [verticalDirection] must not be null.
     */
    var mainAxisAlignment: MainAxisAlignment
        get() = _mainAxisAlignment
        set(value) {
            if (_mainAxisAlignment != value) {
                _mainAxisAlignment = value
                markNeedsLayout()
            }
        }

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
    var mainAxisSize: MainAxisSize
        get() = _mainAxisSize
        set(value) {
            if (_mainAxisSize != value) {
                _mainAxisSize = value
                markNeedsLayout()
            }
        }

    /**
     * How the children should be placed along the cross axis.
     *
     * If the [direction] is [Axis.HORIZONTAL], and the [crossAxisAlignment] is
     * either [CrossAxisAlignment.START] or [CrossAxisAlignment.END], then the
     * [verticalDirection] must not be null.
     *
     * If the [direction] is [Axis.VERTICAL], and the [crossAxisAlignment] is
     * either [CrossAxisAlignment.START] or [CrossAxisAlignment.END], then the
     * [textDirection] must not be null.
     */
    var crossAxisAlignment: CrossAxisAlignment
        get() = _crossAxisAlignment
        set(value) {
            if (_crossAxisAlignment != value) {
                _crossAxisAlignment = value
                markNeedsLayout()
            }
        }

    /**
     * Determines the order to lay children out horizontally and how to interpret
     * `start` and `end` in the horizontal direction.
     *
     * If the [direction] is [Axis.HORIZONTAL], this controls the order in which
     * children are positioned (left-to-right or right-to-left), and the meaning
     * of the [mainAxisAlignment] property's [MainAxisAlignment.START] and
     * [MainAxisAlignment.END] values.
     *
     * If the [direction] is [Axis.HORIZONTAL], and either the
     * [mainAxisAlignment] is either [MainAxisAlignment.START] or
     * [MainAxisAlignment.END], or there's more than one child, then the
     * [textDirection] must not be null.
     *
     * If the [direction] is [Axis.VERTICAL], this controls the meaning of the
     * [crossAxisAlignment] property's [CrossAxisAlignment.START] and
     * [CrossAxisAlignment.END] values.
     *
     * If the [direction] is [Axis.VERTICAL], and the [crossAxisAlignment] is
     * either [CrossAxisAlignment.START] or [CrossAxisAlignment.END], then the
     * [textDirection] must not be null.
     */
    var textDirection: TextDirection?
        get() = _textDirection
        set(value) {
            if (_textDirection != value) {
                _textDirection = value
                markNeedsLayout()
            }
        }

    /**
     * Determines the order to lay children out vertically and how to interpret
     * `start` and `end` in the vertical direction.
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
    var verticalDirection: VerticalDirection
        get() = _verticalDirection
        set(value) {
            if (_verticalDirection != value) {
                _verticalDirection = value
                markNeedsLayout()
            }
        }

    /**
     * If aligning items according to their baseline, which baseline to use.
     *
     * Must not be null if [crossAxisAlignment] is [CrossAxisAlignment.BASELINE].
     */
    var textBaseline: TextBaseline?
        get() = _textBaseline
        set(value) {
            assert(_crossAxisAlignment != CrossAxisAlignment.BASELINE || value != null)
            if (_textBaseline != value) {
                _textBaseline = value
                markNeedsLayout()
            }
        }

    init {
        addAll(children)
    }

    // TODO(migration/Mihai): complete this
//    bool get _debugHasNecessaryDirections {
//        assert(direction != null);
//        assert(crossAxisAlignment != null);
//        if (firstChild != null && lastChild != firstChild) {
//            // i.e. there's more than one child
//            switch (direction) {
//                case Axis.HORIZONTAL:
//                assert(textDirection != null, 'Horizontal $runtimeType with multiple children has a null textDirection, so the layout order is undefined.');
//                break;
//                case Axis.VERTICAL:
//                assert(verticalDirection != null, 'Vertical $runtimeType with multiple children has a null verticalDirection, so the layout order is undefined.');
//                break;
//            }
//        }
//        if (mainAxisAlignment == MainAxisAlignment.START ||
//                mainAxisAlignment == MainAxisAlignment.END) {
//            switch (direction) {
//                case Axis.HORIZONTAL:
//                assert(textDirection != null, 'Horizontal $runtimeType with $mainAxisAlignment has a null textDirection, so the alignment cannot be resolved.');
//                break;
//                case Axis.VERTICAL:
//                assert(verticalDirection != null, 'Vertical $runtimeType with $mainAxisAlignment has a null verticalDirection, so the alignment cannot be resolved.');
//                break;
//            }
//        }
//        if (crossAxisAlignment == CrossAxisAlignment.START ||
//                crossAxisAlignment == CrossAxisAlignment.END) {
//            switch (direction) {
//                case Axis.HORIZONTAL:
//                assert(verticalDirection != null, 'Horizontal $runtimeType with $crossAxisAlignment has a null verticalDirection, so the alignment cannot be resolved.');
//                break;
//                case Axis.VERTICAL:
//                assert(textDirection != null, 'Vertical $runtimeType with $crossAxisAlignment has a null textDirection, so the alignment cannot be resolved.');
//                break;
//            }
//        }
//        return true;
//    }

    // Set during layout if overflow occurred on the main axis.
    private var overflow: Double = 0.0

    override fun setupParentData(child: RenderObject) {
        if (child.parentData !is FlexParentData) {
            child.parentData = FlexParentData()
        }
    }

    internal fun getIntrinsicSize(
        sizingDirection: Axis,
        extent: Double, // the extent in the direction that isn't the sizing direction
        childSize: ChildSizingFunction // a method to find the size in the sizing direction
    ): Double {
        if (_direction == sizingDirection) {
            // INTRINSIC MAIN SIZE
            // Intrinsic main size is the smallest size the flex container can take
            // while maintaining the min/max-content contributions of its flex items.
            var totalFlex = 0.0
            var inflexibleSpace = 0.0
            var maxFlexFractionSoFar = 0.0
            var child = firstChild
            while (child != null) {
                // Smart cast doesn't work here.
                val renderBoxChild = child as RenderBox

                val flex = getFlex(renderBoxChild)
                totalFlex += flex
                if (flex > 0) {
                    val flexFraction = childSize(renderBoxChild, extent) / getFlex(renderBoxChild)
                    maxFlexFractionSoFar = max(maxFlexFractionSoFar, flexFraction)
                } else {
                    inflexibleSpace += childSize(renderBoxChild, extent)
                }

                val childParentData =
                    renderBoxChild.parentData as ContainerParentDataMixin<RenderBox>
                child = childParentData.nextSibling
            }
            return maxFlexFractionSoFar * totalFlex + inflexibleSpace
        } else {
            // INTRINSIC CROSS SIZE
            // Intrinsic cross size is the max of the intrinsic cross sizes of the
            // children, after the flexible children are fit into the available space,
            // with the children sized using their max intrinsic dimensions.
            // TODO(ianh): Support baseline alignment.

            // Get inflexible space using the max intrinsic dimensions of fixed children in the main direction.
            val availableMainSpace = extent
            var totalFlex = 0
            var inflexibleSpace = 0.0
            var maxCrossSize = 0.0
            var child = firstChild
            while (child != null) {
                // Smart cast doesn't work here.
                val renderBoxChild = child as RenderBox

                val flex = getFlex(renderBoxChild)
                totalFlex += flex
                val mainSize: Double
                val crossSize: Double
                if (flex == 0) {
                    when (_direction) {
                        Axis.HORIZONTAL -> {
                            mainSize = renderBoxChild.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)
                            crossSize = childSize(renderBoxChild, mainSize)
                        }
                        Axis.VERTICAL -> {
                            mainSize = renderBoxChild
                                .getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)
                            crossSize = childSize(renderBoxChild, mainSize)
                        }
                    }
                    inflexibleSpace += mainSize
                    maxCrossSize = max(maxCrossSize, crossSize)
                }

                val childParentData =
                    renderBoxChild.parentData as ContainerParentDataMixin<RenderBox>
                child = childParentData.nextSibling
            }

            // Determine the spacePerFlex by allocating the remaining available space.
            val spacePerFlex = max(0.0, (availableMainSpace - inflexibleSpace) / totalFlex)

            // Size remaining (flexible) items, find the maximum cross size.
            child = firstChild
            while (child != null) {
                // Smart cast doesn't work here.
                val renderBoxChild = child as RenderBox

                val flex = getFlex(renderBoxChild)
                if (flex > 0)
                    maxCrossSize = max(maxCrossSize, childSize(renderBoxChild, spacePerFlex * flex))

                val childParentData =
                    renderBoxChild.parentData as ContainerParentDataMixin<RenderBox>
                child = childParentData.nextSibling
            }

            return maxCrossSize
        }
    }

    override fun computeMinIntrinsicWidth(height: Double) = getIntrinsicSize(
        sizingDirection = Axis.HORIZONTAL,
        extent = height,
        childSize = { child, extent -> child.getMinIntrinsicWidth(extent) }
    )

    override fun computeMaxIntrinsicWidth(height: Double) = getIntrinsicSize(
        sizingDirection = Axis.HORIZONTAL,
        extent = height,
        childSize = { child, extent -> child.getMaxIntrinsicWidth(extent) }
    )

    override fun computeMinIntrinsicHeight(width: Double) = getIntrinsicSize(
        sizingDirection = Axis.VERTICAL,
        extent = width,
        childSize = { child, extent -> child.getMinIntrinsicHeight(extent) }
    )

    override fun computeMaxIntrinsicHeight(width: Double) = getIntrinsicSize(
        sizingDirection = Axis.VERTICAL,
        extent = width,
        childSize = { child, extent -> child.getMaxIntrinsicHeight(extent) }
    )

    // TODO(migration/Mihai): add baselines
//    override fun computeDistanceToActualBaseline(baseline: TextBaseline): Double {
//        if (_direction == Axis.HORIZONTAL)
//            return defaultComputeDistanceToHighestActualBaseline(baseline);
//        return defaultComputeDistanceToFirstActualBaseline(baseline);
//    }

    internal fun getFlex(child: RenderBox): Int {
        val childParentData = child.parentData as FlexParentData
        return childParentData.flex
    }

    internal fun getFit(child: RenderBox): FlexFit {
        val childParentData = child.parentData as FlexParentData
        return childParentData.fit
    }

    internal fun getCrossSize(child: RenderBox) = when (_direction) {
        Axis.HORIZONTAL -> child.size.height
        Axis.VERTICAL -> child.size.width
    }

    internal fun getMainSize(child: RenderBox) = when (_direction) {
        Axis.HORIZONTAL -> child.size.width
        Axis.VERTICAL -> child.size.height
    }

    override fun performLayout() {
        // TODO(migration/Mihai): uncomment
//        assert(_debugHasNecessaryDirections);
        // Determine used flex factor, size inflexible items, calculate free space.
        var totalFlex = 0
        var totalChildren = 0

        assert(constraints != null)
        val maxMainSize = if (_direction == Axis.HORIZONTAL) {
            constraints!!.maxWidth
        } else {
            constraints!!.maxHeight
        }

        val canFlex = maxMainSize < Double.POSITIVE_INFINITY
        var crossSize = 0.0
        var allocatedSize = 0.0; // Sum of the sizes of the non-flexible children.
        var child = firstChild
        var lastFlexChild: RenderBox? = null
        while (child != null) {
            val childParentData = child.parentData
            totalChildren++
            val flex = getFlex(child)
            if (flex > 0) {
                // TODO(migration/MIHAI)
//                assert(() {
//                    final String identity = _direction == Axis.HORIZONTAL ? 'row' : 'column';
//                    final String axis = _direction == Axis.HORIZONTAL ? 'horizontal' : 'vertical';
//                    final String dimension = _direction == Axis.HORIZONTAL ? 'width' : 'height';
//                    String error, message;
//                    String addendum = '';
//                    if (!canFlex && (mainAxisSize == MainAxisAlignment.MAX || _getFit(child) == FlexFit.TIGHT)) {
//                        error = 'RenderFlex children have non-zero flex but incoming $dimension constraints are unbounded.';
//                        message = 'When a $identity is in a parent that does not provide a finite $dimension constraint, for example '
//                        'if it is in a $axis scrollable, it will try to shrink-wrap its children along the $axis '
//                        'axis. Setting a flex on a child (e.g. using Expanded) indicates that the child is to '
//                        'expand to fill the remaining space in the $axis direction.';
//                        final StringBuffer information = new StringBuffer();
//                        RenderBox node = this;
//                        switch (_direction) {
//                            case Axis.HORIZONTAL:
//                            while (!node.constraints.hasBoundedWidth && node.parent is RenderBox)
//                                node = node.parent;
//                            if (!node.constraints.hasBoundedWidth)
//                                node = null;
//                            break;
//                            case Axis.VERTICAL:
//                            while (!node.constraints.hasBoundedHeight && node.parent is RenderBox)
//                                node = node.parent;
//                            if (!node.constraints.hasBoundedHeight)
//                                node = null;
//                            break;
//                        }
//                        if (node != null) {
//                            information.writeln('The nearest ancestor providing an unbounded width constraint is:');
//                            information.write('  ');
//                            information.writeln(node.toStringShallow(joiner: '\n  '));
//                        }
//                        information.writeln('See also: https://flutter.io/layout/');
//                        addendum = information.toString();
//                    } else {
//                        return true;
//                    }
//                    throw new FlutterError(
//                            '$error\n'
//                    '$message\n'
//                    'These two directives are mutually exclusive. If a parent is to shrink-wrap its child, the child '
//                    'cannot simultaneously expand to fit its parent.\n'
//                    'Consider setting mainAxisSize to MainAxisAlignment.MIN and using FlexFit.LOOSE fits for the flexible '
//                    'children (using Flexible rather than Expanded). This will allow the flexible children '
//                    'to size themselves to less than the infinite remaining space they would otherwise be '
//                    'forced to take, and then will cause the RenderFlex to shrink-wrap the children '
//                    'rather than expanding to fit the maximum constraints provided by the parent.\n'
//                    'The affected RenderFlex is:\n'
//                    '  $this\n'
//                    'The creator information is set to:\n'
//                    '  $debugCreator\n'
//                    '$addendum'
//                    'If this message did not help you determine the problem, consider using debugDumpRenderTree():\n'
//                    '  https://flutter.io/debugging/#rendering-layer\n'
//                    '  http://docs.flutter.io/flutter/rendering/debugDumpRenderTree.html\n'
//                    'If none of the above helps enough to fix this problem, please don\'t hesitate to file a bug:\n'
//                    '  https://github.com/flutter/flutter/issues/new'
//                    );
//                }());
                totalFlex += (childParentData as FlexParentData).flex
                lastFlexChild = child
            } else {
                val innerConstraints = if (crossAxisAlignment == CrossAxisAlignment.STRETCH) {
                    when (_direction) {
                        Axis.HORIZONTAL -> {
                            BoxConstraints(
                                minHeight = constraints!!.maxHeight,
                                maxHeight = constraints!!.maxHeight
                            )
                        }
                        Axis.VERTICAL -> {
                            BoxConstraints(
                                minWidth = constraints!!.maxWidth,
                                maxWidth = constraints!!.maxWidth
                            )
                        }
                    }
                } else {
                    when (_direction) {
                        Axis.HORIZONTAL -> {
                            BoxConstraints(maxHeight = constraints!!.maxHeight)
                        }
                        Axis.VERTICAL -> {
                            BoxConstraints(maxWidth = constraints!!.maxWidth)
                        }
                    }
                }
                child.layout(innerConstraints, parentUsesSize = true)
                allocatedSize += getMainSize(child)
                crossSize = max(crossSize, getCrossSize(child))
            }
            assert(child.parentData == childParentData)
            child = (childParentData as ContainerParentDataMixin<RenderBox>).nextSibling
        }

        // Distribute free space to flexible children, and determine baseline.
        val freeSpace = max(0.0, (if (canFlex) maxMainSize else 0.0) - allocatedSize)
        var allocatedFlexSpace = 0.0
        var maxBaselineDistance = 0.0
        if (totalFlex > 0 || crossAxisAlignment == CrossAxisAlignment.BASELINE) {
            val spacePerFlex = if (canFlex && totalFlex > 0) {
                (freeSpace / totalFlex)
            } else {
                Double.NaN
            }

            child = firstChild
            while (child != null) {
                val flex = getFlex(child)
                if (flex > 0) {
                    val maxChildExtent = if (canFlex) {
                        if (child == lastFlexChild) {
                            (freeSpace - allocatedFlexSpace)
                        } else {
                            spacePerFlex * flex
                        }
                    } else {
                        Double.POSITIVE_INFINITY
                    }
                    val minChildExtent: Double
                    when (getFit(child)) {
                        FlexFit.TIGHT -> {
                            assert(maxChildExtent < Double.POSITIVE_INFINITY)
                            minChildExtent = maxChildExtent
                        }
                        FlexFit.LOOSE -> {
                            minChildExtent = 0.0
                        }
                    }

                    assert(minChildExtent != null)
                    val innerConstraints: BoxConstraints
                    if (crossAxisAlignment == CrossAxisAlignment.STRETCH) {
                        when (_direction) {
                            Axis.HORIZONTAL -> {
                                innerConstraints = BoxConstraints(
                                        minWidth = minChildExtent,
                                        maxWidth = maxChildExtent,
                                        minHeight = constraints!!.maxHeight,
                                        maxHeight = constraints!!.maxHeight
                                )
                            }
                            Axis.VERTICAL -> {
                                innerConstraints = BoxConstraints(
                                        minWidth = constraints!!.maxWidth,
                                        maxWidth = constraints!!.maxWidth,
                                        minHeight = minChildExtent,
                                        maxHeight = maxChildExtent
                                )
                            }
                        }
                    } else {
                        when (_direction) {
                            Axis.HORIZONTAL -> {
                                innerConstraints = BoxConstraints(
                                        minWidth = minChildExtent,
                                        maxWidth = maxChildExtent,
                                        maxHeight = constraints!!.maxHeight
                                )
                            }
                            Axis.VERTICAL -> {
                                innerConstraints = BoxConstraints(
                                        maxWidth = constraints!!.maxWidth,
                                        minHeight = minChildExtent,
                                        maxHeight = maxChildExtent
                                )
                            }
                        }
                    }

                    child.layout(innerConstraints, parentUsesSize = true)
                    val childSize = getMainSize(child)
                    assert(childSize <= maxChildExtent)
                    allocatedSize += childSize
                    allocatedFlexSpace += maxChildExtent
                    crossSize = max(crossSize, getCrossSize(child))
                }
                if (crossAxisAlignment == CrossAxisAlignment.BASELINE) {
                    assert({
                        if (textBaseline == null)
                            throw FlutterError("To use FlexAlignItems.baseline, you " +
                                    "must also specify which baseline to use using the" +
                                    " \"baseline\" argument.")
                        true
                    })
                    TODO("migration/Mihai baselines")
//                    val distance = child.getDistanceToBaseline(textBaseline, onlyReal = true);
//                    if (distance != null)
//                        maxBaselineDistance = max(maxBaselineDistance, distance);
                }
                val childParentData = child.parentData as ContainerParentDataMixin<RenderBox>
                child = childParentData.nextSibling
            }
        }

        // Align items along the main axis.
        val idealSize =
            if (canFlex && mainAxisSize == MainAxisSize.MAX) maxMainSize else allocatedSize
        val actualSize: Double
        val actualSizeDelta: Double
        when (_direction) {
            Axis.HORIZONTAL -> {
                size = constraints!!.constrain(Size(idealSize, crossSize))
                actualSize = size.width
                crossSize = size.height
            }
            Axis.VERTICAL -> {
                size = constraints!!.constrain(Size(crossSize, idealSize))
                actualSize = size.height
                crossSize = size.width
            }
        }
        actualSizeDelta = actualSize - allocatedSize
        overflow = max(0.0, -actualSizeDelta)

        val remainingSpace = max(0.0, actualSizeDelta)
        val leadingSpace: Double
        val betweenSpace: Double
        // flipMainAxis is used to decide whether to lay out left-to-right/top-to-bottom (false), or
        // right-to-left/bottom-to-top (true). The _startIsTopLeft will return null if there's only
        // one child and the relevant direction is null, in which case we arbitrarily decide not to
        // flip, but that doesn't have any detectable effect.
        val flipMainAxis = !(startIsTopLeft(direction, textDirection, verticalDirection) ?: true)
        when (_mainAxisAlignment) {
            MainAxisAlignment.START -> {
                leadingSpace = 0.0
                betweenSpace = 0.0
            }
            MainAxisAlignment.END -> {
            leadingSpace = remainingSpace
                betweenSpace = 0.0
            }
            MainAxisAlignment.CENTER -> {
                leadingSpace = remainingSpace / 2.0
                betweenSpace = 0.0
            }
            MainAxisAlignment.SPACE_BETWEEN -> {
                leadingSpace = 0.0
                betweenSpace = if (totalChildren > 1) remainingSpace / (totalChildren - 1) else 0.0
            }
            MainAxisAlignment.SPACE_AROUND -> {
                betweenSpace = if (totalChildren > 0) remainingSpace / totalChildren else 0.0
                leadingSpace = betweenSpace / 2.0
            }
            MainAxisAlignment.SPACE_EVENLY -> {
                betweenSpace = if (totalChildren > 0) remainingSpace / (totalChildren + 1) else 0.0
                leadingSpace = betweenSpace
            }
        }

        // Position elements
        var childMainPosition = if (flipMainAxis) actualSize - leadingSpace else leadingSpace
        child = firstChild
        while (child != null) {
            val childParentData = child.parentData
            var childCrossPosition: Double
            when (_crossAxisAlignment) {
                CrossAxisAlignment.START, CrossAxisAlignment.END -> {
                    childCrossPosition = if (
                        startIsTopLeft(direction.flip(), textDirection, verticalDirection) ==
                        (_crossAxisAlignment == CrossAxisAlignment.START)) {
                        0.0
                    } else {
                        crossSize - getCrossSize(child)
                    }
                }
                CrossAxisAlignment.CENTER -> {
                    childCrossPosition = crossSize / 2.0 - getCrossSize(child) / 2.0
                }
                CrossAxisAlignment.STRETCH -> {
                    childCrossPosition = 0.0
                }
                CrossAxisAlignment.BASELINE -> {
                    childCrossPosition = 0.0
                    if (_direction == Axis.HORIZONTAL) {
                        assert(textBaseline != null)
                        TODO("Migration/Mihai: baselines")
//                        val distance = child.getDistanceToBaseline(textBaseline, onlyReal = true);
//                        if (distance != null)
//                            childCrossPosition = maxBaselineDistance - distance;
                    }
                }
            }
            if (flipMainAxis)
                childMainPosition -= getMainSize(child)
            childParentData as? BoxParentData ?: throw IllegalStateException(
                "parent data of Flex children should be instance of BoxParentData")
            when (_direction) {
                Axis.HORIZONTAL -> {
                    childParentData.offset = Offset(childMainPosition, childCrossPosition)
                }
                Axis.VERTICAL -> {
                    childParentData.offset = Offset(childCrossPosition, childMainPosition)
                }
            }
            if (flipMainAxis) {
                childMainPosition -= betweenSpace
            } else {
                childMainPosition += getMainSize(child) + betweenSpace
            }
            child = (childParentData as ContainerParentDataMixin<RenderBox>).nextSibling
        }
    }

    // TODO(migration/Mihai): hit test related
//    @override
//    bool hitTestChildren(HitTestResult result, { Offset position }) {
//        return defaultHitTestChildren(result, position: position);
//    }

    override fun paint(context: PaintingContext, offset: Offset) {
        if (overflow <= 0.0) {
            defaultPaint(context, offset)
            return
        }

        // There's no point in drawing the children if we're empty.
        if (size.isEmpty()) {
            return
        }

        // We have overflow. Clip it.
        context.pushClipRect(needsCompositing, offset, Offset.zero and size, ::defaultPaint)

        assert({
            // Only set this if it's null to save work. It gets reset to null if the
            // _direction changes.
            val debugOverflowHints =
                    "The overflowing ${runtimeType()} has an orientation of $_direction.\n" +
            "The edge of the ${runtimeType()} that is overflowing has been marked " +
            "in the rendering with a yellow and black striped pattern. This is " +
            "usually caused by the contents being too big for the ${runtimeType()}. " +
            "Consider applying a flex factor (e.g. using an Expanded widget) to " +
            "force the children of the ${runtimeType()} to fit within the available " +
            "space instead of being sized to their natural size.\n" +
            "This is considered an error condition because it indicates that there " +
            "is content that cannot be seen. If the content is legitimately bigger " +
            "than the available space, consider clipping it with a ClipRect widget " +
            "before putting it in the flex, or using a scrollable container rather " +
            "than a Flex, like a ListView."

            // Simulate a child rect that overflows by the right amount. This child
            // rect is never used for drawing, just for determining the overflow
            // location and amount.
            val overflowChildRect: Rect
            when (_direction) {
                Axis.HORIZONTAL -> {
                    overflowChildRect =
                            Rect.fromLTWH(0.0, 0.0, size.width + overflow, 0.0)
                }
                Axis.VERTICAL -> {
                    overflowChildRect =
                            Rect.fromLTWH(0.0, 0.0, 0.0, size.height + overflow)
                }
            }
            // TODO(migration/Mihai): overflow indicator
//            paintOverflowIndicator(context, offset, Offset.zero & size, overflowChildRect,
//                                   overflowHints: debugOverflowHints);
            true
        })
    }

    override fun describeApproximatePaintClip(child: RenderObject): Rect? =
            if (overflow > 0.0) Offset.zero and size else null

    override fun toStringShort(): String {
        var header = super.toStringShort()
        if (overflow > 0.0)
            header += " OVERFLOWING"
        return header
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty("direction", direction))
        properties.add(EnumProperty("mainAxisAlignment", mainAxisAlignment))
        properties.add(EnumProperty("mainAxisSize", mainAxisSize))
        properties.add(EnumProperty("crossAxisAlignment", crossAxisAlignment))
        properties.add(EnumProperty("textDirection", textDirection, defaultValue = null))
        properties.add(EnumProperty("verticalDirection", verticalDirection, defaultValue = null))
        properties.add(EnumProperty("textBaseline", textBaseline, defaultValue = null))
    }
}