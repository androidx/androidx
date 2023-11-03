/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl.converters

import androidx.appactions.interaction.proto.Entity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EntityConverterTest {
    @Test
    fun stringConverter_correctBehavior() {
        val stringConverter = EntityConverter.of(TypeSpec.STRING_TYPE_SPEC)
        assertThat(stringConverter.convert("hello")).isEqualTo(
            Entity.newBuilder().setStringValue("hello").build(),
        )
    }

    @Test
    fun booleanConverter_correctBehavior() {
        val booleanConverter = EntityConverter.of(TypeSpec.BOOL_TYPE_SPEC)

        assertThat(booleanConverter.convert(true)).isEqualTo(
            Entity.newBuilder().setBoolValue(true).build(),
        )
    }

    @Test
    fun numberConverter_correctBehavior() {
        val numberConverter = EntityConverter.of(TypeSpec.NUMBER_TYPE_SPEC)

        assertThat(numberConverter.convert(3.14)).isEqualTo(
            Entity.newBuilder().setNumberValue(3.14).build(),
        )
    }
}
