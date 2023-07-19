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
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.javapoet.JTypeName
import org.junit.Assert.fail
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
            element.getMethodByJvmName("returnsNonNull").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(NONNULL)
                assertThat(method.executableType.returnType.nullability)
                    .isEqualTo(NONNULL)
            }
            element.getMethodByJvmName("parameters").let { method ->
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
            element.getMethodByJvmName("parameters").executableType.let { method ->
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
            element.getMethodByJvmName("nullableReturn").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(NULLABLE)
                assertThat(method.executableType.returnType.nullability).isEqualTo(NULLABLE)
            }
            element.getMethodByJvmName("suspendNullableReturn").let { method ->
                // kotlin adds @Nullable annotation for suspend methods' javac signature
                assertThat(method.returnType.nullability).isEqualTo(NULLABLE)
                val executableType = method.executableType
                check(executableType.isSuspendFunction())
                assertThat(executableType.returnType.nullability).isEqualTo(NULLABLE)
                assertThat(executableType.getSuspendFunctionReturnType().nullability)
                    .isEqualTo(NULLABLE)
            }
            element.getMethodByJvmName("genericWithNullableTypeArgReturn").let { method ->
                listOf(method.returnType, method.executableType.returnType).forEach { type ->
                    assertThat(type.nullability).isEqualTo(NONNULL)
                    assertThat(type.typeArguments[0].nullability)
                        .isEqualTo(NONNULL)
                    assertThat(type.typeArguments[1].nullability)
                        .isEqualTo(NULLABLE)
                }
            }
            element.getMethodByJvmName("suspendGenericWithNullableTypeArgReturn").let { method ->
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
            element.getMethodByJvmName("nonNullReturn").let { method ->
                assertThat(method.returnType.nullability).isEqualTo(NONNULL)
                assertThat(method.executableType.returnType.nullability).isEqualTo(NONNULL)
            }
            element.getMethodByJvmName("suspendNonNullReturn").let { method ->
                // suspend methods return nullable in java declarations
                assertThat(method.returnType.nullability).isEqualTo(NULLABLE)
                val executableType = method.executableType
                check(executableType.isSuspendFunction())
                assertThat(executableType.returnType.nullability).isEqualTo(NULLABLE)
                assertThat(executableType.getSuspendFunctionReturnType().nullability)
                    .isEqualTo(NONNULL)
            }
            element.getMethodByJvmName("methodParams").let { method ->
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
            PRIMITIVE_JTYPE_NAMES.forEachIndexed { index, primitiveJTypeName ->
                val primitive = invocation.processingEnv.requireType(primitiveJTypeName)
                assertThat(primitive.nullability).isEqualTo(NONNULL)
                val nullable = primitive.makeNullable()
                assertThat(nullable.nullability).isEqualTo(NULLABLE)
                assertThat(nullable.asTypeName().java).isEqualTo(primitiveJTypeName.box())
                if (invocation.isKsp) {
                    assertThat(nullable.asTypeName().kotlin)
                        .isEqualTo(PRIMITIVE_KTYPE_NAMES[index].copy(nullable = true))
                }

                // When a boxed primitive is marked as non-null, it should stay as boxed primitive
                // Even though this might be counter-intutive (because making it nullable will box
                // it) it is more consistent as it is completely valid to annotate a boxed primitive
                // with non-null while you cannot annoteted a primitive with nullable as it is not
                // a valid state.
                val boxedPrimitive = invocation.processingEnv.requireType(primitiveJTypeName.box())
                val nonNull = boxedPrimitive.makeNonNullable()
                assertThat(nonNull.nullability).isEqualTo(NONNULL)
                assertThat(nonNull.asTypeName().java).isEqualTo(primitiveJTypeName.box())
                if (invocation.isKsp) {
                    assertThat(nonNull.asTypeName().kotlin).isEqualTo(PRIMITIVE_KTYPE_NAMES[index])
                }
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
                assertThat(typeArg.asTypeName().java).isEqualTo(JTypeName.INT.box())
                typeArg.makeNonNullable().let {
                    assertThat(it.asTypeName().java).isEqualTo(JTypeName.INT.box())
                    assertThat(it.nullability).isEqualTo(NONNULL)
                }
                typeArg.makeNonNullable().makeNullable().let {
                    assertThat(it.asTypeName().java).isEqualTo(JTypeName.INT.box())
                    assertThat(it.nullability).isEqualTo(NULLABLE)
                }
                if (invocation.isKsp) {
                    assertThat(typeArg.asTypeName().kotlin).isEqualTo(
                        when (it) {
                            "KotlinClass" -> INT.copy(nullable = true)
                            // A type arg from Java has unknown nullability,
                            // so name defaults to not-null
                            "JavaClass" -> INT
                            else -> fail("Unknown src $it")
                        }
                    )

                    typeArg.makeNonNullable().let {
                        assertThat(it.asTypeName().kotlin).isEqualTo(INT)
                    }
                    typeArg.makeNonNullable().makeNullable().let {
                        assertThat(it.asTypeName().kotlin).isEqualTo(INT.copy(nullable = true))
                    }
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
            "Foo",
            """
            class Foo {
                void subject() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val voidType = invocation.processingEnv.requireTypeElement("Foo")
                .getMethodByJvmName("subject").returnType
            assertThat(voidType.asTypeName().java).isEqualTo(JTypeName.VOID)
            voidType.makeNullable().let {
                assertThat(it.nullability).isEqualTo(NULLABLE)
                assertThat(it.asTypeName().java).isEqualTo(JTypeName.VOID.box())
            }
            if (invocation.isKsp) {
                assertThat(voidType.asTypeName().kotlin).isEqualTo(UNIT)
                voidType.makeNullable().let {
                    // `Unit?` does not make sense so XTypeName's KotlinPoet is non-null Unit
                    assertThat(it.asTypeName().kotlin).isEqualTo(UNIT)
                }
            }
        }
    }

    companion object {
        val PRIMITIVE_JTYPE_NAMES = listOf(
            JTypeName.BOOLEAN,
            JTypeName.BYTE,
            JTypeName.SHORT,
            JTypeName.INT,
            JTypeName.LONG,
            JTypeName.CHAR,
            JTypeName.FLOAT,
            JTypeName.DOUBLE,
        )

        val PRIMITIVE_KTYPE_NAMES = listOf(
            com.squareup.kotlinpoet.BOOLEAN,
            com.squareup.kotlinpoet.BYTE,
            com.squareup.kotlinpoet.SHORT,
            com.squareup.kotlinpoet.INT,
            com.squareup.kotlinpoet.LONG,
            com.squareup.kotlinpoet.CHAR,
            com.squareup.kotlinpoet.FLOAT,
            com.squareup.kotlinpoet.DOUBLE,
        )
    }
}
