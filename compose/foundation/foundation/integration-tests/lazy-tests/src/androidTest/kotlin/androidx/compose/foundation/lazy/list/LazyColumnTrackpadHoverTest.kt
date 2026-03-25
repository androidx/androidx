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

package androidx.compose.foundation.lazy.list

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.pan
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class LazyColumnTrackpadHoverTest {
    @get:Rule val rule = createComposeRule()

    @Before
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun setUp() {
        assumeTrue(ComposeUiFlags.isTrackpadPanHoverFixEnabled)
    }

    val numberOfItemsInLazyColumn = 500

    // Dynamically retrieves the initial node under the mouse.
    @Test
    fun hoverStateLocationAfterScrollDown_topOfListItemHeightScroll_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        // Find all nodes with a TestTag, but exclude the "lazyList" container itself.
        // Note: this must be called again after pan since the items in the list might have changed.
        val initialNodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }
        val mousePointer = Offset(10f, itemHeightPixels / 2)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        // Find the node under the mouse
        val initialNodeUnderMouse =
            initialNodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val initialItemTagUnderMouse =
            initialNodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // Assert is the first item in the list
        assertThat(hoveredItems).contains(initialItemTagUnderMouse)

        // 1. Get the screen height in pixels
        val screenHeightPixels =
            rule.onNodeWithTag("lazyList").fetchSemanticsNode().boundsInRoot.height

        val rowsOnScreen = (screenHeightPixels / itemHeightPixels).toInt()

        rule.onNodeWithTag("lazyList").performTrackpadInput {
            pan(Offset(0f, -itemHeightPixels * rowsOnScreen * 200))
        }
        rule.waitForIdle()

        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // 3. Assert that the hover list matches what is ACTUALLY under the mouse
        assertThat(hoveredItems).doesNotContain(initialItemTagUnderMouse)
        assertThat(hoveredItems).contains(itemTagUnderMouse)
    }

    @Test
    fun hoverStateLocationAfterScrollDown_topOfListDoubleItemHeightScroll_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }
        val twoTimesItemHeightPixels = itemHeightPixels * 2
        val mousePointer = Offset(10f, itemHeightPixels / 2)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        // Assert is the first item in the list
        // This is hard coded to item_0 because we start at the top of the list. If you want to
        // see the dynamic version of this (although testing a different scroll change), see
        // hoverStateLocationAfterScrollDown_topOfListItemHeightScroll_changeInHoverState().
        assertThat(hoveredItems).contains("item_0")

        rule.onNodeWithTag("lazyList").performTrackpadInput {
            pan(Offset(0f, -twoTimesItemHeightPixels))
        }
        rule.waitForIdle()

        // 1. Find all nodes with a TestTag, but exclude the "lazyList" container itself
        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        // 2. Find the node under the mouse
        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // 3. Assert that the hover list matches what is ACTUALLY under the mouse
        assertThat(hoveredItems).doesNotContain("item_0")
        assertThat(hoveredItems).contains(itemTagUnderMouse)
    }

    @Test
    fun hoverState_slowTrackpadPan_updatesHoverDuringGesture() {
        val hoveredItemsHistory = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItemsHistory.add(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }
        val twoTimesItemHeightPixels = itemHeightPixels * 2
        val mousePointer = Offset(10f, itemHeightPixels / 2)

        // Hover over the first item in list (item_0)
        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        assertThat(hoveredItemsHistory).contains("item_0")

        // Perform a continuous pan scroll down by 2 items
        rule.onNodeWithTag("lazyList").performTrackpadInput {
            pan(
                curve = { time -> Offset(0f, -twoTimesItemHeightPixels * (time / 200f)) },
                durationMillis = 200,
            )
        }
        rule.waitForIdle()

        // Assert that we hovered over item_0, item_1, and finally item_2
        assertThat(hoveredItemsHistory).contains("item_0")
        assertThat(hoveredItemsHistory).contains("item_1")
        assertThat(hoveredItemsHistory).contains("item_2")
    }

    @Test
    fun hoverStateLocationAfterMultipleScrollsDown_topOfListItemHeightScroll_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }
        val mousePointer = Offset(10f, itemHeightPixels / 2)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        // Assert is the first item in the list (at Offset 10, 10).
        // This is hard coded to item_0 because we start at the top of the list. If you want to
        // see the dynamic version of this (although testing a different scroll change), see
        // hoverStateLocationAfterScrollDown_topOfListItemHeightScroll_changeInHoverState().
        assertThat(hoveredItems).contains("item_0")

        repeat(10) { // Scroll 10 times to force more movement
            rule.onNodeWithTag("lazyList").performTrackpadInput {
                pan(Offset(0f, -itemHeightPixels))
            }
            rule.waitForIdle()
        }

        rule.waitForIdle()

        // 1. Find all nodes with a TestTag, but exclude the "lazyList" container itself
        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        // 2. Find the node under the mouse
        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // 3. Assert that the hover list matches what is ACTUALLY under the mouse
        assertThat(hoveredItems).doesNotContain("item_0")
        assertThat(hoveredItems).contains(itemTagUnderMouse)
    }

    // Dynamically retrieves the initial node under the mouse.
    @Test
    fun hoverStateLocationAfterScrollUp_topOfListItemHeightScroll_noChangeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        // Find all nodes with a TestTag, but exclude the "lazyList" container itself.
        // Note: this must be called again after pan since the items in the list might have changed.
        val initialNodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }
        val mousePointer = Offset(10f, itemHeightPixels / 2)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        // Find the node under the mouse
        val initialNodeUnderMouse =
            initialNodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val initialItemTagUnderMouse =
            initialNodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // Assert is the first item in the list
        assertThat(hoveredItems).contains(initialItemTagUnderMouse)

        rule.onNodeWithTag("lazyList").performTrackpadInput { pan(Offset(0f, itemHeightPixels)) }
        rule.waitForIdle()

        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // 3. Assert that the hover list matches the original
        assertThat(hoveredItems).contains(initialItemTagUnderMouse)
        assertThat(hoveredItems).contains(itemTagUnderMouse)
        assertThat(initialItemTagUnderMouse == itemTagUnderMouse).isTrue()
    }

    // Dynamically retrieves the initial node under the mouse.
    @Test
    fun hoverStateLocationAfterScrollUp_middleOfListItemHeightScroll_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }

        rule.onNodeWithTag("lazyList").performScrollToIndex(200)

        val mousePointer = Offset(10f, itemHeightPixels * 1.5f)

        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        // Find all nodes with a TestTag, but exclude the "lazyList" container itself.
        // Note: this must be called again after pan since the items in the list might have changed.
        val initialNodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        // Find the node under the mouse
        val initialNodeUnderMouse: SemanticsNode? =
            initialNodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val initialTestTag = initialNodeUnderMouse?.config?.getOrNull(SemanticsProperties.TestTag)
        assertThat(hoveredItems).contains(initialTestTag)

        rule.onNodeWithTag("lazyList").performTrackpadInput { pan(Offset(0f, itemHeightPixels)) }
        rule.waitForIdle()

        // Find all nodes with a TestTag, but exclude the "lazyList" container itself
        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        // Find the node under the mouse
        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // 3. Assert that the hover list matches what is ACTUALLY under the mouse
        assertThat(hoveredItems).doesNotContain(initialTestTag)
        assertThat(hoveredItems).contains(itemTagUnderMouse)
    }

    @Test
    fun hoverStateLocationAfterScrollUp_middleOfListItemHeightMultiple100Scroll_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }

        rule.onNodeWithTag("lazyList").performScrollToIndex(200)

        val mousePointer = Offset(10f, itemHeightPixels * 1.5f)

        rule.onNodeWithTag("lazyList").performTrackpadInput { enter(mousePointer) }
        rule.waitForIdle()

        // Find all nodes with a TestTag, but exclude the "lazyList" container itself.
        // Note: this must be called again after pan since the items in the list might have changed.
        val initialNodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        // Find the node under the mouse
        val initialNodeUnderMouse: SemanticsNode? =
            initialNodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val initialTestTag = initialNodeUnderMouse?.config?.getOrNull(SemanticsProperties.TestTag)
        assertThat(hoveredItems).contains(initialTestTag)

        rule.onNodeWithTag("lazyList").performTrackpadInput {
            pan(Offset(0f, itemHeightPixels * 100))
        }
        rule.waitForIdle()

        // Find all nodes with a TestTag, but exclude the "lazyList" container itself
        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        // Find the node under the mouse
        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                // Use boundsInRoot to check if the point is inside the item
                node.boundsInRoot.contains(mousePointer)
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.get(SemanticsProperties.TestTag)

        // 3. Assert that the hover list matches what is ACTUALLY under the mouse
        assertThat(hoveredItems).doesNotContain(initialTestTag)
        assertThat(hoveredItems).contains(itemTagUnderMouse)
    }

    @Test
    fun hoverState_slowTrackpadPan_classificationDrops_correctHoverExit() {
        val hoveredItems = mutableListOf<String>()
        var view: View? = null

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        val interactionSources = (0..numberOfItemsInLazyColumn).map { MutableInteractionSource() }
        rule.setContent {
            view = LocalView.current
            interactionSources.forEachIndexed { index, interactionSource ->
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        hoveredItems.add(itemTags[index])
                    } else {
                        hoveredItems.remove(itemTags[index])
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .hoverable(interactionSources[index])
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .testTag(itemTags[index])
                    ) {
                        BasicText(
                            text = itemTags[index],
                            style = TextStyle(color = Color.White, fontSize = 16.sp),
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        // Get view location on screen
        val locationOnScreen = intArrayOf(0, 0)
        view!!.getLocationOnScreen(locationOnScreen)
        val viewX = locationOnScreen[0].toFloat()
        val viewY = locationOnScreen[1].toFloat()

        // 1. Hover enter over item_0 at local (10f, 20.dp)
        val localX = 10f
        val localY = with(rule.density) { 20.dp.toPx() }
        val hoverEnterEvent =
            MotionEvent.obtain(
                    /* downTime = */ 0L,
                    /* eventTime = */ 0L,
                    /* action = */ MotionEvent.ACTION_HOVER_ENTER,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_MOUSE
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + localX
                            y = viewY + localY
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_NONE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }
        rule.runOnIdle { view!!.dispatchGenericMotionEvent(hoverEnterEvent) }
        rule.waitForIdle()
        assertThat(hoveredItems).contains("item_0")

        // 2. Start trackpad pan: ACTION_DOWN (classification: SWIPE)
        val itemHeightPixels = with(rule.density) { 40.dp.toPx() }
        val startTime = 10L

        val panDownEvent =
            MotionEvent.obtain(
                    /* downTime = */ startTime,
                    /* eventTime = */ startTime,
                    /* action = */ MotionEvent.ACTION_DOWN,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + localX
                            y = viewY + localY
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE, 0f)
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE, 0f)
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }

        rule.runOnUiThread { view!!.dispatchTouchEvent(panDownEvent) }

        // 3. Pan move: ACTION_MOVE (classification: SWIPE), scroll down by half item height
        val scrollY1 = itemHeightPixels / 2
        val panMoveSwipeEvent =
            MotionEvent.obtain(
                    /* downTime = */ startTime,
                    /* eventTime = */ startTime + 500,
                    /* action = */ MotionEvent.ACTION_MOVE,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + localX
                            y = viewY + localY - scrollY1
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE, 0f)
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE, scrollY1)
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }

        rule.runOnUiThread { view!!.dispatchTouchEvent(panMoveSwipeEvent) }

        // 4. Pan move: ACTION_MOVE (classification: NONE), scroll down further by 1.7 item heights
        val scrollY2 = itemHeightPixels * 1.7f
        val panMoveNoneEvent =
            MotionEvent.obtain(
                    /* downTime = */ startTime,
                    /* eventTime = */ startTime + 1000,
                    /* action = */ MotionEvent.ACTION_MOVE,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + localX
                            y = viewY + localY - scrollY1 - scrollY2
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE, 0f)
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE, scrollY2)
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_NONE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }

        rule.runOnUiThread { view!!.dispatchTouchEvent(panMoveNoneEvent) }

        // 5. End pan: ACTION_UP (classification: NONE)
        val panUpEvent =
            MotionEvent.obtain(
                    /* downTime = */ startTime,
                    /* eventTime = */ startTime + 1500,
                    /* action = */ MotionEvent.ACTION_UP,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + localX
                            y = viewY + localY - scrollY1 - scrollY2
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE, 0f)
                            setAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE, 0f)
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_NONE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }

        rule.runOnUiThread { view!!.dispatchTouchEvent(panUpEvent) }

        rule.waitForIdle()

        // Dispatch hover enter after pan to simulate platform behavior
        val hoverEnterEventAfterPan =
            MotionEvent.obtain(
                    /* downTime = */ startTime,
                    /* eventTime = */ startTime + 1600,
                    /* action = */ MotionEvent.ACTION_HOVER_ENTER,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + localX
                            y = viewY + localY
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_NONE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }

        rule.runOnUiThread { view!!.dispatchGenericMotionEvent(hoverEnterEventAfterPan) }
        rule.waitForIdle()

        // Since it scrolled down by 2.2 item heights, item_0 and item_1 must be completely
        // scrolled off-screen.
        // Assert that item_0 is NO LONGER hovered.
        assertThat(hoveredItems).doesNotContain("item_0")
        assertThat(hoveredItems).doesNotContain("item_1")

        // Find what is actually under the mouse pointer local(10f, 50f) now.
        val nodes =
            rule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)
                        .and(
                            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "lazyList")
                                .not()
                        )
                )
                .fetchSemanticsNodes()

        val nodeUnderMouse =
            nodes.firstOrNull { node ->
                node.layoutInfo.isPlaced && node.boundsInRoot.contains(Offset(localX, localY))
            }

        val itemTagUnderMouse = nodeUnderMouse?.config?.getOrNull(SemanticsProperties.TestTag)
        assertThat(hoveredItems).contains(itemTagUnderMouse)

        // 6. Move hover to item_4 using hover move event
        val item4Node = rule.onNodeWithTag("item_4").fetchSemanticsNode()
        val item4Center = item4Node.boundsInRoot.center
        val item4LocalX = item4Center.x
        val item4LocalY = item4Center.y

        val hoverMoveEvent =
            MotionEvent.obtain(
                    /* downTime = */ startTime,
                    /* eventTime = */ startTime + 1700,
                    /* action = */ MotionEvent.ACTION_HOVER_MOVE,
                    /* pointerCount = */ 1,
                    /* pointerProperties = */ arrayOf(
                        PointerProperties().apply {
                            id = 0
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    ),
                    /* pointerCoords = */ arrayOf(
                        PointerCoords().apply {
                            x = viewX + item4LocalX
                            y = viewY + item4LocalY
                        }
                    ),
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ InputDevice.SOURCE_MOUSE,
                    /* displayId = */ 0,
                    /* flags = */ 0,
                    /* classification = */ MotionEvent.CLASSIFICATION_NONE,
                )!!
                .apply { offsetLocation(-viewX, -viewY) }

        rule.runOnUiThread { view!!.dispatchGenericMotionEvent(hoverMoveEvent) }
        rule.waitForIdle()

        assertThat(hoveredItems).contains("item_4")
        if (itemTagUnderMouse != "item_4") {
            assertThat(hoveredItems).doesNotContain(itemTagUnderMouse)
        }
    }
}
