/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.graphics.blur

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.UniformBlurRenderEffect
import androidx.compose.ui.unit.Density

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurUniform,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = UniformBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurVerticalGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = VerticalBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurVerticalStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = VerticalStopsBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurHorizontalGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = HorizontalBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurHorizontalStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = HorizontalStopsBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurLinearGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = LinearBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurLinearStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = LinearStopsBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurRadialGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = RadialBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurRadialStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = RadialStopsBlurRenderEffect(spec, size, density, edgeTreatment)

internal actual fun ActualProgressiveBlurEffect(
    spec: BlurRadiusShader,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect = ShaderBlurRenderEffect(spec, size, density, edgeTreatment)
