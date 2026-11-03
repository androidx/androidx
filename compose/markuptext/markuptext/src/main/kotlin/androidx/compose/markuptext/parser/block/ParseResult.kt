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

/**
 * Result of attempting to parse a line in a [ParseBlock], represented by a [status] code and an
 * optional associated [block].
 */
internal class ParseResult
private constructor(internal var status: Status, internal var block: ParseBlock? = null) {
    /** Outcome status of a single-line parse step. */
    internal enum class Status {
        /** The current line was accepted by an existing block ([block] is `null`). */
        Accept,

        /** The current line was accepted by a container, opening [block] as a new child. */
        AcceptChild,

        /** Incremental parsing converged with an unchanged sibling ([block] is `null`). */
        Converged,

        /**
         * The block closed without accepting the current line. If [block] is non-null, it is a
         * block already started on the current line, proposed to the ancestor container that will
         * hold it, so that the line is not matched again.
         */
        Reject,

        /**
         * The block gave up all `N` of its previously consumed lines to be replaced in-place by
         * [block] (with `lineCount == N`), which will then process the current line.
         *
         * This lets a block whose type can only be confirmed by later lines parse optimistically
         * and fall back when a later line disproves it, e.g. a link reference definition or a table
         * that turns out to be invalid falls back to a [ParagraphParseBlock] holding the same
         * lines.
         */
        ReplaceSelf,
    }

    /** True if the line was accepted and incorporated at or below this block. */
    val accepted: Boolean
        get() =
            status == Status.Accept || status == Status.AcceptChild || status == Status.Converged

    /**
     * True when re-parsing matches an unverified sibling, signaling to the incremental parser that
     * AST re-parsing has converged and should stop immediately.
     */
    val isConverged: Boolean
        get() = status == Status.Converged

    /** Updates this result in-place to indicate the line was accepted by this block. */
    fun accept() {
        this.status = Status.Accept
        this.block = null
    }

    /**
     * Updates this result in-place to indicate this container accepts and starts [child] under
     * itself.
     */
    fun acceptChild(child: ParseBlock) {
        this.status = Status.AcceptChild
        this.block = child
    }

    /** Updates this result in-place to indicate incremental parsing converged on this line. */
    fun markConverged() {
        this.status = Status.Converged
        this.block = null
    }

    /** Updates this result in-place to indicate this block closed without accepting the line. */
    fun reject() {
        this.status = Status.Reject
        this.block = null
    }

    /**
     * Updates this result in-place to indicate this block closed without accepting the line and
     * proposes [child], already started on the current line, to an ancestor container.
     */
    fun rejectAndPropose(child: ParseBlock?) {
        this.status = Status.Reject
        this.block = child
    }

    /**
     * Updates this result in-place to indicate this block gave up all of its previously consumed
     * lines to be replaced in-place by [replacement].
     */
    fun replaceSelf(replacement: ParseBlock) {
        this.status = Status.ReplaceSelf
        this.block = replacement
    }

    companion object {
        /** Creates a [ParseResult] indicating the line was accepted by this block. */
        fun accept(): ParseResult = ParseResult(Status.Accept)

        /**
         * Creates a [ParseResult] indicating this container accepts and starts [child] under
         * itself.
         */
        fun acceptChild(child: ParseBlock): ParseResult = ParseResult(Status.AcceptChild, child)

        /** Creates a [ParseResult] indicating this block closed without accepting the line. */
        fun reject(): ParseResult = ParseResult(Status.Reject)

        /**
         * Creates a [ParseResult] indicating this block closed without accepting the line and
         * proposes [child], already started on the current line, to an ancestor container.
         */
        fun rejectAndPropose(child: ParseBlock?): ParseResult = ParseResult(Status.Reject, child)

        /**
         * Creates a [ParseResult] indicating this block gave up all of its previously consumed
         * lines to be replaced in-place by [replacement].
         */
        fun replaceSelf(replacement: ParseBlock): ParseResult =
            ParseResult(Status.ReplaceSelf, replacement)
    }
}
