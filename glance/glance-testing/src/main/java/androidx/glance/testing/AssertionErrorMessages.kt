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
 * Builds error message for case where expected amount of matching nodes does not match reality.
 *
 * Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 */
internal fun buildErrorMessageForCountMismatch(
    errorMessage: String,
    matcherDescription: String,
    expectedCount: Int,
    actualCount: Int
): String {
    val sb = StringBuilder()

    sb.append(errorMessage)
    sb.append("\n")

    sb.append("Reason: ")
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
 * Builds error message for general assertion errors.
 *
 * <p>Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 */
internal fun <R> buildGeneralErrorMessage(
    errorMessage: String,
    glanceNode: GlanceNode<R>
): String {
    val sb = StringBuilder()
    sb.append(errorMessage)

    sb.append("\n")
    sb.append("Glance Node: ${glanceNode.toDebugString()}")

    return sb.toString()
}
