/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.brush

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class TextureAnimationProgressHelperTest {

    @Test
    fun getAnimationDurationMillis_whenNoTextureLayers_returnsZeroDuration() {
        assertThat(TextureAnimationProgressHelper.getAnimationDurationMillis(BrushFamily()))
            .isEqualTo(0L)
    }

    @Test
    fun getAnimationDurationMillis_whenTextureLayersAreNotAnimated_returnsZeroDuration() {
        assertThat(
                TextureAnimationProgressHelper.getAnimationDurationMillis(
                    brushFamilyWithNonAnimatedTextureLayers
                )
            )
            .isEqualTo(0L)
    }

    @Test
    fun getAnimationDurationMillis_whenHasAnimation_returnsCorrectDuration() {
        assertThat(
                TextureAnimationProgressHelper.getAnimationDurationMillis(
                    animatedBrushFamily(12345L)
                )
            )
            .isEqualTo(12345L)
    }

    @Test
    fun calculateAnimationProgressFromDurationValue_whenSystemTimeZero_returnsZeroProgress() {
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    systemElapsedTimeMillis = 0L,
                    animationDurationMillis = 12345L,
                )
            )
            .isEqualTo(0F)
    }

    @Test
    fun calculateAnimationProgressFromDurationValue_whenAnimationDurationZero_returnsZeroProgress() {
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    systemElapsedTimeMillis = 12345L,
                    animationDurationMillis = 0L,
                )
            )
            .isEqualTo(0F)
    }

    @Test
    fun calculateAnimationProgressFromDurationValue_whenSystemTimeAndAnimationDurationNotZero_returnsCorrectProgress() {
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 5 days: multiple of 240ms animation duration
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(5),
                    animationDurationMillis = 240L,
                )
            )
            .isEqualTo(0F)
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 5 days + 24 milliseconds: 24ms into an animation cycle of 240ms
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(5) + 24,
                    animationDurationMillis = 240L,
                )
            )
            .isEqualTo(0.1F)
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 30 days: multiple of 240ms animation duration
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(30),
                    animationDurationMillis = 240L,
                )
            )
            .isEqualTo(0F)
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 30 days + 24 milliseconds: 24ms into an animation cycle of 240ms
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(30) + 24,
                    animationDurationMillis = 240L,
                )
            )
            .isEqualTo(0.1F)
    }

    @Test
    fun calculateAnimationProgressFromBrushFamily_whenSystemTimeZero_returnsZeroProgress() {
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    systemElapsedTimeMillis = 0L,
                    brushFamily = animatedBrushFamily(12345L),
                )
            )
            .isEqualTo(0F)
    }

    @Test
    fun calculateAnimationProgressFromBrushFamily_whenNoTextureLayers_returnsZeroProgress() {
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    systemElapsedTimeMillis = 12345L,
                    brushFamily = BrushFamily(),
                )
            )
            .isEqualTo(0F)
    }

    @Test
    fun calculateAnimationProgressFromBrushFamily_whenTextureLayersAreNotAnimated_returnsZeroProgress() {
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    systemElapsedTimeMillis = 12345L,
                    brushFamily = brushFamilyWithNonAnimatedTextureLayers,
                )
            )
            .isEqualTo(0F)
    }

    @Test
    fun calculateAnimationProgressFromBrushFamily_whenSystemTimeNotZero_returnsCorrectProgress() {
        val animatedBrushFamily = animatedBrushFamily(240L)

        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 5 days: multiple of 240ms animation duration
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(5),
                    brushFamily = animatedBrushFamily,
                )
            )
            .isEqualTo(0F)
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 5 days + 24 milliseconds: 24ms into an animation cycle of 240ms
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(5) + 24,
                    brushFamily = animatedBrushFamily,
                )
            )
            .isEqualTo(0.1F)
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 30 days: multiple of 240ms animation duration
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(30),
                    brushFamily = animatedBrushFamily,
                )
            )
            .isEqualTo(0F)
        assertThat(
                TextureAnimationProgressHelper.calculateAnimationProgress(
                    // 30 days + 24 milliseconds: 24ms into an animation cycle of 240ms
                    systemElapsedTimeMillis = TimeUnit.DAYS.toMillis(30) + 24,
                    brushFamily = animatedBrushFamily,
                )
            )
            .isEqualTo(0.1F)
    }

    private fun animatedBrushFamily(animationDurationMillis: Long) =
        BrushFamily(
            coats =
                listOf(
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        BrushPaint.TextureLayer(
                                            clientTextureId = "foo",
                                            sizeX = 128F,
                                            sizeY = 128F,
                                            animationFrames = 2,
                                            animationRows = 1,
                                            animationColumns = 2,
                                            animationDurationMillis = animationDurationMillis,
                                        )
                                    )
                            )
                    )
                )
        )

    private val brushFamilyWithNonAnimatedTextureLayers =
        BrushFamily(
            coats =
                listOf(
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        BrushPaint.TextureLayer(
                                            clientTextureId = "foo",
                                            sizeX = 128F,
                                            sizeY = 128F,
                                            animationFrames = 1,
                                            animationDurationMillis = 12345L,
                                        )
                                    )
                            )
                    )
                )
        )
}
