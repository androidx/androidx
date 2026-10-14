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
internal sealed class LeafParseBlock(id: Long, @IntRange(from = 1) override var lineCount: Int) :
    ParseBlock(id) {
    final override fun processLine(context: ParseContext): ParseResult {
        val result = parseLine(context)
        if (result.accepted) {
            this.lineCount++
        }
        return result
    }
}

/** Container block holding child blocks (BlockQuote, List, ListItem, Document). */
internal abstract class ContainerParseBlock(id: Long) : ParseBlock(id) {
    val children: MutableList<ParseBlock> = mutableListOf()

    /**
     * Number of lines from index 0 of this container that are currently verified and up-to-date.
     * During incremental re-parsing, [verifiedLineCount] increments by 1 for each accepted line,
     * and updates to [expectedConvergedLineCount] upon structural convergence.
     */
    @IntRange(from = 0) var verifiedLineCount: Int = 0

    /**
     * Index of the currently active (open) verified child in [children], or `-1` if no verified
     * child precedes the current parsing position.
     */
    @IntRange(from = -1) var activeChildIndex: Int = -1

    /**
     * Number of lines remaining in the new document between [verifiedLineCount] and the start line
     * of [unverifiedChild], or `-1` if there are no unverified children remaining.
     *
     * When [unverifiedChildLineDelta] reaches `0`, the parser is at the exact line where
     * [unverifiedChild] begins in the updated document and can check if a newly opened block
     * converges with [unverifiedChild].
     */
    @IntRange(from = -1) var unverifiedChildLineDelta: Int = -1

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
    @IntRange(from = -1) var expectedConvergedLineCount: Int = -1

    @get:IntRange(from = 0)
    override val lineCount: Int
        get() = verifiedLineCount

    private val activeChild: ParseBlock?
        get() = children.getOrNull(activeChildIndex)

    val unverifiedChildIndex: Int
        get() = activeChildIndex + 1

    val unverifiedChild: ParseBlock?
        get() = children.getOrNull(unverifiedChildIndex)

    /**
     * Delegates [context] to the currently active child block, if one exists. If there is no active
     * child, returns an unaccepted [ParseResult].
     */
    protected fun parseLineByChild(context: ParseContext): ParseResult {
        return activeChild?.processLine(context) ?: ParseResult.reject()
    }

    fun addChild(child: ParseBlock) {
        child.parent = this
        children.add(child)
        activeChildIndex = children.size - 1
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
        if (children.isEmpty()) {
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
        // child whose line range overlaps or starts at oldLineStart.
        while (
            childIndex < children.size &&
                childStartLine + children[childIndex].lineCount <= oldLineStart
        ) {
            childStartLine += children[childIndex].lineCount
            childIndex++
        }

        if (childIndex == children.size) {
            // The edit starts at or after the end of all existing children (e.g., appending at
            // EOF).
            // All existing children remain verified, and the last child remains active.
            activeChildIndex = children.lastIndex
            verifiedLineCount = childStartLine
            unverifiedChildLineDelta = -1
            expectedConvergedLineCount = -1
            return
        }

        val firstMatchChild = children[childIndex]
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
            children.removeAt(childIndex)
            verifiedLineCount = childStartLine
            activeChildIndex = childIndex - 1
        } else {
            childIndex++
        }

        // Advance childStartLine past the first impacted child in the original text.
        childStartLine += firstMatchOriginalLineCount

        // 3. Remove all subsequent children whose start lines fall within [oldLineStart,
        // oldLineEndExclusive).
        while (childIndex < children.size && childStartLine < oldLineEndExclusive) {
            val child = children[childIndex]
            childStartLine += child.lineCount
            children.removeAt(childIndex)
        }

        // 4. If any trailing unverified children remain after the edited range, compute how many
        // lines ahead the first unverified child starts in the updated text relative to
        // verifiedLineCount:
        // newUnverifiedStartLine = childStartLine + (newLineCount - oldLineCount)
        // unverifiedChildLineDelta = newUnverifiedStartLine - verifiedLineCount
        unverifiedChildLineDelta =
            if (childIndex < children.size) {
                childStartLine - verifiedLineCount + newLineCount - oldLineCount
            } else {
                -1
            }
    }

    private fun markConverged() {
        check(expectedConvergedLineCount >= 0) {
            "expectedConvergedLineCount must be non-negative on convergence (was $expectedConvergedLineCount)"
        }
        activeChildIndex = children.lastIndex
        verifiedLineCount = expectedConvergedLineCount
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
     *     - If a new child block opened on this line (`result.proposedChild` != null), it aligns
     *       with [unverifiedChild], adopts all remaining unverified children, and signals
     *       convergence.
     *     - If the current line was consumed by [activeChild] (`result.proposedChild` == null),
     *       discards the stale [unverifiedChild] and shifts [unverifiedChildLineDelta] to the next
     *       unverified sibling.
     * 5. Inserts any newly proposed child at [unverifiedChildIndex] and updates [verifiedLineCount]
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
            while (children.size > activeChildIndex + 1) {
                children.removeAt(children.size - 1)
            }
            activeChildIndex = children.lastIndex
            unverifiedChildLineDelta = -1
            expectedConvergedLineCount = -1
            return result
        }

        val childToAdd = result.proposedChild
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
                result.isConverged = true
                result.proposedChild = null
                return result
            } else {
                // Mismatch: activeChild consumed the line instead of opening a new block.
                // Discard `unverified` and advance delta by its lineCount to point to the next
                // unverified sibling.
                val oldUnverifiedLines = unverified.lineCount
                children.removeAt(unverifiedChildIndex)
                if (unverifiedChildIndex < children.size) {
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

        // 4. If a new child block was proposed to open on this line, insert it immediately after
        // activeChildIndex (before any remaining unverified children) and make it active.
        if (childToAdd != null) {
            childToAdd.parent = this
            if (children.isEmpty()) {
                children.add(childToAdd)
                activeChildIndex = 0
            } else {
                children.add(unverifiedChildIndex, childToAdd)
                activeChildIndex++
            }
            result.proposedChild = null
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
 * Abstract base container block providing the standard template method for [parseLine]:
 * 1. If [matchLineStart] matches, delegates to [parseLineByChild]. If the child accepts, returns
 *    the result; otherwise, accepts the proposed/default child if [canAcceptChild] permits.
 * 2. If [matchLineStart] does not match, delegates to [parseLineByChild] for lazy continuation if
 *    [supportsLazyContinuation] permits. If the child accepts, returns the result; otherwise,
 *    accepts the proposed child if [canAcceptChild] permits or rejects and bubbles up.
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
        val matchedStart = matchLineStart(context)
        if (!matchedStart) {
            context.startLazyContinuation()
        }
        val isLazyHere = context.isLazyContinuation
        if (matchedStart || supportsLazyContinuation) {
            val result = parseLineByChild(context)
            if (result.accepted) {
                return result
            }
            val proposed = result.proposedChild ?: context.startBlock()
            if (!isLazyHere && canAcceptChild(proposed)) {
                result.acceptChild(proposed)
            } else {
                result.rejectAndPropose(proposed)
            }
            return result
        }

        val proposed = context.startBlock()
        return ParseResult.rejectAndPropose(proposed)
    }
}
