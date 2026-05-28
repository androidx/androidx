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

package androidx.a2ui.core.model

import androidx.a2ui.core.catalog.A2uiCoreCatalog
import androidx.a2ui.core.platform.A2uiComponentRegistry
import androidx.a2ui.core.platform.A2uiDataModel
import androidx.a2ui.core.protocol.A2uiClientError
import androidx.a2ui.core.protocol.A2uiComponentPayload
import androidx.a2ui.core.protocol.A2uiDataPath
import androidx.a2ui.core.protocol.A2uiException
import androidx.a2ui.core.protocol.A2uiUserAction
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("MoveLambdaArgumentOutOfParentheses")
@RunWith(JUnit4::class)
class A2uiSurfaceModelTest {

    private companion object {
        const val SURFACE_ID_1 = "surf-1"
        const val COMPONENT_ID_1 = "btn-1"
        const val COMPONENT_ID_2 = "input-1"

        val emptyActionHandler: (A2uiUserAction) -> Unit = {}
        val emptyErrorHandler: (A2uiClientError) -> Unit = {}
    }

    @Test
    fun constructor_validArguments_retainsProperties() {
        val catalog = TestCatalog()
        val theme = mapOf<String, Any?>("primaryColor" to "blue")
        val dataModel = TestDataModel()
        val registry = TestComponentRegistry()

        val surface =
            A2uiSurfaceModel(
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
            A2uiSurfaceModel(
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

        surface.updateDataModel(A2uiDataPath("/settings/volume"), 10)

        assertThat(dataModel.updates["/settings/volume"]).isEqualTo(10)
    }

    @Test
    fun updateDataModel_nullValue_propagatesToDataModel() {
        val dataModel = TestDataModel()
        val surface = createTestSurface(dataModel = dataModel)

        surface.updateDataModel(A2uiDataPath("/settings/volume"), null)

        assertThat(dataModel.updates["/settings/volume"]).isNull()
    }

    @Test
    fun updateComponent_validComponentDetails_propagatesToRegistry() {
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
    fun updateComponent_emptyProperties_propagatesToRegistry() {
        val registry = TestComponentRegistry()
        val surface = createTestSurface(componentRegistry = registry)

        surface.updateComponents(listOf(A2uiComponentPayload(COMPONENT_ID_1, "button", emptyMap())))

        val component = registry.components[COMPONENT_ID_1]
        assertThat(component).isNotNull()
        assertThat(component?.properties).isEmpty()
    }

    @Test
    fun dispatchAction_validInteraction_constructsAndPropagatesAction() {
        var dispatchedAction: A2uiUserAction? = null
        val actionHandler: (A2uiUserAction) -> Unit = { dispatchedAction = it }
        val surface =
            createTestSurface(onDispatchAction = actionHandler, timeProvider = { 123456789L })
        val actionDef =
            mapOf<String, Any?>(
                "type" to "click",
                "context" to mapOf<String, Any?>("x" to 100, "y" to 200),
            )

        surface.dispatchAction(COMPONENT_ID_1, actionDef)

        assertThat(dispatchedAction).isNotNull()
        assertThat(dispatchedAction?.type).isEqualTo("click")
        assertThat(dispatchedAction?.surfaceId).isEqualTo(SURFACE_ID_1)
        assertThat(dispatchedAction?.componentId).isEqualTo(COMPONENT_ID_1)
        assertThat(dispatchedAction?.context).isEqualTo(mapOf("x" to 100, "y" to 200))
        assertThat(dispatchedAction?.timestamp).isEqualTo(123456789L)
    }

    @Test
    fun dispatchAction_missingType_defaultsToUnknownAction() {
        var dispatchedAction: A2uiUserAction? = null
        val actionHandler: (A2uiUserAction) -> Unit = { dispatchedAction = it }
        val surface = createTestSurface(onDispatchAction = actionHandler)
        val actionDef = mapOf<String, Any?>("context" to emptyMap<String, Any?>())

        surface.dispatchAction(COMPONENT_ID_1, actionDef)

        assertThat(dispatchedAction).isNotNull()
        assertThat(dispatchedAction?.type).isEqualTo("unknown_action")
    }

    @Test
    fun dispatchAction_missingContext_defaultsToEmptyMap() {
        var dispatchedAction: A2uiUserAction? = null
        val actionHandler: (A2uiUserAction) -> Unit = { dispatchedAction = it }
        val surface = createTestSurface(onDispatchAction = actionHandler)
        val actionDef = mapOf<String, Any?>("type" to "click")

        surface.dispatchAction(COMPONENT_ID_1, actionDef)

        assertThat(dispatchedAction).isNotNull()
        assertThat(dispatchedAction?.context).isEmpty()
    }

    @Test
    fun dispatchError_validException_reportsToRegistryAndPropagatesError() {
        val registry = TestComponentRegistry()
        var dispatchedError: A2uiClientError? = null
        val errorHandler: (A2uiClientError) -> Unit = { dispatchedError = it }
        val surface =
            createTestSurface(componentRegistry = registry, onDispatchError = errorHandler)
        val exception = A2uiException.A2uiValidationException("invalid name", "user/name")

        surface.dispatchError(COMPONENT_ID_2, exception)

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

    private fun createTestSurface(
        id: String = SURFACE_ID_1,
        catalog: A2uiCoreCatalog = TestCatalog(),
        theme: Map<String, Any?> = emptyMap(),
        shouldSendDataModel: Boolean = false,
        dataModel: A2uiDataModel = TestDataModel(),
        componentRegistry: A2uiComponentRegistry = TestComponentRegistry(),
        onDispatchAction: (A2uiUserAction) -> Unit = emptyActionHandler,
        onDispatchError: (A2uiClientError) -> Unit = emptyErrorHandler,
        timeProvider: () -> Long = { 0L },
    ): A2uiSurfaceModel {
        return A2uiSurfaceModel(
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

    private class TestCatalog : A2uiCoreCatalog {
        override fun equals(other: Any?): Boolean = other is TestCatalog

        override fun hashCode(): Int = TestCatalog::class.hashCode()

        override fun toString(): String = "TestCatalog"
    }

    private class TestDataModel : A2uiDataModel {
        val updates = mutableMapOf<String, Any?>()
        var isDisposed = false

        override fun update(path: A2uiDataPath, value: Any?) {
            updates[path.normalizedPath] = value
        }

        override fun get(path: A2uiDataPath): Any? {
            return updates[path.normalizedPath]
        }

        override fun dispose() {
            isDisposed = true
        }
    }

    private class TestComponentRegistry : A2uiComponentRegistry {
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

        override fun dispose() {
            isDisposed = true
        }
    }
}
