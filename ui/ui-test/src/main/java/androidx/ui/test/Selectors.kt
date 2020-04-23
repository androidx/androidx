/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import androidx.ui.core.semantics.SemanticsNode

internal val SemanticsNode.siblings: List<SemanticsNode>
    get() {
        val node = this
        return parent?.run { this.children.filter { it.id != node.id } } ?: emptyList()
    }

/**
 * Returns a parent of this node.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 */
fun SemanticsNodeInteraction.parent(): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve a parent.")

    val parentMatcher = selector.appendSelector("parent") { listOfNotNull(node.parent) }
    return SemanticsNodeInteraction(parentMatcher)
}

/**
 * Returns children of this node.
 */
fun SemanticsNodeInteraction.children(): List<SemanticsNodeInteraction> {
    val node = fetchSemanticsNode("Failed to retrieve children.")

    val childrenMatcher = selector.appendSelector("children") { node.children }
    return childrenMatcher.match(getAllSemanticsNodes()).map {
        SemanticsNodeInteraction(it, childrenMatcher)
    }
}

/**
 * Returns exactly one child of this node.
 *
 * Use this only if this node has exactly one child.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 */
fun SemanticsNodeInteraction.child(): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve a child.")

    val childMatcher = selector.appendSelector("child") { node.children }
    return SemanticsNodeInteraction(childMatcher)
}

/**
 * Returns all siblings of this node.
 *
 * Example: For the following tree
 * ```
 * |-A
 *   |-B1
 *   |-B2 <- this node
 *   |-B3
 * Returns B1, B3
 * ```
 */
fun SemanticsNodeInteraction.siblings(): List<SemanticsNodeInteraction> {
    val node = fetchSemanticsNode("Failed to retrieve siblings.")

    val siblingsMatcher = selector.appendSelector("siblings") { node.siblings }
    return siblingsMatcher.match(getAllSemanticsNodes()).map {
        SemanticsNodeInteraction(it, siblingsMatcher)
    }
}

/**
 * Returns exactly one sibling of this node.
 *
 * Use this only if this node has exactly one sibling.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 */
fun SemanticsNodeInteraction.sibling(): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve a sibling.")

    val siblingsMatcher = selector.appendSelector("sibling") { node.siblings }
    return SemanticsNodeInteraction(siblingsMatcher)
}

/**
 * Returns all the ancestors of this node.
 *
 * Example: For the following tree
 * ```
 * |-A
 *   |-B
 *     |-C <- this node
 * Returns B, A
 * ```
 */
fun SemanticsNodeInteraction.ancestors(): List<SemanticsNodeInteraction> {
    val node = fetchSemanticsNode("Failed to retrieve ancestors.")

    val ancestorsMatcher = selector.appendSelector("ancestors") { node.ancestors }
    return ancestorsMatcher.match(getAllSemanticsNodes()).map {
        SemanticsNodeInteraction(it, ancestorsMatcher)
    }
}