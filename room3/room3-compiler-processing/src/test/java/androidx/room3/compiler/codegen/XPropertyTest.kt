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

class XPropertyTest {

    @Test
    fun equals() {
        fun createProperty(name: String) =
            XPropertySpec.builder(
                    name = name,
                    typeName = XClassName.get("test", "Foo"),
                    visibility = VisibilityModifier.PUBLIC,
                    isMutable = false,
                    addJavaNullabilityAnnotation = false,
                )
                .build()
        val property1 = createProperty("a")
        val property2 = createProperty("b")
        val property3 = createProperty("a")

        assertThat(property1).isNotEqualTo(property2)
        assertThat(property1).isEqualTo(property3)
    }
}
