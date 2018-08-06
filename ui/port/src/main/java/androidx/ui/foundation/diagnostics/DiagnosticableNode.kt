package androidx.ui.foundation.diagnostics

// / [DiagnosticsNode] that lazily calls the associated [Diagnosticable] [value]
// / to implement [getChildren] and [getProperties].
open class DiagnosticableNode<T : Diagnosticable>(
    name: String?,
    val value: T,
    style: DiagnosticsTreeStyle?
) : DiagnosticsNode(name = name, style = style) {

    override fun getValue(): Any? {
        return value
    }

    init {
        assert(value != null)
    }

    private val builder by lazy {
        val b = DiagnosticPropertiesBuilder()
        value?.debugFillProperties(b)
        return@lazy b
    }

    override fun getStyle(): DiagnosticsTreeStyle {
        return super.getStyle() ?: builder.defaultDiagnosticsTreeStyle
    }

    override fun getEmptyBodyDescription() = builder.emptyBodyDescription

    override fun getProperties(): List<DiagnosticsNode> = builder.properties

    override fun getChildren(): List<DiagnosticsNode> {
        return emptyList()
    }

    override fun toDescription(parentConfiguration: TextTreeConfiguration?): String {
        return value.toStringShort()
    }
}