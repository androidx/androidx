/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec.Companion.NoOpTransformationSpec
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpecImpl
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.TransformationVariableSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.responsiveTransformationSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ResponsiveTransformationSpecTest {
    @get:Rule val rule = createComposeRule()

    private val SPEC1 =
        ResponsiveTransformationSpecImpl(
            200.dp,
            minElementHeightFraction = 0.01f,
            maxElementHeightFraction = 0.02f,
            minTransitionAreaHeightFraction = 0.03f,
            maxTransitionAreaHeightFraction = 0.04f,
            CubicBezierEasing(0.015f, 0.025f, 0.035f, 0.045f),
            TransformationVariableSpec(
                topValue = 0.011f,
                bottomValue = 0.021f,
                transformationZoneEnterFraction = 0.031f,
                transformationZoneExitFraction = 0.041f,
            ),
            TransformationVariableSpec(
                topValue = 0.012f,
                bottomValue = 0.022f,
                transformationZoneEnterFraction = 0.032f,
                transformationZoneExitFraction = 0.042f,
            ),
            TransformationVariableSpec(
                topValue = 0.013f,
                bottomValue = 0.023f,
                transformationZoneEnterFraction = 0.033f,
                transformationZoneExitFraction = 0.043f,
            ),
        )

    private val SPEC2 =
        ResponsiveTransformationSpecImpl(
            220.dp,
            minElementHeightFraction = 0.1f,
            maxElementHeightFraction = 0.2f,
            minTransitionAreaHeightFraction = 0.3f,
            maxTransitionAreaHeightFraction = 0.4f,
            CubicBezierEasing(0.15f, 0.25f, 0.35f, 0.45f),
            TransformationVariableSpec(
                topValue = 0.11f,
                bottomValue = 0.21f,
                transformationZoneEnterFraction = 0.31f,
                transformationZoneExitFraction = 0.41f,
            ),
            TransformationVariableSpec(
                topValue = 0.12f,
                bottomValue = 0.22f,
                transformationZoneEnterFraction = 0.32f,
                transformationZoneExitFraction = 0.42f,
            ),
            TransformationVariableSpec(
                topValue = 0.13f,
                bottomValue = 0.23f,
                transformationZoneEnterFraction = 0.33f,
                transformationZoneExitFraction = 0.43f,
            ),
        )

    private val SPECS = listOf(SPEC1, SPEC2)

    @Test fun responsive_spec_coerced_to_min_screen_size() = check_responsive_spec(180.dp, SPEC1)

    @Test fun responsive_spec_for_min_screen_size() = check_responsive_spec(200.dp, SPEC1)

    @Test fun responsive_spec_for_max_screen_size() = check_responsive_spec(220.dp, SPEC2)

    @Test fun responsive_spec_coerced_to_max_screen_size() = check_responsive_spec(221.dp, SPEC2)

    @Test
    fun responsive_spec_for_mid_screen_size() {
        val spec = responsiveTransformationSpec(210.dp, SPECS)

        Assert.assertEquals(0.055f, spec.minElementHeightFraction, EPSILON)
        Assert.assertEquals(0.11f, spec.maxElementHeightFraction, EPSILON)
        Assert.assertEquals(0.165f, spec.minTransitionAreaHeightFraction, EPSILON)
        Assert.assertEquals(0.22f, spec.maxTransitionAreaHeightFraction, EPSILON)

        Assert.assertEquals(0.066f, spec.contentAlpha.topValue, EPSILON)
        Assert.assertEquals(0.121f, spec.contentAlpha.bottomValue, EPSILON)
        Assert.assertEquals(0.176f, spec.contentAlpha.transformationZoneEnterFraction, EPSILON)
        Assert.assertEquals(0.231f, spec.contentAlpha.transformationZoneExitFraction, EPSILON)
    }

    @Test(expected = IllegalArgumentException::class)
    fun responsive_with_no_spec() {
        responsiveTransformationSpec(200.dp, emptyList())
    }

    @Test
    fun remember_responsive_with_default_specs() {
        lateinit var spec: TransformationSpec
        rule.setContent { spec = rememberTransformationSpec() }
        @Suppress("UNUSED_VARIABLE") val readBack = spec
    }

    @Test
    fun responsive_transformation_spec_has_correct_values() {
        lateinit var spec: TransformationSpec
        rule.setContent { spec = rememberTransformationSpec() }

        // Anchor item should have original height.
        assertThat(
                spec.getTransformedHeight(
                    140,
                    TransformingLazyColumnItemScrollProgress(
                        topOffsetFraction = 0.418502f,
                        bottomOffsetFraction = 0.726872f,
                    ),
                )
            )
            .isEqualTo(140)

        // Item close to the edge should be scaled down.
        assertThat(
                spec.getTransformedHeight(
                    140,
                    TransformingLazyColumnItemScrollProgress(
                        topOffsetFraction = 0.918502f,
                        bottomOffsetFraction = 1.226872f,
                    ),
                )
            )
            .isNotEqualTo(140)

        // Offscreen item should be scaled down.
        assertThat(
                spec.getTransformedHeight(
                    140,
                    TransformingLazyColumnItemScrollProgress(
                        topOffsetFraction = 1.118502f,
                        bottomOffsetFraction = 1.426872f,
                    ),
                )
            )
            .isNotEqualTo(140)
    }

    @Test
    fun remember_responsive_with_reduced_motion() {
        lateinit var spec: TransformationSpec
        rule.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                spec = rememberTransformationSpec()
            }
        }
        assertThat(spec).isEqualTo(NoOpTransformationSpec)
    }

    @Test
    fun responsive_with_one_spec() {
        val specs1 = listOf(SPEC1)

        Assert.assertEquals(SPEC1, responsiveTransformationSpec(199.dp, specs1))
        Assert.assertEquals(SPEC1, responsiveTransformationSpec(200.dp, specs1))
        Assert.assertEquals(SPEC1, responsiveTransformationSpec(201.dp, specs1))
    }

    @Test
    fun responsive_with_three_specs() {
        fun makeSpec(
            screenSize: Dp,
            minElementHeight: Float,
            maxElementHeight: Float,
            maxTransitionArea: Float,
        ) =
            ResponsiveTransformationSpecImpl(
                screenSize,
                minElementHeightFraction = minElementHeight,
                maxElementHeightFraction = maxElementHeight,
                minTransitionAreaHeightFraction = 0.7f,
                maxTransitionAreaHeightFraction = maxTransitionArea,
                CubicBezierEasing(0.15f, 0.25f, 0.35f, 0.45f),
                TransformationVariableSpec(
                    topValue = 0.11f,
                    bottomValue = 0.21f,
                    transformationZoneEnterFraction = 0.31f,
                    transformationZoneExitFraction = 0.41f,
                ),
                TransformationVariableSpec(
                    topValue = 0.12f,
                    bottomValue = 0.22f,
                    transformationZoneEnterFraction = 0.32f,
                    transformationZoneExitFraction = 0.42f,
                ),
                TransformationVariableSpec(
                    topValue = 0.13f,
                    bottomValue = 0.23f,
                    transformationZoneEnterFraction = 0.33f,
                    transformationZoneExitFraction = 0.43f,
                ),
            )

        val specs3 =
            listOf(
                makeSpec(100.dp, 0.01f, 0.02f, 0.04f),
                makeSpec(200.dp, 0.1f, 0.2f, 0.4f),
                makeSpec(300.dp, 0.5f, 0.6f, 0.7f),
            )

        Assert.assertEquals(specs3.first(), responsiveTransformationSpec(100.dp, specs3))
        Assert.assertEquals(
            0.11f,
            responsiveTransformationSpec(150.dp, specs3).maxElementHeightFraction,
            EPSILON,
        )
        Assert.assertEquals(
            0.1f,
            responsiveTransformationSpec(200.dp, specs3).minElementHeightFraction,
            EPSILON,
        )
        Assert.assertEquals(
            0.55f,
            responsiveTransformationSpec(250.dp, specs3).maxTransitionAreaHeightFraction,
            EPSILON,
        )
        Assert.assertEquals(specs3.last(), responsiveTransformationSpec(300.dp, specs3))
    }

    private val EPSILON = 1e-5f

    private fun check_responsive_spec(
        screenSize: Dp,
        expectedSpec: ResponsiveTransformationSpecImpl,
    ) {
        val spec = responsiveTransformationSpec(screenSize, SPECS)
        Assert.assertEquals(expectedSpec, spec)
    }
}
