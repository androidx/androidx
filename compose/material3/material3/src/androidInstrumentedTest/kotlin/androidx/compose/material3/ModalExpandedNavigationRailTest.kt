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
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class ModalExpandedNavigationRailTest {

    @get:Rule val rule = createComposeRule()
    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun modalRail_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalExpandedNavigationRail(onDismissRequest = {}) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule
            .onNodeWithTag("item")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun modalRail_closes() {
        val railWidth = NavigationRailExpandedTokens.ContainerWidthMinimum
        lateinit var railState: ModalExpandedNavigationRailState
        lateinit var scope: CoroutineScope

        rule.setMaterialContentForSizeAssertions {
            railState = rememberModalExpandedNavigationRailState()
            scope = rememberCoroutineScope()

            ModalExpandedNavigationRail(onDismissRequest = {}, railState = railState) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        // Rail starts as open.
        assertThat(railState.isOpen).isTrue()
        // Close rail.
        scope.launch { railState.close() }
        rule.waitForIdle()

        // Assert rail is not open.
        assertThat(railState.isOpen).isFalse()
        // Assert rail is not displayed.
        rule.onNodeWithTag("item").onParent().isNotDisplayed()
        // Assert rail's offset.
        rule.onNodeWithTag("item").onParent().assertLeftPositionInRootIsEqualTo(-railWidth)
    }

    @Test
    fun modalRail_opens() {
        lateinit var railState: ModalExpandedNavigationRailState
        lateinit var scope: CoroutineScope

        rule.setMaterialContentForSizeAssertions {
            railState = rememberModalExpandedNavigationRailState()
            railState.initialValue = ModalExpandedNavigationRailValue.Closed
            scope = rememberCoroutineScope()

            ModalExpandedNavigationRail(onDismissRequest = {}, railState = railState) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
            scope.launch { railState.close() }
        }

        scope.launch { railState.open() }
        rule.waitForIdle()

        // Assert rail is open.
        assertThat(railState.isOpen).isTrue()
        // Assert rail is displayed.
        rule.onNodeWithTag("item").onParent().isDisplayed()
        // Assert rail's offset.
        rule.onNodeWithTag("item").onParent().assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun modalRail_closes_bySwiping() {
        lateinit var railState: ModalExpandedNavigationRailState

        rule.setMaterialContentForSizeAssertions {
            railState = rememberModalExpandedNavigationRailState()

            ModalExpandedNavigationRail(onDismissRequest = {}, railState = railState) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("item").onParent().performTouchInput { swipeLeft() }
        rule.waitForIdle()

        // Assert rail is not open.
        assertThat(railState.isOpen).isFalse()
        // Assert rail is not displayed.
        rule.onNodeWithTag("item").onParent().isNotDisplayed()
    }

    @Test
    fun modalRail_doesNotClose_bySwiping_gesturesDisabled() {
        lateinit var railState: ModalExpandedNavigationRailState

        rule.setMaterialContentForSizeAssertions {
            railState = rememberModalExpandedNavigationRailState()

            ModalExpandedNavigationRail(
                gesturesEnabled = false,
                onDismissRequest = {},
                railState = railState,
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("item").onParent().performTouchInput { swipeLeft() }
        rule.waitForIdle()

        // Assert rail is still open.
        assertThat(railState.isOpen).isTrue()
        // Assert rail is still displayed.
        rule.onNodeWithTag("item").onParent().isDisplayed()
    }

    @Test
    fun modalRail_closes_byScrimClick() {
        lateinit var closeRail: String
        lateinit var railState: ModalExpandedNavigationRailState
        rule.setMaterialContentForSizeAssertions {
            closeRail = getString(Strings.CloseRail)
            railState = rememberModalExpandedNavigationRailState()

            ModalExpandedNavigationRail(
                gesturesEnabled = false,
                onDismissRequest = {},
                railState = railState,
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        // The rail should be open.
        assertThat(railState.isOpen).isTrue()

        rule
            .onNodeWithContentDescription(closeRail)
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()

        // Assert rail is not open.
        assertThat(railState.isOpen).isFalse()
        // Assert rail is not displayed.
        rule.onNodeWithTag("item").onParent().isNotDisplayed()
    }

    @Test
    fun modalRail_hasPaneTitle() {
        lateinit var paneTitle: String

        rule.setMaterialContentForSizeAssertions {
            paneTitle = getString(Strings.WideNavigationRailPaneTitle)
            ModalExpandedNavigationRail(
                onDismissRequest = {},
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
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
    fun modalRailState_savesAndRestores() {
        lateinit var railState: ModalExpandedNavigationRailState

        restorationTester.setContent { railState = rememberModalExpandedNavigationRailState() }

        assertThat(railState.currentValue).isEqualTo(ModalExpandedNavigationRailValue.Closed)
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(railState.currentValue).isEqualTo(ModalExpandedNavigationRailValue.Closed)
    }

    @Test
    fun modalRailState_respectsConfirmStateChange() {
        lateinit var railState: ModalExpandedNavigationRailState

        restorationTester.setContent {
            railState =
                rememberModalExpandedNavigationRailState(
                    confirmValueChange = { it != ModalExpandedNavigationRailValue.Closed }
                )

            ModalExpandedNavigationRail(onDismissRequest = {}, railState = railState) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    railExpanded = true,
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(railState.currentValue).isEqualTo(ModalExpandedNavigationRailValue.Open)
        }
        rule.onNodeWithTag("item").onParent().performTouchInput { swipeLeft() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(railState.currentValue).isEqualTo(ModalExpandedNavigationRailValue.Open)
        }
        // Assert rail is still open.
        assertThat(railState.isOpen).isTrue()
        // Assert rail is still displayed.
        rule.onNodeWithTag("item").onParent().isDisplayed()
    }
}
