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

import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults
import androidx.room.compiler.processing.testcode.JavaAnnotationWithEnum
import androidx.room.compiler.processing.testcode.JavaAnnotationWithEnumArray
import androidx.room.compiler.processing.testcode.JavaAnnotationWithPrimitiveArray
import androidx.room.compiler.processing.testcode.JavaAnnotationWithTypeReferences
import androidx.room.compiler.processing.testcode.JavaEnum
import androidx.room.compiler.processing.testcode.MainAnnotation
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.testcode.RepeatableJavaAnnotation
import androidx.room.compiler.processing.testcode.TestSuppressWarnings
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.javapoet.ClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// used in typealias test
typealias OtherAnnotationTypeAlias = OtherAnnotation
@RunWith(Parameterized::class)
class XAnnotationTest(
    private val preCompiled: Boolean
) {
    private fun runTest(
        sources: List<Source>,
        handler: (XTestInvocation) -> Unit
    ) {
        if (preCompiled) {
            val compiled = compileFiles(sources)
            val hasKotlinSources = sources.any {
                it is Source.KotlinSource
            }
            val kotlinSources = if (hasKotlinSources) {
                listOf(
                    Source.kotlin("placeholder.kt", "class PlaceholderKotlin")
                )
            } else {
                emptyList()
            }
            val newSources = kotlinSources + Source.java(
                "PlaceholderJava",
                "public class " +
                    "PlaceholderJava {}"
            )
            runProcessorTest(
                sources = newSources,
                handler = handler,
                classpath = compiled
            )
        } else {
            runProcessorTest(
                sources = sources,
                handler = handler
            )
        }
    }

    @Test
    fun readsAnnotationsDeclaredInSources() {
        val source = Source.kotlin(
            "MyClass.kt",
            """
            annotation class MyAnnotation1(val bar: Int)
            @MyAnnotation1(bar = 1)
            class MyClass
            """.trimIndent()
        )
        runTest(
            sources = listOf(source),
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyClass")

            val allAnnotations = element.getAllAnnotations().filterNot {
                // Drop metadata annotation in kapt
                it.name == "Metadata"
            }
            assertThat(allAnnotations).hasSize(1)

            val annotation1 = allAnnotations[0]
            assertThat(annotation1.name)
                .isEqualTo("MyAnnotation1")
            assertThat(annotation1.qualifiedName)
                .isEqualTo("MyAnnotation1")
            assertThat(annotation1.type.typeElement)
                .isEqualTo(invocation.processingEnv.requireTypeElement("MyAnnotation1"))
            assertThat(annotation1.get<Int>("bar"))
                .isEqualTo(1)
            assertThat(annotation1.annotationValues).hasSize(1)
            assertThat(annotation1.annotationValues.first().name).isEqualTo("bar")
            assertThat(annotation1.annotationValues.first().value).isEqualTo(1)
        }
    }

    @Test
    fun annotationsInClassPathCanBeBoxed() {
        val source = Source.kotlin(
            "MyClass.kt",
            """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("a", "b")
            class MyClass
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("MyClass")
            val annotation = element.requireAnnotation<TestSuppressWarnings>()
            assertThat(annotation.name)
                .isEqualTo(TestSuppressWarnings::class.simpleName)
            assertThat(annotation.qualifiedName)
                .isEqualTo(TestSuppressWarnings::class.qualifiedName)
            assertThat(annotation.type.typeElement)
                .isEqualTo(invocation.processingEnv.requireTypeElement(TestSuppressWarnings::class))
            assertThat(
                annotation.asAnnotationBox<TestSuppressWarnings>().value.value
            ).isEqualTo(arrayOf("a", "b"))
        }
    }

    @Test
    fun readSimpleAnnotationValue() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings;
            @TestSuppressWarnings({"warning1", "warning 2"})
            public class Baz {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation = element.requireAnnotation<TestSuppressWarnings>()

            val argument = annotation.getAnnotationValue("value")
            assertThat(argument.name).isEqualTo("value")
            assertThat(
                argument.asStringList()
            ).isEqualTo(
                listOf("warning1", "warning 2")
            )
        }
    }

    @Test
    fun readSimpleAnnotationValueFromClassName() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings;
            @TestSuppressWarnings({"warning1", "warning 2"})
            public class Baz {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation =
                element.requireAnnotation(ClassName.get(TestSuppressWarnings::class.java))

            val argument = annotation.getAnnotationValue("value")
            assertThat(argument.name).isEqualTo("value")
            assertThat(
                argument.asStringList()
            ).isEqualTo(
                listOf("warning1", "warning 2")
            )
        }
    }

    @Test
    fun typeReference_javac() {
        val mySource = Source.java(
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
            """.trimIndent()
        )
        runProcessorTestWithoutKsp(
            listOf(mySource)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation = element.requireAnnotation<MainAnnotation>()

            assertThat(
                annotation.get<List<XType>>("typeList")
            ).containsExactly(
                invocation.processingEnv.requireType(java.lang.String::class),
                invocation.processingEnv.requireType(Integer::class)
            )
            assertThat(
                annotation.get<XType>("singleType")
            ).isEqualTo(
                invocation.processingEnv.requireType(java.lang.Long::class)
            )

            assertThat(annotation.get<Int>("intMethod")).isEqualTo(3)
            annotation.get<XAnnotation>("singleOtherAnnotation")
                .let { other ->
                    assertThat(other.name).isEqualTo(OtherAnnotation::class.simpleName)
                    assertThat(other.qualifiedName).isEqualTo(OtherAnnotation::class.qualifiedName)
                    assertThat(other.get<String>("value"))
                        .isEqualTo("other single")
                }
            annotation.get<List<XAnnotation>>("otherAnnotationArray")
                .let { boxArray ->
                    assertThat(boxArray).hasSize(2)
                    assertThat(boxArray[0].get<String>("value")).isEqualTo("other list 1")
                    assertThat(boxArray[1].get<String>("value")).isEqualTo("other list 2")
                }
        }
    }

    @Test
    fun readSimpleAnnotationValue_kotlin() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("warning1", "warning 2")
            class Subject {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = element.requireAnnotation<TestSuppressWarnings>()

            assertThat(annotation).isNotNull()
            assertThat(
                annotation.get<List<String>>("value")
            ).isEqualTo(
                listOf("warning1", "warning 2")
            )
        }
    }

    @Test
    fun typeReference_kotlin() {
        val mySource = Source.kotlin(
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
            """.trimIndent()
        )
        runTest(
            listOf(mySource)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = element.requireAnnotation<MainAnnotation>()

            assertThat(
                annotation.get<List<XType>>("typeList").map {
                    it.typeName
                }
            ).containsExactly(
                String::class.typeName(),
                Int::class.typeName()
            )
            assertThat(
                annotation.get<XType>("singleType")
            ).isEqualTo(
                invocation.processingEnv.requireType(Long::class.typeName())
            )

            assertThat(annotation.get<Int>("intMethod")).isEqualTo(3)
            annotation.get<XAnnotation>("singleOtherAnnotation")
                .let { other ->
                    assertThat(other.name).isEqualTo(OtherAnnotation::class.simpleName)
                    assertThat(other.qualifiedName).isEqualTo(OtherAnnotation::class.qualifiedName)
                    assertThat(other.get<String>("value"))
                        .isEqualTo("other single")
                }
            annotation.get<List<XAnnotation>>("otherAnnotationArray")
                .let { boxArray ->
                    assertThat(boxArray).hasSize(2)
                    assertThat(boxArray[0].get<String>("value")).isEqualTo("other list 1")
                    assertThat(boxArray[1].get<String>("value")).isEqualTo("other list 2")
                }
        }
    }

    @Test
    fun typeReferenceArray_singleItemInJava() {
        val src = Source.java(
            "Subject",
            """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithTypeReferences;
            @JavaAnnotationWithTypeReferences(String.class)
            class Subject {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(src)
        ) { invocation ->
            if (!invocation.isKsp) return@runTest
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val annotation = subject.requireAnnotation<JavaAnnotationWithTypeReferences>()
            val annotationValue = annotation.get<List<XType>>("value").single()
            assertThat(annotationValue.typeName).isEqualTo(
                ClassName.get(String::class.java)
            )
        }
    }

    @Test
    fun propertyAnnotations() {
        val src = Source.kotlin(
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
                var prop3:Int
                    @OtherAnnotation("_onGetter3")
                    get() = 3

                    @OtherAnnotation("_onSetter3")
                    set(@OtherAnnotation("_onSetterParam3") value) = Unit
            }
            """.trimIndent()
        )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")

            subject.getField("prop1").assertHasSuppressWithValue("onProp1")
            subject.getMethod("getProp1").assertDoesNotHaveAnnotation()
            subject.getMethod("setProp1").assertDoesNotHaveAnnotation()
            subject.getMethod("setProp1").parameters.first().assertDoesNotHaveAnnotation()

            subject.getField("prop2").assertHasSuppressWithValue("onField2")
            subject.getMethod("getProp2").assertHasSuppressWithValue("onGetter2")
            subject.getMethod("setProp2").assertHasSuppressWithValue("onSetter2")
            subject.getMethod("setProp2").parameters.first().assertHasSuppressWithValue(
                "onSetterParam2"
            )

            subject.getMethod("getProp3").assertHasSuppressWithValue("onGetter3")
            subject.getMethod("setProp3").assertHasSuppressWithValue("onSetter3")
            subject.getMethod("setProp3").parameters.first().assertHasSuppressWithValue(
                "onSetterParam3"
            )

            assertThat(
                subject.getMethod("getProp3").getOtherAnnotationValue()
            ).isEqualTo("_onGetter3")
            assertThat(
                subject.getMethod("setProp3").getOtherAnnotationValue()
            ).isEqualTo("_onSetter3")
            val otherAnnotationValue =
                subject.getMethod("setProp3").parameters.first().getOtherAnnotationValue()
            assertThat(
                otherAnnotationValue
            ).isEqualTo("_onSetterParam3")
        }
    }

    @Test
    fun methodAnnotations() {
        val src = Source.kotlin(
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
            """.trimIndent()
        )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getMethod("noAnnotations").let { method ->
                method.assertDoesNotHaveAnnotation()
                method.getParameter("x").assertDoesNotHaveAnnotation()
            }
            subject.getMethod("methodAnnotation").let { method ->
                method.assertHasSuppressWithValue("onMethod")
                method.getParameter("annotated").assertHasSuppressWithValue("onParam")
                method.getParameter("notAnnotated").assertDoesNotHaveAnnotation()
            }
        }
    }

    @Test
    fun constructorParameterAnnotations() {
        val src = Source.kotlin(
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
            """.trimIndent()
        )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.assertHasSuppressWithValue("onClass")
            assertThat(subject.getConstructors()).hasSize(1)
            val constructor = subject.getConstructors().single()
            constructor.getParameter("x").assertHasSuppressWithValue("onConstructorParam")
            subject.getMethod("getX").assertHasSuppressWithValue("onGetter")
            subject.getMethod("setX").assertHasSuppressWithValue("onSetter")
            subject.getField("x").assertHasSuppressWithValue("onField")
        }
    }

    @Test
    fun defaultValues() {
        val kotlinSrc = Source.kotlin(
            "KotlinClass.kt",
            """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults
            @JavaAnnotationWithDefaults
            class KotlinClass
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "JavaClass.java",
            """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults;
            @JavaAnnotationWithDefaults
            class JavaClass {}
            """.trimIndent()
        )
        runTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            listOf("KotlinClass", "JavaClass")
                .map {
                    invocation.processingEnv.requireTypeElement(it)
                }.forEach { typeElement ->
                    val annotation = typeElement.requireAnnotation<JavaAnnotationWithDefaults>()

                    assertThat(annotation.get<Int>("intVal"))
                        .isEqualTo(3)
                    assertThat(annotation.get<List<Int>>("intArrayVal"))
                        .isEqualTo(listOf(1, 3, 5))
                    assertThat(annotation.get<List<String>>("stringArrayVal"))
                        .isEqualTo(listOf("x", "y"))
                    assertThat(annotation.get<String>("stringVal"))
                        .isEqualTo("foo")
                    assertThat(
                        annotation.getAsType("typeVal").rawType.typeName
                    ).isEqualTo(
                        ClassName.get(HashMap::class.java)
                    )
                    assertThat(
                        annotation.getAsTypeList("typeArrayVal").map {
                            it.rawType.typeName
                        }
                    ).isEqualTo(
                        listOf(ClassName.get(LinkedHashMap::class.java))
                    )

                    val enumValueEntry = annotation.getAsEnum("enumVal")
                    assertThat(enumValueEntry.name).isEqualTo("DEFAULT")
                    val javaEnumType = invocation.processingEnv.requireTypeElement(JavaEnum::class)
                    assertThat(enumValueEntry.enumTypeElement)
                        .isEqualTo(javaEnumType)

                    val enumList = annotation.getAsEnumList("enumArrayVal")
                    assertThat(enumList[0].name).isEqualTo("VAL1")
                    assertThat(enumList[1].name).isEqualTo("VAL2")
                    assertThat(enumList[0].enumTypeElement).isEqualTo(javaEnumType)
                    assertThat(enumList[1].enumTypeElement).isEqualTo(javaEnumType)

                    // TODO: KSP mistakenly sees null for the value in a default annotation in
                    //  sources. https://github.com/google/ksp/issues/53
                    if (!invocation.isKsp && !preCompiled) {

                        annotation.getAsAnnotation("otherAnnotationVal")
                            .let { other ->
                                assertThat(other.name).isEqualTo("OtherAnnotation")
                                assertThat(other.get<String>("value"))
                                    .isEqualTo("def")
                            }

                        annotation.getAsAnnotationList("otherAnnotationArrayVal")
                            .forEach { other ->
                                assertThat(other.name).isEqualTo("OtherAnnotation")
                                assertThat(other.get<String>("value"))
                                    .isEqualTo("v1")
                            }
                    }
                }
        }
    }

    @Test
    fun javaPrimitiveArray() {
        // TODO: expand this test for other primitive types: 179081610
        val javaSrc = Source.java(
            "JavaSubject.java",
            """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithPrimitiveArray(intArray = {1, 2, 3})
                Object annotated1;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import androidx.room.compiler.processing.testcode.*;
            class KotlinSubject {
                @JavaAnnotationWithPrimitiveArray(intArray = [1, 2, 3])
                val annotated1:Any = TODO()
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject").map {
                invocation.processingEnv.requireTypeElement(it)
            }.forEach { subject ->
                val annotation = subject.getField("annotated1")
                    .requireAnnotation<JavaAnnotationWithPrimitiveArray>()
                assertThat(
                    annotation.get<List<Int>>("intArray")
                ).isEqualTo(
                    listOf(1, 2, 3)
                )
            }
        }
    }

    @Test
    fun javaEnum() {
        val javaSrc = Source.java(
            "JavaSubject.java",
            """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithEnum(JavaEnum.VAL1)
                Object annotated1;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import androidx.room.compiler.processing.testcode.*;
            class KotlinSubject {
                @JavaAnnotationWithEnum(JavaEnum.VAL1)
                val annotated1: Any = TODO()
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject").map {
                invocation.processingEnv.requireTypeElement(it)
            }.forEach { subject ->
                val annotation = subject.getField("annotated1")
                    .requireAnnotation<JavaAnnotationWithEnum>()
                assertThat(
                    annotation.getAsEnum("value").name
                ).isEqualTo(
                    JavaEnum.VAL1.name
                )
            }
        }
    }

    @Test
    fun javaEnumArray() {
        val javaSrc = Source.java(
            "JavaSubject.java",
            """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithEnumArray(enumArray = {JavaEnum.VAL1, JavaEnum.VAL2})
                Object annotated1;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import androidx.room.compiler.processing.testcode.*;
            class KotlinSubject {
                @JavaAnnotationWithEnumArray(enumArray = [JavaEnum.VAL1, JavaEnum.VAL2])
                val annotated1: Any = TODO()
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject").map {
                invocation.processingEnv.requireTypeElement(it)
            }.forEach { subject ->
                val annotation = subject.getField("annotated1")
                    .requireAnnotation<JavaAnnotationWithEnumArray>()
                assertThat(
                    annotation.getAsEnumList("enumArray").map { it.name }
                ).isEqualTo(
                    listOf(JavaEnum.VAL1.name, JavaEnum.VAL2.name)
                )
            }
        }
    }

    @Test
    fun javaEnumArrayWithDefaultNameAndValue() {
        val annotationSource = Source.java(
            "foo.bar.MyAnnotation",
            """
            package foo.bar;
            public @interface MyAnnotation {
                MyEnum[] value() default {};
            }
            """.trimIndent()
        )
        val enumSource = Source.java(
            "foo.bar.MyEnum",
            """
            package foo.bar;
            enum MyEnum {
                 Bar
            }
            """.trimIndent()
        )
        val classSource = Source.java(
            "foo.bar.Subject",
            """
            package foo.bar;
            @MyAnnotation
            class Subject {}
            """.trimIndent()
        )
        runTest(
            sources = listOf(annotationSource, enumSource, classSource)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("foo.bar.Subject")

            val annotations = subject.getAllAnnotations().filter { it.name == "MyAnnotation" }
            assertThat(annotations).hasSize(1)
        }
    }

    @Test
    fun javaRepeatableAnnotation() {
        val javaSrc = Source.java(
            "JavaSubject",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName};
            @RepeatableJavaAnnotation("x")
            @RepeatableJavaAnnotation("y")
            @RepeatableJavaAnnotation("z")
            public class JavaSubject {}
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName}
            // TODO update when https://youtrack.jetbrains.com/issue/KT-12794 is fixed.
            // right now, kotlin does not support repeatable annotations.
            @RepeatableJavaAnnotation.List(
                RepeatableJavaAnnotation("x"),
                RepeatableJavaAnnotation("y"),
                RepeatableJavaAnnotation("z")
            )
            public class KotlinSubject
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map(invocation.processingEnv::requireTypeElement)
                .forEach { subject ->
                    val annotations = subject.getAllAnnotations().filter {
                        it.name == "RepeatableJavaAnnotation"
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
        val javaSrc = Source.java(
            "JavaSubject",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName};
            @RepeatableJavaAnnotation("x")
            public class JavaSubject {}
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName}
            @RepeatableJavaAnnotation("x")
            public class KotlinSubject
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map(invocation.processingEnv::requireTypeElement)
                .forEach { subject ->
                    val annotations = subject.getAllAnnotations().filter {
                        it.name == "RepeatableJavaAnnotation"
                    }
                    val values = annotations.map { it.get<String>("value") }
                    assertWithMessage(subject.qualifiedName)
                        .that(values)
                        .containsExactly("x")
                }
        }
    }

    @Test
    fun typealiasAnnotation() {
        val source = Source.kotlin(
            "Subject.kt",
            """
            typealias SourceTypeAlias = ${OtherAnnotation::class.qualifiedName}
            @SourceTypeAlias("x")
            class Subject {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) { invocation ->
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
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults;
            @JavaAnnotationWithDefaults(stringVal = "test")
            public class Baz {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotation =
                element.requireAnnotation(ClassName.get(JavaAnnotationWithDefaults::class.java))

            assertThat(annotation.get<String>("stringVal")).isEqualTo("test")
            assertThat(annotation.get<Int>("intVal")).isEqualTo(3)

            // Also test reading theses values through getAs*() methods
            assertThat(annotation.getAsString("stringVal")).isEqualTo("test")
            assertThat(annotation.getAsInt("intVal")).isEqualTo(3)
        }
    }

    // helper function to read what we need
    private fun XAnnotated.getSuppressValues(): List<String>? {
        return this.findAnnotation<TestSuppressWarnings>()
            ?.get<List<String>>("value")
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
        assertWithMessage("$this")
            .that(getSuppressValues())
            .isEqualTo(expected.toList())
    }

    private fun XAnnotated.assertDoesNotHaveAnnotation() {
        assertWithMessage("$this")
            .that(this.findAnnotation<TestSuppressWarnings>())
            .isNull()
        assertWithMessage("$this")
            .that(this.getSuppressValues())
            .isNull()
    }

    private fun XAnnotated.getOtherAnnotationValue(): String? {
        return this.findAnnotation<OtherAnnotation>()
            ?.get<String>("value")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "preCompiled_{0}")
        fun params() = arrayOf(false, true)
    }
}
