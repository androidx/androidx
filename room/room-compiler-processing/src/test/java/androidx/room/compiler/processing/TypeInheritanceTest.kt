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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeInheritanceTest {
    private fun runTest(
        fooClass: String,
        barClass: String,
        bazClass: String,
        handler: (XTestInvocation) -> Unit,
    ) {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class SubClass : BaseClass<Baz, Bar<Baz>>()
            open class BaseClass<T1, T2> {
                val valField : Foo<Bar<Baz>> = TODO()
                val valFieldT1 : Foo<Bar<T1>> = TODO()
                val valFieldT2 : Foo<T2> = TODO()
                var varField : Foo<Bar<Baz>> = TODO()
                var varFieldT1 : Foo<Bar<T1>> = TODO()
                var varFieldT2 : Foo<T2> = TODO()

                fun method(param : Foo<Bar<Baz>>, paramT1 : Foo<Bar<T1>>, paramT2 : Foo<T2>) {}
                fun methodReturn(): Foo<Bar<Baz>> = TODO()
                fun methodReturnT1(): Foo<Bar<T1>> = TODO()
                fun methodReturnT2(): Foo<T2> = TODO()
            }
            $fooClass
            $barClass
            $bazClass
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src), handler = handler)
    }

    private fun XTestInvocation.assertFieldType(
        fieldName: String,
        expectedJTypeName: String,
    ) {
        val expectedKTypeName = expectedJTypeName.replace("? extends", "out")
        val sub = processingEnv.requireTypeElement("SubClass")
        val subField = sub.getField(fieldName).asMemberOf(sub.type)
        assertThat(subField.asTypeName().java.toString()).isEqualTo(expectedJTypeName)
        if (isKsp) {
            assertThat(subField.asTypeName().kotlin.toString()).isEqualTo(expectedKTypeName)
        }

        val base = processingEnv.requireTypeElement("BaseClass")
        val baseField = base.getField(fieldName).asMemberOf(sub.type)
        assertThat(baseField.asTypeName().java.toString()).isEqualTo(expectedJTypeName)
        if (isKsp) {
            assertThat(baseField.asTypeName().kotlin.toString()).isEqualTo(expectedKTypeName)
        }
    }

    private fun XTestInvocation.assertParamType(
        methodName: String,
        paramName: String,
        expectedJTypeName: String,
    ) {
        val expectedKTypeName = expectedJTypeName.replace("? extends", "out")
        val sub = processingEnv.requireTypeElement("SubClass")
        val subMethod = sub.getMethodByJvmName(methodName)
        val paramIndex = subMethod.parameters.indexOf(subMethod.getParameter(paramName))
        val subMethodParam = subMethod.asMemberOf(sub.type).parameterTypes[paramIndex]
        assertThat(subMethodParam.asTypeName().java.toString()).isEqualTo(expectedJTypeName)
        if (isKsp) {
            assertThat(subMethodParam.asTypeName().kotlin.toString()).isEqualTo(expectedKTypeName)
        }

        val base = processingEnv.requireTypeElement("BaseClass")
        val baseMethod = base.getMethodByJvmName(methodName)
        val baseMethodParam = baseMethod.asMemberOf(sub.type).parameterTypes[paramIndex]
        assertThat(baseMethodParam.asTypeName().java.toString()).isEqualTo(expectedJTypeName)
        if (isKsp) {
            assertThat(baseMethodParam.asTypeName().kotlin.toString()).isEqualTo(expectedKTypeName)
        }
    }

    private fun XTestInvocation.assertReturnType(methodName: String, expectedTypeName: String) {
        val sub = processingEnv.requireTypeElement("SubClass")
        val subMethod = sub.getMethodByJvmName(methodName).asMemberOf(sub.type)
        assertThat(subMethod.returnType.typeName.toString()).isEqualTo(expectedTypeName)

        val base = processingEnv.requireTypeElement("BaseClass")
        val baseMethod = base.getMethodByJvmName(methodName).asMemberOf(sub.type)
        assertThat(baseMethod.returnType.typeName.toString()).isEqualTo(expectedTypeName)
    }

    @Test
    fun test_Foo_Bar_Baz() {
        runTest(
            "class Foo<V>",
            "class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_Bar_Baz() {
        runTest(
            "open class Foo<V>",
            "class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_Foo_OpenBar_Baz() {
        runTest(
            "class Foo<V>",
            "open class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_Foo_Bar_OpenBaz() {
        runTest(
            "class Foo<V>",
            "class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_OpenBar_Baz() {
        runTest(
            "open class Foo<V>",
            "open class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_Bar_OpenBaz() {
        runTest(
            "open class Foo<V>",
            "class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_Foo_OpenBar_OpenBaz() {
        runTest(
            "class Foo<V>",
            "open class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_OpenBar_OpenBaz() {
        runTest(
            "open class Foo<V>",
            "open class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_FooOut_Bar_Baz() {
        runTest(
            "class Foo<out V>",
            "class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_Bar_Baz() {
        runTest(
            "open class Foo<out V>",
            "class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_FooOut_OpenBar_Baz() {
        runTest(
            "class Foo<out V>",
            "open class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_FooOut_Bar_OpenBaz() {
        runTest(
            "class Foo<out V>",
            "class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_OpenBar_Baz() {
        runTest(
            "open class Foo<out V>",
            "open class Bar<V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_Bar_OpenBaz() {
        runTest(
            "open class Foo<out V>",
            "class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_FooOut_OpenBar_OpenBaz() {
        runTest(
            "class Foo<out V>",
            "open class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_OpenBar_OpenBaz() {
        runTest(
            "open class Foo<out V>",
            "open class Bar<V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<Baz>>")
        }
    }

    @Test
    fun test_Foo_BarOut_Baz() {
        runTest(
            "class Foo<V>",
            "class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_BarOut_Baz() {
        runTest(
            "open class Foo<V>",
            "class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_Foo_OpenBarOut_Baz() {
        runTest(
            "class Foo<V>",
            "open class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_Foo_BarOut_OpenBaz() {
        runTest(
            "class Foo<V>",
            "class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_OpenBarOut_Baz() {
        runTest(
            "open class Foo<V>",
            "open class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_BarOut_OpenBaz() {
        runTest(
            "open class Foo<V>",
            "class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_Foo_OpenBarOut_OpenBaz() {
        runTest(
            "class Foo<V>",
            "open class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFoo_OpenBarOut_OpenBaz() {
        runTest(
            "open class Foo<V>",
            "open class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_FooOut_BarOut_Baz() {
        runTest(
            "class Foo<out V>",
            "class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_BarOut_Baz() {
        runTest(
            "open class Foo<out V>",
            "class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_FooOut_OpenBarOut_Baz() {
        runTest(
            "class Foo<out V>",
            "open class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_FooOut_BarOut_OpenBaz() {
        runTest(
            "class Foo<out V>",
            "class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_OpenBarOut_Baz() {
        runTest(
            "open class Foo<out V>",
            "open class Bar<out V>",
            "class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_BarOut_OpenBaz() {
        runTest(
            "open class Foo<out V>",
            "class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_FooOut_OpenBarOut_OpenBaz() {
        runTest(
            "class Foo<out V>",
            "open class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }

    @Test
    fun test_OpenFooOut_OpenBarOut_OpenBaz() {
        runTest(
            "open class Foo<out V>",
            "open class Bar<out V>",
            "open class Baz",
        ) { invocation ->
            invocation.assertFieldType("valField", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT1", "Foo<Bar<Baz>>")
            invocation.assertFieldType("valFieldT2", "Foo<Bar<? extends Baz>>")
            invocation.assertFieldType("varField", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertFieldType("varFieldT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "param", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT1", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertParamType("method", "paramT2", "Foo<? extends Bar<? extends Baz>>")
            invocation.assertReturnType("methodReturn", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT1", "Foo<Bar<Baz>>")
            invocation.assertReturnType("methodReturnT2", "Foo<Bar<? extends Baz>>")
        }
    }
}