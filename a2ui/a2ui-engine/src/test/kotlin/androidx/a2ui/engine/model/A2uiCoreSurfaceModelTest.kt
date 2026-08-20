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
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("MoveLambdaArgumentOutOfParentheses")
@RunWith(JUnit4::class)
class A2uiCoreSurfaceModelTest {

    private companion object {
        const val SURFACE_ID_1 = "surf-1"
        const val COMPONENT_ID_1 = "btn-1"
        const val COMPONENT_ID_2 = "input-1"
        const val COMPONENT_ID_3 = "btn-3"

        val emptyActionHandler: (A2uiUserAction) -> Unit = {}
        val emptyErrorHandler: (A2uiClientErrorMessage) -> Unit = {}
    }

    @Test
    fun constructor_validArguments_retainsProperties() {
        val catalog = TestCatalog()
        val theme = mapOf<String, Any?>("primaryColor" to "blue")
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()

        val surface =
            A2uiCoreSurfaceModel(
                id = SURFACE_ID_1,
                catalog = catalog,
                theme = theme,
                shouldSendDataModel = true,
                dataModel = dataModel,
                componentRegistry = registry,
                onDispatchAction = emptyActionHandler,
                onDispatchError = emptyErrorHandler,
            )

        assertThat(surface.id).isEqualTo(SURFACE_ID_1)
        assertThat(surface.catalog).isSameInstanceAs(catalog)
        assertThat(surface.dataModel).isSameInstanceAs(dataModel)
        assertThat(surface.componentRegistry).isSameInstanceAs(registry)
        assertThat(surface.theme).isEqualTo(theme)
        assertThat(surface.shouldSendDataModel).isTrue()
    }

    @Test
    fun constructor_defaults_usesDefaultsAndRetainsEmptyTheme() {
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()

        val surface =
            A2uiCoreSurfaceModel(
                id = SURFACE_ID_1,
                catalog = TestCatalog(),
                dataModel = dataModel,
                componentRegistry = registry,
                onDispatchAction = emptyActionHandler,
                onDispatchError = emptyErrorHandler,
            )

        assertThat(surface.theme).isEmpty()
        assertThat(surface.shouldSendDataModel).isFalse()
    }

    @Test
    fun updateDataModel_validPathAndValue_propagatesToDataModel() {
        val dataModel = TestDataModel()
        val surface = createTestSurface(dataModel = dataModel)

        surface.updateDataModel("/settings/volume", 10)

        assertThat(dataModel.updates["/settings/volume"]).isEqualTo(10)
    }

    @Test
    fun updateDataModel_nullValue_propagatesToDataModel() {
        val dataModel = TestDataModel()
        val surface = createTestSurface(dataModel = dataModel)

        surface.updateDataModel("/settings/volume", null)

        assertThat(dataModel.updates["/settings/volume"]).isNull()
    }

    @Test
    fun updateDataModel_relativeDataPath_dispatchesError() {
        var dispatchedError: A2uiClientErrorMessage? = null
        val surface = createTestSurface(onDispatchError = { dispatchedError = it })

        surface.updateDataModel("settings/volume", 10)

        assertThat(dispatchedError).isNotNull()
        assertThat(dispatchedError?.code).isEqualTo("VALIDATION_FAILED")
        assertThat(dispatchedError?.message).contains("absolute")
        assertThat(dispatchedError?.context?.get("path")).isEqualTo("settings/volume")
    }

    @Test
    fun updateDataModel_invalidEscapeSequence_dispatchesError() {
        var dispatchedError: A2uiClientErrorMessage? = null
        val surface = createTestSurface(onDispatchError = { dispatchedError = it })

        surface.updateDataModel("/~2/volume", 10)

        assertThat(dispatchedError).isNotNull()
        assertThat(dispatchedError?.code).isEqualTo("VALIDATION_FAILED")
        assertThat(dispatchedError?.message).contains("escape sequence")
        assertThat(dispatchedError?.context?.get("path")).isEqualTo("/~2/volume")
    }

    @Test
    fun updateDataModel_trailingEscapeSequence_dispatchesError() {
        var dispatchedError: A2uiClientErrorMessage? = null
        val surface = createTestSurface(onDispatchError = { dispatchedError = it })

        surface.updateDataModel("/volume~", 10)

        assertThat(dispatchedError).isNotNull()
        assertThat(dispatchedError?.code).isEqualTo("VALIDATION_FAILED")
        assertThat(dispatchedError?.message).contains("escape sequence")
        assertThat(dispatchedError?.context?.get("path")).isEqualTo("/volume~")
    }

    @Test
    fun updateComponents_validComponentDetails_propagatesToRegistry() {
        val registry = TestComponentRegistry()
        val surface = createTestSurface(componentRegistry = registry)
        val props = mapOf<String, Any?>("text" to "Click Me")

        surface.updateComponents(listOf(A2uiComponentPayload(COMPONENT_ID_1, "button", props)))

        val component = registry.components[COMPONENT_ID_1]
        assertThat(component).isNotNull()
        assertThat(component?.type).isEqualTo("button")
        assertThat(component?.properties).isEqualTo(props)
    }

    @Test
    fun updateComponents_emptyProperties_propagatesToRegistry() {
        val registry = TestComponentRegistry()
        val surface = createTestSurface(componentRegistry = registry)

        surface.updateComponents(listOf(A2uiComponentPayload(COMPONENT_ID_1, "button", emptyMap())))

        val component = registry.components[COMPONENT_ID_1]
        assertThat(component).isNotNull()
        assertThat(component?.properties).isEmpty()
    }

    @Test
    fun updateComponents_componentNotInCatalog_dispatchesErrorAndSkipsComponent() {
        val registry = TestComponentRegistry()
        var dispatchedError: A2uiClientErrorMessage? = null
        val errorHandler: (A2uiClientErrorMessage) -> Unit = { dispatchedError = it }
        val surface =
            createTestSurface(componentRegistry = registry, onDispatchError = errorHandler)
        val props = mapOf<String, Any?>("text" to "Click Me")

        surface.updateComponents(
            listOf(A2uiComponentPayload(COMPONENT_ID_1, "unknown_type", props))
        )

        assertThat(registry.components).isEmpty()
        assertThat(dispatchedError).isNotNull()
        assertThat(dispatchedError?.code).isEqualTo("VALIDATION_FAILED")
        assertThat(dispatchedError?.message).contains("unknown_type")
        assertThat(dispatchedError?.context?.get("path")).isEqualTo("/components/$COMPONENT_ID_1")
    }

    @Test
    fun updateComponents_invalidSchema_dispatchesErrorAndSkipsComponent() {
        val registry = TestComponentRegistry()
        var dispatchedError: A2uiClientErrorMessage? = null
        val errorHandler: (A2uiClientErrorMessage) -> Unit = { dispatchedError = it }
        val catalog = TestCatalog()
        val surface =
            createTestSurface(
                catalog = catalog,
                componentRegistry = registry,
                onDispatchError = errorHandler,
            )
        // "text" property is expected by "button" schema, but we'll provide an integer instead of a
        // string
        val invalidProps = mapOf<String, Any?>("text" to 123)

        surface.updateComponents(
            listOf(A2uiComponentPayload(COMPONENT_ID_1, "button", invalidProps))
        )

        assertThat(registry.components).isEmpty()
        assertThat(dispatchedError).isNotNull()
        assertThat(dispatchedError?.code).isEqualTo("VALIDATION_FAILED")
        assertThat(dispatchedError?.context?.get("path"))
            .isEqualTo("/components/$COMPONENT_ID_1/text")
    }

    @Test
    fun updateComponents_mixedValidAndInvalid_updatesValidAndDispatchesErrors() {
        val registry = TestComponentRegistry()
        val dispatchedErrors = mutableListOf<A2uiClientErrorMessage>()
        val errorHandler: (A2uiClientErrorMessage) -> Unit = { dispatchedErrors.add(it) }
        val surface =
            createTestSurface(componentRegistry = registry, onDispatchError = errorHandler)
        val validProps = mapOf<String, Any?>("text" to "Click Me")
        val invalidProps = mapOf<String, Any?>("text" to 123)

        surface.updateComponents(
            listOf(
                A2uiComponentPayload(COMPONENT_ID_1, "button", validProps),
                A2uiComponentPayload(COMPONENT_ID_2, "button", invalidProps),
                A2uiComponentPayload(COMPONENT_ID_3, "unknown_type", validProps),
            )
        )

        assertThat(registry.components).hasSize(1)
        assertThat(registry.components[COMPONENT_ID_1]).isNotNull()
        assertThat(dispatchedErrors).hasSize(2)
        assertThat(dispatchedErrors.map { it.code })
            .containsExactly("VALIDATION_FAILED", "VALIDATION_FAILED")
        assertThat(dispatchedErrors.map { it.context["path"] })
            .containsExactly("/components/$COMPONENT_ID_2/text", "/components/$COMPONENT_ID_3")
    }

    @Test
    fun dispatchAction_validInteraction_constructsAndPropagatesAction() {
        var dispatchedAction: A2uiUserAction? = null
        val actionHandler: (A2uiUserAction) -> Unit = { dispatchedAction = it }
        val surface =
            createTestSurface(onDispatchAction = actionHandler, timeProvider = { 123456789L })
        val actionDef =
            mapOf<String, Any?>(
                "event" to
                    mapOf<String, Any?>(
                        "name" to "click",
                        "context" to mapOf<String, Any?>("x" to 100, "y" to 200),
                    )
            )

        surface.dispatchAction(COMPONENT_ID_1, actionDef)

        assertThat(dispatchedAction).isInstanceOf(A2uiEventAction::class.java)
        val serverAction = dispatchedAction as A2uiEventAction
        assertThat(serverAction.eventName).isEqualTo("click")
        assertThat(serverAction.surfaceId).isEqualTo(SURFACE_ID_1)
        assertThat(serverAction.componentId).isEqualTo(COMPONENT_ID_1)
        assertThat(serverAction.context).isEqualTo(mapOf("x" to 100, "y" to 200))
        assertThat(serverAction.timestamp).isEqualTo(123456789L)
    }

    @Test
    fun dispatchAction_invalidPayload_throwsIllegalStateException() {
        val surface = createTestSurface()
        val actionDef = mapOf<String, Any?>()

        assertFailsWith<IllegalStateException> { surface.dispatchAction(COMPONENT_ID_1, actionDef) }
    }

    @Test
    fun dispatchError_validException_reportsToRegistryAndPropagatesError() {
        val registry = TestComponentRegistry()
        var dispatchedError: A2uiClientErrorMessage? = null
        val errorHandler: (A2uiClientErrorMessage) -> Unit = { dispatchedError = it }
        val surface =
            createTestSurface(componentRegistry = registry, onDispatchError = errorHandler)
        val exception = A2uiException.A2uiValidationException("invalid name", "user/name")

        surface.dispatchError(exception, COMPONENT_ID_2)

        assertThat(registry.reportedErrors[COMPONENT_ID_2]).isEqualTo(exception)
        assertThat(dispatchedError).isNotNull()
        assertThat(dispatchedError?.code).isEqualTo("VALIDATION_FAILED")
        assertThat(dispatchedError?.surfaceId).isEqualTo(SURFACE_ID_1)
        assertThat(dispatchedError?.message).isEqualTo("invalid name")
        assertThat(dispatchedError?.context).isEqualTo(mapOf("path" to "user/name"))
    }

    @Test
    fun dispose_activeSurface_cleansUpDataAndComponents() {
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()
        val surface = createTestSurface(dataModel = dataModel, componentRegistry = registry)

        surface.dispose()

        assertThat(dataModel.isDisposed).isTrue()
        assertThat(registry.isDisposed).isTrue()
    }

    @Test
    fun dispose_calledMultipleTimes_isIdempotent() {
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()
        val surface = createTestSurface(dataModel = dataModel, componentRegistry = registry)

        surface.dispose()
        surface.dispose()

        assertThat(dataModel.isDisposed).isTrue()
        assertThat(registry.isDisposed).isTrue()
    }

    @Test
    fun evaluatePayload_pathPayload_resolvesPathUsingValueResolver() {
        val dataModel = TestDataModel()
        val surface = createTestSurface(dataModel = dataModel)
        var capturedResolvedPath: A2uiDataPath? = null
        val valueResolver = A2uiCoreValueResolver { path ->
            capturedResolvedPath = path
            "evaluated_val"
        }
        val path = "/test"
        val payload = mapOf("path" to path)

        val result =
            surface.evaluatePayload(COMPONENT_ID_1, valueResolver, A2uiDataPath("/"), payload)

        assertThat(result).isEqualTo("evaluated_val")
        assertThat(capturedResolvedPath?.path).isEqualTo(path)
    }

    @Test
    fun evaluatePayload_callPayload_executesFunctionFromCatalog() {
        var functionCapturedCallArgs: Map<String, Any>? = null
        val function =
            object : A2uiFunction {
                override val definition = TestFunctionDefinition1

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any {
                    functionCapturedCallArgs = args
                    return "func_result"
                }
            }

        val catalog = TestCatalog(listOf(function))
        val surface = createTestSurface(catalog = catalog)
        val valueResolver = A2uiCoreValueResolver { null }

        val functionArgs = mapOf("arg1" to "val")
        val payload =
            mapOf("call" to TestFunctionDefinition1.name, "args" to mapOf("arg1" to "val"))

        val result =
            surface.evaluatePayload(COMPONENT_ID_1, valueResolver, A2uiDataPath("/"), payload)

        assertThat(functionCapturedCallArgs).isEqualTo(functionArgs)
        assertThat(result).isEqualTo("func_result")
    }

    @Test
    fun equalsAndHashCode_differentInstances_behavesCorrectly() {
        val dataModel1 = TestDataModel()
        val registry1 = TestComponentRegistry()
        val dataModel2 = TestDataModel()
        val registry2 = TestComponentRegistry()
        val surface1 =
            createTestSurface(
                id = SURFACE_ID_1,
                dataModel = dataModel1,
                componentRegistry = registry1,
            )
        val surface2 =
            createTestSurface(
                id = SURFACE_ID_1,
                dataModel = dataModel1,
                componentRegistry = registry1,
            )
        val surface3 =
            createTestSurface(id = "other", dataModel = dataModel2, componentRegistry = registry2)

        val equalsTester =
            EqualsTester().addEqualityGroup(surface1, surface2).addEqualityGroup(surface3)

        equalsTester.testEquals()
    }

    @Test
    fun toString_validInstance_containsAllProperties() {
        val theme = mapOf<String, Any?>("color" to "red")
        val surface =
            createTestSurface(id = SURFACE_ID_1, theme = theme, shouldSendDataModel = true)

        val result = surface.toString()

        assertThat(result).contains("id='$SURFACE_ID_1'")
        assertThat(result).contains("catalog=TestCatalog")
        assertThat(result).contains("theme=$theme")
        assertThat(result).contains("shouldSendDataModel=true")
    }

    @Test
    fun toString_emptyTheme_containsEmptyThemeString() {
        val surface = createTestSurface(theme = emptyMap())

        val result = surface.toString()

        assertThat(result).contains("theme={}")
    }

    @Test
    fun updateComponents_associatedComponentCache_clearsCachedValues() {
        val surface = createTestSurface()

        surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
            "cached_value_1"
        }

        surface.updateComponents(listOf(A2uiComponentPayload(COMPONENT_ID_1, "button", emptyMap())))

        val value1AfterClear =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                "cached_value_2"
            }
        assertThat(value1AfterClear).isEqualTo("cached_value_2")
    }

    @Test
    fun updateComponents_associatedComponentCacheWithDifferentComponent_cacheUntouched() {
        val surface = createTestSurface()

        surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
            "cached_value_1"
        }

        surface.updateComponents(listOf(A2uiComponentPayload(COMPONENT_ID_2, "button", emptyMap())))

        val value1AfterClear =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                "cached_value_2"
            }
        assertThat(value1AfterClear).isEqualTo("cached_value_1")
    }

    @Test
    fun getOrCreateCache_cacheDoesNotExists_createsFunctionScopedCacheAndReturnValue() {
        val surface = createTestSurface()
        var factoryCount = 0

        val cache =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                factoryCount++
                "cached_value_1"
            }

        assertThat(cache).isEqualTo("cached_value_1")
        assertThat(factoryCount).isEqualTo(1)
    }

    @Test
    fun getOrCreateCache_FunctionScoped_cacheExists_returnsCachedValue() {
        val surface = createTestSurface()
        var factoryCount = 0

        val cache =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                factoryCount++
                mutableListOf("cached_value_1")
            }

        val cacheRetry =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                factoryCount++
                mutableListOf("cached_value_2")
            }
        assertThat(cacheRetry).isSameInstanceAs(cache)
        assertThat(factoryCount).isEqualTo(1)
    }

    @Test
    fun getOrCreateFunctionScopedCache_differentDefinitions_returnsSeparateCaches() {
        val surface = createTestSurface()

        var factoryCount1 = 0
        var factoryCount2 = 0

        val cache1 =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                factoryCount1++
                "cached_value_1"
            }
        val cache2 =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition2) {
                factoryCount2++
                "cached_value_2"
            }

        assertThat(factoryCount1).isEqualTo(1)
        assertThat(factoryCount2).isEqualTo(1)
        assertThat(cache1).isEqualTo("cached_value_1")
        assertThat(cache2).isEqualTo("cached_value_2")
    }

    @Test
    fun getOrCreateFunctionScopedCache_differentComponents_returnsSeparateCaches() {
        val surface = createTestSurface()

        var factoryCount1 = 0
        var factoryCount2 = 0

        val cache1 =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                factoryCount1++
                "cached_value_1"
            }
        val cache2 =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_2, TestFunctionDefinition1) {
                factoryCount2++
                "cached_value_2"
            }

        assertThat(factoryCount1).isEqualTo(1)
        assertThat(factoryCount2).isEqualTo(1)
        assertThat(cache1).isEqualTo("cached_value_1")
        assertThat(cache2).isEqualTo("cached_value_2")
    }

    @Test
    fun dispose_clearsCaches() {
        val surface = createTestSurface()

        surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
            "cached_value_1"
        }

        surface.dispose()

        val valueAfter =
            surface.getOrCreateFunctionScopedCache(COMPONENT_ID_1, TestFunctionDefinition1) {
                "cached_value_2"
            }
        assertThat(valueAfter).isEqualTo("cached_value_2")
    }

    private fun createTestSurface(
        id: String = SURFACE_ID_1,
        catalog: A2uiCoreCatalog = TestCatalog(),
        theme: Map<String, Any?> = emptyMap(),
        shouldSendDataModel: Boolean = false,
        dataModel: A2uiCoreDataModel = TestDataModel(),
        componentRegistry: A2uiCoreComponentRegistry = TestComponentRegistry(),
        onDispatchAction: (A2uiUserAction) -> Unit = emptyActionHandler,
        onDispatchError: (A2uiClientErrorMessage) -> Unit = emptyErrorHandler,
        timeProvider: () -> Long = { 0L },
    ): A2uiCoreSurfaceModel {
        return A2uiCoreSurfaceModel(
            id = id,
            catalog = catalog,
            theme = theme,
            shouldSendDataModel = shouldSendDataModel,
            dataModel = dataModel,
            componentRegistry = componentRegistry,
            onDispatchAction = onDispatchAction,
            onDispatchError = onDispatchError,
            timeProvider = timeProvider,
        )
    }

    private object TestFunctionDefinition1 : A2uiFunctionDefinition {
        override val name: String = "test_name_1"
        override val description: String = "test_description_1"
        override val argumentSchema: A2uiSchema = A2uiAnySchema.INSTANCE
        override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.STRING
    }

    private object TestFunctionDefinition2 : A2uiFunctionDefinition {
        override val name: String = "test_name_2"
        override val description: String = "test_description_2"
        override val argumentSchema: A2uiSchema = A2uiAnySchema.INSTANCE
        override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.STRING
    }

    private class TestCatalog(functions: List<A2uiFunction> = emptyList()) : A2uiCoreCatalog {
        override val id: String = "test_catalog"
        override val componentDefinitions: A2uiCoreComponentDefinitionCollection =
            A2uiCoreComponentDefinitionCollection(
                listOf(
                    object : A2uiCoreComponentDefinition {
                        override val name = "button"
                        override val description = "A test button"
                        override val propertySchema =
                            A2uiObjectSchema(properties = mapOf("text" to A2uiStringSchema()))
                    }
                )
            )
        override val functions: A2uiFunctionCollection = A2uiFunctionCollection(functions)
        override val themeSchema: A2uiSchema? = null

        override fun equals(other: Any?): Boolean = other is TestCatalog

        override fun hashCode(): Int = TestCatalog::class.hashCode()

        override fun toString(): String = "TestCatalog"
    }

    private class TestDataModel : A2uiCoreDataModel {
        val updates = mutableMapOf<String, Any?>()
        var isDisposed = false

        override fun update(path: A2uiDataPath, value: Any?) {
            updates[path.normalizedPath] = value
        }

        override fun get(path: A2uiDataPath): Any? {
            return updates[path.normalizedPath]
        }

        override fun close() {
            isDisposed = true
        }
    }

    private class TestComponentRegistry : A2uiCoreComponentRegistry {
        val components = mutableMapOf<String, A2uiComponentPayload>()
        val reportedErrors = mutableMapOf<String, A2uiException>()
        var isDisposed = false

        override fun update(components: List<A2uiComponentPayload>) {
            for (c in components) {
                this.components[c.id] = c
            }
        }

        override fun reportError(id: String, exception: A2uiException) {
            reportedErrors[id] = exception
        }

        override fun close() {
            isDisposed = true
        }
    }
}
