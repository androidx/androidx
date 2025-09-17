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

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
class ListDetailSceneStrategyTest {
    @get:Rule val composeRule = createComposeRule()

    val HomeScreenTestTag = "HomeScreen"
    val ListScreenTestTag = "ListScreen"
    val DetailScreenTestTag = "DetailScreen"
    val DetailPlaceholderScreenTestTag = "PlaceholderScreen"
    val ExtraScreenTestTag = "ExtraScreen"

    @Test
    fun calculateScene_singlePane_nullListDetailScene() {
        var scene: Scene<TestKey>? = null

        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = PaneScaffoldDirective.Default)
            scene = strategy.calculateScene(listOf(nonListDetailEntry, listEntry, detailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene).isNull()
    }

    @Test
    fun calculateScene_dualPane_onlyListDetailEntry() {
        var scene: Scene<TestKey>? = null
        val entries = listOf(listEntry, detailEntry)
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(entries) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactlyElementsIn(entries).inOrder()
    }

    @Test
    fun calculateScene_dualPane_nonListDetailEntryAtTheFront() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(listOf(nonListDetailEntry, listEntry, detailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry, detailEntry).inOrder()
    }

    @Test
    fun calculateScene_dualPane_nonListDetailEntryAtTheEnd() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(listOf(listEntry, detailEntry, nonListDetailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene).isNull()
    }

    @Test
    fun calculateScene_dualPane_onlyListEntry() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(listOf(nonListDetailEntry, listEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry)
    }

    @Test
    fun calculateScene_dualPane_onlyDetailEntry() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(listOf(nonListDetailEntry, detailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(detailEntry)
    }

    @Test
    fun calculateScene_dualPane_consecutiveListDetailPairs() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene =
                strategy.calculateScene(listOf(listEntry, detailEntry, listEntry, detailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry, detailEntry, listEntry, detailEntry)
    }

    @Test
    fun calculateScene_dualPane_nonConsecutiveListDetail() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(listOf(listEntry, nonListDetailEntry, detailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(detailEntry)
    }

    @Test
    fun calculateScene_dualPane_nonConsecutiveListDetailPairs() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene =
                strategy.calculateScene(
                    listOf(listEntry, detailEntry, nonListDetailEntry, listEntry, detailEntry)
                ) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry, detailEntry)
    }

    @Test
    fun calculateScene_dualPane_multipleDetailEntries() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene =
                strategy.calculateScene(
                    listOf(nonListDetailEntry, listEntry, detailEntry, detailEntry)
                ) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry, detailEntry, detailEntry)
    }

    @Test
    fun calculateScene_dualPane_multipleListEntries() {
        var scene: Scene<TestKey>? = null
        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene =
                strategy.calculateScene(
                    listOf(nonListDetailEntry, listEntry, listEntry, detailEntry)
                ) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry, listEntry, detailEntry)
    }

    @Test
    fun calculateScene_dualPane_differentSceneKeys() {
        var scene: Scene<TestKey>? = null

        val listEntry1: NavEntry<TestKey> =
            NavEntry(ListKey, metadata = ListDetailSceneStrategy.listPane(sceneKey = 1)) {}
        val listEntry2: NavEntry<TestKey> =
            NavEntry(ListKey, metadata = ListDetailSceneStrategy.listPane(sceneKey = 2)) {}

        composeRule.setContent {
            val strategy =
                rememberListDetailSceneStrategy<TestKey>(directive = MockDualPaneScaffoldDirective)
            scene = strategy.calculateScene(listOf(listEntry1, listEntry2)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene!!.entries).containsExactly(listEntry2)
    }

    @Test
    fun singlePane_backstackWithList_showsList() {
        val backStack = mutableStateListOf(HomeKey, ListKey)
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = PaneScaffoldDirective.Default)
        }
        composeRule.onNodeWithTag(ListScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun singlePane_backstackWithListDetail_showsOnlyDetail() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"))
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = PaneScaffoldDirective.Default)
        }
        composeRule.onNodeWithTag(DetailScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ListScreenTestTag).assertIsNotDisplayed()
    }

    @Test
    fun dualPane_backstackWithList_showsListAndPlaceholder() {
        val backStack = mutableStateListOf(HomeKey, ListKey)
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }

        composeRule.onNodeWithTag(ListScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailPlaceholderScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun dualPane_backstackWithListDetail_showsListAndDetail() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"))
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }
        composeRule.onNodeWithTag(ListScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailScreenTestTag).assertIsDisplayed()
    }

    @Test
    fun dualPane_backstackWithListDetailExtra_showsDetailAndExtra() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"), ExtraKey("abc"))
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }
        composeRule.onNodeWithTag(DetailScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ExtraScreenTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ListScreenTestTag).assertIsNotDisplayed()
    }

    @Test
    fun dualPane_backstackWithListDetailExtra_onBack_removesExtra() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"), ExtraKey("abc"))
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }
        composeRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        assertThat(backStack).containsExactly(HomeKey, ListKey, DetailKey("abc")).inOrder()
    }

    @Test
    fun dualPane_backstackWithListDetail_onBack_popLatest_removesDetail() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"))
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            NavScreen(
                backStack = backStack,
                backNavigationBehavior = BackNavigationBehavior.PopLatest,
                directive = MockDualPaneScaffoldDirective,
            )
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }
        composeRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        assertThat(backStack).containsExactly(HomeKey, ListKey).inOrder()
    }

    @Test
    fun dualPane_backstackWithListDetail_onBack_defaultBackBehavior_removesListAndDetail() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"))
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }
        composeRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        assertThat(backStack).containsExactly(HomeKey).inOrder()
    }

    @Composable
    fun NavScreen(
        backStack: List<TestKey>,
        backNavigationBehavior: BackNavigationBehavior =
            BackNavigationBehavior.PopUntilScaffoldValueChange,
        directive: PaneScaffoldDirective =
            calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    ) {
        val listDetailSceneStrategy =
            rememberListDetailSceneStrategy<TestKey>(
                backNavigationBehavior = backNavigationBehavior,
                directive = directive,
            )
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            sceneStrategy = listDetailSceneStrategy,
            entryProvider =
                entryProvider {
                    entry<HomeKey> {
                        Box(Modifier.testTag(HomeScreenTestTag).size(100.dp).background(Color.Red))
                    }

                    entry<ListKey>(
                        metadata =
                            ListDetailSceneStrategy.listPane(ListKey) {
                                Box(
                                    Modifier.testTag(DetailPlaceholderScreenTestTag)
                                        .size(100.dp)
                                        .background(Color.Cyan)
                                )
                            }
                    ) {
                        Box(
                            Modifier.testTag(ListScreenTestTag).size(100.dp).background(Color.Green)
                        )
                    }

                    entry<DetailKey>(metadata = ListDetailSceneStrategy.detailPane(ListKey)) {
                        Box(
                            Modifier.testTag(DetailScreenTestTag)
                                .size(100.dp)
                                .background(Color.Blue)
                        )
                    }

                    entry<ExtraKey>(metadata = ListDetailSceneStrategy.extraPane(ListKey)) {
                        Box(
                            Modifier.testTag(ExtraScreenTestTag).size(100.dp).background(Color.Gray)
                        )
                    }
                },
        )
    }
}

private val listEntry: NavEntry<TestKey> =
    NavEntry(ListKey, metadata = ListDetailSceneStrategy.listPane()) {}

private val detailEntry: NavEntry<TestKey> =
    NavEntry(DetailKey("1"), metadata = ListDetailSceneStrategy.detailPane()) {}

private val nonListDetailEntry: NavEntry<TestKey> = NavEntry(HomeKey) {}

private val MockDualPaneScaffoldDirective =
    PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 2,
        horizontalPartitionSpacerSize = 16.dp,
    )
