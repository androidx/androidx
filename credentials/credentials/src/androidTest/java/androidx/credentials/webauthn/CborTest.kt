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

package androidx.credentials.webauthn

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CborTest {

  @Test
  fun encodeDecode_numbers() {
    val obj = 1
    val cborOut = Cbor().encode(obj)
    assertThat(Cbor().decode(cborOut)).isEqualTo(obj)
  }

  @Test
  fun encodeDecode_byteArray() {
    val obj = byteArrayOf(1)
    val cborOut = Cbor().encode(obj)
    assertThat(Cbor().decode(cborOut)).isEqualTo(obj)
  }

  @Test
  fun encodeDecode_string() {
    val obj = "test"
    val cborOut = Cbor().encode(obj)
    assertThat(Cbor().decode(cborOut)).isEqualTo(obj)
  }

  @Test
  fun encodeDecode_list() {
    val obj = listOf("test")
    val cborOut = Cbor().encode(obj)
    assertThat(Cbor().decode(cborOut)).isEqualTo(obj)
  }

  @Test
  fun encodeDecode_map() {
    val obj = mapOf("key" to "value")
    val cborOut = Cbor().encode(obj)
    assertThat(Cbor().decode(cborOut)).isEqualTo(obj)
  }
}
