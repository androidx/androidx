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

package androidx.compose.ui.lighting

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shader

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun createFlatNormalShader(outline: Outline): Shader =
    RuntimeShader(SurfaceGeometryShaders.FlatSurfaceShader).apply { setOutline(outline) }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun createConvexNormalShader(outline: Outline, bevelWidth: Float): Shader =
    RuntimeShader(SurfaceGeometryShaders.ConvexSurfaceShader).apply {
        setOutline(outline)
        setFloatUniform(SurfaceGeometryShaders.UNIFORM_BEVEL, bevelWidth)
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun createConcaveNormalShader(outline: Outline, bevelWidth: Float): Shader =
    RuntimeShader(SurfaceGeometryShaders.ConcaveSurfaceShader).apply {
        setOutline(outline)
        setFloatUniform(SurfaceGeometryShaders.UNIFORM_BEVEL, bevelWidth)
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setOutline(outline: Outline) {
    if (outline is Outline.Rounded) {
        val roundRect = outline.roundRect
        setFloatUniform(
            SurfaceGeometryShaders.UNIFORM_CORNER_RADIUS,
            roundRect.topLeftCornerRadius.x,
            roundRect.topRightCornerRadius.x,
            roundRect.bottomRightCornerRadius.x,
            roundRect.bottomLeftCornerRadius.x,
        )
    } else {
        setFloatUniform(SurfaceGeometryShaders.UNIFORM_CORNER_RADIUS, 0f, 0f, 0f, 0f)
    }
    val bounds = outline.bounds
    setFloatUniform(
        SurfaceGeometryShaders.UNIFORM_CORNER_POSITIONS,
        bounds.left,
        bounds.right,
        bounds.top,
        bounds.bottom,
    )
}
