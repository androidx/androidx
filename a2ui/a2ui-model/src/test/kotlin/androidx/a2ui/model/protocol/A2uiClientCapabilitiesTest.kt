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

class A2uiClientCapabilitiesTest {

    @Test
    fun constructor_defaultValues_returnsExpected() {
        val capabilities =
            A2uiClientCapabilities(supportedCatalogIds = listOf("catalog1", "catalog2"))

        assertThat(capabilities.supportedCatalogIds).isEqualTo(listOf("catalog1", "catalog2"))
    }

    @Test
    fun toPayloadMap_returnsExpectedMap() {
        val capabilities = A2uiClientCapabilities(supportedCatalogIds = listOf("catalog1"))

        val map = capabilities.toPayloadMap()

        val expected =
            mapOf(
                "a2uiClientCapabilities" to
                    mapOf("v0.9" to mapOf("supportedCatalogIds" to listOf("catalog1")))
            )

        assertThat(map).isEqualTo(expected)
    }

    @Test
    fun equalsAndHashCode_variousContracts_matchExpected() {
        EqualsTester()
            .addEqualityGroup(
                A2uiClientCapabilities(listOf("catalog1")),
                A2uiClientCapabilities(listOf("catalog1")),
            )
            .addEqualityGroup(A2uiClientCapabilities(listOf("catalog2")))
            .testEquals()
    }

    @Test
    fun toString_returnsExpectedFormat() {
        val capabilities = A2uiClientCapabilities(listOf("catalog1"))
        assertThat(capabilities.toString())
            .isEqualTo("A2uiClientCapabilities(supportedCatalogIds=[catalog1])")
    }
}
