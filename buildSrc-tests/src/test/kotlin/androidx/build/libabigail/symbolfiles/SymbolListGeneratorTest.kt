/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.libabigail.symbolfiles

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Test

class SymbolListGeneratorTest {

    @Test
    fun writesSimpleFileToList() {
        val input = ByteArrayInputStream("""
            VERSION_1 {
                local:
                    hidden1;
                global:
                    foo;
                    bar;
            };
            VERSION_2 { # wasd
                # Implicit global scope.
                    woodly;
                    extern "C++" {
                        "doodly()";
                    };
                local:
                    qwerty;
            } VERSION_1;
        """.trimIndent().toByteArray())
        val output = ByteArrayOutputStream()
        val generator = SymbolListGenerator(input = input, output = output)
        generator.generate()
        assertThat(output.toString()).isEqualTo("""
            [abi_symbol_list]
              foo
              bar
              woodly
              doodly()
            
        """.trimIndent())
    }
}