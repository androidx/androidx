package androidx.ui.widgets.framework

import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.rendering.obj.RenderObject

/// An [Element] that uses a [LeafRenderObjectWidget] as its configuration.
class LeafRenderObjectElement(widget: LeafRenderObjectWidget) : RenderObjectElement(widget) {

    override fun forgetChild(child: Element) {
        assert(false);
    }

    override fun insertChildRenderObject(child: RenderObject, slot: Any) {
        assert(false);
    }

    override fun moveChildRenderObject(child: RenderObject, slot: Any) {
        assert(false);
    }

    override fun removeChildRenderObject(child: RenderObject) {
        assert(false);
    }

    override fun  debugDescribeChildren(): List<DiagnosticsNode> {
        return widget.debugDescribeChildren();
    }
}