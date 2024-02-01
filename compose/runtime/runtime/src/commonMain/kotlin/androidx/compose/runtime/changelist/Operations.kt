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

package androidx.compose.runtime.changelist

import androidx.compose.runtime.Applier
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.changelist.Operation.IntParameter
import androidx.compose.runtime.changelist.Operation.ObjectParameter
import androidx.compose.runtime.checkPrecondition
import androidx.compose.runtime.requirePrecondition
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * `Operations` is a data structure used to store a sequence of [Operations][Operation] and their
 * respective arguments. Although the Stack is written to as a last-in-first-out structure, it is
 * iterated in a first-in-first-out structure. This makes the structure behave somewhat like a
 * specialized dequeue.
 *
 * `Operations` is backed by three backing arrays: one for the operation sequence, the `int`
 * arguments, and the object arguments. This helps reduce allocations as much as possible.
 *
 * `Operations` is not a thread safe data structure.
 */
internal class Operations : OperationsDebugStringFormattable() {

    private var opCodes = arrayOfNulls<Operation>(InitialCapacity)
    private var opCodesSize = 0

    private var intArgs = IntArray(InitialCapacity)
    private var intArgsSize = 0

    private var objectArgs = arrayOfNulls<Any>(InitialCapacity)
    private var objectArgsSize = 0

    /*
        The two masks below are used to track which arguments have been assigned for the most
        recently pushed operation. When an argument is set, its corresponding bit is set to 1.
        The bit indices correspond to the parameter's offset value. Offset 0 corresponds to the
        least significant bit, so a parameter with offset 2 will correspond to the mask 0b100.
     */
    private var pushedIntMask = 0b0
    private var pushedObjectMask = 0b0

    /**
     * Returns the number of pending operations contained in this operation stack.
     */
    val size: Int
        get() = opCodesSize

    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    /**
     * Resets the collection to its initial state, clearing all stored operations and arguments.
     */
    fun clear() {
        // We don't technically need to clear the opCodes or intArgs arrays, because we ensure
        // that every operation that gets pushed to this data structure has all of its arguments
        // set exactly once. This guarantees that they'll overwrite any stale, dirty values from
        // previous entries on the stack, so we shouldn't ever run into problems of having
        // uninitialized values causing undefined behavior for other operations.
        opCodesSize = 0
        intArgsSize = 0
        // Clear the object arguments array to prevent leaking memory
        objectArgs.fill(null, fromIndex = 0, toIndex = objectArgsSize)
        objectArgsSize = 0
    }

    /**
     * Pushes [operation] to the stack, ensures that there is space in the backing argument
     * arrays to store the parameters, and increments the internal pointers to track the
     * operation's arguments.
     *
     * It is expected that the arguments of this operation will be added after [pushOp]
     * returns. The index to write a parameter is `intArgsSize - operation.ints + arg.offset`
     * for int arguments, and `objectArgsSize - operation.objects + arg.offset` for
     * object arguments.
     *
     * Do not use this API outside of the [Operations] class directly. Use [push] instead.
     * This function is kept visible so that it may be inlined.
     */
    @InternalComposeApi
    fun pushOp(operation: Operation) {
        pushedIntMask = 0b0
        pushedObjectMask = 0b0

        // Resize arrays if needed
        if (opCodesSize == opCodes.size) {
            val resizeAmount = opCodesSize.coerceAtMost(MaxResizeAmount)
            opCodes = opCodes.copyOf(opCodesSize + resizeAmount)
        }
        ensureIntArgsSizeAtLeast(intArgsSize + operation.ints)
        ensureObjectArgsSizeAtLeast(objectArgsSize + operation.objects)

        // Record operation, advance argument pointers
        opCodes[opCodesSize++] = operation
        intArgsSize += operation.ints
        objectArgsSize += operation.objects
    }

    private fun determineNewSize(currentSize: Int, requiredSize: Int): Int {
        val resizeAmount = currentSize.coerceAtMost(MaxResizeAmount)
        return (currentSize + resizeAmount).coerceAtLeast(requiredSize)
    }

    private fun ensureIntArgsSizeAtLeast(requiredSize: Int) {
        val currentSize = intArgs.size
        if (requiredSize > currentSize) {
            intArgs = intArgs.copyOf(determineNewSize(currentSize, requiredSize))
        }
    }

    private fun ensureObjectArgsSizeAtLeast(requiredSize: Int) {
        val currentSize = objectArgs.size
        if (requiredSize > currentSize) {
            objectArgs = objectArgs.copyOf(determineNewSize(currentSize, requiredSize))
        }
    }

    /**
     * Adds an [operation] to the stack with no arguments.
     *
     * If [operation] defines any arguments, you must use the overload that accepts an `args`
     * lambda to provide those arguments. This function will throw an exception if the operation
     * defines any arguments.
     */
    fun push(operation: Operation) {
        requirePrecondition(operation.ints == 0 && operation.objects == 0) {
            "Cannot push $operation without arguments because it expects " +
                "${operation.ints} ints and ${operation.objects} objects."
        }
        @OptIn(InternalComposeApi::class)
        pushOp(operation)
    }

    /**
     * Adds an [operation] to the stack with arguments. To set arguments on the operation, call
     * [WriteScope.setObject] and [WriteScope.setInt] inside of the [args] lambda.
     *
     * The [args] lambda is called exactly once inline. You must set all arguments defined on
     * the [operation] exactly once. An exception is thrown if you attempt to call
     * [WriteScope.setInt] or [WriteScope.setObject] on an argument you have already set, and
     * when [args] returns if not all arguments were set.
     */
    @Suppress("BanInlineOptIn")
    @OptIn(ExperimentalContracts::class)
    inline fun push(operation: Operation, args: WriteScope.() -> Unit) {
        contract { callsInPlace(args, EXACTLY_ONCE) }

        @OptIn(InternalComposeApi::class)
        pushOp(operation)
        WriteScope(this).args()

        // Verify all arguments were written to.
        checkPrecondition(
            pushedIntMask == createExpectedArgMask(operation.ints) &&
                pushedObjectMask == createExpectedArgMask(operation.objects)
        ) {
            var missingIntCount = 0
            val missingInts = buildString {
                repeat(operation.ints) { arg ->
                    if ((0b1 shl arg) and pushedIntMask != 0b0) {
                        if (missingIntCount > 0) append(", ")
                        append(operation.intParamName(IntParameter(arg)))
                        missingIntCount++
                    }
                }
            }

            var missingObjectCount = 0
            val missingObjects = buildString {
                repeat(operation.objects) { arg ->
                    if ((0b1 shl arg) and pushedObjectMask != 0b0) {
                        if (missingIntCount > 0) append(", ")
                        append(operation.objectParamName(ObjectParameter<Nothing>(arg)))
                        missingObjectCount++
                    }
                }
            }

            "Error while pushing $operation. Not all arguments were provided. " +
                "Missing $missingIntCount int arguments ($missingInts) " +
                "and $missingObjectCount object arguments ($missingObjects)."
        }
    }

    /**
     * Returns a bitmask int where the bottommost [paramCount] bits are 1's, and the rest of the
     * bits are 0's. This corresponds to what [pushedIntMask] and [pushedObjectMask] will equal
     * if all [paramCount] arguments are set for the most recently pushed operation.
     */
    private fun createExpectedArgMask(paramCount: Int): Int {
        // Calling ushr(32) no-ops instead of returning 0, so add a special case if paramCount is 0
        return if (paramCount == 0) 0 else 0b0.inv().ushr(Int.SIZE_BITS - paramCount)
    }

    /**
     * Removes the most recently added operation and all of its arguments from the stack, clearing
     * references.
     */
    fun pop() {
        if (isEmpty()) {
            throw NoSuchElementException("Cannot pop(), because the stack is empty.")
        }
        val op = opCodes[--opCodesSize]!!
        opCodes[opCodesSize] = null

        repeat(op.objects) {
            objectArgs[--objectArgsSize] = null
        }

        repeat(op.ints) {
            intArgs[--intArgsSize] = 0
        }
    }

    /**
     * Removes the most recently added operation and all of its arguments from this stack, pushing
     * them into the [other] stack, and then clearing their references in this stack.
     */
    @OptIn(InternalComposeApi::class)
    fun popInto(other: Operations) {
        if (isEmpty()) {
            throw NoSuchElementException("Cannot pop(), because the stack is empty.")
        }
        val op = opCodes[--opCodesSize]!!
        opCodes[opCodesSize] = null

        other.pushOp(op)

        var thisObjIdx = objectArgsSize
        var otherObjIdx = other.objectArgsSize
        repeat(op.objects) {
            otherObjIdx--
            thisObjIdx--
            other.objectArgs[otherObjIdx] = objectArgs[thisObjIdx]
            objectArgs[thisObjIdx] = null
        }

        var thisIntIdx = intArgsSize
        var otherIntIdx = other.intArgsSize
        repeat(op.ints) {
            otherIntIdx--
            thisIntIdx--
            other.intArgs[otherIntIdx] = intArgs[thisIntIdx]
            intArgs[thisIntIdx] = 0
        }

        objectArgsSize -= op.objects
        intArgsSize -= op.ints
    }

    /**
     * Iterates through the stack in the order that items were added, calling [sink] for each
     * operation in the stack.
     *
     * Iteration moves from oldest elements to newest (more like a queue than a stack). [drain] is
     * a destructive operation that also clears the items in the stack, and is used to apply all
     * of the operations in the stack, since they must be applied in the order they were added
     * instead of being popped.
     */
    inline fun drain(
        sink: OpIterator.() -> Unit
    ) {
        forEach(sink)
        clear()
    }

    /**
     * Iterates through the stack, calling [action] for each operation in the stack. Iteration
     * moves from oldest elements to newest (more like a queue than a stack).
     */
    inline fun forEach(
        action: OpIterator.() -> Unit
    ) {
        if (isNotEmpty()) {
            val iterator = OpIterator()
            do {
                iterator.action()
            } while (iterator.next())
        }
    }

    fun executeAndFlushAllPendingOperations(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager
    ) {
        drain {
            with(operation) {
                execute(
                    applier = applier,
                    slots = slots,
                    rememberManager = rememberManager
                )
            }
        }
    }

    private fun String.indent() = "$this    "

    private fun peekOperation() = opCodes[opCodesSize - 1]!!

    private fun topIntIndexOf(parameter: IntParameter) =
        intArgsSize - peekOperation().ints + parameter.offset

    private fun topObjectIndexOf(parameter: ObjectParameter<*>) =
        objectArgsSize - peekOperation().objects + parameter.offset

    @JvmInline
    value class WriteScope(private val stack: Operations) {
        val operation: Operation
            get() = stack.peekOperation()

        fun setInt(parameter: IntParameter, value: Int) = with(stack) {
            val mask = 0b1 shl parameter.offset
            checkPrecondition(pushedIntMask and mask == 0) {
                "Already pushed argument ${operation.intParamName(parameter)}"
            }
            pushedIntMask = pushedIntMask or mask
            intArgs[topIntIndexOf(parameter)] = value
        }

        fun <T> setObject(parameter: ObjectParameter<T>, value: T) = with(stack) {
            val mask = 0b1 shl parameter.offset
            checkPrecondition(pushedObjectMask and mask == 0) {
                "Already pushed argument ${operation.objectParamName(parameter)}"
            }
            pushedObjectMask = pushedObjectMask or mask
            objectArgs[topObjectIndexOf(parameter)] = value
        }
    }

    inner class OpIterator : OperationArgContainer {
        private var opIdx = 0
        private var intIdx = 0
        private var objIdx = 0

        fun next(): Boolean {
            if (opIdx >= opCodesSize) return false

            val op = operation
            intIdx += op.ints
            objIdx += op.objects
            opIdx++
            return opIdx < opCodesSize
        }

        /**
         * Returns the [Operation] at the current position of the iterator in the [Operations].
         */
        val operation: Operation
            get() = opCodes[opIdx]!!

        /**
         * Returns the value of [parameter] for the operation at the current position of the
         * iterator.
         */
        override fun getInt(parameter: IntParameter): Int =
            intArgs[intIdx + parameter.offset]

        /**
         * Returns the value of [parameter] for the operation at the current position of the
         * iterator.
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T> getObject(parameter: ObjectParameter<T>): T =
            objectArgs[objIdx + parameter.offset] as T
    }

    companion object {
        private const val MaxResizeAmount = 1024
        internal const val InitialCapacity = 16
    }

    @Deprecated(
        "toString() will return the default implementation from Any. " +
            "Did you mean to use toDebugString()?",
        ReplaceWith("toDebugString()")
    )
    override fun toString(): String {
        return super.toString()
    }

    override fun toDebugString(linePrefix: String): String {
        return buildString {
            var opNumber = 1
            this@Operations.forEach {
                append(linePrefix)
                append(opNumber++)
                append(". ")
                appendLine(currentOpToDebugString(linePrefix))
            }
        }
    }

    private fun Operations.OpIterator.currentOpToDebugString(
        linePrefix: String
    ): String {
        val operation = operation
        return if (operation.ints == 0 && operation.objects == 0) {
            operation.name
        } else buildString {
            append(operation.name)
            append('(')
            var isFirstParam = true
            val argLinePrefix = linePrefix.indent()
            repeat(operation.ints) { offset ->
                val param = Operation.IntParameter(offset)
                val name = operation.intParamName(param)
                if (!isFirstParam) append(", ") else isFirstParam = false
                appendLine()
                append(argLinePrefix)
                append(name)
                append(" = ")
                append(getInt(param))
            }
            repeat(operation.objects) { offset ->
                val param = Operation.ObjectParameter<Any?>(offset)
                val name = operation.objectParamName(param)
                if (!isFirstParam) append(", ") else isFirstParam = false
                appendLine()
                append(argLinePrefix)
                append(name)
                append(" = ")
                append(getObject(param).formatOpArgumentToString(argLinePrefix))
            }
            appendLine()
            append(linePrefix)
            append(")")
        }
    }

    private fun Any?.formatOpArgumentToString(
        linePrefix: String
    ) = when (this) {
        null -> "null"
        is Array<*> -> asIterable().toCollectionString(linePrefix)
        is IntArray -> asIterable().toCollectionString(linePrefix)
        is LongArray -> asIterable().toCollectionString(linePrefix)
        is FloatArray -> asIterable().toCollectionString(linePrefix)
        is DoubleArray -> asIterable().toCollectionString(linePrefix)
        is Iterable<*> -> toCollectionString(linePrefix)
        is OperationsDebugStringFormattable -> toDebugString(linePrefix)
        else -> toString()
    }

    private fun <T> Iterable<T>.toCollectionString(linePrefix: String): String =
        joinToString(prefix = "[", postfix = "]", separator = ", ") {
            it.formatOpArgumentToString(linePrefix)
        }
}

internal abstract class OperationsDebugStringFormattable {
    abstract fun toDebugString(linePrefix: String = "  "): String
}
