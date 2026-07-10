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

import androidx.kruth.assertThat
import kotlin.test.Test

@OptIn(ExperimentalInkAnimationApi::class)
class TextureAnimationProgressHelperTest {

    @Test
    fun getAnimationDurationMillis_whenNoTextureLayers_returnsZeroDuration() {
        assertThat(TextureAnimationProgressHelper.getAnimationDurationMillis(BrushPaint()))
            .isEqualTo(0L)
    }

    @Test
    fun getAnimationDurationMillis_whenTextureLayersAreNotAnimated_returnsZeroDuration() {
        assertThat(
                TextureAnimationProgressHelper.getAnimationDurationMillis(
                    brushPaintWithNonAnimatedTextureLayers
                )
            )
            .isEqualTo(0L)
    }

    @Test
    fun getAnimationDurationMillis_whenHasAnimation_returnsCorrectDuration() {
        assertThat(
                TextureAnimationProgressHelper.getAnimationDurationMillis(
                    animatedBrushPaint(12345L)
                )
            )
            .isEqualTo(12345L)
    }

    private fun animatedBrushPaint(animationDurationMillis: Long) =
        BrushPaint(
            textureLayers =
                listOf(
                    BrushPaint.StampingTexture(
                        clientTextureId = "foo",
                        animationFrames = 2,
                        animationRows = 1,
                        animationColumns = 2,
                        animationDurationMillis = animationDurationMillis,
                    )
                )
        )

    private val brushPaintWithNonAnimatedTextureLayers =
        BrushPaint(
            textureLayers =
                listOf(
                    BrushPaint.StampingTexture(
                        clientTextureId = "foo",
                        animationFrames = 1,
                        animationDurationMillis = 12345L,
                    )
                )
        )
}
