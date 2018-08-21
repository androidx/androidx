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

package androidx.ui.widgets.binding

import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.RenderObjectWithChildMixin
import androidx.ui.widgets.framework.Element
import androidx.ui.widgets.framework.ElementVisitor
import androidx.ui.widgets.framework.ErrorWidget
import androidx.ui.widgets.framework.RootRenderObjectElement
import androidx.ui.widgets.framework.Widget

// / A [RootRenderObjectElement] that is hosted by a [RenderObject].
// /
// / This element class is the instantiation of a [RenderObjectToWidgetAdapter]
// / widget. It can be used only as the root of an [Element] tree (it cannot be
// / mounted into another [Element]; it's parent must be null).
// /
// / In typical usage, it will be instantiated for a [RenderObjectToWidgetAdapter]
// / whose container is the [RenderView] that connects to the Flutter engine. In
// / this usage, it is normally instantiated by the bootstrapping logic in the
// / [WidgetsFlutterBinding] singleton created by [runApp].
// /
// / Ctor comment:
// / Creates an element that is hosted by a [RenderObject].
// /
// / The [RenderObject] created by this element is not automatically set as a
// / child of the hosting [RenderObject]. To actually attach this element to
// / the render tree, call [RenderObjectToWidgetAdapter.attachToRenderTree].
class RenderObjectToWidgetElement<T : RenderObject>(
    widget: RenderObjectToWidgetAdapter<T>
) : RootRenderObjectElement(widget) {

//    @override
//    RenderObjectToWidgetAdapter<T> get widget => super.widget;

    var _child: Element? = null

    companion object {
        private val _rootChildSlot: Any = Any()
    }

    override fun visitChildren(visitor: ElementVisitor) {
        if (_child != null)
            visitor(_child!!)
    }

    override fun forgetChild(child: Element) {
        assert(child == _child)
        _child = null
    }

    override fun mount(parent: Element?, newSlot: Any?) {
        assert(parent == null)
        super.mount(parent, newSlot)
        _rebuild()
    }

    override fun update(newWidget: Widget) {
        newWidget as RenderObjectToWidgetAdapter<T>

        super.update(newWidget)
        assert(widget == newWidget)
        _rebuild()
    }

    // When we are assigned a new widget, we store it here
    // until we are ready to update to it.
    internal var _newWidget: Widget? = null

    override fun performRebuild() {
        if (_newWidget != null) {
            // _newWidget can be null if, for instance, we were rebuilt
            // due to a reassemble.
            val newWidget = _newWidget
            _newWidget = null
            update(newWidget!!)
        }
        super.performRebuild()
        assert(_newWidget == null)
    }

    private fun _rebuild() {
        try {
            val widgetAdapter = widget as RenderObjectToWidgetAdapter<T>

            _child = updateChild(_child, widgetAdapter.child, _rootChildSlot)
            assert(_child != null)
        } catch (e: Throwable) {
            val details = FlutterErrorDetails(
                    exception = e,
                    stack = e.stackTrace,
                    library = "widgets library",
                    context = "attaching to the render tree"
            )
            FlutterError.reportError(details)
            val error = ErrorWidget.builder(details)
            _child = updateChild(null, error, _rootChildSlot)
        }
    }

//    @override
//    RenderObjectWithChildMixin<T> get renderObject => super.renderObject;

    override fun insertChildRenderObject(child: RenderObject?, slot: Any?) {
        val objectWithMixin = renderObject as RenderObjectWithChildMixin<T>

        assert(slot == _rootChildSlot)
        assert(objectWithMixin.debugValidateChild(child))
        objectWithMixin.child = child as T?
    }

    override fun moveChildRenderObject(child: RenderObject?, slot: Any?) {
        assert(false)
    }

    override fun removeChildRenderObject(child: RenderObject?) {
        val objectWithMixin = renderObject as RenderObjectWithChildMixin<*>

        assert(objectWithMixin.child == child)
        objectWithMixin.child = null
    }
}