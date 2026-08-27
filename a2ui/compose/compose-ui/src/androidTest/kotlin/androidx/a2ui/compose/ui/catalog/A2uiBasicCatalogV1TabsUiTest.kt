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

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
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
class A2uiBasicCatalogV1TabsUiTest {

    private val testTabs =
        object : A2uiBasicCatalogV1.Tabs {
            var capturedTabs: List<A2uiBasicCatalogV1.Tabs.Tab>? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
                modifier: Modifier,
            ) {
                SideEffect { capturedTabs = tabs }
                val titles = tabs.joinToString(",") { it.title }
                val children = tabs.joinToString(",") { it.childId }
                BasicText(text = "Tabs: $titles - $children", modifier = modifier)
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testTabs),
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf(
                                                "title" to mapOf("path" to "/pendingData"),
                                                "child" to "child_1",
                                            )
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

        onNodeWithText("Tabs: Data Arrived - child_1").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/pendingData", "Data Arrived")
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Tabs: Data Arrived - child_1").assertIsDisplayed()
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf(
                                                "title" to mapOf("path" to "/title"),
                                                "child" to "child_1",
                                            )
                                        )
                                ),
                        )
                    ),
                initialData = mapOf("title" to "Tab Title"),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Tabs: Tab Title - child_1").assertIsDisplayed()
        onNodeWithText("Loading...").assertDoesNotExist()

        controller.updateData("/title", null)
        controller.waitForIdle()

        onNodeWithText("Tabs: Tab Title - child_1").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun isReady_partialDynamicData_returnsFalseUntilAllTabTitlesArrive() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf(
                                                "title" to mapOf("path" to "/tabs/tab1"),
                                                "child" to "child_1",
                                            ),
                                            mapOf(
                                                "title" to mapOf("path" to "/tabs/tab2"),
                                                "child" to "child_2",
                                            ),
                                        )
                                ),
                        )
                    ),
                initialData = mapOf("tabs" to mapOf("tab1" to "First Tab")),
            )
        val surface = controller.start()

        setContent {
            A2uiTestSurface(
                surface = surface,
                onLoading = { modifier -> BasicText("Loading...", modifier = modifier) },
            )
        }

        onNodeWithText("Tabs: First Tab,Second Tab - child_1,child_2").assertDoesNotExist()
        onNodeWithText("Loading...").assertIsDisplayed()

        controller.updateData("/tabs/tab2", "Second Tab")
        controller.waitForIdle()

        onNodeWithText("Loading...").assertDoesNotExist()
        onNodeWithText("Tabs: First Tab,Second Tab - child_1,child_2").assertIsDisplayed()
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf("title" to "Tab 1", "child" to "child_1"),
                                            mapOf("title" to "Tab 2", "child" to "child_2"),
                                        )
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Tabs: Tab 1,Tab 2 - child_1,child_2").assertIsDisplayed()
        assertThat(testTabs.capturedTabs)
            .containsExactly(
                A2uiBasicCatalogV1.Tabs.Tab("Tab 1", "child_1"),
                A2uiBasicCatalogV1.Tabs.Tab("Tab 2", "child_2"),
            )
            .inOrder()
    }

    @Test
    fun content_emptyStaticTitle_returnsTrueAndPassesEmptyStringToTypedContent() =
        runComposeUiTest {
            val controller =
                A2uiTestController(
                    catalog = testCatalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "Tabs",
                                properties =
                                    mapOf(
                                        "tabs" to listOf(mapOf("title" to "", "child" to "child_1"))
                                    ),
                            )
                        ),
                )
            val surface = controller.start()

            setContent { A2uiTestSurface(surface) }

            onNodeWithText("Tabs:  - child_1").assertIsDisplayed()
            assertThat(testTabs.capturedTabs)
                .containsExactly(A2uiBasicCatalogV1.Tabs.Tab("", "child_1"))
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf(
                                                "title" to mapOf("path" to "/tabs/first"),
                                                "child" to "child_1",
                                            ),
                                            mapOf(
                                                "title" to mapOf("path" to "/tabs/second"),
                                                "child" to "child_2",
                                            ),
                                        )
                                ),
                        )
                    ),
                initialData =
                    mapOf("tabs" to mapOf("first" to "Dynamic 1", "second" to "Dynamic 2")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Tabs: Dynamic 1,Dynamic 2 - child_1,child_2").assertIsDisplayed()
        assertThat(testTabs.capturedTabs)
            .containsExactly(
                A2uiBasicCatalogV1.Tabs.Tab("Dynamic 1", "child_1"),
                A2uiBasicCatalogV1.Tabs.Tab("Dynamic 2", "child_2"),
            )
            .inOrder()

        controller.updateData("/tabs/first", "Updated 1")
        controller.waitForIdle()

        onNodeWithText("Tabs: Updated 1,Dynamic 2 - child_1,child_2").assertIsDisplayed()
        assertThat(testTabs.capturedTabs)
            .containsExactly(
                A2uiBasicCatalogV1.Tabs.Tab("Updated 1", "child_1"),
                A2uiBasicCatalogV1.Tabs.Tab("Dynamic 2", "child_2"),
            )
            .inOrder()
    }

    @Test
    fun content_mixedStaticAndDynamicTabs_resolvesAllProperties() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf("title" to "Static Tab", "child" to "child_1"),
                                            mapOf(
                                                "title" to mapOf("path" to "/dynamicTitle"),
                                                "child" to "child_2",
                                            ),
                                        )
                                ),
                        )
                    ),
                initialData = mapOf("dynamicTitle" to "Dynamic Tab"),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Tabs: Static Tab,Dynamic Tab - child_1,child_2").assertIsDisplayed()
        assertThat(testTabs.capturedTabs)
            .containsExactly(
                A2uiBasicCatalogV1.Tabs.Tab("Static Tab", "child_1"),
                A2uiBasicCatalogV1.Tabs.Tab("Dynamic Tab", "child_2"),
            )
            .inOrder()
    }

    @Test
    fun content_functionExpression_evaluatesAndPassesToTypedContent() = runComposeUiTest {
        val catalogWithFunctions =
            A2uiCatalog(
                catalogId = "test_catalog",
                components = listOf(testTabs),
                functions = listOf(A2uiFormatStringFunction.INSTANCE),
            )
        val controller =
            A2uiTestController(
                catalog = catalogWithFunctions,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf(
                                                "title" to
                                                    mapOf(
                                                        "call" to "formatString",
                                                        "args" to
                                                            mapOf(
                                                                "value" to
                                                                    "Tab \${/tab/num}: \${/tab/name}"
                                                            ),
                                                    ),
                                                "child" to "child_1",
                                            )
                                        )
                                ),
                        )
                    ),
                initialData = mapOf("tab" to mapOf("num" to "1", "name" to "Overview")),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Tabs: Tab 1: Overview - child_1").assertIsDisplayed()
        assertThat(testTabs.capturedTabs)
            .containsExactly(A2uiBasicCatalogV1.Tabs.Tab("Tab 1: Overview", "child_1"))
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
                            type = "Tabs",
                            properties =
                                mapOf("tabs" to listOf(mapOf("title" to "T", "child" to "C"))),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(hasText("Tabs: T - C") and hasTestTag("custom_tag")).assertIsDisplayed()
    }

    @Test
    fun content_tabsChange_recomposesWithNewTabs() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Old Tab", "child" to "child_old"))
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Tabs: Old Tab - child_old").assertIsDisplayed()

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "tabs" to
                        listOf(
                            mapOf("title" to "New Tab 1", "child" to "child_new_1"),
                            mapOf("title" to "New Tab 2", "child" to "child_new_2"),
                        )
                ),
        )
        controller.waitForIdle()

        onNodeWithText("Tabs: Old Tab - child_old").assertDoesNotExist()
        onNodeWithText("Tabs: New Tab 1,New Tab 2 - child_new_1,child_new_2").assertIsDisplayed()
        assertThat(testTabs.capturedTabs)
            .containsExactly(
                A2uiBasicCatalogV1.Tabs.Tab("New Tab 1", "child_new_1"),
                A2uiBasicCatalogV1.Tabs.Tab("New Tab 2", "child_new_2"),
            )
            .inOrder()
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to listOf(mapOf("title" to "Tab", "child" to "child_1"))
                                ),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(hasText("Tabs: Tab - child_1") and hasTestTag("initial_tag")).assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(hasText("Tabs: Tab - child_1") and hasTestTag("updated_tag")).assertIsDisplayed()
    }
}
