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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1ListUiTest {

    private val testList =
        object : A2uiBasicCatalogV1.List {
            var capturedChildren: List<A2uiComponentReference>? = null
            var capturedDirection: A2uiBasicCatalogV1.List.Direction? = null
            var capturedAlign: A2uiBasicCatalogV1.List.Align? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                children: List<A2uiComponentReference>,
                direction: A2uiBasicCatalogV1.List.Direction,
                align: A2uiBasicCatalogV1.List.Align,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedChildren = children
                    capturedDirection = direction
                    capturedAlign = align
                }
                val childIds = children.joinToString(",") { it.id }
                BasicText(text = "List Children: $childIds", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testList),
            functions = emptyList(),
        )

    @Test
    fun isReady_pendingDynamicData_returnsFalseAndGuardsContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf(
                                            "path" to "/pendingData",
                                            "componentId" to "child_tmpl",
                                        )
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("List Children: child_tmpl").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingData", listOf(mapOf("key" to "value")))
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("List Children: child_tmpl").assertIsDisplayed()
    }

    @Test
    fun isReady_dynamicDataErased_transitionsFromReadyToPending() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf("path" to "/items", "componentId" to "child_tmpl")
                                ),
                        )
                    ),
                initialData = mapOf("items" to listOf("Item")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("List Children: child_tmpl").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/items", null)
        controller.waitForIdle()

        onNodeWithText("List Children: child_tmpl").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_invalidDynamicDataType_returnsFalseAndReportsError() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "List",
                properties =
                    mapOf(
                        "children" to mapOf("path" to "/user/items", "componentId" to "child_tmpl")
                    ),
            )
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents = listOf(payload),
                initialData = mapOf("user" to mapOf("items" to 12345)),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
                onError = { _, _ -> },
            )
        }

        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        assertThat(testList.capturedChildren).isNull()
        val error = controller.outboundErrors.single()
        assertThat(error.message)
            .contains("Type mismatch for child template 'children' in component 'root'")
        assertThat(error.context["path"]).isEqualTo("children")
    }

    @Test
    fun isReady_emptyStaticList_returnsTrueAndRendersContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: ").assertIsDisplayed()
    }

    @Test
    fun content_staticData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf(
                                    "children" to listOf("child_1", "child_2"),
                                    "direction" to "horizontal",
                                    "align" to "end",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: child_1,child_2").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("child_1", "child_2")
            .inOrder()
        assertThat(testList.capturedDirection)
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Horizontal)
        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.End)
    }

    @Test
    fun content_dynamicData_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf("path" to "/items", "componentId" to "child_tmpl"),
                                    "direction" to "vertical",
                                    "align" to "center",
                                ),
                        )
                    ),
                initialData = mapOf("items" to listOf("a", "b", "c")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: child_tmpl,child_tmpl,child_tmpl").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("child_tmpl", "child_tmpl", "child_tmpl")
            .inOrder()
        assertThat(testList.capturedChildren?.map { it.baseDataPath })
            .containsExactly("/items/0", "/items/1", "/items/2")
            .inOrder()
        assertThat(testList.capturedDirection).isEqualTo(A2uiBasicCatalogV1.List.Direction.Vertical)
        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Center)

        controller.updateData("/items/-", "d")
        controller.waitForIdle()

        onNodeWithText("List Children: child_tmpl,child_tmpl,child_tmpl,child_tmpl")
            .assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("child_tmpl", "child_tmpl", "child_tmpl", "child_tmpl")
            .inOrder()
        assertThat(testList.capturedChildren?.map { it.baseDataPath })
            .containsExactly("/items/0", "/items/1", "/items/2", "/items/3")
            .inOrder()
    }

    @Test
    fun content_direction_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf("children" to emptyList<String>(), "direction" to "vertical"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testList.capturedDirection).isEqualTo(A2uiBasicCatalogV1.List.Direction.Vertical)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "direction" to "horizontal"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testList.capturedDirection)
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Horizontal)
    }

    @Test
    fun content_align_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf("children" to emptyList<String>(), "align" to "stretch"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Stretch)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "center"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Center)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "end"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.End)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "start"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Start)
    }

    @Test
    fun content_omittedOptionalProperties_fallsBackToDefaults() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties = mapOf("children" to listOf("child_1")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: child_1").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id }).containsExactly("child_1")
        assertThat(testList.capturedDirection).isEqualTo(A2uiBasicCatalogV1.List.Direction.Vertical)
        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Stretch)
    }

    @Test
    fun content_passedModifier_appliesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("List Children: ") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_childrenChange_recomposesWithNewChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties = mapOf("children" to listOf("old_child")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: old_child").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id }).containsExactly("old_child")

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to listOf("new_child_1", "new_child_2")),
        )
        controller.waitForIdle()

        onNodeWithText("List Children: old_child").assertDoesNotExist()
        onNodeWithText("List Children: new_child_1,new_child_2").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("new_child_1", "new_child_2")
            .inOrder()
    }

    @Test
    fun content_staticToDynamicChildrenChange_recomposesWithNewChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties = mapOf("children" to listOf("child_1", "child_2")),
                        )
                    ),
                initialData = mapOf("items" to listOf("a", "b", "c")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: child_1,child_2").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("child_1", "child_2")
            .inOrder()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf("children" to mapOf("path" to "/items", "componentId" to "child_tmpl")),
        )
        controller.waitForIdle()

        onNodeWithText("List Children: child_1,child_2").assertDoesNotExist()
        onNodeWithText("List Children: child_tmpl,child_tmpl,child_tmpl").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("child_tmpl", "child_tmpl", "child_tmpl")
            .inOrder()
        assertThat(testList.capturedChildren?.map { it.baseDataPath })
            .containsExactly("/items/0", "/items/1", "/items/2")
            .inOrder()
    }

    @Test
    fun content_dynamicDataListCleared_recomposesWithEmptyChildren() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf("path" to "/items", "componentId" to "child_tmpl")
                                ),
                        )
                    ),
                initialData = mapOf("items" to listOf("a", "b")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("List Children: child_tmpl,child_tmpl").assertIsDisplayed()
        assertThat(testList.capturedChildren?.map { it.id })
            .containsExactly("child_tmpl", "child_tmpl")
            .inOrder()

        controller.updateData("/items", emptyList<String>())
        controller.waitForIdle()

        onNodeWithText("List Children: child_tmpl,child_tmpl").assertDoesNotExist()
        onNodeWithText("List Children: ").assertIsDisplayed()
        assertThat(testList.capturedChildren).isEmpty()
    }

    @Test
    fun content_directionChange_recomposesWithNewDirection() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf("children" to emptyList<String>(), "direction" to "vertical"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        assertThat(testList.capturedDirection).isEqualTo(A2uiBasicCatalogV1.List.Direction.Vertical)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "direction" to "horizontal"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testList.capturedDirection)
            .isEqualTo(A2uiBasicCatalogV1.List.Direction.Horizontal)
    }

    @Test
    fun content_alignChange_recomposesWithNewAlign() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties =
                                mapOf("children" to emptyList<String>(), "align" to "stretch"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Stretch)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "center"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testList.capturedAlign).isEqualTo(A2uiBasicCatalogV1.List.Align.Center)
    }

    @Test
    fun content_modifierChange_recomposesWithNewModifier() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "List",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("List Children: ") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("List Children: ") and hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
