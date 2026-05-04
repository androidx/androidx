/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush.behavior

import androidx.collection.MutableIntObjectMap
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/** A [ValueNode] that combines two other values with a binary operation. */
public class BinaryOpNode
private constructor(
    nativePointer: Long,
    /** The input node that produces the first value used in the binary operation. */
    public val firstInput: ValueNode,
    /** The input node that produces the second value used in the binary operation. */
    public val secondInput: ValueNode,
) : ValueNode(nativePointer, listOf(firstInput, secondInput)) {

    /**
     * Creates a [BinaryOpNode] that combines two other values with a binary operation.
     *
     * @param operation the binary operation to perform
     * @param firstInput input node that produces the first value used in the binary operation
     * @param secondInput input node that produces the second value used in the binary operation
     */
    public constructor(
        operation: BinaryOp,
        firstInput: ValueNode,
        secondInput: ValueNode,
    ) : this(BinaryOpNodeNative.createBinaryOp(operation.value), firstInput, secondInput)

    internal companion object {
        internal fun wrapNative(
            unownedNativePointer: Long,
            inputStack: ArrayDeque<ValueNode>,
        ): BinaryOpNode {
            // Inputs are in reverse order at the end of the stack.
            val secondInput = inputStack.removeLast()
            val firstInput = inputStack.removeLast()
            return BinaryOpNode(unownedNativePointer, firstInput, secondInput)
        }
    }

    /** The binary operation to perform. */
    public val operation: BinaryOp = BinaryOpNodeNative.getBinaryOperation(nativePointer)

    override fun toString(): String =
        "BinaryOpNode(${operation.toSimpleString()}, $firstInput, $secondInput)"

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BinaryOpNode) return false
        if (other === this) return true
        return operation == other.operation &&
            firstInput == other.firstInput &&
            secondInput == other.secondInput
    }

    override fun hashCode(): Int {
        var result = operation.hashCode()
        result = 31 * result + firstInput.hashCode()
        result = 31 * result + secondInput.hashCode()
        return result
    }

    /**
     * A binary operation for combining two values in a [BinaryOpNode]. Unless otherwise specified
     * for a particular operator, the result will be null (i.e. undefined) if either input value is
     * null.
     */
    public class BinaryOp
    private constructor(@JvmField internal val value: Int, private val name: String) {
        init {
            check(value !in VALUE_TO_INSTANCE) { "Duplicate BinaryOp value: $value." }
            VALUE_TO_INSTANCE[value] = this
        }

        internal fun toSimpleString(): String = name

        override fun toString(): String = "BinaryOp." + name

        public companion object {
            private val VALUE_TO_INSTANCE = MutableIntObjectMap<BinaryOp>()

            internal fun fromInt(value: Int): BinaryOp =
                checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid BinaryOp value: $value" }

            /** Evaluates to the product of the two input values. */
            @JvmField public val PRODUCT: BinaryOp = BinaryOp(0, "PRODUCT")
            /** Evaluates to the sum of the two input values. */
            @JvmField public val SUM: BinaryOp = BinaryOp(1, "SUM")
            /** Evaluates to the min of the two input values. */
            @JvmField public val MIN: BinaryOp = BinaryOp(2, "MIN")
            /** Evaluates to the max of the two input values. */
            @JvmField public val MAX: BinaryOp = BinaryOp(3, "MAX")
            /**
             * Evaluates to null if the first input is null, otherwise evaluates to the second
             * input.
             */
            @JvmField public val AND_THEN: BinaryOp = BinaryOp(4, "AND_THEN")
            /**
             * Evaluates to first input if it's not null, otherwise evaluates to the second input.
             */
            @JvmField public val OR_ELSE: BinaryOp = BinaryOp(5, "OR_ELSE")
            /**
             * If exactly one input isn't null, evaluates to that one, otherwise evaluates to null.
             */
            @JvmField public val XOR_ELSE: BinaryOp = BinaryOp(6, "XOR_ELSE")
        }
    }
}

/**
 * Singleton wrapper for `BrushBehavior::BinaryOpNode` native methods.
 *
 * Note that even though Kotlin [Node] is an abstract class with several subtypes,
 * [Node.nativePointer] all wrap the _same_ native type (a specialization of `std::variant`).
 */
@UsedByNative
private object BinaryOpNodeNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createBinaryOp(operation: Int): Long

    fun getBinaryOperation(nativePointer: Long): BinaryOpNode.BinaryOp =
        BinaryOpNode.BinaryOp.fromInt(getBinaryOperationInt(nativePointer))

    @UsedByNative private external fun getBinaryOperationInt(nativePointer: Long): Int
}
