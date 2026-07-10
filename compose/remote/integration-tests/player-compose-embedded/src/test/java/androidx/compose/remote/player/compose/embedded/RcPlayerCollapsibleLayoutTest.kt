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

@file:Suppress(
    "RestrictedApiAndroidX"
) // Referring to RemoteCollapsibleColumn, RemoteCollapsibleRow, background
@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for collapsible layouts: when children overflow the main axis, the lowest
 * `CollapsiblePriority` child collapses first (the decision reuses core's priority ordering — see
 * RcPlayerCollapsibleLayout), not simply the last child in document order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCollapsibleLayoutTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @Test
    fun collapsesLowestPriorityChildFirst() {
        // A 100dp-high collapsible column with three 40dp children: A (priority 10),
        // B (priority 1), C (priority 5). Only two fit. Core semantics keep the two
        // highest-priority children (A and C) and collapse B — document-order dropping
        // would instead keep A and B.
        playerRule.setRemoteContent {
            RemoteCollapsibleColumn(modifier = RemoteModifier.size(100.rdp, 100.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .priority(10f)
                            .size(100.rdp, 40.rdp)
                            .background(Color(0xFFAA0000))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "B".rs }
                            .priority(1f)
                            .size(100.rdp, 40.rdp)
                            .background(Color(0xFF00AA00))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "C".rs }
                            .priority(5f)
                            .size(100.rdp, 40.rdp)
                            .background(Color(0xFF0000AA))
                )
            }
        }
        playerRule.composeRule.mainClock.advanceTimeBy(100)

        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("C").assertIsDisplayed()
        // B has the lowest collapsible priority, so it is the one collapsed (measured but
        // never placed — it stays in the raw semantics tree, but is not displayed and not
        // hit-testable).
        playerRule.composeRule.onNodeWithContentDescription("B").assertIsNotDisplayed()

        val aBounds =
            playerRule.composeRule.onNodeWithContentDescription("A").getUnclippedBoundsInRoot()
        val cBounds =
            playerRule.composeRule.onNodeWithContentDescription("C").getUnclippedBoundsInRoot()
        assertThat(aBounds.top.value).isEqualTo(0f)
        // C compacts into B's slot, directly after A (at Y=40dp).
        assertThat(cBounds.top.value).isEqualTo(40f)
    }

    @Test
    fun keepsAllChildrenWhenTheyFit() {
        playerRule.setRemoteContent {
            RemoteCollapsibleColumn(modifier = RemoteModifier.size(100.rdp, 100.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .priority(2f)
                            .size(100.rdp, 40.rdp)
                            .background(Color(0xFFAA0000))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "B".rs }
                            .priority(1f)
                            .size(100.rdp, 40.rdp)
                            .background(Color(0xFF00AA00))
                )
            }
        }
        playerRule.composeRule.mainClock.advanceTimeBy(100)

        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("B").assertIsDisplayed()
    }

    /**
     * Row variant of the priority collapse, with placement assertions: survivors compact in
     * document order — C sits where B would have been, on the very first frame (no settle time).
     */
    @Test
    fun collapsibleRowCollapsesLowestPriorityAndCompactsPlacement() {
        playerRule.composeRule.mainClock.autoAdvance = false
        playerRule.setRemoteContent {
            RemoteCollapsibleRow(modifier = RemoteModifier.size(100.rdp, 100.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .priority(10f)
                            .size(40.rdp, 100.rdp)
                            .background(Color(0xFFAA0000))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "B".rs }
                            .priority(1f)
                            .size(40.rdp, 100.rdp)
                            .background(Color(0xFF00AA00))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "C".rs }
                            .priority(5f)
                            .size(40.rdp, 100.rdp)
                            .background(Color(0xFF0000AA))
                )
            }
        }

        // Assertions run against the first rendered frame.
        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("C").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("B").assertIsNotDisplayed()

        val aBounds =
            playerRule.composeRule.onNodeWithContentDescription("A").getUnclippedBoundsInRoot()
        val cBounds =
            playerRule.composeRule.onNodeWithContentDescription("C").getUnclippedBoundsInRoot()
        assertThat(aBounds.left.value).isEqualTo(0f)
        // C compacts into B's slot, directly after A — not at its document-order offset (80).
        assertThat(cBounds.left.value).isEqualTo(40f)
    }

    @Test
    fun singleContentInContainerWithSizeAndBackground_displaysContent() {
        playerRule.setRemoteContent {
            RemoteCollapsibleColumn(
                modifier = RemoteModifier.size(100.rdp, 100.rdp).background(Color.Red)
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .size(40.rdp, 40.rdp)
                )
            }
        }
        playerRule.composeRule.mainClock.advanceTimeBy(100)

        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
    }

    @Test
    fun contentBiggerThanContainerWithSizeAndBackground_collapsesContent() {
        // A 50dp-high container with two 30dp children (total 60dp > 50dp).
        // The second child exceeds available main-axis height and collapses.
        playerRule.setRemoteContent {
            RemoteCollapsibleColumn(
                modifier = RemoteModifier.size(100.rdp, 50.rdp).background(Color.Red)
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .size(100.rdp, 30.rdp)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "B".rs }
                            .size(100.rdp, 30.rdp)
                )
            }
        }
        playerRule.composeRule.mainClock.advanceTimeBy(100)

        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("B").assertIsNotDisplayed()
    }

    @Test
    fun spacedByRemoteDp_accountsForSpacingWhenCollapsing() {
        // Two 45dp-high children in a 100dp container with 20dp spacing between them
        // (total required height: 45 + 20 + 45 = 110dp > 100dp).
        // A has priority 10, B has priority 1 -> B collapses.
        playerRule.setRemoteContent {
            RemoteCollapsibleColumn(
                modifier = RemoteModifier.size(100.rdp, 100.rdp),
                verticalArrangement = RemoteArrangement.spacedBy(20.rdp),
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .priority(10f)
                            .size(100.rdp, 45.rdp)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "B".rs }
                            .priority(1f)
                            .size(100.rdp, 45.rdp)
                )
            }
        }
        playerRule.composeRule.mainClock.advanceTimeBy(100)

        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("B").assertIsNotDisplayed()
    }

    @Test
    fun spacedByRemoteFloat_accountsForSpacingWhenCollapsing() {
        // Two 45dp-high children in a 100dp container with 20px spacing via RemoteFloat
        // (total required height > 100dp).
        playerRule.setRemoteContent {
            RemoteCollapsibleColumn(
                modifier = RemoteModifier.size(100.rdp, 100.rdp),
                verticalArrangement = RemoteArrangement.spacedBy(20.rf),
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "A".rs }
                            .priority(10f)
                            .size(100.rdp, 45.rdp)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "B".rs }
                            .priority(1f)
                            .size(100.rdp, 45.rdp)
                )
            }
        }
        playerRule.composeRule.mainClock.advanceTimeBy(100)

        playerRule.composeRule.onNodeWithContentDescription("A").assertIsDisplayed()
        playerRule.composeRule.onNodeWithContentDescription("B").assertIsNotDisplayed()
    }
}
