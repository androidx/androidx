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

import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

class JavacTestProcessorTest {

    @Test
    fun getElementsAnnotatedWith() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;
            @OtherAnnotation(value="xx")
            class Baz {
              @OtherAnnotation(value="xx")
              int myField = 0;
              @OtherAnnotation(value="xx")
              void myFunction() { }
            }
            """.trimIndent()
        )
        testProcessor(listOf(source), listOf(OtherAnnotation::class)) { roundEnv ->
            val annotatedElementsByClass = roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class
            )

            val annotatedElementsByName = roundEnv.getElementsAnnotatedWith(
                OtherAnnotation::class.qualifiedName!!
            )

            val targetElement = xProcessingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(
                annotatedElementsByClass
            ).apply {
                containsExactlyElementsIn(annotatedElementsByName)
                hasSize(3)
                contains(targetElement)
                contains(targetElement.getMethod("myFunction"))
                contains(targetElement.getField("myField"))
            }
        }
    }

    private fun testProcessor(
        sources: List<Source>,
        annotations: List<KClass<out Annotation>>,
        doProcessTest: JavacTestProcessor.(XRoundEnv) -> Unit
    ) {
        val invoked = AtomicBoolean(false)

        val testProcessor = object : JavacTestProcessor() {
            override fun doProcess(annotations: Set<XTypeElement>, roundEnv: XRoundEnv): Boolean {
                invoked.set(true)

                doProcessTest(roundEnv)

                return true
            }

            override fun getSupportedAnnotationTypes(): Set<String> {
                return annotations.map { it.java.canonicalName }.toSet()
            }
        }
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            sources.map { it.toJFO() }
        ).processedWith(
            testProcessor
        ).compilesWithoutError()

        assertThat(invoked.get()).isTrue()
    }
}
