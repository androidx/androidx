package androidx.ui.widgets.framework

import androidx.ui.assert
import androidx.ui.developer.timeline.Timeline
import androidx.ui.runtimeType
import androidx.ui.widgets.debugProfileBuildsEnabled
import androidx.ui.widgets.debugWidgetBuilderValue

/**
 * Signature for the callback to [BuildContext.visitChildElements].
 *
 * The argument is the child being visited.
 *
 * It is safe to call `element.visitChildElements` reentrantly within
 * this callback.
 */
typealias ElementVisitor = (Element) -> Unit

/**
 * An [Element] that composes other [Element]s.
 *
 * Rather than creating a [RenderObject] directly, a [ComponentElement] creates
 * [RenderObject]s indirectly by creating other [Element]s.
 *
 * Contrast with [RenderObjectElement].
 */
abstract class ComponentElement(widget: Widget) : Element(widget) {

    var _child: Element? = null

    override fun mount(parent: Element?, newSlot: Any?) {
        super.mount(parent, newSlot)
        assert(_child == null)
        assert(_active)
        _firstBuild()
        assert(_child != null)
    }

    protected open fun _firstBuild() {
        rebuild()
    }

    /**
     * Calls the [StatelessWidget.build] method of the [StatelessWidget] object
     * (for stateless widgets) or the [State.build] method of the [State] object
     * (for stateful widgets) and then updates the widget tree.
     *
     * Called automatically during [mount] to generate the first build, and by
     * [rebuild] when the element needs updating.
     */
    override fun performRebuild() {
        assert {
            if (debugProfileBuildsEnabled) {
                Timeline.startSync("${widget.runtimeType()}")
            }
            true
        }

        assert(_debugSetAllowIgnoredCallsToMarkNeedsBuild(true))
        var built: Widget
        try {
            built = build()
            debugWidgetBuilderValue(widget, built)
        } catch (e: Throwable) {
            built = ErrorWidget.builder(_debugReportException("building $this", e, e.stackTrace))
        } finally {
            // We delay marking the element as clean until after calling build() so
            // that attempts to markNeedsBuild() during build() will be ignored.
            dirty = false
            assert(_debugSetAllowIgnoredCallsToMarkNeedsBuild(false))
        }
        try {
            _child = updateChild(_child, built, slot)
            assert(_child != null)
        } catch (e: Throwable) {
            built = ErrorWidget.builder(_debugReportException("building $this", e, e.stackTrace))
            _child = updateChild(null, built, slot)
        }

        assert {
            if (debugProfileBuildsEnabled) {
                Timeline.finishSync()
            }
            true
        }
    }

    /**
     * Subclasses should override this function to actually call the appropriate
     * `build` function (e.g., [StatelessWidget.build] or [State.build]) for
     * their widget.
     */
    protected abstract fun build(): Widget

    override fun visitChildren(visitor: ElementVisitor) {
        if (_child != null)
            visitor(_child!!)
    }

    override fun forgetChild(child: Element) {
        assert(child == _child)
        _child = null
    }
}