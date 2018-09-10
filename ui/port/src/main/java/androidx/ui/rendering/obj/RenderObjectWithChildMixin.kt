package androidx.ui.rendering.obj

import androidx.ui.foundation.diagnostics.DiagnosticsNode

// / Generic mixin for render objects with one child.
// /
// / Provides a child model for a render object subclass that has a unique child.
// TODO(Migration/andrey): in this specific case we can use abstract class instead of mixin
abstract class RenderObjectWithChildMixin<ChildType : RenderObject> : RenderObject() {
//    // This class is intended to be used as a mixin, and should not be
//    // extended directly.
//    factory RenderObjectWithChildMixin._() => null;

    // TODO(Migration/andrey): no way to have (child is! ChildType) in Kotlin. but maybe we don't need this method anyway
    // / Checks whether the given render object has the correct [runtimeType] to be
    // / a child of this render object.
    // /
    // / Does nothing if assertions are disabled.
    // /
    // / Always returns true.
    fun debugValidateChild(child: RenderObject?): Boolean {
        // TODO(Migration/andrey): no way to have (child is! ChildType) in Kotlin. but maybe we don't need this method anyway
//        assert(() {
//            if (child is! ChildType) {
//            throw new FlutterError(
//                    'A $runtimeType expected a child of type $ChildType but received a '
//            'child of type ${child.runtimeType}.\n'
//            'RenderObjects expect specific types of children because they '
//            'coordinate with their children during layout and paint. For '
//            'example, a RenderSliver cannot be the child of a RenderBox because '
//            'a RenderSliver does not understand the RenderBox layout protocol.\n'
//            '\n'
//            'The $runtimeType that expected a $ChildType child was created by:\n'
//            '  $debugCreator\n'
//            '\n'
//            'The ${child.runtimeType} that did not match the expected child type '
//            'was created by:\n'
//            '  ${child.debugCreator}\n'
//            );
//        }
//            return true;
//        }());
        return true
    }

    // / The render object's unique child
    var child: ChildType? = null
        set(value) {
            field?.let { dropChild(it) }
            field = value
            field?.let { adoptChild(it) }
        }

    override fun attach(owner: Any) {
        super.attach(owner)
        child?.attach(owner)
    }

    override fun detach() {
        super.detach()
        child?.detach()
    }

    override fun redepthChildren() {
        child?.let { redepthChild(it) }
    }

    override fun visitChildren(visitor: RenderObjectVisitor) {
        child?.let { visitor(it) }
    }

    override fun debugDescribeChildren(): List<DiagnosticsNode> {
        return if (child != null) listOf(child!!.toDiagnosticsNode(name = "child")) else emptyList()
    }
}