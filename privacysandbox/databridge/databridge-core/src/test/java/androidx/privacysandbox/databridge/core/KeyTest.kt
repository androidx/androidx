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

package androidx.privacysandbox.databridge.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeyTest {

    @Test
    fun testKey_equals_sameNameAndType() {
        val key1 = Key("intKey", Type.INT)
        val key2 = Key("intKey", Type.INT)
        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun testKey_equals_sameNameAndDifferentType() {
        val key1 = Key("intKey", Type.INT)
        val key2 = Key("intKey", Type.STRING)
        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun testKey_notEquals_differentName() {
        val key1 = Key("intKey_1", Type.INT)
        val key2 = Key("intKey_2", Type.INT)
        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun testKey_notEquals_differentClass() {
        val key = Key("intKey", Type.INT)
        val other = "not a key"
        assertThat(key).isNotEqualTo(other)
    }

    @Test
    fun testKey_sameHashCode_sameNameAndType() {
        val key1 = Key("intKey", Type.INT)
        val key2 = Key("intKey", Type.INT)
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode())
    }

    @Test
    fun testKey_differentHashCode_differentName() {
        val key1 = Key("intKey_1", Type.INT)
        val key2 = Key("intKey_2", Type.INT)
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode())
    }

    @Test
    fun testKey_equalHashCode_sameName_differentType() {
        val key1 = Key("intKey_1", Type.INT)
        val key2 = Key("intKey_1", Type.LONG)
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode())
    }

    @Test
    fun testKey_differentHashCode_differentType() {
        val key1 = Key("intKey", Type.INT)
        val key2 = Key("stringKey", Type.STRING)
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode())
    }

    @Test
    fun testKey_toString_returnsCorrectFormat() {
        val key = Key("booleanKey", Type.BOOLEAN)
        assertThat(key.toString()).isEqualTo("Key : {name=booleanKey}")
    }
}
