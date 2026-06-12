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

package androidx.compose.material3

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ScrollFieldTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun scrollField_initialState() {
        val itemCount = 10
        val initialIndex = 3
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = initialIndex)
            ScrollField(state = state)
        }

        assertThat(state.selectedOption).isEqualTo(initialIndex)
        rule.onNodeWithText("03").assertIsSelected()
    }

    @Test
    fun scrollField_selectOption() {
        val itemCount = 10
        lateinit var state: ScrollFieldState
        rule.setContent {
            state = rememberScrollFieldState(itemCount = itemCount, index = 0)
            ScrollField(state = state)
        }

        rule.onNodeWithText("01").performClick()
        rule.waitForIdle()

        assertThat(state.selectedOption).isEqualTo(1)
        rule.onNodeWithText("01").assertIsSelected()
    }
}
