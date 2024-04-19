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
import androidx.compose.ui.graphics.toComposeRect
import androidx.window.layout.FoldingFeature
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class CalculatePostureTest {
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

    @Test
    fun test_calculatePosture_separatingVerticalBounds() {
        val posture = calculatePosture(allHinges)

        assertThat(posture.separatingVerticalHingeBounds.size).isEqualTo(2)
        assertThat(
            posture.separatingVerticalHingeBounds[0]
        ).isEqualTo(mockHingeBounds2.toComposeRect())
        assertThat(
            posture.separatingVerticalHingeBounds[1]
        ).isEqualTo(mockHingeBounds3.toComposeRect())
    }

    @Test
    fun test_calculatePosture_separatingHorizontalBounds() {
        val posture = calculatePosture(allHinges)

        assertThat(posture.separatingHorizontalHingeBounds.size).isEqualTo(2)
        assertThat(
            posture.separatingHorizontalHingeBounds[0]
        ).isEqualTo(mockHingeBounds5.toComposeRect())
        assertThat(
            posture.separatingHorizontalHingeBounds[1]
        ).isEqualTo(mockHingeBounds6.toComposeRect())
    }

    @Test
    fun test_calculatePosture_occludingVerticalOBounds() {
        val posture = calculatePosture(allHinges)

        assertThat(posture.occludingVerticalHingeBounds.size).isEqualTo(2)
        assertThat(
            posture.occludingVerticalHingeBounds[0]
        ).isEqualTo(mockHingeBounds1.toComposeRect())
        assertThat(
            posture.occludingVerticalHingeBounds[1]
        ).isEqualTo(mockHingeBounds2.toComposeRect())
    }

    @Test
    fun test_calculatePosture_occludingHorizontalBounds() {
        val posture = calculatePosture(allHinges)

        assertThat(posture.occludingHorizontalHingeBounds.size).isEqualTo(2)
        assertThat(
            posture.occludingHorizontalHingeBounds[0]
        ).isEqualTo(mockHingeBounds4.toComposeRect())
        assertThat(
            posture.occludingHorizontalHingeBounds[1]
        ).isEqualTo(mockHingeBounds5.toComposeRect())
    }

    @Test
    fun test_calculatePosture_allVerticalBounds() {
        val posture = calculatePosture(allHinges)

        assertThat(posture.allVerticalHingeBounds.size).isEqualTo(3)
        assertThat(posture.allVerticalHingeBounds[0]).isEqualTo(mockHingeBounds1.toComposeRect())
        assertThat(posture.allVerticalHingeBounds[1]).isEqualTo(mockHingeBounds2.toComposeRect())
        assertThat(posture.allVerticalHingeBounds[2]).isEqualTo(mockHingeBounds3.toComposeRect())
    }

    @Test
    fun test_calculatePosture_allHorizontalBounds() {
        val posture = calculatePosture(allHinges)

        assertThat(posture.allHorizontalHingeBounds.size).isEqualTo(3)
        assertThat(posture.allHorizontalHingeBounds[0]).isEqualTo(mockHingeBounds4.toComposeRect())
        assertThat(posture.allHorizontalHingeBounds[1]).isEqualTo(mockHingeBounds5.toComposeRect())
        assertThat(posture.allHorizontalHingeBounds[2]).isEqualTo(mockHingeBounds6.toComposeRect())
    }

    companion object {
        private val mockHingeBounds1 = Rect(1, 1, 2, 2)
        private val mockHingeBounds2 = Rect(2, 2, 3, 3)
        private val mockHingeBounds3 = Rect(3, 3, 4, 4)
        private val mockHingeBounds4 = Rect(4, 4, 5, 5)
        private val mockHingeBounds5 = Rect(5, 5, 6, 6)
        private val mockHingeBounds6 = Rect(6, 6, 7, 7)
        private val allHinges = listOf(
            MockFoldingFeature(
                isSeparating = false,
                occlusionType = FoldingFeature.OcclusionType.FULL,
                bounds = mockHingeBounds1
            ),
            MockFoldingFeature(
                isSeparating = true,
                occlusionType = FoldingFeature.OcclusionType.FULL,
                bounds = mockHingeBounds2
            ),
            MockFoldingFeature(
                isSeparating = true,
                occlusionType = FoldingFeature.OcclusionType.NONE,
                bounds = mockHingeBounds3
            ),
            MockFoldingFeature(
                isSeparating = false,
                occlusionType = FoldingFeature.OcclusionType.FULL,
                orientation = FoldingFeature.Orientation.HORIZONTAL,
                bounds = mockHingeBounds4
            ),
            MockFoldingFeature(
                isSeparating = true,
                occlusionType = FoldingFeature.OcclusionType.FULL,
                orientation = FoldingFeature.Orientation.HORIZONTAL,
                bounds = mockHingeBounds5
            ),
            MockFoldingFeature(
                isSeparating = true,
                occlusionType = FoldingFeature.OcclusionType.NONE,
                orientation = FoldingFeature.Orientation.HORIZONTAL,
                bounds = mockHingeBounds6
            ),
        )
    }
}

internal class MockFoldingFeature(
    override val isSeparating: Boolean = false,
    override val occlusionType: FoldingFeature.OcclusionType = FoldingFeature.OcclusionType.NONE,
    override val orientation: FoldingFeature.Orientation = FoldingFeature.Orientation.VERTICAL,
    override val state: FoldingFeature.State = FoldingFeature.State.FLAT,
    override val bounds: Rect = Rect(0, 0, 1, 1)
) : FoldingFeature
