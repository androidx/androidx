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

import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getDeclaredMethod
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Test

class XRoundEnvTest {

    @Test
    fun getAnnotatedElements() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.testcode.OtherAnnotation;
            @OtherAnnotation(value="xx")
            class Baz {
                @OtherAnnotation(value="xx")
                var myProperty: Int = 0
                @OtherAnnotation(value="xx")
                fun myFunction() { }
            }
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            val annotatedElementsByClass = testInvocation.roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class
            )

            val annotatedElementsByName = testInvocation.roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class.qualifiedName!!
            )

            val targetElement = testInvocation.processingEnv.requireTypeElement("Baz")

            assertThat(
                annotatedElementsByClass
            ).apply {
                containsExactlyElementsIn(annotatedElementsByName)
                hasSize(3)
                contains(targetElement)
                contains(targetElement.getMethod("myFunction"))

                if (testInvocation.isKsp) {
                    contains(targetElement.getField("myProperty"))
                } else {
                    // Javac sees a property annotation on the synthetic function
                    contains(targetElement.getDeclaredMethod("getMyProperty\$annotations"))
                }
            }
        }
    }

    @Test
    fun getAnnotatedPropertyElements() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.testcode.OtherAnnotation;
            class Baz {
                @get:OtherAnnotation(value="xx")
                var myProperty1: Int = 0
                @set:OtherAnnotation(value="xx")
                var myProperty2: Int = 0
                @field:OtherAnnotation(value="xx")
                var myProperty3: Int = 0
            }
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            val annotatedElements = testInvocation.roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class
            )

            val targetElement = testInvocation.processingEnv.requireTypeElement("Baz")

            assertThat(
                annotatedElements
            ).apply {
                hasSize(3)

                contains(targetElement.getDeclaredMethod("getMyProperty1"))
                contains(targetElement.getDeclaredMethod("setMyProperty2"))
                contains(targetElement.getField("myProperty3"))
            }
        }
    }

    @Test
    fun misalignedAnnotationTargetFailsCompilation() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.XRoundEnvTest.PropertyAnnotation;
            class Baz {
                @PropertyAnnotation
                fun myFun(): Int = 0
            }
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            testInvocation.assertCompilationResult { compilationDidFail() }
        }
    }

    @Test
    fun getAnnotatedTopLevelFunction() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.XRoundEnvTest.TopLevelAnnotation
            @TopLevelAnnotation
            fun myFun(): Int = 0
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            val annotatedElements = testInvocation.roundEnv.getElementsAnnotatedWith(
                TopLevelAnnotation::class
            )
            assertThat(annotatedElements).hasSize(1)
            val subject = annotatedElements.filterIsInstance<XMethodElement>().first()
            assertThat(subject.name).isEqualTo("myFun")
            assertThat(subject.enclosingElement.className).isEqualTo(
                ClassName.get("", "BazKt")
            )
            assertThat(subject.isStatic()).isTrue()
        }
    }

    @Test
    fun getAnnotatedTopLevelProperty() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            @file:JvmName("MyCustomClass")
            package foo.bar
            import androidx.room.compiler.processing.XRoundEnvTest.TopLevelAnnotation
            @get:TopLevelAnnotation
            var myPropertyGetter: Int = 0
            @set:TopLevelAnnotation
            var myPropertySetter: Int = 0
            @field:TopLevelAnnotation
            var myProperty: Int = 0
            """.trimIndent()
        )

        runKspTest(listOf(source)) { testInvocation ->
            val annotatedElements = testInvocation.roundEnv.getElementsAnnotatedWith(
                TopLevelAnnotation::class
            )
            assertThat(annotatedElements).hasSize(3)
            val byName = annotatedElements.associateBy {
                when (it) {
                    is XMethodElement -> it.name
                    is XFieldElement -> it.name
                    else -> error("unexpected type $it")
                }
            }
            val containerClassName = ClassName.get("foo.bar", "MyCustomClass")
            assertThat(byName.keys).containsExactly(
                "getMyPropertyGetter",
                "setMyPropertySetter",
                "myProperty"
            )
            (byName["getMyPropertyGetter"] as XMethodElement).let {
                assertThat(it.returnType.typeName).isEqualTo(TypeName.INT)
                assertThat(it.parameters).hasSize(0)
                assertThat(it.enclosingElement.className).isEqualTo(
                    containerClassName
                )
                assertThat(it.isStatic()).isTrue()
            }
            (byName["setMyPropertySetter"] as XMethodElement).let {
                assertThat(it.returnType.typeName).isEqualTo(TypeName.VOID)
                assertThat(it.parameters).hasSize(1)
                assertThat(it.parameters.first().type.typeName).isEqualTo(TypeName.INT)
                assertThat(it.enclosingElement.className).isEqualTo(
                    containerClassName
                )
                assertThat(it.isStatic()).isTrue()
            }
            (byName["myProperty"] as XFieldElement).let {
                assertThat(it.type.typeName).isEqualTo(TypeName.INT)
                assertThat(it.enclosingElement.className).isEqualTo(
                    containerClassName
                )
                assertThat(it.isStatic()).isTrue()
            }
        }
    }

    @Test
    fun getElementsFromPackageIncludesSources() {
        val source = Source.kotlin(
            "foo/Baz.kt",
            """
            package foo
            class Baz 
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            val elements = testInvocation.processingEnv.getTypeElementsFromPackage("foo")

            val targetElement = testInvocation.processingEnv.requireTypeElement(
                "foo.Baz"
            )

            assertThat(
                elements
            ).contains(targetElement)
        }
    }

    @Test
    fun getElementsFromPackageIncludesBinaries() {
        runProcessorTest { testInvocation ->
            val kspElements = testInvocation.processingEnv.getTypeElementsFromPackage(
                "com.google.devtools.ksp.processing"
            )

            val symbolProcessorType = testInvocation.processingEnv.requireTypeElement(
                "com.google.devtools.ksp.processing.SymbolProcessor"
            )

            assertThat(
                kspElements
            ).contains(symbolProcessorType)
        }
    }

    annotation class TopLevelAnnotation

    @Suppress("unused") // used in tests
    @Target(AnnotationTarget.PROPERTY)
    annotation class PropertyAnnotation
}
