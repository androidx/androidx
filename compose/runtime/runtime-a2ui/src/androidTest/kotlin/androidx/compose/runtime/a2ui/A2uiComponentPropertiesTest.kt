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

package androidx.compose.runtime.a2ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiComponentPropertiesTest {

    @Test
    fun get_returnsValue() {
        val rawMap = mapOf("foo" to "bar", "count" to 42)
        val properties = A2uiComponentProperties(rawMap)

        assertThat(properties["foo"]).isEqualTo("bar")
        assertThat(properties["count"]).isEqualTo(42)
        assertThat(properties["missing"]).isNull()
    }

    @Test
    fun equalsAndHashCode_enforcesStrictIdentityEquality() {
        val mapA = mapOf("key" to "value")
        val mapAIdentical = mapOf("key" to "value") // Deeply equal, but different instance
        val mapB = mapOf("key" to "different")

        val propsA1 = A2uiComponentProperties(mapA)
        val propsA2 = A2uiComponentProperties(mapA)
        val propsAIdentical = A2uiComponentProperties(mapAIdentical)
        val propsB = A2uiComponentProperties(mapB)

        // Reflexivity
        assertThat(propsA1).isEqualTo(propsA1)

        // Equality: wrapping the exact same map instance
        assertThat(propsA1).isEqualTo(propsA2)
        assertThat(propsA2).isEqualTo(propsA1)
        assertThat(propsA1.hashCode()).isEqualTo(propsA2.hashCode())

        // Inequality: wrapping structurally identical maps but different instances
        assertThat(propsA1).isNotEqualTo(propsAIdentical)
        assertThat(propsAIdentical).isNotEqualTo(propsA1)
        assertThat(propsA2).isNotEqualTo(propsAIdentical)

        // Inequality: wrapping a structurally different map instance
        assertThat(propsA1).isNotEqualTo(propsB)
        assertThat(propsB).isNotEqualTo(propsA1)
        assertThat(propsAIdentical).isNotEqualTo(propsB)

        // Edge cases: null and different types
        assertThat(propsA1).isNotEqualTo(null)
        assertThat(propsA1).isNotEqualTo(Any())
    }
}
