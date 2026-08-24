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
class A2uiBasicCatalogV1ColumnUiTest {

    private val testColumn =
        object : A2uiBasicCatalogV1.Column {
            var capturedChildren: List<A2uiComponentReference>? = null
            var capturedJustify: A2uiBasicCatalogV1.Column.Justify? = null
            var capturedAlign: A2uiBasicCatalogV1.Column.Align? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                children: List<A2uiComponentReference>,
                justify: A2uiBasicCatalogV1.Column.Justify,
                align: A2uiBasicCatalogV1.Column.Align,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedChildren = children
                    capturedJustify = justify
                    capturedAlign = align
                }
                val childIds = children.joinToString(",") { it.id }
                BasicText(text = "Column Children: $childIds", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testColumn),
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
                            type = "Column",
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

        onNodeWithText("Column Children: child_tmpl").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingData", listOf(mapOf("key" to "value")))
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Column Children: child_tmpl").assertIsDisplayed()
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
                            type = "Column",
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

        onNodeWithText("Column Children: child_tmpl").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/items", null)
        controller.waitForIdle()

        onNodeWithText("Column Children: child_tmpl").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
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
                            type = "Column",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Column Children: ").assertIsDisplayed()
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
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to listOf("child_1", "child_2"),
                                    "justify" to "center",
                                    "align" to "end",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Column Children: child_1,child_2").assertIsDisplayed()
        assertThat(testColumn.capturedChildren?.map { it.id })
            .containsExactly("child_1", "child_2")
            .inOrder()
        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.Center)
        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.End)
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
                            type = "Column",
                            properties =
                                mapOf(
                                    "children" to
                                        mapOf("path" to "/items", "componentId" to "child_tmpl"),
                                    "justify" to "spaceBetween",
                                    "align" to "center",
                                ),
                        )
                    ),
                initialData = mapOf("items" to listOf("a", "b", "c")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Column Children: child_tmpl,child_tmpl,child_tmpl").assertIsDisplayed()
        assertThat(testColumn.capturedChildren?.map { it.id })
            .containsExactly("child_tmpl", "child_tmpl", "child_tmpl")
            .inOrder()
        assertThat(testColumn.capturedChildren?.map { it.baseDataPath })
            .containsExactly("/items/0", "/items/1", "/items/2")
            .inOrder()
        assertThat(testColumn.capturedJustify)
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceBetween)
        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Center)

        controller.updateData("/items/-", "d")
        controller.waitForIdle()

        onNodeWithText("Column Children: child_tmpl,child_tmpl,child_tmpl,child_tmpl")
            .assertIsDisplayed()
        assertThat(testColumn.capturedChildren?.map { it.id })
            .containsExactly("child_tmpl", "child_tmpl", "child_tmpl", "child_tmpl")
            .inOrder()
        assertThat(testColumn.capturedChildren?.map { it.baseDataPath })
            .containsExactly("/items/0", "/items/1", "/items/2", "/items/3")
            .inOrder()
    }

    @Test
    fun content_justify_resolvesAllValuesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf("children" to emptyList<String>(), "justify" to "center"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.Center)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "end"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.End)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "spaceAround"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedJustify)
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceAround)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "spaceBetween"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedJustify)
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceBetween)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "spaceEvenly"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedJustify)
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceEvenly)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "start"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "stretch"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.Stretch)
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
                            type = "Column",
                            properties =
                                mapOf("children" to emptyList<String>(), "align" to "center"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }
        waitForIdle()

        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Center)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "end"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.End)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "start"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Start)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "stretch"),
        )
        controller.waitForIdle()
        waitForIdle()
        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Stretch)
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
                            type = "Column",
                            properties = mapOf("children" to listOf("child_1")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Column Children: child_1").assertIsDisplayed()
        assertThat(testColumn.capturedChildren?.map { it.id }).containsExactly("child_1")
        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)
        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Stretch)
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
                            type = "Column",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Column Children: ") and hasTestTag("custom_tag")).assertIsDisplayed()
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
                            type = "Column",
                            properties = mapOf("children" to listOf("old_child")),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Column Children: old_child").assertIsDisplayed()
        assertThat(testColumn.capturedChildren?.map { it.id }).containsExactly("old_child")

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to listOf("new_child_1", "new_child_2")),
        )
        controller.waitForIdle()

        onNodeWithText("Column Children: old_child").assertDoesNotExist()
        onNodeWithText("Column Children: new_child_1,new_child_2").assertIsDisplayed()
        assertThat(testColumn.capturedChildren?.map { it.id })
            .containsExactly("new_child_1", "new_child_2")
            .inOrder()
    }

    @Test
    fun content_justifyChange_recomposesWithNewJustify() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Column",
                            properties =
                                mapOf("children" to emptyList<String>(), "justify" to "start"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        assertThat(testColumn.capturedJustify).isEqualTo(A2uiBasicCatalogV1.Column.Justify.Start)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "justify" to "spaceBetween"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testColumn.capturedJustify)
            .isEqualTo(A2uiBasicCatalogV1.Column.Justify.SpaceBetween)
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
                            type = "Column",
                            properties =
                                mapOf("children" to emptyList<String>(), "align" to "stretch"),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Stretch)

        controller.updateComponent(
            id = "root",
            properties = mapOf("children" to emptyList<String>(), "align" to "center"),
        )
        controller.waitForIdle()
        waitForIdle()

        assertThat(testColumn.capturedAlign).isEqualTo(A2uiBasicCatalogV1.Column.Align.Center)
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
                            type = "Column",
                            properties = mapOf("children" to emptyList<String>()),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Column Children: ") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Column Children: ") and hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
