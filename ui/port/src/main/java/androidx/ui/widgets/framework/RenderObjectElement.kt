package androidx.ui.widgets.framework

import androidx.ui.assert
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.rendering.obj.RenderObject

/// An [Element] that uses a [RenderObjectWidget] as its configuration.
///
/// [RenderObjectElement] objects have an associated [RenderObject] widget in
/// the render tree, which handles concrete operations like laying out,
/// painting, and hit testing.
///
/// Contrast with [ComponentElement].
///
/// For details on the lifecycle of an element, see the discussion at [Element].
///
/// ## Writing a RenderObjectElement subclass
///
/// There are three common child models used by most [RenderObject]s:
///
/// * Leaf render objects, with no children: The [LeafRenderObjectElement] class
///   handles this case.
///
/// * A single child: The [SingleChildRenderObjectElement] class handles this
///   case.
///
/// * A linked list of children: The [MultiChildRenderObjectElement] class
///   handles this case.
///
/// Sometimes, however, a render object's child model is more complicated. Maybe
/// it has a two-dimensional array of children. Maybe it constructs children on
/// demand. Maybe it features multiple lists. In such situations, the
/// corresponding [Element] for the [Widget] that configures that [RenderObject]
/// will be a new subclass of [RenderObjectElement].
///
/// Such a subclass is responsible for managing children, specifically the
/// [Element] children of this object, and the [RenderObject] children of its
/// corresponding [RenderObject].
///
/// ### Specializing the getters
///
/// [RenderObjectElement] objects spend much of their time acting as
/// intermediaries between their [widget] and their [renderObject]. To make this
/// more tractable, most [RenderObjectElement] subclasses override these getters
/// so that they return the specific type that the element expects, e.g.:
///
/// ```dart
/// class FooElement extends RenderObjectElement {
///
///   @override
///   Foo get widget => super.widget;
///
///   @override
///   RenderFoo get renderObject => super.renderObject;
///
///   // ...
/// }
/// ```
///
/// ### Slots
///
/// Each child [Element] corresponds to a [RenderObject] which should be
/// attached to this element's render object as a child.
///
/// However, the immediate children of the element may not be the ones that
/// eventually produce the actual [RenderObject] that they correspond to. For
/// example a [StatelessElement] (the element of a [StatelessWidget]) simply
/// corresponds to whatever [RenderObject] its child (the element returned by
/// its [StatelessWidget.build] method) corresponds to.
///
/// Each child is therefore assigned a _slot_ token. This is an identifier whose
/// meaning is private to this [RenderObjectElement] node. When the descendant
/// that finally produces the [RenderObject] is ready to attach it to this
/// node's render object, it passes that slot token back to this node, and that
/// allows this node to cheaply identify where to put the child render object
/// relative to the others in the parent render object.
///
/// ### Updating children
///
/// Early in the lifecycle of an element, the framework calls the [mount]
/// method. This method should call [updateChild] for each child, passing in
/// the widget for that child, and the slot for that child, thus obtaining a
/// list of child [Element]s.
///
/// Subsequently, the framework will call the [update] method. In this method,
/// the [RenderObjectElement] should call [updateChild] for each child, passing
/// in the [Element] that was obtained during [mount] or the last time [update]
/// was run (whichever happened most recently), the new [Widget], and the slot.
/// This provides the object with a new list of [Element] objects.
///
/// Where possible, the [update] method should attempt to map the elements from
/// the last pass to the widgets in the new pass. For example, if one of the
/// elements from the last pass was configured with a particular [Key], and one
/// of the widgets in this new pass has that same key, they should be paired up,
/// and the old element should be updated with the widget (and the slot
/// corresponding to the new widget's new position, also). The [updateChildren]
/// method may be useful in this regard.
///
/// [updateChild] should be called for children in their logical order. The
/// order can matter; for example, if two of the children use [PageStorage]'s
/// `writeState` feature in their build method (and neither has a [Widget.key]),
/// then the state written by the first will be overwritten by the second.
///
/// #### Dynamically determining the children during the build phase
///
/// The child widgets need not necessarily come from this element's widget
/// verbatim. They could be generated dynamically from a callback, or generated
/// in other more creative ways.
///
/// #### Dynamically determining the children during layout
///
/// If the widgets are to be generated at layout time, then generating them when
/// the [update] method won't work: layout of this element's render object
/// hasn't started yet at that point. Instead, the [update] method can mark the
/// render object as needing layout (see [RenderObject.markNeedsLayout]), and
/// then the render object's [RenderObject.performLayout] method can call back
/// to the element to have it generate the widgets and call [updateChild]
/// accordingly.
///
/// For a render object to call an element during layout, it must use
/// [RenderObject.invokeLayoutCallback]. For an element to call [updateChild]
/// outside of its [update] method, it must use [BuildOwner.buildScope].
///
/// The framework provides many more checks in normal operation than it does
/// when doing a build during layout. For this reason, creating widgets with
/// layout-time build semantics should be done with great care.
///
/// #### Handling errors when building
///
/// If an element calls a builder function to obtain widgets for its children,
/// it may find that the build throws an exception. Such exceptions should be
/// caught and reported using [FlutterError.reportError]. If a child is needed
/// but a builder has failed in this way, an instance of [ErrorWidget] can be
/// used instead.
///
/// ### Detaching children
///
/// It is possible, when using [GlobalKey]s, for a child to be proactively
/// removed by another element before this element has been updated.
/// (Specifically, this happens when the subtree rooted at a widget with a
/// particular [GlobalKey] is being moved from this element to an element
/// processed earlier in the build phase.) When this happens, this element's
/// [forgetChild] method will be called with a reference to the affected child
/// element.
///
/// The [forgetChild] method of a [RenderObjectElement] subclass must remove the
/// child element from its child list, so that when it next [update]s its
/// children, the removed child is not considered.
///
/// For performance reasons, if there are many elements, it may be quicker to
/// track which elements were forgotten by storing them in a [Set], rather than
/// proactively mutating the local record of the child list and the identities
/// of all the slots. For example, see the implementation of
/// [MultiChildRenderObjectElement].
///
/// ### Maintaining the render object tree
///
/// Once a descendant produces a render object, it will call
/// [insertChildRenderObject]. If the descendant's slot changes identity, it
/// will call [moveChildRenderObject]. If a descendant goes away, it will call
/// [removeChildRenderObject].
///
/// These three methods should update the render tree accordingly, attaching,
/// moving, and detaching the given child render object from this element's own
/// render object respectively.
///
/// ### Walking the children
///
/// If a [RenderObjectElement] object has any children [Element]s, it must
/// expose them in its implementation of the [visitChildren] method. This method
/// is used by many of the framework's internal mechanisms, and so should be
/// fast. It is also used by the test framework and [debugDumpApp].
abstract class RenderObjectElement(widget: Widget) : Element(widget) {

    //@override
    //RenderObjectWidget get widget => super.widget;

    /// The underlying [RenderObject] for this element.
    var renderObject: RenderObject? = null
        private set

    var _ancestorRenderObjectElement: RenderObjectElement? = null;

    fun _findAncestorRenderObjectElement(): RenderObjectElement? {
        var ancestor = _parent;
        while (ancestor != null && ancestor !is RenderObjectElement)
        ancestor = ancestor._parent;
        return ancestor as RenderObjectElement?;
    }

    fun _findAncestorParentDataElement(): ParentDataElement<RenderObjectWidget>?  {
        var ancestor = _parent;
        while (ancestor != null && ancestor !is RenderObjectElement) {
        if (ancestor is ParentDataElement<RenderObjectWidget>)
            return ancestor;
        ancestor = ancestor._parent;
    }
        return null;
    }

    override fun mount(parent: Element, newSlot: Any?) {
        val renderWidget = widget as RenderObjectWidget

        super.mount(parent, newSlot);
        renderObject = renderWidget.createRenderObject(this);
        assert { _debugUpdateRenderObjectOwner(); true; };
        assert(slot == newSlot);
        attachRenderObject(newSlot);
        dirty = false;
    }

    override fun update(newWidget: Widget) {
        super.update(newWidget);
        assert(widget == newWidget);
        assert{
            _debugUpdateRenderObjectOwner();
            true;
        };
        (widget as RenderObjectWidget).updateRenderObject(this, renderObject);
        dirty = false;
    }

    fun _debugUpdateRenderObjectOwner() {
        assert {
            renderObject.debugCreator = _DebugCreator(this);
            true;
        };
    }

    override fun performRebuild() {
        (widget as RenderObjectWidget).updateRenderObject(this, renderObject);
        dirty = false;
    }

    /// Updates the children of this element to use new widgets.
    ///
    /// Attempts to update the given old children list using the given new
    /// widgets, removing obsolete elements and introducing new ones as necessary,
    /// and then returns the new child list.
    ///
    /// During this function the `oldChildren` list must not be modified. If the
    /// caller wishes to remove elements from `oldChildren` re-entrantly while
    /// this function is on the stack, the caller can supply a `forgottenChildren`
    /// argument, which can be modified while this function is on the stack.
    /// Whenever this function reads from `oldChildren`, this function first
    /// checks whether the child is in `forgottenChildren`. If it is, the function
    /// acts as if the child was not in `oldChildren`.
    ///
    /// This function is a convenience wrapper around [updateChild], which updates
    /// each individual child. When calling [updateChild], this function uses the
    /// previous element as the `newSlot` argument.
    protected fun updateChildren(oldChildren: List<Element>, newWidgets: List<Widget>, forgottenChildren: Set<Element>? = null): List<Element> {
        assert(oldChildren != null);
        assert(newWidgets != null);

        fun replaceWithNullIfForgotten(child: Element): Element? {
            return if (forgottenChildren != null && forgottenChildren.contains(child)) null else child;
        }

        // This attempts to diff the new child list (newWidgets) with
        // the old child list (oldChildren), and produce a new list of elements to
        // be the new list of child elements of this element. The called of this
        // method is expected to update this render object accordingly.

        // The cases it tries to optimize for are:
        //  - the old list is empty
        //  - the lists are identical
        //  - there is an insertion or removal of one or more widgets in
        //    only one place in the list
        // If a widget with a key is in both lists, it will be synced.
        // Widgets without keys might be synced but there is no guarantee.

        // The general approach is to sync the entire new list backwards, as follows:
        // 1. Walk the lists from the top, syncing nodes, until you no longer have
        //    matching nodes.
        // 2. Walk the lists from the bottom, without syncing nodes, until you no
        //    longer have matching nodes. We'll sync these nodes at the end. We
        //    don't sync them now because we want to sync all the nodes in order
        //    from beginning to end.
        // At this point we narrowed the old and new lists to the point
        // where the nodes no longer match.
        // 3. Walk the narrowed part of the old list to get the list of
        //    keys and sync null with non-keyed items.
        // 4. Walk the narrowed part of the new list forwards:
        //     * Sync non-keyed items with null
        //     * Sync keyed items with the source if it exists, else with null.
        // 5. Walk the bottom of the list again, syncing the nodes.
        // 6. Sync null with any items in the list of keys that are still
        //    mounted.

        var newChildrenTop = 0;
        var oldChildrenTop = 0;
        var newChildrenBottom = newWidgets.size - 1;
        var oldChildrenBottom = oldChildren.size - 1;

        val newChildren = if(oldChildren.size == newWidgets.size)
            oldChildren.toMutableList() else mutableListOf()

        var previousChild: Element? = null;

        // Update the top of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            val oldChild = replaceWithNullIfForgotten(oldChildren[oldChildrenTop]);
            val newWidget = newWidgets[newChildrenTop];
            assert(oldChild == null || oldChild._debugLifecycleState == _ElementLifecycle.active);
            if (oldChild == null || !Widget.canUpdate(oldChild.widget, newWidget))
                break;
            val newChild = updateChild(oldChild, newWidget, previousChild);
            assert(newChild._debugLifecycleState == _ElementLifecycle.active);
            newChildren[newChildrenTop] = newChild;
            previousChild = newChild;
            newChildrenTop += 1;
            oldChildrenTop += 1;
        }

        // Scan the bottom of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            val oldChild = replaceWithNullIfForgotten(oldChildren[oldChildrenBottom]);
            val newWidget = newWidgets[newChildrenBottom];
            assert(oldChild == null || oldChild._debugLifecycleState == _ElementLifecycle.active);
            if (oldChild == null || !Widget.canUpdate(oldChild.widget, newWidget))
                break;
            oldChildrenBottom -= 1;
            newChildrenBottom -= 1;
        }

        // Scan the old children in the middle of the list.
        val haveOldChildren = oldChildrenTop <= oldChildrenBottom;
        var oldKeyedChildren: MutableMap<Key, Element>? = null;
        if (haveOldChildren) {
            oldKeyedChildren = mutableMapOf();
            while (oldChildrenTop <= oldChildrenBottom) {
                val oldChild = replaceWithNullIfForgotten(oldChildren[oldChildrenTop]);
                assert(oldChild == null || oldChild._debugLifecycleState == _ElementLifecycle.active);
                if (oldChild != null) {
                    if (oldChild.widget.key != null)
                        oldKeyedChildren[oldChild.widget.key] = oldChild;
                    else
                        deactivateChild(oldChild);
                }
                oldChildrenTop += 1;
            }
        }

        // Update the middle of the list.
        while (newChildrenTop <= newChildrenBottom) {
            var oldChild: Element? = null;
            val newWidget = newWidgets[newChildrenTop];
            if (haveOldChildren) {
                val key = newWidget.key;
                if (key != null) {
                    oldChild = oldKeyedChildren!![key];
                    if (oldChild != null) {
                        if (Widget.canUpdate(oldChild.widget, newWidget)) {
                            // we found a match!
                            // remove it from oldKeyedChildren so we don't unsync it later
                            oldKeyedChildren.remove(key);
                        } else {
                            // Not a match, let's pretend we didn't see it for now.
                            oldChild = null;
                        }
                    }
                }
            }
            assert(oldChild == null || Widget.canUpdate(oldChild.widget, newWidget));
            val newChild = updateChild(oldChild, newWidget, previousChild);
            assert(newChild._debugLifecycleState == _ElementLifecycle.active);
            assert(oldChild == newChild || oldChild == null || oldChild._debugLifecycleState != _ElementLifecycle.active);
            newChildren[newChildrenTop] = newChild;
            previousChild = newChild;
            newChildrenTop += 1;
        }

        // We've scanned the whole list.
        assert(oldChildrenTop == oldChildrenBottom + 1);
        assert(newChildrenTop == newChildrenBottom + 1);
        assert(newWidgets.size - newChildrenTop == oldChildren.size - oldChildrenTop);
        newChildrenBottom = newWidgets.size - 1;
        oldChildrenBottom = oldChildren.size - 1;

        // Update the bottom of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            val oldChild = oldChildren[oldChildrenTop];
            assert(replaceWithNullIfForgotten(oldChild) != null);
            assert(oldChild._debugLifecycleState == _ElementLifecycle.active);
            val newWidget = newWidgets[newChildrenTop];
            assert(Widget.canUpdate(oldChild.widget, newWidget));
            val newChild = updateChild(oldChild, newWidget, previousChild);
            assert(newChild._debugLifecycleState == _ElementLifecycle.active);
            assert(oldChild == newChild || oldChild == null || oldChild._debugLifecycleState != _ElementLifecycle.active);
            newChildren[newChildrenTop] = newChild;
            previousChild = newChild;
            newChildrenTop += 1;
            oldChildrenTop += 1;
        }

        // Clean up any of the remaining middle nodes from the old list.
        if (haveOldChildren && oldKeyedChildren!!.isNotEmpty()) {
            for (oldChild in oldKeyedChildren!!.values) {
                if (forgottenChildren == null || !forgottenChildren.contains(oldChild))
                    deactivateChild(oldChild);
            }
        }

        return newChildren;
    }

    fun deactivate() {
        super.deactivate();
        assert(!renderObject.attached,
                "A RenderObject was still attached when attempting to deactivate its " +
                "RenderObjectElement: $renderObject')";
    }

    override fun unmount() {
        super.unmount();
        assert(!renderObject.attached,
                "A RenderObject was still attached when attempting to unmount its " +
                "RenderObjectElement: $renderObject");
        (widget as RenderObjectWidget).didUnmountRenderObject(renderObject);
    }

    fun _updateParentData(parentData: ParentDataWidget<RenderObjectWidget>) {
        parentData.applyParentData(renderObject);
    }

    override fun _updateSlot(newSlot: Any?) {
        assert(slot != newSlot);
        super._updateSlot(newSlot);
        assert(slot == newSlot);
        _ancestorRenderObjectElement!!.moveChildRenderObject(renderObject, slot);
    }

    override fun attachRenderObject(newSlot: Any?) {
        assert(_ancestorRenderObjectElement == null);
        slot = newSlot;
        _ancestorRenderObjectElement = _findAncestorRenderObjectElement();
        _ancestorRenderObjectElement?.insertChildRenderObject(renderObject, newSlot);
        val parentDataElement = _findAncestorParentDataElement();
        if (parentDataElement != null)
            _updateParentData(parentDataElement.widget);
    }

    override fun detachRenderObject() {
        if (_ancestorRenderObjectElement != null) {
            _ancestorRenderObjectElement!!.removeChildRenderObject(renderObject);
            _ancestorRenderObjectElement = null;
        }
        slot = null;
    }

    /// Insert the given child into [renderObject] at the given slot.
    ///
    /// The semantics of `slot` are determined by this element. For example, if
    /// this element has a single child, the slot should always be null. If this
    /// element has a list of children, the previous sibling is a convenient value
    /// for the slot.
    protected abstract fun insertChildRenderObject(child: RenderObject?, slot: Any?);

    /// Move the given child to the given slot.
    ///
    /// The given child is guaranteed to have [renderObject] as its parent.
    ///
    /// The semantics of `slot` are determined by this element. For example, if
    /// this element has a single child, the slot should always be null. If this
    /// element has a list of children, the previous sibling is a convenient value
    /// for the slot.
    protected abstract fun moveChildRenderObject(child: RenderObject?, slot: Any?);

    /// Remove the given child from [renderObject].
    ///
    /// The given child is guaranteed to have [renderObject] as its parent.
    protected abstract fun removeChildRenderObject(child: RenderObject?);

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties);
        properties.add(DiagnosticsProperty.create("renderObject", renderObject, defaultValue = null));
    }
}