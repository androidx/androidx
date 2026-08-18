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

package androidx.a2ui.compose.runtime

import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiComponentRecordTest {

    @Test
    fun valid_equalsAndHashCode() {
        val sharedMap = mapOf("text" to "Hello World")
        val identicalMapDifferentInstance = mapOf("text" to "Hello World")
        val completelyDifferentMap = mapOf("text" to "Goodbye")

        val props1 = A2uiComponentProperties(sharedMap)
        val props2 = A2uiComponentProperties(sharedMap)
        val propsIdenticalContent = A2uiComponentProperties(identicalMapDifferentInstance)
        val propsDifferent = A2uiComponentProperties(completelyDifferentMap)

        // Group 1: same type, same underlying map instance
        val record1A = A2uiComponentRecord.Valid("Text", props1)
        val record1B = A2uiComponentRecord.Valid("Text", props2)

        // Group 2: different type, but same properties instance
        val record2 = A2uiComponentRecord.Valid("Button", props1)

        // Group 3: same type, identical map content, but different map instance.
        val record3 = A2uiComponentRecord.Valid("Text", propsIdenticalContent)

        // Group 4: completely different properties
        val record4 = A2uiComponentRecord.Valid("Text", propsDifferent)

        // Equality and hashCode within Group 1
        assertThat(record1A).isEqualTo(record1A)
        assertThat(record1A).isEqualTo(record1B)
        assertThat(record1B).isEqualTo(record1A)
        assertThat(record1A.hashCode()).isEqualTo(record1B.hashCode())

        // Inequality across groups
        assertThat(record1A).isNotEqualTo(record2)
        assertThat(record1A).isNotEqualTo(record3)
        assertThat(record1A).isNotEqualTo(record4)

        assertThat(record2).isNotEqualTo(record3)
        assertThat(record2).isNotEqualTo(record4)

        assertThat(record3).isNotEqualTo(record4)

        // Null and different types
        assertThat(record1A).isNotEqualTo(null)
        assertThat(record1A).isNotEqualTo(Any())
    }

    @Test
    fun error_equalsAndHashCode() {
        // Group 1: two distinct exception instances with the exact same semantic data
        val ex1 =
            A2uiValidationException(message = "Missing required property", path = "/user/name")
        val ex2 =
            A2uiValidationException(message = "Missing required property", path = "/user/name")

        val record1A = A2uiComponentRecord.Error(ex1)
        val record1B = A2uiComponentRecord.Error(ex2)

        // Group 2: different message, same path
        val exDiffMessage =
            A2uiValidationException(message = "Wrong data type", path = "/user/name")
        val record2 = A2uiComponentRecord.Error(exDiffMessage)

        // Group 3: different path
        val exDiffPath =
            A2uiValidationException(message = "Missing required property", path = "/user/age")
        val record3 = A2uiComponentRecord.Error(exDiffPath)

        // Group 4: different exception subclass, but same content
        val exDiffClass =
            A2uiRuntimeException(
                message = "Missing required property",
                context = mapOf("path" to "/user/name"),
            )
        val record4 = A2uiComponentRecord.Error(exDiffClass)

        // Equality and hashCode within Group 1
        assertThat(record1A).isEqualTo(record1A)
        assertThat(record1A).isEqualTo(record1B)
        assertThat(record1B).isEqualTo(record1A)
        assertThat(record1A.hashCode()).isEqualTo(record1B.hashCode())

        // Inequality across groups
        assertThat(record1A).isNotEqualTo(record2)
        assertThat(record1A).isNotEqualTo(record3)
        assertThat(record1A).isNotEqualTo(record4)

        assertThat(record2).isNotEqualTo(record3)
        assertThat(record2).isNotEqualTo(record4)

        assertThat(record3).isNotEqualTo(record4)

        // Null and different types
        assertThat(record1A).isNotEqualTo(null)
        assertThat(record1A).isNotEqualTo(Any())
    }
}
