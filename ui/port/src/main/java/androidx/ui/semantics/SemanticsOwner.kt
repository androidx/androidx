package androidx.ui.semantics

import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.change_notifier.ChangeNotifier
import androidx.ui.painting.matrixutils.transformPoint
import androidx.ui.vectormath64.Matrix4

/**
 * Owns [SemanticsNode] objects and notifies listeners of changes to the
 * render tree semantics.
 *
 * To listen for semantic updates, call [PipelineOwner.ensureSemantics] to
 * obtain a [SemanticsHandle]. This will create a [SemanticsOwner] if
 * necessary.
 */
class SemanticsOwner : ChangeNotifier() {
    internal val _dirtyNodes: MutableSet<SemanticsNode> = mutableSetOf()
    internal val _nodes: MutableMap<Int, SemanticsNode> = mutableMapOf()
    internal val _detachedNodes: MutableSet<SemanticsNode> = mutableSetOf()

    /**
     * The root node of the semantics tree, if any.
     *
     * If the semantics tree is empty, returns null.
     */
    val rootSemanticsNode: SemanticsNode?
        get() = _nodes[0]

    override fun dispose() {
        _dirtyNodes.clear()
        _nodes.clear()
        _detachedNodes.clear()
        super.dispose()
    }

    // / Update the semantics using [Window.updateSemantics].
    fun sendSemanticsUpdate() {
        if (_dirtyNodes.isEmpty()) {
            return
        }

        val customSemanticsActionIds: MutableSet<Int> = mutableSetOf()
        val visitedNodes: MutableList<SemanticsNode> = mutableListOf()
        while (_dirtyNodes.isNotEmpty()) {
            val localDirtyNodes =
                _dirtyNodes.filter { node: SemanticsNode -> !_detachedNodes.contains(node) }
                    .toMutableList()
            _dirtyNodes.clear()
            _detachedNodes.clear()
            localDirtyNodes.sortWith(Comparator { a: SemanticsNode, b: SemanticsNode ->
                a.depth - b.depth
            })
            visitedNodes.addAll(localDirtyNodes)
            for (node: SemanticsNode in localDirtyNodes) {
                assert(node._dirty)
                assert(node.parent == null ||
                        !node.parent!!.isPartOfNodeMerging ||
                        node.isMergedIntoParent)
                if (node.isPartOfNodeMerging) {
                    assert(node.mergeAllDescendantsIntoThisNode || node.parent != null)
                    // if we're merged into our parent, make sure our parent is added to the dirty list
                    node.parent?.let {
                        if (it.isPartOfNodeMerging) {
                            it._markDirty() // this can add the node to the dirty list
                        }
                    }
                }
            }
        }
        visitedNodes.sortWith(Comparator { a: SemanticsNode, b: SemanticsNode ->
            a.depth - b.depth
        })
        val builder: SemanticsUpdateBuilder = SemanticsUpdateBuilder()
        for (node in visitedNodes) {
            assert(node.parent?._dirty != true); // could be null (no parent) or false (not dirty)
            // The _serialize() method marks the node as not dirty, and
            // recurses through the tree to do a deep serialization of all
            // contiguous dirty nodes. This means that when we return here,
            // it's quite possible that subsequent nodes are no longer
            // dirty. We skip these here.
            // We also skip any nodes that were reset and subsequently
            // dropped entirely (RenderObject.markNeedsSemanticsUpdate()
            // calls reset() on its SemanticsNode if onlyChanges isn't set,
            // which happens e.g. when the node is no longer contributing
            // semantics).
            if (node._dirty && node.attached) {
                node._addToUpdate(builder, customSemanticsActionIds)
            }
        }
        _dirtyNodes.clear()

        TODO("Need Window plumbed through")
//    ui.window.updateSemantics(builder.build());
//    notifyListeners();
    }

    fun _getSemanticsActionHandlerForId(
        id: Int,
        action: SemanticsAction
    ): _SemanticsActionHandler? {
        var result: SemanticsNode? = _nodes[id]
        if (result != null && result.isPartOfNodeMerging && !result._canPerformAction(action)) {
            result._visitDescendants { node: SemanticsNode ->
                if (node._canPerformAction(action)) {
                    result = node
                    return@_visitDescendants false // found node, abort walk
                }
                return@_visitDescendants true // continue walk
            }
        }
        if (result?._canPerformAction(action) != true) {
            return null
        }
        return result!!._actions[action]
    }

    // / Asks the [SemanticsNode] with the given id to perform the given action.
    // /
    // / If the [SemanticsNode] has not indicated that it can perform the action,
    // / this function does nothing.
    // /
    // / If the given `action` requires arguments they need to be passed in via
    // / the `args` parameter.
    fun performAction(id: Int, action: SemanticsAction, args: Any? = null) {
        assert(action != null)
        val handler = _getSemanticsActionHandlerForId(id, action)
        if (handler != null) {
            handler(args)
            return
        }

        // Default actions if no [handler] was provided.
        if (action == SemanticsAction.showOnScreen) {
            _nodes[id]!!.showOnScreen?.invoke()
        }
    }

    fun _getSemanticsActionHandlerForPosition(
        node: SemanticsNode,
        position: Offset,
        action: SemanticsAction
    ): _SemanticsActionHandler? {
        var position = position
        node.transform?.let {
            val inverse = Matrix4.identity()
            if (inverse.copyInverse(it) == 0.0)
                return null
            position = inverse.transformPoint(position)
        }
        if (!node.rect.contains(position)) {
            return null
        }
        if (node.mergeAllDescendantsIntoThisNode) {
            var result: SemanticsNode? = null
            node._visitDescendants { child: SemanticsNode ->
                if (child._canPerformAction(action)) {
                    result = child
                    return@_visitDescendants false
                }
                return@_visitDescendants true
            }
            return result?._actions!![action]
        }
        if (node.hasChildren) {
            for (child in node._children!!.reversed()) {
                val handler = _getSemanticsActionHandlerForPosition(child, position, action)
                if (handler != null)
                    return handler
            }
        }
        return node._actions[action]
    }
//
//  /// Asks the [SemanticsNode] at the given position to perform the given action.
//  ///
//  /// If the [SemanticsNode] has not indicated that it can perform the action,
//  /// this function does nothing.
//  ///
//  /// If the given `action` requires arguments they need to be passed in via
//  /// the `args` parameter.
//  void performActionAt(Offset position, SemanticsAction action, [dynamic args]) {
//    assert(action != null);
//    val node: SemanticsNode = rootSemanticsNode;
//    if (node == null)
//      return;
//    val handler: _SemanticsActionHandler = _getSemanticsActionHandlerForPosition(node, position, action);
//    if (handler != null)
//      handler(args);
//  }
//
//  @override
//  String toString() => describeIdentity(this);
}