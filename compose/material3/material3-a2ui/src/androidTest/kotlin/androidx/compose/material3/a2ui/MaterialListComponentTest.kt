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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MaterialListComponentTest {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialListComponent, MaterialTextComponent),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    // ==========================================
    // Category 1: Direction Layout Verification
    // ==========================================

    @Test
    fun directionVertical_rendersChildrenInColumn() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf("children" to listOf("item_1", "item_2"), "direction" to "vertical"),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Item One"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Item Two"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        val bounds1 = onNodeWithText("Item One").getUnclippedBoundsInRoot()
        val bounds2 = onNodeWithText("Item Two").getUnclippedBoundsInRoot()

        assertThat(bounds2.top).isAtLeast(bounds1.bottom)
    }

    @Test
    fun directionHorizontal_rendersChildrenInRow() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf("children" to listOf("item_1", "item_2"), "direction" to "horizontal"),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Item One"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Item Two"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        val bounds1 = onNodeWithText("Item One").getUnclippedBoundsInRoot()
        val bounds2 = onNodeWithText("Item Two").getUnclippedBoundsInRoot()

        assertThat(bounds2.left).isAtLeast(bounds1.right)
    }

    @Test
    fun noDirectionSpecified_fallsBackToVertical() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1", "item_2")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Item One"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Item Two"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        val bounds1 = onNodeWithText("Item One").getUnclippedBoundsInRoot()
        val bounds2 = onNodeWithText("Item Two").getUnclippedBoundsInRoot()

        assertThat(bounds2.top).isAtLeast(bounds1.bottom)
    }

    // ==========================================
    // Category 2: Fallback Alignment
    // ==========================================

    @Test
    fun noAlignSpecified_fallsBackToStretch() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1"), "direction" to "vertical"),
            )
        val itemPayload =
            A2uiComponentPayload(id = "item_1", type = "Text", properties = mapOf("text" to "Item"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, itemPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(
                    surface = surface,
                    modifier = Modifier.testTag("custom_list_tag").width(200.dp),
                )
            }
        }

        val listBounds = onNodeWithTag("custom_list_tag").getUnclippedBoundsInRoot()
        val itemBounds = onNodeWithText("Item").getUnclippedBoundsInRoot()

        assertThat(itemBounds.right - itemBounds.left).isEqualTo(listBounds.right - listBounds.left)
    }

    // ==========================================
    // Category 3: Item Spacing (8.dp)
    // ==========================================

    @Test
    fun verticalList_applies8dpSpacingBetweenItems() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf("children" to listOf("item_1", "item_2"), "direction" to "vertical"),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Item One"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Item Two"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        val bounds1 = onNodeWithText("Item One").getUnclippedBoundsInRoot()
        val bounds2 = onNodeWithText("Item Two").getUnclippedBoundsInRoot()

        assertThat(bounds2.top - bounds1.bottom).isEqualTo(8.dp)
    }

    @Test
    fun horizontalList_applies8dpSpacingBetweenItems() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf("children" to listOf("item_1", "item_2"), "direction" to "horizontal"),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Item One"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Item Two"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        val bounds1 = onNodeWithText("Item One").getUnclippedBoundsInRoot()
        val bounds2 = onNodeWithText("Item Two").getUnclippedBoundsInRoot()

        assertThat(bounds2.left - bounds1.right).isEqualTo(8.dp)
    }

    // ==========================================
    // Category 4: Static vs Dynamic Children
    // ==========================================

    @Test
    fun staticChildren_rendersFixedListOfComponents() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1", "item_2")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "First Item"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Second Item"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("First Item").assertIsDisplayed()
        onNodeWithText("Second Item").assertIsDisplayed()
    }

    @Test
    fun dynamicChildren_generatesItemsFromDataModel() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf(
                        "children" to
                            mapOf("path" to "/user/messages", "componentId" to "message_item")
                    ),
            )
        val templatePayload =
            A2uiComponentPayload(
                id = "message_item",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, templatePayload),
                initialData = mapOf("user" to mapOf("messages" to listOf("Hello!", "Goodbye!"))),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Hello!").assertIsDisplayed()
        onNodeWithText("Goodbye!").assertIsDisplayed()
    }

    @Test
    fun dynamicChildren_updatesWhenItemsAddedOrRemoved() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf(
                        "children" to
                            mapOf("path" to "/user/messages", "componentId" to "message_item")
                    ),
            )
        val templatePayload =
            A2uiComponentPayload(
                id = "message_item",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, templatePayload),
                initialData = mapOf("user" to mapOf("messages" to listOf("Message 1"))),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Message 1").assertIsDisplayed()
        onNodeWithText("Message 2").assertDoesNotExist()

        controller.updateData("/user/messages", listOf("Message 1", "Message 2"))
        controller.waitForIdle()

        onNodeWithText("Message 1").assertIsDisplayed()
        onNodeWithText("Message 2").assertIsDisplayed()

        controller.updateData("/user/messages", emptyList<String>())
        controller.waitForIdle()

        onNodeWithText("Message 1").assertDoesNotExist()
        onNodeWithText("Message 2").assertDoesNotExist()
    }

    @Test
    fun dynamicChildren_rendersTemplatesWithGaps() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf(
                        "children" to mapOf("path" to "/user/items", "componentId" to "item_tmpl")
                    ),
            )
        val templatePayload =
            A2uiComponentPayload(
                id = "item_tmpl",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "title")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, templatePayload),
                initialData = mapOf("user" to mapOf("items" to listOf(mapOf("title" to "First")))),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("First").assertIsDisplayed()

        controller.updateData("/user/items/2", mapOf("title" to "Third"))
        controller.waitForIdle()

        onNodeWithText("First").assertIsDisplayed()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
        onNodeWithText("Third").assertIsDisplayed()
    }

    // =======================================================
    // Category 5: Per-Child Async Lifecycle (Loading, Error)
    // =======================================================

    @Test
    fun childInLoadingState_rendersLoadingIndicator() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/pending_text")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun horizontalList_childInLoadingState_rendersLoadingIndicator() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1"), "direction" to "horizontal"),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/pending_text")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromLoadingToSuccess_rendersChildComponent() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/pending_text")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        controller.updateData("/pending_text", "Resolved Text!")
        controller.waitForIdle()

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Resolved Text!").assertIsDisplayed()
    }

    @Test
    fun childInErrorState_rendersErrorFallback() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1", "item_2")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Healthy Item"),
            )
        val item2Payload =
            A2uiComponentPayload(
                id = "item_2",
                type = "Text",
                properties = mapOf("text" to "Crashing Item"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload, item2Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Healthy Item").assertIsDisplayed()

        controller.failComponent("item_2", A2uiRuntimeException("Fatal item crash"))
        controller.waitForIdle()

        onNodeWithText("Healthy Item").assertIsDisplayed()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromErrorToSuccess_displaysRecoveredChild() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val itemPayload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Recovered Item"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, itemPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("item_1", A2uiRuntimeException("Initial Failure"))
        controller.waitForIdle()

        onNodeWithText("Error").assertIsDisplayed()

        controller.updateComponent(
            id = "item_1",
            type = "Text",
            properties = mapOf("text" to "Recovered Item"),
        )
        controller.waitForIdle()

        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithText("Recovered Item").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromLoadingToError_displaysErrorState() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to mapOf("path" to "/pending_text")),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        controller.failComponent("item_1", A2uiRuntimeException("Fatal item crash during load"))
        controller.waitForIdle()

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromErrorToLoading_displaysLoadingIndicator() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val item1Payload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Initial Error Item"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, item1Payload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        controller.failComponent("item_1", A2uiRuntimeException("Initial Failure"))
        controller.waitForIdle()

        onNodeWithText("Error").assertIsDisplayed()

        controller.updateComponent(
            id = "item_1",
            properties = mapOf("text" to mapOf("path" to "/pending_text")),
        )
        controller.waitForIdle()

        onNodeWithText("Error").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromSuccessToLoading_hidesChild() = runComposeUiTest {
        val isReadyState = mutableStateOf(false)
        val stub =
            A2uiComponentStub.withId("stub_item", isReady = { isReadyState.value }) { _, modifier ->
                Text("Ready Child", modifier = modifier)
            }
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("stub_item")),
            )
        val stubPayload = A2uiComponentPayload(id = "stub_item")
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, stubPayload),
                componentStubs = listOf(stub),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        isReadyState.value = true
        controller.waitForIdle()

        onNodeWithText("Ready Child").assertIsDisplayed()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()

        isReadyState.value = false
        controller.waitForIdle()

        onNodeWithText("Ready Child").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun childTransitionsFromSuccessToError_displaysErrorState() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val itemPayload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Healthy Item"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, itemPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Healthy Item").assertIsDisplayed()

        controller.failComponent("item_1", A2uiRuntimeException("Runtime error during execution"))
        controller.waitForIdle()

        onNodeWithText("Healthy Item").assertDoesNotExist()
        onNodeWithText("Error").assertIsDisplayed()
    }

    // ==========================================
    // Category 6: Component Updates & Modifiers
    // ==========================================

    @Test
    fun childTypeChanges_switchesToNewComponentType() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("child_1")),
            )
        val childPayload =
            A2uiComponentPayload(
                id = "child_1",
                type = "Text",
                properties = mapOf("text" to "Initial Text"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, childPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Initial Text").assertIsDisplayed()

        controller.updateComponent(
            id = "child_1",
            type = "Text",
            properties = mapOf("text" to "Updated Text"),
        )
        controller.waitForIdle()

        onNodeWithText("Initial Text").assertDoesNotExist()
        onNodeWithText("Updated Text").assertIsDisplayed()
    }

    @Test
    fun passedModifier_appliedToRootList() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1")),
            )
        val itemPayload =
            A2uiComponentPayload(
                id = "item_1",
                type = "Text",
                properties = mapOf("text" to "Item Text"),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, itemPayload),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_list_tag"))
            }
        }

        onNodeWithTag("custom_list_tag").assertIsDisplayed()
    }

    @Test
    fun directionChanges_reinflatesListLayout() = runComposeUiTest {
        val listPayload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties = mapOf("children" to listOf("item_1"), "direction" to "vertical"),
            )
        val itemPayload =
            A2uiComponentPayload(id = "item_1", type = "Text", properties = mapOf("text" to "Item"))
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(listPayload, itemPayload),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Item").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to listOf("item_1"), "direction" to "horizontal"),
        )
        controller.waitForIdle()

        onNodeWithText("Item").assertIsDisplayed()
    }
}
