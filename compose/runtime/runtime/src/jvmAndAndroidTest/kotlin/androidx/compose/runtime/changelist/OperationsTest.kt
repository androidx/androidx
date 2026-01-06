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

import androidx.compose.runtime.EnableDebugRuntimeChecks
import androidx.compose.runtime.changelist.Operation.ObjectParameter
import androidx.compose.runtime.changelist.TestOperations.MixedOperation
import androidx.compose.runtime.changelist.TestOperations.NoArgsOperation
import androidx.compose.runtime.changelist.TestOperations.OneIntOperation
import androidx.compose.runtime.changelist.TestOperations.OneObjectOperation
import androidx.compose.runtime.changelist.TestOperations.ThreeIntsOperation
import androidx.compose.runtime.changelist.TestOperations.ThreeObjectsOperation
import androidx.compose.runtime.changelist.TestOperations.TwoIntsOperation
import androidx.compose.runtime.changelist.TestOperations.TwoObjectsOperation
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class OperationsTest {

    private val stack = Operations()

    @Test
    fun testInitialization() {
        assertStackState(
            message = "OpStack did not initialize in the expected state",
            stack = stack,
        )
    }

    @Test
    fun testPush_withNoBlock_writesOperationWithNoArgs() {
        stack.push(NoArgsOperation)
        assertStackState(
            message =
                "OpStack was not in the expected state " +
                    "after pushing an operation with no arguments",
            stack = stack,
            expectedOperations = listOf(NoArgsOperation),
        )
    }

    @Test
    fun testPush_withNoBlock_failsIfOperationHasArguments() {
        if (EnableDebugRuntimeChecks) {
            try {
                stack.push(MixedOperation)
                fail("Pushing an operation should fail if it has arguments")
            } catch (_: IllegalArgumentException) {}
        }
    }

    @Test
    fun testPush_intArguments() {
        stack.push(OneIntOperation) {
            val (int1) = OneIntOperation.intParams
            setInt(int1, 42)
        }
        assertStackState(
            stack = stack,
            expectedOperations = listOf(OneIntOperation),
            expectedIntArgs = listOf(42),
        )

        stack.push(TwoIntsOperation) {
            val (int1, int2) = TwoIntsOperation.intParams
            setInt(int1, 1234)
            setInt(int2, 5678)
        }
        assertStackState(
            stack = stack,
            expectedOperations = listOf(OneIntOperation, TwoIntsOperation),
            expectedIntArgs = listOf(42, 1234, 5678),
        )

        stack.push(ThreeIntsOperation) {
            val (int1, int2, int3) = ThreeIntsOperation.intParams
            setInt(int1, 11)
            setInt(int2, 22)
            setInt(int3, 33)
        }
        assertStackState(
            stack = stack,
            expectedOperations = listOf(OneIntOperation, TwoIntsOperation, ThreeIntsOperation),
            expectedIntArgs = listOf(42, 1234, 5678, 11, 22, 33),
        )
    }

    @Test
    fun testPush_objectArguments() {
        stack.push(OneObjectOperation) {
            val (obj1) = OneObjectOperation.objParams
            setObject(obj1, null)
        }
        assertStackState(
            stack = stack,
            expectedOperations = listOf(OneObjectOperation),
            expectedObjArgs = listOf(null),
        )

        stack.push(TwoObjectsOperation) {
            val (obj1, obj2) = TwoObjectsOperation.objParams
            setObject(obj1, "Hello")
            setObject(obj2, "World")
        }
        assertStackState(
            stack = stack,
            expectedOperations = listOf(OneObjectOperation, TwoObjectsOperation),
            expectedObjArgs = listOf(null, "Hello", "World"),
        )

        stack.push(ThreeObjectsOperation) {
            val (obj1, obj2, obj3) = ThreeObjectsOperation.objParams
            setObject(obj1, Unit)
            setObject(obj2, "Another string")
            setObject(obj3, 123.456)
        }
        assertStackState(
            stack = stack,
            expectedOperations =
                listOf(OneObjectOperation, TwoObjectsOperation, ThreeObjectsOperation),
            expectedObjArgs = listOf(null, "Hello", "World", Unit, "Another string", 123.456),
        )
    }

    @Test
    fun testPush_mixedArguments() {
        stack.push(MixedOperation) {
            val (int1, int2) = MixedOperation.intParams
            val (obj1, obj2) = MixedOperation.objParams

            setInt(int1, 999)
            setInt(int2, -1)
            setObject(obj1, "String 1")
            setObject(obj2, "String 2")
        }

        assertStackState(
            stack = stack,
            expectedOperations = listOf(MixedOperation),
            expectedIntArgs = listOf(999, -1),
            expectedObjArgs = listOf("String 1", "String 2"),
        )
    }

    @Test
    fun testPush_variousOperations() {
        pushVariousOperations(stack)
        assertStackInVariousOperationState(stack = stack)
    }

    @Test
    fun testPush_withResizingRequired() {
        check(
            stack.opCodes.size == OperationsInitialCapacity &&
                stack.intArgs.size == OperationsInitialCapacity &&
                stack.objectArgs.size == OperationsInitialCapacity
        ) {
            "OpStack did not initialize one or more of its backing arrays (opCodes, intArgs, " +
                "or objectArgs) to `OpStack.InitialCapacity`. Please use the constant or update " +
                "this test with the correct capacity to ensure that resizing is being tested."
        }

        val itemsToForceResize = OperationsInitialCapacity + 1
        repeat(itemsToForceResize) { opNumber ->
            stack.push(TwoIntsOperation) {
                val (int1, int2) = TwoIntsOperation.intParams
                setInt(int1, opNumber * 10 + 1)
                setInt(int2, opNumber * 10 + 2)
            }
        }

        repeat(itemsToForceResize) { opNumber ->
            stack.push(TwoObjectsOperation) {
                val (obj1, obj2) = TwoObjectsOperation.objParams
                setObject(obj1, "op $opNumber, obj 1")
                setObject(obj2, "op $opNumber, obj 2")
            }
        }

        assertStackState(
            message =
                "Stack was not in the expected state after pushing " +
                    "$itemsToForceResize operations, requiring that the stack resize all " +
                    "of its internal arrays.",
            stack = stack,
            expectedOperations =
                List(itemsToForceResize) { TwoIntsOperation } +
                    List(itemsToForceResize) { TwoObjectsOperation },
            expectedIntArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf(it * 10 + 1, it * 10 + 2) }
                    .take(itemsToForceResize * TwoIntsOperation.ints)
                    .toList(),
            expectedObjArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf("op $it, obj 1", "op $it, obj 2") }
                    .take(itemsToForceResize * TwoObjectsOperation.objects)
                    .toList(),
        )
    }

    @Test
    fun testPush_throwsIfAnyIntArgsNotProvided() {
        if (EnableDebugRuntimeChecks) {
            try {
                stack.push(TwoIntsOperation) {
                    val (_, intArg2) = TwoIntsOperation.intParams
                    setInt(intArg2, 42)
                }
                fail(
                    "Pushing an operation that defines two parameters should fail " +
                        "if only one of the arguments is set"
                )
            } catch (e: IllegalStateException) {
                assertTrue(
                    message =
                        "The thrown exception does not appear to have reported the expected " +
                            "error (its message is '${e.message}')",
                    actual = e.message.orEmpty().contains("Not all arguments were provided"),
                )
            }
        }
    }

    @Test
    fun testPush_throwsIfAnyObjectArgsNotProvided() {
        if (EnableDebugRuntimeChecks) {
            try {
                stack.push(TwoObjectsOperation) {
                    val (_, objectArg2) = TwoObjectsOperation.objParams
                    setObject(objectArg2, Any())
                }
                fail(
                    "Pushing an operation that defines two parameters should fail " +
                        "if only one of the arguments is set"
                )
            } catch (e: IllegalStateException) {
                assertTrue(
                    message =
                        "The thrown exception does not appear to have reported the expected " +
                            "error (its message is '${e.message}')",
                    actual = e.message.orEmpty().contains("Not all arguments were provided"),
                )
            }
        }
    }

    @Test
    fun testPush_throwsIfIntArgProvidedTwice() {
        if (EnableDebugRuntimeChecks) {
            try {
                stack.push(ThreeIntsOperation) {
                    val (_, intArg2, _) = ThreeIntsOperation.intParams
                    setInt(intArg2, 2)
                    setInt(intArg2, 2)
                }
                fail("Pushing an operation should fail if an argument is set twice")
            } catch (e: IllegalStateException) {
                assertTrue(
                    message =
                        "The thrown exception does not appear to have reported the expected " +
                            "error (its message is '${e.message}')",
                    actual = e.message.orEmpty().contains("Already pushed argument"),
                )
            }
        }
    }

    @Test
    fun testPush_throwsIfObjectArgProvidedTwice() {
        if (EnableDebugRuntimeChecks) {
            try {
                stack.push(ThreeObjectsOperation) {
                    val (_, objectArg2, _) = ThreeObjectsOperation.objParams
                    setObject(objectArg2, Any())
                    setObject(objectArg2, Any())
                }
                fail("Pushing an operation should fail if an argument is set twice")
            } catch (e: IllegalStateException) {
                assertTrue(
                    message =
                        "The thrown exception does not appear to have reported the expected " +
                            "error (its message is '${e.message}')",
                    actual = e.message.orEmpty().contains("Already pushed argument"),
                )
            }
        }
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testPop_throwsIfStackIsEmpty() {
        stack.pop()
    }

    @Test
    fun testPop_removesOnlyOperation() {
        stack.push(MixedOperation) {
            val (int1, int2) = MixedOperation.intParams
            val (obj1, obj2) = MixedOperation.objParams

            setInt(int1, 20)
            setInt(int2, 30)
            setObject(obj1, "obj1")
            setObject(obj2, "obj2")
        }

        stack.pop()
        assertStackState(
            message = "Stack should be empty after popping the only item",
            stack = stack,
        )
    }

    @Test
    fun testPop_removesMostRecentlyPushedOperation() {
        stack.push(MixedOperation) {
            val (int1, int2) = MixedOperation.intParams
            val (obj1, obj2) = MixedOperation.objParams

            setInt(int1, 20)
            setInt(int2, 30)
            setObject(obj1, "obj1")
            setObject(obj2, "obj2")
        }

        stack.pop()
        assertStackState(
            message = "Stack should be empty after popping the only item",
            stack = stack,
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testPopInto_throwsIfStackIsEmpty() {
        stack.pop()
    }

    @Test
    fun testPopInto_copiesAndRemovesOperationToNewTarget() {
        repeat(40) { opNumber ->
            stack.push(ThreeObjectsOperation) {
                val (obj1, obj2, obj3) = ThreeObjectsOperation.objParams
                setObject(obj1, "${opNumber}A")
                setObject(obj2, "${opNumber}B")
                setObject(obj3, "${opNumber}C")
            }
        }

        val destinationStack = Operations()
        repeat(20) { opNumber ->
            destinationStack.push(ThreeIntsOperation) {
                val (int1, int2, int3) = ThreeIntsOperation.intParams
                setInt(int1, opNumber * 10)
                setInt(int2, opNumber * 20)
                setInt(int3, opNumber * 30)
            }
        }

        assertStackState(
            message = "Source stack did not initialize to the expected state",
            stack = stack,
            expectedOperations = List(40) { ThreeObjectsOperation },
            expectedObjArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf("${it}A", "${it}B", "${it}C") }
                    .take(40 * ThreeObjectsOperation.objects)
                    .toList(),
        )

        assertStackState(
            message = "Destination stack did not initialize to the expected state",
            stack = destinationStack,
            expectedOperations = List(20) { ThreeIntsOperation },
            expectedIntArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf(it * 10, it * 20, it * 30) }
                    .take(20 * ThreeIntsOperation.ints)
                    .toList(),
        )

        stack.popInto(destinationStack)

        assertStackState(
            message = "The last pushed operation was not removed from the source stack as expected",
            stack = stack,
            expectedOperations = List(39) { ThreeObjectsOperation },
            expectedObjArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf("${it}A", "${it}B", "${it}C") }
                    .take(39 * ThreeObjectsOperation.objects)
                    .toList(),
        )

        assertStackState(
            message = "The popped item was not added to the destination stack as expected",
            stack = destinationStack,
            expectedOperations = List(20) { ThreeIntsOperation } + ThreeObjectsOperation,
            expectedIntArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf(it * 10, it * 20, it * 30) }
                    .take(20 * ThreeIntsOperation.ints)
                    .toList(),
            expectedObjArgs = listOf("39A", "39B", "39C"),
        )
    }

    @Test
    fun testClear_resetsToInitialState() {
        val operationCount = OperationsInitialCapacity * 4
        repeat(operationCount) { opNumber ->
            stack.push(MixedOperation) {
                val (int1, int2) = MixedOperation.intParams
                val (obj1, obj2) = MixedOperation.objParams

                setInt(int1, opNumber)
                setInt(int2, -opNumber)
                setObject(obj1, "obj1:$opNumber")
                setObject(obj2, "obj2:$opNumber")
            }
        }

        assertStackState(
            message = "Stack was not seeded into the expected state",
            stack = stack,
            expectedOperations = List(operationCount) { MixedOperation },
            expectedIntArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf(it, -it) }
                    .take(operationCount * MixedOperation.ints)
                    .toList(),
            expectedObjArgs =
                generateSequence(0) { it + 1 }
                    .flatMap { sequenceOf("obj1:$it", "obj2:$it") }
                    .take(operationCount * MixedOperation.objects)
                    .toList(),
        )

        stack.clear()
        assertStackState(
            message = "Stack did not properly reset after calling clear()",
            stack = stack,
        )
    }

    @Test
    fun testDrain_iteratesThroughAllElements_inPushOrder_andClearsStack() {
        pushVariousOperations(stack)

        val capturedOperations = mutableListOf<Operation>()
        val capturedInts = mutableListOf<Int>()
        val capturedObjects = mutableListOf<Any?>()
        stack.drain {
            capturedOperations += operation
            repeat(operation.ints) { offset -> capturedInts += getInt(offset) }
            repeat(operation.objects) { offset ->
                capturedObjects += getObject(ObjectParameter<Any?>(offset))
            }
        }

        assertEquals(
            message = "The stack's operations were not received in the expected order.",
            expected =
                listOf<Operation>(
                    MixedOperation,
                    NoArgsOperation,
                    TwoIntsOperation,
                    ThreeObjectsOperation,
                    NoArgsOperation,
                ),
            actual = capturedOperations,
        )

        assertEquals(
            message = "The stack's int arguments were not received in the expected order.",
            expected = listOf(999, -1, 0xABCDEF, 0x123456),
            actual = capturedInts,
        )

        assertEquals(
            message = "The stack's object arguments were not received in the expected order.",
            expected = listOf("String 1", "String 2", 1.0, Unit, null),
            actual = capturedObjects,
        )

        assertStackState(message = "Stack should be empty after calling drain()", stack = stack)
    }

    @Test
    fun testForEach_iteratesThroughOperations_inPushOrder() {
        pushVariousOperations(stack)

        val capturedOperations = mutableListOf<Operation>()
        val capturedInts = mutableListOf<Int>()
        val capturedObjects = mutableListOf<Any?>()
        stack.forEach {
            capturedOperations += operation
            repeat(operation.ints) { offset -> capturedInts += getInt(offset) }
            repeat(operation.objects) { offset ->
                capturedObjects += getObject(ObjectParameter<Any?>(offset))
            }
        }

        assertEquals(
            message = "The stack's operations were not received in the expected order.",
            expected =
                listOf<Operation>(
                    MixedOperation,
                    NoArgsOperation,
                    TwoIntsOperation,
                    ThreeObjectsOperation,
                    NoArgsOperation,
                ),
            actual = capturedOperations,
        )

        assertEquals(
            message = "The stack's int arguments were not received in the expected order.",
            expected = listOf(999, -1, 0xABCDEF, 0x123456),
            actual = capturedInts,
        )

        assertEquals(
            message = "The stack's object arguments were not received in the expected order.",
            expected = listOf("String 1", "String 2", 1.0, Unit, null),
            actual = capturedObjects,
        )

        assertStackInVariousOperationState(
            message = "Stack should not be modified after iterating",
            stack = stack,
        )
    }

    private fun pushVariousOperations(stack: Operations) {
        stack.apply {
            push(MixedOperation) {
                val (int1, int2) = MixedOperation.intParams
                val (obj1, obj2) = MixedOperation.objParams

                setInt(int1, 999)
                setInt(int2, -1)
                setObject(obj1, "String 1")
                setObject(obj2, "String 2")
            }

            push(NoArgsOperation)

            push(TwoIntsOperation) {
                val (int1, int2) = TwoIntsOperation.intParams

                setInt(int1, 0xABCDEF)
                setInt(int2, 0x123456)
            }

            push(ThreeObjectsOperation) {
                val (obj1, obj2, obj3) = ThreeObjectsOperation.objParams

                setObject(obj1, 1.0)
                setObject(obj2, Unit)
                setObject(obj3, null)
            }

            push(NoArgsOperation)
        }
    }

    private fun assertStackInVariousOperationState(message: String = "", stack: Operations) {
        assertStackState(
            message = message,
            stack = stack,
            expectedOperations =
                listOf(
                    MixedOperation,
                    NoArgsOperation,
                    TwoIntsOperation,
                    ThreeObjectsOperation,
                    NoArgsOperation,
                ),
            expectedIntArgs = listOf(999, -1, 0xABCDEF, 0x123456),
            expectedObjArgs = listOf("String 1", "String 2", 1.0, Unit, null),
        )
    }

    private fun assertStackState(
        message: String = "",
        stack: Operations,
        expectedOperations: List<Operation> = emptyList(),
        expectedIntArgs: List<Int> = emptyList(),
        expectedObjArgs: List<Any?> = emptyList(),
    ) {
        val errors = mutableListOf<String>()

        val size = stack.size
        val isEmpty = stack.isEmpty()
        val isNotEmpty = stack.isNotEmpty()
        if (size != expectedOperations.size) {
            errors +=
                "Stack did not report correct size " +
                    "(expected ${expectedOperations.size}, was $size)"
        }

        if (isEmpty != expectedOperations.isEmpty()) {
            errors +=
                "isEmpty() did not return expected value " +
                    "(expected ${expectedOperations.isEmpty()}, was $isEmpty)"
        }

        if (isNotEmpty != expectedOperations.isNotEmpty()) {
            errors +=
                "isNotEmpty() did not return expected value " +
                    "(expected ${expectedOperations.isNotEmpty()}, was $isNotEmpty)"
        }

        val actualOpCodes = stack.opCodes
        if (!actualOpCodes.asIterable().startsWith(expectedOperations)) {
            errors +=
                "opCodes array did not match expected operations " +
                    "(expected [${expectedOperations.joinToString()}], was " +
                    "[${actualOpCodes.joinToString()}])"
        }

        val actualIntArgs = stack.intArgs
        if (!actualIntArgs.asIterable().startsWith(expectedIntArgs)) {
            errors +=
                "intArgs array did not match expected operations " +
                    "(expected [${expectedIntArgs.joinToString()}], was " +
                    "[${actualIntArgs.joinToString()}])"
        }

        val actualObjectArgs = stack.objectArgs
        if (!actualObjectArgs.asIterable().startsWith(expectedObjArgs, null)) {
            errors +=
                "objectArgs array did not match expected operations " +
                    "(expected [${expectedObjArgs.joinToString()}], was " +
                    "[${actualObjectArgs.joinToString()}])"
        }

        if (errors.isNotEmpty()) {
            fail(
                message.takeIf { it.isNotBlank() }?.let { "$it\n" }.orEmpty() +
                    "Failed with the following validation errors:" +
                    errors.joinToString { "\n    - $it" }
            )
        }
    }

    private fun <T> Iterable<T>.startsWith(other: Iterable<T>): Boolean {
        val thisIterator = iterator()
        val otherIterator = other.iterator()

        while (otherIterator.hasNext()) {
            if (!thisIterator.hasNext() || thisIterator.next() != otherIterator.next()) {
                return false
            }
        }

        return true
    }

    private fun <T> Iterable<T>.startsWith(other: Iterable<T>, endFill: T): Boolean {
        val thisIterator = iterator()
        val otherIterator = other.iterator()

        while (otherIterator.hasNext()) {
            if (!thisIterator.hasNext() || thisIterator.next() != otherIterator.next()) {
                return false
            }
        }

        while (thisIterator.hasNext()) {
            if (thisIterator.next() != endFill) {
                return false
            }
        }

        return true
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private val Operations.opCodes: Array<Operation?>
        get() = readPropertyReflectively("opCodes")

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private val Operations.intArgs: IntArray
        get() = readPropertyReflectively("intArgs")

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private val Operations.objectArgs: Array<Any?>
        get() = readPropertyReflectively("objectArgs")

    private inline fun <reified T : Any, reified R> T.readPropertyReflectively(
        propertyName: String
    ): R {
        val property =
            this::class
                .declaredMembers
                .mapNotNull {
                    @Suppress("UNCHECKED_CAST")
                    it as? KProperty1<T, R>
                }
                .single { it.name == propertyName }

        property.isAccessible = true
        return property.get(this)
    }
}
