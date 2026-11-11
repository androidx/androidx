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

import androidx.annotation.IntRange

/** Base mutable block used exclusively by the block parser. */
internal sealed class ParseBlock(
    /** Permanent unique identifier preserved across edits for UI stability. */
    val id: Long
) {
    var parent: ContainerParseBlock? = null

    /** Number of lines occupied by this block. */
    @get:IntRange(from = 0) abstract val lineCount: Int

    /**
     * Whether the blank lines at the end of this block ([countTrailingEmptyLines]) are part of its
     * content rather than a separator before the next block. For example, blank lines inside a
     * fenced code block that is closed by the end of its container.
     */
    open val consumesTrailingEmptyLines: Boolean
        get() = true

    /**
     * Counts the lines at the end of this block that are blank as seen by its parent, whether or
     * not they are this block's content.
     *
     * A line counts only if it is blank for the parent, not just after this block's own prefix. For
     * example, a `>` line is blank inside a block quote but is a quote line for the quote's parent,
     * so it does not count.
     *
     * Callers that need the blank lines separating this block from the next one (for example a list
     * deciding whether it is loose) must check [consumesTrailingEmptyLines] first, and treat the
     * count as `0` if it is `true`.
     */
    @IntRange(from = 0) open fun countTrailingEmptyLines(): Int = 0

    /**
     * Parses the current line according to this block's grammar rules. Overridden by subclasses to
     * perform syntax matching.
     *
     * @param context Active parsing context containing [ParseContext.currentLine] and
     *   [ParseContext.startBlock].
     * @return The mutable [ParseResult] indicating success/failure and any active/new block.
     */
    abstract fun parseLine(context: ParseContext): ParseResult

    /**
     * Orchestrates line processing for this block, including invariant bookkeeping, line counting,
     * convergence checks, and child pruning.
     */
    open fun processLine(context: ParseContext): ParseResult {
        return parseLine(context)
    }
}

/** Leaf block containing text content or atomic elements (Paragraph, EmptyLine). */
internal abstract class LeafParseBlock(id: Long, @IntRange(from = 1) lineCount: Int) :
    ParseBlock(id) {

    @get:IntRange(from = 1)
    final override var lineCount: Int = lineCount
        private set

    /** Number of consecutive blank lines this block accepted most recently. */
    @IntRange(from = 0) private var acceptedTrailingEmptyLineCount: Int = 0

    final override fun processLine(context: ParseContext): ParseResult {
        val isEmptyLine = context.currentLine.isEmptyLine()
        val result = parseLine(context)
        if (result.accepted) {
            this.lineCount++
            acceptedTrailingEmptyLineCount =
                if (isEmptyLine) acceptedTrailingEmptyLineCount + 1 else 0
        }
        return result
    }

    /**
     * Counts the blank lines this block accepted after its last non-blank line. The first line of a
     * leaf (the line that opened it) is never blank, except for [EmptyLineParseBlock], which
     * overrides this.
     */
    override fun countTrailingEmptyLines(): Int = acceptedTrailingEmptyLineCount
}

/**
 * Container block holding child blocks (BlockQuote, List, ListItem, Document).
 *
 * This class is the incremental parsing engine: child bookkeeping, invalidation and convergence. It
 * is sealed so that [BaseContainerParseBlock] is its only direct subclass: every container parses
 * its lines through the same template, which keeps per-line state such as
 * [ParseContext.isLazyContinuation] consistent.
 */
internal sealed class ContainerParseBlock(id: Long) : ParseBlock(id) {
    private val _children: MutableList<ParseBlock> = mutableListOf()

    /**
     * Read-only view of the child blocks parsed so far (up to and including the active child).
     *
     * During incremental re-parsing, unverified children after the active child are not part of
     * this list, so [children] always looks exactly like it would in a full parse up to the current
     * line. This is a live view of the backing list: no children are copied.
     */
    val children: List<ParseBlock> =
        object : AbstractList<ParseBlock>() {
            override val size: Int
                get() = activeChildIndex + 1

            override fun get(index: Int): ParseBlock {
                if (index !in 0..activeChildIndex) {
                    throw IndexOutOfBoundsException("Index $index out of bounds for size $size")
                }
                return _children[index]
            }
        }

    /**
     * Number of lines from index 0 of this container that are currently verified and up-to-date.
     * During incremental re-parsing, [verifiedLineCount] increments by 1 for each accepted line,
     * and updates to [expectedConvergedLineCount] upon structural convergence.
     */
    @IntRange(from = 0) private var verifiedLineCount: Int = 0

    /**
     * Index of the currently active (open) verified child in [children], or `-1` if no verified
     * child precedes the current parsing position.
     */
    @IntRange(from = -1) private var activeChildIndex: Int = -1

    /**
     * Number of lines remaining in the new document between [verifiedLineCount] and the start line
     * of [unverifiedChild], or `-1` if there are no unverified children remaining.
     *
     * When [unverifiedChildLineDelta] reaches `0`, the parser is at the exact line where
     * [unverifiedChild] begins in the updated document and can check if a newly opened block
     * converges with [unverifiedChild].
     */
    @IntRange(from = -1) private var unverifiedChildLineDelta: Int = -1

    /**
     * Precomputed total verified line count of this container upon structural convergence, or `-1`
     * if convergence is impossible (when the edit reaches or extends past the end of this
     * container).
     *
     * Performance optimization: without precomputing this value in [invalidateLines], the converged
     * line count would need to be recomputed in `O(children.size)` by summing child line counts
     * upon convergence.
     *
     * This value is only used if convergence actually happens in this block (via [markConverged]).
     * If convergence does not happen in this block—for example:
     * 1. This block closes within the edited range (`!result.accepted`), or
     * 2. It does not converge through the last child (either the last child was removed during
     *    [invalidateLines], or all remaining unverified children were discarded without converging)
     *
     * then this value is never used.
     */
    @IntRange(from = -1) private var expectedConvergedLineCount: Int = -1

    @get:IntRange(from = 0)
    override val lineCount: Int
        get() = verifiedLineCount

    private val activeChild: ParseBlock?
        get() = _children.getOrNull(activeChildIndex)

    private val unverifiedChildIndex: Int
        get() = activeChildIndex + 1

    private val unverifiedChild: ParseBlock?
        get() = _children.getOrNull(unverifiedChildIndex)

    /**
     * Delegates [context] to the currently active child block, if one exists. If there is no active
     * child, returns an unaccepted [ParseResult].
     *
     * If the child does not accept the line, the [ParseContext.currentLine] cursor is restored to
     * where the child received it, so this container (or an ancestor) can open a new block on the
     * whole line if no block was proposed.
     */
    protected fun parseLineByChild(context: ParseContext): ParseResult {
        val child = activeChild ?: return ParseResult.reject()
        val childStartPos = context.currentLine.position
        val oldChildLineCount = child.lineCount
        var result = child.processLine(context)

        if (result.status == ParseResult.Status.ReplaceSelf) {
            val replacement =
                checkNotNull(result.block) { "ReplaceSelf result must carry a replacement block" }
            check(replacement.lineCount == oldChildLineCount) {
                "Replacement block must have lineCount == $oldChildLineCount, was ${replacement.lineCount}"
            }
            replacement.parent = this
            _children[activeChildIndex] = replacement
            context.currentLine.restore(childStartPos)
            result = replacement.processLine(context)
            // A block must replace itself with the final block in one step. Chained replacements
            // re-parse the same line repeatedly and could loop forever.
            check(result.status != ParseResult.Status.ReplaceSelf) {
                "Replacement block ${replacement::class.simpleName} must not replace itself again"
            }
        }

        if (!result.accepted) {
            context.currentLine.restore(childStartPos)
        }
        return result
    }

    /**
     * Sets [child] as the first and active child of this newly started, still empty container, and
     * takes its lines as this container's line count. [child] must already hold its first line (a
     * container child must have its own first child).
     *
     * Block starts must not call this: a block start returns an empty container, and
     * [ParseContext.startBlock] opens the first child on the rest of the line. Later children are
     * added by [processLine].
     */
    fun initFirstChild(child: ParseBlock) {
        check(_children.isEmpty() && verifiedLineCount == 0) {
            "initFirstChild called on a container that is not empty"
        }
        check(child.lineCount > 0) { "The first child must hold its first line" }
        child.parent = this
        _children.add(child)
        activeChildIndex = 0
        verifiedLineCount = child.lineCount
    }

    /**
     * Prepares this container for incremental re-parsing after a text edit replaces lines
     * `[oldLineStart, oldLineEndExclusive)` with [newLineCount] lines.
     *
     * This method partitions [children] into:
     * 1. Verified children before [oldLineStart] (ending at [activeChildIndex]).
     * 2. Invalidated children overlapping `[oldLineStart, oldLineEndExclusive)` (removed or
     *    recursively invalidated).
     * 3. Trailing unverified children starting at [unverifiedChildIndex], whose start line in the
     *    updated text is `verifiedLineCount + unverifiedChildLineDelta` lines from the start of
     *    this container.
     *
     * @param oldLineStart 0-based inclusive line index where the affected range begins in the
     *   original text relative to this container.
     * @param oldLineEndExclusive 0-based exclusive line index where the affected range ends in the
     *   original text relative to this container.
     * @param newLineCount Number of lines occupied by the replacement in the updated text.
     */
    fun invalidateLines(
        @IntRange(from = 0) oldLineStart: Int,
        @IntRange(from = 0) oldLineEndExclusive: Int,
        @IntRange(from = 0) newLineCount: Int,
    ) {
        if (_children.isEmpty()) {
            activeChildIndex = -1
            verifiedLineCount = 0
            unverifiedChildLineDelta = -1
            expectedConvergedLineCount = -1
            return
        }
        val oldLineCount = oldLineEndExclusive - oldLineStart
        // Precompute the converged line count to avoid O(children.size) summation on convergence.
        // Only used if convergence actually happens in this block; never used if the block closes
        // in the edited range or fails to converge through the last child.
        expectedConvergedLineCount =
            if (oldLineEndExclusive < verifiedLineCount) {
                verifiedLineCount + newLineCount - oldLineCount
            } else {
                -1
            }

        var childIndex = 0
        var childStartLine = 0

        // 1. Scan verified children from the beginning of this container until we find the first
        // child whose line range overlaps or starts at oldLineStart. A child that ends exactly at
        // oldLineStart stays verified and becomes the active child, so the edited line is offered
        // to it first, exactly as in a full parse.
        while (childIndex < _children.size) {
            val child = _children[childIndex]
            val childEndLine = childStartLine + child.lineCount
            if (childEndLine > oldLineStart) {
                break
            }
            childStartLine = childEndLine
            childIndex++
        }

        if (childIndex == _children.size) {
            // The edit starts at or after the end of all existing children (e.g., appending at
            // EOF).
            // All existing children remain verified, and the last child remains active.
            activeChildIndex = _children.lastIndex
            verifiedLineCount = childStartLine
            unverifiedChildLineDelta = -1
            expectedConvergedLineCount = -1
            return
        }

        val firstMatchChild = _children[childIndex]
        val firstMatchOriginalLineCount = firstMatchChild.lineCount
        val firstMatchRemoved: Boolean

        // 2. Handle the first child impacted by the edit:
        // - If it's a ContainerParseBlock, recursively invalidate the relative line range inside
        //   it. If its verifiedLineCount drops to 0 (its opening line was invalidated), mark it for
        //   removal; otherwise, retain it as the activeChild so re-parsing resumes inside it.
        // - Else (LeafParseBlock), a leaf cannot be partially retained; mark it for removal.
        if (firstMatchChild is ContainerParseBlock) {
            firstMatchChild.invalidateLines(
                oldLineStart = oldLineStart - childStartLine,
                oldLineEndExclusive = oldLineEndExclusive - childStartLine,
                newLineCount = newLineCount,
            )
            if (firstMatchChild.verifiedLineCount == 0) {
                firstMatchRemoved = true
            } else {
                activeChildIndex = childIndex
                verifiedLineCount = childStartLine + firstMatchChild.verifiedLineCount
                firstMatchRemoved = false
            }
        } else {
            firstMatchRemoved = true
        }

        if (firstMatchRemoved) {
            _children.removeAt(childIndex)
            verifiedLineCount = childStartLine
            activeChildIndex = childIndex - 1
        } else {
            childIndex++
        }

        // Advance childStartLine past the first impacted child in the original text.
        childStartLine += firstMatchOriginalLineCount

        // 3. Remove all subsequent children whose start lines fall within [oldLineStart,
        // oldLineEndExclusive).
        while (childIndex < _children.size && childStartLine < oldLineEndExclusive) {
            val child = _children[childIndex]
            childStartLine += child.lineCount
            _children.removeAt(childIndex)
        }

        // 4. If any trailing unverified children remain after the edited range, compute how many
        // lines ahead the first unverified child starts in the updated text relative to
        // verifiedLineCount:
        // newUnverifiedStartLine = childStartLine + (newLineCount - oldLineCount)
        // unverifiedChildLineDelta = newUnverifiedStartLine - verifiedLineCount
        unverifiedChildLineDelta =
            if (childIndex < _children.size) {
                childStartLine - verifiedLineCount + newLineCount - oldLineCount
            } else {
                -1
            }
    }

    private fun markConverged() {
        val convergedLineCount = expectedConvergedLineCount
        check(convergedLineCount >= 0) {
            "expectedConvergedLineCount must be non-negative on convergence (was $convergedLineCount)"
        }
        activeChildIndex = _children.lastIndex
        verifiedLineCount = convergedLineCount
        unverifiedChildLineDelta = -1
        expectedConvergedLineCount = -1
    }

    /**
     * Orchestrates incremental line parsing for this container:
     * 1. Delegates line parsing via [parseLine] (which routes to [activeChild] or starts a new
     *    block).
     * 2. Propagates early exit if a descendant container already converged
     *    ([ParseResult.isConverged]).
     * 3. Prunes any trailing unverified children if this container rejected the line and closed.
     * 4. Checks for convergence when [unverifiedChildLineDelta] reaches `0`:
     *     - If a new child block opened on this line ([ParseResult.Status.AcceptChild]), it aligns
     *       with [unverifiedChild], adopts all remaining unverified children, and signals
     *       convergence.
     *     - If the current line was consumed by [activeChild] ([ParseResult.Status.Accept]),
     *       discards the stale [unverifiedChild] and shifts [unverifiedChildLineDelta] to the next
     *       unverified sibling.
     * 5. Inserts any newly opened child at [unverifiedChildIndex] and updates [verifiedLineCount]
     *    and [unverifiedChildLineDelta].
     */
    final override fun processLine(context: ParseContext): ParseResult {
        val result = parseLine(context)

        // 1. If a descendant container already converged on this line, mark this container
        // converged and bubble up immediately.
        if (result.isConverged) {
            markConverged()
            return result
        }

        // 2. If this container rejected the line, the container is now closed. Any remaining
        // unverified children after activeChildIndex belonged to the old extent of this container
        // and must be pruned.
        if (!result.accepted) {
            while (_children.size > activeChildIndex + 1) {
                _children.removeAt(_children.size - 1)
            }
            activeChildIndex = _children.lastIndex
            unverifiedChildLineDelta = -1
            expectedConvergedLineCount = -1
            return result
        }

        val childToAdd = if (result.status == ParseResult.Status.AcceptChild) result.block else null
        val unverified = unverifiedChild

        // 3. Convergence check: when unverifiedChildLineDelta == 0, the current line aligns with
        // the start line of `unverified` in the updated document (`result.accepted` is already
        // guaranteed `true` here since `!result.accepted` returned early in Step 2).
        if (unverifiedChildLineDelta == 0 && unverified != null) {
            if (childToAdd != null) {
                // Structural match! A new block is created on an unchanged line under the same
                // parent block, so it can only be identical to `unverified` (`activeChild` already
                // closed and `context.startBlock()` is deterministic on this line; comparing
                // multi-line properties like Setext `headingLevel` would also be wrong on the first
                // line). Adopt all remaining unverified children as verified and stop re-parsing.
                markConverged()
                result.markConverged()
                return result
            } else {
                // Mismatch: activeChild consumed the line instead of opening a new block.
                // Discard `unverified` and advance delta by its lineCount to point to the next
                // unverified sibling.
                val oldUnverifiedLines = unverified.lineCount
                _children.removeAt(unverifiedChildIndex)
                if (unverifiedChildIndex < _children.size) {
                    // A subsequent unverified sibling shifted into unverifiedChildIndex; advance
                    // delta by the removed block's lineCount so delta measures lines to this next
                    // sibling.
                    unverifiedChildLineDelta += oldUnverifiedLines
                } else {
                    // No further unverified children remain in this container to converge with.
                    unverifiedChildLineDelta = -1
                    expectedConvergedLineCount = -1
                }
            }
        }

        // 4. If a new child block was opened on this line, insert it immediately after
        // activeChildIndex (before any remaining unverified children) and make it active.
        if (childToAdd != null) {
            childToAdd.parent = this
            if (_children.isEmpty()) {
                _children.add(childToAdd)
                activeChildIndex = 0
            } else {
                _children.add(unverifiedChildIndex, childToAdd)
                activeChildIndex++
            }
            result.accept()
        }

        // 5. Record that one more line of this container has been verified, closing the gap toward
        // the next unverified child by 1 line.
        this.verifiedLineCount++
        if (unverifiedChild != null) {
            this.unverifiedChildLineDelta--
        }

        return result
    }
}

/**
 * Base class of every container block (the only direct subclass of [ContainerParseBlock]),
 * providing the standard template method for [parseLine]:
 * 1. If [matchLineStart] matches, the line belongs to this container: it is delegated to the active
 *    child, and if the child does not accept it, this container opens a new child on the line if
 *    [canAcceptChild] permits.
 * 2. Otherwise, the line can only be a lazy continuation: it is delegated to the active child if
 *    [supportsLazyContinuation] permits, and rejected (bubbling up) if not accepted.
 */
internal abstract class BaseContainerParseBlock(id: Long) : ContainerParseBlock(id) {

    /**
     * Returns true if the current line matches and consumes this container's required
     * prefix/indent.
     */
    protected abstract fun matchLineStart(context: ParseContext): Boolean

    /** Returns true if [child] is allowed as a direct child of this container. */
    protected abstract fun canAcceptChild(child: ParseBlock): Boolean

    /** Whether this container supports lazy continuation when [matchLineStart] fails. */
    protected abstract val supportsLazyContinuation: Boolean

    final override fun parseLine(context: ParseContext): ParseResult {
        // Once an ancestor failed to match its line start, the line is already a lazy continuation,
        // so this container must not try to match (and consume) its own line start either.
        return if (!context.isLazyContinuation && matchLineStart(context)) {
            parseMatchedLine(context)
        } else {
            parseLazyContinuationLine(context)
        }
    }

    /**
     * Parses a line whose start matched (and consumed) this container's prefix: the line belongs to
     * this container. If the active child does not accept it, this container opens a new child on
     * the line (the block proposed by the child, or a new one from [ParseContext.startBlock]) if
     * [canAcceptChild] permits, and otherwise proposes the block to its ancestors.
     */
    private fun parseMatchedLine(context: ParseContext): ParseResult {
        val result = parseLineByChild(context)
        if (result.accepted) {
            return result
        }
        val child = result.block ?: context.startBlock()
        if (canAcceptChild(child)) {
            result.acceptChild(child)
        } else {
            result.rejectAndPropose(child)
        }
        return result
    }

    /**
     * Parses a line whose start did not match this container's prefix. Such a line can only be a
     * lazy continuation of an open paragraph inside this container, so it is passed to the active
     * child if [supportsLazyContinuation] permits, and this container never opens a new child for
     * it. If the line is not accepted, the result (including any block proposed by the child)
     * bubbles up so an ancestor whose prefix matched can open it.
     */
    private fun parseLazyContinuationLine(context: ParseContext): ParseResult {
        if (!supportsLazyContinuation) {
            return ParseResult.reject()
        }
        context.startLazyContinuation()
        return parseLineByChild(context)
    }
}
