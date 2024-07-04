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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset

actual class Shader

internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader = implementedInJetBrainsFork()

internal actual fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader = implementedInJetBrainsFork()

internal actual fun ActualSweepGradientShader(
    center: Offset,
    colors: List<Color>,
    colorStops: List<Float>?
): Shader = implementedInJetBrainsFork()

internal actual fun ActualImageShader(
    image: ImageBitmap,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader = implementedInJetBrainsFork()
