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

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class CurvedSizeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun proper_nested_sizes_work() = nested_size_test(60f, 30.dp, 30f, 20.dp)

    @Test
    fun inverted_nested_sizes_work() = nested_size_test(30f, 20.dp, 60f, 30.dp)

    @Test
    fun equal_nested_sizes_work() = nested_size_test(30f, 20.dp, 30f, 20.dp)

    private fun nested_size_test(
        angle: Float,
        thickness: Dp,
        innerAngle: Float,
        innerThickness: Dp
    ) {
        val capturedInfo = CapturedInfo()
        val innerCapturedInfo = CapturedInfo()
        var thicknessPx = 0f
        var innerThicknessPx = 0f
        rule.setContent {
            with(LocalDensity.current) {
                thicknessPx = thickness.toPx()
                innerThicknessPx = innerThickness.toPx()
            }
            CurvedLayout {
                curvedRow(
                    modifier = CurvedModifier
                        .spy(capturedInfo)
                        .size(angle, thickness)
                        .spy(innerCapturedInfo)
                        .size(innerAngle, innerThickness)
                ) { }
            }
        }

        rule.runOnIdle {
            capturedInfo.checkDimensions(angle, thicknessPx)
            innerCapturedInfo.checkParentDimensions(angle, thicknessPx)
            innerCapturedInfo.checkDimensions(innerAngle, innerThicknessPx)
            innerCapturedInfo.checkPositionOnParent(0f, 0f)
        }
    }
}
