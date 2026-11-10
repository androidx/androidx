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

package androidx.a2ui.model.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiCatalogDefinitionTest {

    @Test
    fun title_defaultImplementation_returnsNull() {
        assertThat(MinimalCatalogDefinition().title).isNull()
    }

    @Test
    fun description_defaultImplementation_returnsNull() {
        assertThat(MinimalCatalogDefinition().description).isNull()
    }

    @Test
    fun themeSchema_defaultImplementation_returnsNull() {
        assertThat(MinimalCatalogDefinition().themeSchema).isNull()
    }

    @Test
    fun isInline_defaultImplementation_returnsFalse() {
        assertThat(MinimalCatalogDefinition().isInline).isFalse()
    }

    /** Implements only the members of [A2uiCatalogDefinition] that have no default. */
    private class MinimalCatalogDefinition : A2uiCatalogDefinition {
        override val id: String = "https://example.com/catalog/v1"
        override val componentDefinitions: A2uiComponentDefinitionCollection =
            A2uiComponentDefinitionCollection()
        override val functions: A2uiFunctionCollection = A2uiFunctionCollection()
    }
}
