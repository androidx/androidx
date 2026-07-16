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

package androidx.a2ui.engine.processor

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.model.A2uiCoreCacheProvider
import androidx.a2ui.engine.model.A2uiCoreDynamicEvaluator
import androidx.a2ui.engine.model.A2uiCoreExecutionContext
import androidx.a2ui.engine.model.A2uiCoreValueResolver
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [A2uiActionHandler]. */
@RunWith(JUnit4::class)
class A2uiActionHandlerTest {

    @Test
    fun handleAction_serverEvent_resolvesContextAndEmits() = runTest {
        var emittedMessage: A2uiClientToServerMessage? = null
        val handler =
            A2uiActionHandler(
                actionInterceptors = emptyList(),
                emitToServer = { emittedMessage = it },
            )

        val action =
            A2uiEventAction(
                surfaceId = "surf-1",
                componentId = "comp-1",
                timestamp = 123L,
                eventName = "my_event",
                context = mapOf("path" to "/data/value"),
            )

        val executionContext =
            A2uiCoreExecutionContext(
                componentId = "comp-1",
                catalog = FakeCatalog(),
                dispatchError = { _, _ -> },
                valueResolver = FakeValueResolver(),
                dynamicEvaluator = FakeDynamicEvaluator(mapOf("resolved" to true)),
                cacheProvider = FakeCacheProvider(),
            )

        handler.handleAction(action, executionContext)

        assertThat(emittedMessage).isInstanceOf(A2uiClientEventMessage::class.java)
        val eventMessage = emittedMessage as A2uiClientEventMessage
        assertThat(eventMessage.type).isEqualTo("my_event")
        assertThat(eventMessage.surfaceId).isEqualTo("surf-1")
        assertThat(eventMessage.componentId).isEqualTo("comp-1")
        assertThat(eventMessage.timestamp).isEqualTo(123L)
        assertThat(eventMessage.context).isEqualTo(mapOf("resolved" to true))
    }

    @Test
    fun handleAction_localFunction_resolvesArgsAndExecutes() = runTest {
        var executedFunction: String? = null
        var executedArgs: Map<String, Any>? = null

        val handler = A2uiActionHandler(actionInterceptors = emptyList(), emitToServer = {})

        val action =
            A2uiFunctionCallAction(
                surfaceId = "surf-1",
                componentId = "comp-1",
                timestamp = 123L,
                functionName = "my_function",
                args = mapOf("path" to "/data/args"),
            )

        val catalog =
            FakeCatalog(
                object : A2uiFunction {
                    override val definition: A2uiFunctionDefinition =
                        object : A2uiFunctionDefinition {
                            override val name = "my_function"
                            override val description = "test"
                            override val argumentSchema = A2uiAnySchema.INSTANCE
                            override val returnType = A2uiFunctionReturnType.VOID
                        }

                    override fun execute(
                        args: Map<String, Any>,
                        executionContext: A2uiExecutionContext,
                    ): Any? {
                        executedFunction = definition.name
                        executedArgs = args
                        return null
                    }
                }
            )

        val executionContext =
            A2uiCoreExecutionContext(
                componentId = "comp-1",
                catalog = catalog,
                dispatchError = { _, _ -> },
                valueResolver = FakeValueResolver(),
                dynamicEvaluator = FakeDynamicEvaluator(mapOf("arg1" to "val1")),
                cacheProvider = FakeCacheProvider(),
            )

        handler.handleAction(action, executionContext)

        assertThat(executedFunction).isEqualTo("my_function")
        assertThat(executedArgs).isEqualTo(mapOf("arg1" to "val1"))
    }

    @Test
    fun handleAction_interceptorReturnsNull_actionIsDropped() = runTest {
        var emittedMessage: A2uiClientToServerMessage? = null
        val interceptor = A2uiActionInterceptor { null }
        val handler =
            A2uiActionHandler(
                actionInterceptors = listOf(interceptor),
                emitToServer = { emittedMessage = it },
            )

        val action =
            A2uiEventAction(
                surfaceId = "surf-1",
                componentId = "comp-1",
                timestamp = 123L,
                eventName = "my_event",
                context = emptyMap(),
            )

        val executionContext =
            A2uiCoreExecutionContext(
                componentId = "comp-1",
                catalog = FakeCatalog(),
                dispatchError = { _, _ -> },
                valueResolver = FakeValueResolver(),
                dynamicEvaluator = FakeDynamicEvaluator(emptyMap<String, Any>()),
                cacheProvider = FakeCacheProvider(),
            )

        handler.handleAction(action, executionContext)

        assertThat(emittedMessage).isNull()
    }

    @Test
    fun handleAction_interceptorReturnsTransformedAction_transformedActionIsProcessed() = runTest {
        var emittedMessage: A2uiClientToServerMessage? = null
        val interceptor = A2uiActionInterceptor { action ->
            if (action is A2uiEventAction) {
                A2uiEventAction(
                    surfaceId = action.surfaceId,
                    componentId = action.componentId,
                    timestamp = action.timestamp,
                    eventName = "transformed_event",
                    context = action.context,
                )
            } else {
                action
            }
        }
        val handler =
            A2uiActionHandler(
                actionInterceptors = listOf(interceptor),
                emitToServer = { emittedMessage = it },
            )

        val action =
            A2uiEventAction(
                surfaceId = "surf-1",
                componentId = "comp-1",
                timestamp = 123L,
                eventName = "original_event",
                context = emptyMap(),
            )

        val executionContext =
            A2uiCoreExecutionContext(
                componentId = "comp-1",
                catalog = FakeCatalog(),
                dispatchError = { _, _ -> },
                valueResolver = FakeValueResolver(),
                dynamicEvaluator = FakeDynamicEvaluator(emptyMap<String, Any>()),
                cacheProvider = FakeCacheProvider(),
            )

        handler.handleAction(action, executionContext)

        assertThat(emittedMessage).isInstanceOf(A2uiClientEventMessage::class.java)
        val eventMessage = emittedMessage as A2uiClientEventMessage
        assertThat(eventMessage.type).isEqualTo("transformed_event")
    }

    @Test
    fun handleAction_multipleInterceptors_executedInChain() = runTest {
        var emittedMessage: A2uiClientToServerMessage? = null
        val interceptor1 = A2uiActionInterceptor { action ->
            if (action is A2uiEventAction) {
                A2uiEventAction(
                    surfaceId = action.surfaceId,
                    componentId = action.componentId,
                    timestamp = action.timestamp,
                    eventName = action.eventName + "_first",
                    context = action.context,
                )
            } else {
                action
            }
        }
        val interceptor2 = A2uiActionInterceptor { action ->
            if (action is A2uiEventAction) {
                A2uiEventAction(
                    surfaceId = action.surfaceId,
                    componentId = action.componentId,
                    timestamp = action.timestamp,
                    eventName = action.eventName + "_second",
                    context = action.context,
                )
            } else {
                action
            }
        }
        val handler =
            A2uiActionHandler(
                actionInterceptors = listOf(interceptor1, interceptor2),
                emitToServer = { emittedMessage = it },
            )

        val action =
            A2uiEventAction(
                surfaceId = "surf-1",
                componentId = "comp-1",
                timestamp = 123L,
                eventName = "base",
                context = emptyMap(),
            )

        val executionContext =
            A2uiCoreExecutionContext(
                componentId = "comp-1",
                catalog = FakeCatalog(),
                dispatchError = { _, _ -> },
                valueResolver = FakeValueResolver(),
                dynamicEvaluator = FakeDynamicEvaluator(emptyMap<String, Any>()),
                cacheProvider = FakeCacheProvider(),
            )

        handler.handleAction(action, executionContext)

        assertThat(emittedMessage).isInstanceOf(A2uiClientEventMessage::class.java)
        val eventMessage = emittedMessage as A2uiClientEventMessage
        assertThat(eventMessage.type).isEqualTo("base_first_second")
    }

    @Test
    fun handleAction_multipleInterceptors_oneReturnsNull_abortsChainAndDropsAction() = runTest {
        var emittedMessage: A2uiClientToServerMessage? = null
        var secondInterceptorCalled = false
        val interceptor1 = A2uiActionInterceptor { null }
        val interceptor2 = A2uiActionInterceptor { action ->
            secondInterceptorCalled = true
            action
        }
        val handler =
            A2uiActionHandler(
                actionInterceptors = listOf(interceptor1, interceptor2),
                emitToServer = { emittedMessage = it },
            )

        val action =
            A2uiEventAction(
                surfaceId = "surf-1",
                componentId = "comp-1",
                timestamp = 123L,
                eventName = "my_event",
                context = emptyMap(),
            )

        val executionContext =
            A2uiCoreExecutionContext(
                componentId = "comp-1",
                catalog = FakeCatalog(),
                dispatchError = { _, _ -> },
                valueResolver = FakeValueResolver(),
                dynamicEvaluator = FakeDynamicEvaluator(emptyMap<String, Any>()),
                cacheProvider = FakeCacheProvider(),
            )

        handler.handleAction(action, executionContext)

        assertThat(emittedMessage).isNull()
        assertThat(secondInterceptorCalled).isFalse()
    }

    private class FakeCatalog(val func: A2uiFunction? = null) : A2uiCoreCatalog {
        override val id = "fake"
        override val components = emptyList<A2uiCoreComponentDefinition>()
        override val functions = if (func != null) listOf(func) else emptyList()
        override val themeSchema: A2uiSchema? = null

        override fun getComponent(name: String) = null

        override fun getFunction(name: String) = if (name == func?.definition?.name) func else null
    }

    private class FakeValueResolver : A2uiCoreValueResolver {
        override fun resolve(path: A2uiDataPath): Any? = null
    }

    private class FakeDynamicEvaluator(val result: Any?) : A2uiCoreDynamicEvaluator {
        override fun evaluate(
            dataPath: A2uiDataPath,
            payload: Any?,
            executionContext: A2uiExecutionContext,
        ): Any? = result
    }

    private class FakeCacheProvider : A2uiCoreCacheProvider {
        override fun <T : Any> getOrCreateFunctionScopedCache(
            componentId: String,
            functionDefinition: A2uiFunctionDefinition,
            factory: () -> T,
        ): T = factory()
    }
}
