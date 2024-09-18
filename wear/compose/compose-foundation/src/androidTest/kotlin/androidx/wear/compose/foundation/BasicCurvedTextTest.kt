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

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BasicCurvedTextTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun modifying_curved_text_forces_curved_row_remeasure() {
        val capturedInfo = CapturedInfo()
        val text = mutableStateOf("Initial")
        rule.setContent {
            CurvedLayout {
                curvedRow(modifier = CurvedModifier.spy(capturedInfo)) {
                    basicCurvedText(text = text.value, style = CurvedTextStyle(fontSize = 14.sp))
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
                    curvedRow(modifier = CurvedModifier.testTag(TEST_TAG)) {
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
        rule.onNodeWithTag(TEST_TAG).assertWidthIsEqualTo(92.5f.dp)
    }

    @Test
    fun letter_spacing_increases_size() {
        val TAG1 = TEST_TAG + "1"
        val TAG2 = TEST_TAG + "2"
        rule.setContent {
            Box {
                repeat(2) {
                    val tag = if (it == 0) TAG1 else TAG2
                    val style =
                        if (it == 0) CurvedTextStyle(fontSize = 24.sp)
                        else CurvedTextStyle(fontSize = 24.sp, letterSpacing = 0.5f.em)
                    CurvedLayout(modifier = Modifier.size(200.dp)) {
                        curvedRow(modifier = CurvedModifier.testTag(tag)) {
                            basicCurvedText(
                                // Use a small text so we can see its width increase, instead of it
                                // just wrapping around the circle.
                                text = "Text",
                                style = style,
                            )
                        }
                    }
                }
            }
        }

        val width1 = rule.onNodeWithTag(TAG1).fetchSemanticsNode().size.width
        val width2 = rule.onNodeWithTag(TAG2).fetchSemanticsNode().size.width
        // We added 0.5 em spacing, it should be much bigger
        assert(width2 > 1.4f * width1)
    }
}
