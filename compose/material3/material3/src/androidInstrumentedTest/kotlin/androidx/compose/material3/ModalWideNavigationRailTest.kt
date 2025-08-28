/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.NavigationRailCollapsedTokens
import androidx.compose.material3.tokens.NavigationRailExpandedTokens
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [ModalWideNavigationRail. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ModalWideNavigationRailTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun modalWideRail_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalWideNavigationRail {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = false,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        rule
            .onNodeWithTag("item")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun modalWideRail_expands() {
        lateinit var state: WideNavigationRailState
        rule.setMaterialContentForSizeAssertions {
            state = rememberWideNavigationRailState()
            val scope = rememberCoroutineScope()

            ModalWideNavigationRail(
                state = state,
                header = {
                    Button(
                        modifier = Modifier.testTag("header"),
                        onClick = { scope.launch { state.toggle() } },
                    ) {}
                },
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        // Click on header to expand.
        rule.onNodeWithTag("header").performClick()

        // Assert rail is expanded.
        assertThat(state.targetValue.isExpanded).isTrue()
        // Assert width changed to expanded width.
        rule
            .onNodeWithTag("item")
            .onParent()
            .assertWidthIsEqualTo(NavigationRailExpandedTokens.ContainerWidthMinimum)
    }

    @Test
    fun modalWideRail_collapses() {
        lateinit var scope: CoroutineScope
        lateinit var state: WideNavigationRailState
        rule.setMaterialContentForSizeAssertions {
            state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)
            scope = rememberCoroutineScope()
            ModalWideNavigationRail(modifier = Modifier.testTag("rail"), state = state) {
                WideNavigationRailItem(
                    railExpanded = state.targetValue.isExpanded,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        // Collapse.
        rule.runOnIdle { scope.launch { state.toggle() } }

        // Assert rail is collapsed.
        assertThat(state.targetValue.isExpanded).isFalse()
        // Assert width changed to collapse width.
        rule
            .onNodeWithTag("rail")
            .assertWidthIsEqualTo(NavigationRailCollapsedTokens.ContainerWidth)
    }

    @Test
    fun modalWideRail_collapses_byScrimClick() {
        lateinit var closeRail: String
        lateinit var state: WideNavigationRailState

        rule.setMaterialContentForSizeAssertions {
            state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)
            closeRail = getString(Strings.CloseRail)

            ModalWideNavigationRail(modifier = Modifier.testTag("rail"), state = state) {
                WideNavigationRailItem(
                    railExpanded = state.targetValue.isExpanded,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        rule
            .onNodeWithContentDescription(closeRail)
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()

        // Assert rail is collapsed.
        assertThat(state.targetValue.isExpanded).isFalse()
        // Assert width changed to collapse width.
        rule
            .onNodeWithTag("rail")
            .assertWidthIsEqualTo(NavigationRailCollapsedTokens.ContainerWidth)
    }

    @Test
    fun modalWideRail_hasPaneTitle() {
        lateinit var paneTitle: String

        rule.setMaterialContentForSizeAssertions {
            paneTitle = getString(Strings.WideNavigationRailPaneTitle)
            ModalWideNavigationRail(
                state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        rule
            .onNodeWithTag("item")
            .onParent() // rail.
            .onParent() // dialog window.
            .onParent() // parent container that holds dialog and scrim.
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, paneTitle))
    }

    @Test
    fun modalWideRail_hideOnCollapse_collapses() {
        lateinit var state: WideNavigationRailState
        lateinit var scope: CoroutineScope
        rule.setMaterialContentForSizeAssertions {
            state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)
            scope = rememberCoroutineScope()

            ModalWideNavigationRail(
                modifier = Modifier.testTag("rail"),
                state = state,
                hideOnCollapse = true,
            ) {
                WideNavigationRailItem(
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        // Rail starts as expanded.
        assertThat(state.targetValue.isExpanded).isTrue()
        // Collapse rail.
        scope.launch { state.collapse() }
        rule.waitForIdle()

        // Assert rail is not expanded.
        assertThat(state.targetValue.isExpanded).isFalse()
        // Assert rail is not displayed.
        rule.onNodeWithTag("rail").assertDoesNotExist()
    }

    @Test
    fun modalWideRail_hideOnCollapse_expands() {
        lateinit var state: WideNavigationRailState
        lateinit var scope: CoroutineScope

        rule.setMaterialContentForSizeAssertions {
            state = rememberWideNavigationRailState()
            scope = rememberCoroutineScope()

            ModalWideNavigationRail(
                modifier = Modifier.testTag("rail"),
                state = state,
                hideOnCollapse = true,
            ) {
                WideNavigationRailItem(
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        // Expand rail.
        scope.launch { state.expand() }
        rule.waitForIdle()

        // Assert rail is expanded.
        assertThat(state.targetValue.isExpanded).isTrue()
        // Assert rail is displayed.
        rule.onNodeWithTag("rail").isDisplayed()
        // Assert rail's offset.
        rule.onNodeWithTag("rail").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun modalWideRail_hideOnCollapse_collapses_bySwiping() {
        lateinit var state: WideNavigationRailState

        rule.setMaterialContentForSizeAssertions {
            state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)

            ModalWideNavigationRail(
                modifier = Modifier.testTag("rail"),
                state = state,
                hideOnCollapse = true,
            ) {
                WideNavigationRailItem(
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        rule.onNodeWithTag("rail").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        // Assert rail is not expanded.
        assertThat(state.targetValue.isExpanded).isFalse()
        // Assert rail is not displayed.
        rule.onNodeWithTag("rail").assertDoesNotExist()
    }

    @Test
    fun modalWideRail_hideOnCollapse_collapses_byScrimClick() {
        lateinit var closeRail: String
        lateinit var state: WideNavigationRailState
        rule.setMaterialContentForSizeAssertions {
            closeRail = getString(Strings.CloseRail)
            state = rememberWideNavigationRailState(WideNavigationRailValue.Expanded)

            ModalWideNavigationRail(
                modifier = Modifier.testTag("rail"),
                state = state,
                hideOnCollapse = true,
            ) {
                WideNavigationRailItem(
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {},
                )
            }
        }

        // The rail should be expanded.
        assertThat(state.targetValue.isExpanded).isTrue()

        rule
            .onNodeWithContentDescription(closeRail)
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()

        // Assert rail is not expanded.
        assertThat(state.targetValue.isExpanded).isFalse()
        // Assert rail is not displayed.
        rule.onNodeWithTag("item").assertDoesNotExist()
    }
}
