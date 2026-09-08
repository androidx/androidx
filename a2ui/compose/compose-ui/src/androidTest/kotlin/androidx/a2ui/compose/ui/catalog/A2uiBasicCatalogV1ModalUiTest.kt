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
class A2uiBasicCatalogV1ModalUiTest {

    private val testModal =
        object : A2uiBasicCatalogV1.Modal {
            var capturedTriggerId: String? = null
            var capturedContentId: String? = null
            var capturedAccessibility: A2uiBasicCatalogV1.AccessibilityAttributes? = null

            @Composable
            override fun A2uiComponentScope.TypedContent(
                triggerId: String,
                contentId: String,
                accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
                modifier: Modifier,
            ) {
                SideEffect {
                    capturedTriggerId = triggerId
                    capturedContentId = contentId
                    capturedAccessibility = accessibility
                }
                BasicText(
                    text = "Modal: trigger=$triggerId, content=$contentId",
                    modifier = modifier,
                )
            }
        }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(testModal),
            functions = emptyList(),
        )

    @Test
    fun content_resolvesPropertiesAndPassesToTypedContent() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf(
                                    "trigger" to "btn_trigger",
                                    "content" to "dialog_content",
                                    "accessibility" to
                                        mapOf(
                                            "label" to "Dialog Title",
                                            "description" to "Dialog Description",
                                        ),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Modal: trigger=btn_trigger, content=dialog_content").assertIsDisplayed()
        assertThat(testModal.capturedTriggerId).isEqualTo("btn_trigger")
        assertThat(testModal.capturedContentId).isEqualTo("dialog_content")
        assertThat(testModal.capturedAccessibility)
            .isEqualTo(
                A2uiBasicCatalogV1.AccessibilityAttributes(
                    label = "Dialog Title",
                    description = "Dialog Description",
                )
            )
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
                            type = "Modal",
                            properties =
                                mapOf(
                                    "trigger" to "btn_trigger",
                                    "content" to "dialog_content",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface = surface, modifier = Modifier.testTag("custom_tag")) }

        onNode(
                hasText("Modal: trigger=btn_trigger, content=dialog_content") and
                    hasTestTag("custom_tag")
            )
            .assertIsDisplayed()
    }

    @Test
    fun content_triggerIdChange_recomposesWithNewTriggerId() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf(
                                    "trigger" to "old_trigger",
                                    "content" to "dialog_content",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Modal: trigger=old_trigger, content=dialog_content").assertIsDisplayed()
        assertThat(testModal.capturedTriggerId).isEqualTo("old_trigger")

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "trigger" to "new_trigger",
                    "content" to "dialog_content",
                ),
        )
        controller.waitForIdle()

        onNodeWithText("Modal: trigger=old_trigger, content=dialog_content").assertDoesNotExist()
        onNodeWithText("Modal: trigger=new_trigger, content=dialog_content").assertIsDisplayed()
        assertThat(testModal.capturedTriggerId).isEqualTo("new_trigger")
    }

    @Test
    fun content_contentIdChange_recomposesWithNewContentId() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Modal",
                            properties =
                                mapOf(
                                    "trigger" to "btn_trigger",
                                    "content" to "old_content",
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent { A2uiTestSurface(surface) }

        onNodeWithText("Modal: trigger=btn_trigger, content=old_content").assertIsDisplayed()
        assertThat(testModal.capturedContentId).isEqualTo("old_content")

        controller.updateComponent(
            id = "root",
            properties =
                mapOf(
                    "trigger" to "btn_trigger",
                    "content" to "new_content",
                ),
        )
        controller.waitForIdle()

        onNodeWithText("Modal: trigger=btn_trigger, content=old_content").assertDoesNotExist()
        onNodeWithText("Modal: trigger=btn_trigger, content=new_content").assertIsDisplayed()
        assertThat(testModal.capturedContentId).isEqualTo("new_content")
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
                            type = "Modal",
                            properties =
                                mapOf(
                                    "trigger" to "btn_trigger",
                                    "content" to "dialog_content",
                                ),
                        )
                    ),
            )
        val surface = controller.start()
        var modifier by mutableStateOf(Modifier.testTag("initial_tag"))

        setContent { A2uiTestSurface(surface = surface, modifier = modifier) }

        onNode(
                hasText("Modal: trigger=btn_trigger, content=dialog_content") and
                    hasTestTag("initial_tag")
            )
            .assertIsDisplayed()
        onNode(hasTestTag("updated_tag")).assertDoesNotExist()

        modifier = Modifier.testTag("updated_tag")
        waitForIdle()

        onNode(hasTestTag("initial_tag")).assertDoesNotExist()
        onNode(
                hasText("Modal: trigger=btn_trigger, content=dialog_content") and
                    hasTestTag("updated_tag")
            )
            .assertIsDisplayed()
    }
}
