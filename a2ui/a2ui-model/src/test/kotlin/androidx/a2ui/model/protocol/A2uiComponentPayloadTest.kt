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

package androidx.a2ui.model.protocol

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class A2uiComponentPayloadTest {

    @Test
    fun componentPayload_allPropertiesProvided_propertiesAreStored() {
        val payload = A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES)
        assertThat(payload.id).isEqualTo(TEST_COMPONENT_ID)
        assertThat(payload.type).isEqualTo(TEST_TYPE)
        assertThat(payload.properties).isEqualTo(TEST_PROPERTIES)
    }

    @Test
    fun componentPayload_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES),
                A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES),
            )
            .addEqualityGroup(A2uiComponentPayload(TEST_COMPONENT_ID_2, TEST_TYPE, TEST_PROPERTIES))
            .addEqualityGroup(A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE_2, TEST_PROPERTIES))
            .addEqualityGroup(A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES_2))
            .addEqualityGroup(A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, emptyMap()))
            .addEqualityGroup(
                A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, mapOf(TEST_PROP_KEY to null))
            )
            .testEquals()
    }

    @Test
    fun componentPayload_toString_returnsExpectedFormat() {
        val payload = A2uiComponentPayload(TEST_COMPONENT_ID, TEST_TYPE, TEST_PROPERTIES)
        assertThat(payload.toString())
            .isEqualTo(
                "A2uiComponentPayload(id=$TEST_COMPONENT_ID, type=$TEST_TYPE, properties=$TEST_PROPERTIES)"
            )
    }

    companion object {
        private const val TEST_COMPONENT_ID = "test-component"
        private const val TEST_COMPONENT_ID_2 = "test-component-2"
        private const val TEST_TYPE = "test-type"
        private const val TEST_TYPE_2 = "test-type-2"

        private const val TEST_PROP_KEY = "prop"
        private const val TEST_PROP_KEY_2 = "prop-2"
        private const val TEST_PROP_VALUE = "value"
        private const val TEST_PROP_VALUE_2 = "value-2"

        private val TEST_PROPERTIES = mapOf(TEST_PROP_KEY to TEST_PROP_VALUE)
        private val TEST_PROPERTIES_2 = mapOf(TEST_PROP_KEY_2 to TEST_PROP_VALUE_2)
    }
}
