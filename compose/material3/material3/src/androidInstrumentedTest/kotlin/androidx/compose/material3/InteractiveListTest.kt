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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class InteractiveListTest {
    @get:Rule val rule = createComposeRule()
    val ListTag = "list"
    val LeadingTag = "leading"
    val TrailingTag = "trailing"
    val OverlineTag = "overline"
    val SupportingTag = "supporting"
    val ContentTag = "content"

    @Test
    fun clickableListItem_intrinsicSize() {
        val contentSize = 200.dp
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.height(IntrinsicSize.Min)) {
                ClickableListItem(
                    modifier = Modifier.fillMaxHeight().testTag(ListTag),
                    content = { Box(Modifier.size(contentSize)) },
                    leadingContent = { Box(Modifier.fillMaxHeight().testTag(LeadingTag)) },
                    trailingContent = { Box(Modifier.fillMaxHeight().testTag(TrailingTag)) },
                    onClick = {},
                )
            }
        }

        val expectedHeight = contentSize + InteractiveListTopPadding + InteractiveListBottomPadding
        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertHeightIsEqualTo(expectedHeight)
        rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).assertHeightIsEqualTo(contentSize)
        rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).assertHeightIsEqualTo(contentSize)
    }

    @Test
    fun clickableListItem_multipleItems_intrinsicSize() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.width(300.dp).height(IntrinsicSize.Min)) {
                // 2 identical list items. Leading content leaves small space
                // for content, so it has to wrap.
                ClickableListItem(
                    modifier = Modifier.testTag("ListItem1"),
                    leadingContent = { Box(Modifier.width(240.dp)) },
                    content = { Text("A B C D E F G H") },
                    onClick = {},
                )
                ClickableListItem(
                    modifier = Modifier.testTag("ListItem2"),
                    leadingContent = { Box(Modifier.width(240.dp)) },
                    content = { Text("A B C D E F G H") },
                    onClick = {},
                )
            }
        }

        val item1Height =
            rule
                .onNodeWithTag("ListItem1", useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
                .height
        rule.onNodeWithTag("ListItem2", useUnmergedTree = true).assertHeightIsEqualTo(item1Height)
    }

    @Test
    fun clickableListItem_verticalAlignmentCenter_positioning() {
        val height = InteractiveListVerticalAlignmentBreakpoint - 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            ClickableListItem(
                modifier = Modifier.height(height),
                leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                content = { Text("Content", Modifier.testTag(ContentTag)) },
                onClick = {},
            )
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val contentBounds =
            rule.onNodeWithTag(ContentTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.left.assertIsEqualTo(InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo((rule.rootHeight() - leadingBounds.height) / 2)

        val mainContentX = leadingBounds.right + InteractiveListInternalSpacing
        val mainContentHeight =
            overlineBounds.height + supportingBounds.height + contentBounds.height
        overlineBounds.top.assertIsEqualTo((rule.rootHeight() - mainContentHeight) / 2)
        overlineBounds.left.assertIsEqualTo(mainContentX)
        supportingBounds.left.assertIsEqualTo(mainContentX)
        contentBounds.left.assertIsEqualTo(mainContentX)

        trailingNodeBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo((rule.rootHeight() - trailingNodeBounds.height) / 2)
    }

    @Test
    fun clickableListItem_verticalAlignmentTop_positioning() {
        val height = InteractiveListVerticalAlignmentBreakpoint + 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            ClickableListItem(
                modifier = Modifier.height(height),
                leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                content = { Text("Content", Modifier.testTag(ContentTag)) },
                onClick = {},
            )
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val contentBounds =
            rule.onNodeWithTag(ContentTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.left.assertIsEqualTo(InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo(InteractiveListTopPadding)

        val mainContentX = leadingBounds.right + InteractiveListInternalSpacing
        overlineBounds.top.assertIsEqualTo(InteractiveListTopPadding)
        overlineBounds.left.assertIsEqualTo(mainContentX)
        supportingBounds.left.assertIsEqualTo(mainContentX)
        contentBounds.left.assertIsEqualTo(mainContentX)

        trailingNodeBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo(InteractiveListTopPadding)
    }

    @Test
    fun clickableListItem_verticalAlignmentCenter_positioning_rtl() {
        val height = InteractiveListVerticalAlignmentBreakpoint - 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClickableListItem(
                    modifier = Modifier.height(height),
                    leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                    trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                    overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                    supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                    content = { Text("Content", Modifier.testTag(ContentTag)) },
                    onClick = {},
                )
            }
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val contentBounds =
            rule.onNodeWithTag(ContentTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo((rule.rootHeight() - leadingBounds.height) / 2)

        val mainContentRightX = leadingBounds.left - InteractiveListInternalSpacing
        val mainContentHeight =
            overlineBounds.height + supportingBounds.height + contentBounds.height
        overlineBounds.top.assertIsEqualTo((rule.rootHeight() - mainContentHeight) / 2)
        overlineBounds.right.assertIsEqualTo(mainContentRightX)
        supportingBounds.right.assertIsEqualTo(mainContentRightX)
        contentBounds.right.assertIsEqualTo(mainContentRightX)

        trailingNodeBounds.left.assertIsEqualTo(InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo((rule.rootHeight() - trailingNodeBounds.height) / 2)
    }

    @Test
    fun clickableListItem_verticalAlignmentTop_positioning_rtl() {
        val height = InteractiveListVerticalAlignmentBreakpoint + 10.dp
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClickableListItem(
                    modifier = Modifier.height(height),
                    leadingContent = { Box(Modifier.testTag(LeadingTag).size(48.dp)) },
                    trailingContent = { Box(Modifier.testTag(TrailingTag).size(48.dp)) },
                    overlineContent = { Text("Overline", Modifier.testTag(OverlineTag)) },
                    supportingContent = { Text("Supporting", Modifier.testTag(SupportingTag)) },
                    content = { Text("Content", Modifier.testTag(ContentTag)) },
                    onClick = {},
                )
            }
        }

        val leadingBounds =
            rule.onNodeWithTag(LeadingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val overlineBounds =
            rule.onNodeWithTag(OverlineTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingBounds =
            rule.onNodeWithTag(SupportingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val contentBounds =
            rule.onNodeWithTag(ContentTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingNodeBounds =
            rule.onNodeWithTag(TrailingTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        leadingBounds.right.assertIsEqualTo(rule.rootWidth() - InteractiveListStartPadding)
        leadingBounds.top.assertIsEqualTo(InteractiveListTopPadding)

        val mainContentRightX = leadingBounds.left - InteractiveListInternalSpacing
        overlineBounds.top.assertIsEqualTo(InteractiveListTopPadding)
        overlineBounds.right.assertIsEqualTo(mainContentRightX)
        supportingBounds.right.assertIsEqualTo(mainContentRightX)
        contentBounds.right.assertIsEqualTo(mainContentRightX)

        trailingNodeBounds.left.assertIsEqualTo(InteractiveListEndPadding)
        trailingNodeBounds.top.assertIsEqualTo(InteractiveListTopPadding)
    }

    @Test
    fun clickableListItem_semantics() {
        var clicked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            ClickableListItem(
                modifier = Modifier.testTag(ListTag),
                content = { Text("Content") },
                onClick = { clicked = true },
            )
        }

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertHasClickAction()
        assertThat(clicked).isFalse()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()

        assertThat(clicked).isTrue()
    }

    @Test
    fun clickableListItem_longClick() {
        var clicked by mutableStateOf(false)
        var longClicked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            ClickableListItem(
                modifier = Modifier.testTag(ListTag),
                content = { Text("Content") },
                onClick = { clicked = true },
                onLongClick = { longClicked = true },
            )
        }

        rule
            .onNodeWithTag(ListTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        assertThat(clicked).isFalse()
        assertThat(longClicked).isFalse()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).performTouchInput { longClick() }
        rule.waitForIdle()

        assertThat(clicked).isFalse()
        assertThat(longClicked).isTrue()
    }

    @Test
    fun selectableListItem_semantics() {
        var selected by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            SelectableListItem(
                modifier = Modifier.testTag(ListTag),
                content = { Text("Content") },
                selected = selected,
                onClick = { selected = !selected },
            )
        }

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertIsSelectable()
        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertIsNotSelected()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertIsSelected()
    }

    @Test
    fun selectableListItem_longClick() {
        var selected by mutableStateOf(false)
        var longClicked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            SelectableListItem(
                modifier = Modifier.testTag(ListTag),
                content = { Text("Content") },
                selected = selected,
                onClick = { selected = !selected },
                onLongClick = { longClicked = true },
            )
        }

        rule
            .onNodeWithTag(ListTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        assertThat(selected).isFalse()
        assertThat(longClicked).isFalse()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).performTouchInput { longClick() }
        rule.waitForIdle()

        assertThat(selected).isFalse()
        assertThat(longClicked).isTrue()
    }

    @Test
    fun toggleableListItem_semantics() {
        var checked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            ToggleableListItem(
                modifier = Modifier.testTag(ListTag),
                content = { Text("Content") },
                checked = checked,
                onCheckedChange = { checked = it },
            )
        }

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertIsToggleable()
        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertIsOff()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).assertIsOn()
    }

    @Test
    fun toggleableListItem_longClick() {
        var checked by mutableStateOf(false)
        var longClicked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            ToggleableListItem(
                modifier = Modifier.testTag(ListTag),
                content = { Text("Content") },
                checked = checked,
                onCheckedChange = { checked = it },
                onLongClick = { longClicked = true },
            )
        }

        rule
            .onNodeWithTag(ListTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        assertThat(checked).isFalse()
        assertThat(longClicked).isFalse()

        rule.onNodeWithTag(ListTag, useUnmergedTree = true).performTouchInput { longClick() }
        rule.waitForIdle()

        assertThat(checked).isFalse()
        assertThat(longClicked).isTrue()
    }
}
