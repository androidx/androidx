/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FallbackLocationInformationTest {
    @Test
    fun errorMessageInClassFiles() {
        val kotlinSource = Source.kotlin(
            "KotlinSubject.kt",
            """
            package foo.bar
            class KotlinSubject(val constructorProp:Int, constructorArg:Int) {
                var prop: String = ""
                var propWithAccessors: String
                    get() = ""
                    set(myValue) = TODO()
                fun method1(arg1: Int): String = ""
                suspend fun suspendFun(arg1:Int): String = ""
            }
            """.trimIndent()
        )

        val javaSource = Source.java(
            "foo.bar.JavaSubject",
            """
            package foo.bar;
            class JavaSubject {
                String field1;
                // naming this arg0 because javac cannot read the real param name after compilation
                JavaSubject(int arg0) {
                }
                // naming this arg0 because javac cannot read the real param name after compilation
                void method1(int arg0) {}
            }
            """.trimIndent()
        )
        // add a placeholder to not run tests w/ javac since we depend on compiled kotlin
        // sources and javac fails to resolve metadata
        val placeholder = Source.kotlin("MyPlaceholder.kt", "")
        val dependency = compileFiles(listOf(kotlinSource, javaSource))
        runProcessorTest(
            sources = listOf(placeholder),
            classpath = listOf(dependency)
        ) { invocation ->
            val kotlinSubject = invocation.processingEnv.requireTypeElement("foo.bar.KotlinSubject")
            assertThat(
                kotlinSubject.getField("prop").fallbackLocationText
            ).isEqualTo(
                "prop in foo.bar.KotlinSubject"
            )
            kotlinSubject.getMethod("method1").let { method ->
                assertThat(
                    method.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.KotlinSubject.method1(int)"
                )
                assertThat(
                    method.parameters.first().fallbackLocationText
                ).isEqualTo(
                    "arg1 in foo.bar.KotlinSubject.method1(int)"
                )
            }
            kotlinSubject.getMethod("suspendFun").let { suspendFun ->
                assertThat(
                    suspendFun.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.KotlinSubject.suspendFun(int)"
                )
                assertThat(
                    suspendFun.parameters.last().fallbackLocationText
                ).isEqualTo(
                    "return type of foo.bar.KotlinSubject.suspendFun(int)"
                )
            }

            assertThat(
                kotlinSubject.getMethod("getProp").fallbackLocationText
            ).isEqualTo(
                "foo.bar.KotlinSubject.getProp()"
            )
            kotlinSubject.getMethod("setProp").let { propSetter ->
                assertThat(
                    propSetter.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.KotlinSubject.setProp(java.lang.String)"
                )
                assertThat(
                    propSetter.parameters.first().fallbackLocationText
                ).isEqualTo(
                    "arg0 in foo.bar.KotlinSubject.setProp(java.lang.String)"
                )
            }

            kotlinSubject.getMethod("setPropWithAccessors").let { propSetter ->
                // javac does not know that this is synthetic setter
                assertThat(
                    propSetter.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.KotlinSubject.setPropWithAccessors(java.lang.String)"
                )
                assertThat(
                    propSetter.parameters.first().fallbackLocationText
                ).isEqualTo(
                    "myValue in foo.bar.KotlinSubject.setPropWithAccessors(java.lang.String)"
                )
            }

            kotlinSubject.getConstructors().single().let { constructor ->
                assertThat(
                    constructor.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.KotlinSubject.<init>(int, int)"
                )
                assertThat(
                    constructor.parameters.first().fallbackLocationText
                ).isEqualTo(
                    "constructorProp in foo.bar.KotlinSubject.<init>" +
                        "(int, int)"
                )
            }

            val javaSubject = invocation.processingEnv.requireTypeElement("foo.bar.JavaSubject")
            assertThat(
                javaSubject.fallbackLocationText
            ).isEqualTo(
                "foo.bar.JavaSubject"
            )
            val argName = if (invocation.isKsp) { // ¯\_(ツ)_/¯
                "p0"
            } else {
                "arg0"
            }
            javaSubject.getConstructors().single().let { constructor ->
                assertThat(
                    constructor.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.JavaSubject.<init>(int)"
                )
                assertThat(
                    constructor.parameters.single().fallbackLocationText
                ).isEqualTo(
                    "$argName in foo.bar.JavaSubject.<init>(int)"
                )
            }

            assertThat(
                javaSubject.getField("field1").fallbackLocationText
            ).isEqualTo(
                "field1 in foo.bar.JavaSubject"
            )
            javaSubject.getMethod("method1").let { method ->
                assertThat(
                    method.fallbackLocationText
                ).isEqualTo(
                    "foo.bar.JavaSubject.method1(int)"
                )
            }
        }
    }
}