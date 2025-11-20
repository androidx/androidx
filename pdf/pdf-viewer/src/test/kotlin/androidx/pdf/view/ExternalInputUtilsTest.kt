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

package androidx.pdf.view

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExternalInputUtilsTest {

    @Test
    fun calculateGreaterZoom_whenAtZoomLevel_returnsNextZoomLevel() {
        val currentZoom = 2.0f
        val baselineZoom = 2.0f
        val expectedZoom = 2.20f
        assertThat(
                ExternalInputUtils.calculateGreaterZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MAX_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(expectedZoom)
    }

    @Test
    fun calculateGreaterZoom_whenBetweenZoomLevels_returnsNextZoomLevel() {
        val currentZoom = 1.001f
        val baselineZoom = 1.0f
        val expectedZoom = 1.10f
        assertThat(
                ExternalInputUtils.calculateGreaterZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MAX_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(expectedZoom)
    }

    @Test
    fun calculateGreaterZoom_whenTooCloseToNextLevel_skipsNextLevelAndReturnsFollowing() {
        // currentZoomLevel is less than 1% smaller than next level, so we treat it as equal and
        // skip it.
        val currentZoom = 1.091f
        val baselineZoom = 1.0f
        val expectedZoom = 1.25f
        assertThat(
                ExternalInputUtils.calculateGreaterZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MAX_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(expectedZoom)
    }

    @Test
    fun calculateGreaterZoom_whenAtMaxZoomLevel_returnsMaxAbsoluteZoom() {
        val currentZoom = 25.0f
        val baselineZoom = 1.0f
        assertThat(
                ExternalInputUtils.calculateGreaterZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MAX_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(MAX_ABSOLUTE_ZOOM)
    }

    @Test
    fun calculateGreaterZoom_whenAboveMaxZoomLevel_returnsMaxAbsoluteZoom() {
        val currentZoom = 26.0f
        val baselineZoom = 1.0f
        assertThat(
                ExternalInputUtils.calculateGreaterZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MAX_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(MAX_ABSOLUTE_ZOOM)
    }

    @Test
    fun calculateGreaterZoom_withEmptyZoomLevels_returnsMaxAbsoluteZoom() {
        val currentZoom = 1.5f
        assertThat(
                ExternalInputUtils.calculateGreaterZoom(
                    currentZoom,
                    1.0f,
                    emptyList(),
                    MAX_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(MAX_ABSOLUTE_ZOOM)
    }

    @Test
    fun calculateScroll_returnsScrollAmount() {
        val viewportHeight = 1000
        val expectedScroll = 50
        assertThat(ExternalInputUtils.calculateScroll(viewportHeight, SCROLL_FACTOR))
            .isEqualTo(expectedScroll)
    }

    @Test
    fun calculateScroll_withNonMultipleHeight_roundsDownResult() {
        val viewportHeight = 1009
        // 1019 / 20 = 50.45, round down to 50
        val expectedScroll = 50
        assertThat(ExternalInputUtils.calculateScroll(viewportHeight, SCROLL_FACTOR))
            .isEqualTo(expectedScroll)
    }

    @Test
    fun calculateScroll_withNonMultipleHeight_roundsUpResult() {
        val viewportHeight = 1019
        // 1019 / 20 = 50.95, round up to 51
        val expectedScroll = 51
        assertThat(ExternalInputUtils.calculateScroll(viewportHeight, SCROLL_FACTOR))
            .isEqualTo(expectedScroll)
    }

    @Test
    fun calculateScroll_withZeroHeight_returnsZero() {
        assertThat(ExternalInputUtils.calculateScroll(0, SCROLL_FACTOR)).isEqualTo(0)
    }

    @Test
    fun calculateSmallerZoom_whenAtZoomLevel_returnsPreviousZoomLevel() {
        val currentZoom = 2.0f
        val baselineZoom = 2.0f
        val expectedZoom = 1.8f
        assertThat(
                ExternalInputUtils.calculateSmallerZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MIN_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(expectedZoom)
    }

    @Test
    fun calculateSmallerZoom_whenBetweenZoomLevels_returnsPreviousZoomLevel() {
        val currentZoom = 0.95f
        val baselineZoom = 1.0f
        val expectedZoom = 0.9f
        assertThat(
                ExternalInputUtils.calculateSmallerZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MIN_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(expectedZoom)
    }

    @Test
    fun calculateSmallerZoom_whenTooCloseToPreviousLevel_skipsPreviousLevelAndReturnsFollowing() {
        // currentZoomLevel is less than 1% smaller than next level, so we treat it as equal and
        // skip it.
        val currentZoom = 1.009f
        val baselineZoom = 1.0f
        val expectedZoom = 0.9f
        assertThat(
                ExternalInputUtils.calculateSmallerZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MIN_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(expectedZoom)
    }

    @Test
    fun calculateSmallerZoom_whenAtMinZoomLevel_returnsMinAbsoluteZoom() {
        val currentZoom = 0.25f
        val baselineZoom = 1.0f
        assertThat(
                ExternalInputUtils.calculateSmallerZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MIN_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(MIN_ABSOLUTE_ZOOM)
    }

    @Test
    fun calculateSmallerZoom_whenBelowMinZoomLevel_returnsMinAbsoluteZoom() {
        val currentZoom = 0.2f
        val baselineZoom = 1.0f
        assertThat(
                ExternalInputUtils.calculateSmallerZoom(
                    currentZoom,
                    baselineZoom,
                    ZOOM_LEVELS,
                    MIN_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(MIN_ABSOLUTE_ZOOM)
    }

    @Test
    fun calculateSmallerZoom_withEmptyZoomLevels_returnsMinAbsoluteZoom() {
        val currentZoom = 1.5f
        assertThat(
                ExternalInputUtils.calculateSmallerZoom(
                    currentZoom,
                    1.0f,
                    emptyList(),
                    MIN_ABSOLUTE_ZOOM,
                )
            )
            .isEqualTo(MIN_ABSOLUTE_ZOOM)
    }

    private companion object {
        const val SCROLL_FACTOR = 20
        const val MAX_ABSOLUTE_ZOOM = 35.0f
        const val MIN_ABSOLUTE_ZOOM = 0.10f

        val ZOOM_LEVELS =
            listOf(
                0.25f,
                0.33f,
                0.50f,
                0.67f,
                0.75f,
                0.80f,
                0.90f,
                1.0f,
                1.10f,
                1.25f,
                1.50f,
                1.75f,
                2.0f,
                2.50f,
                3.0f,
                4.0f,
                5.0f,
                6.0f,
                7.0f,
                8.0f,
                10.0f,
                12.0f,
                16.0f,
                20.0f,
                25.0f,
            )
    }
}
