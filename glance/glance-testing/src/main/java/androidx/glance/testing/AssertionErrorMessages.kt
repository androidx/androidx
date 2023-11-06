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

package androidx.glance.testing

import java.lang.StringBuilder

/**
 * Builds error message with reason appended.
 *
 * @param errorMessageOnFail message explaining which operation you were about to perform. This
 *                           makes it easier for developer to find where the failure happened.
 * @param reason the reason for failure
 */
internal fun buildErrorMessageWithReason(errorMessageOnFail: String, reason: String): String {
    return "${errorMessageOnFail}\nReason: $reason"
}

/**
 * Builds error reason for case where amount of matching nodes are less than needed to query given
 * index and perform assertions on (e.g. if getting a node at index 2 but only 2 nodes exist in
 * the collection).
 */
internal fun buildErrorReasonForIndexOutOfMatchedNodeBounds(
    matcherDescription: String,
    requestedIndex: Int,
    actualCount: Int
): String {
    return "Not enough node(s) matching condition: ($matcherDescription) " +
        "to get node at index '$requestedIndex'. Found '$actualCount' matching node(s)"
}

/**
 * Builds error reason for case where expected amount of matching nodes does not match reality.
 */
internal fun buildErrorReasonForCountMismatch(
    matcherDescription: String,
    expectedCount: Int,
    actualCount: Int
): String {
    val sb = StringBuilder()

    when (expectedCount) {
        0 -> {
            sb.append("Did not expect any node matching condition: $matcherDescription")
        }

        else -> {
            sb.append("Expected '$expectedCount' node(s) matching condition: $matcherDescription")
        }
    }

    sb.append(", but found '$actualCount'")

    return sb.toString()
}

/**
 * Builds error reason for assertions where at least one node was expected to be present to make
 * assertions on (e.g. assertAny).
 */
internal fun buildErrorReasonForAtLeastOneNodeExpected(
    matcherDescription: String
): String {
    return "Expected to receive at least 1 node " +
        "but 0 nodes were found for condition: ($matcherDescription)"
}

/**
 * Builds error message for general assertion errors involving a single node.
 *
 * <p>Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 */
internal fun <R> buildGeneralErrorMessage(
    errorMessage: String,
    node: GlanceNode<R>
): String {
    val sb = StringBuilder()
    sb.append(errorMessage)

    sb.append("\n")
    sb.append("Glance Node: ${node.toDebugString()}")

    return sb.toString()
}

/**
 * Builds error message for general assertion errors for multiple nodes.
 *
 * <p>Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 */
internal fun <R> buildGeneralErrorMessage(
    errorMessage: String,
    nodes: List<GlanceNode<R>>
): String {
    val sb = StringBuilder()
    sb.append(errorMessage)

    sb.append("\n")
    sb.append("Found ${nodes.size} node(s) that don't match.")

    nodes.forEachIndexed { index, glanceNode ->
        sb.append("\nNon-matching Glance Node #${index + 1}: ${glanceNode.toDebugString()}")
    }

    return sb.toString()
}
