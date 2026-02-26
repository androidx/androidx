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

package androidx.navigation3.runtime

import androidx.kruth.assertThat
import kotlin.test.Test

class NavMetadataTest {

    @Test
    fun testKey_objectKey() {
        val metadata = metadata { put(TestObjectKey, TestObject) }
        assertThat(metadata[TestObjectKey]).isEqualTo(TestObject)
    }

    @Test
    fun testKey_classKey() {
        val metadata = metadata {
            put(classKey1, classObject1)
            put(classKey2, classObject2)
        }

        assertThat(metadata[classKey1]).isEqualTo(classObject1)
        assertThat(metadata[classKey2]).isEqualTo(classObject2)
    }

    @Test
    fun testKey_invalidKey() {
        val metadata = metadata { put(TestObjectKey, TestObject) }
        val wrongKey =
            object : NavMetadataKey<TestObject> {
                override fun toString(): String {
                    // for jsBrowserTest to differentiate instances of same type
                    return "wrongKey"
                }
            }

        assertThat(metadata[wrongKey]).isNull()
    }

    @Test
    fun testKey_differentKeyTypes() {
        val metadata = metadata {
            put(TestObjectKey, TestObject)
            put(classKey1, classObject1)
        }
        assertThat(metadata[TestObjectKey]).isEqualTo(TestObject)
        assertThat(metadata[classKey1]).isEqualTo(classObject1)
    }

    @Test
    fun testValue_lambdaValue() {
        val metadata = metadata {
            put(TestLambdaKey1) { 1 }
            put(TestLambdaKey2) { 2 }
        }
        assertThat(metadata[TestLambdaKey1]?.invoke()).isEqualTo(1)
        assertThat(metadata[TestLambdaKey2]?.invoke()).isEqualTo(2)
    }

    @Test
    fun testOverrideExistingValue() {
        val metadata = metadata {
            put(classKey1, classObject1)
            put(classKey1, classObject2)
        }
        assertThat(metadata[classKey1]).isEqualTo(classObject2)

        val metadata2 =
            metadata { put(classKey1, classObject1) } + metadata { put(classKey1, classObject2) }
        assertThat(metadata2[classKey1]).isEqualTo(classObject2)
    }

    @Test
    fun testPlus() {
        val metadata = metadata { put(classKey1, classObject1) } + mapOf("key" to "value")

        assertThat(metadata[classKey1]).isEqualTo(classObject1)
        assertThat(metadata["key"]).isEqualTo("value")

        val metadata2 =
            metadata { put(classKey1, classObject1) } + metadata { put(classKey2, classObject2) }
        assertThat(metadata2[classKey1]).isEqualTo(classObject1)
        assertThat(metadata2[classKey2]).isEqualTo(classObject2)
    }

    @Test
    fun testContains() {
        val metadata = metadata { put(classKey1, classObject1) }

        assertThat(metadata.contains(classKey1)).isTrue()
        assertThat(metadata.contains(classKey2)).isFalse()
    }

    private object TestObject

    private object TestObjectKey : NavMetadataKey<TestObject>

    private class TestClassObject(val arg: Char)

    private object TestLambdaKey1 : NavMetadataKey<() -> Int> {
        // for jsBrowserTest to differentiate instances of same type
        override fun toString() = "TestLambdaKey1"
    }

    private object TestLambdaKey2 : NavMetadataKey<() -> Int> {
        // for jsBrowserTest to differentiate instances of same type
        override fun toString() = "TestLambdaKey2"
    }

    private class TestClassKey(val arg: Int) : NavMetadataKey<TestClassObject> {
        // for jsBrowserTest to differentiate instances of same type
        override fun toString(): String {
            return super.toString() + "$arg"
        }
    }

    private val classKey1 = TestClassKey(1)
    private val classObject1 = TestClassObject('a')

    private val classKey2 = TestClassKey(2)
    private val classObject2 = TestClassObject('b')

    private object InfixKey : NavMetadataKey<() -> Int>
}
