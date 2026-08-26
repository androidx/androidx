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

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.protocol.A2uiInlineCatalog
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class A2uiCoreCatalogAdapterTest {

    @Test
    fun toInlineCatalog_whenIsInlineTrue_returnsAdapterWithMatchingIdAndSchemas() {
        val testComponent = createTestComponent(TEST_COMPONENT_NAME, TEST_COMPONENT_DESCRIPTION)
        val catalog =
            createCatalog(
                title = TEST_CATALOG_TITLE,
                description = TEST_CATALOG_DESCRIPTION,
                components = listOf(testComponent),
                isInline = true,
            )

        val inlineCatalog = catalog.toInlineCatalog()
        val serializer = catalog.obtainSerializer()

        assertThat(inlineCatalog).isInstanceOf(A2uiCoreCatalogInlineAdapter::class.java)
        assertThat(inlineCatalog.id).isEqualTo(catalog.id)
        assertThat(inlineCatalog.toJsonSchemaMap()).isEqualTo(serializer.jsonSchemaMap)
        assertThat(inlineCatalog.toJsonSchemaString()).isEqualTo(serializer.jsonSchemaString)
    }

    @Test
    fun toInlineCatalog_whenCatalogImplementsA2uiInlineCatalog_returnsSameInstance() {
        val inlineCatalogImpl =
            object : A2uiCoreCatalog, A2uiInlineCatalog {
                override val id: String = TEST_CATALOG_ID_1
                override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
                    A2uiCoreComponentDefinitionCollection()
                override val functions: A2uiFunctionCollection = A2uiFunctionCollection()
                override val isInline: Boolean = true

                override fun toJsonSchemaMap(): Map<String, Any?> = mapOf("id" to id)

                override fun toJsonSchemaString(): String = "{\"id\":\"$id\"}"
            }

        val inlineCatalog = inlineCatalogImpl.toInlineCatalog()

        assertThat(inlineCatalog).isSameInstanceAs(inlineCatalogImpl)
    }

    @Test
    fun toInlineCatalog_whenIsInlineFalse_throwsIllegalStateException() {
        val catalog = createCatalog(id = TEST_CATALOG_ID_1, isInline = false)

        val exception = assertFailsWith<IllegalStateException> { catalog.toInlineCatalog() }

        assertThat(exception.message)
            .contains(
                "Cannot adapt A2uiCoreCatalog(id='$TEST_CATALOG_ID_1') to A2uiInlineCatalog because isInline is false."
            )
    }

    @Test
    fun toInlineCatalog_equalsAndHashCode_contract() {
        val catalog1 =
            createCatalog(id = TEST_CATALOG_ID_1, title = TEST_CATALOG_TITLE, isInline = true)
        val catalog2 =
            createCatalog(id = TEST_CATALOG_ID_1, title = TEST_CATALOG_TITLE, isInline = true)
        val catalog3 =
            createCatalog(id = TEST_CATALOG_ID_2, title = TEST_CATALOG_TITLE, isInline = true)

        EqualsTester()
            .addEqualityGroup(catalog1.toInlineCatalog(), catalog2.toInlineCatalog())
            .addEqualityGroup(catalog3.toInlineCatalog())
            .testEquals()
    }

    @Test
    fun toInlineCatalog_toString_returnsExpectedFormat() {
        val catalog = createCatalog(id = TEST_CATALOG_ID_1, isInline = true)

        val inlineCatalog = catalog.toInlineCatalog()

        assertThat(inlineCatalog.toString()).isEqualTo("A2uiInlineCatalog(id=$TEST_CATALOG_ID_1)")
    }

    private fun createCatalog(
        id: String = TEST_CATALOG_ID_1,
        title: String? = null,
        description: String? = null,
        components: List<A2uiCoreComponentDefinition> = emptyList(),
        functions: List<A2uiFunction> = emptyList(),
        themeSchema: A2uiSchema? = null,
        isInline: Boolean = false,
    ): A2uiCoreCatalog =
        TestCoreCatalog(
            id = id,
            title = title,
            description = description,
            components = components,
            functions = functions,
            themeSchema = themeSchema,
            isInline = isInline,
        )

    private class TestCoreCatalog(
        override val id: String,
        override val title: String? = null,
        override val description: String? = null,
        components: List<A2uiCoreComponentDefinition> = emptyList(),
        functions: List<A2uiFunction> = emptyList(),
        override val themeSchema: A2uiSchema? = null,
        override val isInline: Boolean = false,
    ) : A2uiCoreCatalog {
        override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
            A2uiCoreComponentDefinitionCollection(components)
        override val functions: A2uiFunctionCollection = A2uiFunctionCollection(functions)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TestCoreCatalog) return false
            return id == other.id && title == other.title && description == other.description
        }

        override fun hashCode(): Int = id.hashCode()
    }

    private fun createTestComponent(
        name: String,
        description: String,
    ): A2uiCoreComponentDefinition =
        object : A2uiCoreComponentDefinition {
            override val name: String = name
            override val description: String = description
            override val propertySchema: A2uiSchema = A2uiObjectSchema()
        }

    companion object {
        private const val TEST_CATALOG_ID_1 = "test_catalog_1"
        private const val TEST_CATALOG_ID_2 = "test_catalog_2"
        private const val TEST_CATALOG_TITLE = "Test Catalog Title"
        private const val TEST_CATALOG_DESCRIPTION = "Test Catalog Description"
        private const val TEST_COMPONENT_NAME = "TestComponent"
        private const val TEST_COMPONENT_DESCRIPTION = "Test Component Description"
    }
}
