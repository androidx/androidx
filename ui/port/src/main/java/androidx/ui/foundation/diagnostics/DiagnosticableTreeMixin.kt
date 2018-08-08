package androidx.ui.foundation.diagnostics

// / A class that can be used as a mixin that helps dump string and
// / [DiagnosticsNode] representations of trees.
// /
// / This class is identical to DiagnosticableTree except that it can be used as
// / a mixin.
abstract class DiagnosticableTreeMixin : DiagnosticableTree() {

    override fun toString(): String {
        return toStringParametrized(DiagnosticLevel.debug)
    }

    override fun toStringParametrized(minLevel: DiagnosticLevel): String {
        return toDiagnosticsNode(style = DiagnosticsTreeStyle.singleLine)
                .toStringParametrized(minLevel = minLevel)
    }

    override fun toStringShallow(
        joiner: String,
        minLevel: DiagnosticLevel
    ): String {
        val result = StringBuffer()
        result.append(toStringShort())
        result.append(joiner)
        val builder = DiagnosticPropertiesBuilder()
        debugFillProperties(builder)
        result.append(
                builder.properties
                        .filter { !it.isFiltered(minLevel) }
                        .joinToString(separator = joiner)
        )
        return result.toString()
    }

    override fun toStringDeep(
        prefixLineOne: String,
        prefixOtherLines: String,
        minLevel: DiagnosticLevel
    ): String {
        return toDiagnosticsNode().toStringDeep(
                prefixLineOne = prefixLineOne,
                prefixOtherLines = prefixOtherLines,
                minLevel = minLevel
        )
    }

    override fun toStringShort() = describeIdentity(this)

    override fun toDiagnosticsNode(name: String?, style: DiagnosticsTreeStyle?): DiagnosticsNode {
        return _DiagnosticableTreeNode(
                name = name,
                value = this,
                style = style
        )
    }

    override fun debugDescribeChildren(): List<DiagnosticsNode> = emptyList()

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {}
}