package androidx.ui.foundation.diagnostics

// / Builder to accumulate properties and configuration used to assemble a
// / [DiagnosticsNode] from a [Diagnosticable] object.
class DiagnosticPropertiesBuilder {

    // / Default style to use for the [DiagnosticsNode] if no style is specified.
    var defaultDiagnosticsTreeStyle = DiagnosticsTreeStyle.sparse

    val properties = mutableListOf<DiagnosticsNode>()

    // / Description to show if the node has no displayed properties or children.
    var emptyBodyDescription: String = "" // TODO(Filip): Not sure what is the default here

    // / Add a property to the list of properties.
    fun add(property: DiagnosticsNode) {
        properties.add(property)
    }
}