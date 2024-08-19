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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
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
    fun test_paneExpansionStateSaver() {
        val mockPaneExpansionStateDataMap =
            mutableMapOf(
                Pair(PaneExpansionStateKey.Default, PaneExpansionStateData(1, 0.2F, 3)),
                Pair(
                    TwoPaneExpansionStateKeyImpl(
                        ThreePaneScaffoldRole.Primary,
                        ThreePaneScaffoldRole.Secondary
                    ),
                    PaneExpansionStateData(4, 0.5F, 6)
                ),
                Pair(
                    TwoPaneExpansionStateKeyImpl(
                        ThreePaneScaffoldRole.Secondary,
                        ThreePaneScaffoldRole.Tertiary
                    ),
                    PaneExpansionStateData(7, 0.8F, 9)
                ),
                Pair(
                    TwoPaneExpansionStateKeyImpl(
                        ThreePaneScaffoldRole.Tertiary,
                        ThreePaneScaffoldRole.Primary
                    ),
                    PaneExpansionStateData(10, 0.3F, 12)
                ),
            )

        var savedMap: MutableMap<PaneExpansionStateKey, PaneExpansionStateData>? = null

        restorationTester.setContent {
            savedMap =
                rememberSaveable(saver = PaneExpansionStateSaver()) {
                    mockPaneExpansionStateDataMap
                }
        }

        rule.runOnUiThread {
            // Null it to ensure recomposition happened
            savedMap = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            mockPaneExpansionStateDataMap.entries.forEach {
                assertThat(savedMap!![it.key]).isEqualTo(it.value)
            }
        }
    }
}
