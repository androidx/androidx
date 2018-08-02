package androidx.ui.foundation.diagnostics

/// Styles for displaying a node in a [DiagnosticsNode] tree.
///
/// See also:
///
///  * [DiagnosticsNode.toStringDeep], which dumps text art trees for these
///    styles.
enum class DiagnosticsTreeStyle {
    /// Sparse style for displaying trees.
    ///
    /// See also:
    ///
    ///  * [RenderObject], which uses this style.
    sparse,

    /// Connects a node to its parent with a dashed line.
    ///
    /// See also:
    ///
    ///  * [RenderSliverMultiBoxAdaptor], which uses this style to distinguish
    ///    offstage children from onstage children.
    offstage,

    /// Slightly more compact version of the [sparse] style.
    ///
    /// See also:
    ///
    ///  * [Element], which uses this style.
    dense,

    /// Style that enables transitioning from nodes of one style to children of
    /// another.
    ///
    /// See also:
    ///
    ///  * [RenderParagraph], which uses this style to display a [TextSpan] child
    ///    in a way that is compatible with the [DiagnosticsTreeStyle.sparse]
    ///    style of the [RenderObject] tree.
    transition,

    /// Render the tree just using whitespace without connecting parents to
    /// children using lines.
    ///
    /// See also:
    ///
    ///  * [SliverGeometry], which uses this style.
    whitespace,

    /// Render the tree on a single line without showing children.
    singleLine
}