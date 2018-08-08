package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

// / Abstract const constructor. This constructor enables subclasses to provide
// / const constructors so that they can be used in const expressions.
abstract class SingleChildRenderObjectWidget(
    key: Key,
        // / The widget below this widget in the tree.
        // /
        // / {@macro flutter.widgets.child}
    child: Widget
) : RenderObjectWidget(key) {

    override fun createElement(): SingleChildRenderObjectElement =
            SingleChildRenderObjectElement(this)
}