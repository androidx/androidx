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
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A2uiComponentScopeImplTest {

    private val dispatchedActions = mutableListOf<A2uiUserAction>()
    private val dispatchedErrors = mutableListOf<A2uiClientErrorMessage>()

    private val dataModel = A2uiDataModel()
    private val componentRegistry = A2uiComponentRegistry()
    private val catalog = createCatalog()
    private val surface = createSurface(catalog)

    @Test
    fun observeA2uiComponentState_relativeDataPath_resolvesCorrectly() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                )
            )
        )
        dataModel.update(A2uiDataPath("/base/nested/value"), "Nested Value")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state =
                assertIs<A2uiComponentState.Success>(
                    scope.observeA2uiComponentState(id = "child_component", baseDataPath = "nested")
                )
            val textProp = A2uiProperty.dynamicString("text")
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Nested Value")
    }

    @Test
    fun observeA2uiComponentState_absoluteDataPath_overridesPathAndResolves() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                )
            )
        )
        dataModel.update(A2uiDataPath("/global/value"), "Global Value")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state =
                assertIs<A2uiComponentState.Success>(
                    scope.observeA2uiComponentState(
                        id = "child_component",
                        baseDataPath = "/global",
                    )
                )
            val textProp = A2uiProperty.dynamicString("text")
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Global Value")
    }

    @Test
    fun observeA2uiComponentState_emptyDataPath_appendsCorrectly() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                )
            )
        )
        dataModel.update(A2uiDataPath("/base/value"), "Empty Scope Path Value")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state = scope.observeA2uiComponentState(id = "child_component", baseDataPath = "")
            val textProp = A2uiProperty.dynamicString("text")
            assertIs<A2uiComponentState.Success>(state)
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Empty Scope Path Value")
    }

    @Test
    fun observeA2uiComponentState_nullDataPath_usesBasePath() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                )
            )
        )
        dataModel.update(A2uiDataPath("/base/value"), "Base Value")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state =
                assertIs<A2uiComponentState.Success>(
                    scope.observeA2uiComponentState(id = "child_component", baseDataPath = null)
                )
            val textProp = A2uiProperty.dynamicString("text")
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Base Value")
    }

    @Test
    fun observeA2uiComponentState_dataPathChanges_updatesResolvedPath() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                )
            )
        )
        dataModel.update(A2uiDataPath("/base/path_a/value"), "Value A")
        dataModel.update(A2uiDataPath("/base/path_b/value"), "Value B")

        var currentPath by mutableStateOf("path_a")
        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state =
                assertIs<A2uiComponentState.Success>(
                    scope.observeA2uiComponentState(
                        id = "child_component",
                        baseDataPath = currentPath,
                    )
                )
            val textProp = A2uiProperty.dynamicString("text")
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()
        assertThat(result).isEqualTo("Value A")

        // Trigger recomposition by changing the path
        currentPath = "path_b"
        waitForIdle()

        assertThat(result).isEqualTo("Value B")
    }

    @Test
    fun observeA2uiComponentState_reference_delegatesCorrectly() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                )
            )
        )
        dataModel.update(A2uiDataPath("/base/nested/value"), "Nested Reference Value")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val reference = A2uiComponentReference(id = "child_component", baseDataPath = "nested")
            val state =
                assertIs<A2uiComponentState.Success>(scope.observeA2uiComponentState(reference))
            val textProp = A2uiProperty.dynamicString("text")
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Nested Reference Value")
    }

    @Test
    fun observeA2uiComponentState_referenceChanges_updatesComponentAndPath() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "comp_1",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                ),
                A2uiComponentPayload(
                    id = "comp_2",
                    type = "Text",
                    properties = mapOf("text" to mapOf("path" to "value")),
                ),
            )
        )
        dataModel.update(A2uiDataPath("/base/path_1/value"), "Component 1 Value")
        dataModel.update(A2uiDataPath("/base/path_2/value"), "Component 2 Value")

        var currentRef by mutableStateOf(A2uiComponentReference("comp_1", "path_1"))
        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state =
                assertIs<A2uiComponentState.Success>(scope.observeA2uiComponentState(currentRef))
            val textProp = A2uiProperty.dynamicString("text")
            result = with(state.component.scope) { state.component.properties.bind(textProp) }
        }
        waitForIdle()
        assertThat(result).isEqualTo("Component 1 Value")

        // Trigger recomposition by swapping the component reference
        currentRef = A2uiComponentReference(id = "comp_2", baseDataPath = "path_2")
        waitForIdle()

        assertThat(result).isEqualTo("Component 2 Value")
    }

    @Test
    fun observeA2uiComponentState_childError_updatesRegistryUsingChildId() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(id = "child_component", type = "Text", properties = emptyMap())
            )
        )

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state = scope.observeA2uiComponentState(id = "child_component")
            if (state is A2uiComponentState.Success) {
                SideEffect(state) {
                    state.component.scope.reportError(A2uiRuntimeException("Child Error"))
                }
            }
        }
        waitForIdle()

        val record = componentRegistry.get("child_component")
        assertIs<A2uiComponentRecord.Error>(record)
        assertThat(record.exception.message).isEqualTo("Child Error")
    }

    @Test
    fun observeA2uiComponentState_loadingToSuccess_triggersRecomposition() = runComposeUiTest {
        setContent {
            val scope = rememberComponentScope(path = "/base")
            when (val state = scope.observeA2uiComponentState(id = "late_child")) {
                is A2uiComponentState.Loading -> BasicText("Loading")
                is A2uiComponentState.Success -> BasicText("Success: ${state.component.type}")
                else -> {}
            }
        }

        onNodeWithText("Loading").assertIsDisplayed()

        componentRegistry.update(
            listOf(A2uiComponentPayload(id = "late_child", type = "Image", properties = emptyMap()))
        )
        waitForIdle()

        onNodeWithText("Loading").assertIsNotDisplayed()
        onNodeWithText("Success: Image").assertIsDisplayed()
    }

    @Test
    fun observeA2uiComponentState_registryUpdate_triggersRecomposition() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to "V1"),
                )
            )
        )

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val state =
                assertIs<A2uiComponentState.Success>(
                    scope.observeA2uiComponentState(id = "child_component")
                )
            BasicText("Content: ${state.component.properties.raw["text"]}")
        }

        onNodeWithText("Content: V1").assertIsDisplayed()

        componentRegistry.update(
            listOf(
                A2uiComponentPayload(
                    id = "child_component",
                    type = "Text",
                    properties = mapOf("text" to "V2"),
                )
            )
        )
        waitForIdle()

        onNodeWithText("Content: V1").assertIsNotDisplayed()
        onNodeWithText("Content: V2").assertIsDisplayed()
    }

    @Test
    fun observeA2uiComponentState_successToError_triggersRecomposition() = runComposeUiTest {
        componentRegistry.update(
            listOf(
                A2uiComponentPayload(id = "child_component", type = "Text", properties = emptyMap())
            )
        )

        setContent {
            val scope = rememberComponentScope(path = "/base")
            when (val state = scope.observeA2uiComponentState(id = "child_component")) {
                is A2uiComponentState.Success -> BasicText("Success: ${state.component.type}")
                is A2uiComponentState.Error -> BasicText("Error: ${state.exception.message}")
                else -> {}
            }
        }

        onNodeWithText("Success: Text").assertIsDisplayed()

        componentRegistry.reportError(
            "child_component",
            A2uiRuntimeException("Simulated agent hallucination"),
        )
        waitForIdle()

        onNodeWithText("Success: Text").assertIsNotDisplayed()
        onNodeWithText("Error: Simulated agent hallucination").assertIsDisplayed()
    }

    @Test
    fun observeA2uiComponentState_reference_loadingToSuccess_triggersRecomposition() =
        runComposeUiTest {
            setContent {
                val scope = rememberComponentScope(path = "/base")
                val reference =
                    A2uiComponentReference(id = "late_ref_child", baseDataPath = "nested")
                when (val state = scope.observeA2uiComponentState(reference)) {
                    is A2uiComponentState.Loading -> BasicText("Loading Ref")
                    is A2uiComponentState.Success ->
                        BasicText("Success Ref: ${state.component.type}")
                    else -> {}
                }
            }

            onNodeWithText("Loading Ref").assertIsDisplayed()

            componentRegistry.update(
                listOf(
                    A2uiComponentPayload(
                        id = "late_ref_child",
                        type = "Video",
                        properties = emptyMap(),
                    )
                )
            )
            waitForIdle()

            onNodeWithText("Loading Ref").assertIsNotDisplayed()
            onNodeWithText("Success Ref: Video").assertIsDisplayed()
        }

    @Test
    fun bind_validDynamicProperty_returnsEvaluatedValue() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/text_value"), "Hello World")
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "text_value")))
        val propDef = A2uiProperty.dynamicString("text")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bind(propDef) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Hello World")
    }

    @Test
    fun bind_staticLiteral_returnsLiteral() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("text" to "Literal String"))
        val propDef = A2uiProperty.dynamicString("text")

        var result: String? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bind(propDef) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Literal String")
        assertThat(dispatchedErrors).isEmpty()
    }

    @Test
    fun bind_localFunctionCall_returnsEvaluatedValue() = runComposeUiTest {
        val mockFunction =
            object : A2uiFunction {
                override val definition =
                    object : A2uiFunctionDefinition {
                        override val name = "test_func"
                        override val description = ""
                        override val argumentSchema = A2uiObjectSchema.INSTANCE
                        override val returnType = A2uiFunctionReturnType.STRING
                    }

                override fun execute(
                    args: Map<String, Any>,
                    executionContext: A2uiExecutionContext,
                ): Any {
                    return "Evaluated: ${args["arg"]}"
                }
            }
        val customSurface = createSurface(catalog = createCatalog(functions = listOf(mockFunction)))
        val props =
            A2uiComponentProperties(
                mapOf("text" to mapOf("call" to "test_func", "args" to mapOf("arg" to "Success")))
            )
        val propDef = A2uiProperty.dynamicString("text")

        var result: String? = null
        setContent {
            val scope =
                A2uiComponentScopeImpl(
                    id = "test_scope",
                    baseDataPath = A2uiDataPath("/base"),
                    surface = customSurface,
                    surfaceScope = rememberCoroutineScope(),
                )
            result = with(scope) { props.bind(propDef) }
        }
        waitForIdle()

        assertThat(result).isEqualTo("Evaluated: Success")
    }

    @Test
    fun bind_typeMismatch_returnsNullAndReportsError() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/num_value"), mapOf("a" to "b"))
        val props = A2uiComponentProperties(mapOf("number" to mapOf("path" to "num_value")))
        val propDef = A2uiProperty.dynamicNumber("number")

        var result: Number? = 123
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bind(propDef) }
        }
        waitForIdle()

        assertThat(result).isNull()
        assertThat(dispatchedErrors).hasSize(1)
        assertThat(dispatchedErrors.first().message).contains("Type mismatch")
    }

    @Test
    fun bind_evaluationException_returnsNullAndReportsError() = runComposeUiTest {
        // Invoke an unknown function to force evaluation to throw
        val props = A2uiComponentProperties(mapOf("text" to mapOf("call" to "unknown_func")))
        val propDef = A2uiProperty.dynamicString("text")

        var result: String? = "should become null"
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bind(propDef) }
        }
        waitForIdle()

        assertThat(result).isNull()
        assertThat(dispatchedErrors).hasSize(1)
        assertThat(dispatchedErrors.first().message).contains("not found in catalog")
    }

    @Test
    fun bind_missingProperty_returnsNull() = runComposeUiTest {
        val props = A2uiComponentProperties(emptyMap())
        val propDef = A2uiProperty.dynamicString("missing_key")

        var result: String? = "should become null"
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bind(propDef) }
        }

        waitForIdle()
        assertThat(result).isNull()
        // Missing property simply returns null without throwing errors
        assertThat(dispatchedErrors).isEmpty()
    }

    @Test
    fun bind_explicitNullData_returnsNullWithoutReportingError() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/empty_val"), null)
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "empty_val")))
        val propDef = A2uiProperty.dynamicString("text")

        var result: String? = "should become null"
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bind(propDef) }
        }

        waitForIdle()
        assertThat(result).isNull()
        // No errors should be dispatched because null is a valid value
        assertThat(dispatchedErrors).isEmpty()
    }

    @Test
    fun bind_scopeChanges_reevaluatesPayload() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/scopeA/val"), "Value A")
        dataModel.update(A2uiDataPath("/scopeB/val"), "Value B")
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "val")))
        val propDef = A2uiProperty.dynamicString("text")
        var currentPath by mutableStateOf("/scopeA")

        setContent {
            val scope = rememberComponentScope(path = currentPath)
            val result = with(scope) { props.bind(propDef) } ?: "Empty"
            BasicText("Result: $result")
        }

        onNodeWithText("Result: Value A").assertIsDisplayed()

        currentPath = "/scopeB"
        waitForIdle()

        onNodeWithText("Result: Value A").assertIsNotDisplayed()
        onNodeWithText("Result: Value B").assertIsDisplayed()
    }

    @Test
    fun bind_reactivity_updatesWhenDataModelChanges() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/message"), "Initial Message")
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "message")))
        val propDef = A2uiProperty.dynamicString("text")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val result = with(scope) { props.bind(propDef) } ?: "Empty"
            BasicText("Result: $result")
        }

        onNodeWithText("Result: Initial Message").assertIsDisplayed()

        dataModel.update(A2uiDataPath("/base/message"), "Updated Message")
        waitForIdle()

        onNodeWithText("Result: Initial Message").assertIsNotDisplayed()
        onNodeWithText("Result: Updated Message").assertIsDisplayed()
    }

    @Test
    fun bind_reactivity_updatesWhenDataBecomesAvailable() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "delayed_val")))
        val propDef = A2uiProperty.dynamicString("text")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val result = with(scope) { props.bind(propDef) } ?: "Loading..."
            BasicText(result)
        }

        onNodeWithText("Loading...").assertIsDisplayed()

        dataModel.update(A2uiDataPath("/base/delayed_val"), "Data Arrived")
        waitForIdle()

        onNodeWithText("Loading...").assertIsNotDisplayed()
        onNodeWithText("Data Arrived").assertIsDisplayed()
    }

    @Test
    fun bind_reactivity_updatesWhenDataIsDeleted() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/removable"), "Will Be Deleted")
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "removable")))
        val propDef = A2uiProperty.dynamicString("text")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val result = with(scope) { props.bind(propDef) } ?: "Deleted"
            BasicText(result)
        }

        onNodeWithText("Will Be Deleted").assertIsDisplayed()

        dataModel.update(A2uiDataPath("/base/removable"), null)
        waitForIdle()

        onNodeWithText("Will Be Deleted").assertIsNotDisplayed()
        onNodeWithText("Deleted").assertIsDisplayed()
    }

    @Test
    fun bind_reactivity_reportsErrorWhenDataChangesToInvalidType() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/num_val"), 100) // Stored as an Int
        val props = A2uiComponentProperties(mapOf("number" to mapOf("path" to "num_val")))
        val propDef = A2uiProperty.dynamicNumber("number")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val result = with(scope) { props.bind(propDef) }?.toString() ?: "Invalid"
            BasicText("Value: $result")
        }

        onNodeWithText("Value: 100").assertIsDisplayed()
        assertThat(dispatchedErrors).isEmpty()

        // Update the data model to a value that dynamically fails `dynamicNumber` coercion
        dataModel.update(A2uiDataPath("/base/num_val"), mapOf("a" to "b"))
        waitForIdle()

        onNodeWithText("Value: 100").assertIsNotDisplayed()
        onNodeWithText("Value: Invalid").assertIsDisplayed()
        assertThat(dispatchedErrors).hasSize(1)
        assertThat(dispatchedErrors.first().message).contains("Type mismatch")
    }

    @Test
    fun bind_reactivity_updatesWhenDeepDataChangesInDynamicValue() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/user/name"), "John")
        val props =
            A2uiComponentProperties(
                mapOf("data" to mapOf("nested" to mapOf("path" to "user/name")))
            )
        val propDef = A2uiProperty.dynamicValue("data")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val result = with(scope) { props.bind(propDef) } as? Map<*, *>
            BasicText("Name: ${result?.get("nested")}")
        }

        onNodeWithText("Name: John").assertIsDisplayed()

        dataModel.update(A2uiDataPath("/base/user/name"), "Jane")
        waitForIdle()

        onNodeWithText("Name: John").assertIsNotDisplayed()
        onNodeWithText("Name: Jane").assertIsDisplayed()
    }

    @Test
    fun bind_reactivity_recoversFromInvalidType() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/val"), mapOf("bad" to "type"))
        val props = A2uiComponentProperties(mapOf("number" to mapOf("path" to "val")))
        val propDef = A2uiProperty.dynamicNumber("number")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val result = with(scope) { props.bind(propDef) }?.toString() ?: "Invalid"
            BasicText("Value: $result")
        }

        onNodeWithText("Value: Invalid").assertIsDisplayed()
        assertThat(dispatchedErrors).hasSize(1)

        // Agent self-corrects the type
        dispatchedErrors.clear()
        dataModel.update(A2uiDataPath("/base/val"), 42)
        waitForIdle()

        onNodeWithText("Value: Invalid").assertIsNotDisplayed()
        onNodeWithText("Value: 42").assertIsDisplayed()
        assertThat(dispatchedErrors).isEmpty() // No new errors
    }

    @Test
    fun bindChildReferences_staticList_returnsReferences() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("children" to listOf("child_a", "child_b")))
        val propDef = A2uiProperty.childList("children")

        var result: List<A2uiComponentReference>? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bindChildReferences(propDef) }
        }
        waitForIdle()

        assertThat(result)
            .containsExactly(A2uiComponentReference("child_a"), A2uiComponentReference("child_b"))
            .inOrder()
    }

    @Test
    fun bindChildReferences_dynamicTemplate_returnsReferencesMappedToData() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/items"), listOf("x", "y", "z"))
        val props =
            A2uiComponentProperties(
                mapOf("children" to mapOf("path" to "items", "componentId" to "item_tmpl"))
            )
        val propDef = A2uiProperty.childList("children")

        var result: List<A2uiComponentReference>? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bindChildReferences(propDef) }
        }
        waitForIdle()

        assertThat(result)
            .containsExactly(
                A2uiComponentReference("item_tmpl", "items/0"),
                A2uiComponentReference("item_tmpl", "items/1"),
                A2uiComponentReference("item_tmpl", "items/2"),
            )
            .inOrder()
    }

    @Test
    fun bindChildReferences_dynamicTemplateWithEmptyList_returnsEmptyReferences() =
        runComposeUiTest {
            dataModel.update(A2uiDataPath("/base/empty_items"), emptyList<Any>())
            val props =
                A2uiComponentProperties(
                    mapOf(
                        "children" to mapOf("path" to "empty_items", "componentId" to "item_tmpl")
                    )
                )
            val propDef = A2uiProperty.childList("children")

            var result: List<A2uiComponentReference>? = null
            setContent {
                val scope = rememberComponentScope(path = "/base")
                result = with(scope) { props.bindChildReferences(propDef) }
            }
            waitForIdle()

            assertThat(result).isEmpty()
            assertThat(dispatchedErrors).isEmpty()
        }

    @Test
    fun bindChildReferences_trailingSlashInTemplatePath_formatsReferencesCorrectly() =
        runComposeUiTest {
            dataModel.update(A2uiDataPath("/base/items/"), listOf("A", "B"))
            val props =
                A2uiComponentProperties(
                    mapOf("children" to mapOf("path" to "items/", "componentId" to "tmpl"))
                )
            val propDef = A2uiProperty.childList("children")

            var result: List<A2uiComponentReference>? = null
            setContent {
                val scope = rememberComponentScope(path = "/base")
                result = with(scope) { props.bindChildReferences(propDef) }
            }
            waitForIdle()

            assertThat(result)
                .containsExactly(
                    A2uiComponentReference("tmpl", "items/0"),
                    A2uiComponentReference("tmpl", "items/1"),
                )
                .inOrder()
        }

    @Test
    fun bindChildReferences_sparseListData_generatesAllReferences() = runComposeUiTest {
        // Data model contains a sparse list of size 3 (index 1 is a gap/null)
        dataModel.update(A2uiDataPath("/base/sparse"), listOf("A"))
        dataModel.update(A2uiDataPath("/base/sparse/2"), "C")
        val props =
            A2uiComponentProperties(
                mapOf("children" to mapOf("path" to "sparse", "componentId" to "item_tmpl"))
            )
        val propDef = A2uiProperty.childList("children")

        var result: List<A2uiComponentReference>? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bindChildReferences(propDef) }
        }
        waitForIdle()

        assertThat(result)
            .containsExactly(
                A2uiComponentReference("item_tmpl", "sparse/0"),
                A2uiComponentReference("item_tmpl", "sparse/1"), // Points to the gap
                A2uiComponentReference("item_tmpl", "sparse/2"),
            )
            .inOrder()
    }

    @Test
    fun bindChildReferences_dataEvaluatingToNonList_returnsNullAndReportsError() =
        runComposeUiTest {
            // Agent hallucinates and points the list template to a String instead of an Array
            dataModel.update(A2uiDataPath("/base/not_a_list"), "Just a string")
            val props =
                A2uiComponentProperties(
                    mapOf("children" to mapOf("path" to "not_a_list", "componentId" to "tmpl"))
                )
            val propDef = A2uiProperty.childList("children")

            var result: List<A2uiComponentReference>? = null
            setContent {
                val scope = rememberComponentScope(path = "/base")
                result = with(scope) { props.bindChildReferences(propDef) }
            }
            waitForIdle()

            assertThat(result).isNull()
            assertThat(dispatchedErrors).hasSize(1)
            assertThat(dispatchedErrors.first().message).contains("Type mismatch")
        }

    @Test
    fun bindChildReferences_dataEvaluatingToNull_returnsNullWithoutError() = runComposeUiTest {
        // Data model does not yet contain "/base/missing_items"
        val props =
            A2uiComponentProperties(
                mapOf("children" to mapOf("path" to "missing_items", "componentId" to "item_tmpl"))
            )
        val propDef = A2uiProperty.childList("children")

        var result: List<A2uiComponentReference>? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bindChildReferences(propDef) }
        }
        waitForIdle()

        assertThat(result).isNull()
        assertThat(dispatchedErrors).isEmpty()
    }

    @Test
    fun bindChildReferences_missingProperty_returnsNull() = runComposeUiTest {
        val props = A2uiComponentProperties(emptyMap())
        val propDef = A2uiProperty.childList("children")

        var result: List<A2uiComponentReference>? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            result = with(scope) { props.bindChildReferences(propDef) }
        }
        waitForIdle()

        assertThat(result).isNull()
    }

    @Test
    fun bindChildReferences_reactive_updatesWhenListChanges() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/items"), listOf("A", "B"))
        val props =
            A2uiComponentProperties(
                mapOf("children" to mapOf("path" to "items", "componentId" to "item_tmpl"))
            )
        val propDef = A2uiProperty.childList("children")

        setContent {
            val scope = rememberComponentScope(path = "/base")
            val children = with(scope) { props.bindChildReferences(propDef) }
            BasicText("Child count: ${children?.size}")
        }

        onNodeWithText("Child count: 2").assertIsDisplayed()

        // Agent appends an item to the data array
        dataModel.update(A2uiDataPath("/base/items/-"), "C")
        waitForIdle()

        onNodeWithText("Child count: 3").assertIsDisplayed()

        // Agent truncates the data array
        dataModel.update(A2uiDataPath("/base/items"), listOf("Only One"))
        waitForIdle()

        onNodeWithText("Child count: 1").assertIsDisplayed()
        onNodeWithText("Child count: 2").assertIsNotDisplayed()
        onNodeWithText("Child count: 3").assertIsNotDisplayed()
    }

    @Test
    fun bindChildReferences_invalidTemplate_throws() = runComposeUiTest {
        // Malformed template lacking 'componentId', which should be caught by schema validator.
        val props = A2uiComponentProperties(mapOf("children" to mapOf("path" to "items")))
        val propDef = A2uiProperty.childList("children")

        assertThrows(IllegalStateException::class.java) {
            setContent {
                val scope = rememberComponentScope(path = "/base")
                with(scope) { props.bindChildReferences(propDef) }
            }
        }
    }

    @Test
    fun bindChildReferences_staticListWithInvalidItem_throws() = runComposeUiTest {
        // Malformed static list item, which should be caught by schema validator.
        val props = A2uiComponentProperties(mapOf("children" to listOf("child_a", 123, null)))
        val propDef = A2uiProperty.childList("children")

        assertThrows(IllegalStateException::class.java) {
            setContent {
                val scope = rememberComponentScope(path = "/base")
                with(scope) { props.bindChildReferences(propDef) }
            }
        }
    }

    @Test
    fun bindChildReferences_invalidPayloadType_throws() = runComposeUiTest {
        // Malformed children payload type, which should be caught by schema validator.
        val props = A2uiComponentProperties(mapOf("children" to "invalid_child_list_type"))
        val propDef = A2uiProperty.childList("children")

        assertThrows(IllegalStateException::class.java) {
            setContent {
                val scope = rememberComponentScope(path = "/base")
                with(scope) { props.bindChildReferences(propDef) }
            }
        }
    }

    @Test
    fun bindUpdater_writablePath_returnsUpdaterThatUpdatesDataModel() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "input_val")))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { props.bindUpdater(propDef) }
        }

        waitForIdle()
        assertThat(updater).isNotNull()

        updater?.invoke("New Input")
        waitForIdle()

        assertThat(dataModel[A2uiDataPath("/base/input_val")]).isEqualTo("New Input")
    }

    @Test
    fun bindUpdater_relativePathInNestedScope_updatesCorrectNestedPath() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "relative_val")))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/deeply/nested/scope")
            updater = with(scope) { props.bindUpdater(propDef) }
        }
        waitForIdle()

        updater?.invoke("Nested Update")
        waitForIdle()

        assertThat(dataModel[A2uiDataPath("/deeply/nested/scope/relative_val")])
            .isEqualTo("Nested Update")
    }

    @Test
    fun bindUpdater_invokedWithNull_removesDataFromModel() = runComposeUiTest {
        dataModel.update(A2uiDataPath("/base/input_val"), "Existing Value")
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "input_val")))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String?) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { props.bindUpdater(propDef) }
        }
        waitForIdle()

        updater?.invoke(null)
        waitForIdle()

        assertThat(dataModel[A2uiDataPath("/base/input_val")]).isNull()
    }

    @Test
    fun bindUpdater_literalValue_returnsNull() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("text" to "Read Only Value"))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { props.bindUpdater(propDef) }
        }
        waitForIdle()

        assertThat(updater).isNull()
    }

    @Test
    fun bindUpdater_absolutePathInNestedScope_updatesRootPath() = runComposeUiTest {
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to "/absolute_val")))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { props.bindUpdater(propDef) }
        }
        waitForIdle()

        updater?.invoke("Absolute Update")
        waitForIdle()

        assertThat(dataModel[A2uiDataPath("/absolute_val")]).isEqualTo("Absolute Update")
    }

    @Test
    fun bindUpdater_functionCall_returnsNull() = runComposeUiTest {
        // Agent hallucinates a two-way binding on a function call
        val props = A2uiComponentProperties(mapOf("text" to mapOf("call" to "formatString")))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { props.bindUpdater(propDef) }
        }
        waitForIdle()

        assertThat(updater).isNull()
    }

    @Test
    fun bindUpdater_nonStringPath_throws() = runComposeUiTest {
        // Malformed path, which should be caught the schema validator.
        val props = A2uiComponentProperties(mapOf("text" to mapOf("path" to 123)))
        val propDef = A2uiProperty.dynamicString("text")

        assertThrows(IllegalStateException::class.java) {
            setContent {
                val scope = rememberComponentScope(path = "/base")
                with(scope) { props.bindUpdater(propDef) }
            }
        }
    }

    @Test
    fun bindUpdater_calledWithDifferentPropertyKeys_returnsDifferentUpdaters() = runComposeUiTest {
        val props =
            A2uiComponentProperties(
                mapOf(
                    "text1" to mapOf("path" to "input_val_1"),
                    "text2" to mapOf("path" to "input_val_2"),
                )
            )
        val propDef1 = A2uiProperty.dynamicString("text1")
        val propDef2 = A2uiProperty.dynamicString("text2")

        // Hold the property definition in Compose State so we can swap it
        var currentPropDef by mutableStateOf(propDef1)
        var updater: ((String) -> Unit)? = null

        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { props.bindUpdater(currentPropDef) }
        }
        waitForIdle()

        // Write using the initial updater bound to "text1"
        updater?.invoke("First Update")
        waitForIdle()

        assertThat(dataModel[A2uiDataPath("/base/input_val_1")]).isEqualTo("First Update")
        assertThat(dataModel[A2uiDataPath("/base/input_val_2")]).isNull()

        // Swap the property being bound during a recomposition
        currentPropDef = propDef2
        waitForIdle()

        // Write using the new updater which should now be bound to "text2"
        updater?.invoke("Second Update")
        waitForIdle()

        assertThat(dataModel[A2uiDataPath("/base/input_val_1")]).isEqualTo("First Update")
        assertThat(dataModel[A2uiDataPath("/base/input_val_2")]).isEqualTo("Second Update")
    }

    @Test
    fun bindUpdater_unrelatedPropertyChanges_returnsSameUpdaterInstance() = runComposeUiTest {
        val textPayload = mapOf("path" to "input_val")
        var currentProps by
            mutableStateOf(
                A2uiComponentProperties(mapOf("text" to textPayload, "unrelated" to "A"))
            )
        val propDef = A2uiProperty.dynamicString("text")

        val updaters = mutableListOf<((String) -> Unit)?>()
        setContent {
            val scope = rememberComponentScope(path = "/base")
            val updater = with(scope) { currentProps.bindUpdater(propDef) }
            updaters.add(updater)
        }
        waitForIdle()

        // Update properties but keep text property payload the same
        currentProps = A2uiComponentProperties(mapOf("text" to textPayload, "unrelated" to "B"))
        waitForIdle()

        // Ensure that recomposition ran and the updater wasn't recreated
        assertThat(updaters).hasSize(2)
        assertThat(updaters[0]).isNotNull()
        assertThat(updaters[0]).isSameInstanceAs(updaters[1])
    }

    @Test
    fun bindUpdater_reactivity_transitionsBetweenReadOnlyAndWritable() = runComposeUiTest {
        // Represents the component's current properties, simulating structural agent updates
        var currentProps by
            mutableStateOf(A2uiComponentProperties(mapOf("text" to "Read Only Literal")))
        val propDef = A2uiProperty.dynamicString("text")

        var updater: ((String) -> Unit)? = null
        setContent {
            val scope = rememberComponentScope(path = "/base")
            updater = with(scope) { currentProps.bindUpdater(propDef) }
        }
        waitForIdle()

        assertThat(updater).isNull()

        // Agent updates component to use a writable data binding
        currentProps = A2uiComponentProperties(mapOf("text" to mapOf("path" to "input_val")))
        waitForIdle()

        assertThat(updater).isNotNull()

        // Verify the newly created updater works
        updater?.invoke("New Value")
        waitForIdle()
        assertThat(dataModel[A2uiDataPath("/base/input_val")]).isEqualTo("New Value")

        // Agent updates component back to a literal
        currentProps = A2uiComponentProperties(mapOf("text" to "Read Only Again"))
        waitForIdle()

        assertThat(updater).isNull()
    }

    @Test
    fun reportError_delegatesToSurface() = runComposeUiTest {
        setContent {
            val scope = rememberComponentScope(path = "/base")
            scope.reportError(A2uiException.A2uiValidationException("Manual error", "/base"))
        }

        waitForIdle()
        assertThat(dispatchedErrors).hasSize(1)
        assertThat(dispatchedErrors.first().message).isEqualTo("Manual error")
        assertThat(dispatchedErrors.first().context["path"]).isEqualTo("/base")
    }

    @Composable
    private fun rememberComponentScope(
        path: String,
        coroutineScope: CoroutineScope = rememberCoroutineScope(),
    ) =
        remember(path, coroutineScope) {
            A2uiComponentScopeImpl(
                id = "test_component_scope",
                baseDataPath = A2uiDataPath(path),
                surface = surface,
                surfaceScope = coroutineScope,
            )
        }

    private fun createCatalog(functions: List<A2uiFunction> = emptyList()) =
        object : A2uiCoreCatalog {
            override val id: String = "TestCatalog"
            override val componentDefinitions = emptyList<A2uiCoreComponentDefinition>()
            override val functions = emptyList<A2uiFunction>()
            override val themeSchema: A2uiSchema? = null

            override fun getComponentDefinition(name: String) = null

            override fun getFunction(name: String) = functions.find { it.definition.name == name }
        }

    private fun createSurface(catalog: A2uiCoreCatalog) =
        A2uiCoreSurfaceModel(
            id = "test_surface",
            catalog = catalog,
            dataModel = dataModel,
            componentRegistry = componentRegistry,
            onDispatchAction = { dispatchedActions.add(it) },
            onDispatchError = { dispatchedErrors.add(it) },
        )
}
