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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions.RequestFocus
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyListFocusMoveCompositionCountTest {

    @get:Rule
    val rule = createComposeRule()

    private val composedItems = mutableSetOf<Int>()

    @Test
    fun moveFocus() {
        // Arrange.
        val (rowSize, itemSize) = with(rule.density) { Pair(50.toDp(), 10.toDp()) }
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            LazyRow(Modifier.size(rowSize)) {
                items(100) { index ->
                    Box(
                        Modifier
                            .size(itemSize)
                            .testTag("$index")
                            .focusable()
                    )
                    SideEffect { composedItems.add(index) }
                }
            }
        }
        rule.onNodeWithTag("4").performSemanticsAction(RequestFocus)
        rule.runOnIdle { composedItems.clear() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Right) }

        // Assert
        rule.runOnIdle { assertThat(composedItems).containsExactly(5, 6) }
    }

    @Test
    fun moveFocus_nestedFocusable() {
        // Arrange.
        val (rowSize, itemSize) = with(rule.density) { Pair(50.toDp(), 10.toDp()) }
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            LazyRow(Modifier.size(rowSize)) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).focusable()) {
                        Box(Modifier.size(itemSize).focusable().testTag("$index"))
                    }
                    SideEffect { composedItems.add(index) }
                }
            }
        }
        rule.onNodeWithTag("4").performSemanticsAction(RequestFocus)
        rule.runOnIdle { composedItems.clear() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Right) }

        // Assert
        rule.runOnIdle { assertThat(composedItems).containsExactly(5, 6) }
    }

    @Test
    fun moveFocus_deeplyNestedFocusable() {
        // Arrange.
        val (rowSize, itemSize) = with(rule.density) { Pair(50.toDp(), 10.toDp()) }
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            LazyRow(Modifier.size(rowSize)) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).focusable()) {
                        Box(Modifier.size(itemSize).focusable()) {
                            Box(Modifier.size(itemSize).focusable().testTag("$index"))
                        }
                    }
                    SideEffect { composedItems.add(index) }
                }
            }
        }
        rule.onNodeWithTag("4").performSemanticsAction(RequestFocus)
        rule.runOnIdle { composedItems.clear() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Right) }

        // Assert
        rule.runOnIdle { assertThat(composedItems).containsExactly(5, 6) }
    }
}
