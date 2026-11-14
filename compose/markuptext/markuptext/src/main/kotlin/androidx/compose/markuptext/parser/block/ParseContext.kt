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
     *
     * A container block start consumes only its own prefix and returns an empty container. This
     * function then opens the container's first child on the rest of the line (and, if that child
     * is a container too, its first child, and so on), so block starts never build children.
     *
     * Incremental parsing relies on this function returning the same block for an unchanged line
     * (see [ContainerParseBlock.processLine]).
     */
    fun startBlock(): ParseBlock =
        checkNotNull(startBlock(isInterrupting = false)) {
            "A non-interrupting block start must always open a block (paragraph fallback)"
        }

    /**
     * Attempts to open a [ParseBlock] that interrupts an open [ParagraphParseBlock] on
     * [currentLine], or returns `null` (consuming nothing) if none matches.
     *
     * Only the returned block must be able to interrupt a paragraph. If it is a container, its
     * first child is opened on the rest of the line as in [startBlock], with a normal block start.
     */
    fun tryStartInterruptingBlock(): ParseBlock? = startBlock(isInterrupting = true)

    private fun startBlock(isInterrupting: Boolean): ParseBlock? {
        val block = matchBlockStart(isInterrupting) ?: return null
        if (block is ContainerParseBlock) {
            // The child is complete (with its own first child) before it is attached, because
            // initFirstChild takes the child's line count.
            block.initFirstChild(startBlock())
        }
        return block
    }

    /**
     * Matches the block that starts on [currentLine] and returns it without any children, or `null`
     * (consuming nothing) if [isInterrupting] and no block can interrupt a paragraph.
     */
    private fun matchBlockStart(isInterrupting: Boolean): ParseBlock? {
        val scanner = currentLine

        // 1. Empty Line
        if (scanner.isEmptyLine()) {
            return EmptyLineParseBlock(id = generateBlockId(), lineCount = 1)
        }

        // 2. Blocks starting with a marker character goes into expensive matchMarkedBlockStart.
        // Fast path: each of them begins, after at most [MAX_BLOCK_START_INDENT] spaces, with one
        // of a few marker  characters, so most text lines fail [mayStartMarkedBlock] can skip all
        // the matchers.
        if (scanner.mayStartMarkedBlock()) {
            val block = matchMarkedBlockStart()
            if (block != null) {
                return block
            }
        }

        if (isInterrupting) {
            return null
        }

        // 3. Indented Code Block (4+ leading spaces on a non-empty line; cannot interrupt a
        // paragraph)
        if (scanner.matchesIndentedCodeBlock()) {
            return IndentedCodeBlockParseBlock(
                id = generateBlockId(),
                initialCode = "${scanner.getRawUnconsumedText()}\n",
            )
        }

        // 4. Paragraph fallback
        return ParagraphParseBlock(
            id = generateBlockId(),
            headingLevel = 0,
            initialContent = scanner.getRawUnconsumedText().trimStart().toString(),
            lineCount = 1,
        )
    }

    /**
     * Matches a block that starts with a marker character (step 2 of [matchBlockStart]), or returns
     * `null` (consuming nothing) if none matches.
     */
    private fun matchMarkedBlockStart(): ParseBlock? {
        val scanner = currentLine

        // 2.1. ATX Heading (# Heading)
        val atx = scanner.matchesAtxHeading()
        if (atx != null) {
            return ParagraphParseBlock(
                id = generateBlockId(),
                headingLevel = atx.level,
                initialContent = atx.text,
                lineCount = 1,
            )
        }

        // 2.2. Fenced Code Block (``` or ~~~)
        val fenced = scanner.matchesFencedCodeBlock()
        if (fenced != null) {
            return FencedCodeBlockParseBlock(id = generateBlockId(), fence = fenced)
        }

        // 2.3. Thematic Break / Divider (---, ***, ___)
        if (scanner.matchesDivider()) {
            return DividerParseBlock(id = generateBlockId())
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

/**
 * Whether this line may start a block with a marker character: at most [MAX_BLOCK_START_INDENT]
 * leading spaces followed by a character that begins an ATX heading, fenced code block, or divider.
 * A cheap pre-check before running the full matchers.
 */
private fun LineScanner.mayStartMarkedBlock(): Boolean {
    if (leadingSpaceCount > MAX_BLOCK_START_INDENT) return false
    val c = firstNonSpaceChar
    return c == ATX_HEADING_MARKER || c.isFenceChar() || c.isDividerMarker()
}
