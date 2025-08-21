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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ListDetailSceneStrategyTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun singlePaneNullListDetailScene() {
        var scene: Scene<TestKey>? = null

        composeRule.setContent {
            val strategy = rememberListDetailSceneStrategy<TestKey>()
            scene = strategy.calculateScene(listOf(nonListDetailEntry, listEntry, detailEntry)) {}
        }

        composeRule.waitForIdle()
        assertThat(scene).isNull()
    }

    @Test
    fun onlyListDetailEntry() {
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
    fun nonListDetailEntryAtTheFront() {
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
    fun nonListDetailEntryAtTheEnd() {
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
    fun onlyListEntry() {
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
    fun onlyDetailEntry() {
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
    fun consecutiveListDetailPairs() {
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
    fun nonConsecutiveListDetail() {
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
    fun nonConsecutiveListDetailPairs() {
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
    fun multiple_detailEntries() {
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
    fun multiple_listEntries() {
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
}

private interface TestKey

private object ListKey : TestKey

private object DetailKey : TestKey

private object NonListDetailKey : TestKey

private val listEntry: NavEntry<TestKey> =
    NavEntry(ListKey, metadata = ListDetailSceneStrategy.listPane()) {}

private val detailEntry: NavEntry<TestKey> =
    NavEntry(DetailKey, metadata = ListDetailSceneStrategy.detailPane()) {}

private val nonListDetailEntry: NavEntry<TestKey> = NavEntry(NonListDetailKey) {}

private val MockDualPaneScaffoldDirective =
    PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 3,
        horizontalPartitionSpacerSize = 16.dp,
    )
