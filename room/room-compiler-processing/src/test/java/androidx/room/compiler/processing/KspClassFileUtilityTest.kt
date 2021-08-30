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

import androidx.room.compiler.processing.ksp.KspTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getAllFieldNames
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.symbol.Origin
import org.junit.Test

/**
 * see: https://github.com/google/ksp/issues/250
 */
class KspClassFileUtilityTest {
    @Test
    fun outOfOrderKotlin_fields() {
        val libSource = Source.kotlin(
            "lib.kt",
            """
            class KotlinClass {
                val b: String = TODO()
                val a: String = TODO()
                val c: String = TODO()
                val isB:String = TODO()
                val isA:String = TODO()
                val isC:String = TODO()
            }
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSource))
        runProcessorTest(
            sources = emptyList(),
            classpath = classpath
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("KotlinClass")
            assertThat(element.getAllFieldNames())
                .containsExactly("b", "a", "c", "isB", "isA", "isC")
                .inOrder()
        }
    }

    @Test
    fun outOfOrderJava_fields() {
        val libSource = Source.kotlin(
            "JavaClass.java",
            """
            class JavaClass {
                String b;
                String a;
                String c;
                String isB;
                String isA;
                String isC;
            }
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSource))
        runProcessorTest(
            sources = emptyList(),
            classpath = classpath
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("JavaClass")
            assertThat(element.getAllFieldNames())
                .containsExactly("b", "a", "c", "isB", "isA", "isC")
                .inOrder()
        }
    }

    @Test
    fun outOfOrderKotlin_methods() {
        val libSource = Source.kotlin(
            "lib.kt",
            """
            class KotlinClass {
                fun b(): String = TODO()
                fun a(): String = TODO()
                fun c(): String = TODO()
                fun isB(): String = TODO()
                fun isA(): String = TODO()
                fun isC(): String = TODO()
            }
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSource))
        runProcessorTest(
            sources = listOf(Source.kotlin("Placeholder.kt", "")),
            classpath = classpath
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("KotlinClass")
            assertThat(element.getDeclaredMethods().map { it.name })
                .containsExactly("b", "a", "c", "isB", "isA", "isC")
                .inOrder()
        }
    }

    @Test
    fun outOfOrderJava_methods() {
        val libSource = Source.kotlin(
            "JavaClass.java",
            """
            class JavaClass {
                String b() { return ""; }
                String a() { return ""; }
                String c() { return ""; }
                String isB() { return ""; }
                String isA() { return ""; }
                String isC() { return ""; }
            }
            """.trimIndent()
        )
        val classpath = compileFiles(listOf(libSource))
        runProcessorTest(
            sources = listOf(Source.kotlin("Placeholder.kt", "")),
            classpath = classpath
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("JavaClass")
            assertThat(element.getDeclaredMethods().map { it.name })
                .containsExactly("b", "a", "c", "isB", "isA", "isC")
                .inOrder()
        }
    }

    @Test
    fun trueOrigin() {
        // this test is kind of testing ksp itself but it is still good to keep it in case it
        // breaks.
        fun createSources(pkg: String) = listOf(
            Source.java(
                "$pkg.JavaClass",
                """
                package $pkg;
                public class JavaClass {}
                """.trimIndent()
            ),
            Source.kotlin(
                "$pkg/KotlinClass.kt",
                """
                package $pkg;
                class KotlinClass {}
                """.trimIndent()
            )
        )
        fun XTestInvocation.findOrigin(
            qName: String
        ) = (processingEnv.requireTypeElement(qName) as KspTypeElement).declaration.origin

        val preCompiled = compileFiles(
            createSources("lib")
        )
        runKspTest(
            sources = createSources("main"),
            classpath = preCompiled
        ) { invocation ->
            assertThat(
                invocation.findOrigin("lib.JavaClass")
            ).isEqualTo(Origin.JAVA_LIB)
            assertThat(
                invocation.findOrigin("lib.KotlinClass")
            ).isEqualTo(Origin.KOTLIN_LIB)
            assertThat(
                invocation.findOrigin("main.JavaClass")
            ).isEqualTo(Origin.JAVA)
            assertThat(
                invocation.findOrigin("main.KotlinClass")
            ).isEqualTo(Origin.KOTLIN)
        }
    }
}