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

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiAccessibilityUiTest {

    @Test
    fun nullAttributes_doesNotAddSemantics() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(attributes = null, isClickable = false)
            )
        }

        onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun nonClickable_labelOnly_setsContentDescription() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(label = "Label"),
                            isClickable = false,
                        )
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Label")
    }

    @Test
    fun nonClickable_descriptionOnly_setsContentDescription() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    description = "Description"
                                ),
                            isClickable = false,
                        )
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Description")
    }

    @Test
    fun nonClickable_labelAndDescription_combinesContentDescription() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Label",
                                    description = "Description",
                                ),
                            isClickable = false,
                        )
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Label - Description")
    }

    @Test
    fun nonClickable_emptyAttributes_doesNotSetContentDescription() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes = A2uiBasicCatalogV1.AccessibilityAttributes(),
                            isClickable = false,
                        )
            )
        }

        onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun clickable_labelOnly_setsContentDescription() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(label = "Button"),
                            isClickable = true,
                        )
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Button")
        onNodeWithTag(TestTag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun clickable_descriptionOnly_setsOnClickLabel() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(description = "Submit"),
                            isClickable = true,
                        )
            )
        }

        onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("has onClick with label") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == "Submit"
                }
            )
    }

    @Test
    fun clickable_labelAndDescription_setsContentDescriptionAndOnClickLabel() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Submit",
                                    description = "Submit form",
                                ),
                            isClickable = true,
                        )
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Submit")
        onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("has onClick with label") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == "Submit form"
                }
            )
    }

    @Test
    fun clickable_withNativeClickable_mergesLabelAndPreservesNativeClick() = runComposeUiTest {
        var nativeClicked = false

        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Button",
                                    description = "Action Hint",
                                ),
                            isClickable = true,
                        )
                        .clickable { nativeClicked = true }
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Button")
        onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("has onClick with label") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == "Action Hint"
                }
            )
        onNodeWithTag(TestTag).performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()

        assertThat(nativeClicked).isTrue()
    }

    @Test
    fun container_childrenMaintainIndependentAccessibilityProperties() = runComposeUiTest {
        var actionExecuted = false

        setContent {
            Box(
                modifier =
                    Modifier.testTag("container")
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Container Label",
                                    description = "Container Description",
                                ),
                            isClickable = false,
                        )
            ) {
                Box(
                    modifier =
                        Modifier.testTag("child_non_interactive")
                            .a2uiAccessibility(
                                attributes =
                                    A2uiBasicCatalogV1.AccessibilityAttributes(
                                        label = "Child 1 Label",
                                        description = "Child 1 Description",
                                    ),
                                isClickable = false,
                            )
                )
                Box(
                    modifier =
                        Modifier.testTag("child_interactive")
                            .a2uiAccessibility(
                                attributes =
                                    A2uiBasicCatalogV1.AccessibilityAttributes(
                                        label = "Child 2 Label",
                                        description = "Child 2 Action",
                                    ),
                                isClickable = true,
                            )
                            .clickable { actionExecuted = true }
                )
            }
        }

        onNodeWithTag("container")
            .assertContentDescriptionEquals("Container Label - Container Description")
        onNodeWithContentDescription("Container Label - Container Description").assertExists()

        onNodeWithTag("child_non_interactive")
            .assertContentDescriptionEquals("Child 1 Label - Child 1 Description")
        onNodeWithContentDescription("Child 1 Label - Child 1 Description").assertExists()

        onNodeWithTag("child_interactive").assertContentDescriptionEquals("Child 2 Label")
        onNodeWithContentDescription("Child 2 Label").assertExists()
        onNodeWithTag("child_interactive").performSemanticsAction(SemanticsActions.OnClick)
        assertThat(actionExecuted).isTrue()
    }

    @Test
    fun clickable_blankDescription_doesNotSetOnClickLabel() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "Button",
                                    description = "   ",
                                ),
                            isClickable = true,
                        )
            )
        }

        onNodeWithTag(TestTag).assertContentDescriptionEquals("Button")
        onNodeWithTag(TestTag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun nonClickable_blankAttributes_doesNotSetContentDescription() = runComposeUiTest {
        setContent {
            Box(
                modifier =
                    Modifier.testTag(TestTag)
                        .a2uiAccessibility(
                            attributes =
                                A2uiBasicCatalogV1.AccessibilityAttributes(
                                    label = "  ",
                                    description = "",
                                ),
                            isClickable = false,
                        )
            )
        }

        onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }
}

private const val TestTag = "test_tag"
