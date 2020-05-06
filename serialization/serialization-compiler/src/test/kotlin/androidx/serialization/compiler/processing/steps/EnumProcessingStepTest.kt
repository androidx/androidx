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

package androidx.serialization.compiler.processing.steps

import androidx.serialization.compiler.models.Enum
import androidx.serialization.compiler.models.Reserved
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.junit.Test
import javax.lang.model.SourceVersion
import javax.tools.JavaFileObject

/** Unit tests for [EnumProcessingStep]. */
class EnumProcessingStepTest {
    private val enumValueCorrespondence = Correspondence.from({
            actual: Enum.Value?, expected: Pair<Int, String>? ->
        if (actual != null && expected != null) {
            actual.id == expected.first && actual.element.simpleName.contentEquals(expected.second)
        } else {
            actual == null && expected == null
        }
    }, "has ID and name")

    @Test
    fun testParsing() {
        val enum = compileEnum(JavaFileObjects.forSourceString("TestEnum", """
            import androidx.serialization.EnumValue;
            
            public enum TestEnum {
                @EnumValue(EnumValue.DEFAULT)
                DEFAULT,
                @EnumValue(1)
                ONE
            }
        """.trimIndent()))

        assertThat(enum.values)
            .comparingElementsUsing(enumValueCorrespondence)
            .containsExactly(0 to "DEFAULT", 1 to "ONE")
        assertThat(enum.reserved).isSameInstanceAs(Reserved.empty())
    }

    @Test
    fun testInvalidPrivateEnum() {
        val testEnum = JavaFileObjects.forSourceString("com.example.PrivateEnumTest", """
            package com.example;
            
            import androidx.serialization.EnumValue;
            
            public class PrivateEnumTest {
                private enum PrivateEnum {
                    @EnumValue(0) TEST
                }
            }
        """.trimIndent())

        assertThat(compile(testEnum)).hadErrorContaining(
            "Enum com.example.PrivateEnumTest.PrivateEnum is private and cannot be serialized")
    }

    @Test
    fun testInvalidPrivateNestedEnum() {
        val testEnum = JavaFileObjects.forSourceString("PrivateNestedEnumTest", """
            import androidx.serialization.EnumValue;
            
            public class PrivateNestedEnumTest {
                private static class NestedClass {
                    public enum NestedEnum {
                        @EnumValue(EnumValue.DEFAULT) TEST
                    }
                }
            }
        """.trimIndent())

        assertThat(compile(testEnum)).hadErrorContaining(
            "Enum PrivateNestedEnumTest.NestedClass.NestedEnum is not visible to its package")
    }

    @Test
    fun testInvalidEnumValueAnnotationLocation() {
        val testField = JavaFileObjects.forSourceString("EnumValueFieldTest", """
            import androidx.serialization.EnumValue;
            
            public class EnumValueFieldTest {
                @EnumValue(0)
                public int foo;
            }
        """.trimIndent())

        assertThat(compile(testField))
            .hadErrorContaining("@EnumValue must annotate an enum constant")
    }

    @Test
    fun testInvalidMissingEnumValue() {
        val testEnum = JavaFileObjects.forSourceString("MissingEnumValue", """
            import androidx.serialization.EnumValue;
            
            public enum MissingEnumValue {
                @EnumValue(0)
                ZERO,
                ONE
            }
        """.trimIndent())

        assertThat(compile(testEnum)).hadErrorContaining(
            "To avoid unexpected behavior, all enum constants in a serializable enum must be " +
                    "annotated with @EnumValue")
    }

    private fun compile(vararg sources: JavaFileObject): Compilation {
        return javac().withProcessors(SchemaCompilationProcessor()).compile(*sources)
    }

    private fun compileEnum(source: JavaFileObject): Enum {
        val processor = SchemaCompilationProcessor()
        assertThat(javac().withProcessors(processor).compile(source))
            .succeededWithoutWarnings()

        return processor.enum
    }

    private class SchemaCompilationProcessor : BasicAnnotationProcessor() {
        lateinit var enum: Enum

        override fun initSteps(): List<ProcessingStep> {
            return listOf(EnumProcessingStep(processingEnv) { enum = it })
    }

        override fun getSupportedSourceVersion(): SourceVersion {
            return SourceVersion.latest()
        }
    }
}
