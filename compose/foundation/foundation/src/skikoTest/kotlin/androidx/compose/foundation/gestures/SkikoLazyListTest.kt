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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SkikoLazyListTest {
    // https://github.com/JetBrains/compose-multiplatform/issues/3559
    @Test
    fun textFields() = runComposeUiTest {
        val state by mutableStateOf(LazyListState())
        setContent {
            LazyColumn(state = state, modifier = Modifier.testTag("list").fillMaxSize()) {
                items(1000) {
                    BasicTextField(value = "Text", onValueChange = {}, enabled = it % 2 == 0)
                }
            }
        }

        onNodeWithTag("list").performTouchInput {
            swipe(center, Offset.Zero)
        }
        runOnIdle { assertTrue(state.firstVisibleItemIndex > 0) }
    }

    // reduced https://github.com/JetBrains/compose-multiplatform/issues/3559
    @Test
    fun dynamicModifiers() = runComposeUiTest {
        val state by mutableStateOf(LazyListState())
        setContent {
            LazyColumn(state = state, modifier = Modifier.testTag("list").fillMaxSize()) {
                items(1000) {
                    val enabled = if (it == 0) {
                        Modifier.pointerInput(Unit) {}
                    } else {
                        Modifier
                    }
                    Box(Modifier.size(100.dp).then(enabled).background(Color.Red))
                }
            }
        }

        onNodeWithTag("list").performTouchInput {
            swipe(center, Offset.Zero)
        }
        runOnIdle { assertTrue(state.firstVisibleItemIndex > 0) }
    }
}
