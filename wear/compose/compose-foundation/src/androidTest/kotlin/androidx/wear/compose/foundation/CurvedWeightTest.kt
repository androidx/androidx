/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class CurvedWeightTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun base_weight_size_test() {
        val capturedInfo = CapturedInfo()
        rule.setContent {
            CurvedLayout {
                // The parent row takes 90 degrees, one child takes 30 degrees, so there is 60
                // degrees left for the weighted element.
                curvedRow(modifier = CurvedModifier.angularSize(90f)) {
                    curvedRow(modifier = CurvedModifier
                        .weight(1f)
                        .spy(capturedInfo)
                    ) { }
                    curvedRow(modifier = CurvedModifier.angularSize(30f)) { }
                }
            }
        }

        rule.runOnIdle {
            capturedInfo.checkParentDimensions(expectedAngleDegrees = 60f)
        }
    }

    @Test
    fun distribute_weight_size_test() {
        val capturedInfo1 = CapturedInfo()
        val capturedInfo2 = CapturedInfo()
        rule.setContent {
            CurvedLayout {
                // The parent row takes 90 degrees, one child takes 30 degrees, so there is 60
                // degrees left to distribute between the 2 elements, since the weights are 1 and 2,
                // they get 20 and 40 degrees respectively
                curvedRow(modifier = CurvedModifier.angularSize(90f)) {
                    curvedRow(modifier = CurvedModifier
                        .weight(1f)
                        .spy(capturedInfo1)
                    ) { }
                    curvedRow(modifier = CurvedModifier.angularSize(30f)) { }
                    curvedRow(modifier = CurvedModifier
                        .weight(2f)
                        .spy(capturedInfo2)
                    ) { }
                }
            }
        }

        rule.runOnIdle {
            capturedInfo1.checkParentDimensions(expectedAngleDegrees = 20f)
            capturedInfo2.checkParentDimensions(expectedAngleDegrees = 40f)
        }
    }

    @Test
    fun weight_no_intrinsic_size_test() {
        val capturedInfo = CapturedInfo()
        rule.setContent {
            CurvedLayout {
                // The parent row has no specified size, so it will size according to the children.
                // The weighted children has no intrinsic width, and the second takes 30 degrees,
                // so the row will take 30 degrees.
                // There is no space left in the row, so the weighted child will take 0 degrees.
                curvedRow {
                    curvedRow(modifier = CurvedModifier
                        .weight(1f)
                        .spy(capturedInfo)
                    ) { }
                    curvedRow(modifier = CurvedModifier.angularSize(30f)) { }
                }
            }
        }

        rule.runOnIdle {
            capturedInfo.checkParentDimensions(expectedAngleDegrees = 0f)
        }
    }

    @Test
    fun weight_with_sized_content_test() {
        val capturedInfo = CapturedInfo()
        rule.setContent {
            CurvedLayout {
                // The parent row has no specified size, so it will size according to the children.
                // The weighted children requires 10 degrees, and the second takes 30 degrees,
                // so the row will take 40 degrees.
                // There is 10 degrees left in the row, so the weighted child will take 10 degrees.
                curvedRow {
                    curvedRow(modifier = CurvedModifier
                        .weight(1f)
                        .spy(capturedInfo)
                        .angularSize(10f)
                    ) { }
                    curvedRow(modifier = CurvedModifier.angularSize(30f)) { }
                }
            }
        }

        rule.runOnIdle {
            capturedInfo.checkParentDimensions(expectedAngleDegrees = 10f)
        }
    }
}