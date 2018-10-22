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

package androidx.ui.widgets.framework

import androidx.ui.rendering.obj.ContainerParentDataMixin
import androidx.ui.rendering.obj.ContainerRenderObjectMixin
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.debugChildrenHaveDuplicateKeys

/**
 * An [Element] that uses a [MultiChildRenderObjectWidget] as its configuration.
 *
 * This element subclass can be used for RenderObjectWidgets whose
 * RenderObjects use the [ContainerRenderObjectMixin] mixin with a parent data
 * type that implements [ContainerParentDataMixin<RenderObject>]. Such widgets
 * are expected to inherit from [MultiChildRenderObjectWidget].
 */
class MultiChildRenderObjectElement(widget: MultiChildRenderObjectWidget)
    : RenderObjectElement(widget) {

    init {
        assert(!debugChildrenHaveDuplicateKeys(widget, widget.children))
    }

    /**
     * The current list of children of this element.
     *
     * This list is filtered to hide elements that have been forgotten (using
     * [forgetChild]).
     */
    protected val children get() = _children.filter { !_forgottenChildren.contains(it) }

    private var _children: List<Element> = emptyList()
    // We keep a set of forgotten children to avoid O(n^2) work walking _children
    // repeatedly to remove children.
    private val _forgottenChildren = mutableSetOf<Element>()

    override fun insertChildRenderObject(child: RenderObject, slot: Any?) {
        val elementSlot = slot as Element?

        val renderObject = this.renderObject
                as ContainerRenderObjectMixin<RenderObject, ContainerParentDataMixin<RenderObject>>
        assert(renderObject.debugValidateChild(child))
        renderObject.insert(child, after = elementSlot?.retrieveRenderObject())
        assert(renderObject == this.renderObject)
    }

    override fun moveChildRenderObject(child: RenderObject, slot: Any?) {
        TODO("Remove once ContainerRenderObjectMixin is migrated")
//        val renderObject = this.renderObject
//            as ContainerRenderObjectMixin<RenderObject, ContainerParentDataMixin<RenderObject>>;
//        assert(child.parent == renderObject);
//        renderObject.move(child, after = slot?.renderObject);
//        assert(renderObject == this.renderObject);
    }

    override fun removeChildRenderObject(child: RenderObject) {
        TODO("Remove once ContainerRenderObjectMixin is migrated")
//        val renderObject = this.renderObject
//            as ContainerRenderObjectMixin<RenderObject, ContainerParentDataMixin<RenderObject>>
//        assert(child.parent == renderObject);
//        renderObject.remove(child);
//        assert(renderObject == this.renderObject);
    }

    override fun visitChildren(visitor: ElementVisitor) {
        for (child in _children) {
            if (!_forgottenChildren.contains(child))
                visitor(child)
        }
    }

    override fun forgetChild(child: Element) {
        assert(_children.contains(child))
        assert(!_forgottenChildren.contains(child))
        _forgottenChildren.add(child)
    }

    override fun mount(parent: Element?, newSlot: Any?) {
        val multiChildWidget = widget as MultiChildRenderObjectWidget

        super.mount(parent, newSlot)
        var children = mutableListOf<Element>()
        var previousChild: Element? = null
        for (i in 0 until multiChildWidget.children.size) {
            val newChild = inflateWidget(multiChildWidget.children[i], previousChild)
            children.add(newChild)
            previousChild = newChild
        }
        _children = children.toList()
    }

    override fun update(newWidget: Widget) {
        val newMultiChildWidget = newWidget as MultiChildRenderObjectWidget

        super.update(newWidget)
        assert(widget == newWidget)
        _children = updateChildren(
                _children,
                newMultiChildWidget.children,
                forgottenChildren = _forgottenChildren
        )
        _forgottenChildren.clear()
    }
}