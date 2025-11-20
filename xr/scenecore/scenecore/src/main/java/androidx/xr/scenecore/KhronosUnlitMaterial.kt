/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.runtime.MaterialResource as RtMaterial
import androidx.xr.scenecore.runtime.RenderingRuntime

/**
 * Represents a [Material] in SceneCore.
 *
 * A [Material] defines the visual appearance of a surface when rendered. It encapsulates properties
 * like color, texture, and how light interacts with the surface.
 *
 * It's important to close a [Material] when it's no longer needed to free up resources. This can be
 * done by calling the [close] method or letting it get garbage collected.
 */
public interface Material : AutoCloseable {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val material: RtMaterial

    /**
     * Closes the given [Material].
     *
     * The [Material] can be explicitly closed at anytime or garbage collected. In both cases, its
     * resources are freed and an exception will be thrown if the [Material] is used after being
     * closed.
     */
    @MainThread override public fun close()
}

/**
 * Represents an unlit material, which is not affected by scene lighting.
 *
 * This material type is defined by a base color and supports a limited set of parameters. It is a
 * direct implementation of the following glTF features:
 * - [KHR_materials_unlit
 *   extension](https://github.com/KhronosGroup/glTF/tree/main/extensions/2.0/Khronos/KHR_materials_unlit)
 * - [baseColorTexture](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#metallic-roughness-material)
 * - [baseColorFactor](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#metallic-roughness-material)
 * - [alphaMode (and
 *   alphaCutoff)](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#alpha-coverage)
 */
public class KhronosUnlitMaterial
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val material: RtMaterial,
    internal val alphaMode: AlphaMode,
    internal val session: Session,
) : Material {

    /**
     * Closes the [KhronosUnlitMaterial] and releases its underlying graphics resources.
     *
     * After being closed, the [KhronosUnlitMaterial] should not be used further.
     */
    @MainThread
    override public fun close() {
        session.renderingRuntime.destroyKhronosPbrMaterial(material)
    }

    /**
     * Sets the material's base color using a texture.
     *
     * By default this is a white texture, where all pixels are [1, 1, 1, 1]. In other words, if
     * this is left as default, the base color will always be the base color factor.
     *
     * @param texture The [Texture] to be used as the base color texture, in sRGB color space.
     * @param sampler The [TextureSampler] to be used when sampling the base color texture.
     */
    @JvmOverloads
    @MainThread
    public fun setBaseColorTexture(texture: Texture, sampler: TextureSampler = TextureSampler()) {
        session.renderingRuntime.setBaseColorTextureOnKhronosPbrMaterial(
            material,
            texture.texture,
            sampler.toRtTextureSampler(),
        )
    }

    /**
     * Sets a linear multiplier for the base color.
     *
     * By default this is [1, 1, 1, 1].
     *
     * @param factor The [Vector4] (RGBA) factor multiplied component-wise with the base color
     *   texture.
     */
    @MainThread
    public fun setBaseColorFactor(factor: Vector4) {
        session.renderingRuntime.setBaseColorFactorsOnKhronosPbrMaterial(material, factor)
    }

    /**
     * Sets the alpha cutoff threshold.
     *
     * This value is only used when the material's [alphaMode] is [AlphaMode.MASK].
     *
     * @param alphaCutoff The alpha cutoff. Fragments with alpha below this value are discarded.
     *   Default is 0.5. Valid values are between 0.0 and 1.0, inclusive.
     * @throws IllegalArgumentException if the material's alphaMode is not ALPHA_MODE_MASK.
     */
    @MainThread
    public fun setAlphaCutoff(@FloatRange(from = 0.0, to = 1.0) alphaCutoff: Float) {
        check(alphaMode == AlphaMode.MASK) {
            "Alpha cutoff can only be set when the material's alpha mode is set to ALPHA_MODE_MASK."
        }
        session.renderingRuntime.setAlphaCutoffOnKhronosPbrMaterial(material, alphaCutoff)
    }

    public companion object {
        internal suspend fun createAsync(
            renderingRuntime: RenderingRuntime,
            alphaMode: AlphaMode,
            session: Session,
        ): KhronosUnlitMaterial {
            val material =
                renderingRuntime.createKhronosPbrMaterialAsync(
                    alphaMode.toRtKhronosUnlitMaterialSpec()
                )
            return KhronosUnlitMaterial(material, alphaMode, session)
        }

        /**
         * Asynchronously creates a [KhronosUnlitMaterial].
         *
         * @param session The active [Session] in which to create the material.
         * @param alphaMode The [AlphaMode] to use for the material.
         * @return The newly created [KhronosUnlitMaterial].
         */
        @MainThread
        @JvmStatic
        public suspend fun create(session: Session, alphaMode: AlphaMode): KhronosUnlitMaterial {
            return KhronosUnlitMaterial.createAsync(session.renderingRuntime, alphaMode, session)
        }
    }
}
