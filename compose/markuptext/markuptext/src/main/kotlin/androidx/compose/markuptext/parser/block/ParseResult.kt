/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.markuptext.parser.block

/** Result of attempting to parse a line in a [ParseBlock]. */
internal class ParseResult
private constructor(
    /**
     * True if the line was accepted and incorporated at or below this block. False if this block
     * closed and did NOT accept the line.
     */
    internal var accepted: Boolean = false,

    /**
     * If non-null:
     * - When [accepted] == true: a proposed child accepted and started by THIS container.
     * - When [accepted] == false: a proposed child that must bubble up to an ancestor.
     */
    internal var proposedChild: ParseBlock? = null,

    /**
     * Flag set by [ContainerParseBlock] when re-parsing matches the unverified sibling, signaling
     * to the incremental parser that AST re-parsing has converged and should stop immediately.
     */
    internal var isConverged: Boolean = false,
) {
    /** Updates this result in-place to indicate the line was accepted by this block. */
    fun accept() {
        this.accepted = true
        this.proposedChild = null
    }

    /**
     * Updates this result in-place to indicate this container accepts and starts [child] under
     * itself.
     */
    fun acceptChild(child: ParseBlock) {
        this.accepted = true
        this.proposedChild = child
    }

    /**
     * Updates this result in-place to indicate this block closed without accepting the line and
     * proposes [child] to be accepted by an ancestor.
     */
    fun rejectAndPropose(child: ParseBlock?) {
        this.accepted = false
        this.proposedChild = child
    }

    companion object {
        /** Creates a [ParseResult] indicating the line was accepted by this block. */
        fun accept(): ParseResult = ParseResult(accepted = true)

        /**
         * Creates a [ParseResult] indicating this container accepts and starts [child] under
         * itself.
         */
        fun acceptChild(child: ParseBlock): ParseResult =
            ParseResult(accepted = true, proposedChild = child)

        /**
         * Creates a [ParseResult] indicating this block closed without accepting the line and
         * without proposing a child.
         */
        fun reject(): ParseResult = ParseResult(accepted = false)

        /**
         * Creates a [ParseResult] indicating this block closed without accepting the line and
         * proposes [child] to be accepted by an ancestor.
         */
        fun rejectAndPropose(child: ParseBlock?): ParseResult =
            ParseResult(accepted = false, proposedChild = child)
    }
}
