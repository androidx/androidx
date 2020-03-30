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

package androidx.serialization.compiler

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.junit.Test
import javax.tools.JavaFileObject

/** Integration tests for enum serializer generation. */
class EnumSerializerTest {
    @Test
    fun test() {
        val enum = JavaFileObjects.forSourceString("com.example.TestEnum", """
            package com.example;
            
            import androidx.serialization.EnumValue;
            
            public enum TestEnum {
                @EnumValue(EnumValue.DEFAULT)
                DEFAULT,
                @EnumValue(1)
                ONE,
                @EnumValue(2)
                TWO
            }
        """.trimIndent())

        /* ktlint-disable max-line-length */
        val serializer = JavaFileObjects.forSourceString("com.example.TestEnumSerializer", """
            package com.example;

            import androidx.annotation.NonNull;
            import androidx.serialization.runtime.internal.EnumSerializerV1;
            import java.lang.IllegalArgumentException;
            import java.lang.Override;
            import javax.annotation.processing.Generated;

            @Generated("androidx.serialization.compiler.SerializationProcessor")
            public final class TestEnumSerializer implements EnumSerializerV1<TestEnum> {
                public static final @NonNull TestEnumSerializer INSTANCE = new TestEnumSerializer();

                @Override
                public int encode(@NonNull TestEnum value) {
                    switch (value) {
                        case DEFAULT:
                            return 0;
                        case ONE:
                            return 1;
                        case TWO:
                            return 2;
                        default:
                            throw new IllegalArgumentException("Enum value " + value.toString()
                                    + " does not have a serialization ID.");
                    }
                }

                @Override
                public @NonNull TestEnum decode(int value) {
                    switch (value) {
                        case 1:
                            return TestEnum.ONE;
                        case 2:
                            return TestEnum.TWO;
                        default:
                            return TestEnum.DEFAULT;
                    }
                }
            }
        """.trimIndent())
        /* ktlint-enable max-line-length */

        assertThat(compile(enum))
            .generatedSourceFile("com.example.TestEnumSerializer")
            .hasSourceEquivalentTo(serializer)
    }

    private fun compile(vararg sources: JavaFileObject): Compilation {
        return javac().withProcessors(SerializationProcessor()).compile(*sources)
    }
}