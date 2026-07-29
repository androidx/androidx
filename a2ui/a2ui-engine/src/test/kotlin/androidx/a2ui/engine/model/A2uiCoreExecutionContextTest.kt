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

package androidx.a2ui.engine.model

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("MoveLambdaArgumentOutOfParentheses")
@RunWith(JUnit4::class)
class A2uiCoreExecutionContextTest {

    @Test
    fun resolveValue_resolvesValueUsingValueResolver() {
        val path = A2uiDataPath("/test")
        var resolvedPath: A2uiDataPath? = null
        val resolver = A2uiCoreValueResolver { p ->
            resolvedPath = p
            "resolved_val"
        }
        val env = createTestEnvironment(valueResolver = resolver)

        val result = env.context.resolveValue(path)

        assertThat(result).isEqualTo("resolved_val")
        assertThat(resolvedPath).isEqualTo(path)
    }

    @Test
    fun executeFunction_validInput_returnsFunctionResult() {
        var functionExecutedWithArgs: Map<String, Any>? = null
        val testFunction = createTestFunction { args, _ ->
            functionExecutedWithArgs = args
            "func_result"
        }
        val env = createTestEnvironment(catalogFunctions = listOf(testFunction))

        val args = mapOf("arg1" to "val1")
        val result = env.context.executeFunction(FUNCTION_NAME, args)

        assertThat(result).isEqualTo("func_result")
        assertThat(functionExecutedWithArgs).isEqualTo(args)
    }

    @Test
    fun executeFunction_unregisteredFunction_failsAndDispatchesError() {
        val env = createTestEnvironment()

        val result = env.context.executeFunction(FUNCTION_NAME, emptyMap())

        assertThat(result).isNull()
        val dispatched = env.getDispatchedError()
        assertThat(dispatched).isNotNull()
        assertThat(dispatched?.componentId).isEqualTo(COMPONENT_ID_1)
        assertThat(dispatched?.exception)
            .isInstanceOf(A2uiException.A2uiRuntimeException::class.java)
        assertThat(dispatched?.exception?.message)
            .contains("Function '$FUNCTION_NAME' not found in catalog")
    }

    @Test
    fun executeFunction_functionThrowsValidationException_failsAndDispatchesValidationError() {
        val validationException =
            A2uiException.A2uiValidationException("Invalid arguments", "/args/prop")
        val testFunction = createTestFunction { _, _ -> throw validationException }
        val env = createTestEnvironment(catalogFunctions = listOf(testFunction))

        val result = env.context.executeFunction(FUNCTION_NAME, emptyMap())

        assertThat(result).isNull()
        val dispatched = env.getDispatchedError()
        assertThat(dispatched).isNotNull()
        assertThat(dispatched?.componentId).isEqualTo(COMPONENT_ID_1)
        assertThat(dispatched?.exception).isEqualTo(validationException)
    }

    @Test
    fun executeFunction_functionThrowsGenericException_failsAndDispatchesRuntimeException() {
        val genericException = IllegalStateException("Something went wrong")
        val testFunction = createTestFunction { _, _ -> throw genericException }
        val env = createTestEnvironment(catalogFunctions = listOf(testFunction))

        val result = env.context.executeFunction(FUNCTION_NAME, emptyMap())

        assertThat(result).isNull()

        val dispatched = env.getDispatchedError()
        assertThat(dispatched).isNotNull()
        assertThat(dispatched?.componentId).isEqualTo(COMPONENT_ID_1)
        val error = dispatched?.exception
        assertThat(error).isInstanceOf(A2uiException.A2uiRuntimeException::class.java)
        assertThat(error?.message).isEqualTo("Function '$FUNCTION_NAME' execution failed")
        assertThat(error?.context)
            .isEqualTo(mapOf("originalErrorMessage" to "Something went wrong"))
    }

    @Test
    fun evaluatePayload_evaluatesPayloadUsingDynamicEvaluator() {
        val path = A2uiDataPath("/test")
        var evaluatedDataPath: A2uiDataPath? = null
        var evaluatedPayload: Any? = null
        var evaluatedContext: A2uiExecutionContext? = null
        val evaluator =
            object : A2uiCoreDynamicEvaluator {
                override fun evaluate(
                    dataPath: A2uiDataPath,
                    payload: Any?,
                    executionContext: A2uiExecutionContext,
                ): Any? {
                    evaluatedDataPath = dataPath
                    evaluatedPayload = payload
                    evaluatedContext = executionContext
                    return "evaluated_val"
                }
            }
        val env = createTestEnvironment(dynamicEvaluator = evaluator)

        val result = env.context.evaluatePayload(path, "payload")

        assertThat(result).isEqualTo("evaluated_val")
        assertThat(evaluatedDataPath).isEqualTo(path)
        assertThat(evaluatedPayload).isEqualTo("payload")
        assertThat(evaluatedContext).isSameInstanceAs(env.context)
    }

    @Test
    fun getOrCreateCache_delegatesToFunctionScopedCacheProvider() {
        var cachedComponentId: String? = null
        var cachedFunctionDefinition: A2uiFunctionDefinition? = null
        val expectedValue = "cached_value"

        val provider =
            object : A2uiCoreCacheProvider {
                override fun <T : Any> getOrCreateFunctionScopedCache(
                    componentId: String,
                    functionDefinition: A2uiFunctionDefinition,
                    factory: () -> T,
                ): T {
                    cachedComponentId = componentId
                    cachedFunctionDefinition = functionDefinition
                    return factory()
                }
            }
        val env = createTestEnvironment(cacheProvider = provider)
        val functionDef =
            object : A2uiFunctionDefinition {
                override val name = FUNCTION_NAME
                override val description = "test func"
                override val argumentSchema = A2uiAnySchema.INSTANCE
                override val returnType = A2uiFunctionReturnType.STRING
            }

        val result = env.context.getOrCreateFunctionScopedCache(functionDef) { expectedValue }

        assertThat(result).isEqualTo(expectedValue)
        assertThat(cachedComponentId).isEqualTo(COMPONENT_ID_1)
        assertThat(cachedFunctionDefinition).isSameInstanceAs(functionDef)
    }

    private data class DispatchedError(val exception: A2uiException, val componentId: String?)

    private class TestEnvironment(
        val context: A2uiExecutionContext,
        val getDispatchedError: () -> DispatchedError?,
    )

    private val defaultCacheProvider =
        object : A2uiCoreCacheProvider {
            override fun <T : Any> getOrCreateFunctionScopedCache(
                componentId: String,
                functionDefinition: A2uiFunctionDefinition,
                factory: () -> T,
            ): T {
                return factory()
            }
        }

    private fun createTestEnvironment(
        catalogFunctions: List<A2uiFunction> = emptyList(),
        valueResolver: A2uiCoreValueResolver = A2uiCoreValueResolver { null },
        dynamicEvaluator: A2uiCoreDynamicEvaluator = A2uiCoreDynamicEvaluatorImpl,
        cacheProvider: A2uiCoreCacheProvider = defaultCacheProvider,
    ): TestEnvironment {
        var dispatchedError: DispatchedError? = null
        val context =
            A2uiCoreExecutionContext(
                componentId = COMPONENT_ID_1,
                catalog = TestCatalog(functions = catalogFunctions),
                dispatchError = { exception, componentId ->
                    dispatchedError = DispatchedError(exception, componentId)
                },
                valueResolver = valueResolver,
                dynamicEvaluator = dynamicEvaluator,
                cacheProvider = cacheProvider,
            )
        return TestEnvironment(context = context, getDispatchedError = { dispatchedError })
    }

    private fun createTestFunction(
        execute: (Map<String, Any>, A2uiExecutionContext) -> Any?
    ): A2uiFunction {
        return object : A2uiFunction {
            override val definition =
                object : A2uiFunctionDefinition {
                    override val name = FUNCTION_NAME
                    override val description = "test func"
                    override val argumentSchema = A2uiAnySchema.INSTANCE
                    override val returnType = A2uiFunctionReturnType.STRING
                }

            override fun execute(
                args: Map<String, Any>,
                executionContext: A2uiExecutionContext,
            ): Any? {
                return execute(args, executionContext)
            }
        }
    }

    private class TestCatalog(override val functions: List<A2uiFunction> = emptyList()) :
        A2uiCoreCatalog {
        override val id: String = "test_catalog"
        override val componentDefinitions: List<A2uiCoreComponentDefinition> = emptyList()
        override val themeSchema: A2uiSchema? = null

        override fun getComponentDefinition(name: String): A2uiCoreComponentDefinition? = null

        override fun getFunction(name: String): A2uiFunction? =
            functions.find { it.definition.name == name }
    }

    private companion object {
        const val COMPONENT_ID_1 = "btn-1"
        const val FUNCTION_NAME = "testFunction"
    }
}
