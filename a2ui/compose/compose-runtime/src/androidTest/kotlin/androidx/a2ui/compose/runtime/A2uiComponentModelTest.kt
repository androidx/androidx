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

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiComponentModelTest {

    @Test
    fun equalsAndHashCode_enforcesCorrectEqualityRules() {
        val surface1 = createSurfaceModel("s1")
        val surface2 = createSurfaceModel("s2")

        // A2uiComponentProperties relies on strict referential equality on its underlying raw map.
        // Creating two wrappers sharing the same map instance means they are considered equal.
        val sharedMap = mapOf("A" to 1)
        val props1a = A2uiComponentProperties(sharedMap)
        val props1b = A2uiComponentProperties(sharedMap)
        val props2 = A2uiComponentProperties(mapOf("B" to 2))

        val scope1 = createComponentScope(surface1)
        val scope2 = createComponentScope(surface2)

        // Group 1: same properties across all fields
        val baseModel = A2uiComponentModel(surface1, "Text", props1a, scope1)
        val equalModel = A2uiComponentModel(surface1, "Text", props1b, scope1)

        // Group 2: different surface
        val differentSurfaceModel = A2uiComponentModel(surface2, "Text", props1a, scope1)

        // Group 3: different type
        val differentTypeModel = A2uiComponentModel(surface1, "Button", props1a, scope1)

        // Group 4: different properties
        val differentPropertiesModel = A2uiComponentModel(surface1, "Text", props2, scope1)

        // Group 5: different scope
        val differentScopeModel = A2uiComponentModel(surface1, "Text", props1a, scope2)

        // Equality and hash code for logically equal objects (Group 1)
        assertThat(baseModel).isEqualTo(baseModel)
        assertThat(baseModel).isEqualTo(equalModel)
        assertThat(equalModel).isEqualTo(baseModel)
        assertThat(baseModel.hashCode()).isEqualTo(equalModel.hashCode())

        // Inequality for variations across all other groups
        assertThat(baseModel).isNotEqualTo(differentSurfaceModel)
        assertThat(baseModel).isNotEqualTo(differentTypeModel)
        assertThat(baseModel).isNotEqualTo(differentPropertiesModel)
        assertThat(baseModel).isNotEqualTo(differentScopeModel)

        // Null and unrelated types
        assertThat(baseModel).isNotEqualTo(null)
        assertThat(baseModel).isNotEqualTo("Unrelated Type")
    }

    private fun createSurfaceModel(id: String) =
        A2uiCoreSurfaceModel(
            id = id,
            catalog =
                object : A2uiCoreCatalog {
                    override val id: String = "TestCatalog"
                    override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
                        A2uiCoreComponentDefinitionCollection()
                    override val functions: A2uiFunctionCollection = A2uiFunctionCollection()
                    override val themeSchema: A2uiSchema? = null
                },
            dataModel = A2uiDataModel(),
            componentRegistry = A2uiComponentRegistry(),
            onDispatchAction = {},
            onDispatchError = {},
        )

    private fun createComponentScope(surface: A2uiCoreSurfaceModel): A2uiComponentScope =
        A2uiComponentScopeImpl(
            id = "TestScope",
            baseDataPath = A2uiDataPath("/"),
            surface = surface,
            surfaceScope =
                object : CoroutineScope {
                    override val coroutineContext: CoroutineContext
                        get() = TODO("Not yet implemented")
                },
        )
}
