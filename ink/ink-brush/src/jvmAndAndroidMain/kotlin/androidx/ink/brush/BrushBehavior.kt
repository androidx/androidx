/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.RestrictTo
import androidx.ink.brush.behavior.Node
import androidx.ink.brush.behavior.TerminalNode
import androidx.ink.brush.behavior.ValueNode
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmOverloads

/**
 * A behavior describing how stroke input properties should affect the shape and color of the brush
 * tip.
 *
 * The behavior is conceptually a tree made from the various node types defined below. Each edge of
 * the tree graph represents passing a nullable finite floating point value (with "null"
 * representing an undefined value) from a node to its parent, and each node in the tree fits into
 * one of the following categories:
 * 1. Leaf nodes generate an output value without graph inputs. For example, they can create a value
 *    from properties of stroke input.
 * 2. Filter nodes can conditionally toggle branches of the graph "on" by outputting their input
 *    value, or "off" by outputting a null value.
 * 3. Operator nodes take in one or more input values and generate an output. For example, by
 *    mapping input to output with an easing function.
 * 4. Terminal nodes apply one or more input values to chosen properties of the brush tip.
 *
 * For each input in a stroke, [BrushTip.behaviors] are applied as follows:
 * 1. The actual target modifier (as calculated above) for each tip property is accumulated from
 *    every [BrushBehavior] present on the current [BrushTip]. Multiple behaviors can affect the
 *    same [Target]. Depending on the [Target], modifiers from multiple behaviors will stack either
 *    additively or multiplicatively, according to the documentation for that [Target]. Regardless,
 *    the order of specified behaviors does not affect the result.
 * 2. The modifiers are applied to the shape and color shift values of the tip's state according to
 *    the documentation for each [Target]. The resulting tip property values are then clamped or
 *    normalized to within their valid range of values. E.g. the final value of
 *    [BrushTip.cornerRounding] will be clamped within [0, 1]. Generally: The affected shape values
 *    are those found in [BrushTip] members. The color shift values remain in the range -100% to
 *    +100%. Note that when stored on a vertex, the color shift is encoded such that each channel is
 *    in the range [0, 1], where 0.5 represents a 0% shift.
 *
 * Note that the accumulated tip shape property modifiers may be adjusted by the implementation
 * before being applied: The rates of change of shape properties may be constrained to keep them
 * from changing too rapidly with respect to distance traveled from one input to the next.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkCustomBrushApi
// NotCloseable: Finalize is only used to free the native peer.
@Suppress("NotCloseable")
public class BrushBehavior
private constructor(
    /** A handle to the underlying native [BrushBehavior] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    // The [terminalNodes] val below is a defensive copy of this parameter.
    terminalNodes: List<TerminalNode>,
) {
    /** The targets that this [BrushBehavior] affects. */
    public val terminalNodes: List<TerminalNode> = unmodifiableList(terminalNodes.toList())

    /**
     * A multi-line, human-readable string with a description of this brush behavior and its purpose
     * within the brush, with the intended audience being designers/developers who are editing the
     * brush definition. This string is not generally intended to be displayed to end users.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public val developerComment: String = BrushBehaviorNative.getDeveloperComment(nativePointer)

    /** Constructs a [BrushBehavior] with a list of [TerminalNode]s. */
    @JvmOverloads
    public constructor(
        // The [terminalNodes] val above is a defensive copy of this parameter.
        terminalNodes: List<TerminalNode>,
        developerComment: String = "",
    ) : this(
        BrushBehaviorNative.createFromTerminalNodes(terminalNodes, developerComment),
        terminalNodes,
    )

    /** Constructs a [BrushBehavior] with a single [TerminalNode]. */
    @JvmOverloads
    public constructor(
        terminalNode: TerminalNode,
        developerComment: String = "",
    ) : this(listOf(terminalNode), developerComment)

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushBehavior) return false
        if (other === this) return true
        return terminalNodes == other.terminalNodes && developerComment == other.developerComment
    }

    override fun hashCode(): Int {
        var result = terminalNodes.hashCode()
        result = 31 * result + developerComment.hashCode()
        return result
    }

    override fun toString(): String =
        "BrushBehavior($terminalNodes, developerComment=$developerComment)"

    /** Delete native BrushBehavior memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushBehaviorNative.free(nativePointer)
    }

    public companion object {
        /**
         * Construct a [BrushBehavior] from an unowned heap-allocated native pointer to a C++
         * `BrushBehavior`. Kotlin wrapper objects nested under the [BrushBehavior] are initialized
         * similarly using their own [wrapNative] methods, passing those pointers to newly
         * copy-constructed heap-allocated objects. That avoids the need to call Kotlin constructors
         * for those objects from C++.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): BrushBehavior {
            val terminalNodes = mutableListOf<TerminalNode>()
            val inputStack = ArrayDeque<ValueNode>()
            for (i in 0 until BrushBehaviorNative.getNodeCount(unownedNativePointer)) {
                when (
                    val node =
                        Node.wrapNative(
                            BrushBehaviorNative.newCopyOfNode(unownedNativePointer, i),
                            inputStack,
                        )
                ) {
                    is TerminalNode -> terminalNodes.add(node)
                    is ValueNode -> inputStack.addLast(node)
                    else ->
                        throw IllegalArgumentException(
                            "Node must either be a TerminalNode or ValueNode: $node"
                        )
                }
            }
            return BrushBehavior(unownedNativePointer, terminalNodes)
        }
    }
}

/** Singleton wrapper for `BrushBehavior` native methods. */
@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object BrushBehaviorNative {
    init {
        NativeLoader.load()
    }

    fun createFromTerminalNodes(terminalNodes: List<TerminalNode>, developerComment: String): Long {
        val orderedNodes = ArrayDeque<Node>()
        val stack = ArrayDeque<Node>(terminalNodes)
        while (!stack.isEmpty()) {
            stack.removeLast().let { node ->
                orderedNodes.addFirst(node)
                stack.addAll(node.inputs)
            }
        }
        return createFromOrderedNodes(
            orderedNodes.map { it.nativePointer }.toLongArray(),
            developerComment = developerComment,
        )
    }

    /** Creates a new native `BrushBehavior` with the given ordered nodes. */
    @UsedByNative
    external fun createFromOrderedNodes(
        orderdNodeNativePointers: LongArray,
        developerComment: String,
    ): Long

    /** Release the underlying memory allocated in [createFromOrderedNodes]. */
    @UsedByNative external fun free(nativePointer: Long)

    /** Returns the number of `BrushBehavior::Node`s in the native `BrushBehavior`. */
    @UsedByNative external fun getNodeCount(nativePointer: Long): Int

    @UsedByNative external fun getDeveloperComment(nativePointer: Long): String

    /**
     * Returns an unowned native pointer to a new, stack-allocated copy of the native
     * `BrushBehavior::Node` at the given index in the pointed-at `BrushBehavior`.
     */
    @UsedByNative external fun newCopyOfNode(nativePointer: Long, index: Int): Long
}
