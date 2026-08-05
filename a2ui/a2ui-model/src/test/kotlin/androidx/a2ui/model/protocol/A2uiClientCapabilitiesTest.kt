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
    fun constructor_defaultValues_returnsEmptyInlineCatalogs() {
        val capabilities = A2uiClientCapabilities(supportedCatalogIds = listOf("catalog1"))

        assertThat(capabilities.supportedCatalogIds).isEqualTo(listOf("catalog1"))
        assertThat(capabilities.inlineCatalogs).isEmpty()
    }

    @Test
    fun constructor_explicitValues_returnsExpected() {
        val inlineCatalog = TestInlineCatalog("inline1", mapOf("key" to "value"))
        val capabilities =
            A2uiClientCapabilities(
                supportedCatalogIds = listOf("catalog1", "catalog2"),
                inlineCatalogs = listOf(inlineCatalog),
            )

        assertThat(capabilities.supportedCatalogIds).isEqualTo(listOf("catalog1", "catalog2"))
        assertThat(capabilities.inlineCatalogs).isEqualTo(listOf(inlineCatalog))
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
    fun toPayloadMap_withInlineCatalogs_returnsExpectedMap() {
        val inlineCatalog = TestInlineCatalog("inline1", mapOf("id" to "inline1"))
        val capabilities =
            A2uiClientCapabilities(
                supportedCatalogIds = listOf("catalog1"),
                inlineCatalogs = listOf(inlineCatalog),
            )

        val map = capabilities.toPayloadMap()

        val expected =
            mapOf(
                "a2uiClientCapabilities" to
                    mapOf(
                        "v0.9" to
                            mapOf(
                                "supportedCatalogIds" to listOf("catalog1"),
                                "inlineCatalogs" to listOf(mapOf("id" to "inline1")),
                            )
                    )
            )

        assertThat(map).isEqualTo(expected)
    }

    @Test
    fun toPayloadMap_withEmptySupportedCatalogIds_includesEmptySupportedCatalogIds() {
        val inlineCatalog = TestInlineCatalog("inline1", mapOf("id" to "inline1"))
        val capabilities =
            A2uiClientCapabilities(
                supportedCatalogIds = emptyList(),
                inlineCatalogs = listOf(inlineCatalog),
            )

        val map = capabilities.toPayloadMap()

        val expected =
            mapOf(
                "a2uiClientCapabilities" to
                    mapOf(
                        "v0.9" to
                            mapOf(
                                "supportedCatalogIds" to emptyList<String>(),
                                "inlineCatalogs" to listOf(mapOf("id" to "inline1")),
                            )
                    )
            )

        assertThat(map).isEqualTo(expected)
    }

    @Test
    fun equalsAndHashCode_variousContracts_matchExpected() {
        val inlineCatalog1 = TestInlineCatalog("inline1", mapOf("a" to 1))
        val inlineCatalog2 = TestInlineCatalog("inline1", mapOf("a" to 1))
        EqualsTester()
            .addEqualityGroup(
                A2uiClientCapabilities(listOf("catalog1")),
                A2uiClientCapabilities(listOf("catalog1")),
            )
            .addEqualityGroup(
                A2uiClientCapabilities(listOf("catalog1"), listOf(inlineCatalog1)),
                A2uiClientCapabilities(listOf("catalog1"), listOf(inlineCatalog2)),
            )
            .addEqualityGroup(A2uiClientCapabilities(listOf("catalog2")))
            .testEquals()
    }

    @Test
    fun toString_returnsExpectedFormat() {
        val capabilities = A2uiClientCapabilities(listOf("catalog1"))
        assertThat(capabilities.toString())
            .isEqualTo("A2uiClientCapabilities(supportedCatalogIds=[catalog1], inlineCatalogs=[])")
    }

    @Test
    fun toString_withInlineCatalogs_returnsExpectedFormat() {
        val inlineCatalog = TestInlineCatalog("inline1", mapOf("key" to "value"))
        val capabilities = A2uiClientCapabilities(listOf("catalog1"), listOf(inlineCatalog))
        assertThat(capabilities.toString())
            .isEqualTo(
                "A2uiClientCapabilities(supportedCatalogIds=[catalog1], inlineCatalogs=[TestInlineCatalog(id=inline1, schemaMap={key=value})])"
            )
    }

    @Test
    fun inlineCatalog_serializationMethods_returnExpectedOutputs() {
        val schemaMap = mapOf("id" to "inline1", "components" to emptyMap<String, Any>())
        val inlineCatalog = TestInlineCatalog("inline1", schemaMap)

        assertThat(inlineCatalog.id).isEqualTo("inline1")
        assertThat(inlineCatalog.toJsonSchemaMap()).isEqualTo(schemaMap)
        assertThat(inlineCatalog.toJsonSchemaString()).isEqualTo(schemaMap.toString())
    }

    private class TestInlineCatalog(
        override val id: String,
        private val schemaMap: Map<String, Any?>,
    ) : A2uiInlineCatalog {
        override fun toJsonSchemaMap(): Map<String, Any?> = schemaMap

        override fun toJsonSchemaString(): String = schemaMap.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TestInlineCatalog) return false
            return id == other.id && schemaMap == other.schemaMap
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + schemaMap.hashCode()
            return result
        }

        override fun toString(): String {
            return "TestInlineCatalog(id=$id, schemaMap=$schemaMap)"
        }
    }
}
