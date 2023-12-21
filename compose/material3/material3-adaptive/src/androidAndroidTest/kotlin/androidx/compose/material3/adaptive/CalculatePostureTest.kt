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

import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class CalculatePostureTest {
    @Test
    fun test_calculatePosture_hasOneVerticalHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.VERTICAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL)
            )
        )

        assertThat(posture.hasVerticalHinge).isTrue()
    }

    @Test
    fun test_calculatePosture_hasNoVerticalHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL)
            )
        )

        assertThat(posture.hasVerticalHinge).isFalse()
    }

    @Test
    fun test_calculatePosture_hasMultipleVerticalHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.VERTICAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.VERTICAL),
            )
        )

        assertThat(posture.hasVerticalHinge).isTrue()
    }

    @Test
    fun test_calculatePosture_hasOneSeparatingHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(isSeparating = true),
                MockFoldingFeature(isSeparating = false),
                MockFoldingFeature(isSeparating = false),
            )
        )

        assertThat(posture.hasSeparatingHinge).isTrue()
    }

    @Test
    fun test_calculatePosture_hasNoSeparatingHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(isSeparating = false),
                MockFoldingFeature(isSeparating = false),
            )
        )

        assertThat(posture.hasSeparatingHinge).isFalse()
    }

    @Test
    fun test_calculatePosture_hasMultipleSeparatingHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(isSeparating = true),
                MockFoldingFeature(isSeparating = false),
                MockFoldingFeature(isSeparating = true),
            )
        )

        assertThat(posture.hasSeparatingHinge).isTrue()
    }

    @Test
    fun test_calculatePosture_isTableTop_noSeparating() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(
                    isSeparating = false,
                    orientation = FoldingFeature.Orientation.HORIZONTAL,
                    state = FoldingFeature.State.HALF_OPENED
                ),
            )
        )

        assertThat(posture.isTabletop).isTrue()
    }

    @Test
    fun test_calculatePosture_isTableTop_separating() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(
                    isSeparating = true,
                    orientation = FoldingFeature.Orientation.HORIZONTAL,
                    state = FoldingFeature.State.HALF_OPENED
                ),
            )
        )

        assertThat(posture.isTabletop).isTrue()
    }

    @Test
    fun test_calculatePosture_isNotTableTop_verticalHinge() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(
                    isSeparating = false,
                    orientation = FoldingFeature.Orientation.VERTICAL,
                    state = FoldingFeature.State.HALF_OPENED
                ),
            )
        )

        assertThat(posture.isTabletop).isFalse()
    }

    @Test
    fun test_calculatePosture_isNotTableTop_flat() {
        val posture = calculatePosture(
            listOf(
                MockFoldingFeature(
                    isSeparating = false,
                    orientation = FoldingFeature.Orientation.HORIZONTAL,
                    state = FoldingFeature.State.FLAT
                ),
            )
        )

        assertThat(posture.isTabletop).isFalse()
    }
}

internal class MockFoldingFeature(
    override val isSeparating: Boolean = false,
    override val occlusionType: FoldingFeature.OcclusionType = FoldingFeature.OcclusionType.NONE,
    override val orientation: FoldingFeature.Orientation = FoldingFeature.Orientation.VERTICAL,
    override val state: FoldingFeature.State = FoldingFeature.State.FLAT,
    override val bounds: Rect = Rect(0, 0, 1, 1)
) : FoldingFeature
