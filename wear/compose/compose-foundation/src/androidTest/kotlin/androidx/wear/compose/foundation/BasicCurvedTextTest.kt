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

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BasicCurvedTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
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
            assertEquals(CapturedInfo(2, 3, 1), capturedInfo)
        }
    }

    @Test
    fun curved_text_sized_appropriately() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                CurvedLayout(modifier = Modifier.size(200.dp)) {
                    curvedRow(
                        modifier = CurvedModifier.testTag(TEST_TAG)
                    ) {
                        basicCurvedText(
                            text = "Test text",
                            style = CurvedTextStyle(fontSize = 24.sp),
                        )
                    }
                }
            }
        }

        // This is a pre-calculated value. It was calculated for specific container size,
        // density and font.
        rule.onNodeWithTag(TEST_TAG).assertWidthIsEqualTo(93.dp)
    }
}
