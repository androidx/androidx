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

import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.unit.PxBounds

/**
 * Builds error message for case where expected amount of nodes does not match reality.
 *
 * Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 *
 * In case of only one node that went missing (was seen before) use
 * [buildErrorMessageForNodeMissingInTree] for better clarity.
 *
 * To see some examples, check out "ErrorMessagesTest".
 */
internal fun buildErrorMessageForCountMismatch(
    errorMessage: String,
    selector: SemanticsSelector?,
    foundNodes: List<SemanticsNode>,
    expectedCount: Int
): String {
    val sb = StringBuilder()

    sb.append(errorMessage)
    sb.append("\n")

    sb.append("Reason: ")
    if (expectedCount == 0) {
        sb.append("Did not expect any node")
    } else if (expectedCount == 1) {
        sb.append("Expected exactly '1' node")
    } else {
        sb.append("Expected '$expectedCount' nodes")
    }

    if (foundNodes.isEmpty()) {
        sb.append(" but could not find any")
    } else {
        sb.append(" but found '${foundNodes.size}'")
    }

    if (selector != null) {
        if (foundNodes.size <= 1) {
            sb.append(" node that satisfies: (${selector.description})")
        } else {
            sb.append(" nodes that satisfy: (${selector.description})")
        }
    } else {
        sb.append(".")
    }

    sb.appendln()

    if (foundNodes.isNotEmpty()) {
        if (foundNodes.size == 1) {
            sb.appendln("Node found:")
        } else {
            sb.appendln("Nodes found:")
        }
        sb.appendln(foundNodes.toStringInfo())
    }

    return sb.toString()
}

/**
 * Builds error message for case where node is no longer in the tree but is expected to be.
 *
 * Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 *
 * Note that [lastSeenSemantics] is the last semantics we have seen before we couldn't find the node
 * anymore. This can provide more info to the developer on what could have happened.
 *
 * To see some examples, check out "ErrorMessagesTest".
 */
internal fun buildErrorMessageForNodeMissingInTree(
    errorMessage: String,
    selector: SemanticsSelector,
    lastSeenSemantics: String
): String {
    val sb = StringBuilder()

    sb.append(errorMessage)
    sb.append("\n")

    sb.appendln("The node is no longer in the tree, last known semantics:")
    sb.appendln(lastSeenSemantics)
    sb.append("Original selector: ")
    sb.appendln(selector.description)

    return sb.toString()
}

internal fun buildErrorMessageForAssertAnyFail(
    selector: SemanticsSelector,
    nodes: List<SemanticsNode>,
    assertionMatcher: SemanticsMatcher
): String {
    val sb = StringBuilder()

    sb.appendln("Failed to assertAny(${assertionMatcher.description})")

    sb.appendln("None of the following nodes match:")
    sb.appendln(nodes.toStringInfo())

    sb.append("Selector used: '")
    sb.append(selector.description)
    sb.appendln("'")

    return sb.toString()
}

internal fun buildErrorMessageForAssertAllFail(
    selector: SemanticsSelector,
    nodesNotMatching: List<SemanticsNode>,
    assertionMatcher: SemanticsMatcher
): String {
    val sb = StringBuilder()

    sb.appendln("Failed to assertAll(${assertionMatcher.description})")

    sb.append("Found '${nodesNotMatching.size}' ")
    sb.append(if (nodesNotMatching.size == 1) "node" else "nodes")
    sb.appendln(" not matching:")
    sb.appendln(nodesNotMatching.toStringInfo())

    sb.append("Selector used: '")
    sb.append(selector.description)
    sb.appendln("'")

    return sb.toString()
}

internal fun buildErrorMessageForAtLeastOneNodeExpected(
    errorMessage: String,
    selector: SemanticsSelector
): String {
    val sb = StringBuilder()

    sb.appendln(errorMessage)

    sb.append("Assert needs to receive at least 1 node but 0 nodes were found for selector: ")
    sb.append("'")
    sb.append(selector.description)
    sb.appendln("'")

    return sb.toString()
}

internal fun buildGeneralErrorMessage(
    errorMessage: String,
    selector: SemanticsSelector,
    node: SemanticsNode
): String {
    val sb = StringBuilder()

    sb.appendln(errorMessage)

    sb.appendln("Semantics of the node:")
    sb.appendln(node.toStringInfo())

    sb.append("Selector used: (")
    sb.append(selector.description)
    sb.appendln(")")

    return sb.toString()
}

internal fun buildIndexErrorMessage(
    index: Int,
    selector: SemanticsSelector,
    nodes: List<SemanticsNode>
): String {
    val sb = StringBuilder()

    sb.append("Can't retrieve node at index '$index' of '")
    sb.append(selector.description)
    sb.appendln("'")

    if (nodes.isEmpty()) {
        sb.appendln("There are no existing nodes for that selector.")
    } else if (nodes.size == 1) {
        sb.appendln("There is 1 node only:")
        sb.appendln(nodes.toStringInfo())
    } else {
        sb.appendln("There are '${nodes.size}' nodes only:")
        sb.appendln(nodes.toStringInfo())
    }

    return sb.toString()
}

internal fun Collection<SemanticsNode>.toStringInfo(): String {
    var sb = StringBuilder()
    var i = 1
    forEach {
        if (size > 1) {
            sb.append(i)
            sb.append(") ")
        }
        sb.append(it.toStringInfo())
        if (i < size) {
            sb.appendln()
        }
        ++i
    }
    return sb.toString()
}

internal fun SemanticsNode.toStringInfo(): String {
    var sb = StringBuilder()
    sb.append("Id: $id, Position: ")
    sb.appendln(pxBoundsToShortString(globalBounds))
    sb.appendConfigInfo(config)
    return sb.toString()
}

private fun pxBoundsToShortString(bounds: PxBounds): String {
    return "LTRB(${bounds.left}.px, ${bounds.top}.px, ${bounds.right}.px, ${bounds.bottom}.px)"
}

private fun StringBuilder.appendConfigInfo(config: SemanticsConfiguration) {
    val prefix = "- "
    val separator = "\n"
    val startLength = length

    for ((key, value) in config) {
        append(prefix)
        append(key.name)
        append(" = '")
        append(value)
        append("'")
        append(separator)
    }

    if (config.isMergingSemanticsOfDescendants) {
        append(prefix)
        append("MergeDescendants = 'true'")
        append(separator)
    }

    // Remove last separator
    if (length > startLength) {
        setLength(length - separator.length)
    }
}