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

package androidx.a2ui.compose.ui

import androidx.a2ui.compose.runtime.A2uiComponentModel
import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.A2uiRuntimeCatalog
import androidx.a2ui.compose.runtime.a2uiRuntimeMessageProcessor
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A2uiComponentTest {

    private val testProp = A2uiProperty.string("label")
    private val testComponent =
        object : A2uiComponent {
            override val name = TestComponentName
            override val description = "A test component."
            override val properties = listOf(testProp)

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val text = properties[testProp] ?: "DefaultFallback"
                BasicText(text = text, modifier = modifier)
            }
        }
    private val catalog = A2uiCatalog(TestCatalogId, listOf(testComponent))

    @Test
    fun a2uiComponent_validComponent_rendersSuccessfullyAndPassesModifier() =
        runWithPipeline(
            catalog = catalog as A2uiCoreCatalog,
            componentPayloads =
                listOf(
                    A2uiComponentPayload("root", TestComponentName, mapOf("label" to "Hello A2UI"))
                ),
        ) { surface ->
            setContent {
                val state = assertIs<A2uiComponentState.Success>(observeA2uiComponentState(surface))
                A2uiComponent(
                    component = state.component,
                    modifier = Modifier.testTag("test_component_tag"),
                )
            }

            onNodeWithTag("test_component_tag").assertIsDisplayed()
            onNodeWithText("Hello A2UI").assertIsDisplayed()
        }

    @Test
    fun a2uiComponent_nonComposeCatalog_throws() =
        runWithPipeline(
            catalog =
                object : A2uiCoreCatalog, A2uiRuntimeCatalog {
                    override val id = "non_compose_catalog"
                    override val componentDefinitions =
                        A2uiCoreComponentDefinitionCollection(
                            listOf(
                                object : A2uiCoreComponentDefinition {
                                    override val name = "TestComponent"
                                    override val description = ""
                                    override val propertySchema = A2uiObjectSchema()
                                }
                            )
                        )
                    override val functions = A2uiFunctionCollection()
                    override val themeSchema: A2uiSchema? = null
                },
            componentPayloads = listOf(A2uiComponentPayload("root", TestComponentName, emptyMap())),
        ) { surface ->
            var exception: IllegalStateException? = null
            try {
                setContent {
                    val state =
                        assertIs<A2uiComponentState.Success>(observeA2uiComponentState(surface))
                    A2uiComponent(component = state.component)
                }
            } catch (e: IllegalStateException) {
                exception = e
            }

            assertThat(exception).hasMessageThat().contains("Catalog must implement A2uiCatalog")
        }

    @Test
    fun a2uiComponent_withUnregisteredType_throwsIllegalStateException() =
        runWithPipeline(
            catalog = catalog as A2uiCoreCatalog,
            componentPayloads = listOf(A2uiComponentPayload("root", TestComponentName, emptyMap())),
        ) { surface ->
            var exception: IllegalStateException? = null
            try {
                setContent {
                    val state =
                        assertIs<A2uiComponentState.Success>(observeA2uiComponentState(surface))
                    // Create a model identical to the resolved one, but replace the component type.
                    val badModel =
                        A2uiComponentModel(
                            surface = state.component.surface,
                            type = "MissingComponent",
                            properties = state.component.properties,
                            scope = state.component.scope,
                        )
                    A2uiComponent(component = badModel)
                }
            } catch (e: IllegalStateException) {
                exception = e
            }

            assertThat(exception)
                .hasMessageThat()
                .contains("Component with type 'MissingComponent' is not registered")
        }

    private fun runWithPipeline(
        catalog: A2uiCoreCatalog,
        componentPayloads: List<A2uiComponentPayload>,
        interceptors: List<A2uiActionInterceptor> = emptyList(),
        block: ComposeUiTest.(A2uiCoreSurfaceModel) -> Unit,
    ) = runComposeUiTest {
        // Inherit the test dispatcher, but use a new job to detach the message processor loop from
        // the test job, so that the test doesn't hang.
        val testContext = currentCoroutineContext()
        val backgroundScope = CoroutineScope(testContext.minusKey(Job) + Job())

        // Hook into the test's lifecycle to terminate the background scope on test completion.
        testContext.job.invokeOnCompletion { backgroundScope.cancel() }

        val processor =
            a2uiRuntimeMessageProcessor(
                catalogs = listOf(catalog as A2uiRuntimeCatalog),
                interceptors = interceptors,
            )
        backgroundScope.launch { processor.collectMessages() }

        processor.processMessage(A2uiCreateSurfaceMessage(TestSurfaceId, catalog.id))
        processor.processMessage(A2uiUpdateComponentsMessage(TestSurfaceId, componentPayloads))

        // Execute all queued background coroutines.
        val dispatcher = testContext[ContinuationInterceptor] as? TestDispatcher
        dispatcher?.scheduler?.runCurrent()

        val surface = assertIs<A2uiCoreSurfaceModel>(processor.activeSurfaces.value.first())
        block(surface)
    }
}

private const val TestCatalogId = "TestCatalog"
private const val TestSurfaceId = "TestSurface"
private const val TestComponentName = "TestComponent"
