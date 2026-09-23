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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
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
            CurvedModifier.padding(radial = 4.dp, angular = 6.dp),
        )

    @Test
    fun basic_padding_works() =
        check_padding_result(
            outerPadding = 3.dp,
            innerPadding = 4.dp,
            beforePadding = 5.dp,
            afterPadding = 6.dp,
            CurvedModifier.padding(outer = 3.dp, inner = 4.dp, before = 5.dp, after = 6.dp),
        )

    @Test
    fun nested_padding_works() =
        check_padding_result(
            11.dp,
            14.dp,
            18.dp,
            25.dp,
            CurvedModifier.padding(3.dp, 4.dp, 5.dp, 6.dp).padding(8.dp, 10.dp, 13.dp, 19.dp),
        )

    @Test
    fun arcPaddingValues_allParametersSpecified_constructsValuesWithAllParameters() {
        val outer = 3.dp
        val inner = 5.dp
        val before = 7.dp
        val after = 9.dp

        val paddingValues =
            ArcPaddingValues(outer = outer, inner = inner, before = before, after = after)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 3.dp, inner = 5.dp, before = 7.dp, after = 9.dp)
            )
    }

    @Test
    fun arcPaddingValues_all_constructsValuesWithUniformDimensions() {
        val all = 4.dp

        val paddingValues = ArcPaddingValues(all = all)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 4.dp, inner = 4.dp, before = 4.dp, after = 4.dp)
            )
    }

    @Test
    fun arcPaddingValues_radialAndAngular_constructsValuesWithRadialAndAngularDimensions() {
        val radial = 6.dp
        val angular = 8.dp

        val paddingValues = ArcPaddingValues(radial = radial, angular = angular)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 6.dp, inner = 6.dp, before = 8.dp, after = 8.dp)
            )
    }

    @Test
    fun arcPaddingValues_outerOnly_defaultsOtherDimensionsToZero() {
        val outer = 2.dp

        val paddingValues = ArcPaddingValues(outer = outer)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 2.dp, inner = 0.dp, before = 0.dp, after = 0.dp)
            )
    }

    @Test
    fun arcPaddingValues_innerOnly_defaultsOtherDimensionsToZero() {
        val inner = 3.dp

        val paddingValues = ArcPaddingValues(inner = inner)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 0.dp, inner = 3.dp, before = 0.dp, after = 0.dp)
            )
    }

    @Test
    fun arcPaddingValues_beforeOnly_defaultsOtherDimensionsToZero() {
        val before = 4.dp

        val paddingValues = ArcPaddingValues(before = before)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 0.dp, inner = 0.dp, before = 4.dp, after = 0.dp)
            )
    }

    @Test
    fun arcPaddingValues_afterOnly_defaultsOtherDimensionsToZero() {
        val after = 5.dp

        val paddingValues = ArcPaddingValues(after = after)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 0.dp, inner = 0.dp, before = 0.dp, after = 5.dp)
            )
    }

    @Test
    fun arcPaddingValues_radialOnly_defaultsAngularToZero() {
        val radial = 6.dp

        val paddingValues = ArcPaddingValues(radial = radial)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 6.dp, inner = 6.dp, before = 0.dp, after = 0.dp)
            )
    }

    @Test
    fun arcPaddingValues_angularOnly_defaultsRadialToZero() {
        val angular = 8.dp

        val paddingValues = ArcPaddingValues(angular = angular)

        assertThat(paddingValues)
            .isEqualTo(
                ArcPaddingValuesImpl(outer = 0.dp, inner = 0.dp, before = 8.dp, after = 8.dp)
            )
    }

    @Test
    fun arcPaddingValuesImpl_sameDimensions_areEqual() {
        val padding1 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val padding2 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)

        assertThat(padding1).isEqualTo(padding2)
    }

    @Test
    fun arcPaddingValuesImpl_sameInstance_areEqual() {
        val padding = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)

        val areEqual = padding == padding

        assertThat(areEqual).isTrue()
    }

    @Test
    fun arcPaddingValuesImpl_differentOuter_notEqual() {
        val padding1 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val padding2 = ArcPaddingValuesImpl(outer = 9.dp, inner = 2.dp, before = 3.dp, after = 4.dp)

        assertThat(padding1).isNotEqualTo(padding2)
    }

    @Test
    fun arcPaddingValuesImpl_differentInner_notEqual() {
        val padding1 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val padding2 = ArcPaddingValuesImpl(outer = 1.dp, inner = 9.dp, before = 3.dp, after = 4.dp)

        assertThat(padding1).isNotEqualTo(padding2)
    }

    @Test
    fun arcPaddingValuesImpl_differentBefore_notEqual() {
        val padding1 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val padding2 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 9.dp, after = 4.dp)

        assertThat(padding1).isNotEqualTo(padding2)
    }

    @Test
    fun arcPaddingValuesImpl_differentAfter_notEqual() {
        val padding1 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val padding2 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 9.dp)

        assertThat(padding1).isNotEqualTo(padding2)
    }

    @Test
    fun arcPaddingValuesImpl_differentType_notEqual() {
        val padding = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val otherObject =
            object : ArcPaddingValues {
                override fun calculateOuterPadding(radialDirection: CurvedDirection.Radial) = 1.dp

                override fun calculateInnerPadding(radialDirection: CurvedDirection.Radial) = 2.dp

                override fun calculateBeforePadding(
                    layoutDirection: LayoutDirection,
                    angularDirection: CurvedDirection.Angular,
                ) = 3.dp

                override fun calculateAfterPadding(
                    layoutDirection: LayoutDirection,
                    angularDirection: CurvedDirection.Angular,
                ) = 4.dp
            }

        assertThat(padding).isNotEqualTo(otherObject)
    }

    @Test
    fun arcPaddingValuesImpl_nullOther_notEqual() {
        val padding = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val otherObject: Any? = null

        assertThat(padding).isNotEqualTo(otherObject)
    }

    @Test
    fun arcPaddingValuesImpl_sameDimensions_sameHashCode() {
        val padding1 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)
        val padding2 = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)

        val hashCode1 = padding1.hashCode()
        val hashCode2 = padding2.hashCode()

        assertThat(hashCode1).isEqualTo(hashCode2)
    }

    @Test
    fun arcPaddingValuesImpl_toString_matchesExpectedFormat() {
        val padding = ArcPaddingValuesImpl(outer = 1.dp, inner = 2.dp, before = 3.dp, after = 4.dp)

        val stringResult = padding.toString()

        assertThat(stringResult)
            .isEqualTo(
                "ArcPaddingValuesImpl(outer=${1.dp}, inner=${2.dp}, before=${3.dp}, after=${4.dp})"
            )
    }

    @Test
    fun arcPaddingValuesImpl_calculateOuterPadding_returnsOuter() {
        val padding = ArcPaddingValuesImpl(outer = 5.dp, inner = 6.dp, before = 7.dp, after = 8.dp)

        assertThat(padding.calculateOuterPadding(CurvedDirection.Radial.OutsideIn)).isEqualTo(5.dp)
        assertThat(padding.calculateOuterPadding(CurvedDirection.Radial.InsideOut)).isEqualTo(5.dp)
    }

    @Test
    fun arcPaddingValuesImpl_calculateInnerPadding_returnsInner() {
        val padding = ArcPaddingValuesImpl(outer = 5.dp, inner = 6.dp, before = 7.dp, after = 8.dp)

        assertThat(padding.calculateInnerPadding(CurvedDirection.Radial.InsideOut)).isEqualTo(6.dp)
        assertThat(padding.calculateInnerPadding(CurvedDirection.Radial.OutsideIn)).isEqualTo(6.dp)
    }

    @Test
    fun arcPaddingValuesImpl_calculateBeforePadding_returnsBefore() {
        val padding = ArcPaddingValuesImpl(outer = 5.dp, inner = 6.dp, before = 7.dp, after = 8.dp)
        val angularDirections =
            listOf(
                CurvedDirection.Angular.Normal,
                CurvedDirection.Angular.Reversed,
                CurvedDirection.Angular.Clockwise,
                CurvedDirection.Angular.CounterClockwise,
            )
        val layoutDirections = listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)

        for (layoutDirection in layoutDirections) {
            for (angularDirection in angularDirections) {
                assertThat(padding.calculateBeforePadding(layoutDirection, angularDirection))
                    .isEqualTo(7.dp)
            }
        }
    }

    @Test
    fun arcPaddingValuesImpl_calculateAfterPadding_returnsAfter() {
        val padding = ArcPaddingValuesImpl(outer = 5.dp, inner = 6.dp, before = 7.dp, after = 8.dp)
        val angularDirections =
            listOf(
                CurvedDirection.Angular.Normal,
                CurvedDirection.Angular.Reversed,
                CurvedDirection.Angular.Clockwise,
                CurvedDirection.Angular.CounterClockwise,
            )
        val layoutDirections = listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)

        for (layoutDirection in layoutDirections) {
            for (angularDirection in angularDirections) {
                assertThat(padding.calculateAfterPadding(layoutDirection, angularDirection))
                    .isEqualTo(8.dp)
            }
        }
    }

    @Test
    fun curvedModifier_paddingRadialOnly_appliesRadialPaddingWithZeroAngular() {
        val radial = 4.dp

        val modifier = CurvedModifier.padding(radial = radial)

        check_padding_result(
            outerPadding = 4.dp,
            innerPadding = 4.dp,
            beforePadding = 0.dp,
            afterPadding = 0.dp,
            modifier = modifier,
        )
    }

    @Test
    fun curvedModifier_paddingAngularOnly_appliesAngularPaddingWithZeroRadial() {
        val angular = 6.dp

        val modifier = CurvedModifier.padding(angular = angular)

        check_padding_result(
            outerPadding = 0.dp,
            innerPadding = 0.dp,
            beforePadding = 6.dp,
            afterPadding = 6.dp,
            modifier = modifier,
        )
    }

    @Test
    fun curvedModifier_paddingAllZero_appliesZeroPadding() {
        val all = 0.dp

        val modifier = CurvedModifier.padding(all = all)

        check_padding_result(
            outerPadding = 0.dp,
            innerPadding = 0.dp,
            beforePadding = 0.dp,
            afterPadding = 0.dp,
            modifier = modifier,
        )
    }

    @Test
    fun curvedModifier_paddingWithArcPaddingValues_appliesSpecifiedDimensions() {
        val paddingValues =
            ArcPaddingValues(outer = 2.dp, inner = 4.dp, before = 6.dp, after = 8.dp)

        val modifier = CurvedModifier.padding(paddingValues = paddingValues)

        check_padding_result(
            outerPadding = 2.dp,
            innerPadding = 4.dp,
            beforePadding = 6.dp,
            afterPadding = 8.dp,
            modifier = modifier,
        )
    }

    @Test
    fun curvedModifier_paddingWithCustomArcPaddingValues_appliesLtrPaddingInLayoutPass() {
        val customPaddingValues =
            object : ArcPaddingValues {
                override fun calculateOuterPadding(radialDirection: CurvedDirection.Radial) = 5.dp

                override fun calculateInnerPadding(radialDirection: CurvedDirection.Radial) = 7.dp

                override fun calculateBeforePadding(
                    layoutDirection: LayoutDirection,
                    angularDirection: CurvedDirection.Angular,
                ) = if (layoutDirection == LayoutDirection.Ltr) 9.dp else 0.dp

                override fun calculateAfterPadding(
                    layoutDirection: LayoutDirection,
                    angularDirection: CurvedDirection.Angular,
                ) = if (layoutDirection == LayoutDirection.Ltr) 11.dp else 0.dp
            }

        val modifier = CurvedModifier.padding(paddingValues = customPaddingValues)

        check_padding_result(
            outerPadding = 5.dp,
            innerPadding = 7.dp,
            beforePadding = 9.dp,
            afterPadding = 11.dp,
            modifier = modifier,
            layoutDirection = LayoutDirection.Ltr,
        )
    }

    @Test
    fun curvedModifier_paddingWithCustomArcPaddingValues_appliesRtlPaddingInLayoutPass() {
        val customPaddingValues =
            object : ArcPaddingValues {
                override fun calculateOuterPadding(radialDirection: CurvedDirection.Radial) = 5.dp

                override fun calculateInnerPadding(radialDirection: CurvedDirection.Radial) = 7.dp

                override fun calculateBeforePadding(
                    layoutDirection: LayoutDirection,
                    angularDirection: CurvedDirection.Angular,
                ) = if (layoutDirection == LayoutDirection.Rtl) 15.dp else 0.dp

                override fun calculateAfterPadding(
                    layoutDirection: LayoutDirection,
                    angularDirection: CurvedDirection.Angular,
                ) = if (layoutDirection == LayoutDirection.Rtl) 20.dp else 0.dp
            }

        val modifier = CurvedModifier.padding(paddingValues = customPaddingValues)

        check_padding_result(
            outerPadding = 5.dp,
            innerPadding = 7.dp,
            beforePadding = 15.dp,
            afterPadding = 20.dp,
            modifier = modifier,
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    private fun check_padding_result(
        outerPadding: Dp,
        innerPadding: Dp,
        beforePadding: Dp,
        afterPadding: Dp,
        modifier: CurvedModifier,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
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
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CurvedLayout {
                    curvedRow(
                        modifier =
                            CurvedModifier.spy(paddedCapturedInfo)
                                .then(modifier)
                                .spy(componentCapturedInfo)
                                .size(
                                    sweepDegrees = componentSweepDegrees,
                                    thickness = componentThickness,
                                )
                    ) {}
                }
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
                componentThicknessPx + outerPaddingPx + innerPaddingPx,
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
