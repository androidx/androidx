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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1ColumnTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.column),
        )

    @Test
    fun children_fixedList_rendersAllChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1", "child2")),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Text("Child 1", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Text("Child 2", modifier = modifier)
                        },
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Child 1").assertIsDisplayed()
        onNodeWithText("Child 2").assertIsDisplayed()
    }

    @Test
    fun children_emptyList_rendersNoChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("column_tag"))
            }
        }

        onNode(hasTestTag("column_tag")).assertExists()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun children_dynamicTemplate_appendsNewItemOnDataUpdate() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf(
                                            "path" to "/catalog/products",
                                            "componentId" to "product_template",
                                        )
                                ),
                        ),
                        A2uiComponentPayload(
                            id = "product_template",
                            properties = mapOf("title" to mapOf("path" to "title")),
                        ),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("product_template") { props, modifier ->
                            val textProp = remember { A2uiProperty.dynamicString("title") }
                            val text = props.bind(textProp) ?: "Unknown"
                            Text(text = "Stubbed: $text", modifier = modifier)
                        }
                    ),
                initialData =
                    mapOf(
                        "catalog" to
                            mapOf(
                                "products" to
                                    listOf(mapOf("title" to "Camera"), mapOf("title" to "Laptop"))
                            )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Verify initial rendered templates
        onNodeWithText("Stubbed: Camera").assertIsDisplayed()
        onNodeWithText("Stubbed: Laptop").assertIsDisplayed()
        onNodeWithText("Stubbed: Tablet").assertDoesNotExist()

        // Simulate the agent appending a new item to the data model array
        controller.updateData("/catalog/products/-", mapOf("title" to "Tablet"))
        controller.waitForIdle()

        // Verify the Column dynamically instantiated a new child stub
        onNodeWithText("Stubbed: Tablet").assertIsDisplayed()
    }

    @Test
    fun children_dynamicTemplateWithGaps_rendersFallbacksForMissingIndices() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf(
                                            "path" to "/catalog/products",
                                            "componentId" to "product_template",
                                        )
                                ),
                        ),
                        A2uiComponentPayload(
                            id = "product_template",
                            properties = mapOf("title" to mapOf("path" to "title")),
                        ),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("product_template") { props, modifier ->
                            val textProp = remember { A2uiProperty.dynamicString("title") }
                            // Fallback to "Unknown" if the dynamically bound title is null
                            val text = props.bind(textProp) ?: "Unknown"
                            Text(text = "Stubbed: $text", modifier = modifier)
                        }
                    ),
                initialData =
                    mapOf("catalog" to mapOf("products" to listOf(mapOf("title" to "Camera")))),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Stubbed: Camera").assertIsDisplayed()

        // Agent hallucinates a gap, writing to index 2 (skipping index 1)
        controller.updateData("/catalog/products/2", mapOf("title" to "Tablet"))
        controller.waitForIdle()

        // Verify the UI doesn't crash
        // Index 0: Valid item
        onNodeWithText("Stubbed: Camera").assertIsDisplayed()
        // Index 1: The gap correctly evaluates child template bound properties as null
        onNodeWithText("Stubbed: Unknown").assertIsDisplayed()
        // Index 2: Valid hallucinated out-of-order item
        onNodeWithText("Stubbed: Tablet").assertIsDisplayed()
    }

    @Test
    fun children_dynamicTemplate_clearsChildrenWhenDataSetToNull() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf(
                                            "path" to "/catalog/products",
                                            "componentId" to "product_template",
                                        )
                                ),
                        ),
                        A2uiComponentPayload(
                            id = "product_template",
                            properties = mapOf("title" to mapOf("path" to "title")),
                        ),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("product_template") { props, modifier ->
                            val textProp = remember { A2uiProperty.dynamicString("title") }
                            val text = props.bind(textProp) ?: "Unknown"
                            Text(text = "Stubbed: $text", modifier = modifier)
                        }
                    ),
                initialData =
                    mapOf(
                        "catalog" to
                            mapOf(
                                "products" to
                                    listOf(mapOf("title" to "Camera"), mapOf("title" to "Laptop"))
                            )
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Stubbed: Camera").assertIsDisplayed()
        onNodeWithText("Stubbed: Laptop").assertIsDisplayed()

        controller.updateData("/catalog/products", null)
        controller.waitForIdle()

        onNodeWithText("Stubbed: Camera").assertDoesNotExist()
        onNodeWithText("Stubbed: Laptop").assertDoesNotExist()
    }

    @Test
    fun children_listChanges_rendersUpdatedChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child_1")),
                        ),
                        A2uiComponentPayload(id = "child_1"),
                        A2uiComponentPayload(id = "child_2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Child One", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_2") { _, modifier ->
                            Text("Child Two", modifier = modifier)
                        },
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Child One").assertIsDisplayed()

        controller.updateComponent(id = "root", properties = mapOf("children" to listOf("child_2")))
        controller.waitForIdle()

        onNodeWithText("Child One").assertDoesNotExist()
        onNodeWithText("Child Two").assertIsDisplayed()
    }

    @Test
    fun children_differentStates_rendersLoadingErrorAndSuccess() = runComposeUiTest {
        val loadingStub =
            A2uiComponentStub.withId("loading_child", isReady = { false }) { _, modifier ->
                Text("Loading Child", modifier = modifier)
            }
        val errorStub =
            A2uiComponentStub.withId("error_child") { _, modifier ->
                Text("Error Child", modifier = modifier)
            }
        val successStub =
            A2uiComponentStub.withId("success_child") { _, modifier ->
                Text("Success Child", modifier = modifier)
            }

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to
                                        listOf("loading_child", "error_child", "success_child")
                                ),
                        ),
                        A2uiComponentPayload(id = "loading_child"),
                        A2uiComponentPayload(id = "error_child"),
                        A2uiComponentPayload(id = "success_child"),
                    ),
                componentStubs = listOf(loadingStub, errorStub, successStub),
            )

        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("error_child", A2uiRuntimeException("Child error occurred"))
        controller.waitForIdle()

        onAllNodesWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG)[0].assertIsDisplayed()
        onNodeWithText("Error").assertIsDisplayed()
        onNodeWithText("Success Child").assertIsDisplayed()
    }

    @Test
    fun child_fails_rendersErrorFallback() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("failing_child")),
                        ),
                        A2uiComponentPayload(id = "failing_child"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("failing_child") { _, modifier ->
                            Text("Child Text", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("failing_child", A2uiRuntimeException("Failed to load child"))
        controller.waitForIdle()

        onNodeWithText("Child Text").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun child_typeChanges_rendersNewComponentType() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child_1")),
                        ),
                        A2uiComponentPayload(
                            id = "child_1",
                            type = "Progress",
                            properties = emptyMap(),
                        ),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withType("Progress") { _, modifier ->
                            CircularProgressIndicator(modifier = modifier.testTag("progress"))
                        },
                        A2uiComponentStub.withType("Text") { _, modifier ->
                            Text("Text Content", modifier = modifier)
                        },
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag("progress").assertIsDisplayed()

        controller.updateComponent(id = "child_1", type = "Text", properties = emptyMap())
        controller.waitForIdle()

        onNodeWithTag("progress").assertDoesNotExist()
        onNodeWithText("Text Content").assertIsDisplayed()
    }

    @Test
    fun transition_loadingToSuccess_displaysChild() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId(id = "delayed_text_id", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Submit", modifier = modifier)
            }

        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("delayed_text_id")),
                        ),
                        A2uiComponentPayload(id = "delayed_text_id"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Submit").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        isReadyState.value = true
        waitForIdle()
        controller.waitForIdle()

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Submit").assertIsDisplayed()
    }

    @Test
    fun transition_loadingToError_displaysError() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("delayed_child", isReady = { isReadyState.value }) {
                _,
                modifier ->
                Text("Child", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("delayed_child")),
                        ),
                        A2uiComponentPayload(id = "delayed_child"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Child").assertDoesNotExist()
        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        controller.failComponent("delayed_child", A2uiRuntimeException("Failed to load"))
        controller.waitForIdle()

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Child").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun transition_errorToLoading_displaysLoading() = runComposeUiTest {
        val isReadyState = mutableStateOf(true)
        val stub =
            A2uiComponentStub.withId("child_id", isReady = { isReadyState.value }) { _, modifier ->
                Text("Recovered Child", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child_id")),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("child_id", A2uiRuntimeException("Initial Error"))
        controller.waitForIdle()

        onNodeWithText("Error").assertIsDisplayed()

        isReadyState.value = false
        controller.updateComponent(id = "child_id", properties = emptyMap())
        controller.waitForIdle()

        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun transition_errorToSuccess_displaysRecoveredChild() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child_id")),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_id") { _, modifier ->
                            Text("Recovered Child", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("child_id", A2uiRuntimeException("Initial Error"))
        controller.waitForIdle()

        onNodeWithText("Recovered Child").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()

        controller.updateComponent(id = "child_id", properties = emptyMap())
        controller.waitForIdle()

        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithText("Recovered Child").assertIsDisplayed()
    }

    @Test
    fun transition_successToLoading_displaysLoading() = runComposeUiTest {
        val isReadyState = mutableStateOf(true)
        val stub =
            A2uiComponentStub.withId("child_id", isReady = { isReadyState.value }) { _, modifier ->
                Text("Dynamic Child", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child_id")),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }
        controller.waitForIdle()

        onNodeWithText("Dynamic Child").assertIsDisplayed()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()

        isReadyState.value = false
        waitForIdle()
        controller.waitForIdle()

        onNodeWithText("Dynamic Child").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun transition_successToError_displaysError() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child_id")),
                        ),
                        A2uiComponentPayload(id = "child_id"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_id") { _, modifier ->
                            Text("Healthy Child", modifier = modifier)
                        }
                    ),
            )

        val surface = controller.start()
        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Healthy Child").assertIsDisplayed()
        onNodeWithText("Error").assertDoesNotExist()

        controller.failComponent("child_id", A2uiRuntimeException("Runtime Failure"))
        controller.waitForIdle()

        onNodeWithText("Healthy Child").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun modifier_parentModifier_isAppliedInAllChildStates() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("stub_child", isReady = { isReadyState.value }) { _, modifier
                ->
                Text("Ready Content", modifier = modifier)
            }
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("stub_child")),
                        ),
                        A2uiComponentPayload(id = "stub_child"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("column_tag"))
            }
        }

        // Test tag applied in Loading state
        onNode(hasTestTag("column_tag")).assertIsDisplayed()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        // Transition to Success state
        isReadyState.value = true
        waitForIdle()
        controller.waitForIdle()
        onNode(hasTestTag("column_tag")).assertIsDisplayed()

        // Transition to Error state
        controller.failComponent("stub_child", A2uiRuntimeException("Failure"))
        controller.waitForIdle()
        onNode(hasTestTag("column_tag")).assertIsDisplayed()
    }

    @Test
    fun justify_start_arrangesChildrenAtTop() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "start",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        assertThat(child1Bounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2Bounds.top.value)
            .isWithin(0.5f)
            .of((child1Bounds.bottom + MaterialA2uiBasicCatalogV1Column.ItemSpacing).value)
    }

    @Test
    fun justify_end_arrangesChildrenAtBottom() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf("children" to listOf("child1", "child2"), "justify" to "end"),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        assertThat(child2Bounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child1Bounds.bottom.value)
            .isWithin(0.5f)
            .of((child2Bounds.top - MaterialA2uiBasicCatalogV1Column.ItemSpacing).value)
    }

    @Test
    fun justify_center_arrangesChildrenInCenter() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "center",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val childrenCenter = (child1Bounds.top + child2Bounds.bottom) / 2
        val columnCenter = (columnBounds.top + columnBounds.bottom) / 2
        assertThat(childrenCenter.value).isWithin(0.5f).of(columnCenter.value)
        assertThat(child1Bounds.top).isGreaterThan(columnBounds.top)
        assertThat(child2Bounds.bottom).isLessThan(columnBounds.bottom)
    }

    @Test
    fun justify_spaceBetween_spacesChildrenBetween() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "spaceBetween",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        assertThat(child1Bounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2Bounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child2Bounds.top).isGreaterThan(child1Bounds.bottom)
    }

    @Test
    fun justify_spaceAround_spacesChildrenAround() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "spaceAround",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val spaceBefore = child1Bounds.top - columnBounds.top
        val spaceBetween = child2Bounds.top - child1Bounds.bottom
        val spaceAfter = columnBounds.bottom - child2Bounds.bottom

        assertThat(spaceBefore.value).isWithin(0.5f).of(30f)
        assertThat(spaceBetween.value).isWithin(0.5f).of(60f)
        assertThat(spaceAfter.value).isWithin(0.5f).of(30f)
    }

    @Test
    fun justify_spaceEvenly_spacesChildrenEvenly() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "spaceEvenly",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val spaceBefore = child1Bounds.top - columnBounds.top
        val spaceBetween = child2Bounds.top - child1Bounds.bottom
        val spaceAfter = columnBounds.bottom - child2Bounds.bottom

        assertThat(spaceBefore.value).isWithin(0.5f).of(40f)
        assertThat(spaceBetween.value).isWithin(0.5f).of(40f)
        assertThat(spaceAfter.value).isWithin(0.5f).of(40f)
    }

    @Test
    fun justify_stretch_stretchesChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "stretch",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child2")
                                        .sizeIn(minWidth = 50.dp, minHeight = 40.dp)
                            )
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val expectedChildHeight = (200.dp - MaterialA2uiBasicCatalogV1Column.ItemSpacing) / 2

        assertThat(child1Bounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2Bounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child1Bounds.height.value).isWithin(0.5f).of(expectedChildHeight.value)
        assertThat(child2Bounds.height.value).isWithin(0.5f).of(expectedChildHeight.value)
    }

    @Test
    fun align_start_alignsChildrenAtStart() = runComposeUiTest {
        val childWidth = 50.dp
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1"), "align" to "start"),
                        ),
                        A2uiComponentPayload(id = "child1"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = childWidth, minHeight = 40.dp)
                            )
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(200.dp).height(100.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()

        assertThat(child1Bounds.left.value).isWithin(0.5f).of(columnBounds.left.value)
        assertThat(child1Bounds.width.value).isWithin(0.5f).of(childWidth.value)
    }

    @Test
    fun align_center_alignsChildrenAtCenterHorizontally() = runComposeUiTest {
        val childWidth = 50.dp
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1"), "align" to "center"),
                        ),
                        A2uiComponentPayload(id = "child1"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = childWidth, minHeight = 40.dp)
                            )
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(200.dp).height(100.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()

        val childCenter = (child1Bounds.left + child1Bounds.right) / 2
        val columnCenter = (columnBounds.left + columnBounds.right) / 2
        assertThat(childCenter.value).isWithin(0.5f).of(columnCenter.value)
        assertThat(child1Bounds.width.value).isWithin(0.5f).of(childWidth.value)
    }

    @Test
    fun align_end_alignsChildrenAtEnd() = runComposeUiTest {
        val childWidth = 50.dp
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1"), "align" to "end"),
                        ),
                        A2uiComponentPayload(id = "child1"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = childWidth, minHeight = 40.dp)
                            )
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(200.dp).height(100.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()

        assertThat(child1Bounds.right.value).isWithin(0.5f).of(columnBounds.right.value)
        assertThat(child1Bounds.width.value).isWithin(0.5f).of(childWidth.value)
    }

    @Test
    fun align_stretch_stretchesChildrenHorizontally() = runComposeUiTest {
        val childWidth = 50.dp
        val columnWidth = 200.dp
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf("children" to listOf("child1"), "align" to "stretch"),
                        ),
                        A2uiComponentPayload(id = "child1"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier
                                        .testTag("child1")
                                        .sizeIn(minWidth = childWidth, minHeight = 40.dp)
                            )
                        }
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(columnWidth),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()

        assertThat(child1Bounds.width).isEqualTo(columnBounds.width)
    }

    @Test
    fun children_withWeights_distributeHeightProportionally() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1", "child2")),
                        ),
                        A2uiComponentPayload(id = "child1", properties = mapOf("weight" to 1)),
                        A2uiComponentPayload(id = "child2", properties = mapOf("weight" to 3)),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(modifier = modifier.testTag("child1").width(50.dp))
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(modifier = modifier.testTag("child2").width(50.dp))
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val totalAvailableHeight = 200.dp - MaterialA2uiBasicCatalogV1Column.ItemSpacing
        val expectedChild1Height = totalAvailableHeight * (1f / 4f)
        val expectedChild2Height = totalAvailableHeight * (3f / 4f)

        assertThat(child1Bounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2Bounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child1Bounds.height.value).isWithin(0.5f).of(expectedChild1Height.value)
        assertThat(child2Bounds.height.value).isWithin(0.5f).of(expectedChild2Height.value)
    }

    @Test
    fun children_mixedWeightAndFixedHeight_occupiesRemainingHeight() = runComposeUiTest {
        val fixedChildHeight = 50.dp
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1", "child2")),
                        ),
                        A2uiComponentPayload(id = "child1"),
                        A2uiComponentPayload(id = "child2", properties = mapOf("weight" to 1)),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(
                                modifier =
                                    modifier.testTag("child1").width(50.dp).height(fixedChildHeight)
                            )
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(modifier = modifier.testTag("child2").width(50.dp))
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val expectedChild2Height =
            200.dp - fixedChildHeight - MaterialA2uiBasicCatalogV1Column.ItemSpacing

        assertThat(child1Bounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child1Bounds.height.value).isWithin(0.5f).of(fixedChildHeight.value)
        assertThat(child2Bounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child2Bounds.height.value).isWithin(0.5f).of(expectedChild2Height.value)
    }

    @Test
    fun justify_stretch_withCustomWeights_distributesHeightProportionally() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child1", "child2"),
                                    "justify" to "stretch",
                                ),
                        ),
                        A2uiComponentPayload(id = "child1", properties = mapOf("weight" to 1)),
                        A2uiComponentPayload(id = "child2", properties = mapOf("weight" to 3)),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(modifier = modifier.testTag("child1").width(50.dp))
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(modifier = modifier.testTag("child2").width(50.dp))
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val child1Bounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2Bounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val totalAvailableHeight = 200.dp - MaterialA2uiBasicCatalogV1Column.ItemSpacing
        val expectedChild1Height = totalAvailableHeight * (1f / 4f)
        val expectedChild2Height = totalAvailableHeight * (3f / 4f)

        assertThat(child1Bounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2Bounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child1Bounds.height.value).isWithin(0.5f).of(expectedChild1Height.value)
        assertThat(child2Bounds.height.value).isWithin(0.5f).of(expectedChild2Height.value)
    }

    @Test
    fun children_weightsUpdated_recomposesAndUpdatesLayout() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties = mapOf("children" to listOf("child1", "child2")),
                        ),
                        A2uiComponentPayload(id = "child1", properties = mapOf("weight" to 1)),
                        A2uiComponentPayload(id = "child2", properties = mapOf("weight" to 3)),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child1") { _, modifier ->
                            Box(modifier = modifier.testTag("child1").width(50.dp))
                        },
                        A2uiComponentStub.withId("child2") { _, modifier ->
                            Box(modifier = modifier.testTag("child2").width(50.dp))
                        },
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("column_tag").width(100.dp).height(200.dp),
                )
            }
        }

        val columnBounds = onNodeWithTag("column_tag").getUnclippedBoundsInRoot()
        val totalAvailableHeight = 200.dp - MaterialA2uiBasicCatalogV1Column.ItemSpacing

        val child1InitialBounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2InitialBounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val expectedChild1InitialHeight = totalAvailableHeight * (1f / 4f)
        val expectedChild2InitialHeight = totalAvailableHeight * (3f / 4f)

        assertThat(child1InitialBounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2InitialBounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child1InitialBounds.height.value)
            .isWithin(0.5f)
            .of(expectedChild1InitialHeight.value)
        assertThat(child2InitialBounds.height.value)
            .isWithin(0.5f)
            .of(expectedChild2InitialHeight.value)

        controller.updateComponent(id = "child1", properties = mapOf("weight" to 3))
        controller.updateComponent(id = "child2", properties = mapOf("weight" to 1))
        controller.waitForIdle()

        val child1UpdatedBounds = onNodeWithTag("child1").getUnclippedBoundsInRoot()
        val child2UpdatedBounds = onNodeWithTag("child2").getUnclippedBoundsInRoot()

        val expectedChild1UpdatedHeight = totalAvailableHeight * (3f / 4f)
        val expectedChild2UpdatedHeight = totalAvailableHeight * (1f / 4f)

        assertThat(child1UpdatedBounds.top.value).isWithin(0.5f).of(columnBounds.top.value)
        assertThat(child2UpdatedBounds.bottom.value).isWithin(0.5f).of(columnBounds.bottom.value)
        assertThat(child1UpdatedBounds.height.value)
            .isWithin(0.5f)
            .of(expectedChild1UpdatedHeight.value)
        assertThat(child2UpdatedBounds.height.value)
            .isWithin(0.5f)
            .of(expectedChild2UpdatedHeight.value)
    }
}
