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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class LazyColumnMouseHoverTest {
    @get:Rule val rule = createComposeRule()

    val numberOfItemsInLazyColumn = 500

    @Test
    fun hoverStateLocationAfterSingleVerySmallScrollDown_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        rule.setContent {
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val pointerEvent = awaitPointerEvent()
                                        if (pointerEvent.type == PointerEventType.Enter) {
                                            hoveredItems.add(itemTags[index])
                                        } else if (pointerEvent.type == PointerEventType.Exit) {
                                            hoveredItems.remove(itemTags[index])
                                        }
                                    }
                                }
                            }
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

        val mousePointer = Offset(10f, 10f)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performMouseInput { enter(mousePointer) }
        rule.waitForIdle()

        // Assert is the first item in the list (at Offset 10, 10).
        assertThat(hoveredItems).contains("item_0")

        rule.onNodeWithTag("lazyList").performMouseInput { scroll(10f) }
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
    fun hoverStateLocationAfterSingleSmallScrollDown_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        rule.setContent {
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val pointerEvent = awaitPointerEvent()
                                        if (pointerEvent.type == PointerEventType.Enter) {
                                            hoveredItems.add(itemTags[index])
                                        } else if (pointerEvent.type == PointerEventType.Exit) {
                                            hoveredItems.remove(itemTags[index])
                                        }
                                    }
                                }
                            }
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

        val mousePointer = Offset(10f, 10f)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performMouseInput { enter(mousePointer) }
        rule.waitForIdle()

        // Assert is the first item in the list (at Offset 10, 10).
        assertThat(hoveredItems).contains("item_0")

        rule.onNodeWithTag("lazyList").performMouseInput { scroll(100f) }
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
    fun hoverStateLocationAfterMultipleSmallScrollsDown_changeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        rule.setContent {
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val pointerEvent = awaitPointerEvent()
                                        if (pointerEvent.type == PointerEventType.Enter) {
                                            hoveredItems.add(itemTags[index])
                                        } else if (pointerEvent.type == PointerEventType.Exit) {
                                            hoveredItems.remove(itemTags[index])
                                        }
                                    }
                                }
                            }
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

        val mousePointer = Offset(10f, 10f)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performMouseInput { enter(mousePointer) }
        rule.waitForIdle()

        // Assert is the first item in the list (at Offset 10, 10).
        assertThat(hoveredItems).contains("item_0")

        repeat(10) { // Scroll 10 times to force more movement
            rule.onNodeWithTag("lazyList").performMouseInput { scroll(100f) }
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

    @Test
    fun hoverStateLocationAfterSingleVerySmallScrollUp_noChangeInHoverState() {
        val hoveredItems = mutableListOf<String>()

        val itemTags = (0..numberOfItemsInLazyColumn).map { "item_$it" }
        rule.setContent {
            LazyColumn(Modifier.fillMaxSize().testTag("lazyList")) {
                items(numberOfItemsInLazyColumn) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .padding(bottom = 5.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val pointerEvent = awaitPointerEvent()
                                        if (pointerEvent.type == PointerEventType.Enter) {
                                            hoveredItems.add(itemTags[index])
                                        } else if (pointerEvent.type == PointerEventType.Exit) {
                                            hoveredItems.remove(itemTags[index])
                                        }
                                    }
                                }
                            }
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

        val mousePointer = Offset(10f, 10f)

        // Hover over the first item in list
        rule.onNodeWithTag("lazyList").performMouseInput { enter(mousePointer) }
        rule.waitForIdle()

        // Assert is the first item in the list (at Offset 10, 10).
        assertThat(hoveredItems).contains("item_0")

        rule.onNodeWithTag("lazyList").performMouseInput { scroll(-10f) }
        rule.waitForIdle()

        // 3. Assert that the hover list has not changed
        assertThat(hoveredItems).contains("item_0")
    }
}
