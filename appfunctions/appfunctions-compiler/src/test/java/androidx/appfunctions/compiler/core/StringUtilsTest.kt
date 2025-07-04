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

package androidx.appfunctions.compiler.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StringUtilsTest {
    @Test
    fun toPascalCase_simpleString() {
        assertThat("hello world".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_dotSeparator() {
        assertThat("hello.world".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_slashSeparator() {
        assertThat("hello/world".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_underscoreSeparator() {
        assertThat("hello_world".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_hyphenSeparator() {
        assertThat("hello-world".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_uppercaseString() {
        assertThat("HELLO_WORLD".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_mixedCaseString() {
        assertThat("hELLO_wORLD".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_emptyString_returnsEmptyString() {
        assertThat("".toPascalCase()).isEqualTo("")
    }

    @Test
    fun toPascalCase_leadingAndTrailingSeparators() {
        assertThat("_hello_world_".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_spacesAsSeparators() {
        assertThat("hello. world".toPascalCase()).isEqualTo("HelloWorld")
    }

    @Test
    fun toPascalCase_multipleSeparators() {
        assertThat("hello. world.test".toPascalCase()).isEqualTo("HelloWorldTest")
    }

    @Test
    fun toPascalCase_multipleAdjacentSeparators() {
        assertThat("hello...world".toPascalCase()).isEqualTo("HelloWorld")
    }
}
