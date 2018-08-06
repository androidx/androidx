package androidx.ui.widgets.framework.key

import androidx.ui.assert
import androidx.ui.foundation.Key
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.runtimeType
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.Element
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulElement
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.framework._ElementLifecycle

// / A key that is unique across the entire app.
// /
// / Global keys uniquely identify elements. Global keys provide access to other
// / objects that are associated with elements, such as the a [BuildContext] and,
// / for [StatefulWidget]s, a [State].
// /
// / Widgets that have global keys reparent their subtrees when they are moved
// / from one location in the tree to another location in the tree. In order to
// / reparent its subtree, a widget must arrive at its new location in the tree
// / in the same animation frame in which it was removed from its old location in
// / the tree.
// /
// / Global keys are relatively expensive. If you don't need any of the features
// / listed above, consider using a [Key], [ValueKey], [ObjectKey], or
// / [UniqueKey] instead.
// /
// / You cannot simultaneously include two widgets in the tree with the same
// / global key. Attempting to do so will assert at runtime.
// /
// / See also the discussion at [Widget.key].
// /
// / Ctor comment:
// / Creates a global key without a label.
// /
// / Used by subclasses because the factory constructor shadows the implicit
// / constructor.
abstract class GlobalKey<T : State<StatefulWidget>>() : Key() {

    companion object {

        // / Creates a [LabeledGlobalKey], which is a [GlobalKey] with a label used for
        // / debugging.
        // /
        // / The label is purely for debugging and not used for comparing the identity
        // / of the key.
        fun <T : State<StatefulWidget>> withLabel(debugLabel: String): LabeledGlobalKey<T> {
            return LabeledGlobalKey(debugLabel)
        }

        val _registry: MutableMap<GlobalKey<*>, Element> = mutableMapOf()
        val _removedKeys: MutableSet<GlobalKey<*>> = mutableSetOf()
        val _debugIllFatedElements: MutableSet<Element> = mutableSetOf()
        val _debugReservations: MutableMap<GlobalKey<*>, Element> = mutableMapOf()

        fun _debugVerifyIllFatedPopulation() {
            assert {
                var duplicates: MutableMap<GlobalKey<*>, MutableSet<Element>>? = null
                for (element in _debugIllFatedElements) {
                if (element._debugLifecycleState != _ElementLifecycle.defunct) {
                    assert(element != null)
                    assert(element.widget != null)
                    assert(element.widget.key != null)
                    val key = element.widget.key
                    assert(_registry.containsKey(key))
                    duplicates = duplicates ?: mutableMapOf()
                    val elements = duplicates.getOrPut(key as GlobalKey<*>, { mutableSetOf() })
                    elements!!.add(element)
                    elements!!.add(_registry[key]!!)
                }
            }
                _debugIllFatedElements.clear()
                _debugReservations.clear()
                if (duplicates != null) {
                   val buffer = StringBuffer()
                    buffer.append("Multiple widgets used the same GlobalKey.\n")
                    for (key in duplicates.keys) {
                        val elements = duplicates[key]
                        buffer.append("The key $key was used by ${elements!!.size} widgets:")
                        for (element in elements!!)
                        buffer.append("- $element")
                    }
                    buffer.append("A GlobalKey can only be specified on one widget at a time in the widget tree.")
                    throw FlutterError(buffer.toString())
                }
                true
            }
        }
    }

    fun _register(element: Element) {
        assert {
            if (_registry.containsKey(this)) {
                assert(element.widget != null)
                assert(_registry[this]!!.widget != null)
                assert(element.widget.runtimeType() != _registry[this]!!.widget.runtimeType())
                _debugIllFatedElements.add(_registry[this]!!)
            }
            true
        }
        _registry[this] = element
    }

    fun _unregister(element: Element) {
        assert {
            if (_registry.containsKey(this) && _registry[this] != element) {
                assert(element.widget != null)
                assert(_registry[this]!!.widget != null)
                assert(element.widget.runtimeType() != _registry[this]!!.widget.runtimeType())
            }
            true
        }
        if (_registry[this] == element) {
            _registry.remove(this)
            _removedKeys.add(this)
        }
    }

    fun _debugReserveFor(parent: Element) {
        assert {
            assert(parent != null)
            if (_debugReservations.containsKey(this) && _debugReservations[this] != parent) {
                // It's possible for an element to get built multiple times in one
                // frame, in which case it'll reserve the same child's key multiple
                // times. We catch multiple children of one widget having the same key
                // by verifying that an element never steals elements from itself, so we
                // don't care to verify that here as well.
                val older = _debugReservations[this].toString()
                val newer = parent.toString()
                if (older != newer) {
                    throw FlutterError(
                            "Multiple widgets used the same GlobalKey.\n" +
                            "The key $this was used by multiple widgets. The parents of those widgets were:\n" +
                            "- $older\n" +
                            "- $newer\n" +
                            "A GlobalKey can only be specified on one widget at a time in the widget tree."
                    )
                }
                throw FlutterError(
                        "Multiple widgets used the same GlobalKey.\n" +
                        "The key $this was used by multiple widgets. The parents of those widgets were " +
                        "different widgets that both had the following description:\n" +
                        "  $newer\n" +
                        "A GlobalKey can only be specified on one widget at a time in the widget tree."
                )
            }
            _debugReservations[this] = parent
            true
        }
    }

    val _currentElement get() = _registry[this]

    // / The build context in which the widget with this key builds.
    // /
    // / The current context is null if there is no widget in the tree that matches
    // / this global key.
    val currentContext: BuildContext get() = _currentElement as BuildContext

    // / The widget in the tree that currently has this global key.
    // /
    // / The current widget is null if there is no widget in the tree that matches
    // / this global key.
    val currentWidget: Widget get() = _currentElement?.widget!!

    // / The [State] for the widget in the tree that currently has this global key.
    // /
    // / The current state is null if (1) there is no widget in the tree that
    // / matches this global key, (2) that widget is not a [StatefulWidget], or the
    // / associated [State] object is not a subtype of `T`.
    inline fun <reified T> currentState(): T? {
        val element = _currentElement
        if (element is StatefulElement) {
            // final StatefulElement statefulElement = element;
            val state = element.state
            if (state is T)
                return state
        }
        return null
    }
}