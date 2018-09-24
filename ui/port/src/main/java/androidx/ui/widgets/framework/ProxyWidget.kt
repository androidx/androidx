package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

/**
 * A widget that has a child widget provided to it, instead of building a new
 * widget.
 *
 * Useful as a base class for other widgets, such as [InheritedWidget] and
 * [ParentDataWidget].
 *
 * See also:
 *
 *  * [InheritedWidget], for widgets that introduce ambient state that can
 *    be read by descendant widgets.
 *  * [ParentDataWidget], for widgets that populate the
 *    [RenderObject.parentData] slot of their child's [RenderObject] to
 *    configure the parent widget's layout.
 *  * [StatefulWidget] and [State], for widgets that can build differently
 *    several times over their lifetime.
 *  * [StatelessWidget], for widgets that always build the same way given a
 *    particular configuration and ambient state.
 *  * [Widget], for an overview of widgets in general.
 */
abstract class ProxyWidget(
    key: Key,
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget
) : Widget(key)