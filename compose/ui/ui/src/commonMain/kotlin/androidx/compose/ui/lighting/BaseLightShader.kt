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
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEachIndexed

/**
 * Base implementation of [LightShader] that caches the last uploaded inputs, packs [Light] uniform
 * arrays, resolves [SurfaceGeometry], and only invokes the platform-specific [uploadLights],
 * [uploadMaterial], and [uploadGeometry] hooks for uniform groups whose inputs have changed.
 *
 * Subclasses must call [initialize] from their `init` block once their underlying platform shader
 * is ready to receive uniforms.
 */
internal abstract class BaseLightShader(
    lights: List<Light>,
    surfaceMaterial: SurfaceMaterial,
    surfaceGeometrySpec: SurfaceGeometrySpec,
    density: Density,
    layoutDirection: LayoutDirection,
    size: Size,
) : LightShader {

    protected abstract val nativeShader: Shader
    private val currentLights = mutableListOf<Light>().apply { addAll(lights) }

    private var currentMaterial: SurfaceMaterial = surfaceMaterial
    private var currentGeometrySpec: SurfaceGeometrySpec = surfaceGeometrySpec
    private var currentLayoutDirection: LayoutDirection = layoutDirection
    private var currentSize: Size = size

    private var currentDensity: Density = Density(density.density, density.fontScale)

    private val lightTypes = IntArray(LightShader.MaxLightsAllowed)
    private val lightColors = FloatArray(LightShader.MaxLightsAllowed * LightColorComponents)
    private val lightPositions = FloatArray(LightShader.MaxLightsAllowed * LightVectorComponents)
    private val lightDirections = FloatArray(LightShader.MaxLightsAllowed * LightVectorComponents)
    private val lightFalloffRadii = FloatArray(LightShader.MaxLightsAllowed)
    private val lightCutoffCosines = FloatArray(LightShader.MaxLightsAllowed)
    private val lightIntensities = FloatArray(LightShader.MaxLightsAllowed)

    protected val materialColor: FloatArray = FloatArray(LightColorComponents)

    /** Uploads the initial lights, material, and geometry uniforms to the platform shader. */
    protected fun initialize() {
        packAndUploadLights(currentLights)
        uploadMaterial(currentMaterial)
        val geometry =
            currentGeometrySpec.createGeometry(currentSize, currentLayoutDirection, currentDensity)
        uploadGeometry(geometry)
    }

    override fun applyTo(paint: Paint) {
        paint.shader = nativeShader
    }

    /** Re-uploads every uniform whose inputs changed since the last call. */
    override fun update(
        lights: List<Light>,
        surfaceMaterial: SurfaceMaterial,
        surfaceGeometrySpec: SurfaceGeometrySpec,
        density: Density,
        layoutDirection: LayoutDirection,
        size: Size,
    ) {
        val densityChanged = currentDensity.density != density.density
        if (densityChanged) {
            currentDensity = Density(density.density, density.fontScale)
        }

        if (densityChanged || currentLights != lights) {
            packAndUploadLights(lights)
            currentLights.clear()
            currentLights.addAll(lights)
        }

        if (currentMaterial != surfaceMaterial) {
            currentMaterial = surfaceMaterial
            uploadMaterial(surfaceMaterial)
        }

        val geometryChanged =
            densityChanged ||
                currentGeometrySpec != surfaceGeometrySpec ||
                currentLayoutDirection != layoutDirection ||
                currentSize != size
        if (geometryChanged) {
            val geometry = surfaceGeometrySpec.createGeometry(size, layoutDirection, currentDensity)
            currentGeometrySpec = surfaceGeometrySpec
            currentLayoutDirection = layoutDirection
            currentSize = size
            uploadGeometry(geometry)
        }
    }

    private fun packAndUploadLights(lights: List<Light>) {
        requirePrecondition(lights.size <= LightShader.MaxLightsAllowed) {
            "At most ${LightShader.MaxLightsAllowed} lights are supported, but ${lights.size} were provided"
        }

        lightTypes.fill(0)
        lightColors.fill(0f)
        lightPositions.fill(0f)
        lightDirections.fill(0f)
        lightFalloffRadii.fill(0f)
        lightCutoffCosines.fill(0f)
        lightIntensities.fill(0f)

        val density = currentDensity
        lights.fastForEachIndexed { index, light ->
            with(light) {
                density.packUniforms(
                    index = index,
                    lightTypes = lightTypes,
                    lightColors = lightColors,
                    lightPositions = lightPositions,
                    lightDirections = lightDirections,
                    lightFalloffRadii = lightFalloffRadii,
                    lightCutoffCosines = lightCutoffCosines,
                    lightIntensities = lightIntensities,
                )
            }
        }

        uploadLights(
            lightCount = lights.size,
            lightTypes = lightTypes,
            lightColors = lightColors,
            lightPositions = lightPositions,
            lightDirections = lightDirections,
            lightFalloffRadii = lightFalloffRadii,
            lightCutoffCosines = lightCutoffCosines,
            lightIntensities = lightIntensities,
        )
    }

    /** Uploads the packed light uniform arrays for [lightCount] active lights. */
    protected abstract fun uploadLights(
        lightCount: Int,
        lightTypes: IntArray,
        lightColors: FloatArray,
        lightPositions: FloatArray,
        lightDirections: FloatArray,
        lightFalloffRadii: FloatArray,
        lightCutoffCosines: FloatArray,
        lightIntensities: FloatArray,
    )

    /** Uploads the uniforms for [material]. */
    protected abstract fun uploadMaterial(material: SurfaceMaterial)

    /** Uploads the normal shader and thickness for the resolved [geometry]. */
    protected abstract fun uploadGeometry(geometry: SurfaceGeometry)
}
