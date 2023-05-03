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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

private const val LogParameterizedCalls = false

@RunWith(Parameterized::class)
class PrimitiveSnapshotStateTests<S, T>(
    private val implementation: PrimitiveSnapshotStateImplementation<S, T>
) {

    private val valueIterator = implementation.sampleValues.iterator()

    private fun nextValue() = valueIterator.next()

    private fun logCall(functionInvocation: String) {
        if (LogParameterizedCalls) {
            println("Invoking $functionInvocation")
        }
    }

    @Test
    fun testCreation() {
        val initialValue = nextValue()
        logCall("${implementation.creatorFunctionName}($initialValue)")
        implementation.creator.invoke(initialValue)
    }

    @Test
    fun testReadValue() {
        val initialValue = nextValue()
        logCall("${implementation.creatorFunctionName}($initialValue)")
        val mutableState = implementation.creator.invoke(initialValue)

        logCall("mutableState.value")
        val value = implementation.valueProperty.get(mutableState)

        assertEquals(
            message = "The $implementation's value did not contain the expected value after " +
                "being instantiated with value $initialValue",
            expected = initialValue,
            actual = value
        )
    }

    @Test
    fun testWriteValue() {
        val initialValue = nextValue()
        logCall("${implementation.creatorFunctionName}($initialValue)")
        val mutableState = implementation.creator.invoke(initialValue)

        val nextValue = nextValue()
        logCall("mutableState.value = $nextValue")
        implementation.valueProperty.set(mutableState, nextValue)

        assertEquals(
            message = "The $implementation's value did not contain the expected value after " +
                "being reassigned from $initialValue to $nextValue",
            expected = nextValue,
            actual = implementation.valueProperty.get(mutableState)
        )
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{1}")
        fun initParameters() = arrayOf(
            arrayOf(
                PrimitiveSnapshotStateImplementation(
                clazz = MutableIntState::class.java,
                creator = ::mutableIntStateOf,
                valueProperty = MutableIntState::intValue,
                sampleValues = generateSequence(1) { it + 1 }
                )
            ),
            arrayOf(
                PrimitiveSnapshotStateImplementation(
                clazz = MutableLongState::class.java,
                creator = ::mutableLongStateOf,
                valueProperty = MutableLongState::longValue,
                sampleValues = generateSequence(1) { it + 1 }
                )
            ),
            arrayOf(
                PrimitiveSnapshotStateImplementation(
                clazz = MutableFloatState::class.java,
                creator = ::mutableFloatStateOf,
                valueProperty = MutableFloatState::floatValue,
                sampleValues = generateSequence(1f) { it + 1 }
                )
            ),
            arrayOf(
                PrimitiveSnapshotStateImplementation(
                clazz = MutableDoubleState::class.java,
                creator = ::mutableDoubleStateOf,
                valueProperty = MutableDoubleState::value,
                sampleValues = generateSequence(1.0) { it + 1 }
                )
            )
        )

        data class PrimitiveSnapshotStateImplementation<S, T>(
            val clazz: Class<S>,
            val creator: (T) -> S,
            val valueProperty: KMutableProperty1<S, T>,
            val sampleValues: Sequence<T>
        ) {
            val creatorFunctionName: String
                get() = (creator as? KCallable<*>)?.name ?: "(Unknown Function)"

            override fun toString(): String = clazz.simpleName
        }
    }
}