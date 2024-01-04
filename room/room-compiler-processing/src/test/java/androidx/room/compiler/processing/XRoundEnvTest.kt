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

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.javapoet.JTypeName
import org.junit.Test

class XRoundEnvTest {

    @Test
    fun getAnnotatedElements() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.testcode.OtherAnnotation
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
            assertThat(annotatedElementsByClass).containsExactlyElementsIn(annotatedElementsByName)
            if (testInvocation.isKsp) {
                assertThat(annotatedElementsByClass.map { it.name }).containsExactly(
                    "Baz",
                    "myProperty",
                    "myFunction",
                )
            } else {
                assertThat(annotatedElementsByClass.map { it.name }).containsExactly(
                    "Baz",
                    // TODO(b/290234031): Fix XRoundEnv to return the property rather than the
                    //  synthetic "$annotations" method in KAPT
                    "getMyProperty\$annotations",
                    "myFunction",
                )
            }
        }
    }

    @Test
    fun getAnnotatedPropertyElements() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.testcode.OtherAnnotation
            class Baz {
                @get:OtherAnnotation(value="xx")
                var myProperty1: Int = 0
                @set:OtherAnnotation(value="xx")
                var myProperty2: Int = 0
                @field:OtherAnnotation(value="xx")
                var myProperty3: Int = 0
                companion object {
                    @get:OtherAnnotation(value="xx")
                    @JvmStatic
                    val myProperty4: String = ""
                    @get:OtherAnnotation(value="xx")
                    const val myProperty5: String = ""
                }
            }
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            val annotatedElements = testInvocation.roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class
            )

            val baz = testInvocation.processingEnv.requireTypeElement("Baz")

            assertThat(
                annotatedElements.map { it.name }
            ).containsExactly(
                "getMyProperty4",
                "myProperty3",
                "getMyProperty1",
                "setMyProperty2",
                "getMyProperty4"
            )
            baz.getDeclaredMethods().forEach { method ->
              assertWithMessage("Enclosing element of method ${method.jvmName}")
                .that(method.enclosingElement.name)
                .isEqualTo("Baz")
            }
        }
    }

    @Test
    fun getAnnotatedPackageElements() {
        val source = Source.java(
            // Packages can be annotated in `package-info.java` files.
            "foo.bar.foobar.package-info",
            """
            @OtherAnnotation(value = "xx")
            package foo.bar.foobar;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;
            """.trimIndent()
        )

        runProcessorTest(listOf(source)) { testInvocation ->
            (testInvocation.roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class
            ).single() as XPackageElement).apply {
                assertThat(name).isEqualTo("foobar")
                assertThat(qualifiedName).isEqualTo("foo.bar.foobar")
                assertThat(kindName()).isEqualTo("package")
                assertThat(validate()).isTrue()
            }.getAllAnnotations().single().apply {
                assertThat(qualifiedName)
                    .isEqualTo("androidx.room.compiler.processing.testcode.OtherAnnotation")
            }.annotationValues.single().apply {
                assertThat(name).isEqualTo("value")
                assertThat(value).isEqualTo("xx")
            }
        }
    }

    @Test
    fun defaultPackage() {
        val javaSource = Source.java(
            "FooBar",
            """
            class FooBar {}
            """.trimIndent()
        )
        val kotlinSource = Source.kotlin(
            "FooBarKt.kt",
            """
            class FooBarKt
            """.trimIndent()
        )
        runProcessorTest(listOf(javaSource, kotlinSource)) { testInvocation ->
            testInvocation.processingEnv.requireTypeElement("FooBar").apply {
                assertThat(packageName).isEqualTo("")
            }
            testInvocation.processingEnv.requireTypeElement("FooBarKt").apply {
                assertThat(packageName).isEqualTo("")
            }
        }
    }

    @Test
    fun misalignedAnnotationTargetFailsCompilation() {
        val source = Source.kotlin(
            "Baz.kt",
            """
            import androidx.room.compiler.processing.XRoundEnvTest.PropertyAnnotation
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
            assertThat(subject.jvmName).isEqualTo("myFun")
            assertThat(subject.enclosingElement.asClassName()).isEqualTo(
                XClassName.get("", "BazKt")
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
                    is XMethodElement -> it.jvmName
                    is XFieldElement -> it.name
                    else -> error("unexpected type $it")
                }
            }
            val containerClassName = XClassName.get("foo.bar", "MyCustomClass")
            assertThat(byName.keys).containsExactly(
                "getMyPropertyGetter",
                "setMyPropertySetter",
                "myProperty"
            )
            (byName["getMyPropertyGetter"] as XMethodElement).let {
                assertThat(it.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
                assertThat(it.parameters).hasSize(0)
                assertThat(it.enclosingElement.asClassName()).isEqualTo(containerClassName)
                assertThat(it.isStatic()).isTrue()
            }
            (byName["setMyPropertySetter"] as XMethodElement).let {
                assertThat(it.returnType.asTypeName().java).isEqualTo(JTypeName.VOID)
                assertThat(it.returnType.asTypeName().kotlin).isEqualTo(UNIT)
                assertThat(it.parameters).hasSize(1)
                assertThat(it.parameters.first().type.asTypeName())
                    .isEqualTo(XTypeName.PRIMITIVE_INT)
                assertThat(it.enclosingElement.asClassName()).isEqualTo(containerClassName)
                assertThat(it.isStatic()).isTrue()
            }
            (byName["myProperty"] as XFieldElement).let {
                assertThat(it.type.asTypeName().java).isEqualTo(JTypeName.INT)
                assertThat(it.type.asTypeName().kotlin).isEqualTo(INT)
                assertThat(it.enclosingElement.asClassName()).isEqualTo(containerClassName)
                assertThat(it.isStatic()).isTrue()
            }
        }
    }

    @Test
    fun getTypeElementsFromPackageIncludesSources() {
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
            ).containsExactly(targetElement)
        }
    }

    @Test
    fun getTypeElementsFromPackageIncludesBinaries() {
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

    @Test
    fun getTypeElementsFromPackageReturnsEmptyListForUnknownPackage() {
        runProcessorTest { testInvocation ->
            val kspElements = testInvocation.processingEnv.getTypeElementsFromPackage(
                "com.example.unknown.package"
            )

            assertThat(kspElements).isEmpty()
        }
    }

    @Test
    fun getElementsFromPackageInSource() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            package foo.bar
            val p: Int = TODO()
            fun f(): String = TODO()
            """.trimIndent()
        )
        runProcessorTest(listOf(source)) { invocation ->
            val elements = invocation.processingEnv.getElementsFromPackage(
                "foo.bar"
            )
            if (invocation.isKsp) {
                assertThat(
                    elements.map { it.name }
                ).containsExactly("p", "f")
            } else {
                assertThat(
                    elements.map { it.name }
                ).containsExactly("FooKt")
            }
        }
    }

    @Test
    fun getElementsFromPackageInClass() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            package foo.bar
            val p: Int = TODO()
            fun f(): String = TODO()
            """.trimIndent()
        )
        runProcessorTest(classpath = compileFiles(listOf(source))) { invocation ->
            val elements = invocation.processingEnv.getElementsFromPackage(
                "foo.bar"
            )
            if (invocation.isKsp) {
                assertThat(
                    elements.map { it.name }
                ).containsExactly("p", "f")
            } else {
                assertThat(
                    elements.map { it.name }
                ).containsExactly("FooKt")
            }
        }
    }

    @Test
    fun getElementsFromPackageReturnsEmptyListForUnknownPackage() {
        runProcessorTest { testInvocation ->
            val elements = testInvocation.processingEnv.getElementsFromPackage(
                "com.example.unknown.package"
            )

            assertThat(elements).isEmpty()
        }
    }

    @Test
    fun getAnnotatedParamElements() {
        runProcessorTest(
            listOf(
                Source.kotlin(
                    "Baz.kt",
                    """
                    import androidx.room.compiler.processing.XRoundEnvTest.TopLevelAnnotation
                    class Baz constructor(
                        @param:TopLevelAnnotation val ctorProperty: String,
                        @TopLevelAnnotation ctorParam: String
                    ) {
                        @setparam:TopLevelAnnotation
                        var property: String = ""
                        fun method(@TopLevelAnnotation methodParam: String) {}
                    }
                    """.trimIndent()
                )
            )
        ) { testInvocation ->
            val typeElement = testInvocation.processingEnv.requireTypeElement("Baz")
            val annotatedElements =
                testInvocation.roundEnv.getElementsAnnotatedWith(TopLevelAnnotation::class)
            val annotatedParams = annotatedElements.filterIsInstance<XExecutableParameterElement>()
            assertThat(annotatedParams.map { it.name }).containsExactly(
                "ctorProperty",
                "ctorParam",
                "p0",
                "methodParam",
            ).inOrder()
            assertThat(annotatedParams.map { it.jvmName }).containsExactly(
                "ctorProperty",
                "ctorParam",
                "p0",
                "methodParam",
            ).inOrder()
            assertThat(annotatedParams.map { it.enclosingElement }).containsExactly(
                typeElement.findPrimaryConstructor(),
                typeElement.findPrimaryConstructor(),
                typeElement.getDeclaredMethodByJvmName("setProperty"),
                typeElement.getDeclaredMethodByJvmName("method"),
            ).inOrder()
        }
    }

    @Test
    fun getElementsAnnotatedWithMissingTypeAnnotation() {
        runProcessorTest(
            listOf(
                Source.kotlin(
                    "Baz.kt",
                    """
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) { testInvocation ->
            // Expect zero elements to be returned from the round for an annotation whose type is
            // missing. This is allowed since there are processors whose capabilities might be
            // dynamic based on user classpath.
            val annotatedElements =
                testInvocation.roundEnv.getElementsAnnotatedWith("MissingTypeAnnotation")
            assertThat(annotatedElements).hasSize(0)
        }
    }

    annotation class TopLevelAnnotation

    @Suppress("unused") // used in tests
    @Target(AnnotationTarget.PROPERTY)
    annotation class PropertyAnnotation
}
