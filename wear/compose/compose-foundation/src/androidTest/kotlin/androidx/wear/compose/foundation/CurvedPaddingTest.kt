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

class CurvedPaddingTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun padding_all_works() =
        check_padding_result(3.dp, 3.dp, 3.dp, 3.dp, CurvedModifier.padding(3.dp))

    @Test
    fun padding_angular_and_radial_works() =
        check_padding_result(
            outerPadding = 4.dp,
            innerPadding = 4.dp,
            beforePadding = 6.dp,
            afterPadding = 6.dp,
            CurvedModifier.padding(radial = 4.dp, angular = 6.dp)
        )

    @Test
    fun basic_padding_works() =
        check_padding_result(
            outerPadding = 3.dp,
            innerPadding = 4.dp,
            beforePadding = 5.dp,
            afterPadding = 6.dp,
            CurvedModifier.padding(outer = 3.dp, inner = 4.dp, before = 5.dp, after = 6.dp)
        )

    @Test
    fun nested_padding_works() =
        check_padding_result(
            11.dp,
            14.dp,
            18.dp,
            25.dp,
            CurvedModifier.padding(3.dp, 4.dp, 5.dp, 6.dp).padding(8.dp, 10.dp, 13.dp, 19.dp)
        )

    private fun check_padding_result(
        outerPadding: Dp,
        innerPadding: Dp,
        beforePadding: Dp,
        afterPadding: Dp,
        modifier: CurvedModifier
    ) {

        val paddedCapturedInfo = CapturedInfo()
        val componentCapturedInfo = CapturedInfo()

        val componentThickness = 10.dp
        val componentSweepDegrees = 90f

        var outerPaddingPx = 0f
        var innerPaddingPx = 0f
        var beforePaddingPx = 0f
        var afterPaddingPx = 0f
        var componentThicknessPx = 0f

        rule.setContent {
            with(LocalDensity.current) {
                outerPaddingPx = outerPadding.toPx()
                innerPaddingPx = innerPadding.toPx()
                beforePaddingPx = beforePadding.toPx()
                afterPaddingPx = afterPadding.toPx()
                componentThicknessPx = componentThickness.toPx()
            }
            CurvedLayout {
                curvedRow(
                    modifier =
                        CurvedModifier.spy(paddedCapturedInfo)
                            .then(modifier)
                            .spy(componentCapturedInfo)
                            .size(
                                sweepDegrees = componentSweepDegrees,
                                thickness = componentThickness
                            )
                ) {}
            }
        }

        rule.runOnIdle {
            val measureRadius = componentCapturedInfo.lastLayoutInfo!!.measureRadius
            val beforePaddingAsAngle = beforePaddingPx / measureRadius
            val afterPaddingAsAngle = afterPaddingPx / measureRadius

            // Check sizes.
            val paddingAsAngle = (beforePaddingAsAngle + afterPaddingAsAngle).toDegrees()
            paddedCapturedInfo.checkDimensions(
                componentSweepDegrees + paddingAsAngle,
                componentThicknessPx + outerPaddingPx + innerPaddingPx
            )
            componentCapturedInfo.checkDimensions(componentSweepDegrees, componentThicknessPx)

            // Check its position.
            componentCapturedInfo.checkPositionRelativeTo(
                paddedCapturedInfo,
                expectedAngularPositionDegrees = beforePaddingAsAngle,
                expectedRadialPositionPx = outerPaddingPx,
            )
        }
    }
}
