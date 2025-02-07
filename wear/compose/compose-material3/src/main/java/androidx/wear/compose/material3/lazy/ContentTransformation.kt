/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress

internal fun GraphicsLayerScope.contentTransformation(
    behavior: TransformingLazyColumnScrollTransformBehavior,
    scrollProgress: () -> TransformingLazyColumnItemScrollProgress
) =
    with(behavior) {
        scrollProgress()
            .takeIf { it != TransformingLazyColumnItemScrollProgress.Unspecified }
            ?.let {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                shape =
                    object : Shape {
                        override fun createOutline(
                            size: Size,
                            layoutDirection: LayoutDirection,
                            density: Density
                        ): Outline =
                            Outline.Rounded(
                                RoundRect(
                                    rect =
                                        Rect(
                                            left = 0f,
                                            top = 0f,
                                            right =
                                                size.width -
                                                    2f * size.width * it.contentXOffsetFraction,
                                            bottom = it.morphedHeight(size.height)
                                        ),
                                )
                            )
                    }
                translationX = size.width * it.contentXOffsetFraction * it.scale
                translationY = -1f * size.height * (1f - it.scale) / 2f
                alpha = it.contentAlpha
                scaleX = it.scale
                scaleY = it.scale
            }
    }

internal fun GraphicsLayerScope.contentTransformation(
    transformState: TransformationState?,
) =
    transformState?.let {
        compositingStrategy = CompositingStrategy.Offscreen
        clip = true
        shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline =
                    Outline.Rounded(
                        RoundRect(
                            rect =
                                Rect(
                                    left = 0f,
                                    top = 0f,
                                    right =
                                        size.width - 2f * size.width * it.contentXOffsetFraction,
                                    bottom = it.morphedHeight
                                ),
                        )
                    )
            }
        translationX = size.width * it.contentXOffsetFraction * it.scale
        translationY = -1f * size.height * (1f - it.scale) / 2f
        alpha = it.contentAlpha
        scaleX = it.scale
        scaleY = it.scale
    }
