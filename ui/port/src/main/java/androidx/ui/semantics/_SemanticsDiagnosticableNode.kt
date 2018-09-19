package androidx.ui.semantics

import androidx.ui.foundation.diagnostics.DiagnosticableNode
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.foundation.diagnostics.DiagnosticsTreeStyle

internal class _SemanticsDiagnosticableNode(
    name: String? = null,
    value: SemanticsNode,
    style: DiagnosticsTreeStyle,
    val childOrder: DebugSemanticsDumpOrder
) : DiagnosticableNode<SemanticsNode>(name = name, value = value, style = style) {

    override fun getChildren(): List<DiagnosticsNode> {
        TODO("Not implemented")
//    if (value != null)
//      return value.debugDescribeChildren(childOrder: childOrder);
//
//    return const <DiagnosticsNode>[];
    }
}