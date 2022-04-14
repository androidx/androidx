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

package androidx.wear.compose.foundation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.sp
import androidx.test.filters.FlakyTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BasicCurvedTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @FlakyTest(bugId = 227338558)
    fun modifying_curved_text_forces_curved_row_remeasure() {
        val capturedInfo = CapturedInfo()
        val text = mutableStateOf("Initial")
        rule.setContent {
            CurvedLayout {
                curvedRow(modifier = CurvedModifier.spy(capturedInfo)) {
                    basicCurvedText(
                        text = text.value,
                        style = CurvedTextStyle(fontSize = 14.sp)
                    )
                }
            }
        }

        rule.runOnIdle {
            capturedInfo.reset()
            text.value = "New Value"
        }

        rule.runOnIdle {
            // TODO(b/219885899): Investigate why we need the extra passes.
            assertEquals(CapturedInfo(2, 3, 3), capturedInfo)
        }
    }
}
