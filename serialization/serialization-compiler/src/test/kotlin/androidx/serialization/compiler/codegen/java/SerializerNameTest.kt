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

import org.junit.Test
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName

/** Unit tests for [serializerName]. */
class SerializerNameTest {
    @Test
    fun testSimpleName() {
        assertThat(serializerName(ClassName.get("com.example", "Test")))
            .isEqualTo(ClassName.get("com.example", "TestSerializer"))
    }

    @Test
    fun testNestedName() {
        assertThat(serializerName(ClassName.get("com.example", "Outer", "Inner")))
            .isEqualTo(ClassName.get("com.example", "Outer_InnerSerializer"))
    }

    @Test
    fun testDefaultPackage() {
        assertThat(serializerName(ClassName.get("", "Test")))
            .isEqualTo(ClassName.get("", "TestSerializer"))
    }
}
