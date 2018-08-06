package androidx.ui.widgets.framework

// / An [Element] that uses a [StatelessWidget] as its configuration.
class StatelessElement(widget: StatelessWidget) : ComponentElement(widget) {

    override fun build(): Widget = (widget as StatelessWidget).build(this)

    override fun update(newWidget: Widget) {
        newWidget as StatelessWidget
        super.update(newWidget)
        assert(widget == newWidget)
        dirty = true
        rebuild()
    }
}
