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

package androidx.compose.runtime.a2ui

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTestApi::class)
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

    @Test
    fun observe_missingComponent_returnsLoadingState() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()

            setContent {
                val state = observeA2uiComponentState(surface)
                BasicText("State: ${state::class.simpleName}")
            }

            onNodeWithText("State: Loading").assertIsDisplayed()
        }

    @Test
    fun observe_errorComponent_returnsErrorState() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()
            surface.componentRegistry.reportError(
                "root",
                A2uiValidationException("Invalid root component", path = "/"),
            )

            setContent {
                val state = observeA2uiComponentState(surface)
                if (state is A2uiComponentState.Error) {
                    BasicText("Error: ${state.exception.message}")
                }
            }

            onNodeWithText("Error: Invalid root component").assertIsDisplayed()
        }

    @Test
    fun observe_validComponent_returnsSuccessState() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Button", mapOf("label" to "Click Me")))
            )

            setContent {
                val state = observeA2uiComponentState(surface)
                if (state is A2uiComponentState.Success) {
                    val label = state.component.properties.raw["label"] as? String
                    BasicText("Success: ${state.component.type} - $label")
                }
            }

            onNodeWithText("Success: Button - Click Me").assertIsDisplayed()
        }

    @Test
    fun observe_loadingToSuccess_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()

            setContent {
                when (val state = observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Loading -> BasicText("Loading")
                    is A2uiComponentState.Success -> BasicText("Ready: ${state.component.type}")
                    is A2uiComponentState.Error -> BasicText("Error")
                }
            }

            onNodeWithText("Loading").assertIsDisplayed()

            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Text", emptyMap()))
            )
            waitForIdle()

            onNodeWithText("Loading").assertIsNotDisplayed()
            onNodeWithText("Ready: Text").assertIsDisplayed()
        }

    @Test
    fun observe_errorToSuccess_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()
            surface.componentRegistry.reportError(
                "root",
                A2uiValidationException("Initial failure", path = "/"),
            )

            setContent {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Error -> BasicText("State: Error")
                    is A2uiComponentState.Success -> BasicText("State: Success")
                    else -> {}
                }
            }

            onNodeWithText("State: Error").assertIsDisplayed()

            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Button", emptyMap()))
            )
            waitForIdle()

            onNodeWithText("State: Error").assertIsNotDisplayed()
            onNodeWithText("State: Success").assertIsDisplayed()
        }

    @Test
    fun observe_successToError_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Button", emptyMap()))
            )

            setContent {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> BasicText("State: Success")
                    is A2uiComponentState.Error -> BasicText("State: Error")
                    else -> {}
                }
            }

            onNodeWithText("State: Success").assertIsDisplayed()

            surface.componentRegistry.reportError(
                "root",
                A2uiValidationException("Subsequent failure", path = "/"),
            )
            waitForIdle()

            onNodeWithText("State: Success").assertIsNotDisplayed()
            onNodeWithText("State: Error").assertIsDisplayed()
        }

    @Test
    fun observe_identicalComponentUpdate_doesNotRecompose() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Text", mapOf("text" to "A")))
            )

            var recompositions = 0

            setContent {
                val state = observeA2uiComponentState(surface)
                recompositions++
                if (state is A2uiComponentState.Success) {
                    BasicText("Recompositions: $recompositions")
                }
            }

            onNodeWithText("Recompositions: 1").assertIsDisplayed()
            val initialRecompositions = recompositions

            // Update with identical payload
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Text", mapOf("text" to "A")))
            )
            waitForIdle()

            onNodeWithText("Recompositions: 1").assertIsDisplayed()
            assertThat(recompositions).isEqualTo(initialRecompositions)
        }

    @Test
    fun observe_componentUpdateWithNewData_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Text", mapOf("text" to "Version 1")))
            )

            setContent {
                val state = observeA2uiComponentState(surface)
                if (state is A2uiComponentState.Success) {
                    val text = state.component.properties.raw["text"] as? String
                    BasicText("Success: $text")
                }
            }

            onNodeWithText("Success: Version 1").assertIsDisplayed()

            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Text", mapOf("text" to "Version 2")))
            )
            waitForIdle()

            onNodeWithText("Success: Version 1").assertIsNotDisplayed()
            onNodeWithText("Success: Version 2").assertIsDisplayed()
        }

    @Test
    fun observe_surfaceChange_recreatesState() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val surface1 = createSurfaceModel()
            surface1.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Surface 1 Content", emptyMap()))
            )

            val surface2 = createSurfaceModel()
            surface2.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "Surface 2 Content", emptyMap()))
            )

            var currentSurface by mutableStateOf(surface1)

            setContent {
                val state = observeA2uiComponentState(currentSurface)
                if (state is A2uiComponentState.Success) {
                    BasicText("Ready: ${state.component.type}")
                }
            }

            onNodeWithText("Ready: Surface 1 Content").assertIsDisplayed()

            currentSurface = surface2
            waitForIdle()

            onNodeWithText("Ready: Surface 1 Content").assertIsNotDisplayed()
            onNodeWithText("Ready: Surface 2 Content").assertIsDisplayed()
        }

    @Test
    fun observe_invalidRegistry_throwsException() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val invalidRegistry =
                object : A2uiCoreComponentRegistry {
                    override fun update(components: List<A2uiComponentPayload>) {}

                    override fun reportError(id: String, exception: A2uiException) {}

                    override fun close() {}
                }
            val surface = createSurfaceModel(registry = invalidRegistry)

            val caughtException =
                assertFailsWith<IllegalArgumentException> {
                    setContent { observeA2uiComponentState(surface) }
                }

            assertThat(caughtException)
                .hasMessageThat()
                .contains("requires an A2uiComponentRegistry")
        }

    private fun createSurfaceModel(
        registry: A2uiCoreComponentRegistry = A2uiComponentRegistry()
    ): A2uiCoreSurfaceModel {
        return A2uiCoreSurfaceModel(
            id = "TestSurface",
            catalog =
                object : A2uiCoreCatalog {
                    override val id: String = "TestCatalog"
                    override val components: List<A2uiCoreComponentDefinition> = emptyList()
                    override val functions: List<A2uiFunction> = emptyList()
                    override val themeSchema: A2uiSchema? = null

                    override fun getComponent(name: String): A2uiCoreComponentDefinition? = null

                    override fun getFunction(name: String): A2uiFunction? = null
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
            surface = surface,
            surfaceScope =
                object : CoroutineScope {
                    override val coroutineContext: CoroutineContext
                        get() = TODO("Not yet implemented")
                },
        )
}
