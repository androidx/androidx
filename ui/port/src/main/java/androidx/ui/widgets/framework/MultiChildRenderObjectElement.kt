package androidx.ui.widgets.framework

import androidx.ui.rendering.obj.RenderObject

/// An [Element] that uses a [MultiChildRenderObjectWidget] as its configuration.
///
/// This element subclass can be used for RenderObjectWidgets whose
/// RenderObjects use the [ContainerRenderObjectMixin] mixin with a parent data
/// type that implements [ContainerParentDataMixin<RenderObject>]. Such widgets
/// are expected to inherit from [MultiChildRenderObjectWidget].
class MultiChildRenderObjectElement(override val widget: MultiChildRenderObjectWidget) : RenderObjectElement(widget) {

    init {
        assert(!debugChildrenHaveDuplicateKeys(widget, widget.children))
    }

    /// The current list of children of this element.
    ///
    /// This list is filtered to hide elements that have been forgotten (using
    /// [forgetChild]).
    protected val children get() = _children.filter { !_forgottenChildren.contains(it) }

    private var _children: List<Element> = emptyList();
    // We keep a set of forgotten children to avoid O(n^2) work walking _children
    // repeatedly to remove children.
    private val _forgottenChildren = mutableSetOf<Element>()

    override fun insertChildRenderObject(child: RenderObject, slot: Element) {
        final ContainerRenderObjectMixin<RenderObject, ContainerParentDataMixin<RenderObject>> renderObject = this.renderObject;
        assert(renderObject.debugValidateChild(child));
        renderObject.insert(child, after: slot?.renderObject);
        assert(renderObject == this.renderObject);
    }

    override fun moveChildRenderObject(child: RenderObject, slot: Any) {
        final ContainerRenderObjectMixin<RenderObject, ContainerParentDataMixin<RenderObject>> renderObject = this.renderObject;
        assert(child.parent == renderObject);
        renderObject.move(child, after: slot?.renderObject);
        assert(renderObject == this.renderObject);
    }

    override fun removeChildRenderObject(child: RenderObject) {
        final ContainerRenderObjectMixin<RenderObject, ContainerParentDataMixin<RenderObject>> renderObject = this.renderObject;
        assert(child.parent == renderObject);
        renderObject.remove(child);
        assert(renderObject == this.renderObject);
    }

    override fun visitChildren(visitor: ElementVisitor) {
        for (child in _children) {
            if (!_forgottenChildren.contains(child))
                visitor(child);
        }
    }

    override fun forgetChild(child: Element) {
        assert(_children.contains(child));
        assert(!_forgottenChildren.contains(child));
        _forgottenChildren.add(child);
    }

    override fun mount(parent: Element, newSlot: Any) {
        super.mount(parent, newSlot);
        var children = mutableListOf<Element>()
        var previousChild: Element? = null;
        for (i in 0.._children.size) {
            val newChild = inflateWidget(widget.children[i], previousChild);
            children.add(newChild)
            previousChild = newChild;
        }
        _children = children.toList()
    }

    override fun update(newWidget: MultiChildRenderObjectWidget) {
        super.update(newWidget);
        assert(widget == newWidget);
        _children = updateChildren(_children, widget.children, forgottenChildren = _forgottenChildren);
        _forgottenChildren.clear();
    }
}