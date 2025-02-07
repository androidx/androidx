/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsNode

internal actual inline fun <R> wrapAssertionErrorsWithNodeInfo(
    selector: SemanticsSelector,
    node: SemanticsNode,
    block: () -> R
): R {
    try {
        return block()
    } catch (e: AssertionError) {
        throw ProxyAssertionError(e.message.orEmpty(), selector, node, e)
    }
}

internal class ProxyAssertionError(
    message: String,
    selector: SemanticsSelector,
    node: SemanticsNode,
    cause: Throwable
) : AssertionError(buildGeneralErrorMessage(message, selector, node), cause) {
    init {
        // Duplicate the stack trace to make troubleshooting easier.
        stackTrace = cause.stackTrace
    }
}
