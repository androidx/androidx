/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ThreePaneScaffoldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun threePaneScaffold_allPanesHidden_noVisiblePanes() {
         val testScaffoldValue = ThreePaneScaffoldValue(
             PaneAdaptedValue.Hidden,
             PaneAdaptedValue.Hidden,
             PaneAdaptedValue.Hidden
         )
         rule.setContent {
             SampleThreePaneScaffold(scaffoldValue = testScaffoldValue)
         }

         rule.onNodeWithTag("PrimaryPane").assertDoesNotExist()
         rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
         rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_oneExpandedPane_onlyExpandedPanesAreVisible() {
        val testScaffoldValue = ThreePaneScaffoldValue(
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Hidden,
            PaneAdaptedValue.Hidden
        )
        rule.setContent {
            SampleThreePaneScaffold(scaffoldValue = testScaffoldValue)
        }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_twoExpandedPanes_onlyExpandedPanesAreVisible() {
        val testScaffoldValue = ThreePaneScaffoldValue(
            PaneAdaptedValue.Hidden,
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Expanded
        )
        rule.setContent {
            SampleThreePaneScaffold(scaffoldValue = testScaffoldValue)
        }

        rule.onNodeWithTag("PrimaryPane").assertDoesNotExist()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertExists()
    }

    @Test
    fun threePaneScaffold_threeExpandedPanes_onlyExpandedPanesAreVisible() {
        val testScaffoldValue = ThreePaneScaffoldValue(
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Expanded
        )
        rule.setContent {
            SampleThreePaneScaffold(scaffoldValue = testScaffoldValue)
        }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertExists()
    }

    @Test
    fun threePaneScaffold_scaffoldValueChangeWithSinglePane_expandedPanesAreChanged() {
        var testScaffoldValue by mutableStateOf(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Hidden
            )
        )
        rule.setContent {
            SampleThreePaneScaffold(scaffoldValue = testScaffoldValue)
        }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()

        testScaffoldValue = ThreePaneScaffoldValue(
            PaneAdaptedValue.Hidden,
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Hidden
        )

        rule.waitForIdle()

        rule.onNodeWithTag("PrimaryPane").assertDoesNotExist()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }

    @Test
    fun threePaneScaffold_scaffoldValueChangeWithDualPane_expandedPanesAreChanged() {
        var testScaffoldValue by mutableStateOf(
            ThreePaneScaffoldValue(
                PaneAdaptedValue.Expanded,
                PaneAdaptedValue.Hidden,
                PaneAdaptedValue.Expanded
            )
        )
        rule.setContent {
            SampleThreePaneScaffold(scaffoldValue = testScaffoldValue)
        }

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertDoesNotExist()
        rule.onNodeWithTag("TertiaryPane").assertExists()

        testScaffoldValue = ThreePaneScaffoldValue(
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Hidden
        )

        rule.waitForIdle()

        rule.onNodeWithTag("PrimaryPane").assertExists()
        rule.onNodeWithTag("SecondaryPane").assertExists()
        rule.onNodeWithTag("TertiaryPane").assertDoesNotExist()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockScaffoldDirective = PaneScaffoldDirective(
    contentPadding = PaddingValues(0.dp),
    maxHorizontalPartitions = 1,
    horizontalPartitionSpacerSize = 0.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
    excludedBounds = emptyList()
)

internal const val ThreePaneScaffoldTestTag = "SampleThreePaneScaffold"

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SampleThreePaneScaffold(scaffoldValue: ThreePaneScaffoldValue) {
    SampleThreePaneScaffold(
        MockScaffoldDirective,
        scaffoldValue,
        ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SampleThreePaneScaffold(
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    windowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
) {
    ThreePaneScaffold(
        modifier = Modifier.fillMaxSize().testTag(ThreePaneScaffoldTestTag),
        scaffoldDirective = scaffoldDirective,
        scaffoldValue = scaffoldValue,
        paneOrder = paneOrder,
        windowInsets = windowInsets,
        secondaryPane = {
            AnimatedPane(
                modifier = Modifier.testTag(tag = "SecondaryPane")
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.secondary
                ) {}
            }
        },
        tertiaryPane = {
            AnimatedPane(
                modifier = Modifier.testTag(tag = "TertiaryPane")
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiary
                ) {}
            }
        }
    ) {
        AnimatedPane(
            modifier = Modifier.testTag(tag = "PrimaryPane")
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary
            ) {}
        }
    }
}
