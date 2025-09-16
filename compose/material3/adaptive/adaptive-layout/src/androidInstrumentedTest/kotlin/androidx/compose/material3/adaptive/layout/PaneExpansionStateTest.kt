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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PaneExpansionStateTest {

    @get:Rule val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun rememberPaneExpansionState_unspecifiedInitialAnchoredIndex_noCurrentAnchor() {
        var paneExpansionState: PaneExpansionState? = null

        rule.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    anchors =
                        listOf(MockAnchor0, MockAnchor1, MockAnchor2, MockAnchor3, MockAnchor4)
                )
        }

        rule.runOnUiThread { assertThat(paneExpansionState!!.currentAnchor).isNull() }
    }

    @Test
    fun rememberPaneExpansionState_illegalInitialAnchoredIndex_throws() {
        assertFailsWith<IndexOutOfBoundsException> {
            rule.setContent {
                rememberPaneExpansionState(anchors = emptyList(), initialAnchoredIndex = 0)
            }
        }
    }

    @Test
    fun rememberPaneExpansionState_changeInitialAnchoredIndex_keepCurrentAnchor() {
        var paneExpansionState: PaneExpansionState? = null
        val anchorsState =
            mutableStateListOf(MockAnchor0, MockAnchor1, MockAnchor2, MockAnchor3, MockAnchor4)
        val initialAnchoredIndexState = mutableIntStateOf(3)

        rule.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    anchors = anchorsState.toList(),
                    initialAnchoredIndex = initialAnchoredIndexState.intValue,
                )
        }

        rule.runOnUiThread {
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3)

            // Changes initial index should not affect the current anchor
            initialAnchoredIndexState.intValue = 0
        }

        rule.waitForIdle()
        rule.runOnUiThread { assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3) }
    }

    @Test
    fun rememberPaneExpansionState_changeCurrentAnchorIndex_keepCurrentAnchor() {
        var paneExpansionState: PaneExpansionState? = null
        val anchorsState =
            mutableStateListOf(MockAnchor0, MockAnchor1, MockAnchor2, MockAnchor3, MockAnchor4)
        val initialAnchoredIndexState = mutableIntStateOf(3)

        rule.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    anchors = anchorsState.toList(),
                    initialAnchoredIndex = initialAnchoredIndexState.intValue,
                )
        }

        rule.runOnUiThread {
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3)

            // Changing current anchor's index should not affect the current anchor
            anchorsState.removeAt(0)
        }

        rule.waitForIdle()
        rule.runOnUiThread { assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3) }
    }

    @Test
    fun rememberPaneExpansionState_restoreWithDifferentInitialAnchoredIndex_keepCurrentAnchor() {
        var paneExpansionState: PaneExpansionState? = null
        val anchorsState =
            mutableStateListOf(MockAnchor0, MockAnchor1, MockAnchor2, MockAnchor3, MockAnchor4)
        val initialAnchoredIndexState = mutableIntStateOf(3)

        restorationTester.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    anchors = anchorsState.toList(),
                    initialAnchoredIndex = initialAnchoredIndexState.intValue,
                )
        }

        rule.runOnUiThread {
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3)

            // Changes initial index should not affect the current anchor
            initialAnchoredIndexState.intValue = 0

            // Null it to ensure recomposition happened
            paneExpansionState = null
        }

        restorationTester.emulateSavedInstanceStateRestore()
        rule.runOnUiThread {
            // The current anchor should be the same after restoring
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3)
        }
    }

    @Test
    fun rememberPaneExpansionState_removeCurrentAnchorFromAnchors_useNewInitialAnchor() {
        var paneExpansionState: PaneExpansionState? = null
        val anchorsState =
            mutableStateListOf(MockAnchor0, MockAnchor1, MockAnchor2, MockAnchor3, MockAnchor4)
        val initialAnchoredIndexState = mutableIntStateOf(3)

        rule.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    anchors = anchorsState.toList(),
                    initialAnchoredIndex = initialAnchoredIndexState.intValue,
                )
        }

        rule.runOnUiThread {
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3)

            anchorsState.removeAt(initialAnchoredIndexState.intValue)
        }

        rule.waitForIdle()
        rule.runOnUiThread { assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor4) }
    }

    @Test
    fun rememberPaneExpansionState_updateAnchors_useNewInitialAnchor() {
        var paneExpansionState: PaneExpansionState? = null
        val anchorsState = mutableStateListOf(MockAnchor0, MockAnchor1, MockAnchor2)
        val initialAnchoredIndexState = mutableIntStateOf(2)

        rule.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    anchors = anchorsState.toList(),
                    initialAnchoredIndex = initialAnchoredIndexState.intValue,
                )
        }

        rule.runOnUiThread {
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor2)

            // Removing the current anchor should clear the current anchor
            anchorsState.clear()
            anchorsState.addAll(listOf(MockAnchor3, MockAnchor4))
            initialAnchoredIndexState.intValue = 1
        }

        rule.waitForIdle()
        rule.runOnUiThread { assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor4) }
    }

    @Test
    fun rememberPaneExpansionState_differentKey_hasDifferentInitialAnchor() {
        var paneExpansionState: PaneExpansionState? = null
        val mockThreePaneScaffoldValue1 =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
            )
        val mockThreePaneScaffoldValue2 =
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Expanded,
            )
        val keyProviderState = mutableStateOf(mockThreePaneScaffoldValue1)
        val anchorsState =
            mutableStateListOf(MockAnchor0, MockAnchor1, MockAnchor2, MockAnchor3, MockAnchor4)
        val initialAnchoredIndexState = mutableIntStateOf(3)

        rule.setContent {
            paneExpansionState =
                rememberPaneExpansionState(
                    keyProvider = keyProviderState.value,
                    anchors = anchorsState.toList(),
                    initialAnchoredIndex = initialAnchoredIndexState.intValue,
                )
        }

        rule.runOnUiThread {
            assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor3)

            // Changing the current key and the initial anchored index
            keyProviderState.value = mockThreePaneScaffoldValue2
            initialAnchoredIndexState.intValue = 1
        }

        rule.waitForIdle()
        rule.runOnUiThread { assertThat(paneExpansionState!!.currentAnchor).isEqualTo(MockAnchor1) }
    }

    @Test
    fun test_paneExpansionStateKeySaver_defaultKey() {
        val mockKey = PaneExpansionStateKey.Default
        var savedKey: PaneExpansionStateKey? = null

        restorationTester.setContent {
            savedKey = rememberSaveable(saver = PaneExpansionStateKeySaver()) { mockKey }
        }

        rule.runOnUiThread {
            // Null it to ensure recomposition happened
            savedKey = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(savedKey).isEqualTo(mockKey) }
    }

    @Test
    fun test_paneExpansionStateKeySaver_twoPaneKey() {
        val mockKey =
            TwoPaneExpansionStateKeyImpl(
                ThreePaneScaffoldRole.Secondary,
                ThreePaneScaffoldRole.Tertiary,
            )
        var savedKey: PaneExpansionStateKey? = null

        restorationTester.setContent {
            savedKey = rememberSaveable(saver = PaneExpansionStateKeySaver()) { mockKey }
        }

        rule.runOnUiThread {
            // Null it to ensure recomposition happened
            savedKey = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(savedKey).isEqualTo(mockKey) }
    }

    @Test
    fun test_paneExpansionStateDataSaver_nullAnchor() {
        val mockData = PaneExpansionStateData(1, 0.2F, 3, null)

        var savedData: PaneExpansionStateData? = null

        restorationTester.setContent {
            savedData = rememberSaveable(saver = PaneExpansionStateDataSaver()) { mockData }
        }

        rule.runOnUiThread {
            // Null it to ensure recomposition happened
            savedData = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(savedData).isEqualTo(mockData) }
    }

    @Test
    fun test_paneExpansionStateDataSaver_proportionalAnchor() {
        val mockData = PaneExpansionStateData(4, 0.5F, 6, PaneExpansionAnchor.Proportion(0.4F))

        var savedData: PaneExpansionStateData? = null

        restorationTester.setContent {
            savedData = rememberSaveable(saver = PaneExpansionStateDataSaver()) { mockData }
        }

        rule.runOnUiThread {
            // Null it to ensure recomposition happened
            savedData = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(savedData).isEqualTo(mockData) }
    }

    @Test
    fun test_paneExpansionStateDataSaver_offsetAnchor() {
        val mockData =
            PaneExpansionStateData(7, 0.8F, 9, PaneExpansionAnchor.Offset.fromStart(200.dp))

        var savedData: PaneExpansionStateData? = null

        restorationTester.setContent {
            savedData = rememberSaveable(saver = PaneExpansionStateDataSaver()) { mockData }
        }

        rule.runOnUiThread {
            // Null it to ensure recomposition happened
            savedData = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread { assertThat(savedData).isEqualTo(mockData) }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockAnchor0 = PaneExpansionAnchor.Proportion(0f)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockAnchor1 = PaneExpansionAnchor.Offset.fromStart(200.dp)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockAnchor2 = PaneExpansionAnchor.Proportion(0.5f)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockAnchor3 = PaneExpansionAnchor.Offset.fromEnd(200.dp)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockAnchor4 = PaneExpansionAnchor.Proportion(1f)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockAnchor5 = PaneExpansionAnchor.Offset.fromStart(500.dp)
