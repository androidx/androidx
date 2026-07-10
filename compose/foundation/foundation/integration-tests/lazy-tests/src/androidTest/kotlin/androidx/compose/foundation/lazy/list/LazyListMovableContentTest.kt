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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LazyListMovableContentTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun lazyListInsideMovableContent_movesItem_andPreservesScrollPosition() {
        val items = (0..20).map { it.toString() }
        var showLazyList by mutableStateOf(true)

        rule.setContent {
            val state = rememberLazyListState()

            val movableContent = remember {
                movableContentOf {
                    LazyColumn(Modifier.size(100.dp).testTag("LazyList"), state = state) {
                        items(items, key = { it }) { item ->
                            Box(Modifier.size(50.dp).testTag("Item$item"))
                        }
                    }
                }
            }

            Box {
                if (showLazyList) {
                    movableContent()
                } else {
                    Box(Modifier.size(100.dp).testTag("ParentBox")) { movableContent() }
                }
            }
        }

        rule.onNodeWithTag("LazyList").performScrollToIndex(20)
        rule.waitForIdle()
        rule.onNodeWithTag("Item20").assertIsDisplayed()

        // Move the LazyList into the nested Box
        showLazyList = false
        rule.waitForIdle()

        rule.onNodeWithTag("ParentBox").assertIsDisplayed()
        rule.onNodeWithTag("LazyList").assertIsDisplayed()
        rule.onNodeWithTag("Item20").assertIsDisplayed()
    }

    @Test
    fun lazyListInsideMovableContent_movesItemAcrossLists() {
        val items = (0..20).map { it.toString() }
        var switchContent by mutableStateOf(true)

        rule.setContent {
            val movableContent = remember {
                movableContentOf { Box(Modifier.size(100.dp)) { BasicText("Movable Box") } }
            }

            Column() {
                LazyColumn(Modifier.size(100.dp).testTag("LazyList1").background(Color.Red)) {
                    items(items, key = { it }) { item ->
                        Box(Modifier.size(100.dp)) { BasicText("First List=$item") }
                    }

                    if (switchContent) {
                        item { movableContent() }
                    }
                }

                LazyColumn(Modifier.size(100.dp).testTag("LazyList2").background(Color.Blue)) {
                    if (!switchContent) {
                        item { movableContent() }
                    }
                    items(items, key = { it }) { item ->
                        Box(Modifier.size(100.dp)) { BasicText("Second List=$item") }
                    }
                }
            }
        }

        rule.onNodeWithTag("LazyList1").performScrollToIndex(21)
        rule.waitForIdle()
        rule.onNodeWithText("Movable Box").assertIsDisplayed()
        val position = rule.onNodeWithText("Movable Box").fetchSemanticsNode().positionOnScreen

        // Move the LazyList into the nested Box
        switchContent = false
        rule.waitForIdle()

        rule.onNodeWithTag("LazyList2").performScrollToIndex(0)
        rule.waitForIdle()
        rule.onNodeWithText("Movable Box").assertIsDisplayed()
        val newPosition = rule.onNodeWithText("Movable Box").fetchSemanticsNode().positionOnScreen

        assertThat(position).isNotEqualTo(newPosition)
    }
}
