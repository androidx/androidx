/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.kotlin

import androidx.room.testing.TestProcessor
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import toJFO
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class JvmDescriptorUtilsTest {

    private val describeAnnotation =
        """
        package androidx.room.test;

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Target;

        @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
        public @interface Describe { }
        """.toJFO("androidx.room.test.Describe")

    @Test
    fun descriptor_method_simple() {
        singleRun(
            """
            package androidx.room.test;

            public class DummyClass {
                @Describe
                public void emptyMethod() {
                }
            }
            """.toJFO("androidx.room.test.DummyClass")
        ) { descriptors ->
            assertThat(descriptors.first())
                .isEqualTo("emptyMethod()V")
        }.compilesWithoutError()
    }

    @Test
    fun descriptor_method_primitiveParams() {
        singleRun(
            """
            package androidx.room.test;

            class DummyClass {
                @Describe
                void method1(boolean yesOrNo, int number) { }

                @Describe
                byte method2(char letter) { return 0; }

                @Describe
                void method3(double realNumber1, float realNummber2) { }

                @Describe
                void method4(long bigNumber, short littlerNumber) { }
            }
            """.toJFO("androidx.room.test.DummyClass")
        ) { descriptors ->
            assertThat(descriptors)
                .isEqualTo(setOf("method1(ZI)V", "method2(C)B", "method3(DF)V", "method4(JS)V"))
        }.compilesWithoutError()
    }

    @Test
    fun descriptor_method_classParam_javaTypes() {
        singleRun(
            """
            package androidx.room.test;

            import java.util.ArrayList;
            import java.util.List;

            class DummyClass {
                @Describe
                void method1(Object something) { }

                @Describe
                Object method2() { return null; }

                @Describe
                List<String> method3(ArrayList<Integer> list) { return null; }
            }
            """.toJFO("androidx.room.test.DummyClass")
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                setOf(
                    "method1(Ljava/lang/Object;)V",
                    "method2()Ljava/lang/Object;",
                    "method3(Ljava/util/ArrayList;)Ljava/util/List;"
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun descriptor_method_classParam_testClass() {
        val extraJfo =
            """
            package androidx.room.test;

            class DataClass { }
            """.toJFO("androidx.room.test.DataClass")

        singleRun(
            """
            package androidx.room.test;

            class DummyClass {
                @Describe
                void method1(DataClass data) { }

                @Describe
                DataClass method2() { return null; }
            }
            """.toJFO("androidx.room.test.DummyClass"), extraJfo
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                setOf(
                    "method1(Landroidx/room/test/DataClass;)V",
                    "method2()Landroidx/room/test/DataClass;"
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun descriptor_method_classParam_innerTestClass() {
        val extraJfo =
            """
            package androidx.room.test;

            class DataClass {

                class MemberInnerData { }

                static class StaticInnerData { }

                enum EnumData {
                    VALUE1, VALUE2
                }
            }
            """.toJFO("androidx.room.test.DataClass")

        singleRun(
            """
            package androidx.room.test;

            class DummyClass {
                @Describe
                void method1(DataClass.MemberInnerData data) { }

                @Describe
                void method2(DataClass.StaticInnerData data) { }

                @Describe
                void method3(DataClass.EnumData enumData) { }

                @Describe
                DataClass.StaticInnerData method4() { return null; }
            }
            """.toJFO("androidx.room.test.DummyClass"), extraJfo
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                setOf(
                    "method1(Landroidx/room/test/DataClass\$MemberInnerData;)V",
                    "method2(Landroidx/room/test/DataClass\$StaticInnerData;)V",
                    "method3(Landroidx/room/test/DataClass\$EnumData;)V",
                    "method4()Landroidx/room/test/DataClass\$StaticInnerData;"
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun descriptor_method_arrayParams() {
        val extraJfo =
            """
            package androidx.room.test;

            class DataClass { }
            """.toJFO("androidx.room.test.DataClass")

        singleRun(
            """
            package androidx.room.test;

            class DummyClass {
                @Describe
                void method1(DataClass[] data) { }

                @Describe
                DataClass[] method2() { return null; }

                @Describe
                void method3(int[] array) { }

                @Describe
                void method4(int... array) { }
            }
            """.toJFO("androidx.room.test.DummyClass"), extraJfo
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                setOf(
                    "method1([Landroidx/room/test/DataClass;)V",
                    "method2()[Landroidx/room/test/DataClass;",
                    "method3([I)V",
                    "method4([I)V"
                )
            )
        }.compilesWithoutError()
    }

    private fun singleRun(
        vararg jfo: JavaFileObject,
        handler: (Set<String>) -> Unit
    ): CompileTester = Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
        .that(listOf(describeAnnotation) + jfo)
        .processedWith(TestProcessor.builder()
            .nextRunHandler {
                it.roundEnv.getElementsAnnotatedWith(it.annotations.first()).map { element ->
                    MoreElements.asExecutable(element).descriptor(it.processingEnv.typeUtils)
                }.toSet().let(handler)
                true
            }
            .forAnnotations("androidx.room.test.Describe")
            .build())
}