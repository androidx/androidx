package androidx.ui.widgets.framework

import androidx.ui.assert
import androidx.ui.foundation.debugPrint
import androidx.ui.widgets.debugPrintGlobalKeyedWidgetLifecycle
import androidx.ui.widgets.framework.key.GlobalKey

class _InactiveElements {

    private var _locked = false;


    val _elements = mutableSetOf<Element>();

    fun _unmount(element: Element) {
        assert(element._debugLifecycleState == _ElementLifecycle.inactive);
        assert {
            if (debugPrintGlobalKeyedWidgetLifecycle) {
                if (element.widget.key is GlobalKey<*>)
                    debugPrint("Discarding $element from inactive elements list.");
            }
            true;
        };
        element.visitChildren {
            child ->
                assert(child._parent == element);
                _unmount(child);
        };
        element.unmount();
        assert(element._debugLifecycleState == _ElementLifecycle.defunct);
    }

//    fun _unmountAll() {
//        _locked = true;
//        final List<Element> elements = _elements.toList()..sort(Element._sort);
//        _elements.clear();
//        try {
//            elements.reversed.forEach(_unmount);
//        } finally {
//            assert(_elements.isEmpty);
//            _locked = false;
//        }
//    }

    fun _deactivateRecursively(element: Element) {
        assert(element._debugLifecycleState == _ElementLifecycle.active);
        element.deactivate();
        assert(element._debugLifecycleState == _ElementLifecycle.inactive);
        element.visitChildren(::_deactivateRecursively);
        assert { element.debugDeactivated(); true; };
    }

    fun add(element: Element) {
        assert(!_locked);
        assert(!_elements.contains(element));
        assert(element._parent == null);
        if (element._active)
            _deactivateRecursively(element);
        _elements.add(element);
    }

    fun remove(element: Element) {
        assert(!_locked);
        assert(_elements.contains(element));
        assert(element._parent == null);
        _elements.remove(element);
        assert(!element._active);
    }

    fun debugContains(element: Element): Boolean {
        var result = false;
        assert {
            result = _elements.contains(element);
            true;
        };
        return result;
    }
}