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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class LevitatedPaneScreenshotTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3_ADAPTIVE)

    @Test
    fun levitatedPane_asDialog_default() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue = PaneAdaptedValue.Levitated(Alignment.Center)
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "levitatedPane_asDialog_default")
    }

    @Test
    fun levitatedPane_asDialog_withScrim() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue =
                    PaneAdaptedValue.Levitated(
                        alignment = Alignment.Center,
                        scrim = { LevitatedPaneScrim() },
                    )
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "levitatedPane_asDialog_withScrim")
    }

    @Test
    fun levitatedPane_asDialog_withShape() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue = PaneAdaptedValue.Levitated(Alignment.Center),
                levitatedPaneModifier = { Modifier.clip(RoundedCornerShape(32.dp)) },
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "levitatedPane_asDialog_withShape")
    }

    @Test
    fun levitatedPane_asBottomSheet_default() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue = PaneAdaptedValue.Levitated(Alignment.BottomCenter),
                levitatedPaneModifier = { Modifier.preferredWidth(1f).preferredHeight(0.5f) },
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "levitatedPane_asBottomSheet_default")
    }

    @Test
    fun levitatedPane_asBottomSheet_withDragHandle() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue =
                    PaneAdaptedValue.Levitated(
                        alignment = Alignment.BottomCenter,
                        dragToResizeState =
                            rememberDragToResizeState(dockedEdge = DockedEdge.Bottom),
                    ),
                levitatedPaneModifier = { Modifier.preferredWidth(1f).preferredHeight(0.5f) },
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "levitatedPane_asBottomSheet_withDragHandle")
    }

    @Test
    fun levitatedPane_asBottomSheet_withShape() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue = PaneAdaptedValue.Levitated(Alignment.BottomCenter),
                levitatedPaneModifier = {
                    Modifier.preferredWidth(1f)
                        .preferredHeight(0.5f)
                        .clip(RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp))
                },
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "levitatedPane_asBottomSheet_withShape")
    }

    @Test
    fun levitatedPane_asBottomSheet_withDragHandleAndShape() {
        rule.setContent {
            ThreePaneScaffoldWithLevitatedPane(
                levitatedPaneValue =
                    PaneAdaptedValue.Levitated(
                        alignment = Alignment.BottomCenter,
                        dragToResizeState =
                            rememberDragToResizeState(dockedEdge = DockedEdge.Bottom),
                    ),
                levitatedPaneModifier = {
                    Modifier.preferredWidth(1f)
                        .preferredHeight(0.5f)
                        .clip(RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp))
                },
            )
        }

        rule
            .onNodeWithTag(ThreePaneScaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "levitatedPane_asBottomSheet_withDragHandleAndShape",
            )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ThreePaneScaffoldWithLevitatedPane(
    levitatedPaneValue: PaneAdaptedValue.Levitated,
    levitatedPaneModifier: PaneScaffoldScope.() -> Modifier = { Modifier },
    primaryContent: (@Composable ThreePaneScaffoldScope.() -> Unit) = {},
    secondaryContent: (@Composable ThreePaneScaffoldScope.() -> Unit) = {},
    tertiaryContent: (@Composable ThreePaneScaffoldScope.() -> Unit) = {},
) {
    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    val value =
        ThreePaneScaffoldValue(
            PaneAdaptedValue.Expanded,
            PaneAdaptedValue.Hidden,
            levitatedPaneValue,
        )
    ListDetailPaneScaffold(
        modifier = Modifier.fillMaxSize().testTag(ThreePaneScaffoldTestTag),
        directive = directive,
        value = value,
        listPane = {
            AnimatedPane(
                modifier =
                    Modifier.testTag(tag = "SecondaryPane")
                        .background(color = MaterialTheme.colorScheme.secondary)
            ) {
                secondaryContent()
            }
        },
        detailPane = {
            AnimatedPane(
                modifier =
                    Modifier.testTag(tag = "PrimaryPane")
                        .background(color = MaterialTheme.colorScheme.primary)
            ) {
                primaryContent()
            }
        },
        extraPane = {
            AnimatedPane(
                modifier =
                    levitatedPaneModifier()
                        .testTag(tag = "TertiaryPane")
                        .background(color = MaterialTheme.colorScheme.tertiary),
                dragToResizeHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                tertiaryContent()
            }
        },
    )
}
