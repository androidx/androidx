package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

// / A superclass for RenderObjectWidgets that configure RenderObject subclasses
// / that have no children.
abstract class LeafRenderObjectWidget(key: Key) : RenderObjectWidget(key) {

    override fun createElement() = LeafRenderObjectElement(this)
}