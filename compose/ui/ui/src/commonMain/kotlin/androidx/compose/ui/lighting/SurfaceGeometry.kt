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

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shader

/**
 * A [SurfaceGeometrySpec] resolved against a concrete size, layout direction and density.
 *
 * A geometry exposes a shader that, for every fragment, returns the surface normal in `xyz` and a
 * coverage mask in `a`: an alpha of `0` means the fragment lies outside of the surface and must not
 * be lit.
 */
internal abstract class SurfaceGeometry {

    /**
     * Height of the surface towards the viewer. The attenuation of the Light is calculated based on
     * the resultant distance calculated as `lightElevation - thickness`.
     */
    abstract val thickness: Float

    /** The resolved outline of the surface, in pixels. */
    abstract val outline: Outline

    /**
     * Returns the normal shader for this geometry. This may create a new Shader instance on some
     * platforms, if uniforms have changed.
     */
    abstract fun getOrBuildNormalShader(): Shader
}

/** A surface that is completely flat. */
internal class FlatSurfaceGeometry(
    override val outline: Outline,
    override val thickness: Float,
) : SurfaceGeometry() {
    private var normalShader: Shader? = null

    override fun getOrBuildNormalShader(): Shader =
        normalShader ?: createFlatNormalShader(outline).also { normalShader = it }
}

/**
 * A surface that bulges outwards, with normals tilting away from the center across the bevel.
 *
 * @param bevelWidth width of the curved band along the outline, in pixels.
 */
internal class ConvexSurfaceGeometry(
    override val outline: Outline,
    override val thickness: Float,
    val bevelWidth: Float,
) : SurfaceGeometry() {
    private var normalShader: Shader? = null

    override fun getOrBuildNormalShader(): Shader =
        normalShader ?: createConvexNormalShader(outline, bevelWidth).also { normalShader = it }
}

/**
 * A surface that dishes inwards, with normals tilting towards the center across the bevel.
 *
 * @param bevelWidth width of the curved band along the outline, in pixels.
 */
internal class ConcaveSurfaceGeometry(
    override val outline: Outline,
    override val thickness: Float,
    val bevelWidth: Float,
) : SurfaceGeometry() {
    private var normalShader: Shader? = null

    override fun getOrBuildNormalShader(): Shader =
        normalShader ?: createConcaveNormalShader(outline, bevelWidth).also { normalShader = it }
}

internal expect fun createFlatNormalShader(outline: Outline): Shader

internal expect fun createConvexNormalShader(outline: Outline, bevelWidth: Float): Shader

internal expect fun createConcaveNormalShader(outline: Outline, bevelWidth: Float): Shader
