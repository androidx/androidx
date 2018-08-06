package androidx.ui.widgets.framework

import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.DiagnosticsTreeStyle
import androidx.ui.runtimeType

// / Describes the configuration for an [Element].
// /
// / Widgets are the central class hierarchy in the Flutter framework. A widget
// / is an immutable description of part of a user interface. Widgets can be
// / inflated into elements, which manage the underlying render tree.
// /
// / Widgets themselves have no mutable state (all their fields must be final).
// / If you wish to associate mutable state with a widget, consider using a
// / [StatefulWidget], which creates a [State] object (via
// / [StatefulWidget.createState]) whenever it is inflated into an element and
// / incorporated into the tree.
// /
// / A given widget can be included in the tree zero or more times. In particular
// / a given widget can be placed in the tree multiple times. Each time a widget
// / is placed in the tree, it is inflated into an [Element], which means a
// / widget that is incorporated into the tree multiple times will be inflated
// / multiple times.
// /
// / The [key] property controls how one widget replaces another widget in the
// / tree. If the [runtimeType] and [key] properties of the two widgets are
// / [operator==], respectively, then the new widget replaces the old widget by
// / updating the underlying element (i.e., by calling [Element.update] with the
// / new widget). Otherwise, the old element is removed from the tree, the new
// / widget is inflated into an element, and the new element is inserted into the
// / tree.
// /
// / See also:
// /
// /  * [StatefulWidget] and [State], for widgets that can build differently
// /    several times over their lifetime.
// /  * [InheritedWidget], for widgets that introduce ambient state that can
// /    be read by descendant widgets.
// /  * [StatelessWidget], for widgets that always build the same way given a
// /    particular configuration and ambient state.
abstract class Widget(
        // / Controls how one widget replaces another widget in the tree.
        // /
        // / If the [runtimeType] and [key] properties of the two widgets are
        // / [operator==], respectively, then the new widget replaces the old widget by
        // / updating the underlying element (i.e., by calling [Element.update] with the
        // / new widget). Otherwise, the old element is removed from the tree, the new
        // / widget is inflated into an element, and the new element is inserted into the
        // / tree.
        // /
        // / In addition, using a [GlobalKey] as the widget's [key] allows the element
        // / to be moved around the tree (changing parent) without losing state. When a
        // / new widget is found (its key and type do not match a previous widget in
        // / the same location), but there was a widget with that same global key
        // / elsewhere in the tree in the previous frame, then that widget's element is
        // / moved to the new location.
        // /
        // / Generally, a widget that is the only child of another widget does not need
        // / an explicit key.
        // /
        // / See also the discussions at [Key] and [GlobalKey].

        // / Inflates this configuration to a concrete instance.
        // /
        // / A given widget can be included in the tree zero or more times. In particular
        // / a given widget can be placed in the tree multiple times. Each time a widget
        // / is placed in the tree, it is inflated into an [Element], which means a
        // / widget that is incorporated into the tree multiple times will be inflated
        // / multiple times.
    val key: Key
) : DiagnosticableTree() {

    companion object {
        // / Whether the `newWidget` can be used to update an [Element] that currently
        // / has the `oldWidget` as its configuration.
        // /
        // / An element that uses a given widget as its configuration can be updated to
        // / use another widget as its configuration if, and only if, the two widgets
        // / have [runtimeType] and [key] properties that are [operator==].
        // /
        // / If the widgets have no key (their key is null), then they are considered a
        // / match if they have the same type, even if their children are completely
        // / different.
        fun canUpdate(oldWidget: Widget, newWidget: Widget): Boolean {
            return oldWidget.runtimeType() == newWidget.runtimeType() &&
                    oldWidget.key == newWidget.key
        }
    }

    internal abstract fun createElement(): Element

    // / A short, textual description of this widget.
    override fun toStringShort(): String {
        return if (key == null) "${runtimeType()}" else "${runtimeType()}-$key"
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.defaultDiagnosticsTreeStyle = DiagnosticsTreeStyle.dense
    }
}