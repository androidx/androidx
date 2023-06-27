/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FocusActionsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun tag(index: Int): String = "tag_$index"

    @Test
    fun requestFocus_focuses() {
        rule.setContent {
            Box(
                Modifier
                    .size(1.dp)
                    .testTag(tag(0))
                    .focusable()
            )
            Box(
                Modifier
                    .size(1.dp)
                    .testTag(tag(1))
                    .focusable()
            )
        }

        rule.onNodeWithTag(tag(0)).assertIsNotFocused()
        rule.onNodeWithTag(tag(0)).requestFocus()
        rule.onNodeWithTag(tag(0)).assertIsFocused()

        rule.onNodeWithTag(tag(1)).requestFocus()
        rule.onNodeWithTag(tag(1)).assertIsFocused()
        rule.onNodeWithTag(tag(0)).assertIsNotFocused()
    }
}