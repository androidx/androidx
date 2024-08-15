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

import androidx.kruth.assertThat
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.compat.XConverters.getProcessingEnv
import androidx.room.compiler.processing.compat.XConverters.toJavac
import androidx.room.compiler.processing.compat.XConverters.toKS
import androidx.room.compiler.processing.compat.XConverters.toXProcessing
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.testcode.TestSuppressWarnings
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.devtools.ksp.getDeclaredFunctions
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic
import org.junit.Test

class XConvertersTest {

    val kotlinSrc =
        Source.kotlin(
            "KotlinClass.kt",
            """
        @androidx.room.compiler.processing.testcode.TestSuppressWarnings("warning1")
        class KotlinClass {
          var field = 1
          fun foo(param: Int) {
          }
        }
        """
                .trimIndent()
        )
    val javaSrc =
        Source.java(
            "JavaClass",
            """
        @androidx.room.compiler.processing.testcode.TestSuppressWarnings("warning1")
        public class JavaClass {
          public int field = 1;
          public void foo(int param) {
          }
        }
        """
                .trimIndent()
        )

    @Test
    fun typeMirror() {
        val kotlinSrc =
            Source.kotlin(
                "KotlinClass.kt",
                """
            class KotlinClass {
              class FooImpl<T: String>: Foo<T> {
                override fun foo(param: T) {}
              }
              interface Foo<T> {
                fun foo(param: T)
              }
            }
            """
                    .trimIndent()
            )
        val javaSrc =
            Source.java(
                "JavaClass",
                """
            public class JavaClass {
              static class FooImpl<T extends String> implements Foo<T> {
                @Override public void foo(T param) {}
              }
              interface Foo<T> {
                void foo(T param);
              }
            }
            """
                    .trimIndent()
            )
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            // Test toXProcessing returns an equivalent XType
            fun assertEqualTypes(t: XType?, tFromXConverters: XType?) {
                if (t == tFromXConverters) {
                    return
                }
                if (t == null || tFromXConverters == null) {
                    assertThat(t).isNull()
                    assertThat(tFromXConverters).isNull()
                    return
                }
                assertThat(t.asTypeName()).isEqualTo(tFromXConverters.asTypeName())
                assertThat(t.typeElement).isEqualTo(tFromXConverters.typeElement)
                assertThat(t.rawType.asTypeName()).isEqualTo(tFromXConverters.rawType.asTypeName())
                assertThat(t.typeArguments.size).isEqualTo(tFromXConverters.typeArguments.size)
                for (i in 0..t.typeArguments.size) {
                    assertEqualTypes(t.typeArguments[i], tFromXConverters.typeArguments[i])
                }
                assertEqualTypes(t.boxed(), tFromXConverters.boxed())
                assertEqualTypes(t.extendsBoundOrSelf(), tFromXConverters.extendsBoundOrSelf())

                // Test calling nullability is okay for "normal" xprocessing types
                assertThat(t.nullability).isNotNull()

                // Test calling nullability throws for xprocessing types created with XConverters
                try {
                    tFromXConverters.nullability
                    error("Expected the above statement to fail.")
                } catch (e: IllegalStateException) {
                    assertThat(e.message)
                        .contains("XType#nullibility cannot be called from this type")
                }
            }

            fun TypeMirror.equivalence() = MoreTypes.equivalence().wrap(this)

            val xKotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass.FooImpl")
            val xJavaClass = invocation.processingEnv.requireTypeElement("JavaClass.FooImpl")

            if (invocation.isKsp) {
                val kotlinClass = invocation.getKspTypeElement("KotlinClass.FooImpl")
                val javaClass = invocation.getKspTypeElement("JavaClass.FooImpl")

                assertThat(xKotlinClass.type.toKS()).isEqualTo(kotlinClass.asType(emptyList()))
                assertThat(xJavaClass.type.toKS()).isEqualTo(javaClass.asType(emptyList()))

                assertEqualTypes(
                    xKotlinClass.type,
                    kotlinClass.asType(emptyList()).toXProcessing(invocation.processingEnv)
                )
                assertEqualTypes(
                    xJavaClass.type,
                    javaClass.asType(emptyList()).toXProcessing(invocation.processingEnv)
                )
            } else {
                val kotlinClass = invocation.getJavacTypeElement("KotlinClass.FooImpl")
                val javaClass = invocation.getJavacTypeElement("JavaClass.FooImpl")

                // Test toJavac returns an equivalent TypeMirror
                assertThat(xKotlinClass.type.toJavac().equivalence())
                    .isEqualTo(kotlinClass.asType().equivalence())
                assertThat(xJavaClass.type.toJavac().equivalence())
                    .isEqualTo(javaClass.asType().equivalence())

                assertEqualTypes(
                    xKotlinClass.type,
                    kotlinClass.asType().toXProcessing(invocation.processingEnv)
                )
                assertEqualTypes(
                    xJavaClass.type,
                    javaClass.asType().toXProcessing(invocation.processingEnv)
                )
            }
        }
    }

    @Test
    fun typeElement() {
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                assertThat(kotlinClass.toKS())
                    .isEqualTo(invocation.getKspTypeElement("KotlinClass"))
                assertThat(javaClass.toKS()).isEqualTo(invocation.getKspTypeElement("JavaClass"))

                assertThat(
                        invocation
                            .getKspTypeElement("KotlinClass")
                            .toXProcessing(invocation.processingEnv)
                    )
                    .isEqualTo(kotlinClass)
                assertThat(
                        invocation
                            .getKspTypeElement("JavaClass")
                            .toXProcessing(invocation.processingEnv)
                    )
                    .isEqualTo(javaClass)
            } else {
                assertThat(kotlinClass.toJavac())
                    .isEqualTo(invocation.getJavacTypeElement("KotlinClass"))
                assertThat(javaClass.toJavac())
                    .isEqualTo(invocation.getJavacTypeElement("JavaClass"))

                assertThat(
                        invocation
                            .getJavacTypeElement("KotlinClass")
                            .toXProcessing(invocation.processingEnv)
                    )
                    .isEqualTo(kotlinClass)
                assertThat(
                        invocation
                            .getJavacTypeElement("JavaClass")
                            .toXProcessing(invocation.processingEnv)
                    )
                    .isEqualTo(javaClass)
            }
        }
    }

    @Test
    fun executableElement() {
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                assertThat(
                        (kotlinClass.getConstructors() + kotlinClass.getDeclaredMethods())
                            .filterNot { it is KspSyntheticPropertyMethodElement }
                            .map { it.toKS() }
                    )
                    .containsExactlyElementsIn(
                        invocation.getKspTypeElement("KotlinClass").getDeclaredFunctions().toList()
                    )

                assertThat(
                        (javaClass.getConstructors() + javaClass.getDeclaredMethods()).map {
                            it.toKS()
                        }
                    )
                    .containsExactlyElementsIn(
                        invocation.getKspTypeElement("JavaClass").getDeclaredFunctions().toList()
                    )

                val kotlinFoo =
                    invocation.getKspTypeElement("KotlinClass").getDeclaredFunctions().first {
                        it.simpleName.asString() == "foo"
                    }
                assertThat(kotlinFoo.toXProcessing(invocation.processingEnv))
                    .isEqualTo(kotlinClass.getDeclaredMethodByJvmName("foo"))
                val javaFoo =
                    invocation.getKspTypeElement("JavaClass").getDeclaredFunctions().first {
                        it.simpleName.asString() == "foo"
                    }
                assertThat(javaFoo.toXProcessing(invocation.processingEnv))
                    .isEqualTo(javaClass.getDeclaredMethodByJvmName("foo"))
            } else {
                assertThat(kotlinClass.getDeclaredMethods().map { it.toJavac() })
                    .containsExactlyElementsIn(
                        ElementFilter.methodsIn(
                            invocation.getJavacTypeElement("KotlinClass").enclosedElements
                        )
                    )
                assertThat(javaClass.getDeclaredMethods().map { it.toJavac() })
                    .containsExactlyElementsIn(
                        ElementFilter.methodsIn(
                            invocation.getJavacTypeElement("JavaClass").enclosedElements
                        )
                    )

                val kotlinFoo =
                    ElementFilter.methodsIn(
                            invocation.getJavacTypeElement("KotlinClass").enclosedElements
                        )
                        .first { it.simpleName.toString() == "foo" }
                assertThat(kotlinFoo.toXProcessing(invocation.processingEnv))
                    .isEqualTo(kotlinClass.getDeclaredMethodByJvmName("foo"))
                val javaFoo =
                    ElementFilter.methodsIn(
                            invocation.getJavacTypeElement("JavaClass").enclosedElements
                        )
                        .first { it.simpleName.toString() == "foo" }
                assertThat(javaFoo.toXProcessing(invocation.processingEnv))
                    .isEqualTo(javaClass.getDeclaredMethodByJvmName("foo"))
            }
        }
    }

    @Test
    fun variableElement_field() {
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                assertThat(kotlinClass.getDeclaredFields().map { it.toKS() })
                    .containsExactlyElementsIn(
                        invocation.getKspTypeElement("KotlinClass").getAllProperties().toList()
                    )
                assertThat(javaClass.getDeclaredFields().map { it.toKS() })
                    .containsExactlyElementsIn(
                        invocation.getKspTypeElement("JavaClass").getAllProperties().toList()
                    )

                val kotlinField =
                    invocation.getKspTypeElement("KotlinClass").getAllProperties().first {
                        it.simpleName.asString() == "field"
                    }
                assertThat(kotlinField.toXProcessing(invocation.processingEnv))
                    .isEqualTo(kotlinClass.getDeclaredField("field"))
                val javaField =
                    invocation.getKspTypeElement("JavaClass").getAllProperties().first {
                        it.simpleName.asString() == "field"
                    }
                assertThat(javaField.toXProcessing(invocation.processingEnv))
                    .isEqualTo(javaClass.getDeclaredField("field"))
            } else {
                assertThat(kotlinClass.getDeclaredFields().map { it.toJavac() })
                    .containsExactlyElementsIn(
                        ElementFilter.fieldsIn(
                            invocation.getJavacTypeElement("KotlinClass").enclosedElements
                        )
                    )
                assertThat(javaClass.getDeclaredFields().map { it.toJavac() })
                    .containsExactlyElementsIn(
                        ElementFilter.fieldsIn(
                            invocation.getJavacTypeElement("JavaClass").enclosedElements
                        )
                    )

                val kotlinField =
                    ElementFilter.fieldsIn(
                            invocation.getJavacTypeElement("KotlinClass").enclosedElements
                        )
                        .first { it.simpleName.toString() == "field" }
                assertThat(kotlinField.toXProcessing(invocation.processingEnv))
                    .isEqualTo(kotlinClass.getDeclaredField("field"))
                val javaField =
                    ElementFilter.fieldsIn(
                            invocation.getJavacTypeElement("JavaClass").enclosedElements
                        )
                        .first { it.simpleName.toString() == "field" }
                assertThat(javaField.toXProcessing(invocation.processingEnv))
                    .isEqualTo(javaClass.getDeclaredField("field"))
            }
        }
    }

    @Test
    fun variableElement_parameter() {
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                assertThat(
                        kotlinClass.getDeclaredMethodByJvmName("foo").parameters.map { it.toKS() }
                    )
                    .containsExactlyElementsIn(
                        invocation
                            .getKspTypeElement("KotlinClass")
                            .getAllFunctions()
                            .first { it.simpleName.asString() == "foo" }
                            .parameters
                    )
                assertThat(javaClass.getDeclaredMethodByJvmName("foo").parameters.map { it.toKS() })
                    .containsExactlyElementsIn(
                        invocation
                            .getKspTypeElement("JavaClass")
                            .getAllFunctions()
                            .first { it.simpleName.asString() == "foo" }
                            .parameters
                    )

                val kotlinParam =
                    invocation
                        .getKspTypeElement("KotlinClass")
                        .getAllFunctions()
                        .first { it.simpleName.asString() == "foo" }
                        .parameters
                        .first()
                assertThat(kotlinParam.toXProcessing(invocation.processingEnv))
                    .isEqualTo(kotlinClass.getDeclaredMethodByJvmName("foo").parameters.first())
                val javaParam =
                    invocation
                        .getKspTypeElement("JavaClass")
                        .getAllFunctions()
                        .first { it.simpleName.asString() == "foo" }
                        .parameters
                        .first()
                assertThat(javaParam.toXProcessing(invocation.processingEnv))
                    .isEqualTo(javaClass.getDeclaredMethodByJvmName("foo").parameters.first())
            } else {
                assertThat(
                        kotlinClass.getDeclaredMethodByJvmName("foo").parameters.map {
                            it.toJavac()
                        }
                    )
                    .containsExactlyElementsIn(
                        ElementFilter.methodsIn(
                                invocation.getJavacTypeElement("KotlinClass").enclosedElements
                            )
                            .first { it.simpleName.toString() == "foo" }
                            .parameters
                    )
                assertThat(
                        javaClass.getDeclaredMethodByJvmName("foo").parameters.map { it.toJavac() }
                    )
                    .containsExactlyElementsIn(
                        ElementFilter.methodsIn(
                                invocation.getJavacTypeElement("JavaClass").enclosedElements
                            )
                            .first { it.simpleName.toString() == "foo" }
                            .parameters
                    )

                val kotlinParam =
                    ElementFilter.methodsIn(
                            invocation.getJavacTypeElement("KotlinClass").enclosedElements
                        )
                        .first { it.simpleName.toString() == "foo" }
                        .parameters
                        .first()
                assertThat(kotlinParam.toXProcessing(invocation.processingEnv))
                    .isEqualTo(kotlinClass.getDeclaredMethodByJvmName("foo").parameters.first())
                val javaParam =
                    ElementFilter.methodsIn(
                            invocation.getJavacTypeElement("JavaClass").enclosedElements
                        )
                        .first { it.simpleName.toString() == "foo" }
                        .parameters
                        .first()
                assertThat(javaParam.toXProcessing(invocation.processingEnv))
                    .isEqualTo(javaClass.getDeclaredMethodByJvmName("foo").parameters.first())
            }
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun annotation() {
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .toKS()
                    )
                    .isEqualTo(
                        invocation.getKspTypeElement("KotlinClass").annotations.first {
                            it.shortName.asString() == "TestSuppressWarnings"
                        }
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .toKS()
                    )
                    .isEqualTo(
                        invocation.getKspTypeElement("JavaClass").annotations.first {
                            it.shortName.asString() == "TestSuppressWarnings"
                        }
                    )

                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .qualifiedName
                    )
                    .isEqualTo(
                        invocation
                            .getKspTypeElement("KotlinClass")
                            .annotations
                            .first { it.shortName.asString() == "TestSuppressWarnings" }
                            .toXProcessing(invocation.processingEnv)
                            .qualifiedName
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .qualifiedName
                    )
                    .isEqualTo(
                        invocation
                            .getKspTypeElement("JavaClass")
                            .annotations
                            .first { it.shortName.asString() == "TestSuppressWarnings" }
                            .toXProcessing(invocation.processingEnv)
                            .qualifiedName
                    )
            } else {
                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .toJavac()
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("KotlinClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .toJavac()
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("JavaClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                    )

                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .qualifiedName
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("KotlinClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                            .toXProcessing(invocation.processingEnv)
                            .qualifiedName
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .qualifiedName
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("JavaClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                            .toXProcessing(invocation.processingEnv)
                            .qualifiedName
                    )
            }
        }
    }

    @Test
    fun annotationValues() {
        runProcessorTest(
            sources = listOf(kotlinSrc, javaSrc),
            // Not yet implemented: KSValueArgumentImpl.getParent
            kotlincArguments = KOTLINC_LANGUAGE_1_9_ARGS
        ) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .toKS()
                    )
                    .isEqualTo(
                        invocation
                            .getKspTypeElement("KotlinClass")
                            .annotations
                            .first { it.shortName.asString() == "TestSuppressWarnings" }
                            .arguments
                            .first()
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .toKS()
                    )
                    .isEqualTo(
                        invocation
                            .getKspTypeElement("JavaClass")
                            .annotations
                            .first { it.shortName.asString() == "TestSuppressWarnings" }
                            .arguments
                            .first()
                    )

                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .name
                    )
                    .isEqualTo(
                        invocation
                            .getKspTypeElement("KotlinClass")
                            .annotations
                            .first { it.shortName.asString() == "TestSuppressWarnings" }
                            .arguments
                            .first()
                            .toXProcessing(invocation.processingEnv)
                            .name
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .name
                    )
                    .isEqualTo(
                        invocation
                            .getKspTypeElement("JavaClass")
                            .annotations
                            .first { it.shortName.asString() == "TestSuppressWarnings" }
                            .arguments
                            .first()
                            .toXProcessing(invocation.processingEnv)
                            .name
                    )
            } else {
                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .toJavac()
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("KotlinClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                            .elementValues
                            .toList()
                            .first()
                            .second
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .toJavac()
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("JavaClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                            .elementValues
                            .toList()
                            .first()
                            .second
                    )

                assertThat(
                        kotlinClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .name
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("KotlinClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                            .elementValues
                            .toList()
                            .first()
                            .let {
                                it.second.toXProcessing(it.first, invocation.processingEnv).name
                            }
                    )
                assertThat(
                        javaClass
                            .requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))
                            .annotationValues
                            .first()
                            .name
                    )
                    .isEqualTo(
                        MoreElements.getAnnotationMirror(
                                invocation.getJavacTypeElement("JavaClass"),
                                TestSuppressWarnings::class.java
                            )
                            .get()
                            .elementValues
                            .toList()
                            .first()
                            .let {
                                it.second.toXProcessing(it.first, invocation.processingEnv).name
                            }
                    )
            }
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun customFiler() {
        var runCount = 0
        runKaptTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val className = ClassName.get("foo.bar", "ToBeGenerated")
            if (invocation.processingEnv.findTypeElement(className.toString()) == null) {
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
                assertThat(invocation.processingEnv.findTypeElement(className.toString()))
                    .isNotNull()
            }
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun customMessager() {
        runKaptTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            // Check that we can create a custom messager and toJavac() returns it
            val customMessager =
                object : Messager by invocation.processingEnv.messager.toJavac() {
                    override fun printMessage(kind: Diagnostic.Kind?, msg: CharSequence?) {
                        // We have to use the XMessager from XProcessingEnv here so that it runs the
                        // hooks that are attached to the testing infrastructure. Otherwise, the
                        // error
                        // is produced by not recorded. We may want to add a method to XMessager to
                        // copy
                        // the watchers from another messager, e.g.
                        // XMessager#copyWatchers(XMessager)
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

    @Suppress("DEPRECATION")
    @Test
    fun getProcessingEnvTest() {
        runProcessorTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
                    annotation class FooAnnotation(val value: String)
                    @FooAnnotation("someValue")
                    interface Foo {
                        fun method()
                    }
                    """
                            .trimIndent()
                    )
                )
        ) { invocation ->
            val foo = invocation.processingEnv.requireTypeElement("Foo")
            assertThat(foo.getProcessingEnv()).isEqualTo(invocation.processingEnv)

            val fooType = foo.type
            assertThat(fooType.getProcessingEnv()).isEqualTo(invocation.processingEnv)

            val fooAnnotation = foo.requireAnnotation(ClassName.get("", "FooAnnotation"))
            assertThat(fooAnnotation.getProcessingEnv()).isEqualTo(invocation.processingEnv)

            val fooAnnotationValue = fooAnnotation.getAnnotationValue("value")
            assertThat(fooAnnotationValue.getProcessingEnv()).isEqualTo(invocation.processingEnv)

            val fooMethodType = foo.getDeclaredMethodByJvmName("method").executableType
            assertThat(fooMethodType.getProcessingEnv()).isEqualTo(invocation.processingEnv)
        }
    }

    @Test
    fun xTypeName() {
        runProcessorTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            val kotlinClass = invocation.processingEnv.requireTypeElement("KotlinClass")
            val javaClass = invocation.processingEnv.requireTypeElement("JavaClass")

            if (invocation.isKsp) {
                val kotlinElement = invocation.getKspTypeElement("KotlinClass")
                val javaElement = invocation.getKspTypeElement("JavaClass")
                assertThat(kotlinElement.toXProcessing(invocation.processingEnv).asClassName())
                    .isEqualTo(kotlinClass.asClassName())
                assertThat(javaElement.toXProcessing(invocation.processingEnv).asClassName())
                    .isEqualTo(javaClass.asClassName())
                assertThat(
                        kotlinElement
                            .asType(emptyList())
                            .toXProcessing(invocation.processingEnv)
                            .asTypeName()
                    )
                    .isEqualTo(kotlinClass.asClassName())
                assertThat(
                        javaElement
                            .asType(emptyList())
                            .toXProcessing(invocation.processingEnv)
                            .asTypeName()
                    )
                    .isEqualTo(javaClass.asClassName())
            } else {
                val kotlinElement = invocation.getJavacTypeElement("KotlinClass")
                val javaElement = invocation.getJavacTypeElement("JavaClass")
                assertThat(kotlinElement.toXProcessing(invocation.processingEnv).asClassName())
                    .isEqualTo(kotlinClass.asClassName())
                assertThat(javaElement.toXProcessing(invocation.processingEnv).asClassName())
                    .isEqualTo(javaClass.asClassName())
                assertThat(
                        kotlinElement.asType().toXProcessing(invocation.processingEnv).asTypeName()
                    )
                    .isEqualTo(kotlinClass.asClassName())
                assertThat(
                        javaElement.asType().toXProcessing(invocation.processingEnv).asTypeName()
                    )
                    .isEqualTo(javaClass.asClassName())
            }
        }
    }

    private fun XTestInvocation.getJavacTypeElement(fqn: String) =
        (this.processingEnv as JavacProcessingEnv).delegate.elementUtils.getTypeElement(fqn)

    private fun XTestInvocation.getKspTypeElement(fqn: String) =
        (this.processingEnv as KspProcessingEnv).resolver.let { resolver ->
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(fqn))
        }!!
}
