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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TextFieldFocusTest {
    @Test
    fun shouldRequestFocusOnClick() = runComposeUiTest {
        setContent {
            Column {
                BasicTextField("", {}, modifier = Modifier.testTag("field1"))
                BasicTextField("", {}, modifier = Modifier.testTag("field2"))
            }
        }

        onNodeWithTag("field1").assertIsNotFocused()
        onNodeWithTag("field2").assertIsNotFocused()

        onNodeWithTag("field1").performClick()
        onNodeWithTag("field1").assertIsFocused()
        onNodeWithTag("field2").assertIsNotFocused()

        onNodeWithTag("field2").performClick()
        onNodeWithTag("field1").assertIsNotFocused()
        onNodeWithTag("field2").assertIsFocused()
    }

    // bug https://github.com/JetBrains/compose-multiplatform/issues/3526
    @Test
    fun shouldRequestFocusOnClickInLazyList() = runComposeUiTest {
        val size = 1000

        setContent {
            Column {
                LazyColumn(modifier = Modifier.size(100.dp).testTag("list")) {
                    items(size) {
                        val name = "field$it"
                        BasicTextField(name, {}, modifier = Modifier.testTag(name))
                    }
                }
            }
        }

        repeat(100) {
            val index = Random.nextInt(size)
            onNodeWithTag("list").performScrollToIndex(index)
            onNodeWithTag("field$index").performClick()
            onNodeWithTag("field$index").assertExists().assertIsFocused()
        }
    }
}