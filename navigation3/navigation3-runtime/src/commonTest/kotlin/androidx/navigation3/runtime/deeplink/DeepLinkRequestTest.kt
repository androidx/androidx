/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.runtime.deeplink

import androidx.kruth.assertThat
import androidx.navigation3.runtime.IgnoreAndroidHostTestTarget
import kotlin.test.Test
import kotlin.test.assertFailsWith

@IgnoreAndroidHostTestTarget
class DeepLinkRequestTest {

    @Test
    fun testUri() {
        val uri = "test.com/test"
        val request = DeepLinkRequest("test.com/test")
        assertThat(request.uri.toString()).isEqualTo(uri)
        assertThat(request.extras.isEmpty()).isTrue()
    }

    @Test
    fun testExtra() {
        val intKey = object : RequestExtrasKey<Int> {}
        val strKey = object : RequestExtrasKey<String> {}
        val boolKey = object : RequestExtrasKey<Boolean> {}

        val extras = requestExtras {
            put(intKey, 42)
            put(strKey, "test")
        }

        val request = DeepLinkRequest(null, extras)
        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras.size).isEqualTo(2)
        assertThat(request.extras[intKey]).isEqualTo(42)
        assertThat(request.extras[strKey]).isEqualTo("test")
        assertThat(request.extras[boolKey]).isNull()
    }

    @Test
    fun testEmptyRequestFails() {
        assertFailsWith<IllegalArgumentException> { DeepLinkRequest() }
    }

    @Test
    fun testExtraContains() {
        val intKey = object : RequestExtrasKey<Int> {}
        val boolKey = object : RequestExtrasKey<Boolean> {}

        val extras = RequestExtras(mutableMapOf(intKey to 42))

        assertThat(intKey in extras).isTrue()
        assertThat(boolKey in extras).isFalse()
    }

    @Test
    fun testExtraDsl() {
        val testKey = object : RequestExtrasKey<Boolean> {}
        val request = DeepLinkRequest(null, requestExtras { put(testKey, true) })

        assertThat(request.uri).isNull()
        assertThat(request.extras[testKey]).isNotNull()
        assertThat(request.extras[testKey]).isEqualTo(true)
    }

    @Test
    fun testExtraWrongKey() {
        val testKey = object : RequestExtrasKey<Int> {}
        val wrongKey = object : RequestExtrasKey<Int> {}
        val request = DeepLinkRequest(null, requestExtras { put(testKey, 1) })

        assertThat(request.uri).isNull()
        assertThat(request.extras[wrongKey]).isNull()
    }

    @Test
    fun testExtrasPlus() {
        val intKey = object : RequestExtrasKey<Int> {}
        val strKey = object : RequestExtrasKey<String> {}

        val extras1 = RequestExtras(mutableMapOf(intKey to 1))
        val extras2 = RequestExtras(mutableMapOf(intKey to 2, strKey to "test"))

        val combined = extras1 + extras2
        assertThat(combined.size).isEqualTo(2)
        assertThat(combined[intKey]).isEqualTo(2)
        assertThat(combined[strKey]).isEqualTo("test")
    }

    @Test
    fun testExtrasMinus() {
        val intKey = object : RequestExtrasKey<Int> {}
        val strKey = object : RequestExtrasKey<String> {}

        val extras = RequestExtras(mutableMapOf(intKey to 1, strKey to "test"))
        val extrasToRemove = RequestExtras(mutableMapOf(intKey to 1))
        val withoutIntKey = extras - extrasToRemove

        assertThat(withoutIntKey.size).isEqualTo(1)
        assertThat(withoutIntKey[intKey]).isNull()
        assertThat(withoutIntKey[strKey]).isEqualTo("test")
    }
}
