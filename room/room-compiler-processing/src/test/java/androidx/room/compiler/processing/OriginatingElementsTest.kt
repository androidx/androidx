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

import androidx.room.compiler.processing.ksp.KSClassDeclarationAsOriginatingElement
import androidx.room.compiler.processing.ksp.KSFileAsOriginatingElement
import androidx.room.compiler.processing.ksp.KspTypeElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

@RunWith(JUnit4::class)
class OriginatingElementsTest {

    @Test
    fun xElementIsConvertedToOriginatingElement() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public class Baz {
                    private void foo() {}
                    public String bar(String[] param1) {
                        return "";
                    }
                }
                    """.trimIndent()
                )
            )
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")

            val originatingElement = element.originatingElementForPoet()
            assertThat(originatingElement).isNotNull()

            if (it.isKsp) {
                assertThat(originatingElement).isInstanceOf(KSFileAsOriginatingElement::class.java)

                val originatingFile = (originatingElement as KSFileAsOriginatingElement).ksFile
                assertThat(originatingFile)
                    .isEqualTo(
                        (element as KspTypeElement).declaration.containingFile
                    )
            } else {
                assertThat(originatingElement).isInstanceOf(TypeElement::class.java)
                assertThat((originatingElement as TypeElement).qualifiedName.toString())
                    .isEqualTo("foo.bar.Baz")
            }
        }
    }

    @Test
    fun classPathTypeIsConvertedToOriginatingElement() {
        runProcessorTest {
            val element = it.processingEnv
                .requireTypeElement("com.google.devtools.ksp.processing.SymbolProcessor")

            val originatingElement = element.originatingElementForPoet()
            assertThat(originatingElement).isNotNull()

            if (it.isKsp) {
                assertThat(originatingElement)
                    .isInstanceOf(KSClassDeclarationAsOriginatingElement::class.java)

                val ksClassDeclaration =
                    (originatingElement as KSClassDeclarationAsOriginatingElement)
                        .ksClassDeclaration
                assertThat(ksClassDeclaration)
                    .isEqualTo(
                        (element as KspTypeElement).declaration
                    )
            } else {
                assertThat(originatingElement).isInstanceOf(TypeElement::class.java)
                assertThat((originatingElement as TypeElement).qualifiedName.toString())
                    .isEqualTo("com.google.devtools.ksp.processing.SymbolProcessor")
            }
        }
    }

    @Test
    fun syntheticPropertyElementConvertedToOriginatingElement() {
        runProcessorTest(
            sources = listOf(
                Source.kotlin(
                    "Foo.kt",
                    """
            class Foo {
                companion object {
                    @JvmStatic
                    var bar = 1
                }
            }
                    """.trimIndent()
                )
            )
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("Foo")
            val syntheticPropertyElements = element.getDeclaredMethods()

            // Synthetic getter and setter methods are created.
            assertThat(syntheticPropertyElements).hasSize(2)

            syntheticPropertyElements.forEach { syntheticPropertyElement ->
                val originatingElement = syntheticPropertyElement.originatingElementForPoet()
                assertThat(originatingElement).isNotNull()

                if (invocation.isKsp) {
                    assertThat(originatingElement)
                        .isInstanceOf(KSFileAsOriginatingElement::class.java)

                    val originatingFile = (originatingElement as KSFileAsOriginatingElement).ksFile
                    assertThat(originatingFile)
                        .isEqualTo(
                            (syntheticPropertyElement as KspSyntheticPropertyMethodElement)
                                .field.declaration.containingFile
                        )
                } else {
                    assertThat(originatingElement).isInstanceOf(ExecutableElement::class.java)
                }
            }
        }
    }

    @Test
    fun originatingElementIsAddedToPoet() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public class Baz {
                    private void foo() {}
                    public String bar(String[] param1) {
                        return "";
                    }
                }
                    """.trimIndent()
                )
            )
        ) {
            val xTypeElement = it.processingEnv.requireTypeElement("foo.bar.Baz")

            val javaPoetTypeSpec = TypeSpec.classBuilder("Foo")
                .addOriginatingElement(xTypeElement)
                .build()

            val kotlinPoetTypeSpec = com.squareup.kotlinpoet.TypeSpec.classBuilder("Foo")
                .addOriginatingElement(xTypeElement)
                .build()

            assertThat(javaPoetTypeSpec.originatingElements).apply {
                hasSize(1)
                containsExactlyElementsIn(kotlinPoetTypeSpec.originatingElements)
                containsExactly(xTypeElement.originatingElementForPoet())
            }
        }
    }
}