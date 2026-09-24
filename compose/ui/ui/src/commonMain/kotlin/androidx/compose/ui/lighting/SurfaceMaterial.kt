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

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.internal.requirePrecondition

/**
 * Describes how a surface reflects the light that reaches it.
 *
 * A material has three independent reflection components that are evaluated for every light
 * illuminating the surface and summed, so adding a component can only make the result brighter:
 * - [diffuse] scatters light evenly in every direction and provides the overall shading.
 * - [specular] adds the tight highlight of a glossy surface.
 * - [fresnel] brightens the surface where it is viewed at a grazing angle.
 *
 * The [diffuse] and [specular] components are calculated as per the Blinn-Phong reflection model.
 * The [fresnel] component is calculated using Schlick's approximation.
 *
 * Any component that is not set is disabled and contributes nothing to the lighting, so
 * [SurfaceMaterial.None] has all reflection components disabled and renders no lighting at all. Set
 * at least [diffuse] to see a result.
 */
@Immutable
public class SurfaceMaterial
internal constructor(
    internal val diffuseColor: Color = Color.Unspecified,
    internal val specularColor: Color = Color.Unspecified,
    internal val specularShininess: Float = 1f,
    internal val fresnelReflectivity: Float = 0f,
    internal val fresnelIntensity: Float = 0f,
) {
    /**
     * Returns a copy of this [SurfaceMaterial] with the diffuse reflection component set to
     * [color], leaving the other reflection components unchanged.
     *
     * Diffuse reflection scatters light equally in every direction, so its contribution depends
     * only on the angle between the surface normal and the light and is independent of the angle it
     * is viewed from. This is what gives a lit surface its overall sense of shape.
     *
     * @param color the color the surface reflects. It is multiplied by the color and intensity of
     *   each light, so a white surface takes on the color of the light while a red surface only
     *   reflects the red it receives. Pass [Color.Unspecified] to disable diffuse reflection.
     */
    public fun diffuse(color: Color): SurfaceMaterial =
        SurfaceMaterial(
            diffuseColor = color,
            specularColor = specularColor,
            specularShininess = specularShininess,
            fresnelReflectivity = fresnelReflectivity,
            fresnelIntensity = fresnelIntensity,
        )

    /**
     * Returns a copy of this [SurfaceMaterial] with the specular highlight component set to [color]
     * and [shininess], leaving the other reflection components unchanged.
     *
     * Specular reflection is the bright highlight that appears where a glossy surface reflects a
     * light back towards the viewer. Unlike [diffuse], it moves as the surface tilts and can be
     * used to give the surface a polished or glossy look.
     *
     * @param color the color of the highlight. It is multiplied by the color and intensity of each
     *   light. Pass [Color.Unspecified] to disable specular reflection.
     * @param shininess the specular exponent. Larger values concentrate the highlight into a
     *   smaller, sharper spot; smaller values spread it into a broad highlight. Must be positive.
     * @throws IllegalArgumentException if [shininess] is not positive.
     */
    public fun specular(
        color: Color,
        @FloatRange(from = 0.0, fromInclusive = false) shininess: Float,
    ): SurfaceMaterial {
        requirePrecondition(shininess > 0f) {
            "shininess must be greater than 0, but was $shininess"
        }
        return SurfaceMaterial(
            diffuseColor = diffuseColor,
            specularColor = color,
            specularShininess = shininess,
            fresnelReflectivity = fresnelReflectivity,
            fresnelIntensity = fresnelIntensity,
        )
    }

    /**
     * Returns a copy of this [SurfaceMaterial] with the Fresnel grazing-angle component set to
     * [reflectivity] and [intensity], leaving the other reflection components unchanged.
     *
     * Real surfaces become more reflective the closer to edge-on they are viewed, which looks like
     * a rim of light around the curved parts of a surface. The term is estimated with Schlick's
     * approximation and added on top of [diffuse] and [specular].
     *
     * The rim needs geometry that curves away from the viewer, such as
     * [SurfaceGeometrySpec.convex]. On a surface built with [SurfaceGeometrySpec.flat] the interior
     * normals all face the viewer directly, so the grazing angle term vanishes and only the head-on
     * [reflectivity] is left, which only brightens the surface with no rim.
     *
     * @param reflectivity reflectance when the surface is viewed head-on. `0.04` is representative
     *   of common non-metals such as plastic or glass; polished metals are closer to `1`. A value
     *   of `0` disables the Fresnel contribution. Expected to be in the range `0..1`.
     * @param intensity multiplier applied to the Fresnel contribution. Use it to strengthen or
     *   soften the rim without changing the physical reflectance. A value of `0` disables the
     *   Fresnel contribution. Expected to be in the range `0..1`.
     * @throws IllegalArgumentException if [reflectivity] or [intensity] is not in `[0, 1]`.
     */
    public fun fresnel(
        @FloatRange(from = 0.0, to = 1.0) reflectivity: Float,
        @FloatRange(from = 0.0, to = 1.0) intensity: Float = 1.0f,
    ): SurfaceMaterial {
        requirePrecondition(reflectivity in 0f..1f) {
            "reflectivity must be in [0, 1], but was $reflectivity"
        }
        requirePrecondition(intensity in 0f..1f) {
            "intensity must be in [0, 1], but was $intensity"
        }
        return SurfaceMaterial(
            diffuseColor = diffuseColor,
            specularColor = specularColor,
            specularShininess = specularShininess,
            fresnelReflectivity = reflectivity,
            fresnelIntensity = intensity,
        )
    }

    internal val isDiffuseEnabled: Boolean
        get() = diffuseColor.isSpecified

    internal val isSpecularEnabled: Boolean
        get() = specularColor.isSpecified

    internal val isFresnelEnabled: Boolean
        get() = fresnelReflectivity > 0f && fresnelIntensity > 0f

    internal fun packDiffuseColor(outColor: FloatArray): FloatArray =
        outColor.fillFrom(diffuseColor)

    internal fun packSpecularColor(outColor: FloatArray): FloatArray =
        outColor.fillFrom(specularColor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfaceMaterial) return false
        if (diffuseColor != other.diffuseColor) return false
        if (specularColor != other.specularColor) return false
        if (specularShininess != other.specularShininess) return false
        if (fresnelReflectivity != other.fresnelReflectivity) return false
        if (fresnelIntensity != other.fresnelIntensity) return false
        return true
    }

    override fun hashCode(): Int {
        var result = diffuseColor.hashCode()
        result = 31 * result + specularColor.hashCode()
        result = 31 * result + specularShininess.hashCode()
        result = 31 * result + fresnelReflectivity.hashCode()
        result = 31 * result + fresnelIntensity.hashCode()
        return result
    }

    override fun toString(): String =
        "SurfaceMaterial(" +
            "diffuseColor=$diffuseColor, " +
            "specularColor=$specularColor, " +
            "specularShininess=$specularShininess, " +
            "fresnelReflectivity=$fresnelReflectivity, " +
            "fresnelIntensity=$fresnelIntensity)"

    public companion object {
        /** A [SurfaceMaterial] with all reflection components disabled. */
        public val None: SurfaceMaterial = SurfaceMaterial()

        /**
         * Creates a [SurfaceMaterial] with the diffuse reflection component set to [color].
         *
         * @param color the color the surface reflects. It is multiplied by the color and intensity
         *   of each light, so a white surface takes on the color of the light while a red surface
         *   only reflects the red it receives.
         * @see SurfaceMaterial.diffuse
         */
        public fun diffuse(color: Color): SurfaceMaterial = None.diffuse(color)

        /**
         * Creates a [SurfaceMaterial] with the specular highlight component set to [color] and
         * [shininess].
         *
         * @param color the color of the highlight. It is multiplied by the color and intensity of
         *   each light.
         * @param shininess the specular exponent. Larger values concentrate the highlight into a
         *   smaller, sharper spot; smaller values spread it into a broad highlight. Must be
         *   positive.
         * @throws IllegalArgumentException if [shininess] is not positive.
         * @see SurfaceMaterial.specular
         */
        public fun specular(
            color: Color,
            @FloatRange(from = 0.0, fromInclusive = false) shininess: Float,
        ): SurfaceMaterial = None.specular(color, shininess)

        /**
         * Creates a [SurfaceMaterial] with the Fresnel grazing-angle component set to
         * [reflectivity] and [intensity].
         *
         * @param reflectivity reflectance when the surface is viewed head-on. `0.04` is
         *   representative of common non-metals such as plastic or glass; polished metals are
         *   closer to `1`. A value of `0` disables the Fresnel contribution. Expected to be in the
         *   range `0..1`.
         * @param intensity multiplier applied to the Fresnel contribution. Use it to strengthen or
         *   soften the rim without changing the physical reflectance. A value of `0` disables the
         *   Fresnel contribution. Expected to be in the range `0..1`.
         * @throws IllegalArgumentException if [reflectivity] or [intensity] is not in `[0, 1]`.
         * @see SurfaceMaterial.fresnel
         */
        public fun fresnel(
            @FloatRange(from = 0.0, to = 1.0) reflectivity: Float,
            @FloatRange(from = 0.0, to = 1.0) intensity: Float = 1.0f,
        ): SurfaceMaterial = None.fresnel(reflectivity, intensity)
    }
}

/**
 * Converts [color] to [ColorSpaces.LinearExtendedSrgb] and writes its RGBA channels into the first
 * four elements of this array.
 */
private fun FloatArray.fillFrom(color: Color): FloatArray {
    val linearSrgbColor = color.convert(ColorSpaces.LinearExtendedSrgb)
    this[0] = linearSrgbColor.red
    this[1] = linearSrgbColor.green
    this[2] = linearSrgbColor.blue
    this[3] = linearSrgbColor.alpha
    return this
}
