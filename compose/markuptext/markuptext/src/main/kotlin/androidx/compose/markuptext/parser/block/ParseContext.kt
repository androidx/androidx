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

import androidx.compose.markuptext.parser.LineScanner

/**
 * Maintains document-scoped and line-scoped state across initial and incremental parsing passes.
 *
 * ### Lifecycle
 * A single [ParseContext] instance is bound to a [DocumentParseBlock] for its entire lifetime and
 * must be reused across both the initial full parse and all subsequent incremental parses:
 * 1. Instantiate one [ParseContext] alongside a [DocumentParseBlock].
 * 2. For each line during the initial parse—or each re-parsed line following
 *    [ContainerParseBlock.invalidateLines] during an incremental parse—call [resetLine] with a new
 *    [LineScanner], then pass this context to [DocumentParseBlock.processLine].
 * 3. During incremental parsing, stop processing lines once [DocumentParseBlock.processLine]
 *    returns a [ParseResult] with [ParseResult.isConverged] set to `true`.
 *
 * ### State Scopes & Transitions
 * State is divided into two scopes:
 * * **Document-scoped state** (persists across initial and incremental parses):
 *     * Block ID counter ([generateBlockId]): Monotonically increments from `0L` for each created
 *       [ParseBlock]. Reusing the same [ParseContext] across incremental edits guarantees newly
 *       allocated blocks receive unique [ParseBlock.id]s that do not collide with retained blocks.
 *     * [linkReferences]: Accumulates document-wide link reference definitions.
 * * **Line-scoped state** (reset per line via [resetLine]):
 *     * [currentLine]: Replaced by [resetLine] at the start of each line. As the line descends the
 *       active [ContainerParseBlock] hierarchy, containers advance the [LineScanner] cursor past
 *       matched block prefixes and indentation.
 *     * [isLazyContinuation]: Reset to `false` by [resetLine]. Transitions monotonically from
 *       `false` to `true` via [startLazyContinuation] when an open [BaseContainerParseBlock] fails
 *       [BaseContainerParseBlock.matchLineStart]. Once `true`, it remains `true` for all descendant
 *       blocks on the current line until the next [resetLine] call.
 */
internal class ParseContext {
    private var idCounter = 0L

    /** Returns the next unique [ParseBlock.id] for this document's lifetime. */
    fun generateBlockId(): Long = idCounter++

    /** Active [LineScanner] for the line currently being parsed, updated via [resetLine]. */
    lateinit var currentLine: LineScanner
        internal set

    /**
     * Whether [currentLine] is being parsed as an unprefixed lazy continuation of an open container
     * block. Reset to `false` by [resetLine] and transitions monotonically to `true` via
     * [startLazyContinuation] as a line descends the block tree.
     */
    var isLazyContinuation: Boolean = false
        private set

    /**
     * Marks [currentLine] as a lazy continuation line for all descendant blocks on the current
     * line.
     */
    internal fun startLazyContinuation() {
        isLazyContinuation = true
    }

    /** Document-wide registry of link reference definitions (e.g., `[label]: url "title"`). */
    val linkReferences: MutableMap<String, String> = mutableMapOf()

    /**
     * Opens a new [ParseBlock] on [currentLine], falling back to [ParagraphParseBlock] for plain
     * text.
     */
    fun startBlock(): ParseBlock {
        return tryStartInterruptingBlock()
            ?: ParagraphParseBlock(
                id = generateBlockId(),
                headingLevel = 0,
                initialContent = currentLine.getRawUnconsumedText().trimStart().toString(),
                lineCount = 1,
            )
    }

    /**
     * Attempts to open a [ParseBlock] that can interrupt an open [ParagraphParseBlock] on
     * [currentLine], or returns `null` if none matches.
     */
    fun tryStartInterruptingBlock(): ParseBlock? {
        val scanner = currentLine

        // 1. Empty Line
        if (scanner.isEmptyLine()) {
            return EmptyLineParseBlock(id = generateBlockId(), lineCount = 1)
        }

        // 2. ATX Heading (# Heading)
        val atx = scanner.matchesAtxHeading()
        if (atx != null) {
            return ParagraphParseBlock(
                id = generateBlockId(),
                headingLevel = atx.level,
                initialContent = atx.text,
                lineCount = 1,
            )
        }

        return null
    }

    /**
     * Prepares line-scoped state for a new line by setting [currentLine] to [scanner] and resetting
     * [isLazyContinuation] to `false`.
     */
    fun resetLine(scanner: LineScanner) {
        this.currentLine = scanner
        this.isLazyContinuation = false
    }
}
