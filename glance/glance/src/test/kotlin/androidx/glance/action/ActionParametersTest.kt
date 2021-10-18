/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.action

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionParametersTest {
    @Test
    fun testKeyName() {
        val name = "key_name"
        val intKey1 = ActionParameters.Key<Int>(name)
        val intKey2 = ActionParameters.Key<Int>(name)
        assertThat(intKey1).isEqualTo(intKey2)
    }

    @Test
    fun testKeepLast() {
        val name = "key_name"
        val intKey1 = ActionParameters.Key<Int>(name)
        val intKey2 = ActionParameters.Key<Int>(name)
        val params = mutableActionParametersOf()

        params[intKey1] = 25
        assertTrue(intKey1 in params)
        assertTrue(intKey2 in params)
        assertThat(params[intKey1]).isEqualTo(25)
        assertThat(params[intKey2]).isEqualTo(25)

        params[intKey2] = 300
        assertTrue(intKey1 in params)
        assertTrue(intKey2 in params)
        assertThat(params[intKey1]).isEqualTo(300)
        assertThat(params[intKey2]).isEqualTo(300)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun testKeyConflict() {
        val name = "key_name"
        val intKey = ActionParameters.Key<Int>(name)
        val stringKey = ActionParameters.Key<String>(name)
        val params = mutableActionParametersOf()

        params[intKey] = 25
        assertTrue(intKey in params)
        assertTrue(stringKey in params)
        assertThat(params[intKey]).isEqualTo(25)
        assertFailsWith<ClassCastException> {
            val value = params[stringKey] // Only throws if assigned to a variable
        }
    }

    @Test
    fun testTypedKey() {
        val booleanKey = ActionParameters.Key<Boolean>("key_name")
        val params = actionParametersOf(booleanKey to true)
        assertTrue(booleanKey in params)
        assertTrue(params[booleanKey]!!)
    }

    @Test
    fun testNoKey() {
        val booleanKey = ActionParameters.Key<Boolean>("key_name")
        val params = mutableActionParametersOf()
        assertFalse(booleanKey in params)
    }

    @Test
    fun testMixedKeys() {
        val string = "test string"
        val booleanKey = ActionParameters.Key<Boolean>("key_name")
        val stringKey = ActionParameters.Key<String>("key_name2")
        val params = mutableActionParametersOf()
        params[stringKey] = string
        params[booleanKey] = false

        assertTrue(booleanKey in params)
        assertTrue(stringKey in params)
        assertFalse(params[booleanKey]!!)
        assertThat(params[stringKey]).isEqualTo(string)
    }

    @Test
    fun testMixedKeysConstructor() {
        val string = "test_string"
        val booleanKey = ActionParameters.Key<Boolean>("key_name")
        val stringKey = ActionParameters.Key<String>("key_name2")
        val params = actionParametersOf(stringKey to string, booleanKey to true)

        assertTrue(booleanKey in params)
        assertTrue(stringKey in params)
        assertTrue(params[booleanKey]!!)
        assertThat(params[stringKey]).isEqualTo(string)
    }

    @Test
    fun testClear() {
        val intKey = ActionParameters.Key<Int>("name")
        val params = mutableActionParametersOf(intKey to 309)
        val empty = mutableActionParametersOf()
        params.clear()
        assertThat(params).isEqualTo(empty)
    }

    @Test
    fun testEquals() {
        val keyName = "name"
        val value = "hello"
        val key1 = ActionParameters.Key<String>(keyName)
        val key2 = ActionParameters.Key<String>(keyName)
        val params1 = actionParametersOf(key1 to value)
        val params2 = actionParametersOf(key2 to value)
        assertThat(params1).isEqualTo(params2)
    }
}
