/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package androidx.compose.material3.adaptive.navigation3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.Scene
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SupportingPaneSceneStrategyTest {
    @get:Rule val composeRule = createComposeRule()

    val HomeScreenTestTag = "HomeScreen"
    val MainScreenTestTag = "MainScreen"
    val SupportingScreenTestTag = "SupportingScreen"
    val ExtraScreenTestTag = "ExtraScreen"

    @Test
    fun calculateScene_singlePane_nullSupportingPaneScene() {
        var scene: Scene<TestKey>? = null

        composeRule.setContent {
            val strategy =
                rememberSupportingPaneSceneStrategy<TestKey>(
                    directive = PaneScaffoldDirective.Default
                )
            scene = strategy.calculateScene(listOf(nonSupportingPaneEntry, mainEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene).isNull()
    }

    @Test
    fun calculateScene_reflowedPane_mainAndSupporting() {
        var scene: Scene<TestKey>? = null
        val entries = listOf(mainEntry, supportingEntry)
        composeRule.setContent {
            val strategy =
                rememberSupportingPaneSceneStrategy<TestKey>(
                    directive = MockDualVerticalPaneScaffoldDirective
                )
            scene = strategy.calculateScene(entries) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactlyElementsIn(entries).inOrder()
    }

    @Test
    fun calculateScene_dualPane_mainAndSupporting() {
        var scene: Scene<TestKey>? = null
        val entries = listOf(mainEntry, supportingEntry)
        composeRule.setContent {
            val strategy =
                rememberSupportingPaneSceneStrategy<TestKey>(
                    directive = MockDualPaneScaffoldDirective
                )
            scene = strategy.calculateScene(entries) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactlyElementsIn(entries).inOrder()
    }

    @Test
    fun calculateScene_dualPane_onlyMain() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberSupportingPaneSceneStrategy<TestKey>(
                    directive = MockDualPaneScaffoldDirective
                )
            scene = strategy.calculateScene(listOf(nonSupportingPaneEntry, mainEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(mainEntry)
    }

    @Test
    fun calculateScene_dualPane_nonConsecutiveEntries() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberSupportingPaneSceneStrategy<TestKey>(
                    directive = MockDualPaneScaffoldDirective
                )
            scene =
                strategy.calculateScene(
                    listOf(mainEntry, nonSupportingPaneEntry, supportingEntry)
                ) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(supportingEntry)
    }

    @Test
    fun calculateScene_dualPane_differentSceneKeys() {
        var scene: Scene<TestKey>? = null

        val mainEntry1: NavEntry<TestKey> =
            NavEntry(MainKey, metadata = SupportingPaneSceneStrategy.mainPane(sceneKey = 1)) {}
        val mainEntry2: NavEntry<TestKey> =
            NavEntry(MainKey, metadata = SupportingPaneSceneStrategy.mainPane(sceneKey = 2)) {}

        composeRule.setContent {
            val strategy =
                rememberSupportingPaneSceneStrategy<TestKey>(
                    directive = MockDualPaneScaffoldDirective
                )
            scene = strategy.calculateScene(listOf(mainEntry1, mainEntry2)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(mainEntry2)
    }

    @Test
    fun singlePane_backstackWithMainPane_showsMainPane() {
        val backStack = mutableStateListOf(HomeKey, MainKey)
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = PaneScaffoldDirective.Default)
        }
        composeRule.onNodeWithTag(MainScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun reflowedPane_backstackWithMainAndSupporting_showsMainAndSupportingPane() {
        val backStack = mutableStateListOf(HomeKey, MainKey, SupportingKey)
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualVerticalPaneScaffoldDirective)
        }
        composeRule.onNodeWithTag(MainScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(SupportingScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun dualPane_backstackWithMainPane_showsMainPane() {
        val backStack = mutableStateListOf(HomeKey, MainKey)
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }

        composeRule.onNodeWithTag(MainScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun dualPane_backstackWithMainAndSupporting_showsMainAndSupporting() {
        val backStack = mutableStateListOf(HomeKey, MainKey, SupportingKey)
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }
        composeRule.onNodeWithTag(MainScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(SupportingScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun dualPane_backstackWithMainSupportingExtra_showsSupportingAndExtra() {
        val backStack = mutableStateListOf(HomeKey, MainKey, SupportingKey, ExtraKey("abc"))
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }
        composeRule.onNodeWithTag(SupportingScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ExtraScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(MainScreenTestTag).assertIsNotDisplayed()
    }

    @Composable
    fun NavScreen(
        backStack: List<TestKey>,
        backNavigationBehavior: BackNavigationBehavior =
            BackNavigationBehavior.PopUntilScaffoldValueChange,
        directive: PaneScaffoldDirective =
            calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    ) {
        val supportingSceneStrategy =
            rememberSupportingPaneSceneStrategy<TestKey>(
                backNavigationBehavior = backNavigationBehavior,
                directive = directive,
            )
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            sceneStrategy = supportingSceneStrategy,
            entryProvider =
                entryProvider {
                    entry<HomeKey> {
                        Box(Modifier.testTag(HomeScreenTestTag).size(100.dp).background(Color.Red))
                    }

                    entry<MainKey>(metadata = SupportingPaneSceneStrategy.mainPane(MainKey)) {
                        Box(
                            Modifier.testTag(MainScreenTestTag).size(100.dp).background(Color.Green)
                        )
                    }

                    entry<SupportingKey>(
                        metadata = SupportingPaneSceneStrategy.supportingPane(MainKey)
                    ) {
                        Box(
                            Modifier.testTag(SupportingScreenTestTag)
                                .size(100.dp)
                                .background(Color.Blue)
                        )
                    }

                    entry<ExtraKey>(metadata = SupportingPaneSceneStrategy.extraPane(MainKey)) {
                        Box(
                            Modifier.testTag(ExtraScreenTestTag).size(100.dp).background(Color.Gray)
                        )
                    }
                },
        )
    }
}

private val mainEntry: NavEntry<TestKey> =
    NavEntry(MainKey, metadata = SupportingPaneSceneStrategy.mainPane()) {}

private val supportingEntry: NavEntry<TestKey> =
    NavEntry(SupportingKey, metadata = SupportingPaneSceneStrategy.supportingPane()) {}

private val nonSupportingPaneEntry: NavEntry<TestKey> = NavEntry(HomeKey) {}

private val MockDualPaneScaffoldDirective =
    PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 2,
        horizontalPartitionSpacerSize = 16.dp,
    )

private val MockDualVerticalPaneScaffoldDirective =
    PaneScaffoldDirective.Default.copy(maxVerticalPartitions = 2)
