/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.room.compiler.processing.XNullability.NONNULL
import androidx.room.compiler.processing.XNullability.NULLABLE
import androidx.room.compiler.processing.XNullability.UNKNOWN
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.TypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XNullabilityTest {
    /**
     * This is important for javac
     */
    @Test
    fun elementInferredNullability() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;

            import androidx.annotation.*;
            import java.util.List;
            class Baz {
                public static int primitiveInt;
                public static Integer boxedInt;
                @NonNull
                public static List<String> nonNullAnnotated;
                @Nullable
                public static List<String> nullableAnnotated;
                @NonNull
                public String returnsNonNull() {
                    return "";
                }

                public String parameters(
                    int primitiveParam,
                    @Nullable String nullableParam,
                    @NonNull String nonNullParam,
                    List<String> unknown
                ) {
                    return "";
                }
            }
            """.trimIndent()
        )
        // TODO run with KSP once https://github.com/google/ksp/issues/167 is fixed
        runProcessorTestWithoutKsp(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("primitiveInt").let { field ->
                assertThat(field.type.nullability).isEqualTo(NONNULL)
            }
            element.getField("boxedInt").let { field ->
                assertThat(field.type.nullability).isEqualTo(UNKNOWN)
            }
            element.getField("nonNullAnnotated").let { field ->
                assertThat(field.type.nullability).isEqualTo(NONNULL)
            }
            element.getField("nullableAnnotated").let { field ->
                assertThat(field.type.nullability).isEqualTo(NULLABLE)
            }
            element.getMethod("returnsNonNull").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(NONNULL)
                assertThat(method.executableType.returnType.nullability)
                    .isEqualTo(NONNULL)
            }
            element.getMethod("parameters").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(UNKNOWN)
                method.getParameter("primitiveParam").let { param ->
                    assertThat(param.type.nullability).isEqualTo(NONNULL)
                }
                method.getParameter("nullableParam").let { param ->
                    assertThat(param.type.nullability).isEqualTo(NULLABLE)
                }
                method.getParameter("nonNullParam").let { param ->
                    assertThat(param.type.nullability).isEqualTo(NONNULL)
                }
                method.getParameter("unknown").let { param ->
                    assertThat(param.type.nullability).isEqualTo(UNKNOWN)
                }
            }
            // also assert parameter types from executable type
            element.getMethod("parameters").executableType.let { method ->
                assertThat(method.returnType.nullability).isEqualTo(UNKNOWN)
                // int primitiveParam,
                method.parameterTypes[0].let { paramType ->
                    assertThat(paramType.nullability).isEqualTo(NONNULL)
                }
                // @Nullable String nullableParam,
                method.parameterTypes[1].let { paramType ->
                    assertThat(paramType.nullability).isEqualTo(NULLABLE)
                }
                // @NonNull String nonNullParam,
                method.parameterTypes[2].let { paramType ->
                    assertThat(paramType.nullability).isEqualTo(NONNULL)
                }
                // List<String> unknown
                method.parameterTypes[3].let { paramType ->
                    assertThat(paramType.nullability).isEqualTo(UNKNOWN)
                }
            }
            // types inferred from type elements shouldn't have nullability
            assertThat(element.type.nullability).isEqualTo(UNKNOWN)
        }
    }

    @Test
    fun kotlinNullability() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            package foo.bar;

            import androidx.annotation.*;
            import java.util.List;
            class Baz(
                val intField: Int,
                val nullableIntField: Int?,
                val genericFieldWithNullableTypeParam: List<Int?>
            ) {
                fun nullableReturn(): Int? = TODO()
                suspend fun suspendNullableReturn(): Int? = TODO()
                fun genericWithNullableTypeArgReturn(): Map<String, Long?> = TODO()
                suspend fun suspendGenericWithNullableTypeArgReturn(): Map<String, Long?> = TODO()
                
                fun nonNullReturn(): Int = TODO()
                suspend fun suspendNonNullReturn(): Int = TODO()
                
                fun methodParams(
                    nonNull: Int,
                    nullable: Int?,
                    nullableGenericWithNonNullType: List<Int>?,
                    nullableGenericWithNullableType: List<Int?>?,
                    nonNullGenericWithNonNullType: List<Int>,
                    nonNullGenericWithNullableType: List<Int?>
                ) {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("intField").let { field ->
                assertThat(field.type.nullability).isEqualTo(NONNULL)
            }
            element.getField("nullableIntField").let { field ->
                assertThat(field.type.nullability).isEqualTo(NULLABLE)
            }
            element.getField("genericFieldWithNullableTypeParam").let { field ->
                assertThat(field.type.nullability).isEqualTo(NONNULL)
                val declared = field.type
                assertThat(declared.typeArguments.first().nullability).isEqualTo(NULLABLE)
            }
            element.getMethod("nullableReturn").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(NULLABLE)
                assertThat(method.executableType.returnType.nullability).isEqualTo(NULLABLE)
            }
            element.getMethod("suspendNullableReturn").let { method ->
                // kotlin adds @Nullable annotation for suspend methods' javac signature
                assertThat(method.returnType.nullability).isEqualTo(NULLABLE)
                val executableType = method.executableType
                check(executableType.isSuspendFunction())
                assertThat(executableType.returnType.nullability).isEqualTo(NULLABLE)
                assertThat(executableType.getSuspendFunctionReturnType().nullability)
                    .isEqualTo(NULLABLE)
            }
            element.getMethod("genericWithNullableTypeArgReturn").let { method ->
                listOf(method.returnType, method.executableType.returnType).forEach { type ->
                    assertThat(type.nullability).isEqualTo(NONNULL)
                    assertThat(type.typeArguments[0].nullability)
                        .isEqualTo(NONNULL)
                    assertThat(type.typeArguments[1].nullability)
                        .isEqualTo(NULLABLE)
                }
            }
            element.getMethod("suspendGenericWithNullableTypeArgReturn").let { method ->
                val executableType = method.executableType
                check(executableType.isSuspendFunction())
                executableType.getSuspendFunctionReturnType().let { type ->
                    assertThat(type.nullability).isEqualTo(NONNULL)
                    assertThat(type.typeArguments[0].nullability)
                        .isEqualTo(NONNULL)
                    assertThat(type.typeArguments[1].nullability)
                        .isEqualTo(NULLABLE)
                }
                listOf(method.returnType, executableType.returnType).forEach { type ->
                    // kotlin suspend functions return nullable in jvm stub
                    assertThat(type.nullability).isEqualTo(NULLABLE)
                    assertThat(type.typeArguments).isEmpty()
                }
            }
            element.getMethod("nonNullReturn").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(NONNULL)
                assertThat(method.executableType.returnType.nullability).isEqualTo(NONNULL)
            }
            element.getMethod("suspendNonNullReturn").let { method ->
                // suspend methods return nullable in java declarations
                assertThat(method.returnType.nullability).isEqualTo(NULLABLE)
                val executableType = method.executableType
                check(executableType.isSuspendFunction())
                assertThat(executableType.returnType.nullability).isEqualTo(NULLABLE)
                assertThat(executableType.getSuspendFunctionReturnType().nullability)
                    .isEqualTo(NONNULL)
            }
            element.getMethod("methodParams").let { method ->
                assertThat(method.getParameter("nonNull").type.nullability)
                    .isEqualTo(NONNULL)
                assertThat(method.getParameter("nullable").type.nullability)
                    .isEqualTo(NULLABLE)
                assertThat(
                    method.parameters.filter {
                        it.type.typeArguments.isNotEmpty()
                    }.map {
                        Triple(
                            first = it.name,
                            second = it.type.nullability,
                            third = it.type.typeArguments.single().nullability
                        )
                    }
                ).containsExactly(
                    Triple("nullableGenericWithNonNullType", NULLABLE, NONNULL),
                    Triple("nullableGenericWithNullableType", NULLABLE, NULLABLE),
                    Triple("nonNullGenericWithNonNullType", NONNULL, NONNULL),
                    Triple("nonNullGenericWithNullableType", NONNULL, NULLABLE)
                )
            }
        }
    }

    @Test
    fun changeNullability_primitives() {
        runProcessorTest { invocation ->
            PRIMITIVE_TYPES.forEach { primitiveTypeName ->
                val primitive = invocation.processingEnv.requireType(primitiveTypeName)
                assertThat(primitive.nullability).isEqualTo(NONNULL)
                val nullable = primitive.makeNullable()
                assertThat(nullable.nullability).isEqualTo(NULLABLE)
                assertThat(nullable.typeName).isEqualTo(primitiveTypeName.box())

                // When a boxed primitive is marked as non-null, it should stay as boxed primitive
                // Even though this might be counter-intutive (because making it nullable will box
                // it) it is more consistent as it is completely valid to annotate a boxed primitive
                // with non-null while you cannot annoteted a primitive with nullable as it is not
                // a valid state.
                val boxedPrimitive = invocation.processingEnv.requireType(primitiveTypeName.box())
                val nonNull = boxedPrimitive.makeNonNullable()
                assertThat(nonNull.nullability).isEqualTo(NONNULL)
                assertThat(nonNull.typeName).isEqualTo(primitiveTypeName.box())
            }
        }
    }

    @Test
    fun changeNullability_typeArguments() {
        // we need to make sure we don't convert type arguments into primitives!!
        val kotlinSrc = Source.kotlin(
            "KotlinClas.kt",
            """
                class KotlinClass(val subject: List<Int?>)
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "JavaClass",
            """
                class JavaClass {
                    java.util.List<Integer> subject;
                }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(javaSrc, kotlinSrc)) { invocation ->
            listOf("KotlinClass", "JavaClass").forEach {
                val subject = invocation.processingEnv.requireTypeElement(it)
                    .getField("subject").type
                val typeArg = subject.typeArguments.first()
                assertThat(typeArg.typeName).isEqualTo(TypeName.INT.box())
                typeArg.makeNonNullable().let {
                    assertThat(it.typeName).isEqualTo(TypeName.INT.box())
                    assertThat(it.nullability).isEqualTo(NONNULL)
                }
                typeArg.makeNonNullable().makeNullable().let {
                    assertThat(it.typeName).isEqualTo(TypeName.INT.box())
                    assertThat(it.nullability).isEqualTo(NULLABLE)
                }
            }
        }
    }

    @Test
    fun changeNullability_declared() {
        runProcessorTest { invocation ->
            val subject = invocation.processingEnv.requireType("java.util.List")
            subject.makeNullable().let {
                assertThat(it.nullability).isEqualTo(NULLABLE)
            }
            subject.makeNonNullable().let {
                assertThat(it.nullability).isEqualTo(NONNULL)
            }
            // ksp defaults to non-null so we do double conversion here to ensure it flips
            // nullability
            subject.makeNullable().makeNonNullable().let {
                assertThat(it.nullability).isEqualTo(NONNULL)
            }
        }
    }

    @Test
    fun changeNullability_arrayTypes() {
        runProcessorTest { invocation ->
            val subject = invocation.processingEnv.getArrayType(
                invocation.processingEnv.requireType("java.util.List")
            )
            subject.makeNullable().let {
                assertThat(it.nullability).isEqualTo(NULLABLE)
                assertThat(it.isArray()).isTrue()
            }
            subject.makeNonNullable().let {
                assertThat(it.nullability).isEqualTo(NONNULL)
                assertThat(it.isArray()).isTrue()
            }
            // ksp defaults to non-null so we do double conversion here to ensure it flips
            // nullability
            subject.makeNullable().makeNonNullable().let {
                assertThat(it.nullability).isEqualTo(NONNULL)
                assertThat(it.isArray()).isTrue()
            }
        }
    }

    @Test
    fun makeNullable_void() {
        val src = Source.java(
            "Foo.java",
            """
            class Foo {
                void subject() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val voidType = invocation.processingEnv.requireTypeElement("Foo")
                .getMethod("subject").returnType
            assertThat(voidType.typeName).isEqualTo(TypeName.VOID)
            voidType.makeNullable().let {
                assertThat(it.nullability).isEqualTo(NULLABLE)
                assertThat(it.typeName).isEqualTo(TypeName.VOID.box())
            }
        }
    }

    companion object {
        val PRIMITIVE_TYPES = listOf(
            TypeName.BOOLEAN,
            TypeName.BYTE,
            TypeName.SHORT,
            TypeName.INT,
            TypeName.LONG,
            TypeName.CHAR,
            TypeName.FLOAT,
            TypeName.DOUBLE,
        )
    }
}
