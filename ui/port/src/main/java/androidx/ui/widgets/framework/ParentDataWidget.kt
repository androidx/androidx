package androidx.ui.widgets.framework

import androidx.ui.foundation.Key
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.runtimeType

/**
 * Base class for widgets that hook [ParentData] information to children of
 * [RenderObjectWidget]s.
 *
 * This can be used to provide per-child configuration for
 * [RenderObjectWidget]s with more than one child. For example, [Stack] uses
 * the [Positioned] parent data widget to position each child.
 *
 * A [ParentDataWidget] is specific to a particular kind of [RenderObject], and
 * thus also to a particular [RenderObjectWidget] class. That class is `T`, the
 * [ParentDataWidget] type argument.
 *
 * ## Sample code
 *
 * This example shows how you would build a [ParentDataWidget] to configure a
 * `FrogJar` widget's children by specifying a [Size] for each one.
 *
 * ```dart
 * class FrogSize extends ParentDataWidget<FrogJar> {
 *   FrogSize({
 *     Key key,
 *     @required this.size,
 *     @required Widget child,
 *   }) : assert(child != null),
 *        assert(size != null),
 *        super(key: key, child: child);
 *
 *   final Size size;
 *
 *   @override
 *   void applyParentData(RenderObject renderObject) {
 *     final FrogJarParentData parentData = renderObject.parentData;
 *     if (parentData.size != size) {
 *       parentData.size = size;
 *       final RenderFrogJar targetParent = renderObject.parent;
 *       targetParent.markNeedsLayout();
 *     }
 *   }
 * }
 * ```
 *
 * See also:
 *
 *  * [RenderObject], the superclass for layout algorithms.
 *  * [RenderObject.parentData], the slot that this class configures.
 *  * [ParentData], the superclass of the data that will be placed in
 *    [RenderObject.parentData] slots.
 *  * [RenderObjectWidget], the class for widgets that wrap [RenderObject]s.
 *    The `T` type parameter for [ParentDataWidget] is a [RenderObjectWidget].
 *  * [StatefulWidget] and [State], for widgets that can build differently
 *    several times over their lifetime.
 */
abstract class ParentDataWidget<T : RenderObjectWidget>(
    key: Key,
    child: Widget
) : ProxyWidget(key, child) {

    override fun createElement(): ParentDataElement<T> = ParentDataElement<T>(this)

    /**
     * Subclasses should override this method to return true if the given
     * ancestor is a RenderObjectWidget that wraps a RenderObject that can handle
     * the kind of ParentData widget that the ParentDataWidget subclass handles.
     *
     * The default implementation uses the type argument.
     */
    fun debugIsValidAncestor(ancestor: RenderObjectWidget): Boolean {
        // TODO(Migration/Filip): Not possible
        // assert(T != dynamic);
        // assert(T != RenderObjectWidget);
        // return ancestor is T;
        return true
    }

    /**
     * Subclasses should override this to describe the requirements for using the
     * ParentDataWidget subclass. It is called when debugIsValidAncestor()
     * returned false for an ancestor, or when there are extraneous
     * [ParentDataWidget]s in the ancestor chain.
     */
    fun debugDescribeInvalidAncestorChain(
        description: String,
        ownershipChain: String,
        foundValidAncestor: Boolean,
        badAncestors: Iterable<Widget>
    ): String {
        // TODO(Migration/Filip): Not possible + had to remove $T in strings
        // assert(T != dynamic);
        // assert(T != RenderObjectWidget);
        var result: String? = null
        if (!foundValidAncestor) {
            result = "${runtimeType()} widgets must be placed inside T widgets.\n" +
                    "$description has no T ancestor at all.\n"
        } else {
            assert(!badAncestors.none())
            result = "${runtimeType()} widgets must be placed directly inside T widgets.\n" +
                    "$description has a T ancestor, but there are other widgets between them:\n"
            for (ancestor in badAncestors) {
                if (ancestor.runtimeType() == runtimeType()) {
                    result += "- $ancestor (this is a different ${runtimeType()} than the one " +
                            "with the problem)\n"
                } else {
                    result += "- $ancestor\n"
                }
            }
            result += "These widgets cannot come between a ${runtimeType()} and its T.\n"
        }
        result += "The ownership chain for the parent of the offending ${runtimeType()} was:\n" +
                "  $ownershipChain"
        return result
    }

    /**
     * Write the data from this widget into the given render object's parent data.
     *
     * The framework calls this function whenever it detects that the
     * [RenderObject] associated with the [child] has outdated
     * [RenderObject.parentData]. For example, if the render object was recently
     * inserted into the render tree, the render object's parent data might not
     * match the data in this widget.
     *
     * Subclasses are expected to override this function to copy data from their
     * fields into the [RenderObject.parentData] field of the given render
     * object. The render object's parent is guaranteed to have been created by a
     * widget of type `T`, which usually means that this function can assume that
     * the render object's parent data object inherits from a particular class.
     *
     * If this function modifies data that can change the parent's layout or
     * painting, this function is responsible for calling
     * [RenderObject.markNeedsLayout] or [RenderObject.markNeedsPaint] on the
     * parent, as appropriate.
     */
    internal abstract fun applyParentData(renderObject: RenderObject?)

    /**
     * Whether the [ParentDataElement.applyWidgetOutOfTurn] method is allowed
     * with this widget.
     *
     * This should only return true if this widget represents a [ParentData]
     * configuration that will have no impact on the layout or paint phase.
     *
     * See also:
     *
     *  * [ParentDataElement.applyWidgetOutOfTurn], which verifies this in debug
     *    mode.
     */
    internal fun debugCanApplyOutOfTurn(): Boolean = false
}