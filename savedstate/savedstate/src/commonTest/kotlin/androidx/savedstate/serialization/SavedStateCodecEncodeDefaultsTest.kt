/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.savedstate.serialization

import androidx.kruth.assertThat
import androidx.savedstate.IgnoreWebTarget
import androidx.savedstate.serialization.utils.SavedStateSerializationBaseTest
import kotlin.test.Test
import kotlinx.serialization.Serializable

@IgnoreWebTarget
internal class SavedStateCodecEncodeDefaultsTest : SavedStateSerializationBaseTest() {

    @Test
    fun encodeDefaults_false() {
        val config = SavedStateConfiguration { encodeDefaults = false }
        doTest(Data(), config) {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[]")
        }
    }

    @Test
    fun encodeDefaults_true() {
        val config = SavedStateConfiguration { encodeDefaults = true }
        doTest(Data(), config) {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7]")
        }
    }

    @Test
    fun encodeDefault_true_nullWithNullableStaticType() {
        val config = SavedStateConfiguration { encodeDefaults = true }
        doTest<Data?>(null, config) {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[=null]")
        }
    }

    @Test
    fun encodeDefault_true_nonNullWithNullableStaticType() {
        val config = SavedStateConfiguration { encodeDefaults = true }
        doTest<Data?>(Data(), config) {
            assertThat(original).isEqualTo(deserialized)
            assertThat(representation).isEqualTo(platformRepresentation)
            assertThat(representation).isEqualTo("[value=7]")
        }
    }

    @Serializable private data class Data(val value: Int = 7)
}
