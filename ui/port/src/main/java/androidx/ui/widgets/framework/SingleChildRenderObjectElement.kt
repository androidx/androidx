package androidx.ui.widgets.framework

import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.RenderObjectWithChildMixin

/**
 * An [Element] that uses a [SingleChildRenderObjectWidget] as its configuration.
 *
 * The child is optional.
 *
 * This element subclass can be used for RenderObjectWidgets whose
 * RenderObjects use the [RenderObjectWithChildMixin] mixin. Such widgets are
 * expected to inherit from [SingleChildRenderObjectWidget].
 */
class SingleChildRenderObjectElement(
    widget: SingleChildRenderObjectWidget
) : RenderObjectElement(widget) {

    var _child: Element? = null

    override fun visitChildren(visitor: ElementVisitor) {
        if (_child != null)
            visitor(_child!!)
    }

    override fun forgetChild(child: Element) {
        assert(child == _child)
        _child = null
    }

    override fun mount(parent: Element?, newSlot: Any?) {
        val singleChildWidget = widget as SingleChildRenderObjectWidget
        super.mount(parent, newSlot)
        _child = updateChild(_child, singleChildWidget.child!!, null)
    }

    override fun update(newWidget: Widget) {
        val singleChildWidget = newWidget as SingleChildRenderObjectWidget
        super.update(newWidget)
        assert(widget == newWidget)
        _child = updateChild(_child, singleChildWidget.child!!, null)
    }

    override fun insertChildRenderObject(child: RenderObject?, slot: Any?) {
        val renderObject = this.renderObject as RenderObjectWithChildMixin<RenderObject>
        assert(slot == null)
        assert(renderObject.debugValidateChild(child))
        renderObject.child = child
        assert(renderObject == this.renderObject)
    }

    override fun moveChildRenderObject(child: RenderObject?, slot: Any?) {
        assert(false)
    }

    override fun removeChildRenderObject(child: RenderObject?) {
        val renderObject = this.renderObject as RenderObjectWithChildMixin<RenderObject>
        assert(renderObject.child == child)
        renderObject.child = null
        assert(renderObject == this.renderObject)
    }
}
