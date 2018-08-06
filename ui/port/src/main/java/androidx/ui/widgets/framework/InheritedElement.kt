package androidx.ui.widgets.framework

import androidx.ui.assert

// / An [Element] that uses a [InheritedWidget] as its configuration.
class InheritedElement(widget: InheritedWidget) : ProxyElement(widget) {

    val _dependents: MutableSet<Element> = mutableSetOf()

//    override fun _updateInheritance() {
//        assert(_active);
//        final Map<Type, InheritedElement> incomingWidgets = _parent?._inheritedWidgets;
//        if (incomingWidgets != null)
//            _inheritedWidgets = new HashMap<Type, InheritedElement>.from(incomingWidgets);
//        else
//        _inheritedWidgets = new HashMap<Type, InheritedElement>();
//        _inheritedWidgets[widget.runtimeType] = this;
//    }

    override fun debugDeactivated() {
        assert {
            assert(_dependents.isEmpty())
            return@assert true
        }
        super.debugDeactivated()
    }

    // / Calls [Element.didChangeDependencies] of all dependent elements, if
    // / [InheritedWidget.updateShouldNotify] returns true.
    // /
    // / Notifies all dependent elements that this inherited widget has changed.
    // /
    // / [InheritedElement] calls this function if the [widget]'s
    // / [InheritedWidget.updateShouldNotify] returns true.
    // /
    // / This method must be called during the build phase. Usually this method is
    // / called automatically when an inherited widget is rebuilt, e.g. as a
    // / result of calling [State.setState] above the inherited widget.
    override fun notifyClients(oldWidget: Widget) {
        val inheritedWidget = oldWidget as InheritedWidget
        if (inheritedWidget.updateShouldNotify(oldWidget))
            return
        assert(_debugCheckOwnerBuildTargetExists("notifyClients"))
        for (dependent in _dependents) {
            assert {
                // check that it really is our descendant
                var ancestor = dependent._parent
                while (ancestor != this && ancestor != null)
                    ancestor = ancestor._parent
                return@assert ancestor == this
            }
            // check that it really depends on us
            assert(dependent._dependencies!!.contains(this))
            dependent.didChangeDependencies()
        }
    }
}