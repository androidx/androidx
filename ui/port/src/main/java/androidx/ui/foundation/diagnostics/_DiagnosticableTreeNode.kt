package androidx.ui.foundation.diagnostics

/// [DiagnosticsNode] for an instance of [DiagnosticableTree].
class _DiagnosticableTreeNode(
        name: String?,
        value: DiagnosticableTree,
        style: DiagnosticsTreeStyle?
) : DiagnosticableNode<DiagnosticableTree>(
        name = name,
        value = value,
        style = style) {

    override fun getChildren(): List<DiagnosticsNode> {
        value ?: return emptyList()
        return value.debugDescribeChildren();
    }
}