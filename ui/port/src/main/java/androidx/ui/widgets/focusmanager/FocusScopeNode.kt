/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.widgets.focusmanager

/**
 * An interior node in the focus tree.
 *
 * The focus tree keeps track of which widget is the user's current focus. The focused widget often
 * listens for keyboard events.
 *
 * The interior nodes in the focus tree cannot themselves be focused but instead remember previous
 * focus states. A scope is currently active in its parent whenever [isFirstFocus] is true. If that
 * scope is detached from its parent, its previous sibling becomes the parent's first focus.
 *
 * A [FocusNode] has the overall focus when the node is focused in its parent [FocusScopeNode] and
 * [FocusScopeNode.isFirstFocus] is true for that scope and all its ancestor scopes.
 *
 * See also:
 *
 *  * [FocusNode], which is a leaf node in the focus tree that can receive focus.
 *  * [FocusScope.of], which provides the [FocusScopeNode] for a given [BuildContext].
 *  * [FocusScope], which is a widget that associates a [FocusScopeNode] with its location in the
 *  tree.
 */
// TODO(Migration/nona): Inherit from DiagnosticableTreeMixin
class FocusScopeNode {
    var manager: FocusManager? = null
    internal var parent: FocusScopeNode? = null

    internal var nextSibling: FocusScopeNode? = null
    internal var previousSibling: FocusScopeNode? = null

    internal var firstChild: FocusScopeNode? = null
    internal var lastChild: FocusScopeNode? = null

    internal var focus: FocusNode? = null

    /** Whether this scope is currently active in its parent scope. */
    val isFirstFocus
        get() = parent?.let { it.firstChild == this } ?: true

    private fun prepend(child: FocusScopeNode) {
        assert(child != this)
        assert(child != firstChild)
        assert(child != lastChild)
        assert(child.parent == null)
        assert(child.manager == null)
        assert(child.nextSibling == null)
        assert(child.previousSibling == null)

        class LoopChecker() {
            fun checkLoop(): Boolean {
                var node = this@FocusScopeNode
                while (node.parent != null) {
                    node = node.parent!!
                }
                return node != child // indicates we are about to create a cycle
            }
        }
        assert(LoopChecker().checkLoop())
        child.parent = this
        child.nextSibling = firstChild
        firstChild?.let { it.previousSibling = child }
        firstChild = child
        if (lastChild == null) {
            lastChild = child
        }
        child.updateManager(manager)
    }

    private fun updateManager(manager: FocusManager?) {
        fun update(child: FocusScopeNode) {
            if (child.manager == manager) return
            child.manager = manager
            // We don't proactively null out the manager for FocusNodes because the manager holds
            // the currently active focus node until the end of the microtask, even if that node is
            // detached from the focus tree.
            if (manager != null) child.focus?.manager = manager
            child.visitChildren(::update)
        }

        update(this)
    }

    private fun visitChildren(visitor: (FocusScopeNode) -> Unit) {
        var child = firstChild
        while (child != null) {
            visitor(child)
            child = child.nextSibling
        }
    }

    private fun debugUltimatePreviousSiblingOf(
        child: FocusScopeNode,
        equals: FocusScopeNode?
    ): Boolean {
        var tmpChild = child
        while (true) {
            assert(tmpChild.previousSibling != tmpChild)
            tmpChild = tmpChild.previousSibling ?: break
        }
        return tmpChild == equals
    }

    private fun debugUltimateNextSiblingOf(
        child: FocusScopeNode,
        equals: FocusScopeNode?
    ): Boolean {
        var tmpChild = child
        while (true) {
            assert(tmpChild.nextSibling != tmpChild)
            tmpChild = tmpChild.nextSibling ?: break
        }
        return tmpChild == equals
    }

    private fun remove(child: FocusScopeNode) {
        assert(child.parent == this)
        assert(child.manager == manager)
        assert(debugUltimatePreviousSiblingOf(child, equals = firstChild))
        assert(debugUltimateNextSiblingOf(child, equals = lastChild))
        val localPrevSib = child.previousSibling
        if (localPrevSib == null) {
            assert(firstChild == child)
            firstChild = child.nextSibling
        } else {
            localPrevSib.previousSibling?.let { it.nextSibling = child.nextSibling }
        }
        val localNextSib = child.nextSibling
        if (localNextSib == null) {
            assert(lastChild == child)
            lastChild = child.previousSibling
        } else {
            localNextSib.previousSibling = child.previousSibling
        }
        child.previousSibling = null
        child.nextSibling = null
        child.parent = null
        child.updateManager(null)
    }

    private fun focusChainChanged() {
        if (isFirstFocus) manager?.markNeedsUpdate()
    }

    /**
     * Requests that the given node becomes the focus for this scope.
     *
     * If the given node is currently focused in another scope, the node will first be unfocused in
     * that scope.
     *
     * The node will receive the overall focus if this [isFirstFocus] is true in this scope and all
     * its ancestor scopes. The node is notified that it has received the overall focus in a
     * microtask.
     */
    fun requestFocus(node: FocusNode) {
        if (focus == node) return
        focus?.unfocus()
        node.hasKeyboardToken = true
        setFocus(node)
    }

    /**
     * If this scope lacks a focus, request that the given node becomes the focus.
     *
     * Useful for widgets that wish to grab the focus if no other widget already has the focus.
     *
     * The node is notified that it has received the overall focus in a microtask.
     */
    fun autofocus(node: FocusNode) {
        if (focus == null) {
            node.hasKeyboardToken = true
            setFocus(node)
        }
    }

    /**
     * Adopts the given node if it is focused in another scope.
     *
     * A widget that requests that a node is focused should call this method
     * during its `build` method in case the widget is moved from one location
     * in the tree to another location that has a different focus scope.
     */
    fun reparentIfNeeded(node: FocusNode) {
        if (node.parent == null || node.parent == this) return
        node.unfocus()
        assert(node.parent == null)
        if (focus == null) setFocus(node)
    }

    private fun setFocus(node: FocusNode) {
        assert(node.parent == null)
        assert(focus == null)
        node.parent = this
        node.manager = manager
        node.hasKeyboardToken = true
        focus = node
        focusChainChanged()
    }

    internal fun resignFocus(node: FocusNode) {
        focus.let {
            if (it != node) return
            it.parent = null
            it.manager = null
        }
        focus = null
        focusChainChanged()
    }

    /**
     * Makes the given child the first focus of this scope.
     *
     * If the child has another parent scope, the child is first removed from that scope. After this
     * method returns [isFirstFocus] will be true for the child.
     */
    fun setFirstFocus(child: FocusScopeNode) {
        assert(child.parent == null || child.parent == this)
        if (firstChild == child) return
        child.detach()
        prepend(child)
        assert(child.parent == this)
        focusChainChanged()
    }

    /**
     * Adopts the given scope if it is the first focus of another scope.
     *
     * A widget that sets a scope as the first focus of another scope should call this method during
     * its `build` method in case the widget is moved from one location in the tree to another
     * location that has a different focus scope.
     *
     * If the given scope is not the first focus of its old parent, the scope is simply detached
     * from its old parent.
     */
    fun reparentScopeIfNeeded(child: FocusScopeNode) {
        if (child.parent == null || child.parent == this) return

        if (child.isFirstFocus) setFirstFocus(child)
        else child.detach()
    }

    /**
     * Remove this scope from its parent child list.
     *
     * This method is safe to call even if this scope does not have a parent.
     *
     * A widget that sets a scope as the first focus of another scope should call this method during
     * [State.dispose] to avoid leaving dangling children in their parent scope.
     */
    fun detach() {
        focusChainChanged()
        parent?.remove(this)
        assert(parent == null)
    }

//    @override
//    void debugFillProperties(DiagnosticPropertiesBuilder properties) {
//        super.debugFillProperties(properties);
//        if (_focus != null)
//            properties.add(DiagnosticsProperty<FocusNode>('focus', _focus));
//    }
//
//    @override
//    List<DiagnosticsNode> debugDescribeChildren() {
//        final List<DiagnosticsNode> children = <DiagnosticsNode>[];
//        if (_firstChild != null) {
//            FocusScopeNode child = _firstChild;
//            int count = 1;
//            while (true) {
//                children.add(child.toDiagnosticsNode(name: 'child $count'));
//                if (child == _lastChild)
//                    break;
//                child = child._nextSibling;
//                count += 1;
//            }
//        }
//        return children;
//    }
}