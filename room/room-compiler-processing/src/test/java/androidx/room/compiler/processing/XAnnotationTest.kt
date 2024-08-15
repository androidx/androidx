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
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.compat.XConverters.toJavac
import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults
import androidx.room.compiler.processing.testcode.JavaAnnotationWithEnum
import androidx.room.compiler.processing.testcode.JavaAnnotationWithEnumArray
import androidx.room.compiler.processing.testcode.JavaAnnotationWithPrimitiveArray
import androidx.room.compiler.processing.testcode.JavaAnnotationWithTypeReferences
import androidx.room.compiler.processing.testcode.JavaEnum
import androidx.room.compiler.processing.testcode.MainAnnotation
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.testcode.RepeatableJavaAnnotation
import androidx.room.compiler.processing.testcode.RepeatableKotlinAnnotation
import androidx.room.compiler.processing.testcode.TestSuppressWarnings
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.asJTypeName
import androidx.room.compiler.processing.util.asKTypeName
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getDeclaredField
import androidx.room.compiler.processing.util.getDeclaredMethodByJvmName
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethodByJvmName
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import com.squareup.kotlinpoet.javapoet.JAnnotationSpec
import com.squareup.kotlinpoet.javapoet.JClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// used in typealias test
typealias OtherAnnotationTypeAlias = OtherAnnotation

@RunWith(Parameterized::class)
class XAnnotationTest(private val preCompiled: Boolean) {
    private fun runTest(
        sources: List<Source>,
        kotlincArgs: List<String> = emptyList(),
        handler: (XTestInvocation) -> Unit
    ) {
        if (preCompiled) {
            val compiled = compileFiles(sources)
            val hasKotlinSources = sources.any { it is Source.KotlinSource }
            val kotlinSources =
                if (hasKotlinSources) {
                    listOf(Source.kotlin("placeholder.kt", "class PlaceholderKotlin"))
                } else {
                    emptyList()
                }
            val newSources =
                kotlinSources +
                    Source.java("PlaceholderJava", "public class " + "PlaceholderJava {}")
            runProcessorTest(
                sources = newSources,
                handler = handler,
                classpath = compiled,
                kotlincArguments = kotlincArgs
            )
        } else {
            runProcessorTest(sources = sources, handler = handler, kotlincArguments = kotlincArgs)
        }
    }

    @Test
    fun typeParameterAnnotationsOnFunction() {
        val kotlinSource =
            Source.kotlin(
                "foo.bar.Subject.kt",
                """
            package foo.bar
            import kotlin.collections.*

            @Target(AnnotationTarget.TYPE)
            annotation class SomeAnnotation(val value: String)

            class Subject {
                fun myFunction(): Map<@SomeAnnotation("someString") Int, Int> {
                    return emptyMap()
                }
            }
            """
                    .trimIndent()
            )
        val javaSource =
            Source.java(
                "foo.bar.Subject",
                """
            package foo.bar;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.Map;
            import java.util.HashMap;

            @Target(ElementType.TYPE_USE)
            @interface SomeAnnotation {
                String value();
            }

            class Subject {
                Map<@SomeAnnotation("someString") Integer, Integer> myFunction() {
                    return new HashMap<>();
                }
            }
            """
                    .trimIndent()
            )

        listOf(javaSource, kotlinSource).forEach { source ->
            runTest(sources = listOf(source)) { invocation ->
                if (!invocation.isKsp) return@runTest
                val subject = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
                val method = subject.getMethodByJvmName("myFunction")
                val firstArg = method.returnType.typeArguments.first()
                val annotation = firstArg.getAllAnnotations().first()
                assertThat(annotation.name).isEqualTo("SomeAnnotation")

                assertThat(annotation.annotationValues.first().value).isEqualTo("someString")
            }
        }
    }

    @Test
    fun testJvmNameAnnotationValue() {
        val kotlinSrc =
            Source.kotlin(
                "MyAnnotation.kt",
                """
            @Target(AnnotationTarget.CLASS)
            annotation class MyAnnotation(
                @get:JvmName("stringParameter")
                val stringParam: String,
                val intParam: Int,
                @get:JvmName("longParameter")
                val longParam: Long
            )
            """
                    .trimIndent()
            )
        val javaSrc =
            Source.java(
                "Foo",
                """
            @MyAnnotation(stringParameter = "1", intParam = 2, longParameter = 3)
            public class Foo {}
            """
                    .trimIndent()
            )
        // https://github.com/google/ksp/issues/1890
        runTest(sources = listOf(javaSrc, kotlinSrc), kotlincArgs = KOTLINC_LANGUAGE_1_9_ARGS) {
            invocation ->
            val typeElement = invocation.processingEnv.requireTypeElement("Foo")
            val annotation =
                typeElement.getAllAnnotations().single { it.qualifiedName == "MyAnnotation" }
            assertThat(annotation.annotationValues.map { it.value })
                .containsExactly("1", 2, 3.toLong())
                .inOrder()
        }
    }

    @Test
    fun readsAnnotationsDeclaredInSources() {
        val source =
            Source.kotlin(
                "MyClass.kt",
                """
            annotation class MyAnnotation1(val bar: Int)
            @MyAnnotation1(bar = 1)
            class MyClass
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(source),
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyClass")

            val allAnnotations =
                element.getAllAnnotations().filterNot {
                    // Drop metadata annotation in kapt
                    it.name == "Metadata"
                }
            assertThat(allAnnotations).hasSize(1)

            val annotation1 = allAnnotations[0]
            assertThat(annotation1.name).isEqualTo("MyAnnotation1")
            assertThat(annotation1.qualifiedName).isEqualTo("MyAnnotation1")
            assertThat(annotation1.type.typeElement)
                .isEqualTo(invocation.processingEnv.requireTypeElement("MyAnnotation1"))
            assertThat(annotation1.get<Int>("bar")).isEqualTo(1)
            assertThat(annotation1.annotationValues).hasSize(1)
            assertThat(annotation1.annotationValues.first().name).isEqualTo("bar")
            assertThat(annotation1.annotationValues.first().value).isEqualTo(1)
        }
    }

    @Test
    fun annotationSpec() {
        val source =
            Source.java(
                "test.MyClass",
                """
            package test;
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings;
            @TestSuppressWarnings("test")
            public class MyClass {}
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(source),
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("test.MyClass")
            val annotation =
                element.requireAnnotation(JClassName.get(TestSuppressWarnings::class.java))
            if (!invocation.isKsp) {
                assertThat(annotation.toAnnotationSpec())
                    .isEqualTo(JAnnotationSpec.get(annotation.toJavac()))
            }
        }
    }

    @Test
    fun getAnnotationsAnnotatedWith() {
        val source =
            Source.kotlin(
                "MyClass.kt",
                """
            package foo.bar

            @Target(AnnotationTarget.ANNOTATION_CLASS)
            @Retention(AnnotationRetention.SOURCE)
            annotation class SourceAnnotation

            @Target(AnnotationTarget.ANNOTATION_CLASS)
            @Retention(AnnotationRetention.BINARY)
            annotation class BinaryAnnotation

            @Target(AnnotationTarget.ANNOTATION_CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            annotation class RuntimeAnnotation

            @SourceAnnotation
            @BinaryAnnotation
            @RuntimeAnnotation
            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            annotation class Foo

            @Foo
            class MyClass
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(source),
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.MyClass")

            val annotationsForAnnotations =
                if (preCompiled) {
                    // Source level annotations are gone if it's pre-compiled
                    listOf("BinaryAnnotation", "RuntimeAnnotation")
                } else {
                    listOf("SourceAnnotation", "BinaryAnnotation", "RuntimeAnnotation")
                }

            annotationsForAnnotations.forEach {
                val annotations = element.getAnnotationsAnnotatedWith(JClassName.get("foo.bar", it))
                assertThat(annotations).hasSize(1)
                val annotation = annotations.first()
                assertThat(annotation.name).isEqualTo("Foo")
                assertThat(annotation.qualifiedName).isEqualTo("foo.bar.Foo")
                assertThat(annotation.type.typeElement)
                    .isEqualTo(invocation.processingEnv.requireTypeElement("foo.bar.Foo"))
            }
        }
    }

    @Test
    fun annotationsInClassPathCanBeBoxed() {
        val source =
            Source.kotlin(
                "MyClass.kt",
                """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("a", "b")
            class MyClass
            """
                    .trimIndent()
            )
        runTest(sources = listOf(source)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyClass")
            val annotation = element.requireAnnotation<TestSuppressWarnings>()
            assertThat(annotation.name).isEqualTo(TestSuppressWarnings::class.simpleName)
            assertThat(annotation.qualifiedName)
                .isEqualTo(TestSuppressWarnings::class.qualifiedName)
            assertThat(annotation.type.typeElement)
                .isEqualTo(invocation.processingEnv.requireTypeElement(TestSuppressWarnings::class))
            assertThat(annotation.asAnnotationBox<TestSuppressWarnings>().value.value)
                .isEqualTo(arrayOf("a", "b"))
        }
    }

    @Test
    fun readSimpleAnnotationValue() {
        val source =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings;
            @TestSuppressWarnings({"warning1", "warning 2"})
            public class Baz {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(source)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation = element.requireAnnotation<TestSuppressWarnings>()

            val argument = annotation.getAnnotationValue("value")
            assertThat(argument.name).isEqualTo("value")
            assertThat(argument.asStringList()).isEqualTo(listOf("warning1", "warning 2"))
        }
    }

    @Test
    fun readSimpleAnnotationValueFromClassName() {
        val source =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings;
            @TestSuppressWarnings({"warning1", "warning 2"})
            public class Baz {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(source)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation =
                element.requireAnnotation(JClassName.get(TestSuppressWarnings::class.java))

            val argument = annotation.getAnnotationValue("value")
            assertThat(argument.name).isEqualTo("value")
            assertThat(argument.asStringList()).isEqualTo(listOf("warning1", "warning 2"))
        }
    }

    @Test
    fun typeReference_javac() {
        val mySource =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.MainAnnotation;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;
            @MainAnnotation(
                typeList = {String.class, Integer.class},
                singleType = Long.class,
                intMethod = 3,
                otherAnnotationArray = {
                    @OtherAnnotation(
                        value = "other list 1"
                    ),
                    @OtherAnnotation("other list 2"),
                },
                singleOtherAnnotation = @OtherAnnotation("other single")
            )
            public class Baz {
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithoutKsp(listOf(mySource)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation = element.requireAnnotation<MainAnnotation>()

            assertThat(annotation.get<List<XType>>("typeList"))
                .containsExactly(
                    invocation.processingEnv.requireType(java.lang.String::class),
                    invocation.processingEnv.requireType(Integer::class)
                )
            assertThat(annotation.get<XType>("singleType"))
                .isEqualTo(invocation.processingEnv.requireType(java.lang.Long::class))

            assertThat(annotation.get<Int>("intMethod")).isEqualTo(3)
            annotation.get<XAnnotation>("singleOtherAnnotation").let { other ->
                assertThat(other.name).isEqualTo(OtherAnnotation::class.simpleName)
                assertThat(other.qualifiedName).isEqualTo(OtherAnnotation::class.qualifiedName)
                assertThat(other.get<String>("value")).isEqualTo("other single")
            }
            annotation.get<List<XAnnotation>>("otherAnnotationArray").let { boxArray ->
                assertThat(boxArray).hasSize(2)
                assertThat(boxArray[0].get<String>("value")).isEqualTo("other list 1")
                assertThat(boxArray[1].get<String>("value")).isEqualTo("other list 2")
            }
        }
    }

    @Test
    fun readSimpleAnnotationValue_kotlin() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("warning1", "warning 2")
            class Subject {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(source)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = element.requireAnnotation<TestSuppressWarnings>()

            assertThat(annotation).isNotNull()
            assertThat(annotation.get<List<String>>("value"))
                .isEqualTo(listOf("warning1", "warning 2"))
        }
    }

    @Test
    fun typeReference_kotlin() {
        val mySource =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.compiler.processing.testcode.MainAnnotation
            import androidx.room.compiler.processing.testcode.OtherAnnotation

            @MainAnnotation(
                typeList = [String::class, Int::class],
                singleType = Long::class,
                intMethod = 3,
                otherAnnotationArray = [
                    OtherAnnotation(
                        value = "other list 1"
                    ),
                    OtherAnnotation(
                        value = "other list 2"
                    )
                ],
                singleOtherAnnotation = OtherAnnotation("other single")
            )
            public class Subject {
            }
            """
                    .trimIndent()
            )
        runTest(listOf(mySource)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = element.requireAnnotation<MainAnnotation>()

            assertThat(annotation.get<List<XType>>("typeList").map { it.asTypeName() })
                .containsExactly(String::class.asClassName(), XTypeName.PRIMITIVE_INT)
            assertThat(annotation.get<XType>("singleType"))
                .isEqualTo(invocation.processingEnv.requireType(Long::class))

            assertThat(annotation.get<Int>("intMethod")).isEqualTo(3)
            annotation.get<XAnnotation>("singleOtherAnnotation").let { other ->
                assertThat(other.name).isEqualTo(OtherAnnotation::class.simpleName)
                assertThat(other.qualifiedName).isEqualTo(OtherAnnotation::class.qualifiedName)
                assertThat(other.get<String>("value")).isEqualTo("other single")
            }
            annotation.get<List<XAnnotation>>("otherAnnotationArray").let { boxArray ->
                assertThat(boxArray).hasSize(2)
                assertThat(boxArray[0].get<String>("value")).isEqualTo("other list 1")
                assertThat(boxArray[1].get<String>("value")).isEqualTo("other list 2")
            }
        }
    }

    @Test
    fun typeReferenceArray_singleItemInJava() {
        val src =
            Source.java(
                "Subject",
                """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithTypeReferences;
            @JavaAnnotationWithTypeReferences(String.class)
            class Subject {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            if (!invocation.isKsp) return@runTest
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = subject.requireAnnotation<JavaAnnotationWithTypeReferences>()
            val annotationValue = annotation.get<List<XType>>("value").single()
            assertThat(annotationValue.asTypeName().java).isEqualTo(String::class.asJTypeName())
        }
    }

    @Test
    fun propertyAnnotations() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.compiler.processing.testcode.OtherAnnotation
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            class Subject {
                @TestSuppressWarnings("onProp1")
                var prop1:Int = TODO()

                @get:TestSuppressWarnings("onGetter2")
                @set:TestSuppressWarnings("onSetter2")
                @field:TestSuppressWarnings("onField2")
                @setparam:TestSuppressWarnings("onSetterParam2")
                var prop2:Int = TODO()

                @get:TestSuppressWarnings("onGetter3")
                @set:TestSuppressWarnings("onSetter3")
                @setparam:TestSuppressWarnings("onSetterParam3")
                @setparam:KotlinTestQualifier
                var prop3:Int
                    @OtherAnnotation("_onGetter3")
                    get() = 3

                    @OtherAnnotation("_onSetter3")
                    set(@OtherAnnotation("_onSetterParam3") value) = Unit
            }

            @Target(AnnotationTarget.VALUE_PARAMETER)
            @Retention(AnnotationRetention.RUNTIME) annotation class KotlinTestQualifier
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")

            subject.getField("prop1").assertHasSuppressWithValue("onProp1")
            subject.getMethodByJvmName("getProp1").assertDoesNotHaveAnnotation()
            subject.getMethodByJvmName("setProp1").assertDoesNotHaveAnnotation()
            subject.getMethodByJvmName("setProp1").parameters.first().assertDoesNotHaveAnnotation()

            subject.getField("prop2").assertHasSuppressWithValue("onField2")
            subject.getMethodByJvmName("getProp2").assertHasSuppressWithValue("onGetter2")
            subject.getMethodByJvmName("setProp2").assertHasSuppressWithValue("onSetter2")
            subject
                .getMethodByJvmName("setProp2")
                .parameters
                .first()
                .assertHasSuppressWithValue("onSetterParam2")

            subject.getMethodByJvmName("getProp3").assertHasSuppressWithValue("onGetter3")
            subject.getMethodByJvmName("setProp3").assertHasSuppressWithValue("onSetter3")
            subject
                .getMethodByJvmName("setProp3")
                .parameters
                .first()
                .assertHasSuppressWithValue("onSetterParam3")
            val annotations =
                subject
                    .getMethodByJvmName("setProp3")
                    .parameters
                    .first()
                    .getAllAnnotations()
                    .filter { it.qualifiedName == "KotlinTestQualifier" }

            assertThat(annotations).hasSize(1)

            assertThat(subject.getMethodByJvmName("getProp3").getOtherAnnotationValue())
                .isEqualTo("_onGetter3")
            assertThat(subject.getMethodByJvmName("setProp3").getOtherAnnotationValue())
                .isEqualTo("_onSetter3")
            val otherAnnotationValue =
                subject.getMethodByJvmName("setProp3").parameters.first().getOtherAnnotationValue()
            assertThat(otherAnnotationValue).isEqualTo("_onSetterParam3")
        }
    }

    @Test
    fun methodAnnotations() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.compiler.processing.testcode.OtherAnnotation
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            class Subject {
                fun noAnnotations(x:Int): Unit = TODO()
                @TestSuppressWarnings("onMethod")
                fun methodAnnotation(
                    @TestSuppressWarnings("onParam") annotated:Int,
                    notAnnotated:Int
                ): Unit = TODO()
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getMethodByJvmName("noAnnotations").let { method ->
                method.assertDoesNotHaveAnnotation()
                method.getParameter("x").assertDoesNotHaveAnnotation()
            }
            subject.getMethodByJvmName("methodAnnotation").let { method ->
                method.assertHasSuppressWithValue("onMethod")
                method.getParameter("annotated").assertHasSuppressWithValue("onParam")
                method.getParameter("notAnnotated").assertDoesNotHaveAnnotation()
            }
        }
    }

    @Test
    fun constructorParameterAnnotations() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("onClass")
            data class Subject(
                @field:TestSuppressWarnings("onField")
                @param:TestSuppressWarnings("onConstructorParam")
                @get:TestSuppressWarnings("onGetter")
                @set:TestSuppressWarnings("onSetter")
                var x:Int
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.assertHasSuppressWithValue("onClass")
            assertThat(subject.getConstructors()).hasSize(1)
            val constructor = subject.getConstructors().single()
            constructor.getParameter("x").assertHasSuppressWithValue("onConstructorParam")
            subject.getMethodByJvmName("getX").assertHasSuppressWithValue("onGetter")
            subject.getMethodByJvmName("setX").assertHasSuppressWithValue("onSetter")
            subject.getField("x").assertHasSuppressWithValue("onField")
        }
    }

    @Test
    fun defaultValues() {
        val kotlinSrc =
            Source.kotlin(
                "KotlinClass.kt",
                """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults
            @JavaAnnotationWithDefaults
            class KotlinClass
            """
                    .trimIndent()
            )
        val javaSrc =
            Source.java(
                "JavaClass",
                """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults;
            @JavaAnnotationWithDefaults
            class JavaClass {}
            """
                    .trimIndent()
            )
        runTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            listOf("KotlinClass", "JavaClass")
                .map { invocation.processingEnv.requireTypeElement(it) }
                .forEach { typeElement ->
                    val annotation = typeElement.requireAnnotation<JavaAnnotationWithDefaults>()

                    assertThat(annotation.defaultValues.map { it.name })
                        .containsExactly(
                            "stringVal",
                            "stringArrayVal",
                            "typeVal",
                            "typeArrayVal",
                            "intVal",
                            "intArrayVal",
                            "enumVal",
                            "enumArrayVal",
                            "otherAnnotationVal",
                            "otherAnnotationArrayVal"
                        )
                        .inOrder()

                    assertThat(annotation.get<Int>("intVal")).isEqualTo(3)
                    assertThat(annotation.get<List<Int>>("intArrayVal")).isEqualTo(listOf(1, 3, 5))
                    assertThat(annotation.get<List<String>>("stringArrayVal"))
                        .isEqualTo(listOf("x", "y"))
                    assertThat(annotation.get<String>("stringVal")).isEqualTo("foo")
                    assertThat(annotation.getAsType("typeVal").rawType.asTypeName().java)
                        .isEqualTo(HashMap::class.asJTypeName())
                    assertThat(
                            annotation.getAsTypeList("typeArrayVal").map {
                                it.rawType.asTypeName().java
                            }
                        )
                        .isEqualTo(listOf(LinkedHashMap::class.asJTypeName()))
                    if (invocation.isKsp) {
                        assertThat(annotation.getAsType("typeVal").rawType.asTypeName().kotlin)
                            .isEqualTo(HashMap::class.asKTypeName())
                        assertThat(
                                annotation.getAsTypeList("typeArrayVal").map {
                                    it.rawType.asTypeName().kotlin
                                }
                            )
                            .isEqualTo(listOf(LinkedHashMap::class.asKTypeName()))
                    } else {
                        assertThat(annotation.toAnnotationSpec(includeDefaultValues = false))
                            .isEqualTo(JAnnotationSpec.get(annotation.toJavac()))
                        assertThat(annotation.toAnnotationSpec())
                            .isNotEqualTo(JAnnotationSpec.get(annotation.toJavac()))
                    }

                    val enumValueEntry = annotation.getAsEnum("enumVal")
                    assertThat(enumValueEntry.name).isEqualTo("DEFAULT")
                    val javaEnumType = invocation.processingEnv.requireTypeElement(JavaEnum::class)
                    assertThat(enumValueEntry.enclosingElement).isEqualTo(javaEnumType)
                    val enumList = annotation.getAsEnumList("enumArrayVal")
                    assertThat(enumList[0].name).isEqualTo("VAL1")
                    assertThat(enumList[1].name).isEqualTo("VAL2")
                    assertThat(enumList[0].enclosingElement).isEqualTo(javaEnumType)
                    assertThat(enumList[1].enclosingElement).isEqualTo(javaEnumType)

                    // TODO: KSP mistakenly sees null for the value in a default annotation in
                    //  sources. https://github.com/google/ksp/issues/53
                    if (!invocation.isKsp && !preCompiled) {

                        annotation.getAsAnnotation("otherAnnotationVal").let { other ->
                            assertThat(other.name).isEqualTo("OtherAnnotation")
                            assertThat(other.get<String>("value")).isEqualTo("def")
                        }

                        annotation.getAsAnnotationList("otherAnnotationArrayVal").forEach { other ->
                            assertThat(other.name).isEqualTo("OtherAnnotation")
                            assertThat(other.get<String>("value")).isEqualTo("v1")
                        }
                    }
                }
        }
    }

    @Test
    fun javaPrimitiveArray() {
        // TODO: expand this test for other primitive types: 179081610
        val javaSrc =
            Source.java(
                "JavaSubject",
                """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithPrimitiveArray(intArray = {1, 2, 3})
                Object annotated1;
            }
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            import androidx.room.compiler.processing.testcode.*
            class KotlinSubject {
                @JavaAnnotationWithPrimitiveArray(intArray = [1, 2, 3])
                val annotated1:Any = TODO()
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(javaSrc, kotlinSrc)) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map { invocation.processingEnv.requireTypeElement(it) }
                .forEach { subject ->
                    val annotation =
                        subject
                            .getField("annotated1")
                            .requireAnnotation<JavaAnnotationWithPrimitiveArray>()
                    assertThat(annotation.get<List<Int>>("intArray")).isEqualTo(listOf(1, 2, 3))
                }
        }
    }

    @Test
    fun javaEnum() {
        val javaSrc =
            Source.java(
                "JavaSubject",
                """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithEnum(JavaEnum.VAL1)
                Object annotated1;
            }
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            import androidx.room.compiler.processing.testcode.*
            class KotlinSubject {
                @JavaAnnotationWithEnum(JavaEnum.VAL1)
                val annotated1: Any = TODO()
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(javaSrc, kotlinSrc)) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map { invocation.processingEnv.requireTypeElement(it) }
                .forEach { subject ->
                    val annotation =
                        subject.getField("annotated1").requireAnnotation<JavaAnnotationWithEnum>()
                    assertThat(annotation.getAsEnum("value").name).isEqualTo(JavaEnum.VAL1.name)
                }
        }
    }

    @Test
    fun javaEnumArray() {
        val javaSrc =
            Source.java(
                "JavaSubject",
                """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithEnumArray(enumArray = {JavaEnum.VAL1, JavaEnum.VAL2})
                Object annotated1;
            }
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            import androidx.room.compiler.processing.testcode.*;
            class KotlinSubject {
                @JavaAnnotationWithEnumArray(enumArray = [JavaEnum.VAL1, JavaEnum.VAL2])
                val annotated1: Any = TODO()
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(javaSrc, kotlinSrc)) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map { invocation.processingEnv.requireTypeElement(it) }
                .forEach { subject ->
                    val annotation =
                        subject
                            .getField("annotated1")
                            .requireAnnotation<JavaAnnotationWithEnumArray>()
                    assertThat(annotation.getAsEnumList("enumArray").map { it.name })
                        .isEqualTo(listOf(JavaEnum.VAL1.name, JavaEnum.VAL2.name))
                }
        }
    }

    @Test
    fun javaEnumArrayWithDefaultNameAndValue() {
        val annotationSource =
            Source.java(
                "foo.bar.MyAnnotation",
                """
            package foo.bar;
            public @interface MyAnnotation {
                MyEnum[] value() default {};
            }
            """
                    .trimIndent()
            )
        val enumSource =
            Source.java(
                "foo.bar.MyEnum",
                """
            package foo.bar;
            enum MyEnum {
                 Bar
            }
            """
                    .trimIndent()
            )
        val classSource =
            Source.java(
                "foo.bar.Subject",
                """
            package foo.bar;
            @MyAnnotation
            class Subject {}
            """
                    .trimIndent()
            )
        runTest(sources = listOf(annotationSource, enumSource, classSource)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("foo.bar.Subject")

            val annotations = subject.getAllAnnotations().filter { it.name == "MyAnnotation" }
            assertThat(annotations).hasSize(1)
        }
    }

    @Test
    fun repeatableAnnotation() {
        val javaSrc =
            Source.java(
                "JavaSubject",
                """
            import ${RepeatableJavaAnnotation::class.qualifiedName};
            @RepeatableJavaAnnotation("x")
            @RepeatableJavaAnnotation("y")
            @RepeatableJavaAnnotation("z")
            public class JavaSubject {}
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            import ${RepeatableKotlinAnnotation::class.qualifiedName}
            @RepeatableKotlinAnnotation("x")
            @RepeatableKotlinAnnotation("y")
            @RepeatableKotlinAnnotation("z")
            public class KotlinSubject
            """
                    .trimIndent()
            )
        // https://github.com/google/ksp/issues/1883
        runTest(sources = listOf(javaSrc, kotlinSrc), kotlincArgs = KOTLINC_LANGUAGE_1_9_ARGS) {
            invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map(invocation.processingEnv::requireTypeElement)
                .forEach { subject ->
                    val annotations =
                        subject.getAllAnnotations().filter {
                            it.name == "RepeatableJavaAnnotation" ||
                                it.name == "RepeatableKotlinAnnotation"
                        }
                    val values = annotations.map { it.get<String>("value") }
                    assertWithMessage(subject.qualifiedName)
                        .that(values)
                        .containsExactly("x", "y", "z")
                }
        }
    }

    @Test
    fun javaRepeatableAnnotation_notRepeated() {
        val javaSrc =
            Source.java(
                "JavaSubject",
                """
            import ${RepeatableJavaAnnotation::class.qualifiedName};
            @RepeatableJavaAnnotation("x")
            public class JavaSubject {}
            """
                    .trimIndent()
            )
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            import ${RepeatableJavaAnnotation::class.qualifiedName}
            @RepeatableJavaAnnotation("x")
            public class KotlinSubject
            """
                    .trimIndent()
            )
        runTest(sources = listOf(javaSrc, kotlinSrc)) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map(invocation.processingEnv::requireTypeElement)
                .forEach { subject ->
                    val annotations =
                        subject.getAllAnnotations().filter { it.name == "RepeatableJavaAnnotation" }
                    val values = annotations.map { it.get<String>("value") }
                    assertWithMessage(subject.qualifiedName).that(values).containsExactly("x")
                }
        }
    }

    @Test
    fun kotlinRepeatableAnnotation_notRepeated() {
        val kotlinSrc =
            Source.kotlin(
                "KotlinSubject.kt",
                """
            import ${RepeatableKotlinAnnotation::class.qualifiedName}
            @RepeatableKotlinAnnotation("x")
            public class KotlinSubject
            """
                    .trimIndent()
            )
        runTest(sources = listOf(kotlinSrc)) { invocation ->
            listOf("KotlinSubject").map(invocation.processingEnv::requireTypeElement).forEach {
                subject ->
                val annotations =
                    subject.getAllAnnotations().filter { it.name == "RepeatableKotlinAnnotation" }
                val values = annotations.map { it.get<String>("value") }
                assertWithMessage(subject.qualifiedName).that(values).containsExactly("x")
            }
        }
    }

    @Test
    fun typealiasAnnotation() {
        val source =
            Source.kotlin(
                "Subject.kt",
                """
            typealias SourceTypeAlias = ${OtherAnnotation::class.qualifiedName}
            @SourceTypeAlias("x")
            class Subject {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(source)) { invocation ->
            // TODO use getSymbolsWithAnnotation after
            // https://github.com/google/ksp/issues/506 is fixed
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = subject.getAnnotation(OtherAnnotation::class)
            assertThat(annotation).isNotNull()
            assertThat(annotation?.value?.value).isEqualTo("x")

            val annotation2 = subject.getAnnotation(OtherAnnotationTypeAlias::class)
            assertThat(annotation2).isNotNull()
            assertThat(annotation2?.value?.value).isEqualTo("x")
        }
    }

    @Test
    fun readPrimitiveAnnotationValueUsingClass() {
        val source =
            Source.java(
                "foo.bar.Baz",
                """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults;
            @JavaAnnotationWithDefaults(stringVal = "test")
            public class Baz {
            }
            """
                    .trimIndent()
            )
        runTest(sources = listOf(source)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation =
                element.requireAnnotation(JClassName.get(JavaAnnotationWithDefaults::class.java))

            assertThat(annotation.get<String>("stringVal")).isEqualTo("test")
            assertThat(annotation.get<Int>("intVal")).isEqualTo(3)

            // Also test reading theses values through getAs*() methods
            assertThat(annotation.getAsString("stringVal")).isEqualTo("test")
            assertThat(annotation.getAsInt("intVal")).isEqualTo(3)
        }
    }

    // This is testing the workaround for https://github.com/google/ksp/issues/1198
    @Test
    fun paramTargetInPrimaryCtorProperty() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
            package test
            class Subject(
                @MyAnnotation field: String,
                @MyAnnotation val valField: String,
                @MyAnnotation var varField: String,
            )
            @Target(AnnotationTarget.VALUE_PARAMETER)
            annotation class MyAnnotation
            """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            // Verifies the KspRoundEnv side of the workaround.
            if (!preCompiled) {
                val annotatedElements =
                    invocation.roundEnv.getElementsAnnotatedWith("test.MyAnnotation")
                assertThat(annotatedElements.all { it is XExecutableParameterElement }).isTrue()
                assertThat(annotatedElements.map { it.name })
                    .containsExactly("field", "valField", "varField")
            }

            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            val myAnnotation = invocation.processingEnv.requireTypeElement("test.MyAnnotation")

            val constructorParameters = subject.getConstructors().single().parameters
            assertThat(constructorParameters.map { it.name })
                .containsExactly("field", "valField", "varField")
            fun getCtorParameterAnnotationElements(paramName: String): List<XTypeElement> {
                return constructorParameters
                    .first { it.name == paramName }
                    .getAllAnnotations()
                    .map(XAnnotation::typeElement)
            }
            assertThat(getCtorParameterAnnotationElements("field")).contains(myAnnotation)
            assertThat(getCtorParameterAnnotationElements("valField")).contains(myAnnotation)
            assertThat(getCtorParameterAnnotationElements("varField")).contains(myAnnotation)

            assertThat(subject.getDeclaredFields().map(XFieldElement::name))
                .containsExactly("valField", "varField")
            fun getDeclaredFieldAnnotationElements(fieldName: String): List<XTypeElement> {
                return subject
                    .getDeclaredField(fieldName)
                    .getAllAnnotations()
                    .map(XAnnotation::typeElement)
            }
            assertThat(getDeclaredFieldAnnotationElements("valField")).doesNotContain(myAnnotation)
            assertThat(getDeclaredFieldAnnotationElements("varField")).doesNotContain(myAnnotation)
        }
    }

    @Test
    fun fieldTargetInPrimaryCtorProperty() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
            package test
            class Subject(
                @MyAnnotation val valField: String,
                @MyAnnotation var varField: String,
            )
            @Target(AnnotationTarget.FIELD)
            annotation class MyAnnotation
            """
                            .trimIndent()
                    )
                ),
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            val myAnnotation = invocation.processingEnv.requireTypeElement("test.MyAnnotation")

            val constructorParameters = subject.getConstructors().single().parameters
            assertThat(constructorParameters.map { it.name })
                .containsExactly("valField", "varField")
            fun getCtorParameterAnnotationElements(paramName: String): List<XTypeElement> {
                return constructorParameters
                    .first { it.name == paramName }
                    .getAllAnnotations()
                    .map(XAnnotation::typeElement)
            }
            assertThat(getCtorParameterAnnotationElements("valField")).doesNotContain(myAnnotation)
            assertThat(getCtorParameterAnnotationElements("varField")).doesNotContain(myAnnotation)

            assertThat(subject.getDeclaredFields().map(XFieldElement::name))
                .containsExactly("valField", "varField")
            fun getDeclaredFieldAnnotationElements(fieldName: String): List<XTypeElement> {
                return subject
                    .getDeclaredField(fieldName)
                    .getAllAnnotations()
                    .map(XAnnotation::typeElement)
            }
            assertThat(getDeclaredFieldAnnotationElements("valField")).contains(myAnnotation)
            assertThat(getDeclaredFieldAnnotationElements("varField")).contains(myAnnotation)
        }
    }

    @Test
    fun propertyTargetInPrimaryCtorProperty() {
        runTest(
            sources =
                listOf(
                    Source.kotlin(
                        "Foo.kt",
                        """
                    package test
                    class Subject(
                        @MyAnnotation val valField: String,
                        @MyAnnotation var varField: String,
                    )
                    @Target(AnnotationTarget.PROPERTY)
                    annotation class MyAnnotation
                    """
                            .trimIndent()
                    )
                ),
            // https://github.com/google/ksp/issues/1882
            kotlincArgs = KOTLINC_LANGUAGE_1_9_ARGS
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("test.Subject")
            val myAnnotation = invocation.processingEnv.requireTypeElement("test.MyAnnotation")

            val constructorParameters = subject.getConstructors().single().parameters
            assertThat(constructorParameters.map { it.name })
                .containsExactly("valField", "varField")
            fun getCtorParameterAnnotationElements(paramName: String): List<XTypeElement> {
                return constructorParameters
                    .first { it.name == paramName }
                    .getAllAnnotations()
                    .map(XAnnotation::typeElement)
            }
            assertThat(getCtorParameterAnnotationElements("valField")).doesNotContain(myAnnotation)
            assertThat(getCtorParameterAnnotationElements("varField")).doesNotContain(myAnnotation)

            assertThat(subject.getDeclaredFields().map(XFieldElement::name))
                .containsExactly("valField", "varField")
            fun getDeclaredFieldAnnotationElements(fieldName: String): List<XTypeElement> {
                return subject
                    .getDeclaredField(fieldName)
                    .getAllAnnotations()
                    .map(XAnnotation::typeElement)
            }
            if (!invocation.isKsp && preCompiled) {
                // KAPT places property annotations without targets on the property, which
                // then get put onto the synthetic $annotations method in the KAPT stub.
                // Unfortunately, synthetic methods can only be read when processing the
                // source so it's missing on precompiled class files:
                // https://youtrack.jetbrains.com/issue/KT-34684
                assertThat(getDeclaredFieldAnnotationElements("valField"))
                    .doesNotContain(myAnnotation)
                assertThat(getDeclaredFieldAnnotationElements("varField"))
                    .doesNotContain(myAnnotation)
            } else {
                assertThat(getDeclaredFieldAnnotationElements("valField")).contains(myAnnotation)
                assertThat(getDeclaredFieldAnnotationElements("varField")).contains(myAnnotation)
            }
        }
    }

    @Test
    fun typeAnnotations() {
        val kotlinSource =
            Source.kotlin(
                "foo.bar.Subject.kt",
                """
            package foo.bar

            interface Foo<T>
            open class FooImpl<T>
            class Bar

            @Target(AnnotationTarget.TYPE)
            annotation class A
            @Target(
                AnnotationTarget.CLASS,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.FIELD,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.VALUE_PARAMETER,
                AnnotationTarget.TYPE,
            )
            annotation class B
            @Target(
                AnnotationTarget.CLASS,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.FIELD,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.VALUE_PARAMETER,
            )
            annotation class C
            annotation class D

            @B @C @D
            class Subject @B @C @D constructor(
                @B @C @D param: @A @B Foo<@A @B Bar>
            ) : @A @B FooImpl<@A @B Bar>(), @A @B Foo<@A @B Bar> {
                @B @C @D val field: @A @B Foo<@A @B Bar> = TODO()
                @B @C @D fun method(
                    @B @C @D param: @A @B Foo<@A @B Bar>
                ): @A @B Foo<@A @B Bar> = TODO()
            }
            """
                    .trimIndent()
            )
        val javaSource =
            Source.java(
                "foo.bar.Subject",
                """
            package foo.bar;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.lang.annotation.Repeatable;

            interface Foo<T> {}
            class FooImpl<T> {}
            class Bar {}

            @Target({ElementType.TYPE_USE})
            @interface A {}
            @Target({
                ElementType.METHOD,
                ElementType.FIELD,
                ElementType.CONSTRUCTOR,
                ElementType.PARAMETER,
                ElementType.TYPE_USE
            })
            @interface B {}
            @Target({
                ElementType.METHOD,
                ElementType.FIELD,
                ElementType.CONSTRUCTOR,
                ElementType.PARAMETER,
                ElementType.TYPE,
            })
            @interface C {}
            @interface D {}

            @B @C @D
            class Subject extends @A @B FooImpl<@A @B Bar> implements @A @B Foo<@A @B Bar> {
                @A @B @C @D Foo<@A @B Bar> field;
                @B @C @D Subject(@A @B @C @D Foo<@A @B Bar> param) {}
                @A @B @C @D Foo<@A @B Bar> method(@A @B @C @D Foo<@A @B Bar> param) {
                    throw new RuntimeException();
                }
            }
            """
                    .trimIndent()
            )

        listOf(javaSource, kotlinSource).forEach { source ->
            // https://github.com/google/ksp/issues/1882
            runTest(sources = listOf(source), kotlincArgs = KOTLINC_LANGUAGE_1_9_ARGS) { invocation
                ->
                fun XAnnotated.getAllAnnotationTypeElements(): List<XTypeElement> {
                    return getAllAnnotations()
                        .filter {
                            !it.qualifiedName.contentEquals("org.jetbrains.annotations.NotNull")
                        }
                        .map { it.typeElement }
                }

                val subject = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
                val superClass = subject.superClass!!
                val superInterface = subject.superInterfaces.single()
                val field = subject.getDeclaredField("field")
                val method = subject.getDeclaredMethodByJvmName("method")
                val constructor = subject.getConstructors().single()
                val a = invocation.processingEnv.requireTypeElement("foo.bar.A")
                val b = invocation.processingEnv.requireTypeElement("foo.bar.B")
                val c = invocation.processingEnv.requireTypeElement("foo.bar.C")
                val d = invocation.processingEnv.requireTypeElement("foo.bar.D")

                // Check that the synthetic annotations method does not appear in the list of
                // declared methods.
                if (source == javaSource) {
                    assertThat(subject.getDeclaredMethods().map { it.name })
                        .containsExactly("method")
                } else {
                    if (invocation.isKsp || preCompiled) {
                        assertThat(subject.getDeclaredMethods().map { it.name })
                            .containsExactly("getField", "method")
                            .inOrder()
                    } else {
                        // TODO(b/290800523): Remove the synthetic annotations method from the list
                        //  of declared methods so that KAPT matches KSP.
                        assertThat(subject.getDeclaredMethods().map { it.name })
                            .containsExactly("getField", "getField\$annotations", "method")
                            .inOrder()
                    }
                }

                // Check the annotations on the elements
                mapOf(
                        "class" to subject,
                        "field" to field,
                        "method" to method,
                        "methodParameter" to method.parameters.single(),
                        "constructor" to constructor,
                        "constructorParameter" to constructor.parameters.single(),
                    )
                    .forEach { (desc, element) ->
                        if (
                            element == field &&
                                !invocation.isKsp &&
                                source == kotlinSource &&
                                preCompiled
                        ) {
                            // KAPT places property annotations without targets on the property,
                            // which
                            // then get put onto the synthetic $annotations method in the KAPT stub.
                            // Unfortunately, synthetic methods can only be read when processing the
                            // source so it's missing on precompiled class files:
                            // https://youtrack.jetbrains.com/issue/KT-34684
                            assertWithMessage("$desc element: $element")
                                .that(element.getAllAnnotationTypeElements())
                                .containsExactly(b, c)
                        } else {
                            assertWithMessage("$desc element: $element")
                                .that(
                                    // TODO(bcorso): Consider automatically removing kotlin.Metadata
                                    //  annotation so that KAPT and KSP agree, and exposing the
                                    // metadata
                                    //  explicitly via a property of the type/element.
                                    // Filter out kotlin.Metadata.
                                    element.getAllAnnotationTypeElements().filterNot {
                                        it.qualifiedName == "kotlin.Metadata"
                                    }
                                )
                                .containsExactly(b, c, d)
                        }
                    }

                // Check the annotations on the types and type arguments
                mapOf(
                        "superClass" to superClass,
                        "superClassArg" to superClass.typeArguments.single(),
                        "superInterface" to superInterface,
                        "superInterfaceArg" to superInterface.typeArguments.single(),
                        "field" to field.type,
                        "fieldArg" to field.type.typeArguments.single(),
                        "methodReturnType" to method.returnType,
                        "methodReturnTypeArg" to method.returnType.typeArguments.single(),
                        "methodParameter" to method.parameters.single().type,
                        "methodParameterArg" to
                            method.parameters.single().type.typeArguments.single(),
                        "constructorParameter" to constructor.parameters.single().type,
                        "constructorParameterArg" to
                            constructor.parameters.single().type.typeArguments.single(),
                    )
                    .forEach { (desc, type) ->
                        if (!invocation.isKsp && source == javaSource && preCompiled) {
                            // We can't see type annotations from precompiled Java classes in JAVAC.
                            //   https://github.com/google/ksp/issues/1296
                            assertWithMessage("$desc type: $type")
                                .that(type.getAllAnnotationTypeElements())
                                .isEmpty()
                        } else {
                            assertWithMessage("$desc type: $type")
                                .that(type.getAllAnnotationTypeElements())
                                .containsExactly(a, b)
                        }
                    }
            }
        }
    }

    @Test
    fun repeatedTypeAnnotations() {
        val kotlinSource =
            Source.kotlin(
                "foo.bar.Subject.kt",
                """
            package foo.bar

            @Target(AnnotationTarget.TYPE)
            @Repeatable
            annotation class A(val value: Int)

            open class Base

            class Subject : @A(0) @A(1) Base()
            """
                    .trimIndent()
            )
        val javaSource =
            Source.java(
                "foo.bar.Subject",
                """
            package foo.bar;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.lang.annotation.Repeatable;

            class Base {}

            @Repeatable(AContainer.class)
            @Target(ElementType.TYPE_USE)
            @interface A {
                int value();
            }

            @Target(ElementType.TYPE_USE)
            @interface AContainer {
                A[] value();
            }

            class Subject extends @A(0) @A(1) Base {}
            """
                    .trimIndent()
            )

        listOf(javaSource, kotlinSource).forEach { source ->
            runTest(sources = listOf(source)) { invocation ->
                // We can't see type annotations from precompiled Java classes. Skipping it for now:
                // https://github.com/google/ksp/issues/1296
                if (source == javaSource && preCompiled) {
                    return@runTest
                }
                val subject = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
                val base = subject.superClass!!
                assertThat(base.getAllAnnotations()[0].name).isEqualTo("A")
                assertThat(base.getAllAnnotations()[0].qualifiedName).isEqualTo("foo.bar.A")
                assertThat(base.getAllAnnotations()[0].annotationValues.first().asInt())
                    .isEqualTo(0)
                assertThat(base.getAllAnnotations()[1].name).isEqualTo("A")
                assertThat(base.getAllAnnotations()[1].qualifiedName).isEqualTo("foo.bar.A")
                assertThat(base.getAllAnnotations()[1].annotationValues.first().asInt())
                    .isEqualTo(1)
            }
        }
    }

    @Test
    fun typeParameterAnnotations() {
        val kotlinSource =
            Source.kotlin(
                "foo.bar.Subject.kt",
                """
            package foo.bar

            @Target(AnnotationTarget.TYPE_PARAMETER)
            annotation class A(val value: Int)

            class Subject<@A(42) T>
            """
                    .trimIndent()
            )
        val javaSource =
            Source.java(
                "foo.bar.Subject",
                """
            package foo.bar;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.lang.annotation.Repeatable;

            @Target(ElementType.TYPE_PARAMETER)
            @interface A {
                int value();
            }

            class Subject<@A(42) T> {}
            """
                    .trimIndent()
            )

        fun test(invocation: XTestInvocation) {
            val subject = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
            assertThat(subject.typeParameters.first().getAllAnnotations().first().name)
                .isEqualTo("A")

            assertThat(
                    subject.typeParameters.first().getAllAnnotations().first().get("value") as Int
                )
                .isEqualTo(42)
        }

        listOf(javaSource, kotlinSource).forEach { source ->
            runTest(sources = listOf(source)) { invocation ->
                if (invocation.isKsp) { // doesn't work
                    if (source === javaSource) {
                        if (preCompiled) {
                            // test(invocation)
                        } else {
                            // test(invocation)
                        }
                    } else {
                        if (preCompiled) {
                            // test(invocation)
                        } else {
                            // test(invocation)
                        }
                    }
                } else {
                    if (source === javaSource) {
                        if (preCompiled) {
                            test(invocation)
                        } else {
                            test(invocation)
                        }
                    } else {
                        if (preCompiled) {
                            test(invocation)
                        } else {
                            // test(invocation) // doesn't work
                        }
                    }
                }
            }
        }
    }

    // helper function to read what we need
    private fun XAnnotated.getSuppressValues(): List<String>? {
        return this.findAnnotation<TestSuppressWarnings>()?.get<List<String>>("value")
    }

    private inline fun <reified T : Annotation> XAnnotated.requireAnnotation(): XAnnotation {
        return findAnnotation<T>() ?: error("Annotation ${T::class} not found on $this")
    }

    private inline fun <reified T : Annotation> XAnnotated.findAnnotation(): XAnnotation? {
        return getAllAnnotations().singleOrNull { it.name == T::class.simpleName }
    }

    private fun XAnnotated.assertHasSuppressWithValue(vararg expected: String) {
        assertWithMessage("has suppress annotation $this")
            .that(this.findAnnotation<TestSuppressWarnings>())
            .isNotNull()
        assertWithMessage("$this").that(getSuppressValues()).isEqualTo(expected.toList())
    }

    private fun XAnnotated.assertDoesNotHaveAnnotation() {
        assertWithMessage("$this").that(this.findAnnotation<TestSuppressWarnings>()).isNull()
        assertWithMessage("$this").that(this.getSuppressValues()).isNull()
    }

    private fun XAnnotated.getOtherAnnotationValue(): String? {
        return this.findAnnotation<OtherAnnotation>()?.get<String>("value")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "preCompiled_{0}")
        fun params() = arrayOf(false, true)
    }
}
