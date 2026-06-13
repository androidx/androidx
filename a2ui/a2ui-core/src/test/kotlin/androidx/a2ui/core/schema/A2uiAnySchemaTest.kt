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

package androidx.a2ui.core.schema

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val TEST_DESCRIPTION_1 = "testDescription1"
private const val TEST_DESCRIPTION_2 = "testDescription2"
private const val DESCRIPTION_FIELD = "description"

@RunWith(JUnit4::class)
class A2uiAnySchemaTest {
    @Test
    fun toJsonSchema_withoutDescription_returnsCorrectJsonObject() {
        assertThat(Json.parseToJsonElement(A2uiAnySchema().toJsonSchema()))
            .isEqualTo(buildJsonObject {})
    }

    @Test
    fun toJsonSchema_withDescription_returnsCorrectJsonObject() {
        assertThat(Json.parseToJsonElement(A2uiAnySchema(TEST_DESCRIPTION_1).toJsonSchema()))
            .isEqualTo(buildJsonObject { put(DESCRIPTION_FIELD, TEST_DESCRIPTION_1) })
    }

    @Test
    fun equalsAndHashCode_behavesAccordingToContract() {
        EqualsTester()
            .addEqualityGroup(A2uiAnySchema(TEST_DESCRIPTION_1), A2uiAnySchema(TEST_DESCRIPTION_1))
            .addEqualityGroup(A2uiAnySchema(TEST_DESCRIPTION_2))
            .addEqualityGroup(A2uiAnySchema(null))
            .testEquals()
    }

    @Test
    fun toString_withDescription_returnsExpectedFormat() {
        val schema = A2uiAnySchema(TEST_DESCRIPTION_1)
        assertThat(schema.toString()).isEqualTo("Any(description=$TEST_DESCRIPTION_1)")
    }

    @Test
    fun toString_withoutDescription_returnsExpectedFormat() {
        val schema = A2uiAnySchema(null)
        assertThat(schema.toString()).isEqualTo("Any(description=null)")
    }
}
