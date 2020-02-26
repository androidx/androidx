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

import androidx.serialization.compiler.testEnum
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Unit tests for [enumSerializer]. */
class EnumSerializerTest {
    @Test
    fun testFullClass() {
        /* ktlint-disable max-line-length */
        assertThat(testJavaGenerator(this::class).enumSerializer(testEnum()).toString())
            .contains("""
                @Generated("androidx.serialization.compiler.codegen.java.EnumSerializerTest")
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
                                throw new IllegalArgumentException("Enum value " + value.toString() +
                                        " does not have a serialization ID.");
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
    }
}
