package androidx.ui.widgets.framework

/** An [Element] that uses a [ProxyWidget] as its configuration. */
abstract class ProxyElement(widget: ProxyWidget) : ComponentElement(widget) {

    override fun build(): Widget = (widget as ProxyWidget).child

    override fun update(newWidget: Widget) {
        val oldWidget = widget
        assert(widget != newWidget)
        super.update(newWidget)
        assert(widget == newWidget)
        notifyClients(oldWidget)
        dirty = true
        rebuild()
    }

    /**
     * Notify other objects that the widget associated with this element has changed.
     *
     * Called during [update] after changing the widget associated with this
     * element but before rebuilding this element.
     */
    protected abstract fun notifyClients(oldWidget: Widget)
}