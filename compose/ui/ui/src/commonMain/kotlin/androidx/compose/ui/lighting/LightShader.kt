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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Manages the shader that evaluates a Blinn-Phong lighting model for a single illuminated surface,
 * keeping its uniforms in sync with the lights, material, and resolved surface geometry.
 *
 * Call [update] to push changed inputs before drawing, then bind the shader via [applyTo].
 *
 * All lights passed to the `LightShader` factory and to [update] must already be resolved into the
 * surface's local coordinate space ([LightPosition.Local]) and contain at most [MaxLightsAllowed]
 * entries.
 */
internal interface LightShader {

    /** Applies the shader with the current state of uniforms to [paint]. */
    fun applyTo(paint: Paint)

    /**
     * Re-uploads every uniform whose inputs changed since the last call.
     *
     * @throws IllegalArgumentException if [lights] has more than [MaxLightsAllowed] entries or
     *   contains a light that is not positioned with [LightPosition.Local], or if
     *   [surfaceGeometrySpec] uses a shape that is not supported (see [SurfaceGeometrySpec]).
     */
    fun update(
        lights: List<Light>,
        surfaceMaterial: SurfaceMaterial,
        surfaceGeometrySpec: SurfaceGeometrySpec,
        density: Density,
        layoutDirection: LayoutDirection,
        size: Size,
    )

    companion object
}

/**
 * Creates a [LightShader] that lights the surface described by [surfaceGeometrySpec] and
 * [surfaceMaterial] with [lights].
 *
 * Use [LightShader.Companion.isSupported] to determine if the platform supports this.
 *
 * @param lights the lights illuminating the surface. Each must be positioned with
 *   [LightPosition.Local], and at most [LightShader.MaxLightsAllowed] are supported.
 * @param surfaceMaterial how the surface reflects the light that reaches it.
 * @param surfaceGeometrySpec the virtual three-dimensional shape of the surface.
 * @param density converts the `Dp` values of [lights] and [surfaceGeometrySpec] to pixels.
 * @param layoutDirection the layout direction used to resolve the surface's shape.
 * @param size size of the surface, in pixels.
 * @throws IllegalArgumentException if [lights] has more than [LightShader.MaxLightsAllowed] entries
 *   or contains a light that is not positioned with [LightPosition.Local], or if
 *   [surfaceGeometrySpec] uses a shape that is not supported (see [SurfaceGeometrySpec]).
 */
internal expect fun LightShader(
    lights: List<Light>,
    surfaceMaterial: SurfaceMaterial,
    surfaceGeometrySpec: SurfaceGeometrySpec,
    density: Density,
    layoutDirection: LayoutDirection,
    size: Size,
): LightShader

/** The maximum number of [Light]s the Shader supports. */
internal expect val LightShader.Companion.MaxLightsAllowed: Int

/** Whether the current platform can render [LightShader]. */
internal expect fun LightShader.Companion.isSupported(): Boolean
