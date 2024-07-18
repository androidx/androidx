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

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XTypeParameterElementTest {

    @Test
    fun classTypeParameters() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo<T1, T2>
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")

            assertThat(foo.typeParameters).hasSize(2)
            val t1 = foo.typeParameters[0]
            val t2 = foo.typeParameters[1]
            assertThat(t1.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.bounds).isEmpty()
            assertThat(t2.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.bounds).isEmpty()

            // Note: Javac and KSP have different default types when no bounds are provided.
            val expectedBoundType = if (invocation.isKsp) {
                invocation.processingEnv.requireType("kotlin.Any").makeNullable()
            } else {
                invocation.processingEnv.requireType("java.lang.Object")
            }

            assertThat(t1.bounds).hasSize(1)
            val t1Bound = t1.bounds[0]
            assertThat(t1Bound.asTypeName().java.toString()).isEqualTo("java.lang.Object")
            if (invocation.isKsp) {
                assertThat(t1Bound.asTypeName().kotlin.toString()).isEqualTo("kotlin.Any?")
            }
            assertThat(t1Bound.isSameType(expectedBoundType)).isTrue()

            assertThat(t2.bounds).hasSize(1)
            val t2Bound = t2.bounds[0]
            assertThat(t2Bound.asTypeName().java.toString()).isEqualTo("java.lang.Object")
            if (invocation.isKsp) {
                assertThat(t2Bound.asTypeName().kotlin.toString()).isEqualTo("kotlin.Any?")
            }
            assertThat(t2Bound.isSameType(expectedBoundType)).isTrue()
        }
    }

    @Test
    fun classTypeParametersWithBounds() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo<T1 : Bar, T2 : Baz?>
            open class Bar
            open class Baz
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")

            assertThat(foo.typeParameters).hasSize(2)
            val t1 = foo.typeParameters[0]
            val t2 = foo.typeParameters[1]
            assertThat(t1.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.bounds.map { it.toString() }).containsExactly("Bar")
            assertThat(t2.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.bounds.map { it.toString() }).containsExactly("Baz")

            assertThat(t1.bounds).hasSize(1)
            val t1Bound = t1.bounds[0]
            assertThat(t1Bound.asTypeName().java.toString()).isEqualTo("Bar")
            if (invocation.isKsp) {
                assertThat(t1Bound.asTypeName().kotlin.toString()).isEqualTo("Bar")
            }
            val bar = invocation.processingEnv.requireType("Bar")
            assertThat(t1Bound.isSameType(bar)).isTrue()

            assertThat(t2.bounds).hasSize(1)
            val t2Bound = t2.bounds[0]
            assertThat(t2Bound.asTypeName().java.toString()).isEqualTo("Baz")
            if (invocation.isKsp) {
                assertThat(t2Bound.asTypeName().kotlin.toString()).isEqualTo("Baz?")
            }
            val nullableBar = invocation.processingEnv.requireType("Baz").makeNullable()
            assertThat(t2Bound.isSameType(nullableBar)).isTrue()
        }
    }

    @Test
    fun classTypeParametersWithInOut() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo<in T1 : Bar?, out T2 : Baz>
            open class Bar
            open class Baz
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")

            assertThat(foo.typeParameters).hasSize(2)
            val t1 = foo.typeParameters[0]
            val t2 = foo.typeParameters[1]
            assertThat(t1.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.bounds.map { it.toString() }).containsExactly("Bar")
            assertThat(t2.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.bounds.map { it.toString() }).containsExactly("Baz")

            assertThat(t1.bounds).hasSize(1)
            val t1Bound = t1.bounds[0]
            assertThat(t1Bound.asTypeName().java.toString()).isEqualTo("Bar")
            if (invocation.isKsp) {
                assertThat(t1Bound.asTypeName().kotlin.toString()).isEqualTo("Bar?")
            }
            val nullableBar = invocation.processingEnv.requireType("Bar").makeNullable()
            assertThat(t1Bound.isSameType(nullableBar)).isTrue()

            assertThat(t2.bounds).hasSize(1)
            val t2Bound = t2.bounds[0]
            assertThat(t2Bound.asTypeName().java.toString()).isEqualTo("Baz")
            if (invocation.isKsp) {
                assertThat(t2Bound.asTypeName().kotlin.toString()).isEqualTo("Baz")
            }
            val baz = invocation.processingEnv.requireType("Baz")
            assertThat(t2Bound.isSameType(baz)).isTrue()
        }
    }

    @Test
    fun javaClassTypeParametersWithExtends() {
        val src = Source.java(
            "Foo",
            """
            class Foo<T extends Bar & Baz> {}
            class Bar {}
            interface Baz {}
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")

            assertThat(foo.typeParameters).hasSize(1)
            val t = foo.typeParameters[0]
            assertThat(t.name).isEqualTo("T")
            assertThat(t.typeVariableName.name).isEqualTo("T")
            assertThat(t.typeVariableName.bounds.map { it.toString() })
                .containsExactly("Bar", "Baz")
            assertThat(t.bounds).hasSize(2)

            val bound0 = t.bounds[0]
            assertThat(bound0.asTypeName().java.toString()).isEqualTo("Bar")
            if (invocation.isKsp) {
                assertThat(bound0.asTypeName().kotlin.toString()).isEqualTo("Bar")
            }
            val bar = invocation.processingEnv.requireType("Bar")
            assertThat(bound0.isSameType(bar)).isTrue()
            assertThat(bound0.nullability).isEqualTo(XNullability.UNKNOWN)

            val bound1 = t.bounds[1]
            assertThat(bound1.asTypeName().java.toString()).isEqualTo("Baz")
            if (invocation.isKsp) {
                assertThat(bound1.asTypeName().kotlin.toString()).isEqualTo("Baz")
            }
            val baz = invocation.processingEnv.requireType("Baz")
            assertThat(bound1.isSameType(baz)).isTrue()
            assertThat(bound1.nullability).isEqualTo(XNullability.UNKNOWN)
        }
    }

    @Test
    fun methodTypeParameters() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo {
              fun <T1, T2> someMethod(t1: T1, t2: T2) {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")
            val methods = foo.getDeclaredMethods()
            assertThat(methods).hasSize(1)

            val method = methods[0]
            assertThat(method.jvmName).isEqualTo("someMethod")
            assertThat(method.typeParameters).hasSize(2)

            val t1 = method.typeParameters[0]
            val t2 = method.typeParameters[1]
            assertThat(t1.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.bounds).isEmpty()
            assertThat(t2.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.bounds).isEmpty()

            // Note: Javac and KSP have different default types when no bounds are provided.
            val expectedBoundType = if (invocation.isKsp) {
                invocation.processingEnv.requireType("kotlin.Any").makeNullable()
            } else {
                invocation.processingEnv.requireType("java.lang.Object")
            }

            assertThat(t1.bounds).hasSize(1)
            val t1Bound = t1.bounds[0]
            assertThat(t1Bound.asTypeName().java.toString()).isEqualTo("java.lang.Object")
            if (invocation.isKsp) {
                assertThat(t1Bound.asTypeName().kotlin.toString()).isEqualTo("kotlin.Any?")
            }
            assertThat(t1Bound.isSameType(expectedBoundType)).isTrue()

            assertThat(t2.bounds).hasSize(1)
            val t2Bound = t2.bounds[0]
            assertThat(t2Bound.asTypeName().java.toString()).isEqualTo("java.lang.Object")
            if (invocation.isKsp) {
                assertThat(t2Bound.asTypeName().kotlin.toString()).isEqualTo("kotlin.Any?")
            }
            assertThat(t2Bound.isSameType(expectedBoundType)).isTrue()
        }
    }

    @Test
    fun methodTypeParametersWithBounds() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Foo {
              fun <T1 : Bar, T2 : Baz?> someMethod(t1: T1, t2: T2) {}
            }
            open class Bar
            open class Baz
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")
            val methods = foo.getDeclaredMethods()
            assertThat(methods).hasSize(1)

            val method = methods[0]
            assertThat(method.jvmName).isEqualTo("someMethod")
            assertThat(method.typeParameters).hasSize(2)

            val t1 = method.typeParameters[0]
            val t2 = method.typeParameters[1]
            assertThat(t1.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.name).isEqualTo("T1")
            assertThat(t1.typeVariableName.bounds.map { it.toString() }).containsExactly("Bar")
            assertThat(t2.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.name).isEqualTo("T2")
            assertThat(t2.typeVariableName.bounds.map { it.toString() }).containsExactly("Baz")

            assertThat(t1.bounds).hasSize(1)
            val t1Bound = t1.bounds[0]
            assertThat(t1Bound.asTypeName().java.toString()).isEqualTo("Bar")
            if (invocation.isKsp) {
                assertThat(t1Bound.asTypeName().kotlin.toString()).isEqualTo("Bar")
            }
            val bar = invocation.processingEnv.requireType("Bar")
            assertThat(t1Bound.isSameType(bar)).isTrue()

            assertThat(t2.bounds).hasSize(1)
            val t2Bound = t2.bounds[0]
            assertThat(t2Bound.asTypeName().java.toString()).isEqualTo("Baz")
            if (invocation.isKsp) {
                assertThat(t2Bound.asTypeName().kotlin.toString()).isEqualTo("Baz?")
            }
            val nullableBar = invocation.processingEnv.requireType("Baz").makeNullable()
            assertThat(t2Bound.isSameType(nullableBar)).isTrue()
        }
    }

    @Test
    fun javaMethodTypeParametersWithExtends() {
        val src = Source.java(
            "Foo",
            """
            class Foo {
              <T extends Bar & Baz> void someMethod(T t) {}
            }
            class Bar {}
            interface Baz {}
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")
            val methods = foo.getDeclaredMethods()
            assertThat(methods).hasSize(1)

            val method = methods[0]
            assertThat(method.jvmName).isEqualTo("someMethod")
            assertThat(method.typeParameters).hasSize(1)

            val t = method.typeParameters[0]
            assertThat(t.name).isEqualTo("T")
            assertThat(t.typeVariableName.name).isEqualTo("T")
            assertThat(t.typeVariableName.bounds.map { it.toString() })
                .containsExactly("Bar", "Baz")
            assertThat(t.bounds).hasSize(2)

            val bound0 = t.bounds[0]
            assertThat(bound0.asTypeName().java.toString()).isEqualTo("Bar")
            if (invocation.isKsp) {
                assertThat(bound0.asTypeName().kotlin.toString()).isEqualTo("Bar")
            }
            val bar = invocation.processingEnv.requireType("Bar")
            assertThat(bound0.isSameType(bar)).isTrue()
            assertThat(bound0.nullability).isEqualTo(XNullability.UNKNOWN)

            val bound1 = t.bounds[1]
            assertThat(bound1.asTypeName().java.toString()).isEqualTo("Baz")
            if (invocation.isKsp) {
                assertThat(bound1.asTypeName().kotlin.toString()).isEqualTo("Baz")
            }
            val baz = invocation.processingEnv.requireType("Baz")
            assertThat(bound1.isSameType(baz)).isTrue()
            assertThat(bound1.nullability).isEqualTo(XNullability.UNKNOWN)
        }
    }

    // Note: constructor type parameters are only allowed in Java sources.
    @Test
    fun javaConstructorTypeParametersWithExtends() {
        val src = Source.java(
            "Foo",
            """
            class Foo {
              <T extends Bar & Baz> Foo(T t) {}
            }
            class Bar {}
            interface Baz {}
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")
            val constructors = foo.getConstructors()
            assertThat(constructors).hasSize(1)

            val constructor = constructors[0]
            assertThat(constructor.typeParameters).hasSize(1)

            val t = constructor.typeParameters[0]
            assertThat(t.name).isEqualTo("T")
            assertThat(t.typeVariableName.name).isEqualTo("T")
            assertThat(t.typeVariableName.bounds.map { it.toString() })
                .containsExactly("Bar", "Baz")
            assertThat(t.bounds).hasSize(2)

            val bound0 = t.bounds[0]
            assertThat(bound0.asTypeName().java.toString()).isEqualTo("Bar")
            if (invocation.isKsp) {
                assertThat(bound0.asTypeName().kotlin.toString()).isEqualTo("Bar")
            }
            val bar = invocation.processingEnv.requireType("Bar")
            assertThat(bound0.isSameType(bar)).isTrue()
            assertThat(bound0.nullability).isEqualTo(XNullability.UNKNOWN)

            val bound1 = t.bounds[1]
            assertThat(bound1.asTypeName().java.toString()).isEqualTo("Baz")
            if (invocation.isKsp) {
                assertThat(bound1.asTypeName().kotlin.toString()).isEqualTo("Baz")
            }
            val baz = invocation.processingEnv.requireType("Baz")
            assertThat(bound1.isSameType(baz)).isTrue()
            assertThat(bound1.nullability).isEqualTo(XNullability.UNKNOWN)

            assertThat(constructor.parameters).hasSize(1)
            val parameter = constructor.parameters[0]
            assertThat(parameter.name).isEqualTo("t")
            assertThat(parameter.type.asTypeName().java.toString()).isEqualTo("T")
            if (invocation.isKsp) {
                assertThat(parameter.type.asTypeName().kotlin.toString()).isEqualTo("T")
            }
        }
    }
}
