/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.areObjectsOfSameType
import androidx.compose.ui.node.TraversableNode.Companion.VisitSubtreeIfAction

/**
 * Allows [Modifier.Node] classes to traverse up/down the Node tree for classes of the same type or
 * for a particular key (traverseKey).
 *
 * Note: The actual traversals are done in extension functions (see bottom of file).
 */
interface TraversableNode : DelegatableNode {
    val traverseKey: Any

    companion object {
        /**
         * Tree traversal actions for the traverseSubtreeWithKeyIf related functions:
         *  - VisitSubtree - visit the subtree of current match
         *  - SkipSubtree - do NOT visit the subtree of the current match
         *  - CancelTraversal - cancels the traversal (returns from function call)
         *
         * To see examples of all the actions, see TraversableModifierNodeTest. For a return/cancel
         * example specifically, see
         * traverseSubtreeWithSameKeyIf_cancelTraversalOfDifferentClassSameKey().
         */
        enum class VisitSubtreeIfAction {
            VisitSubtree,
            SkipSubtree,
            CancelTraversal
        }
    }
}

// *********** Nearest Traversable Ancestor methods ***********
/**
 * Finds the nearest ancestor with a matching [key].
 */
fun DelegatableNode.nearestTraversableAncestorWithKey(
    key: Any?
): TraversableNode? {
    visitAncestors(Nodes.Traversable) {
        if (key == it.traverseKey) {
            return it
        }
    }
    return null
}

/**
 * Finds the nearest ancestor of the same class and key.
 */
fun <T> T.nearestTraversableAncestor(): T? where T : TraversableNode {
    visitAncestors(Nodes.Traversable) {
        if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
            @Suppress("UNCHECKED_CAST")
            return it as T
        }
    }
    return null
}

// *********** Traverse Ancestors methods ***********
/**
 * Executes [block] for all ancestors with a matching [key].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
fun DelegatableNode.traverseAncestorsWithKey(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitAncestors(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all ancestors of the same class and key.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
fun <T> T.traverseAncestors(block: (T) -> Boolean) where T : TraversableNode {
    visitAncestors(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

// *********** Traverse Children methods ***********
/**
 * Executes [block] for all direct children of the node with a matching [key].
 *
 * Note 1: This stops at the children and does not include grandchildren and so on down the tree.
 *
 * Note 2: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
fun DelegatableNode.traverseChildrenWithKey(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitChildren(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all direct children of the node that are of the same class.
 *
 * Note 1: This stops at the children and does not include grandchildren and so on down the tree.
 *
 * Note 2: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
fun <T> T.traverseChildren(block: (T) -> Boolean) where T : TraversableNode {
    visitChildren(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

// *********** Traverse Subtree methods ***********
/**
 * Executes [block] for all nodes in the subtree with a matching [key].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
fun DelegatableNode.traverseSubtreeWithKey(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitSubtree(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all nodes of the same class in the subtree.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
fun <T> T.traverseSubtree(block: (T) -> Boolean) where T : TraversableNode {
    visitSubtree(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

// *********** Traverse Subtree If methods ***********
/**
 * Conditionally executes [block] for each node with a matching [key] in the subtree.
 *
 * Note 1: For nodes that do not have the same key, it will continue to execute the [block] for
 * the subtree below that non-matching node (where there may be a node that matches).
 *
 * Note 2: The parameter [block]'s return value [VisitSubtreeIfAction] will determine the next step
 * in the traversal.
 */
fun DelegatableNode.traverseSubtreeIfWithKey(
    key: Any?,
    block: (TraversableNode) -> VisitSubtreeIfAction
) {
    visitSubtreeIf(Nodes.Traversable) {
        val action = if (key == it.traverseKey) {
            block(it)
        } else {
            VisitSubtreeIfAction.VisitSubtree
        }
        if (action == VisitSubtreeIfAction.CancelTraversal) return

        // visitSubtreeIf() requires a true to continue down the subtree and a false if you
        // want to skip the subtree, so we check if the action is NOT EQUAL to the subtree
        // to trigger false if the action is Skip subtree and true otherwise.
        action != VisitSubtreeIfAction.SkipSubtree
    }
}

/**
 * Conditionally executes [block] for each node of the same class in the subtree.
 *
 * Note 1: For nodes that do not have the same key, it will continue to execute the [block] for
 * the subtree below that non-matching node (where there may be a node that matches).
 *
 * Note 2: The parameter [block]'s return value [VisitSubtreeIfAction] will determine the next step
 * in the traversal.
 */
fun <T> T.traverseSubtreeIf(block: (T) -> VisitSubtreeIfAction) where T : TraversableNode {
    visitSubtreeIf(Nodes.Traversable) {
        val action =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                VisitSubtreeIfAction.VisitSubtree
            }
        if (action == VisitSubtreeIfAction.CancelTraversal) return

        // visitSubtreeIf() requires a true to continue down the subtree and a false if you
        // want to skip the subtree, so we check if the action is NOT EQUAL to the subtree
        // to trigger false if the action is Skip subtree and true otherwise.
        action != VisitSubtreeIfAction.SkipSubtree
    }
}
