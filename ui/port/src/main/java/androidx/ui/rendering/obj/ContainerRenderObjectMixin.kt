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

package androidx.ui.rendering.obj

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextBaseline
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.flex.ContainerBoxParentData

// / Generic mixin for render objects with a list of children.
// /
// / Provides a child model for a render object subclass that has a doubly-linked
// / list of children.
// TODO(migration/Mihai): complete this class, has many commented out parts
// TODO(migration/Mihai): inheriting from RenderBox to workaround mixins for flex, but this a hack
abstract class ContainerRenderObjectMixin<
        ChildType : RenderObject,
        ParentDataType : ContainerParentDataMixin<ChildType>
        > : RenderBox() {
    // This class is intended to be used as a mixin, and should not be
    // extended directly.
//    factory ContainerRenderObjectMixin._() => null;

//    bool _debugUltimatePreviousSiblingOf(ChildType child, { ChildType equals }) {
//        ParentDataType childParentData = child.parentData;
//        while (childParentData.previousSibling != null) {
//            assert(childParentData.previousSibling != child);
//            child = childParentData.previousSibling;
//            childParentData = child.parentData;
//        }
//        return child == equals;
//    }
//    bool _debugUltimateNextSiblingOf(ChildType child, { ChildType equals }) {
//        ParentDataType childParentData = child.parentData;
//        while (childParentData.nextSibling != null) {
//            assert(childParentData.nextSibling != child);
//            child = childParentData.nextSibling;
//            childParentData = child.parentData;
//        }
//        return child == equals;
//    }

    // / The number of children.
    var childCount = 0
        private set

    // / Checks whether the given render object has the correct [runtimeType] to be
    // / a child of this render object.
    // /
    // / Does nothing if assertions are disabled.
    // /
    // / Always returns true.
//    bool debugValidateChild(RenderObject child) {
//        assert(() {
//            if (child is! ChildType) {
//            throw new FlutterError(
//                    'A $runtimeType expected a child of type $ChildType but received a '
//            'child of type ${child.runtimeType}.\n'
//            'RenderObjects expect specific types of children because they '
//            'coordinate with their children during layout and paint. For '
//            'example, a RenderSliver cannot be the child of a RenderBox because '
//            'a RenderSliver does not understand the RenderBox layout protocol.\n'
//            '\n'
//            'The $runtimeType that expected a $ChildType child was created by:\n'
//            '  $debugCreator\n'
//            '\n'
//            'The ${child.runtimeType} that did not match the expected child type '
//            'was created by:\n'
//            '  ${child.debugCreator}\n'
//            );
//        }
//            return true;
//        }());
//        return true;
//    }

    // / The first child in the child list.
    var firstChild: ChildType? = null
        private set
    // / The last child in the child list.
    var lastChild: ChildType? = null
        private set

    private fun insertIntoChildList(child: ChildType, after: ChildType? = null) {
        val childParentData = child.parentData as ParentDataType
        assert(childParentData.nextSibling == null)
        assert(childParentData.previousSibling == null)
        childCount += 1
        assert(childCount > 0)
        if (after == null) {
            // insert at the start (firstChild)
            childParentData.nextSibling = firstChild
            if (firstChild != null) {
                val firstChildParentData = firstChild!!.parentData as ParentDataType
                firstChildParentData.previousSibling = child
            }
            firstChild = child
            if (lastChild == null) {
                lastChild = child
            }
        } else {
            assert(firstChild != null)
            assert(lastChild != null)
//            assert(_debugUltimatePreviousSiblingOf(after, equals: firstChild));
//            assert(_debugUltimateNextSiblingOf(after, equals: lastChild));
            val afterParentData = after.parentData as ParentDataType
            if (afterParentData.nextSibling == null) {
                // insert at the end (lastChild); we'll end up with two or more children
                assert(after == lastChild)
                childParentData.previousSibling = after
                afterParentData.nextSibling = child
                lastChild = child
            } else {
                // insert in the middle; we'll end up with three or more children
                // set up links from child to siblings
                childParentData.nextSibling = afterParentData.nextSibling
                childParentData.previousSibling = after
                // set up links from siblings to child
                val childPreviousSiblingParentData =
                    childParentData.previousSibling!!.parentData as ParentDataType
                val childNextSiblingParentData =
                    childParentData.nextSibling!!.parentData as ParentDataType
                childPreviousSiblingParentData.nextSibling = child
                childNextSiblingParentData.previousSibling = child
                assert(afterParentData.nextSibling == child)
            }
        }
    }
    // / Insert child into this render object's child list after the given child.
    // /
    // / If `after` is null, then this inserts the child at the start of the list,
    // / and the child becomes the new [firstChild].
    fun insert(child: ChildType, after: ChildType? = null) {
        assert(child != this, { "A RenderObject cannot be inserted into itself." })
        assert(after != this, { "A RenderObject cannot simultaneously be both the parent" +
                "and the sibling of another RenderObject." })
        assert(child != after, { "A RenderObject cannot be inserted after itself." })
        assert(child != firstChild)
        assert(child != lastChild)
        adoptChild(child)
        insertIntoChildList(child, after = after)
    }

    // / Append child to the end of this render object's child list.
    fun add(child: ChildType) {
        insert(child, after = lastChild)
    }

    // / Add all the children to the end of this render object's child list.
    fun addAll(children: List<ChildType>?) {
        children?.forEach {
            add(it)
        }
    }

//    void _removeFromChildList(ChildType child) {
//        final ParentDataType childParentData = child.parentData;
//        assert(_debugUltimatePreviousSiblingOf(child, equals: firstChild));
//        assert(_debugUltimateNextSiblingOf(child, equals: lastChild));
//        assert(_childCount >= 0);
//        if (childParentData.previousSibling == null) {
//            assert(firstChild == child);
//            firstChild = childParentData.nextSibling;
//        } else {
//            final ParentDataType childPreviousSiblingParentData = childParentData.previousSibling.parentData;
//            childPreviousSiblingParentData.nextSibling = childParentData.nextSibling;
//        }
//        if (childParentData.nextSibling == null) {
//            assert(lastChild == child);
//            lastChild = childParentData.previousSibling;
//        } else {
//            final ParentDataType childNextSiblingParentData = childParentData.nextSibling.parentData;
//            childNextSiblingParentData.previousSibling = childParentData.previousSibling;
//        }
//        childParentData.previousSibling = null;
//        childParentData.nextSibling = null;
//        _childCount -= 1;
//    }

    // / Remove this child from the child list.
    // /
    // / Requires the child to be present in the child list.
//    void remove(ChildType child) {
//        _removeFromChildList(child);
//        dropChild(child);
//    }

    // / Remove all their children from this render object's child list.
    // /
    // / More efficient than removing them individually.
//    void removeAll() {
//        ChildType child = firstChild;
//        while (child != null) {
//            final ParentDataType childParentData = child.parentData;
//            final ChildType next = childParentData.nextSibling;
//            childParentData.previousSibling = null;
//            childParentData.nextSibling = null;
//            dropChild(child);
//            child = next;
//        }
//        firstChild = null;
//        lastChild = null;
//        _childCount = 0;
//    }

    // / Move this child in the child list to be before the given child.
    // /
    // / More efficient than removing and re-adding the child. Requires the child
    // / to already be in the child list at some position. Pass null for before to
    // / move the child to the end of the child list.
//    void move(ChildType child, { ChildType after }) {
//        assert(child != this);
//        assert(after != this);
//        assert(child != after);
//        assert(child.parent == this);
//        final ParentDataType childParentData = child.parentData;
//        if (childParentData.previousSibling == after)
//            return;
//        _removeFromChildList(child);
//        _insertIntoChildList(child, after: after);
//        markNeedsLayout();
//    }

    override fun attach(owner: Any /* PipelineOwner */) {
        super.attach(owner)
        var child = firstChild
        while (child != null) {
            child.attach(owner)
            val childParentData = child.parentData as ParentDataType
            child = childParentData.nextSibling
        }
    }

    override fun detach() {
        super.detach()
        var child = firstChild
        while (child != null) {
            child.detach()
            val childParentData = child.parentData as ParentDataType
            child = childParentData.nextSibling
        }
    }

    override fun redepthChildren() {
        var child = firstChild
        while (child != null) {
            redepthChild(child)
            val childParentData = child.parentData as ParentDataType
            child = childParentData.nextSibling
        }
    }

    override fun visitChildren(visitor: RenderObjectVisitor) {
        var child = firstChild
        while (child != null) {
            visitor(child)
            val childParentData = child.parentData as ParentDataType
            child = childParentData.nextSibling
        }
    }

    // / The previous child before the given child in the child list.
    fun childBefore(child: ChildType): ChildType? {
        assert(child != null)
        assert(child.parent == this)
        val childParentData = child.parentData as ParentDataType
        return childParentData.previousSibling
    }

    // / The next child after the given child in the child list.
    fun childAfter(child: ChildType): ChildType? {
        assert(child != null)
        assert(child.parent == this)
        val childParentData = child.parentData as ParentDataType
        return childParentData.nextSibling
    }

//    @override
//    List<DiagnosticsNode> debugDescribeChildren() {
//        final List<DiagnosticsNode> children = <DiagnosticsNode>[];
//        if (firstChild != null) {
//            ChildType child = firstChild;
//            int count = 1;
//            while (true) {
//                children.add(child.toDiagnosticsNode(name: 'child $count'));
//                if (child == lastChild)
//                    break;
//                count += 1;
//                final ParentDataType childParentData = child.parentData;
//                child = childParentData.nextSibling;
//            }
//        }
//        return children;
//    }

    // TODO(Migration/Mihai): these are the contents of RenderBoxContainerDefaultsMixin
    // Putting them here as a work around for mixins..

    // / Returns the baseline of the first child with a baseline.
    // /
    // / Useful when the children are displayed vertically in the same order they
    // / appear in the child list.
    fun defaultComputeDistanceToFirstActualBaseline(baseline: TextBaseline): Double {
        TODO("Migration/Mihai: baselines")
//        assert(!debugNeedsLayout);
//        ChildType child = firstChild;
//        while (child != null) {
//            final ParentDataType childParentData = child.parentData;
//            final double result = child.getDistanceToActualBaseline(baseline);
//            if (result != null)
//                return result + childParentData.offset.dy;
//            child = childParentData.nextSibling;
//        }
//        return null;
    }

    // / Returns the minimum baseline value among every child.
    // /
    // / Useful when the vertical position of the children isn't determined by the
    // / order in the child list.
    fun defaultComputeDistanceToHighestActualBaseline(baseline: TextBaseline): Double {
        TODO("Migration/Mihai: baselines")
//        assert(!debugNeedsLayout);
//        double result;
//        ChildType child = firstChild;
//        while (child != null) {
//            final ParentDataType childParentData = child.parentData;
//            double candidate = child.getDistanceToActualBaseline(baseline);
//            if (candidate != null) {
//                candidate += childParentData.offset.dy;
//                if (result != null)
//                    result = math.min(result, candidate);
//                else
//                    result = candidate;
//            }
//            child = childParentData.nextSibling;
//        }
//        return result;
    }

    // / Performs a hit test on each child by walking the child list backwards.
    // /
    // / Stops walking once after the first child reports that it contains the
    // / given point. Returns whether any children contain the given point.
    // /
    // / See also:
    // /
    // /  * [defaultPaint], which paints the children appropriate for this
    // /    hit-testing strategy.
    fun defaultHitTestChildren(result: HitTestResult, position: Offset? = null) {
        TODO("Migration/Mihai: flex hit test")
//        // the x, y parameters have the top left of the node's box as the origin
//        ChildType child = lastChild;
//        while (child != null) {
//            final ParentDataType childParentData = child.parentData;
//            if (child.hitTest(result, position: position - childParentData.offset))
//            return true;
//            child = childParentData.previousSibling;
//        }
//        return false;
    }

    // / Paints each child by walking the child list forwards.
    // /
    // / See also:
    // /
    // /  * [defaultHitTestChildren], which implements hit-testing of the children
    // /    in a manner appropriate for this painting strategy.
    fun defaultPaint(context: PaintingContext, offset: Offset) {
        var child: RenderBox? = firstChild as RenderBox
        while (child != null) {
            val childParentData = child.parentData as ContainerBoxParentData<RenderBox>
            context.paintChild(child, childParentData.offset + offset)
            child = childParentData.nextSibling
        }
    }

    // / Returns a list containing the children of this render object.
    // /
    // / This function is useful when you need random-access to the children of
    // / this render object. If you're accessing the children in order, consider
    // / walking the child list directly.
    fun getChildrenAsList(): List<ChildType> {
        val result = mutableListOf<ChildType>()
        var child: RenderBox? = firstChild as RenderBox
        while (child != null) {
            val childParentData = child.parentData as ContainerBoxParentData<RenderBox>
            result.add(child as ChildType)
            child = childParentData.nextSibling
        }
        return result
    }
}