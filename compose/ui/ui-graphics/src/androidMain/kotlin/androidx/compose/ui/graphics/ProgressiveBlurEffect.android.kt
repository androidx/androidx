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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurUniform
import androidx.compose.ui.unit.Density

/**
 * Base progressive-blur [RenderEffect] backing [BlurRadiusSpec.createRenderEffect].
 *
 * Compares value-equal over (radius, size, density, edgeTreatment) so consumers like
 * `Modifier.blur` can skip pushing an unchanged effect to the layer.
 */
@Immutable
internal abstract class ProgressiveBlurRenderEffect(
    internal val radius: BlurRadiusSpec,
    internal val size: Size,
    internal val density: Density,
    internal val edgeTreatment: TileMode,
) : RenderEffect() {

    /** False when [radius] wraps caller-mutable state that defeats structural comparison. */
    protected open val hasStructuralEquality: Boolean
        get() = true

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressiveBlurRenderEffect) return false

        // A shader-based radius wraps mutable user-owned shader state (uniform updates).
        // Since android.graphics.RenderEffect snapshots uniform values at creation, two wrappers
        // built over the same mutable state must never compare equal.
        if (!hasStructuralEquality || !other.hasStructuralEquality) return false

        if (radius != other.radius) return false
        if (size != other.size) return false
        if (density != other.density) return false
        if (edgeTreatment != other.edgeTreatment) return false

        return true
    }

    final override fun hashCode(): Int {
        var result = radius.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + density.hashCode()
        result = 31 * result + edgeTreatment.hashCode()
        return result
    }

    final override fun toString(): String {
        return "ProgressiveBlurEffect(radius=$radius, size=$size, density=$density, " +
            "edgeTreatment=$edgeTreatment)"
    }
}

/** The uniform fast path: the platform blur effect, supported from API 31. */
internal class UniformBlurRenderEffect(
    private val spec: BlurUniform,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : ProgressiveBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun createRenderEffect(): android.graphics.RenderEffect {
        val radiusPx = with(density) { spec.radius.toPx() }
        if (radiusPx <= 0f) return identityRenderEffect()
        val tile =
            if (edgeTreatment == TileMode.Decal) {
                android.graphics.Shader.TileMode.DECAL
            } else {
                android.graphics.Shader.TileMode.CLAMP
            }
        return android.graphics.RenderEffect.createBlurEffect(radiusPx, radiusPx, tile)
    }
}

/**
 * Base for spatially-varying radii realized through the runtime-shader pipeline, which requires
 * API 33. Below that the effect resolves to an identity effect and content renders unblurred.
 */
internal abstract class RuntimeShaderBlurRenderEffect(
    radius: BlurRadiusSpec,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : ProgressiveBlurRenderEffect(radius, size, density, edgeTreatment) {

    protected val isDecal: Boolean
        get() = edgeTreatment == TileMode.Decal

    override fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.S)
    final override fun createRenderEffect(): android.graphics.RenderEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buildRuntimeShaderEffect() ?: identityRenderEffect()
        } else {
            identityRenderEffect()
        }

    /** Builds the platform effect, or null when the configuration resolves to no blur. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    protected abstract fun buildRuntimeShaderEffect(): android.graphics.RenderEffect?
}

/** Renders content unchanged using a zero-offset [android.graphics.RenderEffect]. */
@RequiresApi(Build.VERSION_CODES.S)
private fun identityRenderEffect(): android.graphics.RenderEffect =
    android.graphics.RenderEffect.createOffsetEffect(0f, 0f)
