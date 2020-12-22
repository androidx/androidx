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

import androidx.room.compiler.processing.util.CONTINUATION_CLASS_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.UNIT_CLASS_NAME
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import org.junit.Test

class XExecutableTypeTest {
    @Test
    fun inheritanceResolution() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface<T> {
                fun getT(): T
                fun setT(t:T): Unit
                suspend fun suspendGetT(): T
                suspend fun suspendSetT(t:T): Unit
            }
            abstract class Subject : MyInterface<String>
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")

            // helper method to get executable types both from sub class and also as direct child of
            // the given type
            fun checkMethods(
                methodName: String,
                vararg subjects: XTypeElement,
                callback: (XMethodType) -> Unit
            ) {
                assertThat(subjects).isNotEmpty()
                subjects.forEach {
                    callback(myInterface.getMethod(methodName).asMemberOf(it.type))
                    callback(it.getMethod(methodName).executableType)
                }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            checkMethods("getT", subject) { type ->
                assertThat(type.parameterTypes).isEmpty()
                assertThat(type.returnType.typeName).isEqualTo(String::class.typeName())
            }
            checkMethods("setT", subject) { type ->
                assertThat(type.parameterTypes).containsExactly(
                    invocation.processingEnv.requireType(String::class)
                )
                assertThat(type.returnType.typeName).isEqualTo(TypeName.VOID)
            }
            checkMethods("suspendGetT", subject) { type ->
                assertThat(type.parameterTypes.first().typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        CONTINUATION_CLASS_NAME,
                        WildcardTypeName.supertypeOf(String::class.java)
                    )
                )
                assertThat(type.returnType.typeName).isEqualTo(TypeName.OBJECT)
            }
            checkMethods("suspendSetT", subject) { type ->
                assertThat(type.parameterTypes.first().typeName).isEqualTo(
                    String::class.typeName()
                )
                assertThat(type.parameterTypes[1].typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        CONTINUATION_CLASS_NAME,
                        WildcardTypeName.supertypeOf(UNIT_CLASS_NAME)
                    )
                )
                assertThat(type.returnType.typeName).isEqualTo(TypeName.OBJECT)
            }
        }
    }

    @Test
    fun kotlinPropertyInheritance() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            interface MyInterface<T> {
                val immutableT: T
                var mutableT: T?
                val list: List<T>
                val nullableList: List<T?>
            }
            abstract class Subject : MyInterface<String>
            abstract class NullableSubject: MyInterface<String?>
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")

            // helper method to get executable types both from sub class and also as direct child of
            // the given type
            fun checkMethods(
                methodName: String,
                vararg subjects: XTypeElement,
                callback: (XMethodType) -> Unit
            ) {
                assertThat(subjects).isNotEmpty()
                subjects.forEach {
                    callback(myInterface.getMethod(methodName).asMemberOf(it.type))
                    callback(it.getMethod(methodName).executableType)
                }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            checkMethods("getImmutableT", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NONNULL)
                }

                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }
            checkMethods("getMutableT", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                }
                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }
            checkMethods("setMutableT", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(TypeName.VOID)
                assertThat(method.parameterTypes.first().nullability)
                    .isEqualTo(XNullability.NULLABLE)
                assertThat(method.parameterTypes.first().typeName)
                    .isEqualTo(String::class.typeName())
                assertThat(method.typeVariableNames).isEmpty()
            }
            checkMethods("getList", subject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.java,
                        String::class.java
                    )
                )
                assertThat(method.returnType.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                assertThat(method.returnType.typeArguments.first().extendsBound()).isNull()
                if (invocation.isKsp) {
                    // kapt cannot read type parameter nullability yet
                    assertThat(
                        method.returnType.typeArguments.first().nullability
                    ).isEqualTo(
                        XNullability.NONNULL
                    )
                }
            }

            val nullableSubject = invocation.processingEnv.requireTypeElement("NullableSubject")
            // check that nullability is inferred from type parameters as well
            checkMethods("getImmutableT", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                }
                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }

            checkMethods("getMutableT", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(String::class.typeName())
                if (invocation.isKsp) {
                    // we don't get proper nullable here for kapt
                    // partially related to b/169629272
                    assertThat(method.returnType.nullability).isEqualTo(XNullability.NULLABLE)
                }
                assertThat(method.parameterTypes).isEmpty()
                assertThat(method.typeVariableNames).isEmpty()
            }

            checkMethods("setMutableT", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(TypeName.VOID)
                assertThat(method.parameterTypes.first().nullability)
                    .isEqualTo(XNullability.NULLABLE)
                assertThat(method.parameterTypes.first().typeName)
                    .isEqualTo(String::class.typeName())
                assertThat(method.typeVariableNames).isEmpty()
            }

            checkMethods("getList", nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.java,
                        String::class.java
                    )
                )
                assertThat(method.returnType.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                if (invocation.isKsp) {
                    assertThat(
                        method.returnType.typeArguments.first().nullability
                    ).isEqualTo(
                        XNullability.NULLABLE
                    )
                }
            }
            checkMethods("getNullableList", subject, nullableSubject) { method ->
                assertThat(method.returnType.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.java,
                        String::class.java
                    )
                )
                assertThat(method.returnType.nullability).isEqualTo(
                    XNullability.NONNULL
                )
                if (invocation.isKsp) {
                    assertThat(
                        method.returnType.typeArguments.first().nullability
                    ).isEqualTo(
                        XNullability.NULLABLE
                    )
                }
            }
        }
    }
}
