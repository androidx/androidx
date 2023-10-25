/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(JUnit4::class)
class PaneScaffoldDirectiveTest {
    @Test
    fun test_calculateStandardPaneScaffoldDirective_compactWidth() {
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                Posture()
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(16.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(16.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(0.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(16.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(16.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(0.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_mediumWidth() {
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(750.dp, 900.dp)),
                Posture()
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(0.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(0.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_expandedWidth() {
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(1200.dp, 800.dp)),
                Posture()
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(0.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_tabletop() {
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(isTabletop = true)
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(2)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(0.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(24.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_compactWidth() {
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
                Posture()
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(16.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(16.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(0.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(16.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(16.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(0.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_mediumWidth() {
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(750.dp, 900.dp)),
                Posture()
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(0.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_expandedWidth() {
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(1200.dp, 800.dp)),
                Posture()
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(0.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_tabletop() {
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(isTabletop = true)
            )
        )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(2)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateRightPadding(LayoutDirection.Ltr)
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.verticalSpacerSize).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateTopPadding()
        ).isEqualTo(24.dp)
        assertThat(
            scaffoldDirective.gutterSizes.contentPadding.calculateBottomPadding()
        ).isEqualTo(24.dp)
        assertThat(scaffoldDirective.gutterSizes.horizontalSpacerSize).isEqualTo(24.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_alwaysAvoidHinge() {
        val occludingHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
        )
        val allHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
            Rect(2F, 2F, 3F, 3F)
        )
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(
                    allVerticalHingeBounds = allHingeBounds,
                    occludingVerticalHingeBounds = occludingHingeBounds
                )
            ),
            HingePolicy.AlwaysAvoid
        )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(allHingeBounds)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_avoidOccludingHinge() {
        val occludingHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
        )
        val allHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
            Rect(2F, 2F, 3F, 3F)
        )
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(
                    allVerticalHingeBounds = allHingeBounds,
                    occludingVerticalHingeBounds = occludingHingeBounds
                )
            ),
            HingePolicy.AvoidOccluding
        )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(occludingHingeBounds)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_neverAvoidHinge() {
        val occludingHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
        )
        val allHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
            Rect(2F, 2F, 3F, 3F)
        )
        val scaffoldDirective = calculateStandardPaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(
                    allVerticalHingeBounds = allHingeBounds,
                    occludingVerticalHingeBounds = occludingHingeBounds
                )
            ),
            HingePolicy.NeverAvoid
        )

        assertThat(scaffoldDirective.excludedBounds).isEmpty()
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_alwaysAvoidHinge() {
        val occludingHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
        )
        val allHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
            Rect(2F, 2F, 3F, 3F)
        )
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(
                    allVerticalHingeBounds = allHingeBounds,
                    occludingVerticalHingeBounds = occludingHingeBounds
                )
            ),
            HingePolicy.AlwaysAvoid
        )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(allHingeBounds)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_avoidOccludingHinge() {
        val occludingHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
        )
        val allHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
            Rect(2F, 2F, 3F, 3F)
        )
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(
                    allVerticalHingeBounds = allHingeBounds,
                    occludingVerticalHingeBounds = occludingHingeBounds
                )
            ),
            HingePolicy.AvoidOccluding
        )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(occludingHingeBounds)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_neverAvoidHinge() {
        val occludingHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
        )
        val allHingeBounds = listOf(
            Rect(0F, 0F, 1F, 1F),
            Rect(1F, 1F, 2F, 2F),
            Rect(2F, 2F, 3F, 3F)
        )
        val scaffoldDirective = calculateDensePaneScaffoldDirective(
            WindowAdaptiveInfo(
                WindowSizeClass.calculateFromSize(DpSize(700.dp, 800.dp)),
                Posture(
                    allVerticalHingeBounds = allHingeBounds,
                    occludingVerticalHingeBounds = occludingHingeBounds
                )
            ),
            HingePolicy.NeverAvoid
        )

        assertThat(scaffoldDirective.excludedBounds).isEmpty()
    }
}
