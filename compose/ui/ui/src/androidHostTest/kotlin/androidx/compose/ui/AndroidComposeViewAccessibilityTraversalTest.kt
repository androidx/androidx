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

package androidx.compose.ui

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidComposeViewAccessibilityTraversalTest {

    @get:Rule val rule = createComposeRule()

    private var originalSemanticsSortUnclippedFallbackEnabled = true

    @Before
    fun setUp() {
        originalSemanticsSortUnclippedFallbackEnabled =
            ComposeUiFlags.isSemanticsSortUnclippedFallbackEnabled
    }

    @After
    fun tearDown() {
        ComposeUiFlags.isSemanticsSortUnclippedFallbackEnabled =
            originalSemanticsSortUnclippedFallbackEnabled
    }

    @Test
    fun findViewByAccessibilityIdTraversal_doesNotCrash() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val view = View(activity)
        // With the fix applied, this should not crash (no IllegalArgumentException). It should
        // safely return null because no view has the accessibility id View.NO_ID. (Accessibility
        // view ids come from a static counter starting at 1, so a hard-coded positive id could
        // collide with this view's own id once earlier tests have advanced the counter.)
        val result = AndroidComposeView.findViewByAccessibilityIdTraversal(View.NO_ID, view)
        assertThat(result).isNull()
    }

    @Test
    fun testMergedDescendants_respectsTraversalIndex() {
        var androidComposeView: AndroidComposeView? = null
        val parentTag = "parent"
        val text1 = "first"
        val text2 = "second"
        val text3 = "third"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            SimpleRow(
                Modifier.semantics(mergeDescendants = true) { isTraversalGroup = true }
                    .testTag(parentTag)
            ) {
                SimpleText(text1, Modifier.semantics { traversalIndex = 1f })
                SimpleText(text3, Modifier.semantics { traversalIndex = 3f })
                SimpleText(text2, Modifier.semantics { traversalIndex = 2f })
            }
        }

        val delegate =
            ViewCompat.getAccessibilityDelegate(androidComposeView!!)
                as AndroidComposeViewAccessibilityDelegateCompat
        delegate.accessibilityForceEnabledForTesting = true

        val child1Id =
            rule.onNodeWithContentDescription(text1, useUnmergedTree = true).fetchSemanticsNode().id
        val child2Id =
            rule.onNodeWithContentDescription(text2, useUnmergedTree = true).fetchSemanticsNode().id
        val child3Id =
            rule.onNodeWithContentDescription(text3, useUnmergedTree = true).fetchSemanticsNode().id

        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        val parentId = rule.onNodeWithTag(parentTag).fetchSemanticsNode().id

        val parentInfo = provider.createAccessibilityNodeInfo(parentId)
        assertChildOrder(parentInfo, listOf(child1Id, child2Id, child3Id))
    }

    @Test
    fun testMergedDescendants_respectsTraversalIndex_complex() {
        var androidComposeView: AndroidComposeView? = null
        val parentTag = "parent"
        val groupTag = "nestedGroup"
        val text1 = "first"
        val text2 = "second"
        val text3 = "third"
        val text4 = "fourth"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            DynamicRow(
                Modifier.semantics(mergeDescendants = true) { isTraversalGroup = true }
                    .testTag(parentTag)
            ) {
                DynamicRow {
                    DynamicRow(Modifier.semantics { isTraversalGroup = true }.testTag(groupTag)) {
                        SimpleText(text2, Modifier.semantics { traversalIndex = 2f })
                        SimpleText(text1, Modifier.semantics { traversalIndex = 1f })
                    }
                }
                DynamicRow {
                    SimpleText(text4, Modifier.semantics { traversalIndex = 4f })
                    SimpleText(text3, Modifier.semantics { traversalIndex = 3f })
                }
            }
        }

        val delegate =
            ViewCompat.getAccessibilityDelegate(androidComposeView!!)
                as AndroidComposeViewAccessibilityDelegateCompat
        delegate.accessibilityForceEnabledForTesting = true

        val child1Id =
            rule.onNodeWithContentDescription(text1, useUnmergedTree = true).fetchSemanticsNode().id
        val child2Id =
            rule.onNodeWithContentDescription(text2, useUnmergedTree = true).fetchSemanticsNode().id
        val child3Id =
            rule.onNodeWithContentDescription(text3, useUnmergedTree = true).fetchSemanticsNode().id
        val child4Id =
            rule.onNodeWithContentDescription(text4, useUnmergedTree = true).fetchSemanticsNode().id
        val groupId = rule.onNodeWithTag(groupTag, useUnmergedTree = true).fetchSemanticsNode().id

        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        val parentId = rule.onNodeWithTag(parentTag).fetchSemanticsNode().id

        val parentInfo = provider.createAccessibilityNodeInfo(parentId)
        assertChildOrder(parentInfo, listOf(groupId, child3Id, child4Id))

        val groupInfo = provider.createAccessibilityNodeInfo(groupId)
        assertChildOrder(groupInfo, listOf(child2Id, child1Id))
    }

    @Test
    fun testPartiallyVisibleRow_visuallyClippedChildDoesNotSortBeforeTopRow() {
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val row1Text = "row1"
        val row1ButtonText = "row1_button"
        val row2Text = "row2"
        val row3Text = "row3"
        val row3ExpandText = "row3_expand"
        val row3CheckboxText = "row3_checkbox"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A clipped vertical scroll container (height = 100px) with three merging rows:
            // - row1 at y = -40..20 (partially clipped at top y = 0), containing:
            //   - row1Button (x = 70..90, y = -30..-10): 100% clipped above y = 0
            // - row2 at y = 30..60 (fully visible)
            // - row3 at y = 80..140 (partially clipped at bottom y = 100), containing:
            //   1) row3Expand (x = 40..60, y = 110..130): 100% clipped below y = 100
            //   2) row3Checkbox (x = 70..90, y = 95..135): partially visible (top 5px at 95..100)
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, -40), IntOffset(0, 30), IntOffset(0, 80)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 40f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                PlacedLayout(
                    width = 100,
                    height = 60,
                    childOffsets = listOf(IntOffset(70, 10)),
                    modifier = Modifier.mergingSemantics(row1Text),
                ) {
                    MergingBox(row1ButtonText, width = 20, height = 20)
                }
                MergingBox(row2Text, width = 100, height = 30)
                PlacedLayout(
                    width = 100,
                    height = 60,
                    childOffsets = listOf(IntOffset(40, 30), IntOffset(70, 15)),
                    modifier = Modifier.mergingSemantics(row3Text),
                ) {
                    MergingBox(row3ExpandText, width = 20, height = 20)
                    MergingBox(row3CheckboxText, width = 20, height = 40)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        // Unclipped sorting bounds within each merging row preserve both top-to-bottom row order
        // and left-to-right intra-row order across top-clipped and bottom-clipped straddling rows.
        assertTraversalOrder(
            delegate,
            idOf(row1Text),
            idOf(row1ButtonText),
            idOf(row2Text),
            idOf(row3Text),
            idOf(row3ExpandText),
            idOf(row3CheckboxText),
        )
    }

    @Test
    fun testNestedClippedScrollContainer_partiallyClippedRowDoesNotSortBeforeStickyHeader() {
        var androidComposeView: AndroidComposeView? = null
        val rootTag = "root"
        val stickyHeaderText = "sticky_header"
        val row1Text = "row1"
        val row1ButtonText = "row1_button"
        val row2Text = "row2"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A parent container with:
            // - stickyHeader at x = 50..100, y = 0..40
            // - A clipped scroll region below it at y = 40..140 (without isTraversalGroup = true),
            //   where row1 (x = 0..40) is scrolled up to y = 10..70 in window coordinates so its
            //   top 30px (y = 10..40) are clipped by the scroll region at y = 40.
            // Because outer non-merging containers use clipped boundsInWindow (y = 40..70 for
            // row1), row1 does not overlap stickyHeader (y = 0..40) and sorts after stickyHeader.
            PlacedLayout(
                width = 100,
                height = 140,
                childOffsets = listOf(IntOffset(50, 0), IntOffset(0, 40)),
                modifier = Modifier.semantics { isTraversalGroup = true }.testTag(rootTag),
            ) {
                MergingBox(stickyHeaderText, width = 50, height = 40)
                PlacedLayout(
                    width = 100,
                    height = 100,
                    childOffsets = listOf(IntOffset(0, -30), IntOffset(0, 40)),
                    modifier =
                        Modifier.clipToBounds().semantics {
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 30f }, maxValue = { 100f })
                        },
                ) {
                    PlacedLayout(
                        width = 40,
                        height = 60,
                        childOffsets = listOf(IntOffset(10, 5)),
                        modifier = Modifier.mergingSemantics(row1Text),
                    ) {
                        MergingBox(row1ButtonText, width = 20, height = 20)
                    }
                    MergingBox(row2Text, width = 40, height = 30)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(rootTag).fetchSemanticsNode().id)

        assertTraversalOrder(
            delegate,
            idOf(stickyHeaderText),
            idOf(row1Text),
            idOf(row1ButtonText),
            idOf(row2Text),
        )
    }

    @Test
    fun testMultiLinePartiallyVisibleRow_preservesTopToBottomAndLeftToRightChildOrder() {
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val rowText = "row"
        val topLeftText = "row_top_left"
        val topRightText = "row_top_right"
        val bottomText = "row_bottom"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A clipped vertical scroll container (height = 100px) with a partially visible
            // multi-line merging row at y = 80..160 (clipped at y = 100), containing:
            // - topLeft (x = 10..30, y = 85..98): visible
            // - topRight (x = 40..60, y = 92..112): partially clipped (y = 92..100 visible)
            // - bottom (x = 10..30, y = 115..135): 100% clipped below y = 100
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, 80)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                PlacedLayout(
                    width = 100,
                    height = 80,
                    childOffsets = listOf(IntOffset(10, 5), IntOffset(40, 12), IntOffset(10, 35)),
                    modifier = Modifier.mergingSemantics(rowText),
                ) {
                    MergingBox(topLeftText, width = 20, height = 13)
                    MergingBox(topRightText, width = 20, height = 20)
                    MergingBox(bottomText, width = 20, height = 20)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        assertTraversalOrder(
            delegate,
            idOf(rowText),
            idOf(topLeftText),
            idOf(topRightText),
            idOf(bottomText),
        )
    }

    @Test
    fun testTallPartiallyVisibleCard_preservesOrderAcrossMultipleOffscreenRows() {
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val cardText = "card"
        val visibleButtonText = "card_visible_button"
        val upperOffscreenText = "card_upper_offscreen"
        val lowerOffscreenText = "card_lower_offscreen"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A clipped vertical scroll container (height = 100px) with a tall merging card placed
            // at y = 40..190 (partially visible at y = 40..100), containing three child buttons:
            // - visibleButton at (x = 70, y = 50..70): fully visible
            // - upperOffscreen at (x = 70, y = 110..130): 100% clipped below y = 100 (right side)
            // - lowerOffscreen at (x = 10, y = 160..180): 100% clipped below y = 100 (left side)
            // Unclipped bounds within the merging card keep upperOffscreen (y = 110..130) and
            // lowerOffscreen (y = 160..180) in distinct vertical rows so upperOffscreen sorts
            // before lowerOffscreen despite having a larger x coordinate.
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, 40)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 20f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                PlacedLayout(
                    width = 100,
                    height = 150,
                    childOffsets = listOf(IntOffset(70, 10), IntOffset(70, 70), IntOffset(10, 120)),
                    modifier = Modifier.mergingSemantics(cardText),
                ) {
                    MergingBox(visibleButtonText, width = 20, height = 20)
                    MergingBox(upperOffscreenText, width = 20, height = 20)
                    MergingBox(lowerOffscreenText, width = 20, height = 20)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        assertTraversalOrder(
            delegate,
            idOf(cardText),
            idOf(visibleButtonText),
            idOf(upperOffscreenText),
            idOf(lowerOffscreenText),
        )
    }

    @Test
    fun testHorizontalRail_halfShownCard_clippedChildrenAreReadAfterTheirCard() {
        var androidComposeView: AndroidComposeView? = null
        val rootTag = "root"
        val cardAText = "cardA"
        val cardAButtonText = "cardA_button"
        val cardBText = "cardB"
        val cardBVisibleText = "cardB_visible"
        val cardBClippedText = "cardB_clipped"
        val belowText = "below"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A horizontally clipped rail (x = 0..100, y = 0..60) with two merging cards, plus a
            // control below the rail (y = 70..90):
            // - cardA (x = 0..40, fully visible) containing cardA_button (x = 10..30)
            // - cardB (x = 60..160, clipped at x = 100) containing:
            //   - cardB_visible (x = 70..90): visible
            //   - cardB_clipped (x = 120..140): 100% clipped right of x = 100
            PlacedLayout(
                width = 200,
                height = 100,
                childOffsets = listOf(IntOffset(0, 0), IntOffset(0, 70)),
                modifier = Modifier.semantics { isTraversalGroup = true }.testTag(rootTag),
            ) {
                PlacedLayout(
                    width = 100,
                    height = 60,
                    childOffsets = listOf(IntOffset(0, 0), IntOffset(60, 0)),
                    modifier =
                        Modifier.clipToBounds().semantics {
                            horizontalScrollAxisRange =
                                ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                        },
                ) {
                    PlacedLayout(
                        width = 40,
                        height = 60,
                        childOffsets = listOf(IntOffset(10, 20)),
                        modifier = Modifier.mergingSemantics(cardAText),
                    ) {
                        MergingBox(cardAButtonText, width = 20, height = 20)
                    }
                    PlacedLayout(
                        width = 100,
                        height = 60,
                        childOffsets = listOf(IntOffset(10, 20), IntOffset(60, 20)),
                        modifier = Modifier.mergingSemantics(cardBText),
                    ) {
                        MergingBox(cardBVisibleText, width = 20, height = 20)
                        MergingBox(cardBClippedText, width = 20, height = 20)
                    }
                }
                MergingBox(belowText, width = 40, height = 20)
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(rootTag).fetchSemanticsNode().id)

        // cardB is a clipped merging container, so its children follow it (sorted left to right
        // by unclipped bounds) instead of cardB_clipped sorting first at Rect.Zero.
        assertTraversalOrder(
            delegate,
            idOf(cardAText),
            idOf(cardAButtonText),
            idOf(cardBText),
            idOf(cardBVisibleText),
            idOf(cardBClippedText),
            idOf(belowText),
        )
    }

    @Test
    fun testHorizontalRail_rtl_halfShownCard_clippedChildrenAreReadRightToLeftAfterTheirCard() {
        var androidComposeView: AndroidComposeView? = null
        val rootTag = "root"
        val cardAText = "cardA"
        val cardAButtonText = "cardA_button"
        val cardBText = "cardB"
        val cardBVisibleText = "cardB_visible"
        val cardBClippedText = "cardB_clipped"
        val belowText = "below"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                // Mirror image of the LTR rail: the rail starts at its right edge (x = 100).
                // - cardA (x = 60..100, fully visible) containing cardA_button (x = 70..90)
                // - cardB (x = -40..60, clipped at x = 0) containing:
                //   - cardB_visible (x = 20..40): visible
                //   - cardB_clipped (x = -30..-10): 100% clipped left of x = 0
                PlacedLayout(
                    width = 200,
                    height = 100,
                    childOffsets = listOf(IntOffset(0, 0), IntOffset(0, 70)),
                    modifier = Modifier.semantics { isTraversalGroup = true }.testTag(rootTag),
                ) {
                    PlacedLayout(
                        width = 100,
                        height = 60,
                        childOffsets = listOf(IntOffset(60, 0), IntOffset(-40, 0)),
                        modifier =
                            Modifier.clipToBounds().semantics {
                                horizontalScrollAxisRange =
                                    ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                            },
                    ) {
                        PlacedLayout(
                            width = 40,
                            height = 60,
                            childOffsets = listOf(IntOffset(10, 20)),
                            modifier = Modifier.mergingSemantics(cardAText),
                        ) {
                            MergingBox(cardAButtonText, width = 20, height = 20)
                        }
                        PlacedLayout(
                            width = 100,
                            height = 60,
                            childOffsets = listOf(IntOffset(60, 20), IntOffset(10, 20)),
                            modifier = Modifier.mergingSemantics(cardBText),
                        ) {
                            MergingBox(cardBVisibleText, width = 20, height = 20)
                            MergingBox(cardBClippedText, width = 20, height = 20)
                        }
                    }
                    MergingBox(belowText, width = 40, height = 20)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(rootTag).fetchSemanticsNode().id)

        // Right to left: cardA (right edge) first, then cardB and its children, with the visible
        // child (right = 40) before the clipped one (right = -10).
        assertTraversalOrder(
            delegate,
            idOf(cardAText),
            idOf(cardAButtonText),
            idOf(cardBText),
            idOf(cardBVisibleText),
            idOf(cardBClippedText),
            idOf(belowText),
        )
    }

    @Test
    fun testClippedRow_explicitIsTraversalGroupTrue_sortsChildrenByUnclippedBounds() {
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val rowText = "row"
        val expandText = "row_expand"
        val checkboxText = "row_checkbox"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A clipped merging row that explicitly declares isTraversalGroup = true, containing:
            // - expand (x = 40..60, y = 110..130): 100% clipped below y = 100
            // - checkbox (x = 70..90, y = 95..135): partially visible (y = 95..100)
            // Unclipped subtree bounds keep expand (left) before checkbox (right) instead of
            // comparing checkbox's clipped y = 95..100 against expand's unclipped y = 110..130.
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, 80)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                PlacedLayout(
                    width = 100,
                    height = 60,
                    childOffsets = listOf(IntOffset(40, 30), IntOffset(70, 15)),
                    modifier =
                        Modifier.mergingSemantics(rowText).semantics { isTraversalGroup = true },
                ) {
                    MergingBox(expandText, width = 20, height = 20)
                    MergingBox(checkboxText, width = 20, height = 40)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        assertTraversalOrder(delegate, idOf(rowText), idOf(expandText), idOf(checkboxText))
    }

    @Test
    fun testClippedRow_explicitIsTraversalGroupFalse_optsOutOfGroupingAndKeepsFlatOrder() {
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val row1Text = "row1"
        val row2Text = "row2"
        val subRowText = "row2_unclipped_subrow"
        val subRowPrioritizedText = "subrow_prioritized_button"
        val row2PrioritizedText = "row2_prioritized_button"
        val row2ClippedButtonText = "row2_clipped_button"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A clipped vertical scroll container (y = 0..100) with:
            // - row1 (y = 0..30), fully visible
            // - row2 (y = 80..140, clipped at y = 100) with an explicit isTraversalGroup = false,
            //   containing:
            //   - subRow (x = 10..90, y = 82..96): 100% visible unclipped nested merging container
            //     holding subRowPrioritized (traversalIndex = -2f), verifying row2's opt-out does
            //     not force unclipped nested merging containers into implicit traversal groups.
            //   - row2Prioritized (x = 20..40, y = 110..130, traversalIndex = -1f): 100% clipped,
            //     verifying row2's opt-out flattens children so -1f sorts ahead of row1.
            //   - row2ClippedButton (x = 60..80, y = 110..130): 100% clipped with default index,
            //     falling back to unclipped bounds (y = 110..130) because clipped bounds are empty.
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, 0), IntOffset(0, 80)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                MergingBox(row1Text, width = 100, height = 30)
                PlacedLayout(
                    width = 100,
                    height = 60,
                    childOffsets = listOf(IntOffset(10, 2), IntOffset(20, 30), IntOffset(60, 30)),
                    modifier =
                        Modifier.mergingSemantics(row2Text).semantics { isTraversalGroup = false },
                ) {
                    PlacedLayout(
                        width = 80,
                        height = 14,
                        childOffsets = listOf(IntOffset(10, 2)),
                        modifier = Modifier.mergingSemantics(subRowText),
                    ) {
                        MergingBox(
                            subRowPrioritizedText,
                            width = 10,
                            height = 10,
                            modifier = Modifier.semantics { traversalIndex = -2f },
                        )
                    }
                    MergingBox(
                        row2PrioritizedText,
                        width = 20,
                        height = 20,
                        modifier = Modifier.semantics { traversalIndex = -1f },
                    )
                    MergingBox(row2ClippedButtonText, width = 20, height = 20)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        assertTraversalOrder(
            delegate,
            idOf(subRowPrioritizedText),
            idOf(row2PrioritizedText),
            idOf(row1Text),
            idOf(row2Text),
            idOf(subRowText),
            idOf(row2ClippedButtonText),
        )
    }

    @Test
    fun testClippedContainer_unclippedNestedMergingContainer_staysFlatWhileClippedNestedGroups() {
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val row1Text = "row1"
        val outerCardText = "outer_card"
        val unclippedSubRowText = "unclipped_subrow"
        val subRowPrioritizedText = "subrow_prioritized_button"
        val clippedSubRowText = "clipped_subrow"
        val clippedSubRowButtonText = "clipped_subrow_button"
        val outerClippedButtonText = "outer_clipped_button"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // A clipped vertical scroll container (y = 0..100) with:
            // - row1 (y = 0..20), fully visible
            // - outerCard (y = 30..150, clipped at y = 100), containing:
            //   - unclippedSubRow (x = 10..90, y = 40..70): 100% visible unclipped nested merging
            //     container holding subRowPrioritized (traversalIndex = -2f). Because
            //     unclippedSubRow is not clipped, it does not become an implicit traversal group
            //     and does not trap subRowPrioritized behind itself.
            //   - clippedSubRow (x = 10..90, y = 85..125): partially clipped nested merging
            //     container holding clippedSubRowButton (y = 105..120, traversalIndex = -1f).
            //     Because clippedSubRow is clipped, it becomes an implicit traversal group and
            //     keeps clippedSubRowButton grouped right after itself.
            //   - outerClippedButton (x = 10..30, y = 130..145): 100% clipped below y = 100,
            //     sorted after clippedSubRow by unclipped window bounds.
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, 0), IntOffset(0, 30)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                MergingBox(row1Text, width = 100, height = 20)
                PlacedLayout(
                    width = 100,
                    height = 120,
                    childOffsets = listOf(IntOffset(10, 10), IntOffset(10, 55), IntOffset(10, 100)),
                    modifier = Modifier.mergingSemantics(outerCardText),
                ) {
                    PlacedLayout(
                        width = 80,
                        height = 30,
                        childOffsets = listOf(IntOffset(10, 5)),
                        modifier = Modifier.mergingSemantics(unclippedSubRowText),
                    ) {
                        MergingBox(
                            subRowPrioritizedText,
                            width = 20,
                            height = 20,
                            modifier = Modifier.semantics { traversalIndex = -2f },
                        )
                    }
                    PlacedLayout(
                        width = 80,
                        height = 40,
                        childOffsets = listOf(IntOffset(10, 20)),
                        modifier = Modifier.mergingSemantics(clippedSubRowText),
                    ) {
                        MergingBox(
                            clippedSubRowButtonText,
                            width = 20,
                            height = 15,
                            modifier = Modifier.semantics { traversalIndex = -1f },
                        )
                    }
                    MergingBox(outerClippedButtonText, width = 20, height = 15)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        assertTraversalOrder(
            delegate,
            idOf(row1Text),
            idOf(outerCardText),
            idOf(subRowPrioritizedText),
            idOf(unclippedSubRowText),
            idOf(clippedSubRowText),
            idOf(clippedSubRowButtonText),
            idOf(outerClippedButtonText),
        )
    }

    @Test
    fun testUnclippedMergingContainer_preservesExistingTraversalAndInternalClipBehavior() {
        var androidComposeView: AndroidComposeView? = null
        val rootTag = "root"
        val outerCardText = "outer_card"
        val innerPrioritizedButtonText = "inner_prioritized_button"
        val innerTitleText = "inner_title"
        val innerClippedChildText = "inner_clipped_child"
        val innerDeferredButtonText = "inner_deferred_button"
        val outerMiddleButtonText = "outer_middle_button"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            // An unclipped merging container (outerCard at y = 0..140) and an outer sibling
            // (outerMiddleButton at y = 150..170, traversalIndex = 1f). Because outerCard is not
            // clipped, it does not become an implicit traversal group and does not disable
            // clipping for internal clipToBounds containers:
            // - innerPrioritizedButton (traversalIndex = -1f) sorts before outerCard (0f).
            // - innerTitle (x = 20..80, y = 10..30) sorts before innerClippedChild (clipped to
            //   x = 0..60, y = 40..90 by an internal clipToBounds container, even though its
            //   unclipped top is y = 10).
            // - outerMiddleButton (traversalIndex = 1f) sorts before innerDeferredButton (2f).
            PlacedLayout(
                width = 100,
                height = 180,
                childOffsets = listOf(IntOffset(0, 0), IntOffset(0, 150)),
                modifier = Modifier.semantics { isTraversalGroup = true }.testTag(rootTag),
            ) {
                PlacedLayout(
                    width = 100,
                    height = 140,
                    childOffsets =
                        listOf(
                            IntOffset(70, 10),
                            IntOffset(20, 10),
                            IntOffset(0, 40),
                            IntOffset(10, 110),
                        ),
                    modifier = Modifier.mergingSemantics(outerCardText),
                ) {
                    MergingBox(
                        innerPrioritizedButtonText,
                        width = 20,
                        height = 20,
                        modifier = Modifier.semantics { traversalIndex = -1f },
                    )
                    MergingBox(innerTitleText, width = 60, height = 20)
                    PlacedLayout(
                        width = 100,
                        height = 60,
                        // Unclipped window y = 10..90; clipped window y = 40..90
                        childOffsets = listOf(IntOffset(0, -30)),
                        modifier = Modifier.clipToBounds(),
                    ) {
                        MergingBox(innerClippedChildText, width = 60, height = 80)
                    }
                    MergingBox(
                        innerDeferredButtonText,
                        width = 20,
                        height = 20,
                        modifier = Modifier.semantics { traversalIndex = 2f },
                    )
                }
                MergingBox(
                    outerMiddleButtonText,
                    width = 40,
                    height = 20,
                    modifier = Modifier.semantics { traversalIndex = 1f },
                )
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(rootTag).fetchSemanticsNode().id)

        assertTraversalOrder(
            delegate,
            idOf(innerPrioritizedButtonText),
            idOf(outerCardText),
            idOf(innerTitleText),
            idOf(innerClippedChildText),
            idOf(outerMiddleButtonText),
            idOf(innerDeferredButtonText),
        )
    }

    @Test
    fun testPartiallyVisibleRow_visuallyClippedChild_fallbackDisabled_sortsBeforeVisibleContent() {
        ComposeUiFlags.isSemanticsSortUnclippedFallbackEnabled = false
        var androidComposeView: AndroidComposeView? = null
        val listTag = "list"
        val row1Text = "row1"
        val row2Text = "row2"
        val row2ButtonText = "row2_button"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            PlacedLayout(
                width = 100,
                height = 100,
                childOffsets = listOf(IntOffset(0, 10), IntOffset(0, 80)),
                modifier =
                    Modifier.clipToBounds()
                        .semantics {
                            isTraversalGroup = true
                            verticalScrollAxisRange =
                                ScrollAxisRange(value = { 0f }, maxValue = { 100f })
                        }
                        .testTag(listTag),
            ) {
                MergingBox(row1Text, width = 100, height = 40)
                PlacedLayout(
                    width = 100,
                    height = 60,
                    childOffsets = listOf(IntOffset(70, 30)),
                    modifier = Modifier.mergingSemantics(row2Text),
                ) {
                    MergingBox(row2ButtonText, width = 20, height = 20)
                }
            }
        }

        val delegate = forceEnabledAccessibilityDelegate(androidComposeView!!)
        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        provider.createAccessibilityNodeInfo(rule.onNodeWithTag(listTag).fetchSemanticsNode().id)

        // Without the fix, row2Button has Rect.Zero bounds and sorts before row1.
        assertTraversalOrder(delegate, idOf(row2ButtonText), idOf(row1Text), idOf(row2Text))
    }

    @Composable
    private fun DynamicRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            var width = 0
            var height = 0
            placeables.forEach {
                width += it.width
                height = maxOf(height, it.height)
            }
            layout(width, height) {
                var x = 0
                placeables.forEach {
                    it.place(x, 0)
                    x += it.width
                }
            }
        }
    }

    @Composable
    private fun SimpleRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(100, 100) {
                var x = 0
                placeables.forEach {
                    it.place(x, 0)
                    x += it.width
                }
            }
        }
    }

    @Composable
    private fun SimpleText(text: String, modifier: Modifier = Modifier) {
        Layout(content = {}, modifier = modifier.semantics { contentDescription = text }) { _, _ ->
            layout(10, 10) {}
        }
    }

    /**
     * A fixed-size layout that places its i-th child at `childOffsets[i]`. Children are measured
     * without constraints so they can overflow and be clipped by an ancestor.
     */
    @Composable
    private fun PlacedLayout(
        width: Int,
        height: Int,
        childOffsets: List<IntOffset>,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Layout(content, modifier) { measurables, _ ->
            val placeables = measurables.map { it.measure(Constraints()) }
            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.place(childOffsets[index])
                }
            }
        }
    }

    /** A merging, screen-reader-focusable node of a fixed size (e.g. a button or a checkbox). */
    @Composable
    private fun MergingBox(
        text: String,
        width: Int,
        height: Int,
        modifier: Modifier = Modifier,
    ) {
        Layout(content = {}, modifier = modifier.mergingSemantics(text)) { _, _ ->
            layout(width, height) {}
        }
    }

    private fun Modifier.mergingSemantics(text: String): Modifier =
        semantics(mergeDescendants = true) { contentDescription = text }

    private fun forceEnabledAccessibilityDelegate(
        view: AndroidComposeView
    ): AndroidComposeViewAccessibilityDelegateCompat =
        (ViewCompat.getAccessibilityDelegate(view) as AndroidComposeViewAccessibilityDelegateCompat)
            .also { it.accessibilityForceEnabledForTesting = true }

    private fun idOf(contentDescription: String): Int =
        rule
            .onNodeWithContentDescription(contentDescription, useUnmergedTree = true)
            .fetchSemanticsNode()
            .id

    /** Asserts that [ids] are the complete traversal order from start to finish. */
    private fun assertTraversalOrder(
        delegate: AndroidComposeViewAccessibilityDelegateCompat,
        vararg ids: Int,
    ) {
        assertThat(delegate.idToBeforeMap.size).isEqualTo(ids.size - 1)
        assertThat(delegate.idToAfterMap.size).isEqualTo(ids.size - 1)
        assertThat(delegate.idToAfterMap.getOrDefault(ids.first(), -1)).isEqualTo(-1)
        for (i in 0 until ids.lastIndex) {
            assertThat(delegate.idToBeforeMap.getOrDefault(ids[i], -1)).isEqualTo(ids[i + 1])
            assertThat(delegate.idToAfterMap.getOrDefault(ids[i + 1], -1)).isEqualTo(ids[i])
        }
        assertThat(delegate.idToBeforeMap.getOrDefault(ids.last(), -1)).isEqualTo(-1)
    }

    private fun assertChildOrder(parentInfo: AccessibilityNodeInfoCompat?, expectedIds: List<Int>) {
        assertThat(parentInfo).isNotNull()
        assertThat(parentInfo!!.childCount).isEqualTo(expectedIds.size)
        for (i in expectedIds.indices) {
            val child = parentInfo.getChild(i)
            assertThat(child).isNotNull()
            assertThat(getVirtualViewId(child!!)).isEqualTo(expectedIds[i])
        }
    }

    private fun getVirtualViewId(nodeInfo: AccessibilityNodeInfoCompat): Int {
        val info = nodeInfo.unwrap()
        val field = info.javaClass.getDeclaredField("mSourceNodeId")
        field.isAccessible = true
        val sourceNodeId = field.get(info) as Long
        return (sourceNodeId shr 32).toInt()
    }
}
