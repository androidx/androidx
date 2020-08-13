/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.room.compiler.processing.testcode.MainAnnotation
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XAnnotationBoxTest {
    @Test
    fun readSimpleAnotationValue() {
        val source = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            @SuppressWarnings({"warning1", "warning 2"})
            public class Baz {
            }
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotationBox = element.toAnnotationBox(SuppressWarnings::class)
            assertThat(annotationBox).isNotNull()
            assertThat(
                annotationBox!!.value.value
            ).isEqualTo(
                arrayOf("warning1", "warning 2")
            )
        }
    }

    @Test
    fun typeReference() {
        val mySource = Source.java(
            "foo.bar.Baz", """
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
        val targetName = "foo.bar.Baz"
        runProcessorTest(
            listOf(mySource)
        ) {
            val element = it.processingEnv.requireTypeElement(targetName)
            element.toAnnotationBox(MainAnnotation::class)!!.let { annotation ->
                assertThat(
                    annotation.getAsTypeList("typeList")
                ).containsExactly(
                    it.processingEnv.requireType(java.lang.String::class.java.canonicalName),
                    it.processingEnv.requireType(java.lang.Integer::class.java.canonicalName)
                )
                assertThat(
                    annotation.getAsType("singleType")
                ).isEqualTo(
                    it.processingEnv.requireType(java.lang.Long::class.java.canonicalName)
                )

                assertThat(annotation.value.intMethod).isEqualTo(3)
                annotation.getAsAnnotationBox<OtherAnnotation>("singleOtherAnnotation")
                    .let { other ->
                        assertThat(other.value.value).isEqualTo("other single")
                    }
                annotation.getAsAnnotationBoxArray<OtherAnnotation>("otherAnnotationArray")
                    .let { boxArray ->
                        assertThat(boxArray).hasLength(2)
                        assertThat(boxArray[0].value.value).isEqualTo("other list 1")
                        assertThat(boxArray[1].value.value).isEqualTo("other list 2")
                    }
            }
        }
    }
}
