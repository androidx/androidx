package androidx.ui.widgets.framework

import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError

/// An [Element] that uses a [ParentDataWidget] as its configuration.
class ParentDataElement<T : RenderObjectWidget>(override var widget: ParentDataWidget<T>) : ProxyElement(widget) {

    override fun mount(parent: Element, newSlot: Any) {
        assert{
            val badAncestors = mutableListOf<Widget>()
            var ancestor : Element? = parent;
            while (ancestor != null) {
                if (ancestor is ParentDataElement<RenderObjectWidget>) {
                    badAncestors.add(ancestor.widget);
                } else if (ancestor is RenderObjectElement) {
                    if (widget.debugIsValidAncestor(ancestor.widget))
                        break;
                    badAncestors.add(ancestor.widget);
                }
                ancestor = ancestor._parent;
            }
            if (ancestor != null && badAncestors.isEmpty())
                true;
            throw FlutterError(
                    "Incorrect use of ParentDataWidget.\n" +
                            widget.debugDescribeInvalidAncestorChain(
                                    description = "$this",
                                    ownershipChain = parent.debugGetCreatorChain(10),
                                    foundValidAncestor = ancestor != null,
                                    badAncestors = badAncestors
                             )
                );
        };
        super.mount(parent, newSlot);
    }

    private fun _applyParentData(widget: ParentDataWidget<T> ) {
        fun applyParentDataToChild(child: Element) {
            if (child is RenderObjectElement) {
                child._updateParentData(widget);
            } else {
                assert(child !is ParentDataElement<RenderObjectWidget>);
                child.visitChildren(applyParentDataToChild);
            }
        }
        visitChildren(applyParentDataToChild);
    }

    /// Calls [ParentDataWidget.applyParentData] on the given widget, passing it
    /// the [RenderObject] whose parent data this element is ultimately
    /// responsible for.
    ///
    /// This allows a render object's [RenderObject.parentData] to be modified
    /// without triggering a build. This is generally ill-advised, but makes sense
    /// in situations such as the following:
    ///
    ///  * Build and layout are currently under way, but the [ParentData] in question
    ///    does not affect layout, and the value to be applied could not be
    ///    determined before build and layout (e.g. it depends on the layout of a
    ///    descendant).
    ///
    ///  * Paint is currently under way, but the [ParentData] in question does not
    ///    affect layout or paint, and the value to be applied could not be
    ///    determined before paint (e.g. it depends on the compositing phase).
    ///
    /// In either case, the next build is expected to cause this element to be
    /// configured with the given new widget (or a widget with equivalent data).
    ///
    /// Only [ParentDataWidget]s that return true for
    /// [ParentDataWidget.debugCanApplyOutOfTurn] can be applied this way.
    ///
    /// The new widget must have the same child as the current widget.
    ///
    /// An example of when this is used is the [AutomaticKeepAlive] widget. If it
    /// receives a notification during the build of one of its descendants saying
    /// that its child must be kept alive, it will apply a [KeepAlive] widget out
    /// of turn. This is safe, because by definition the child is already alive,
    /// and therefore this will not change the behavior of the parent this frame.
    /// It is more efficient than requesting an additional frame just for the
    /// purpose of updating the [KeepAlive] widget.
    fun applyWidgetOutOfTurn(newWidget: ParentDataWidget<T>) {
        assert(newWidget != null);
        assert(newWidget.debugCanApplyOutOfTurn());
        assert(newWidget.child == widget.child);
        _applyParentData(newWidget);
    }

    override fun notifyClients(oldWidget: Widget) {
        oldWidget as ParentDataWidget<T>
        _applyParentData(widget);
    }
}