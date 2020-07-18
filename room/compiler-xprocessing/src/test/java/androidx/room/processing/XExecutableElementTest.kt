/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import androidx.room.processing.util.Source
import androidx.room.processing.util.getDeclaredMethod
import androidx.room.processing.util.getMethod
import androidx.room.processing.util.getParameter
import androidx.room.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.TypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XExecutableElementTest {
    @Test
    fun basic() {
        runProcessorTest(
            listOf(
                Source.java(
                    "foo.bar.Baz", """
                package foo.bar;
                public class Baz {
                    private void foo() {}
                    public int bar(int param1) {
                        return 3;
                    }
                }
            """.trimIndent()
                )
            )
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getDeclaredMethod("foo").let { method ->
                assertThat(method.isJavaDefault()).isFalse()
                assertThat(method.isVarArgs()).isFalse()
                assertThat(method.isOverrideableIgnoringContainer()).isFalse()
                assertThat(method.parameters).isEmpty()
                val returnType = method.returnType
                assertThat(returnType.isVoid()).isTrue()
                assertThat(returnType.defaultValue()).isEqualTo("null")
            }
            element.getDeclaredMethod("bar").let { method ->
                assertThat(method.isOverrideableIgnoringContainer()).isTrue()
                assertThat(method.parameters).hasSize(1)
                method.getParameter("param1").let { param ->
                    assertThat(param.type.isPrimitiveInt()).isTrue()
                }
                assertThat(method.returnType.isPrimitiveInt()).isTrue()
            }
        }
    }
    @Test
    fun isVarArgs() {
        val subject = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            interface Baz {
                void method(String... inputs);
            }
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.getMethod("method").isVarArgs()).isTrue()
        }
    }

    @Test
    fun kotlinDefaultImpl() {
        val subject = Source.kotlin(
            "Baz.kt", """
            package foo.bar;
            import java.util.List;
            interface Baz {
                fun noDefault()
                fun withDefault(): Int {
                    return 3;
                }
                fun nameMatch()
                fun nameMatch(param:Int) {}
                fun withDefaultWithParams(param1:Int, param2:String) {}
                fun withDefaultWithTypeArgs(param1: List<String>): String {
                    return param1.first();
                }
            }
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getDeclaredMethod("noDefault").let { method ->
                assertThat(method.findKotlinDefaultImpl()).isNull()
            }
            element.getDeclaredMethod("withDefault").let { method ->
                val defaultImpl = method.findKotlinDefaultImpl()
                assertThat(defaultImpl).isNotNull()
                assertThat(defaultImpl!!.returnType.typeName).isEqualTo(TypeName.INT)
                // default impl gets self as first parameter
                assertThat(defaultImpl.parameters).hasSize(1)
                assertThat(defaultImpl.parameters.first().type)
                    .isEqualTo(element.type)
            }
            element.getDeclaredMethods().first {
                it.name == "nameMatch" && it.parameters.isEmpty()
            }.let { nameMatchWithoutDefault ->
                assertThat(nameMatchWithoutDefault.findKotlinDefaultImpl()).isNull()
            }

            element.getDeclaredMethods().first {
                it.name == "nameMatch" && it.parameters.size == 1
            }.let { nameMatchWithoutDefault ->
                assertThat(nameMatchWithoutDefault.findKotlinDefaultImpl()).isNotNull()
            }

            element.getDeclaredMethod("withDefaultWithParams").let { method ->
                val defaultImpl = method.findKotlinDefaultImpl()
                assertThat(defaultImpl).isNotNull()
                assertThat(defaultImpl!!.parameters.drop(1).map {
                    it.name
                }).containsExactly("param1", "param2")
            }

            element.getDeclaredMethod("withDefaultWithTypeArgs").let { method ->
                val defaultImpl = method.findKotlinDefaultImpl()
                assertThat(defaultImpl).isNotNull()
                assertThat(defaultImpl!!.parameters.drop(1).map {
                    it.name
                }).containsExactly("param1")
            }
        }
    }
}
