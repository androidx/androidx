/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.isConstructor
import androidx.room.compiler.processing.isField
import androidx.room.compiler.processing.isMethod
import androidx.room.compiler.processing.isTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class KspJvmDescriptorUtilsTest(
    private val isPreCompiled: Boolean
) {
    private val describeAnnotation =
        Source.java(
            "androidx.room.test.Describe",
            """
            package androidx.room.test;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
            public @interface Describe { }
            """)

    @Test
    fun descriptor_method_simple() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor))
                    .containsExactly("emptyMethod()V")
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;
                public class DummyClass {
                    @Describe public void emptyMethod() {}
                }
                """)
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test
                class DummyClass {
                    @Describe fun emptyMethod() {}
                }
                """)
        )
    }

    @Test
    fun descriptor_field() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "field1:I",
                    "field2:Ljava/lang/String;",
                    "field3:Ljava/lang/Object;",
                    "field4:Ljava/util/List;"
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;

                import java.util.List;

                class DummyClass<T> {
                    @Describe int field1;
                    @Describe String field2;
                    @Describe T field3;
                    @Describe List<String> field4;
                }
                """)
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test
                class DummyClass<T> {
                    @Describe val field1: Int = TODO()
                    @Describe val field2: String = TODO()
                    @Describe val field3: T = TODO()
                    @Describe val field4: List<String> = TODO()
                }
                """)
        )
    }

    @Test
    fun descriptor_method_erasured() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsAtLeast(
                    "method1(Landroidx/room/test/Foo;)V",
                    "method2()Landroidx/room/test/Foo;",
                    "method3()Ljava/util/List;",
                    "method4()Ljava/util/Map;",
                    "method5()Ljava/util/ArrayList;",
                    "method6(Ljava/lang/Object;)Landroidx/room/test/Foo;",
                    "method7(Ljava/lang/Object;)Ljava/lang/Object;",
                    "method8(Ljava/lang/Object;)Ljava/lang/String;",
                    "method9(Landroidx/room/test/Foo;)Landroidx/room/test/Foo;",
                    "method10()Ljava/util/Collection;",
                    "method11()Landroidx/room/test/Foo;",
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;

                import java.util.ArrayList;
                import java.util.Collection;
                import java.util.List;
                import java.util.Map;

                class DummyClass<T extends Foo> {
                    @Describe void method1(T param) { }
                    @Describe T method2() { return null; }
                    @Describe List<? extends String> method3() { return null; }
                    @Describe Map<T, String> method4() { return null; }
                    @Describe ArrayList<Map<T, String>> method5() { return null; }
                    @Describe <I, O extends T> O method6(I param) { return null; }
                    @Describe static <I, O extends I> O method7(I param) { return null; }
                    @Describe static <I, O extends String> O method8(I param) { return null; }
                    @Describe
                    static <I extends Foo, O extends I> O method9(I param) { return null; }
                    @Describe static <P extends Collection & Foo> P method10() { return null; }
                    @Describe static <P extends Foo & Collection<?>> P method11() { return null; }
                }
                interface Foo {}
                """)
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test
                class DummyClass<T: Foo> {
                    @Describe fun method1(param: T) {}
                    @Describe fun method2(): T = TODO()
                    @Describe fun method3(): List<out String> = TODO()
                    @Describe fun method4(): Map<T, String> = TODO()
                    @Describe fun method5(): ArrayList<Map<T, String>> = TODO()
                    @Describe fun <I, O: T> method6(param: I): O = TODO()
                    companion object {
                        @Describe fun <I, O : I> method7(param: I): O  = TODO()
                        @Describe fun <I, O: String> method8(param: I): O = TODO()
                        @Describe fun <I: Foo, O: I> method9(param: I): O  = TODO()
                        @Describe fun <P> method10(): P where P: Collection<*>, P: Foo = TODO()
                        @Describe fun <P> method11(): P where P: Foo, P: Collection<*> = TODO()
                    }
                }
                interface Foo
                """)
        )
    }

    @Test
    fun descriptor_class_erasured() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "method1(Ljava/lang/Object;)Ljava/lang/Object;",
                    "method2(Ljava/lang/Object;)Ljava/lang/String;",
                    "method3(Ljava/lang/Object;)Ljava/lang/String;",
                    "method4(Ljava/lang/Object;)Ljava/lang/Object;",
                    "method5(Landroidx/room/test/Outer\$Foo;)Landroidx/room/test/Outer\$Foo;",
                    "method6(Landroidx/room/test/Outer\$Bar;)Landroidx/room/test/Outer\$Bar;",
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.Outer",
                """
                package androidx.room.test;
                class Outer {
                    class MyClass1<I, O extends I> {
                        @Describe O method1(I input) { return null; }
                        @Describe static <I, O extends String> O method2(I input) { return null; }
                    }
                    class MyClass2<I, O extends String> {
                        @Describe O method3(I input) { return null; }
                        @Describe static <I, O extends I> O method4(I input) { return null; }
                    }
                    class MyClass3<I extends Foo, O extends I> {
                        @Describe O method5(I input) { return null; }
                        @Describe static <I extends Bar, O extends I> O method6(I input) {
                         return null;
                        }
                    }
                    class Foo {}
                    class Bar {}
                }
                """)
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.Outer.kt",
                """
                package androidx.room.test
                class Outer {
                    class MyClass1<I, O: I> {
                        @Describe fun method1(input: I): O = TODO()
                        companion object {
                            @Describe fun <I, O: String> method2(input: I): O = TODO()
                        }
                    }
                    class MyClass2<I, O: String> {
                        @Describe fun method3(input: I): O = TODO()
                        companion object {
                            @Describe fun <I, O: I> method4(input: I): O = TODO()
                        }
                    }
                    class MyClass3<I: Foo, O: I> {
                        @Describe fun method5(input: I): O = TODO()
                        companion object {
                            @Describe fun <I: Bar, O: I> method6(input: I): O = TODO()
                        }
                    }
                    class Foo
                    class Bar
                }
                """)
        )
    }

    @Test
    fun descriptor_method_primitiveParams() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "method1(ZI)V",
                    "method2(C)B",
                    "method3(DF)V",
                    "method4(JS)V"
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe void method1(boolean yesOrNo, int number) { }
                    @Describe byte method2(char letter) { return 0; }
                    @Describe void method3(double realNumber1, float realNumber2) { }
                    @Describe void method4(long bigNumber, short littlerNumber) { }
                }
                """)
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test
                class DummyClass {
                    @Describe fun method1(yesOrNo: Boolean, number: Int) {}
                    @Describe fun method2(letter: Char): Byte = TODO()
                    @Describe fun method3(realNumber1: Double, realNumber2: Float) {}
                    @Describe fun method4(bigNumber: Long, littlerNumber: Short) {}
                }
                """)
        )
    }

    @Test
    fun descriptor_method_classParam_javaTypes() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "method1(Ljava/lang/Object;)V",
                    "method2()Ljava/lang/Object;",
                    "method3(Ljava/util/ArrayList;)Ljava/util/List;",
                    "method4()Ljava/util/Map;"
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;

                class DummyClass {
                    @Describe void method1(Object something) { }
                    @Describe Object method2() { return null; }
                    @Describe List<String> method3(ArrayList<Integer> list) { return null; }
                    @Describe Map<String, Object> method4() { return null; }
                }
                """)
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe fun method1(something: Object) {}
                    @Describe fun method2(): Object = TODO()
                    @Describe fun method3(list: ArrayList<Integer>): List<String> = TODO()
                    @Describe fun method4(): Map<String, Object> = TODO()
                }
                """)
        )
    }

    @Test
    fun descriptor_method_classParam_testClass() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "method1(Landroidx/room/test/DataClass;)V",
                    "method2()Landroidx/room/test/DataClass;"
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DataClass",
                """
                package androidx.room.test;
                class DataClass {}
                """),
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe void method1(DataClass data) { }
                    @Describe DataClass method2() { return null; }
                }
                """),
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe fun method1(data: DataClass) {}
                    @Describe fun method2(): DataClass = TODO()
                }
                class DataClass
                """),
        )
    }

    @Test
    fun descriptor_method_classParam_innerTestClass() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "method1(Landroidx/room/test/DataClass\$MemberInnerData;)V",
                    "method2(Landroidx/room/test/DataClass\$StaticInnerData;)V",
                    "method3(Landroidx/room/test/DataClass\$EnumData;)V",
                    "method4()Landroidx/room/test/DataClass\$StaticInnerData;"
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DataClass",
                """
                package androidx.room.test;
                class DataClass {
                    class MemberInnerData { }
                    static class StaticInnerData { }
                    enum EnumData { VALUE1, VALUE2 }
                }
                """
            ),
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe void method1(DataClass.MemberInnerData data) { }
                    @Describe void method2(DataClass.StaticInnerData data) { }
                    @Describe void method3(DataClass.EnumData enumData) { }
                    @Describe DataClass.StaticInnerData method4() { return null; }
                }
                """),
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test
                class DummyClass {
                    @Describe fun method1(data: DataClass.MemberInnerData) {}
                    @Describe fun method2(data: DataClass.StaticInnerData) {}
                    @Describe fun method3(enumData: DataClass.EnumData) {}
                    @Describe fun method4(): DataClass.StaticInnerData = TODO()
                }
                class DataClass {
                    inner class MemberInnerData
                    class StaticInnerData
                    enum class EnumData { VALUE1, VALUE2 }
                }
                """),
        )
    }

    @Test
    fun descriptor_method_arrayParams() {
        fun checkSources(vararg sources: Source) {
            runTest(sources = sources) { invocation ->
                assertThat(invocation.annotatedElements().map(this::descriptor)).containsExactly(
                    "method1([Landroidx/room/test/DataClass;)V",
                    "method2()[Landroidx/room/test/DataClass;",
                    "method3([I)V",
                    "method4([I)V"
                )
            }
        }
        checkSources(
            Source.java(
                "androidx.room.test.DataClass",
                """
                package androidx.room.test;
                class DataClass {}
                """),
            Source.java(
                "androidx.room.test.DummyClass",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe void method1(DataClass[] data) { }
                    @Describe DataClass[] method2() { return null; }
                    @Describe void method3(int[] array) { }
                    @Describe void method4(int... array) { }
                }
                """),
        )
        checkSources(
            Source.kotlin(
                "androidx.room.test.DummyClass.kt",
                """
                package androidx.room.test;
                class DummyClass {
                    @Describe fun method1(data: Array<DataClass>) {}
                    @Describe fun method2(): Array<DataClass> = TODO()
                    @Describe fun method3(array: IntArray) {}
                    @Describe fun method4(vararg array: Int) {}
                }
                class DataClass
                """),
        )
    }

    private fun runTest(
        vararg sources: Source,
        handler: (XTestInvocation) -> Unit
    ) {
        if (isPreCompiled) {
            val compiled = compileFiles(listOf(*sources) + describeAnnotation)
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
                sources = listOf(*sources) + describeAnnotation,
                handler = handler
            )
        }
    }

    private fun XTestInvocation.annotatedElements(): Set<XElement> {
        // RoundEnv.getElementsAnnotatedWith() only processes current round and could not see
        // precompiled classes.
        val typeElements = processingEnv.getTypeElementsFromPackage("androidx.room.test")
        return typeElements
            .flatMap {
                it.getElementsAnnotatedWith(ClassName.get(
                "androidx.room.test", "Describe"))
            }.toSet()
    }

    private fun XTypeElement.getElementsAnnotatedWith(annotation: ClassName): Set<XElement> {
        return (getEnclosedElements().filter { !it.isTypeElement() } + this)
            .filter { it.hasAnnotation(annotation) }
            .toSet() + getEnclosedTypeElements().flatMap { it.getElementsAnnotatedWith(annotation) }
    }

    private fun descriptor(element: XElement): String {
        return when {
            element.isField() -> element.jvmDescriptor
            element.isMethod() -> element.jvmDescriptor
            element.isConstructor() -> element.jvmDescriptor
            else -> error("Unsupported element to describe.")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isPreCompiled_{0}")
        fun params(): List<Array<Any>> {
            return listOf(arrayOf(false), arrayOf(true))
        }
    }
}