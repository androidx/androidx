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

package androidx.serialization.compiler.codegen.java

import androidx.serialization.compiler.codegen.javaGenEnv
import androidx.serialization.compiler.testEnum
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import org.junit.Test

/** Unit tests for [generateEnumCoder] and [enumCoderName]. */
class EnumCoderTest {
    @Test
    fun testEnumCoderName() {
        assertThat(enumCoderName(ClassName.get("com.example", "Test")))
            .isEqualTo(ClassName.get("com.example", "\$SerializationTestEnumCoder"))

        assertThat(enumCoderName(ClassName.get("", "Outer", "Inner", "TestEnum")))
            .isEqualTo(ClassName.get("", "\$SerializationOuter_Inner_TestEnumEnumCoder"))
    }

    @Test
    fun testGenerateEnumCoder() {
        assertThat(generateEnumCoder(testEnum(), javaGenEnv(this::class)).toString()).contains("""
            package com.example;

            import androidx.annotation.NonNull;
            import androidx.annotation.Nullable;
            import javax.annotation.Generated;

            /**
             * Serialization of enum {@link TestEnum}.
             */
            @Generated("androidx.serialization.compiler.codegen.java.EnumCoderTest")
            public final class ${'$'}SerializationTestEnumEnumCoder {
                private ${'$'}SerializationTestEnumEnumCoder() {
                }

                public static int encode(@Nullable TestEnum testEnum) {
                    if (testEnum != null) {
                        switch (testEnum) {
                            case ONE:
                                return 1;
                            case TWO:
                                return 2;
                        }
                    }
                    return 0; // DEFAULT
                }

                @NonNull
                public static TestEnum decode(int value) {
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
    }
}
