package androidx.ui.widgets.framework

import androidx.ui.Type
import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.runtimeType

/** An [Element] that uses a [StatefulWidget] as its configuration. */
class StatefulElement(widget: StatefulWidget) : ComponentElement(widget) {

    /**
     * The [State] instance associated with this location in the tree.
     *
     * There is a one-to-one relationship between [State] objects and the
     * [StatefulElement] objects that hold them. The [State] objects are created
     * by [StatefulElement] in [mount].
     */
    var state: State<StatefulWidget>? = widget.createState()
        private set

    init {
        assert {
            if (!state!!._debugTypesAreRight(widget)) {
                throw FlutterError(
                        "StatefulWidget.createState must return a subtype of " +
                        "State<${widget.runtimeType()}>\n" +
                        "The createState function for ${widget.runtimeType()} returned a state " +
                        "of type ${state!!.runtimeType()}, which is not a subtype of " +
                        "State<${widget.runtimeType()}>, violating the contract for createState."
                )
            }
            true
        }

        val s = state!!
        assert(s._element == null)
        s._element = this
        assert(s.widget == null)
        s.widget = widget
        assert(s._debugLifecycleState == _StateLifecycle.created)
    }
//
    override fun build(): Widget = state!!.build(this)

    override fun _reassemble() {
        state!!.reassemble()
        super._reassemble()
    }

    override fun _firstBuild() {
        assert(state!!._debugLifecycleState == _StateLifecycle.created)
        try {
            _debugSetAllowIgnoredCallsToMarkNeedsBuild(true)
            val debugCheckForReturnedFuture = state!!.initState() as Any
// TODO(Migration/Filip): Fix Future
//            assert(() {
//                if (debugCheckForReturnedFuture is Future) {
//                    throw new FlutterError(
//                            '${_state.runtimeType}.initState() returned a Future.\n'
//                    'State.initState() must be a void method without an `async` keyword.\n'
//                    'Rather than awaiting on asynchronous work directly inside of initState,\n'
//                    'call a separate method to do this work without awaiting it.'
//                    );
//                }
//                return true;
//            }());
        } finally {
            _debugSetAllowIgnoredCallsToMarkNeedsBuild(false)
        }
        assert { state!!._debugLifecycleState = _StateLifecycle.initialized; true; }
        state!!.didChangeDependencies()
        assert { state!!._debugLifecycleState = _StateLifecycle.ready; true; }
        super._firstBuild()
    }

    override fun update(newWidget: Widget) {
        newWidget as StatefulWidget

        super.update(newWidget)
        assert(widget == newWidget)
        val oldWidget = state!!.widget
        // Notice that we mark ourselves as dirty before calling didUpdateWidget to
        // let authors call setState from within didUpdateWidget without triggering
        // asserts.
        dirty = true
        state!!.widget = widget as StatefulWidget
        try {
            _debugSetAllowIgnoredCallsToMarkNeedsBuild(true)
            val debugCheckForReturnedFuture = state!!.didUpdateWidget(oldWidget) as Any
// TODO(Migration/Filip): Fix Future
//            assert(() {
//                if (debugCheckForReturnedFuture is Future) {
//                    throw new FlutterError(
//                            '${_state.runtimeType}.didUpdateWidget() returned a Future.\n'
//                    'State.didUpdateWidget() must be a void method without an `async` keyword.\n'
//                    'Rather than awaiting on asynchronous work directly inside of didUpdateWidget,\n'
//                    'call a separate method to do this work without awaiting it.'
//                    );
//                }
//                return true;
//            }());
        } finally {
            _debugSetAllowIgnoredCallsToMarkNeedsBuild(false)
        }
        rebuild()
    }

    override fun activate() {
        super.activate()
        // Since the State could have observed the deactivate() and thus disposed of
        // resources allocated in the build method, we have to rebuild the widget
        // so that its State can reallocate its resources.
        assert(_active); // otherwise markNeedsBuild is a no-op
        markNeedsBuild()
    }

    override fun deactivate() {
        state!!.deactivate()
        super.deactivate()
    }

    override fun unmount() {
        super.unmount()
        state!!.dispose()
        assert {
            if (state!!._debugLifecycleState == _StateLifecycle.defunct)
                true
            throw FlutterError(
                    "${state!!.runtimeType()}.dispose failed to call super.dispose.\n" +
                    "dispose() implementations must always call their superclass dispose()" +
                    " method, to ensure that all the resources used by the widget are fully" +
                    " released."
            )
        }
        state!!._element = null
        state = null
    }

    override fun inheritFromWidgetOfExactType(targetType: Type): InheritedWidget? {
        assert {
            if (state!!._debugLifecycleState == _StateLifecycle.created) {
                throw FlutterError(
                        "inheritFromWidgetOfExactType($targetType) was called before " +
                        "${state!!.runtimeType()}.initState() completed.\n" +
                        "When an inherited widget changes, for example if the value of Theme.of()" +
                        " changes, its dependent widgets are rebuilt. If the dependent widget's" +
                        " reference to the inherited widget is in a constructor or an initState()" +
                        " method, then the rebuilt dependent widget will not reflect the changes" +
                        " in the inherited widget.\n" +
                        "Typically references to to inherited widgets should occur in widget" +
                        " build() methods. Alternatively, initialization based on inherited " +
                        "widgets can be placed in the didChangeDependencies method, which is" +
                        " called after initState and whenever the dependencies change thereafter."
                )
            }
            if (state!!._debugLifecycleState == _StateLifecycle.defunct) {
                throw FlutterError(
                        "inheritFromWidgetOfExactType($targetType) called after dispose(): " +
                        "$this\n" +
                        "This error happens if you call inheritFromWidgetOfExactType() on the " +
                        "BuildContext for a widget that no longer appears in the widget tree " +
                        "(e.g., whose parent widget no longer includes the widget in its " +
                        "build). This error can occur when code calls " +
                        "inheritFromWidgetOfExactType() from a timer or an animation callback. " +
                        "The preferred solution is to cancel the timer or stop listening to the " +
                        "animation in the dispose() callback. Another solution is to check the " +
                        "'mounted' property of this object before calling " +
                        "inheritFromWidgetOfExactType() to ensure the object is still in the " +
                        "tree.\n" +
                        "This error might indicate a memory leak if " +
                        "inheritFromWidgetOfExactType() is being called because another object " +
                        "is retaining a reference to this State object after it has been " +
                        "removed from the tree. To avoid memory leaks, consider breaking the " +
                        "reference to this object during dispose()."
                )
            }
            true
        }
        return super.inheritFromWidgetOfExactType(targetType)
    }

    override fun didChangeDependencies() {
        super.didChangeDependencies()
        state!!.didChangeDependencies()
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("state", state, defaultValue = null))
    }
}