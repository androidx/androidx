/*
 * Copyright (C) 2026 The Android Open Source Project
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

@file:JvmName("BrushCoatSamples")

package androidx.ink.brush.samples

import androidx.annotation.Sampled
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.ColorFunction
import androidx.ink.brush.BrushPaint.StampingTexture
import androidx.ink.brush.BrushTip

/**
 * Creates a brush coat with a fallback paint that should be used by renderers that can't support
 * the preferred paint.
 */
@Sampled
public fun createBrushCoatWithPaintFallback(): BrushCoat {
    // For this brush coat, we'd prefer to stamp a translucent texture onto each particle.
    val preferredPaint = BrushPaint(textureLayers = listOf(StampingTexture("translucent-gradient")))
    // However, some rendering contexts (such as older Android versions, or stroke meshes embedded
    // into PDFs) don't support stamping textures. As a fallback, we can use a color function to
    // apply
    // a flat translucency onto the stroke.
    val fallbackPaint = BrushPaint(colorFunctions = listOf(ColorFunction.OpacityMultiplier(0.5f)))
    return BrushCoat(
        tip = BrushTip(particleGapDistanceScale = 1.0f),
        paintPreferences = listOf(preferredPaint, fallbackPaint),
    )
}
