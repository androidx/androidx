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

package androidx.serialization.compiler.processing.parsers

import androidx.serialization.compiler.processing.ext.asTypeElement
import androidx.serialization.compiler.models.Reserved
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep
import com.google.common.collect.SetMultimap
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.junit.Test
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element

/** Unit tests for [parseReserved]. */
class ParseReservedTest {
    @Test
    fun testIds() {
        assertThat(compile("@Reserved(ids = {1, 2, 3})").ids).containsExactly(1, 2, 3)
    }

    @Test
    fun testNames() {
        assertThat(compile("""@Reserved(names = {"foo", "bar"})""").names)
            .containsExactly("foo", "bar")
    }

    @Test
    fun testIdRanges() {
        val reserved = """
            @Reserved(idRanges = {
                @Reserved.IdRange(from = 1, to = 2),
                @Reserved.IdRange(from = 4, to = 3) // Reversed for testing
            })
        """.trimIndent()
        assertThat(compile(reserved).idRanges).containsExactly(1..2, 3..4)
    }

    private fun compile(reserved: String): Reserved {
        val processor =
            ReservedProcessor()
        val source = JavaFileObjects.forSourceString("TestClass", """
            import androidx.serialization.Reserved;
            
            $reserved
            public final class TestClass {}
        """.trimIndent())
        assertThat(javac().withProcessors(processor).compile(source)).succeededWithoutWarnings()
        return processor.reserved
    }

    private class ReservedProcessingStep(
        private val onReserved: (Reserved) -> Unit
    ) : ProcessingStep {
        override fun annotations(): Set<Class<out Annotation>> {
            return setOf(androidx.serialization.Reserved::class.java)
        }

        override fun process(
            elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
        ): Set<Element> {
            elementsByAnnotation[androidx.serialization.Reserved::class.java].forEach {
                onReserved(parseReserved(it.asTypeElement()))
            }

            return emptySet()
        }
    }

    private class ReservedProcessor : BasicAnnotationProcessor() {
        lateinit var reserved: Reserved

        override fun initSteps(): List<ProcessingStep> = listOf(
            ReservedProcessingStep { reserved = it }
        )

        override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()
    }
}
