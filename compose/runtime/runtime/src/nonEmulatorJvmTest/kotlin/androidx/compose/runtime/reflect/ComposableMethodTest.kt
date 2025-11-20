/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.reflect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.withRunningRecomposer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

@Composable private fun ComposableFunction() {}

private fun nonComposableFunction() {}

@Suppress("UNUSED_PARAMETER")
private fun nonComposableFunctionWithComposerParam(unused: Composer) {}

@Composable
private fun composableFunctionWithDefaults(
    s1: String,
    s2: String,
    s3: String = "a",
    s4: String = "a",
    s5: String = "a",
): String {
    return s1 + s2 + s3 + s4 + s5
}

@Composable private fun OverloadedComposable() {}

@Suppress("UNUSED_PARAMETER") @Composable private fun OverloadedComposable(s: String) {}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun OverloadedComposable(
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String,
) {}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun OverloadedComposable(
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String,
    v11: String,
) {}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun OverloadedComposable(
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String,
    v11: String,
    v12: String,
) {}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun DifferentParametersTypes(
    v1: String,
    v2: Any,
    v3: Int,
    v4: Float,
    v5: Double,
    v6: Long,
) {}

private class ComposablesWrapper {
    @Composable fun ComposableMethod() {}

    fun nonComposableMethod() {}

    @Suppress("UNUSED_PARAMETER") fun nonComposableMethodWithComposerParam(unused: Composer) {}

    @Composable
    fun composableMethodWithDefaults(
        s1: String,
        s2: String,
        s3: String = "a",
        s4: String = "a",
        s5: String = "a",
    ): String {
        return s1 + s2 + s3 + s4 + s5
    }

    @Composable fun OverloadedComposableMethod() {}

    @Suppress("UNUSED_PARAMETER") @Composable fun OverloadedComposableMethod(s: String) {}

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun OverloadedComposableMethod(
        v1: String,
        v2: String,
        v3: String,
        v4: String,
        v5: String,
        v6: String,
        v7: String,
        v8: String,
        v9: String,
        v10: String,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun OverloadedComposableMethod(
        v1: String,
        v2: String,
        v3: String,
        v4: String,
        v5: String,
        v6: String,
        v7: String,
        v8: String,
        v9: String,
        v10: String,
        v11: String,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun OverloadedComposableMethod(
        v1: String,
        v2: String,
        v3: String,
        v4: String,
        v5: String,
        v6: String,
        v7: String,
        v8: String,
        v9: String,
        v10: String,
        v11: String,
        v12: String,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun DifferentParametersTypesMethod(
        v1: String,
        v2: Any,
        v3: Int,
        v4: Float,
        v5: Double,
        v6: Long,
    ) {}
}

class ComposableMethodTest {
    private val clazz = Class.forName("androidx.compose.runtime.reflect.ComposableMethodTestKt")
    private val wrapperClazz = Class.forName("androidx.compose.runtime.reflect.ComposablesWrapper")

    private val composable = clazz.declaredMethods.find { it.name == "ComposableFunction" }!!
    private val nonComposable = clazz.declaredMethods.find { it.name == "nonComposableFunction" }!!
    private val nonComposableWithComposer =
        clazz.declaredMethods.find { it.name == "nonComposableFunctionWithComposerParam" }!!
    private val composableMethod =
        wrapperClazz.declaredMethods.find { it.name == "ComposableMethod" }!!
    private val nonComposableMethod =
        wrapperClazz.declaredMethods.find { it.name == "nonComposableMethod" }!!
    private val nonComposableMethodWithComposer =
        wrapperClazz.declaredMethods.find { it.name == "nonComposableMethodWithComposerParam" }!!

    @Test
    fun test_isComposable_correctly_checks_functions() {
        assertNotNull(composable.asComposableMethod())
        assertNull(nonComposable.asComposableMethod())
        assertNull(nonComposableWithComposer.asComposableMethod())
        assertNotNull(composableMethod.asComposableMethod())
        assertNull(nonComposableMethod.asComposableMethod())
        assertNull(nonComposableMethodWithComposer.asComposableMethod())
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_getDeclaredComposableMethod_differentiates_overloaded_functions() {
        val method0 = clazz.getDeclaredComposableMethod("OverloadedComposable")
        val method1 = clazz.getDeclaredComposableMethod("OverloadedComposable", String::class.java)
        val method10 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(10) { String::class.java },
            )
        val method11 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(11) { String::class.java },
            )
        val method12 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(12) { String::class.java },
            )

        assertNotEquals(method0, method1)
        assertNotEquals(method0, method10)
        assertNotEquals(method0, method11)
        assertNotEquals(method0, method12)
        assertNotEquals(method1, method10)
        assertNotEquals(method1, method11)
        assertNotEquals(method1, method12)
        assertNotEquals(method10, method11)
        assertNotEquals(method10, method12)
        assertNotEquals(method11, method12)
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_getDeclaredComposableMethod_differentiates_overloaded_methods() {
        val method0 = wrapperClazz.getDeclaredComposableMethod("OverloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                String::class.java,
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(10) { String::class.java },
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(11) { String::class.java },
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(12) { String::class.java },
            )

        assertNotEquals(method0, method1)
        assertNotEquals(method0, method10)
        assertNotEquals(method0, method11)
        assertNotEquals(method0, method12)
        assertNotEquals(method1, method10)
        assertNotEquals(method1, method11)
        assertNotEquals(method1, method12)
        assertNotEquals(method10, method11)
        assertNotEquals(method10, method12)
        assertNotEquals(method11, method12)
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_getDeclaredComposableMethod_works_with_default_params() {
        clazz.getDeclaredComposableMethod(
            "composableFunctionWithDefaults",
            *Array(5) { String::class.java },
        )

        wrapperClazz.getDeclaredComposableMethod(
            "composableMethodWithDefaults",
            *Array(5) { String::class.java },
        )
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_realParametersCount_returns_correct_number_of_parameters() {
        val function0 = clazz.getDeclaredComposableMethod("OverloadedComposable")
        val function1 =
            clazz.getDeclaredComposableMethod("OverloadedComposable", String::class.java)
        val function10 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(10) { String::class.java },
            )
        val function11 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(11) { String::class.java },
            )
        val function12 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(12) { String::class.java },
            )

        val method0 = wrapperClazz.getDeclaredComposableMethod("OverloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                String::class.java,
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(10) { String::class.java },
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(11) { String::class.java },
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(12) { String::class.java },
            )

        assertEquals(0, function0.parameterCount)
        assertEquals(1, function1.parameterCount)
        assertEquals(10, function10.parameterCount)
        assertEquals(11, function11.parameterCount)
        assertEquals(12, function12.parameterCount)

        assertEquals(0, method0.parameterCount)
        assertEquals(1, method1.parameterCount)
        assertEquals(10, method10.parameterCount)
        assertEquals(11, method11.parameterCount)
        assertEquals(12, method12.parameterCount)

        assertEquals(0, composableMethod.asComposableMethod()!!.parameterCount)
    }

    @Suppress("NewApi")
    @Throws(NoSuchMethodException::class)
    @Test
    fun test_realParameters_returns_correct_parameters() {
        val function0 = clazz.getDeclaredComposableMethod("OverloadedComposable")
        val function1 =
            clazz.getDeclaredComposableMethod("OverloadedComposable", String::class.java)
        val function10 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(10) { String::class.java },
            )
        val function11 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(11) { String::class.java },
            )
        val function12 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(12) { String::class.java },
            )

        val method0 = wrapperClazz.getDeclaredComposableMethod("OverloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                String::class.java,
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(10) { String::class.java },
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(11) { String::class.java },
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(12) { String::class.java },
            )

        val diffParameters =
            clazz.getDeclaredComposableMethod(
                "DifferentParametersTypes",
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            )

        val diffParametersMethod =
            wrapperClazz.getDeclaredComposableMethod(
                "DifferentParametersTypesMethod",
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            )

        assertEquals(0, function0.parameters.size)
        assertEquals(1, function1.parameters.size)
        assertEquals(10, function10.parameters.size)
        assertEquals(11, function11.parameters.size)
        assertEquals(12, function12.parameters.size)
        assertEquals(12, function12.parameters.size)

        assertEquals(0, method0.parameters.size)
        assertEquals(1, method1.parameters.size)
        assertEquals(10, method10.parameters.size)
        assertEquals(11, method11.parameters.size)
        assertEquals(12, method12.parameters.size)

        assertEquals(0, composableMethod.asComposableMethod()!!.parameters.size)

        assertEquals(6, diffParameters.parameters.size)
        assertEquals(
            listOf(
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            ),
            diffParameters.parameters.map { it.type },
        )

        assertEquals(6, diffParametersMethod.parameters.size)
        assertEquals(
            listOf(
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            ),
            diffParametersMethod.parameters.map { it.type },
        )
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_realParameterTypes_returns_correct_parameterTypes() {
        val function0 = clazz.getDeclaredComposableMethod("OverloadedComposable")
        val function1 =
            clazz.getDeclaredComposableMethod("OverloadedComposable", String::class.java)
        val function10 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(10) { String::class.java },
            )
        val function11 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(11) { String::class.java },
            )
        val function12 =
            clazz.getDeclaredComposableMethod(
                "OverloadedComposable",
                *Array(12) { String::class.java },
            )

        val method0 = wrapperClazz.getDeclaredComposableMethod("OverloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                String::class.java,
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(10) { String::class.java },
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(11) { String::class.java },
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "OverloadedComposableMethod",
                *Array(12) { String::class.java },
            )

        val diffParameters =
            clazz.getDeclaredComposableMethod(
                "DifferentParametersTypes",
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            )

        val diffParametersMethod =
            wrapperClazz.getDeclaredComposableMethod(
                "DifferentParametersTypesMethod",
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            )

        assertEquals(0, function0.parameterTypes.size)
        assertEquals(1, function1.parameterTypes.size)
        assertEquals(10, function10.parameterTypes.size)
        assertEquals(11, function11.parameterTypes.size)
        assertEquals(12, function12.parameterTypes.size)
        assertEquals(12, function12.parameterTypes.size)

        assertEquals(0, method0.parameterTypes.size)
        assertEquals(1, method1.parameterTypes.size)
        assertEquals(10, method10.parameterTypes.size)
        assertEquals(11, method11.parameterTypes.size)
        assertEquals(12, method12.parameterTypes.size)

        assertEquals(0, composableMethod.asComposableMethod()!!.parameterTypes.size)

        assertEquals(6, diffParameters.parameterTypes.size)
        assertEquals(
            listOf(
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            ),
            diffParameters.parameterTypes.toList(),
        )

        assertEquals(6, diffParametersMethod.parameterTypes.size)
        assertEquals(
            listOf(
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java,
            ),
            diffParametersMethod.parameterTypes.toList(),
        )
    }

    private class TestFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
    }

    private fun <T> executeWithComposer(block: (composer: Composer) -> T): T =
        runBlocking(TestFrameClock()) {
            fun compose(recomposer: Recomposer, block: @Composable () -> Unit): Composition {
                return Composition(EmptyApplier(), recomposer).apply { setContent(block) }
            }

            var res: T? = null
            withRunningRecomposer { r -> compose(r) { res = block(currentComposer) } }
            res!!
        }

    @Test
    fun testInvokeComposableFunctions() {

        val composableWithDefaults =
            clazz.declaredMethods
                .find { it.name == "composableFunctionWithDefaults" }!!
                .asComposableMethod()!!
        composableWithDefaults.asMethod().isAccessible = true

        val resABAAA = executeWithComposer {
            composableWithDefaults.invoke(it, null, "a", "b") as String
        }

        val resABCAA = executeWithComposer {
            composableWithDefaults.invoke(it, null, "a", "b", "c") as String
        }

        val resABCDA = executeWithComposer {
            composableWithDefaults.invoke(it, null, "a", "b", "c", "d") as String
        }

        val resABCDE = executeWithComposer {
            composableWithDefaults.invoke(it, null, "a", "b", "c", "d", "e") as String
        }

        val resABADA = executeWithComposer {
            composableWithDefaults.invoke(it, null, "a", "b", null, "d") as String
        }

        assertEquals("abaaa", resABAAA)
        assertEquals("abcaa", resABCAA)
        assertEquals("abcda", resABCDA)
        assertEquals("abcde", resABCDE)
        assertEquals("abada", resABADA)
    }

    @Test
    fun testInvokeComposableMethods() {

        val composableWithDefaults =
            wrapperClazz.declaredMethods
                .find { it.name == "composableMethodWithDefaults" }!!
                .asComposableMethod()!!
        composableWithDefaults.asMethod().isAccessible = true

        val instance = ComposablesWrapper()

        val resABAAA = executeWithComposer {
            composableWithDefaults.invoke(it, instance, "a", "b") as String
        }

        val resABCAA = executeWithComposer {
            composableWithDefaults.invoke(it, instance, "a", "b", "c") as String
        }

        val resABCDA = executeWithComposer {
            composableWithDefaults.invoke(it, instance, "a", "b", "c", "d") as String
        }

        val resABCDE = executeWithComposer {
            composableWithDefaults.invoke(it, instance, "a", "b", "c", "d", "e") as String
        }

        val resABADA = executeWithComposer {
            composableWithDefaults.invoke(it, instance, "a", "b", null, "d") as String
        }

        assertEquals("abaaa", resABAAA)
        assertEquals("abcaa", resABCAA)
        assertEquals("abcda", resABCDA)
        assertEquals("abcde", resABCDE)
        assertEquals("abada", resABADA)
    }
}
