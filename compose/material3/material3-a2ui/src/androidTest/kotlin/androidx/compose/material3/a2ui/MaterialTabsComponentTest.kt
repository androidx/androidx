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
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialTabsComponentTest {

    private val testCatalog =
        A2uiCatalog(catalogId = "test_catalog", components = listOf(MaterialTabsComponent))

    @Test
    fun tabs_rendersTabTitlesAndDefaultSelectedChild() = runComposeUiTest {
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
                        ),
                        A2uiComponentPayload(id = "child_1"),
                        A2uiComponentPayload(id = "child_2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Child 1 Content", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_2") { _, modifier ->
                            Text("Child 2 Content", modifier = modifier)
                        },
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Tab 1").assertIsDisplayed().assertIsSelected()
        onNodeWithText("Tab 2").assertIsDisplayed().assertIsNotSelected()
        onNodeWithText("Child 1 Content").assertIsDisplayed()
        onNodeWithText("Child 2 Content").assertDoesNotExist()
    }

    @Test
    fun tabs_userClick_switchesActiveTabAndRendersSelectedChild() = runComposeUiTest {
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
                        ),
                        A2uiComponentPayload(id = "child_1"),
                        A2uiComponentPayload(id = "child_2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Child 1 Content", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_2") { _, modifier ->
                            Text("Child 2 Content", modifier = modifier)
                        },
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Tab 1").assertIsSelected()
        onNodeWithText("Tab 2").assertIsNotSelected()
        onNodeWithText("Child 1 Content").assertIsDisplayed()
        onNodeWithText("Child 2 Content").assertDoesNotExist()

        onNodeWithText("Tab 2").performClick()
        controller.waitForIdle()

        onNodeWithText("Tab 1").assertIsNotSelected()
        onNodeWithText("Tab 2").assertIsSelected()
        onNodeWithText("Child 1 Content").assertDoesNotExist()
        onNodeWithText("Child 2 Content").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabs_stateRestoration_preservesSelectedTab() = runComposeUiTest {
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
                        ),
                        A2uiComponentPayload(id = "child_1"),
                        A2uiComponentPayload(id = "child_2"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Child 1 Content", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_2") { _, modifier ->
                            Text("Child 2 Content", modifier = modifier)
                        },
                    ),
            )
        val surface = controller.start()
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Tab 1").assertIsSelected()
        onNodeWithText("Tab 2").assertIsNotSelected()
        onNodeWithText("Child 1 Content").assertIsDisplayed()
        onNodeWithText("Child 2 Content").assertDoesNotExist()

        onNodeWithText("Tab 2").performClick()
        controller.waitForIdle()

        onNodeWithText("Tab 1").assertIsNotSelected()
        onNodeWithText("Tab 2").assertIsSelected()
        onNodeWithText("Child 1 Content").assertDoesNotExist()
        onNodeWithText("Child 2 Content").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Tab 1").assertIsNotSelected()
        onNodeWithText("Tab 2").assertIsSelected()
        onNodeWithText("Child 1 Content").assertDoesNotExist()
        onNodeWithText("Child 2 Content").assertIsDisplayed()
    }

    @Test
    fun tabs_dynamicTitleBinding_updatesWhenDataChanges() = runComposeUiTest {
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
                                                "title" to mapOf("path" to "/tabs/first/title"),
                                                "child" to "child_1",
                                            )
                                        )
                                ),
                        ),
                        A2uiComponentPayload(id = "child_1"),
                    ),
                initialData = mapOf("tabs" to mapOf("first" to mapOf("title" to "Overview"))),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Content", modifier = modifier)
                        }
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Overview").assertIsDisplayed().assertIsSelected()

        controller.updateData("/tabs/first/title", "Dashboard")
        controller.waitForIdle()

        onNodeWithText("Overview").assertDoesNotExist()
        onNodeWithText("Dashboard").assertIsDisplayed().assertIsSelected()
    }

    @Test
    fun tabs_emptyList_rendersNothing() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Tabs",
                            properties = mapOf("tabs" to emptyList<Map<String, Any>>()),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("tabs_tag"))
            }
        }

        onNodeWithText("Tab 1").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun tabs_selectedTabRemoved_clampsSelectionToRemainingTabs() = runComposeUiTest {
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
                                            mapOf("title" to "Tab 3", "child" to "child_3"),
                                        )
                                ),
                        ),
                        A2uiComponentPayload(id = "child_1"),
                        A2uiComponentPayload(id = "child_2"),
                        A2uiComponentPayload(id = "child_3"),
                    ),
                componentStubs =
                    listOf(
                        A2uiComponentStub.withId("child_1") { _, modifier ->
                            Text("Content 1", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_2") { _, modifier ->
                            Text("Content 2", modifier = modifier)
                        },
                        A2uiComponentStub.withId("child_3") { _, modifier ->
                            Text("Content 3", modifier = modifier)
                        },
                    ),
            )
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        // Select Tab 3 (index 2)
        onNodeWithText("Tab 3").performClick()
        controller.waitForIdle()

        onNodeWithText("Tab 1").assertIsNotSelected()
        onNodeWithText("Tab 2").assertIsNotSelected()
        onNodeWithText("Tab 3").assertIsSelected()
        onNodeWithText("Content 3").assertIsDisplayed()

        // Dynamically update tabs to only contain Tab 1 and Tab 2
        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "tabs" to
                        listOf(
                            mapOf("title" to "Tab 1", "child" to "child_1"),
                            mapOf("title" to "Tab 2", "child" to "child_2"),
                        )
                ),
        )
        controller.waitForIdle()

        // Index clamped to 1 (Tab 2)
        onNodeWithText("Tab 3").assertDoesNotExist()
        onNodeWithText("Content 3").assertDoesNotExist()

        onNodeWithText("Tab 1").assertIsNotSelected()
        onNodeWithText("Tab 2").assertIsSelected()
        onNodeWithText("Content 2").assertIsDisplayed()
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf("title" to "Tab 1", "child" to "failing_child")
                                        )
                                ),
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
    fun child_idChanges_rendersNewChildComponent() = runComposeUiTest {
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
                                        listOf(mapOf("title" to "Tab 1", "child" to "child_1"))
                                ),
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

        controller.updateComponent(
            id = "root",
            properties = mapOf("tabs" to listOf(mapOf("title" to "Tab 1", "child" to "child_2"))),
        )
        controller.waitForIdle()

        onNodeWithText("Child One").assertDoesNotExist()
        onNodeWithText("Child Two").assertIsDisplayed()
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Tab 1", "child" to "child_1"))
                                ),
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
                Text("Content", modifier = modifier)
            }

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
                                            mapOf("title" to "Tab 1", "child" to "delayed_text_id")
                                        )
                                ),
                        ),
                        A2uiComponentPayload(id = "delayed_text_id"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        onNodeWithText("Content").assertDoesNotExist()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        isReadyState.value = true
        waitForIdle()
        controller.waitForIdle()

        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertDoesNotExist()
        onNodeWithText("Content").assertIsDisplayed()
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(
                                            mapOf("title" to "Tab 1", "child" to "delayed_child")
                                        )
                                ),
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Tab 1", "child" to "child_id"))
                                ),
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Tab 1", "child" to "child_id"))
                                ),
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Tab 1", "child" to "child_id"))
                                ),
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Tab 1", "child" to "child_id"))
                                ),
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
                            type = "Tabs",
                            properties =
                                mapOf(
                                    "tabs" to
                                        listOf(mapOf("title" to "Tab 1", "child" to "stub_child"))
                                ),
                        ),
                        A2uiComponentPayload(id = "stub_child"),
                    ),
                componentStubs = listOf(stub),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface, modifier = Modifier.testTag("tabs_tag"))
            }
        }

        // Test tag applied in Loading state
        onNode(hasTestTag("tabs_tag")).assertIsDisplayed()
        onNodeWithTag(MaterialA2uiDefaults.LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()

        // Transition to Success state
        isReadyState.value = true
        waitForIdle()
        controller.waitForIdle()
        onNode(hasTestTag("tabs_tag")).assertIsDisplayed()

        // Transition to Error state
        controller.failComponent("stub_child", A2uiRuntimeException("Failure"))
        controller.waitForIdle()
        onNode(hasTestTag("tabs_tag")).assertIsDisplayed()
    }
}
