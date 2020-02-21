/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Model
import androidx.test.filters.MediumTest
import androidx.ui.core.TestTag
import androidx.ui.graphics.Color
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Model
private class State {
    var progress = 0f
}

@MediumTest
@RunWith(JUnit4::class)
class DeterminateProgressTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun determinateProgress_testSemantics() {
        val tag = "linear"
        val state = State()

        composeTestRule
            .setContent {
                TestTag(tag = tag) {
                    DeterminateProgressIndicator(progress = state.progress) {
                        ColoredRect(Color.Cyan, width = 50.dp, height = 50.dp)
                    }
                }
            }

        findByTag(tag)
            .assertValueEquals("0.0")

        composeTestRule.runOnUiThread {
            state.progress = 0.5f
        }

        findByTag(tag)
            .assertValueEquals("0.5")
    }
}