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
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
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
        runProcessorTest(
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
                val declared = field.type.asDeclaredType()
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
                    assertThat(type.asDeclaredType().typeArguments[0].nullability)
                        .isEqualTo(NONNULL)
                    assertThat(type.asDeclaredType().typeArguments[1].nullability)
                        .isEqualTo(NULLABLE)
                }
            }
            element.getMethod("suspendGenericWithNullableTypeArgReturn").let { method ->
                val executableType = method.executableType
                check(executableType.isSuspendFunction())
                executableType.getSuspendFunctionReturnType().let { type ->
                    assertThat(type.nullability).isEqualTo(NONNULL)
                    assertThat(type.asDeclaredType().typeArguments[0].nullability)
                        .isEqualTo(NONNULL)
                    assertThat(type.asDeclaredType().typeArguments[1].nullability)
                        .isEqualTo(NULLABLE)
                }
                listOf(method.returnType, executableType.returnType).forEach { type ->
                    // kotlin suspend functions return nullable in jvm stub
                    assertThat(type.nullability).isEqualTo(NULLABLE)
                    assertThat(type.asDeclaredType().typeArguments).isEmpty()
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
                        it.type.isDeclared() && it.type.asDeclaredType().typeArguments.isNotEmpty()
                    }.map {
                        Triple(
                            first = it.name,
                            second = it.type.nullability,
                            third = it.type.asDeclaredType().typeArguments.single().nullability
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
}
