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
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiComponentStateTest {

    @Test
    fun loadingState_equalsAndHashCode() {
        val state1 = A2uiComponentState.Loading
        val state2 = A2uiComponentState.Loading

        assertThat(state1).isEqualTo(state1)
        assertThat(state1).isEqualTo(state2)
        assertThat(state1.hashCode()).isEqualTo(state2.hashCode())
    }

    @Test
    fun errorState_equalsAndHashCode() {
        val exception1 = A2uiRuntimeException("First error")
        val exception1a = A2uiRuntimeException("First error")
        val exception2 = A2uiRuntimeException("Second error")

        val state1 = A2uiComponentState.Error(exception1)
        val state1Duplicate = A2uiComponentState.Error(exception1)
        val state1a = A2uiComponentState.Error(exception1a)
        val state2 = A2uiComponentState.Error(exception2)

        // Group 1: error states with the same exception instance and different instance but
        // same message are equal and share the same hashCode.
        assertThat(state1).isEqualTo(state1Duplicate)
        assertThat(state1).isEqualTo(state1a)
        assertThat(state1.hashCode()).isEqualTo(state1Duplicate.hashCode())
        assertThat(state1.hashCode()).isEqualTo(state1a.hashCode())

        // Group 2: error states with a different exception instance are not equal.
        assertThat(state1).isNotEqualTo(state2)
        assertThat(state1a).isNotEqualTo(state2)
    }

    @Test
    fun successState_equalsAndHashCode() {
        val surface = createSurfaceModel()
        val scope = createComponentScope(surface)

        // A2uiComponentProperties relies on referential equality on its map.
        val sharedMap = mapOf("A" to 1)
        val props1a = A2uiComponentProperties(sharedMap)
        val props1b = A2uiComponentProperties(sharedMap)
        val props2 = A2uiComponentProperties(mapOf("B" to 2))

        val component1a = A2uiComponentModel(surface, "Text", props1a, scope)
        val component1b = A2uiComponentModel(surface, "Text", props1b, scope)
        val component2 = A2uiComponentModel(surface, "Button", props2, scope)

        val state1a = A2uiComponentState.Success(component1a)
        val state1b = A2uiComponentState.Success(component1b)
        val state2 = A2uiComponentState.Success(component2)

        // Group 1: same underlying component models are equal and share the same hashCode.
        assertThat(state1a).isEqualTo(state1a)
        assertThat(state1a).isEqualTo(state1b)
        assertThat(state1a.hashCode()).isEqualTo(state1b.hashCode())

        // Group 2: different underlying component models are not equal.
        assertThat(state1a).isNotEqualTo(state2)
        assertThat(state1b).isNotEqualTo(state2)
    }

    private fun createSurfaceModel(
        registry: A2uiCoreComponentRegistry = A2uiComponentRegistry()
    ): A2uiCoreSurfaceModel {
        return A2uiCoreSurfaceModel(
            id = "TestSurface",
            catalog =
                object : A2uiCoreCatalog {
                    override val id: String = "TestCatalog"
                    override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
                        A2uiCoreComponentDefinitionCollection()
                    override val functions: A2uiFunctionCollection = A2uiFunctionCollection()
                    override val themeSchema: A2uiSchema? = null
                },
            dataModel = A2uiDataModel(),
            componentRegistry = registry,
            onDispatchAction = {},
            onDispatchError = {},
        )
    }

    private fun createComponentScope(surface: A2uiCoreSurfaceModel): A2uiComponentScope =
        A2uiComponentScopeImpl(
            id = "TestScope",
            baseDataPath = A2uiDataPath("/"),
            surface = surface,
        )
}
