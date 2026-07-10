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

package androidx.room3.compiler.codegen

import androidx.kruth.assertThat
import org.junit.Test

class XCodeBlockTest {

    @Test
    fun equals() {
        val codeBlock1 = XCodeBlock.of("a")
        val codeBlock2 = XCodeBlock.of("b")
        val codeBlock3 = XCodeBlock.of("a")

        assertThat(codeBlock1).isNotEqualTo(codeBlock2)
        assertThat(codeBlock1).isEqualTo(codeBlock3)
    }
}
