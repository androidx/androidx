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

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.A2uiRuntimeCatalog
import androidx.a2ui.compose.runtime.a2uiRuntimeMessageProcessor
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiCatalogReadinessTest {

    @Test
    fun asReadinessEvaluator_delegatesToComponentIsReady() {
        var isReadyFlag by mutableStateOf(false)
        val testComponent = StubComponent("TestComponent") { isReadyFlag }
        val catalog = A2uiCatalog(TestCatalogId, listOf(testComponent))

        val evaluator = catalog.asReadinessEvaluator()

        runWithPipeline(
            catalog,
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap())),
        ) { surface, _, _ ->
            setContent {
                val state = observeA2uiComponentState(surface)
                if (state is A2uiComponentState.Success) {
                    val ready = evaluator.isReady(state.component)
                    BasicText(if (ready) "Ready" else "Not Ready")
                }
            }

            onNodeWithText("Not Ready").assertIsDisplayed()

            isReadyFlag = true
            waitForIdle()

            onNodeWithText("Ready").assertIsDisplayed()
        }
    }

    @Test
    fun asReadinessEvaluator_unknownComponentType_throws() {
        val testComponent = StubComponent("TestComponent")
        val sourceCatalog = A2uiCatalog(TestCatalogId, listOf(testComponent))
        val emptyCatalog = A2uiCatalog("empty_catalog", emptyList())

        val evaluator = emptyCatalog.asReadinessEvaluator()

        runWithPipeline(
            sourceCatalog,
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap())),
        ) { surface, _, _ ->
            var exception: Exception? = null
            try {
                setContent {
                    val state =
                        assertIs<A2uiComponentState.Success>(observeA2uiComponentState(surface))
                    evaluator.isReady(state.component)
                }
            } catch (e: IllegalStateException) {
                exception = e
            }

            assertThat(exception).hasMessageThat().contains("TestComponent")
            assertThat(exception).hasMessageThat().contains("not registered")
        }
    }

    @Test
    fun asReadinessEvaluator_isReadyWithDynamicBinding_reactsToDataModelChanges() {
        val boundProp = A2uiProperty.dynamicString("dynamicText")
        val testComponent =
            StubComponent("DataBoundComponent", properties = listOf(boundProp)) { props ->
                props.bind(boundProp) != null
            }
        val catalog = A2uiCatalog(TestCatalogId, listOf(testComponent))
        val payload =
            A2uiComponentPayload(
                "root",
                "DataBoundComponent",
                mapOf("dynamicText" to mapOf("path" to "/welcome")),
            )

        val evaluator = catalog.asReadinessEvaluator()

        runWithPipeline(catalog, listOf(payload)) { surface, processor, dispatcher ->
            setContent {
                val state = observeA2uiComponentState(surface)
                if (state is A2uiComponentState.Success) {
                    val ready = evaluator.isReady(state.component)
                    BasicText(if (ready) "Ready" else "Not Ready")
                }
            }

            onNodeWithText("Not Ready").assertIsDisplayed()

            processor.processMessage(
                A2uiUpdateDataModelMessage(TestSurfaceId, "/welcome", "Hello World")
            )
            dispatcher?.scheduler?.runCurrent()
            waitForIdle()

            onNodeWithText("Ready").assertIsDisplayed()
        }
    }

    @Test
    fun asReadinessEvaluator_defaultIsReady_returnsTrue() {
        val catalog = A2uiCatalog(TestCatalogId, listOf(StubComponent("TestComponent")))

        val evaluator = catalog.asReadinessEvaluator()

        runWithPipeline(
            catalog,
            listOf(A2uiComponentPayload("root", "TestComponent", emptyMap())),
        ) { surface, _, _ ->
            setContent {
                val state = assertIs<A2uiComponentState.Success>(observeA2uiComponentState(surface))
                val ready = evaluator.isReady(state.component)
                BasicText(if (ready) "Ready" else "Not Ready")
            }

            onNodeWithText("Ready").assertIsDisplayed()
        }
    }

    @Test
    fun asReadinessEvaluator_multipleComponents_delegatesToCorrectComponentType() {
        val readyComponent = StubComponent("ReadyComponent") { true }
        val unreadyComponent = StubComponent("UnreadyComponent") { false }
        val catalog = A2uiCatalog(TestCatalogId, listOf(readyComponent, unreadyComponent))

        val evaluator = catalog.asReadinessEvaluator()

        runWithPipeline(
            catalog,
            listOf(A2uiComponentPayload("root", "ReadyComponent", emptyMap())),
        ) { surface, processor, dispatcher ->
            setContent {
                val state = assertIs<A2uiComponentState.Success>(observeA2uiComponentState(surface))
                val ready = evaluator.isReady(state.component)
                BasicText("${state.component.type}: $ready")
            }

            onNodeWithText("ReadyComponent: true").assertIsDisplayed()

            processor.processMessage(
                A2uiUpdateComponentsMessage(
                    TestSurfaceId,
                    listOf(A2uiComponentPayload("root", "UnreadyComponent", emptyMap())),
                )
            )
            dispatcher?.scheduler?.runCurrent()
            waitForIdle()

            onNodeWithText("UnreadyComponent: false").assertIsDisplayed()
        }
    }

    private fun runWithPipeline(
        catalog: A2uiCatalog,
        componentPayloads: List<A2uiComponentPayload>,
        block: ComposeUiTest.(A2uiCoreSurfaceModel, A2uiMessageProcessor, TestDispatcher?) -> Unit,
    ) = runComposeUiTest {
        // Inherit the test dispatcher, but use a new job to detach the message processor loop from
        // the test job, so that the test doesn't hang.
        val testContext = currentCoroutineContext()
        val backgroundScope = CoroutineScope(testContext.minusKey(Job) + Job())

        // Hook into the test's lifecycle to terminate the background scope on test completion.
        testContext.job.invokeOnCompletion { backgroundScope.cancel() }

        val processor =
            a2uiRuntimeMessageProcessor(catalogs = listOf(catalog as A2uiRuntimeCatalog))
        backgroundScope.launch { processor.collectMessages() }

        processor.processMessage(A2uiCreateSurfaceMessage(TestSurfaceId, catalog.id))
        processor.processMessage(A2uiUpdateComponentsMessage(TestSurfaceId, componentPayloads))

        // Execute all queued background coroutines.
        val dispatcher = testContext[ContinuationInterceptor] as? TestDispatcher
        dispatcher?.scheduler?.runCurrent()

        val surface = assertIs<A2uiCoreSurfaceModel>(processor.activeSurfaces.value.first())
        block(surface, processor, dispatcher)
    }

    private class StubComponent(
        override val name: String,
        override val description: String = "Stub component $name",
        override val properties: List<A2uiProperty<*>> = emptyList(),
        private val isReadyBlock:
            @Composable
            A2uiComponentScope.(A2uiComponentProperties) -> Boolean =
            {
                true
            },
    ) : A2uiComponent {

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            isReadyBlock(properties)

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {}
    }
}

private const val TestCatalogId = "test_catalog"
private const val TestSurfaceId = "TestSurface"
