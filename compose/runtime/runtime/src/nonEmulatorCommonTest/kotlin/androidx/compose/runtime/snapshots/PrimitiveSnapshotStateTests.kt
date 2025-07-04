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
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ENABLE_LOGS = false

class PrimitiveSnapshotStateTests {

    // ---- Int
    @Test fun testCreation_Int() = testCreation(intImpl)

    @Test fun testReadValue_Int() = testReadValue(intImpl)

    @Test fun testWriteValue_Int() = testWriteValue(intImpl)

    // ---- Long
    @Test fun testCreation_Long() = testCreation(longImpl)

    @Test fun testReadValue_Long() = testReadValue(longImpl)

    @Test fun testWriteValue_Long() = testWriteValue(longImpl)

    // ---- Float
    @Test fun testCreation_Float() = testCreation(floatImpl)

    @Test fun testReadValue_Float() = testReadValue(floatImpl)

    @Test fun testWriteValue_Float() = testWriteValue(floatImpl)

    // ---- Double
    @Test fun testCreation_Double() = testCreation(doubleImpl)

    @Test fun testReadValue_Double() = testReadValue(doubleImpl)

    @Test fun testWriteValue_Double() = testWriteValue(doubleImpl)

    companion object {
        data class PrimitiveSnapshotStateImplementation<S : Any, T>(
            val kClass: KClass<S>,
            val creator: (T) -> S,
            val valueProperty: KMutableProperty1<S, T>,
            val sampleValues: Sequence<T>,
        ) {
            val creatorFunctionName: String
                get() = (creator as? KCallable<*>)?.name ?: "(Unknown Function)"

            override fun toString(): String = kClass.simpleName ?: "(Unknown Class)"
        }

        private val intImpl =
            PrimitiveSnapshotStateImplementation(
                kClass = MutableIntState::class,
                creator = ::mutableIntStateOf,
                valueProperty = MutableIntState::intValue,
                sampleValues = generateSequence(1) { it + 1 },
            )

        private val longImpl =
            PrimitiveSnapshotStateImplementation(
                kClass = MutableLongState::class,
                creator = ::mutableLongStateOf,
                valueProperty = MutableLongState::longValue,
                sampleValues = generateSequence(1L) { it + 1 },
            )

        private val floatImpl =
            PrimitiveSnapshotStateImplementation(
                kClass = MutableFloatState::class,
                creator = ::mutableFloatStateOf,
                valueProperty = MutableFloatState::floatValue,
                sampleValues = generateSequence(1f) { it + 1 },
            )

        private val doubleImpl =
            PrimitiveSnapshotStateImplementation(
                kClass = MutableDoubleState::class,
                creator = ::mutableDoubleStateOf,
                valueProperty = MutableDoubleState::doubleValue,
                sampleValues = generateSequence(1.0) { it + 1 },
            )

        private fun <S : Any, T> testCreation(impl: PrimitiveSnapshotStateImplementation<S, T>) {
            val initialValue = impl.sampleValues.first()

            logCall("${impl.creatorFunctionName}($initialValue)")
            impl.creator.invoke(initialValue)
        }

        private fun <S : Any, T> testReadValue(impl: PrimitiveSnapshotStateImplementation<S, T>) {
            val initialValue = impl.sampleValues.first()

            logCall("${impl.creatorFunctionName}($initialValue)")
            val mutableState = impl.creator.invoke(initialValue)

            logCall("mutableState.value")
            val actualValue = impl.valueProperty.get(mutableState)

            assertEquals(
                expected = initialValue,
                actual = actualValue,
                message =
                    "Expected $initialValue, but got $actualValue for ${impl.kClass.simpleName}",
            )
        }

        private fun <S : Any, T> testWriteValue(impl: PrimitiveSnapshotStateImplementation<S, T>) {
            val (initialValue, nextValue) = impl.sampleValues.take(2).toList()

            logCall("${impl.creatorFunctionName}($initialValue)")
            val mutableState = impl.creator.invoke(initialValue)

            logCall("mutableState.value = $nextValue")
            impl.valueProperty.set(mutableState, nextValue)

            val actualValue = impl.valueProperty.get(mutableState)
            assertEquals(
                expected = nextValue,
                actual = actualValue,
                message =
                    "Expected $nextValue after writing, but got $actualValue for ${impl.kClass.simpleName}",
            )
        }

        private fun logCall(message: String) {
            if (ENABLE_LOGS) {
                println(message)
            }
        }
    }
}
