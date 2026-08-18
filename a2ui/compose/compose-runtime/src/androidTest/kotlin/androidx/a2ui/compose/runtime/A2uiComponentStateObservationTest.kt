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
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiComponentStateObservationTest {

    @Test
    fun observe_missingComponent_returnsLoadingState() = runComposeUiTest {
        val surface = createSurfaceModel()

        setContent {
            val state = observeA2uiComponentState(surface)
            BasicText("State: ${state::class.simpleName}")
        }

        onNodeWithText("State: Loading").assertIsDisplayed()
    }

    @Test
    fun observe_errorComponent_returnsErrorState() = runComposeUiTest {
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
    fun observe_validComponent_returnsSuccessState() = runComposeUiTest {
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
    fun observe_loadingToSuccess_triggersRecomposition() = runComposeUiTest {
        val surface = createSurfaceModel()

        setContent {
            when (val state = observeA2uiComponentState(surface)) {
                is A2uiComponentState.Loading -> BasicText("Loading")
                is A2uiComponentState.Success -> BasicText("Ready: ${state.component.type}")
                is A2uiComponentState.Error -> BasicText("Error")
            }
        }

        onNodeWithText("Loading").assertIsDisplayed()

        surface.componentRegistry.update(listOf(A2uiComponentPayload("root", "Text", emptyMap())))
        waitForIdle()

        onNodeWithText("Loading").assertIsNotDisplayed()
        onNodeWithText("Ready: Text").assertIsDisplayed()
    }

    @Test
    fun observe_errorToSuccess_triggersRecomposition() = runComposeUiTest {
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

        surface.componentRegistry.update(listOf(A2uiComponentPayload("root", "Button", emptyMap())))
        waitForIdle()

        onNodeWithText("State: Error").assertIsNotDisplayed()
        onNodeWithText("State: Success").assertIsDisplayed()
    }

    @Test
    fun observe_successToError_triggersRecomposition() = runComposeUiTest {
        val surface = createSurfaceModel()
        surface.componentRegistry.update(listOf(A2uiComponentPayload("root", "Button", emptyMap())))

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
    fun observe_identicalComponentUpdate_doesNotRecompose() = runComposeUiTest {
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
    fun observe_componentUpdateWithNewData_triggersRecomposition() = runComposeUiTest {
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
    fun observe_surfaceChange_recreatesState() = runComposeUiTest {
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
    fun observe_invalidSurface_throwsException() = runComposeUiTest {
        val invalidSurface =
            object : A2uiSurfaceModel {
                override val id: String = "TestSurface"
            }

        val caughtException =
            assertFailsWith<IllegalArgumentException> {
                setContent { observeA2uiComponentState(invalidSurface) }
            }

        assertThat(caughtException).hasMessageThat().contains("requires an A2uiCoreSurfaceModel")
    }

    @Test
    fun observe_invalidRegistry_throwsException() = runComposeUiTest {
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

        assertThat(caughtException).hasMessageThat().contains("requires an A2uiComponentRegistry")
    }

    @Test
    fun observe_noReadinessEvaluator_defaultsToReadyAndReturnsSuccess() = runComposeUiTest {
        val surface = createSurfaceModel()
        surface.componentRegistry.update(
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap()))
        )

        setContent {
            when (observeA2uiComponentState(surface)) {
                is A2uiComponentState.Success -> BasicText("Success")
                is A2uiComponentState.Loading -> BasicText("Loading")
                is A2uiComponentState.Error -> BasicText("Error")
            }
        }

        onNodeWithText("Success").assertIsDisplayed()
    }

    @Test
    fun observe_readinessEvaluatorReturnsFalse_returnsLoading() = runComposeUiTest {
        val surface = createSurfaceModel()
        surface.componentRegistry.update(
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap()))
        )

        val fakeEvaluator =
            object : A2uiReadinessEvaluator {
                @Composable
                override fun isReady(componentModel: A2uiComponentModel): Boolean = false
            }

        setContent {
            CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> BasicText("Success")
                    is A2uiComponentState.Loading -> BasicText("Loading")
                    is A2uiComponentState.Error -> BasicText("Error")
                }
            }
        }

        onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun observe_readinessEvaluatorReturnsTrue_returnsSuccess() = runComposeUiTest {
        val surface = createSurfaceModel()
        surface.componentRegistry.update(
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap()))
        )

        val fakeEvaluator =
            object : A2uiReadinessEvaluator {
                @Composable override fun isReady(componentModel: A2uiComponentModel): Boolean = true
            }

        setContent {
            CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> BasicText("Success")
                    is A2uiComponentState.Loading -> BasicText("Loading")
                    is A2uiComponentState.Error -> BasicText("Error")
                }
            }
        }

        onNodeWithText("Success").assertIsDisplayed()
    }

    @Test
    fun observe_readinessEvaluatorChangesToTrue_transitionsFromLoadingToSuccess() =
        runComposeUiTest {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "TestComponent", emptyMap()))
            )

            var isReadyState by mutableStateOf(false)
            val fakeEvaluator =
                object : A2uiReadinessEvaluator {
                    @Composable
                    override fun isReady(componentModel: A2uiComponentModel): Boolean = isReadyState
                }

            setContent {
                CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                    when (observeA2uiComponentState(surface)) {
                        is A2uiComponentState.Success -> BasicText("Success")
                        is A2uiComponentState.Loading -> BasicText("Loading")
                        is A2uiComponentState.Error -> BasicText("Error")
                    }
                }
            }

            onNodeWithText("Loading").assertIsDisplayed()

            isReadyState = true
            waitForIdle()

            onNodeWithText("Success").assertIsDisplayed()
        }

    @Test
    fun observe_componentHasError_ignoresEvaluatorAndReturnsError() = runComposeUiTest {
        val surface = createSurfaceModel()
        surface.componentRegistry.reportError("root", A2uiRuntimeException("Test error"))

        var evaluatorCalled = false
        val fakeEvaluator =
            object : A2uiReadinessEvaluator {
                @Composable
                override fun isReady(componentModel: A2uiComponentModel): Boolean {
                    evaluatorCalled = true
                    return true
                }
            }

        setContent {
            CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> BasicText("Success")
                    is A2uiComponentState.Loading -> BasicText("Loading")
                    is A2uiComponentState.Error -> BasicText("Error")
                }
            }
        }

        onNodeWithText("Error").assertIsDisplayed()
        assertThat(evaluatorCalled).isFalse()
    }

    @Test
    fun observe_componentIsMissing_ignoresEvaluatorAndReturnsLoading() = runComposeUiTest {
        val surface = createSurfaceModel()
        var evaluatorCalled = false
        val fakeEvaluator =
            object : A2uiReadinessEvaluator {
                @Composable
                override fun isReady(componentModel: A2uiComponentModel): Boolean {
                    evaluatorCalled = true
                    return true
                }
            }

        setContent {
            CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> BasicText("Success")
                    is A2uiComponentState.Loading -> BasicText("Loading")
                    is A2uiComponentState.Error -> BasicText("Error")
                }
            }
        }

        onNodeWithText("Loading").assertIsDisplayed()
        assertThat(evaluatorCalled).isFalse()
    }

    @Test
    fun observe_readinessEvaluatorChangesToFalse_transitionsFromSuccessToLoading() =
        runComposeUiTest {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "TestComponent", emptyMap()))
            )

            var isReadyState by mutableStateOf(true)
            val fakeEvaluator =
                object : A2uiReadinessEvaluator {
                    @Composable
                    override fun isReady(componentModel: A2uiComponentModel): Boolean = isReadyState
                }

            setContent {
                CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                    when (observeA2uiComponentState(surface)) {
                        is A2uiComponentState.Success -> BasicText("Success")
                        is A2uiComponentState.Loading -> BasicText("Loading")
                        is A2uiComponentState.Error -> BasicText("Error")
                    }
                }
            }

            onNodeWithText("Success").assertIsDisplayed()

            isReadyState = false
            waitForIdle()

            onNodeWithText("Loading").assertIsDisplayed()
        }

    @Test
    fun observe_componentUpdateWithUnreadyPayload_transitionsFromSuccessToLoading() =
        runComposeUiTest {
            val surface = createSurfaceModel()
            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "TestComponent", mapOf("text" to "ready")))
            )

            val fakeEvaluator =
                object : A2uiReadinessEvaluator {
                    @Composable
                    override fun isReady(componentModel: A2uiComponentModel): Boolean {
                        return componentModel.properties.raw["text"] == "ready"
                    }
                }

            setContent {
                CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                    when (observeA2uiComponentState(surface)) {
                        is A2uiComponentState.Success -> BasicText("Success")
                        is A2uiComponentState.Loading -> BasicText("Loading")
                        is A2uiComponentState.Error -> BasicText("Error")
                    }
                }
            }

            onNodeWithText("Success").assertIsDisplayed()

            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "TestComponent", mapOf("text" to "unready")))
            )
            waitForIdle()

            onNodeWithText("Loading").assertIsDisplayed()

            surface.componentRegistry.update(
                listOf(A2uiComponentPayload("root", "TestComponent", mapOf("text" to "ready")))
            )
            waitForIdle()

            onNodeWithText("Success").assertIsDisplayed()
        }

    @Test
    fun observe_componentHasErrorWhileNotReady_transitionsFromLoadingToError() = runComposeUiTest {
        val surface = createSurfaceModel()
        surface.componentRegistry.update(
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap()))
        )

        val fakeEvaluator =
            object : A2uiReadinessEvaluator {
                @Composable
                override fun isReady(componentModel: A2uiComponentModel): Boolean = false
            }

        setContent {
            CompositionLocalProvider(LocalA2uiReadinessEvaluator provides fakeEvaluator) {
                when (observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> BasicText("Success")
                    is A2uiComponentState.Loading -> BasicText("Loading")
                    is A2uiComponentState.Error -> BasicText("Error")
                }
            }
        }

        onNodeWithText("Loading").assertIsDisplayed()

        surface.componentRegistry.reportError("root", A2uiRuntimeException("Test error"))
        waitForIdle()

        onNodeWithText("Error").assertIsDisplayed()
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
