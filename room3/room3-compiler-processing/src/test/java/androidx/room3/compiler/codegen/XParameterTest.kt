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

class XParameterTest {

    @Test
    fun equals() {
        fun createParameter(name: String) =
            XParameterSpec.builder(
                    name = name,
                    typeName = XClassName.get("test", "Foo"),
                    addJavaNullabilityAnnotation = false,
                )
                .build()
        val parameter1 = createParameter("a")
        val parameter2 = createParameter("b")
        val parameter3 = createParameter("a")

        assertThat(parameter1).isNotEqualTo(parameter2)
        assertThat(parameter1).isEqualTo(parameter3)
    }
}
