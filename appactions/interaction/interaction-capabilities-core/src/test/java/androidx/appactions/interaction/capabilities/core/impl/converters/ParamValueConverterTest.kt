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

import androidx.appactions.interaction.proto.ParamValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParamValueConverterTest {
    @Test
    fun stringConverter_correctBehavior() {
        val stringParamValue = ParamValue.newBuilder().setStringValue("hello").build()
        val stringConverter = ParamValueConverter.of(TypeSpec.STRING_TYPE_SPEC)

        assertThat(stringConverter.fromParamValue(stringParamValue)).isEqualTo("hello")
        assertThat(stringConverter.toParamValue("hello")).isEqualTo(stringParamValue)
    }

    @Test
    fun booleanConverter_correctBehavior() {
        val booleanParamValue = ParamValue.newBuilder().setBoolValue(true).build()
        val booleanConverter = ParamValueConverter.of(TypeSpec.BOOL_TYPE_SPEC)

        assertThat(booleanConverter.fromParamValue(booleanParamValue)).isEqualTo(true)
        assertThat(booleanConverter.toParamValue(true)).isEqualTo(booleanParamValue)
    }

    @Test
    fun numberConverter_correctBehavior() {
        val numberParamValue = ParamValue.newBuilder().setNumberValue(3.14).build()
        val numberConverter = ParamValueConverter.of(TypeSpec.NUMBER_TYPE_SPEC)

        assertThat(numberConverter.fromParamValue(numberParamValue)).isEqualTo(3.14)
        assertThat(numberConverter.toParamValue(3.14)).isEqualTo(numberParamValue)
    }
}
