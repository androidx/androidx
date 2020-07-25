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

package androidx.compose.foundation

import androidx.compose.runtime.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.preferredSize
import androidx.ui.semantics.AccessibilityRangeInfo
import androidx.ui.test.assertRangeInfoEquals
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnUiThread
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DeterminateProgressTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun determinateProgress_testSemantics() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        composeTestRule
            .setContent {
                Box(
                    Modifier
                        .testTag(tag)
                        .determinateProgressIndicator(progress.value)
                        .preferredSize(50.dp)
                        .background(color = Color.Cyan)
                )
            }

        onNodeWithTag(tag)
            .assertValueEquals("0 percent")
            .assertRangeInfoEquals(AccessibilityRangeInfo(0f, 0f..1f))

        runOnUiThread {
            progress.value = 0.005f
        }

        onNodeWithTag(tag)
            .assertValueEquals("1 percent")
            .assertRangeInfoEquals(AccessibilityRangeInfo(0.005f, 0f..1f))

        runOnUiThread {
            progress.value = 0.5f
        }

        onNodeWithTag(tag)
            .assertValueEquals("50 percent")
            .assertRangeInfoEquals(AccessibilityRangeInfo(0.5f, 0f..1f))
    }
}