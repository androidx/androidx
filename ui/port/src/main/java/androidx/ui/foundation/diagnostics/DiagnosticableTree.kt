package androidx.ui.foundation.diagnostics

/// A base class for providing string and [DiagnosticsNode] debug
/// representations describing the properties and children of an object.
///
/// The string debug representation is generated from the intermediate
/// [DiagnosticsNode] representation. The [DiagnosticsNode] representation is
/// also used by debugging tools displaying interactive trees of objects and
/// properties.
///
/// See also:
///
///  * [DiagnosticableTreeMixin], which provides a mixin that implements this
///    class.
///  * [Diagnosticable], which should be used instead of this class to provide
///    diagnostics for objects without children.
abstract class DiagnosticableTree : Diagnosticable() {

    /// Returns a one-line detailed description of the object.
    ///
    /// This description is often somewhat long. This includes the same
    /// information given by [toStringDeep], but does not recurse to any children.
    ///
    /// `joiner` specifies the string which is place between each part obtained
    /// from [debugFillProperties]. Passing a string such as `'\n '` will result
    /// in a multiline string that indents the properties of the object below its
    /// name (as per [toString]).
    ///
    /// `minLevel` specifies the minimum [DiagnosticLevel] for properties included
    /// in the output.
    ///
    /// See also:
    ///
    ///  * [toString], for a brief description of the object.
    ///  * [toStringDeep], for a description of the subtree rooted at this object.
    open fun toStringShallow(
        joiner: String = ", ",
        minLevel: DiagnosticLevel = DiagnosticLevel.debug
    ): String {
        val result = StringBuffer()
        result.append(toString())
        result.append(joiner)
        val builder = DiagnosticPropertiesBuilder()
        debugFillProperties(builder)
        result.append(
            builder.properties.filter { !it.isFiltered(minLevel) }.joinToString(separator = joiner)
        )
        return result.toString()
    }

    /// Returns a string representation of this node and its descendants.
    ///
    /// `prefixLineOne` will be added to the front of the first line of the
    /// output. `prefixOtherLines` will be added to the front of each other line.
    /// If `prefixOtherLines` is null, the `prefixLineOne` is used for every line.
    /// By default, there is no prefix.
    ///
    /// `minLevel` specifies the minimum [DiagnosticLevel] for properties included
    /// in the output.
    ///
    /// The [toStringDeep] method takes other arguments, but those are intended
    /// for internal use when recursing to the descendants, and so can be ignored.
    ///
    /// See also:
    ///
    ///  * [toString], for a brief description of the object but not its children.
    ///  * [toStringShallow], for a detailed description of the object but not its
    ///    children.
    open fun toStringDeep(
        prefixLineOne: String = "",
        prefixOtherLines: String,
        minLevel: DiagnosticLevel = DiagnosticLevel.debug
    ): String {
        return toDiagnosticsNode().toStringDeep(
            prefixLineOne = prefixLineOne,
            prefixOtherLines = prefixOtherLines,
            minLevel = minLevel)
    }

    override fun toStringShort(): String = describeIdentity(this)

    override fun toDiagnosticsNode(
            name: String?,
            style: DiagnosticsTreeStyle?
    ): DiagnosticsNode {
        return _DiagnosticableTreeNode(
                name = name,
                value = this,
                style = style
        )
    }

    /// Returns a list of [DiagnosticsNode] objects describing this node's
    /// children.
    ///
    /// Children that are offstage should be added with `style` set to
    /// [DiagnosticsTreeStyle.offstage] to indicate that they are offstage.
    ///
    /// The list must not contain any null entries. If there are explicit null
    /// children to report, consider [new DiagnosticsNode.message] or
    /// [DiagnosticsProperty<Object>] as possible [DiagnosticsNode] objects to
    /// provide.
    ///
    /// See also:
    ///
    ///  * [RenderTable.debugDescribeChildren], which provides high quality custom
    ///    descriptions for its child nodes.
    ///
    /// Used by [toStringDeep], [toDiagnosticsNode] and [toStringShallow].
    open fun debugDescribeChildren(): List<DiagnosticsNode> = emptyList()
}