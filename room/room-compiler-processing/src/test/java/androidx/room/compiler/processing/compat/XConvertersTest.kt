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

package androidx.room.compiler.processing.compat

import androidx.room.compiler.processing.compat.XConverters.toJavac
import androidx.room.compiler.processing.compat.XConverters.toXProcessing
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.testcode.TestSuppressWarnings
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getDeclaredMethod
import androidx.room.compiler.processing.util.runKaptTest
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

class XConvertersTest {

    val kotlinSrc = Source.kotlin(
        "KotlinClass.kt",
        """
        @androidx.room.compiler.processing.testcode.TestSuppressWarnings("warning1")
        class KotlinClass {
          var field = 1
          fun foo(param: Int) {
          }
        }
        """.trimIndent()
    )
    val javaSrc = Source.java(
        "JavaClass",
        """
        @androidx.room.compiler.processing.testcode.TestSuppressWarnings("warning1")
        public class JavaClass {
          public int field = 1;
          public void foo(int param) {
          }
        }
        """.trimIndent()
    )

    @Test
    fun typeElement() {
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            assertThat(kotlinClass.toJavac())
                .isEqualTo(invocation.getJavacTypeElement("KotlinClass"))
            assertThat(javaClass.toJavac())
                .isEqualTo(invocation.getJavacTypeElement("JavaClass"))

            assertThat(
                invocation.getJavacTypeElement("KotlinClass")
                    .toXProcessing(invocation.processingEnv)
            ).isEqualTo(kotlinClass)
            assertThat(
                invocation.getJavacTypeElement("JavaClass")
                    .toXProcessing(invocation.processingEnv)
            ).isEqualTo(javaClass)
        }
    }

    @Test
    fun executableElement() {
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            assertThat(
                kotlinClass.getDeclaredMethods().map { it.toJavac() }
            ).containsExactlyElementsIn(
                ElementFilter.methodsIn(
                    invocation.getJavacTypeElement("KotlinClass").enclosedElements
                )
            )
            assertThat(
                javaClass.getDeclaredMethods().map { it.toJavac() }
            ).containsExactlyElementsIn(
                ElementFilter.methodsIn(
                    invocation.getJavacTypeElement("JavaClass").enclosedElements
                )
            )

            val kotlinFoo = ElementFilter.methodsIn(
                invocation.getJavacTypeElement("KotlinClass").enclosedElements
            ).first { it.simpleName.toString() == "foo" }
            assertThat(kotlinFoo.toXProcessing(invocation.processingEnv))
                .isEqualTo(kotlinClass.getDeclaredMethod("foo"))
            val javaFoo = ElementFilter.methodsIn(
                invocation.getJavacTypeElement("JavaClass").enclosedElements
            ).first { it.simpleName.toString() == "foo" }
            assertThat(javaFoo.toXProcessing(invocation.processingEnv))
                .isEqualTo(javaClass.getDeclaredMethod("foo"))
        }
    }

    @Test
    fun variableElement_field() {
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            assertThat(
                kotlinClass.getDeclaredFields().map { it.toJavac() }
            ).containsExactlyElementsIn(
                ElementFilter.fieldsIn(
                    invocation.getJavacTypeElement("KotlinClass").enclosedElements
                )
            )
            assertThat(
                javaClass.getDeclaredFields().map { it.toJavac() }
            ).containsExactlyElementsIn(
                ElementFilter.fieldsIn(
                    invocation.getJavacTypeElement("JavaClass").enclosedElements
                )
            )

            val kotlinField = ElementFilter.fieldsIn(
                invocation.getJavacTypeElement("KotlinClass").enclosedElements
            ).first { it.simpleName.toString() == "field" }
            assertThat(kotlinField.toXProcessing(invocation.processingEnv))
                .isEqualTo(kotlinClass.getDeclaredField("field"))
            val javaField = ElementFilter.fieldsIn(
                invocation.getJavacTypeElement("JavaClass").enclosedElements
            ).first { it.simpleName.toString() == "field" }
            assertThat(javaField.toXProcessing(invocation.processingEnv))
                .isEqualTo(javaClass.getDeclaredField("field"))
        }
    }

    @Test
    fun variableElement_parameter() {
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            assertThat(
                kotlinClass.getDeclaredMethod("foo").parameters.map { it.toJavac() }
            ).containsExactlyElementsIn(
                ElementFilter.methodsIn(
                    invocation.getJavacTypeElement("KotlinClass").enclosedElements
                ).first { it.simpleName.toString() == "foo" }.parameters
            )
            assertThat(
                javaClass.getDeclaredMethod("foo").parameters.map { it.toJavac() }
            ).containsExactlyElementsIn(
                ElementFilter.methodsIn(
                    invocation.getJavacTypeElement("JavaClass").enclosedElements
                ).first { it.simpleName.toString() == "foo" }.parameters
            )

            val kotlinParam = ElementFilter.methodsIn(
                invocation.getJavacTypeElement("KotlinClass").enclosedElements
            ).first { it.simpleName.toString() == "foo" }.parameters.first()
            assertThat(kotlinParam.toXProcessing(invocation.processingEnv))
                .isEqualTo(kotlinClass.getDeclaredMethod("foo").parameters.first())
            val javaParam = ElementFilter.methodsIn(
                invocation.getJavacTypeElement("JavaClass").enclosedElements
            ).first { it.simpleName.toString() == "foo" }.parameters.first()
            assertThat(javaParam.toXProcessing(invocation.processingEnv))
                .isEqualTo(javaClass.getDeclaredMethod("foo").parameters.first())
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun annotation() {
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            assertThat(
                kotlinClass.requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                    .toJavac()
            ).isEqualTo(
                MoreElements.getAnnotationMirror(
                    invocation.getJavacTypeElement("KotlinClass"),
                    TestSuppressWarnings::class.java
                ).get()
            )
            assertThat(
                javaClass.requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                    .toJavac()
            ).isEqualTo(
                MoreElements.getAnnotationMirror(
                    invocation.getJavacTypeElement("JavaClass"),
                    TestSuppressWarnings::class.java
                ).get()
            )
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun customFiler() {
        var runCount = 0
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            val className = ClassName.get("foo.bar", "ToBeGenerated")
            if (invocation.processingEnv.findTypeElement(className) == null) {
                // Assert that this is only run only on the first round
                assertThat(++runCount).isEqualTo(1)

                // Check that we can create a custom filer and toJavac() returns it
                val filer = invocation.processingEnv.filer.toJavac()
                val customFiler = object : Filer by filer {}
                val customXFiler = customFiler.toXProcessing(invocation.processingEnv)
                assertThat(customXFiler.toJavac()).isEqualTo(customFiler)
                val spec = TypeSpec.classBuilder(className).build()
                customXFiler.write(JavaFile.builder(className.packageName(), spec).build())
            } else {
                // Asserts that the class was generated in the second round
                assertThat(++runCount).isEqualTo(2)
                assertThat(invocation.processingEnv.findTypeElement(className)).isNotNull()
            }
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun customMessager() {
        runKaptTest(
            sources = listOf(kotlinSrc, javaSrc)
        ) { invocation ->
            // Check that we can create a custom messager and toJavac() returns it
            val customMessager = object : Messager by invocation.processingEnv.messager.toJavac() {
                override fun printMessage(kind: Diagnostic.Kind?, msg: CharSequence?) {
                    // We have to use the XMessager from XProcessingEnv here so that it runs the
                    // hooks that are attached to the testing infrastructure. Otherwise, the error
                    // is produced by not recorded. We may want to add a method to XMessager to copy
                    // the watchers from another messager, e.g. XMessager#copyWatchers(XMessager)
                    invocation.processingEnv.messager.printMessage(kind!!, "Custom: $msg")
                }
            }
            val customXMessager = customMessager.toXProcessing()
            assertThat(customXMessager.toJavac()).isEqualTo(customMessager)

            // Check that the custom messager prints the proper message
            customXMessager.printMessage(Diagnostic.Kind.ERROR, "error msg")
            invocation.assertCompilationResult {
                compilationDidFail()
                hasErrorCount(1)
                hasWarningCount(0)
                hasError("Custom: error msg")
            }
        }
    }

    private fun XTestInvocation.getJavacTypeElement(fqn: String) =
        (this.processingEnv as JavacProcessingEnv).delegate.elementUtils.getTypeElement(fqn)
}